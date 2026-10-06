package com.wasted.domesurvival.forge.client.screen;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceMenu;

import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class ShaftFurnaceScreen extends AbstractContainerScreen<ShaftFurnaceMenu> {
    private static final String KEY="gui.domesurvival.shaft_v2.";
    private static final int TEXT=0xFFCAD2D4,MUTED=0xFF98A5AB,COPPER=0xFFE2AC82,BLUE=0xFF83B8D2;
    private static final ResourceLocation PANEL=tex("panel"),CONFIG=tex("configuration"),MODULES=tex("modules");
    private static final ResourceLocation WIDGETS=new ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png");
    private static final EnumMap<RelativeSide,Rect> SIDES=new EnumMap<>(RelativeSide.class);
    static {
        SIDES.put(RelativeSide.TOP,new Rect(46,51,20,20));SIDES.put(RelativeSide.LEFT,new Rect(22,75,20,20));
        SIDES.put(RelativeSide.FRONT,new Rect(46,75,20,20));SIDES.put(RelativeSide.RIGHT,new Rect(70,75,20,20));
        SIDES.put(RelativeSide.BOTTOM,new Rect(46,99,20,20));SIDES.put(RelativeSide.BACK,new Rect(70,99,20,20));
    }
    public ShaftFurnaceScreen(ShaftFurnaceMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=220;imageHeight=266;}
    private static ResourceLocation tex(String n){return new ResourceLocation("domesurvival","textures/gui/shaft_furnace_v2/"+n+".png");}
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
                    g,leftPos+17,topPos+61,12,71,menu.burnRemaining(),menu.burnTotal());
            int width=(int)Math.min(85,(long)menu.progress()*85/Math.max(1,menu.progressMax()));
            if(width>0){g.enableScissor(leftPos+82,topPos+84,leftPos+82+width,topPos+92);widget(g,82,84,85,8,0,48,64,8);g.disableScissor();}
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
            text(g,t("efficiency"),59,57,145,COPPER);text(g,t("fuel_bonus"),59,73,145,TEXT);
            text(g,t("same_speed"),59,88,145,MUTED);text(g,t("next_fuel"),59,105,145,MUTED);
        }else if(menu.isSidePanelOpen()){
            text(g,Component.translatable("gui.domesurvival.side_config"),15,33,190,TEXT);
            text(g,t("input"),111,52,90,BLUE);text(g,t("input_short"),111,66,90,TEXT);
            text(g,t("output"),111,87,90,COPPER);text(g,t("output_short"),111,101,90,TEXT);
            for(var e:SIDES.entrySet()){Rect r=e.getValue();g.drawCenteredString(font,Component.translatable("gui.domesurvival.coal_generator.side_letter."+e.getKey().name().toLowerCase(Locale.ROOT)),r.x+10,r.y+5,e.getKey()==RelativeSide.FRONT?MUTED:TEXT);}
        }else{
            text(g,t("smelting"),15,33,190,COPPER);
            text(g,t("duration",String.format(Locale.ROOT,"%.1f",menu.progressMax()/20.0)),82,62,84,TEXT);
        }
        text(g,playerInventoryTitle,14,146,62,TEXT);
    }
    private void help(GuiGraphics g,Component text,int x,int y){g.renderTooltip(font,font.split(text,220),x,y);}
    @Override protected void renderTooltip(GuiGraphics g,int x,int y){
        // A module's electric-machine defaults are misleading here. Display its
        // actual furnace effect once, including when hovering the player's item.
        if(hoveredSlot!=null&&hoveredSlot.hasItem()&&getMenu().getCarried().isEmpty()&&hoveredSlot.getItem().getItem() instanceof MachineModuleItem module){
            help(g,t(module.module().type()==MachineModuleType.EFFICIENCY?"module_help":"unsupported_module"),x,y);return;
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
        }else if(inside(x,y,new Rect(14,58,18,77)))help(g,t("burn",menu.burnRemaining(),menu.burnTotal()),x,y);
        else if(inside(x,y,new Rect(79,81,91,14)))help(g,t("progress",menu.progress(),menu.progressMax()),x,y);
    }
    private record Rect(int x,int y,int w,int h){boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
}
