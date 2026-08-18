$ErrorActionPreference = "Stop"
$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
$CommandJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\integration\customnpcs\JosephScriptCommand.java"

if (-not (Test-Path -LiteralPath $Joseph)) { throw "Joseph GUI source not found: $Joseph" }
if (-not (Test-Path -LiteralPath $CommandJava)) { throw "JosephScriptCommand.java not found: $CommandJava" }

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_questline_v73_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force
Copy-Item -LiteralPath $CommandJava -Destination (Join-Path $Backup "JosephScriptCommand.java") -Force

function Replace-RequiredRegex {
    param([string]$Text,[string]$Pattern,[string]$Replacement,[string]$Label)
    $rx = [regex]::new($Pattern,[System.Text.RegularExpressions.RegexOptions]::Singleline)
    $matches = $rx.Matches($Text)
    if ($matches.Count -ne 1) {
        throw "Patch anchor '$Label' expected exactly 1 match, found $($matches.Count). Install V7.2 first."
    }
    return $rx.Replace($Text,[System.Text.RegularExpressions.MatchEvaluator]{ param($m) return $Replacement },1)
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Joseph,[Text.Encoding]::UTF8)

if ($Text.Contains("GUI v7.3 MULTIMOD + TEST SKIP")) {
    Write-Host "[OK] Joseph V7.3 is already installed." -ForegroundColor Green
    exit 0
}
if (-not $Text.Contains("GUI v7.2 UI + REWARDS FIX")) {
    throw "V7.3 requires Joseph Questline V7.2 as the current local script."
}

$Text = $Text.Replace("/* Dome Survival - Joseph Cooper GUI v7.2 UI + REWARDS FIX */","/* Dome Survival - Joseph Cooper GUI v7.3 MULTIMOD + TEST SKIP */")
$Text = $Text.Replace('var GUI_NAME = "dome_joseph_v72_ui_rewards_fix";','var GUI_NAME = "dome_joseph_v73_multimod_testskip";')

$StageBlock = @'
/* Stage 01 - Dome survival + first Farmer's Delight kitchen tools. */
var S1_COMPLETE = "domesurvival.stage01.complete.v71";
var S1 = [
    { key: "domesurvival.stage01.logs.v73",      id: "minecraft:oak_log",                   name: "Дубовое бревно", req: 32 },
    { key: "domesurvival.stage01.saplings.v73",  id: "minecraft:oak_sapling",               name: "Саженец дуба", req: 8 },
    { key: "domesurvival.stage01.wheat.v73",     id: "minecraft:wheat",                     name: "Пшеница", req: 24 },
    { key: "domesurvival.stage01.charcoal.v73",  id: "minecraft:charcoal",                  name: "Древесный уголь", req: 16 },
    { key: "domesurvival.stage01.glass.v73",     id: "minecraft:glass",                     name: "Стекло", req: 32 },
    { key: "domesurvival.stage01.copper.v73",    id: "minecraft:copper_ingot",              name: "Медный слиток", req: 12 },
    { key: "domesurvival.stage01.board.v73",     id: "farmersdelight:cutting_board",        name: "Farmer's Delight: cutting_board", req: 1 },
    { key: "domesurvival.stage01.pot.v73",       id: "farmersdelight:cooking_pot",          name: "Farmer's Delight: cooking_pot", req: 1 },
    { key: "domesurvival.stage01.skillet.v73",   id: "farmersdelight:skillet",              name: "Farmer's Delight: skillet", req: 1 }
];

