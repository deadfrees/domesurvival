import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Installs the audited guaranteed biological modules into the authored map.
 * The operation is idempotent and refuses to overwrite items or use machines
 * such as furnaces, brewing stands, hoppers and dispensers as loot storage.
 */
public final class WorldBioCapsuleInstaller {
    private static final String MODULE_ID = "domesurvival:bio_module";
    private static final String OPTIONAL_LOOT_TABLE = "domesurvival:chests/bio_module_cache";
    private static final int MODULE_VERSION = 1;
    private static final int STORAGE_SLOTS = 27;

    public static void main(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            throw new IllegalArgumentException(
                    "Expected: <region-dir> <storage-plan.csv> <capsule-plan.json> <journal.csv> [--verify]"
            );
        }

        Path regionDir = Path.of(args[0]).toAbsolutePath().normalize();
        Path storagePlan = Path.of(args[1]).toAbsolutePath().normalize();
        Path capsulePlan = Path.of(args[2]).toAbsolutePath().normalize();
        Path journalFile = Path.of(args[3]).toAbsolutePath().normalize();
        boolean verifyOnly = args.length == 5 && "--verify".equals(args[4]);
        if (!Files.isDirectory(regionDir)) {
            throw new IOException("Region directory is missing: " + regionDir);
        }

        Map<String, StorageTarget> storage = readStoragePlan(storagePlan);
        List<Assignment> assignments = readAssignments(capsulePlan, storage);
        List<OptionalTarget> optionalTargets = readOptionalTargets(capsulePlan, storage);
        validateAssignments(assignments);
        validateOptionalTargets(assignments, optionalTargets);

        if (verifyOnly) {
            verifyInstalled(regionDir, assignments);
            verifyOptionalCaches(regionDir, optionalTargets);
            System.out.println("Verified guaranteed biological modules: " + assignments.size());
            System.out.println("Verified optional biological caches: " + optionalTargets.size());
            return;
        }

        preflight(regionDir, assignments);
        preflightOptionalCaches(regionDir, optionalTargets);
        List<JournalRow> journal = install(regionDir, assignments);
        writeJournal(journalFile, journal);
        Path optionalJournalFile = journalFile.resolveSibling("bio_optional_cache_install_journal.csv");
        List<OptionalJournalRow> optionalJournal = installOptionalCaches(regionDir, optionalTargets);
        writeOptionalJournal(optionalJournalFile, optionalJournal);
        verifyInstalled(regionDir, assignments);
        verifyOptionalCaches(regionDir, optionalTargets);

