package com.wasted.domesurvival.forge.entity;

import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * A normal vanilla Painting with one deliberate gameplay difference:
 * when broken (including when its supporting wall disappears), it drops
 * domesurvival:memory_painting instead of minecraft:painting.
 */
public final class MemoryPaintingEntity extends Painting {
    public MemoryPaintingEntity(EntityType<? extends Painting> type, Level level) {
        super(type, level);
    }

    public MemoryPaintingEntity(
            Level level,
            BlockPos pos,
            Direction direction,
            Holder<PaintingVariant> variant
    ) {
        this(ModEntityTypes.MEMORY_PAINTING.get(), level);
        this.pos = pos.immutable();
        this.setDirection(direction);
        this.setVariant(variant);
    }

    @Override
    public void dropItem(Entity breaker) {
        if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }

        this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);

        if (breaker instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        this.spawnAtLocation(new ItemStack(ModItems.MEMORY_PAINTING.get()));
    }
}
