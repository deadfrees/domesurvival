$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
$CommandJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\integration\customnpcs\JosephScriptCommand.java"

if (-not (Test-Path -LiteralPath $Joseph)) {
    throw "Joseph GUI source not found: $Joseph"
}
if (-not (Test-Path -LiteralPath $CommandJava)) {
    throw "JosephScriptCommand.java not found: $CommandJava"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_questline_v71_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force
Copy-Item -LiteralPath $CommandJava -Destination (Join-Path $Backup "JosephScriptCommand.java") -Force

function Replace-RequiredRegex {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Replacement,
        [string]$Label
    )

    $rx = [regex]::new(
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )

    $matches = $rx.Matches($Text)
    if ($matches.Count -ne 1) {
        throw "Patch anchor '$Label' expected exactly 1 match, found $($matches.Count). Source version is not the expected Joseph v7.0/v7.1 base."
    }

    return $rx.Replace(
        $Text,
        [System.Text.RegularExpressions.MatchEvaluator]{
            param($m)
            return $Replacement
        },
        1
    )
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Joseph, [Text.Encoding]::UTF8)

# Version marker.
$Text = $Text.Replace(
    "/* Dome Survival - Joseph Cooper GUI v7.0 RELEASE RESET */",
    "/* Dome Survival - Joseph Cooper GUI v7.1 SURVIVAL QUESTLINE */"
)
$Text = $Text.Replace(
    'var GUI_NAME = "dome_joseph_v70_release_reset";',
    'var GUI_NAME = "dome_joseph_v71_survival_questline";'
)

$StageBlock = @'
/* Stage 01 - Life support: only resources realistically available before the base is industrialized. */
var S1_COMPLETE = "domesurvival.stage01.complete.v71";
var S1 = [
    { key: "domesurvival.stage01.logs.v71",     id: "minecraft:oak_log",      name: "Дубовые брёвна",   req: 32 },
    { key: "domesurvival.stage01.saplings.v71", id: "minecraft:oak_sapling",  name: "Саженцы дуба",     req: 8 },
    { key: "domesurvival.stage01.wheat.v71",    id: "minecraft:wheat",        name: "Пшеница",          req: 24 },
    { key: "domesurvival.stage01.charcoal.v71", id: "minecraft:charcoal",     name: "Древесный уголь",  req: 16 },
    { key: "domesurvival.stage01.glass.v71",    id: "minecraft:glass",        name: "Стекло",           req: 32 },
    { key: "domesurvival.stage01.copper.v71",   id: "minecraft:copper_ingot", name: "Медные слитки",    req: 12 }
];

/* Stage 02 - Workshop. Java Bridge still owns the core 64 iron / 32 copper / 24 redstone. */
var S2_EXTRA_COMPLETE = "domesurvival.stage02.extras.complete.v71";
var S2 = [
    { key: "domesurvival.stage02.stone.v71",    id: "minecraft:stone_bricks",          name: "Каменные кирпичи", req: 48 },
    { key: "domesurvival.stage02.bars.v71",     id: "minecraft:iron_bars",             name: "Железные прутья",  req: 16 },
    { key: "domesurvival.stage02.panes.v71",    id: "minecraft:glass_pane",            name: "Стеклянные панели",req: 24 },
    { key: "domesurvival.stage02.piston.v71",   id: "minecraft:piston",                name: "Поршни",           req: 6 },
    { key: "domesurvival.stage02.furnace.v71",  id: "domesurvival:copper_furnace",    name: "Медная печь",      req: 1 },
    { key: "domesurvival.stage02.hopper.v71",   id: "domesurvival:copper_hopper",     name: "Медная воронка",   req: 1 }
];

/* Logical-reset fallback for old test worlds where Java workshop SavedData is already complete. */
var RELEASE_RESET_LOCK = "domesurvival.release.logical_reset.v70";
var S2_RESET_CORE_COMPLETE = "domesurvival.stage02.resetcore.complete.v70";
var S2_RESET_CORE = [
    { key: "domesurvival.stage02.resetcore.iron.v70",     id: "minecraft:iron_ingot",   name: "Железо",   req: 64 },
    { key: "domesurvival.stage02.resetcore.copper.v70",   id: "minecraft:copper_ingot", name: "Медь",     req: 32 },
    { key: "domesurvival.stage02.resetcore.redstone.v70", id: "minecraft:redstone",     name: "Редстоун", req: 24 }
];

