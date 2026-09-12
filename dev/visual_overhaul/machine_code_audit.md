# Аудит кода машин, труб и динамического рендера

Дата: 12 сентября 2026. Область: текущие файлы `src/main/java` и связанные blockstates/models в `src/main/resources`. Это снимок **рабочего дерева**, включая незакоммиченные изменения; история Git, резервные копии и старые патчи не считаются активным контентом. В рамках этого аудита игровые исходники и ресурсы не изменялись. Проверка статическая: производительность и совместимость с Embeddium здесь не объявляются проверенными в клиенте.

Все ID ниже имеют namespace `domesurvival:`. Пути Java в тексте относительно `src/main/java/com/wasted/domesurvival/forge/`; пути JSON — относительно `src/main/resources/assets/domesurvival/`.

## 1. Действующие машины и технические устройства

«Baked JSON» означает обычную модель блока, запекаемую рендерером Minecraft; наличие BlockEntity само по себе не означает наличие BER. Таблица содержит каждый собственный активный ID машин, накопителей, технических переходов и внутренних частей.

| Registry ID | Назначение / BlockEntity | Визуальная реализация | GUI / меню | Уже доступный визуальный сигнал |
|---|---|---|---|---|
| `copper_furnace` | Медная печь; `CopperFurnaceBlockEntity` | Baked JSON, 24 элемента в основной модели | Vanilla `FurnaceMenu` / `FurnaceScreen` | Наследуемые `facing`, `lit` |
| `shaft_furnace` | Шахтная печь; `ShaftFurnaceBlockEntity` | Multipart `shaft_furnace_ready`, `_ready_on`, `_bottom_output`; cutout | `ShaftFurnaceMenu` / `ShaftFurnaceScreen` | `facing`, `lit` |
| `shaft_furnace_part` | Внутренняя связанная часть; `ShaftFurnacePartBlockEntity` | Baked JSON частей; cutout | Взаимодействие связано с основной печью | `facing`, `local_x`, `local_y` |
| `coke_oven` | Коксовая печь; `CokeOvenBlockEntity` | Multipart `coke_oven_ready`, `_ready_on`, `_bottom_output`; cutout | `CokeOvenMenu` / `CokeOvenScreen` | `facing`, `lit` |
| `coke_oven_part` | Внутренняя связанная часть; `CokeOvenPartBlockEntity` | Baked JSON частей; cutout | Взаимодействие связано с основной печью | Ориентация и координаты части |
| `machine_stabilizer` | Пассивный стабилизатор / компонент; без BE | Baked JSON, 61 элемент | Нет | Нет временной анимации |
| `coal_generator` | Угольный генератор; `CoalGeneratorBlockEntity` | `minecraft:block/orientable` + порты multipart | `CoalGeneratorMenu` / `CoalGeneratorScreen` | `facing`, `lit`, шесть `port_*` |
| `water_purifier` | Очиститель воды; `WaterPurifierBlockEntity` | `minecraft:block/orientable` + порты | `WaterPurifierMenu` / `WaterPurifierScreen` | `facing`, `lit`, шесть `port_*` |
| `oxygen_electrolyzer` | Электролизёр; `OxygenElectrolyzerBlockEntity` | `minecraft:block/orientable` + порты | `OxygenElectrolyzerMenu` / `OxygenElectrolyzerScreen` | `facing`, `lit`, шесть `port_*` |
| `oxygen_filler` | Заправщик / вентиляция; `OxygenFillerBlockEntity` | `minecraft:block/orientable` + порты; клиентские частицы | `OxygenFillerMenu` / `OxygenFillerScreen` | `facing`, `lit`, `port_*`; синхронизированный режим вентиляции |
| `bioincubator` | Биоинкубатор; `BioincubatorBlockEntity` | Baked JSON, 69 элементов основной модели + порты | `BioincubatorMenu` / `BioincubatorScreen` | `facing`, `lit`, шесть `port_*` |
| `sand_sieve` | Сито; `SandSieveBlockEntity` | Baked JSON 15 элементов + `SandSieveBlockEntityRenderer` | `SandSieveMenu` / `SandSieveScreen` | `facing`, `active`, сетка, песок, начало и тип цикла |
| `forming_press` | Формовочный пресс; `FormingPressBlockEntity` | `minecraft:block/orientable`; без BER | `FormingPressMenu` / `FormingPressScreen` | `facing`, `active` |
| `filter_regeneration_station` | Регенератор фильтров; `FilterRegenerationBlockEntity` | `minecraft:block/orientable`; без BER | `FilterRegenerationMenu` / `FilterRegenerationScreen` | `facing`, `active` |
| `energy_buffer` | Накопитель энергии; `EnergyBufferBlockEntity` | Baked cube / варианты уровня + multipart портов | `EnergyBufferMenu` / `EnergyBufferScreen` | `facing`, `energy_level=0..4`, `port_*` |
| `energy_buffer_titan` | Титановый накопитель; `TitanEnergyBufferBlockEntity` | `energy_buffer_titan_0..4` + порты | `TitanEnergyBufferMenu` / `TitanEnergyBufferScreen` | `facing`, `energy_level=0..4`, `port_*` |
| `energy_buffer_adamantium` | Адамантиевый накопитель; `AdamantiumEnergyBufferBlockEntity` | Варианты уровня + порты | `AdamantiumEnergyBufferMenu` / `AdamantiumEnergyBufferScreen` | `facing`, `energy_level=0..4`, `port_*` |
| `energy_buffer_creative` | Творческий источник; `CreativeEnergyBufferBlockEntity` | Baked cube + порты | `CreativeEnergyBufferMenu` / `CreativeEnergyBufferScreen` | `facing`, `port_*`; без `energy_level` |
| `oxygen_complex_air_intake` | Воздухозаборник комплекса; общий `OxygenComplexBlockEntity` | JSON formed/unformed, off/on + зарегистрированный общий BER портов; частицы всасывания | Общие `OxygenComplexMenu` / `OxygenComplexScreen` | `facing`, `formed`, `active` |
| `oxygen_complex_filtration` | Фильтрация комплекса; общий `OxygenComplexBlockEntity` | JSON formed/unformed, off/on + `OxygenComplexPortRenderer` | То же общее меню | `facing`, `formed`, `active`; верхний физический порт |
| `oxygen_complex_compression` | Компрессия комплекса; общий `OxygenComplexBlockEntity` | JSON formed/unformed, off/on + общий BER (без физического порта на этой роли) | То же общее меню | `facing`, `formed`, `active`; движения поршня сейчас нет |
| `oxygen_complex_output` | Выход комплекса; общий `OxygenComplexBlockEntity` | JSON formed/unformed, off/on + `OxygenComplexPortRenderer` | То же общее меню | `facing`, `formed`, `active`; нижний и задний порты |
| `solar_panel_mk1` | Солнечная панель I; `SolarPanelBlockEntity` | `forge:obj`, `models/block/solar_panel_mk1.obj`; cutout | Общие `SolarPanelMenu` / `SolarPanelScreen` | `facing`; механической анимации нет |
| `solar_panel_mk2` | Солнечная панель II; `SolarPanelBlockEntity` | `forge:obj`, `models/block/solar_panel_mk2.obj`; cutout | То же меню | `facing` |
| `solar_panel_mk3` | Солнечная панель III; `SolarPanelBlockEntity` | `forge:obj`, `models/block/solar_panel_mk3.obj`; cutout | То же меню | `facing` |
| `universal_tank` | Универсальный резервуар; `UniversalTankBlockEntity` | `UniversalTankBlockEntityRenderer`: единичный или общий корпус, стекло, содержимое и клапаны; отдельная JSON-модель для соответствующих представлений | `UniversalTankMenu` / `UniversalTankScreen` | Вид/количество содержимого, размер структуры, master, режимы сторон |
| `service_pass_through` | Герметичный проход коммуникаций; `ServicePassThroughBlockEntity` | Baked корпус + `ServicePassThroughRenderer` установленной трубы | Нет отдельного меню | `axis` и синхронизированный установленный conduit |
| `airlock_gate` | Многоблочные ворота; `AirlockGateBlockEntity` | Baked unformed + `AirlockGateBlockEntityRenderer` собранных створок | Нет отдельного контейнерного GUI | `facing`, `formed`, `master`, `motion`; время старта/длительность |
| `airlock_control_panel` | Панель управления шлюзом; `AirlockControlPanelBlockEntity` | Baked JSON, 54 элемента | Нет отдельного контейнерного GUI | Состояние блока/связанного шлюза |
| `copper_hopper` | Медная воронка; `TieredHopperBlockEntity` | Baked JSON, 25 элементов | `TieredHopperMenu` / `TieredHopperScreen` | Сторона выхода, состояние воронки |
| `steel_hopper` | Стальная воронка; `TieredHopperBlockEntity` | Baked JSON, 29 элементов | То же меню | Сторона выхода, состояние воронки |
| `desh_hopper` | Воронка из Desh; `TieredHopperBlockEntity` | Baked JSON, 35 элементов | То же меню | Сторона выхода, состояние воронки |