/* Stage 02 - Workshop. Java Bridge owns the core 64 iron / 32 copper / 24 redstone. */
var S2_EXTRA_COMPLETE = "domesurvival.stage02.extras.complete.v71";
var S2 = [
    { key: "domesurvival.stage02.stone.v73",     id: "minecraft:stone_bricks",                    name: "Каменные кирпичи", req: 32 },
    { key: "domesurvival.stage02.furnace.v73",   id: "domesurvival:copper_furnace",              name: "Медная печь", req: 1 },
    { key: "domesurvival.stage02.hopper.v73",    id: "domesurvival:copper_hopper",               name: "Медная воронка", req: 1 },
    { key: "domesurvival.stage02.ieiron.v73",    id: "immersiveengineering:component_iron",       name: "Immersive Engineering: component_iron", req: 4 },
    { key: "domesurvival.stage02.iewire.v73",    id: "immersiveengineering:wire_copper",          name: "Immersive Engineering: wire_copper", req: 8 },
    { key: "domesurvival.stage02.mekcircuit.v73",id: "mekanism:basic_control_circuit",            name: "Mekanism: basic_control_circuit", req: 2 },
    { key: "domesurvival.stage02.mekalloy.v73",  id: "mekanism:alloy_infused",                    name: "Mekanism: alloy_infused", req: 4 }
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
    { key: "domesurvival.stage03.tablet.v73",      id: "mekanism:energy_tablet",                       name: "Mekanism: energy_tablet", req: 1 },
    { key: "domesurvival.stage03.circuit.v73",     id: "mekanism:basic_control_circuit",              name: "Mekanism: basic_control_circuit", req: 4 },
    { key: "domesurvival.stage03.alloy.v73",       id: "mekanism:alloy_infused",                      name: "Mekanism: alloy_infused", req: 8 },
    { key: "domesurvival.stage03.electronic.v73",  id: "immersiveengineering:component_electronic",   name: "Immersive Engineering: component_electronic", req: 4 },
    { key: "domesurvival.stage03.copperwire.v73",  id: "immersiveengineering:wire_copper",            name: "Immersive Engineering: wire_copper", req: 12 },
    { key: "domesurvival.stage03.darksteel.v73",   id: "enderio:dark_steel_ingot",                    name: "Ender IO: dark_steel_ingot", req: 4 },
    { key: "domesurvival.stage03.coal.v73",        id: "minecraft:coal",                               name: "Уголь", req: 32 }
];

/* Stage 04 - Water loop and productive soil inside the dome. */
var S4_COMPLETE = "domesurvival.stage04.complete.v71";
var S4 = [
    { key: "domesurvival.stage04.filter.v73",      id: "domesurvival:water_filter_cartridge", name: "Обычный фильтрующий картридж", req: 4 },
    { key: "domesurvival.stage04.ifilter.v73",     id: "domesurvival:improved_water_filter",  name: "Улучшенный фильтрующий картридж", req: 1 },
    { key: "domesurvival.stage04.pipe.v73",        id: "domesurvival:basic_fluid_pipe",       name: "Базовая жидкостная труба", req: 12 },
    { key: "domesurvival.stage04.tank.v73",        id: "domesurvival:universal_tank",         name: "Универсальный резервуар", req: 1 },
    { key: "domesurvival.stage04.compost.v73",     id: "farmersdelight:organic_compost",      name: "Farmer's Delight: organic_compost", req: 4 },
    { key: "domesurvival.stage04.richsoil.v73",    id: "farmersdelight:rich_soil",            name: "Farmer's Delight: rich_soil", req: 4 },
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
    { key: "domesurvival.stage05.mekcircuit.v73",   id: "mekanism:advanced_control_circuit",           name: "Mekanism: advanced_control_circuit", req: 2 },
    { key: "domesurvival.stage05.mekalloy.v73",     id: "mekanism:alloy_reinforced",                   name: "Mekanism: alloy_reinforced", req: 4 },
    { key: "domesurvival.stage05.iesteel.v73",      id: "immersiveengineering:component_steel",        name: "Immersive Engineering: component_steel", req: 4 },
    { key: "domesurvival.stage05.ieadvanced.v73",   id: "immersiveengineering:component_electronic_adv",name: "Immersive Engineering: component_electronic_adv", req: 2 },
    { key: "domesurvival.stage05.darksteel.v73",    id: "enderio:dark_steel_ingot",                    name: "Ender IO: dark_steel_ingot", req: 6 }
];

