# DOMESURVIVAL — аудит GUI, экипировки и клиентского рендера

Дата: 2026-09-12. Проверены исходники и ресурсы рабочего дерева; это статический аудит, а не подтверждение поведения в запущенном Minecraft. Ассеты и Java в рамках этого отчёта не изменялись. Корень Java-путей в таблицах: `src/main/java/com/wasted/domesurvival/forge/`; корень ресурсов: `src/main/resources/assets/domesurvival/`. Все Registry ID, если не оговорено иное, имеют namespace `domesurvival:`.

## 1. Полный перечень экранов контейнеров

Найден **21 собственный класс Screen**, 21 собственный тип меню и дополнительный vanilla FurnaceScreen для медной печи. `MetallurgyGui` — общий рисующий helper, не дополнительный Screen. Основные интерфейсы рисуются прямоугольниками `GuiGraphics`, строками, предметами и отдельными пиктограммами: общей фоновой PNG-текстуры машины у них нет. Размеры ниже — логические пиксели GUI, а не размеры текстур. «Порты» означает `textures/gui/coal_generator_ports.png`, 24×6 px.

| Название / Menu Registry ID | Screen / Menu | Размер основной панели | Модель / текстура интерфейса | Анимация / данные | Переработка / приоритет |
|---|---|---:|---|---|---|
| Угольный генератор / `coal_generator` | `client/screen/CoalGeneratorScreen.java`; `machine/coal/CoalGeneratorMenu.java` | 220×266 | Процедурная панель + порты | Заполнение энергии, остаток топлива; смена боковой конфигурации | Да: общий стиль, сохранить схему сторон / HIGH |
| Шахтная печь / `shaft_furnace` | `client/screen/ShaftFurnaceScreen.java`; `machine/shaft/ShaftFurnaceMenu.java` | 220×266 | `client/screen/MetallurgyGui.java` | Прогресс и остаток горения | Да: согласовать с остальными машинами / MEDIUM |
| Коксовая печь / `coke_oven` | `client/screen/CokeOvenScreen.java`; `machine/shaft/CokeOvenMenu.java` | 220×266 | `MetallurgyGui` | Прогресс и остаток горения | Да / MEDIUM |
| Очиститель воды / `water_purifier` | `client/screen/WaterPurifierScreen.java`; `machine/water/WaterPurifierMenu.java` | 220×284 | Процедурная панель + порты | Энергия, сырая/чистая вода, progress, status | Да: единая ресурсная семантика / HIGH |
| Электролизёр / `oxygen_electrolyzer` | `client/screen/OxygenElectrolyzerScreen.java`; `machine/oxygen/OxygenElectrolyzerMenu.java` | 220×266 | Процедурная панель + порты | Энергия, вода, кислород, progress, status | Да / HIGH |
| Заправщик кислорода / `oxygen_filler` | `client/screen/OxygenFillerScreen.java`; `machine/oxygen/OxygenFillerMenu.java` | 220×282 | Процедурная панель + порты | Баллон, кислород, энергия, режим вентиляции и состояния помещения | Да: сохранить различие заправки и вентиляции / HIGH |
| Биоинкубатор / `bioincubator` | `machine/bio/BioincubatorScreen.java`; `machine/bio/BioincubatorMenu.java` | 300×310 | Процедурная панель + порты + `textures/gui/bio/*` | Запасы и этапы восстановления/инкубации | Да: крупная панель, проверить малое окно / HIGH |
| Песчаное сито / `sand_sieve` | `machine/sieve/SandSieveScreen.java`; `machine/sieve/SandSieveMenu.java` | 300×227 | Процедурная панель | Прогресс, сетка, ресурсы | Да / MEDIUM |
| Энергобуфер / `energy_buffer` | `client/screen/EnergyBufferScreen.java`; `machine/energy/EnergyBufferMenu.java` | 176×104 | Процедурная панель + порты | Запас + отдельный overlay скорости передачи | Да: убрать конфликт рисующих слоёв при будущем обновлении / HIGH |
| Титановый буфер / `energy_buffer_titan` | `client/screen/TitanEnergyBufferScreen.java`; `machine/energy/TitanEnergyBufferMenu.java` | 176×104 | То же | То же | Да / HIGH |
| Адамантиевый буфер / `energy_buffer_adamantium` | `client/screen/AdamantiumEnergyBufferScreen.java`; `machine/energy/AdamantiumEnergyBufferMenu.java` | 176×104 | То же | То же | Да / HIGH |
| Творческий буфер / `energy_buffer_creative` | `client/screen/CreativeEnergyBufferScreen.java`; `machine/energy/CreativeEnergyBufferMenu.java` | 176×104 | То же | Бесконечность отображается отдельной веткой overlay | Да; сохранить семантику ∞ / HIGH |
| Багажник Lanos / `lanos_trunk` | `lanos/LanosTrunkScreen.java`; `lanos/LanosTrunkMenu.java` | 176×166 | Процедурная панель | Нет отдельной временной анимации | Да / LOW |
| Формовочный пресс / `forming_press` | `machine/forming/FormingPressScreen.java`; `machine/forming/FormingPressMenu.java` | 220×266 | Процедурная панель + порты | Прогресс, выбор доступной формы, status | Да / HIGH |
| Регенератор фильтров / `filter_regeneration_station` | `machine/filter/FilterRegenerationScreen.java`; `machine/filter/FilterRegenerationMenu.java` | 220×266 | Процедурная панель | Прогресс, ресурс фильтра, число циклов, status | Да / HIGH |
| Солнечные панели MK1–MK3 / menu `solar_panel` | `machine/solar/client/SolarPanelScreen.java`; `machine/solar/SolarPanelMenu.java` | 220×142 | Процедурная панель | Энергия и условия генерации | Да / MEDIUM |
| Кислородный комплекс, четыре секции / menu `oxygen_complex` | `client/screen/OxygenComplexScreen.java`; `machine/oxygen/complex/OxygenComplexMenu.java` | 360×326 | Процедурная панель; четыре схемы модулей | 4 временные иллюстрации, реальные active-флаги и буферы; два эффекта не отключаются в ожидании | Да: правдивость индикаторов и доступность на малом окне / HIGH |
| Универсальный бак / `universal_tank` | `client/screen/UniversalTankScreen.java`; `storage/tank/UniversalTankMenu.java` | 220×156 | Процедурная панель + порты + fluid atlas | Уровень/тип содержимого, размер структуры | Да / MEDIUM |
| Воронки copper/steel/desh / menu `tiered_hopper` | `client/TieredHopperScreen.java`; `hopper/TieredHopperMenu.java` | 176×(114 + 18×rows) | Процедурная панель | Нет отдельной временной анимации | Да / LOW |
| Коннектор предметной трубы / `item_pipe_connector` | `client/itempipe/ItemConnectorScreen.java`; `itempipe/ItemConnectorMenu.java` | 236×126 | Процедурная панель | Режим соединения/фильтров | Да: ясные вход/выход/выкл. / HIGH |
| Фильтрующая предметная труба / `filtering_item_pipe` | `client/itempipe/FilteringItemPipeScreen.java`; `itempipe/FilteringItemPipeMenu.java` | 300×262 | Процедурная панель | Правила фильтрации по сторонам | Да; не менять ghost/slot semantics / HIGH |
| Медная печь / block `copper_furnace`, menu `minecraft:furnace` | Vanilla `FurnaceScreen` / `FurnaceMenu`; создание в `machine/copper/CopperFurnaceBlockEntity.java` | Vanilla 176×166 | Vanilla `minecraft:textures/gui/container/furnace.png` | Vanilla пламя и progress | Отдельный редизайн не нужен на первом этапе / LOW |

