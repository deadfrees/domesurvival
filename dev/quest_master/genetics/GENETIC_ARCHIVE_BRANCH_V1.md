# Genetic Archive side branch V1

Trigger:
- `OXYGEN_INFRASTRUCTURE_READY`
- wait 12,000 world ticks (10 minutes at 20 TPS)
- when at least one player is online: set `GENETIC_ARCHIVE_SIGNAL_FOUND`
- play `domesurvival:genetic_archive_signal` to every online player
- broadcast Joseph's purple chat message
- force-complete hidden FTB bridge quest `70CE4EBCBA38CD21` for online players

FTB Quests:
- hidden technical bridge: `70CE4EBCBA38CD21`
- `4AA418B9DF3B79A4` — «Последние из живых»: obtain vanilla `minecraft:compass`
- `515C1A05E15F3F67` — «Настройка на маяк»: automatically bind one ordinary compass
- `0F7B71D5BDCBD296` — «Следуй за стрелкой»: reach hidden target within 18 blocks

The visible branch is optional and all visible quests use `hide_until_deps_complete: true`.
The first quest depends on the technical quest in the existing always-invisible Joseph bridge chapter.

Compass:
- remains `minecraft:compass`
- uses vanilla 1.20.1 Lodestone NBT
- `LodestoneTracked=false`
- never overwrites a player's existing Lodestone compass
- target is deterministic per world and stored in SavedData
- target radius: 240..300 blocks from the starter dome center
- exact XYZ are not shown to normal players

Debug commands (permission 2):
- `/domearchive status`
- `/domearchive trigger`

V1 deliberately stops at discovery of the archive location.
Physical genetic capsules and the Bioincubator are the next phase, so this patch does not introduce placeholder capsule items that would later break save compatibility.

Installer V1.1 note: shared sounds.json is merged semantically, never replaced wholesale.