/* Stage 03 - Emergency power. Workshop exists, but leaving the dome is still dangerous. */
var S3_COMPLETE = "domesurvival.stage03.complete.v71";
var S3 = [
    { key: "domesurvival.stage03.generator.v71",  id: "domesurvival:coal_generator",      name: "Угольный генератор", req: 1 },
    { key: "domesurvival.stage03.matrix.v71",     id: "domesurvival:pulse_matrix",        name: "Импульсная матрица", req: 2 },
    { key: "domesurvival.stage03.stabilizer.v71", id: "domesurvival:machine_stabilizer",  name: "Стабилизатор",       req: 1 },
    { key: "domesurvival.stage03.darksteel.v71",  id: "enderio:dark_steel_ingot",         name: "Тёмная сталь",       req: 2 },
    { key: "domesurvival.stage03.coal.v71",       id: "minecraft:coal",                   name: "Уголь",              req: 64 },
    { key: "domesurvival.stage03.redstone.v71",   id: "minecraft:redstone",               name: "Редстоун",           req: 24 }
];

/* Stage 04 - Water purification inside the dome. */
var S4_COMPLETE = "domesurvival.stage04.complete.v71";
var S4 = [
    { key: "domesurvival.stage04.filter.v71",    id: "domesurvival:water_filter_cartridge", name: "Картриджи фильтра", req: 4 },
    { key: "domesurvival.stage04.ifilter.v71",   id: "domesurvival:improved_water_filter",  name: "Улучшенный фильтр", req: 1 },
    { key: "domesurvival.stage04.pipe.v71",      id: "domesurvival:basic_fluid_pipe",       name: "Жидкостные трубы",  req: 12 },
    { key: "domesurvival.stage04.tank.v71",      id: "domesurvival:universal_tank",         name: "Универсальный бак", req: 1 },
    { key: "domesurvival.stage04.copper.v71",    id: "minecraft:copper_ingot",              name: "Медные слитки",     req: 16 },
    { key: "domesurvival.stage04.glass.v71",     id: "minecraft:glass",                     name: "Стекло",            req: 16 }
];

/* Stage 05 - Oxygen infrastructure. Completion creates the first controlled outside-work capability. */
var S5_COMPLETE = "domesurvival.stage05.complete.v71";
var S5 = [
    { key: "domesurvival.stage05.matrix.v71",     id: "domesurvival:pulse_matrix",       name: "Импульсные матрицы", req: 2 },
    { key: "domesurvival.stage05.stabilizer.v71", id: "domesurvival:machine_stabilizer", name: "Стабилизатор",       req: 1 },
    { key: "domesurvival.stage05.pipe.v71",       id: "domesurvival:oxygen_pipe",        name: "Кислородные трубы",  req: 16 },
    { key: "domesurvival.stage05.tank.v71",       id: "domesurvival:small_oxygen_tank",  name: "Малые баллоны",      req: 2 },
    { key: "domesurvival.stage05.darksteel.v71",  id: "enderio:dark_steel_ingot",        name: "Тёмная сталь",       req: 4 },
    { key: "domesurvival.stage05.redstone.v71",   id: "minecraft:redstone",              name: "Редстоун",           req: 24 }
];

/* Stage 06 - First real outside expedition after oxygen infrastructure exists. */
var S6_COMPLETE = "domesurvival.stage06.complete.v71";
var S6 = [
    { key: "domesurvival.stage06.rawiron.v71",   id: "minecraft:raw_iron",    name: "Необработанное железо", req: 32 },
    { key: "domesurvival.stage06.rawcopper.v71", id: "minecraft:raw_copper",  name: "Необработанная медь",   req: 48 },
    { key: "domesurvival.stage06.coal.v71",      id: "minecraft:coal",        name: "Уголь",                 req: 64 },
    { key: "domesurvival.stage06.redstone.v71",  id: "minecraft:redstone",    name: "Редстоун",              req: 32 },
    { key: "domesurvival.stage06.gold.v71",      id: "minecraft:gold_ingot",  name: "Золотые слитки",        req: 12 },
    { key: "domesurvival.stage06.diamond.v71",   id: "minecraft:diamond",     name: "Алмазы",                req: 4 }
];

