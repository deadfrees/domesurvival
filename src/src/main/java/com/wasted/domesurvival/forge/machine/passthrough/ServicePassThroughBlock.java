package com.wasted.domesurvival.forge.machine.passthrough;

import cofh.lib.api.block.IWrenchable;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class ServicePassThroughBlock extends BaseEntityBlock implements IWrenchable {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");

    private static final ResourceLocation MACHINE_WRENCH_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "machine_wrench");

    public ServicePassThroughBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(OCCUPIED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(AXIS, context.getClickedFace().getAxis())
                .setValue(OCCUPIED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, OCCUPIED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }


    /**
     * The model intentionally contains a visual service opening, but the whole
     * block remains targetable. Without this full outline shape the player's
     * ray can pass through the rendered conduit and select the block behind it,
     * making the wrench appear broken.
     *
     * This only changes the selection/interaction outline. It does not close
     * the rendered tunnel and does not alter the pass-through network logic.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ServicePassThroughBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        ResourceLocation heldId = ForgeRegistries.ITEMS.getKey(held.getItem());

        // Direct fallback for the pack's Thermal WrenchItem. Normally Thermal
        // reaches IWrenchable through onItemUseFirst; this branch guarantees the
        // pass-through remains configurable even if another interaction hook
        // consumes/skips that callback in the assembled modpack.
        if (MACHINE_WRENCH_ID.equals(heldId)) {
            if (!level.isClientSide) {
                if (player.isSecondaryUseActive()) {
                    if (level.getBlockEntity(pos) instanceof ServicePassThroughBlockEntity pass) {
                        pass.removeInstalledConduit(player);
                    }
                } else {
                    cycleAxis(level, pos, state, player);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!(held.getItem() instanceof BlockItem)) {
            return InteractionResult.PASS;
        }

        ServiceConduitKind kind = ServiceConduitKind.detect(held);
        if (kind == ServiceConduitKind.EMPTY) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof ServicePassThroughBlockEntity pass) {
            if (!pass.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("Технический проход уже занят. Shift+ключ — извлечь коммуникацию."),
                        true
                );
                return InteractionResult.CONSUME;
            }

            Direction.Axis axis = hit.getDirection().getAxis();
            if (state.getValue(AXIS) != axis) {
                level.setBlock(pos, state.setValue(AXIS, axis), Block.UPDATE_ALL);
                pass.onAxisChanged();
            }

            if (pass.installConduit(held, kind)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                player.displayClientMessage(
                        Component.literal("Коммуникация установлена. Ось: " + axis.getName().toUpperCase()),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void wrenchBlock(Level level, BlockPos pos, BlockState state,
                            HitResult target, Player player) {
        if (!level.isClientSide) {
            cycleAxis(level, pos, state, player);
        }
    }

    private static void cycleAxis(Level level, BlockPos pos, BlockState state, Player player) {
        Direction.Axis current = state.getValue(AXIS);
        Direction.Axis next = switch (current) {
            case X -> Direction.Axis.Z;
            case Z -> Direction.Axis.Y;
            case Y -> Direction.Axis.X;
        };

        if (level.setBlock(pos, state.setValue(AXIS, next), Block.UPDATE_ALL)
                && level.getBlockEntity(pos) instanceof ServicePassThroughBlockEntity pass) {
            pass.onAxisChanged();
        }

        player.displayClientMessage(
                Component.literal("Ось технического прохода: " + next.getName().toUpperCase()),
                true
        );
    }

    @Override
    public boolean canWrench(Level level, BlockPos pos, BlockState state, Player player) {
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ServicePassThroughBlockEntity pass) {
            pass.dropInstalledConduit();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
