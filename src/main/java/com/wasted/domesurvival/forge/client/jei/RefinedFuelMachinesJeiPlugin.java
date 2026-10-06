package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.coal.CoalGeneratorBlockEntity;
import mezz.jei.api.*;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@JeiPlugin
public final class RefinedFuelMachinesJeiPlugin implements IModPlugin {
    public record GeneratorFuel(ItemStack fuel,int ticks){}
    public static final RecipeType<SmeltingRecipe> COPPER=RecipeType.create("domesurvival","copper_furnace",SmeltingRecipe.class);
    public static final RecipeType<GeneratorFuel> COAL=RecipeType.create("domesurvival","coal_generator",GeneratorFuel.class);
    @Override public ResourceLocation getPluginUid(){return new ResourceLocation("domesurvival","refined_fuel_machines");}
    @Override public void registerCategories(IRecipeCategoryRegistration r){
        var helper=r.getJeiHelpers().getGuiHelper();
        r.addRecipeCategories(new CopperFurnaceJeiCategory(helper),new CoalGeneratorJeiCategory(helper));
    }
    @Override public void registerRecipes(IRecipeRegistration r){
        var world=Minecraft.getInstance().level;if(world==null)return;
        // RecipeManager is authoritative; each recipe ID is registered once.
        r.addRecipes(COPPER,world.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING));
        List<GeneratorFuel> fuels=new ArrayList<>();
        for(var item:ForgeRegistries.ITEMS){var stack=new ItemStack(item);int ticks=CoalGeneratorBlockEntity.getFuelBurnTime(stack);if(ticks>0)fuels.add(new GeneratorFuel(stack,ticks));}
        r.addRecipes(COAL,fuels);
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){
        r.addRecipeCatalyst(ModBlocks.COPPER_FURNACE.get(),COPPER);
        r.addRecipeCatalyst(ModBlocks.COAL_GENERATOR.get(),COAL);
    }
}
