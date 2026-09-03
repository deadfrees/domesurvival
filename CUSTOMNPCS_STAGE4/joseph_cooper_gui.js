/* Dome Survival - Joseph Cooper GUI v7.5.1 PROGRESSION BALANCE */
var API = Java.type("noppes.npcs.api.NpcAPI").Instance();
var Bridge = Java.type("com.wasted.domesurvival.forge.progression.JosephCooperBridge");
var V75ForgeRegistries = Java.type("net.minecraftforge.registries.ForgeRegistries");
var V75Registries = Java.type("net.minecraft.core.registries.Registries");
var V75TagKey = Java.type("net.minecraft.tags.TagKey");
var V75ResourceLocation = Java.type("net.minecraft.resources.ResourceLocation");
var V75_ITEM_TAG_CACHE = {};

var GUI_NAME = "dome_joseph_v751_progression_balance";
var GUI_WIDTH = 320;
var GUI_HEIGHT = 230;

var BTN_PROJECT = 6501;
var BTN_BASE = 6502;
var BTN_PLAN = 6503;
var BTN_CLOSE = 6504;
var BTN_CONTRIBUTE = 6505;
var BTN_BACK = 6506;

/* Stage 01 - Dome survival + first Farmer's Delight kitchen tools. */
var S1_COMPLETE = "domesurvival.stage01.complete.v71";
var S1 = [
    { key: "domesurvival.stage01.logs.v73",      id: "minecraft:oak_log",                   name: "Дубовое бревно", req: 32 },
    { key: "domesurvival.stage01.saplings.v73",  id: "minecraft:oak_sapling",               name: "Саженец дуба", req: 8 },
    { key: "domesurvival.stage01.wheat.v73",     id: "minecraft:wheat",                     name: "Пшеница", req: 24 },
    { key: "domesurvival.stage01.charcoal.v73",  id: "minecraft:charcoal",                  name: "Древесный уголь", req: 16 },
    { key: "domesurvival.stage01.glass.v73",     id: "minecraft:glass",                     name: "Стекло", req: 32 },
    { key: "domesurvival.stage01.copper.v73",    id: "minecraft:copper_ingot",              name: "Медный слиток", req: 12 },
    { key: "domesurvival.stage01.board.v73",     id: "farmersdelight:cutting_board",        name: "Разделочная доска", req: 1 },
    { key: "domesurvival.stage01.pot.v73",       id: "farmersdelight:cooking_pot",          name: "Кухонный котёл", req: 1 },
    { key: "domesurvival.stage01.skillet.v73",   id: "farmersdelight:skillet",              name: "Сковорода", req: 1 }
];

/* Stage 02 - Workshop. Java Bridge owns the core 64 iron / 32 copper / 24 redstone. */
var S2_EXTRA_COMPLETE = "domesurvival.stage02.extras.complete.v71";
var S2 = [
    { key: "domesurvival.stage02.stone.v73",     id: "minecraft:stone_bricks",                    name: "Каменные кирпичи", req: 32 },
    { key: "domesurvival.stage02.furnace.v73",   id: "domesurvival:copper_furnace",              name: "Медная печь", req: 1 },
    { key: "domesurvival.stage02.hopper.v73",    id: "domesurvival:copper_hopper",               name: "Медная воронка", req: 1 },
    { key: "domesurvival.stage02.ieiron.v73",    id: "immersiveengineering:component_iron",       name: "Железный механический компонент", req: 4 },
    { key: "domesurvival.stage02.iewire.v73",    id: "immersiveengineering:wire_copper",          name: "Медный провод", req: 8 },
    { key: "domesurvival.stage02.mekcircuit.v73",id: "mekanism:basic_control_circuit",            name: "Базовая схема управления", req: 2 },
    { key: "domesurvival.stage02.mekalloy.v73",  id: "mekanism:alloy_infused",                    name: "Наполненный сплав", req: 4 }
];

/* Logical-reset fallback for old test worlds where Java workshop SavedData is already complete. */
var RELEASE_RESET_LOCK = "domesurvival.release.logical_reset.v70";
var S2_RESET_CORE_COMPLETE = "domesurvival.stage02.resetcore.complete.v70";
var S2_RESET_CORE = [
    { key: "domesurvival.stage02.resetcore.iron.v70",     id: "minecraft:iron_ingot",   name: "Железный слиток", req: 64 },
    { key: "domesurvival.stage02.resetcore.copper.v70",   id: "minecraft:copper_ingot", name: "Медный слиток", req: 32 },
    { key: "domesurvival.stage02.resetcore.redstone.v70", id: "minecraft:redstone",     name: "Редстоун", req: 24 }
];

/* Stage 03 - Emergency power using DomeSurvival + Mekanism + IE + Ender IO. */
var S3_COMPLETE = "domesurvival.stage03.complete.v71";
var S3 = [
    { key: "domesurvival.stage03.generator.v73",   id: "domesurvival:coal_generator",                 name: "Стабилизатор пламени", req: 1 },
    { key: "domesurvival.stage03.matrix.v73",      id: "domesurvival:pulse_matrix",                   name: "Импульсная матрица", req: 2 },
    { key: "domesurvival.stage03.stabilizer.v73",  id: "domesurvival:machine_stabilizer",             name: "Машинный стабилизатор", req: 1 },
    { key: "domesurvival.stage03.tablet.v73",      id: "mekanism:energy_tablet",                       name: "Энергетический планшет", req: 1 },
    { key: "domesurvival.stage03.circuit.v73",     id: "mekanism:basic_control_circuit",              name: "Базовая схема управления", req: 4 },
    { key: "domesurvival.stage03.alloy.v73",       id: "mekanism:alloy_infused",                      name: "Наполненный сплав", req: 8 },
    { key: "domesurvival.stage03.electronic.v73",  id: "immersiveengineering:electron_tube",   name: "Электронная лампа", req: 4 },
    { key: "domesurvival.stage03.copperwire.v73",  id: "immersiveengineering:wire_copper",            name: "Медный провод", req: 12 },
    { key: "domesurvival.stage03.darksteel.v73",   id: "enderio:dark_steel_ingot",                    name: "Слиток тёмной стали", req: 4 },
    { key: "domesurvival.stage03.coal.v73",        id: "minecraft:coal",                               name: "Уголь", req: 32 }
];

/* Stage 04 - Water loop and productive soil inside the dome. */
var S4_COMPLETE = "domesurvival.stage04.complete.v71";
var S4 = [
    { key: "domesurvival.stage04.filter.v73",      id: "domesurvival:water_filter_cartridge", name: "Обычный фильтрующий картридж", req: 4 },
    { key: "domesurvival.stage04.ifilter.v73",     id: "domesurvival:improved_water_filter",  name: "Улучшенный фильтрующий картридж", req: 1 },
    { key: "domesurvival.stage04.pipe.v73",        id: "domesurvival:basic_fluid_pipe",       name: "Базовая жидкостная труба", req: 12 },
    { key: "domesurvival.stage04.tank.v73",        id: "domesurvival:universal_tank",         name: "Универсальный резервуар", req: 1 },
    { key: "domesurvival.stage04.compost.v73",     id: "farmersdelight:organic_compost",      name: "Органический компост", req: 4 },
    { key: "domesurvival.stage04.richsoil.v73",    id: "farmersdelight:rich_soil",            name: "Плодородная почва", req: 4 },
    { key: "domesurvival.stage04.copper.v73",      id: "minecraft:copper_ingot",              name: "Медный слиток", req: 16 },
    { key: "domesurvival.stage04.glass.v73",       id: "minecraft:glass",                     name: "Стекло", req: 16 }
];

/* Stage 05 - Oxygen infrastructure; tech requirements are deliberately cross-mod. */
var S5_COMPLETE = "domesurvival.stage05.complete.v71";
var S5 = [
    { key: "domesurvival.stage05.matrix.v73",       id: "domesurvival:pulse_matrix",                   name: "Импульсная матрица", req: 2 },
    { key: "domesurvival.stage05.stabilizer.v73",   id: "domesurvival:machine_stabilizer",             name: "Машинный стабилизатор", req: 1 },
    { key: "domesurvival.stage05.pipe.v73",         id: "domesurvival:oxygen_pipe",                    name: "Кислородная труба", req: 16 },
    { key: "domesurvival.stage05.tank.v73",         id: "domesurvival:small_oxygen_tank",              name: "Малый кислородный баллон", req: 2 },
    { key: "domesurvival.stage05.reservoir.v73",    id: "domesurvival:universal_tank",                 name: "Универсальный резервуар", req: 1 },
    { key: "domesurvival.stage05.mekcircuit.v73",   id: "mekanism:advanced_control_circuit",           name: "Продвинутая схема управления", req: 2 },
    { key: "domesurvival.stage05.mekalloy.v73",     id: "mekanism:alloy_reinforced",                   name: "Укреплённый сплав", req: 4 },
    { key: "domesurvival.stage05.iesteel.v73",      id: "immersiveengineering:component_steel",        name: "Стальной механический компонент", req: 4 },
    { key: "domesurvival.stage05.ieadvanced.v73",   id: "immersiveengineering:circuit_board",name: "Электронная лампа_adv", req: 2 },
    { key: "domesurvival.stage05.darksteel.v73",    id: "enderio:dark_steel_ingot",                    name: "Слиток тёмной стали", req: 6 }
];