/* Stage 07 - Internal logistics: item/fluid/energy/oxygen distribution. */
var S7_COMPLETE = "domesurvival.stage07.complete.v71";
var S7 = [
    { key: "domesurvival.stage07.itempipe.v71",  id: "domesurvival:copper_item_pipe",   name: "Медные предметные трубы", req: 16 },
    { key: "domesurvival.stage07.filterpipe.v71",id: "domesurvival:filtering_item_pipe",name: "Фильтрующие трубы",       req: 4 },
    { key: "domesurvival.stage07.hopper.v71",    id: "domesurvival:copper_hopper",      name: "Медные воронки",          req: 4 },
    { key: "domesurvival.stage07.energypipe.v71",id: "domesurvival:basic_energy_pipe",  name: "Энергетические трубы",    req: 8 },
    { key: "domesurvival.stage07.fluidpipe.v71", id: "domesurvival:basic_fluid_pipe",   name: "Жидкостные трубы",        req: 8 },
    { key: "domesurvival.stage07.o2pipe.v71",    id: "domesurvival:oxygen_pipe",        name: "Кислородные трубы",       req: 8 }
];

/* Stage 08 - Base reserve. No new building: a stockpile and resilient infrastructure milestone. */
var S8_COMPLETE = "domesurvival.stage08.complete.v71";
var S8 = [
    { key: "domesurvival.stage08.buffer.v71",    id: "domesurvival:energy_buffer",           name: "Энергоблок",           req: 1 },
    { key: "domesurvival.stage08.tank.v71",      id: "domesurvival:universal_tank",          name: "Универсальные баки",   req: 2 },
    { key: "domesurvival.stage08.filter.v71",    id: "domesurvival:industrial_water_filter", name: "Промышленный фильтр",  req: 1 },
    { key: "domesurvival.stage08.steelpipe.v71", id: "domesurvival:steel_item_pipe",         name: "Стальные предметные трубы", req: 8 },
    { key: "domesurvival.stage08.steelhop.v71",  id: "domesurvival:steel_hopper",            name: "Стальные воронки",     req: 2 },
    { key: "domesurvival.stage08.darksteel.v71", id: "enderio:dark_steel_ingot",             name: "Тёмная сталь",         req: 8 }
];

/* One-time shared base reward packages. The completing player physically receives the supplies. */
var R1_KEY = "domesurvival.stage01.reward.v71";
var R1 = [
    { id: "minecraft:bread", count: 12, name: "Хлеб" },
    { id: "minecraft:torch", count: 24, name: "Факелы" },
    { id: "minecraft:bone_meal", count: 16, name: "Костная мука" }
];

var R2_KEY = "domesurvival.stage02.reward.v71";
var R2 = [
    { id: "domesurvival:machine_wrench", count: 1, name: "Машинный ключ" },
    { id: "domesurvival:copper_item_pipe", count: 8, name: "Медные предметные трубы" },
    { id: "domesurvival:basic_fluid_pipe", count: 8, name: "Жидкостные трубы" },
    { id: "domesurvival:copper_hopper", count: 2, name: "Медные воронки" }
];

var R3_KEY = "domesurvival.stage03.reward.v71";
var R3 = [
    { id: "domesurvival:energy_buffer", count: 1, name: "Энергоблок" },
    { id: "domesurvival:basic_energy_pipe", count: 12, name: "Энергетические трубы" },
    { id: "minecraft:coal", count: 32, name: "Резервный уголь" }
];

var R4_KEY = "domesurvival.stage04.reward.v71";
var R4 = [
    { id: "domesurvival:water_purifier", count: 1, name: "Очиститель воды" },
    { id: "domesurvival:universal_tank", count: 1, name: "Универсальный бак" },
    { id: "domesurvival:reinforced_fluid_pipe", count: 8, name: "Усиленные жидкостные трубы" },
    { id: "domesurvival:water_filter_cartridge", count: 2, name: "Запасные картриджи" }
];

var R5_KEY = "domesurvival.stage05.reward.v71";
var R5 = [
    { id: "domesurvival:oxygen_electrolyzer", count: 1, name: "Кислородный электролизёр" },
    { id: "domesurvival:oxygen_filler", count: 1, name: "Заправщик баллонов" },
    { id: "domesurvival:oxygen_mask", count: 2, name: "Кислородные маски" },
    { id: "domesurvival:medium_oxygen_tank", count: 2, name: "Средние баллоны" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 8, name: "Усиленные кислородные трубы" }
];

