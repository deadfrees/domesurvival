# Проверка визуального эталона

Результат: **PASS**. Ошибок: 0; предупреждений: 0.

Источник: неизменяемый снимок текущего рабочего дерева до прототипа (2375 файлов src), а не Git HEAD.
SHA256 baseline: `f7b50db7484bbf021092ff09f9cf3f07b212a8081633a3e76dbce2a78e49e663`.

Совпадают: 2371; изменены: 4; добавлены: 0; удалены: 0.

## Разрешённые изменения

- `src/main/resources/assets/domesurvival/models/block/filter_regeneration_station.json`
- `src/main/resources/assets/domesurvival/models/block/filter_regeneration_station_active.json`
- `src/main/resources/assets/domesurvival/models/item/filter_regeneration_station.json`
- `src/main/java/com/wasted/domesurvival/forge/machine/filter/FilterRegenerationScreen.java`

## Выполненные проверки

- PASS — Pinned baseline and all src file hashes
- PASS — Local vanilla resource archive
- PASS — Inactive, active and item JSON inheritance contract
- PASS — Geometry / UV / references / display: domesurvival:block/filter_regeneration_station
- PASS — Geometry / UV / references / display: domesurvival:block/filter_regeneration_station_active
- PASS — Geometry / UV / references / display: domesurvival:item/filter_regeneration_station
- PASS — Unchanged facing/active blockstate mapping

## Геометрия до runtime culling

| Model | Cuboids | Quads | Triangles | Duplicate faces | Shared internal faces |
|---|---:|---:|---:|---:|---:|
| domesurvival:block/filter_regeneration_station | 69 | 218 | 436 | 0 | 0 |
| domesurvival:block/filter_regeneration_station_active | 69 | 218 | 436 | 0 | 0 |
| domesurvival:item/filter_regeneration_station | 69 | 218 | 436 | 0 | 0 |

## Замечания

Статических нарушений заявленного контракта не обнаружено.

## Ограничения

- Hash comparison covers all existing/additional/removed src files against the pinned pre-existing dirty-tree baseline. Files outside src, runtime resource packs, mods, build settings and already existing gameplay defects are not certified.
- FilterRegenerationScreen.java is the sole allowed Java change. Hash allowlisting cannot prove that an arbitrary edit to that screen is purely visual; compilation and human review of its diff remain necessary.
- Seven display contexts are checked after JSON inheritance. Head is an optional eighth context. This verifies presence and finite/nonzero transforms, not actual in-game size or visual quality.
- Quads and triangles are declared effective faces before runtime culling (triangles = quads * 2), not GPU draw calls or measured performance.
- Duplicate-face check finds identical four-vertex polygons, including supported element rotations. It does not detect partial coplanar overlaps, cuboid intersections or near-coplanar depth fighting; opposite internal coincident faces are reported as warnings.
- PNG signatures and resource references are checked; this does not decode texture pixels, assess atlas UV seams, texture resolution quality or effective resource-pack overrides.
- Vanilla resources are read from the local 1.20.1 client jar. Missing archives or unresolved external resources fail rather than being silently assumed valid.
- No Minecraft launch, screenshots, multiplayer test, chunk-reload test, Embeddium test, interaction test or performance benchmark is performed by this script.

Полный отчёт с эффективными display transforms, parent chains и хешами ссылочных ресурсов: `prototype_verification.json`.
