# Genetic Archive side branch V2

Trigger:
- `OXYGEN_INFRASTRUCTURE_READY`
- wait 12,000 world ticks (10 minutes at 20 TPS)
- when at least one player is online: set `GENETIC_ARCHIVE_SIGNAL_FOUND`
- play `domesurvival:genetic_archive_signal` to every online player
- broadcast Joseph's purple chat message
- grant the technical advancement and synchronize hidden FTB bridge quest `70CE4EBCBA38CD21`

FTB Quests:
- hidden technical bridge: `70CE4EBCBA38CD21`
- `4AA418B9DF3B79A4` — «Последние из живых»: obtain vanilla `minecraft:compass`
- `515C1A05E15F3F67` — «Настройка на маяк»: automatically bind one ordinary compass
- `0F7B71D5BDCBD296` — «Следуй за стрелкой»: reach hidden target within 32 blocks
- `3B095F94C8D72753` — «Последние образцы»: recover one chicken, sheep and cow capsule
- `4D7992E0A771B3A1` — «Биоинкубатор»: obtain the machine
- `6274AE251790C825` — «Первое дыхание»: complete the first successful incubation

The visible branch is optional and all visible quests use `hide_until_deps_complete: true`.
The first quest depends on the technical quest in the existing always-invisible Joseph bridge chapter.

Compass:
- remains `minecraft:compass`
- uses vanilla 1.20.1 Lodestone NBT
- `LodestoneTracked=false`
- never overwrites a player's existing Lodestone compass
- target is deterministic per world and stored in SavedData
- the target is selected from `#domesurvival:genetic_archive_targets`
- target structures include available desert temples, graveyards and dungeon mods
- the locator prefers a target at least 384 blocks from the world spawn
- exact XYZ are not shown to normal players

Archive cache:
- placed once after the selected target chunk is loaded; chunks are never force-generated
- prefers a free position near an existing loaded container
- if the structure has no storage or reports technical `Y=0`, uses its actual surface
- contains one chicken, one sheep, one cow and one damaged pig capsule
- the four guaranteed samples are registered in the same persistent distribution ledger as normal loot
- random structure loot cannot produce those four first copies before the archive is reached
- one first copy of every available species is distributed before any second copy can appear
- a second copy must be at least 500 horizontal blocks from its first copy
- one generated building can contain at most one randomly distributed module
- existing worlds migrate the archive samples into the ledger when its chunk is next loaded

Debug commands (permission 2):
- `/domearchive status`
- `/domearchive trigger`

`/domearchive status` also reports cache, distribution-ledger registration, guards, sample recovery, database unlock,
Bioincubator readiness and first-birth state, which makes the complete branch auditable in any save.
