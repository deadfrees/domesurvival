import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/**
 * Targeted CustomNPCs development bridge V1.3.
 *
 * It does NOT ForgeGradle-deobfuscate the production JAR.
 * Instead it translates raw Minecraft SRG field/method names to MojMap inside
 * the CustomNPCs JAR itself. V1.2 handled Mixins only; V1.3 also repairs normal
 * CustomNPCs classes because production classes such as PlayerData directly
 * reference fields like f_121853_ in a MojMap runClient.
 */
public final class CustomNpcsDevBridge {
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

    private static long mixinClassesChanged;
    private static long normalAccessorCallsitesChanged;
    private static long refmapFilesChanged;
    private static long rawMixinTokensChanged;
    private static long rawNormalClassTokensChanged;
    private static long accessTransformerFilesChanged;
    private static long accessorNamesChanged;
    private static long invokerNamesChanged;
    private static final Map<String, Long> changes = new TreeMap<>();

    private record Entry(String name, byte[] data, long time, boolean directory) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: CustomNpcsDevBridge <mapping.srg> <input.jar> <output.jar> <report.txt>");
            System.exit(2);
        }

        Path mappingFile = Paths.get(args[0]);
        Path inputJar = Paths.get(args[1]);
        Path outputJar = Paths.get(args[2]);
        Path report = Paths.get(args[3]);

        Map<String, String> mappings = loadMappings(mappingFile);
        if (mappings.size() < 1000) {
            throw new IllegalStateException("Too few SRG->MojMap mappings: " + mappings.size());
        }
        if (!mappings.containsKey("f_135345_")) {
            throw new IllegalStateException("Required SynchedEntityData mapping f_135345_ is missing.");
        }

        byte[] input = Files.readAllBytes(inputJar);
        byte[] output = patchJar(input, mappings);
        Files.createDirectories(outputJar.toAbsolutePath().getParent());
        Files.write(outputJar, output);

        StringBuilder sb = new StringBuilder();
        sb.append("DomeSurvival CustomNPCs SRG-to-MojMap Bridge V1.3\n");
        sb.append("Mappings: ").append(mappings.size()).append('\n');
        sb.append("f_135345_ -> ").append(mappings.get("f_135345_")).append('\n');
        sb.append("Mixin classes changed: ").append(mixinClassesChanged).append('\n');
        sb.append("Normal accessor/invoker callsites changed: ").append(normalAccessorCallsitesChanged).append('\n');
        sb.append("Refmap/mixin JSON files changed: ").append(refmapFilesChanged).append('\n');
        sb.append("Raw SRG mixin tokens changed: ").append(rawMixinTokensChanged).append('\n');
        sb.append("Raw SRG normal-class tokens changed: ").append(rawNormalClassTokensChanged).append('\n');
        sb.append("Access-transformer/resource files changed: ").append(accessTransformerFilesChanged).append('\n');
        sb.append("Accessor names changed: ").append(accessorNamesChanged).append('\n');
        sb.append("Invoker names changed: ").append(invokerNamesChanged).append('\n');
        sb.append("\nChanges:\n");
        changes.forEach((k, v) -> sb.append(k).append(" x").append(v).append('\n'));

        if (mixinClassesChanged == 0 || rawMixinTokensChanged == 0) {
            throw new IllegalStateException(
                    "No CustomNPCs Mixin SRG names were patched; refusing to launch an unchanged JAR.");
        }
        if (rawNormalClassTokensChanged == 0) {
            throw new IllegalStateException(
                    "No raw SRG names were found in normal CustomNPCs classes; V1.3 expected production SRG references.");
        }

        Files.createDirectories(report.toAbsolutePath().getParent());
        Files.writeString(report, sb.toString(), StandardCharsets.UTF_8);
        System.out.print(sb);
    }

    private static byte[] patchJar(byte[] input, Map<String, String> mappings) throws IOException {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(input))) {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                if (!seen.add(ze.getName())) continue;
                entries.add(new Entry(ze.getName(), zin.readAllBytes(), ze.getTime(), ze.isDirectory()));
            }
        }

        // Discover accessor/invoker method renames from Mixin classes first.
        Map<String, String> methodRenames = new LinkedHashMap<>();
        for (Entry e : entries) {
            if (!e.name.endsWith(".class") || !isMixinClass(e.data)) continue;
            for (String s : readUtf8Constants(e.data)) {
                discoverMethodRenames(s, mappings, methodRenames);
            }
        }

        List<Entry> patched = new ArrayList<>(entries.size());
        boolean jarChanged = false;

        for (Entry e : entries) {
            byte[] data = e.data;
            boolean changed = false;

            if (e.name.endsWith(".class")) {
                boolean mixin = isMixinClass(data);
                PatchResult result = patchClass(data, mappings, methodRenames, mixin);
                data = result.bytes;
                changed = result.changed;
                if (changed && mixin) mixinClassesChanged++;
                if (changed && !mixin) normalAccessorCallsitesChanged++;
            } else if (isSrgTextResource(e.name)) {
                String original = new String(data, StandardCharsets.UTF_8);
                String replaced = replaceRawTokens(original, mappings, true);
                for (var mr : methodRenames.entrySet()) {
                    replaced = replaced.replace(mr.getKey(), mr.getValue());
                }
                if (!replaced.equals(original)) {
                    data = replaced.getBytes(StandardCharsets.UTF_8);
                    changed = true;
                    String lowerName = e.name.toLowerCase(Locale.ROOT);
                    if (lowerName.contains("refmap") || lowerName.contains("mixin")) {
                        refmapFilesChanged++;
                    } else {
                        accessTransformerFilesChanged++;
                    }
                }
            }

            jarChanged |= changed;
            patched.add(new Entry(e.name, data, e.time, e.directory));
        }

        if (!jarChanged) return input;

        ByteArrayOutputStream bout = new ByteArrayOutputStream(input.length);
        try (ZipOutputStream zout = new ZipOutputStream(bout)) {
            for (Entry e : patched) {
                String upper = e.name.toUpperCase(Locale.ROOT);
                if (upper.startsWith("META-INF/")
                        && (upper.endsWith(".SF") || upper.endsWith(".RSA")
                        || upper.endsWith(".DSA") || upper.endsWith(".EC"))) {
                    continue;
                }

                ZipEntry ze = new ZipEntry(e.name);
                if (e.time >= 0) ze.setTime(e.time);
                zout.putNextEntry(ze);
                if (!e.directory) zout.write(e.data);
                zout.closeEntry();
            }
        }
        return bout.toByteArray();
    }

    private record PatchResult(byte[] bytes, boolean changed) {}

    private static PatchResult patchClass(
            byte[] input,
            Map<String, String> mappings,
            Map<String, String> methodRenames,
            boolean mixin
    ) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(input));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(input.length + 128);
        DataOutputStream out = new DataOutputStream(buffer);

        int magic = in.readInt();
        if (magic != 0xCAFEBABE) return new PatchResult(input, false);

        out.writeInt(magic);
        out.writeShort(in.readUnsignedShort());
        out.writeShort(in.readUnsignedShort());

        int cpCount = in.readUnsignedShort();
        out.writeShort(cpCount);
        boolean changed = false;

        for (int i = 1; i < cpCount; i++) {
            int tag = in.readUnsignedByte();
            out.writeByte(tag);

            switch (tag) {
                case 1 -> {
                    String original = in.readUTF();
                    String value = original;

                    // Accessor/invoker method definitions in Mixins AND callsites in
                    // normal CustomNPCs classes must stay in sync.
                    for (var mr : methodRenames.entrySet()) {
                        if (value.equals(mr.getKey())) {
                            value = mr.getValue();
                            changed = true;
                            if (mr.getKey().startsWith("get")
                                    || mr.getKey().startsWith("set")
                                    || mr.getKey().startsWith("is")) {
                                accessorNamesChanged++;
                            } else {
                                invokerNamesChanged++;
                            }
                            changes.merge(mr.getKey() + " -> " + mr.getValue(), 1L, Long::sum);
                        }
                    }

                    // V1.3: production CustomNPCs contains direct SRG references
                    // in ordinary classes too (for example PlayerData -> f_121853_).
                    // Remap raw SRG names in every CustomNPCs class, but nowhere
                    // outside this one JAR.
                    String patched = replaceRawTokens(value, mappings, mixin);
                    if (!patched.equals(value)) {
                        value = patched;
                        changed = true;
                    }

                    out.writeUTF(value);
                }
                case 3, 4 -> out.writeInt(in.readInt());
                case 5, 6 -> { out.writeLong(in.readLong()); i++; }
                case 7, 8, 16, 19, 20 -> out.writeShort(in.readUnsignedShort());
                case 9, 10, 11, 12, 17, 18 -> {
                    out.writeShort(in.readUnsignedShort());
                    out.writeShort(in.readUnsignedShort());
                }
                case 15 -> {
                    out.writeByte(in.readUnsignedByte());
                    out.writeShort(in.readUnsignedShort());
                }
                default -> throw new IOException("Unsupported constant-pool tag " + tag);
            }
        }

        in.transferTo(out);
        out.flush();
        return changed ? new PatchResult(buffer.toByteArray(), true) : new PatchResult(input, false);
    }

    private static String replaceRawTokens(
            String input,
            Map<String, String> mappings,
            boolean mixinOrResource
    ) {
        Matcher matcher = RAW_SRG.matcher(input);
        StringBuffer sb = null;
        while (matcher.find()) {
            String source = canonical(matcher.group(1));
            String mapped = mappings.get(source);
            if (mapped == null) continue;
            if (sb == null) sb = new StringBuffer();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(mapped));
            if (mixinOrResource) {
                rawMixinTokensChanged++;
            } else {
                rawNormalClassTokensChanged++;
            }
            changes.merge(source + " -> " + mapped, 1L, Long::sum);
        }
        if (sb == null) return input;
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void discoverMethodRenames(
            String value,
            Map<String, String> mappings,
            Map<String, String> output
    ) {
        Matcher a = ACCESSOR_NAME.matcher(value);
        while (a.find()) {
            String mapped = mappings.get(canonical(a.group(2)));
            if (mapped != null) {
                output.put(a.group(), a.group(1) + capitalize(mapped));
            }
        }

        Matcher i = INVOKER_NAME.matcher(value);
        while (i.find()) {
            String mapped = mappings.get(canonical(i.group(2)));
            if (mapped != null) {
                output.put(i.group(), i.group(1) + capitalize(mapped));
            }
        }
    }

    private static boolean isSrgTextResource(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json") && (lower.contains("refmap") || lower.contains("mixin"))) {
            return true;
        }
        return lower.endsWith("accesstransformer.cfg")
                || lower.contains("access_transformer")
                || lower.endsWith(".at");
    }

    private static boolean isMixinClass(byte[] data) {
        return indexOf(data, MIXIN_MARKER) >= 0;
    }

    private static int indexOf(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static List<String> readUtf8Constants(byte[] input) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(input));
        if (in.readInt() != 0xCAFEBABE) return List.of();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int cpCount = in.readUnsignedShort();

        List<String> out = new ArrayList<>();
        for (int i = 1; i < cpCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> out.add(in.readUTF());
                case 3, 4 -> in.readInt();
                case 5, 6 -> { in.readLong(); i++; }
                case 7, 8, 16, 19, 20 -> in.readUnsignedShort();
                case 9, 10, 11, 12, 17, 18 -> {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                }
                case 15 -> {
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                }
                default -> throw new IOException("Unsupported constant-pool tag " + tag);
            }
        }
        return out;
    }

    private static Map<String, String> loadMappings(Path file) throws IOException {
        Map<String, String> out = new HashMap<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");

            if (p.length >= 3 && "FD:".equals(p[0])) {
                putIfSrg(out, lastName(p[1]), lastName(p[2]));
                continue;
            }
            if (p.length >= 5 && "MD:".equals(p[0])) {
                putIfSrg(out, lastName(p[1]), lastName(p[3]));
                continue;
            }
            if (p.length == 2 && isSrg(p[0])) {
                putIfSrg(out, p[0], p[1]);
                continue;
            }
            if (p.length >= 3 && SRG_METHOD.matcher(p[0]).matches() && p[1].startsWith("(")) {
                putIfSrg(out, p[0], p[2]);
            }
        }
        return out;
    }

    private static void putIfSrg(Map<String, String> out, String source, String dest) {
        if (isSrg(source) && dest != null && !dest.isBlank() && !source.equals(dest)) {
            out.put(source, dest);
        }
    }

    private static boolean isSrg(String s) {
        return SRG_FIELD.matcher(s).matches() || SRG_METHOD.matcher(s).matches();
    }

    private static String lastName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String canonical(String token) {
        if (token == null || token.isEmpty()) return token;
        char first = Character.toLowerCase(token.charAt(0));
        return first == token.charAt(0) ? token : first + token.substring(1);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