/* Stage 06 - First controlled outside expedition after oxygen is ready. */
var S6_COMPLETE = "domesurvival.stage06.complete.v71";
var S6 = [
    { key: "domesurvival.stage06.rawiron.v73",    id: "minecraft:raw_iron",                        name: "Необработанное железо", req: 32 },
    { key: "domesurvival.stage06.rawcopper.v73",  id: "minecraft:raw_copper",                      name: "Необработанная медь", req: 48 },
    { key: "domesurvival.stage06.gold.v73",       id: "minecraft:gold_ingot",                      name: "Золотой слиток", req: 12 },
    { key: "domesurvival.stage06.diamond.v73",    id: "minecraft:diamond",                         name: "Алмаз", req: 4 },
    { key: "domesurvival.stage06.tomato.v73",     id: "farmersdelight:tomato",                     name: "Томат (Farmer's Delight)", req: 16 },
    { key: "domesurvival.stage06.cabbage.v73",    id: "farmersdelight:cabbage",                    name: "Капуста (Farmer's Delight)", req: 16 },
    { key: "domesurvival.stage06.onion.v73",      id: "farmersdelight:onion",                      name: "Лук (Farmer's Delight)", req: 16 },
    { key: "domesurvival.stage06.jerky.v73",      id: "brewinandchewin:jerky",                     name: "Вяленое мясо (Brewin' And Chewin')", req: 8 },
    { key: "domesurvival.stage06.steelplate.v73", id: "immersiveengineering:plate_steel",          ids: ["ad_astra:steel_plate", "immersiveengineering:plate_steel", "mekanism:plate_steel", "thermal:steel_plate"], tag: "forge:plates/steel", name: "Стальная пластина", req: 8 },
    { key: "domesurvival.stage06.meksteel.v73",   id: "mekanism:ingot_steel",                      ids: ["ad_astra:steel_ingot", "mekanism:ingot_steel", "immersiveengineering:ingot_steel", "thermal:steel_ingot"], tag: "forge:ingots/steel", name: "Стальной слиток", req: 8 }
];

/* Stage 07 - Internal logistics with a mix of DomeSurvival and industrial components. */
var S7_COMPLETE = "domesurvival.stage07.complete.v71";
var S7 = [
    { key: "domesurvival.stage07.itempipe.v73",    id: "domesurvival:copper_item_pipe",              name: "Медная транспортная труба", req: 16 },
    { key: "domesurvival.stage07.filterpipe.v73",  id: "domesurvival:filtering_item_pipe",           name: "Фильтрующая транспортная труба", req: 4 },
    { key: "domesurvival.stage07.hopper.v73",      id: "domesurvival:copper_hopper",                 name: "Медная воронка", req: 4 },
    { key: "domesurvival.stage07.energypipe.v73",  id: "domesurvival:basic_energy_pipe",             name: "Энерготруба I уровня", req: 8 },
    { key: "domesurvival.stage07.fluidpipe.v73",   id: "domesurvival:basic_fluid_pipe",              name: "Базовая жидкостная труба", req: 8 },
    { key: "domesurvival.stage07.o2pipe.v73",      id: "domesurvival:oxygen_pipe",                   name: "Кислородная труба", req: 8 },
    { key: "domesurvival.stage07.configurator.v73",id: "mekanism:configurator",                      name: "Конфигуратор", req: 1 },
    { key: "domesurvival.stage07.mekalloy.v73",    id: "mekanism:alloy_infused",                     name: "Наполненный сплав", req: 8 },
    { key: "domesurvival.stage07.electronic.v73",  id: "immersiveengineering:electron_tube",  name: "Электронная лампа", req: 4 },
    { key: "domesurvival.stage07.darksteel.v73",   id: "enderio:dark_steel_ingot",                   name: "Слиток тёмной стали", req: 4 }
];

/* Stage 08 - Base reserve: hardware + food reserve + fermentation/morale reserve. */
var S8_COMPLETE = "domesurvival.stage08.complete.v71";
var S8 = [
    { key: "domesurvival.stage08.buffer.v73",      id: "domesurvival:energy_buffer",             name: "Энергоблок серии «Сталь»", req: 1 },
    { key: "domesurvival.stage08.tank.v73",        id: "domesurvival:universal_tank",            name: "Универсальный резервуар", req: 2 },
    { key: "domesurvival.stage08.steelpipe.v73",   id: "domesurvival:steel_item_pipe",           name: "Стальная транспортная труба", req: 8 },
    { key: "domesurvival.stage08.steelhop.v73",    id: "domesurvival:steel_hopper",              name: "Стальная воронка", req: 2 },
    { key: "domesurvival.stage08.energy2.v73",     id: "domesurvival:reinforced_energy_pipe",    name: "Усиленная энерготруба II уровня", req: 4 },
    { key: "domesurvival.stage08.mekalloy.v73",    id: "mekanism:alloy_reinforced",              name: "Укреплённый сплав", req: 8 },
    { key: "domesurvival.stage08.rice.v73",        id: "farmersdelight:rice",                     name: "Рис", req: 32 },
    { key: "domesurvival.stage08.keg.v73",         id: "brewinandchewin:keg",                    name: "Бочонок", req: 1 },
    { key: "domesurvival.stage08.beer.v73",        id: "brewinandchewin:beer",                   name: "Пиво", req: 8 },
    { key: "domesurvival.stage08.mead.v73",        id: "brewinandchewin:mead",                   name: "Медовуха", req: 4 }
];


/* Stage 09 - Program Exodus: long-range communications and protected control network. */
var S9_COMPLETE = "domesurvival.stage09.complete.v74";
var S9 = [
    { key: "domesurvival.stage09.radio.v74", id: "ad_astra:radio", name: "Радиостанция (Ad Astra)", req: 2 },
    { key: "domesurvival.stage09.cable.v74", id: "ad_astra:steel_cable", name: "Стальной кабель (Ad Astra)", req: 16 },
    { key: "domesurvival.stage09.press.v74", id: "thermal:machine_press", name: "Механический пресс (Thermal)", req: 1 },
    { key: "domesurvival.stage09.circuit.v74", id: "mekanism:advanced_control_circuit", name: "Продвинутая схема управления (Mekanism)", req: 4 },
    { key: "domesurvival.stage09.alloy.v74", id: "mekanism:alloy_reinforced", name: "Укреплённый сплав (Mekanism)", req: 4 },
    { key: "domesurvival.stage09.board.v74", id: "immersiveengineering:circuit_board", name: "Печатная плата (Immersive Engineering)", req: 4 },
    { key: "domesurvival.stage09.darksteel.v74", id: "enderio:dark_steel_ingot", name: "Слиток тёмной стали (Ender IO)", req: 8 },
    { key: "domesurvival.stage09.energy.v74", id: "domesurvival:reinforced_energy_pipe", name: "Усиленная энерготруба II уровня", req: 8 }
];

var S10_COMPLETE = "domesurvival.stage10.complete.v74";
var S10 = [
    { key: "domesurvival.stage10.genericsteel.v75", id: "ad_astra:steel_ingot", ids: ["ad_astra:steel_ingot", "mekanism:ingot_steel", "immersiveengineering:ingot_steel", "thermal:steel_ingot"], tag: "forge:ingots/steel", name: "Стальной слиток", req: 48 },
    { key: "domesurvival.stage10.genericplate.v75", id: "ad_astra:steel_plate", ids: ["ad_astra:steel_plate", "immersiveengineering:plate_steel", "mekanism:plate_steel", "thermal:steel_plate"], tag: "forge:plates/steel", name: "Стальная пластина", req: 36 },
    { key: "domesurvival.stage10.tank.v74", id: "ad_astra:steel_tank", name: "Стальной бак (Ad Astra)", req: 2 },
    { key: "domesurvival.stage10.fan.v74", id: "ad_astra:engine_fan", name: "Вентилятор двигателя (Ad Astra)", req: 2 },
    { key: "domesurvival.stage10.alloy.v75", id: "mekanism:alloy_reinforced", name: "Укреплённый сплав (Mekanism)", req: 8 },
    { key: "domesurvival.stage10.darksteel.v74", id: "enderio:dark_steel_ingot", name: "Слиток тёмной стали (Ender IO)", req: 8 },
    { key: "domesurvival.stage10.obsidian.v75", id: "minecraft:obsidian", name: "Обсидиан", req: 16 }
];

var S11_COMPLETE = "domesurvival.stage11.complete.v74";
var S11 = [
    { key: "domesurvival.stage11.engine.v74", id: "ad_astra:steel_engine", name: "Стальной ракетный двигатель (Ad Astra)", req: 1 },
    { key: "domesurvival.stage11.fin.v74", id: "ad_astra:rocket_fin", name: "Ракетный стабилизатор (Ad Astra)", req: 4 },
    { key: "domesurvival.stage11.nose.v74", id: "ad_astra:rocket_nose_cone", name: "Носовой обтекатель ракеты (Ad Astra)", req: 1 },
    { key: "domesurvival.stage11.ogear.v74", id: "ad_astra:oxygen_gear", name: "Кислородное снаряжение (Ad Astra)", req: 1 },
    { key: "domesurvival.stage11.otank.v74", id: "ad_astra:oxygen_tank", name: "Кислородный баллон (Ad Astra)", req: 2 },
    { key: "domesurvival.stage11.gastank.v74", id: "ad_astra:large_gas_tank", name: "Большой газовый баллон (Ad Astra)", req: 1 },
    { key: "domesurvival.stage11.press.v74", id: "thermal:machine_press", name: "Механический пресс (Thermal)", req: 1 },
    { key: "domesurvival.stage11.circuit.v74", id: "mekanism:advanced_control_circuit", name: "Продвинутая схема управления (Mekanism)", req: 4 },
    { key: "domesurvival.stage11.board.v74", id: "immersiveengineering:circuit_board", name: "Печатная плата (Immersive Engineering)", req: 4 },
    { key: "domesurvival.stage11.obsidian.v75", id: "minecraft:obsidian", name: "Обсидиан", req: 16 }
];

