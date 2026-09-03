package com.wasted.domesurvival.forge.machine.forming;

import com.wasted.domesurvival.forge.machine.side.PortVisual;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class FormingPressBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final EnumProperty<PortVisual> PORT_UP = EnumProperty.create("port_up", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_DOWN = EnumProperty.create("port_down", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_NORTH = EnumProperty.create("port_north", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_SOUTH = EnumProperty.create("port_south", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_WEST = EnumProperty.create("port_west", PortVisual.class);
    public static final EnumProperty<PortVisual> PORT_EAST = EnumProperty.create("port_east", PortVisual.class);

    public FormingPressBlock(Properties properties) {
        super(properties);
        registerDefaultState(withDefaultPorts(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(ACTIVE, false),
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

    private static BlockState withDefaultPorts(BlockState state, Direction facing) {
        BlockState configured = state;
        for (Direction direction : Direction.values()) {
            configured = configured.setValue(portProperty(direction), PortVisual.OFF);
        }
        configured = configured.setValue(PORT_UP, PortVisual.INPUT);
        configured = configured.setValue(portProperty(facing.getClockWise()), PortVisual.OUTPUT);
        configured = configured.setValue(portProperty(facing.getOpposite()), PortVisual.INPUT);
        return configured;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return withDefaultPorts(defaultBlockState().setValue(FACING, facing).setValue(ACTIVE, false), facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, PORT_UP, PORT_DOWN, PORT_NORTH, PORT_SOUTH, PORT_WEST, PORT_EAST);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FormingPressBlockEntity press) {
                NetworkHooks.openScreen(serverPlayer, press, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FormingPressBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                FormingPressRegistry.FORMING_PRESS_BLOCK_ENTITY.get(),
                FormingPressBlockEntity::serverTick
        );
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FormingPressBlockEntity press) {
                for (int slot = 0; slot < press.getInventory().getSlots(); slot++) {
                    ItemStack stack = press.getInventory().getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        popResource(level, pos, stack.copy());
                    }
                }
            }
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