Боковые панели обычно 96×122 и расположены за основной шириной; у биоинкубатора 106×126, у комплекса блок 132×116 внутри общей компоновки. Поэтому одной проверки `imageWidth` недостаточно: нужно проверять полную область видимых/кликабельных элементов. Изменение координат слотов потребует изменения Menu и выходит за визуальный первый этап.

## 2. JEI, HUD и внешние интерфейсы

| Объект | ID / файлы | Модель / текстура / размер | Движение и назначение | Действие / приоритет |
|---|---|---|---|---|
| JEI, 10 категорий | `client/jei/DomeSurvivalJeiPlugin.java`, `DomeMachineRecipeCategory.java`; `coke_oven`, `shaft_furnace`, `water_purifier`, `oxygen_electrolyzer`, `oxygen_filler`, `bio_repair`, `bio_incubation`, `sand_sieve`, `forming_press`, `filter_regeneration` | Blank drawable 180×70; vanilla JEI slots, item/fluid rendering, рисованная стрелка | Стрелка статическая; список рецептов и вероятности — функциональные данные | Согласовать стрелку/подписи/цвета; рецепты не менять / MEDIUM |
| Кислородный HUD | overlay `domesurvival:oxygen`, `client/OxygenHudOverlay.java` | `oxygen_full/half/empty.png`, каждый 9×9 | Реальный `ClientOxygenState`; HUD скрыт в creative/spectator и при полном запасе в дышащей среде; под водой строка сдвигается | Сохранить поведение и шаг 8 px / HIGH |
| Старый источник баллона HUD | `textures/gui/oxygen_tank_source.png` | 16×16 | Не используется текущим OxygenHudOverlay; причина удаления описана комментарием playtest | Не возвращать автоматически / LOW |
| Скорость энергобуфера | `client/EnergyStorageTransferRateOverlay.java` | Нет PNG; маска цвета `#30363A`, подписи поверх Screen | Реальные input/output per tick из `EnergyTransferRateMenu`; текст уменьшается до 0.72×; reflection читает energy/capacity | При смене панели обновить этот слой вместе с ней: сейчас фиксированные координаты и заливка / HIGH |
| Общие меню Minecraft | `client/DomeSurvivalScreenTuner.java` | `textures/gui/ui/v32/{world,network,system,corridor}.png`, по 1920×1080 | Статические фоны; исключены контейнеры, JEI, TitleScreen, loading. Кнопки страниц creative заменены прозрачными с сохранением действия | Не накладывать оформление машин глобально / MEDIUM |
| Загрузка мира | `client/EarlyWorldLoadingBackground.java`, `mixin/LevelLoadingScreenAccessor.java` | `textures/gui/early_world_loading.png`, 1920×1080; каталог `textures/gui/loading/` | Отрисовка реального прогресса загрузки; не декоративный бесконечный прогресс | Сохранить достоверность / LOW |
| CustomNPCs Joseph | `client/JosephGuiButtonOverlay.java`; связанные скрипты/интеграция проекта | Нативный ICustomGui, без PNG в renderer; кнопки 6501–6506 | Подписи «Проект», «Состояние базы», «План развития», «Закрыть», «Передать ресурсы», «Назад» | Сохранять ID и оригинальные действия; GBPort не имеет getName/getVisible/getEnabled / MEDIUM |
| FancyMenu | 5 файлов `config/fancymenu/customization/domesurvival_{title,select_world,create_world,join_multiplayer,options}_screen.txt` | Отдельные фоны, логотипы, кнопки и панели в `config/fancymenu/assets/domesurvival/` | Декларации enabled=true; кнопки normal/hover/disabled, looping background animations=false | Требует проверки установленного FancyMenu и фактического порядка с ScreenTuner / MEDIUM |
| FTB Quests | `src/main/resources/assets/ftbquests/ftb_quests_theme.txt` | 7 глав с `textures/gui/quests/chapter_*.png`, каждый 1672×941 | Native ThemeProperties.BACKGROUND, тематические цвета линий/состояний; не собственный Screen | Сохранить chapter ID, квесты и зависимые условия / MEDIUM |
| Погодный overlay | `client/weather/SurfaceWeatherClientEvents.java` | `textures/gui/solar_heat_vignette.png`, 256×256 | Сглаживание видимости, тепловая пульсация, изменение тумана по клиентскому погодному состоянию | Проверить совместимость видимости HUD / MEDIUM |