var S12_COMPLETE = "domesurvival.stage12.complete.v74";
var S12 = [
    { key: "domesurvival.stage12.radio.v75", id: "ad_astra:radio", name: "Радиостанция (Ad Astra)", req: 1 },
    { key: "domesurvival.stage12.cable.v75", id: "ad_astra:steel_cable", name: "Стальной кабель (Ad Astra)", req: 32 },
    { key: "domesurvival.stage12.genericplate.v75", id: "ad_astra:steel_plate", ids: ["ad_astra:steel_plate", "immersiveengineering:plate_steel", "mekanism:plate_steel", "thermal:steel_plate"], tag: "forge:plates/steel", name: "Стальная пластина", req: 32 },
    { key: "domesurvival.stage12.refinery.v75", id: "thermal:machine_refinery", name: "Фракционирующий перегонный аппарат (Thermal)", req: 1 },
    { key: "domesurvival.stage12.energy.v75", id: "domesurvival:high_voltage_energy_pipe", name: "Высоковольтная энерготруба III уровня", req: 12 },
    { key: "domesurvival.stage12.buffer.v75", id: "domesurvival:energy_buffer_titan", name: "Энергоблок серии «Титан»", req: 1 },
    { key: "domesurvival.stage12.board.v75", id: "immersiveengineering:circuit_board", name: "Печатная плата (Immersive Engineering)", req: 6 },
    { key: "domesurvival.stage12.darksteel.v75", id: "enderio:dark_steel_ingot", name: "Слиток тёмной стали (Ender IO)", req: 12 },
    { key: "domesurvival.stage12.blaze.v75", id: "minecraft:blaze_powder", name: "Огненный порошок", req: 32 }
];

/* One-time shared base reward packages. V73 keys intentionally reissue the revised multimod packages once. */
var R1_KEY = "domesurvival.stage01.reward.v73";
var R1 = [
    { id: "farmersdelight:stove", count: 1, name: "Плита (Farmer's Delight)" },
    { id: "farmersdelight:cabbage_seeds", count: 4, name: "Семена капусты (Farmer's Delight)" },
    { id: "farmersdelight:tomato_seeds", count: 4, name: "Семена томата (Farmer's Delight)" },
    { id: "minecraft:bread", count: 8, name: "Хлеб" }
];

var R2_KEY = "domesurvival.stage02.reward.v73";
var R2 = [
    { id: "domesurvival:machine_wrench", count: 1, name: "Ключ инженера" },
    { id: "immersiveengineering:hammer", count: 1, name: "Молот инженера (Immersive Engineering)" },
    { id: "minecraft:iron_pickaxe", count: 1, name: "Железная кирка" },
    { id: "minecraft:lantern", count: 8, name: "Фонарь" },
    { id: "minecraft:chest", count: 4, name: "Сундук" }
];

var R3_KEY = "domesurvival.stage03.reward.v73";
var R3 = [
    { id: "minecraft:coal", count: 32, name: "Уголь" },
    { id: "minecraft:redstone", count: 16, name: "Редстоун" },
    { id: "minecraft:iron_ingot", count: 16, name: "Железный слиток" },
    { id: "minecraft:torch", count: 32, name: "Факел" }
];

var R4_KEY = "domesurvival.stage04.reward.v733";
var R4 = [
    { id: "domesurvival:reinforced_fluid_pipe", count: 12, name: "Усиленная жидкостная труба" },
    { id: "farmersdelight:rich_soil", count: 8, name: "Плодородная почва (Farmer's Delight)" },
    { id: "farmersdelight:oak_cabinet", count: 2, name: "Дубовая кладовая (Farmer's Delight)" },
    { id: "farmersdelight:straw_bale", count: 4, name: "Тюк соломы (Farmer's Delight)" },
    { id: "minecraft:water_bucket", count: 2, name: "Ведро воды" }
];

var R5_KEY = "domesurvival.stage05.reward.v733";
var R5 = [
    { id: "domesurvival:oxygen_mask", count: 2, name: "Кислородная маска" },
    { id: "domesurvival:medium_oxygen_tank", count: 2, name: "Средний кислородный баллон" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 12, name: "Усиленная кислородная труба" },
    { id: "mekanism:energy_tablet", count: 1, name: "Энергетический планшет (Mekanism)" },
    { id: "immersiveengineering:component_steel", count: 4, name: "Стальной механический компонент (Immersive Engineering)" },
    { id: "minecraft:cooked_beef", count: 16, name: "Стейк" }
];

var R6_KEY = "domesurvival.stage06.reward.v73";
var R6 = [
    { id: "domesurvival:surface_suit_helmet", count: 1, name: "Шлем защитного костюма" },
    { id: "domesurvival:surface_suit_chestplate", count: 1, name: "Куртка защитного костюма" },
    { id: "domesurvival:surface_suit_leggings", count: 1, name: "Штаны защитного костюма" },
    { id: "domesurvival:surface_suit_boots", count: 1, name: "Ботинки защитного костюма" },
    { id: "domesurvival:large_oxygen_tank", count: 1, name: "Большой кислородный баллон" },
    { id: "brewinandchewin:tankard", count: 2, name: "Кружка (Brewin' And Chewin')" }
];

var R7_KEY = "domesurvival.stage07.reward.v73";
var R7 = [
    { id: "domesurvival:filtering_item_pipe", count: 4, name: "Фильтрующая транспортная труба" },
    { id: "domesurvival:reinforced_fluid_pipe", count: 8, name: "Усиленная жидкостная труба" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 8, name: "Усиленная кислородная труба" },
    { id: "minecraft:barrel", count: 8, name: "Бочка" },
    { id: "minecraft:hopper", count: 4, name: "Воронка" }
];

var R8_KEY = "domesurvival.stage08.reward.v73";
var R8 = [
    { id: "domesurvival:large_oxygen_tank", count: 2, name: "Большой кислородный баллон" },
    { id: "domesurvival:filtering_item_pipe", count: 4, name: "Фильтрующая транспортная труба" },
    { id: "brewinandchewin:mead", count: 4, name: "Медовуха (Brewin' And Chewin')" },
    { id: "minecraft:golden_apple", count: 4, name: "Золотое яблоко" },
    { id: "minecraft:ender_pearl", count: 8, name: "Эндер-жемчуг" }
];


var R9_KEY = "domesurvival.stage09.reward.v74";
var R9 = [
    { id: "ad_astra:hammer", count: 1, name: "Молот (Ad Astra)" },
    { id: "minecraft:spyglass", count: 1, name: "Подзорная труба" },
    { id: "minecraft:compass", count: 1, name: "Компас" },
    { id: "minecraft:clock", count: 1, name: "Часы" },
    { id: "minecraft:ender_pearl", count: 8, name: "Эндер-жемчуг" }
];

var R10_KEY = "domesurvival.stage10.reward.v74";
var R10 = [
    { id: "mekanism:configurator", count: 1, name: "Конфигуратор (Mekanism)" },
    { id: "immersiveengineering:hammer", count: 1, name: "Молот инженера (Immersive Engineering)" },
    { id: "minecraft:diamond_pickaxe", count: 1, name: "Алмазная кирка" },
    { id: "minecraft:golden_apple", count: 4, name: "Золотое яблоко" },
    { id: "minecraft:experience_bottle", count: 16, name: "Пузырёк опыта" }
];

var R11_KEY = "domesurvival.stage11.reward.v74";
var R11 = [
    { id: "domesurvival:large_oxygen_tank", count: 2, name: "Большой кислородный баллон" },
    { id: "brewinandchewin:jerky", count: 16, name: "Вяленое мясо (Brewin' And Chewin')" },
    { id: "minecraft:golden_apple", count: 8, name: "Золотое яблоко" },
    { id: "minecraft:ender_pearl", count: 16, name: "Эндер-жемчуг" },
    { id: "minecraft:firework_rocket", count: 16, name: "Фейерверк" }
];

var R12_KEY = "domesurvival.stage12.reward.v74";
var R12 = [
    { id: "ad_astra:hammer", count: 1, name: "Молот (Ad Astra)" },
    { id: "ad_astra:oxygen_gear", count: 1, name: "Кислородное снаряжение (Ad Astra)" },
    { id: "ad_astra:oxygen_tank", count: 2, name: "Кислородный баллон (Ad Astra)" },
    { id: "domesurvival:high_voltage_energy_pipe", count: 8, name: "Высоковольтная энерготруба III уровня" },
    { id: "farmersdelight:roast_chicken_block", count: 1, name: "Жареная курица (Farmer's Delight)" },
    { id: "brewinandchewin:jerky", count: 16, name: "Вяленое мясо (Brewin' And Chewin')" },
    { id: "minecraft:golden_apple", count: 8, name: "Золотое яблоко" }
];

var lastInteractByPlayer = {};
var lastNpcPosByPlayer = {};


/* Visible native-button workaround for the 1.20.1 unofficial CustomNPCs port.
   The button remains the clickable component; a normal GUI label is rendered
   after it so the caption is visible even when the port does not draw IButton labels. */
function addVisibleButton(gui, buttonId, text, x, y, width, height, labelId) {
    gui.addButton(buttonId, "", x, y, width, height);

    var label = gui.addLabel(
        labelId,
        text,
        x,
        y + Math.max(4, Math.floor((height - 9) / 2)),
        width,
        10,
        0xFFFFFF
    );

    /* Newer API builds support centered labels. The unofficial port may not,
       so keep this optional. The caption still remains visible if it fails. */
    try {
        label.setAlignment(1);
    } catch (ignoredAlignment) {
    }

    return label;
}

