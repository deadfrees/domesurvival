# DOMESURVIVAL — ENERGY PIPE DESIGN

Актуальная правка пользователя: **«нужно чтобы они были формой как текущие в игре»**. Она заменяет прежнее направление с увеличением диаметра, массивными кольцами и усилителями. Дата обновления: 2026-09-13.

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

Скрипт записывает только исходники, preview и evidence энергетических труб. Результат проверяется в `dev/energy_pipes/greybox_results.json`; один exit code launcher не считается подтверждением выполнения Blender. В отчёте сохранены SHA-256 генератора и шести исходных игровых JSON.

## Листы просмотра

- [Линейка, один масштаб и серый материал](../source_assets/blender/energy_pipes/previews/01_energy_pipe_lineup.png).
- [Front / side / top / ракурс секции](../source_assets/blender/energy_pipes/previews/02_energy_pipe_views.png).
- [Все соединения и узлы](../source_assets/blender/energy_pipes/previews/03_energy_pipe_connections_review.jpg), [полный PNG](../source_assets/blender/energy_pipes/previews/03_energy_pipe_connections.png).
- [Линии по 10 блоков](../source_assets/blender/energy_pipes/previews/04_energy_pipe_long_lines.png).

Это рендеры Blender **без игровых текстур**, для проверки формы. Ракурс секции не является снимком Minecraft inventory: текущий игровой item наследует только core, что отдельно отмечено в аудите. Материалы, пиксельные текстуры и новые display transforms в этой правке не реализованы.

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

Производственные игровые ресурсы на этом этапе не заменены. Игровые тесты, FE regression, FPS и build не заявляются как выполненные. Последующая работа с текстурами и Tier 1 должна сохранять утверждённую пользователем текущую форму; прежняя цель различать Tier новой геометрией больше не применяется.