## 3. Вся носимая экипировка

У каждого предмета есть отдельная inventory-модель `models/item/<ID>.json`; их текстуры перечислены в общем файловом аудите. Здесь показана именно надетая модель. Все восемь предметов имеют vanilla inventory / Curios GUI; собственного контейнерного экрана у предметов нет.

| Название / Registry ID | Надетая модель | Текстура и размер | Renderer / привязка / анимация | Переработка / приоритет |
|---|---|---|---|---|
| Кислородная маска / `oxygen_mask` | `models/m40_mask_mesh.bin`: 600 вершин, 900 индексов, **300 треугольников** по заголовку DM40 v1 | `textures/models/armor/m40_gasmask_domesurvival.png`, 256×256 | `client/render/OxygenMaskCurioRenderer.java` → `M40MaskMesh`; `entityCutoutNoCull`; следует `HumanoidModel.head.translateAndRotate` | Да: плотность пикселя и крепление; перепаковку UV делать только вместе с мешем / HIGH |
| Малый баллон / `small_oxygen_tank` | `client/model/OxygenTankModel.java`, корпус 5.25×8.75×3.05 модельных единиц | `textures/models/armor/oxygen_tank.png`, 64×32 | `OxygenTankCurioRenderer`, ICurioRenderer.HumanoidRender, body; наследует позу игрока, самостоятельного sway нет | Да: малый сейчас крупнее среднего; это явно намеренная версия v23, не исправлять молча / HIGH |
| Средний баллон / `medium_oxygen_tank` | Тот же builder, корпус 4×7×2.75 | Та же 64×32 | То же; отдельный кэш размера | Да / HIGH |
| Большой баллон / `large_oxygen_tank` | Тот же builder, корпус 7×11.25×3.70 | Та же 64×32 | То же | Да / HIGH |
| Шлем защитного костюма / `surface_suit_helmet` | Vanilla Humanoid armor, `item/SurfaceSuitItem.java` | `textures/models/armor/surface_suit_layer_1.png`, 64×64 | Vanilla armor renderer; наследует head; вырезано лицо под отдельную Curios-маску | Да: сохранить вырез и читаемость с маской / HIGH |
| Куртка костюма / `surface_suit_chestplate` | Vanilla Humanoid armor | `surface_suit_layer_1.png`, 64×64 | Vanilla torso/arms | Да: проверить ремни/баллон / HIGH |
| Брюки костюма / `surface_suit_leggings` | Vanilla Humanoid armor | `surface_suit_layer_2.png`, 64×64 | Vanilla legs | Да / MEDIUM |
| Ботинки костюма / `surface_suit_boots` | Vanilla Humanoid armor | `surface_suit_layer_1.png`, 64×64 | Vanilla legs | Да / MEDIUM |

