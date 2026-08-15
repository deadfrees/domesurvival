/* Dome Survival - Joseph Cooper GUI v7.0 RELEASE RESET */
var API = Java.type("noppes.npcs.api.NpcAPI").Instance();
var Bridge = Java.type("com.wasted.domesurvival.forge.progression.JosephCooperBridge");

var GUI_NAME = "dome_joseph_v70_release_reset";
var GUI_WIDTH = 320;
var GUI_HEIGHT = 230;

var BTN_PROJECT = 6501;
var BTN_BASE = 6502;
var BTN_PLAN = 6503;
var BTN_CLOSE = 6504;
var BTN_CONTRIBUTE = 6505;
var BTN_BACK = 6506;

/* Stage 01 - Life support */
var S1_COMPLETE = "domesurvival.stage01.complete.v65";
var S1 = [
    { key: "domesurvival.stage01.glass.v65",   id: "minecraft:glass",       name: "Стекло",             req: 64 },
    { key: "domesurvival.stage01.logs.v65",    id: "minecraft:oak_log",     name: "Дубовые брёвна",     req: 48 },
    { key: "domesurvival.stage01.saplings.v65",id: "minecraft:oak_sapling", name: "Саженцы дуба",       req: 12 },
    { key: "domesurvival.stage01.charcoal.v65",id: "minecraft:charcoal",    name: "Древесный уголь",    req: 32 },
    { key: "domesurvival.stage01.wheat.v65",   id: "minecraft:wheat",       name: "Пшеница",            req: 32 },
    { key: "domesurvival.stage01.iron.v65",    id: "minecraft:iron_ingot",  name: "Железные слитки",    req: 16 }
];

/* Stage 02 - Workshop extras. Bridge still owns its original iron/copper/redstone requirements. */
var S2_EXTRA_COMPLETE = "domesurvival.stage02.extras.complete.v65";
var S2 = [
    { key: "domesurvival.stage02.stone.v65", id: "minecraft:stone_bricks", name: "Каменные кирпичи", req: 64 },
    { key: "domesurvival.stage02.bars.v65",  id: "minecraft:iron_bars",    name: "Железные прутья",  req: 32 },
    { key: "domesurvival.stage02.panes.v65", id: "minecraft:glass_pane",   name: "Стеклянные панели", req: 32 },
    { key: "domesurvival.stage02.piston.v65",id: "minecraft:piston",       name: "Поршни",            req: 8 }
];

/* Logical-reset fallback for old test worlds where Java workshop SavedData is already complete.
   Fresh release worlds continue to use JosephCooperBridge for the workshop core. */
var RELEASE_RESET_LOCK = "domesurvival.release.logical_reset.v70";
var S2_RESET_CORE_COMPLETE = "domesurvival.stage02.resetcore.complete.v70";
var S2_RESET_CORE = [
    { key: "domesurvival.stage02.resetcore.iron.v70",     id: "minecraft:iron_ingot",   name: "Железо",   req: 64 },
    { key: "domesurvival.stage02.resetcore.copper.v70",   id: "minecraft:copper_ingot", name: "Медь",     req: 32 },
    { key: "domesurvival.stage02.resetcore.redstone.v70", id: "minecraft:redstone",     name: "Редстоун", req: 24 }
];

/* Stage 03 - Extraction and processing */
var S3_COMPLETE = "domesurvival.stage03.complete.v65";
var S3 = [
    { key: "domesurvival.stage03.coal.v65",   id: "minecraft:coal",         name: "Уголь",             req: 128 },
    { key: "domesurvival.stage03.iron.v65",   id: "minecraft:iron_ingot",   name: "Железные слитки",   req: 64 },
    { key: "domesurvival.stage03.copper.v65", id: "minecraft:copper_ingot", name: "Медные слитки",     req: 64 },
    { key: "domesurvival.stage03.redstone.v65",id:"minecraft:redstone",     name: "Редстоун",          req: 32 },
    { key: "domesurvival.stage03.gold.v65",   id: "minecraft:gold_ingot",   name: "Золотые слитки",    req: 16 }
];

