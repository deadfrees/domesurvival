import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Read-only Minecraft Anvil audit utility. It extracts containers and signs
 * and creates a two-block-per-pixel surface overview without loading the world.
 *
 * Usage:
 *   java WorldMapAudit <region-dir> <output-dir> <min-rx> <max-rx> <min-rz> <max-rz> [scale]
 */
public final class WorldMapAudit {
    private static final Set<String> AIR = Set.of(
            "minecraft:air", "minecraft:cave_air", "minecraft:void_air"
    );
    private static final int MIN_SECTION_Y = -4;
    private static final int MAX_SECTION_Y = 19;

    private final Path regionDir;
    private final Path outputDir;
    private final int minRegionX;
    private final int maxRegionX;
    private final int minRegionZ;
    private final int maxRegionZ;
    private final int scale;
    private final int minBlockX;
    private final int minBlockZ;
    private final BufferedImage overview;
    private final List<SiteRecord> sites;
    private final Map<Long, ChunkBlocks> siteChunks = new HashMap<>();
    private final List<ContainerRecord> containers = new ArrayList<>();
    private final List<MarkerRecord> markers = new ArrayList<>();
    private final List<StructureRecord> structures = new ArrayList<>();
    private final Map<String, Long> surfaceCounts = new HashMap<>();
    private int chunksRead;
    private int chunksFailed;