Баллон состоит из 9 cuboid-частей (корпус, клапан, две скобы, четыре ремня, перемычка): до 54 quad-граней до отсечения. Геометрия использует дробные модельные координаты; перед упрощением нужно сравнить надетый вид. Баллоны не имеют пружин, шланга до головы или собственной анимационной системы.

**Фактическая Curios-схема:** `OxygenMaskItem` расширяет `Item`, реализует `ICurioItem`, принимает только slot `oxygen_mask`; это не ArmorItem. `data/domesurvival/curios/slots/oxygen_mask.json` задаёт size=1, order=70, icon `curios:slot/empty_head_slot`; `data/domesurvival/curios/entities/player.json` добавляет этот слот игроку. `data/curios/tags/items/oxygen_mask.json` содержит маску; `data/curios/tags/items/back.json` содержит три баллона. Наличие back-slot зависит также от конфигурации/окружения Curios; нельзя выводить его наличие только из item tag.

**Наследие:** `M40MaskRenderLayer.java` проверяет HEAD, но регистрация/создание этого слоя в текущих исходниках не найдены. `OxygenMaskModel.java` — намеренно пустая модель 64×32, зарегистрирована в `ClientModEvents`, комментарий о старом ArmorItem больше не соответствует `OxygenMaskItem`. `OxygenEquipmentModelCache.maskModel()` также относится к старому пути. `textures/models/armor/oxygen_mask.png` 64×32 и `surface_suit_blue_layer_1/2.png` 64×64 существуют, но текущие renderer/item-классы используют M40 и зелёные `surface_suit_layer_*`. Ничего из этого не удалять без отдельной проверки ресурсных пакетов и сохранений.

