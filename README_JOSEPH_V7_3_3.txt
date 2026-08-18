DomeSurvival Joseph Questline V7.3.3
====================================

Changes:
1. All cross-mod quest and reward labels are explicitly Russian.
2. Farmer's Delight:
   Разделочная доска, Кухонный котёл, Сковорода, Органический компост,
   Плодородная почва, Томат, Капуста, Лук, Рис, etc.
3. Brewin' And Chewin':
   Вяленое мясо, Бочонок, Пиво, Медовуха, Кружка.
4. Mekanism:
   Базовая/Продвинутая схема управления, Наполненный/Укреплённый сплав,
   Энергетический планшет, Конфигуратор, Стальной слиток.
5. Immersive Engineering:
   Железный/Стальной механический компонент, Медный провод,
   Электронная лампа, Печатная плата, Стальная пластина, Молот инженера.
6. The Stage 01 physical upgrade now converts the COMPLETE connected road:
   dirt + coarse dirt + rooted dirt -> minecraft:dirt_path.
   Existing dirt_path blocks are traversal connectors.
   Radius 64, maximum 4096 visited road blocks.
7. Stage 01 gets a fresh path-upgrade key, so an already completed V7.3.2
   world will run the expanded road conversion once more on Joseph interaction.
8. Removed from rewards:
   - Water Purifier
   - Oxygen Electrolyzer
   - Oxygen Filler
9. Replacement Stage 04 reward:
   2 Universal Reservoirs
   12 Reinforced Fluid Pipes
   8 Rich Soil
   2 Oak Cabinets
   4 Straw Bales
10. Replacement Stage 05 reward:
   2 Oxygen Masks
   2 Medium Oxygen Tanks
   12 Reinforced Oxygen Pipes
   2 Advanced Control Circuits
   1 Energy Tablet
   4 Steel Mechanical Components

Install:
  .\APPLY_JOSEPH_QUESTLINE_V7_3_3_AUTO.bat

Then:
  .\dev\RUN_DEV_FULL.bat

In WASTED_TEST:
  /josephscript apply

If Stage 01 is already complete, simply right-click Joseph once after the
restart: the new V7.3.3 path key makes the full road conversion run again.