var R6_KEY = "domesurvival.stage06.reward.v71";
var R6 = [
    { id: "domesurvival:surface_suit_helmet", count: 1, name: "Шлем костюма" },
    { id: "domesurvival:surface_suit_chestplate", count: 1, name: "Куртка костюма" },
    { id: "domesurvival:surface_suit_leggings", count: 1, name: "Штаны костюма" },
    { id: "domesurvival:surface_suit_boots", count: 1, name: "Ботинки костюма" },
    { id: "domesurvival:large_oxygen_tank", count: 1, name: "Большой баллон" },
    { id: "minecraft:cooked_beef", count: 16, name: "Экспедиционный паёк" }
];

var R7_KEY = "domesurvival.stage07.reward.v71";
var R7 = [
    { id: "domesurvival:steel_item_pipe", count: 12, name: "Стальные предметные трубы" },
    { id: "domesurvival:steel_hopper", count: 2, name: "Стальные воронки" },
    { id: "domesurvival:reinforced_energy_pipe", count: 8, name: "Усиленные энерготрубы" },
    { id: "domesurvival:reinforced_fluid_pipe", count: 8, name: "Усиленные жидкостные трубы" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 8, name: "Усиленные кислородные трубы" },
    { id: "domesurvival:machine_wrench", count: 1, name: "Машинный ключ" }
];

var R8_KEY = "domesurvival.stage08.reward.v71";
var R8 = [
    { id: "domesurvival:high_voltage_energy_pipe", count: 8, name: "Высоковольтные энерготрубы" },
    { id: "domesurvival:reinforced_glass", count: 16, name: "Усиленное стекло" },
    { id: "domesurvival:large_oxygen_tank", count: 2, name: "Большие баллоны" },
    { id: "domesurvival:industrial_water_filter", count: 1, name: "Резервный промышленный фильтр" },
    { id: "domesurvival:filtering_item_pipe", count: 4, name: "Фильтрующие трубы" }
];

var lastInteractByPlayer = {};
'@
$CustomButton = @'
function customGuiButton(e) {
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
            notices1.push(grantStageReward(e.player, R1_KEY, R1));
            celebrate(e.player,
                "§a[КУПОЛ] Этап 01 «Жизнеобеспечение купола» завершён!",
                "§e[КУПОЛ] Доступен этап 02: «Восстановление мастерской»."
            );
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
            celebrate(e.player,
                "§a[КУПОЛ] Этап 02 «Восстановление мастерской» завершён!",
                "§e[КУПОЛ] Доступен этап 03: «Аварийное энергоснабжение»."
            );
        }

        openProject(e.player, compactNotice(notices2));
        return;
    }

    if (!stage3Complete(e.player)) {
        completeSimpleStage(e.player, S3, S3_COMPLETE, R3_KEY, R3,
            "§a[КУПОЛ] Этап 03 «Аварийное энергоснабжение» завершён!",
            "§e[КУПОЛ] Доступен этап 04: «Вода и фильтрация».");
        return;
    }

    if (!stage4Complete(e.player)) {
        completeSimpleStage(e.player, S4, S4_COMPLETE, R4_KEY, R4,
            "§a[КУПОЛ] Этап 04 «Вода и фильтрация» завершён!",
            "§e[КУПОЛ] Доступен этап 05: «Кислородный контур».");
        return;
    }

    if (!stage5Complete(e.player)) {
        completeSimpleStage(e.player, S5, S5_COMPLETE, R5_KEY, R5,
            "§a[КУПОЛ] Этап 05 «Кислородный контур» завершён!",
            "§e[КУПОЛ] Доступен этап 06: «Первая внешняя экспедиция».");
        return;
    }

    if (!stage6Complete(e.player)) {
        completeSimpleStage(e.player, S6, S6_COMPLETE, R6_KEY, R6,
            "§a[КУПОЛ] Этап 06 «Первая внешняя экспедиция» завершён!",
            "§e[КУПОЛ] Доступен этап 07: «Логистика купола».");
        return;
    }

    if (!stage7Complete(e.player)) {
        completeSimpleStage(e.player, S7, S7_COMPLETE, R7_KEY, R7,
            "§a[КУПОЛ] Этап 07 «Логистика купола» завершён!",
            "§e[КУПОЛ] Доступен этап 08: «Аварийный резерв базы».");
        return;
    }

    if (!stage8Complete(e.player)) {
        completeSimpleStage(e.player, S8, S8_COMPLETE, R8_KEY, R8,
            "§a[КУПОЛ] Этап 08 «Аварийный резерв базы» завершён!",
            "§e[КУПОЛ] Базовый цикл стабилизации завершён. Открыта программа «Исход».");
        return;
    }

    openProject(e.player, "Базовый цикл завершён. Следующая глава — программа «Исход».");
}