Дополнительно сохранены legacy ID `airlock_door` и `airlock_panel` в `ModBlocks`; BlockItem для них намеренно не зарегистрирован, текущая система использует `airlock_gate` и `airlock_control_panel`. Они не должны исчезать вследствие визуального рефакторинга. Автомобили `lanos_decorative`, `lanos_abandoned`, внутренняя `lanos_hitbox_part` и `LanosTrunkBlockEntity` находятся вне машинной производственной системы, но входят в общий аудит визуальных объектов.

В текущем дереве **удалены** `machine/transformer/*` и ресурсы transformer. Трансформатор не входит в действующий перечень и не должен восстанавливаться по старым копиям. `machine/solar/*` и ресурсы трёх панелей существуют как незакоммиченные добавления и входят в перечень.

Источники регистрации: `block/ModBlocks.java`, `registry/ModBlockEntities.java`, `registry/ModMenuTypes.java`, `machine/forming/FormingPressRegistry.java`, `machine/filter/FilterRegenerationRegistry.java`, `machine/oxygen/complex/OxygenComplexRegistry.java`, `machine/solar/SolarPanelRegistry.java`, `machine/passthrough/ServicePassThroughRegistry.java`, `storage/tank/UniversalTankRegistry.java`, `airlock/gate/AirlockGateRegistry.java`, `airlock/AirlockPanelRegistry.java`, `hopper/HopperRegistryEvents.java`.

