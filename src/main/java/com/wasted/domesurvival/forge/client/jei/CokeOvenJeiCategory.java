package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.shaft.CokeOvenBlock;
import com.wasted.domesurvival.forge.machine.shaft.CokeOvenBlockEntity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** The single existing coking recipe, using the oven's own brickwork and model. */
final class CokeOvenJeiCategory implements IRecipeCategory<DomeMachineRecipe> {
    private final IDrawable icon;
    private final List<ItemStack> fuels = new ArrayList<>();
    CokeOvenJeiCategory(IGuiHelper helper) {
        icon=helper.createDrawableItemStack(new ItemStack(ModBlocks.COKE_OVEN.get()));
        for(var item:ForgeRegistries.ITEMS) {
            ItemStack stack=new ItemStack(item);
            if(CokeOvenBlockEntity.isValidFuel(stack))fuels.add(stack);
        }
    }
    public RecipeType<DomeMachineRecipe> getRecipeType(){return DomeSurvivalJeiPlugin.COKE_OVEN;}
    public Component getTitle(){return Component.translatable("block.domesurvival.coke_oven");}
    public int getWidth(){return 180;} public int getHeight(){return 128;} public IDrawable getIcon(){return icon;}
    public void setRecipe(IRecipeLayoutBuilder b,DomeMachineRecipe recipe,IFocusGroup focus){
        b.addInputSlot(17,36).addItemStacks(recipe.itemInputs().get(0));
        b.addSlot(RecipeIngredientRole.CATALYST,17,70).addItemStacks(fuels).addTooltipCallback((view,tooltip)->
            tooltip.add(Component.translatable("jei.domesurvival.note.any_furnace_fuel")));
        b.addOutputSlot(147,53).addItemStacks(recipe.itemOutputs().get(0));
    }
    public void draw(DomeMachineRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        g.blit(new ResourceLocation("domesurvival","textures/gui/coke_oven_v2/jei.png"),0,0,180,128,0,0,720,512,720,512);
        RefinedMachineJeiArt.text(g,getTitle(),12,9,156,0xFFF1D7B9);
        RefinedMachineJeiArt.machine(g,ModBlocks.COKE_OVEN.get().defaultBlockState().setValue(CokeOvenBlock.LIT,true),false);
        RefinedMachineJeiArt.progress(g,49,85,82,recipe.processTicks());
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.coke_v2.jei_time"),12,101,156,0xFFCAD2D4);
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.coke_v2.jei_base"),12,113,156,0xFF98A5AB);
    }
}