M40 загружается один раз как classpath binary; это не GeckoLib и не runtime GLB loader. Замена модели ресурсным пакетом и F3+T не гарантируют перезагрузку статического `DATA`. До любых правок примитивов проверить соответствие индексного потока draw mode выбранного RenderType непосредственно в игре; число треугольников из binary само по себе не доказывает корректность фактической выдачи вершин.

## 4. Прочий renderer-код и реально существующее движение

| Объект / ID | Файл | Технология / состояние | Оценка |
|---|---|---|---|
| Шлюз / `airlock_gate` | `client/render/AirlockGateBlockEntityRenderer.java` | Native BER, `gate.renderProgress(partialTick)`, сдвиг полотен, поворот по facing, clipping UV | Реальная механическая анимация; сохранить master, progress, границы рендера / HIGH |
| Сито / `sand_sieve` | `client/render/SandSieveBlockEntityRenderer.java` | Native BER; ACTIVE включает sin-сдвиг сетки ±0.05/0.026 блока и наклон ±1.7°; sand progression через `animationProgress` | Реальная анимация сетки и опускания песка / HIGH |
| Бак / `universal_tank` | `client/render/UniversalTankBlockEntityRenderer.java` | Процедурный каркас/стекло, fluid sprite+tint из IClientFluidTypeExtensions, уровень содержимого; один master рисует общую структуру | Изменение заполнения, не механический цикл; translucent sorting требует игры / MEDIUM |
| Порты кислородного комплекса | `client/render/OxygenComplexPortRenderer.java` | Native BER, реальная конфигурация сторон | Смена вида портов, собственной циклической механики нет / HIGH |
| Проходка / `service_pass_through` | `client/render/ServicePassThroughRenderer.java` | Native BER, отображение проходящих систем | Не самостоятельная машина; сохранить существующие соединения / HIGH |
| Коннекторы item pipes | `client/itempipe/ItemPipeBlockEntityRenderer.java` | Entity cutout; INPUT/OUTPUT/DISABLED выбирается из block entity, соединение из `hasObjectConnector` | Сам BER статичен; нельзя подменять режим декоративным цветом / HIGH |
| Fluid pipes | `transport/fluid/FluidPipeBlockEntityRenderer.java` | Custom BER присутствует; подробная регистрация и port logic — в аудите труб | Наличие класса не равно подтверждённому активному пути / HIGH |
| Пакеты item pipes | `client/itempipe/ItemPipePacketParticle.java`, `ItemPipeClientEvents.java` | Зарегистрирован particle provider, движение частицы | Реальный существующий визуальный канал; перенос предметов не связывать с частотой кадров / MEDIUM |
| Картина / `memory_painting` | `client/painting/MemoryPaintingRenderer.java` | Делегирует vanilla PaintingRenderer; не skeletal entity | Без отдельной анимации / LOW |
| Zombie / Drowned / Skeleton / Enderman / Spider / Creeper | `client/horror/NativeHorrorAnimator.java` + шесть `mixin/Horror*ModelMixin.java` | Изменяют native `setupAnim`, позы, походку, атаки, напряжение/ранения по состоянию vanilla сущности | Реальная программная анимация; не трогать AI/атаку/коллизии / MEDIUM |
| Кислотный дождь, песчаная буря, вентиляционные пузырьки | `client/particle/{AcidRainParticle,SandstormParticle,VentilationBubbleParticle}.java` | Native particle providers | Есть движение частиц; настройки частиц и бюджет сохранять / MEDIUM |
| Solarite ore | `block/SolariteOreBlock.java#animateTick` | Золотые пылевые частицы | Декоративное событие, не индикатор работы / LOW |
| Кислородный комплекс в мире | `machine/oxygen/complex/OxygenComplexBlock.java#animateTick` | Облака при работающем состоянии | Есть эффект работы; отдельного поршневого BER пока нет / HIGH |
| OxygenComplex GUI | `client/screen/OxygenComplexScreen.java`, методы drawIntakeModule/drawFilterModule/drawCompressionModule/drawOutputModule | Вход: pulse из tickCount без active-проверки. Фильтрация: bubbles только isFilterActive. Сжатие: needle из tickCount без active-проверки. Выход: pulse только isOutputActive | Приоритетная визуальная проблема: стрелка выглядит как датчик давления, но не читает давление / HIGH |

