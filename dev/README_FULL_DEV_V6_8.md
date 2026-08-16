# DomeSurvival FULL DEV V6.8 — JarJar-aware Mixin SRG Bridge

V6.7 fixed the CraftTweaker regression by stopping global SRG rewriting of
ordinary top-level mod classes. Its report correctly showed:

    Raw SRG top-level non-mixin changes: 0

The next FULL DEV crash exposed a separate ForgeGradle/JarJar boundary:

    EntityCulling -> nested META-INF/jars/TRansition-1.0.21...
    Minecraft.m_91087_()
    DetectedVersion.m_195834_()

The outer EntityCulling JAR is passed through fg.deobf, but its nested
production JarJar is still loaded with SRG member names.

V6.8 therefore uses three explicit scopes:

1. Top-level normal classes:
   raw SRG is NEVER rewritten.

2. Top-level actual @Mixin classes:
   raw SRG is rewritten SRG -> Mojang mappings.

3. Classes inside META-INF/jars/*.jar:
   raw SRG is rewritten for the entire nested archive, because that archive is
   not remapped by the outer fg.deobf dependency.

Accessor/invoker inference from V6.7 remains enabled.

Safety invariants:

    Raw SRG top-level non-mixin changes: 0

Runtime validation also requires the current nested TRansition failures to be
found and remapped:

    RAW_NESTED m_91087_ -> getInstance
    RAW_NESTED m_195834_ -> <mapped Mojang name>

Original run/mods JARs are never edited. Only dev/generated/fullmods copies are
changed.

Run:

    dev\FORCE_REBUILD_FULL_CACHE.bat
    dev\RUN_DEV_FULL.bat
