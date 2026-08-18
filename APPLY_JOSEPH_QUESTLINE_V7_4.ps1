$ErrorActionPreference = "Stop"
$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
$CommandJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\integration\customnpcs\JosephScriptCommand.java"

if (-not (Test-Path -LiteralPath $Joseph)) { throw "Joseph GUI source not found: $Joseph" }
if (-not (Test-Path -LiteralPath $CommandJava)) { throw "JosephScriptCommand.java not found: $CommandJava" }

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Js = [IO.File]::ReadAllText($Joseph,[Text.Encoding]::UTF8)
$Java = [IO.File]::ReadAllText($CommandJava,[Text.Encoding]::UTF8)

if ($Js.Contains("GUI v7.4 EXODUS")) {
    Write-Host "[OK] Joseph V7.4 is already installed." -ForegroundColor Green
    exit 0
}
if (-not $Js.Contains("GUI v7.3.3 RUSSIAN + FULL PATH + REWARDS")) {
    throw "V7.4 requires V7.3.3. Use the AUTO installer from this archive."
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_v74_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force
Copy-Item -LiteralPath $CommandJava -Destination (Join-Path $Backup "JosephScriptCommand.java") -Force

function Replace-RequiredRegex {
    param([string]$Text,[string]$Pattern,[string]$Replacement,[string]$Label)
    $rx = [regex]::new($Pattern,[System.Text.RegularExpressions.RegexOptions]::Singleline)
    $matches = $rx.Matches($Text)
    if ($matches.Count -ne 1) { throw "Patch anchor '$Label' expected 1 match, found $($matches.Count)." }
    return $rx.Replace($Text,[System.Text.RegularExpressions.MatchEvaluator]{ param($m) return $Replacement },1)
}

$Js = $Js.Replace("/* Dome Survival - Joseph Cooper GUI v7.3.3 RUSSIAN + FULL PATH + REWARDS */","/* Dome Survival - Joseph Cooper GUI v7.4 EXODUS */")
$Js = $Js.Replace('var GUI_NAME = "dome_joseph_v733_ru_fullpath_rewards";','var GUI_NAME = "dome_joseph_v74_exodus";')

$Stages = @'

/* Stage 09 - Program Exodus: long-range communications and protected control network. */
var S9_COMPLETE = "domesurvival.stage09.complete.v74";
var S9 = [
    { key: "domesurvival.stage09.radio.v74", id: "ad_astra:radio", name: "Радиостанция", req: 2 },
    { key: "domesurvival.stage09.cable.v74", id: "ad_astra:steel_cable", name: "Стальной кабель", req: 16 },
    { key: "domesurvival.stage09.press.v74", id: "thermal:machine_press", name: "Механический пресс", req: 1 },
    { key: "domesurvival.stage09.circuit.v74", id: "mekanism:advanced_control_circuit", name: "Продвинутая схема управления", req: 4 },
    { key: "domesurvival.stage09.alloy.v74", id: "mekanism:alloy_reinforced", name: "Укреплённый сплав", req: 4 },
    { key: "domesurvival.stage09.board.v74", id: "immersiveengineering:circuit_board", name: "Печатная плата", req: 4 },
    { key: "domesurvival.stage09.darksteel.v74", id: "enderio:dark_steel_ingot", name: "Слиток тёмной стали", req: 8 },
    { key: "domesurvival.stage09.energy.v74", id: "domesurvival:reinforced_energy_pipe", name: "Усиленная энерготруба II уровня", req: 8 }
];

var S10_COMPLETE = "domesurvival.stage10.complete.v74";
var S10 = [
    { key: "domesurvival.stage10.aasteel.v74", id: "ad_astra:steel_ingot", name: "Стальной слиток Ad Astra", req: 32 },
    { key: "domesurvival.stage10.plate.v74", id: "ad_astra:steel_plate", name: "Стальная пластина Ad Astra", req: 24 },
    { key: "domesurvival.stage10.tank.v74", id: "ad_astra:steel_tank", name: "Стальной бак", req: 2 },
    { key: "domesurvival.stage10.fan.v74", id: "ad_astra:engine_fan", name: "Вентилятор двигателя", req: 2 },
    { key: "domesurvival.stage10.pulverizer.v74", id: "thermal:machine_pulverizer", name: "Измельчитель", req: 1 },
    { key: "domesurvival.stage10.smelter.v74", id: "thermal:machine_smelter", name: "Индукционная плавильня", req: 1 },
    { key: "domesurvival.stage10.meksteel.v74", id: "mekanism:ingot_steel", name: "Стальной слиток Mekanism", req: 16 },
    { key: "domesurvival.stage10.ieplate.v74", id: "immersiveengineering:plate_steel", name: "Стальная пластина Immersive Engineering", req: 12 },
    { key: "domesurvival.stage10.darksteel.v74", id: "enderio:dark_steel_ingot", name: "Слиток тёмной стали", req: 8 }
];

var S11_COMPLETE = "domesurvival.stage11.complete.v74";
var S11 = [
    { key: "domesurvival.stage11.engine.v74", id: "ad_astra:steel_engine", name: "Стальной ракетный двигатель", req: 1 },
    { key: "domesurvival.stage11.fin.v74", id: "ad_astra:rocket_fin", name: "Ракетный стабилизатор", req: 4 },
    { key: "domesurvival.stage11.nose.v74", id: "ad_astra:rocket_nose_cone", name: "Носовой обтекатель ракеты", req: 1 },
    { key: "domesurvival.stage11.ogear.v74", id: "ad_astra:oxygen_gear", name: "Кислородное снаряжение", req: 1 },
    { key: "domesurvival.stage11.otank.v74", id: "ad_astra:oxygen_tank", name: "Кислородный баллон Ad Astra", req: 2 },
    { key: "domesurvival.stage11.gastank.v74", id: "ad_astra:large_gas_tank", name: "Большой газовый баллон", req: 1 },
    { key: "domesurvival.stage11.press.v74", id: "thermal:machine_press", name: "Механический пресс", req: 1 },
    { key: "domesurvival.stage11.circuit.v74", id: "mekanism:advanced_control_circuit", name: "Продвинутая схема управления", req: 4 },
    { key: "domesurvival.stage11.board.v74", id: "immersiveengineering:circuit_board", name: "Печатная плата", req: 4 },
    { key: "domesurvival.stage11.glass.v74", id: "domesurvival:reinforced_glass", name: "Усиленное стекло купола", req: 16 }
];

var S12_COMPLETE = "domesurvival.stage12.complete.v74";
var S12 = [
    { key: "domesurvival.stage12.radio.v74", id: "ad_astra:radio", name: "Радиостанция", req: 1 },
    { key: "domesurvival.stage12.cable.v74", id: "ad_astra:steel_cable", name: "Стальной кабель", req: 32 },
    { key: "domesurvival.stage12.plate.v74", id: "ad_astra:steel_plate", name: "Стальная пластина Ad Astra", req: 32 },
    { key: "domesurvival.stage12.tank.v74", id: "ad_astra:steel_tank", name: "Стальной бак", req: 4 },
    { key: "domesurvival.stage12.engine.v74", id: "ad_astra:steel_engine", name: "Стальной ракетный двигатель", req: 1 },
    { key: "domesurvival.stage12.fin.v74", id: "ad_astra:rocket_fin", name: "Ракетный стабилизатор", req: 4 },
    { key: "domesurvival.stage12.nose.v74", id: "ad_astra:rocket_nose_cone", name: "Носовой обтекатель ракеты", req: 1 },
    { key: "domesurvival.stage12.gastank.v74", id: "ad_astra:large_gas_tank", name: "Большой газовый баллон", req: 2 },
    { key: "domesurvival.stage12.refinery.v74", id: "thermal:machine_refinery", name: "Фракционирующий перегонный аппарат", req: 1 },
    { key: "domesurvival.stage12.energy.v74", id: "domesurvival:high_voltage_energy_pipe", name: "Высоковольтная энерготруба III уровня", req: 12 }
];

'@
$Rewards = @'

var R9_KEY = "domesurvival.stage09.reward.v74";
var R9 = [
    { id: "thermal:machine_pulverizer", count: 1, name: "Измельчитель" },
    { id: "thermal:machine_smelter", count: 1, name: "Индукционная плавильня" },
    { id: "ad_astra:hammer", count: 1, name: "Молот Ad Astra" },
    { id: "ad_astra:steel_plate", count: 8, name: "Стальная пластина Ad Astra" },
    { id: "domesurvival:reinforced_glass", count: 8, name: "Усиленное стекло купола" }
];

var R10_KEY = "domesurvival.stage10.reward.v74";
var R10 = [
    { id: "ad_astra:steel_cable", count: 24, name: "Стальной кабель" },
    { id: "ad_astra:steel_plate", count: 12, name: "Стальная пластина Ad Astra" },
    { id: "domesurvival:high_voltage_energy_pipe", count: 8, name: "Высоковольтная энерготруба III уровня" },
    { id: "mekanism:energy_tablet", count: 2, name: "Энергетический планшет" },
    { id: "immersiveengineering:circuit_board", count: 2, name: "Печатная плата" }
];

var R11_KEY = "domesurvival.stage11.reward.v74";
var R11 = [
    { id: "ad_astra:steel_tank", count: 2, name: "Стальной бак" },
    { id: "ad_astra:oxygen_tank", count: 2, name: "Кислородный баллон Ad Astra" },
    { id: "ad_astra:large_gas_tank", count: 1, name: "Большой газовый баллон" },
    { id: "ad_astra:radio", count: 1, name: "Радиостанция" },
    { id: "domesurvival:large_oxygen_tank", count: 2, name: "Большой кислородный баллон" },
    { id: "brewinandchewin:jerky", count: 16, name: "Вяленое мясо" }
];

var R12_KEY = "domesurvival.stage12.reward.v74";
var R12 = [
    { id: "ad_astra:hammer", count: 1, name: "Молот Ad Astra" },
    { id: "ad_astra:oxygen_gear", count: 1, name: "Кислородное снаряжение" },
    { id: "ad_astra:oxygen_tank", count: 2, name: "Кислородный баллон Ad Astra" },
    { id: "domesurvival:high_voltage_energy_pipe", count: 8, name: "Высоковольтная энерготруба III уровня" },
    { id: "domesurvival:reinforced_glass", count: 16, name: "Усиленное стекло купола" },
    { id: "farmersdelight:roast_chicken_block", count: 1, name: "Жареная курица" },
    { id: "brewinandchewin:jerky", count: 16, name: "Вяленое мясо" }
];

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
'@
$Completion = @'
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

function ensureStage1PathUpgrade
'@
$Current = @'
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
    if (stage == 9) { openStage9(player, notice); return; }
    if (stage == 10) { openStage10(player, notice); return; }
    if (stage == 11) { openStage11(player, notice); return; }
    if (stage == 12) { openStage12(player, notice); return; }
    openStage13Locked(player, notice);
}
'@
$Pages = @'
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
    openResourceStage(player, notice, "ПРОЕКТ 12 / ПРЕДСТАРТОВАЯ ГОТОВНОСТЬ", "Резерв энергетики, топлива, связи и систем жизнеобеспечения", S12, S12_COMPLETE, "Земная часть программы «Исход» завершена.", "Передать резерв");
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

