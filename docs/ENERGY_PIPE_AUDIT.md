# DOMESURVIVAL — ENERGY PIPE AUDIT

Дата: 2026-09-12. Minecraft 1.20.1 / Forge. Ветка: `visual/energy-pipes`.

Статус: **статический аудит завершён; Blender Python pipeline PASS**. Проверены актуальные Java и ресурсы из `src/main`, а не архивы или результаты предыдущей переработки. Производственные файлы в рамках этого этапа не изменены. Проверка в Minecraft и измерение FPS на этом этапе не выполнялись.

## Проверка Blender

- `blender` не найден в PATH; стандартная MSI-папка также отсутствует. После уточнения пользователя проверен Microsoft Store package через `Get-AppxPackage *Blender*`.
- Установлен пакет `BlenderFoundation.Blender_5.2.1.0_x64__ppwjx1n5r4v9t`.
- Реальный EXE: `C:\Program Files\WindowsApps\BlenderFoundation.Blender_5.2.1.0_x64__ppwjx1n5r4v9t\Blender\blender.exe`.
- Прямой запуск EXE возвращает Windows `Access is denied`, в том числе вне sandbox. Права WindowsApps не менялись. Manifest пакета объявляет штатный alias `blender-launcher.exe`; запуск через него прошёл успешно.
- Рабочий CLI: `C:\Users\deadfrees\AppData\Local\Microsoft\WindowsApps\blender-launcher.exe`.
- Выполнен [test_energy_pipe_pipeline.py](../source_assets/blender/scripts/test_energy_pipe_pipeline.py), использующий `bpy`. Версия из **работающего** `bpy.app.version_string`: **5.2.1 LTS**. Launcher завершился с кодом 0; дополнительно проверен созданный отчёт `status: PASS`, поскольку одного кода launcher недостаточно для проверки дочернего Blender.
- Скрипт создал пустую сцену, эталонный куб 16×16×16, восьмигранный тестовый сегмент 4×4×16 (16 вершин, 10 полигонов), применил transforms, проверил наружные нормали, сохранил `.blend` и экспортировал промежуточный OBJ.
- Результаты: [тестовый .blend](../source_assets/blender/energy_pipes/pipeline_test/test_energy_pipe_pipeline.blend), [OBJ](../source_assets/blender/energy_pipes/pipeline_test/test_energy_pipe_pipeline.obj), [pipeline_result.json](../source_assets/blender/energy_pipes/pipeline_test/pipeline_result.json). Они находятся вне production resources и не заменяют игровые модели.

Проверенная команда:

```powershell
& 'C:\Users\deadfrees\AppData\Local\Microsoft\WindowsApps\blender-launcher.exe' --background --factory-startup --python-exit-code 1 --python 'C:\domesurvival\source_assets\blender\scripts\test_energy_pipe_pipeline.py'
```

OBJ используется исключительно для проверки экспорта. Он не предлагается как игровой формат. Основная автоматизация моделирования — Blender Python API.

## A. Три реальные энерготрубы

| Tier | Registry ID блока и предмета | Tier enum | Лимит |
|---|---|---|---:|
| 1 | `domesurvival:basic_energy_pipe` | `BASIC` | 256 FE/t |
| 2 | `domesurvival:reinforced_energy_pipe` | `REINFORCED` | 1 024 FE/t |
| 3 | `domesurvival:high_voltage_energy_pipe` | `HIGH_VOLTAGE` | 4 096 FE/t |

Регистрация: [ModBlocks.java](../src/main/java/com/wasted/domesurvival/forge/block/ModBlocks.java), строки 137–157 и 227–235. Лимиты: [EnergyPipeTier.java](../src/main/java/com/wasted/domesurvival/forge/transport/energy/EnergyPipeTier.java).

Общий Block class: `EnergyPipeBlock`, наследник `BaseEntityBlock`, реализует `IWrenchable`. Общий BlockEntity: `EnergyPipeBlockEntity`; зарегистрирован единый тип `domesurvival:energy_pipe` для всех трёх блоков в [ModBlockEntities.java](../src/main/java/com/wasted/domesurvival/forge/registry/ModBlockEntities.java), строки 162–172. Предмет: `EnergyPipeBlockItem`, обычный `BlockItem` с подсказкой скорости передачи и настройки ключом.

### FE, capacity, NBT и networking

