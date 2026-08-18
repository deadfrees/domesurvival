$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
$Bridge = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\progression\JosephCooperBridge.java"

if (-not (Test-Path -LiteralPath $Joseph)) { throw "Joseph GUI source not found: $Joseph" }
if (-not (Test-Path -LiteralPath $Bridge)) { throw "JosephCooperBridge.java not found: $Bridge" }

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_questline_v72_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force
Copy-Item -LiteralPath $Bridge -Destination (Join-Path $Backup "JosephCooperBridge.java") -Force

function Replace-RequiredRegex {
    param([string]$Text,[string]$Pattern,[string]$Replacement,[string]$Label)
    $rx = [regex]::new($Pattern,[System.Text.RegularExpressions.RegexOptions]::Singleline)
    $matches = $rx.Matches($Text)
    if ($matches.Count -ne 1) {
        throw "Patch anchor '$Label' expected exactly 1 match, found $($matches.Count). Apply V7.1 first or restore the V7.1 source backup."
    }
    return $rx.Replace($Text,[System.Text.RegularExpressions.MatchEvaluator]{ param($m) return $Replacement },1)
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Joseph,[Text.Encoding]::UTF8)

if (-not $Text.Contains("GUI v7.1 SURVIVAL QUESTLINE") -and -not $Text.Contains("GUI v7.2 UI + REWARDS FIX")) {
    throw "Expected Joseph V7.1 script. Current file has a different version marker."
}

$Text = $Text.Replace("/* Dome Survival - Joseph Cooper GUI v7.1 SURVIVAL QUESTLINE */","/* Dome Survival - Joseph Cooper GUI v7.2 UI + REWARDS FIX */")
$Text = $Text.Replace('var GUI_NAME = "dome_joseph_v71_survival_questline";','var GUI_NAME = "dome_joseph_v72_ui_rewards_fix";')

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

        /* CustomNPCs 1.20.1 IPlayer.giveItem(String, int) is the primary path.
           Unlike command execution it does not depend on command-block/op permissions. */
        try { ok = !!player.giveItem(reward.id, reward.count); } catch (ignoredGiveById) {}

        /* API stack fallback for unofficial-port differences. */
        if (!ok) {
            try {
                var stack = API.createItem(reward.id, 0, reward.count);
                ok = !!player.giveItem(stack);
            } catch (ignoredGiveStack) {}
        }

        if (ok) {
            writeInt(data, itemKey, 1);
            granted.push(reward.name + " x" + reward.count);
        } else {
            allDone = false;
            failed.push(reward.name);
        }
    }

    /* Check old per-item flags too: this allows retry after a full inventory
       without duplicating the items which were already delivered. */
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
$PendingFunc = @'
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

    /* Completed V7.1 worlds had reward flags even when command-based delivery failed.
       V7.2 uses fresh reward keys, so the packages are reissued once. */
    for (var i = 0; i < messages.length; i++) {
        try { player.message("§a[КУПОЛ] " + messages[i]); } catch (ignoredRewardMessage) {}
    }
}
'@
$MakeProject = @'
function makeGui(player) { return API.createCustomGui(GUI_NAME, GUI_WIDTH, GUI_HEIGHT, false, player); }
function makeProjectGui(player) { return API.createCustomGui(GUI_NAME + "_project", 440, 300, false, player); }

function text(value)
'@
$ProjectHeader = @'
function addProjectHeader(gui, section) {
    gui.addLabel(1, "ДЖОЗЕФ КУППЕР", 18, 12, 220, 11, 0xE6B84A);
    gui.addLabel(2, "Координатор купола", 18, 25, 220, 11, 0xB8B8B8);
    gui.addLabel(3, "БАЗА-01", 365, 12, 58, 11, 0x808080);
    gui.addLabel(4, "--------------------------------------------------------------------", 18, 39, 404, 11, 0x555555);
    gui.addLabel(5, section, 18, 52, 404, 11, 0xFFD75A);
}

function renderSetGrid(gui, startId, defs, state, y) {
    for (var i = 0; i < defs.length; i++) {
        var line = defs[i].name + ": " + state.values[i] + " / " + defs[i].req;
        var color = state.values[i] >= defs[i].req ? 0x77DD77 : 0xEEEEEE;
        gui.addLabel(startId + i, line, 28, y + i * 14, 390, 11, color);
    }
}

