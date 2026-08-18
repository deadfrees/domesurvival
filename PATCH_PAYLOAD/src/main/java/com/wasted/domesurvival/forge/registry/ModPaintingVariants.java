package com.wasted.domesurvival.forge.registry;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Dome Survival memory painting variants for Forge 47.4.x / Minecraft 1.20.1.
 *
 * PaintingVariant dimensions are PIXELS in 1.20.1.
 * 16 px = 1 Minecraft block.
 *
 * V3.9.4 deliberately replaces this file as one authoritative unit instead
 * of attempting further PowerShell regex/string edits over partially patched
 * V3.9 / V3.9.1 / V3.9.2 / V3.9.3 source.
 */
public final class ModPaintingVariants {
    public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
            DeferredRegister.create(ForgeRegistries.Keys.PAINTING_VARIANTS, DomeSurvival.MOD_ID);

    public static final RegistryObject<PaintingVariant> P_01_TRIO_FRIENDS =
            PAINTING_VARIANTS.register("01_trio_friends", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> P_02_RECORDING_IN_YARD =
            PAINTING_VARIANTS.register("02_recording_in_yard", () -> new PaintingVariant(64, 80));
    public static final RegistryObject<PaintingVariant> P_03_AIRSOFT_TEAM =
            PAINTING_VARIANTS.register("03_airsoft_team", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> P_04_FISHING_CLOSEUP =
            PAINTING_VARIANTS.register("04_fishing_closeup", () -> new PaintingVariant(64, 80));
    public static final RegistryObject<PaintingVariant> P_05_CALM_LAKE_FISHING =
            PAINTING_VARIANTS.register("05_calm_lake_fishing", () -> new PaintingVariant(48, 64));
    public static final RegistryObject<PaintingVariant> P_06_RELAXING_ON_GRASS =
            PAINTING_VARIANTS.register("06_relaxing_on_grass", () -> new PaintingVariant(48, 64));
    public static final RegistryObject<PaintingVariant> P_07_PINK_HAT_PORTRAIT =
            PAINTING_VARIANTS.register("07_pink_hat_portrait", () -> new PaintingVariant(16, 16));
    public static final RegistryObject<PaintingVariant> P_08_WATERMELON_PARK =
            PAINTING_VARIANTS.register("08_watermelon_park", () -> new PaintingVariant(64, 80));
    public static final RegistryObject<PaintingVariant> P_09_WHITE_HAT_PORTRAIT =
            PAINTING_VARIANTS.register("09_white_hat_portrait", () -> new PaintingVariant(16, 16));
    public static final RegistryObject<PaintingVariant> P_10_FLEXING_PORTRAIT =
            PAINTING_VARIANTS.register("10_flexing_portrait", () -> new PaintingVariant(16, 16));
    public static final RegistryObject<PaintingVariant> P_11_PRIZE_SHOP_WINNERS =
            PAINTING_VARIANTS.register("11_prize_shop_winners", () -> new PaintingVariant(64, 80));
    public static final RegistryObject<PaintingVariant> P_12_KITCHEN_CHARACTER =
            PAINTING_VARIANTS.register("12_kitchen_character", () -> new PaintingVariant(16, 16));
    public static final RegistryObject<PaintingVariant> P_13_MUSIC_STUDIO_FRIENDS =
            PAINTING_VARIANTS.register("13_music_studio_friends", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> P_14_MIRROR_GROUP_SELFIE =
            PAINTING_VARIANTS.register("14_mirror_group_selfie", () -> new PaintingVariant(64, 80));
    public static final RegistryObject<PaintingVariant> P_15_VOXEL_COMPANY_BRIGHT_LIGHT =
            PAINTING_VARIANTS.register("15_voxel_company_bright_light", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> P_16_TRICOLOR_PORTRAIT =
            PAINTING_VARIANTS.register("16_tricolor_portrait", () -> new PaintingVariant(16, 16));
    public static final RegistryObject<PaintingVariant> P_17_BEE_HERO_AMBER_HIVE =
            PAINTING_VARIANTS.register("17_bee_hero_amber_hive", () -> new PaintingVariant(16, 16));
    public static final RegistryObject<PaintingVariant> P_18_WEDDING_KISS_TREE =
            PAINTING_VARIANTS.register("18_wedding_kiss_tree", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> P_19_NIGHT_SELFIE_FRIENDSHIP =
            PAINTING_VARIANTS.register("19_night_selfie_friendship", () -> new PaintingVariant(16, 16));
    public static final RegistryObject<PaintingVariant> P_20_BROWN_SUIT_LIMO =
            PAINTING_VARIANTS.register("20_brown_suit_limo", () -> new PaintingVariant(64, 80));
    public static final RegistryObject<PaintingVariant> P_21_BW_PARTY_POINT =
            PAINTING_VARIANTS.register("21_bw_party_point", () -> new PaintingVariant(64, 80));
    public static final RegistryObject<PaintingVariant> P_22_PARTY_TOAST_INDOOR =
            PAINTING_VARIANTS.register("22_party_toast_indoor", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_01_TRIO_FRIENDS =
            PAINTING_VARIANTS.register("compact_01_trio_friends", () -> new PaintingVariant(48, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_02_RECORDING_IN_YARD =
            PAINTING_VARIANTS.register("compact_02_recording_in_yard", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_03_AIRSOFT_TEAM =
            PAINTING_VARIANTS.register("compact_03_airsoft_team", () -> new PaintingVariant(48, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_04_FISHING_CLOSEUP =
            PAINTING_VARIANTS.register("compact_04_fishing_closeup", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_05_CALM_LAKE_FISHING =
            PAINTING_VARIANTS.register("compact_05_calm_lake_fishing", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_06_RELAXING_ON_GRASS =
            PAINTING_VARIANTS.register("compact_06_relaxing_on_grass", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_07_PINK_HAT_PORTRAIT =
            PAINTING_VARIANTS.register("compact_07_pink_hat_portrait", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_08_WATERMELON_PARK =
            PAINTING_VARIANTS.register("compact_08_watermelon_park", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_09_WHITE_HAT_PORTRAIT =
            PAINTING_VARIANTS.register("compact_09_white_hat_portrait", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_10_FLEXING_PORTRAIT =
            PAINTING_VARIANTS.register("compact_10_flexing_portrait", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_11_PRIZE_SHOP_WINNERS =
            PAINTING_VARIANTS.register("compact_11_prize_shop_winners", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_12_KITCHEN_CHARACTER =
            PAINTING_VARIANTS.register("compact_12_kitchen_character", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_13_MUSIC_STUDIO_FRIENDS =
            PAINTING_VARIANTS.register("compact_13_music_studio_friends", () -> new PaintingVariant(48, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_14_MIRROR_GROUP_SELFIE =
            PAINTING_VARIANTS.register("compact_14_mirror_group_selfie", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_15_VOXEL_COMPANY_BRIGHT_LIGHT =
            PAINTING_VARIANTS.register("compact_15_voxel_company_bright_light", () -> new PaintingVariant(48, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_16_TRICOLOR_PORTRAIT =
            PAINTING_VARIANTS.register("compact_16_tricolor_portrait", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_17_BEE_HERO_AMBER_HIVE =
            PAINTING_VARIANTS.register("compact_17_bee_hero_amber_hive", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_18_WEDDING_KISS_TREE =
            PAINTING_VARIANTS.register("compact_18_wedding_kiss_tree", () -> new PaintingVariant(48, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_19_NIGHT_SELFIE_FRIENDSHIP =
            PAINTING_VARIANTS.register("compact_19_night_selfie_friendship", () -> new PaintingVariant(32, 32));
    public static final RegistryObject<PaintingVariant> COMPACT_20_BROWN_SUIT_LIMO =
            PAINTING_VARIANTS.register("compact_20_brown_suit_limo", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_21_BW_PARTY_POINT =
            PAINTING_VARIANTS.register("compact_21_bw_party_point", () -> new PaintingVariant(32, 48));
    public static final RegistryObject<PaintingVariant> COMPACT_22_PARTY_TOAST_INDOOR =
            PAINTING_VARIANTS.register("compact_22_party_toast_indoor", () -> new PaintingVariant(48, 32));

    private ModPaintingVariants() {
    }
}
