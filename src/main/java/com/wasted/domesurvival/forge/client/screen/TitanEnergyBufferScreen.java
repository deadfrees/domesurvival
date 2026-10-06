package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferMenu;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.client.render.TitanBufferPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

/** Titan storage instruments; the side selector stays inside the machine window. */
public final class TitanEnergyBufferScreen extends AbstractContainerScreen<TitanEnergyBufferMenu> {
    private static final String KEY="gui.domesurvival.steel_buffer_v2.";
    private static final ResourceLocation PANEL=tex("panel"), CONFIG=tex("configuration");
    private static final ResourceLocation WIDGETS=new ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png");
    private static final int TEXT=0xFFCAD2D4,BLUE=0xFF83B8D2,ORANGE=0xFFE2AC82;
    private static final EnumMap<RelativeSide,Rect> SIDES=new EnumMap<>(RelativeSide.class);
    static {
        SIDES.put(RelativeSide.TOP,new Rect(46,51,20,20));SIDES.put(RelativeSide.LEFT,new Rect(22,75,20,20));
        SIDES.put(RelativeSide.FRONT,new Rect(46,75,20,20));SIDES.put(RelativeSide.RIGHT,new Rect(70,75,20,20));
        SIDES.put(RelativeSide.BOTTOM,new Rect(46,99,20,20));SIDES.put(RelativeSide.BACK,new Rect(70,99,20,20));
    }
    public TitanEnergyBufferScreen(TitanEnergyBufferMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=220;imageHeight=266;}
    private static ResourceLocation tex(String name){return new ResourceLocation("domesurvival","textures/gui/titan_buffer_v2/"+name+".png");}
    private static RelativeSide actual(RelativeSide s){return s==RelativeSide.LEFT?RelativeSide.RIGHT:s==RelativeSide.RIGHT?RelativeSide.LEFT:s;}
    private boolean inside(double x,double y,Rect r){return r.contains(x-leftPos,y-topPos);}
    private Component t(String key,Object... args){return Component.translatable(KEY+key,args);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&inside(x,y,new Rect(192,6,20,20))){
            menu.setSidePanelOpen(!menu.isSidePanelOpen());send(menu.isSidePanelOpen()?202:201);return true;
        }
        if(button==0&&menu.isSidePanelOpen())for(var e:SIDES.entrySet())if(inside(x,y,e.getValue())){
            if(e.getKey()!=RelativeSide.FRONT)send(TitanEnergyBufferMenu.sideButtonId(actual(e.getKey())));return true;
        }
        return super.mouseClicked(x,y,button);
    }
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private void widget(GuiGraphics g,int x,int y,int u,int v){g.blit(WIDGETS,leftPos+x,topPos+y,20,20,u*4F,v*4F,80,80,512,256);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.blit(PANEL,leftPos,topPos,220,266,0,0,880,1064,880,1064);widget(g,192,6,0,0);
        if(menu.isSidePanelOpen()){
            g.blit(CONFIG,leftPos+8,topPos+29,204,111,0,0,816,444,816,444);
            for(var e:SIDES.entrySet()){
                SideMode m=menu.getSideMode(actual(e.getKey()));Rect r=e.getValue();widget(g,r.x,r.y,m==SideMode.INPUT?20:m==SideMode.OUTPUT?40:0,24);
            }
        }else{
            // Reveal a fixed-width texture, then draw stationary divisions above it.
            int fill=(int)Math.min(122,(long)menu.getEnergyStored()*122/Math.max(1,menu.getEnergyCapacity()));
            if(fill>0){
                g.enableScissor(leftPos+19,topPos+47,leftPos+19+fill,topPos+55);
                g.blit(WIDGETS,leftPos+19,topPos+47,122,8,0,192,256,32,512,256);g.disableScissor();
            }
            for(int i=1;i<4;i++)g.fill(leftPos+19+i*122/4,topPos+47,leftPos+20+i*122/4,topPos+55,0xFF35434B);
            TitanBufferPreview.draw(g,leftPos+176,topPos+104,30,menu.getEnergyStored(),menu.getEnergyCapacity());
        }
        if(menu.isSidePanelOpen()||inside(mx,my,new Rect(192,6,20,20)))g.renderOutline(leftPos+192,topPos+6,20,20,BLUE);
    }
    private void label(GuiGraphics g,Component value,int x,int y,int width,int color){
        String s=value.getString();if(font.width(s)>width)s=font.plainSubstrByWidth(s,width-font.width("..."))+"...";
        g.drawString(font,s,x,y,color,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        label(g,title,13,10,166,0xFFF1D7B9);label(g,playerInventoryTitle,14,146,62,TEXT);
        if(menu.isSidePanelOpen()){
            label(g,Component.translatable("gui.domesurvival.side_config"),15,33,190,TEXT);
            label(g,t("input"),111,52,90,BLUE);label(g,t("input_role"),111,66,90,TEXT);
            label(g,t("output"),111,87,90,ORANGE);label(g,t("output_role"),111,101,90,TEXT);
            for(var e:SIDES.entrySet()){Rect r=e.getValue();g.drawCenteredString(font,Component.translatable("gui.domesurvival.coal_generator.side_letter."+e.getKey().name().toLowerCase(Locale.ROOT)),r.x+10,r.y+5,e.getKey()==RelativeSide.FRONT?0xFF78858B:TEXT);}
        }else{
            int percent=(int)Math.min(100,(long)menu.getEnergyStored()*100/Math.max(1,menu.getEnergyCapacity()));
            label(g,t("charge",percent),18,35,132,TEXT);
            label(g,Component.literal(menu.getEnergyStored()+" / "+menu.getEnergyCapacity()+" FE"),18,68,144,TEXT);
            label(g,t("flow_in",menu.getInputPerTick()),18,90,129,BLUE);
            label(g,t("flow_out",menu.getOutputPerTick()),18,106,129,ORANGE);
        }
    }
    private void help(GuiGraphics g,Component text,int x,int y){g.renderTooltip(font,font.split(text,220),x,y);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);
        if(hoveredSlot!=null&&hoveredSlot.hasItem())return;
        if(inside(x,y,new Rect(192,6,20,20)))help(g,Component.translatable("gui.domesurvival.side_config"),x,y);
        else if(menu.isSidePanelOpen()){
            for(var e:SIDES.entrySet())if(inside(x,y,e.getValue()))help(g,t(e.getKey()==RelativeSide.FRONT?"front":switch(menu.getSideMode(actual(e.getKey()))){case INPUT->"input_help";case OUTPUT->"output_help";default->"off";}),x,y);
        }else if(inside(x,y,new Rect(18,83,129,32)))help(g,t("rate_help",menu.getMaxInputPerTick(),menu.getMaxOutputPerTick()),x,y);
        else if(inside(x,y,new Rect(174,43,24,24)))help(g,t("charging_help"),x,y);
    }
    private record Rect(int x,int y,int w,int h){boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
}
