# Internal reward visual cleanup

FTB Quests 1.20.1 intentionally hides `RewardAutoClaim.INVISIBLE` rewards during normal play, but explicitly renders them while quest editing is enabled.

Because development/testing commonly leaves FTB editing mode enabled, the technical feedback command rewards were visible as command blocks.

Phase 3 keeps the reward `auto: "invisible"` and does not change its behavior. It only overrides the editor-facing fallback:
- title: blank
- icon: `minecraft:gray_stained_glass_pane`

Result:
- normal gameplay: technical reward is not visible at all;
- editing mode: a neutral gray pane is shown instead of a command block;
- sound/particles continue working exactly as before.

This also applies retroactively to Chapter 0.
