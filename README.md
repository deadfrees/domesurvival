# DomeSurvival Dev

Target stack:
- Minecraft 1.20.1
- Forge 47.4.10 baseline
- Java 17 target
- Map: WASTED V0.5

Dome V1 geometry:
- center: X=-506, Y=62, Z=-641
- surface radius: 50
- vertical skirt: 3
- top: Y=115
- underground safe radius: 45
- underground minimum: Y=-64

## Fast Java tests

```bash
./dev/scripts/build-core.sh
```

The core is intentionally Minecraft-independent. The local harness compiles with `javac --release 17` and rejects output unless classfile major version is 61 (Java 17).

## Forge layer

`build.gradle` is prepared for Forge 1.20.1 / 47.4.10 and requests a Java 17 toolchain. Full Forge dependency resolution/run tasks require a Gradle installation/wrapper and Forge artifacts. The pure-Java core can be tested independently in this environment.

## WASTED test copy

```bash
./dev/scripts/prepare-wasted-test-world.sh
```

This creates a disposable world under `dev/worlds/WASTED_TEST` while leaving the uploaded original archive untouched.