function getStored(player) {
'@
$RewardHelpers = @'

function rewardAlreadyClaimed(player, rewardKey) {
    return readInt(getStored(player), rewardKey) > 0;
}

function grantStageReward(player, rewardKey, rewards) {
    if (rewardAlreadyClaimed(player, rewardKey)) return "";

    var data = getStored(player);
    if (data == null) return "Награда подготовлена, но хранилище прогресса временно недоступно.";

    var playerName = String(player.getName());
    var granted = [];

    for (var i = 0; i < rewards.length; i++) {
        var reward = rewards[i];
        var command = "give " + playerName + " " + reward.id + " " + reward.count;

        if (executeWorldCommand(player, command)) {
            granted.push(reward.name + " x" + reward.count);
        }
    }

    writeInt(data, rewardKey, 1);

    if (granted.length == 0) return "Этап завершён. Награда отмечена как выданная.";
    return "Награда базы: " + granted.join(", ");
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


'@
$StageCompletion = @'
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

function grantPendingRewards(player) {
    if (stage1Complete(player)) grantStageReward(player, R1_KEY, R1);
    if (stage2Complete(player)) grantStageReward(player, R2_KEY, R2);
    if (stage3Complete(player)) grantStageReward(player, R3_KEY, R3);
    if (stage4Complete(player)) grantStageReward(player, R4_KEY, R4);
    if (stage5Complete(player)) grantStageReward(player, R5_KEY, R5);
    if (stage6Complete(player)) grantStageReward(player, R6_KEY, R6);
    if (stage7Complete(player)) grantStageReward(player, R7_KEY, R7);
    if (stage8Complete(player)) grantStageReward(player, R8_KEY, R8);
}

function inventoryCount(player, id) {
'@
$CompactNotice = @'
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
'@
$CurrentFuncs = @'
function currentStage(player) {
    if (!stage1Complete(player)) return 1;
    if (!stage2Complete(player)) return 2;
    if (!stage3Complete(player)) return 3;
    if (!stage4Complete(player)) return 4;
    if (!stage5Complete(player)) return 5;
    if (!stage6Complete(player)) return 6;
    if (!stage7Complete(player)) return 7;
    if (!stage8Complete(player)) return 8;
    return 9;
}

function currentProjectTitle(player) {
    var stage = currentStage(player);
    if (stage == 1) return "Жизнеобеспечение купола";
    if (stage == 2) {
        try { return String(Bridge.projectTitle()); } catch (ignored) { return "Восстановление мастерской"; }
    }
    if (stage == 3) return "Аварийное энергоснабжение";
    if (stage == 4) return "Вода и фильтрация";
    if (stage == 5) return "Кислородный контур";
    if (stage == 6) return "Первая внешняя экспедиция";
    if (stage == 7) return "Логистика купола";
    if (stage == 8) return "Аварийный резерв базы";
    return "Программа «Исход»";
}
'@
$OpenMain = @'
function openMain(player) {
    var gui = makeGui(player);
    addHeader(gui, "СПИСОК / ТЕКУЩАЯ ОБСТАНОВКА");

    var stage = currentStage(player);
    var narrative = "";

    if (stage == 1) narrative = "Сначала удерживаем жизнь внутри купола: древесина, пища, стекло, топливо и минимальный запас металла.";
    else if (stage == 2) narrative = "Жизнеобеспечение стабилизировано. Восстанавливаем мастерскую — центральную точку ремонта и сборки.";
    else if (stage == 3) narrative = "Мастерская работает. Следующая угроза — отключение энергии. Собираем аварийный энергоконтур.";
    else if (stage == 4) narrative = "Электропитание есть. Теперь нужна собственная очистка воды и резерв хранения жидкости.";
    else if (stage == 5) narrative = "Вода стабилизирована. Создаём кислородный контур, чтобы вылазки перестали быть почти самоубийственными.";
    else if (stage == 6) narrative = "Кислородное оборудование готово. Проводим первую серьёзную экспедицию и возвращаем стратегическое сырьё.";
    else if (stage == 7) narrative = "Запасы появились, но база захлёбывается ручной переноской. Строим внутреннюю логистическую сеть.";
    else if (stage == 8) narrative = "Основные системы работают. Формируем аварийный резерв, чтобы один отказ больше не ставил купол на грань гибели.";
    else narrative = "Купол стабилизирован. Теперь можно переходить от выживания к большой цели — программе «Исход» и подготовке будущих космических работ.";

    addLines(gui, 20, 18, 72, 282, 0xEEEEEE, narrative, 52, 5);

    gui.addLabel(80, "Текущий проект:", 18, 134, 96, 11, 0xAAAAAA);
    gui.addLabel(81, currentProjectTitle(player), 116, 134, 190, 11, 0xFFFFFF);
    gui.addLabel(82, stage == 9 ? "[ ГЛАВА ЗАВЕРШЕНА ]" : "[ В РАБОТЕ ]", 202, 147, 112, 11, 0xFFD45A);

    addVisibleButton(gui, BTN_PROJECT, "Проект", 18, 160, 136, 20, 7601);
    addVisibleButton(gui, BTN_BASE, "Состояние базы", 166, 160, 136, 20, 7602);
    addVisibleButton(gui, BTN_PLAN, "План развития", 18, 190, 136, 20, 7603);
    addVisibleButton(gui, BTN_CLOSE, "Закрыть", 166, 190, 136, 20, 7604);

    player.showCustomGui(gui);
}

function addNotice
'@
$OpenProject = @'
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

    openStage9Locked(player, notice);
}
'@
$GenericStages = @'
function openResourceStage(player, notice, header, description, defs, completeKey, completeText, buttonText) {
    var gui = makeGui(player);
    addHeader(gui, header);
    gui.addLabel(20, description, 18, 72, 284, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 18, 92, 140, 11, 0xAAAAAA);

    var state = setState(player, defs, completeKey);
    renderSetGrid(gui, 30, defs, state, 108);

    if (state.complete) {
        gui.addLabel(70, "Статус", 18, 158, 80, 11, 0xAAAAAA);
        gui.addLabel(71, completeText, 28, 174, 276, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, buttonText, 75, 168, 170, 20, 7610);
    }

    addNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
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
        "ПРОЕКТ 04 / ВОДА И ФИЛЬТРАЦИЯ",
        "Очистка воды и резерв жидкостей",
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
        "Дублирование критических запасов и узлов",
        S8, S8_COMPLETE,
        "Аварийный резерв сформирован. Базовый цикл завершён.",
        "Передать резерв"
    );
}

function openStage9Locked(player, notice) {
    var gui = makeGui(player);
    addHeader(gui, "СЛЕДУЮЩАЯ ГЛАВА / ПРОГРАММА «ИСХОД»");
    gui.addLabel(20, "Базовая стабилизация купола завершена", 18, 80, 284, 11, 0x55FF55);
    addLines(
        gui, 30, 28, 104, 270, 0xEEEEEE,
        "Следующий цикл будет посвящён дальней связи, научной инфраструктуре, подготовке длительных экспедиций и переходу к космической программе.",
        48, 5
    );
    addNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openBase
'@
$OpenBase = @'
function openBase(player) {
    var gui = makeGui(player);
    addHeader(gui, "СОСТОЯНИЕ БАЗЫ");

    var s1 = stage1Complete(player);
    var s2 = stage2Complete(player);
    var s3 = stage3Complete(player);
    var s4 = stage4Complete(player);
    var s5 = stage5Complete(player);
    var s6 = stage6Complete(player);
    var s7 = stage7Complete(player);
    var s8 = stage8Complete(player);
    var built = logicalResetActive(player) ? s2 : workshopBuilt();

    var rows = [
        { name: "Жизнеобеспечение", value: s1 ? "Стабильно" : "Формируется", ok: s1 },
        { name: "Мастерская", value: s2 ? (built ? "Восстановлена" : "Готова") : "Не восстановлена", ok: s2 },
        { name: "Энергоконтур", value: s3 ? "Резерв готов" : (s2 ? "Формируется" : "Заблокирован"), ok: s3 },
        { name: "Водоочистка", value: s4 ? "Стабильна" : (s3 ? "Формируется" : "Заблокирована"), ok: s4 },
        { name: "Кислород", value: s5 ? "Контур готов" : (s4 ? "Формируется" : "Заблокирован"), ok: s5 },
        { name: "Вылазки / логистика", value: s7 ? "Налажены" : (s6 ? "Логистика формируется" : (s5 ? "Экспедиция готовится" : "Заблокированы")), ok: s7 },
        { name: "Аварийный резерв", value: s8 ? "Сформирован" : (s7 ? "Формируется" : "Заблокирован"), ok: s8 }
    ];

    for (var i = 0; i < rows.length; i++) {
        var y = 70 + i * 17;
        gui.addLabel(20 + i * 2, rows[i].name, 20, y, 142, 11, 0xBBBBBB);
        gui.addLabel(21 + i * 2, rows[i].value, 166, y, 140, 11, rows[i].ok ? 0x55FF55 : 0xFFD45A);
    }

    var stage = currentStage(player);
    var priority = currentProjectTitle(player);
    gui.addLabel(60, "Приоритет:", 20, 190, 75, 11, 0xE6B84A);
    gui.addLabel(61, priority, 94, 190, 210, 11, stage == 9 ? 0x55FF55 : 0xEEEEEE);

    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openPlan
'@
$OpenPlan = @'
function openPlan(player) {
    var gui = makeGui(player);
    addHeader(gui, "ПЛАН РАЗВИТИЯ");

    var current = currentStage(player);
    var stages = [
        "01  Жизнеобеспечение купола",
        "02  Восстановление мастерской",
        "03  Аварийное энергоснабжение",
        "04  Вода и фильтрация",
        "05  Кислородный контур",
        "06  Первая внешняя экспедиция",
        "07  Логистика купола",
        "08  Аварийный резерв базы",
        "09  Программа «Исход»"
    ];

    for (var i = 0; i < stages.length; i++) {
        var stageNumber = i + 1;
        var color = 0x888888;

        if (stageNumber < current) color = 0x55FF55;
        else if (stageNumber == current) color = 0xFFD45A;

        gui.addLabel(20 + i, stages[i], 24, 70 + i * 14, 280, 11, color);
    }

    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

'@

$Text = Replace-RequiredRegex $Text `
    '/\* Stage 01 - Life support \*/.*?var lastInteractByPlayer = \{\};' `
    $StageBlock `
    "stage definitions"

$Text = Replace-RequiredRegex $Text `
    'function customGuiButton\(e\) \{.*?function getStored\(player\) \{' `
    $CustomButton `
    "customGuiButton"

# Insert reward helpers immediately before workshopCoreComplete().
if ($Text.Contains("function rewardAlreadyClaimed(player, rewardKey)")) {
    Write-Host "[INFO] Reward helpers already present."
}
else {
    $Text = $Text.Replace(
        "function workshopCoreComplete() {",
        $RewardHelpers + "`r`nfunction workshopCoreComplete() {"
    )
}

$Text = Replace-RequiredRegex $Text `
    'function stage3Complete\(player\) \{.*?function inventoryCount\(player, id\) \{' `
    $StageCompletion `
    "stage completion chain"

$Text = Replace-RequiredRegex $Text `
    'function compactNotice\(parts\) \{.*?\n\}' `
    $CompactNotice `
    "compactNotice"

$Text = Replace-RequiredRegex $Text `
    'function currentStage\(player\) \{.*?function currentProjectTitle\(player\) \{.*?\n\}' `
    $CurrentFuncs `
    "current stage/title"

$Text = Replace-RequiredRegex $Text `
    'function openMain\(player\) \{.*?function addNotice' `
    $OpenMain `
    "openMain"

$Text = Replace-RequiredRegex $Text `
    'function openProject\(player, notice\) \{.*?\n\}' `
    $OpenProject `
    "openProject"

$Text = Replace-RequiredRegex $Text `
    'function openStage3\(player, notice\) \{.*?function openBase' `
    $GenericStages `
    "stages 03-09"

$Text = Replace-RequiredRegex $Text `
    'function openBase\(player\) \{.*?function openPlan' `
    $OpenBase `
    "openBase"

$Text = Replace-RequiredRegex $Text `
    'function openPlan\(player\) \{.*\z' `
    $OpenPlan `
    "openPlan"

# Existing completed worlds receive any missing one-time base reward once.
$OldInteractTail = @'
    /* Existing saves which already finished the original workshop are treated as having finished Stage 01. */
    migrateStage1IfNeeded(e.player);
    openMain(e.player);
'@
$NewInteractTail = @'
    /* Existing saves which already finished the original workshop are treated as having finished Stage 01. */
    migrateStage1IfNeeded(e.player);
    grantPendingRewards(e.player);
    openMain(e.player);
'@

if ($Text.Contains($OldInteractTail)) {
    $Text = $Text.Replace($OldInteractTail, $NewInteractTail)
}
elseif (-not $Text.Contains("grantPendingRewards(e.player);")) {
    throw "Could not patch Joseph interact() reward migration hook."
}

# Sanity checks before writing.
$RequiredJsMarkers = @(
    'GUI v7.1 SURVIVAL QUESTLINE',
    'domesurvival.stage08.complete.v71',
    'function stage8Complete(player)',
    'function grantStageReward(player, rewardKey, rewards)',
    'function openStage9Locked(player, notice)',
    'enderio:dark_steel_ingot',
    'domesurvival:oxygen_electrolyzer',
    'domesurvival:steel_item_pipe'
)

foreach ($Marker in $RequiredJsMarkers) {
    if (-not $Text.Contains($Marker)) {
        throw "JS validation failed. Missing marker: $Marker"
    }
}

[IO.File]::WriteAllText($Joseph, $Text, $Utf8NoBom)

# Extend admin/test reset coverage to all new stages.
$Java = [IO.File]::ReadAllText($CommandJava, [Text.Encoding]::UTF8)

$OldPrefix = @'
        "domesurvival.stage05.", "domesurvival.stage5.",
        "domesurvival.workshop.",
'@
$NewPrefix = @'
        "domesurvival.stage05.", "domesurvival.stage5.",
        "domesurvival.stage06.", "domesurvival.stage6.",
        "domesurvival.stage07.", "domesurvival.stage7.",
        "domesurvival.stage08.", "domesurvival.stage8.",
        "domesurvival.stage09.", "domesurvival.stage9.",
        "domesurvival.workshop.",
'@

if ($Java.Contains($OldPrefix)) {
    $Java = $Java.Replace($OldPrefix, $NewPrefix)
}
elseif (-not $Java.Contains('"domesurvival.stage08.", "domesurvival.stage8.",')) {
    throw "Could not extend JosephScriptCommand RESET_PREFIXES."
}

$Java = $Java.Replace(
    "Right-click Joseph: Stage 01 should be active, Stages 02-05 locked.",
    "Right-click Joseph: Stage 01 should be active, Stages 02-09 locked."
)
$Java = $Java.Replace(
    "Start state: Stage 01 active; Stages 02-05 locked.",
    "Start state: Stage 01 active; Stages 02-09 locked."
)
$Java = $Java.Replace(
    "Теперь Stage 01 активен, Stage 02-05 закрыты.",
    "Теперь Stage 01 активен, Stage 02-09 закрыты."
)

[IO.File]::WriteAllText($CommandJava, $Java, $Utf8NoBom)

# Refresh the external CustomNPCs script used by the dev instance.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))

$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) {
    $Targets.Add($WorldLocal)
}

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $Target = Join-Path $TargetDir "joseph_cooper_gui.js"
    Copy-Item -LiteralPath $Joseph -Destination $Target -Force

    $A = (Get-FileHash -LiteralPath $Joseph -Algorithm SHA256).Hash
    $B = (Get-FileHash -LiteralPath $Target -Algorithm SHA256).Hash
    if ($A -ne $B) {
        throw "CustomNPCs script copy verification failed: $Target"
    }

    Write-Host "[OK] Joseph script -> $Target" -ForegroundColor Green
}

Write-Host ""
Write-Host "[OK] Joseph Questline V7.1 applied." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  1. .\dev\RUN_DEV_FULL.bat"
Write-Host "  2. In WASTED_TEST: /josephscript apply"
Write-Host "  3. For clean quest testing without deleting the existing workshop: /josephscript resetprogress"
Write-Host ""
Write-Host "Do NOT use /josephscript resettest unless you intentionally want to remove the workshop for a complete Stage 02 rebuild test."
exit 0
