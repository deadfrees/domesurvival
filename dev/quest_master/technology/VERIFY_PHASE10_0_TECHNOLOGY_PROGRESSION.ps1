[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$errors = [System.Collections.Generic.List[string]]::new()

function Require-Text {
    param([string]$RelativePath, [string[]]$Needles)
    $path = Join-Path $projectPath $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $errors.Add("Missing file: $RelativePath")
        return
    }
    $text = Get-Content -LiteralPath $path -Raw
    foreach ($needle in $Needles) {
        if (-not $text.Contains($needle)) {
            $errors.Add("Missing '$needle' in $RelativePath")
        }
    }
}

Require-Text "src\main\resources\domesurvival.mixins.json" @("RecipeManagerMixin")
Require-Text "src\main\java\com\wasted\domesurvival\forge\network\ModNetwork.java" @('PROTOCOL_VERSION = "7"', "TechnologySyncPacket", "BioModuleRegistrySyncPacket")
Require-Text "src\main\java\com\wasted\domesurvival\forge\technology\TechnologyRegistry.java" @(
    "domesurvival:coal_generator",
    "domesurvival:water_purifier",
    "domesurvival:oxygen_electrolyzer",
    "domesurvival:coke_oven",
    "domesurvival:shaft_furnace",
    "immersiveengineering:hammer",
    "mekanism:electrolytic_separator",
    "immersiveengineering:fluid_pump",
    "immersiveengineering:voltmeter"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\CokeOvenBlockEntity.java" @(
    "ModItems.COAL_COKE",
    "PROCESS_TIME = 2_250",
    "Items.COAL",
    "getInputPortCapability",
    "getOutputPortCapability",
    "inputCapability",
    "outputCapability"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\ShaftFurnaceBlockEntity.java" @(
    "ModItems.STEEL_INGOT",
    "ModItems.SLAG",
    "PROCESS_TIME = 3_000",
    'new ResourceLocation("forge", "coal_coke")',
    "getInputPortCapability",
    "getOutputPortCapability",
    "inputCapability",
    "outputCapability",
    "getCounterClockWise",
    "getClockWise",
    "getOpposite",
    "Direction.DOWN"
)
Require-Text "src\main\resources\data\domesurvival\recipes\coke_oven.json" @(
    "minecraft:bricks",
    "minecraft:clay",
    "minecraft:blast_furnace",
    "domesurvival:lead_gear"
)
Require-Text "src\main\resources\data\domesurvival\recipes\shaft_furnace.json" @(
    "domesurvival:tin_gear",
    "domesurvival:lead_gear",
    "domesurvival:coal_coke",
    "minecraft:blast_furnace",
    "minecraft:iron_block",
    "minecraft:copper_block"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\sieve\SandSieveBlock.java" @(
    "tryStartCycle",
    "SAND_SIEVE_PROCESS",
    "level.addFreshEntity(entity)"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\sieve\SandSieveBlockEntity.java" @(
    "tryStartCycle",
    "Items.CLAY_BALL",
    "ForgeCapabilities.FLUID_HANDLER",
    "Direction.DOWN"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\block\ModBlocks.java" @(
    'BLOCKS.register("sand_sieve"',
    'ITEMS.register("sand_sieve"'
)
Require-Text "src\main\resources\data\domesurvival\recipes\sand_sieve.json" @(
    "minecraft:iron_bars",
    "minecraft:piston",
    "minecraft:hopper",
    "domesurvival:sand_sieve"
)
Require-Text "src\main\resources\assets\domesurvival\models\item\sand_sieve.json" @(
    '"parent": "domesurvival:block/sand_sieve"'
)
Require-Text "src\main\resources\assets\domesurvival\models\block\sand_sieve.json" @(
    "minecraft:block/dark_oak_planks",
    "minecraft:block/polished_blackstone_bricks"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\bio\BioincubatorBlockEntity.java" @(
    "MODE_INCUBATION",
    "MODE_REPAIR",
    "REPAIR_PROCESS_TICKS = 1_800",
    "finishRepair",
    "BioModuleItem.create(sample.entityId(), false)",
    "BIO_REPAIR_KIT",
    "BIOGEL",
    "NUTRIENT_MIX"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\bio\BioincubatorMenu.java" @(
    "MODE_BUTTON",
    "ModeSlot",
    "SLOT_OUTPUT"
)
Require-Text "src\main\resources\data\domesurvival\recipes\bio_repair_kit.json" @(
    "domesurvival:steel_ingot",
    "domesurvival:pulse_matrix"
)
Require-Text "src\main\resources\data\domesurvival\recipes\biogel.json" @("minecraft:clay_ball", "minecraft:bone_meal", "minecraft:sugar")
Require-Text "src\main\resources\data\domesurvival\recipes\nutrient_mix.json" @("minecraft:wheat", "minecraft:carrot", "minecraft:potato", "minecraft:bone_meal")
Require-Text "src\main\java\com\wasted\domesurvival\forge\quest\GeneticArchiveDiscoveryService.java" @(
    "genetic_archive_targets",
    "DISCOVERY_DELAY_TICKS = 12_000L",
    "MIN_EXCURSION_DISTANCE = 384",
    "cacheLedger="
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\quest\GeneticArchiveSampleCacheService.java" @(
    "GeneticArchiveDiscoverySavedData.get(level).target()",
    "ModItems.CHICKEN_CRYOCAPSULE.get(), 1",
    "ModItems.SHEEP_CRYOCAPSULE.get(), 1",
    "ModItems.COW_CRYOCAPSULE.get(), 1",
    "ModItems.DAMAGED_PIG_CRYOCAPSULE.get(), 1",
    "recordGuaranteedArchiveSamples"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\bio\BioModuleDistributionSavedData.java" @(
    "GUARANTEED_ARCHIVE_SPECIES",
    "allFirstCopiesAssigned",
    "MIN_PAIR_DISTANCE_SQUARED = 500L * 500L",
    "occupiedLocations.contains(locationKey)"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\technology\TechnologyEvents.java" @(
    "GENETIC_SAMPLES_RECOVERED",
    "FAUNA_RESTORATION_STARTED",
    "bioincubator_first_birth"
)
Require-Text "src\main\resources\data\domesurvival\tags\worldgen\structure\genetic_archive_targets.json" @(
    "betterdeserttemples:desert_temple",
    "minecraft:desert_pyramid",
    "betterdungeons:zombie_dungeon",
    "dungeoncrawl:dungeon"
)
Require-Text "dev\quest_master\ftbquests\quests\chapters\11FF60B844BBED5B.snbt" @(
    "70CE4EBCBA38CD21",
    "domesurvival:quest_actions/genetic_archive_signal"
)
Require-Text "dev\quest_master\ftbquests\quests\chapters\76CBABB04B110F16.snbt" @(
    "4AA418B9DF3B79A4",
    "515C1A05E15F3F67",
    "0F7B71D5BDCBD296",
    "3B095F94C8D72753",
    "4D7992E0A771B3A1",
    "6274AE251790C825"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\CokeOvenBlock.java" @(
    "getStateForPlacement",
    "clearLegacyParts",
    "COKE_OVEN_PART"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\CokeOvenBlockEntity.java" @(
    "getCounterClockWise",
    "getClockWise",
    "getOpposite",
    "Direction.DOWN",
    "inputCapability.cast",
    "outputCapability.cast"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\ShaftFurnaceBlock.java" @(
    "RADIUS = 1",
    "HEIGHT = 2",
    "SHAFT_FURNACE_PART",
    "clearStructure",
    "super.setPlacedBy"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\ShaftFurnacePartBlock.java" @(
    "isInputPort",
    "isOutputPort",
    "RenderShape.INVISIBLE",
    "Shapes.empty"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\ShaftFurnacePartBlockEntity.java" @(
    "ForgeCapabilities.ITEM_HANDLER",
    "portSide",
    "getInputPortCapability",
    "getOutputPortCapability"
)
Require-Text "src\main\resources\assets\domesurvival\textures\block\metallurgy\detailed\shaft_furnace_fire.png.mcmeta" @(
    '"frametime": 4',
    '"interpolate": false'
)
Require-Text "src\main\resources\assets\domesurvival\blockstates\coke_oven.json" @("coke_oven_ready", "coke_oven_ready_on", "coke_oven_bottom_output")
Require-Text "src\main\resources\assets\domesurvival\models\block\coke_oven_ready.json" @("domesurvival:block/bfbricks", "domesurvival:block/bftoolst")
Require-Text "src\main\resources\assets\domesurvival\models\block\coke_oven_ready_on.json" @("domesurvival:block/bfbrickslit", "domesurvival:block/campfire_log_lit")
Require-Text "src\main\resources\assets\domesurvival\models\block\coke_oven_bottom_output.json" @("bottom_output_collar", "domesurvival:block/bftoolshot")
Require-Text "src\main\resources\assets\domesurvival\models\item\coke_oven.json" @("domesurvival:block/bfbricks", '"display"')
Require-Text "src\main\resources\assets\domesurvival\blockstates\shaft_furnace.json" @("shaft_furnace_ready", "shaft_furnace_ready_on", "shaft_furnace_bottom_output")
Require-Text "src\main\resources\assets\domesurvival\blockstates\shaft_furnace_part.json" @("shaft_furnace_part_empty")
Require-Text "src\main\resources\assets\domesurvival\models\block\shaft_furnace_part_empty.json" @('"elements": []')
Require-Text "src\main\resources\assets\domesurvival\models\block\shaft_furnace_ready.json" @("domesurvival:block/shaft_furnace_dark/bfbricks", "domesurvival:block/shaft_furnace_dark/bftoolst", "minecraft:block/polished_deepslate", "domesurvival:block/shaft_furnace_dark/wither_skeleton_head", "wither_skull_head")
Require-Text "src\main\resources\assets\domesurvival\models\block\shaft_furnace_ready_on.json" @("domesurvival:block/shaft_furnace_dark/bfbrickslit", "domesurvival:block/shaft_furnace_dark/campfire_log_lit_blue", "domesurvival:block/shaft_furnace_dark/blue_fire", "domesurvival:block/shaft_furnace_dark/wither_skeleton_head", "wither_skull_head")
Require-Text "src\main\resources\assets\domesurvival\models\block\shaft_furnace_bottom_output.json" @("bottom_output_collar", "minecraft:block/polished_deepslate")
Require-Text "src\main\resources\assets\domesurvival\textures\block\shaft_furnace_dark\blue_fire.png.mcmeta" @('"frametime": 2', '"interpolate": false')
Require-Text "src\main\resources\assets\domesurvival\models\item\shaft_furnace.json" @("domesurvival:block/shaft_furnace_dark/bfbricksdark", "minecraft:block/polished_deepslate", "domesurvival:block/shaft_furnace_dark/wither_skeleton_head", "wither_skull_head", '"display"')
Require-Text "src\main\resources\assets\domesurvival\models\item\steel_ingot.json" @("domesurvival:item/metallurgy/steel_ingot")
Require-Text "src\main\resources\assets\domesurvival\models\item\coal_coke.json" @("domesurvival:item/metallurgy/coal_coke")
Require-Text "src\main\resources\assets\domesurvival\models\item\slag.json" @("minecraft:item/flint")
Require-Text "src\main\java\com\wasted\domesurvival\forge\client\ClientModEvents.java" @("registerMetallurgyItemColors")
Require-Text "src\main\java\com\wasted\domesurvival\forge\client\screen\MetallurgyGui.java" @(
    "drawProcess",
    "drawHeatChamber",
    "System.currentTimeMillis()",
    "PROGRESS_W"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\client\screen\CokeOvenScreen.java" @(
    "getTimerText",
    "getProgressMax() - menu.getProgress()",
    "formatTicks"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\client\screen\ShaftFurnaceScreen.java" @(
    "getTimerText",
    "getProgressMax() - menu.getProgress()",
    "formatTicks"
)

$metallurgyGuiPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\client\screen\MetallurgyGui.java"
$metallurgyGuiText = Get-Content -LiteralPath $metallurgyGuiPath -Raw
if ($metallurgyGuiText.Contains("drawPort") -or $metallurgyGuiText.Contains("int glint")) {
    $errors.Add("Metallurgy GUI still renders connector markers or a moving process glint")
}

foreach ($screenName in @("CokeOvenScreen.java", "ShaftFurnaceScreen.java")) {
    $screenPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\client\screen\$screenName"
    $screenText = Get-Content -LiteralPath $screenPath -Raw
    if ($screenText.Contains("input_top_tooltip") -or $screenText.Contains("output_bottom_tooltip")) {
        $errors.Add("Connector tooltip remains in $screenName")
    }
}
Require-Text "src\main\resources\data\forge\tags\items\coal_coke.json" @("domesurvival:coal_coke")
Require-Text "src\main\resources\data\forge\tags\items\ingots\steel.json" @("domesurvival:steel_ingot")
Require-Text "src\main\resources\data\domesurvival\recipes\airlock_binding_key.json" @("minecraft:compass", "domesurvival:tin_gear", "domesurvival:steel_gear")
Require-Text "src\main\resources\data\domesurvival\recipes\airlock_control_panel.json" @("minecraft:piston", "domesurvival:lead_gear")
Require-Text "src\main\resources\data\domesurvival\recipes\airlock_gate.json" @("minecraft:piston", "domesurvival:nickel_gear", "domesurvival:lead_gear", "domesurvival:steel_gear")
Require-Text "src\main\java\com\wasted\domesurvival\forge\technology\TechnologyRegistry.java" @(
    "domesurvival:steel_gear",
    "domesurvival:tin_gear",
    "domesurvival:lead_gear",
    "domesurvival:nickel_gear",
    "domesurvival:airlock_binding_key",
    "domesurvival:airlock_control_panel",
    "domesurvival:airlock_gate"
)
Require-Text "dev\quest_master\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt" @(
    "POWER_PROGRAM_STARTED",
    "POWER_TRANSMISSION_TECH_KNOWN",
    "POWER_STORAGE_TECH_KNOWN",
    "DOME_POWER_ONLINE"
)
Require-Text "dev\quest_master\ftbquests\quests\chapters\76CBABB04B110F16.snbt" @(
    "WATER_PURIFICATION_TECH_KNOWN",
    "OXYGEN_ELECTROLYSIS_TECH_KNOWN",
    "OXYGEN_DISTRIBUTION_TECH_KNOWN",
    "OXYGEN_FILLING_TECH_KNOWN",
    "PORTABLE_OXYGEN_TECH_KNOWN"
)