- [EnergyPipeNetwork.java](../src/main/java/com/wasted/domesurvival/forge/transport/energy/EnergyPipeNetwork.java) обходит связный компонент труб по шести направлениям. Смешанные Tier разрешены.
- Лимит **всего связного компонента на тик** равен минимальному лимиту входящей в него трубы; не сумме скоростей и не отдельному бюджету каждого отвода. Проход через сервисный блок также может ограничить компонент.
- Собственного хранилища FE/capacity у `EnergyPipeBlockEntity` нет; он не предоставляет собственный `IEnergyStorage`. Сеть напрямую извлекает и вставляет энергию в capability соседних машин.
- При выборе источников/приёмников учитываются `canExtract`, `canReceive` и режим стороны; перед реальным переносом используются simulation-вызовы. Одна и та же машина не используется одновременно как источник для самой себя. Начальный приёмник меняется по игровому времени.
- NBT: compound `SideModes`, ключи `north/east/south/west/up/down`, значения byte ordinal enum `AUTO/INPUT/OUTPUT/DISABLED`. По умолчанию все стороны `AUTO`. Сохранять формат и порядок enum.
- Смена режима обновляет blockstate и отправляет обновление блока клиентам; отдельный новый сетевой протокол для визуала не нужен. У энерготруб нет специального пакета анимации или клиентского тика.
- FE logic, capacity, рецепты, progression, NBT, networking и placement в этом этапе не менялись и не предлагаются к изменению.

## B. Connection logic

Источник: [EnergyPipeBlock.java](../src/main/java/com/wasted/domesurvival/forge/transport/energy/EnergyPipeBlock.java), методы `canConnect`, `refreshConnections`, `getStateForPlacement`, `updateShape`, `neighborChanged`; [EnergyPipeBlockEntity.java](../src/main/java/com/wasted/domesurvival/forge/transport/energy/EnergyPipeBlockEntity.java), `setSideMode`.

1. Если собственная сторона `DISABLED`, соединение выключено.
2. Если сосед — любой `EnergyPipeBlock`, соединение включается при разрешённой противоположной стороне соседа. Tier не ограничивает совместимость. При временно отсутствующем BE соседа blockstate допускает соединение.
3. Если сосед — другой блок, требуется BlockEntity с `ForgeCapabilities.ENERGY` на стороне `direction.getOpposite()`.
4. Иначе соединения нет.

Наличие capability определяет **видимую ветвь**. `canReceive/canExtract` проверяются при переносе FE, поэтому видимая ветвь сама по себе не доказывает поток энергии. `INPUT` означает поступление энергии **в сеть из машины**, `OUTPUT` — **из сети в машину**. Эти режимы не превращают соединения между трубами в направленные рёбра; между трубами важен `DISABLED`.

При обычном размещении все шесть флагов вычисляются из окружения. Изменение соседей обновляет нужный флаг/все флаги. Ключ переключает режимы стороны; Shift+ключ возле трубы той же семьи использует [PipeWrenchConnectionService.java](../src/main/java/com/wasted/domesurvival/forge/pipe/PipeWrenchConnectionService.java) для отключения/восстановления стороны. Модель должна следовать готовому blockstate.

Сервисные проходы уже поддержаны: `ServicePassThroughTraversal.resolve(..., ENERGY)` используется при поиске труб и машин; `ServicePassThroughBlockEntity` предоставляет FE bridge на совместимой оси при установленном энергетическом проводнике. Сохранять этот путь и не модифицировать сервисные блоки.

## C. Текущие модели, текстуры и renderer

Все пути ниже относительно `src/main/resources/assets/domesurvival/`. Для каждой строки `<id>` — имя без namespace.

| Registry ID | Current model | Current texture | Connection system | Current renderer | Current performance approach |
|---|---|---|---|---|---|
| `domesurvival:basic_energy_pipe` | `models/block/basic_energy_pipe_core.json` + `basic_energy_pipe_arm.json`; item → core | `textures/block/basic_energy_pipe.png` (64²), `_core.png` (64²), `_detail.png` (16²) | 6 boolean, `blockstates/basic_energy_pipe.json`, multipart | `RenderShape.MODEL`, baked JSON; без BER | Геометрия секции чанка, 36 + 26d quads |
| `domesurvival:reinforced_energy_pipe` | `models/block/reinforced_energy_pipe_core.json` + `reinforced_energy_pipe_arm.json`; item → core | `textures/block/reinforced_energy_pipe.png` (64²), `_core.png` (64²), `_detail.png` (16²) | 6 boolean, `blockstates/reinforced_energy_pipe.json`, multipart | `RenderShape.MODEL`, baked JSON; без BER | Геометрия секции чанка, 36 + 26d quads |
| `domesurvival:high_voltage_energy_pipe` | `models/block/high_voltage_energy_pipe_core.json` + `high_voltage_energy_pipe_arm.json`; item → core | `textures/block/high_voltage_energy_pipe.png` (64²), `_core.png` (64²), `_detail.png` (16²) | 6 boolean, `blockstates/high_voltage_energy_pipe.json`, multipart | `RenderShape.MODEL`, baked JSON; без BER | Геометрия секции чанка, 36 + 26d quads |

