$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
$Bridge = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\progression\JosephCooperBridge.java"

if (-not (Test-Path -LiteralPath $Joseph)) { throw "Joseph GUI source not found: $Joseph" }
if (-not (Test-Path -LiteralPath $Bridge)) { throw "JosephCooperBridge.java not found: $Bridge" }

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Js = [IO.File]::ReadAllText($Joseph,[Text.Encoding]::UTF8)
$Java = [IO.File]::ReadAllText($Bridge,[Text.Encoding]::UTF8)

if (-not $Js.Contains("GUI v7.3 MULTIMOD + TEST SKIP") -and
    -not $Js.Contains("GUI v7.3.2 MULTIMOD + PATH")) {
    throw "Stage-1 path upgrade requires Joseph V7.3."
}

if ($Js.Contains("GUI v7.3.2 MULTIMOD + PATH")) {
    Write-Host "[OK] Stage-1 path upgrade already installed." -ForegroundColor Green
    exit 0
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_path_v732_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force
Copy-Item -LiteralPath $Bridge -Destination (Join-Path $Backup "JosephCooperBridge.java") -Force

$Js = $Js.Replace(
    "/* Dome Survival - Joseph Cooper GUI v7.3 MULTIMOD + TEST SKIP */",
    "/* Dome Survival - Joseph Cooper GUI v7.3.2 MULTIMOD + PATH */"
)
$Js = $Js.Replace(
    'var GUI_NAME = "dome_joseph_v73_multimod_testskip";',
    'var GUI_NAME = "dome_joseph_v732_multimod_path";'
)

$JsHelper = @'

var STAGE1_PATH_UPGRADE_KEY = "domesurvival.stage01.path_upgraded.v73";

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

'@

# Helper is inserted before inventory operations.
if (-not $Js.Contains("function ensureStage1PathUpgrade(player)")) {
    $anchor = "function inventoryCount(player, id) {"
    if (-not $Js.Contains($anchor)) { throw "Could not find inventoryCount() JS anchor." }
    $Js = $Js.Replace($anchor, $JsHelper + "`r`n" + $anchor)
}

# Real Stage 01 completion: upgrade immediately and include its notice.
$oldStage1 = @'
        if (!before1 && after1) {
            notices1.push(grantStageReward(e.player, R1_KEY, R1));
            celebrate(e.player,
'@
$newStage1 = @'
        if (!before1 && after1) {
            var pathNotice1 = ensureStage1PathUpgrade(e.player);
            if (pathNotice1 != null && pathNotice1.length > 0) notices1.push(pathNotice1);
            notices1.push(grantStageReward(e.player, R1_KEY, R1));
            celebrate(e.player,
'@

if ($Js.Contains($oldStage1)) {
    $Js = $Js.Replace($oldStage1,$newStage1)
}
elseif (-not $Js.Contains("var pathNotice1 = ensureStage1PathUpgrade(e.player);")) {
    throw "Could not patch normal Stage 01 completion hook."
}

# /josephscript nextstage path: on the next right-click stage1 is already
# complete, so apply the physical path upgrade here too.
$oldInteract = @'
    grantPendingRewards(e.player);
    openMain(e.player);
'@
$newInteract = @'
    grantPendingRewards(e.player);
    ensureStage1PathUpgrade(e.player);
    openMain(e.player);
'@

if ($Js.Contains($oldInteract)) {
    $Js = $Js.Replace($oldInteract,$newInteract)
}
elseif (-not $Js.Contains("ensureStage1PathUpgrade(e.player);")) {
    throw "Could not patch Joseph interact() path hook."
}

# Java imports.
if (-not $Java.Contains("import net.minecraft.core.BlockPos;")) {
    $Java = $Java.Replace(
        "import net.minecraft.server.MinecraftServer;",
        "import net.minecraft.core.BlockPos;`r`nimport net.minecraft.server.MinecraftServer;`r`nimport net.minecraft.server.level.ServerLevel;"
    )
}
if (-not $Java.Contains("import net.minecraft.world.level.block.Blocks;")) {
    $Java = $Java.Replace(
        "import net.minecraft.server.level.ServerPlayer;",
        "import net.minecraft.server.level.ServerPlayer;`r`nimport net.minecraft.world.level.block.Blocks;`r`nimport net.minecraft.world.level.block.state.BlockState;"
    )
}
if (-not $Java.Contains("import java.util.ArrayDeque;")) {
    $Java = $Java.Replace(
        "import net.minecraftforge.server.ServerLifecycleHooks;",
        "import net.minecraftforge.server.ServerLifecycleHooks;`r`n`r`nimport java.util.ArrayDeque;`r`nimport java.util.HashSet;`r`nimport java.util.Set;"
    )
}

$JavaMethod = @'

    /**
     * Stage 01 environmental upgrade.
     *
     * Converts ONLY the connected exposed dirt/coarse-dirt strip nearest Joseph
     * into vanilla dirt path blocks. The search is deliberately bounded so it
     * cannot spread through the whole dome terrain.
     *
     * @return changed block count; 0 when a finished path is already found;
     *         negative values mean the path could not be resolved safely.
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
        boolean finishedPathNearby = false;

        // Joseph can stand one or two blocks above the visible trail depending
        // on the floor/model placement, so scan a small vertical band.
        for (int dy = -2; dy <= 0; dy++) {
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    BlockPos pos = npc.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (state.is(Blocks.DIRT_PATH)) {
                        finishedPathNearby = true;
                    }

                    if (!isStage1TrailSource(state) || !isExposedTrailSurface(level, pos)) {
                        continue;
                    }

                    double distance = dx * dx + dz * dz + dy * dy * 2.0;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        start = pos.immutable();
                    }
                }
            }
        }

        if (start == null) {
            return finishedPathNearby ? 0 : -1;
        }

        final int maxRadius = 40;
        final int maxBlocks = 192;
        final int baseY = start.getY();

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(start);
        visited.add(start.asLong());

        int changed = 0;

        while (!queue.isEmpty() && changed < maxBlocks) {
            BlockPos pos = queue.removeFirst();

            int dxFromStart = pos.getX() - start.getX();
            int dzFromStart = pos.getZ() - start.getZ();
            if (dxFromStart * dxFromStart + dzFromStart * dzFromStart > maxRadius * maxRadius) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!isStage1TrailSource(state) || !isExposedTrailSurface(level, pos)) {
                continue;
            }

            level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 3);
            changed++;

            // Horizontal connected trail only. A one-block vertical tolerance
            // allows small steps without letting the search spread vertically.
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                for (int yOffset = -1; yOffset <= 1; yOffset++) {
                    BlockPos next = new BlockPos(
                        pos.getX() + dir[0],
                        baseY + yOffset,
                        pos.getZ() + dir[1]
                    );

                    if (Math.abs(next.getY() - baseY) > 1 || !visited.add(next.asLong())) {
                        continue;
                    }

                    BlockState nextState = level.getBlockState(next);
                    if (isStage1TrailSource(nextState) && isExposedTrailSurface(level, next)) {
                        queue.addLast(next);
                    }
                }
            }
        }

        return changed;
    }

    private static boolean isStage1TrailSource(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT);
    }

    private static boolean isExposedTrailSurface(ServerLevel level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.isAir() || above.canBeReplaced();
    }

'@

if (-not $Java.Contains("public static int upgradeStage1Path(")) {
    $anchor = "    public static String progressText() {"
    if (-not $Java.Contains($anchor)) { throw "Could not find JosephCooperBridge progressText() anchor." }
    $Java = $Java.Replace($anchor, $JavaMethod + "`r`n" + $anchor)
}

$Required = @(
    "GUI v7.3.2 MULTIMOD + PATH",
    "function ensureStage1PathUpgrade(player)",
    "STAGE1_PATH_UPGRADE_KEY",
    "Bridge.upgradeStage1Path",
    "public static int upgradeStage1Path(",
    "Blocks.DIRT_PATH",
    "Blocks.COARSE_DIRT",
    "maxBlocks = 192"
)
foreach ($marker in $Required) {
    if (-not ($Js.Contains($marker) -or $Java.Contains($marker))) {
        throw "V7.3.2 validation failed. Missing marker: $marker"
    }
}

[IO.File]::WriteAllText($Joseph,$Js,$Utf8NoBom)
[IO.File]::WriteAllText($Bridge,$Java,$Utf8NoBom)

# Refresh the script used by CustomNPCs.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))
$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) { $Targets.Add($WorldLocal) }

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $Target = Join-Path $TargetDir "joseph_cooper_gui.js"
    Copy-Item -LiteralPath $Joseph -Destination $Target -Force
}

Write-Host "[OK] Stage 01 path upgrade installed." -ForegroundColor Green
Write-Host "After Stage 01, connected exposed dirt/coarse dirt nearest Joseph becomes minecraft:dirt_path."
Write-Host "Safety limits: radius 40 blocks, maximum 192 converted blocks, no grass/stone/structures touched."
exit 0
