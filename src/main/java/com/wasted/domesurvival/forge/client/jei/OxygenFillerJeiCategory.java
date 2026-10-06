package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.client.render.OxygenFillerPreview;
import com.wasted.domesurvival.forge.client.screen.OxygenFillerScreen;
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

final class OxygenFillerJeiCategory implements IRecipeCategory<DomeMachineRecipe> {
    private static final ResourceLocation PANEL=new ResourceLocation("domesurvival","textures/gui/oxygen_filler_v2/jei.png");
    private final IDrawable icon;
    OxygenFillerJeiCategory(IGuiHelper helper){icon=helper.createDrawableItemStack(new ItemStack(ModBlocks.OXYGEN_FILLER.get()));}
    public RecipeType<DomeMachineRecipe> getRecipeType(){return DomeSurvivalJeiPlugin.OXYGEN_FILLER;}
    public Component getTitle(){return Component.translatable("block.domesurvival.oxygen_filler");}
    public IDrawable getIcon(){return icon;}
    public int getWidth(){return 180;}public int getHeight(){return 128;}
    public void setRecipe(IRecipeLayoutBuilder b,DomeMachineRecipe r,IFocusGroup focus){
        b.addInputSlot(17,52).addItemStacks(r.itemInputs().get(0));
        b.addOutputSlot(147,52).addItemStacks(r.itemOutputs().get(0));
    }
    public void draw(DomeMachineRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        g.blit(PANEL,0,0,180,128,0,0,720,512,720,512);
        RefinedMachineJeiArt.text(g,getTitle(),12,9,156,0xFFF1D7B9);
        int pressure=(int)(net.minecraft.Util.getMillis()%6000)/6;
        OxygenFillerPreview.draw(g,90,53,28,true,0,pressure);
        RefinedMachineJeiArt.progress(g,52,87,76,120);
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.filler_v2.jei_details",r.energyPerTick()*r.processTicks(),String.format(java.util.Locale.ROOT,"%.1f",r.processTicks()/20.0)),12,111,156,0xFFCAD2D4);
    }
    @Override public List<Component> getTooltipStrings(DomeMachineRecipe recipe,IRecipeSlotsView slots,double x,double y){
        return x>=49&&x<132&&y>=85&&y<98?List.of(recipe.note()):List.of();
    }
}
