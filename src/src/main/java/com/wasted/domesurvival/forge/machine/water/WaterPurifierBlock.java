package com.wasted.domesurvival.forge.machine.water;

import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** Industrial water purifier sharing the coal generator machine shell and side routing. */
public final class WaterPurifierBlock extends BaseEntityBlock implements cofh.lib.api.block.IDismantleable {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final EnumProperty<PortVisual> PORT_UP = EnumProperty.create("port_up", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_DOWN = EnumProperty.create("port_down", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_NORTH = EnumProperty.create("port_north", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_SOUTH = EnumProperty.create("port_south", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_WEST = EnumProperty.create("port_west", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_EAST = EnumProperty.create("port_east", PortVisual.class);

    public WaterPurifierBlock(Properties properties) {
        super(properties);
        registerDefaultState(withDefaultPorts(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false),
                Direction.NORTH
        ));
    }

    public static EnumProperty<PortVisual> portProperty(Direction direction) {
        return switch (direction) {
            case UP -> PORT_UP;
            case DOWN -> PORT_DOWN;
            case NORTH -> PORT_NORTH;
            case SOUTH -> PORT_SOUTH;
            case WEST -> PORT_WEST;
            case EAST -> PORT_EAST;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return withDefaultPorts(defaultBlockState().setValue(FACING, facing).setValue(LIT, false), facing);
    }

    private static BlockState withDefaultPorts(BlockState state, Direction facing) {
        BlockState configured = state;
        for (Direction direction : Direction.values()) {
            configured = configured.setValue(portProperty(direction), PortVisual.OFF);
        }

        configured = configured.setValue(PORT_UP, PortVisual.INPUT);
        configured = configured.setValue(portProperty(facing.getCounterClockWise()), PortVisual.INPUT);
        configured = configured.setValue(PORT_DOWN, PortVisual.OUTPUT);
        configured = configured.setValue(portProperty(facing.getClockWise()), PortVisual.OUTPUT);
        configured = configured.setValue(portProperty(facing.getOpposite()), PortVisual.OUTPUT);
        configured = configured.setValue(portProperty(facing), PortVisual.OFF);
        return configured;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, LIT, PORT_UP, PORT_DOWN, PORT_NORTH, PORT_SOUTH, PORT_WEST, PORT_EAST);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WaterPurifierBlockEntity purifier) {
                NetworkHooks.openScreen(serverPlayer, purifier, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterPurifierBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.WATER_PURIFIER.get(),
                WaterPurifierBlockEntity::serverTick
        );
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WaterPurifierBlockEntity purifier) {
                for (int slot = 0; slot < purifier.getInventory().getSlots(); slot++) {
                    ItemStack stack = purifier.getInventory().getStackInSlot(slot);
                    if (!stack.isEmpty()) popResource(level, pos, stack.copy());
                }
            }
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }

    /**
     * CoFH/Thermal dismantle clone.
     * Thermal's own WrenchItem performs the actual dismantle; this method only
     * tells the standard clone-stack path how to preserve this machine's
     * BlockEntity data in the returned BlockItem.
     */
    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.phys.HitResult target,
            net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.item.ItemStack stack = super.getCloneItemStack(level, pos, state);
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!stack.isEmpty() && blockEntity != null) {
            blockEntity.saveToItem(stack);
        }
        return stack;
    }
}
