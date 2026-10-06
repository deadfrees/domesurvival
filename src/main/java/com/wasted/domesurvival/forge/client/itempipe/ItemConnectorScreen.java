package com.wasted.domesurvival.forge.client.itempipe;

import com.wasted.domesurvival.forge.itempipe.ItemConnectorMenu;
import com.wasted.domesurvival.forge.itempipe.ItemConnectorMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Own controls keep modes legible when a resource pack reskins vanilla buttons. */
public final class ItemConnectorScreen extends AbstractContainerScreen<ItemConnectorMenu> {
    private static final ResourceLocation PANEL=new ResourceLocation("domesurvival","textures/gui/item_connector_v2/panel.png");
    private static final ResourceLocation WIDGETS=new ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png");
    public ItemConnectorScreen(ItemConnectorMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=236;imageHeight=176;inventoryLabelY=1000;
    }
    private static Component modeName(ItemConnectorMode mode){return Component.translatable("gui.domesurvival.item_pipe.mode."+mode.id());}
    private static Component help(ItemConnectorMode mode){return Component.translatable("gui.domesurvival.item_pipe.connector_v2.help."+mode.id());}
    private static int color(ItemConnectorMode mode){return switch(mode){case INPUT->0xFF83B8D2;case OUTPUT->0xFFE0A267;case DISABLED->0xFF98A5AB;};}
    @Override protected void init(){
        super.init();
        addMode(ItemConnectorMode.INPUT,12);addMode(ItemConnectorMode.OUTPUT,85);addMode(ItemConnectorMode.DISABLED,158);
    }
    private void addMode(ItemConnectorMode mode,int x){
        Button button=new Button(leftPos+x,topPos+90,66,28,modeName(mode),ignored->{
            if(menu.mode()!=mode&&minecraft!=null&&minecraft.gameMode!=null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,mode.ordinal());
        },message->message.get()){
            @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
                boolean selected=menu.mode()==mode;
                if(isHoveredOrFocused())g.fill(getX()+2,getY()+2,getX()+64,getY()+26,0x223F6878);
                if(selected||isHoveredOrFocused())g.renderOutline(getX()+1,getY()+1,64,26,color(mode));
                g.fill(getX()+7,getY()+5,getX()+59,getY()+7,selected?color(mode):0xFF49565C);
                String label=getMessage().getString();
                g.drawString(font,label,getX()+(66-font.width(label))/2,getY()+12,selected?color(mode):0xFFCAD2D4,false);
            }
        };
        button.setTooltip(Tooltip.create(help(mode)));addRenderableWidget(button);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.blit(PANEL,leftPos,topPos,236,176,0,0,944,704,944,704);
        int u=switch(menu.mode()){case INPUT->20;case OUTPUT->40;case DISABLED->0;};
        g.blit(WIDGETS,leftPos+19,topPos+41,20,20,u*4F,96,80,80,512,256);
    }
    private void text(GuiGraphics g,Component value,int x,int y,int width,int color){
        String s=value.getString();if(font.width(s)>width)s=font.plainSubstrByWidth(s,width-font.width("..."))+"...";
        g.drawString(font,s,x,y,color,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        text(g,title,13,10,210,0xFF251C13);
        text(g,Component.translatable("gui.domesurvival.item_pipe.side",Component.translatable("message.domesurvival.item_pipe.side."+menu.side().getName())),50,34,169,0xFFCAD2D4);
        text(g,Component.translatable("gui.domesurvival.item_pipe.connector_v2.rate",menu.itemsPerCycle(),menu.cooldownTicks()),50,48,169,0xFF98A5AB);
        text(g,Component.translatable("gui.domesurvival.item_pipe.connector_v2.selected",modeName(menu.mode())),50,63,169,color(menu.mode()));
        g.drawWordWrap(font,help(menu.mode()),16,134,204,0xFFCAD2D4);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
}
