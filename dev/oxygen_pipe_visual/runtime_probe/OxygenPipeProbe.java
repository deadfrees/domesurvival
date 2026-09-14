package com.wasted.domesurvival.oxygenpipeprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.capability.*;
import com.wasted.domesurvival.forge.machine.oxygen.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Disposable capability sources and fixture, compiled only by oxygen_test.init.gradle. */
@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class OxygenPipeProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.oxygenPipeReview");
    static final Path OUT=Path.of("../../dev/oxygen_pipe_visual/runtime").toAbsolutePath().normalize();
    static boolean started,fixture,planned;static volatile boolean ready,pauseTransfer;static int age,ticks,step,failures;
    static final List<Step> steps=new ArrayList<>();record Step(int delay,Runnable action){}
    static Block block(int i){return switch(i){case 0->ModBlocks.OXYGEN_PIPE.get();case 1->ModBlocks.REINFORCED_OXYGEN_PIPE.get();default->ModBlocks.HIGH_FLOW_OXYGEN_PIPE.get();};}
    static BlockPos pos(int x,int i){return new BlockPos(x,100,i*4);}
    static synchronized void log(String s){System.out.println("[OXYGEN_PIPE_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    @SubscribeEvent public static void capability(AttachCapabilitiesEvent<BlockEntity> event){
        if(!ENABLED||!(event.getObject() instanceof ChestBlockEntity))return;
        var storage=new OxygenStorage(10000,10000,10000);LazyOptional<IOxygenStorage> optional=LazyOptional.of(()->storage);
        event.addCapability(new ResourceLocation("domesurvival","oxygen_review_source"),new ICapabilityProvider(){
            @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ModCapabilities.OXYGEN?optional.cast():LazyOptional.empty();}
        });event.addListener(optional::invalidate);
    }
    static OxygenStorage storage(ServerLevel level,BlockPos p){return (OxygenStorage)level.getBlockEntity(p).getCapability(ModCapabilities.OXYGEN,Direction.WEST).orElseThrow(()->new IllegalStateException("Missing review oxygen capability"));}
    static void refresh(ServerLevel level,BlockPos p){var s=level.getBlockState(p);if(s.getBlock() instanceof OxygenPipeBlock)level.setBlockAndUpdate(p,OxygenPipeBlock.refreshConnections(level,p,s));}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!ENABLED||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("oxygen-pipe-review"))throw new IllegalStateException("Isolated directory required");
        var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(!fixture){fixture=true;level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-3;x<20;x++)for(int z=-5;z<14;z++)level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,3.5,102,-5,0,24);
            for(int i=0;i<3;i++){
                for(int x=0;x<8;x++)level.setBlockAndUpdate(pos(x,i),block(i).defaultBlockState());
                level.setBlockAndUpdate(pos(-1,i),Blocks.CHEST.defaultBlockState());level.setBlockAndUpdate(pos(8,i),Blocks.CHEST.defaultBlockState());
                for(int x=0;x<8;x++)refresh(level,pos(x,i));
                player.getInventory().setItem(i,new ItemStack(block(i),64));
                for(Direction d:Direction.values())level.setBlockAndUpdate(new BlockPos(12,102,i*4).relative(d),block(i).defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(12,102,i*4),block(i).defaultBlockState());
                for(int y=100;y<104;y++)level.setBlockAndUpdate(new BlockPos(16,y,i*4),block(i).defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(17,100,i*4),block(i).defaultBlockState());
                check(level.getBlockEntity(pos(3,i))==null,"Tier "+i+" still has no ticking block entity");
            }ready=true;return;
        }
        age++;
        for(int i=0;i<3;i++){
            if(age==20||age==30){OxygenPipeConnectionData.get(level).toggle(pos(4,i),Direction.EAST);refresh(level,pos(4,i));refresh(level,pos(5,i));}
            if(age==20){var data=OxygenPipeConnectionData.load(OxygenPipeConnectionData.get(level).save(new CompoundTag()));check(data.isDisconnected(pos(4,i),Direction.EAST),"Tier "+i+" wrench disconnect survives NBT round trip");}
            if(age==40||age==50){level.setBlockAndUpdate(pos(4,i),block(age==40?0:i).defaultBlockState());refresh(level,pos(4,i));}
            if(age==60)level.removeBlock(pos(4,i),false);
            if(age==70){level.setBlockAndUpdate(pos(4,i),block(i).defaultBlockState());refresh(level,pos(4,i));}
            var src=storage(level,pos(-1,i));var dst=storage(level,pos(8,i));src.setStoredInternal(10000);dst.setStoredInternal(0);
            int moved=pauseTransfer?0:OxygenPipeTransferService.pull(level,pos(8,i),dst,4096,d->d==Direction.WEST);
            if(age==5||age==25||age==35||age==45||age==55||age==65||age==75){
                int expected=(age==25||age==65)?0:age==45?30:new int[]{30,60,120}[i];
                check(moved==expected,"Tier "+i+" age "+age+" throughput expected "+expected+" actual "+moved);
                check(src.getOxygenStored()+dst.getOxygenStored()==10000,"Tier "+i+" oxygen conserved at age "+age);
            }
        }
    }
    static void camera(double x,double y,double z,float yaw,float pitch){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();server.getPlayerList().getPlayers().get(0).teleportTo(server.overworld(),x,y,z,yaw,pitch);});}
    static void shot(String name){try(NativeImage image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);image.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static void add(int n,Runnable r){steps.add(new Step(n,r));}
    static void baked(){var mc=Minecraft.getInstance();int good=0,oriented=0,shapes=0;
        for(int i=0;i<3;i++)for(BlockState state:block(i).getStateDefinition().getPossibleStates()){
            int count=0;for(Direction d:Direction.values())if(state.getValue(OxygenPipeBlock.property(d)))count++;
            var model=mc.getBlockRenderer().getBlockModel(state);
            var glass=model.getQuads(state,null,RandomSource.create(1),ModelData.EMPTY,RenderType.translucent());
            var metal=model.getQuads(state,null,RandomSource.create(1),ModelData.EMPTY,RenderType.cutout());
            if(glass.size()==6+3*count&&metal.size()==24*(i+1)*count&&java.util.stream.Stream.concat(glass.stream(),metal.stream()).noneMatch(q->q.getSprite().contents().name().toString().contains("missingno")))good++;
            var bounds=state.getShape(mc.level,BlockPos.ZERO,CollisionContext.empty()).bounds();boolean correct=true;
            for(Direction.Axis axis:Direction.Axis.values()){
                Direction negative=Direction.fromAxisAndDirection(axis,Direction.AxisDirection.NEGATIVE),positive=negative.getOpposite();
                double lo=state.getValue(OxygenPipeBlock.property(negative))?0:6.75/16.,hi=state.getValue(OxygenPipeBlock.property(positive))?1:9.25/16.;
                correct &= Math.abs(bounds.min(axis)-lo)<.0001&&Math.abs(bounds.max(axis)-hi)<.0001;
            }if(correct)shapes++;
            double[] min={2,2,2},max={-1,-1,-1};
            for(var q:java.util.stream.Stream.concat(glass.stream(),metal.stream()).toList()){
                var v=q.getVertices();for(int k=0;k<v.length;k+=8)for(int axis=0;axis<3;axis++){float n=Float.intBitsToFloat(v[k+axis]);min[axis]=Math.min(min[axis],n);max[axis]=Math.max(max[axis],n);}
            }
            boolean ok=true;for(Direction d:Direction.values())if(state.getValue(OxygenPipeBlock.property(d))){int axis=d.getAxis().ordinal();ok &= Math.abs((d.getAxisDirection()==Direction.AxisDirection.POSITIVE?max[axis]:min[axis])-(d.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0))<.0001;}
            if(ok)oriented++;
        }
        check(good==192,"192 connection combinations: correct glass and metal quads, valid sprites: "+good);
        check(oriented==192,"192 connection combinations reach the correct block faces, including vertical: "+oriented);
        check(shapes==192,"192 collision shapes match thin 2.5px collar envelope: "+shapes);
        for(int i=0;i<3;i++){var stack=new ItemStack(block(i));check(mc.getItemRenderer().getModel(stack,mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Inventory model tier "+i);}
    }
    static void gas(boolean expected){try{var c=Class.forName("com.wasted.domesurvival.forge.client.oxygen.OxygenGasFlowRenderer");var field=c.getDeclaredField("ACTIVE");field.setAccessible(true);var flows=(Map<?,?>)field.get(null);check(!flows.isEmpty()==expected,"Real oxygen gas visibility expected "+expected+", active paths "+flows.size());}catch(Exception ex){check(false,"Gas visibility "+ex);}}
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;baked();gas(true);
        add(40,()->shot("01_three_glass_tiers"));
        for(int i=0;i<3;i++){final int row=i;add(10,()->camera(3.5,100,row*4-1.9,0,30));add(45,()->shot("tier"+row+"_glass"));}
        add(10,()->camera(14,104,-4,0,25));add(50,()->shot("02_vertical_junctions"));
        add(10,()->{mc.options.hideGui=false;mc.setScreen(new InventoryScreen(mc.player));});add(30,()->shot("03_inventory"));
        add(10,()->{mc.player.closeContainer();mc.setScreen(null);mc.player.getInventory().selected=0;});add(30,()->shot("04_in_hand"));
        add(10,()->{mc.options.hideGui=true;camera(3.5,100,6.1,0,30);});add(40,()->{gas(true);shot("05_active_gas");pauseTransfer=true;});add(25,()->{gas(false);shot("06_stopped_gas");pauseTransfer=false;});add(25,()->{gas(true);shot("07_resumed_gas");});
        add(10,()->log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures));add(20,mc::stop);
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!ENABLED||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);
            mc.createWorldOpenFlows().createFreshLevel("oxygen_glass_review_"+System.currentTimeMillis(),new LevelSettings("Oxygen glass review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>140){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;steps.get(step++).action().run();}
    }
}