/* Stage 04 - Safe expeditions */
var S4_COMPLETE = "domesurvival.stage04.complete.v68";
var S4 = [
    { key: "domesurvival.stage04.food.v68",   id: "minecraft:cooked_beef", name: "Стейки",          req: 32 },
    { key: "domesurvival.stage04.torches.v68",id: "minecraft:torch",       name: "Факелы",          req: 64 },
    { key: "domesurvival.stage04.arrows.v68", id: "minecraft:arrow",       name: "Стрелы",          req: 64 },
    { key: "domesurvival.stage04.bows.v68",   id: "minecraft:bow",         name: "Луки",            req: 2 },
    { key: "domesurvival.stage04.shields.v68",id: "minecraft:shield",      name: "Щиты",            req: 2 },
    { key: "domesurvival.stage04.swords.v68", id: "minecraft:iron_sword",  name: "Железные мечи",   req: 2 }
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
    e.setCanceled(true);
    var playerName = String(e.player.getName());
    var now = new Date().getTime();
    var previous = lastInteractByPlayer[playerName];
    if (previous != null && (now - previous) < 250) return;
    lastInteractByPlayer[playerName] = now;

    try {
        lastNpcPosByPlayer[playerName] = { x: Number(e.npc.getX()), y: Number(e.npc.getY()), z: Number(e.npc.getZ()) };
    } catch (ignoredPos) {}

    /* Existing saves which already finished the original workshop are treated as having finished Stage 01. */
    migrateStage1IfNeeded(e.player);
    openMain(e.player);
}

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
        if (!before1 && after1) {
            celebrate(e.player,
                "§a[КУПОЛ] Этап 01 «Жизнеобеспечение купола» завершён!",
                "§e[КУПОЛ] Доступен этап 02: «Восстановление мастерской»."
            );
        }
        openProject(e.player, notice1);
        return;
    }

    if (!stage2Complete(e.player)) {
        var before2 = stage2Complete(e.player);
        var notices = [];

        if (logicalResetActive(e.player)) {
            var resetCoreNotice = contributeSet(e.player, S2_RESET_CORE, S2_RESET_CORE_COMPLETE);
            if (resetCoreNotice != null && resetCoreNotice.length > 0) notices.push(resetCoreNotice);
        } else if (!workshopCoreComplete()) {
            try {
                var bridgeNotice = String(Bridge.contributeWorkshop(e.player.getName()));
                if (bridgeNotice != null && bridgeNotice.length > 0) notices.push(bridgeNotice);
            } catch (ignoredCore) {}
        }

        var extraNotice = contributeSet(e.player, S2, S2_EXTRA_COMPLETE);
        if (extraNotice != null && extraNotice.length > 0) notices.push(extraNotice);

        var after2 = stage2Complete(e.player);
        if (after2) {
            var buildNotice = finalizeWorkshopIfReady(e.player);
            if (buildNotice != null && buildNotice.length > 0) notices.push(buildNotice);
        }
        if (!before2 && after2) {
            celebrate(e.player,
                "§a[КУПОЛ] Этап 02 «Восстановление мастерской» завершён!",
                "§e[КУПОЛ] Доступен этап 03: «Добыча и переработка ресурсов»."
            );
        }
        openProject(e.player, compactNotice(notices));
        return;
    }

    if (!stage3Complete(e.player)) {
        var before3 = stage3Complete(e.player);
        var notice3 = contributeSet(e.player, S3, S3_COMPLETE);
        var after3 = stage3Complete(e.player);
        if (!before3 && after3) {
            celebrate(e.player,
                "§a[КУПОЛ] Этап 03 «Добыча и переработка ресурсов» завершён!",
                "§e[КУПОЛ] Доступен этап 04: «Безопасные вылазки»."
            );
        }
        openProject(e.player, notice3);
        return;
    }

    if (!stage4Complete(e.player)) {
        var before4 = stage4Complete(e.player);
        var notice4 = contributeSet(e.player, S4, S4_COMPLETE);
        var after4 = stage4Complete(e.player);
        if (!before4 && after4) {
            celebrate(e.player,
                "§a[КУПОЛ] Этап 04 «Безопасные вылазки» завершён!",
                "§e[КУПОЛ] Доступен этап 05: «Расширение базы»."
            );
        }
        openProject(e.player, notice4);
        return;
    }

    openProject(e.player, "Этап 05 уже доступен.");
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

function inventoryCount(player, id) {
    try { return Math.max(0, Number(player.inventoryItemCount(id))); } catch (ignoredOld) {}
    try {
        var stack = API.createItem(id, 0, 1);
        return Math.max(0, Number(player.getInventory().count(stack, true, true)));
    } catch (ignoredNew) {}
    return 0;
}