function init(e) {
    try { e.npc.getDisplay().setName("Джозеф Куппер"); } catch (ignored) {}
    try { e.npc.getDisplay().setTitle("Координатор купола"); } catch (ignored) {}
    try { e.npc.getDisplay().setSkinTexture("domesurvival:textures/npc/joseph_cooper.png"); } catch (ignored) {}
    try { e.npc.getDisplay().setShowName(0); } catch (ignored) {}
    try { e.npc.getAi().setMovingType(0); } catch (ignored) {}
    try { e.npc.getAi().setReturnsHome(true); } catch (ignored) {}
    try { e.npc.getAi().setRetaliateType(3); } catch (ignored) {}
    try { e.npc.getAi().setInteractWithNPCs(false); } catch (ignored) {}
    try { e.npc.updateClient(); } catch (ignored) {}
}

function interact(e) {
    /* DOMESURVIVAL_V741_CANCEL_INTERACT
       Critical: prevent CustomNPCs from falling through to Advanced -> Interact Lines. */
    try { e.setCanceled(true); } catch (ignoredCancel) {}
    e.setCanceled(true);
    var playerName = String(e.player.getName());
    var now = new Date().getTime();
    var previous = lastInteractByPlayer[playerName];
    if (previous != null && (now - previous) < 250) return;
    lastInteractByPlayer[playerName] = now;

    try {
        lastNpcPosByPlayer[playerName] = { x: Number(e.npc.getX()), y: Number(e.npc.getY()), z: Number(e.npc.getZ()) };
    } catch (ignoredPos) {}

    migrateV75Balance(e.player);

    /* Existing saves which already finished the original workshop are treated as having finished Stage 01. */
    migrateStage1IfNeeded(e.player);
    grantPendingRewards(e.player);
    ensureStage1PathUpgrade(e.player);

    /* Self-heal saves where the old /josephscript nextstage completed Stage 02
       without placing its physical workshop reward. */
    if (stage2Complete(e.player) && !workshopBuilt()) {
        var workshopRepair = finalizeWorkshopIfReady(e.player);
        if (workshopRepair != null && workshopRepair.length > 0) {
            try { e.player.message("§e[КУПОЛ] " + workshopRepair); } catch (ignoredRepairMessage) {}
        }
    }
    openMain(e.player);
}

function customGuiButton(e) {
    migrateV75Balance(e.player);
    if (e.buttonId == BTN_PROJECT) { openProject(e.player, ""); return; }
    if (e.buttonId == BTN_BASE) { openBase(e.player); return; }
    if (e.buttonId == BTN_PLAN) { openPlan(e.player); return; }
    if (e.buttonId == BTN_CLOSE) { e.player.closeGui(); return; }
    if (e.buttonId == BTN_BACK) { openMain(e.player); return; }
    if (e.buttonId != BTN_CONTRIBUTE) return;

    migrateStage1IfNeeded(e.player);

    if (!stage1Complete(e.player)) {
        var before1 = stage1Complete(e.player);
        var notice1 = contributeSet(e.player, S1, S1_COMPLETE);
        var after1 = stage1Complete(e.player);
        var notices1 = [notice1];
        if (!before1 && after1) {
            var pathNotice1 = ensureStage1PathUpgrade(e.player);
            if (pathNotice1 != null && pathNotice1.length > 0) notices1.push(pathNotice1);
            notices1.push(grantStageReward(e.player, R1_KEY, R1));
            celebrate(e.player, "§a[КУПОЛ] Этап 01 «Жизнеобеспечение купола» завершён!", "§e[КУПОЛ] Доступен этап 02: «Восстановление мастерской».");
        }
        openProject(e.player, compactNotice(notices1));
        return;
    }

    if (!stage2Complete(e.player)) {
        var before2 = stage2Complete(e.player);
        var notices2 = [];
        if (logicalResetActive(e.player)) {
            var resetCoreNotice = contributeSet(e.player, S2_RESET_CORE, S2_RESET_CORE_COMPLETE);
            if (resetCoreNotice != null && resetCoreNotice.length > 0) notices2.push(resetCoreNotice);
        } else if (!workshopCoreComplete()) {
            try {
                var bridgeNotice = String(Bridge.contributeWorkshop(e.player.getName()));
                if (bridgeNotice != null && bridgeNotice.length > 0) notices2.push(bridgeNotice);
            } catch (ignoredCore) {}
        }
        var extraNotice = contributeSet(e.player, S2, S2_EXTRA_COMPLETE);
        if (extraNotice != null && extraNotice.length > 0) notices2.push(extraNotice);
        var after2 = stage2Complete(e.player);
        if (after2) {
            var buildNotice = finalizeWorkshopIfReady(e.player);
            if (buildNotice != null && buildNotice.length > 0) notices2.push(buildNotice);
        }
        if (!before2 && after2) {
            notices2.push(grantStageReward(e.player, R2_KEY, R2));
            celebrate(e.player, "§a[КУПОЛ] Этап 02 «Восстановление мастерской» завершён!", "§e[КУПОЛ] Доступен этап 03: «Аварийное энергоснабжение».");
        }
        openProject(e.player, compactNotice(notices2));
        return;
    }

    if (!stage3Complete(e.player)) { completeSimpleStage(e.player, S3, S3_COMPLETE, R3_KEY, R3, "§a[КУПОЛ] Этап 03 «Аварийное энергоснабжение» завершён!", "§e[КУПОЛ] Доступен этап 04: «Вода и агроконтур»."); return; }
    if (!stage4Complete(e.player)) { completeSimpleStage(e.player, S4, S4_COMPLETE, R4_KEY, R4, "§a[КУПОЛ] Этап 04 «Вода и агроконтур» завершён!", "§e[КУПОЛ] Доступен этап 05: «Кислородный контур»."); return; }
    if (!stage5Complete(e.player)) { completeSimpleStage(e.player, S5, S5_COMPLETE, R5_KEY, R5, "§a[КУПОЛ] Этап 05 «Кислородный контур» завершён!", "§e[КУПОЛ] Доступен этап 06: «Первая внешняя экспедиция»."); return; }
    if (!stage6Complete(e.player)) { completeSimpleStage(e.player, S6, S6_COMPLETE, R6_KEY, R6, "§a[КУПОЛ] Этап 06 «Первая внешняя экспедиция» завершён!", "§e[КУПОЛ] Доступен этап 07: «Логистика купола»."); return; }
    if (!stage7Complete(e.player)) { completeSimpleStage(e.player, S7, S7_COMPLETE, R7_KEY, R7, "§a[КУПОЛ] Этап 07 «Логистика купола» завершён!", "§e[КУПОЛ] Доступен этап 08: «Аварийный резерв базы»."); return; }
    if (!stage8Complete(e.player)) { completeSimpleStage(e.player, S8, S8_COMPLETE, R8_KEY, R8, "§a[КУПОЛ] Этап 08 «Аварийный резерв базы» завершён!", "§e[КУПОЛ] Открыта программа «Исход»: этап 09 «Дальняя связь»."); return; }
    if (!stage9Complete(e.player)) { completeSimpleStage(e.player, S9, S9_COMPLETE, R9_KEY, R9, "§a[КУПОЛ] Этап 09 «Дальняя связь» завершён!", "§e[КУПОЛ] Доступен этап 10: «Ракетные материалы»."); return; }
    if (!stage10Complete(e.player)) { completeSimpleStage(e.player, S10, S10_COMPLETE, R10_KEY, R10, "§a[КУПОЛ] Этап 10 «Ракетные материалы» завершён!", "§e[КУПОЛ] Доступен этап 11: «Ракетный модуль»."); return; }
    if (!stage11Complete(e.player)) { completeSimpleStage(e.player, S11, S11_COMPLETE, R11_KEY, R11, "§a[КУПОЛ] Этап 11 «Ракетный модуль» завершён!", "§e[КУПОЛ] Доступен этап 12: «Предстартовая готовность»."); return; }
    if (!stage12Complete(e.player)) { completeSimpleStage(e.player, S12, S12_COMPLETE, R12_KEY, R12, "§a[КУПОЛ] Этап 12 «Предстартовая готовность» завершён!", "§e[КУПОЛ] Земная часть программы «Исход» завершена. Следующая глава — Луна."); return; }

    openProject(e.player, "Земная часть программы «Исход» завершена.");
}

function getStored(player) {
    try { return player.getWorld().getStoreddata(); } catch (ignored) { return null; }
}

function readInt(data, key) {
    if (data == null) return 0;
    try {
        var raw = data.get(key);
        if (raw == null || String(raw).length == 0) return 0;
        var value = Math.floor(Number(raw));
        if (isNaN(value) || value < 0) return 0;
        return value;
    } catch (ignored) { return 0; }
}

function writeInt(data, key, value) {
    if (data == null) return;
    try { data.put(key, String(Math.max(0, Math.floor(value)))); } catch (ignored) {}
}


function rewardAlreadyClaimed(player, rewardKey) {
    return readInt(getStored(player), rewardKey) > 0;
}

