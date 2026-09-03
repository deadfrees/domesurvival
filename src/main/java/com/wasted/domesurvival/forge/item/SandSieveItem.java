package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Early desert tool that turns a placed sand block into clay by interacting with the world.
 * The sand is deliberately consumed without its normal drop, so this cannot duplicate sand.
 */
public final class SandSieveItem extends Item {
    private static final float CLAY_DROP_CHANCE = 0.10F;
    private static final int USE_COOLDOWN_TICKS = 6;

    public SandSieveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState sand = level.getBlockState(pos);

        if (!sand.is(BlockTags.SAND)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null || player.isSpectator() || !level.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!level.removeBlock(pos, false)) {
            return InteractionResult.FAIL;
        }

        if (level.random.nextFloat() < CLAY_DROP_CHANCE) {
            ItemEntity clayDrop = new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.35D,
                    pos.getZ() + 0.5D,
                    new ItemStack(Items.CLAY_BALL)
            );
            clayDrop.setDefaultPickUpDelay();
            clayDrop.setDeltaMovement(
                    (level.random.nextDouble() - 0.5D) * 0.08D,
                    0.16D,
                    (level.random.nextDouble() - 0.5D) * 0.08D
            );
            level.addFreshEntity(clayDrop);
        }

        level.playSound(
                null,
                pos,
                SoundEvents.SAND_BREAK,
                SoundSource.BLOCKS,
                0.85F,
                0.9F + level.random.nextFloat() * 0.2F
        );

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, sand),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    22,
                    0.34D,
                    0.28D,
                    0.34D,
                    0.04D
            );
        }

        if (!player.getAbilities().instabuild) {
            context.getItemInHand().hurtAndBreak(
                    1,
                    player,
                    brokenPlayer -> brokenPlayer.broadcastBreakEvent(context.getHand())
            );
        }
        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.domesurvival.sand_sieve.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
