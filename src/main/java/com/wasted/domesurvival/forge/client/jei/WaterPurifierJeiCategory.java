package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.client.render.WaterPurifierPreview;
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

final class WaterPurifierJeiCategory implements IRecipeCategory<DomeMachineRecipe> {
    private static final ResourceLocation PANEL=new ResourceLocation("domesurvival","textures/gui/water_purifier_v2/jei.png");
    private final IDrawable icon;
    WaterPurifierJeiCategory(IGuiHelper helper){icon=helper.createDrawableItemStack(new ItemStack(ModBlocks.WATER_PURIFIER.get()));}
    public RecipeType<DomeMachineRecipe> getRecipeType(){return DomeSurvivalJeiPlugin.WATER_PURIFIER;}
    public Component getTitle(){return Component.translatable("block.domesurvival.water_purifier");}
    public IDrawable getIcon(){return icon;}public int getWidth(){return 180;}public int getHeight(){return 128;}
    public void setRecipe(IRecipeLayoutBuilder b,DomeMachineRecipe r,IFocusGroup focus){
        b.addInputSlot(17,36).addFluidStack(r.fluidInputs().get(0).getFluid(),r.fluidInputs().get(0).getAmount()).setFluidRenderer(250,false,16,39);
        b.addOutputSlot(147,36).addFluidStack(r.fluidOutputs().get(0).getFluid(),r.fluidOutputs().get(0).getAmount()).setFluidRenderer(200,false,16,39);
        b.addSlot(RecipeIngredientRole.CATALYST,50,84).addItemStacks(r.itemInputs().get(0)).addTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.domesurvival.note.filter_damaged")));
    }
    public void draw(DomeMachineRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        g.blit(PANEL,0,0,180,128,0,0,720,512,720,512);
        RefinedMachineJeiArt.text(g,getTitle(),12,9,156,0xFFF1D7B9);
        WaterPurifierPreview.draw(g,90,52,28,true,3000,2000);
        RefinedMachineJeiArt.progress(g,79,87,52,r.processTicks());
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.purifier_v2.jei_details",r.energyPerTick()*r.processTicks(),String.format(java.util.Locale.ROOT,"%.1f",r.processTicks()/20.0)),12,111,156,0xFFCAD2D4);
    }
}