function grantStageReward(player, rewardKey, rewards) {
    var data = getStored(player);
    if (data == null) return "Награда подготовлена, но хранилище прогресса временно недоступно.";
    if (readInt(data, rewardKey) > 0) return "";

    var granted = [];
    var failed = [];
    var allDone = true;

    for (var i = 0; i < rewards.length; i++) {
        var reward = rewards[i];
        var itemKey = rewardKey + ".item." + i;
        if (readInt(data, itemKey) > 0) continue;

        var ok = false;
        try { ok = !!player.giveItem(reward.id, reward.count); } catch (ignoredGiveById) {}
        if (!ok) {
            try {
                var stack = API.createItem(reward.id, 0, reward.count);
                ok = !!player.giveItem(stack);
            } catch (ignoredGiveStack) {}
        }

        if (ok) {
            writeInt(data, itemKey, 1);
            granted.push(questItemName(reward) + " x" + reward.count);
        } else {
            allDone = false;
            failed.push(questItemName(reward));
        }
    }

    for (var j = 0; j < rewards.length; j++) {
        if (readInt(data, rewardKey + ".item." + j) <= 0) {
            allDone = false;
            break;
        }
    }

    if (allDone) writeInt(data, rewardKey, 1);

    if (failed.length > 0) {
        return "Не удалось выдать: " + failed.join(", ") + ". Освободи место в инвентаре и снова открой Джозефа.";
    }
    if (granted.length > 0) return "Награда базы: " + granted.join(", ");
    return "";
}

function completeSimpleStage(player, defs, completeKey, rewardKey, rewards, message1, message2) {
    var before = setState(player, defs, completeKey).complete;
    var notice = contributeSet(player, defs, completeKey);
    var after = setState(player, defs, completeKey).complete;
    var parts = [notice];

    if (!before && after) {
        parts.push(grantStageReward(player, rewardKey, rewards));
        celebrate(player, message1, message2);
    }

    openProject(player, compactNotice(parts));
}


function workshopCoreComplete() {
    try { return !!Bridge.workshopComplete(); } catch (ignored) { return false; }
}

function workshopBuilt() {
    try { return !!Bridge.workshopBuilt(); } catch (ignored) { return false; }
}

function finalizeWorkshopIfReady(player) {
    if (logicalResetActive(player)) return "";
    if (!stage2Complete(player)) return "";
    if (workshopBuilt()) return "";

    try {
        var result = String(Bridge.finalizeWorkshop(player.getName()));
        return result == null ? "" : result;
    } catch (ignoredFinalize) {
        return "";
    }
}

function setState(player, defs, completeKey) {
    var data = getStored(player);
    var values = [];
    var complete = true;
    for (var i = 0; i < defs.length; i++) {
        var value = Math.min(defs[i].req, readInt(data, defs[i].key));
        values.push(value);
        if (value < defs[i].req) complete = false;
    }
    var flag = readInt(data, completeKey) > 0;
    complete = complete || flag;
    if (complete && !flag) writeInt(data, completeKey, 1);
    return { data: data, values: values, complete: complete };
}

function fillSet(player, defs, completeKey) {
    var data = getStored(player);
    for (var i = 0; i < defs.length; i++) writeInt(data, defs[i].key, defs[i].req);
    writeInt(data, completeKey, 1);
}

function logicalResetActive(player) {
    var data = getStored(player);
    return readInt(data, RELEASE_RESET_LOCK) > 0;
}

function migrateStage1IfNeeded(player) {
    if (logicalResetActive(player)) return;
    if (!workshopCoreComplete()) return;
    var state = setState(player, S1, S1_COMPLETE);
    if (!state.complete) fillSet(player, S1, S1_COMPLETE);
}

function stage1Complete(player) {
    migrateStage1IfNeeded(player);
    return setState(player, S1, S1_COMPLETE).complete;
}

function stage2ExtrasComplete(player) {
    return setState(player, S2, S2_EXTRA_COMPLETE).complete;
}

function stage2CoreComplete(player) {
    if (logicalResetActive(player)) return setState(player, S2_RESET_CORE, S2_RESET_CORE_COMPLETE).complete;
    return workshopCoreComplete();
}

function stage2Complete(player) {
    return stage1Complete(player) && stage2CoreComplete(player) && stage2ExtrasComplete(player);
}

function stage3Complete(player) {
    if (!stage2Complete(player)) return false;
    return setState(player, S3, S3_COMPLETE).complete;
}
function stage4Complete(player) {
    if (!stage3Complete(player)) return false;
    return setState(player, S4, S4_COMPLETE).complete;
}
function stage5Complete(player) {
    if (!stage4Complete(player)) return false;
    return setState(player, S5, S5_COMPLETE).complete;
}
function stage6Complete(player) {
    if (!stage5Complete(player)) return false;
    return setState(player, S6, S6_COMPLETE).complete;
}
function stage7Complete(player) {
    if (!stage6Complete(player)) return false;
    return setState(player, S7, S7_COMPLETE).complete;
}
function stage8Complete(player) {
    if (!stage7Complete(player)) return false;
    return setState(player, S8, S8_COMPLETE).complete;
}
function stage9Complete(player) {
    if (!stage8Complete(player)) return false;
    return setState(player, S9, S9_COMPLETE).complete;
}
function stage10Complete(player) {
    if (!stage9Complete(player)) return false;
    return setState(player, S10, S10_COMPLETE).complete;
}
function stage11Complete(player) {
    if (!stage10Complete(player)) return false;
    return setState(player, S11, S11_COMPLETE).complete;
}
function stage12Complete(player) {
    if (!stage11Complete(player)) return false;
    return setState(player, S12, S12_COMPLETE).complete;
}

function grantPendingRewards(player) {
    var messages = [];
    var result = "";
    if (stage1Complete(player)) { result = grantStageReward(player, R1_KEY, R1); if (result.length > 0) messages.push(result); }
    if (stage2Complete(player)) { result = grantStageReward(player, R2_KEY, R2); if (result.length > 0) messages.push(result); }
    if (stage3Complete(player)) { result = grantStageReward(player, R3_KEY, R3); if (result.length > 0) messages.push(result); }
    if (stage4Complete(player)) { result = grantStageReward(player, R4_KEY, R4); if (result.length > 0) messages.push(result); }
    if (stage5Complete(player)) { result = grantStageReward(player, R5_KEY, R5); if (result.length > 0) messages.push(result); }
    if (stage6Complete(player)) { result = grantStageReward(player, R6_KEY, R6); if (result.length > 0) messages.push(result); }
    if (stage7Complete(player)) { result = grantStageReward(player, R7_KEY, R7); if (result.length > 0) messages.push(result); }
    if (stage8Complete(player)) { result = grantStageReward(player, R8_KEY, R8); if (result.length > 0) messages.push(result); }
    if (stage9Complete(player)) { result = grantStageReward(player, R9_KEY, R9); if (result.length > 0) messages.push(result); }
    if (stage10Complete(player)) { result = grantStageReward(player, R10_KEY, R10); if (result.length > 0) messages.push(result); }
    if (stage11Complete(player)) { result = grantStageReward(player, R11_KEY, R11); if (result.length > 0) messages.push(result); }
    if (stage12Complete(player)) { result = grantStageReward(player, R12_KEY, R12); if (result.length > 0) messages.push(result); }
    for (var i = 0; i < messages.length; i++) {
        try { player.message("§a[КУПОЛ] " + messages[i]); } catch (ignoredRewardMessage) {}
    }
}

var STAGE1_PATH_UPGRADE_KEY = "domesurvival.stage01.path_upgraded.v733";

function ensureStage1PathUpgrade(player) {
    if (!stage1Complete(player)) return "";

    var data = getStored(player);
    if (data == null) return "";
    if (readInt(data, STAGE1_PATH_UPGRADE_KEY) > 0) return "";

    var playerName = String(player.getName());
    var pos = lastNpcPosByPlayer[playerName];
    if (pos == null) return "";

    try {
        var changed = Number(Bridge.upgradeStage1Path(playerName, pos.x, pos.y, pos.z));

        if (changed >= 0) {
            writeInt(data, STAGE1_PATH_UPGRADE_KEY, 1);

            if (changed > 0) {
                try {
                    player.message("§a[КУПОЛ] Земляная дорога к выходу приведена в порядок: создана постоянная тропа.");
                } catch (ignoredPathMessage) {}
                return "Тропа к выходу благоустроена.";
            }

            return "";
        }
    } catch (ignoredPathUpgrade) {}

    return "";
}

function inventoryCount(player, id) {
    try { return Math.max(0, Number(player.inventoryItemCount(id))); } catch (ignoredOld) {}
    try {
        var stack = API.createItem(id, 0, 1);
        return Math.max(0, Number(player.getInventory().count(stack, true, true)));
    } catch (ignoredNew) {}
    return 0;
}

function v75PushUnique(list, value) {
    if (value == null) return;
    var textValue = String(value);
    if (textValue.length == 0) return;
    for (var i = 0; i < list.length; i++) {
        if (String(list[i]) == textValue) return;
    }
    list.push(textValue);
}

function v75IdsFromForgeTag(tagId) {
    var tagName = String(tagId);
    if (V75_ITEM_TAG_CACHE[tagName] != null) return V75_ITEM_TAG_CACHE[tagName];

    var result = [];
    try {
        var key = V75TagKey.create(V75Registries.ITEM, new V75ResourceLocation(tagName));
        var manager = V75ForgeRegistries.ITEMS.tags();
        var tag = manager.getTag(key);
        var iterator = V75ForgeRegistries.ITEMS.getValues().iterator();
        while (iterator.hasNext()) {
            var item = iterator.next();
            if (!tag.contains(item)) continue;
            var itemId = V75ForgeRegistries.ITEMS.getKey(item);
            if (itemId != null) v75PushUnique(result, String(itemId));
        }
    } catch (ignoredForgeTag) {
        /* Exact-id fallbacks below keep the quest usable on unusual ports. */
    }

    V75_ITEM_TAG_CACHE[tagName] = result;
    return result;
}

function acceptedIds(def) {
    var result = [];

    if (def.tag != null && String(def.tag).length > 0) {
        var tagged = v75IdsFromForgeTag(def.tag);
        for (var t = 0; t < tagged.length; t++) v75PushUnique(result, tagged[t]);
    }

    if (def.ids != null) {
        for (var i = 0; i < def.ids.length; i++) v75PushUnique(result, def.ids[i]);
    }

    if (def.id != null) v75PushUnique(result, def.id);
    return result;
}