`d` — число подключённых сторон. Все модели включают ambient occlusion. В активных моделях нет нестандартного loader, `render_type`, OBJ, GLTF или GeckoLib. В Java нет регистрации BER или отдельного render layer для энерготруб. Собственный BlockEntity необходим серверной логике, но не рисует трубу каждый кадр.

Для каждого Tier также лежат `models/block/<id>_inventory.json` (3 кубоида, 18 граней) и `textures/block/<id>_arm.png` (16²). **В текущей цепочке blockstate/item/model эти файлы не используются**. Название `_arm.png` не означает, что активный arm JSON использует его. Не удалять их в аудите.

Активный `models/item/<id>.json` наследует `<id>_core`, а не `<id>_inventory`. Семь требуемых display contexts уже заданы для всех Tier и полностью одинаковы:

| Context | Rotation | Translation | Uniform scale |
|---|---|---|---:|
| GUI, JEI, hotbar | 30, 225, 0 | 0, 0, 0 | 2.35 |
| First person right | 0, 45, 0 | 1.13, 3.2, 1.13 | 1.2 |
| First person left | 0, 225, 0 | −1.13, 3.2, 1.13 | 1.2 |
| Third person right | 75, 45, 0 | 0, 2.5, 0 | 0.95 |
| Third person left | 75, 225, 0 | 0, 2.5, 0 | 0.95 |
| Ground | 0, 0, 0 | 0, 2, 0 | 0.75 |
| Fixed / item frame | 0, 180, 0 | 0, 0, 0 | 1.8 |

Наличие transforms подтверждено статически; фактический размер в руке, JEI и рамке требует игровой проверки после изменения item geometry.

## D. NORTH / SOUTH / EAST / WEST / UP / DOWN

У всех Tier ровно шесть `BooleanProperty`; других свойств, включая `facing`, Tier, machine type или flow, нет. Значения по умолчанию — `false`. Tier хранится в экземпляре блока. Всего 64 сочетания на Tier.

Core присутствует всегда. Базовый arm направлен к NORTH, вдоль отрицательного Z. Ось модели проходит через X=8, Y=8. Blockstate вращает одну и ту же деталь:

| Свойство | Ось Minecraft | Поворот multipart | Граница соединения |
|---|---|---|---|
| `north` | −Z | x=0, y=0 | z=0 |
| `south` | +Z | x=0, y=180 | z=16 |
| `east` | +X | x=0, y=90 | x=16 |
| `west` | −X | x=0, y=270 | x=0 |
| `up` | +Y | x=270, y=0 | y=16 |
| `down` | −Y | x=90, y=0 | y=0 |

У всех шести правил `uvlock: true`. Сохранить существующие углы Minecraft blockstate; не переносить углы Blender напрямую без преобразования осей. Ноль подключений даёт только core, одно — тупиковый отвод, два противоположных — прямую, два соседних — угол, три — T или пространственный узел, четыре — cross или пространственный узел, пять/шесть — разветвлённый узел. Не нужно 64 отдельных mesh-файла.

## Подтверждённые визуальные проблемы

