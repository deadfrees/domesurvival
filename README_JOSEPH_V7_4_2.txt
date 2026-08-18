DomeSurvival Joseph V7.4.2 — STAGE1_PATH_UPGRADE_KEY fix
================================================================

Observed runtime error:
  ReferenceError: "STAGE1_PATH_UPGRADE_KEY" is not defined

The Joseph external script is loaded, but Nashorn stops evaluating it because
the Stage 1 road-upgrade key is referenced without its var declaration.

This patch:
- restores:
    var STAGE1_PATH_UPGRADE_KEY =
        "domesurvival.stage01.path_upgraded.v733";
- keeps Nashorn/ES5 syntax;
- updates the source script;
- refreshes CustomNPCs external-script copies;
- DOES NOT reset quest progress;
- DOES NOT rebuild/delete the workshop or dome.

Install:
  .\APPLY_JOSEPH_V7_4_2_STAGE1_KEY_FIX.bat

Then fully restart the dev client:
  .\dev\RUN_DEV_FULL.bat

In the world:
  /josephscript apply
  /josephscript inspect

Expected:
  errored=false
  valid=true
