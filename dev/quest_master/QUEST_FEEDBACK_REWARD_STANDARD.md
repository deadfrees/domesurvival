# Quest Feedback & Rewards Standard v1

## Обычный квест
- FTB completion toast.
- Короткий звук опыта.
- 7 лёгких FIREWORK particles.
- Небольшая видимая награда.

## Milestone
- Более заметный level-up звук.
- Усиленный FIREWORK + END_ROD burst.

## Завершение главы
- Challenge completion sound.
- Крупный, но короткий particle burst.
- Награда главы.

## Hidden / bridge
- Без звука, particles и видимых наград.

## Производительность
- Только event-driven auto-claim command reward.
- Нет tick handler, polling, world scan или FireworkRocketEntity.

## Глава 0 — награды
- 01. Под Куполом: 2 × `minecraft:bread`; feedback `normal`
- 02. Воздух — часть стены: 2 × `minecraft:apple`; feedback `normal`
- 03. За воротами нет воздуха: 2 × `minecraft:bread`; feedback `normal`
- 04. Солнце тоже враг: 2 × `minecraft:baked_potato`; feedback `normal`
- 05. Погода здесь не фон: 1 × `minecraft:leather`; feedback `normal`
- 06. Верстак — первая машина: 8 × `minecraft:oak_planks`; feedback `normal`
- 07. Камень работает дольше: 4 × `minecraft:coal`; feedback `normal`
- 08. Склад до экспедиции: 2 × `minecraft:bread`; feedback `normal`
- 09. Свет — это время: 4 × `minecraft:charcoal`; feedback `normal`
- 10. Джозеф Куппер: 1 × `minecraft:book`; feedback `normal`
- 11. Найди шлюз: 6 × `minecraft:iron_nugget`; feedback `normal`
- 12. Навес не атмосфера: 8 × `minecraft:cobblestone`; feedback `normal`
- 13. Правило возврата: 2 × `minecraft:cooked_beef`; feedback `milestone`
- 14. Пока это наш дом: 1 × `minecraft:golden_apple`; feedback `chapter`