/* Stage 06 - First controlled outside expedition after oxygen is ready. */
var S6_COMPLETE = "domesurvival.stage06.complete.v71";
var S6 = [
    { key: "domesurvival.stage06.rawiron.v73",    id: "minecraft:raw_iron",                         name: "Необработанное железо", req: 32 },
    { key: "domesurvival.stage06.rawcopper.v73",  id: "minecraft:raw_copper",                       name: "Необработанная медь", req: 48 },
    { key: "domesurvival.stage06.gold.v73",       id: "minecraft:gold_ingot",                       name: "Золотой слиток", req: 12 },
    { key: "domesurvival.stage06.diamond.v73",    id: "minecraft:diamond",                          name: "Алмаз", req: 4 },
    { key: "domesurvival.stage06.tomato.v73",     id: "farmersdelight:tomato",                      name: "Farmer's Delight: tomato", req: 16 },
    { key: "domesurvival.stage06.cabbage.v73",    id: "farmersdelight:cabbage",                     name: "Farmer's Delight: cabbage", req: 16 },
    { key: "domesurvival.stage06.onion.v73",      id: "farmersdelight:onion",                       name: "Farmer's Delight: onion", req: 16 },
    { key: "domesurvival.stage06.jerky.v73",      id: "brewinandchewin:jerky",                      name: "Brewin' And Chewin': jerky", req: 8 },
    { key: "domesurvival.stage06.steelplate.v73", id: "immersiveengineering:plate_steel",           name: "Immersive Engineering: plate_steel", req: 8 },
    { key: "domesurvival.stage06.meksteel.v73",   id: "mekanism:steel_ingot",                       name: "Mekanism: steel_ingot", req: 8 }
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
    { key: "domesurvival.stage07.configurator.v73",id: "mekanism:configurator",                      name: "Mekanism: configurator", req: 1 },
    { key: "domesurvival.stage07.mekalloy.v73",    id: "mekanism:alloy_infused",                     name: "Mekanism: alloy_infused", req: 8 },
    { key: "domesurvival.stage07.electronic.v73",  id: "immersiveengineering:component_electronic",  name: "Immersive Engineering: component_electronic", req: 4 },
    { key: "domesurvival.stage07.darksteel.v73",   id: "enderio:dark_steel_ingot",                   name: "Ender IO: dark_steel_ingot", req: 4 }
];

/* Stage 08 - Base reserve: hardware + food reserve + fermentation/morale reserve. */
var S8_COMPLETE = "domesurvival.stage08.complete.v71";
var S8 = [
    { key: "domesurvival.stage08.buffer.v73",      id: "domesurvival:energy_buffer",             name: "Энергоблок серии «Сталь»", req: 1 },
    { key: "domesurvival.stage08.tank.v73",        id: "domesurvival:universal_tank",            name: "Универсальный резервуар", req: 2 },
    { key: "domesurvival.stage08.steelpipe.v73",   id: "domesurvival:steel_item_pipe",           name: "Стальная транспортная труба", req: 8 },
    { key: "domesurvival.stage08.steelhop.v73",    id: "domesurvival:steel_hopper",              name: "Стальная воронка", req: 2 },
    { key: "domesurvival.stage08.energy2.v73",     id: "domesurvival:reinforced_energy_pipe",    name: "Усиленная энерготруба II уровня", req: 4 },
    { key: "domesurvival.stage08.mekalloy.v73",    id: "mekanism:alloy_reinforced",              name: "Mekanism: alloy_reinforced", req: 8 },
    { key: "domesurvival.stage08.rice.v73",        id: "farmersdelight:rice",                     name: "Farmer's Delight: rice", req: 32 },
    { key: "domesurvival.stage08.keg.v73",         id: "brewinandchewin:keg",                    name: "Brewin' And Chewin': keg", req: 1 },
    { key: "domesurvival.stage08.beer.v73",        id: "brewinandchewin:beer",                   name: "Brewin' And Chewin': beer", req: 8 },
    { key: "domesurvival.stage08.mead.v73",        id: "brewinandchewin:mead",                   name: "Brewin' And Chewin': mead", req: 4 }
];