function takeItem(player, state, def, index) {
    var current = state.values[index];
    var need = def.req - current;
    if (need <= 0) return 0;

    var accepted = acceptedIds(def);
    var totalRemoved = 0;

    for (var i = 0; i < accepted.length && totalRemoved < need; i++) {
        var id = accepted[i];
        var available = inventoryCount(player, id);
        var amount = Math.min(need - totalRemoved, available);
        if (amount <= 0) continue;

        var removed = false;
        try { removed = !!player.removeItem(id, amount); } catch (ignoredRemove) {}
        if (!removed) continue;

        totalRemoved += amount;
    }

    if (totalRemoved <= 0) return 0;

    state.values[index] = current + totalRemoved;
    writeInt(state.data, def.key, state.values[index]);
    return totalRemoved;
}

function contributeSet(player, defs, completeKey) {
    var state = setState(player, defs, completeKey);
    if (state.complete) return "Эта часть проекта уже завершена.";

    var total = 0;
    var types = 0;
    for (var i = 0; i < defs.length; i++) {
        var amount = takeItem(player, state, defs[i], i);
        if (amount > 0) { total += amount; types++; }
    }

    var complete = true;
    for (var j = 0; j < defs.length; j++) {
        if (state.values[j] < defs[j].req) { complete = false; break; }
    }
    if (complete) writeInt(state.data, completeKey, 1);

    if (total <= 0) return "Нечего передавать: нужных ресурсов нет в инвентаре.";
    return "Ресурсы приняты: " + total + " шт. (" + types + " типов).";
}

function compactNotice(parts) {
    if (parts == null || parts.length == 0) return "";

    /* Completion reward is the most important line after a successful hand-in. */
    for (var i = parts.length - 1; i >= 0; i--) {
        var reward = String(parts[i]);
        if (reward.indexOf("Награда базы") >= 0) return reward;
    }

    /* Then prefer a meaningful completion/build result. */
    for (var j = parts.length - 1; j >= 0; j--) {
        var important = String(parts[j]);
        if (important.indexOf("восстановлена") >= 0
                || important.indexOf("заверш") >= 0
                || important.indexOf("готов") >= 0) {
            return important;
        }
    }

    /* Otherwise show the latest successful contribution. */
    for (var k = parts.length - 1; k >= 0; k--) {
        var accepted = String(parts[k]);
        if (accepted.indexOf("Ресурсы приняты") >= 0) return accepted;
    }

    for (var n = 0; n < parts.length; n++) {
        var candidate = String(parts[n]);
        if (candidate.indexOf("Нечего передавать") < 0 && candidate.length > 0) return candidate;
    }

    return String(parts[parts.length - 1]);
}

function commandCoord(value) { return Number(value).toFixed(2); }

function executeWorldCommand(player, command) {
    try {
        var world = player.getWorld();
        API.executeCommandSilent(world, command);
        return true;
    } catch (ignoredCommand) {
        return false;
    }
}

function particleBurst(world, particle, x, y, z, dx, dy, dz, speed, count) {
    try {
        world.spawnParticle(particle, x, y, z, dx, dy, dz, speed, count);
        return true;
    } catch (ignoredParticle) {
        return false;
    }
}

function fireworkParticles(world, x, y, z) {
    var ok = particleBurst(world, "minecraft:firework", x, y, z, 0.42, 0.48, 0.42, 0.10, 55);
    if (!ok) ok = particleBurst(world, "firework", x, y, z, 0.42, 0.48, 0.42, 0.10, 55);
    if (!ok) particleBurst(world, "minecraft:end_rod", x, y, z, 0.38, 0.38, 0.38, 0.07, 35);
}

function playQuestCompleteSound(player, x, y, z) {
    var sx = commandCoord(x);
    var sy = commandCoord(y);
    var sz = commandCoord(z);

    // Vanilla command first: reliable on Forge 1.20.1 when CustomNPCs sound wrappers are picky.
    var played = executeWorldCommand(player,
        'playsound minecraft:ui.toast.challenge_complete master @a ' + sx + ' ' + sy + ' ' + sz + ' 1.15 1.0');

    if (!played) {
        try {
            player.getWorld().playSoundAt(API.getIPos(x, y, z), "minecraft:ui.toast.challenge_complete", 1.15, 1.0);
            played = true;
        } catch (ignoredWorldSound) {}
    }

    if (!played) {
        try { player.playSound("minecraft:entity.player.levelup", 1.0, 1.0); } catch (ignoredPlayerSound) {}
    }
}

function celebrate(player, message1, message2) {
    var playerName = String(player.getName());
    var x = Number(player.getX());
    var y = Number(player.getY());
    var z = Number(player.getZ());
    var savedPos = lastNpcPosByPlayer[playerName];
    if (savedPos != null) {
        x = Number(savedPos.x);
        y = Number(savedPos.y);
        z = Number(savedPos.z);
    }

    var leftX = x - 0.90;
    var rightX = x + 0.90;
    var fireY = y + 1.15;
    var fireZ = z;

    var first = 'summon minecraft:firework_rocket ' + commandCoord(leftX) + ' ' + commandCoord(fireY) + ' ' + commandCoord(fireZ) +
        ' {Life:0,LifeTime:18,FireworksItem:{id:"minecraft:firework_rocket",Count:1b,tag:{Fireworks:{Flight:1b,Explosions:[{Type:1b,Flicker:1b,Trail:1b,Colors:[I;16755200,5635925],FadeColors:[I;16777215]}]}}}}}';

    var second = 'summon minecraft:firework_rocket ' + commandCoord(rightX) + ' ' + commandCoord(fireY) + ' ' + commandCoord(fireZ) +
        ' {Life:0,LifeTime:24,FireworksItem:{id:"minecraft:firework_rocket",Count:1b,tag:{Fireworks:{Flight:1b,Explosions:[{Type:1b,Flicker:1b,Trail:1b,Colors:[I;5635925,16766720],FadeColors:[I;16777215]}]}}}}}';

    // Actual rockets through the public CustomNPCs command API.
    executeWorldCommand(player, first);
    executeWorldCommand(player, second);

    // Guaranteed visible celebration even if this unofficial port rejects rocket entity NBT.
    var world = player.getWorld();
    fireworkParticles(world, leftX, y + 2.45, fireZ);
    fireworkParticles(world, rightX, y + 3.05, fireZ);

    // Launch sound + dedicated quest-complete sound.
    executeWorldCommand(player,
        'playsound minecraft:entity.firework_rocket.launch master @a ' + commandCoord(x) + ' ' + commandCoord(y + 1.0) + ' ' + commandCoord(z) + ' 0.8 1.0');
    playQuestCompleteSound(player, x, y + 1.0, z);

    try {
        world.broadcast(message1);
        world.broadcast(message2);
    } catch (ignoredBroadcast) {
        try { player.message(message1); } catch (ignoredMessage1) {}
        try { player.message(message2); } catch (ignoredMessage2) {}
    }
}


function makeGui(player) { return API.createCustomGui(GUI_NAME, GUI_WIDTH, GUI_HEIGHT, false, player); }
function makeProjectGui(player) { return API.createCustomGui(GUI_NAME + "_project", 460, 360, false, player); }

function text(value) { if (value == null) return ""; return String(value).replace(/\r/g, ""); }

function wrap(value, maxLen) {
    var source = text(value);
    var paragraphs = source.split("\n");
    var result = [];
    for (var p = 0; p < paragraphs.length; p++) {
        var paragraph = paragraphs[p];
        if (paragraph.length == 0) { result.push(""); continue; }
        var words = paragraph.split(" ");
        var line = "";
        for (var i = 0; i < words.length; i++) {
            var word = words[i];
            if (line.length == 0) line = word;
            else if ((line.length + 1 + word.length) <= maxLen) line += " " + word;
            else { result.push(line); line = word; }
        }
        if (line.length > 0) result.push(line);
    }
    return result;
}

function addLines(gui, startId, x, y, width, color, value, maxLen, maxLines) {
    var lines = wrap(value, maxLen);
    if (maxLines != null && lines.length > maxLines) {
        lines = lines.slice(0, maxLines);
        if (lines.length > 0) lines[lines.length - 1] += "...";
    }
    for (var i = 0; i < lines.length; i++) gui.addLabel(startId + i, lines[i], x, y + (i * 12), width, 11, color);
}

function addHeader(gui, section) {
    gui.addLabel(1, "ДЖОЗЕФ КУППЕР", 16, 12, 180, 11, 0xE6B84A);
    gui.addLabel(2, "Координатор купола", 16, 25, 180, 11, 0xB8B8B8);
    gui.addLabel(3, "БАЗА-01", 257, 12, 50, 11, 0x808080);
    gui.addLabel(4, "--------------------------------------------------", 16, 39, 290, 11, 0x555555);
    gui.addLabel(5, section, 16, 52, 290, 11, 0xFFD75A);
}

function addProjectHeader(gui, section) {
    gui.addLabel(1, "ДЖОЗЕФ КУППЕР", 18, 12, 230, 11, 0xE6B84A);
    gui.addLabel(2, "Координатор купола", 18, 25, 230, 11, 0xB8B8B8);
    gui.addLabel(3, "БАЗА-01", 385, 12, 58, 11, 0x808080);
    gui.addLabel(4, "------------------------------------------------------------------------", 18, 39, 424, 11, 0x555555);
    gui.addLabel(5, section, 18, 52, 424, 11, 0xFFD75A);
}

