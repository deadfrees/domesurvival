package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.client.render.BioincubatorPreview;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/** Both incubation modes use the same physical chamber as the machine screen. */
final class BioincubatorJeiCategory implements IRecipeCategory<DomeMachineRecipe> {
    private final boolean repair;
    private final IDrawable icon;
    BioincubatorJeiCategory(IGuiHelper helper,boolean repair){this.repair=repair;icon=helper.createDrawableItemStack(new ItemStack(ModBlocks.BIOINCUBATOR.get()));}
    public RecipeType<DomeMachineRecipe> getRecipeType(){return repair?DomeSurvivalJeiPlugin.BIO_REPAIR:DomeSurvivalJeiPlugin.BIO_INCUBATION;}
    public Component getTitle(){return Component.translatable(repair?"jei.domesurvival.bio_repair":"jei.domesurvival.bio_incubation");}
    public int getWidth(){return 180;}public int getHeight(){return 128;}
    public IDrawable getIcon(){return icon;}
    public void setRecipe(IRecipeLayoutBuilder builder,DomeMachineRecipe recipe,IFocusGroup focus){
        for(int i=0;i<recipe.itemInputs().size();i++)builder.addInputSlot(14+28*i,81).addItemStacks(recipe.itemInputs().get(i));
        for(var fluid:recipe.fluidInputs())builder.addInputSlot(14,32).setFluidRenderer(fluid.getAmount(),true,12,36).addFluidStack(fluid.getFluid(),fluid.getAmount());
        for(var output:recipe.itemOutputs())builder.addOutputSlot(150,81).addItemStacks(output).addRichTooltipCallback((view,tooltip)->tooltip.add(recipe.note()));
    }
    public void draw(DomeMachineRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){
        var panel=new ResourceLocation("domesurvival","textures/gui/bioincubator_v2/"+(repair?"jei_repair":"jei")+".png");
        g.blit(panel,0,0,180,128,0,0,720,512,720,512);
        DomeJeiStyle.drawCenteredClamped(g,getTitle(),90,10,156,DomeJeiStyle.TEXT);
        ItemStack sample=recipe.itemInputs().isEmpty()?ItemStack.EMPTY:recipe.itemInputs().get(0).get(0);
        BioincubatorPreview.draw(g,105,42,22,true,repair?1:0,sample);
        int filled=(int)(122*DomeJeiStyle.animationFraction(recipe.processTicks()));
        if(filled>0)g.blit(new ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png"),45,65,filled,4,0,192,Math.max(1,256*filled/122),32,512,256);
        String stats=(long)recipe.processTicks()*recipe.energyPerTick()+" FE · "+String.format(Locale.ROOT,"%.1f s",recipe.processTicks()/20D);
        DomeJeiStyle.drawCenteredClamped(g,Component.literal(stats),90,111,156,DomeJeiStyle.TEXT_DIM);
    }
}