function openBase
'@
$OpenBase = @'
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

function openPlan
'@
$OpenPlan = @'
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

'@

$anchor = "/* One-time shared base reward packages."
$idx = $Js.IndexOf($anchor)
if ($idx -lt 0) { throw "Reward section anchor not found." }
$Js = $Js.Insert($idx,$Stages + "`r`n")

$runtime = "var lastInteractByPlayer = {};"
if (-not $Js.Contains($runtime)) { throw "Runtime anchor not found." }
$Js = $Js.Replace($runtime,$Rewards + "`r`n" + $runtime)

$Js = Replace-RequiredRegex $Js 'function customGuiButton\(e\) \{.*?function getStored\(player\) \{' $CustomButton "customGuiButton"
$Js = Replace-RequiredRegex $Js 'function stage3Complete\(player\) \{.*?function ensureStage1PathUpgrade' $Completion "completion chain"
$Js = Replace-RequiredRegex $Js 'function currentStage\(player\) \{.*?function currentProjectTitle\(player\) \{.*?\n\}' $Current "current stage"
$Js = Replace-RequiredRegex $Js 'function openProject\(player, notice\) \{.*?\n\}' $OpenProject "openProject"
$Js = Replace-RequiredRegex $Js 'function openStage9Locked\(player, notice\) \{.*?function openBase' $Pages "stage 09-13 pages"
$Js = Replace-RequiredRegex $Js 'function openBase\(player\) \{.*?function openPlan' $OpenBase "openBase"
$Js = Replace-RequiredRegex $Js 'function openPlan\(player\) \{.*\z' $OpenPlan "openPlan"

