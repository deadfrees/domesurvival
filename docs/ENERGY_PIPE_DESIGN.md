# DOMESURVIVAL — ENERGY PIPE DESIGN

Актуальная правка пользователя: **«нужно чтобы они были формой как текущие в игре»**. Она заменяет прежнее направление с увеличением диаметра, массивными кольцами и усилителями. Дата обновления: 2026-09-13.

## Текущее оформление Tier 1

`basic_energy_pipe` получил игровые пиксельные материалы, UV и предметную модель. Основа палитры — существующие стальные детали сборки и `VISUAL_STYLE_GUIDE.md`: корпус `#596361`, графит `#303A3B`, углубления `#171E20`, светлая кромка `#8F9990`, янтарная маркировка энергии `#C5A252`. Износ ограничен отдельными пикселями. Одна янтарная полоса на накладках обозначает Tier I; она не показывает наличие или направление потока FE.

Шесть непрозрачных PNG 16×16 разделены по назначению: body, core, panel, rail, end, edge. UV используют **2 texels на Minecraft unit** (32 на блок); узкие поверхности получают соответствующую часть текстуры без растягивания целого изображения. Продольные кромки следуют направлению отвода: у шести multipart arms `uvlock=false`, селекторы и углы поворота прежние. Emissive, PBR-карты, прозрачность и анимация не используются.

Предмет теперь содержит core и два противоположных arm: 17 кубоидов, 88 quads. Это прямая секция существующей трубы. Заданы GUI, ground, fixed и обе руки first/third person. Число граней **мировой** модели не изменилось; только предмет раньше показывал один core (36 quads).

Tier 2/3 сохраняют текущие игровые материалы и masters формы. По этапу 7 исходного ТЗ перенос нового оформления на них следует после подтверждения результата Tier 1.

- [Текстурированный master Tier 1](../source_assets/blender/energy_pipes/energy_pipe_tier_1.blend).
- [Сцена оформления](../source_assets/blender/energy_pipes/05_energy_pipe_tier1_styled.blend) и [рендер Blender](../source_assets/blender/energy_pipes/previews/05_energy_pipe_tier1_styled.png).
- [Blender Python — материалы и экспорт](../source_assets/blender/scripts/style_energy_pipe_tier1.py).
- [Blockbench — редактируемая прямая секция](../source_assets/blockbench/energy_pipes/energy_pipe_tier_1.bbmodel), с вложенными PNG, явными UV и группами частей. Проверен формат данных; проверка через UI Blockbench не проводилась.
- [Проверки и игровые снимки Tier 1](ENERGY_PIPE_TIER1_VALIDATION.md).

## Форма

Blender masters теперь строятся непосредственно из действующих `models/block/<id>_core.json` и `<id>_arm.json` в `src/main/resources/assets/domesurvival/`. Геометрия не реконструируется по памяти или картинке: скрипт импортирует координаты каждого элемента и список его граней.

| Параметр | Tier 1 — basic | Tier 2 — reinforced | Tier 3 — high_voltage |
|---|---:|---:|---:|
| Основное сечение отвода | 3×3 units | 3×3 units | 3×3 units |
| Размер отвода с четырьмя рейками | 3.4×3.4 units | 3.4×3.4 units | 3.4×3.4 units |
| Корпус центрального узла | 3.5×3.5×3.5 units | 3.5×3.5×3.5 units | 3.5×3.5×3.5 units |
| Узел с шестью накладками | 3.96 units | 3.96 units | 3.96 units |
| Детали core / arm | 7 / 5 | 7 / 5 | 7 / 5 |

Сохраняются квадратное тонкое сечение, четыре тонкие продольные рейки и компактный кубический центр с накладками. Новые кольца, плечи и муфты из предыдущего концепта убраны. Старшие Tier не утолщаются. Геометрия Tier в текущих игровых JSON одинаковая, поэтому в одном сером материале три версии выглядят одинаково — это ожидаемое соответствие источнику.

## Соединения и центральный узел

Сборка повторяет текущий multipart: **core всегда присутствует**, каждый arm включается своим флагом NORTH/SOUTH/EAST/WEST/UP/DOWN. Отключённая сторона не добавляет отдельный cap: остаётся существующая накладка core. Поле `cap: []` в числовом описании обозначает отсутствие такой дополнительной детали.

В Blender +Y соответствует Minecraft NORTH, +Z — UP. 16 units = один блок, origin Blender — центр блока. Преобразование из Blender в Minecraft: `(x+8, z+8, 8-y)`. Базовый arm из JSON направлен на NORTH; после преобразования доходит до Blender Y=8. Соседние блоки стыкуются на тех же плоскостях, что и в игре.

Правила подключения, режимы сторон, коллизии, регистрация, NBT, Forge Energy и совместимость с машинами остаются без изменений. На листе соединений показана абстрактная опорная пластина у торца трубы; это reference внутри Blender, не переработка ассета машины и не новая муфта.

## Исходники