/* One-time shared base reward packages. V73 keys intentionally reissue the revised multimod packages once. */
var R1_KEY = "domesurvival.stage01.reward.v73";
var R1 = [
    { id: "farmersdelight:stove", count: 1, name: "Farmer's Delight: stove" },
    { id: "farmersdelight:cabbage_seeds", count: 4, name: "Farmer's Delight: cabbage_seeds" },
    { id: "farmersdelight:tomato_seeds", count: 4, name: "Farmer's Delight: tomato_seeds" },
    { id: "minecraft:bread", count: 8, name: "Хлеб" }
];

var R2_KEY = "domesurvival.stage02.reward.v73";
var R2 = [
    { id: "domesurvival:machine_wrench", count: 1, name: "Ключ инженера" },
    { id: "domesurvival:copper_item_pipe", count: 8, name: "Медная транспортная труба" },
    { id: "domesurvival:basic_fluid_pipe", count: 8, name: "Базовая жидкостная труба" },
    { id: "immersiveengineering:hammer", count: 1, name: "Immersive Engineering: hammer" },
    { id: "mekanism:configurator", count: 1, name: "Mekanism: configurator" }
];

var R3_KEY = "domesurvival.stage03.reward.v73";
var R3 = [
    { id: "domesurvival:energy_buffer", count: 1, name: "Энергоблок серии «Сталь»" },
    { id: "domesurvival:basic_energy_pipe", count: 12, name: "Энерготруба I уровня" },
    { id: "mekanism:advanced_control_circuit", count: 2, name: "Mekanism: advanced_control_circuit" },
    { id: "immersiveengineering:component_electronic", count: 2, name: "Immersive Engineering: component_electronic" },
    { id: "minecraft:coal", count: 32, name: "Уголь" }
];

var R4_KEY = "domesurvival.stage04.reward.v73";
var R4 = [
    { id: "domesurvival:water_purifier", count: 1, name: "Очиститель воды" },
    { id: "domesurvival:universal_tank", count: 1, name: "Универсальный резервуар" },
    { id: "domesurvival:reinforced_fluid_pipe", count: 8, name: "Усиленная жидкостная труба" },
    { id: "farmersdelight:rich_soil", count: 4, name: "Farmer's Delight: rich_soil" }
];

var R5_KEY = "domesurvival.stage05.reward.v73";
var R5 = [
    { id: "domesurvival:oxygen_electrolyzer", count: 1, name: "Электролизёр кислорода" },
    { id: "domesurvival:oxygen_filler", count: 1, name: "Кислородный наполнитель" },
    { id: "domesurvival:oxygen_mask", count: 2, name: "Кислородная маска" },
    { id: "domesurvival:medium_oxygen_tank", count: 2, name: "Средний кислородный баллон" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 8, name: "Усиленная кислородная труба" },
    { id: "mekanism:energy_tablet", count: 1, name: "Mekanism: energy_tablet" }
];

var R6_KEY = "domesurvival.stage06.reward.v73";
var R6 = [
    { id: "domesurvival:surface_suit_helmet", count: 1, name: "Шлем защитного костюма" },
    { id: "domesurvival:surface_suit_chestplate", count: 1, name: "Куртка защитного костюма" },
    { id: "domesurvival:surface_suit_leggings", count: 1, name: "Штаны защитного костюма" },
    { id: "domesurvival:surface_suit_boots", count: 1, name: "Ботинки защитного костюма" },
    { id: "domesurvival:large_oxygen_tank", count: 1, name: "Большой кислородный баллон" },
    { id: "brewinandchewin:tankard", count: 2, name: "Brewin' And Chewin': tankard" }
];

