DomeSurvival Joseph V7.4.1 — Interaction Fix
================================================

Symptom:
- /josephscript inspect shows:
  externalFileLoaded=true
  linked=[joseph_cooper_gui.js]
  errored=false
  valid=true
  containsHelloDev=false
- but Joseph still says "Hello Dev" instead of opening the quest GUI.

Cause:
The old phrase is not in the linked JavaScript anymore. It is legacy CustomNPCs
Advanced -> Interact Lines data. Also the JS interaction event must explicitly
cancel the normal CustomNPCs fallback interaction path.

Fix:
1. Injects e.setCanceled(true) at the start of Joseph's interact(e).
2. Extends /josephscript apply so it clears old Advanced -> Interact Lines.
3. Keeps all current quest progress, Stage 01 path, rewards and V7.4 stages.
4. Does NOT call resetprogress or resettest.

Install:
  .\APPLY_JOSEPH_V7_4_1_INTERACT_FIX.bat

Then rebuild/run:
  .\dev\RUN_DEV_FULL.bat

In WASTED_TEST:
  /josephscript apply
  /josephscript inspect

Then right-click Joseph.

Do not reset quest progress for this repair.
