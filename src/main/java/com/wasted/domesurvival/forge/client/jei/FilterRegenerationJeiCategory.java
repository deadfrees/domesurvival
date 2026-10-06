package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.machine.filter.FilterRegenerationRegistry;
import com.wasted.domesurvival.forge.client.render.FilterRegenerationPreview;
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
import java.util.List;

final class FilterRegenerationJeiCategory implements IRecipeCategory<DomeMachineRecipe> {
    private static final ResourceLocation PANEL=new ResourceLocation("domesurvival","textures/gui/filter_regeneration_v2/jei.png");
    private final IDrawable icon;
    FilterRegenerationJeiCategory(IGuiHelper helper){icon=helper.createDrawableItemStack(new ItemStack(FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get()));}
    public RecipeType<DomeMachineRecipe> getRecipeType(){return DomeSurvivalJeiPlugin.FILTER_REGENERATION;}
    public Component getTitle(){return Component.translatable("block.domesurvival.filter_regeneration_station");}
    public IDrawable getIcon(){return icon;}
    public int getWidth(){return 180;}public int getHeight(){return 128;}
    public void setRecipe(IRecipeLayoutBuilder b,DomeMachineRecipe r,IFocusGroup focus){
        b.addInputSlot(17,68).addItemStacks(r.itemInputs().get(0));
        b.addInputSlot(17,33).addItemStacks(r.itemInputs().get(1));
        b.addOutputSlot(147,68).addItemStacks(r.itemOutputs().get(0));
    }
    public void draw(DomeMachineRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        g.blit(PANEL,0,0,180,128,0,0,720,512,720,512);
        RefinedMachineJeiArt.text(g,getTitle(),12,9,156,0xFFF1D7B9);
        FilterRegenerationPreview.draw(g,90,56,28,true,0,0);
        RefinedMachineJeiArt.progress(g,52,89,76,r.processTicks());
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.filter_regeneration_v2.jei_details",r.energyPerTick()*r.processTicks(),String.format(java.util.Locale.ROOT,"%.1f",r.processTicks()/20.0)),12,111,156,0xFFCAD2D4);
    }
    @Override public List<Component> getTooltipStrings(DomeMachineRecipe recipe,IRecipeSlotsView slots,double x,double y){
        return x>=49&&x<132&&y>=87&&y<100?List.of(recipe.note()):List.of();
    }
}
