# Genetic Archive Building Analysis — V1.6

Source analyzed: `r.-3.-2-BLOCKS.mca` from the authored map.

Target point: `-1088 / 92 / -676`.

## Geometry actually present

The selected building is a tall, mostly empty containment tower rather than a normal room grid.

- Main usable lower hall around Y=92: approximately 22 × 21 blocks.
- Lower structural frame: approximately X=-1101..-1075, Z=-687..-663.
- Middle glass/terracotta shell: approximately X=-1098..-1078, Z=-684..-666.
- Upper inner shell: approximately X=-1096..-1080, Z=-682..-668.
- Central clear vertical volume at the quest point runs roughly Y=92..102.
- Solid roof is present around Y=103.
- Existing palette is strongly industrial: end-stone brick, brown/white terracotta, polished andesite, iron bars and white stained glass.

This makes the building a very good fit for a failed pre-catastrophe genetic containment/archive site.

## Added authored zones

1. North preservation wall:
   - 3 intact light-blue cryogenic columns.
   - 1 damaged red/exposed-bar column.

2. West and east quarantine bays:
   - partial iron-bar containment around the existing hostile-spawner zones.
   - intentional breaks leave escape paths so the combat phase still works.

3. South laboratory:
   - two quartz work benches.
   - brewing apparatus, detector panels, pots and dead comparator controls.
   - two cauldrons.

4. Archive storage:
   - two supply barrels.
   - one contains `Протокол 7-Б`, a three-page lore log foreshadowing the Bioincubator.

5. Lighting:
   - four ceiling sea-lantern units attached beneath the existing Y=103 roof.

## Safety / compatibility

- Only AIR positions identified in the actual region file are targeted.
- Existing authored blocks are never overwritten.
- Placement requires the real building signature to match.
- All four local chunks must already be loaded by normal gameplay.
- No forced chunk generation/loading.
- The center around the quest arrival/sample cache remains clear.
- The two previous spawner centers remain unobstructed.
- Decoration is one-shot SavedData; player destruction/editing is respected afterward.