GeckoLib/GeoRenderer/GeoModel и собственный BlockEntityWithoutLevelRenderer в проверенных Java/build.gradle не обнаружены. Основной стек: vanilla JSON/baked models, Forge native BER, Curios HumanoidRender, ручной M40 mesh, GuiGraphics, JEI, native particles и mixins. Отсутствие собственной библиотеки в исходниках не означает отсутствия зависимых модов с их renderer-кодом в установленной сборке.

## 5. Основные риски и безопасная очередь

1. **HIGH:** неинформативная анимация intake/gauge в неработающем OxygenComplex. Будущая правка только клиентских draw-методов: существующие `isIntakeActive()`/`isCompressionActive()`, без новых packets или статусов; не именовать fill-level давлением.
2. **HIGH:** разный pixel density у M40 256×256, кубических баллонов 64×32 и зелёной брони 64×64. У маски по визуальной проверке большая часть atlas пустая: размер atlas не равен требованию HD-детализации. Перепаковка требует нового UV/меша и отдельной проверки.
3. **HIGH:** GUI для комплекса и биоинкубатора и выдвижные боковые панели могут выходить за доступную GUI-площадь. Сначала проверить реальные GUI scale/window, затем предлагать клиентскую компоновку без перестановки слотов.
4. **HIGH:** EnergyStorageTransferRateOverlay перекрывает фиксированную область старого экрана. Замена одного Screen без этого overlay оставит прямоугольник другого цвета/пересечения текста.
5. **MEDIUM:** два пути фоновых меню: FancyMenu configs и `DomeSurvivalScreenTuner`; нужно проверить активный modpack, приоритет событий и installed versions, прежде чем заменять фоны.
6. **MEDIUM:** несколько GUI используют `Component.literal` для русских подписей и неодинаковые обозначения FE/RF. Будущая визуальная локализация должна оставлять значения и единицы механики неизменными.
7. **LOW:** неиспользуемые wearable-модели/текстуры и старые комментарии создают ложную карту зависимостей. Удаление не входит в первый этап.

Первый проход: единая спецификация; одна тестовая машина; сравнение старого/нового вида и игровая проверка. Массовый перенос GUI, новая wearable-анимация и добавление renderer-библиотеки сейчас не выполняются. Реальные новые состояния машины, датчик давления, изменения Curios/NBT/слотов/пакетов требуют отдельной функциональной задачи.

## 6. Файловый каталог GUI/armor

Ниже автоматически перечислены фактические PNG и их размеры по заголовку PNG. Этот каталог включает резервные ассеты; наличие файла не означает его активное использование.

