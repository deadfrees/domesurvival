$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
$Bridge = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\progression\JosephCooperBridge.java"

if (-not (Test-Path -LiteralPath $Joseph)) { throw "Joseph GUI source not found: $Joseph" }
if (-not (Test-Path -LiteralPath $Bridge)) { throw "JosephCooperBridge.java not found: $Bridge" }

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Js = [IO.File]::ReadAllText($Joseph,[Text.Encoding]::UTF8)
$Java = [IO.File]::ReadAllText($Bridge,[Text.Encoding]::UTF8)

if ($Js.Contains("GUI v7.3.3 RUSSIAN + FULL PATH + REWARDS")) {
    Write-Host "[OK] Joseph V7.3.3 is already installed." -ForegroundColor Green
    exit 0
}

if (-not $Js.Contains("GUI v7.3.2 MULTIMOD + PATH")) {
    throw "V7.3.3 requires V7.3.2. Run the AUTO installer from this archive."
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_v733_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force
Copy-Item -LiteralPath $Bridge -Destination (Join-Path $Backup "JosephCooperBridge.java") -Force

function Replace-RequiredRegex {
    param([string]$Text,[string]$Pattern,[string]$Replacement,[string]$Label)
    $rx = [regex]::new($Pattern,[System.Text.RegularExpressions.RegexOptions]::Singleline)
    $m = $rx.Matches($Text)
    if ($m.Count -ne 1) {
        throw "Patch anchor '$Label' expected 1 match, found $($m.Count)."
    }
    return $rx.Replace($Text,[System.Text.RegularExpressions.MatchEvaluator]{ param($match) return $Replacement },1)
}

$Js = $Js.Replace(
    "/* Dome Survival - Joseph Cooper GUI v7.3.2 MULTIMOD + PATH */",
    "/* Dome Survival - Joseph Cooper GUI v7.3.3 RUSSIAN + FULL PATH + REWARDS */"
)
$Js = $Js.Replace(
    'var GUI_NAME = "dome_joseph_v732_multimod_path";',
    'var GUI_NAME = "dome_joseph_v733_ru_fullpath_rewards";'
)

# Fresh key makes already-completed Stage 01 worlds run the expanded road pass once.
$Js = $Js.Replace(
    'var STAGE1_PATH_UPGRADE_KEY = "domesurvival.stage01.path_upgraded.v73";',
    'var STAGE1_PATH_UPGRADE_KEY = "domesurvival.stage01.path_upgraded.v733";'
)

$QuestName = @'
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
'@
$R4 = @'
var R4_KEY = "domesurvival.stage04.reward.v733";
var R4 = [
    { id: "domesurvival:universal_tank", count: 2, name: "Универсальный резервуар" },
    { id: "domesurvival:reinforced_fluid_pipe", count: 12, name: "Усиленная жидкостная труба" },
    { id: "farmersdelight:rich_soil", count: 8, name: "Плодородная почва" },
    { id: "farmersdelight:oak_cabinet", count: 2, name: "Дубовая кладовая" },
    { id: "farmersdelight:straw_bale", count: 4, name: "Тюк соломы" }
];
'@
$R5 = @'
var R5_KEY = "domesurvival.stage05.reward.v733";
var R5 = [
    { id: "domesurvival:oxygen_mask", count: 2, name: "Кислородная маска" },
    { id: "domesurvival:medium_oxygen_tank", count: 2, name: "Средний кислородный баллон" },
    { id: "domesurvival:reinforced_oxygen_pipe", count: 12, name: "Усиленная кислородная труба" },
    { id: "mekanism:advanced_control_circuit", count: 2, name: "Продвинутая схема управления" },
    { id: "mekanism:energy_tablet", count: 1, name: "Энергетический планшет" },
    { id: "immersiveengineering:component_steel", count: 4, name: "Стальной механический компонент" }
];
'@
$JavaPath = @'

    /**
     * Stage 01 environmental upgrade.
     *
     * Expands the already-visible trail near Joseph into the COMPLETE connected
     * dirt/coarse-dirt/rooted-dirt road. Existing DIRT_PATH blocks are used as
     * connectors so a previous partial conversion can be safely completed.
     *
     * The traversal is bounded and never changes sand, stone, machines,
     * containers, grass, structures or arbitrary terrain.
     */
    public static int upgradeStage1Path(String playerName, double npcX, double npcY, double npcZ) {
        ServerPlayer player = findPlayer(playerName);
        if (player == null) {
            return -2;
        }

        ServerLevel level = player.serverLevel();
        BlockPos npc = BlockPos.containing(npcX, npcY, npcZ);

        BlockPos start = null;
        double bestDistance = Double.MAX_VALUE;

        // Prefer an already existing dirt path. That lets V7.3.3 continue from
        // the partially converted V7.3.2 road instead of searching only for
        // unconverted dirt beside Joseph.
        for (int dy = -2; dy <= 0; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos pos = npc.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (!state.is(Blocks.DIRT_PATH) && !isStage1TrailSource(state)) {
                        continue;
                    }
                    if (!isExposedTrailSurface(level, pos)) {
                        continue;
                    }

                    double distance = dx * dx + dz * dz + dy * dy * 2.0;

                    // Dirt path gets priority over source dirt at similar range.
                    if (state.is(Blocks.DIRT_PATH)) {
                        distance -= 500.0;
                    }

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        start = pos.immutable();
                    }
                }
            }
        }

        if (start == null) {
            return -1;
        }

        final int maxRadius = 64;
        final int maxBlocks = 4096;
        final int minY = start.getY() - 1;
        final int maxY = start.getY() + 1;

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(start);
        visited.add(start.asLong());

        int changed = 0;

        while (!queue.isEmpty() && visited.size() <= maxBlocks) {
            BlockPos pos = queue.removeFirst();

            int dxFromStart = pos.getX() - start.getX();
            int dzFromStart = pos.getZ() - start.getZ();
            if (dxFromStart * dxFromStart + dzFromStart * dzFromStart > maxRadius * maxRadius) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            boolean isExistingPath = state.is(Blocks.DIRT_PATH);
            boolean isSource = isStage1TrailSource(state);

            if ((!isExistingPath && !isSource) || !isExposedTrailSurface(level, pos)) {
                continue;
            }

            if (isSource) {
                level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 3);
                changed++;
            }

            // Traverse both existing path and not-yet-converted dirt. This is
            // the key difference from V7.3.2 and completes the whole road.
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos next = new BlockPos(
                        pos.getX() + dir[0],
                        y,
                        pos.getZ() + dir[1]
                    );

                    if (!visited.add(next.asLong())) {
                        continue;
                    }

                    int ndx = next.getX() - start.getX();
                    int ndz = next.getZ() - start.getZ();
                    if (ndx * ndx + ndz * ndz > maxRadius * maxRadius) {
                        continue;
                    }

                    BlockState nextState = level.getBlockState(next);
                    if ((nextState.is(Blocks.DIRT_PATH) || isStage1TrailSource(nextState))
                            && isExposedTrailSurface(level, next)) {
                        queue.addLast(next);
                    }
                }
            }
        }

        return changed;
    }

    private static boolean isStage1TrailSource(BlockState state) {
        return state.is(Blocks.DIRT)
            || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.ROOTED_DIRT);
    }

    private static boolean isExposedTrailSurface(ServerLevel level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.isAir() || above.canBeReplaced();
    }