$recipeRoot = Join-Path $projectPath "src\main\resources\data\domesurvival\recipes"
$legacyClayRecipe = Join-Path $recipeRoot "desert_clay_washing.json"
if (Test-Path -LiteralPath $legacyClayRecipe -PathType Leaf) {
    $errors.Add("Legacy crafting recipe still produces clay instead of using world sand sifting")
}
$earlyEnderIo = Get-ChildItem -LiteralPath $recipeRoot -Filter "*.json" -File |
    Select-String -SimpleMatch '"enderio:'
if ($earlyEnderIo) {
    $errors.Add("A DomeSurvival recipe still has a direct EnderIO dependency before the automation stage")
}

foreach ($airlockRecipe in @("airlock_binding_key.json", "airlock_control_panel.json", "airlock_gate.json")) {
    $airlockText = Get-Content -LiteralPath (Join-Path $recipeRoot $airlockRecipe) -Raw
    if ($airlockText.Contains('"mekanism:')) {
        $errors.Add("Early airlock recipe still depends on Mekanism: $airlockRecipe")
    }
}

foreach ($metallurgyRecipe in @("coke_oven.json", "shaft_furnace.json")) {
    $metallurgyText = Get-Content -LiteralPath (Join-Path $recipeRoot $metallurgyRecipe) -Raw
    if ($metallurgyText.Contains('"immersiveengineering:')) {
        $errors.Add("Early DomeSurvival metallurgy still depends on Immersive Engineering: $metallurgyRecipe")
    }
}

