package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.entity.MemoryPaintingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
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

public final class MemoryPaintingItem extends Item {
    private static final String[] LARGE_VARIANT_IDS = {
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

    private static final String[] COMPACT_VARIANT_IDS = {
            "compact_01_trio_friends",
            "compact_02_recording_in_yard",
            "compact_03_airsoft_team",
            "compact_04_fishing_closeup",
            "compact_05_calm_lake_fishing",
            "compact_06_relaxing_on_grass",
            "compact_07_pink_hat_portrait",
            "compact_08_watermelon_park",
            "compact_09_white_hat_portrait",
            "compact_10_flexing_portrait",
            "compact_11_prize_shop_winners",
            "compact_12_kitchen_character",
            "compact_13_music_studio_friends",
            "compact_14_mirror_group_selfie",
            "compact_15_voxel_company_bright_light",
            "compact_16_tricolor_portrait",
            "compact_17_bee_hero_amber_hive",
            "compact_18_wedding_kiss_tree",
            "compact_19_night_selfie_friendship",
            "compact_20_brown_suit_limo",
            "compact_21_bw_party_point",
            "compact_22_party_toast_indoor"
    };

    private static final int SEARCH_RADIUS_HORIZONTAL = 4;
    private static final int SEARCH_RADIUS_VERTICAL = 4;

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

        List<Holder<PaintingVariant>> large =
                resolveVariants(registry, LARGE_VARIANT_IDS);
        List<Holder<PaintingVariant>> compact =
                resolveVariants(registry, COMPACT_VARIANT_IDS);

        List<BlockPos> anchors = candidateAnchors(basePos, face);

        if (tryPlace(level, player, stack, face, anchors, large)) {
            return InteractionResult.CONSUME;
        }

        if (tryPlace(level, player, stack, face, anchors, compact)) {
            return InteractionResult.CONSUME;
        }

        System.err.println(
                "[DomeSurvival] Memory painting: registered variants exist, "
                        + "but none fit the selected wall."
        );
        return InteractionResult.FAIL;
    }

    private static boolean tryPlace(
            Level level,
            Player player,
            ItemStack stack,
            Direction face,
            List<BlockPos> anchors,
            List<Holder<PaintingVariant>> source
    ) {
        if (source.isEmpty()) {
            return false;
        }

        ArrayList<Holder<PaintingVariant>> variants = new ArrayList<>(source);
        Collections.shuffle(variants);

        for (BlockPos anchor : anchors) {
            for (Holder<PaintingVariant> variant : variants) {
                MemoryPaintingEntity painting =
                        new MemoryPaintingEntity(level, anchor, face, variant);

                if (!painting.survives()) {
                    continue;
                }

                painting.playPlacementSound();
                level.gameEvent(player, GameEvent.ENTITY_PLACE, painting.position());
                level.addFreshEntity(painting);

                if (player == null || !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return true;
            }
        }

        return false;
    }

    private static List<Holder<PaintingVariant>> resolveVariants(
            Registry<PaintingVariant> registry,
            String[] ids
    ) {
        ArrayList<Holder<PaintingVariant>> result = new ArrayList<>(ids.length);

        for (String path : ids) {
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
