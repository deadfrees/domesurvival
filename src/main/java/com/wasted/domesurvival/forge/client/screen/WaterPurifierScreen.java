package com.wasted.domesurvival.forge.client.screen;
import com.wasted.domesurvival.forge.machine.water.WaterPurifierMenu;

import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class WaterPurifierScreen extends AbstractContainerScreen<WaterPurifierMenu> {
    private static final String KEY="gui.domesurvival.purifier_v2.";
    private static final int TEXT=0xFFCAD2D4,MUTED=0xFF98A5AB,COPPER=0xFFE2AC82,BLUE=0xFF83B8D2;
    private static final ResourceLocation PANEL=tex("panel"),CONFIG=tex("configuration"),MODULES=tex("modules");
    private static final ResourceLocation WIDGETS=new ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png");
    private static final EnumMap<RelativeSide,Rect> SIDES=new EnumMap<>(RelativeSide.class);
    static {
        SIDES.put(RelativeSide.TOP,new Rect(46,51,20,20));SIDES.put(RelativeSide.LEFT,new Rect(22,75,20,20));
        SIDES.put(RelativeSide.FRONT,new Rect(46,75,20,20));SIDES.put(RelativeSide.RIGHT,new Rect(70,75,20,20));
        SIDES.put(RelativeSide.BOTTOM,new Rect(46,99,20,20));SIDES.put(RelativeSide.BACK,new Rect(70,99,20,20));
    }
    public WaterPurifierScreen(WaterPurifierMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=220;imageHeight=266;}
    private static ResourceLocation tex(String n){return new ResourceLocation("domesurvival","textures/gui/water_purifier_v2/"+n+".png");}
    private static RelativeSide actual(RelativeSide s){return s==RelativeSide.LEFT?RelativeSide.RIGHT:s==RelativeSide.RIGHT?RelativeSide.LEFT:s;}
    private boolean inside(double x,double y,Rect r){return r.contains(x-leftPos,y-topPos);}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private void tab(int id){menu.setTab(id);send(id);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&inside(x,y,new Rect(192,6,20,20))){tab(menu.isSidePanelOpen()?201:202);return true;}
        if(button==0&&inside(x,y,new Rect(168,6,20,20))){tab(menu.isModulePanelOpen()?201:200);return true;}
        if(button==0&&menu.isSidePanelOpen())for(var e:SIDES.entrySet())if(inside(x,y,e.getValue())){
            if(e.getKey()!=RelativeSide.FRONT)send(100+actual(e.getKey()).ordinal());return true;
        }
        return super.mouseClicked(x,y,button);
    }
    private void widget(GuiGraphics g,int x,int y,int w,int h,int u,int v,int sw,int sh){
        g.blit(WIDGETS,leftPos+x,topPos+y,w,h,u*4F,v*4F,sw*4,sh*4,512,256);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.blit(PANEL,leftPos,topPos,220,266,0,0,880,1064,880,1064);
        widget(g,192,6,20,20,0,0,20,20);widget(g,168,6,20,20,88,0,20,20);
        if(menu.isModulePanelOpen())g.blit(MODULES,leftPos+8,topPos+29,204,111,0,0,816,444,816,444);
        else if(menu.isSidePanelOpen()){
            g.blit(CONFIG,leftPos+8,topPos+29,204,111,0,0,816,444,816,444);
            for(var e:SIDES.entrySet()){
                Rect r=e.getValue();SideMode m=menu.getSideMode(actual(e.getKey()));
                widget(g,r.x,r.y,20,20,m==SideMode.INPUT?20:m==SideMode.OUTPUT?40:0,24,20,20);
            }
        }else{
            com.wasted.domesurvival.forge.client.gui.MachineGaugeRenderer.segmented(
                    g,leftPos+17,topPos+46,10,80,menu.energyStored(),menu.energyCapacity());
            fluid(g,45,46,22,50,menu.rawWater(),menu.rawCapacity(),net.minecraft.world.level.material.Fluids.WATER);
            fluid(g,181,46,22,50,menu.purifiedWater(),menu.purifiedCapacity(),com.wasted.domesurvival.forge.fluid.ModFluids.PURIFIED_WATER.get());
            com.wasted.domesurvival.forge.client.render.WaterPurifierPreview.draw(g,leftPos+123,topPos+70,25,menu.status()==1,menu.rawWater(),menu.purifiedWater());
            int width=(int)Math.min(85,(long)menu.progress()*85/Math.max(1,menu.progressMax()));
            if(width>0){g.enableScissor(leftPos+82,topPos+111,leftPos+82+width,topPos+119);widget(g,82,111,85,8,0,48,64,8);g.disableScissor();}

        }
        if(menu.isModulePanelOpen()||inside(mx,my,new Rect(168,6,20,20)))g.renderOutline(leftPos+168,topPos+6,20,20,COPPER);
        if(menu.isSidePanelOpen()||inside(mx,my,new Rect(192,6,20,20)))g.renderOutline(leftPos+192,topPos+6,20,20,BLUE);
    }
    private void text(GuiGraphics g,Component value,int x,int y,int width,int color){
        String s=value.getString();if(font.width(s)>width)s=font.plainSubstrByWidth(s,width-font.width("..."))+"...";
        g.drawString(font,s,x,y,color,false);
    }
    private Component t(String key,Object...args){return Component.translatable(KEY+key,args);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        text(g,title,13,10,146,0xFFF1D7B9);
        if(menu.isModulePanelOpen()){
            text(g,Component.translatable("gui.domesurvival.upgrade_modules"),15,34,190,TEXT);
            text(g,t("module_title"),59,54,145,COPPER);text(g,t("module_next"),59,72,145,TEXT);
            text(g,t("module_conflict"),59,89,145,MUTED);
        }else if(menu.isSidePanelOpen()){
            text(g,Component.translatable("gui.domesurvival.side_config"),15,33,190,TEXT);
            text(g,t("input"),111,52,90,BLUE);text(g,t("input_short"),111,66,90,TEXT);
            text(g,t("output"),111,87,90,COPPER);text(g,t("output_short"),111,101,90,TEXT);
            for(var e:SIDES.entrySet()){Rect r=e.getValue();g.drawCenteredString(font,Component.translatable("gui.domesurvival.coal_generator.side_letter."+e.getKey().name().toLowerCase(Locale.ROOT)),r.x+10,r.y+5,e.getKey()==RelativeSide.FRONT?MUTED:TEXT);}
        }else{
            text(g,t("conversion"),44,33,162,TEXT);

        }
        text(g,playerInventoryTitle,14,146,62,TEXT);
    }
    private void help(GuiGraphics g,Component text,int x,int y){g.renderTooltip(font,font.split(text,220),x,y);}
    @Override protected void renderTooltip(GuiGraphics g,int x,int y){
        // Describe the hovered module, without control instructions.
        if(hoveredSlot!=null&&hoveredSlot.hasItem()&&getMenu().getCarried().isEmpty()&&hoveredSlot.getItem().getItem() instanceof MachineModuleItem module){
            String key=switch(module.module().type()){
                case BUFFER -> "buffer_help";
                case EFFICIENCY -> "efficiency_help";
                case OVERDRIVE -> "overdrive_help";
                default -> "unsupported_module";
            };
            help(g,t(key),x,y);return;
        }
        super.renderTooltip(g,x,y);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);
        if(hoveredSlot!=null&&hoveredSlot.hasItem())return;
        if(inside(x,y,new Rect(192,6,20,20)))help(g,Component.translatable("gui.domesurvival.side_config"),x,y);
        else if(inside(x,y,new Rect(168,6,20,20)))help(g,Component.translatable("gui.domesurvival.upgrade_modules"),x,y);
        else if(menu.isModulePanelOpen()&&inside(x,y,new Rect(18,49,185,73)))help(g,t("module_help"),x,y);
        else if(menu.isSidePanelOpen()){
            for(var e:SIDES.entrySet())if(inside(x,y,e.getValue())){
                var m=menu.getSideMode(actual(e.getKey()));
                help(g,t(e.getKey()==RelativeSide.FRONT?"front":m==SideMode.INPUT?"input_help":m==SideMode.OUTPUT?"output_help":"off"),x,y);
            }
        }else if(inside(x,y,new Rect(14,43,16,86)))help(g,t("energy",menu.energyStored(),menu.energyCapacity()),x,y);
        else if(inside(x,y,new Rect(42,43,28,56)))help(g,t("raw",menu.rawWater(),menu.rawCapacity()),x,y);
        else if(inside(x,y,new Rect(178,43,28,56)))help(g,t("purified",menu.purifiedWater(),menu.purifiedCapacity()),x,y);
        else if(inside(x,y,new Rect(79,108,91,14)))help(g,t("process",menu.progress(),menu.progressMax(),menu.cycleEnergy()),x,y);
        else if(inside(x,y,new Rect(42,107,24,24)))help(g,t("bucket_help"),x,y);
        else if(inside(x,y,new Rect(178,107,24,24)))help(g,t("filter_help"),x,y);

    }
    private void fluid(GuiGraphics g,int x,int y,int w,int h,int amount,int capacity,net.minecraft.world.level.material.Fluid fluid){
        int filled=(int)((long)h*amount/Math.max(1,capacity));if(filled<=0)return;
        var ext=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
        int tint=ext.getTintColor();g.setColor(((tint>>16)&255)/255F,((tint>>8)&255)/255F,(tint&255)/255F,1);
        g.blit(leftPos+x,topPos+y+h-filled,0,w,filled,sprite);g.setColor(1,1,1,1);
    }
    private record Rect(int x,int y,int w,int h){boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
}