function questItemName(def) {
    if (def == null) return "<?>";

    /* V7.3.3 intentionally uses the Russian quest label directly.
       The CustomNPCs API item factory in this port did not resolve some
       third-party display names correctly and V7.3 showed registry IDs. */
    try {
        if (def.name != null && String(def.name).length > 0) {
            return String(def.name);
        }
    } catch (ignoredName) {}

    return String(def.id);
}

function renderSetGrid(gui, startId, defs, state, y) {
    for (var i = 0; i < defs.length; i++) {
        var line = questItemName(defs[i]) + ": " + state.values[i] + " / " + defs[i].req;
        var color = state.values[i] >= defs[i].req ? 0x77DD77 : 0xEEEEEE;
        gui.addLabel(startId + i, line, 28, y + i * 14, 410, 11, color);
    }
}

function renderStage2Grid(gui, player, y) {
    var lines = [];

    if (logicalResetActive(player)) {
        var resetCore = setState(player, S2_RESET_CORE, S2_RESET_CORE_COMPLETE);
        for (var r = 0; r < S2_RESET_CORE.length; r++) {
            lines.push(questItemName(S2_RESET_CORE[r]) + ": " + resetCore.values[r] + " / " + S2_RESET_CORE[r].req);
        }
    } else {
        try {
            var core = String(Bridge.progressText()).replace(/\r/g, "").split("\n");
            for (var i = 0; i < core.length; i++) if (String(core[i]).length > 0) lines.push(String(core[i]));
        } catch (ignoredCoreText) {}
    }

    var extras = setState(player, S2, S2_EXTRA_COMPLETE);
    for (var j = 0; j < S2.length; j++) {
        lines.push(questItemName(S2[j]) + ": " + extras.values[j] + " / " + S2[j].req);
    }

    for (var k = 0; k < lines.length; k++) {
        gui.addLabel(40 + k, lines[k], 28, y + k * 14, 410, 11, 0xEEEEEE);
    }
}

var V75_BALANCE_MIGRATION = "domesurvival.v75.balance_migration";
var V75_R9_S10_REFUND = "domesurvival.v75.r9_s10_machine_refund";

function v75GiveItem(player, id, count) {
    var ok = false;
    try { ok = !!player.giveItem(id, count); } catch (ignoredGiveById) {}
    if (!ok) {
        try {
            var stack = API.createItem(id, 0, count);
            ok = !!player.giveItem(stack);
        } catch (ignoredGiveStack) {}
    }
    return ok;
}

function migrateV75Balance(player) {
    var data = getStored(player);
    if (data == null) return;

    if (readInt(data, V75_BALANCE_MIGRATION) <= 0) {
        var oldSteel = readInt(data, "domesurvival.stage10.aasteel.v74")
            + readInt(data, "domesurvival.stage10.meksteel.v74");
        var oldPlate = readInt(data, "domesurvival.stage10.plate.v74")
            + readInt(data, "domesurvival.stage10.ieplate.v74");

        if (oldSteel > 0) writeInt(data, "domesurvival.stage10.genericsteel.v75", Math.min(48, oldSteel));
        if (oldPlate > 0) writeInt(data, "domesurvival.stage10.genericplate.v75", Math.min(36, oldPlate));

        writeInt(data, V75_BALANCE_MIGRATION, 1);
    }

    if (readInt(data, V75_R9_S10_REFUND) <= 0 && readInt(data, R9_KEY) > 0) {
        var refundedAny = false;
        if (readInt(data, "domesurvival.stage10.pulverizer.v74") > 0) {
            refundedAny = v75GiveItem(player, "thermal:machine_pulverizer", 1) || refundedAny;
        }
        if (readInt(data, "domesurvival.stage10.smelter.v74") > 0) {
            refundedAny = v75GiveItem(player, "thermal:machine_smelter", 1) || refundedAny;
        }
        /* Mark once regardless: repeating the migration must never duplicate machines. */
        writeInt(data, V75_R9_S10_REFUND, 1);
        if (refundedAny) {
            try { player.message("§a[КУПОЛ] Возвращено оборудование, которое старая версия квеста заставляла сразу сдавать обратно."); } catch (ignoredRefundMessage) {}
        }
    }
}

function currentStage(player) {
    if (!stage1Complete(player)) return 1;
    if (!stage2Complete(player)) return 2;
    if (!stage3Complete(player)) return 3;
    if (!stage4Complete(player)) return 4;
    if (!stage5Complete(player)) return 5;
    if (!stage6Complete(player)) return 6;
    if (!stage7Complete(player)) return 7;
    if (!stage8Complete(player)) return 8;
    if (!stage9Complete(player)) return 9;
    if (!stage10Complete(player)) return 10;
    if (!stage11Complete(player)) return 11;
    if (!stage12Complete(player)) return 12;
    return 13;
}

function currentProjectTitle(player) {
    var stage = currentStage(player);
    if (stage == 1) return "Жизнеобеспечение купола";
    if (stage == 2) {
        try { return String(Bridge.projectTitle()); } catch (ignored) { return "Восстановление мастерской"; }
    }
    if (stage == 3) return "Аварийное энергоснабжение";
    if (stage == 4) return "Вода и агроконтур";
    if (stage == 5) return "Кислородный контур";
    if (stage == 6) return "Первая внешняя экспедиция";
    if (stage == 7) return "Логистика купола";
    if (stage == 8) return "Аварийный резерв базы";
    if (stage == 9) return "Дальняя связь";
    if (stage == 10) return "Ракетные материалы";
    if (stage == 11) return "Ракетный модуль";
    if (stage == 12) return "Предстартовая готовность";
    return "Луна / первая внеземная база";
}

function openMain(player) {
    var gui = makeGui(player);
    addHeader(gui, "СПИСОК / ТЕКУЩАЯ ОБСТАНОВКА");

    migrateV75Balance(player);

    var stage = currentStage(player);
    var narrative = "";

    if (stage == 1) narrative = "Сначала удерживаем жизнь внутри купола: древесина, пища, стекло, топливо и минимальный запас металла.";
    else if (stage == 2) narrative = "Жизнеобеспечение стабилизировано. Восстанавливаем мастерскую — центральную точку ремонта и сборки.";
    else if (stage == 3) narrative = "Мастерская работает. Следующая угроза — отключение энергии. Собираем аварийный энергоконтур.";
    else if (stage == 4) narrative = "Электропитание есть. Теперь разворачиваем собственную очистку воды и агроконтур внутри базы.";
    else if (stage == 5) narrative = "Вода и пищевой контур работают. Создаём кислородную инфраструктуру для безопасной работы за пределами купола.";
    else if (stage == 6) narrative = "Кислородное оборудование готово. Проводим первую серьёзную экспедицию и возвращаем стратегическое сырьё.";
    else if (stage == 7) narrative = "Запасы появились, но база захлёбывается ручной переноской. Строим внутреннюю логистическую сеть.";
    else if (stage == 8) narrative = "Критические системы запущены. Формируем аварийный резерв, чтобы один отказ больше не ставил базу на грань гибели.";
    else if (stage == 9) narrative = "Резерв сформирован. Начинается программа «Исход»: нужна дальняя связь и защищённый контур управления будущими космическими работами.";
    else if (stage == 10) narrative = "Дальний канал связи поднят. Переходим к серийному производству стали, пластин и жаростойких материалов ракетного класса.";
    else if (stage == 11) narrative = "Материальная база готова. Теперь комплектуем отдельный ракетный модуль: двигатель, корпус, управление и жизнеобеспечение.";
    else if (stage == 12) narrative = "Ракетный модуль собран. Осталось развернуть наземный предстартовый контур: энергетику, связь, переработку топлива и резерв материалов.";
    else narrative = "Земная часть программы «Исход» завершена. База готова поддержать первый перелёт; следующая глава начнётся уже после выхода к Луне.";

    addLines(gui, 20, 18, 72, 282, 0xEEEEEE, narrative, 52, 5);

    gui.addLabel(80, "Текущий проект:", 18, 134, 96, 11, 0xAAAAAA);
    gui.addLabel(81, currentProjectTitle(player), 116, 134, 190, 11, 0xFFFFFF);
    gui.addLabel(82, stage == 13 ? "[ ГЛАВА ЗАВЕРШЕНА ]" : "[ В РАБОТЕ ]", 202, 147, 112, 11, stage == 13 ? 0x55FF55 : 0xFFD45A);

    addVisibleButton(gui, BTN_PROJECT, "Проект", 18, 160, 136, 20, 7601);
    addVisibleButton(gui, BTN_BASE, "Состояние базы", 166, 160, 136, 20, 7602);
    addVisibleButton(gui, BTN_PLAN, "План развития", 18, 190, 136, 20, 7603);
    addVisibleButton(gui, BTN_CLOSE, "Закрыть", 166, 190, 136, 20, 7604);

    player.showCustomGui(gui);
}
function addProjectNotice(gui, notice) {
    if (notice == null || String(notice).length == 0) return;
    var lines = wrap(String(notice), 72);
    if (lines.length > 2) lines = lines.slice(0, 2);
    for (var i = 0; i < lines.length; i++) {
        gui.addLabel(90 + i, lines[i], 22, 292 + i * 12, 416, 11, 0xFFD45A);
    }
}

function openProject(player, notice) {
    var stage = currentStage(player);
    if (stage == 1) { openStage1(player, notice); return; }
    if (stage == 2) { openStage2(player, notice); return; }
    if (stage == 3) { openStage3(player, notice); return; }
    if (stage == 4) { openStage4(player, notice); return; }
    if (stage == 5) { openStage5(player, notice); return; }
    if (stage == 6) { openStage6(player, notice); return; }
    if (stage == 7) { openStage7(player, notice); return; }
    if (stage == 8) { openStage8(player, notice); return; }
    if (stage == 9) { openStage9(player, notice); return; }
    if (stage == 10) { openStage10(player, notice); return; }
    if (stage == 11) { openStage11(player, notice); return; }
    if (stage == 12) { openStage12(player, notice); return; }
    openStage13Locked(player, notice);
}

