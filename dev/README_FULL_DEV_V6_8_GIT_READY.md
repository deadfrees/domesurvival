# FULL DEV V6.8 Git-ready hotfix

The runtime behavior is unchanged from the working V6.8 profile.

This hotfix removes the only remaining machine-local dependency:

`dev/tools/bin/MixinSrgBridge.class`

The repository already ignores `*.class`, so a fresh clone would receive
`MixinSrgBridge.java` but not the compiled bridge class.

`PREPARE_FULL_DEV_RUNTIME.bat` now compiles the bridge locally on every FULL DEV
preparation:

    javac --release 17 -encoding UTF-8

Requirements:

- Java 17 JDK
- `java` and `javac` available in PATH

The generated `.class` remains local/ignored and does not need to be committed.

Second developer after Git pull:

    dev\SECOND_DEVELOPER_SETUP.bat

Normal daily workflow:

    dev\UPDATE_AND_RUN_FULL.bat


## Developer 2 without a separate production instance

An external canonical source is optional.

If `C:\domesurvival\run\mods` already contains the approved full physical pack,
V6.8 can use that folder as its local baseline. No symlink or external runtime
reference is used.

An external `DOMESURVIVAL_MODPACK_SOURCE` remains optional and is useful only
for automatic repair/reseeding of missing third-party JARs.
