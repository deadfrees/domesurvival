$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"

if (-not (Test-Path -LiteralPath $Joseph)) {
    throw "Joseph GUI source not found: $Joseph"
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Js = [IO.File]::ReadAllText($Joseph, [Text.Encoding]::UTF8)

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_v75_balance_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force

function Replace-RequiredRegex {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Replacement,
        [string]$Description
    )

    $Rx = [regex]::new($Pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
    $Matches = $Rx.Matches($Text)
    if ($Matches.Count -ne 1) {
        throw "Expected exactly one $Description block, found $($Matches.Count)."
    }
    return $Rx.Replace($Text, $Replacement, 1)
}

# ---------------------------------------------------------------------------
# V7.5 Forge-tag support for genuinely generic material requirements.
# If the runtime tag API is unavailable for any reason, each definition still
# has a concrete id/ids fallback and the quest remains usable.
# ---------------------------------------------------------------------------
$InteropMarker = 'var V75_ITEM_TAG_CACHE = {};'
if (-not $Js.Contains($InteropMarker)) {
    $BridgeAnchor = 'var Bridge = Java.type("com.wasted.domesurvival.forge.progression.JosephCooperBridge");'
    if (-not $Js.Contains($BridgeAnchor)) {
        throw "Could not find Joseph Bridge declaration."
    }

    $Interop = @'
var V75ForgeRegistries = Java.type("net.minecraftforge.registries.ForgeRegistries");
var V75Registries = Java.type("net.minecraft.core.registries.Registries");
var V75TagKey = Java.type("net.minecraft.tags.TagKey");
var V75ResourceLocation = Java.type("net.minecraft.resources.ResourceLocation");
var V75_ITEM_TAG_CACHE = {};
'@
    $Js = $Js.Replace($BridgeAnchor, $BridgeAnchor + "`r`n" + $Interop)
}

# ---------------------------------------------------------------------------
# Stage 06: steel is now genuinely generic. Any mod item in the Forge steel
# ingot / steel plate tags is accepted, with explicit modpack fallbacks.
# ---------------------------------------------------------------------------
$S6 = @'
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
'@
$Js = Replace-RequiredRegex $Js 'var S6_COMPLETE\s*=\s*"[^\"]+";\s*var S6\s*=\s*\[.*?\];' $S6 'Stage 06'

# ---------------------------------------------------------------------------
# Stages 09-12. Exact machinery/components are explicitly labelled by mod.
# Generic steel rows accept the complete Forge material tag.
# Stage 10 no longer asks the player to hand back the Thermal machines awarded
# by the old Stage 09 package. Stage 12 no longer consumes the same rocket
# engine/fins/nose/gas tank that were just assembled in Stage 11.
# ---------------------------------------------------------------------------
$S9 = @'
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
'@
$Js = Replace-RequiredRegex $Js 'var S9_COMPLETE\s*=\s*"[^\"]+";\s*var S9\s*=\s*\[.*?\];' $S9 'Stage 09'

$S10 = @'
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
'@
$Js = Replace-RequiredRegex $Js 'var S10_COMPLETE\s*=\s*"[^\"]+";\s*var S10\s*=\s*\[.*?\];' $S10 'Stage 10'

$S11 = @'
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
'@
$Js = Replace-RequiredRegex $Js 'var S11_COMPLETE\s*=\s*"[^\"]+";\s*var S11\s*=\s*\[.*?\];' $S11 'Stage 11'

$S12 = @'
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
'@
$Js = Replace-RequiredRegex $Js 'var S12_COMPLETE\s*=\s*"[^\"]+";\s*var S12\s*=\s*\[.*?\];' $S12 'Stage 12'

# ---------------------------------------------------------------------------
# Reward policy V7.5: a quest reward is a benefit, not a prepaid requirement.
# None of these reward IDs is required by a later V7.5 stage. Dome construction
# blocks are completely absent from rewards.
# ---------------------------------------------------------------------------
$RewardBlocks = @{}
$RewardBlocks[1] = @'
var R1 = [
    { id: "farmersdelight:stove", count: 1, name: "Плита (Farmer's Delight)" },
    { id: "farmersdelight:cabbage_seeds", count: 4, name: "Семена капусты (Farmer's Delight)" },
    { id: "farmersdelight:tomato_seeds", count: 4, name: "Семена томата (Farmer's Delight)" },
    { id: "minecraft:bread", count: 8, name: "Хлеб" }
];
'@
$RewardBlocks[2] = @'
var R2 = [
    { id: "domesurvival:machine_wrench", count: 1, name: "Ключ инженера" },
    { id: "immersiveengineering:hammer", count: 1, name: "Молот инженера (Immersive Engineering)" },
    { id: "minecraft:iron_pickaxe", count: 1, name: "Железная кирка" },
    { id: "minecraft:lantern", count: 8, name: "Фонарь" },
    { id: "minecraft:chest", count: 4, name: "Сундук" }
];
'@
$RewardBlocks[3] = @'
var R3 = [
    { id: "minecraft:coal", count: 32, name: "Уголь" },
    { id: "minecraft:redstone", count: 16, name: "Редстоун" },
    { id: "minecraft:iron_ingot", count: 16, name: "Железный слиток" },
    { id: "minecraft:torch", count: 32, name: "Факел" }
];
'@
$RewardBlocks[4] = @'
var R4 = [
    { id: "domesurvival:reinforced_fluid_pipe", count: 12, name: "Усиленная жидкостная труба" },
    { id: "farmersdelight:rich_soil", count: 8, name: "Плодородная почва (Farmer's Delight)" },
    { id: "farmersdelight:oak_cabinet", count: 2, name: "Дубовая кладовая (Farmer's Delight)" },
    { id: "farmersdelight:straw_bale", count: 4, name: "Тюк соломы (Farmer's Delight)" },
    { id: "minecraft:water_bucket", count: 2, name: "Ведро воды" }
];
'@
$RewardBlocks[5] = @'
var R5 = [
    { id: "domesurvival:oxygen_mask", count: 2, name: "Кислородная маска" },
    { id: "domesurvival:medium_oxygen_tank", count: 2, name: "Средний кислородный баллон" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 12, name: "Усиленная кислородная труба" },
    { id: "mekanism:energy_tablet", count: 1, name: "Энергетический планшет (Mekanism)" },
    { id: "immersiveengineering:component_steel", count: 4, name: "Стальной механический компонент (Immersive Engineering)" },
    { id: "minecraft:cooked_beef", count: 16, name: "Стейк" }
];
'@
$RewardBlocks[6] = @'
var R6 = [
    { id: "domesurvival:surface_suit_helmet", count: 1, name: "Шлем защитного костюма" },
    { id: "domesurvival:surface_suit_chestplate", count: 1, name: "Куртка защитного костюма" },
    { id: "domesurvival:surface_suit_leggings", count: 1, name: "Штаны защитного костюма" },
    { id: "domesurvival:surface_suit_boots", count: 1, name: "Ботинки защитного костюма" },
    { id: "domesurvival:large_oxygen_tank", count: 1, name: "Большой кислородный баллон" },
    { id: "brewinandchewin:tankard", count: 2, name: "Кружка (Brewin' And Chewin')" }
];
'@
$RewardBlocks[7] = @'
var R7 = [
    { id: "domesurvival:filtering_item_pipe", count: 4, name: "Фильтрующая транспортная труба" },
    { id: "domesurvival:reinforced_fluid_pipe", count: 8, name: "Усиленная жидкостная труба" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 8, name: "Усиленная кислородная труба" },
    { id: "minecraft:barrel", count: 8, name: "Бочка" },
    { id: "minecraft:hopper", count: 4, name: "Воронка" }
];
'@
$RewardBlocks[8] = @'
var R8 = [
    { id: "domesurvival:large_oxygen_tank", count: 2, name: "Большой кислородный баллон" },
    { id: "domesurvival:filtering_item_pipe", count: 4, name: "Фильтрующая транспортная труба" },
    { id: "brewinandchewin:mead", count: 4, name: "Медовуха (Brewin' And Chewin')" },
    { id: "minecraft:golden_apple", count: 4, name: "Золотое яблоко" },
    { id: "minecraft:ender_pearl", count: 8, name: "Эндер-жемчуг" }
];
'@
$RewardBlocks[9] = @'
var R9 = [
    { id: "ad_astra:hammer", count: 1, name: "Молот (Ad Astra)" },
    { id: "minecraft:spyglass", count: 1, name: "Подзорная труба" },
    { id: "minecraft:compass", count: 1, name: "Компас" },
    { id: "minecraft:clock", count: 1, name: "Часы" },
    { id: "minecraft:ender_pearl", count: 8, name: "Эндер-жемчуг" }
];
'@
$RewardBlocks[10] = @'
var R10 = [
    { id: "mekanism:configurator", count: 1, name: "Конфигуратор (Mekanism)" },
    { id: "immersiveengineering:hammer", count: 1, name: "Молот инженера (Immersive Engineering)" },
    { id: "minecraft:diamond_pickaxe", count: 1, name: "Алмазная кирка" },
    { id: "minecraft:golden_apple", count: 4, name: "Золотое яблоко" },
    { id: "minecraft:experience_bottle", count: 16, name: "Пузырёк опыта" }
];
'@
$RewardBlocks[11] = @'
var R11 = [
    { id: "domesurvival:large_oxygen_tank", count: 2, name: "Большой кислородный баллон" },
    { id: "brewinandchewin:jerky", count: 16, name: "Вяленое мясо (Brewin' And Chewin')" },
    { id: "minecraft:golden_apple", count: 8, name: "Золотое яблоко" },
    { id: "minecraft:ender_pearl", count: 16, name: "Эндер-жемчуг" },
    { id: "minecraft:firework_rocket", count: 16, name: "Фейерверк" }
];
'@
$RewardBlocks[12] = @'
var R12 = [
    { id: "ad_astra:hammer", count: 1, name: "Молот (Ad Astra)" },
    { id: "ad_astra:oxygen_gear", count: 1, name: "Кислородное снаряжение (Ad Astra)" },
    { id: "ad_astra:oxygen_tank", count: 2, name: "Кислородный баллон (Ad Astra)" },
    { id: "domesurvival:high_voltage_energy_pipe", count: 8, name: "Высоковольтная энерготруба III уровня" },
    { id: "farmersdelight:roast_chicken_block", count: 1, name: "Жареная курица (Farmer's Delight)" },
    { id: "brewinandchewin:jerky", count: 16, name: "Вяленое мясо (Brewin' And Chewin')" },
    { id: "minecraft:golden_apple", count: 8, name: "Золотое яблоко" }
];
'@

for ($i = 1; $i -le 12; $i++) {
    $Pattern = 'var R' + $i + '\s*=\s*\[.*?\];'
    $Js = Replace-RequiredRegex $Js $Pattern $RewardBlocks[$i] ('Reward R' + $i)
}

# ---------------------------------------------------------------------------
# Generic resource intake. defs may specify:
#   id  = normal exact fallback
#   ids = accepted equivalent registry IDs
#   tag = Forge item tag, e.g. forge:ingots/steel
# This implements the rule: if GUI says simply "Стальной слиток", any mod's
# item registered in forge:ingots/steel can satisfy it.
# ---------------------------------------------------------------------------
$InventoryFunctions = @'
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
'@
$Js = Replace-RequiredRegex $Js 'function inventoryCount\(player, id\) \{.*?function contributeSet\(player, defs, completeKey\) \{' $InventoryFunctions 'inventory/resource intake'

# ---------------------------------------------------------------------------
# Migration for the already-running test world.
# - preserves old Stage 10 steel/plate contributions by merging them into the
#   new generic rows;
# - if the old Stage 09 reward was claimed and its Pulverizer/Smelter were then
#   handed straight into Stage 10, gives one of each back once.
# ---------------------------------------------------------------------------
$Migration = @'
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
'@
$Js = Replace-RequiredRegex $Js 'function currentStage\(player\) \{' $Migration 'V7.5 migration/currentStage anchor'

# Run migration before every Joseph UI interaction.
if (-not $Js.Contains('migrateV75Balance(e.player);')) {
    $Js = $Js.Replace(
        '    /* Existing saves which already finished the original workshop are treated as having finished Stage 01. */',
        '    migrateV75Balance(e.player);' + "`r`n`r`n" + '    /* Existing saves which already finished the original workshop are treated as having finished Stage 01. */'
    )
}

# Also migrate before button processing in case the GUI was already open when a
# developer reloads the external script.
$ButtonAnchor = 'function customGuiButton(e) {'
if ($Js.Contains($ButtonAnchor) -and -not $Js.Contains("function customGuiButton(e) {`r`n    migrateV75Balance(e.player);")) {
    $Js = $Js.Replace($ButtonAnchor, $ButtonAnchor + "`r`n    migrateV75Balance(e.player);")
}

# ---------------------------------------------------------------------------
# Main situation text: every late-game stage gets its own briefing instead of
# repeating the same "Купол стабилизирован..." paragraph for stages 9-13.
# ---------------------------------------------------------------------------
if (-not ([regex]::IsMatch($Js, 'function\s+openMain\s*\(\s*player\s*\)\s*\{'))) {
    throw "Could not find function openMain(player) in joseph_cooper_gui.js. The source differs from the expected questline version."
}

$OpenMain = @'
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
'@

# V7.5.1: replace openMain until the next TOP-LEVEL JS function, regardless
# of that function's name. The V7.5 installer incorrectly assumed it was
# always followed by function addNotice.
$Js = Replace-RequiredRegex `
    $Js `
    'function\s+openMain\s*\(\s*player\s*\)\s*\{.*?(?=\r?\nfunction\s+[A-Za-z_$][A-Za-z0-9_$]*\s*\()' `
    $OpenMain `
    'main situation page'

# Stage 12 description must match its new role (no duplicate rocket module parts).
$Js = $Js.Replace(
    'openResourceStage(player, notice, "ПРОЕКТ 12 / ПРЕДСТАРТОВАЯ ГОТОВНОСТЬ", "Резерв энергетики, топлива, связи и систем жизнеобеспечения", S12, S12_COMPLETE, "Земная часть программы «Исход» завершена.", "Передать резерв");',
    'openResourceStage(player, notice, "ПРОЕКТ 12 / ПРЕДСТАРТОВАЯ ГОТОВНОСТЬ", "Наземная энергетика, связь, переработка топлива и пусковой резерв", S12, S12_COMPLETE, "Земная часть программы «Исход» завершена.", "Передать резерв");'
)

# Version marker / GUI id.
$Js = [regex]::Replace($Js, '^/\* Dome Survival - Joseph Cooper GUI .*?\*/', '/* Dome Survival - Joseph Cooper GUI v7.5.1 PROGRESSION BALANCE */', 1)
$Js = [regex]::Replace($Js, 'var GUI_NAME\s*=\s*"[^"]+";', 'var GUI_NAME = "dome_joseph_v751_progression_balance";', 1)

# ---------------------------------------------------------------------------
# Hard policy: construction-only dome blocks are allowed to exist in the world
# and generation code, but never as quest requirements or quest rewards.
# ---------------------------------------------------------------------------
$ForbiddenQuestIds = @(
    'domesurvival:reinforced_glass',
    'domesurvival:dome_frame',
    'domesurvival:dome_foundation'
)
foreach ($Forbidden in $ForbiddenQuestIds) {
    if ($Js.Contains('id: "' + $Forbidden + '"')) {
        throw "Forbidden dome construction block remains in Joseph quest data: $Forbidden"
    }
}

# Validation of the core V7.5 invariants.
$Required = @(
    'var V75_ITEM_TAG_CACHE = {};',
    'tag: "forge:ingots/steel"',
    'tag: "forge:plates/steel"',
    'function acceptedIds(def)',
    'function migrateV75Balance(player)',
    'domesurvival.stage10.genericsteel.v75',
    'domesurvival.stage10.genericplate.v75',
    'Энергоблок серии «Титан»',
    'stage == 13 ? "[ ГЛАВА ЗАВЕРШЕНА ]"'
)
foreach ($Marker in $Required) {
    if (-not $Js.Contains($Marker)) {
        throw "V7.5 validation failed. Missing marker: $Marker"
    }
}

# Ensure old reward->next-stage traps are gone from their respective arrays.
$BadRewardFragments = @(
    '{ id: "domesurvival:steel_item_pipe", count: 12',
    '{ id: "domesurvival:steel_hopper", count: 2',
    '{ id: "domesurvival:reinforced_energy_pipe", count: 8, name: "Усиленная энерготруба II уровня" },`r`n    { id: "mekanism:alloy_reinforced"',
    '{ id: "thermal:machine_pulverizer", count: 1',
    '{ id: "thermal:machine_smelter", count: 1',
    '{ id: "ad_astra:steel_plate", count: 8',
    '{ id: "ad_astra:steel_tank", count: 2, name: "Стальной бак" },`r`n    { id: "ad_astra:oxygen_tank"'
)
foreach ($Bad in $BadRewardFragments) {
    if ($Js.Contains($Bad)) {
        throw "Old reward-to-requirement progression fragment remains: $Bad"
    }
}

[IO.File]::WriteAllText($Joseph, $Js, $Utf8NoBom)

# Refresh active CustomNPCs copies.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))
$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath (Split-Path -Parent $WorldLocal)) {
    $Targets.Add($WorldLocal)
}

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
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Joseph V7.5.1 - PROGRESSION BALANCE" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] Generic steel accepts Forge-tag equivalents." -ForegroundColor Green
Write-Host "[OK] Reward -> later requirement traps removed." -ForegroundColor Green
Write-Host "[OK] Dome construction blocks removed from all Joseph quests/rewards." -ForegroundColor Green
Write-Host "[OK] Stage 9-13 situation briefings are unique." -ForegroundColor Green
Write-Host "[OK] Existing quest completion was NOT reset." -ForegroundColor Green
Write-Host "[OK] Old Stage 10 steel/plate progress will migrate automatically." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  1. Fully restart Minecraft: .\dev\RUN_DEV_FULL.bat"
Write-Host "  2. In world: /josephscript apply"
Write-Host "  3. Then: /josephscript inspect"
Write-Host ""
Write-Host "Expected: errored=false, valid=true"
exit 0
