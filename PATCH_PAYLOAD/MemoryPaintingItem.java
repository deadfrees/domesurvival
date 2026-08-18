package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Dome Survival memory painting.
 *
 * <p>This item intentionally does NOT use minecraft:placeable, therefore a
 * normal vanilla PaintingItem cannot select Dome Survival memories. The item
 * resolves its own exact painting variants from the PAINTING_VARIANT registry,
 * then uses the vanilla Painting entity for wall/support behaviour.</p>
 */
public final class MemoryPaintingItem extends Item {
    private static final String[] MEMORY_VARIANT_IDS = {
            "01_trio_friends",
            "02_recording_in_yard",
            "03_airsoft_team",
            "04_fishing_closeup",
            "05_calm_lake_fishing",
            "06_relaxing_on_grass",
            "07_pink_hat_portrait",
            "08_watermelon_park",
            "09_white_hat_portrait",
            "10_flexing_portrait",
            "11_prize_shop_winners",
            "12_kitchen_character",
            "13_music_studio_friends",
            "14_mirror_group_selfie",
            "15_voxel_company_bright_light",
            "16_tricolor_portrait",
            "17_bee_hero_amber_hive",
            "18_wedding_kiss_tree",
            "19_night_selfie_friendship",
            "20_brown_suit_limo",
            "21_bw_party_point",
            "22_party_toast_indoor"
    };

    private static final int SEARCH_RADIUS_HORIZONTAL = 3;
    private static final int SEARCH_RADIUS_VERTICAL = 3;

    public MemoryPaintingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos basePos = context.getClickedPos().relative(face);

        if (player != null && !player.mayUseItemAt(basePos, face, stack)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Registry<PaintingVariant> registry =
                level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT);

        List<Holder<PaintingVariant>> variants = resolveMemoryVariants(registry);
        if (variants.isEmpty()) {
            DomeSurvival.LOGGER.error(
                    "Memory painting placement failed: no Dome Survival painting variants are loaded."
            );
            return InteractionResult.FAIL;
        }

        Collections.shuffle(variants);

        // First try the exact vanilla-like attachment position. If none of the
        // large custom artworks fit there, scan a small area on the same wall.
        // This keeps large 3x4 / 4x5 memories practical to hang without forcing
        // the player to discover the entity anchor pixel-perfectly.
        for (BlockPos anchor : candidateAnchors(basePos, face)) {
            for (Holder<PaintingVariant> variant : variants) {
                Painting painting = new Painting(level, anchor, face, variant);
                if (!painting.survives()) {
                    continue;
                }

                painting.playPlacementSound();
                level.gameEvent(player, GameEvent.ENTITY_PLACE, painting.position());
                level.addFreshEntity(painting);

                if (player == null || !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.FAIL;
    }

    private static List<Holder<PaintingVariant>> resolveMemoryVariants(
            Registry<PaintingVariant> registry
    ) {
        ArrayList<Holder<PaintingVariant>> result =
                new ArrayList<>(MEMORY_VARIANT_IDS.length);

        for (String path : MEMORY_VARIANT_IDS) {
            ResourceLocation id = new ResourceLocation(DomeSurvival.MOD_ID, path);
            ResourceKey<PaintingVariant> key =
                    ResourceKey.create(Registries.PAINTING_VARIANT, id);
            Optional<Holder.Reference<PaintingVariant>> holder = registry.getHolder(key);
            holder.ifPresent(result::add);
        }

        return result;
    }

    private static List<BlockPos> candidateAnchors(BlockPos base, Direction face) {
        ArrayList<BlockPos> positions = new ArrayList<>(
                (SEARCH_RADIUS_HORIZONTAL * 2 + 1)
                        * (SEARCH_RADIUS_VERTICAL * 2 + 1)
        );

        positions.add(base);

        for (int distance = 1;
             distance <= SEARCH_RADIUS_HORIZONTAL + SEARCH_RADIUS_VERTICAL;
             distance++) {

            for (int vertical = -SEARCH_RADIUS_VERTICAL;
                 vertical <= SEARCH_RADIUS_VERTICAL;
                 vertical++) {

                int horizontalAbs = distance - Math.abs(vertical);
                if (horizontalAbs < 0 || horizontalAbs > SEARCH_RADIUS_HORIZONTAL) {
                    continue;
                }

                if (horizontalAbs == 0) {
                    addWallOffset(positions, base, face, 0, vertical);
                } else {
                    addWallOffset(positions, base, face, horizontalAbs, vertical);
                    addWallOffset(positions, base, face, -horizontalAbs, vertical);
                }
            }
        }

        return positions;
    }

    private static void addWallOffset(
            List<BlockPos> output,
            BlockPos base,
            Direction face,
            int horizontal,
            int vertical
    ) {
        BlockPos shifted;

        if (face.getAxis() == Direction.Axis.X) {
            shifted = base.offset(0, vertical, horizontal);
        } else {
            shifted = base.offset(horizontal, vertical, 0);
        }

        if (!output.contains(shifted)) {
            output.add(shifted);
        }
    }
}
