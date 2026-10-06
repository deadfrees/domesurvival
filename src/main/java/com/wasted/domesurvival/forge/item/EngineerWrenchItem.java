package com.wasted.domesurvival.forge.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Native engineer tool. Thermal/CoFH are optional compatibility targets only. */
public final class EngineerWrenchItem extends Item {
    private static final Set<String> PORTABLE = Set.of("forming_press", "coal_generator", "copper_furnace", "coke_oven", "shaft_furnace",
            "water_purifier", "oxygen_electrolyzer", "oxygen_filler", "filter_regeneration_station", "organic_processor", "bioincubator", "energy_buffer",
            "energy_buffer_titan", "energy_buffer_adamantium", "energy_buffer_creative", "universal_tank");
    // These clone paths omit contents which their own onRemove must release/account for.
    private static final Set<String> KEEP_ENTITY_ON_REMOVAL = Set.of("universal_tank", "energy_buffer",
            "energy_buffer_titan", "energy_buffer_adamantium", "energy_buffer_creative");
    public EngineerWrenchItem(Properties properties) { super(properties); }
    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copyWithCount(1); }
    @Override public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action.name().equals("wrench") || action.name().equals("wrench_dismantle");
    }
    @Override public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) { return true; }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player=context.getPlayer();Level level=context.getLevel();BlockPos pos=context.getClickedPos();
        if(player==null||!player.mayBuild()||!player.mayUseItemAt(pos,context.getClickedFace(),stack)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        BlockState state=level.getBlockState(pos);Block block=state.getBlock();
        ResourceLocation id=ForgeRegistries.BLOCKS.getKey(block);
        boolean dome=id!=null&&id.getNamespace().equals("domesurvival");
        BlockHitResult hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),pos,context.isInside());
        if(player.isShiftKeyDown()) {
            if(dome&&PORTABLE.contains(id.getPath())) {
                if(level.isClientSide)return InteractionResult.SUCCESS;
                if(!(player instanceof ServerPlayer serverPlayer)||ForgeHooks.onBlockBreakEvent(level,serverPlayer.gameMode.getGameModeForPlayer(),serverPlayer,pos)==-1)return InteractionResult.FAIL;
                if (!level.getBlockState(pos).equals(state)) return InteractionResult.FAIL;
                ItemStack portable=block.getCloneItemStack(state,hit,level,pos,player);
                if(portable.isEmpty())return InteractionResult.FAIL;
                // Buffers drop the excluded ChargeSlot separately; the reservoir updates
                // shared contents. Other machines transfer their whole inventory to NBT.
                var entity = level.getBlockEntity(pos);
                if(!KEEP_ENTITY_ON_REMOVAL.contains(id.getPath()))level.removeBlockEntity(pos);
                if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),11)) {
                    if(entity!=null){entity.clearRemoved();level.setBlockEntity(entity);}
                    return InteractionResult.FAIL;
                }
                Block.popResource(level,pos,portable);
                return InteractionResult.CONSUME;
            }
            return optionalDismantle(block,level,pos,state,hit,player);
        }
        // Own pipe blocks already implement their connector configuration in Block.use.
        // Let the normal block handler run; Shift pipe cutting is handled by existing events.
        if(block instanceof com.wasted.domesurvival.forge.itempipe.ItemPipeBlock
                ||block instanceof com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlock
                ||block instanceof com.wasted.domesurvival.forge.transport.fluid.FluidPipeBlock
                ||block instanceof com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeBlock)return InteractionResult.PASS;
        InteractionResult compatibility=optionalWrench(block,level,pos,state,hit,player);
        if(compatibility!=InteractionResult.PASS)return compatibility;
        BlockState rotated=rotation(state,level,pos);
        if(rotated.equals(state))return InteractionResult.PASS;
        if(!level.isClientSide) {
            if(!level.setBlock(pos,rotated,3))return InteractionResult.FAIL;
            level.updateNeighborsAt(pos,block);
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.forming.FormingPressBlockEntity press)
                press.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.copper.CopperFurnaceBlockEntity furnace)
                furnace.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.shaft.CokeOvenBlockEntity oven)
                oven.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceBlockEntity shaft)
                shaft.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.water.WaterPurifierBlockEntity purifier)
                purifier.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.oxygen.OxygenElectrolyzerBlockEntity electrolyzer)
                electrolyzer.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerBlockEntity filler)
                filler.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.filter.FilterRegenerationBlockEntity regenerator)
                regenerator.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.bio.BioincubatorBlockEntity incubator)
                incubator.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockEntity buffer)
                buffer.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferBlockEntity buffer)
                buffer.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlockEntity buffer)
                buffer.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.organic.OrganicProcessorBlockEntity processor)
                processor.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) { return InteractionResult.PASS; }

    private static BlockState rotation(BlockState state, Level level, BlockPos pos) {
        // A generic rotation must never separate a multiblock or move an extended piston.
        if (state.hasProperty(BlockStateProperties.DOOR_HINGE)
                || state.hasProperty(BlockStateProperties.BED_PART)
                || state.hasProperty(BlockStateProperties.EYE)
                || state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE
                || state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED)) return state;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction original = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            for (Direction direction = original.getClockWise(); direction != original; direction = direction.getClockWise()) {
                BlockState candidate = state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                if (candidate.canSurvive(level, pos)) return candidate;
            }
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            return cycleSupported(state, level, pos, BlockStateProperties.FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
            return cycleSupported(state, level, pos, BlockStateProperties.FACING_HOPPER);
        } else if (state.hasProperty(BlockStateProperties.AXIS)) {
            return cycleSupported(state, level, pos, BlockStateProperties.AXIS);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState cycleSupported(BlockState state, Level level, BlockPos pos, Property<T> property) {
        BlockState candidate = state.cycle(property);
        while (!candidate.equals(state)) {
            if (candidate.canSurvive(level, pos)) return candidate;
            candidate = candidate.cycle(property);
        }
        return state;
    }

    // Reflection keeps both the item and native machine path loadable without CoFH.
    // Existing external implementations keep their own permissions and dismantling rules.
    private static Class<?> optionalContract(Object target,String name) {
        try { Class<?> type=Class.forName(name,false,target.getClass().getClassLoader());return type.isInstance(target)?type:null; }
        catch(ClassNotFoundException ignored){return null;}
    }
    private static InteractionResult optionalDismantle(Block block,Level level,BlockPos pos,BlockState state,HitResult hit,Player player) {
        Class<?> api=optionalContract(block,"cofh.lib.api.block.IDismantleable");if(api==null)return InteractionResult.PASS;
        try {
            if(!(boolean)api.getMethod("canDismantle",Level.class,BlockPos.class,BlockState.class,Player.class).invoke(block,level,pos,state,player))return InteractionResult.PASS;
            if(!level.isClientSide){
                if(player instanceof ServerPlayer sp&&ForgeHooks.onBlockBreakEvent(level,sp.gameMode.getGameModeForPlayer(),sp,pos)==-1)return InteractionResult.FAIL;
                api.getMethod("dismantleBlock",Level.class,BlockPos.class,BlockState.class,HitResult.class,Player.class,boolean.class).invoke(block,level,pos,state,hit,player,false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }catch(ReflectiveOperationException ex){com.mojang.logging.LogUtils.getLogger().warn("Optional wrench dismantle failed",ex);return InteractionResult.FAIL;}
    }
    private static InteractionResult optionalWrench(Block block,Level level,BlockPos pos,BlockState state,HitResult hit,Player player) {
        Class<?> api=optionalContract(block,"cofh.lib.api.block.IWrenchable");if(api==null)return InteractionResult.PASS;
        try {
            if(!(boolean)api.getMethod("canWrench",Level.class,BlockPos.class,BlockState.class,Player.class).invoke(block,level,pos,state,player))return InteractionResult.PASS;
            if(!level.isClientSide)api.getMethod("wrenchBlock",Level.class,BlockPos.class,BlockState.class,HitResult.class,Player.class).invoke(block,level,pos,state,hit,player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }catch(ReflectiveOperationException ex){com.mojang.logging.LogUtils.getLogger().warn("Optional wrench interaction failed",ex);return InteractionResult.FAIL;}
    }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flags) {
        tooltip.add(Component.translatable("item.domesurvival.machine_wrench.controls"));
        tooltip.add(Component.translatable("item.domesurvival.machine_wrench.portable"));
    }
}