| Файл | Размер px |
|---|---:|
| `config/fancymenu/assets/domesurvival/backgrounds/bg_connecting_airlock.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_create_world_sector.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_death_emergency.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_loading_oxygen_complex.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_main_dome_night.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_multiplayer_comms.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_pause_dark_interior.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_settings_terminal.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/backgrounds/bg_world_select_corridor.png` | 2560×1440 |
| `config/fancymenu/assets/domesurvival/buttons/button_danger_disabled.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_danger_hover.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_danger_normal.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_exit_disabled.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_exit_hover.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_exit_normal.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_mods_disabled.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_mods_hover.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_mods_normal.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_multi_disabled.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_multi_hover.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_multi_normal.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_primary_disabled.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_primary_hover.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_primary_normal.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_settings_disabled.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_settings_hover.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_settings_normal.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_single_disabled.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_single_hover.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/buttons/button_single_normal.png` | 620×92 |
| `config/fancymenu/assets/domesurvival/logos/logo_dome_survival_emblem.png` | 256×256 |
| `config/fancymenu/assets/domesurvival/logos/logo_dome_survival_full.png` | 1300×360 |
| `config/fancymenu/assets/domesurvival/logos/logo_dome_survival_small.png` | 720×210 |
| `config/fancymenu/assets/domesurvival/ui/loading_bar_bg.png` | 1000×44 |
| `config/fancymenu/assets/domesurvival/ui/loading_bar_fill.png` | 1000×44 |
| `config/fancymenu/assets/domesurvival/ui/overlay_vignette.png` | 1920×1080 |
| `config/fancymenu/assets/domesurvival/ui/panel_list.png` | 1600×1080 |
| `config/fancymenu/assets/domesurvival/ui/panel_main_menu.png` | 720×1100 |
| `config/fancymenu/assets/domesurvival/ui/panel_popup.png` | 1100×700 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/bio_faces_atlas.png` | 160×128 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/chicken.png` | 36×36 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/cow.png` | 36×36 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/mule.png` | 36×36 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/parrot.png` | 36×36 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/pig.png` | 36×36 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/polar_bear.png` | 36×36 |
| `src/main/resources/assets/domesurvival/textures/gui/bio/sheep.png` | 36×36 |
| `src/main/resources/assets/domesurvival/textures/gui/coal_generator_ports.png` | 24×6 |
| `src/main/resources/assets/domesurvival/textures/gui/early_world_loading.png` | 1920×1080 |
| `src/main/resources/assets/domesurvival/textures/gui/loading/loading_bar_fill_v27.png` | 1223×30 |
| `src/main/resources/assets/domesurvival/textures/gui/loading/loading_dome_city.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/oxygen_empty.png` | 9×9 |
| `src/main/resources/assets/domesurvival/textures/gui/oxygen_full.png` | 9×9 |
| `src/main/resources/assets/domesurvival/textures/gui/oxygen_half.png` | 9×9 |
| `src/main/resources/assets/domesurvival/textures/gui/oxygen_tank_source.png` | 16×16 |
| `src/main/resources/assets/domesurvival/textures/gui/quests/chapter_00_under_dome.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/quests/chapter_01_first_days.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/quests/chapter_02_beyond_gate.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/quests/chapter_03_settlement.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/quests/chapter_04_food_system.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/quests/chapter_06_power.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/quests/chapter_07_industrial_district.png` | 1672×941 |
| `src/main/resources/assets/domesurvival/textures/gui/solar_heat_vignette.png` | 256×256 |
| `src/main/resources/assets/domesurvival/textures/gui/ui/v32/corridor.png` | 1920×1080 |
| `src/main/resources/assets/domesurvival/textures/gui/ui/v32/network.png` | 1920×1080 |
| `src/main/resources/assets/domesurvival/textures/gui/ui/v32/system.png` | 1920×1080 |
| `src/main/resources/assets/domesurvival/textures/gui/ui/v32/world.png` | 1920×1080 |
| `src/main/resources/assets/domesurvival/textures/models/armor/m40_gasmask_domesurvival.png` | 256×256 |
| `src/main/resources/assets/domesurvival/textures/models/armor/oxygen_mask.png` | 64×32 |
| `src/main/resources/assets/domesurvival/textures/models/armor/oxygen_tank.png` | 64×32 |
| `src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_blue_layer_1.png` | 64×64 |
| `src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_blue_layer_2.png` | 64×64 |
| `src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_layer_1.png` | 64×64 |
| `src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_layer_2.png` | 64×64 |