'@
$Js = $Js.Replace('Farmer''s Delight: cutting_board','Разделочная доска')
$Js = $Js.Replace('Farmer''s Delight: cooking_pot','Кухонный котёл')
$Js = $Js.Replace('Farmer''s Delight: skillet','Сковорода')
$Js = $Js.Replace('Farmer''s Delight: organic_compost','Органический компост')
$Js = $Js.Replace('Farmer''s Delight: rich_soil','Плодородная почва')
$Js = $Js.Replace('Farmer''s Delight: tomato','Томат')
$Js = $Js.Replace('Farmer''s Delight: cabbage','Капуста')
$Js = $Js.Replace('Farmer''s Delight: onion','Лук')
$Js = $Js.Replace('Farmer''s Delight: rice','Рис')
$Js = $Js.Replace('Farmer''s Delight: stove','Кухонная плита')
$Js = $Js.Replace('Farmer''s Delight: cabbage_seeds','Семена капусты')
$Js = $Js.Replace('Farmer''s Delight: tomato_seeds','Семена томатов')
$Js = $Js.Replace('Brewin'' And Chewin'': jerky','Вяленое мясо')
$Js = $Js.Replace('Brewin'' And Chewin'': keg','Бочонок')
$Js = $Js.Replace('Brewin'' And Chewin'': beer','Пиво')
$Js = $Js.Replace('Brewin'' And Chewin'': mead','Медовуха')
$Js = $Js.Replace('Brewin'' And Chewin'': tankard','Кружка')
$Js = $Js.Replace('Mekanism: basic_control_circuit','Базовая схема управления')
$Js = $Js.Replace('Mekanism: advanced_control_circuit','Продвинутая схема управления')
$Js = $Js.Replace('Mekanism: alloy_infused','Наполненный сплав')
$Js = $Js.Replace('Mekanism: alloy_reinforced','Укреплённый сплав')
$Js = $Js.Replace('Mekanism: energy_tablet','Энергетический планшет')
$Js = $Js.Replace('Mekanism: configurator','Конфигуратор')
$Js = $Js.Replace('Mekanism: steel_ingot','Стальной слиток')
$Js = $Js.Replace('Immersive Engineering: component_iron','Железный механический компонент')
$Js = $Js.Replace('Immersive Engineering: component_steel','Стальной механический компонент')
$Js = $Js.Replace('Immersive Engineering: wire_copper','Медный провод')
$Js = $Js.Replace('Immersive Engineering: plate_steel','Стальная пластина')
$Js = $Js.Replace('Immersive Engineering: hammer','Молот инженера')
$Js = $Js.Replace('Immersive Engineering: component_electronic','Электронная лампа')
$Js = $Js.Replace('Immersive Engineering: component_electronic_adv','Печатная плата')
$Js = $Js.Replace('Ender IO: dark_steel_ingot','Слиток тёмной стали')

