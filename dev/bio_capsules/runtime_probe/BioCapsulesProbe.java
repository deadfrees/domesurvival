package com.wasted.domesurvival.capsuleprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.client.BioModuleModels;
import com.wasted.domesurvival.forge.item.*;
import com.wasted.domesurvival.forge.bio.BioLootData;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class BioCapsulesProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.bioCapsulesReview");
    static final Path OUT=Path.of("../../dev/bio_capsules/runtime").toAbsolutePath().normalize();
    static final String[] SPECIES="cow pig sheep chicken rabbit horse donkey llama goat camel wolf cat ocelot fox bee panda turtle axolotl frog mooshroom sniffer strider hoglin mule parrot polar_bear".split(" ");
    static boolean started;static int frames,phase,failures;static Gallery gallery;
    static void log(String s){System.out.println("[CAPSULE_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    static void check(boolean ok,String text){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+text);}
    static ItemStack capsule(String species,boolean damaged){return BioModuleItem.create(new ResourceLocation("minecraft",species),damaged);}
    static BakedModel model(ItemStack s){return Minecraft.getInstance().getItemRenderer().getModel(s,null,null,0);}
    static void validate(){
        var mc=Minecraft.getInstance();var unique=Collections.newSetFromMap(new IdentityHashMap<BakedModel,Boolean>());
        for(int d=0;d<2;d++)for(int i=0;i<SPECIES.length;i++){
            var s=capsule(SPECIES[i],d==1);var before=s.getTag().copy();var m=model(s);
            check(BioModuleModels.variant(s)==(i+1+26*d)/64F,"Stable predicate "+SPECIES[i]+"/"+d);
            check(m!=mc.getModelManager().getMissingModel()&&unique.add(m),"Distinct baked model "+SPECIES[i]+"/"+d);
            var quads=new ArrayList<>(m.getQuads(null,null,RandomSource.create(0)));
            for(var face:Direction.values())quads.addAll(m.getQuads(null,face,RandomSource.create(0)));
            check(!quads.isEmpty()&&quads.stream().noneMatch(q->q.getSprite().contents().name().getPath().equals("missingno")),"All textures resolved "+SPECIES[i]+"/"+d);
            check(before.equals(s.getTag()),"Rendering preserves NBT "+SPECIES[i]+"/"+d);
        }
        var empty=new ItemStack(ModItems.BIO_MODULE.get());
        var custom=BioModuleItem.create(new ResourceLocation("othermod","pig"),true);
        var invalid=empty.copy();invalid.getOrCreateTag().putString("EntityId","invalid id!");
        check(model(empty)==model(custom)&&model(empty)==model(invalid)&&!unique.contains(model(empty)),"Unknown and malformed IDs use neutral fallback");
        var old=List.of(ModItems.COW_CRYOCAPSULE.get(),ModItems.SHEEP_CRYOCAPSULE.get(),ModItems.CHICKEN_CRYOCAPSULE.get(),ModItems.DAMAGED_PIG_CRYOCAPSULE.get());
        for(var item:old)check(model(new ItemStack(item))!=mc.getModelManager().getMissingModel(),"Legacy capsule model "+item);
        var mutable=capsule("cow",true);var damaged=model(mutable);mutable.getOrCreateTag().putBoolean("Damaged",false);
        check(model(mutable)==model(capsule("cow",false))&&model(mutable)!=damaged,"Repair switches appearance on same stack");
        check(unique.size()==52,"All 52 variants distinct");
    }
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception e){check(false,e.toString());}}
    static class Gallery extends Screen {
        int page;
        Gallery(){super(Component.literal("Bio capsule review"));}
        @Override public void render(GuiGraphics g,int mx,int my,float dt){
            g.fill(0,0,width,height,0xFF151C20);
            g.drawCenteredString(font,"BIO CAPSULES / "+(page==2?"HAND · GROUND · FRAME":page==1?"INVENTORY 16 PX":"26 SPECIES / VIABLE + DAMAGED"),width/2,10,0xE3E7E8);
            if(page<2){
                for(int i=0;i<SPECIES.length;i++){
                    int x=10+(i%7)*100,y=35+(i/7)*96;
                    g.fill(x,y,x+94,y+88,0xFF263137);
                    g.drawCenteredString(font,SPECIES[i],x+47,y+4,0xC5D1D5);
                    float scale=page==0?2:1;
                    for(int d=0;d<2;d++){
                        g.pose().pushPose();g.pose().translate(x+10+d*43+(page==1?8:0),y+23,0);g.pose().scale(scale,scale,1);
                        g.renderItem(capsule(SPECIES[i],d==1),0,0);g.pose().popPose();
                    }
                }
            }else{
                ItemDisplayContext[] contexts={ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.GROUND,ItemDisplayContext.FIXED};
                for(int row=0;row<4;row++)for(int col=0;col<4;col++){
                    int x=100+col*165,y=75+row*90;
                    if(row==0)g.drawCenteredString(font,contexts[col].name().replace("_RIGHT_HAND",""),x,y-30,0xC5D1D5);
                    var pose=g.pose();pose.pushPose();pose.translate(x,y,150);pose.scale(65,-65,65);
                    Minecraft.getInstance().getItemRenderer().renderStatic(capsule(new String[]{"cow","fox","axolotl","rabbit"}[row],row==3),contexts[col],15728880,OverlayTexture.NO_OVERLAY,pose,g.bufferSource(),null,0);
                    g.flush();pose.popPose();
                }
            }
        }
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.getOverlay()==null&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            started=true;mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(60);mc.resizeDisplay();
            try{validate();gallery=new Gallery();mc.setScreen(gallery);}catch(Throwable ex){check(false,"Exception "+ex);ex.printStackTrace();mc.stop();}
        }
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(!ENABLED||!started||gallery==null||e.phase!=TickEvent.Phase.END||Minecraft.getInstance().getOverlay()!=null)return;
        if(++frames<60)return;frames=0;var mc=Minecraft.getInstance();
        if(phase==0){shot("01_capsules");gallery.page=1;}
        if(phase==1){shot("02_inventory_size");gallery.page=2;}
        if(phase==2){shot("03_display_contexts");mc.reloadResourcePacks();}
        if(phase==3){validate();log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();}
        phase++;
    }
}
