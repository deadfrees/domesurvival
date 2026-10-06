package com.wasted.domesurvival.forge.block;

import com.wasted.domesurvival.forge.machine.copper.CopperFurnaceBlockEntity;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Early-game copper furnace with IC2 Iron Furnace-style speed and fuel economy. */
public final class CopperFurnaceBlock extends AbstractFurnaceBlock implements cofh.lib.api.block.IDismantleable {
    private static final java.util.EnumMap<net.minecraft.core.Direction, net.minecraft.world.level.block.state.properties.EnumProperty<com.wasted.domesurvival.forge.machine.side.PortVisual>> PORTS = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
    static { for (var side : net.minecraft.core.Direction.values()) PORTS.put(side,
            net.minecraft.world.level.block.state.properties.EnumProperty.create("port_" + side.getName(), com.wasted.domesurvival.forge.machine.side.PortVisual.class)); }
    public static net.minecraft.world.level.block.state.properties.EnumProperty<com.wasted.domesurvival.forge.machine.side.PortVisual> portProperty(net.minecraft.core.Direction side) { return PORTS.get(side); }
    public CopperFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        for (var property : PORTS.values()) builder.add(property);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperFurnaceBlockEntity(pos, state);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopperFurnaceBlockEntity furnace) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, furnace, pos);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CopperFurnaceBlockEntity furnace && !furnace.isDismantling()) {
            for (int i=0;i<furnace.getModules().getSlots();i++) {
                var module=furnace.getModules().getStackInSlot(i);
                if (!module.isEmpty()) popResource(level,pos,module.copy());
            }
        }
        super.onRemove(state,level,pos,next,moved);
    }

    @Override public void dismantleBlock(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.phys.HitResult hit, Player player, boolean returnToPlayer) {
        // Transfer vanilla contents/XP and upgrades together; suppress onRemove drops.
        var portable=getCloneItemStack(state,hit,level,pos,player);
        if (portable.isEmpty() || level.isClientSide) return;
        var entity=level.getBlockEntity(pos);
        level.removeBlockEntity(pos);
        if (level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),11)) popResource(level,pos,portable);
        else if(entity!=null){entity.clearRemoved();level.setBlockEntity(entity);}
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                type,
                ModBlockEntities.COPPER_FURNACE.get(),
                CopperFurnaceBlockEntity::serverTick
        );
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