# Replace two questionable IE IDs with confirmed localized IE 1.20.1 items.
$Js = $Js.Replace('id: "immersiveengineering:component_electronic"', 'id: "immersiveengineering:electron_tube"')
$Js = $Js.Replace('id: "immersiveengineering:component_electronic_adv"', 'id: "immersiveengineering:circuit_board"')

# Correct Mekanism's actual 1.20.x steel registry ID.
$Js = $Js.Replace('id: "mekanism:steel_ingot"', 'id: "mekanism:ingot_steel"')

$Js = Replace-RequiredRegex $Js `
    'function questItemName\(def\) \{.*?\n\}' `
    $QuestName `
    "questItemName"

$Js = Replace-RequiredRegex $Js `
    'var R4_KEY = "domesurvival\.stage04\.reward\.[^"]+";.*?\n\];' `
    $R4 `
    "Stage 04 reward"

$Js = Replace-RequiredRegex $Js `
    'var R5_KEY = "domesurvival\.stage05\.reward\.[^"]+";.*?\n\];' `
    $R5 `
    "Stage 05 reward"

$Java = Replace-RequiredRegex $Java `
    '    public static int upgradeStage1Path\(.*?    private static boolean isExposedTrailSurface\(ServerLevel level, BlockPos pos\) \{.*?\n    \}' `
    $JavaPath `
    "complete Stage 01 path algorithm"

# Validation: no English fallback prefixes should remain in quest labels.
$Forbidden = @(
    "Farmer's Delight:",
    "Brewin' And Chewin':",
    "Mekanism:",
    "Immersive Engineering:",
    "Ender IO:",
    'id: "mekanism:steel_ingot"',
    'id: "immersiveengineering:component_electronic"',
    'id: "immersiveengineering:component_electronic_adv"',
    'id: "domesurvival:water_purifier"',
    'id: "domesurvival:oxygen_electrolyzer"',
    'id: "domesurvival:oxygen_filler"'
)
foreach ($bad in $Forbidden) {
    if ($Js.Contains($bad)) {
        throw "V7.3.3 validation failed. Forbidden text/item remains: $bad"
    }
}

$RequiredJs = @(
    "GUI v7.3.3 RUSSIAN + FULL PATH + REWARDS",
    'name: "Разделочная доска"',
    'name: "Кухонный котёл"',
    'name: "Базовая схема управления"',
    'name: "Наполненный сплав"',
    'name: "Железный механический компонент"',
    'name: "Медный провод"',
    'name: "Продвинутая схема управления"',
    'name: "Укреплённый сплав"',
    'name: "Вяленое мясо"',
    'name: "Бочонок"',
    'name: "Пиво"',
    'name: "Медовуха"',
    'id: "mekanism:ingot_steel"',
    'id: "immersiveengineering:electron_tube"',
    'id: "immersiveengineering:circuit_board"',
    'domesurvival.stage01.path_upgraded.v733'
)
foreach ($marker in $RequiredJs) {
    if (-not $Js.Contains($marker)) {
        throw "V7.3.3 JS validation failed. Missing: $marker"
    }
}

$RequiredJava = @(
    "maxRadius = 64",
    "maxBlocks = 4096",
    "Blocks.ROOTED_DIRT",
    "nextState.is(Blocks.DIRT_PATH)"
)
foreach ($marker in $RequiredJava) {
    if (-not $Java.Contains($marker)) {
        throw "V7.3.3 Java validation failed. Missing: $marker"
    }
}

[IO.File]::WriteAllText($Joseph,$Js,$Utf8NoBom)
[IO.File]::WriteAllText($Bridge,$Java,$Utf8NoBom)

# Refresh CustomNPCs external script copies.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))
$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) { $Targets.Add($WorldLocal) }

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $Target = Join-Path $TargetDir "joseph_cooper_gui.js"
    Copy-Item -LiteralPath $Joseph -Destination $Target -Force
}

Write-Host ""
Write-Host "[OK] Joseph V7.3.3 installed." -ForegroundColor Green
Write-Host " - all quest/reward labels are Russian"
Write-Host " - Stage 01 road pass expanded to the full connected dirt road"
Write-Host " - water purifier removed from rewards"
Write-Host " - oxygen electrolyzer removed from rewards"
Write-Host " - oxygen filler removed from rewards"
Write-Host ""
Write-Host "Restart FULL DEV. Right-click Joseph once to re-run the Stage 01 full-road upgrade."
exit 0