function takeItem(player, state, def, index) {
    var current = state.values[index];
    var need = def.req - current;
    if (need <= 0) return 0;

    var available = inventoryCount(player, def.id);
    var amount = Math.min(need, available);
    if (amount <= 0) return 0;

    var removed = false;
    try { removed = !!player.removeItem(def.id, amount); } catch (ignoredRemove) {}
    if (!removed) return 0;

    state.values[index] = current + amount;
    writeInt(state.data, def.key, state.values[index]);
    return amount;
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
    for (var i = parts.length - 1; i >= 0; i--) {
        var accepted = String(parts[i]);
        if (accepted.indexOf("Ресурсы приняты") >= 0) return accepted;
    }
    for (var j = 0; j < parts.length; j++) {
        var candidate = String(parts[j]);
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

function renderSetGrid(gui, startId, defs, state, y) {
    var rows = Math.ceil(defs.length / 2);
    for (var i = 0; i < defs.length; i++) {
        var col = i < rows ? 0 : 1;
        var row = col == 0 ? i : (i - rows);
        var x = col == 0 ? 28 : 166;
        var line = defs[i].name + ": " + state.values[i] + " / " + defs[i].req;
        var color = state.values[i] >= defs[i].req ? 0x77DD77 : 0xEEEEEE;
        gui.addLabel(startId + i, line, x, y + row * 14, 136, 11, color);
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
    for (var j = 0; j < S2.length; j++) lines.push(S2[j].name + ": " + extras.values[j] + " / " + S2[j].req);

    var rows = Math.ceil(lines.length / 2);
    for (var k = 0; k < lines.length; k++) {
        var col = k < rows ? 0 : 1;
        var row = col == 0 ? k : k - rows;
        gui.addLabel(40 + k, lines[k], col == 0 ? 28 : 166, y + row * 14, 136, 11, 0xEEEEEE);
    }
}

function currentStage(player) {
    if (!stage1Complete(player)) return 1;
    if (!stage2Complete(player)) return 2;
    if (!stage3Complete(player)) return 3;
    if (!stage4Complete(player)) return 4;
    return 5;
}

function currentProjectTitle(player) {
    var stage = currentStage(player);
    if (stage == 1) return "Жизнеобеспечение купола";
    if (stage == 2) {
        try { return String(Bridge.projectTitle()); } catch (ignored) { return "Восстановление мастерской"; }
    }
    if (stage == 3) return "Добыча и переработка ресурсов";
    if (stage == 4) return "Безопасные вылазки";
    return "Расширение базы";
}

function openMain(player) {
    var gui = makeGui(player);
    addHeader(gui, "СПИСОК / ТЕКУЩАЯ ОБСТАНОВКА");
    var stage = currentStage(player);
    var narrative = "";
    if (stage == 1) narrative = "Сначала стабилизируем жизнь внутри купола: укрытие, деревья, топливо и запас пищи.";
    else if (stage == 2) narrative = "Жизнеобеспечение стабильно. Теперь восстанавливаем мастерскую и комплектуем рабочую зону.";
    else if (stage == 3) narrative = "Мастерская готова. Нужен серьёзный запас топлива и обработанного металла для дальнейшей работы.";
    else if (stage == 4) narrative = "Снабжение стабилизировано. Формируем экспедиционный комплект для безопасных выходов за пределы купола.";
    else narrative = "Первая экспедиция подготовлена. Следующий шаг — расширение полезной площади базы.";
    addLines(gui, 20, 18, 72, 282, 0xEEEEEE, narrative, 52, 5);

    gui.addLabel(80, "Текущий проект:", 18, 134, 96, 11, 0xAAAAAA);
    gui.addLabel(81, currentProjectTitle(player), 116, 134, 190, 11, 0xFFFFFF);
    gui.addLabel(82, stage == 5 ? "[ ДОСТУПНО ]" : "[ В РАБОТЕ ]", 238, 147, 78, 11, 0xFFD45A);

    addVisibleButton(gui, BTN_PROJECT, "Проект", 18, 160, 136, 20, 7601);
    addVisibleButton(gui, BTN_BASE, "Состояние базы", 166, 160, 136, 20, 7602);
    addVisibleButton(gui, BTN_PLAN, "План развития", 18, 190, 136, 20, 7603);
    addVisibleButton(gui, BTN_CLOSE, "Закрыть", 166, 190, 136, 20, 7604);
    player.showCustomGui(gui);
}

function addNotice(gui, notice) {
    if (notice == null || String(notice).length == 0) return;
    gui.addLabel(90, String(notice), 18, 188, 284, 11, 0xFFD45A);
}

function openProject(player, notice) {
    var stage = currentStage(player);
    if (stage == 1) { openStage1(player, notice); return; }
    if (stage == 2) { openStage2(player, notice); return; }
    if (stage == 3) { openStage3(player, notice); return; }
    if (stage == 4) { openStage4(player, notice); return; }
    openStage5Locked(player, notice);
}

function openStage1(player, notice) {
    var gui = makeGui(player);
    addHeader(gui, "ПРОЕКТ 01 / ЖИЗНЕОБЕСПЕЧЕНИЕ");
    gui.addLabel(20, "Стабилизация жизни внутри купола", 18, 72, 284, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 18, 92, 120, 11, 0xAAAAAA);
    var state = setState(player, S1, S1_COMPLETE);
    renderSetGrid(gui, 30, S1, state, 108);

    if (state.complete) {
        gui.addLabel(70, "Статус", 18, 158, 80, 11, 0xAAAAAA);
        gui.addLabel(71, "Жизнеобеспечение стабилизировано. Этап 02 доступен.", 28, 174, 270, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать ресурсы", 75, 168, 170, 20, 7610);
    }
    addNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openStage2(player, notice) {
    var gui = makeGui(player);
    addHeader(gui, "ПРОЕКТ 02 / МАСТЕРСКАЯ");
    gui.addLabel(20, "Восстановление и комплектация мастерской", 18, 72, 284, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 18, 92, 120, 11, 0xAAAAAA);
    renderStage2Grid(gui, player, 108);

    var complete = stage2Complete(player);
    if (complete && !workshopBuilt()) {
        var retryBuildNotice = finalizeWorkshopIfReady(player);
        if (retryBuildNotice != null && retryBuildNotice.length > 0) {
            notice = retryBuildNotice;
        }
    }
    if (complete) {
        gui.addLabel(70, "Статус", 18, 158, 80, 11, 0xAAAAAA);
        gui.addLabel(71, workshopBuilt() ? "Мастерская восстановлена и укомплектована. Этап 03 доступен." : "Все материалы собраны. Этап 03 доступен.", 28, 174, 280, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать ресурсы", 75, 168, 170, 20, 7610);
    }
    addNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openStage3(player, notice) {
    var gui = makeGui(player);
    addHeader(gui, "ПРОЕКТ 03 / ДОБЫЧА И ПЕРЕРАБОТКА");
    gui.addLabel(20, "Стратегический запас сырья и металлов", 18, 72, 284, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемые ресурсы", 18, 92, 120, 11, 0xAAAAAA);
    var state = setState(player, S3, S3_COMPLETE);
    renderSetGrid(gui, 30, S3, state, 108);

    if (state.complete) {
        gui.addLabel(70, "Статус", 18, 158, 80, 11, 0xAAAAAA);
        gui.addLabel(71, "Снабжение стабилизировано. Этап 04 доступен.", 28, 174, 270, 11, 0x55FF55);
    } else {
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать ресурсы", 75, 168, 170, 20, 7610);
    }
    addNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openStage4(player, notice) {
    var gui = makeGui(player);
    addHeader(gui, "ПРОЕКТ 04 / БЕЗОПАСНЫЕ ВЫЛАЗКИ");
    gui.addLabel(20, "Подготовка экспедиционного комплекта", 18, 72, 284, 11, 0xFFFFFF);
    gui.addLabel(21, "Требуемое снабжение", 18, 92, 140, 11, 0xAAAAAA);
    var state = setState(player, S4, S4_COMPLETE);
    renderSetGrid(gui, 30, S4, state, 108);

    if (state.complete) {
        gui.addLabel(70, "Статус", 18, 158, 80, 11, 0xAAAAAA);
        gui.addLabel(71, "Экспедиционный комплект готов. Этап 05 доступен.", 28, 174, 270, 11, 0x55FF55);
    } else {
        var info = (notice != null && String(notice).length > 0)
            ? String(notice)
            : "Перед выходом обязательны кислородная маска и баллон.";
        gui.addLabel(72, info, 18, 151, 284, 11, 0xFFD45A);
        addVisibleButton(gui, BTN_CONTRIBUTE, "Передать снабжение", 75, 170, 170, 20, 7611);
    }

    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openStage5Locked(player, notice) {
    var gui = makeGui(player);
    addHeader(gui, "ЭТАП 05 / РАСШИРЕНИЕ БАЗЫ");
    gui.addLabel(20, "Следующий этап открыт", 18, 80, 284, 11, 0x55FF55);
    addLines(gui, 30, 28, 104, 270, 0xEEEEEE, "Экспедиционный комплект готов. Следующим проектом станет расширение полезной площади базы.", 48, 4);
    addNotice(gui, notice);
    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openBase(player) {
    var gui = makeGui(player);
    addHeader(gui, "СОСТОЯНИЕ БАЗЫ");
    var s1 = stage1Complete(player);
    var s2 = stage2Complete(player);
    var s3 = stage3Complete(player);
    var s4 = stage4Complete(player);
    var built = logicalResetActive(player) ? stage2Complete(player) : workshopBuilt();

    gui.addLabel(20, "Жизнеобеспечение", 24, 78, 120, 11, 0xBBBBBB);
    gui.addLabel(21, s1 ? "Стабильно" : "Формируется", 166, 78, 134, 11, s1 ? 0x55FF55 : 0xFFD45A);
    gui.addLabel(22, "Мастерская", 24, 102, 120, 11, 0xBBBBBB);
    gui.addLabel(23, s2 ? (built ? "Восстановлена" : "Готова") : (workshopCoreComplete() ? "Комплектуется" : "Не восстановлена"), 166, 102, 134, 11, s2 ? 0x55FF55 : 0xFFD45A);
    gui.addLabel(24, "Производство", 24, 126, 120, 11, 0xBBBBBB);
    gui.addLabel(25, s2 ? "Готово" : "Недоступно", 166, 126, 134, 11, s2 ? 0x55FF55 : 0x888888);
    gui.addLabel(26, "Снабжение", 24, 150, 120, 11, 0xBBBBBB);
    gui.addLabel(27, !s2 ? "Заблокировано" : (s3 ? "Стабильно" : "Формируется"), 166, 150, 134, 11, s3 ? 0x55FF55 : (s2 ? 0xFFD45A : 0x888888));

    gui.addLabel(30, "Приоритет", 24, 174, 90, 11, 0xE6B84A);
    var priority = !s1 ? "Стабилизировать жизнеобеспечение купола." : (!s2 ? "Восстановить и укомплектовать мастерскую." : (!s3 ? "Создать стратегический запас ресурсов." : (!s4 ? "Подготовить безопасные вылазки за пределы купола." : "Подготовить расширение базы.")));
    gui.addLabel(31, priority, 105, 174, 195, 11, 0xEEEEEE);

    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}

function openPlan(player) {
    var gui = makeGui(player);
    addHeader(gui, "ПЛАН РАЗВИТИЯ");
    var s1 = stage1Complete(player);
    var s2 = stage2Complete(player);
    var s3 = stage3Complete(player);
    var s4 = stage4Complete(player);
    var stages = [
        "01  Жизнеобеспечение купола",
        "02  Мастерская",
        "03  Добыча и переработка ресурсов",
        "04  Безопасные вылазки",
        "05  Расширение базы"
    ];

    for (var i = 0; i < stages.length; i++) {
        var color = 0x888888;
        if (i == 0) color = s1 ? 0x55FF55 : 0xFFD45A;
        if (i == 1) color = !s1 ? 0x888888 : (s2 ? 0x55FF55 : 0xFFD45A);
        if (i == 2) color = !s2 ? 0x888888 : (s3 ? 0x55FF55 : 0xFFD45A);
        if (i == 3) color = !s3 ? 0x888888 : (s4 ? 0x55FF55 : 0xFFD45A);
        if (i == 4) color = s4 ? 0xFFD45A : 0x888888;
        gui.addLabel(20 + i, stages[i], 26, 76 + (i * 25), 270, 11, color);
    }

    addVisibleButton(gui, BTN_BACK, "Назад", 110, 207, 100, 18, 7620);
    player.showCustomGui(gui);
}
