# Project status

## Ready
- Forge 1.20.1 / 47.4.10 project skeleton
- Java 17 compilation target
- Minecraft-independent dome geometry core
- Dome center: -506 62 -641
- Surface radius: 50
- Underground safe radius: 45, down to Y=-64
- Reinforced glass and dome frame registrations
- `/dome preview`
- `/dome generate`
- `/dome status`
- Batched shell generation (750 blocks/tick)
- SavedData guard against duplicate generation
- Disposable WASTED test-world workflow

## Verified locally
- Core sources compile with `javac --release 17`
- Geometry self-test passes
- Output classfile major version = 61 (Java 17)
- Planned V1 shell = 24,950 blocks

## Next integration gate
Resolve ForgeGradle/Forge dependencies and run `runServer` against the disposable WASTED test world. Then validate preview, generation, chunk saving and reload behavior before oxygen logic is added.
