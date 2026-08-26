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
Require-Text "src\main\java\com\wasted\domesurvival\forge\network\ModNetwork.java" @('PROTOCOL_VERSION = "5"', "TechnologySyncPacket")
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
    "PROCESS_TIME = 1_200",
    "Items.COAL"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\ShaftFurnaceBlockEntity.java" @(
    "ModItems.STEEL_INGOT",
    "ModItems.SLAG",
    "PROCESS_TIME = 1_600",
    'new ResourceLocation("forge", "coal_coke")'
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
Require-Text "src\main\resources\data\domesurvival\recipes\desert_clay_washing.json" @(
    "minecraft:sand",
    "minecraft:water_bucket",
    '"count": 4'
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\CokeOvenBlock.java" @(
    "DOUBLE_BLOCK_HALF",
    "DoubleBlockHalf.UPPER",
    "UPPER_SHAPE"
)
Require-Text "src\main\java\com\wasted\domesurvival\forge\machine\shaft\ShaftFurnaceBlock.java" @(
    "DOUBLE_BLOCK_HALF",
    "DoubleBlockHalf.UPPER",
    "UPPER_SHAPE"
)
Require-Text "src\main\resources\assets\domesurvival\textures\block\metallurgy\coke_oven_front_on.png.mcmeta" @(
    '"frames": [0, 1, 2, 3, 2, 1]'
)
Require-Text "src\main\resources\assets\domesurvival\textures\block\metallurgy\shaft_furnace_front_on.png.mcmeta" @(
    '"frames": [0, 1, 2, 3, 2, 1]'
)
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
