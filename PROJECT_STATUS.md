# DomeSurvival project status — V1.1

Target: Minecraft 1.20.1 / Forge 47.4.10 / Java 17.

WASTED dome center: X=-506, Y=62, Z=-641.
Surface radius: 50. Underground safe radius: 45 down to Y=-64.

Implemented:
- reinforced_glass and dome_frame blocks;
- /dome preview;
- /dome generate (fresh world: builds current V1.1 structure);
- /dome upgrade (migrates existing V1 prototype to V1.1);
- /dome status with persistent structure version;
- /dome check to report SURFACE_DOME / SURFACE_SKIRT / UNDERGROUND_SAFE / AIRLOCK / OUTSIDE;
- V1.1 foundation ring, four meridian ribs, middle frame ring and south temporary airlock passage;
- generation/update queue at 750 block operations per server tick;
- Java-only geometry/planner tests with Java 17 bytecode target.

Not implemented yet:
- functional airlock doors/interlock;
- oxygen storage/production/consumption;
- solar/rain/environmental damage;
- starter mine entrance;
- ore retrogen.
