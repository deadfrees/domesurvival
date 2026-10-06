package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceBlock;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceBlockEntity;
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

/** Existing steel/slag recipe with its actual model, coke fuels and deepslate GUI. */
final class ShaftFurnaceJeiCategory implements IRecipeCategory<DomeMachineRecipe> {
    private static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath("domesurvival","textures/gui/shaft_furnace_v2/jei.png");
    private final IDrawable icon;
    private final List<ItemStack> fuels=new ArrayList<>();
    ShaftFurnaceJeiCategory(IGuiHelper helper) {
        icon=helper.createDrawableItemStack(new ItemStack(ModBlocks.SHAFT_FURNACE.get()));
        for(var item:ForgeRegistries.ITEMS) {
            var stack=new ItemStack(item);
            if(ShaftFurnaceBlockEntity.isValidCoke(stack)&&net.minecraftforge.common.ForgeHooks.getBurnTime(stack,net.minecraft.world.item.crafting.RecipeType.SMELTING)>0)fuels.add(stack);
        }
    }
    public RecipeType<DomeMachineRecipe> getRecipeType(){return DomeSurvivalJeiPlugin.SHAFT_FURNACE;}
    public Component getTitle(){return Component.translatable("block.domesurvival.shaft_furnace");}
    public int getWidth(){return 180;} public int getHeight(){return 128;} public IDrawable getIcon(){return icon;}
    public void setRecipe(IRecipeLayoutBuilder b,DomeMachineRecipe recipe,IFocusGroup focus){
        b.addInputSlot(17,36).addItemStacks(recipe.itemInputs().get(0));
        b.addSlot(RecipeIngredientRole.CATALYST,17,70).addItemStacks(fuels).addTooltipCallback((view,tooltip)->
            tooltip.add(Component.translatable("jei.domesurvival.note.coke_is_heat")));
        b.addOutputSlot(147,36).addItemStacks(recipe.itemOutputs().get(0));
        b.addOutputSlot(147,70).addItemStacks(recipe.itemOutputs().get(1));
    }
    public void draw(DomeMachineRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        g.blit(PANEL,0,0,180,128,0,0,720,512,720,512);
        RefinedMachineJeiArt.text(g,getTitle(),12,9,156,0xFFF1D7B9);
        RefinedMachineJeiArt.machine(g,ModBlocks.SHAFT_FURNACE.get().defaultBlockState().setValue(ShaftFurnaceBlock.LIT,true),false);
        RefinedMachineJeiArt.progress(g,49,85,82,recipe.processTicks());
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.shaft_v2.jei_time"),12,101,156,0xFFCAD2D4);
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.shaft_v2.jei_base"),12,113,156,0xFF98A5AB);
    }
}