# Java /nextstage through Stage 12.
if (-not $Java.Contains('STAGE9_COMPLETE_KEY')) {
    $Java = $Java.Replace(
        '    private static final String STAGE8_COMPLETE_KEY = "domesurvival.stage08.complete.v71";',
        '    private static final String STAGE8_COMPLETE_KEY = "domesurvival.stage08.complete.v71";' + "`r`n" +
        '    private static final String STAGE9_COMPLETE_KEY = "domesurvival.stage09.complete.v74";' + "`r`n" +
        '    private static final String STAGE10_COMPLETE_KEY = "domesurvival.stage10.complete.v74";' + "`r`n" +
        '    private static final String STAGE11_COMPLETE_KEY = "domesurvival.stage11.complete.v74";' + "`r`n" +
        '    private static final String STAGE12_COMPLETE_KEY = "domesurvival.stage12.complete.v74";'
    )
}
if (-not $Java.Contains('case 12 -> data.put(STAGE12_COMPLETE_KEY, 1);')) {
    $Java = $Java.Replace(
        '            case 8 -> data.put(STAGE8_COMPLETE_KEY, 1);',
        '            case 8 -> data.put(STAGE8_COMPLETE_KEY, 1);' + "`r`n" +
        '            case 9 -> data.put(STAGE9_COMPLETE_KEY, 1);' + "`r`n" +
        '            case 10 -> data.put(STAGE10_COMPLETE_KEY, 1);' + "`r`n" +
        '            case 11 -> data.put(STAGE11_COMPLETE_KEY, 1);' + "`r`n" +
        '            case 12 -> data.put(STAGE12_COMPLETE_KEY, 1);'
    )
}
$Java = $Java.Replace('        if (stage > 8) {','        if (stage > 12) {')
$Java = $Java.Replace('"[JosephScript] Все тестируемые этапы 01-08 уже завершены."','"[JosephScript] Все тестируемые этапы 01-12 уже завершены."')

