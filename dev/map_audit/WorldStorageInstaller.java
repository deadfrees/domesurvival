import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Offline installer for an audited storage_plan.csv. It uses Minecraft's own
 * RegionFile and NBT classes and refuses to overwrite anything except air.
 */
public final class WorldStorageInstaller {
    private static final int BLOCKS_PER_SECTION = 4096;

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected: <region-dir> <storage-plan.csv> <journal.csv>");
        }
        Path regionDir = Path.of(args[0]).toAbsolutePath().normalize();
        Path planFile = Path.of(args[1]).toAbsolutePath().normalize();
        Path journalFile = Path.of(args[2]).toAbsolutePath().normalize();
        if (!Files.isDirectory(regionDir)) {
            throw new IOException("Region directory is missing: " + regionDir);
        }

        List<Placement> placements = readPlan(planFile);
        validateUniqueTargets(placements);
        validateAll(regionDir, placements);

        List<JournalEntry> journal = installAll(regionDir, placements);
        writeJournal(journalFile, journal);
        System.out.println("Installed storage blocks: " + placements.size());
        System.out.println("Modified chunks: " + journal.size());
        System.out.println("Journal: " + journalFile);
    }

    private static List<Placement> readPlan(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("Empty storage plan: " + file);
        }
        List<String> header = parseCsvLine(lines.get(0));
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) columns.put(header.get(i), i);
        for (String required : List.of("site_id", "status", "x", "y", "z", "facing")) {
            if (!columns.containsKey(required)) throw new IOException("Missing CSV column: " + required);
        }

        List<Placement> result = new ArrayList<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            if (lines.get(lineNumber).isBlank()) continue;
            List<String> values = parseCsvLine(lines.get(lineNumber));
            if (!"ADD".equals(value(values, columns, "status"))) continue;
            result.add(new Placement(
                    value(values, columns, "site_id"),
                    Integer.parseInt(value(values, columns, "x")),
                    Integer.parseInt(value(values, columns, "y")),
                    Integer.parseInt(value(values, columns, "z")),
                    value(values, columns, "facing")
            ));
        }
        if (result.isEmpty()) throw new IOException("No ADD placements in " + file);
        return List.copyOf(result);
    }

    private static void validateUniqueTargets(List<Placement> placements) {
        Map<String, String> occupied = new HashMap<>();
        for (Placement placement : placements) {
            String key = placement.x + "," + placement.y + "," + placement.z;
            String previous = occupied.putIfAbsent(key, placement.siteId);
            if (previous != null) {
                throw new IllegalStateException("Duplicate target " + key + " for " + previous + " and " + placement.siteId);
            }
            if (!List.of("north", "south", "east", "west", "up", "down").contains(placement.facing)) {
                throw new IllegalStateException("Invalid barrel facing for " + placement.siteId + ": " + placement.facing);
            }
        }
    }

    private static void validateAll(Path regionDir, List<Placement> placements) throws IOException {
        Map<RegionKey, List<Placement>> byRegion = groupByRegion(placements);
        int checked = 0;
        for (Map.Entry<RegionKey, List<Placement>> entry : byRegion.entrySet()) {
            Path regionPath = entry.getKey().path(regionDir);
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                Map<Long, List<Placement>> byChunk = groupByChunk(entry.getValue());
                for (Map.Entry<Long, List<Placement>> chunkEntry : byChunk.entrySet()) {
                    ChunkPos chunkPos = chunkPos(chunkEntry.getKey());
                    CompoundTag root = readChunk(region, chunkPos);
                    for (Placement placement : chunkEntry.getValue()) {
                        validateTarget(root, placement);
                        checked++;
                    }
                }
            }
        }
        if (checked != placements.size()) {
            throw new IOException("Validated " + checked + " placements, expected " + placements.size());
        }
        System.out.println("Preflight validated air targets: " + checked);
    }

    private static List<JournalEntry> installAll(Path regionDir, List<Placement> placements) throws Exception {
        Map<RegionKey, List<Placement>> byRegion = groupByRegion(placements);
        List<JournalEntry> journal = new ArrayList<>();
        for (Map.Entry<RegionKey, List<Placement>> entry : byRegion.entrySet()) {
            Path regionPath = entry.getKey().path(regionDir);
            String beforeRegionHash = sha256(regionPath);
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                Map<Long, List<Placement>> byChunk = groupByChunk(entry.getValue());
                for (Map.Entry<Long, List<Placement>> chunkEntry : byChunk.entrySet()) {
                    ChunkPos chunkPos = chunkPos(chunkEntry.getKey());
                    CompoundTag root = readChunk(region, chunkPos);
                    List<String> sites = new ArrayList<>();
                    for (Placement placement : chunkEntry.getValue()) {
                        validateTarget(root, placement);
                        installBarrel(root, placement);
                        sites.add(placement.siteId);
                    }
                    try (DataOutputStream output = region.getChunkDataOutputStream(chunkPos)) {
                        NbtIo.write(root, output);
                    }
                    journal.add(new JournalEntry(regionPath.getFileName().toString(),
                            chunkPos.x, chunkPos.z, String.join("|", sites), beforeRegionHash, ""));
                }
                region.flush();
            }
            String afterRegionHash = sha256(regionPath);
            for (int i = 0; i < journal.size(); i++) {
                JournalEntry row = journal.get(i);
                if (row.region.equals(regionPath.getFileName().toString()) && row.afterHash.isEmpty()) {
                    journal.set(i, new JournalEntry(row.region, row.chunkX, row.chunkZ, row.sites,
                            row.beforeHash, afterRegionHash));
                }
            }
        }
        return journal;
    }

    private static void validateTarget(CompoundTag root, Placement placement) throws IOException {
        CompoundTag chunk = root.contains("Level", Tag.TAG_COMPOUND) ? root.getCompound("Level") : root;
        ListTag sections = chunk.getList(chunk.contains("sections") ? "sections" : "Sections", Tag.TAG_COMPOUND);
        CompoundTag section = findSection(sections, Math.floorDiv(placement.y, 16));
        if (section == null) throw new IOException("Missing section for " + placement.siteId);
        CompoundTag blockStates = section.contains("block_states", Tag.TAG_COMPOUND)
                ? section.getCompound("block_states") : section;
        ListTag palette = blockStates.getList(blockStates.contains("palette") ? "palette" : "Palette", Tag.TAG_COMPOUND);
        long[] data = blockStates.getLongArray(blockStates.contains("data") ? "data" : "BlockStates");
        int blockIndex = localBlockIndex(placement.x, placement.y, placement.z);
        int paletteIndex = paletteIndexAt(blockIndex, palette.size(), data);
        String current = palette.getCompound(paletteIndex).getString("Name");
        if (!isAir(current)) {
            throw new IOException("Target is not air for " + placement.siteId + " at "
                    + placement.x + "," + placement.y + "," + placement.z + ": " + current);
        }

        ListTag blockEntities = chunk.getList(chunk.contains("block_entities") ? "block_entities" : "TileEntities", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag blockEntity = blockEntities.getCompound(i);
            if (blockEntity.getInt("x") == placement.x
                    && blockEntity.getInt("y") == placement.y
                    && blockEntity.getInt("z") == placement.z) {
                throw new IOException("Block entity already exists at target for " + placement.siteId);
            }
        }
    }

    private static void installBarrel(CompoundTag root, Placement placement) throws IOException {
        CompoundTag chunk = root.contains("Level", Tag.TAG_COMPOUND) ? root.getCompound("Level") : root;
        String sectionsKey = chunk.contains("sections") ? "sections" : "Sections";
        ListTag sections = chunk.getList(sectionsKey, Tag.TAG_COMPOUND);
        CompoundTag section = findSection(sections, Math.floorDiv(placement.y, 16));
        if (section == null) throw new IOException("Missing section for " + placement.siteId);
        String blockStatesKey = section.contains("block_states", Tag.TAG_COMPOUND) ? "block_states" : null;
        CompoundTag blockStates = blockStatesKey == null ? section : section.getCompound(blockStatesKey);
        String paletteKey = blockStates.contains("palette") ? "palette" : "Palette";
        String dataKey = blockStates.contains("data") ? "data" : "BlockStates";
        ListTag palette = blockStates.getList(paletteKey, Tag.TAG_COMPOUND);
        long[] oldData = blockStates.getLongArray(dataKey);

        int[] indices = unpackPaletteIndices(palette.size(), oldData);
        int barrelIndex = findOrAddBarrelState(palette, placement.facing);
        indices[localBlockIndex(placement.x, placement.y, placement.z)] = barrelIndex;
        blockStates.putLongArray(dataKey, packPaletteIndices(palette.size(), indices));
        blockStates.put(paletteKey, palette);
        if (blockStatesKey != null) section.put(blockStatesKey, blockStates);
        chunk.put(sectionsKey, sections);

        String entitiesKey = chunk.contains("block_entities") ? "block_entities" : "TileEntities";
        ListTag blockEntities = chunk.getList(entitiesKey, Tag.TAG_COMPOUND);
        CompoundTag barrel = new CompoundTag();
        barrel.putString("id", "minecraft:barrel");
        barrel.putInt("x", placement.x);
        barrel.putInt("y", placement.y);
        barrel.putInt("z", placement.z);
        barrel.put("Items", new ListTag());
        blockEntities.add(barrel);
        chunk.put(entitiesKey, blockEntities);
    }

    private static CompoundTag findSection(ListTag sections, int sectionY) {
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag section = sections.getCompound(i);
            if (section.getByte("Y") == (byte) sectionY) return section;
        }
        return null;
    }

    private static int findOrAddBarrelState(ListTag palette, String facing) {
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag state = palette.getCompound(i);
            if (!"minecraft:barrel".equals(state.getString("Name"))) continue;
            CompoundTag properties = state.getCompound("Properties");
            if (facing.equals(properties.getString("facing")) && "false".equals(properties.getString("open"))) {
                return i;
            }
        }
        CompoundTag state = new CompoundTag();
        state.putString("Name", "minecraft:barrel");
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", facing);
        properties.putString("open", "false");
        state.put("Properties", properties);
        palette.add(state);
        return palette.size() - 1;
    }

    private static int localBlockIndex(int x, int y, int z) {
        return (Math.floorMod(y, 16) << 8) | (Math.floorMod(z, 16) << 4) | Math.floorMod(x, 16);
    }

    private static int paletteIndexAt(int index, int paletteSize, long[] data) {
        if (paletteSize <= 1 || data.length == 0) return 0;
        int bits = bitsForPalette(paletteSize);
        int valuesPerLong = 64 / bits;
        int longIndex = index / valuesPerLong;
        int bitIndex = (index % valuesPerLong) * bits;
        long mask = (1L << bits) - 1L;
        return (int) ((data[longIndex] >>> bitIndex) & mask);
    }

    private static int[] unpackPaletteIndices(int paletteSize, long[] data) {
        int[] result = new int[BLOCKS_PER_SECTION];
        if (paletteSize <= 1 || data.length == 0) return result;
        for (int i = 0; i < result.length; i++) result[i] = paletteIndexAt(i, paletteSize, data);
        return result;
    }

    private static long[] packPaletteIndices(int paletteSize, int[] indices) {
        int bits = bitsForPalette(paletteSize);
        int valuesPerLong = 64 / bits;
        long mask = (1L << bits) - 1L;
        long[] data = new long[(BLOCKS_PER_SECTION + valuesPerLong - 1) / valuesPerLong];
        for (int i = 0; i < indices.length; i++) {
            int longIndex = i / valuesPerLong;
            int bitIndex = (i % valuesPerLong) * bits;
            data[longIndex] |= ((long) indices[i] & mask) << bitIndex;
        }
        return data;
    }

    private static int bitsForPalette(int paletteSize) {
        return Math.max(4, 32 - Integer.numberOfLeadingZeros(Math.max(1, paletteSize - 1)));
    }

    private static boolean isAir(String block) {
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
                || "minecraft:void_air".equals(block);
    }

    private static CompoundTag readChunk(RegionFile region, ChunkPos chunkPos) throws IOException {
        try (DataInputStream input = region.getChunkDataInputStream(chunkPos)) {
            if (input == null) throw new IOException("Missing chunk " + chunkPos);
            return NbtIo.read(input);
        }
    }

    private static Map<RegionKey, List<Placement>> groupByRegion(List<Placement> placements) {
        Map<RegionKey, List<Placement>> result = new LinkedHashMap<>();
        placements.stream().sorted(Comparator.comparingInt((Placement p) -> p.z).thenComparingInt(p -> p.x))
                .forEach(placement -> result.computeIfAbsent(
                        new RegionKey(Math.floorDiv(placement.x, 512), Math.floorDiv(placement.z, 512)),
                        ignored -> new ArrayList<>()).add(placement));
        return result;
    }

    private static Map<Long, List<Placement>> groupByChunk(List<Placement> placements) {
        Map<Long, List<Placement>> result = new LinkedHashMap<>();
        for (Placement placement : placements) {
            int chunkX = Math.floorDiv(placement.x, 16);
            int chunkZ = Math.floorDiv(placement.z, 16);
            long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(placement);
        }
        return result;
    }

    private static ChunkPos chunkPos(long key) {
        return new ChunkPos((int) (key >> 32), (int) key);
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return java.util.HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    private static void writeJournal(Path file, List<JournalEntry> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("region,chunk_x,chunk_z,sites,before_sha256,after_sha256");
        for (JournalEntry row : rows) {
            lines.add(csv(row.region) + "," + row.chunkX + "," + row.chunkZ + "," + csv(row.sites)
                    + "," + row.beforeHash + "," + row.afterHash);
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String value(List<String> values, Map<String, Integer> columns, String name) throws IOException {
        int index = columns.get(name);
        if (index >= values.size()) throw new IOException("Missing value for " + name);
        return values.get(index);
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record Placement(String siteId, int x, int y, int z, String facing) {}

    private record RegionKey(int x, int z) {
        private Path path(Path regionDir) {
            return regionDir.resolve("r." + x + "." + z + ".mca");
        }
    }

    private record JournalEntry(String region, int chunkX, int chunkZ, String sites,
                                String beforeHash, String afterHash) {}
}
