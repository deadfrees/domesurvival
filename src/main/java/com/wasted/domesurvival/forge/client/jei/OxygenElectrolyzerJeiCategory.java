package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.client.render.OxygenElectrolyzerPreview;
import com.wasted.domesurvival.forge.client.screen.OxygenElectrolyzerScreen;
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

final class OxygenElectrolyzerJeiCategory implements IRecipeCategory<DomeMachineRecipe> {
    private static final ResourceLocation PANEL=new ResourceLocation("domesurvival","textures/gui/oxygen_electrolyzer_v2/jei.png");
    private final IDrawable icon;
    OxygenElectrolyzerJeiCategory(IGuiHelper helper){icon=helper.createDrawableItemStack(new ItemStack(ModBlocks.OXYGEN_ELECTROLYZER.get()));}
    public RecipeType<DomeMachineRecipe> getRecipeType(){return DomeSurvivalJeiPlugin.OXYGEN_ELECTROLYZER;}
    public Component getTitle(){return Component.translatable("block.domesurvival.oxygen_electrolyzer");}
    public IDrawable getIcon(){return icon;}
    public int getWidth(){return 180;}public int getHeight(){return 128;}
    public void setRecipe(IRecipeLayoutBuilder b,DomeMachineRecipe r,IFocusGroup focus){
        b.addInputSlot(17,36).addFluidStack(r.fluidInputs().get(0).getFluid(),r.fluidInputs().get(0).getAmount()).setFluidRenderer(200,false,16,39);
    }
    public void draw(DomeMachineRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        g.blit(PANEL,0,0,180,128,0,0,720,512,720,512);
        RefinedMachineJeiArt.text(g,getTitle(),12,9,156,0xFFF1D7B9);
        OxygenElectrolyzerPreview.draw(g,90,52,28,true,3000,2000);
        OxygenElectrolyzerScreen.gas(g,145,34,20,43,96,96);
        RefinedMachineJeiArt.text(g,Component.literal("96 O₂"),140,78,36,0xFFCAD2D4);
        RefinedMachineJeiArt.progress(g,52,87,76,r.processTicks());
        RefinedMachineJeiArt.text(g,Component.translatable("gui.domesurvival.electrolyzer_v2.jei_details",r.energyPerTick()*r.processTicks(),String.format(java.util.Locale.ROOT,"%.1f",r.processTicks()/20.0)),12,111,156,0xFFCAD2D4);
    }
    @Override public List<Component> getTooltipStrings(DomeMachineRecipe recipe,IRecipeSlotsView slots,double x,double y){
        return x>=143&&x<176&&y>=32&&y<86?List.of(Component.translatable("gui.domesurvival.electrolyzer_v2.jei_oxygen")):List.of();
    }
}
