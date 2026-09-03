package com.wasted.domesurvival.forge.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compatibility guard for Lost Cities 1.20-7.5.x asset packs.
 *
 * Lost Cities throws when a palette references a block from an optional mod
 * that is not installed. LCMT 1.0.10 contains many such references.
 *
 * This mixin only intervenes when the referenced block id is genuinely absent
 * from the Forge block registry. Existing blocks are left completely untouched.
 */
@Pseudo
@Mixin(targets = "mcjty.lostcities.varia.Tools", remap = false)
public abstract class LostCitiesMissingBlockCompatMixin {

    @Unique
    private static final Logger DOMESURVIVAL$LOGGER = LogUtils.getLogger();

    @Unique
    private static final Set<ResourceLocation> DOMESURVIVAL$WARNED =
            ConcurrentHashMap.newKeySet();

    @Unique
    private static final Set<String> DOMESURVIVAL$WARNED_INVALID_STATES =
            ConcurrentHashMap.newKeySet();

    @Inject(
            method = "stringToState",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void domesurvival$replaceMissingPaletteBlock(
            String rawState,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (rawState == null || rawState.isBlank()) {
            return;
        }

        String idText = rawState;
        int stateStart = rawState.indexOf('[');
        if (stateStart >= 0) {
            idText = rawState.substring(0, stateStart);
        }

        ResourceLocation id = ResourceLocation.tryParse(idText.trim());
        if (id == null) {
            return;
        }

        if (ForgeRegistries.BLOCKS.containsKey(id)) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null && domesurvival$hasUnsupportedProperties(block, rawState)) {
                BlockState repairedState = domesurvival$copyCompatibleProperties(
                        block.defaultBlockState(),
                        rawState
                );

                if (DOMESURVIVAL$WARNED_INVALID_STATES.add(rawState)) {
                    DOMESURVIVAL$LOGGER.warn(
                            "[DomeSurvival LCMT Compat] Removed unsupported properties from Lost Cities palette state '{}'",
                            rawState
                    );
                }

                cir.setReturnValue(repairedState);
            }
            return;
        }

        Block fallback = domesurvival$selectFallback(id);
        BlockState fallbackState = domesurvival$copyCompatibleProperties(
                fallback.defaultBlockState(),
                rawState
        );

        if (DOMESURVIVAL$WARNED.add(id)) {
            ResourceLocation fallbackId = ForgeRegistries.BLOCKS.getKey(fallback);
            DOMESURVIVAL$LOGGER.warn(
                    "[DomeSurvival LCMT Compat] Missing Lost Cities palette block '{}' -> '{}'",
                    id,
                    fallbackId
            );
        }

