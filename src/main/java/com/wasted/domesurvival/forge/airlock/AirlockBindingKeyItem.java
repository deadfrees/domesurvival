package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reusable system-control key.
 *
 * Normal RMB on a formed gate selects that gate for 1..N panel binding.
 * Shift+RMB on another formed gate pairs the selected gate with it as one
 * two-door airlock interlock. Shift+RMB in the air clears only the selection.
 */
public final class AirlockBindingKeyItem extends Item {
    private static final String TAG_SELECTED_GATE = "SelectedGate";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_POS = "Pos";

    public AirlockBindingKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.getBlock() instanceof AirlockGateBlock gate) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (player != null && player.isShiftKeyDown()) {
                return handleShiftGate(
                        (ServerLevel) level,
                        clickedPos,
                        clickedState,
                        gate,
                        stack,
                        player
                );
            }

            return selectGate(
                    (ServerLevel) level,
                    clickedPos,
                    clickedState,
                    gate,
                    stack,
                    player
            );
        }

        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                clearSelection(stack);
                player.displayClientMessage(
                        Component.translatable("message.domesurvival.airlock_binding_key.cleared")
                                .withStyle(ChatFormatting.YELLOW),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (clickedState.getBlock() instanceof AirlockControlPanelBlock panelBlock) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            return bindPanel(
                    (ServerLevel) level,
                    clickedPos,
                    clickedState,
                    panelBlock,
                    stack,
                    player
            );
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            clearSelection(stack);
            player.displayClientMessage(
                    Component.translatable("message.domesurvival.airlock_binding_key.cleared")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private InteractionResult selectGate(ServerLevel level,
                                         BlockPos clickedPos,
                                         BlockState clickedState,
                                         AirlockGateBlock gate,
                                         ItemStack stack,
                                         @Nullable Player player) {
        BlockPos masterPos;

        if (!clickedState.getValue(AirlockGateBlock.FORMED)) {
            // Use the construction block's stored facing so commissioning stays
            // deterministic even if the player looks sideways afterward.
            Direction preferredFacing =
                    clickedState.getValue(AirlockGateBlock.FACING);

            AirlockGateBlock.CommissionedGate commissioned =
                    gate.commissionLargestSquare(
                            level,
                            clickedPos,
                            preferredFacing
                    );

            if (commissioned == null) {
                send(
                        player,
                        "message.domesurvival.airlock_binding_key.formation_incomplete",
                        ChatFormatting.RED
                );
                return InteractionResult.CONSUME;
            }

            masterPos = commissioned.masterPos();
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                        "message.domesurvival.airlock_binding_key.gate_formed",
                                        commissioned.size(),
                                        commissioned.size()
                                )
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            }
        } else {
            masterPos = gate.resolveMasterPos(level, clickedPos, clickedState);
        }

        if (masterPos == null || !gate.isValidMaster(level, masterPos)) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.gate_not_formed",
                    ChatFormatting.RED
            );
            return InteractionResult.CONSUME;
        }

        CompoundTag selected = new CompoundTag();
        selected.putString(TAG_DIMENSION, level.dimension().location().toString());
        selected.putLong(TAG_POS, masterPos.asLong());
        stack.getOrCreateTag().put(TAG_SELECTED_GATE, selected);

        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.domesurvival.airlock_binding_key.gate_selected",
                                    masterPos.getX(),
                                    masterPos.getY(),
                                    masterPos.getZ()
                            )
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }

        return InteractionResult.CONSUME;
    }

    private InteractionResult handleShiftGate(ServerLevel level,
                                              BlockPos clickedPos,
                                              BlockState clickedState,
                                              AirlockGateBlock clickedGate,
                                              ItemStack stack,
                                              @Nullable Player player) {
        BlockPos clickedMasterPos = clickedGate.resolveMasterPos(
                level,
                clickedPos,
                clickedState
        );
        if (clickedMasterPos == null) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.gate_not_formed",
                    ChatFormatting.RED
            );
            return InteractionResult.CONSUME;
        }

        SelectedGate selectedGate = readSelection(stack);

        /*
         * No selected gate: Shift+RMB on an already paired gate is the explicit
         * unpair gesture. This never affects panel bindings.
         */
        if (selectedGate == null) {
            AirlockGateBlock.InterlockPairResult result =
                    clickedGate.clearInterlockPair(level, clickedMasterPos);

            switch (result) {
                case UNPAIRED -> send(
                        player,
                        "message.domesurvival.airlock_binding_key.gates_unpaired",
                        ChatFormatting.YELLOW
                );
                case NOT_PAIRED -> send(
                        player,
                        "message.domesurvival.airlock_binding_key.gate_not_paired",
                        ChatFormatting.GRAY
                );
                default -> send(
                        player,
                        "message.domesurvival.airlock_binding_key.gate_missing",
                        ChatFormatting.RED
                );
            }

            return InteractionResult.CONSUME;
        }

        if (!level.dimension().location().equals(selectedGate.dimension())) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.wrong_dimension",
                    ChatFormatting.RED
            );
            return InteractionResult.CONSUME;
        }

        BlockPos firstMasterPos = selectedGate.pos();
        if (firstMasterPos.equals(clickedMasterPos)) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.pair_same_gate",
                    ChatFormatting.YELLOW
            );
            return InteractionResult.CONSUME;
        }

        BlockState firstState = level.getBlockState(firstMasterPos);
        if (!(firstState.getBlock() instanceof AirlockGateBlock firstGate)
                || !firstGate.isValidMaster(level, firstMasterPos)) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.gate_missing",
                    ChatFormatting.RED
            );
            return InteractionResult.CONSUME;
        }

        AirlockGateBlock.InterlockPairResult result =
                firstGate.pairInterlock(level, firstMasterPos, clickedMasterPos);

        switch (result) {
            case PAIRED -> {
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                            "message.domesurvival.airlock_binding_key.gates_paired",
                                            firstMasterPos.getX(),
                                            firstMasterPos.getY(),
                                            firstMasterPos.getZ(),
                                            clickedMasterPos.getX(),
                                            clickedMasterPos.getY(),
                                            clickedMasterPos.getZ()
                                    )
                                    .withStyle(ChatFormatting.GREEN),
                            true
                    );
                }
            }
            case NOT_CLOSED -> send(
                    player,
                    "message.domesurvival.airlock_binding_key.pair_requires_closed",
                    ChatFormatting.RED
            );
            case SAME_GATE -> send(
                    player,
                    "message.domesurvival.airlock_binding_key.pair_same_gate",
                    ChatFormatting.YELLOW
            );
            default -> send(
                    player,
                    "message.domesurvival.airlock_binding_key.gate_missing",
                    ChatFormatting.RED
            );
        }

        return InteractionResult.CONSUME;
    }

    private InteractionResult bindPanel(ServerLevel level,
                                        BlockPos panelPos,
                                        BlockState panelState,
                                        AirlockControlPanelBlock panelBlock,
                                        ItemStack stack,
                                        @Nullable Player player) {
        SelectedGate selectedGate = readSelection(stack);
        if (selectedGate == null) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.no_gate_selected",
                    ChatFormatting.YELLOW
            );
            return InteractionResult.CONSUME;
        }

        if (!level.dimension().location().equals(selectedGate.dimension())) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.wrong_dimension",
                    ChatFormatting.RED
            );
            return InteractionResult.CONSUME;
        }

        BlockPos gateMasterPos = selectedGate.pos();
        BlockState gateState = level.getBlockState(gateMasterPos);
        if (!(gateState.getBlock() instanceof AirlockGateBlock gate)
                || !gate.isValidMaster(level, gateMasterPos)) {
            send(
                    player,
                    "message.domesurvival.airlock_binding_key.gate_missing",
                    ChatFormatting.RED
            );
            return InteractionResult.CONSUME;
        }

        AirlockControlPanelBlockEntity panel =
                panelBlock.ensureBlockEntity(level, panelPos, panelState);
        panel.bind(level, gateMasterPos);

        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.domesurvival.airlock_binding_key.panel_bound",
                                    gateMasterPos.getX(),
                                    gateMasterPos.getY(),
                                    gateMasterPos.getZ()
                            )
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }

        return InteractionResult.CONSUME;
    }

    private static void send(@Nullable Player player,
                             String key,
                             ChatFormatting color) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(key).withStyle(color),
                    true
            );
        }
    }

    private static void clearSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }

        tag.remove(TAG_SELECTED_GATE);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    @Nullable
    private static SelectedGate readSelection(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_SELECTED_GATE, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag selected = root.getCompound(TAG_SELECTED_GATE);
        if (!selected.contains(TAG_DIMENSION) || !selected.contains(TAG_POS)) {
            return null;
        }

        ResourceLocation dimension =
                ResourceLocation.tryParse(selected.getString(TAG_DIMENSION));
        if (dimension == null) {
            return null;
        }

        return new SelectedGate(
                dimension,
                BlockPos.of(selected.getLong(TAG_POS))
        );
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable Level level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(
                Component.translatable(
                                "tooltip.domesurvival.airlock_binding_key.purpose"
                        )
                        .withStyle(ChatFormatting.AQUA)
        );
        tooltip.add(
                Component.translatable(
                                "tooltip.domesurvival.airlock_binding_key.form_hint"
                        )
                        .withStyle(ChatFormatting.BLUE)
        );

        SelectedGate selectedGate = readSelection(stack);
        if (selectedGate == null) {
            tooltip.add(
                    Component.translatable(
                                    "tooltip.domesurvival.airlock_binding_key.unselected"
                            )
                            .withStyle(ChatFormatting.GRAY)
            );
            tooltip.add(
                    Component.translatable(
                                    "tooltip.domesurvival.airlock_binding_key.unpair_hint"
                            )
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
        } else {
            BlockPos pos = selectedGate.pos();

            tooltip.add(
                    Component.translatable(
                                    "tooltip.domesurvival.airlock_binding_key.selected"
                            )
                            .withStyle(ChatFormatting.GREEN)
            );
            tooltip.add(
                    Component.translatable(
                                    "tooltip.domesurvival.airlock_binding_key.coordinates",
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()
                            )
                            .withStyle(ChatFormatting.GRAY)
            );
            tooltip.add(
                    Component.literal(selectedGate.dimension().toString())
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
            tooltip.add(
                    Component.translatable(
                                    "tooltip.domesurvival.airlock_binding_key.bind_hint"
                            )
                            .withStyle(ChatFormatting.YELLOW)
            );
            tooltip.add(
                    Component.translatable(
                                    "tooltip.domesurvival.airlock_binding_key.pair_hint"
                            )
                            .withStyle(ChatFormatting.GOLD)
            );
        }

        tooltip.add(
                Component.translatable(
                                "tooltip.domesurvival.airlock_binding_key.clear_hint"
                        )
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    private record SelectedGate(ResourceLocation dimension, BlockPos pos) {
    }
}
