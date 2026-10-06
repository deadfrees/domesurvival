package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.coal.CoalGeneratorBlockEntity;
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
import java.util.Locale;
import static com.wasted.domesurvival.forge.client.jei.RefinedFuelMachinesJeiPlugin.GeneratorFuel;

final class CoalGeneratorJeiCategory implements IRecipeCategory<GeneratorFuel> {
    private final IDrawable icon;
    CoalGeneratorJeiCategory(IGuiHelper h){icon=h.createDrawableItemStack(new ItemStack(ModBlocks.COAL_GENERATOR.get()));}
    public RecipeType<GeneratorFuel> getRecipeType(){return RefinedFuelMachinesJeiPlugin.COAL;}
    public Component getTitle(){return Component.translatable("block.domesurvival.coal_generator");}
    public int getWidth(){return 180;}public int getHeight(){return 128;}public IDrawable getIcon(){return icon;}
    public void setRecipe(IRecipeLayoutBuilder b,GeneratorFuel r,IFocusGroup f){b.addInputSlot(17,48).addItemStack(r.fuel());}
    public void draw(GeneratorFuel r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        RefinedMachineJeiArt.panel(g,"coal",getTitle());RefinedMachineJeiArt.machine(g,ModBlocks.COAL_GENERATOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,true),false);RefinedMachineJeiArt.progress(g,49,85,82,r.ticks());
        g.blit(new ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png"),146,38,12,41,256,0,48,188,512,256);
        int rate=CoalGeneratorBlockEntity.GENERATION_PER_TICK;
        RefinedMachineJeiArt.text(g,RefinedMachineJeiArt.t("jei.rate",rate,String.format(Locale.ROOT,"%.0f",r.ticks()/20.0)),12,101,156,0xFFCAD2D4);
        RefinedMachineJeiArt.text(g,RefinedMachineJeiArt.t("jei.coal",(long)r.ticks()*rate),12,113,156,0xFFE0BC7E);
    }
}