if (-not $Java.Contains('if (!storedFlag(data, STAGE12_COMPLETE_KEY)) return 12;')) {
    $old = '        if (!storedFlag(data, STAGE8_COMPLETE_KEY)) return 8;' + "`r`n" + '        return 9;'
    $new = '        if (!storedFlag(data, STAGE8_COMPLETE_KEY)) return 8;' + "`r`n" +
           '        if (!storedFlag(data, STAGE9_COMPLETE_KEY)) return 9;' + "`r`n" +
           '        if (!storedFlag(data, STAGE10_COMPLETE_KEY)) return 10;' + "`r`n" +
           '        if (!storedFlag(data, STAGE11_COMPLETE_KEY)) return 11;' + "`r`n" +
           '        if (!storedFlag(data, STAGE12_COMPLETE_KEY)) return 12;' + "`r`n" +
           '        return 13;'
    if (-not $Java.Contains($old)) { throw "Could not extend currentQuestStageForTesting." }
    $Java = $Java.Replace($old,$new)
}

if (-not $Java.Contains('"domesurvival.stage12.", "domesurvival.stage12.",')) {
    $old = '        "domesurvival.stage09.", "domesurvival.stage9.",'
    $new = '        "domesurvival.stage09.", "domesurvival.stage9.",' + "`r`n" +
           '        "domesurvival.stage10.", "domesurvival.stage10.",' + "`r`n" +
           '        "domesurvival.stage11.", "domesurvival.stage11.",' + "`r`n" +
           '        "domesurvival.stage12.", "domesurvival.stage12.",'
    if ($Java.Contains($old)) { $Java = $Java.Replace($old,$new) }
}
$Java = $Java.Replace("Stages 02-09 locked","Stages 02-13 locked")
$Java = $Java.Replace("Stage 02-09 закрыты","Stage 02-13 закрыты")

$requiredJs = @(
    "GUI v7.4 EXODUS",
    "domesurvival.stage09.complete.v74",
    "domesurvival.stage12.complete.v74",
    "ad_astra:radio",
    "ad_astra:steel_engine",
    "ad_astra:rocket_fin",
    "ad_astra:rocket_nose_cone",
    "thermal:machine_press",
    "thermal:machine_pulverizer",
    "thermal:machine_smelter",
    "thermal:machine_refinery",
    "function stage12Complete(player)",
    "function openStage13Locked(player, notice)"
)
foreach ($m in $requiredJs) { if (-not $Js.Contains($m)) { throw "V7.4 JS validation failed: $m" } }

$rewardStart = $Js.IndexOf("var R1_KEY")
$rewardText = $Js.Substring($rewardStart)
foreach ($bad in @('id: "domesurvival:water_purifier"','id: "domesurvival:oxygen_electrolyzer"','id: "domesurvival:oxygen_filler"')) {
    if ($rewardText.Contains($bad)) { throw "Forbidden reward remains: $bad" }
}

foreach ($m in @("STAGE12_COMPLETE_KEY","case 12 -> data.put(STAGE12_COMPLETE_KEY, 1);","if (!storedFlag(data, STAGE12_COMPLETE_KEY)) return 12;","stage > 12")) {
    if (-not $Java.Contains($m)) { throw "V7.4 Java validation failed: $m" }
}

[IO.File]::WriteAllText($Joseph,$Js,$Utf8NoBom)
[IO.File]::WriteAllText($CommandJava,$Java,$Utf8NoBom)

$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))
$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) { $Targets.Add($WorldLocal) }
foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $Target = Join-Path $TargetDir "joseph_cooper_gui.js"
    Copy-Item -LiteralPath $Joseph -Destination $Target -Force
}
Write-Host "[OK] Joseph Questline V7.4 EXODUS installed." -ForegroundColor Green
Write-Host "Stages 09-12 added. Stage 13 is the locked Moon chapter."
Write-Host "Fast test remains: /josephscript nextstage"
exit 0
