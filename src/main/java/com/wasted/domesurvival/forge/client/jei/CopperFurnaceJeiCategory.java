package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.block.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

final class CopperFurnaceJeiCategory implements IRecipeCategory<SmeltingRecipe> {
    private final IDrawable icon;
    private final List<ItemStack> fuels=new ArrayList<>();
    CopperFurnaceJeiCategory(IGuiHelper h){
        icon=h.createDrawableItemStack(new ItemStack(ModBlocks.COPPER_FURNACE.get()));
        for(var item:ForgeRegistries.ITEMS){var stack=new ItemStack(item);if(ForgeHooks.getBurnTime(stack,net.minecraft.world.item.crafting.RecipeType.SMELTING)>0)fuels.add(stack);}
    }
    public RecipeType<SmeltingRecipe> getRecipeType(){return RefinedFuelMachinesJeiPlugin.COPPER;}
    public Component getTitle(){return Component.translatable("block.domesurvival.copper_furnace");}
    public int getWidth(){return 180;}public int getHeight(){return 128;}public IDrawable getIcon(){return icon;}
    public void setRecipe(IRecipeLayoutBuilder b,SmeltingRecipe recipe,IFocusGroup focus){
        b.addInputSlot(17,36).addIngredients(recipe.getIngredients().get(0));
        b.addSlot(RecipeIngredientRole.CATALYST,17,70).addItemStacks(fuels).addTooltipCallback((view,tooltip)->{
            view.getDisplayedItemStack().ifPresent(fuel->{int ticks=ForgeHooks.getBurnTime(fuel,net.minecraft.world.item.crafting.RecipeType.SMELTING);
                tooltip.add(RefinedMachineJeiArt.t("jei.fuel",String.format(Locale.ROOT,"%.1f",ticks*1.25/20)));});
        });
        b.addOutputSlot(147,53).addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }
    public void draw(SmeltingRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        int ticks=(int)Math.max(1,((long)recipe.getCookingTime()*4+4)/5);
        RefinedMachineJeiArt.panel(g,"copper",getTitle());RefinedMachineJeiArt.machine(g,ModBlocks.COPPER_FURNACE.get().defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT,true),false);RefinedMachineJeiArt.progress(g,49,85,82,ticks);
        RefinedMachineJeiArt.text(g,RefinedMachineJeiArt.t("jei.copper",String.format(Locale.ROOT,"%.1f",ticks/20.0)),12,101,156,0xFFCAD2D4);
        RefinedMachineJeiArt.text(g,RefinedMachineJeiArt.t("jei.base"),12,113,156,0xFF98A5AB);
    }
}
