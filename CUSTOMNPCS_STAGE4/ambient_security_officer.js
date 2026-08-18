/*
 * Dome Survival - ambient NPC
 * iVan — Служба безопасности купола
 *
 * V5.6:
 * - stays on exact post position
 * - does not walk
 * - turns toward the nearest player within 48 blocks
 * - always says: Player's Club
 */

var NPC_TAG = "domesurvival_ambient_security";
var INTERACT_COOLDOWN_MS = 700;
var PHRASE = "Player's Club";

var FIXED_X = -534.938;
var FIXED_Y = 62.0;
var FIXED_Z = -664.466;

var lastInteractByPlayer = {};

function safe(action) {
    try {
        action();
    } catch (ignored) {
    }
}

var LOOK_RANGE = 48.0;
var LOOK_RANGE_SQ = LOOK_RANGE * LOOK_RANGE;

function keepOnPost(e) {
    safe(function() { e.npc.getAi().setMovingType(0); });
    safe(function() { e.npc.getAi().setReturnsHome(false); });
    safe(function() {
        e.npc.setPosition(FIXED_X, FIXED_Y, FIXED_Z);
    });
}

function nearestPlayer(e) {
    var players = null;

    try {
        players = e.npc.getWorld().getAllPlayers();
    } catch (ignored1) {
        try {
            players = e.npc.world.getAllPlayers();
        } catch (ignored2) {
            return null;
        }
    }

    if (players == null || players.length == 0) {
        return null;
    }

    var closest = null;
    var closestSq = LOOK_RANGE_SQ;

    for (var i = 0; i < players.length; i++) {
        var player = players[i];

        try {
            var dx = player.getX() - FIXED_X;
            var dy = player.getY() - FIXED_Y;
            var dz = player.getZ() - FIXED_Z;
            var distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq <= closestSq) {
                closestSq = distanceSq;
                closest = player;
            }
        } catch (ignored) {
        }
    }

    return closest;
}

function facePlayer(e, player) {
    if (player == null) {
        return;
    }

    try {
        var dx = player.getX() - FIXED_X;
        var dz = player.getZ() - FIXED_Z;

        if (dx * dx + dz * dz < 0.0001) {
            return;
        }

        // Minecraft yaw:
        // 0 = +Z, -90 = +X, 90 = -X, +/-180 = -Z
        var yaw = Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0;

        while (yaw < -180.0) {
            yaw += 360.0;
        }
        while (yaw >= 180.0) {
            yaw -= 360.0;
        }

        e.npc.setRotation(yaw);
    } catch (ignored) {
    }
}

function updatePostLook(e) {
    keepOnPost(e);
    facePlayer(e, nearestPlayer(e));
}

function init(e) {
    safe(function() { e.npc.addTag(NPC_TAG); });
    safe(function() { e.npc.getDisplay().setName("iVan"); });
    safe(function() { e.npc.getDisplay().setTitle("Служба безопасности купола"); });
    safe(function() {
        e.npc.getDisplay().setSkinTexture(
            "domesurvival:textures/npc/dome_security_officer.png"
        );
    });
    safe(function() { e.npc.getDisplay().setShowName(0); });

    safe(function() { e.npc.getAi().setMovingType(0); });
    safe(function() { e.npc.getAi().setReturnsHome(false); });
    safe(function() { e.npc.getAi().setRetaliateType(3); });
    safe(function() { e.npc.getAi().setInteractWithNPCs(false); });
    safe(function() { e.npc.getAi().setStopOnInteract(false); });

    safe(function() { e.npc.setHome(-535, 62, -664); });

    updatePostLook(e);
    safe(function() { e.npc.updateClient(); });
}

function tick(e) {
    updatePostLook(e);
}

function target(e) {
    safe(function() { e.setCanceled(true); });
    updatePostLook(e);
}

function interact(e) {
    safe(function() { e.setCanceled(true); });

    keepOnPost(e);
    facePlayer(e, e.player);

    var key = String(e.player.getUUID());
    var now = new Date().getTime();
    var previous = lastInteractByPlayer[key];

    if (previous != null && now - previous < INTERACT_COOLDOWN_MS) {
        return;
    }

    lastInteractByPlayer[key] = now;

    safe(function() {
        e.npc.sayTo(e.player, PHRASE);
    });
}