        cir.setReturnValue(fallbackState);
    }

    /**
     * Picks a vanilla substitute with compatible geometry/state properties
     * where practical. Purely decorative objects fall back to air so that
     * absent furniture/props cannot obstruct generated structures.
     */
    @Unique
    private static Block domesurvival$selectFallback(ResourceLocation id) {
        String path = id.getPath().toLowerCase(Locale.ROOT);

        // Doors: preserve facing/half/hinge/open/powered.
        if (path.contains("glass_door")) {
            return Blocks.IRON_DOOR;
        }
        if (path.contains("door")) {
            return domesurvival$containsAny(path, "maple", "dark_oak")
                    ? Blocks.DARK_OAK_DOOR
                    : Blocks.OAK_DOOR;
        }

        // Beds: preserve facing/occupied/part.
        if (path.contains("bed")) {
            if (path.contains("red")) {
                return Blocks.RED_BED;
            }
            return Blocks.WHITE_BED;
        }

        // Fence gates before generic fences.
        if (path.contains("fence_gate")) {
            return Blocks.OAK_FENCE_GATE;
        }

        // Stairs and staircase-like blocks.
        if (domesurvival$containsAny(path, "stairs", "staircase", "gravelsteps")) {
            if (domesurvival$containsAny(path, "oak", "spruce", "birch", "maple", "sugi", "wood")) {
                return Blocks.OAK_STAIRS;
            }
            if (path.contains("marble")) {
                return Blocks.QUARTZ_STAIRS;
            }
            if (domesurvival$containsAny(path, "red_shingle", "red_brick")) {
                return Blocks.RED_NETHER_BRICK_STAIRS;
            }
            if (path.contains("moss")) {
                return Blocks.MOSSY_STONE_BRICK_STAIRS;
            }
            return Blocks.STONE_BRICK_STAIRS;
        }

        // Vertical slabs have no vanilla equivalent; use a full structural block.
        if (path.contains("vertical_slab")) {
            if (path.contains("diorite")) {
                return Blocks.POLISHED_DIORITE;
            }
            if (path.contains("red")) {
                return Blocks.RED_NETHER_BRICKS;
            }
            return Blocks.SMOOTH_STONE;
        }

        // Fences/hedges and thin metal geometry.
        if (domesurvival$containsAny(path, "iron_grid", "ironnetting", "iron_mesh",
                "ironmesh", "razor_wire", "wire", "mesh", "bars")) {
            return Blocks.IRON_BARS;
        }
        if (domesurvival$containsAny(path, "fence", "hedge")) {
            return Blocks.OAK_FENCE;
        }

        // Plants.
        if (path.contains("ivy")) {
            return Blocks.VINE;
        }
        if (path.contains("leaf_pile")) {
            return Blocks.MOSS_CARPET;
        }
        if (path.contains("leaves")) {
            return Blocks.OAK_LEAVES;
        }
        if (path.contains("pottedplant")) {
            return Blocks.POTTED_FERN;
        }

        // Transparent construction materials.
        if (domesurvival$containsAny(path, "glass", "window", "mirror")) {
            if (path.contains("red")) {
                return Blocks.RED_STAINED_GLASS;
            }
            if (path.contains("gray") || path.contains("weathered")) {
                return Blocks.GRAY_STAINED_GLASS;
            }
            if (path.contains("brown") || path.contains("dark_oak")) {
                return Blocks.BROWN_STAINED_GLASS;
            }
            return Blocks.GLASS;
        }

        // Masonry / structural materials.
        if (path.contains("basalt_cobblestone")) {
            return Blocks.COBBLESTONE;
        }
        if (path.contains("basalt")) {
            return Blocks.POLISHED_BASALT;
        }
        if (path.contains("marble")) {
            return Blocks.QUARTZ_BLOCK;
        }
        if (path.contains("gravel")) {
            return Blocks.GRAVEL;
        }
        if (path.contains("cobblestone_bricks")) {
            return Blocks.STONE_BRICKS;
        }
        if (path.contains("bricks")) {
            return path.contains("red") ? Blocks.RED_NETHER_BRICKS : Blocks.STONE_BRICKS;
        }
        if (path.contains("planks")) {
            return Blocks.DARK_OAK_PLANKS;
        }
        if (domesurvival$containsAny(path, "concrete", "barrier")) {
            return Blocks.GRAY_CONCRETE;
        }
        if (domesurvival$containsAny(path, "steel_beam", "ironplate", "squarebrick")) {
            return Blocks.IRON_BLOCK;
        }
        if (path.contains("mossy_stone")) {
            return Blocks.MOSSY_COBBLESTONE;
        }

        // Ducts, vents and utility metalwork.
        if (domesurvival$containsAny(path, "vent", "duct", "aircondition", "electricbox",
                "warningpost", "pole")) {
            return Blocks.IRON_BARS;
        }

        // Lighting. Prefer small vanilla lights where possible.
        if (path.contains("lightswitch")) {
            return Blocks.LEVER;
        }
        if (domesurvival$containsAny(path, "tablelamp")) {
            return Blocks.TORCH;
        }
        if (domesurvival$containsAny(path, "lamp", "light", "spotlight")) {
            return Blocks.SEA_LANTERN;
        }

        // Storage/containers that are normally full-block props.
        if (domesurvival$containsAny(path, "crate", "cabinet", "drawer", "storage",
                "locker", "shelves", "freezer", "toolbox", "weaponbox",
                "ammunitionbox", "medicalbox", "medical_box", "medicine_boxes")) {
            return Blocks.BARREL;
        }
        if (path.contains("barrel")) {
            return Blocks.BARREL;
        }

        // Technology/workstation blocks that occupy a full cube.
        if (domesurvival$containsAny(path, "controller", "disk_drive", "workbench",
                "electricity_generator")) {
            return Blocks.IRON_BLOCK;
        }

        // Organic Spore-style construction blocks.
        if (domesurvival$containsAny(path, "biomass", "mycelium", "fungal", "growth",
                "bloomfung", "organite", "acidic", "brain_remnants")) {
            return Blocks.MOSS_BLOCK;
        }

        // Simple solid props that are better represented by a material block.
        if (path.contains("sandbag")) {
            return Blocks.SAND;
        }
        if (path.contains("woodenpallet")) {
            return Blocks.OAK_PLANKS;
        }
        if (path.contains("manhole")) {
            return Blocks.IRON_TRAPDOOR;
        }
        if (path.equals("base")) {
            return Blocks.STONE;
        }

        // Vehicles, chairs, sofas, paper, curtains and other non-structural
        // decorative objects are intentionally omitted.
        return Blocks.AIR;
    }

    /**
     * Copies only state properties that also exist on the selected fallback.
     * This keeps doors, stairs, fences, bars, leaves, etc. oriented correctly,
     * while safely ignoring mod-specific properties.
     */
    @Unique
    private static BlockState domesurvival$copyCompatibleProperties(
            BlockState state,
            String rawState
    ) {
        int open = rawState.indexOf('[');
        int close = rawState.lastIndexOf(']');

        if (open < 0 || close <= open + 1) {
            return state;
        }

        String body = rawState.substring(open + 1, close);
        String[] assignments = body.split(",");

        for (String assignment : assignments) {
            int equals = assignment.indexOf('=');
            if (equals <= 0 || equals >= assignment.length() - 1) {
                continue;
            }

            String propertyName = assignment.substring(0, equals).trim();
            String propertyValue = assignment.substring(equals + 1).trim();

            Property<?> property = state.getBlock()
                    .getStateDefinition()
                    .getProperty(propertyName);

            if (property != null) {
                state = domesurvival$applyProperty(state, property, propertyValue);
            }
        }

        return state;
    }

    /**
     * Detects properties left over from optional blocks after an asset-pack
     * substitution (for example {@code weathering} on vanilla stone bricks).
     * Lost Cities otherwise forwards the whole string to the vanilla parser,
     * which aborts generation of the affected chunk.
     */
    @Unique
    private static boolean domesurvival$hasUnsupportedProperties(
            Block block,
            String rawState
    ) {
        int open = rawState.indexOf('[');
        int close = rawState.lastIndexOf(']');

        if (open < 0 || close <= open + 1) {
            return false;
        }

        String body = rawState.substring(open + 1, close);
        for (String assignment : body.split(",")) {
            int equals = assignment.indexOf('=');
            if (equals <= 0) {
                continue;
            }

            String propertyName = assignment.substring(0, equals).trim();
            if (block.getStateDefinition().getProperty(propertyName) == null) {
                return true;
            }
        }

        return false;
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState domesurvival$applyProperty(
            BlockState state,
            Property<?> property,
            String value
    ) {
        Property rawProperty = property;
        Optional parsed = rawProperty.getValue(value);

        if (parsed.isPresent()) {
            return state.setValue(rawProperty, (Comparable) parsed.get());
        }

        return state;
    }

    @Unique
    private static boolean domesurvival$containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