function openStage1(player, notice) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "ПРОЕКТ 01 / ЖИЗНЕОБЕСПЕЧЕНИЕ");
    gui.addLabel(20, "Жизнь внутри купола и первая полевая кухня", 20, 72, 420, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 20, 92, 160, 11, 0xAAAAAA);

    var state = setState(player, S1, S1_COMPLETE);
    renderSetGrid(gui, 30, S1, state, 110);

    if (state.complete) {
        gui.addLabel(70, "Жизнеобеспечение стабилизировано. Этап 02 доступен.", 28, 260, 410, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать ресурсы", 145, 258, 170, 20, 7610);
    }

    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 180, 333, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openStage2(player, notice) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "ПРОЕКТ 02 / МАСТЕРСКАЯ");
    gui.addLabel(20, "Восстановление и промышленная комплектация мастерской", 20, 72, 420, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 20, 92, 160, 11, 0xAAAAAA);
    renderStage2Grid(gui, player, 108);

    var complete = stage2Complete(player);
    if (complete && !workshopBuilt()) {
        var retryBuildNotice = finalizeWorkshopIfReady(player);
        if (retryBuildNotice != null && retryBuildNotice.length > 0) notice = retryBuildNotice;
    }

    if (complete) {
        gui.addLabel(70,
            workshopBuilt()
                ? "Мастерская восстановлена и укомплектована. Этап 03 доступен."
                : "Этап 02 завершён. Для тестовой команды физическая постройка может быть пропущена.",
            28, 260, 410, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать ресурсы", 145, 258, 170, 20, 7610);
    }

    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 180, 333, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openResourceStage(player, notice, header, description, defs, completeKey, completeText, buttonText) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, header);
    gui.addLabel(20, description, 20, 72, 420, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 20, 92, 160, 11, 0xAAAAAA);

    var state = setState(player, defs, completeKey);
    renderSetGrid(gui, 30, defs, state, 110);

    if (state.complete) {
        gui.addLabel(70, completeText, 28, 260, 410, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, buttonText, 145, 258, 170, 20, 7610);
    }

    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 180, 333, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openStage3(player, notice) {
    openResourceStage(
        player, notice,
        "ПРОЕКТ 03 / АВАРИЙНАЯ ЭНЕРГИЯ",
        "Резервное питание ключевых систем купола",
        S3, S3_COMPLETE,
        "Энергетический резерв готов. Этап 04 доступен.",
        "Передать оборудование"
    );
}

function openStage4(player, notice) {
    openResourceStage(
        player, notice,
        "ПРОЕКТ 04 / ВОДА И АГРОКОНТУР",
        "Очистка воды и плодородная почва внутри купола",
        S4, S4_COMPLETE,
        "Водоочистка стабилизирована. Этап 05 доступен.",
        "Передать оборудование"
    );
}

function openStage5(player, notice) {
    openResourceStage(
        player, notice,
        "ПРОЕКТ 05 / КИСЛОРОДНЫЙ КОНТУР",
        "Производство, заправка и разводка кислорода",
        S5, S5_COMPLETE,
        "Кислородный контур готов. Этап 06 доступен.",
        "Передать оборудование"
    );
}

function openStage6(player, notice) {
    openResourceStage(
        player, notice,
        "ПРОЕКТ 06 / ПЕРВАЯ ЭКСПЕДИЦИЯ",
        "Вернуть с поверхности стратегическое сырьё",
        S6, S6_COMPLETE,
        "Экспедиция успешна. Этап 07 доступен.",
        "Сдать добычу"
    );
}

function openStage7(player, notice) {
    openResourceStage(
        player, notice,
        "ПРОЕКТ 07 / ЛОГИСТИКА КУПОЛА",
        "Автоматизация перемещения ресурсов между системами",
        S7, S7_COMPLETE,
        "Логистическая сеть подготовлена. Этап 08 доступен.",
        "Передать оборудование"
    );
}

function openStage8(player, notice) {
    openResourceStage(
        player, notice,
        "ПРОЕКТ 08 / АВАРИЙНЫЙ РЕЗЕРВ",
        "Резерв техники, продовольствия и ферментированных запасов",
        S8, S8_COMPLETE,
        "Аварийный резерв сформирован. Базовый цикл завершён.",
        "Передать резерв"
    );
}

function openStage9(player, notice) {
    openResourceStage(player, notice, "ПРОЕКТ 09 / ДАЛЬНЯЯ СВЯЗЬ", "Защищённая связь и контур управления программы «Исход»", S9, S9_COMPLETE, "Связь стабилизирована. Этап 10 доступен.", "Передать оборудование");
}
function openStage10(player, notice) {
    openResourceStage(player, notice, "ПРОЕКТ 10 / РАКЕТНЫЕ МАТЕРИАЛЫ", "Производство стали, пластин и деталей ракетного класса", S10, S10_COMPLETE, "Материальная база готова. Этап 11 доступен.", "Передать материалы");
}
function openStage11(player, notice) {
    openResourceStage(player, notice, "ПРОЕКТ 11 / РАКЕТНЫЙ МОДУЛЬ", "Комплектование двигателя, корпуса и систем жизнеобеспечения", S11, S11_COMPLETE, "Ракетный модуль укомплектован. Этап 12 доступен.", "Передать компоненты");
}
function openStage12(player, notice) {
    openResourceStage(player, notice, "ПРОЕКТ 12 / ПРЕДСТАРТОВАЯ ГОТОВНОСТЬ", "Наземная энергетика, связь, переработка топлива и пусковой резерв", S12, S12_COMPLETE, "Земная часть программы «Исход» завершена.", "Передать резерв");
}
function openStage13Locked(player, notice) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "СЛЕДУЮЩАЯ ГЛАВА / ЛУНА");
    gui.addLabel(20, "Земная часть программы «Исход» завершена", 20, 80, 420, 11, 0x55FF55);
    addLines(gui, 30, 28, 104, 410, 0xEEEEEE,
        "Следующая глава начнётся после первого перелёта: лунная база, добыча внеземных материалов и переход к сплавам следующего технологического уровня. Деш до этой точки намеренно не требуется.",
        70, 6);
    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 180, 333, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openBase(player) {
    var gui = makeGui(player);
    addHeader(gui, "СОСТОЯНИЕ БАЗЫ");
    var s1 = stage1Complete(player);
    var s2 = stage2Complete(player);
    var s5 = stage5Complete(player);
    var s8 = stage8Complete(player);
    var s9 = stage9Complete(player);
    var s10 = stage10Complete(player);
    var s12 = stage12Complete(player);
    var built = logicalResetActive(player) ? s2 : workshopBuilt();

    var rows = [
        { name: "Жизнеобеспечение", value: s1 ? "Стабильно" : "Формируется", ok: s1 },
        { name: "Мастерская", value: s2 ? (built ? "Восстановлена" : "Готова") : "Не восстановлена", ok: s2 },
        { name: "Энергия / вода / O2", value: s5 ? "Стабильны" : "Формируются", ok: s5 },
        { name: "Экспедиции / логистика", value: s8 ? "Стабильны" : "Формируются", ok: s8 },
        { name: "Дальняя связь", value: s9 ? "Готова" : (s8 ? "Формируется" : "Заблокирована"), ok: s9 },
        { name: "Ракетные материалы", value: s10 ? "Подготовлены" : (s9 ? "Формируются" : "Заблокированы"), ok: s10 },
        { name: "Предстартовая готовность", value: s12 ? "Готово к следующей главе" : (s10 ? "Формируется" : "Заблокирована"), ok: s12 }
    ];
    for (var i = 0; i < rows.length; i++) {
        var y = 70 + i * 17;
        gui.addLabel(20 + i * 2, rows[i].name, 20, y, 142, 11, 0xBBBBBB);
        gui.addLabel(21 + i * 2, rows[i].value, 166, y, 140, 11, rows[i].ok ? 0x55FF55 : 0xFFD45A);
    }
    var stage = currentStage(player);
    gui.addLabel(60, "Приоритет:", 20, 190, 75, 11, 0xE6B84A);
    gui.addLabel(61, currentProjectTitle(player), 94, 190, 210, 11, stage == 13 ? 0x55FF55 : 0xEEEEEE);
    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openPlan(player) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "ПЛАН РАЗВИТИЯ");
    var current = currentStage(player);
    var stages = [
        "01  Жизнеобеспечение купола",
        "02  Восстановление мастерской",
        "03  Аварийное энергоснабжение",
        "04  Вода и агроконтур",
        "05  Кислородный контур",
        "06  Первая внешняя экспедиция",
        "07  Логистика купола",
        "08  Аварийный резерв базы",
        "09  Дальняя связь",
        "10  Ракетные материалы",
        "11  Ракетный модуль",
        "12  Предстартовая готовность",
        "13  Луна / первая внеземная база"
    ];
    for (var i = 0; i < stages.length; i++) {
        var stageNumber = i + 1;
        var color = 0x888888;
        if (stageNumber < current) color = 0x55FF55;
        else if (stageNumber == current) color = 0xFFD45A;
        gui.addLabel(20 + i, stages[i], 24, 66 + i * 18, 410, 11, color);
    }
    addVisibleButton(gui, BTN_BACK, "Назад", 180, 333, 100, 18, 7620);
    player.showCustomGui(gui);
}
