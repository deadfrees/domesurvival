package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.core.airlock.AirlockDoor;
import com.wasted.domesurvival.core.airlock.AirlockState;
import com.wasted.domesurvival.core.airlock.AirlockStateMachine;
import com.wasted.domesurvival.core.airlock.AirlockTransition;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public final class AirlockService {
    private AirlockService() {
    }

    public static void handlePanelUse(ServerLevel level, BlockPos panelPos, Player player) {
        AirlockDoor side = panelSide(panelPos);
        if (side == null) {
            player.displayClientMessage(Component.literal("[DomeSurvival] Неизвестная панель шлюза."), false);
            return;
        }

        AirlockTransition result = toggle(level, side);
        if (result.allowed()) {
            player.displayClientMessage(Component.literal("[DomeSurvival] " + localizedState(result.state())), false);
        } else {
            player.displayClientMessage(Component.literal("[DomeSurvival] Блокировка: " + result.message()), false);
        }
    }

    public static AirlockTransition toggle(ServerLevel level, AirlockDoor side) {
        DomeSavedData data = DomeSavedData.get(level);
        AirlockTransition transition = AirlockStateMachine.toggle(data.airlockState(), side);
        if (!transition.allowed()) return transition;

        data.setAirlockState(transition.state());
        applyDoorVisual(level, side, side == AirlockDoor.INNER
                ? transition.state().innerOpen()
                : transition.state().outerOpen());
        syncPanelVisuals(level, transition.state());
        return transition;
    }

    public static void reset(ServerLevel level) {
        DomeSavedData data = DomeSavedData.get(level);
        data.resetAirlock();
        applyDoorVisual(level, AirlockDoor.INNER, false);
        applyDoorVisual(level, AirlockDoor.OUTER, false);
        syncPanelVisuals(level, data.airlockState());
    }

    /** Synchronizes panel indicators with the persisted door state. */
    public static void syncVisuals(ServerLevel level) {
        syncPanelVisuals(level, DomeSavedData.get(level).airlockState());
    }

    private static void syncPanelVisuals(ServerLevel level, AirlockState state) {
        DomeSpec spec = DomeSpec.wastedV1();
        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerPanelZ()),
                state.innerOpen());
        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.outerPanelZ()),
                state.outerOpen());
    }

    private static void applyPanelVisual(ServerLevel level, BlockPos pos, boolean open) {
        var state = level.getBlockState(pos);
        if (state.is(ModBlocks.AIRLOCK_PANEL.get())) {
            level.setBlock(pos, state.setValue(AirlockPanelBlock.LIT, open), 3);
        }
    }

    public static boolean isBreathable(ServerLevel level) {
        return DomeSavedData.get(level).airlockState().breathable();
    }

    public static AirlockState state(ServerLevel level) {
        return DomeSavedData.get(level).airlockState();
    }

    private static AirlockDoor panelSide(BlockPos pos) {
        DomeSpec spec = DomeSpec.wastedV1();
        if (pos.getX() != spec.airlockPanelX() || pos.getY() != spec.airlockPanelY()) return null;
        if (pos.getZ() == spec.innerPanelZ()) return AirlockDoor.INNER;
        if (pos.getZ() == spec.outerPanelZ()) return AirlockDoor.OUTER;
        return null;
    }

    private static void applyDoorVisual(ServerLevel level, AirlockDoor side, boolean open) {
        DomeSpec spec = DomeSpec.wastedV1();
        int z = side == AirlockDoor.INNER ? spec.innerDoorZ() : spec.outerDoorZ();

        for (int x = spec.airlockCenterX() - spec.airlockDoorHalfWidth();
             x <= spec.airlockCenterX() + spec.airlockDoorHalfWidth(); x++) {
            for (int y = spec.airlockDoorMinY(); y <= spec.airlockDoorMaxY(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                level.setBlock(pos,
                        open ? Blocks.AIR.defaultBlockState() : ModBlocks.AIRLOCK_DOOR.get().defaultBlockState(),
                        3);
            }
        }
    }

    public static String localizedState(AirlockState state) {
        return "внутренняя=" + (state.innerOpen() ? "ОТКРЫТА" : "закрыта")
                + ", внешняя=" + (state.outerOpen() ? "ОТКРЫТА" : "закрыта")
                + ", камера=" + (state.pressure().name().equals("PRESSURIZED")
                ? "ПОД ДАВЛЕНИЕМ" : "РАЗГЕРМЕТИЗИРОВАНА");
    }
}
