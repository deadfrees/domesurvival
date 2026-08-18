DomeSurvival Custom Paintings V3.5 — LOGGER compile fix
========================================================

The uploaded V3.4 and FULL_DEV logs fail on the same single Java error:

    MemoryPaintingItem.java:92
    DomeSurvival.LOGGER.error(...)
    cannot find symbol: variable LOGGER

DomeSurvival.java does not define a LOGGER field, so V3.4 cannot compile.

V3.5 changes only the painting diagnostic:
    DomeSurvival.LOGGER.error(...)
becomes a local System.err diagnostic.

No painting data, quest data, world data, dome, workshop or progression state
is changed.

The installer runs a full:
    gradlew.bat -PdomeFullDev=true clean build --no-daemon

Install:
    .\APPLY_CUSTOM_PAINTINGS_V3_5_LOGGER_COMPILEFIX.bat

On success:
    .\dev\RUN_DEV_FULL.bat

Then:
    /give @s domesurvival:memory_painting