$cokeOvenRecipeText = Get-Content -LiteralPath (Join-Path $recipeRoot "coke_oven.json") -Raw
if ($cokeOvenRecipeText.Contains('"domesurvival:coal_coke"') -or
    $cokeOvenRecipeText.Contains('"forge:ingots/steel"') -or
    $cokeOvenRecipeText.Contains('"domesurvival:steel_ingot"')) {
    $errors.Add("Coke oven recipe contains a circular coke or steel dependency")
}

$shaftRecipeText = Get-Content -LiteralPath (Join-Path $recipeRoot "shaft_furnace.json") -Raw
if ($shaftRecipeText.Contains('"forge:ingots/steel"') -or
    $shaftRecipeText.Contains('"domesurvival:steel_ingot"') -or
    $shaftRecipeText.Contains('"domesurvival:steel_gear"')) {
    $errors.Add("Shaft furnace recipe contains a circular steel dependency")
}

$technologyText = Get-Content -LiteralPath (Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\technology\TechnologyRegistry.java") -Raw
if ($technologyText.Contains('"immersiveengineering:blastbrick"')) {
    $errors.Add("IE blast bricks are still unlocked at the early power stage")
}

foreach ($machineEntity in @("CokeOvenBlockEntity.java", "ShaftFurnaceBlockEntity.java")) {
    $machinePath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\machine\shaft\$machineEntity"
    $machineText = Get-Content -LiteralPath $machinePath -Raw
    if ($machineText.Contains("side == null") -or $machineText.Contains("fullCapability")) {
        $errors.Add("$machineEntity exposes an unsided item handler that bypasses top/bottom connectors")
    }
}

foreach ($file in @("38F6E366B367B563.snbt", "4A2E731D5C9B684F.snbt", "76CBABB04B110F16.snbt")) {
    $source = Join-Path $projectPath "dev\quest_master\ftbquests\quests\chapters\$file"
    $runtime = Join-Path $projectPath "run\config\ftbquests\quests\chapters\$file"
    if ((Test-Path -LiteralPath $runtime) -and
        ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $runtime -Algorithm SHA256).Hash)) {
        $errors.Add("Runtime quest differs from source: $file")
    }
}

$builtJar = Join-Path $projectPath "build\libs\domesurvival-0.1.1.jar"
$runtimeJar = Join-Path $projectPath "run\mods\domesurvival-0.1.1-dev.jar"
if ((Test-Path -LiteralPath $builtJar) -and (Test-Path -LiteralPath $runtimeJar) -and
    ((Get-FileHash -LiteralPath $builtJar -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $runtimeJar -Algorithm SHA256).Hash)) {
    $errors.Add("Runtime DomeSurvival JAR differs from the freshly built JAR")
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    throw "Technology progression verification failed with $($errors.Count) error(s)."
}

Write-Host "Technology progression v10.0 verification passed." -ForegroundColor Green