        long installed = journal.stream().filter(row -> "INSTALLED".equals(row.status)).count();
        long existing = journal.size() - installed;
        System.out.println("Guaranteed biological modules: " + assignments.size());
        System.out.println("Installed: " + installed + ", already present: " + existing);
        long optionalInstalled = optionalJournal.stream().filter(row -> "INSTALLED".equals(row.status)).count();
        System.out.println("Optional biological caches: " + optionalTargets.size()
                + " (installed " + optionalInstalled + ")");
        System.out.println("Journal: " + journalFile);
        System.out.println("Optional journal: " + optionalJournalFile);
    }

    private static Map<String, StorageTarget> readStoragePlan(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IOException("Empty storage plan: " + file);
        List<String> header = parseCsvLine(lines.get(0));
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) columns.put(header.get(i), i);
        for (String required : List.of("site_id", "status", "x", "y", "z")) {
            if (!columns.containsKey(required)) throw new IOException("Missing CSV column: " + required);
        }

        Map<String, StorageTarget> result = new LinkedHashMap<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (line.isBlank()) continue;
            List<String> values = parseCsvLine(line);
            String site = value(values, columns, "site_id");
            String status = value(values, columns, "status");
            if (!"EXISTING".equals(status) && !"ADD".equals(status)) {
                throw new IOException("Unresolved storage for site " + site + ": " + status);
            }
            StorageTarget previous = result.put(site, new StorageTarget(
                    site,
                    Integer.parseInt(value(values, columns, "x")),
                    Integer.parseInt(value(values, columns, "y")),
                    Integer.parseInt(value(values, columns, "z"))
            ));
            if (previous != null) throw new IOException("Duplicate storage site: " + site);
        }
        return Map.copyOf(result);
    }

    private static List<Assignment> readAssignments(Path file, Map<String, StorageTarget> storage) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonArray guaranteed = root.getAsJsonArray("guaranteed");
        if (guaranteed == null) throw new IOException("Missing guaranteed array in " + file);

        List<Assignment> result = new ArrayList<>();
        for (JsonElement element : guaranteed) {
            JsonObject entry = element.getAsJsonObject();
            String entity = entry.get("entity").getAsString();
            for (String key : List.of("first", "second")) {
                JsonObject slot = entry.getAsJsonObject(key);
                if (slot == null) throw new IOException("Missing " + key + " assignment for " + entity);
                String site = slot.get("site").getAsString();
                StorageTarget target = storage.get(site);
                if (target == null) throw new IOException("No storage target for site " + site);
                result.add(new Assignment(site, entity, slot.get("damaged").getAsBoolean(), target));
            }
        }
        return List.copyOf(result);
    }

    private static List<OptionalTarget> readOptionalTargets(Path file,
                                                            Map<String, StorageTarget> storage) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonArray optional = root.getAsJsonArray("optional_random_sites");
        if (optional == null) throw new IOException("Missing optional_random_sites array in " + file);

        List<OptionalTarget> result = new ArrayList<>();
        for (JsonElement element : optional) {
            String site = element.getAsString();
            StorageTarget target = storage.get(site);
            if (target == null) throw new IOException("No storage target for optional site " + site);
            result.add(new OptionalTarget(site, target));
        }
        return List.copyOf(result);
    }

    private static void validateAssignments(List<Assignment> assignments) throws IOException {
        if (assignments.size() != 46) {
            throw new IOException("Expected 46 guaranteed capsule locations, got " + assignments.size());
        }
        Set<String> sites = new LinkedHashSet<>();
        Set<String> coordinates = new LinkedHashSet<>();
        Map<String, Integer> speciesCounts = new HashMap<>();
        for (Assignment assignment : assignments) {
            if (!sites.add(assignment.site)) throw new IOException("Duplicate capsule site: " + assignment.site);
            String coordinate = assignment.target.x + "," + assignment.target.y + "," + assignment.target.z;
            if (!coordinates.add(coordinate)) throw new IOException("Two capsule sites share container " + coordinate);
            speciesCounts.merge(assignment.entity, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : speciesCounts.entrySet()) {
            if (entry.getValue() != 2) {
                throw new IOException("Species does not have exactly two guaranteed modules: " + entry);
            }
        }
    }

    private static void validateOptionalTargets(List<Assignment> assignments,
                                                List<OptionalTarget> optionalTargets) throws IOException {
        if (optionalTargets.size() != 20) {
            throw new IOException("Expected 20 optional capsule sites, got " + optionalTargets.size());
        }
        Set<String> guaranteedSites = new LinkedHashSet<>();
        Set<String> guaranteedCoordinates = new LinkedHashSet<>();
        for (Assignment assignment : assignments) {
            guaranteedSites.add(assignment.site);
            guaranteedCoordinates.add(coordinate(assignment.target));
        }
        Set<String> optionalSites = new LinkedHashSet<>();
        Set<String> optionalCoordinates = new LinkedHashSet<>();
        for (OptionalTarget optional : optionalTargets) {
            if (!optionalSites.add(optional.site)) {
                throw new IOException("Duplicate optional capsule site: " + optional.site);
            }
            String coordinate = coordinate(optional.target);
            if (!optionalCoordinates.add(coordinate)) {
                throw new IOException("Two optional sites share container " + coordinate);
            }
            if (guaranteedSites.contains(optional.site) || guaranteedCoordinates.contains(coordinate)) {
                throw new IOException("Optional site overlaps guaranteed capsule: " + optional.site);
            }
        }
    }

    private static String coordinate(StorageTarget target) {
        return target.x + "," + target.y + "," + target.z;
    }

    private static void preflight(Path regionDir, List<Assignment> assignments) throws IOException {
        int checked = 0;
        for (Map.Entry<RegionKey, List<Assignment>> regionEntry : groupByRegion(assignments).entrySet()) {
            Path regionPath = regionEntry.getKey().path(regionDir);
            if (!Files.isRegularFile(regionPath)) throw new IOException("Missing region file: " + regionPath);
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                for (Map.Entry<Long, List<Assignment>> chunkEntry : groupByChunk(regionEntry.getValue()).entrySet()) {
                    CompoundTag root = readChunk(region, chunkPos(chunkEntry.getKey()));
                    for (Assignment assignment : chunkEntry.getValue()) {
                        CompoundTag container = findStorage(root, assignment);
                        inspectContainer(container, assignment, false);
                        checked++;
                    }
                }
            }
        }
        if (checked != assignments.size()) {
            throw new IOException("Preflight checked " + checked + " assignments, expected " + assignments.size());
        }
        System.out.println("Preflight validated capsule containers: " + checked);
    }

    private static void preflightOptionalCaches(Path regionDir,
                                                List<OptionalTarget> optionalTargets) throws IOException {
        int checked = 0;
        for (Map.Entry<RegionKey, List<OptionalTarget>> regionEntry
                : groupOptionalByRegion(optionalTargets).entrySet()) {
            Path regionPath = regionEntry.getKey().path(regionDir);
            if (!Files.isRegularFile(regionPath)) throw new IOException("Missing region file: " + regionPath);
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                for (Map.Entry<Long, List<OptionalTarget>> chunkEntry
                        : groupOptionalByChunk(regionEntry.getValue()).entrySet()) {
                    CompoundTag root = readChunk(region, chunkPos(chunkEntry.getKey()));
                    for (OptionalTarget optional : chunkEntry.getValue()) {
                        CompoundTag container = findOptionalStorage(root, optional);
                        inspectOptionalCache(container, optional, false);
                        checked++;
                    }
                }
            }
        }
        if (checked != optionalTargets.size()) {
            throw new IOException("Preflight checked " + checked + " optional caches, expected "
                    + optionalTargets.size());
        }
        System.out.println("Preflight validated optional cache containers: " + checked);
    }

    private static List<JournalRow> install(Path regionDir, List<Assignment> assignments) throws Exception {
        List<JournalRow> journal = new ArrayList<>();
        for (Map.Entry<RegionKey, List<Assignment>> regionEntry : groupByRegion(assignments).entrySet()) {
            Path regionPath = regionEntry.getKey().path(regionDir);
            String beforeHash = sha256(regionPath);
            int journalStart = journal.size();
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                for (Map.Entry<Long, List<Assignment>> chunkEntry : groupByChunk(regionEntry.getValue()).entrySet()) {
                    ChunkPos chunkPos = chunkPos(chunkEntry.getKey());
                    CompoundTag root = readChunk(region, chunkPos);
                    boolean changed = false;
                    for (Assignment assignment : chunkEntry.getValue()) {
                        CompoundTag container = findStorage(root, assignment);
                        InstallResult result = inspectContainer(container, assignment, true);
                        changed |= result.installed;
                        journal.add(new JournalRow(assignment, result.slot,
                                result.installed ? "INSTALLED" : "ALREADY_PRESENT",
                                regionPath.getFileName().toString(), chunkPos.x, chunkPos.z,
                                beforeHash, ""));
                    }
                    if (changed) {
                        try (DataOutputStream output = region.getChunkDataOutputStream(chunkPos)) {
                            NbtIo.write(root, output);
                        }
                    }
                }
                region.flush();
            }
            String afterHash = sha256(regionPath);
            for (int i = journalStart; i < journal.size(); i++) {
                JournalRow row = journal.get(i);
                journal.set(i, row.withAfterHash(afterHash));
            }
        }
        return journal;
    }

    private static List<OptionalJournalRow> installOptionalCaches(Path regionDir,
                                                                  List<OptionalTarget> targets) throws Exception {
        List<OptionalJournalRow> journal = new ArrayList<>();
        for (Map.Entry<RegionKey, List<OptionalTarget>> regionEntry
                : groupOptionalByRegion(targets).entrySet()) {
            Path regionPath = regionEntry.getKey().path(regionDir);
            String beforeHash = sha256(regionPath);
            int journalStart = journal.size();
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                for (Map.Entry<Long, List<OptionalTarget>> chunkEntry
                        : groupOptionalByChunk(regionEntry.getValue()).entrySet()) {
                    ChunkPos chunkPos = chunkPos(chunkEntry.getKey());
                    CompoundTag root = readChunk(region, chunkPos);
                    boolean changed = false;
                    for (OptionalTarget optional : chunkEntry.getValue()) {
                        CompoundTag container = findOptionalStorage(root, optional);
                        boolean installed = inspectOptionalCache(container, optional, true);
                        changed |= installed;
                        journal.add(new OptionalJournalRow(optional,
                                installed ? "INSTALLED" : "ALREADY_PRESENT",
                                regionPath.getFileName().toString(), chunkPos.x, chunkPos.z,
                                beforeHash, ""));
                    }
                    if (changed) {
                        try (DataOutputStream output = region.getChunkDataOutputStream(chunkPos)) {
                            NbtIo.write(root, output);
                        }
                    }
                }
                region.flush();
            }
            String afterHash = sha256(regionPath);
            for (int i = journalStart; i < journal.size(); i++) {
                OptionalJournalRow row = journal.get(i);
                journal.set(i, row.withAfterHash(afterHash));
            }
        }
        return journal;
    }

    private static void verifyInstalled(Path regionDir, List<Assignment> assignments) throws IOException {
        int verified = 0;
        for (Map.Entry<RegionKey, List<Assignment>> regionEntry : groupByRegion(assignments).entrySet()) {
            Path regionPath = regionEntry.getKey().path(regionDir);
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                for (Map.Entry<Long, List<Assignment>> chunkEntry : groupByChunk(regionEntry.getValue()).entrySet()) {
                    CompoundTag root = readChunk(region, chunkPos(chunkEntry.getKey()));
                    for (Assignment assignment : chunkEntry.getValue()) {
                        CompoundTag container = findStorage(root, assignment);
                        InstallResult result = findMatchingModule(container, assignment);
                        if (result == null) {
                            throw new IOException("Missing installed module at site " + assignment.site);
                        }
                        verified++;
                    }
                }
            }
        }
        if (verified != assignments.size()) {
            throw new IOException("Verified " + verified + " modules, expected " + assignments.size());
        }
    }

    private static void verifyOptionalCaches(Path regionDir,
                                             List<OptionalTarget> optionalTargets) throws IOException {
        int verified = 0;
        for (Map.Entry<RegionKey, List<OptionalTarget>> regionEntry
                : groupOptionalByRegion(optionalTargets).entrySet()) {
            Path regionPath = regionEntry.getKey().path(regionDir);
            try (RegionFile region = new RegionFile(regionPath, regionDir, true)) {
                for (Map.Entry<Long, List<OptionalTarget>> chunkEntry
                        : groupOptionalByChunk(regionEntry.getValue()).entrySet()) {
                    CompoundTag root = readChunk(region, chunkPos(chunkEntry.getKey()));
                    for (OptionalTarget optional : chunkEntry.getValue()) {
                        CompoundTag container = findOptionalStorage(root, optional);
                        if (!OPTIONAL_LOOT_TABLE.equals(container.getString("LootTable"))) {
                            throw new IOException("Missing optional cache loot table at site " + optional.site);
                        }
                        verified++;
                    }
                }
            }
        }
        if (verified != optionalTargets.size()) {
            throw new IOException("Verified " + verified + " optional caches, expected "
                    + optionalTargets.size());
        }
    }

    private static CompoundTag findStorage(CompoundTag root, Assignment assignment) throws IOException {
        CompoundTag chunk = root.contains("Level", Tag.TAG_COMPOUND) ? root.getCompound("Level") : root;
        String entitiesKey = chunk.contains("block_entities") ? "block_entities" : "TileEntities";
        ListTag blockEntities = chunk.getList(entitiesKey, Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag blockEntity = blockEntities.getCompound(i);
            if (blockEntity.getInt("x") != assignment.target.x
                    || blockEntity.getInt("y") != assignment.target.y
                    || blockEntity.getInt("z") != assignment.target.z) continue;
            String id = blockEntity.getString("id");
            if (!isLootStorage(id)) {
                throw new IOException("Target for " + assignment.site + " is not loot storage: " + id);
            }
            if (blockEntity.contains("LootTable", Tag.TAG_STRING)) {
                throw new IOException("Unresolved LootTable at fixed capsule site " + assignment.site);
            }
            return blockEntity;
        }
        throw new IOException("Missing storage block entity for " + assignment.site + " at "
                + assignment.target.x + "," + assignment.target.y + "," + assignment.target.z);
    }

    private static CompoundTag findOptionalStorage(CompoundTag root, OptionalTarget optional) throws IOException {
        CompoundTag chunk = root.contains("Level", Tag.TAG_COMPOUND) ? root.getCompound("Level") : root;
        String entitiesKey = chunk.contains("block_entities") ? "block_entities" : "TileEntities";
        ListTag blockEntities = chunk.getList(entitiesKey, Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag blockEntity = blockEntities.getCompound(i);
            if (blockEntity.getInt("x") != optional.target.x
                    || blockEntity.getInt("y") != optional.target.y
                    || blockEntity.getInt("z") != optional.target.z) continue;
            String id = blockEntity.getString("id");
            if (!isLootStorage(id)) {
                throw new IOException("Optional target for " + optional.site
                        + " is not loot storage: " + id);
            }
            return blockEntity;
        }
        throw new IOException("Missing optional storage block entity for " + optional.site + " at "
                + optional.target.x + "," + optional.target.y + "," + optional.target.z);
    }

    private static boolean inspectOptionalCache(CompoundTag container, OptionalTarget optional,
                                                boolean install) throws IOException {
        String lootTable = container.getString("LootTable");
        if (OPTIONAL_LOOT_TABLE.equals(lootTable)) return false;
        if (!lootTable.isBlank()) {
            throw new IOException("Different LootTable already occupies optional site " + optional.site
                    + ": " + lootTable);
        }
        ListTag items = container.getList("Items", Tag.TAG_COMPOUND);
        if (!items.isEmpty()) {
            throw new IOException("Optional cache is no longer empty at site " + optional.site);
        }
        if (install) container.putString("LootTable", OPTIONAL_LOOT_TABLE);
        return install;
    }

    private static boolean isLootStorage(String id) {
        return id.contains("chest") || id.contains("barrel") || id.contains("crate")
                || id.contains("storage") || id.contains("shulker") || id.contains("locker");
    }

    private static InstallResult inspectContainer(CompoundTag container, Assignment assignment,
                                                  boolean install) throws IOException {
        InstallResult existing = findMatchingModule(container, assignment);
        if (existing != null) return existing;

        ListTag items = container.getList("Items", Tag.TAG_COMPOUND);
        boolean[] occupied = new boolean[STORAGE_SLOTS];
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            if (MODULE_ID.equals(item.getString("id"))) {
                throw new IOException("Different biological module already occupies site " + assignment.site);
            }
            int slot = Byte.toUnsignedInt(item.getByte("Slot"));
            if (slot < occupied.length) occupied[slot] = true;
        }
        int freeSlot = -1;
        for (int slot = 0; slot < occupied.length; slot++) {
            if (!occupied[slot]) {
                freeSlot = slot;
                break;
            }
        }
        if (freeSlot < 0) throw new IOException("No free slot in storage for site " + assignment.site);
        if (!install) return new InstallResult(freeSlot, false);

        CompoundTag item = new CompoundTag();
        item.putByte("Slot", (byte) freeSlot);
        item.putString("id", MODULE_ID);
        item.putByte("Count", (byte) 1);
        CompoundTag moduleData = new CompoundTag();
        moduleData.putString("EntityId", assignment.entity);
        moduleData.putBoolean("Damaged", assignment.damaged);
        moduleData.putInt("Version", MODULE_VERSION);
        item.put("tag", moduleData);
        items.add(item);
        container.put("Items", items);
        return new InstallResult(freeSlot, true);
    }

    private static InstallResult findMatchingModule(CompoundTag container, Assignment assignment) {
        ListTag items = container.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            if (!MODULE_ID.equals(item.getString("id"))) continue;
            CompoundTag data = item.getCompound("tag");
            if (assignment.entity.equals(data.getString("EntityId"))
                    && assignment.damaged == data.getBoolean("Damaged")
                    && data.getInt("Version") == MODULE_VERSION) {
                return new InstallResult(Byte.toUnsignedInt(item.getByte("Slot")), false);
            }
        }
        return null;
    }

    private static CompoundTag readChunk(RegionFile region, ChunkPos chunkPos) throws IOException {
        try (DataInputStream input = region.getChunkDataInputStream(chunkPos)) {
            if (input == null) throw new IOException("Missing chunk " + chunkPos);
            return NbtIo.read(input);
        }
    }

    private static Map<RegionKey, List<Assignment>> groupByRegion(List<Assignment> assignments) {
        Map<RegionKey, List<Assignment>> result = new LinkedHashMap<>();
        assignments.stream().sorted(Comparator.comparingInt((Assignment a) -> a.target.z)
                        .thenComparingInt(a -> a.target.x))
                .forEach(assignment -> result.computeIfAbsent(
                        new RegionKey(Math.floorDiv(assignment.target.x, 512),
                                Math.floorDiv(assignment.target.z, 512)),
                        ignored -> new ArrayList<>()).add(assignment));
        return result;
    }

    private static Map<Long, List<Assignment>> groupByChunk(List<Assignment> assignments) {
        Map<Long, List<Assignment>> result = new LinkedHashMap<>();
        for (Assignment assignment : assignments) {
            int chunkX = Math.floorDiv(assignment.target.x, 16);
            int chunkZ = Math.floorDiv(assignment.target.z, 16);
            long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(assignment);
        }
        return result;
    }

    private static Map<RegionKey, List<OptionalTarget>> groupOptionalByRegion(List<OptionalTarget> targets) {
        Map<RegionKey, List<OptionalTarget>> result = new LinkedHashMap<>();
        targets.stream().sorted(Comparator.comparingInt((OptionalTarget target) -> target.target.z)
                        .thenComparingInt(target -> target.target.x))
                .forEach(target -> result.computeIfAbsent(
                        new RegionKey(Math.floorDiv(target.target.x, 512),
                                Math.floorDiv(target.target.z, 512)),
                        ignored -> new ArrayList<>()).add(target));
        return result;
    }

    private static Map<Long, List<OptionalTarget>> groupOptionalByChunk(List<OptionalTarget> targets) {
        Map<Long, List<OptionalTarget>> result = new LinkedHashMap<>();
        for (OptionalTarget target : targets) {
            int chunkX = Math.floorDiv(target.target.x, 16);
            int chunkZ = Math.floorDiv(target.target.z, 16);
            long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(target);
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

    private static void writeJournal(Path file, List<JournalRow> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("site_id,entity,damaged,x,y,z,slot,status,region,chunk_x,chunk_z,before_sha256,after_sha256");
        for (JournalRow row : rows) {
            Assignment assignment = row.assignment;
            lines.add(csv(assignment.site) + "," + csv(assignment.entity) + "," + assignment.damaged
                    + "," + assignment.target.x + "," + assignment.target.y + "," + assignment.target.z
                    + "," + row.slot + "," + row.status + "," + row.region + "," + row.chunkX + ","
                    + row.chunkZ + "," + row.beforeHash + "," + row.afterHash);
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static void writeOptionalJournal(Path file, List<OptionalJournalRow> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("site_id,loot_table,x,y,z,status,region,chunk_x,chunk_z,before_sha256,after_sha256");
        for (OptionalJournalRow row : rows) {
            OptionalTarget optional = row.optional;
            lines.add(csv(optional.site) + "," + csv(OPTIONAL_LOOT_TABLE)
                    + "," + optional.target.x + "," + optional.target.y + "," + optional.target.z
                    + "," + row.status + "," + row.region + "," + row.chunkX + "," + row.chunkZ
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

    private record StorageTarget(String site, int x, int y, int z) {}
    private record Assignment(String site, String entity, boolean damaged, StorageTarget target) {}
    private record OptionalTarget(String site, StorageTarget target) {}
    private record InstallResult(int slot, boolean installed) {}

    private record RegionKey(int x, int z) {
        private Path path(Path regionDir) {
            return regionDir.resolve("r." + x + "." + z + ".mca");
        }
    }

    private record JournalRow(Assignment assignment, int slot, String status,
                              String region, int chunkX, int chunkZ,
                              String beforeHash, String afterHash) {
        private JournalRow withAfterHash(String value) {
            return new JournalRow(assignment, slot, status, region, chunkX, chunkZ, beforeHash, value);
        }
    }

    private record OptionalJournalRow(OptionalTarget optional, String status,
                                      String region, int chunkX, int chunkZ,
                                      String beforeHash, String afterHash) {
        private OptionalJournalRow withAfterHash(String value) {
            return new OptionalJournalRow(optional, status, region, chunkX, chunkZ, beforeHash, value);
        }
    }
}