1. **Все три Tier имеют совершенно одинаковую геометрию** core, arm и неиспользуемого inventory. Сравнение JSON elements даёт одинаковые SHA-256 для соответствующих частей. Различается материал: базовый янтарный, усиленный голубой, высоковольтный фиолетовый (осмотр PNG). Серые модели неотличимы.
2. Core — куб 3.5 units с шестью накладками; общий размер 3.96 units. Arm — квадратный стержень 3 units с четырьмя тонкими рейками; наружный размер 3.4 units. Рост мощности не выражен силуэтом, и размеры меньше ориентиров ТЗ.
3. Предмет рисует только небольшой junction. Масштаб GUI увеличен до 2.35, но это не создаёт узнаваемый силуэт секции трубы. Подключение существующего inventory само по себе тоже не решит различимость Tier: его геометрия одинакова.
4. Все 36 граней core и 26 граней arm используют целый UV-квадрат `[0,0,16,16]`, включая тонкие боковины. Например, 64 пикселя body растянуты на сторону arm 3×6.25 units. Pixel density различается даже по осям одной грани; 16² detail растягивается на рейки толщиной 0.2 units. Требуется новая UV-развёртка, а не увеличение разрешения.
5. Core panels остаются на всех шести сторонах при любом подключении; arm входит в их область. Часть граней скрыта внутри пересекающихся деталей, но остаётся в модели. У этих деталей нет `cullface`. Это подтверждённая лишняя геометрия; **видимый z-fighting в игре не подтверждён**.
6. У arm нет отдельного кольца/муфты у границы блока. Свободная сторона показывает накладку core; специального terminal/end cap в активной системе нет. Геометрические разрывы на границе соседних блоков статически не обнаружены: arm доходит до 0, после поворота — до соответствующей границы 16.
7. Коллизия/контур выделения у всех Tier шириной 6 units (`CORE` от 5 до 11 и шесть соответствующих arm shapes), заметно шире текущего изображения. Менять `VoxelShape` ради модели запрещено текущим scope; проектировать новый силуэт с учётом этого ограничения.

Проблемы 1–7 относятся ко всем трём Tier. Missing model/texture references и некорректные bounds в проверенных pipe JSON не найдены. Это не заменяет проверку model bake в клиенте.

## E–G. Рекомендуемый runtime и разделение работы

Сохранить **Minecraft JSON + multipart + baked rendering**. Архитектура уже покрывает все соединения; серверную часть не менять. Новые registry IDs, BlockState properties, capability, networking, BER и клиентские тики для этого не нужны.

**Blender:** разработка серого силуэта, сечения, central housing/junction, conductor, reinforcement, connector rings, универсальной муфты, торцевой заглушки; master `.blend` каждого Tier; сцены прямой, угла, T, cross, шести направлений и сравнения в одном масштабе. Имена объектов должны отражать назначение. `bpy` создаёт и проверяет геометрию воспроизводимо, с flat shading и применёнными transforms.

**Minecraft JSON / Blockbench:** runtime core/arm/необязательный cap, условный выбор частей по существующим boolean, UV, texture references, item assembly и display transforms. Master следует строить из деталей, представимых JSON elements. Произвольный восьмиугольный Blender mesh нельзя автоматически считать обычным Minecraft JSON: сначала проверить разложение на ограниченный набор кубоидов/допустимых поворотов. Для первого варианта предпочтительно ступенчатое сечение с компактными угловыми деталями. OBJ/GLTF в runtime не оправданы текущей задачей.

## H–I. Предлагаемые размеры и форма (ещё не утверждены)

16 units = 1 Minecraft block. Числа — стартовые ограничения для greybox, не результаты моделирования.

| Tier | Наружный размер прямой секции | Junction / максимальная муфта | Форма |
|---|---:|---:|---|
| 1 | 4.5 units | 5 units | Лёгкий проводник в тонкой оболочке; ступенчатые/срезанные углы; один компактный пояс кольца; минимум усилителей |
| 2 | 5.5 units | 6 units | Более толстая оболочка; две противоположные силовые секции; двойные кольца и усиленные плечи узла |
| 3 | 6 units | 6 units | Тяжёлый компактный короб; четыре силовые секции; более длинный усиленный пояс муфты и плотный распределительный узел |

Tier 3 выбран по нижней границе допустимых 6–7 units, чтобы сохранить внешний размер в текущем 6-unit контуре. Рост до 6.5–7 вынес бы видимые детали за существующий hitbox; это не повод менять механику. Tier 1 остаётся тоньше hitbox, как и текущая модель; проверить удобство выбора стороны ключом в игре.

Общие материалы: тёмный металл, сталь, небольшие янтарные маркировки; одинаковая плотность пикселей. Болты и маркировки — текстурой. Различимость в сером обеспечивается числом и формой усилителей, шириной секции и конструкцией кольца. Начать с 16²/32² текстур и UV по размеру граней; 64² только при доказанной необходимости.

## J. План junction