function renderStage2Grid(gui, player, y) {
    var lines = [];

    if (logicalResetActive(player)) {
        var resetCore = setState(player, S2_RESET_CORE, S2_RESET_CORE_COMPLETE);
        for (var r = 0; r < S2_RESET_CORE.length; r++) {
            lines.push(S2_RESET_CORE[r].name + ": " + resetCore.values[r] + " / " + S2_RESET_CORE[r].req);
        }
    } else {
        try {
            var core = String(Bridge.progressText()).replace(/\r/g, "").split("\n");
            for (var i = 0; i < core.length; i++) if (String(core[i]).length > 0) lines.push(String(core[i]));
        } catch (ignored) {}
    }

    var extras = setState(player, S2, S2_EXTRA_COMPLETE);
    for (var j = 0; j < S2.length; j++) {
        lines.push(S2[j].name + ": " + extras.values[j] + " / " + S2[j].req);
    }

    for (var k = 0; k < lines.length; k++) {
        gui.addLabel(40 + k, lines[k], 28, y + k * 14, 390, 11, 0xEEEEEE);
    }
}

function currentStage
'@
$ProjectNotice = @'
function addProjectNotice(gui, notice) {
    if (notice == null || String(notice).length == 0) return;
    var lines = wrap(String(notice), 68);
    if (lines.length > 2) lines = lines.slice(0, 2);
    for (var i = 0; i < lines.length; i++) {
        gui.addLabel(90 + i, lines[i], 22, 251 + i * 12, 396, 11, 0xFFD45A);
    }
}

