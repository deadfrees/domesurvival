# Phase 2 technical notes

Target: Minecraft 1.20.1, Forge 47.4.x, FTB Quests 2001.4.22.

## Important V1 -> V2 ID correction

Before publishing the first real FTB SNBT, the 2001.4.22 source was checked.

`BaseQuestFile.readID(StringTag)` uses `Long.parseLong(hex, 16)`. Therefore a
16-character ID whose highest hex digit is `8..F` cannot be loaded as a signed
positive Java long.

Phase 1 was unaffected because its IDs existed only in Dome Survival's Java
string registry and `/domequest registry`; no FTB SNBT existed yet.

Phase 2 replaces the campaign registry with signed-safe IDs **before** any FTB
quest progress is published. This is the last safe point to do it.

## FTB integration choices

- Chapter text is stored inline in 1.20.1 SNBT. We do not depend on the
  problematic Forge 1.20.1 external lang migration path.
- Story flag rewards use command rewards with:
  - `elevate_perms: true`
  - `silent: true`
  - `auto: "invisible"`
- FTB Quests 2001.4.15 fixed forced-progress handling for auto-claim rewards;
  target runtime is 2001.4.22.
- The visible Chapter 0 is linear, but visually branched.
- The Joseph technical bridge is in an `always_invisible` chapter and does not
  count as a player-facing campaign chapter.