**Ловушка аудита:** комментарий перед `COPPER_FURNACE` в `ModBlocks.java` описывает пассивное шасси без BE, но исполняемый код создаёт `CopperFurnaceBlock`, наследующий `AbstractFurnaceBlock`, с настоящим BE, серверным тикером и `FurnaceMenu`. Классификация в таблице основана на исполняемом коде.

## 2. Все 13 ID труб

| Registry ID | Семейство / уровень | BE и рендер | GUI |
|---|---|---|---|
| `oxygen_pipe` | Oxygen BASIC | BE нет; baked multipart | Нет |
| `reinforced_oxygen_pipe` | Oxygen REINFORCED | BE нет; baked multipart | Нет |
| `high_flow_oxygen_pipe` | Oxygen HIGH_FLOW | BE нет; baked multipart | Нет |
| `basic_energy_pipe` | Energy BASIC | `EnergyPipeBlockEntity`; baked multipart, BER нет | Нет |
| `reinforced_energy_pipe` | Energy REINFORCED | То же | Нет |
| `high_voltage_energy_pipe` | Energy HIGH_VOLTAGE | То же | Нет |
| `basic_fluid_pipe` | Fluid BASIC | `FluidPipeBlockEntity`; baked multipart/cutout, активного BER нет | Нет |
| `reinforced_fluid_pipe` | Fluid REINFORCED | То же | Нет |
| `high_pressure_fluid_pipe` | Fluid HIGH_PRESSURE | То же | Нет |
| `copper_item_pipe` | Item COPPER | `ItemPipeBlockEntity`; baked multipart + `ItemPipeBlockEntityRenderer` коннекторов | `ItemConnectorMenu` / `ItemConnectorScreen` |
| `steel_item_pipe` | Item STEEL | То же | То же |
| `desh_item_pipe` | Item DESH | То же | То же |
| `filtering_item_pipe` | Item FILTERING | То же | Также `FilteringItemPipeMenu` / `FilteringItemPipeScreen` |