function openProject
'@
$OpenStage1 = @'
function openStage1(player, notice) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "ПРОЕКТ 01 / ЖИЗНЕОБЕСПЕЧЕНИЕ");
    gui.addLabel(20, "Стабилизация жизни внутри купола", 20, 72, 400, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 20, 92, 150, 11, 0xAAAAAA);

    var state = setState(player, S1, S1_COMPLETE);
    renderSetGrid(gui, 30, S1, state, 110);

    if (state.complete) {
        gui.addLabel(70, "Жизнеобеспечение стабилизировано. Этап 02 доступен.", 28, 207, 390, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать ресурсы", 135, 205, 170, 20, 7610);
    }

    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 170, 276, 100, 18, 7620);
    player.showCustomGui(gui);
}
'@
$OpenStage2 = @'
function openStage2(player, notice) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "ПРОЕКТ 02 / МАСТЕРСКАЯ");
    gui.addLabel(20, "Восстановление и комплектация мастерской", 20, 72, 400, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 20, 92, 150, 11, 0xAAAAAA);
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
                : "Все материалы собраны. Этап 03 доступен.",
            28, 237, 390, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать ресурсы", 135, 236, 170, 20, 7610);
    }

    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 170, 276, 100, 18, 7620);
    player.showCustomGui(gui);
}
'@
$ResourceStage = @'
function openResourceStage(player, notice, header, description, defs, completeKey, completeText, buttonText) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, header);
    gui.addLabel(20, description, 20, 72, 400, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 20, 92, 150, 11, 0xAAAAAA);

    var state = setState(player, defs, completeKey);
    renderSetGrid(gui, 30, defs, state, 110);

    if (state.complete) {
        gui.addLabel(70, completeText, 28, 207, 390, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, buttonText, 135, 205, 170, 20, 7610);
    }

    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 170, 276, 100, 18, 7620);
    player.showCustomGui(gui);
}
'@
$Stage9 = @'
function openStage9Locked(player, notice) {
    var gui = makeProjectGui(player);
    addProjectHeader(gui, "СЛЕДУЮЩАЯ ГЛАВА / ПРОГРАММА «ИСХОД»");
    gui.addLabel(20, "Базовая стабилизация купола завершена", 20, 80, 400, 11, 0x55FF55);
    addLines(
        gui, 30, 28, 104, 390, 0xEEEEEE,
        "Следующий цикл будет посвящён дальней связи, научной инфраструктуре, подготовке длительных экспедиций и переходу к космической программе.",
        66, 5
    );
    addProjectNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 170, 276, 100, 18, 7620);
    player.showCustomGui(gui);
}
'@
$Text = $Text.Replace('name: "Дубовые брёвна"','name: "Дубовое бревно"')
$Text = $Text.Replace('name: "Саженцы дуба"','name: "Саженец дуба"')
$Text = $Text.Replace('name: "Медные слитки"','name: "Медный слиток"')
$Text = $Text.Replace('name: "Железные слитки"','name: "Железный слиток"')
$Text = $Text.Replace('name: "Стеклянные панели"','name: "Стеклянная панель"')
$Text = $Text.Replace('name: "Поршни"','name: "Поршень"')
$Text = $Text.Replace('name: "Угольный генератор"','name: "Стабилизатор пламени"')
$Text = $Text.Replace('name: "Импульсная матрица"','name: "Импульсная матрица"')
$Text = $Text.Replace('name: "Стабилизатор"','name: "Машинный стабилизатор"')
$Text = $Text.Replace('name: "Картриджи фильтра"','name: "Обычный фильтрующий картридж"')
$Text = $Text.Replace('name: "Улучшенный фильтр"','name: "Улучшенный фильтрующий картридж"')
$Text = $Text.Replace('name: "Жидкостные трубы"','name: "Базовая жидкостная труба"')
$Text = $Text.Replace('name: "Универсальный бак"','name: "Универсальный резервуар"')
$Text = $Text.Replace('name: "Универсальные баки"','name: "Универсальный резервуар"')
$Text = $Text.Replace('name: "Кислородные трубы"','name: "Кислородная труба"')
$Text = $Text.Replace('name: "Малые баллоны"','name: "Малый кислородный баллон"')
$Text = $Text.Replace('name: "Медные предметные трубы"','name: "Медная транспортная труба"')
$Text = $Text.Replace('name: "Фильтрующие трубы"','name: "Фильтрующая транспортная труба"')
$Text = $Text.Replace('name: "Медные воронки"','name: "Медная воронка"')
$Text = $Text.Replace('name: "Энергетические трубы"','name: "Энерготруба I уровня"')
$Text = $Text.Replace('name: "Стальные предметные трубы"','name: "Стальная транспортная труба"')
$Text = $Text.Replace('name: "Стальные воронки"','name: "Стальная воронка"')
$Text = $Text.Replace('name: "Энергоблок"','name: "Энергоблок серии «Сталь»"')
$Text = $Text.Replace('name: "Промышленный фильтр"','name: "Воздушный фильтрующий картридж"')
$Text = $Text.Replace('name: "Золотые слитки"','name: "Золотой слиток"')
$Text = $Text.Replace('name: "Алмазы"','name: "Алмаз"')
$Text = $Text.Replace('name: "Хлеб"','name: "Хлеб"')
$Text = $Text.Replace('name: "Факелы"','name: "Факел"')
$Text = $Text.Replace('name: "Костная мука"','name: "Костная мука"')
$Text = $Text.Replace('name: "Машинный ключ"','name: "Ключ инженера"')
$Text = $Text.Replace('name: "Резервный уголь"','name: "Уголь"')
$Text = $Text.Replace('name: "Усиленные жидкостные трубы"','name: "Усиленная жидкостная труба"')
$Text = $Text.Replace('name: "Запасные картриджи"','name: "Обычный фильтрующий картридж"')
$Text = $Text.Replace('name: "Кислородный электролизёр"','name: "Электролизёр кислорода"')
$Text = $Text.Replace('name: "Заправщик баллонов"','name: "Кислородный наполнитель"')
$Text = $Text.Replace('name: "Кислородные маски"','name: "Кислородная маска"')
$Text = $Text.Replace('name: "Средние баллоны"','name: "Средний кислородный баллон"')
$Text = $Text.Replace('name: "Усиленные кислородные трубы"','name: "Усиленная кислородная труба"')
$Text = $Text.Replace('name: "Шлем костюма"','name: "Шлем защитного костюма"')
$Text = $Text.Replace('name: "Куртка костюма"','name: "Куртка защитного костюма"')
$Text = $Text.Replace('name: "Штаны костюма"','name: "Штаны защитного костюма"')
$Text = $Text.Replace('name: "Ботинки костюма"','name: "Ботинки защитного костюма"')
$Text = $Text.Replace('name: "Большой баллон"','name: "Большой кислородный баллон"')
$Text = $Text.Replace('name: "Экспедиционный паёк"','name: "Стейк"')
$Text = $Text.Replace('name: "Усиленные энерготрубы"','name: "Усиленная энерготруба II уровня"')
$Text = $Text.Replace('name: "Высоковольтные энерготрубы"','name: "Высоковольтная энерготруба III уровня"')
$Text = $Text.Replace('name: "Усиленное стекло"','name: "Усиленное стекло купола"')
$Text = $Text.Replace('name: "Большие баллоны"','name: "Большой кислородный баллон"')
$Text = $Text.Replace('name: "Резервный промышленный фильтр"','name: "Воздушный фильтрующий картридж"')


# Replace ambiguous external-material requirements with exact DomeSurvival items.
$Text = [regex]::Replace($Text,
    '\{ key: "domesurvival\.stage03\.darksteel\.v71",\s*id: "enderio:dark_steel_ingot",\s*name: "[^"]+",\s*req: 2 \}',
    '{ key: "domesurvival.stage03.energypipe.v72", id: "domesurvival:basic_energy_pipe", name: "Энерготруба I уровня", req: 8 }')
$Text = [regex]::Replace($Text,
    '\{ key: "domesurvival\.stage05\.darksteel\.v71",\s*id: "enderio:dark_steel_ingot",\s*name: "[^"]+",\s*req: 4 \}',
    '{ key: "domesurvival.stage05.reservoir.v72", id: "domesurvival:universal_tank", name: "Универсальный резервуар", req: 1 }')