- Закрытый центральный корпус с шестью точно совпадающими посадочными площадками. У каждого Tier собственная форма корпуса.
- Arm начинается на посадочной плоскости и заканчивается строго на границе блока. Углы, T и cross собираются вокруг корпуса, без открытых дыр и копланарных декоративных поверхностей.
- Если нужны заглушки, применять ориентируемый cap при `<side>=false`; при `true` заменять его отводом. Использовать те же шесть boolean, без новой логики соединений.
- Размер центрального корпуса близок к диаметру линии: ряд из десяти блоков не должен выглядеть рядом отдельных машин.
- Проверить все 64 сочетания и все пары разных Tier, включая переходы Tier 1↔2↔3. Общая плоскость окончания и согласованный профиль торца должны исключать просветы между разными диаметрами.

## K. План machine coupling

Один универсальный торцевой пояс на arm у границы блока, с небольшим утолщением/защёлкой. Такой же пояс участвует в pipe-to-pipe соединении. В имеющемся blockstate невозможно выбрать эксклюзивный вариант «machine coupling» по типу соседа: `true` означает и трубу, и машину. Нельзя добавлять распознавание машин ради визуала.

Муфта должна закончиться внутри своего блока, без захода в модель машины. Проверить её рядом с существующими FE-портами и сервисным проходом, в том числе в шести направлениях. Порты машин могут иметь визуальные размеры и смещения, не отражённые FE capability; универсальное идеальное совпадение со всеми портами до осмотра не обещается. Машины не переделывать.

## L. Производительность

Текущий статический бюджет одинаков для всех Tier:

| Подключений | Кубоидов | Quads | Условных треугольников (2 на quad) |
|---:|---:|---:|---:|
| 0 | 7 | 36 | 72 |
| 1 | 12 | 62 | 124 |
| 2 / прямая | 17 | 88 | 176 |
| 3 | 22 | 114 | 228 |
| 4 | 27 | 140 | 280 |
| 5 | 32 | 166 | 332 |
| 6 | 37 | 192 | 384 |

100 двухсторонних труб — 8 800 исходных quads; 100 шестисторонних — 19 200. Это подсчёт граней JSON до видимости/отсечения, **не** draw calls и не измеренный FPS. Baked geometry включается в геометрию чанка; отдельного вызова рендера на каждую трубу не требуется.

Цель нового визуала: не превысить текущие 88 quads для прямой и 192 для шести соединений на старте; включать в бюджет все cap/ring детали. Это проектный бюджет, а не уже достигнутая оптимизация. Удалять заведомо скрытые грани вручную/при экспорте, не ставить `cullface` на внутренние торцы без понимания отсечения соседями. Избегать прозрачности, per-pipe анимации, эмиссивных шейдеров и динамического rebuild каждый тик.

У существующей сети есть серверный тик каждого BE, но компонент обрабатывается один раз за игровой тик: `processed` предотвращает повторный обход. Состав компонента собирается заново на следующем тике. Этот CPU cost сохраняется; визуальный этап его не оптимизирует. Обещать прирост FPS без сравнительного замера нельзя. Ожидание при сохранении бюджета и render path — сопоставимая графическая нагрузка, без новых постоянных клиентских вычислений.

## Проверки и следующий этап

Выполнен [audit_energy_pipes.mjs](../dev/energy_pipes/audit_energy_pipes.mjs). [Машиночитаемый результат](../dev/energy_pipes/audit_results.json): 15 JSON-файлов, 12 PNG headers, 192 комбинации multipart; все требуемые направления/display contexts, положительные bounds внутри блока, существование используемых texture/model references; ошибок нет. SHA-256 JSON elements подтверждает идентичную геометрию Tier.

Дополнительно выполнен Blender Python pipeline: сцена, масштаб, transforms, normals, сохранение `.blend`, экспорт OBJ и отчёт PASS. Тестовый цилиндр служит проверкой автоматизации и не является greybox линейки или готовой игровой моделью.

Не выполнены на этапе аудита: greybox линейки, игровой model bake, проверка UV в игре, FE regression, FPS/frame time stress test. Gradle build не запускался: production code/resources не изменены, статический аудит не является заявкой на готовую игровую реализацию.

Следующий этап после аудита: создать greybox всех Tier через `bpy` и показать front/side/top, straight/corner/T/cross, machine connection и inventory angle → утвердить форму → закончить только Tier 1 → проверить в Minecraft, включая разрыв/восстановление, вертикаль, шесть направлений, смешанные Tier и 100+ труб → build/latest.log → после подтверждения переносить стиль на Tier 2/3.

Изменения других ассетов и систем DOMESURVIVAL в этот этап не входят. Ранее существовавшие незакоммиченные изменения оставлены как были; они не включаются в коммит аудита.