- [Генератор Blender Python API](../source_assets/blender/scripts/create_energy_pipe_greyboxes.py).
- [Tier 1 master](../source_assets/blender/energy_pipes/energy_pipe_tier_1.blend).
- [Tier 2 master](../source_assets/blender/energy_pipes/energy_pipe_tier_2.blend).
- [Tier 3 master](../source_assets/blender/energy_pipes/energy_pipe_tier_3.blend).
- [Сцена сравнения](../source_assets/blender/energy_pipes/01_energy_pipe_lineup.blend).
- [Кубоиды и ссылки на исходные игровые модели](../source_assets/blender/energy_pipes/greybox_cuboids.json).

В каждом master есть видимая прямая сборка, скрытая коллекция `SOURCE_MODULES_current_game_core_arm` с исходными частями и wire reference куб 16×16×16. Названия деталей берутся из игровых JSON. Масштаб и повороты объектов применены к вершинам; smooth shading и subdivision не используются. Отдельные части доступны для дальнейшей работы через `bpy`, ручного моделирования не требуется.

```powershell
& 'C:\Users\deadfrees\AppData\Local\Microsoft\WindowsApps\blender-launcher.exe' --background --factory-startup --python-exit-code 1 --python 'C:\domesurvival\source_assets\blender\scripts\create_energy_pipe_greyboxes.py'
```

Команда выше воспроизводит исторический этап формы и перезаписывает masters серым материалом. Для текущего оформления после неё запускается `style_energy_pipe_tier1.py` тем же способом. Его результат проверяется в `dev/energy_pipes/tier1_style_result.json`; один exit code launcher не считается подтверждением выполнения Blender. Исторический отчёт `greybox_results.json` хранит хеши до текстурирования; актуальные координаты проверяет `verify_energy_pipe_game_shape.py`.

## Листы просмотра

- [Линейка, один масштаб и серый материал](../source_assets/blender/energy_pipes/previews/01_energy_pipe_lineup.png).
- [Front / side / top / ракурс секции](../source_assets/blender/energy_pipes/previews/02_energy_pipe_views.png).
- [Все соединения и узлы](../source_assets/blender/energy_pipes/previews/03_energy_pipe_connections_review.jpg), [полный PNG](../source_assets/blender/energy_pipes/previews/03_energy_pipe_connections.png).
- [Линии по 10 блоков](../source_assets/blender/energy_pipes/previews/04_energy_pipe_long_lines.png).

Листы 01–04 — исторические рендеры Blender **без игровых текстур**, для проверки формы. Новое оформление Tier 1 показано на листе 05 и игровых снимках в отчёте проверки. Tier 2/3 пока используют прежние предметные модели core.

## Runtime и бюджет

Сохраняется Minecraft JSON + multipart, baked rendering. Blender — исходник для работы над визуалом; OBJ/GLTF, BlockEntityRenderer, прозрачность и динамическая геометрия не добавляются.

| Соединений | Кубоидов | Quads для каждого Tier |
|---:|---:|---:|
| 0 | 7 | 36 |
| 1 | 12 | 62 |
| 2 | 17 | 88 |
| 3 | 22 | 114 |
| 4 | 27 | 140 |
| 5 | 32 | 166 |
| 6 | 37 | 192 |

Бюджет совпадает с текущими моделями. 100 прямых секций — 8 800 исходных quads. Это число граней до отсечения, а не измеренный FPS или количество draw calls.

## Проверка

Проверяются 192 комбинации сторон, bounds, направление концов, копланарные грани и 54 сочетания Tier/направления по плоскости стыка. Сохранённые `.blend` дополнительно сверяются с JSON по координатам вершин и нормалям граней. Хеши файлов `src/main` сравниваются до и после генерации.

[Проверка сохранённых masters](../source_assets/blender/scripts/verify_energy_pipe_game_shape.py) открывает каждый `.blend` через `bpy` и независимо сопоставляет его с игровым JSON. [Отчёт](../dev/energy_pipes/game_shape_verification.json): PASS, три master-файла, 36 исходных элементов; координаты вершин и наборы направлений граней совпадают.

В текущем этапе изменены пять игровых JSON Tier 1 и добавлены шесть его PNG. Первая сверка после текстурирования подтвердила неизменность остальных 2 367 файлов `src/main`. Позже рабочее дерево изменилось за пределами труб, в том числе появились отдельные JEI-классы. Эти изменения не входят в оформление труб и не откатываются. Финальная проверка отдельно показывает PASS ассетов труб и `DRIFT_OUTSIDE_PIPE_SCOPE` всего рабочего дерева, со списком расхождений относительно начального снимка: [tier1_static_checks.json](../dev/energy_pipes/tier1_static_checks.json). Исходные пять JSON сохранены в `source_assets/baseline/energy_pipes/tier1` для точного сравнения в Minecraft. Данные runtime-проверок и ограничения замеров вынесены в отдельный [отчёт](ENERGY_PIPE_TIER1_VALIDATION.md).