Registry источники: кислород/энергия — `block/ModBlocks.java`; жидкость — `transport/fluid/FluidPipeRegistry.java`; предметы — `itempipe/ItemPipeRegistry.java`.

### Точные свойства и правила соединения

У всех четырёх семейств существуют **шесть BooleanProperty** с сериализованными именами `north`, `east`, `south`, `west`, `up`, `down`. Набор даёт 64 комбинации. По умолчанию все false; постановка и обновления соседей вычисляют значения через `refreshConnections` / `updateConnections` и `updateShape`. JSON multipart рисует core всегда, arm/connection — только при соответствующем true. В `oxygen_pipe.json` базовый arm направлен на north, east/south/west используют Y=90/180/270, up/down используют X=90/270. Нельзя менять смысл осей или заменять условия догадками о соседях в новом рендерере.

| Семейство | Источник истины для соединений | Важные исключения |
|---|---|---|
| Oxygen | `machine/oxygen/OxygenPipeBlock.java:104`, `OxygenPipeConnectionData` | Сначала проверяется ручной разрыв собственной стороны; у соседней кислородной трубы — встречной стороны. Для машин проверяется `ModCapabilities.OXYGEN` на `direction.getOpposite()`. Маски ручных разрывов сохраняются в серверном SavedData (`Mask`), отдельного BE нет. Клиенту нельзя придумывать маску из отсутствующего клиентского SavedData; визуал должен доверять синхронизированному blockstate. |
| Energy | `transport/energy/EnergyPipeBlock.java:179`, `EnergyPipeBlockEntity` | Собственная сторона и встречная сторона трубы проверяются через `getSideMode(...).isConnectionEnabled()`. Машина проверяется на `ForgeCapabilities.ENERGY` с противоположной стороны. Режимы сохраняются как `SideModes`, обновляются соседи, инвалидируется сеть. |
| Fluid | `transport/fluid/FluidPipeBlock.java:159`, `FluidPipeBlockEntity` | Аналогично энергии, но capability `ForgeCapabilities.FLUID_HANDLER`; собственные `FluidPipeSideMode` и NBT `SideModes`. |
| Item | `itempipe/ItemPipeBlock.java:300`, `ItemPipeBlockEntity` | Ручной разрыв на обоих концах трубы. У `ServicePassThroughBlockEntity` отдельное правило: ITEM и совместимая ось. Иначе `ITEM_HANDLER` противоположной стороны **или null**. `hasObjectConnector` отдельно исключает item-трубы и корректный проход; не тождественен `canConnect`. |

Общие действия ключом находятся в `pipe/PipeWrenchConnectionService.java`, `PipeWrenchInteractionEvents.java`, `PipeWrenchReliableInteractionEvents.java`. Транспорт работает в `OxygenPipeTransferService`, `EnergyPipeNetwork`, `FluidPipeNetwork`, `ItemPipeNetworkManager`. Визуальные изменения не должны затрагивать эти классы, tier-скорости, маршрутизацию, инвентари, capabilities или NBT.

Для service pass-through `ServicePassThroughRenderer.java:57` строит прямое визуальное состояние установленного блока, оставляя true только направления вдоль `axis`. Осевое масштабирование `0.996` предотвращает совпадение торцов на границе; радиальный диаметр не уменьшается. Это часть существующего визуального контракта.

## 3. Регистрация BER и технологии

Обнаружено **6 действующих регистраций BlockEntityRenderer**:

| BER | Регистрация | Что он действительно рисует |
|---|---|---|
| `SandSieveBlockEntityRenderer` | `client/ClientModEvents.java:123` | Сетка как ItemRenderer и песок как `renderSingleBlock`; PoseStack, sin и partial ticks |
| `AirlockGateBlockEntityRenderer` | `client/AirlockGateClientEvents.java:22` | Раздвижные створки собранных ворот, только master; собственные quads с освещением |
| `OxygenComplexPortRenderer` | `client/OxygenComplexClientRenderEvents.java:21` | Состояние физических портов, не механизмы компрессора |
| `UniversalTankBlockEntityRenderer` | `client/UniversalTankClientEvents.java:41` | Корпус, стекло, содержимое, клапаны; единая модель многоблочной структуры |
| `ServicePassThroughRenderer` | `client/render/ServicePassThroughClientEvents.java:20` | Модель установленной коммуникации внутри прохода |
| `ItemPipeBlockEntityRenderer` | `client/itempipe/ItemPipeClientEvents.java:31` | Статические по времени 3D-муфты и текстуры режима возле предметных устройств |

`transport/fluid/FluidPipeBlockEntityRenderer.java` существует, но `render` намеренно пуст, а регистрация отсутствует. Само имя класса не является доказательством анимации жидкости.

Стек машин: стандартные blockstate JSON variants/multipart, JSON elements, Forge OBJ loader для солнечных панелей, vanilla BER/PoseStack/VertexConsumer, ItemRenderer для сетки сита, cutout и translucent атласы для резервуара и частиц. В проверенном собственном коде машин нет GeckoLib-контроллеров/GeoBlockRenderer. Наличие GeckoLib в сборке зависимостей другого мода не означает его использование этими машинами.

## 4. Состояние и синхронизация: границы безопасной анимации

1. `lit` и `active` уже передаются как blockstate. Генератор, очиститель, электролизёр, заправщик и инкубатор используют lit; пресс, регенератор, сито и комплекс используют active. Привязать визуальную работу вентилятора к этим данным можно без нового игрового состояния.
2. Порты большинства одиночных машин представлены `port_up/down/north/south/west/east` типа `PortVisual`: **off/input/output**. `PortVisual.fromMode` отображает `BOTH` в `OUTPUT`. Это существующая семантика, а не потерянный вариант JSON; не добавлять четвертый режим в gameplay ради цвета.
3. `RelativeSide.resolve` задаёт FRONT=facing, BACK=opposite, LEFT=counterClockWise, RIGHT=clockWise, TOP=UP, BOTTOM=DOWN. Выбор стороны нельзя заменить другой convention в моделях.
4. Комплекс использует `OxygenComplexPortLayout`: реальные порты только TOP на FILTRATION, BOTTOM и BACK на OUTPUT. Хотя `hostRole` имеет ветки FRONT/LEFT/RIGHT, `isPhysicalPort` их отсекает. Рисовать активные порты на каждом из четырёх блоков было бы неверно.
5. Menu `ContainerData` синхронизирует энергию, прогресс и статусы **с открытым экраном**. Это не гарантирует доступность тех же значений у world BER. Например, у регенератора нет собственных `getUpdateTag/getUpdatePacket`; его `progress`, inventory и energy нельзя читать в новом BER как достоверные клиентские значения.
6. У энергобуферов есть BE update packet/tag плюс blockstate energy_level и портов. У oxygen filler есть update tag/packet для режима; вентиляционные частицы используют этот существующий режим. У oxygen complex есть собственный tag/packet и данные контроллера.
7. Сито: `SandSieveBlockEntity.java:331` считает animationProgress из game time + partialTick и cycleStartedAt, учитывает wet/dry duration; `getUpdateTag` возвращает `saveWithoutMetadata`, packet читает load. Сетка/песок рендерятся из уже синхронизированного inventory.
8. Ворота: `AirlockGateBlockEntity.java:64` использует `motion`, время начала, длительность и quintic smootherstep. `GateAnimStart`, `GateAnimDuration`, `GateAnimOpening` уже сохранены и синхронизируются. Они связаны с фактическим проходом и не должны меняться ради другой формы створки.
9. ItemPipe BE передаёт connectorModes, manualDisconnectMask, filter/route настройки через update packet/tag. Energy/Fluid BE сохраняют side modes и обновляют blockstates, но собственных update packet/tag в этих классах нет. Нельзя считать клиентский `getSideMode` дополнительным достоверным источником для новых индикаторов без отдельного решения по синхронизации.

