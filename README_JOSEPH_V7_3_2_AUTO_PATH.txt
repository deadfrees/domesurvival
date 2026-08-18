DomeSurvival Joseph Questline V7.3.2 AUTO + Stage 01 Path
================================================================

This package supersedes the failed direct V7.3 install.

It auto-detects the current Joseph script and supports:
V7.0 -> V7.1 -> V7.2 -> V7.3 -> V7.3.2
V7.1 -> V7.2 -> V7.3 -> V7.3.2
V7.2 -> V7.3 -> V7.3.2
V7.3 -> V7.3.2

New Stage 01 environmental upgrade
----------------------------------
After Stage 01 is completed, the existing exposed dirt/coarse-dirt strip
nearest Joseph is converted into vanilla minecraft:dirt_path.

The conversion is intentionally conservative:
- only minecraft:dirt and minecraft:coarse_dirt
- only exposed surface blocks
- connected trail only
- radius <= 40 blocks from the detected trail start
- maximum 192 blocks
- grass, stone, machines, containers and structures are not changed

The same upgrade is triggered after /josephscript nextstage:
run nextstage, then right-click Joseph once.

Install:
  .\APPLY_JOSEPH_QUESTLINE_V7_3_2_AUTO.bat

Then:
  .\dev\RUN_DEV_FULL.bat

In WASTED_TEST:
  /josephscript apply

Fast test:
  /josephscript nextstage