    private WorldMapAudit(Path regionDir, Path outputDir,
                          int minRegionX, int maxRegionX,
                          int minRegionZ, int maxRegionZ, int scale, Path sitesFile) throws IOException {
        this.regionDir = regionDir;
        this.outputDir = outputDir;
        this.minRegionX = minRegionX;
        this.maxRegionX = maxRegionX;
        this.minRegionZ = minRegionZ;
        this.maxRegionZ = maxRegionZ;
        this.scale = scale;
        this.sites = sitesFile == null ? List.of() : readSites(sitesFile);
        this.minBlockX = minRegionX * 512;
        this.minBlockZ = minRegionZ * 512;
        int width = ((maxRegionX - minRegionX + 1) * 512 + scale - 1) / scale;
        int height = ((maxRegionZ - minRegionZ + 1) * 512 + scale - 1) / scale;
        this.overview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = overview.createGraphics();
        graphics.setColor(new Color(15, 17, 19));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            throw new IllegalArgumentException(
                    "Expected: <region-dir> <output-dir> <min-rx> <max-rx> <min-rz> <max-rz> [scale]"
            );
        }
        WorldMapAudit audit = new WorldMapAudit(
                Path.of(args[0]), Path.of(args[1]),
                Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                Integer.parseInt(args[4]), Integer.parseInt(args[5]),
                args.length >= 7 ? Integer.parseInt(args[6]) : 2,
                args.length >= 8 ? Path.of(args[7]) : null
        );
        audit.run();
    }

    private void run() throws Exception {
        Files.createDirectories(outputDir);
        long started = System.nanoTime();
        int totalRegions = (maxRegionX - minRegionX + 1) * (maxRegionZ - minRegionZ + 1);
        int processedRegions = 0;

        for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                Path file = regionDir.resolve("r." + regionX + "." + regionZ + ".mca");
                if (Files.isRegularFile(file) && Files.size(file) >= 8192L) {
                    scanRegion(file, regionX, regionZ);
                }
                processedRegions++;
                if (processedRegions % 8 == 0 || processedRegions == totalRegions) {
                    System.out.printf(Locale.ROOT,
                            "regions=%d/%d chunks=%d containers=%d markers=%d failed=%d%n",
                            processedRegions, totalRegions, chunksRead,
                            containers.size(), markers.size(), chunksFailed);
                }
            }
        }

        drawContainerOverlay();
        writeContainers();
        writeStructures();
        writeMarkers();
        writeSurfaceCounts();
        if (!sites.isEmpty()) {
            writeStoragePlan();
        }
        writeSummary((System.nanoTime() - started) / 1_000_000_000.0D);
        ImageIO.write(overview, "png", outputDir.resolve("surface_overview.png").toFile());
    }

    private void scanRegion(Path file, int regionX, int regionZ) throws IOException {
        try (var channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            var header = java.nio.ByteBuffer.allocate(4096);
            readFully(channel, header);
            header.flip();
            int[] locations = new int[1024];
            for (int i = 0; i < locations.length; i++) {
                locations[i] = header.getInt();
            }

            for (int local = 0; local < 1024; local++) {
                int location = locations[local];
                int sectorOffset = location >>> 8;
                int sectorCount = location & 0xFF;
                if (sectorOffset < 2 || sectorCount == 0) {
                    continue;
                }

                int localX = local & 31;
                int localZ = local >>> 5;
                int chunkX = regionX * 32 + localX;
                int chunkZ = regionZ * 32 + localZ;
                try {
                    Map<String, Object> root = readChunk(channel, file, sectorOffset, sectorCount, chunkX, chunkZ);
                    if (root != null) {
                        inspectChunk(root, chunkX, chunkZ);
                        chunksRead++;
                    }
                } catch (Exception exception) {
                    chunksFailed++;
                    System.err.printf(Locale.ROOT, "Failed chunk %d,%d in %s: %s%n",
                            chunkX, chunkZ, file.getFileName(), exception.getMessage());
                }
            }
        }
    }

    private Map<String, Object> readChunk(java.nio.channels.SeekableByteChannel channel,
                                          Path regionFile,
                                          int sectorOffset,
                                          int sectorCount,
                                          int chunkX,
                                          int chunkZ) throws IOException {
        channel.position((long) sectorOffset * 4096L);
        var lengthBuffer = java.nio.ByteBuffer.allocate(5);
        readFully(channel, lengthBuffer);
        lengthBuffer.flip();
        int length = lengthBuffer.getInt();
        int compressionByte = Byte.toUnsignedInt(lengthBuffer.get());
        boolean external = (compressionByte & 0x80) != 0;
        int compression = compressionByte & 0x7F;
        if (length <= 0 || (!external && length > sectorCount * 4096 - 4)) {
            throw new IOException("invalid chunk length " + length);
        }

        InputStream source;
        if (external) {
            Path externalFile = regionFile.getParent().resolve("c." + chunkX + "." + chunkZ + ".mcc");
            source = Files.newInputStream(externalFile);
        } else {
            byte[] compressed = new byte[length - 1];
            var payload = java.nio.ByteBuffer.wrap(compressed);
            readFully(channel, payload);
            source = new java.io.ByteArrayInputStream(compressed);
        }

        try (InputStream decoded = switch (compression) {
            case 1 -> new GZIPInputStream(source);
            case 2 -> new InflaterInputStream(source);
            case 3 -> source;
            default -> throw new IOException("unsupported compression " + compression);
        }; DataInputStream input = new DataInputStream(new BufferedInputStream(decoded))) {
            int rootType = input.readUnsignedByte();
            if (rootType != 10) {
                throw new IOException("root tag is not a compound: " + rootType);
            }
            input.readUTF();
            return readCompoundPayload(input);
        }
    }

    @SuppressWarnings("unchecked")
    private void inspectChunk(Map<String, Object> root, int fallbackChunkX, int fallbackChunkZ) {
        Map<String, Object> chunk = compound(root.get("Level"));
        if (chunk == null) {
            chunk = root;
        }
        int chunkX = number(chunk.get("xPos"), fallbackChunkX);
        int chunkZ = number(chunk.get("zPos"), fallbackChunkZ);

        Object blockEntitiesRaw = firstPresent(chunk, "block_entities", "TileEntities");
        if (blockEntitiesRaw instanceof List<?> blockEntities) {
            for (Object raw : blockEntities) {
                Map<String, Object> entity = compound(raw);
                if (entity != null) {
                    inspectBlockEntity(entity, chunkX, chunkZ);
                }
            }
        }

        inspectStructures(chunk, chunkX, chunkZ);

        Object sectionsRaw = firstPresent(chunk, "sections", "Sections");
        if (sectionsRaw instanceof List<?> sections) {
            TreeMap<Integer, Section> decoded = decodeSections(sections);
            paintChunkSurface(chunkX, chunkZ, decoded);
            if (isSiteChunk(chunkX, chunkZ)) {
                siteChunks.put(chunkKey(chunkX, chunkZ), new ChunkBlocks(decoded));
            }
        }
    }

    private void inspectStructures(Map<String, Object> chunk, int chunkX, int chunkZ) {
        Map<String, Object> structureData = compound(firstPresent(chunk, "structures", "Structures"));
        if (structureData == null) return;
        Map<String, Object> starts = compound(firstPresent(structureData, "starts", "Starts"));
        if (starts == null) return;

        for (Map.Entry<String, Object> entry : starts.entrySet()) {
            Map<String, Object> start = compound(entry.getValue());
            if (start == null) continue;
            String id = string(start.get("id"));
            if (id.isBlank() || "INVALID".equalsIgnoreCase(id)) id = entry.getKey();
            if (id.isBlank() || "INVALID".equalsIgnoreCase(id)) continue;
            int[] bounds = structureBounds(start);
            if (bounds == null) continue;

            structures.add(new StructureRecord(
                    id, chunkX, chunkZ,
                    bounds[0], bounds[1], bounds[2],
                    bounds[3], bounds[4], bounds[5]
            ));
        }
    }

    private static int[] structureBounds(Map<String, Object> start) {
        if (start.get("BB") instanceof int[] direct && direct.length >= 6) {
            return direct;
        }
        if (!(start.get("Children") instanceof List<?> children)) return null;

        int[] union = null;
        for (Object rawChild : children) {
            Map<String, Object> child = compound(rawChild);
            if (child == null || !(child.get("BB") instanceof int[] box) || box.length < 6) continue;
            if (union == null) {
                union = box.clone();
                continue;
            }
            union[0] = Math.min(union[0], box[0]);
            union[1] = Math.min(union[1], box[1]);
            union[2] = Math.min(union[2], box[2]);
            union[3] = Math.max(union[3], box[3]);
            union[4] = Math.max(union[4], box[4]);
            union[5] = Math.max(union[5], box[5]);
        }
        return union;
    }

    private void inspectBlockEntity(Map<String, Object> tag, int chunkX, int chunkZ) {
        String id = string(tag.get("id"));
        int x = number(tag.get("x"), chunkX * 16);
        int y = number(tag.get("y"), 0);
        int z = number(tag.get("z"), chunkZ * 16);
        String customName = string(tag.get("CustomName"));
        String lootTable = string(tag.get("LootTable"));
        int itemStacks = tag.get("Items") instanceof List<?> items ? items.size() : 0;

        if (isContainer(id, tag)) {
            containers.add(new ContainerRecord(x, y, z, id, customName, lootTable, itemStacks));
        }

        if (id.contains("sign") || id.contains("banner") || id.contains("lectern")
                || id.contains("command_block") || !customName.isBlank()) {
            String text = extractText(tag);
            markers.add(new MarkerRecord(x, y, z, id, customName, text));
        }
    }

    private static boolean isContainer(String id, Map<String, Object> tag) {
        // Machines such as brewing stands and furnaces also expose an Items tag,
        // but they are not suitable discoverable loot storage. Counting them here
        // previously left one planned capsule inside a brewing stand.
        return id.contains("chest") || id.contains("barrel") || id.contains("crate")
                || id.contains("storage") || id.contains("shulker") || id.contains("locker");
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Map<String, Object> tag) {
        List<String> values = new ArrayList<>();
        for (String key : List.of("Text1", "Text2", "Text3", "Text4", "Command")) {
            String value = string(tag.get(key));
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        for (String side : List.of("front_text", "back_text")) {
            Map<String, Object> text = compound(tag.get(side));
            if (text != null && text.get("messages") instanceof List<?> messages) {
                for (Object message : messages) {
                    String value = string(message);
                    if (!value.isBlank() && !"{\"text\":\"\"}".equals(value)) {
                        values.add(value);
                    }
                }
            }
        }
        return String.join(" | ", values);
    }

    private static TreeMap<Integer, Section> decodeSections(List<?> rawSections) {
        TreeMap<Integer, Section> sections = new TreeMap<>(Comparator.reverseOrder());
        for (Object raw : rawSections) {
            Map<String, Object> sectionTag = compound(raw);
            if (sectionTag == null) {
                continue;
            }
            int sectionY = number(sectionTag.get("Y"), Integer.MIN_VALUE);
            if (sectionY < MIN_SECTION_Y || sectionY > MAX_SECTION_Y) {
                continue;
            }
            Map<String, Object> blockStates = compound(firstPresent(sectionTag, "block_states", "BlockStates"));
            if (blockStates == null) {
                blockStates = sectionTag;
            }
            Object paletteRaw = firstPresent(blockStates, "palette", "Palette");
            if (!(paletteRaw instanceof List<?> paletteList) || paletteList.isEmpty()) {
                continue;
            }
            List<String> palette = new ArrayList<>(paletteList.size());
            for (Object paletteEntry : paletteList) {
                Map<String, Object> entry = compound(paletteEntry);
                palette.add(entry == null ? "minecraft:air" : string(entry.get("Name")));
            }
            Object dataRaw = firstPresent(blockStates, "data", "BlockStates");
            long[] data = dataRaw instanceof long[] longs ? longs : new long[0];
            sections.put(sectionY, new Section(palette, data));
        }
        return sections;
    }

    private void paintChunkSurface(int chunkX, int chunkZ, TreeMap<Integer, Section> sections) {
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                String surface = "minecraft:air";
                search:
                for (Map.Entry<Integer, Section> entry : sections.entrySet()) {
                    Section section = entry.getValue();
                    if (section.isUniformAir()) {
                        continue;
                    }
                    for (int localY = 15; localY >= 0; localY--) {
                        String block = section.block(localX, localY, localZ);
                        if (!AIR.contains(block)) {
                            surface = block;
                            break search;
                        }
                    }
                }
                surfaceCounts.merge(surface, 1L, Long::sum);
                paintBlock(chunkX * 16 + localX, chunkZ * 16 + localZ, colorFor(surface));
            }
        }
    }

    private boolean isSiteChunk(int chunkX, int chunkZ) {
        for (SiteRecord site : sites) {
            int minChunkX = Math.floorDiv(site.x - 26, 16);
            int maxChunkX = Math.floorDiv(site.x + 26, 16);
            int minChunkZ = Math.floorDiv(site.z - 26, 16);
            int maxChunkZ = Math.floorDiv(site.z + 26, 16);
            if (chunkX >= minChunkX && chunkX <= maxChunkX
                    && chunkZ >= minChunkZ && chunkZ <= maxChunkZ) {
                return true;
            }
        }
        return false;
    }

    private String blockAt(int x, int y, int z) {
        ChunkBlocks chunk = siteChunks.get(chunkKey(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
        if (chunk == null) {
            return "minecraft:void_air";
        }
        Section section = chunk.sections.get(Math.floorDiv(y, 16));
        if (section == null) {
            return "minecraft:air";
        }
        return section.block(Math.floorMod(x, 16), Math.floorMod(y, 16), Math.floorMod(z, 16));
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private void paintBlock(int blockX, int blockZ, int rgb) {
        int pixelX = Math.floorDiv(blockX - minBlockX, scale);
        int pixelZ = Math.floorDiv(blockZ - minBlockZ, scale);
        if (pixelX >= 0 && pixelX < overview.getWidth() && pixelZ >= 0 && pixelZ < overview.getHeight()) {
            overview.setRGB(pixelX, pixelZ, rgb);
        }
    }

    private void drawContainerOverlay() {
        Graphics2D graphics = overview.createGraphics();
        graphics.setColor(new Color(255, 55, 55));
        for (ContainerRecord container : containers) {
            int x = Math.floorDiv(container.x - minBlockX, scale);
            int z = Math.floorDiv(container.z - minBlockZ, scale);
            graphics.fillRect(x - 1, z - 1, 3, 3);
        }
        graphics.dispose();
    }

    private static int colorFor(String block) {
        String id = block.toLowerCase(Locale.ROOT);
        if (AIR.contains(id)) return 0x0F1113;
        if (id.contains("water")) return 0x356BAA;
        if (id.contains("lava") || id.contains("magma")) return 0xD55220;
        if (id.contains("glass")) return 0x86B7C4;
        if (id.contains("sandstone")) return 0xCBB887;
        if (id.contains("sand")) return 0xD9C27C;
        if (id.contains("red_sand") || id.contains("terracotta")) return 0xA6603A;
        if (id.contains("brick")) return id.contains("nether") ? 0x512932 : 0x974B3B;
        if (id.contains("concrete")) return 0x777A7D;
        if (id.contains("deepslate") || id.contains("blackstone")) return 0x34353B;
        if (id.contains("stone") || id.contains("andesite") || id.contains("tuff")) return 0x777777;
        if (id.contains("iron") || id.contains("metal") || id.contains("machine")) return 0xAAB0B4;
        if (id.contains("copper")) return 0xA76549;
        if (id.contains("gold")) return 0xD8B34A;
        if (id.contains("plank") || id.contains("log") || id.contains("wood")) return 0x785536;
        if (id.contains("leaves") || id.contains("moss") || id.contains("grass")) return 0x5B7E3D;
        if (id.contains("dirt") || id.contains("mud")) return 0x77553C;
        if (id.contains("snow") || id.contains("quartz") || id.contains("white")) return 0xD8D8D2;
        if (id.contains("bedrock")) return 0x292929;
        return 0x686868;
    }

    private void writeContainers() throws IOException {
        containers.sort(Comparator.comparingInt((ContainerRecord r) -> r.z)
                .thenComparingInt(r -> r.x).thenComparingInt(r -> r.y));
        try (Writer writer = utf8Writer(outputDir.resolve("containers.csv"))) {
            writer.write("x,y,z,id,custom_name,loot_table,item_stacks\n");
            for (ContainerRecord record : containers) {
                writer.write(record.x + "," + record.y + "," + record.z + ","
                        + csv(record.id) + "," + csv(record.customName) + ","
                        + csv(record.lootTable) + "," + record.itemStacks + "\n");
            }
        }
    }

    private void writeStructures() throws IOException {
        structures.sort(Comparator.comparing(StructureRecord::id)
                .thenComparingInt(StructureRecord::startChunkX)
                .thenComparingInt(StructureRecord::startChunkZ));
        try (Writer writer = utf8Writer(outputDir.resolve("structure_starts.csv"))) {
            writer.write("structure_id,category,danger,start_chunk_x,start_chunk_z,min_x,min_y,min_z,max_x,max_y,max_z,containers,empty_containers\n");
            for (StructureRecord structure : structures) {
                int containerCount = 0;
                int emptyCount = 0;
                for (ContainerRecord container : containers) {
                    if (!structure.contains(container.x, container.y, container.z)) continue;
                    containerCount++;
                    if (container.lootTable.isBlank() && container.itemStacks == 0) emptyCount++;
                }
                writer.write(csv(structure.id) + "," + csv(structure.category()) + ","
                        + csv(structure.danger()) + ","
                        + structure.startChunkX + "," + structure.startChunkZ + ","
                        + structure.minX + "," + structure.minY + "," + structure.minZ + ","
                        + structure.maxX + "," + structure.maxY + "," + structure.maxZ + ","
                        + containerCount + "," + emptyCount + "\n");
            }
        }
    }

    private void writeMarkers() throws IOException {
        markers.sort(Comparator.comparingInt((MarkerRecord r) -> r.z)
                .thenComparingInt(r -> r.x).thenComparingInt(r -> r.y));
        try (Writer writer = utf8Writer(outputDir.resolve("markers.csv"))) {
            writer.write("x,y,z,id,custom_name,text\n");
            for (MarkerRecord record : markers) {
                writer.write(record.x + "," + record.y + "," + record.z + ","
                        + csv(record.id) + "," + csv(record.customName) + "," + csv(record.text) + "\n");
            }
        }
    }

    private void writeSurfaceCounts() throws IOException {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(surfaceCounts.entrySet());
        entries.sort(Map.Entry.<String, Long>comparingByValue().reversed());
        try (Writer writer = utf8Writer(outputDir.resolve("surface_blocks.csv"))) {
            writer.write("block,count\n");
            for (Map.Entry<String, Long> entry : entries) {
                writer.write(csv(entry.getKey()) + "," + entry.getValue() + "\n");
            }
        }
    }

    private void writeStoragePlan() throws IOException {
        List<StoragePlan> plans = new ArrayList<>();
        for (SiteRecord site : sites) {
            ContainerRecord existing = nearestContainer(site, 32.0D, 40.0D);
            if (existing != null) {
                plans.add(StoragePlan.existing(site, existing));
                continue;
            }

            PlacementCandidate best = findPlacement(site, 12, 8);
            if (best == null || best.score < 60.0D) {
                PlacementCandidate expanded = findPlacement(site, 24, 12);
                if (expanded != null && (best == null || expanded.score > best.score)) {
                    best = expanded;
                }
            }
            plans.add(best == null ? StoragePlan.unresolved(site) : StoragePlan.add(site, best));
        }

        try (Writer writer = utf8Writer(outputDir.resolve("storage_plan.csv"))) {
            writer.write("site_id,name,category,status,x,y,z,facing,score,anchor_x,anchor_y,anchor_z\n");
            for (StoragePlan plan : plans) {
                writer.write(csv(plan.site.id) + "," + csv(plan.site.name) + "," + csv(plan.site.category)
                        + "," + plan.status + "," + nullable(plan.x) + "," + nullable(plan.y) + ","
                        + nullable(plan.z) + "," + csv(plan.facing) + "," + nullable(plan.score) + ","
                        + plan.site.x + "," + plan.site.y + "," + plan.site.z + "\n");
            }
        }

        try (Writer writer = utf8Writer(outputDir.resolve("add_missing_storage.mcfunction"))) {
            writer.write("# Generated by WorldMapAudit. One empty barrel per audited site without nearby storage.\n");
            writer.write("# Run only after backing up the WASTED_TEST world.\n");
            for (StoragePlan plan : plans) {
                if (!"ADD".equals(plan.status)) {
                    continue;
                }
                writer.write("# " + plan.site.id + " | " + plan.site.name + " | " + plan.site.category + "\n");
                writer.write("forceload add " + plan.x + " " + plan.z + "\n");
                writer.write("setblock " + plan.x + " " + plan.y + " " + plan.z
                        + " minecraft:barrel[facing=" + plan.facing + ",open=false] keep\n");
                writer.write("forceload remove " + plan.x + " " + plan.z + "\n");
            }
        }
    }

    private PlacementCandidate findPlacement(SiteRecord site, int horizontalRadius, int verticalRadius) {
        PlacementCandidate best = null;
        for (int y = site.y - verticalRadius; y <= site.y + verticalRadius; y++) {
            for (int z = site.z - horizontalRadius; z <= site.z + horizontalRadius; z++) {
                for (int x = site.x - horizontalRadius; x <= site.x + horizontalRadius; x++) {
                    PlacementCandidate candidate = scorePlacement(site, x, y, z);
                    if (candidate != null && (best == null || candidate.score > best.score)) {
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private ContainerRecord nearestContainer(SiteRecord site, double horizontalLimit, double distanceLimit) {
        ContainerRecord best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ContainerRecord container : containers) {
            double dx = container.x - site.x;
            double dy = container.y - site.y;
            double dz = container.z - site.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double distance = Math.sqrt(horizontal * horizontal + dy * dy);
            if (horizontal <= horizontalLimit && distance <= distanceLimit && distance < bestDistance) {
                best = container;
                bestDistance = distance;
            }
        }
        return best;
    }

    private PlacementCandidate scorePlacement(SiteRecord site, int x, int y, int z) {
        if (!AIR.contains(blockAt(x, y, z)) || !AIR.contains(blockAt(x, y + 1, z))) {
            return null;
        }
        String floor = blockAt(x, y - 1, z);
        boolean farmSite = site.category.startsWith("FARM_");
        if (!isPlacementSurface(floor) && !(farmSite && isFarmSurface(floor))) {
            return null;
        }

        String facing = "";
        int adjacentWalls = 0;
        if (isWall(blockAt(x - 1, y, z), farmSite)) { facing = "east"; adjacentWalls++; }
        if (isWall(blockAt(x + 1, y, z), farmSite)) { if (facing.isEmpty()) facing = "west"; adjacentWalls++; }
        if (isWall(blockAt(x, y, z - 1), farmSite)) { if (facing.isEmpty()) facing = "south"; adjacentWalls++; }
        if (isWall(blockAt(x, y, z + 1), farmSite)) { if (facing.isEmpty()) facing = "north"; adjacentWalls++; }
        if (adjacentWalls == 0) {
            return null;
        }

        int roofDistance = 0;
        for (int dy = 2; dy <= 10; dy++) {
            if (!AIR.contains(blockAt(x, y + dy, z))) {
                roofDistance = dy;
                break;
            }
        }

        int openSides = 0;
        if (AIR.contains(blockAt(x - 1, y, z))) openSides++;
        if (AIR.contains(blockAt(x + 1, y, z))) openSides++;
        if (AIR.contains(blockAt(x, y, z - 1))) openSides++;
        if (AIR.contains(blockAt(x, y, z + 1))) openSides++;
        if (openSides == 0) {
            return null;
        }

        double dx = x - site.x;
        double dy = y - site.y;
        double dz = z - site.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double score = 40.0D - distance;
        score += roofDistance > 0 ? 34.0D - roofDistance : -18.0D;
        score += Math.min(adjacentWalls, 2) * 8.0D;
        score += isManMade(floor) ? 16.0D : -6.0D;
        score += openSides >= 2 ? 5.0D : 0.0D;
        return new PlacementCandidate(x, y, z, facing, score);
    }

    private static boolean isPlacementSurface(String block) {
        String id = block.toLowerCase(Locale.ROOT);
        return !AIR.contains(id)
                && !id.contains("water") && !id.contains("lava")
                && !id.contains("leaves") && !id.contains("grass")
                && !id.contains("flower") && !id.contains("bush")
                && !id.contains("torch") && !id.contains("sign")
                && !id.contains("rail") && !id.contains("carpet")
                && !id.contains("slab") && !id.contains("stair")
                && !id.contains("fence") && !id.contains("wall");
    }

    private static boolean isFarmSurface(String block) {
        String id = block.toLowerCase(Locale.ROOT);
        return id.contains("dirt") || id.contains("grass_block") || id.contains("farmland")
                || id.contains("hay_block") || id.contains("path");
    }

    private static boolean isWall(String block, boolean farmSite) {
        String id = block.toLowerCase(Locale.ROOT);
        return (isPlacementSurface(id) || (farmSite && (id.contains("fence") || id.contains("wall"))))
                && !id.contains("door") && !id.contains("trapdoor")
                && !id.contains("chest") && !id.contains("barrel")
                && !id.contains("glass") && !id.contains("pane");
    }

    private static boolean isManMade(String block) {
        String id = block.toLowerCase(Locale.ROOT);
        return id.contains("plank") || id.contains("brick") || id.contains("stone")
                || id.contains("concrete") || id.contains("terracotta") || id.contains("tile")
                || id.contains("metal") || id.contains("iron") || id.contains("copper")
                || id.contains("quartz") || id.contains("wood") || id.contains("log");
    }

    private static String nullable(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static List<SiteRecord> readSites(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile(
                "\\{\\\"id\\\":\\\"([^\\\"]+)\\\",\\\"name\\\":\\\"([^\\\"]+)\\\","
                        + "\\\"x\\\":(-?\\d+),\\\"y\\\":(-?\\d+),\\\"z\\\":(-?\\d+),"
                        + "\\\"category\\\":\\\"([^\\\"]+)\\\"}"
        );
        Matcher matcher = pattern.matcher(json);
        List<SiteRecord> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(new SiteRecord(
                    matcher.group(1), matcher.group(2),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)),
                    Integer.parseInt(matcher.group(5)),
                    matcher.group(6)
            ));
        }
        if (result.isEmpty()) {
            throw new IOException("no sites parsed from " + file);
        }
        return List.copyOf(result);
    }

    private void writeSummary(double seconds) throws IOException {
        String json = "{\n"
                + "  \"region_bounds\": [" + minRegionX + ", " + maxRegionX + ", " + minRegionZ + ", " + maxRegionZ + "],\n"
                + "  \"block_bounds\": [" + minBlockX + ", " + ((maxRegionX + 1) * 512 - 1)
                + ", " + minBlockZ + ", " + ((maxRegionZ + 1) * 512 - 1) + "],\n"
                + "  \"map_scale_blocks_per_pixel\": " + scale + ",\n"
                + "  \"chunks_read\": " + chunksRead + ",\n"
                + "  \"chunks_failed\": " + chunksFailed + ",\n"
                + "  \"containers\": " + containers.size() + ",\n"
                + "  \"structure_starts\": " + structures.size() + ",\n"
                + "  \"markers\": " + markers.size() + ",\n"
                + "  \"elapsed_seconds\": " + String.format(Locale.ROOT, "%.3f", seconds) + "\n"
                + "}\n";
        Files.writeString(outputDir.resolve("summary.json"), json, StandardCharsets.UTF_8);
    }

    private static Writer utf8Writer(Path path) throws IOException {
        return new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static void readFully(java.nio.channels.SeekableByteChannel channel,
                                  java.nio.ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) {
                throw new EOFException();
            }
        }
    }

    private static Object firstPresent(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> compound(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Map<String, Object> readCompoundPayload(DataInput input) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        while (true) {
            int type = input.readUnsignedByte();
            if (type == 0) {
                return result;
            }
            String name = input.readUTF();
            result.put(name, readPayload(input, type));
        }
    }

    private static Object readPayload(DataInput input, int type) throws IOException {
        return switch (type) {
            case 1 -> input.readByte();
            case 2 -> input.readShort();
            case 3 -> input.readInt();
            case 4 -> input.readLong();
            case 5 -> input.readFloat();
            case 6 -> input.readDouble();
            case 7 -> {
                int length = checkedLength(input.readInt());
                byte[] values = new byte[length];
                input.readFully(values);
                yield values;
            }
            case 8 -> input.readUTF();
            case 9 -> {
                int childType = input.readUnsignedByte();
                int length = checkedLength(input.readInt());
                List<Object> values = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    values.add(readPayload(input, childType));
                }
                yield values;
            }
            case 10 -> readCompoundPayload(input);
            case 11 -> {
                int length = checkedLength(input.readInt());
                int[] values = new int[length];
                for (int i = 0; i < length; i++) values[i] = input.readInt();
                yield values;
            }
            case 12 -> {
                int length = checkedLength(input.readInt());
                long[] values = new long[length];
                for (int i = 0; i < length; i++) values[i] = input.readLong();
                yield values;
            }
            default -> throw new IOException("unsupported NBT tag type " + type);
        };
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > 64_000_000) {
            throw new IOException("invalid NBT array/list length " + length);
        }
        return length;
    }

    private record ContainerRecord(int x, int y, int z, String id,
                                   String customName, String lootTable, int itemStacks) {}

    private record MarkerRecord(int x, int y, int z, String id,
                                String customName, String text) {}

    private record StructureRecord(String id, int startChunkX, int startChunkZ,
                                   int minX, int minY, int minZ,
                                   int maxX, int maxY, int maxZ) {
        private boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        private String category() {
            String value = structurePath();
            if (containsAny(value, "plague", "asylum", "laboratory", "stronghold",
                    "temple", "catacomb", "archeolog", "lich_prison", "underground_cabin")) {
                return "RESEARCH_ARCHIVE";
            }
            if (containsAny(value, "foundry", "mechanical", "mining", "mineshaft", "factory")) {
                return "INDUSTRIAL_MINING";
            }
            if (containsAny(value, "village", "house", "camp", "city", "town", "inn")) {
                return "RESIDENTIAL_LOGISTICS";
            }
            if (containsAny(value, "dungeon", "grave", "crypt", "fort", "outpost", "mansion")) {
                return "MILITARY_DANGER";
            }
            return "OTHER";
        }

        private String danger() {
            String value = structurePath();
            if (containsAny(value, "stronghold", "plague", "asylum", "lich", "dungeon",
                    "crypt", "fort", "mansion", "ancient_city")) return "HIGH";
            if (containsAny(value, "temple", "catacomb", "grave", "mineshaft", "mining")) return "MEDIUM";
            return "LOW";
        }

        private String structurePath() {
            String value = id.toLowerCase(Locale.ROOT);
            int separator = value.indexOf(':');
            return separator < 0 ? value : value.substring(separator + 1);
        }

        private static boolean containsAny(String value, String... needles) {
            for (String needle : needles) if (value.contains(needle)) return true;
            return false;
        }
    }

    private record SiteRecord(String id, String name, int x, int y, int z, String category) {}

    private record PlacementCandidate(int x, int y, int z, String facing, double score) {}

    private record ChunkBlocks(TreeMap<Integer, Section> sections) {}

    private record StoragePlan(SiteRecord site, String status,
                               Integer x, Integer y, Integer z,
                               String facing, Double score) {
        private static StoragePlan existing(SiteRecord site, ContainerRecord container) {
            return new StoragePlan(site, "EXISTING", container.x, container.y, container.z, "", null);
        }

        private static StoragePlan add(SiteRecord site, PlacementCandidate candidate) {
            return new StoragePlan(site, "ADD", candidate.x, candidate.y, candidate.z,
                    candidate.facing, Math.round(candidate.score * 10.0D) / 10.0D);
        }

        private static StoragePlan unresolved(SiteRecord site) {
            return new StoragePlan(site, "UNRESOLVED", null, null, null, "", null);
        }
    }

    private static final class Section {
        private final List<String> palette;
        private final long[] data;
        private final int bits;
        private final int valuesPerLong;
        private final long mask;

        private Section(List<String> palette, long[] data) {
            this.palette = palette;
            this.data = data;
            this.bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.size() - 1)));
            this.valuesPerLong = 64 / bits;
            this.mask = (1L << bits) - 1L;
        }

        private boolean isUniformAir() {
            return palette.size() == 1 && AIR.contains(palette.get(0));
        }

        private String block(int x, int y, int z) {
            if (palette.size() == 1 || data.length == 0) {
                return palette.get(0);
            }
            int index = (y << 8) | (z << 4) | x;
            int longIndex = index / valuesPerLong;
            int bitIndex = (index % valuesPerLong) * bits;
            if (longIndex < 0 || longIndex >= data.length) {
                return "minecraft:air";
            }
            int paletteIndex = (int) ((data[longIndex] >>> bitIndex) & mask);
            return paletteIndex >= 0 && paletteIndex < palette.size()
                    ? palette.get(paletteIndex)
                    : "minecraft:air";
        }
    }
}
