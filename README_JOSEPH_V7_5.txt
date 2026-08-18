DomeSurvival Joseph V7.5 — Progression Balance
================================================

This patch addresses four progression-design problems:

1. Rewards are no longer prepaid requirements.
   Reward packages R1-R12 were audited and rebuilt so their exact item IDs are
   not required by later V7.5 stages. The worst traps are removed entirely:
   - Stage 7 no longer rewards Stage 8 steel pipes/hoppers/reinforced energy;
   - Stage 9 no longer rewards the Pulverizer/Smelter/steel plates Stage 10
     immediately asked the player to surrender;
   - Stage 10 no longer rewards Stage 11 circuit boards;
   - Stage 11 no longer rewards the rocket parts Stage 12 immediately consumed.

2. Generic material names are genuinely generic.
   Definitions can now use Forge item tags. "Стальной слиток" accepts items in
   forge:ingots/steel and "Стальная пластина" accepts forge:plates/steel.
   Explicit ID fallbacks cover Ad Astra, Mekanism, Immersive Engineering and
   Thermal. Exact modded machines/components are explicitly labelled by mod.

3. Dome construction blocks are quest-forbidden.
   domesurvival:reinforced_glass, domesurvival:dome_frame and
   domesurvival:dome_foundation are not accepted as Joseph requirements and are
   not granted as Joseph rewards. They remain construction-only world blocks.

4. Current-situation text no longer repeats.
   Stages 9, 10, 11, 12 and completed Stage 13 each have a separate situation
   briefing. The completion badge now appears only at Stage 13.

Existing test-world migration:
- old Stage 10 Ad Astra/Mekanism steel progress is merged into the new generic
  steel row;
- old Ad Astra/IE plate progress is merged into the generic plate row;
- if old Stage 9 rewards were claimed and the rewarded Thermal Pulverizer or
  Smelter was then surrendered into Stage 10, one of each is returned once.
- completion flags are not reset.

Install in C:\domesurvival:
  .\APPLY_JOSEPH_V7_5_PROGRESSION_BALANCE.bat

Then fully restart:
  .\dev\RUN_DEV_FULL.bat

In world:
  /josephscript apply
  /josephscript inspect

Expected:
  errored=false
  valid=true
