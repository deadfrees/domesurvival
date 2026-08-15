package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenElectrolyzerBlockEntity;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenElectrolyzerMenu;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public final class OxygenElectrolyzerScreen extends AbstractContainerScreen<OxygenElectrolyzerMenu> {
    private static final ResourceLocation PORT_TEXTURE = new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");
    private static final int PANEL_WIDTH = 220, PANEL_HEIGHT = 266;
    private static final int GEAR_X = 224, GEAR_Y = 8, GEAR_SIZE = 20;
    private static final int SIDE_PANEL_X = 248, SIDE_PANEL_WIDTH = 96, SIDE_PANEL_HEIGHT = 122;
    private static final int ENERGY_X = 14, ENERGY_Y = 37, ENERGY_W = 18, ENERGY_H = 53;
    private static final int ENERGY_VALUE_X = 42, ENERGY_VALUE_Y = 38, ENERGY_VALUE_W = 166, ENERGY_VALUE_H = 15;
    private static final int BAR_Y = 111, BAR_W = 60, BAR_H = 14;
    private static final int WATER_X = 14, PROCESS_X = 82, OXYGEN_X = 150;
    private static final int INVENTORY_X = 11, INVENTORY_Y = 158, INVENTORY_SLOT = 22, INVENTORY_STEP = 22, HOTBAR_Y = 226;
    private static final int SIDE_BUTTON_SIZE = 14, SIDE_GRID_STEP = 22;
    private static final EnumMap<RelativeSide, Rect> SIDE_RECTS = createSideRects();
    private static final int PORT_SIZE = 6, PORT_TEX_WIDTH = 24, PORT_TEX_HEIGHT = 6, PORT_OFF_U = 0, PORT_INPUT_U = 12, PORT_OUTPUT_U = 18;
    private static final int ENERGY_MAIN = 0xFF8D792A, ENERGY_BRIGHT = 0xFFAA9438;
    private static final int WATER_MAIN = 0xFF2B8E97, WATER_BRIGHT = 0xFF72D4DB;
    private static final int OXYGEN_MAIN = 0xFFADB4B8, OXYGEN_BRIGHT = 0xFFE4E8EA;
    private boolean sidePanelOpen;

    public OxygenElectrolyzerScreen(OxygenElectrolyzerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = PANEL_WIDTH; imageHeight = PANEL_HEIGHT; }
    @Override protected void init() { super.init(); leftPos = (width - PANEL_WIDTH) / 2; topPos = (height - imageHeight) / 2; }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> regions = new EnumMap<>(RelativeSide.class);
        int centerX = SIDE_PANEL_X + (SIDE_PANEL_WIDTH - SIDE_BUTTON_SIZE) / 2, middleY = 66;
        regions.put(RelativeSide.TOP, new Rect(centerX, middleY - SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.LEFT, new Rect(centerX - SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.FRONT, new Rect(centerX, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.RIGHT, new Rect(centerX + SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BOTTOM, new Rect(centerX, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BACK, new Rect(centerX + SIDE_GRID_STEP, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        return regions;
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { if (button == 0) { if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) { sidePanelOpen = !sidePanelOpen; return true; } if (sidePanelOpen) { RelativeSide side = getHoveredSide(mouseX, mouseY); if (side != null && minecraft != null && minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, OxygenElectrolyzerMenu.sideButtonId(machineSideForVisualSide(side))); return true; }}} return super.mouseClicked(mouseX, mouseY, button); }
    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) { double lx = mouseX - leftPos, ly = mouseY - topPos; return lx >= x && lx < x + w && ly >= y && ly < y + h; }
    private static RelativeSide machineSideForVisualSide(RelativeSide side) { return switch (side) { case LEFT -> RelativeSide.RIGHT; case RIGHT -> RelativeSide.LEFT; default -> side; }; }

    @Override public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg); super.render(gg, mouseX, mouseY, partialTick); renderTooltip(gg, mouseX, mouseY);
        if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) { gg.renderTooltip(font, Component.translatable("gui.domesurvival.side_config"), mouseX, mouseY); return; }
        if (sidePanelOpen) { RelativeSide hovered = getHoveredSide(mouseX, mouseY); if (hovered != null) { List<Component> tooltip = new ArrayList<>(); tooltip.add(Component.translatable("gui.domesurvival.side." + hovered.name().toLowerCase(Locale.ROOT))); tooltip.add(getSideModeTooltip(menu.getSideMode(machineSideForVisualSide(hovered)))); gg.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY); return; } }
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY) || isHovering(ENERGY_VALUE_X, ENERGY_VALUE_Y, ENERGY_VALUE_W, ENERGY_VALUE_H, mouseX, mouseY)) gg.renderTooltip(font, Component.translatable("gui.domesurvival.oxygen_electrolyzer.energy_tooltip", menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        else if (isHovering(WATER_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) gg.renderTooltip(font, Component.translatable("gui.domesurvival.oxygen_electrolyzer.water_tooltip", menu.getWater(), menu.getWaterCapacity()), mouseX, mouseY);
        else if (isHovering(OXYGEN_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) gg.renderTooltip(font, Component.translatable("gui.domesurvival.oxygen_electrolyzer.oxygen_tooltip", menu.getOxygen(), menu.getOxygenCapacity()), mouseX, mouseY);
        else if (isHovering(PROCESS_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) gg.renderTooltip(font, Component.translatable("gui.domesurvival.oxygen_electrolyzer.progress_tooltip", menu.getProgressMax() <= 0 ? 0 : menu.getProgress() * 100 / menu.getProgressMax()), mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x=leftPos, y=topPos;
        drawPanel(gg,x,y,PANEL_WIDTH,PANEL_HEIGHT,0xFF30363A);
        drawFrame(gg,x+ENERGY_X,y+ENERGY_Y,ENERGY_W,ENERGY_H,0xFF14191C);
        drawFrame(gg,x+ENERGY_VALUE_X,y+ENERGY_VALUE_Y,ENERGY_VALUE_W,ENERGY_VALUE_H,0xFF151A1D);
        drawFrame(gg,x+WATER_X,y+BAR_Y,BAR_W,BAR_H,0xFF151A1D);
        drawFrame(gg,x+PROCESS_X,y+BAR_Y,BAR_W,BAR_H,0xFF151A1D);
        drawFrame(gg,x+OXYGEN_X,y+BAR_Y,BAR_W,BAR_H,0xFF151A1D);
        int eCap=Math.max(1,menu.getEnergyCapacity()); int eFill=Math.min(ENERGY_H-6,(int)((long)menu.getEnergyStored()*(ENERGY_H-6)/eCap));
        if(eFill>0){int bottom=y+ENERGY_Y+ENERGY_H-3, top=bottom-eFill; gg.fill(x+ENERGY_X+3,top,x+ENERGY_X+ENERGY_W-3,bottom,ENERGY_MAIN); gg.fill(x+ENERGY_X+4,top,x+ENERGY_X+6,bottom,ENERGY_BRIGHT);}
        fillBar(gg,x+WATER_X,y+BAR_Y,BAR_W,BAR_H,menu.getWater(),menu.getWaterCapacity(),WATER_MAIN,WATER_BRIGHT);
        fillBar(gg,x+OXYGEN_X,y+BAR_Y,BAR_W,BAR_H,menu.getOxygen(),menu.getOxygenCapacity(),OXYGEN_MAIN,OXYGEN_BRIGHT);
        int pMax=Math.max(1,menu.getProgressMax()); int p=Math.min(BAR_W-6,menu.getProgress()*(BAR_W-6)/pMax);
        if(p>0){gg.fill(x+PROCESS_X+3,y+BAR_Y+3,x+PROCESS_X+3+p,y+BAR_Y+BAR_H-3,ENERGY_MAIN); gg.fill(x+PROCESS_X+3,y+BAR_Y+4,x+PROCESS_X+3+p,y+BAR_Y+6,ENERGY_BRIGHT);}        
        gg.fill(x+10,y+151,x+PANEL_WIDTH-10,y+152,0xFF171B1F); gg.fill(x+10,y+152,x+PANEL_WIDTH-10,y+153,0xFF4B5359);
        for(int r=0;r<3;r++) for(int c=0;c<9;c++) drawSlot(gg,x+INVENTORY_X+c*INVENTORY_STEP,y+INVENTORY_Y+r*INVENTORY_STEP,INVENTORY_SLOT);
        for(int c=0;c<9;c++) drawSlot(gg,x+INVENTORY_X+c*INVENTORY_STEP,y+HOTBAR_Y,INVENTORY_SLOT);
        drawGear(gg,x+GEAR_X,y+GEAR_Y,sidePanelOpen);
        if(sidePanelOpen){drawPanel(gg,x+SIDE_PANEL_X,y,SIDE_PANEL_WIDTH,SIDE_PANEL_HEIGHT,0xFF252B2F); drawSideModel(gg,mouseX,mouseY);}    }

    @Override protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        drawClamped(gg,title,10,8,PANEL_WIDTH-20,0xFFE0E4E6);
        drawClamped(gg,Component.translatable("gui.domesurvival.oxygen_electrolyzer.energy_section"),14,24,PANEL_WIDTH-28,0xFFC5CBCD);
        drawCentered(gg,Component.translatable("gui.domesurvival.oxygen_electrolyzer.energy_compact",compact(menu.getEnergyStored()),compact(menu.getEnergyCapacity())),ENERGY_VALUE_X+3,ENERGY_VALUE_Y+4,ENERGY_VALUE_W-6,ENERGY_BRIGHT);
        drawClamped(gg,Component.translatable("gui.domesurvival.oxygen_electrolyzer.consumption",OxygenElectrolyzerBlockEntity.ENERGY_PER_TICK),42,58,166,0xFFC1C7CA);
        drawClamped(gg,Component.translatable("gui.domesurvival.oxygen_electrolyzer.cycle",OxygenElectrolyzerBlockEntity.PROCESS_TICKS/20.0D),42,70,166,0xFFC1C7CA);
        drawCentered(gg,Component.translatable("gui.domesurvival.oxygen_electrolyzer.water_short"),WATER_X,100,BAR_W,0xFF8CE0E6);
        drawCentered(gg,Component.translatable("gui.domesurvival.oxygen_electrolyzer.process_short"),PROCESS_X,100,BAR_W,ENERGY_BRIGHT);
        drawCentered(gg,Component.translatable("gui.domesurvival.oxygen_electrolyzer.oxygen_short"),OXYGEN_X,100,BAR_W,0xFFE4E8EA);
        drawClamped(gg,getStatusText(),14,132,PANEL_WIDTH-28,getStatusColor());
        drawClamped(gg,playerInventoryTitle,14,141,126,0xFFC5CBCD);
        if(sidePanelOpen) drawCentered(gg,Component.translatable("gui.domesurvival.side_config"),SIDE_PANEL_X+4,8,SIDE_PANEL_WIDTH-8,0xFFE0E4E6);
    }

    private Component getStatusText(){return switch(menu.getStatus()){case OxygenElectrolyzerBlockEntity.STATUS_RUNNING -> Component.translatable("gui.domesurvival.oxygen_electrolyzer.status.running");case OxygenElectrolyzerBlockEntity.STATUS_NO_WATER -> Component.translatable("gui.domesurvival.oxygen_electrolyzer.status.no_water");case OxygenElectrolyzerBlockEntity.STATUS_NO_ENERGY -> Component.translatable("gui.domesurvival.oxygen_electrolyzer.status.no_energy");case OxygenElectrolyzerBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("gui.domesurvival.oxygen_electrolyzer.status.output_full");default -> Component.translatable("gui.domesurvival.oxygen_electrolyzer.status.idle");};}
    private int getStatusColor(){return menu.getStatus()==OxygenElectrolyzerBlockEntity.STATUS_RUNNING?OXYGEN_BRIGHT:0xFFA9B1B5;}
    private void drawSideModel(GuiGraphics gg,int mx,int my){for(RelativeSide s:RelativeSide.values()){Rect r=SIDE_RECTS.get(s);boolean h=s!=RelativeSide.FRONT&&r.contains(mx,my,leftPos,topPos);drawFace(gg,r,s,menu.getSideMode(machineSideForVisualSide(s)),h);}}
    private void drawFace(GuiGraphics gg,Rect r,RelativeSide s,SideMode mode,boolean hover){int x=leftPos+r.x,y=topPos+r.y;int outer=hover?ENERGY_MAIN:0xFF0E1214,rim=hover?0xFF5A5140:0xFF3D454A,face=s==RelativeSide.FRONT?0xFF20262A:0xFF252B2F;gg.fill(x,y,x+r.w,y+r.h,outer);gg.fill(x+1,y+1,x+r.w-1,y+r.h-1,rim);gg.fill(x+2,y+2,x+r.w-2,y+r.h-2,face);if(s==RelativeSide.FRONT){gg.fill(x+4,y+5,x+r.w-4,y+r.h-4,0xFF121719);gg.fill(x+6,y+8,x+r.w-6,y+r.h-5,OXYGEN_MAIN);gg.fill(x+6,y+6,x+r.w-6,y+7,0xFFE7F5FF);return;}int u=switch(mode){case INPUT->PORT_INPUT_U;case OUTPUT,BOTH->PORT_OUTPUT_U;case DISABLED->PORT_OFF_U;};gg.blit(PORT_TEXTURE,x+Math.max(1,(r.w-PORT_SIZE)/2),y+Math.max(1,(r.h-PORT_SIZE)/2),u,0,PORT_SIZE,PORT_SIZE,PORT_TEX_WIDTH,PORT_TEX_HEIGHT);}    
    private void drawGear(GuiGraphics gg,int x,int y,boolean active){drawFrame(gg,x,y,GEAR_SIZE,GEAR_SIZE,active?0xFF3A3427:0xFF252B2F);int cx=x+10,cy=y+10,m=active?ENERGY_BRIGHT:0xFF687278;gg.fill(cx-5,cy-2,cx+5,cy+2,m);gg.fill(cx-2,cy-5,cx+2,cy+5,m);gg.fill(cx-4,cy-4,cx+4,cy+4,m);gg.fill(cx-2,cy-2,cx+2,cy+2,0xFF151A1D);}    
    private static void drawPanel(GuiGraphics gg,int x,int y,int w,int h,int fill){gg.fill(x,y,x+w,y+h,0xFF0C0F11);gg.fill(x+1,y+1,x+w-1,y+h-1,0xFF464E53);gg.fill(x+2,y+2,x+w-2,y+h-2,fill);gg.fill(x+3,y+3,x+w-3,y+4,0xFF50585D);gg.fill(x+3,y+h-4,x+w-3,y+h-3,0xFF50585D);}    
    private static void drawFrame(GuiGraphics gg,int x,int y,int w,int h,int fill){gg.fill(x,y,x+w,y+h,0xFF0B0E10);gg.fill(x+1,y+1,x+w-1,y+h-1,0xFF4C555A);gg.fill(x+2,y+2,x+w-2,y+h-2,fill);}    
    private static void drawSlot(GuiGraphics gg,int x,int y,int size){int inset=Math.max(2,(size-16)/2);gg.fill(x,y,x+size,y+size,0xFF0D1012);gg.fill(x+1,y+1,x+size-1,y+size-1,0xFF3E464B);gg.fill(x+inset,y+inset,x+inset+16,y+inset+16,0xFF1B2125);}    
    private static void fillBar(GuiGraphics gg,int x,int y,int w,int h,int v,int c,int main,int bright){if(v<=0||c<=0)return;int f=Math.min(w-6,(int)((long)v*(w-6)/c));if(f<=0)return;gg.fill(x+3,y+3,x+3+f,y+h-3,main);gg.fill(x+3,y+4,x+3+f,y+6,bright);}    
    private void drawClamped(GuiGraphics gg,Component c,int x,int y,int w,int color){String v=c.getString();if(font.width(v)>w){String d="...";v=font.plainSubstrByWidth(v,Math.max(0,w-font.width(d)))+d;}gg.drawString(font,v,x,y,color,false);}    
    private void drawCentered(GuiGraphics gg,Component c,int x,int y,int w,int color){String v=c.getString();if(font.width(v)>w){String d="...";v=font.plainSubstrByWidth(v,Math.max(0,w-font.width(d)))+d;}gg.drawString(font,v,x+Math.max(0,(w-font.width(v))/2),y,color,false);}    
    private static String compact(int v){if(v<1000)return Integer.toString(v);if(v%1000==0)return(v/1000)+"k";return String.format(Locale.ROOT,"%.1fk",v/1000.0D);}    
    private RelativeSide getHoveredSide(double mx,double my){for(RelativeSide s:RelativeSide.values()){if(s==RelativeSide.FRONT)continue;Rect r=SIDE_RECTS.get(s);if(r.contains(mx,my,leftPos,topPos))return s;}return null;}    
    private static Component getSideModeTooltip(SideMode mode){return switch(mode){case INPUT->Component.translatable("gui.domesurvival.side_state.input");case OUTPUT,BOTH->Component.translatable("gui.domesurvival.side_state.output");case DISABLED->Component.translatable("gui.domesurvival.side_state.disabled");};}
    private record Rect(int x,int y,int w,int h){boolean contains(double mx,double my,int lp,int tp){double lx=mx-lp,ly=my-tp;return lx>=x&&lx<x+w&&ly>=y&&ly<y+h;}}
}