var R7_KEY = "domesurvival.stage07.reward.v73";
var R7 = [
    { id: "domesurvival:steel_item_pipe", count: 12, name: "Стальная транспортная труба" },
    { id: "domesurvival:steel_hopper", count: 2, name: "Стальная воронка" },
    { id: "domesurvival:reinforced_energy_pipe", count: 8, name: "Усиленная энерготруба II уровня" },
    { id: "domesurvival:reinforced_fluid_pipe", count: 8, name: "Усиленная жидкостная труба" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 8, name: "Усиленная кислородная труба" },
    { id: "mekanism:alloy_reinforced", count: 4, name: "Mekanism: alloy_reinforced" }
];

var R8_KEY = "domesurvival.stage08.reward.v73";
var R8 = [
    { id: "domesurvival:high_voltage_energy_pipe", count: 8, name: "Высоковольтная энерготруба III уровня" },
    { id: "domesurvival:reinforced_glass", count: 16, name: "Усиленное стекло купола" },
    { id: "domesurvival:large_oxygen_tank", count: 2, name: "Большой кислородный баллон" },
    { id: "domesurvival:filtering_item_pipe", count: 4, name: "Фильтрующая транспортная труба" },
    { id: "brewinandchewin:mead", count: 4, name: "Brewin' And Chewin': mead" }
];

var lastInteractByPlayer = {};
'@
$RenderBlock = @'
function addProjectHeader(gui, section) {
    gui.addLabel(1, "ДЖОЗЕФ КУППЕР", 18, 12, 230, 11, 0xE6B84A);
    gui.addLabel(2, "Координатор купола", 18, 25, 230, 11, 0xB8B8B8);
    gui.addLabel(3, "БАЗА-01", 385, 12, 58, 11, 0x808080);
    gui.addLabel(4, "------------------------------------------------------------------------", 18, 39, 424, 11, 0x555555);
    gui.addLabel(5, section, 18, 52, 424, 11, 0xFFD75A);
}

function questItemName(def) {
    if (def == null) return "<?>";
    try {
        var stack = API.createItem(def.id, 0, 1);
        if (stack != null && !stack.isEmpty()) {
            var display = String(stack.getDisplayName());
            if (display != null && display.length > 0) return display;
        }
    } catch (ignoredDisplayName) {}
    try {
        if (def.name != null && String(def.name).length > 0) return String(def.name);
    } catch (ignoredFallback) {}
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

function currentStage
'@
$RewardFunc = @'
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
'@
$OpenStage1 = @'
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
'@
$OpenStage2 = @'
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
'@
$ResourceStage = @'
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
'@
$ProjectNotice = @'
function addProjectNotice(gui, notice) {
    if (notice == null || String(notice).length == 0) return;
    var lines = wrap(String(notice), 72);
    if (lines.length > 2) lines = lines.slice(0, 2);
    for (var i = 0; i < lines.length; i++) {
        gui.addLabel(90 + i, lines[i], 22, 292 + i * 12, 416, 11, 0xFFD45A);
    }
}

function openProject
'@
$Stage9 = @'
function openStage9Locked(player, notice) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "СЛЕДУЮЩАЯ ГЛАВА / ПРОГРАММА «ИСХОД»");
    gui.addLabel(20, "Базовая стабилизация купола завершена", 20, 80, 420, 11, 0x55FF55);
    addLines(
        gui, 30, 28, 104, 410, 0xEEEEEE,
        "Следующий цикл будет посвящён дальней связи, научной инфраструктуре, подготовке длительных экспедиций и переходу к космической программе.",
        70, 5
    );
    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 180, 333, 100, 18, 7620);
    player.showCustomGui(gui);
}
'@
$JavaConstants = @'
    private static final String STAGE1_COMPLETE_KEY = "domesurvival.stage01.complete.v71";
    private static final String STAGE2_EXTRAS_COMPLETE_KEY = "domesurvival.stage02.extras.complete.v71";
    private static final String STAGE2_RESET_CORE_COMPLETE_KEY = "domesurvival.stage02.resetcore.complete.v70";
    private static final String STAGE3_COMPLETE_KEY = "domesurvival.stage03.complete.v71";
    private static final String STAGE4_COMPLETE_KEY = "domesurvival.stage04.complete.v71";
    private static final String STAGE5_COMPLETE_KEY = "domesurvival.stage05.complete.v71";
    private static final String STAGE6_COMPLETE_KEY = "domesurvival.stage06.complete.v71";
    private static final String STAGE7_COMPLETE_KEY = "domesurvival.stage07.complete.v71";
    private static final String STAGE8_COMPLETE_KEY = "domesurvival.stage08.complete.v71";

