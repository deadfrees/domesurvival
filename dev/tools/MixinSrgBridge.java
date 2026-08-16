import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public final class MixinSrgBridge {
    private static final byte[] MIXIN_MARKER =
            "org/spongepowered/asm/mixin/Mixin".getBytes(StandardCharsets.ISO_8859_1);

    private static final Pattern RAW_SRG =
            Pattern.compile("(?<![A-Za-z0-9_$])([fFmM]_\\d+_)(?![A-Za-z0-9_$])");

    private static final Pattern ACCESSOR_NAME =
            Pattern.compile("(?<![A-Za-z0-9_$])(get|set|is)([Ff]_\\d+_)(?![A-Za-z0-9_$])");

    private static final Pattern INVOKER_NAME =
            Pattern.compile("(?<![A-Za-z0-9_$])(call|invoke)([Mm]_\\d+_)(?![A-Za-z0-9_$])");

    private static final Pattern SRG_FIELD = Pattern.compile("f_\\d+_");
    private static final Pattern SRG_METHOD = Pattern.compile("m_\\d+_");

    private static final class EntryData {
        final String name;
        final byte[] data;
        final long time;
        final boolean directory;

        EntryData(String name, byte[] data, long time, boolean directory) {
            this.name = name;
            this.data = data;
            this.time = time;
            this.directory = directory;
        }
    }

    private static final class ZipPatchResult {
        final byte[] bytes;
        final boolean changed;

        ZipPatchResult(byte[] bytes, boolean changed) {
            this.bytes = bytes;
            this.changed = changed;
        }
    }

    private static final class Stats {
        long topLevelMixinClassesChanged;
        long topLevelNormalClassesChanged;
        long nestedClassesChanged;

        long rawTopLevelMixinChanges;
        long rawTopLevelNormalChanges; // MUST remain zero.
        long rawNestedJarChanges;

        long accessorDefinitionsChanged;
        long accessorCallsitesChanged;
        long invokerDefinitionsChanged;
        long invokerCallsitesChanged;

        long nestedJarsChanged;

        final Map<String, Long> rawTopMixinCounts = new TreeMap<>();
        final Map<String, Long> rawNestedCounts = new TreeMap<>();
        final Map<String, Long> accessorCounts = new TreeMap<>();
        final Map<String, Long> invokerCounts = new TreeMap<>();

        long totalChanges() {
            return rawTopLevelMixinChanges
                    + rawTopLevelNormalChanges
                    + rawNestedJarChanges
                    + accessorDefinitionsChanged
                    + accessorCallsitesChanged
                    + invokerDefinitionsChanged
                    + invokerCallsitesChanged;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println(
                    "Usage: MixinSrgBridge <mapping.srg> <jarDir> <report.txt>");
            System.exit(2);
        }

        Path mappingFile = Paths.get(args[0]);
        Path jarDir = Paths.get(args[1]);
        Path report = Paths.get(args[2]);

        Map<String, String> mappings = loadMappings(mappingFile);

        if (mappings.size() < 1000) {
            throw new IllegalStateException(
                    "Too few SRG->MojMap mappings parsed: " + mappings.size());
        }

        requireMapping(mappings, "f_131257_");
        requireMapping(mappings, "f_62776_");
        requireMapping(mappings, "m_91087_");
        requireMapping(mappings, "m_195834_");

        List<Path> jars;
        try (var stream = Files.list(jarDir)) {
            jars = stream
                    .filter(p -> p.getFileName().toString()
                            .toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .sorted()
                    .toList();
        }

        if (jars.isEmpty()) {
            throw new IllegalStateException("No JARs found in " + jarDir);
        }

        StringBuilder reportText = new StringBuilder();
        reportText.append("DomeSurvival JarJar-aware Mixin SRG Bridge V6.8\n");
        reportText.append("Mappings: ").append(mappings.size()).append('\n');
        reportText.append("f_131257_ -> ")
                .append(mappings.get("f_131257_")).append('\n');
        reportText.append("f_62776_ -> ")
                .append(mappings.get("f_62776_")).append('\n');
        reportText.append("m_91087_ -> ")
                .append(mappings.get("m_91087_")).append('\n');
        reportText.append("m_195834_ -> ")
                .append(mappings.get("m_195834_")).append('\n');
        reportText.append("Accessor example: getF_62776_ -> get")
                .append(capitalize(mappings.get("f_62776_")))
                .append("\n\n");

        Stats total = new Stats();

        for (Path jar : jars) {
            Stats stats = new Stats();
            byte[] input = Files.readAllBytes(jar);

            ZipPatchResult result =
                    patchZip(input, mappings, stats, 0, false);

            if (result.changed) {
                Path temp =
                        jar.resolveSibling(jar.getFileName() + ".bridge.tmp");

                Files.write(temp, result.bytes);

                try {
                    Files.move(
                            temp,
                            jar,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(
                            temp,
                            jar,
                            StandardCopyOption.REPLACE_EXISTING);
                }

                reportText.append(jar.getFileName())
                        .append(": rawTopMixin=")
                        .append(stats.rawTopLevelMixinChanges)
                        .append(", rawNested=")
                        .append(stats.rawNestedJarChanges)
                        .append(", accessorDefs=")
                        .append(stats.accessorDefinitionsChanged)
                        .append(", accessorCalls=")
                        .append(stats.accessorCallsitesChanged)
                        .append(", topNormalChanged=")
                        .append(stats.topLevelNormalClassesChanged)
                        .append(", nestedClassesChanged=")
                        .append(stats.nestedClassesChanged)
                        .append(", nestedJarsChanged=")
                        .append(stats.nestedJarsChanged)
                        .append('\n');

                for (var e : stats.accessorCounts.entrySet()) {
                    reportText.append("  ACCESSOR ")
                            .append(e.getKey())
                            .append(" x")
                            .append(e.getValue())
                            .append('\n');
                }

                for (var e : stats.rawTopMixinCounts.entrySet()) {
                    reportText.append("  RAW_TOP_MIXIN ")
                            .append(e.getKey())
                            .append(" -> ")
                            .append(mappings.get(canonical(e.getKey())))
                            .append(" x")
                            .append(e.getValue())
                            .append('\n');
                }

                for (var e : stats.rawNestedCounts.entrySet()) {
                    reportText.append("  RAW_NESTED ")
                            .append(e.getKey())
                            .append(" -> ")
                            .append(mappings.get(canonical(e.getKey())))
                            .append(" x")
                            .append(e.getValue())
                            .append('\n');
                }
            }

            merge(total, stats);
        }

        reportText.append("\nScope safety:\n");
        reportText.append("Raw SRG top-level mixin changes: ")
                .append(total.rawTopLevelMixinChanges).append('\n');
        reportText.append("Raw SRG top-level non-mixin changes: ")
                .append(total.rawTopLevelNormalChanges).append('\n');
        reportText.append("Raw SRG nested JarJar changes: ")
                .append(total.rawNestedJarChanges).append('\n');
        reportText.append("Top-level normal classes changed: ")
                .append(total.topLevelNormalClassesChanged).append('\n');
        reportText.append("Nested classes changed: ")
                .append(total.nestedClassesChanged).append('\n');
        reportText.append("Nested JARs changed: ")
                .append(total.nestedJarsChanged).append('\n');
        reportText.append("Accessor definitions changed: ")
                .append(total.accessorDefinitionsChanged).append('\n');
        reportText.append("Accessor callsites changed: ")
                .append(total.accessorCallsitesChanged).append('\n');

        if (total.rawTopLevelNormalChanges != 0) {
            throw new IllegalStateException(
                    "SAFETY FAILURE: raw SRG changed in top-level non-mixin classes");
        }

        if (total.rawNestedJarChanges == 0) {
            throw new IllegalStateException(
                    "JarJar bridge did not find any nested SRG references");
        }

        Path parent = report.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                report,
                reportText.toString(),
                StandardCharsets.UTF_8);

        System.out.print(reportText);
    }

    private static void merge(Stats dst, Stats src) {
        dst.topLevelMixinClassesChanged += src.topLevelMixinClassesChanged;
        dst.topLevelNormalClassesChanged += src.topLevelNormalClassesChanged;
        dst.nestedClassesChanged += src.nestedClassesChanged;

        dst.rawTopLevelMixinChanges += src.rawTopLevelMixinChanges;
        dst.rawTopLevelNormalChanges += src.rawTopLevelNormalChanges;
        dst.rawNestedJarChanges += src.rawNestedJarChanges;

        dst.accessorDefinitionsChanged += src.accessorDefinitionsChanged;
        dst.accessorCallsitesChanged += src.accessorCallsitesChanged;
        dst.invokerDefinitionsChanged += src.invokerDefinitionsChanged;
        dst.invokerCallsitesChanged += src.invokerCallsitesChanged;

        dst.nestedJarsChanged += src.nestedJarsChanged;

        src.rawTopMixinCounts.forEach(
                (k, v) -> dst.rawTopMixinCounts.merge(k, v, Long::sum));

        src.rawNestedCounts.forEach(
                (k, v) -> dst.rawNestedCounts.merge(k, v, Long::sum));

        src.accessorCounts.forEach(
                (k, v) -> dst.accessorCounts.merge(k, v, Long::sum));

        src.invokerCounts.forEach(
                (k, v) -> dst.invokerCounts.merge(k, v, Long::sum));
    }

    private static ZipPatchResult patchZip(
            byte[] input,
            Map<String, String> mappings,
            Stats stats,
            int depth,
            boolean nestedJarMode) throws IOException {

        if (depth > 4) {
            return new ZipPatchResult(input, false);
        }

        List<EntryData> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try (ZipInputStream in =
                     new ZipInputStream(new ByteArrayInputStream(input))) {

            ZipEntry entry;

            while ((entry = in.getNextEntry()) != null) {
                if (!seen.add(entry.getName())) {
                    continue;
                }

                entries.add(
                        new EntryData(
                                entry.getName(),
                                in.readAllBytes(),
                                entry.getTime(),
                                entry.isDirectory()));
            }
        }

        // Accessor/invoker definitions are discovered from real Mixin classes
        // in the current archive.
        Map<String, String> methodRenames = new LinkedHashMap<>();

        for (EntryData entry : entries) {
            if (!entry.name.endsWith(".class")
                    || !isMixinClass(entry.data)) {
                continue;
            }

            for (String utf8 : readUtf8Constants(entry.data)) {
                discoverMethodRenames(
                        utf8,
                        mappings,
                        methodRenames);
            }
        }

        boolean changed = false;
        List<EntryData> patchedEntries =
                new ArrayList<>(entries.size());

        for (EntryData entry : entries) {
            byte[] data = entry.data;
            boolean entryChanged = false;

            if (entry.name.endsWith(".class")) {
                boolean mixinClass = isMixinClass(data);
                long before = stats.totalChanges();

                data = patchClass(
                        data,
                        mappings,
                        methodRenames,
                        mixinClass,
                        nestedJarMode,
                        stats);

                entryChanged = stats.totalChanges() > before;

                if (entryChanged) {
                    if (nestedJarMode) {
                        stats.nestedClassesChanged++;
                    } else if (mixinClass) {
                        stats.topLevelMixinClassesChanged++;
                    } else {
                        stats.topLevelNormalClassesChanged++;
                    }
                }

            } else if (entry.name.toLowerCase(Locale.ROOT).endsWith(".jar")
                    && data.length >= 4
                    && data[0] == 'P'
                    && data[1] == 'K') {

                // ForgeGradle fg.deobf remaps the outer dependency, but a
                // production JarJar under META-INF/jars is still loaded as its
                // original nested archive. Those classes therefore need the
                // same SRG->MojMap conversion themselves.
                boolean childNestedJarMode =
                        nestedJarMode
                                || entry.name.replace('\\', '/')
                                .toUpperCase(Locale.ROOT)
                                .startsWith("META-INF/JARS/");

                ZipPatchResult nested = patchZip(
                        data,
                        mappings,
                        stats,
                        depth + 1,
                        childNestedJarMode);

                if (nested.changed) {
                    data = nested.bytes;
                    entryChanged = true;
                    stats.nestedJarsChanged++;
                }
            }

            changed |= entryChanged;

            patchedEntries.add(
                    new EntryData(
                            entry.name,
                            data,
                            entry.time,
                            entry.directory));
        }

        if (!changed) {
            return new ZipPatchResult(input, false);
        }

        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream(
                        Math.max(8192, input.length));

        try (ZipOutputStream out =
                     new ZipOutputStream(bytes)) {

            for (EntryData entry : patchedEntries) {
                String upper =
                        entry.name.toUpperCase(Locale.ROOT);

                // Any archive whose bytecode was changed can no longer keep
                // the original signatures.
                if (upper.startsWith("META-INF/")
                        && (upper.endsWith(".SF")
                        || upper.endsWith(".RSA")
                        || upper.endsWith(".DSA")
                        || upper.endsWith(".EC"))) {
                    continue;
                }

                ZipEntry zipEntry =
                        new ZipEntry(entry.name);

                if (entry.time >= 0) {
                    zipEntry.setTime(entry.time);
                }

                out.putNextEntry(zipEntry);

                if (!entry.directory) {
                    out.write(entry.data);
                }

                out.closeEntry();
            }
        }

        return new ZipPatchResult(
                bytes.toByteArray(),
                true);
    }

    private static void discoverMethodRenames(
            String value,
            Map<String, String> mappings,
            Map<String, String> methodRenames) {

        Matcher accessor =
                ACCESSOR_NAME.matcher(value);

        while (accessor.find()) {
            String key =
                    canonical(accessor.group(2));

            String mapped =
                    mappings.get(key);

            if (mapped != null) {
                methodRenames.put(
                        accessor.group(),
                        accessor.group(1)
                                + capitalize(mapped));
            }
        }

        Matcher invoker =
                INVOKER_NAME.matcher(value);

        while (invoker.find()) {
            String key =
                    canonical(invoker.group(2));

            String mapped =
                    mappings.get(key);

            if (mapped != null) {
                methodRenames.put(
                        invoker.group(),
                        invoker.group(1)
                                + capitalize(mapped));
            }
        }
    }

    private static byte[] patchClass(
            byte[] input,
            Map<String, String> mappings,
            Map<String, String> methodRenames,
            boolean mixinClass,
            boolean nestedJarMode,
            Stats stats) throws IOException {

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(input));

        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream(
                        input.length + 128);

        DataOutputStream out =
                new DataOutputStream(buffer);

        int magic = in.readInt();

        if (magic != 0xCAFEBABE) {
            return input;
        }

        out.writeInt(magic);
        out.writeShort(in.readUnsignedShort());
        out.writeShort(in.readUnsignedShort());

        int cpCount =
                in.readUnsignedShort();

        out.writeShort(cpCount);

        for (int i = 1; i < cpCount; i++) {
            int tag =
                    in.readUnsignedByte();

            out.writeByte(tag);

            switch (tag) {
                case 1 -> {
                    String original = in.readUTF();
                    String patched = original;

                    // Rename only exact accessor/invoker names discovered from
                    // actual Mixin classes.
                    for (var e : methodRenames.entrySet()) {
                        String source = e.getKey();
                        String target = e.getValue();

                        if (patched.equals(source)) {
                            patched = target;

                            if (mixinClass) {
                                if (isAccessorPrefix(source)) {
                                    stats.accessorDefinitionsChanged++;
                                    stats.accessorCounts.merge(
                                            source + " -> " + target,
                                            1L,
                                            Long::sum);
                                } else {
                                    stats.invokerDefinitionsChanged++;
                                    stats.invokerCounts.merge(
                                            source + " -> " + target,
                                            1L,
                                            Long::sum);
                                }
                            } else {
                                if (isAccessorPrefix(source)) {
                                    stats.accessorCallsitesChanged++;
                                } else {
                                    stats.invokerCallsitesChanged++;
                                }
                            }
                        }
                    }

                    // Top-level: raw SRG only in real Mixin classes.
                    // Nested META-INF/jars: raw SRG in all classes because the
                    // nested production archive is not remapped by fg.deobf.
                    boolean allowRawSrg =
                            mixinClass || nestedJarMode;

                    if (allowRawSrg) {
                        Matcher raw =
                                RAW_SRG.matcher(patched);

                        StringBuffer sb = null;

                        while (raw.find()) {
                            String source =
                                    raw.group(1);

                            String mapped =
                                    mappings.get(
                                            canonical(source));

                            if (mapped == null) {
                                continue;
                            }

                            if (sb == null) {
                                sb = new StringBuffer();
                            }

                            raw.appendReplacement(
                                    sb,
                                    Matcher.quoteReplacement(
                                            mapped));

                            if (nestedJarMode) {
                                stats.rawNestedJarChanges++;
                                stats.rawNestedCounts.merge(
                                        source,
                                        1L,
                                        Long::sum);
                            } else if (mixinClass) {
                                stats.rawTopLevelMixinChanges++;
                                stats.rawTopMixinCounts.merge(
                                        source,
                                        1L,
                                        Long::sum);
                            } else {
                                // Defensive invariant. This branch should
                                // never be reachable.
                                stats.rawTopLevelNormalChanges++;
                            }
                        }

                        if (sb != null) {
                            raw.appendTail(sb);
                            patched = sb.toString();
                        }
                    }

                    out.writeUTF(patched);
                }

                case 3, 4 ->
                        out.writeInt(in.readInt());

                case 5, 6 -> {
                    out.writeLong(in.readLong());
                    i++;
                }

                case 7, 8, 16, 19, 20 ->
                        out.writeShort(
                                in.readUnsignedShort());

                case 9, 10, 11, 12, 17, 18 -> {
                    out.writeShort(
                            in.readUnsignedShort());
                    out.writeShort(
                            in.readUnsignedShort());
                }

                case 15 -> {
                    out.writeByte(
                            in.readUnsignedByte());
                    out.writeShort(
                            in.readUnsignedShort());
                }

                default ->
                        throw new IOException(
                                "Unsupported constant-pool tag "
                                        + tag);
            }
        }

        in.transferTo(out);
        out.flush();

        return buffer.toByteArray();
    }

    private static boolean isAccessorPrefix(String value) {
        return value.startsWith("get")
                || value.startsWith("set")
                || value.startsWith("is");
    }

    private static List<String> readUtf8Constants(
            byte[] input) throws IOException {

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(input));

        if (in.readInt() != 0xCAFEBABE) {
            return List.of();
        }

        in.readUnsignedShort();
        in.readUnsignedShort();

        int cpCount =
                in.readUnsignedShort();

        List<String> result =
                new ArrayList<>();

        for (int i = 1; i < cpCount; i++) {
            int tag =
                    in.readUnsignedByte();

            switch (tag) {
                case 1 ->
                        result.add(in.readUTF());

                case 3, 4 ->
                        in.readInt();

                case 5, 6 -> {
                    in.readLong();
                    i++;
                }

                case 7, 8, 16, 19, 20 ->
                        in.readUnsignedShort();

                case 9, 10, 11, 12, 17, 18 -> {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                }

                case 15 -> {
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                }

                default ->
                        throw new IOException(
                                "Unsupported constant-pool tag "
                                        + tag);
            }
        }

        return result;
    }

    private static boolean isMixinClass(byte[] data) {
        return indexOf(data, MIXIN_MARKER) >= 0;
    }

    private static int indexOf(
            byte[] data,
            byte[] needle) {

        outer:
        for (int i = 0;
             i <= data.length - needle.length;
             i++) {

            for (int j = 0;
                 j < needle.length;
                 j++) {

                if (data[i + j] != needle[j]) {
                    continue outer;
                }
            }

            return i;
        }

        return -1;
    }

    private static Map<String, String> loadMappings(
            Path file) throws IOException {

        Map<String, String> out =
                new HashMap<>();

        for (String raw :
                Files.readAllLines(
                        file,
                        StandardCharsets.UTF_8)) {

            String line = raw.trim();

            if (line.isEmpty()
                    || line.startsWith("#")) {
                continue;
            }

            String[] parts =
                    line.split("\\s+");

            if (parts.length >= 3
                    && "FD:".equals(parts[0])) {
                putIfSrg(
                        out,
                        lastName(parts[1]),
                        lastName(parts[2]));
                continue;
            }

            if (parts.length >= 5
                    && "MD:".equals(parts[0])) {
                putIfSrg(
                        out,
                        lastName(parts[1]),
                        lastName(parts[3]));
                continue;
            }

            if (parts.length == 2
                    && isSrg(parts[0])) {
                putIfSrg(
                        out,
                        parts[0],
                        parts[1]);
                continue;
            }

            if (parts.length >= 3
                    && SRG_METHOD.matcher(parts[0]).matches()
                    && parts[1].startsWith("(")) {
                putIfSrg(
                        out,
                        parts[0],
                        parts[2]);
            }
        }

        return out;
    }

    private static void requireMapping(
            Map<String, String> mappings,
            String source) {

        if (!mappings.containsKey(source)) {
            throw new IllegalStateException(
                    "Required mapping is missing: "
                            + source);
        }
    }

    private static boolean isSrg(String value) {
        return SRG_FIELD.matcher(value).matches()
                || SRG_METHOD.matcher(value).matches();
    }

    private static void putIfSrg(
            Map<String, String> out,
            String source,
            String destination) {

        if (isSrg(source)
                && destination != null
                && !destination.isBlank()
                && !source.equals(destination)) {

            out.put(
                    source,
                    destination);
        }
    }

    private static String lastName(String path) {
        int slash =
                path.lastIndexOf('/');

        return slash >= 0
                ? path.substring(slash + 1)
                : path;
    }

    private static String canonical(String token) {
        if (token == null
                || token.isEmpty()) {
            return token;
        }

        char first =
                Character.toLowerCase(
                        token.charAt(0));

        if (first == token.charAt(0)) {
            return token;
        }

        return first
                + token.substring(1);
    }

    private static String capitalize(String value) {
        if (value == null
                || value.isEmpty()) {
            return value;
        }

        return Character.toUpperCase(
                value.charAt(0))
                + value.substring(1);
    }
}
