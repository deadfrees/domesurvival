package com.wasted.domesurvival.forge.client.jei;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.item.BioModuleItem;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.item.SieveMeshItem;
import com.wasted.domesurvival.forge.machine.bio.BioincubatorBlockEntity;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenElectrolyzerBlockEntity;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerBlockEntity;
import com.wasted.domesurvival.forge.machine.shaft.CokeOvenBlockEntity;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceBlockEntity;
import com.wasted.domesurvival.forge.machine.water.WaterPurifierBlockEntity;
import com.wasted.domesurvival.forge.machine.sieve.SandSieveBlockEntity;
import com.wasted.domesurvival.forge.machine.sieve.SieveDropTable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class DomeSurvivalJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = id("jei_plugin");

    public static final RecipeType<DomeMachineRecipe> COKE_OVEN = type("coke_oven");
    public static final RecipeType<DomeMachineRecipe> SHAFT_FURNACE = type("shaft_furnace");
    public static final RecipeType<DomeMachineRecipe> WATER_PURIFIER = type("water_purifier");
    public static final RecipeType<DomeMachineRecipe> OXYGEN_ELECTROLYZER = type("oxygen_electrolyzer");
    public static final RecipeType<DomeMachineRecipe> OXYGEN_FILLER = type("oxygen_filler");
    public static final RecipeType<DomeMachineRecipe> BIO_REPAIR = type("bio_repair");
    public static final RecipeType<DomeMachineRecipe> BIO_INCUBATION = type("bio_incubation");
    public static final RecipeType<DomeMachineRecipe> SAND_SIEVE = type("sand_sieve");

    private static volatile List<SpeciesDefinition> bundledSpecies;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(
                ModItems.BIO_MODULE.get(),
                ModItems.SMALL_OXYGEN_TANK.get(),
                ModItems.MEDIUM_OXYGEN_TANK.get(),
                ModItems.LARGE_OXYGEN_TANK.get()
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new DomeMachineRecipeCategory(helper, COKE_OVEN,
                        Component.translatable("jei.domesurvival.coke_oven"),
                        new ItemStack(ModBlocks.COKE_OVEN.get())),
                new DomeMachineRecipeCategory(helper, SHAFT_FURNACE,
                        Component.translatable("jei.domesurvival.shaft_furnace"),
                        new ItemStack(ModBlocks.SHAFT_FURNACE.get())),
                new DomeMachineRecipeCategory(helper, WATER_PURIFIER,
                        Component.translatable("jei.domesurvival.water_purifier"),
                        new ItemStack(ModBlocks.WATER_PURIFIER.get())),
                new DomeMachineRecipeCategory(helper, OXYGEN_ELECTROLYZER,
                        Component.translatable("jei.domesurvival.oxygen_electrolyzer"),
                        new ItemStack(ModBlocks.OXYGEN_ELECTROLYZER.get())),
                new DomeMachineRecipeCategory(helper, OXYGEN_FILLER,
                        Component.translatable("jei.domesurvival.oxygen_filler"),
                        new ItemStack(ModBlocks.OXYGEN_FILLER.get())),
                new DomeMachineRecipeCategory(helper, BIO_REPAIR,
                        Component.translatable("jei.domesurvival.bio_repair"),
                        new ItemStack(ModBlocks.BIOINCUBATOR.get())),
                new DomeMachineRecipeCategory(helper, BIO_INCUBATION,
                        Component.translatable("jei.domesurvival.bio_incubation"),
                        new ItemStack(ModBlocks.BIOINCUBATOR.get())),
                new DomeMachineRecipeCategory(helper, SAND_SIEVE,
                        Component.translatable("jei.domesurvival.sand_sieve"),
                        new ItemStack(ModBlocks.SAND_SIEVE.get()))
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(COKE_OVEN, List.of(cokeRecipe()));
        registration.addRecipes(SHAFT_FURNACE, List.of(shaftRecipe()));
        registration.addRecipes(WATER_PURIFIER, waterPurifierRecipes());
        registration.addRecipes(OXYGEN_ELECTROLYZER, List.of(electrolysisRecipe()));
        registration.addRecipes(OXYGEN_FILLER, oxygenFillingRecipes());
        registration.addRecipes(BIO_REPAIR, biologicalRepairRecipes());
        registration.addRecipes(BIO_INCUBATION, biologicalIncubationRecipes());
        registration.addRecipes(SAND_SIEVE, sandSieveRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.COKE_OVEN.get(), COKE_OVEN);
        registration.addRecipeCatalyst(ModBlocks.SHAFT_FURNACE.get(), SHAFT_FURNACE);
        registration.addRecipeCatalyst(ModBlocks.WATER_PURIFIER.get(), WATER_PURIFIER);
        registration.addRecipeCatalyst(ModBlocks.OXYGEN_ELECTROLYZER.get(), OXYGEN_ELECTROLYZER);
        registration.addRecipeCatalyst(ModBlocks.OXYGEN_FILLER.get(), OXYGEN_FILLER);
        registration.addRecipeCatalyst(ModBlocks.BIOINCUBATOR.get(), BIO_REPAIR, BIO_INCUBATION);
        registration.addRecipeCatalyst(ModBlocks.SAND_SIEVE.get(), SAND_SIEVE);
    }

    private static DomeMachineRecipe cokeRecipe() {
        return recipe("coke_oven/coal", DomeMachineRecipe.Layout.COKE_OVEN,
                List.of(List.of(new ItemStack(Items.COAL)),
                        List.of(new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL), new ItemStack(Items.OAK_PLANKS))),
                List.of(), List.of(List.of(new ItemStack(ModItems.COAL_COKE.get()))), List.of(),
                Component.translatable("jei.domesurvival.note.any_furnace_fuel"),
                CokeOvenBlockEntity.PROCESS_TIME, 0);
    }

    private static DomeMachineRecipe shaftRecipe() {
        List<ItemStack> iron = List.of(Ingredient.of(ItemTags.create(
                ResourceLocation.fromNamespaceAndPath("forge", "ingots/iron")
        )).getItems());
        if (iron.isEmpty()) iron = List.of(new ItemStack(Items.IRON_INGOT));
        return recipe("shaft_furnace/steel", DomeMachineRecipe.Layout.SHAFT_FURNACE,
                List.of(iron, List.of(new ItemStack(ModItems.COAL_COKE.get()))), List.of(),
                List.of(List.of(new ItemStack(ModItems.STEEL_INGOT.get())),
                        List.of(new ItemStack(ModItems.SLAG.get()))), List.of(),
                Component.translatable("jei.domesurvival.note.coke_is_heat"),
                ShaftFurnaceBlockEntity.PROCESS_TIME, 0);
    }

    private static List<DomeMachineRecipe> waterPurifierRecipes() {
        return List.of(
                purifierRecipe("basic", ModItems.WATER_FILTER_CARTRIDGE.get(),
                        ModItems.BASIC_FILTER_PROCESS_TICKS, ModItems.BASIC_FILTER_ENERGY_PER_TICK),
                purifierRecipe("improved", ModItems.IMPROVED_WATER_FILTER.get(),
                        ModItems.IMPROVED_FILTER_PROCESS_TICKS, ModItems.IMPROVED_FILTER_ENERGY_PER_TICK)
        );
    }

    private static DomeMachineRecipe purifierRecipe(String name, Item filter, int ticks, int energy) {
        return recipe("water_purifier/" + name, DomeMachineRecipe.Layout.WATER_PURIFIER,
                List.of(List.of(new ItemStack(filter))),
                List.of(new FluidStack(Fluids.WATER, WaterPurifierBlockEntity.RAW_WATER_PER_CYCLE)),
                List.of(),
                List.of(new FluidStack(ModFluids.PURIFIED_WATER.get(), WaterPurifierBlockEntity.PURIFIED_WATER_PER_CYCLE)),
                Component.translatable("jei.domesurvival.note.filter_damaged"), ticks, energy);
    }

    private static DomeMachineRecipe electrolysisRecipe() {
        return recipe("oxygen_electrolyzer/oxygen", DomeMachineRecipe.Layout.OXYGEN_ELECTROLYZER,
                List.of(),
                List.of(new FluidStack(ModFluids.PURIFIED_WATER.get(), OxygenElectrolyzerBlockEntity.WATER_PER_CYCLE)),
                List.of(), List.of(),
                Component.translatable("jei.domesurvival.note.oxygen_output",
                        OxygenElectrolyzerBlockEntity.OXYGEN_PER_CYCLE),
                OxygenElectrolyzerBlockEntity.PROCESS_TICKS, OxygenElectrolyzerBlockEntity.ENERGY_PER_TICK);
    }

    private static List<DomeMachineRecipe> oxygenFillingRecipes() {
        return List.of(
                oxygenFillingRecipe("small", ModItems.SMALL_OXYGEN_TANK.get()),
                oxygenFillingRecipe("medium", ModItems.MEDIUM_OXYGEN_TANK.get()),
                oxygenFillingRecipe("large", ModItems.LARGE_OXYGEN_TANK.get())
        );
    }

    private static DomeMachineRecipe oxygenFillingRecipe(String name, Item item) {
        ItemStack empty = new ItemStack(item);
        ItemStack full = new ItemStack(item);
        OxygenTankItem tank = (OxygenTankItem) item;
        tank.setOxygen(empty, 0);
        tank.setOxygen(full, tank.capacity());
        return recipe("oxygen_filler/" + name, DomeMachineRecipe.Layout.OXYGEN_FILLER,
                List.of(List.of(empty)), List.of(), List.of(List.of(full)), List.of(),
                Component.translatable("jei.domesurvival.note.oxygen_required", tank.capacity()),
                tank.capacity() / OxygenFillerBlockEntity.OXYGEN_FILL_PER_TICK,
                OxygenFillerBlockEntity.ENERGY_PER_FILL_TICK);
    }

    private static List<DomeMachineRecipe> biologicalRepairRecipes() {
        ArrayList<DomeMachineRecipe> recipes = new ArrayList<>();
        for (SpeciesDefinition species : species()) {
            ArrayList<ItemStack> damagedInputs = new ArrayList<>();
            damagedInputs.add(BioModuleItem.create(species.entityId(), true));
            ItemStack legacy = legacyCapsule(species.entityId(), true);
            if (!legacy.isEmpty()) damagedInputs.add(legacy);

            recipes.add(recipe("bio_repair/" + pathId(species.entityId()), DomeMachineRecipe.Layout.BIO_REPAIR,
                    List.of(List.copyOf(damagedInputs),
                            List.of(new ItemStack(ModItems.BIO_REPAIR_KIT.get())),
                            List.of(new ItemStack(ModItems.BIOGEL.get())),
                            List.of(new ItemStack(ModItems.NUTRIENT_MIX.get()))),
                    List.of(new FluidStack(ModFluids.PURIFIED_WATER.get(), BioincubatorBlockEntity.REPAIR_WATER_MB)),
                    List.of(List.of(BioModuleItem.create(species.entityId(), false))), List.of(),
                    Component.translatable("jei.domesurvival.note.repaired_module"),
                    BioincubatorBlockEntity.REPAIR_PROCESS_TICKS,
                    BioincubatorBlockEntity.REPAIR_ENERGY_PER_TICK));
        }
        return List.copyOf(recipes);
    }

    private static List<DomeMachineRecipe> biologicalIncubationRecipes() {
        ArrayList<DomeMachineRecipe> recipes = new ArrayList<>();
        for (SpeciesDefinition species : species()) {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(species.entityId());
            Item feed = ForgeRegistries.ITEMS.getValue(species.feedItem());
            if (entityType == null || feed == null) continue;

            ArrayList<ItemStack> capsules = new ArrayList<>();
            capsules.add(BioModuleItem.create(species.entityId(), false));
            ItemStack legacy = legacyCapsule(species.entityId(), false);
            if (!legacy.isEmpty()) capsules.add(legacy);

            ArrayList<List<ItemStack>> outputs = new ArrayList<>();
            SpawnEggItem egg = SpawnEggItem.byId(entityType);
            if (egg != null) outputs.add(List.of(new ItemStack(egg)));

            recipes.add(recipe("bio_incubation/" + pathId(species.entityId()),
                    DomeMachineRecipe.Layout.BIO_INCUBATION,
                    List.of(List.copyOf(capsules),
                            List.of(new ItemStack(feed, species.feedCount()))),
                    List.of(new FluidStack(ModFluids.PURIFIED_WATER.get(), species.waterMb())),
                    List.copyOf(outputs), List.of(),
                    Component.translatable("jei.domesurvival.note.creates_baby", entityType.getDescription()),
                    species.processTicks(), species.energyPerTick()));
        }
        return List.copyOf(recipes);
    }

    private static List<DomeMachineRecipe> sandSieveRecipes() {
        return List.of(
                drySieveRecipe("fiber", ModItems.FIBER_SIEVE_MESH.get(), SieveMeshItem.Tier.FIBER),
                drySieveRecipe("copper", ModItems.COPPER_SIEVE_MESH.get(), SieveMeshItem.Tier.COPPER),
                drySieveRecipe("steel", ModItems.STEEL_SIEVE_MESH.get(), SieveMeshItem.Tier.STEEL),
                wetSieveRecipe("fiber", ModItems.FIBER_SIEVE_MESH.get(), SieveMeshItem.Tier.FIBER),
                wetSieveRecipe("copper", ModItems.COPPER_SIEVE_MESH.get(), SieveMeshItem.Tier.COPPER),
                wetSieveRecipe("steel", ModItems.STEEL_SIEVE_MESH.get(), SieveMeshItem.Tier.STEEL)
        );
    }

    private static DomeMachineRecipe drySieveRecipe(String name, Item mesh, SieveMeshItem.Tier tier) {
        SieveDropTable.Chances chances = SieveDropTable.dry(tier);
        return recipe("sand_sieve/dry_" + name, DomeMachineRecipe.Layout.SAND_SIEVE,
                List.of(List.of(new ItemStack(Items.SAND), new ItemStack(Items.RED_SAND)),
                        List.of(new ItemStack(mesh))),
                List.of(), List.of(possibleResults(chances, true)), List.of(),
                Component.translatable("jei.domesurvival.note.dry_sieve", chances.clayPercent()),
                sieveDetails(false, tier, chances), SandSieveBlockEntity.DRY_PROCESS_TICKS, 0);
    }

    private static DomeMachineRecipe wetSieveRecipe(String name, Item mesh, SieveMeshItem.Tier tier) {
        SieveDropTable.Chances chances = SieveDropTable.wet(tier);
        return recipe("sand_sieve/wet_" + name, DomeMachineRecipe.Layout.SAND_SIEVE,
                List.of(List.of(new ItemStack(Items.SAND, SandSieveBlockEntity.WET_SAND_PER_CYCLE),
                                new ItemStack(Items.RED_SAND, SandSieveBlockEntity.WET_SAND_PER_CYCLE)),
                        List.of(new ItemStack(mesh))),
                List.of(new FluidStack(Fluids.WATER,
                        SandSieveBlockEntity.WET_WATER_PER_CYCLE)),
                List.of(List.of(new ItemStack(Items.CLAY_BALL)), possibleResults(chances, false)), List.of(),
                Component.translatable("jei.domesurvival.note.wet_sieve", chances.clayPercent()),
                sieveDetails(true, tier, chances), SandSieveBlockEntity.WET_PROCESS_TICKS, 0);
    }

    private static List<ItemStack> possibleResults(SieveDropTable.Chances chances, boolean includeClay) {
        ArrayList<ItemStack> results = new ArrayList<>();
        if (includeClay && chances.clayPercent() > 0) results.add(new ItemStack(Items.CLAY_BALL));
        if (chances.flintPercent() > 0) results.add(new ItemStack(Items.FLINT));
        if (chances.boneMealPercent() > 0) results.add(new ItemStack(Items.BONE_MEAL));
        if (chances.rawCopperPercent() > 0) results.add(new ItemStack(Items.RAW_COPPER));
        if (chances.ironNuggetPercent() > 0) results.add(new ItemStack(Items.IRON_NUGGET));
        if (chances.goldNuggetPercent() > 0) results.add(new ItemStack(Items.GOLD_NUGGET));
        if (chances.redstonePercent() > 0) results.add(new ItemStack(Items.REDSTONE));
        return List.copyOf(results);
    }

    private static List<Component> sieveDetails(boolean wet, SieveMeshItem.Tier tier,
                                                SieveDropTable.Chances chances) {
        ArrayList<Component> details = new ArrayList<>();
        if (wet) {
            details.add(Component.translatable("jei.domesurvival.sieve_details.wet_primary",
                    chances.clayPercent()));
            if (tier == SieveMeshItem.Tier.FIBER) {
                details.add(Component.translatable("jei.domesurvival.sieve_details.wet_fiber",
                        chances.boneMealPercent()));
            } else {
                details.add(Component.translatable("jei.domesurvival.sieve_details.wet_metals",
                        chances.boneMealPercent(), chances.rawCopperPercent(), chances.ironNuggetPercent()));
            }
        } else {
            details.add(Component.translatable("jei.domesurvival.sieve_details.dry_primary",
                    chances.clayPercent(), chances.flintPercent(), chances.boneMealPercent()));
            if (tier != SieveMeshItem.Tier.FIBER) {
                details.add(Component.translatable("jei.domesurvival.sieve_details.dry_metals",
                        chances.rawCopperPercent(), chances.ironNuggetPercent()));
            }
        }
        if (tier == SieveMeshItem.Tier.STEEL) {
            details.add(Component.translatable("jei.domesurvival.sieve_details.rare",
                    chances.goldNuggetPercent(), chances.redstonePercent()));
        }
        return List.copyOf(details);
    }

    private static DomeMachineRecipe recipe(String path, DomeMachineRecipe.Layout layout,
                                            List<List<ItemStack>> itemInputs, List<FluidStack> fluidInputs,
                                            List<List<ItemStack>> itemOutputs, List<FluidStack> fluidOutputs,
                                            Component note, int ticks, int energy) {
        return recipe(path, layout, itemInputs, fluidInputs, itemOutputs, fluidOutputs,
                note, List.of(note), ticks, energy);
    }

    private static DomeMachineRecipe recipe(String path, DomeMachineRecipe.Layout layout,
                                            List<List<ItemStack>> itemInputs, List<FluidStack> fluidInputs,
                                            List<List<ItemStack>> itemOutputs, List<FluidStack> fluidOutputs,
                                            Component note, List<Component> outputNotes, int ticks, int energy) {
        return new DomeMachineRecipe(id(path), layout, itemInputs, fluidInputs, itemOutputs, fluidOutputs,
                note, outputNotes, ticks, energy);
    }

    private static List<SpeciesDefinition> species() {
        List<SpeciesDefinition> cached = bundledSpecies;
        if (cached != null) return cached;

        ArrayList<SpeciesDefinition> loaded = new ArrayList<>();
        try (var stream = DomeSurvivalJeiPlugin.class.getResourceAsStream(
                "/data/domesurvival/bio_module_loot/default.json")) {
            if (stream == null) return bundledSpecies = List.of();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("species")) {
                JsonObject value = element.getAsJsonObject();
                ResourceLocation entity = ResourceLocation.tryParse(value.get("entity").getAsString());
                ResourceLocation feed = ResourceLocation.tryParse(value.get("feed").getAsString());
                if (entity == null || feed == null) continue;
                loaded.add(new SpeciesDefinition(entity, feed,
                        value.get("feed_count").getAsInt(), value.get("water_mb").getAsInt(),
                        value.get("energy_per_tick").getAsInt(), value.get("process_ticks").getAsInt()));
            }
        } catch (Exception ignored) {
            loaded.clear();
        }
        bundledSpecies = List.copyOf(loaded);
        return bundledSpecies;
    }

    private static ItemStack legacyCapsule(ResourceLocation entity, boolean damaged) {
        if (damaged && entity.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "pig"))) {
            return new ItemStack(ModItems.DAMAGED_PIG_CRYOCAPSULE.get());
        }
        if (damaged) return ItemStack.EMPTY;
        return switch (entity.getPath()) {
            case "chicken" -> new ItemStack(ModItems.CHICKEN_CRYOCAPSULE.get());
            case "sheep" -> new ItemStack(ModItems.SHEEP_CRYOCAPSULE.get());
            case "cow" -> new ItemStack(ModItems.COW_CRYOCAPSULE.get());
            default -> ItemStack.EMPTY;
        };
    }

    private static String pathId(ResourceLocation id) {
        return id.getNamespace() + "/" + id.getPath();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, path);
    }

    private static RecipeType<DomeMachineRecipe> type(String path) {
        return RecipeType.create(DomeSurvival.MOD_ID, path, DomeMachineRecipe.class);
    }

    private record SpeciesDefinition(ResourceLocation entityId, ResourceLocation feedItem,
                                     int feedCount, int waterMb, int energyPerTick, int processTicks) { }
}