Общие OFF/STARTING/RUNNING/STOPPING/ERROR/NO_POWER/FULL/BLOCKED сейчас **не являются общим перечислением машин**. Допустимо иметь клиентскую фазу плавного старта/останова по изменению уже синхронизированного active, но нельзя утверждать конкретную причину остановки в мире без существующего клиентского сигнала. GUI может отображать реальные причины из текущего меню.

## 5. Реально существующие анимации и динамические эффекты

| Объект | Подтверждение | Характер движения |
|---|---|---|
| Сито | `client/render/SandSieveBlockEntityRenderer.java:24`; `machine/sieve/SandSieveBlock.java:81` | Сетка двигается по X/Z и наклоняется sin-функциями при active; куб песка плавно спускается и уменьшается по прогрессу; песчинки появляются в animateTick |
| Ворота | `client/render/AirlockGateBlockEntityRenderer.java:54`; `airlock/gate/AirlockGateBlockEntity.java:64` | Две створки разъезжаются по реальному состоянию, интерполяция partial ticks и плавные концы |
| Воздухозаборник комплекса | `machine/oxygen/complex/OxygenComplexBlock.java:117` | CLOUD направляется внутрь решётки только для AIR_INTAKE + formed + active, с вероятностным пропуском |
| Заправщик в режиме вентиляции | `machine/oxygen/OxygenFillerBlockEntity.java:433` | Периодическая `VENTILATION_BUBBLE`, только VENTILATION + lit; фаза зависит от позиции |
| Предметные трубы | `itempipe/ItemPipeNetworkManager.java:379`; `client/itempipe/ItemPipePacketParticle.java` | Сервер ведёт travel visual по маршруту и отправляет частицу в текущей точке; частица живёт 2 тика, размер 0.10, без физики |
| Универсальный резервуар | `client/render/UniversalTankBlockEntityRenderer.java` | Геометрия уровня и содержимого отражает синхронизированные значения; fluid sprite может иметь собственную атласную анимацию, это не движение корпуса |
| Машины с active/lit | Их blockstates и *_on/*_active модели | Переключение внешнего состояния; это не полноценная механическая анимация |
| Порты машин и предметных труб | `PortVisual`, `OxygenComplexPortRenderer`, `ItemPipeBlockEntityRenderer` | Динамически выбранная маркировка режима; временной анимации нет |

В остальных проверенных машинах (включая forming press, filter regenerator, солнечные панели и модуль компрессии) подтверждённого движения механизмов сейчас нет. Список файлов texture `.mcmeta` и внешних пакетов анимации следует смотреть в общем ресурсоаудите: их наличие отдельно от renderer не доказывает привязку к машине.

## 6. Основные проблемы и приоритеты

- **HIGH — слабая читаемость семейства базовых машин.** Coal generator, water purifier, oxygen electrolyzer/filler, forming press, filter regeneration используют один принцип orientable cube. Регенератор буквально ссылается на `coal_generator_top` и `coal_generator_side`; собственным является только front. Это проверенное переиспользование, а не предположение по скриншоту.
- **HIGH — механизмы без движения.** Регенератор, пресс и компрессионный модуль по состоянию не демонстрируют свою работу формой/движением. Безопасное начало — визуальный механизм по active.
- **HIGH — world renderer и GUI имеют разную доступность данных.** Нельзя превратить серверное поле progress в источник клиентской анимации без анализа протокола. В эталоне ограничить world-анимацию существующим active, а точные причины выводить в GUI.
- **HIGH — транспортные соединения должны оставаться без изменений.** Новый корпус/фланец обязан совпасть с реальными six-direction props, ручными разрывами, возможностями соседей и service pass-through.
- **MEDIUM — неодинаковая сложность моделей.** В пределах одной системы встречаются orientable cube, 15/24/54/61/69 элементов и OBJ. Само число элементов не является FPS-измерением, но оправдывает бюджет геометрии и проверку плотных групп машин.
- **MEDIUM — материалы портов переиспользованы несистемно.** Oxygen complex TOP использует energy_buffer `back` / `side_input` / `side_output`, остальные порты — `oxygen_complex/port_*`. Сначала описать общую маркировку, затем переносить без изменения смыслов.
- **MEDIUM — статические динамические BER.** ItemPipe обходит шесть соседей и проверяет capabilities каждый кадр, затем рисует frame/face через entityCutoutNoCull. OxygenComplexPortRenderer ищет controller каждый кадр. Это реальные точки для будущего профилирования; сейчас их нельзя объявить доказанными bottleneck.
- **MEDIUM — дальние многоблочные BER.** Ворота `shouldRenderOffScreen=true`, дистанция 96 блоков; резервуар для unified model тоже допускает offscreen и имеет viewDistance 128. Это нужно для крупных структур, но требует проверки плотных сцен и корректных границ. У ворот работа зависит от size², у резервуара есть прозрачность.
- **MEDIUM — визуал перемещения предметов имеет сетевую стоимость.** `tickTravelVisuals` отправляет particles с сервера; не увеличивать частоту/количество ради декоративного потока. Проверить существующие лимиты и нагрузку перед отдельным улучшением.
- **LOW — неточность комментария copper_furnace.** Не приводит к визуальной ошибке сама по себе, но опасна для автоматической классификации/рефакторинга.

## 7. Предлагаемый первый эталон

**`domesurvival:filter_regeneration_station` — станция регенерации фильтров.** Средняя сложность: полноценная машина с энергией, двумя слотами, реальными статусами и GUI, но без многоблочных частей/портовой сети. Позволяет показать уникальный силуэт, сервисную кассету, вентиляцию, индикацию и движение без правки производства.

Безопасный состав эталона:

1. Корпус JSON с узнаваемой кассетой на фронте, рамкой, решёткой и одним визуальным вентилятором, остающийся в существующем объёме блока. Сохранить ID, `facing`, `active`, все rotations и существующий collision/interaction.
2. Собственные текстуры regenerator вместо coal_generator side/top, с пиксельной плотностью общего руководства; читаемый off/active фронт.
3. Небольшой клиентский BER для вентилятора/сервисного механизма, работающий только от `active` и gameTime+partialTick. Статический корпус оставить baked. Краткий плавный старт/останов допустим в клиентском presentation-state; он не должен вводить новые тики, сетевые пакеты или NBT. Не анимировать ремонт фильтра как точный progress, поскольку BE не синхронизирует его для world renderer.
4. GUI сохранить размеры, слоты, меню, data indices и hit areas. Можно менять фон, рамки, иконки, цвет существующих статусов и их компоновку вне слотов. Точные состояния уже есть: READY, REGENERATING, NO_ENERGY, NO_FILTER, NO_MEDIA, FILTER_HEALTHY, EXHAUSTED. Не добавлять выдуманные FULL/BLOCKED.
5. До демонстрации: проверить off/active, четыре facing, восстановление после chunk reload, несколько машин рядом, мир/инвентарь/GUI, обычное освещение. Указать отдельно, что реально проверено в клиенте и что подтверждено только сборкой/статически.

Прототип не требует изменения `FilterRegenerationBlockEntity`, `FilterRegenerationMenu`, рецептов, ёмкости 20 000 FE, расхода 40 FE/t, длительности 200 ticks, восстановления 25%, ограничения 8 регенераций, `Inventory`/`Energy`/`Progress`/`DomeRegenCycles` или capabilities. Эти величины приведены как зафиксированные границы, не как предложение их менять.

## 8. Что можно менять без изменения функциональности

Разрешённый визуальный слой: формы JSON/OBJ, UV и собственные текстуры, item display transforms, чисто клиентский BER, статусы/иконки GUI на уже имеющихся данных, визуальные фланцы в существующих multipart, уменьшение лишних скрытых граней без изменения collision. Форма стрелок/портов должна сохранять input/output/off смысл. Блоки с ранее полным collision можно визуально разбивать на панели, но нельзя выдавать визуальную щель за реально проходимое отверстие.

За пределами безопасного слоя: новые/удалённые registry IDs, recipes, capacities, transfer budgets, side config, inventory/slots, SavedData/NBT/network packets, общая новая серверная машина состояний, соединения труб, многоблочная структура и collision. Такие изменения не нужны для выбранного первого эталона.
