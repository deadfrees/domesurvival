package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.core.airlock.AirlockDoor;
import com.wasted.domesurvival.core.airlock.AirlockPressure;
import com.wasted.domesurvival.core.airlock.AirlockState;
import com.wasted.domesurvival.core.airlock.AirlockStateMachine;
import com.wasted.domesurvival.core.airlock.AirlockTransition;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateMotion;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class AirlockService {
    private AirlockService() {
    }

    /** Legacy full-block panel handler, kept for existing unmigrated panels. */
    public static void handlePanelUse(ServerLevel level, BlockPos panelPos, Player player) {
        AirlockDoor side = legacyPanelSide(panelPos);
        if (side == null) {
            player.displayClientMessage(Component.literal("[ШЛЮЗ] Неизвестная панель управления.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        applyToggle(level, side, player);
    }

    /**
     * New V53F wall-mounted panel handler.
     *
     * The panel itself occupies the air block in front of the wall.
     * The old legacy-panel coordinate is now the support wall block.
     * That support coordinate tells us exactly which original airlock shutter
     * this panel controls, so no door search or guessing is required.
     */
    public static void handleMountedPanelUse(ServerLevel level, BlockPos panelPos,
                                             BlockState panelState, Player player) {
        if (StarterDomeAirlockV58.isInstalled(level)) {
            player.displayClientMessage(
                    Component.translatable("message.domesurvival.airlock_panel.not_bound")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        AirlockDoor side = mountedPanelSide(panelPos, panelState);
        if (side == null) {
            player.displayClientMessage(Component.literal(
                    "[ШЛЮЗ] Эта панель не привязана к стартовому шлюзу."
            ).withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        applyToggle(level, side, player);
    }

    private static void applyToggle(ServerLevel level, AirlockDoor side, Player player) {
        AirlockTransition result = toggle(level, side);
        if (result.allowed()) {
            player.displayClientMessage(doorStatusComponent(side, result.state()), false);
        } else {
            player.displayClientMessage(interlockComponent(result.message()), false);
        }
    }

    public static AirlockTransition toggle(ServerLevel level, AirlockDoor side) {
        if (StarterDomeAirlockV58.isInstalled(level)) {
            return toggleV58Gate(level, side);
        }

        DomeSavedData data = DomeSavedData.get(level);
        AirlockState before = data.airlockState();
        AirlockTransition transition = AirlockStateMachine.toggle(before, side);
        if (!transition.allowed()) return transition;

        boolean opened = side == AirlockDoor.INNER
                ? transition.state().innerOpen()
                : transition.state().outerOpen();

        data.setAirlockState(transition.state());
        applyDoorVisual(level, side, opened);
        syncPanelVisuals(level, transition.state());
        playDoorSound(level, side, opened);
        return transition;
    }

    private static AirlockTransition toggleV58Gate(ServerLevel level, AirlockDoor side) {
        AirlockGateBlock gate = AirlockGateRegistry.AIRLOCK_GATE.get();
        BlockPos masterPos = StarterDomeAirlockV58.gateMasterPos(side);
        AirlockGateBlock.ToggleResult result = gate.requestToggle(level, masterPos);
        AirlockState current = state(level);

        return switch (result) {
            case OPENING -> new AirlockTransition(
                    true,
                    current,
                    side == AirlockDoor.INNER
                            ? "Внутренние ворота открываются."
                            : "Внешние ворота открываются."
            );
            case CLOSING -> new AirlockTransition(
                    true,
                    current,
                    side == AirlockDoor.INNER
                            ? "Внутренние ворота закрываются."
                            : "Внешние ворота закрываются."
            );
            case MOVING -> new AirlockTransition(
                    false,
                    current,
                    "Ворота уже находятся в движении."
            );
            case INTERLOCK_BLOCKED -> new AirlockTransition(
                    false,
                    current,
                    "Вторая пара ворот ещё не закрыта полностью."
            );
            case INTERLOCK_INVALID -> new AirlockTransition(
                    false,
                    current,
                    "Связь между парами ворот повреждена."
            );
            case INVALID -> new AirlockTransition(
                    false,
                    current,
                    "Контроллер ворот стартового шлюза не найден."
            );
        };
    }

    public static void reset(ServerLevel level) {
        if (StarterDomeAirlockV58.isInstalled(level)) {
            AirlockGateBlock gate = AirlockGateRegistry.AIRLOCK_GATE.get();
            requestCloseIfOpen(level, gate, StarterDomeAirlockV58.innerGateMasterPos());
            requestCloseIfOpen(level, gate, StarterDomeAirlockV58.outerGateMasterPos());
            DomeSavedData.get(level).resetAirlock();
            return;
        }

        DomeSavedData data = DomeSavedData.get(level);
        AirlockState before = data.airlockState();
        data.resetAirlock();
        applyDoorVisual(level, AirlockDoor.INNER, false);
        applyDoorVisual(level, AirlockDoor.OUTER, false);
        syncPanelVisuals(level, data.airlockState());

        if (before.innerOpen()) {
            playDoorSound(level, AirlockDoor.INNER, false);
        }
        if (before.outerOpen()) {
            playDoorSound(level, AirlockDoor.OUTER, false);
        }
    }

    private static void requestCloseIfOpen(ServerLevel level,
                                           AirlockGateBlock gate,
                                           BlockPos masterPos) {
        if (!gate.isValidMaster(level, masterPos)) {
            return;
        }
        BlockState state = level.getBlockState(masterPos);
        if (state.getValue(AirlockGateBlock.MOTION) == AirlockGateMotion.OPEN) {
            gate.requestToggle(level, masterPos);
        }
    }

    /** Synchronizes both legacy full-block and new wall-mounted panel indicators. */
    public static void syncVisuals(ServerLevel level) {
        if (StarterDomeAirlockV58.isInstalled(level)) {
            return;
        }
        syncPanelVisuals(level, DomeSavedData.get(level).airlockState());
    }

    private static void syncPanelVisuals(ServerLevel level, AirlockState state) {
        DomeSpec spec = DomeSpec.wastedV1();

        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerPanelZ()),
                state.innerOpen());
        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerDomePanelZ()),
                state.innerOpen());
        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.outerPanelZ()),
                state.outerOpen());
    }

    /**
     * anchorPos is the ORIGINAL full-block panel position.
     *
     * If the legacy panel still exists, update its LIT state.
     * If it has been migrated, the anchor is now wall material and the new
     * panel is one horizontal block away from it. Update ACTIVE there.
     */
    private static void applyPanelVisual(ServerLevel level, BlockPos anchorPos, boolean open) {
        BlockState anchorState = level.getBlockState(anchorPos);

        if (anchorState.is(ModBlocks.AIRLOCK_PANEL.get())) {
            level.setBlock(anchorPos, anchorState.setValue(AirlockPanelBlock.LIT, open), 3);
        }

        for (Direction outward : Direction.Plane.HORIZONTAL) {
            BlockPos panelPos = anchorPos.relative(outward);
            BlockState panelState = level.getBlockState(panelPos);

            if (panelState.is(AirlockPanelRegistry.block())
                    && panelState.hasProperty(AirlockControlPanelBlock.FACING)
                    && panelState.getValue(AirlockControlPanelBlock.FACING) == outward
                    && panelState.getValue(AirlockControlPanelBlock.ACTIVE) != open) {
                level.setBlock(
                        panelPos,
                        panelState.setValue(AirlockControlPanelBlock.ACTIVE, open),
                        3
                );
            }
        }
    }

    public static boolean isBreathable(ServerLevel level) {
        return state(level).breathable();
    }

    public static AirlockState state(ServerLevel level) {
        if (!StarterDomeAirlockV58.isInstalled(level)) {
            return DomeSavedData.get(level).airlockState();
        }

        boolean outerNotClosed = !StarterDomeAirlockV58.isGateClosed(level, AirlockDoor.OUTER);
        boolean innerNotClosed = !StarterDomeAirlockV58.isGateClosed(level, AirlockDoor.INNER);

        // Outer exposure always wins for safety if a corrupt/manual state ever
        // leaves both gates non-closed. The normal V57 server interlock prevents
        // this during gameplay.
        if (outerNotClosed) {
            return new AirlockState(false, true, AirlockPressure.DEPRESSURIZED);
        }
        if (innerNotClosed) {
            return new AirlockState(true, false, AirlockPressure.PRESSURIZED);
        }
        return AirlockState.initial();
    }

    public static Component statusComponent(AirlockState state) {
        if (state.outerOpen() || state.pressure() == AirlockPressure.DEPRESSURIZED) {
            return Component.literal("[ШЛЮЗ] ВНЕШНЯЯ ОТКРЫТА • КАМЕРА РАЗГЕРМЕТИЗИРОВАНА")
                    .withStyle(ChatFormatting.RED);
        }
        if (state.innerOpen()) {
            return Component.literal("[ШЛЮЗ] ВНУТРЕННЯЯ ОТКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                    .withStyle(ChatFormatting.RED);
        }
        return Component.literal("[ШЛЮЗ] ОБЕ ДВЕРИ ЗАКРЫТЫ • КАМЕРА ПОД ДАВЛЕНИЕМ")
                .withStyle(ChatFormatting.GREEN);
    }

    public static Component doorStatusComponent(AirlockDoor door, AirlockState state) {
        if (door == AirlockDoor.INNER) {
            if (state.innerOpen()) {
                return Component.literal("[ШЛЮЗ] ВНУТРЕННЯЯ ОТКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                        .withStyle(ChatFormatting.RED);
            }
            return Component.literal("[ШЛЮЗ] ВНУТРЕННЯЯ ЗАКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                    .withStyle(ChatFormatting.GREEN);
        }

        if (state.outerOpen() || state.pressure() == AirlockPressure.DEPRESSURIZED) {
            return Component.literal("[ШЛЮЗ] ВНЕШНЯЯ ОТКРЫТА • КАМЕРА РАЗГЕРМЕТИЗИРОВАНА")
                    .withStyle(ChatFormatting.RED);
        }
        return Component.literal("[ШЛЮЗ] ВНЕШНЯЯ ЗАКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                .withStyle(ChatFormatting.GREEN);
    }

    public static Component interlockComponent(String detail) {
        return Component.literal("[ШЛЮЗ] БЛОКИРОВКА • " + detail)
                .withStyle(ChatFormatting.YELLOW);
    }

    private static AirlockDoor legacyPanelSide(BlockPos pos) {
        DomeSpec spec = DomeSpec.wastedV1();
        if (pos.getX() != spec.airlockPanelX() || pos.getY() != spec.airlockPanelY()) return null;

        if (pos.getZ() == spec.innerPanelZ() || pos.getZ() == spec.innerDomePanelZ()) {
            return AirlockDoor.INNER;
        }
        if (pos.getZ() == spec.outerPanelZ()) {
            return AirlockDoor.OUTER;
        }
        return null;
    }

    /**
     * Resolves a wall-mounted panel through its support wall block.
     * Migration places that support exactly at the old panel coordinate.
     */
    private static AirlockDoor mountedPanelSide(BlockPos panelPos, BlockState panelState) {
        if (!panelState.hasProperty(AirlockControlPanelBlock.FACING)) return null;

        Direction facing = panelState.getValue(AirlockControlPanelBlock.FACING);
        BlockPos supportPos = panelPos.relative(facing.getOpposite());
        return legacyPanelSide(supportPos);
    }

    private static void applyDoorVisual(ServerLevel level, AirlockDoor side, boolean open) {
        DomeSpec spec = DomeSpec.wastedV1();
        int z = side == AirlockDoor.INNER ? spec.innerDoorZ() : spec.outerDoorZ();

        for (int x = spec.airlockCenterX() - spec.airlockDoorHalfWidth();
             x <= spec.airlockCenterX() + spec.airlockDoorHalfWidth(); x++) {
            for (int y = spec.airlockDoorMinY(); y <= spec.airlockDoorMaxY(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                level.setBlock(
                        pos,
                        open ? Blocks.AIR.defaultBlockState()
                                : ModBlocks.AIRLOCK_DOOR.get().defaultBlockState(),
                        3
                );
            }
        }
    }

    private static void playDoorSound(ServerLevel level, AirlockDoor side, boolean opened) {
        DomeSpec spec = DomeSpec.wastedV1();
        int z = side == AirlockDoor.INNER ? spec.innerDoorZ() : spec.outerDoorZ();
        BlockPos soundPos = new BlockPos(spec.airlockCenterX(), spec.baseY() + 2, z);

        level.playSound(
                null,
                soundPos,
                opened ? ModSounds.AIRLOCK_OPEN.get() : ModSounds.AIRLOCK_CLOSE.get(),
                SoundSource.BLOCKS,
                1.25F,
                1.0F
        );
    }

    public static String localizedState(AirlockState state) {
        return "внутренняя=" + (state.innerOpen() ? "ОТКРЫТА" : "закрыта")
                + ", внешняя=" + (state.outerOpen() ? "ОТКРЫТА" : "закрыта")
                + ", камера=" + (state.pressure() == AirlockPressure.PRESSURIZED
                ? "ПОД ДАВЛЕНИЕМ" : "РАЗГЕРМЕТИЗИРОВАНА");
    }
}