'@
$JavaMethod = @'
    private static int completeNextStage(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IData data = getQuestStoredData(player);

        if (data == null) {
            source.sendFailure(Component.literal(
                "[JosephScript] CustomNPCs world StoredData is unavailable."
            ));
            return 0;
        }

        int stage = currentQuestStageForTesting(data);
        if (stage > 8) {
            source.sendSuccess(() -> Component.literal(
                "[JosephScript] Все тестируемые этапы 01-08 уже завершены."
            ), false);
            return 1;
        }

        switch (stage) {
            case 1 -> data.put(STAGE1_COMPLETE_KEY, 1);
            case 2 -> {
                data.put(STAGE2_EXTRAS_COMPLETE_KEY, 1);

                if (storedFlag(data, RESET_LOCK_KEY)) {
                    data.put(STAGE2_RESET_CORE_COMPLETE_KEY, 1);
                } else {
                    DomeProgressSavedData progress = DomeProgressSavedData.get(player.serverLevel());
                    progress.addWorkshopContribution(
                        WorkshopProject.IRON_REQUIRED,
                        WorkshopProject.COPPER_REQUIRED,
                        WorkshopProject.REDSTONE_REQUIRED
                    );
                }
            }
            case 3 -> data.put(STAGE3_COMPLETE_KEY, 1);
            case 4 -> data.put(STAGE4_COMPLETE_KEY, 1);
            case 5 -> data.put(STAGE5_COMPLETE_KEY, 1);
            case 6 -> data.put(STAGE6_COMPLETE_KEY, 1);
            case 7 -> data.put(STAGE7_COMPLETE_KEY, 1);
            case 8 -> data.put(STAGE8_COMPLETE_KEY, 1);
            default -> {
                return 0;
            }
        }

        final int completedStage = stage;
        source.sendSuccess(() -> Component.literal(
            String.format(
                Locale.ROOT,
                "[JosephScript] ТЕСТ: этап %02d завершён без сдачи предметов. ПКМ по Джозефу — получить награду и открыть следующий этап.",
                completedStage
            )
        ), true);

        if (stage == 2 && !storedFlag(data, RESET_LOCK_KEY)) {
            source.sendSuccess(() -> Component.literal(
                "[JosephScript] nextstage заполняет прогресс материалов мастерской, но не используется для проверки её физического строительства. Для этого проходи этап 02 обычной сдачей ресурсов."
            ), false);
        }

        return 1;
    }

    private static int currentQuestStageForTesting(IData data) {
        if (!storedFlag(data, STAGE1_COMPLETE_KEY)) return 1;

        boolean stage2Core = storedFlag(data, RESET_LOCK_KEY)
            ? storedFlag(data, STAGE2_RESET_CORE_COMPLETE_KEY)
            : isJavaWorkshopAlreadyComplete();
        if (!stage2Core || !storedFlag(data, STAGE2_EXTRAS_COMPLETE_KEY)) return 2;

        if (!storedFlag(data, STAGE3_COMPLETE_KEY)) return 3;
        if (!storedFlag(data, STAGE4_COMPLETE_KEY)) return 4;
        if (!storedFlag(data, STAGE5_COMPLETE_KEY)) return 5;
        if (!storedFlag(data, STAGE6_COMPLETE_KEY)) return 6;
        if (!storedFlag(data, STAGE7_COMPLETE_KEY)) return 7;
        if (!storedFlag(data, STAGE8_COMPLETE_KEY)) return 8;
        return 9;
    }

    private static IData getQuestStoredData(ServerPlayer player) {
        try {
            NpcAPI api = NpcAPI.Instance();
            if (api == null) return null;

            IWorld world = api.getIWorld(player.serverLevel());
            if (world == null) return null;

            return world.getStoreddata();
        } catch (Throwable error) {
            error.printStackTrace();
            return null;
        }
    }

    private static boolean storedFlag(IData data, String key) {
        if (data == null || key == null || !data.has(key)) return false;
        try {
            Object raw = data.get(key);
            if (raw instanceof Number number) return number.intValue() > 0;
            return Integer.parseInt(String.valueOf(raw)) > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }


'@


$Text = Replace-RequiredRegex $Text `
    '/\* Stage 01 - Life support:.*?var lastInteractByPlayer = \{\};' `
    $StageBlock `
    "stage and reward definitions"

$Text = Replace-RequiredRegex $Text `
    'function addProjectHeader\(gui, section\) \{.*?function currentStage' `
    $RenderBlock `
    "project rendering"

$Text = Replace-RequiredRegex $Text `
    'function grantStageReward\(player, rewardKey, rewards\) \{.*?\n\}' `
    $RewardFunc `
    "reward delivery"

$Text = Replace-RequiredRegex $Text `
    'function openStage1\(player, notice\) \{.*?\n\}' `
    $OpenStage1 `
    "openStage1"

$Text = Replace-RequiredRegex $Text `
    'function openStage2\(player, notice\) \{.*?\n\}' `
    $OpenStage2 `
    "openStage2"

$Text = Replace-RequiredRegex $Text `
    'function openResourceStage\(player, notice, header, description, defs, completeKey, completeText, buttonText\) \{.*?\n\}' `
    $ResourceStage `
    "openResourceStage"

$Text = Replace-RequiredRegex $Text `
    'function addProjectNotice\(gui, notice\) \{.*?function openProject' `
    $ProjectNotice `
    "addProjectNotice"

$Text = Replace-RequiredRegex $Text `
    'function openStage9Locked\(player, notice\) \{.*?\n\}' `
    $Stage9 `
    "openStage9Locked"

$Text = $Text.Replace(
    'function makeProjectGui(player) { return API.createCustomGui(GUI_NAME + "_project", 440, 300, false, player); }',
    'function makeProjectGui(player) { return API.createCustomGui(GUI_NAME + "_project", 460, 360, false, player); }'
)

# Stage titles/descriptions updated to reflect the broader multimod survival loop.
$Text = $Text.Replace('if (stage == 4) return "Вода и фильтрация";','if (stage == 4) return "Вода и агроконтур";')
$Text = $Text.Replace('"ПРОЕКТ 04 / ВОДА И ФИЛЬТРАЦИЯ"','"ПРОЕКТ 04 / ВОДА И АГРОКОНТУР"')
$Text = $Text.Replace('"Очистка воды и резерв жидкостей"','"Очистка воды и плодородная почва внутри купола"')
$Text = $Text.Replace('"Водоочистка"','"Вода / агроконтур"')
$Text = $Text.Replace('"04  Вода и фильтрация"','"04  Вода и агроконтур"')
$Text = $Text.Replace('"Дублирование критических запасов и узлов"','"Резерв техники, продовольствия и ферментированных запасов"')

$RequiredJs = @(
    'GUI v7.3 MULTIMOD + TEST SKIP',
    'farmersdelight:cutting_board',
    'farmersdelight:rice',
    'brewinandchewin:keg',
    'brewinandchewin:beer',
    'brewinandchewin:mead',
    'mekanism:advanced_control_circuit',
    'immersiveengineering:component_electronic_adv',
    'enderio:dark_steel_ingot',
    'function questItemName(def)',
    '460, 360'
)
foreach ($Marker in $RequiredJs) {
    if (-not $Text.Contains($Marker)) { throw "JS validation failed. Missing marker: $Marker" }
}

[IO.File]::WriteAllText($Joseph,$Text,$Utf8NoBom)

# ----- Java dev command: /josephscript nextstage -----
$Java = [IO.File]::ReadAllText($CommandJava,[Text.Encoding]::UTF8)

if (-not $Java.Contains('import com.wasted.domesurvival.forge.progression.DomeProgressSavedData;')) {
    $Java = $Java.Replace(
        'import com.wasted.domesurvival.forge.progression.WorkshopUpgradeApplier;',
        "import com.wasted.domesurvival.forge.progression.DomeProgressSavedData;`r`nimport com.wasted.domesurvival.forge.progression.WorkshopProject;`r`nimport com.wasted.domesurvival.forge.progression.WorkshopUpgradeApplier;"
    )
}

if (-not $Java.Contains('STAGE8_COMPLETE_KEY')) {
    $Java = $Java.Replace(
        '    private static final String PREPARED_KEY = "domesurvival.release.prepared.v70";',
        '    private static final String PREPARED_KEY = "domesurvival.release.prepared.v70";' + "`r`n" + $JavaConstants.TrimEnd()
    )
}

if (-not $Java.Contains('Commands.literal("nextstage")')) {
    $Java = $Java.Replace(
        '                .then(Commands.literal("resetprogress")' + "`r`n" + '                    .executes(context -> resetProgress(context.getSource())))',
        '                .then(Commands.literal("resetprogress")' + "`r`n" + '                    .executes(context -> resetProgress(context.getSource())))' + "`r`n" +
        '                .then(Commands.literal("nextstage")' + "`r`n" + '                    .executes(context -> completeNextStage(context.getSource())))'
    )
}

if (-not $Java.Contains('private static int completeNextStage(')) {
    $Java = $Java.Replace(
        '    private static int resetProgress(CommandSourceStack source) throws CommandSyntaxException {',
        $JavaMethod + '    private static int resetProgress(CommandSourceStack source) throws CommandSyntaxException {'
    )
}

$Java = $Java.Replace(
    '"[JosephScript] Right-click Joseph: Stage 01 should be active, Stages 02-09 locked."',
    '"[JosephScript] Right-click Joseph: Stage 01 should be active, Stages 02-09 locked. Test helper: /josephscript nextstage"'
)

$RequiredJava = @(
    'Commands.literal("nextstage")',
    'private static int completeNextStage(',
    'DomeProgressSavedData.get(player.serverLevel())',
    'WorkshopProject.IRON_REQUIRED',
    'STAGE8_COMPLETE_KEY'
)
foreach ($Marker in $RequiredJava) {
    if (-not $Java.Contains($Marker)) { throw "Java validation failed. Missing marker: $Marker" }
}

[IO.File]::WriteAllText($CommandJava,$Java,$Utf8NoBom)

# Refresh CustomNPCs external script copies.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))
$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) { $Targets.Add($WorldLocal) }

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $Target = Join-Path $TargetDir "joseph_cooper_gui.js"
    Copy-Item -LiteralPath $Joseph -Destination $Target -Force
    $A = (Get-FileHash -LiteralPath $Joseph -Algorithm SHA256).Hash
    $B = (Get-FileHash -LiteralPath $Target -Algorithm SHA256).Hash
    if ($A -ne $B) { throw "Script verification failed: $Target" }
    Write-Host "[OK] Joseph script -> $Target" -ForegroundColor Green
}

Write-Host ""
Write-Host "[OK] Joseph Questline V7.3 applied." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Added quest integration: Farmer's Delight, Brewin' And Chewin', Mekanism, Immersive Engineering, Ender IO."
Write-Host "Added test command: /josephscript nextstage"
Write-Host "The command completes ONLY the currently active stage and does not consume items."
Write-Host ""
Write-Host "Next: .\dev\RUN_DEV_FULL.bat"
exit 0