$Text = [regex]::Replace($Text,
    '\{ key: "domesurvival\.stage08\.darksteel\.v71",\s*id: "enderio:dark_steel_ingot",\s*name: "[^"]+",\s*req: 8 \}',
    '{ key: "domesurvival.stage08.energy2.v72", id: "domesurvival:reinforced_energy_pipe", name: "Усиленная энерготруба II уровня", req: 4 }')

# Fresh reward keys intentionally reissue packages once for V7.1 worlds where command-based delivery marked them complete without giving items.
for ($i = 1; $i -le 8; $i++) {
    $Text = $Text.Replace('var R' + $i + '_KEY = "domesurvival.stage0' + $i + '.reward.v71";',
                          'var R' + $i + '_KEY = "domesurvival.stage0' + $i + '.reward.v72";')
}

$Text = Replace-RequiredRegex $Text `
    'function grantStageReward\(player, rewardKey, rewards\) \{.*?\n\}' `
    $RewardFunc `
    "grantStageReward"

$Text = Replace-RequiredRegex $Text `
    'function grantPendingRewards\(player\) \{.*?\n\}' `
    $PendingFunc `
    "grantPendingRewards"

$Text = Replace-RequiredRegex $Text `
    'function makeGui\(player\) \{.*?function text\(value\)' `
    $MakeProject `
    "makeProjectGui"

$Text = Replace-RequiredRegex $Text `
    'function renderSetGrid\(gui, startId, defs, state, y\) \{.*?function currentStage' `
    $ProjectHeader `
    "project render layout"

$Text = Replace-RequiredRegex $Text `
    'function addNotice\(gui, notice\) \{.*?function openProject' `
    $ProjectNotice `
    "project notice"

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
    'function openStage9Locked\(player, notice\) \{.*?\n\}' `
    $Stage9 `
    "openStage9Locked"

# Exact display names for the Stage 02 Java-owned core.
$Java = [IO.File]::ReadAllText($Bridge,[Text.Encoding]::UTF8)
$Java = $Java.Replace('return "Железо: " + data.workshopIron() + " / " + WorkshopProject.IRON_REQUIRED',
                      'return "Железный слиток: " + data.workshopIron() + " / " + WorkshopProject.IRON_REQUIRED')
$Java = $Java.Replace('+ "\nМедь: " + data.workshopCopper() + " / " + WorkshopProject.COPPER_REQUIRED',
                      '+ "\nМедный слиток: " + data.workshopCopper() + " / " + WorkshopProject.COPPER_REQUIRED')
[IO.File]::WriteAllText($Bridge,$Java,$Utf8NoBom)

# Final sanity checks.
$RequiredMarkers = @(
    'GUI v7.2 UI + REWARDS FIX',
    'function makeProjectGui(player)',
    'function addProjectHeader(gui, section)',
    'player.giveItem(reward.id, reward.count)',
    'domesurvival.stage01.reward.v72',
    'domesurvival.stage08.reward.v72',
    'name: "Стабилизатор пламени"',
    'name: "Машинный стабилизатор"',
    'name: "Универсальный резервуар"',
    'name: "Энергоблок серии «Сталь»"',
    'name: "Медная транспортная труба"',
    'name: "Воздушный фильтрующий картридж"'
)
foreach ($Marker in $RequiredMarkers) {
    if (-not $Text.Contains($Marker)) { throw "V7.2 validation failed. Missing marker: $Marker" }
}

# No old shortened requirement labels should remain.
$Forbidden = @(
    'name: "Угольный генератор"',
    'name: "Стабилизатор"',
    'name: "Универсальный бак"',
    'name: "Жидкостные трубы"',
    'name: "Медные предметные трубы"',
    'name: "Стальные предметные трубы"'
)
foreach ($Bad in $Forbidden) {
    if ($Text.Contains($Bad)) { throw "V7.2 validation failed. Old shortened label remains: $Bad" }
}

[IO.File]::WriteAllText($Joseph,$Text,$Utf8NoBom)

# Refresh external CustomNPCs script copies.
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
    if ($A -ne $B) { throw "CustomNPCs script copy verification failed: $Target" }
    Write-Host "[OK] Joseph script -> $Target" -ForegroundColor Green
}

Write-Host ""
Write-Host "[OK] Joseph Questline V7.2 applied." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "V7.2 fixes:"
Write-Host " - wide 440x300 project pages"
Write-Host " - single-column resource list; no text collisions"
Write-Host " - exact in-game DomeSurvival names"
Write-Host " - direct CustomNPCs giveItem rewards (no command permissions)"
Write-Host " - fresh V7.2 reward keys reissue missing Stage 01-08 rewards once"
exit 0
