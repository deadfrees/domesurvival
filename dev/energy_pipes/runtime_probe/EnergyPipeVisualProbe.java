package com.wasted.domesurvival.energyprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.transport.energy.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Test-only integration fixture. Guarded to an isolated disposable client directory. */
@Mod.EventBusSubscriber(modid="domesurvival", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class EnergyPipeVisualProbe {
    private static final boolean ENABLED=Boolean.getBoolean("dome.energyPipeProbe");
    private static final boolean HAND_REVIEW=Boolean.getBoolean("dome.energyPipeHandReview");
    private static final Path OUT=Path.of("../../dev/energy_pipes/"+(HAND_REVIEW?"runtime_tier1_item_review":"runtime_tier1_release")).toAbsolutePath().normalize();
    private static final BlockPos SOURCE=new BlockPos(24,200,10), SINK=new BlockPos(29,200,10), CUT=new BlockPos(26,200,10);
    private static final List<BlockPos> PIPES=new ArrayList<>();
    private static boolean started,fixture;
    private static volatile boolean ready,reloading,serverDone;
    private static int age,tick,stage,failures,stoppedEnergy,previousEnergy;
    private static List<String> packs;
    private static String sampling="";
    private static final List<Double> frames=new ArrayList<>();
    private static long previousFrame;

    private static void log(String text) {
        System.out.println("[ENERGY_PIPE_PROBE] "+text);
        try {Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static synchronized void check(boolean ok,String text){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+text);}
    private static void pipe(ServerLevel level,BlockPos pos,Block block){level.setBlockAndUpdate(pos,block.defaultBlockState());PIPES.add(pos);}
    private static void pipe(ServerLevel level,int x,int y,int z){pipe(level,new BlockPos(x,y,z),ModBlocks.BASIC_ENERGY_PIPE.get());}
    private static int connections(BlockState state){int n=0;for(var e:state.getValues().entrySet())if(e.getKey() instanceof BooleanProperty&&Boolean.TRUE.equals(e.getValue()))n++;return n;}
    private static void place(ServerPlayer player,BlockPos pos){
        ItemStack held=player.getMainHandItem();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.BASIC_ENERGY_PIPE.get()));
        player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos.below(),false)));
        player.setItemInHand(InteractionHand.MAIN_HAND,held);
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!ENABLED||event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("energy-pipe-visual"))throw new IllegalStateException("Isolated directory required");
        ServerLevel level=server.overworld();ServerPlayer player=server.getPlayerList().getPlayers().get(0);
        if(!fixture){
            fixture=true;level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-5;x<=39;x++)for(int z=-5;z<=15;z++)level.setBlockAndUpdate(new BlockPos(x,199,z),Blocks.SMOOTH_STONE.defaultBlockState());
            // 100 basic pipes, five parallel 20-block lines.
            for(int x=0;x<20;x++)for(int row=0;row<5;row++)pipe(level,x,200,row*2);
            pipe(level,22,200,0);
            for(int y=200;y<=203;y++)pipe(level,25,y,0);
            pipe(level,29,200,0);pipe(level,30,200,0);pipe(level,29,200,1);
            pipe(level,34,200,0);pipe(level,33,200,0);pipe(level,35,200,0);pipe(level,34,200,1);
            pipe(level,22,200,5);for(Direction d:Direction.Plane.HORIZONTAL)pipe(level,new BlockPos(22,200,5).relative(d),ModBlocks.BASIC_ENERGY_PIPE.get());
            pipe(level,27,202,5);for(Direction d:Direction.values())pipe(level,new BlockPos(27,202,5).relative(d),ModBlocks.BASIC_ENERGY_PIPE.get());
            pipe(level,32,200,5);pipe(level,new BlockPos(33,200,5),ModBlocks.REINFORCED_ENERGY_PIPE.get());pipe(level,new BlockPos(31,200,5),ModBlocks.HIGH_VOLTAGE_ENERGY_PIPE.get());
            level.setBlockAndUpdate(SOURCE,ModBlocks.ENERGY_BUFFER.get().defaultBlockState());
            level.setBlockAndUpdate(SINK,ModBlocks.ENERGY_BUFFER.get().defaultBlockState());
            for(int x=25;x<29;x++)pipe(level,x,200,10);
            for(BlockPos pos:PIPES)level.setBlockAndUpdate(pos,EnergyPipeBlock.refreshConnections(level,pos,level.getBlockState(pos)));
            var source=(EnergyBufferBlockEntity)level.getBlockEntity(SOURCE);
            CompoundTag data=source.saveWithoutMetadata();data.putInt("Energy",100000);source.load(data);source.setChanged();
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,6.5,200,-3.5,0,12);
            player.getInventory().setItem(0,new ItemStack(ModBlocks.BASIC_ENERGY_PIPE.get(),64));
            player.getInventory().setItem(1,new ItemStack(ModBlocks.REINFORCED_ENERGY_PIPE.get()));
            player.getInventory().setItem(2,new ItemStack(ModBlocks.HIGH_VOLTAGE_ENERGY_PIPE.get()));
            BlockPos placed=new BlockPos(36,200,12);place(player,placed);
            check(level.getBlockState(placed).is(ModBlocks.BASIC_ENERGY_PIPE.get()),"Actual BlockItem placement");
            check(player.gameMode.destroyBlock(placed)&&level.isEmptyBlock(placed),"Actual player break");
            check(connections(level.getBlockState(new BlockPos(22,200,0)))==0,"Isolated node");
            check(connections(level.getBlockState(new BlockPos(25,201,0)))==2,"Vertical UP/DOWN");
            check(connections(level.getBlockState(new BlockPos(29,200,0)))==2,"Corner");
            check(connections(level.getBlockState(new BlockPos(34,200,0)))==3,"T junction");
            check(connections(level.getBlockState(new BlockPos(22,200,5)))==4,"Cross junction");
            check(connections(level.getBlockState(new BlockPos(27,202,5)))==6,"Six-direction junction");
            check(connections(level.getBlockState(new BlockPos(32,200,5)))==2,"Mixed tiers connect");
            check(connections(level.getBlockState(new BlockPos(25,200,10)))==2,"Pipe connects to actual energy buffer port");
            ready=true;log("Fixture ready: 100-pipe benchmark plus junction/FE fixtures");
        }
        age++;
        var source=(EnergyBufferBlockEntity)level.getBlockEntity(SOURCE);
        var sink=(EnergyBufferBlockEntity)level.getBlockEntity(SINK);
        if(age>=2&&age<=15){
            int received=sink.getEnergyStored()-previousEnergy;
            check(received>=0&&received<=256,"FE network tick "+age+" transfer="+received+" <= 256");
            check(source.getEnergyStored()+sink.getEnergyStored()==100000,"FE conservation tick "+age);
        }
        previousEnergy=sink.getEnergyStored();
        if(age==20)check(sink.getEnergyStored()>0,"Real FE transfer reaches sink");
        if(age==30){((EnergyPipeBlockEntity)level.getBlockEntity(CUT)).setSideMode(Direction.EAST,EnergyPipeSideMode.DISABLED);stoppedEnergy=sink.getEnergyStored();}
        if(age==40){check(sink.getEnergyStored()==stoppedEnergy,"Disabled side stops FE");((EnergyPipeBlockEntity)level.getBlockEntity(CUT)).setSideMode(Direction.EAST,EnergyPipeSideMode.AUTO);}
        if(age==50){check(sink.getEnergyStored()>stoppedEnergy,"Re-enabled side resumes FE");player.gameMode.destroyBlock(CUT);stoppedEnergy=sink.getEnergyStored();}
        if(age==60){check(sink.getEnergyStored()==stoppedEnergy,"Broken line stops FE");place(player,CUT);check(level.getBlockState(CUT).is(ModBlocks.BASIC_ENERGY_PIPE.get()),"Actual BlockItem reconnects line");}
        if(age==70){check(sink.getEnergyStored()>stoppedEnergy,"Replaced pipe resumes FE");serverDone=true;}
    }

    private static void camera(double x,double y,double z,float yaw,float pitch){Minecraft mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{var s=mc.getSingleplayerServer();s.getPlayerList().getPlayers().get(0).teleportTo(s.overworld(),x,y,z,yaw,pitch);});}
    private static void shot(String name){Minecraft mc=Minecraft.getInstance();check(name.contains("inventory")?(mc.screen instanceof InventoryScreen||mc.screen instanceof CreativeModeInventoryScreen):mc.screen==null,"Capture screen context "+name);try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(OUT);image.writeToFile(OUT.resolve(name));log("Screenshot "+name);}catch(Exception e){check(false,"Screenshot failed "+e);}}
    private static void pack(boolean old){
        Minecraft mc=Minecraft.getInstance();var repo=mc.getResourcePackRepository();repo.reload();var chosen=new ArrayList<>(packs);
        if(old)chosen.add("file/energy_pipe_before");repo.setSelected(chosen);
        check(repo.getSelectedIds().contains("file/energy_pipe_before")==old,"Comparison pack selected="+old);
        reloading=true;mc.reloadResourcePacks().whenComplete((unused,error)->{if(error!=null)check(false,"Resource reload: "+error);reloading=false;});
    }
    private static void measure(String name){sampling=name;frames.clear();previousFrame=0;}
    private static void finishMeasure(){
        String name=sampling;sampling="";var sorted=new ArrayList<>(frames);Collections.sort(sorted);
        check(sorted.size()>100,"Frame sample size "+name+"="+sorted.size());
        if(sorted.isEmpty())return;
        double mean=sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double median=sorted.get(sorted.size()/2),p95=sorted.get(Math.min(sorted.size()-1,(int)(sorted.size()*.95)));
        String line=String.format(Locale.ROOT,"%s,%d,%.4f,%.4f,%.4f,%.2f%n",name,sorted.size(),mean,median,p95,1000/mean);
        try{Files.writeString(OUT.resolve("frame_times.csv"),line,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}
        log("FRAME_METRICS name,frames,mean_ms,median_ms,p95_ms,mean_fps: "+line.trim());
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||sampling.isEmpty())return;
        long now=System.nanoTime();if(previousFrame!=0)frames.add((now-previousFrame)/1_000_000.0);previousFrame=now;
    }
    private static void bakedModels(){
        Minecraft mc=Minecraft.getInstance();int verified=0;
        for(BlockState state:ModBlocks.BASIC_ENERGY_PIPE.get().getStateDefinition().getPossibleStates()){
            var model=mc.getBlockRenderer().getBlockModel(state);var quads=model.getQuads(state,null,RandomSource.create(1));
            boolean valid=quads.size()==36+26*connections(state)&&quads.stream().noneMatch(q->q.getSprite().contents().name().toString().contains("missingno"));
            if(valid)verified++;
        }
        check(verified==64,"All 64 Tier 1 baked states have correct quads and valid sprites");
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            started=true;log("Creating fresh isolated world");mc.options.pauseOnLostFocus=false;mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.guiScale().set(2);mc.options.fov().set(60);mc.options.renderDistance().set(6);
            mc.options.bobView().set(false);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(260);
            mc.createWorldOpenFlows().createFreshLevel("energy_pipe_tier1_"+System.currentTimeMillis(),
                new LevelSettings("Energy pipe visual test",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;
        }
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null||reloading)return;tick++;
        if(stage==0&&tick>100){mc.setScreen(null);mc.options.hideGui=true;bakedModels();packs=new ArrayList<>(mc.getResourcePackRepository().getSelectedIds());stage=HAND_REVIEW?20:1;tick=0;
            if(HAND_REVIEW){camera(38,201,0,0,10);mc.options.hideGui=false;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}}
        else if(stage==1&&tick>50){shot("01_tier1_close.png");camera(28,204,-6,0,23);stage++;tick=0;}
        else if(stage==2&&tick>60){shot("02_junctions.png");camera(26.5,201,6,0,12);stage++;tick=0;}
        else if(stage==3&&tick>60){shot("03_machine_connection.png");mc.options.hideGui=false;stage++;tick=0;}
        else if(stage==4&&tick>40){shot("04_first_person.png");mc.setScreen(new InventoryScreen(mc.player));stage++;tick=0;}
        else if(stage==5&&tick>40){shot("05_inventory.png");mc.player.closeContainer();mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);stage++;tick=0;}
        else if(stage==6&&tick>40){shot("06_third_person.png");mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;
            mc.getSingleplayerServer().execute(()->{var level=mc.getSingleplayerServer().overworld();level.setBlockAndUpdate(new BlockPos(26,201,7),Blocks.SMOOTH_STONE.defaultBlockState());
                var frame=new ItemFrame(level,new BlockPos(26,201,6),Direction.NORTH);frame.setItem(new ItemStack(ModBlocks.BASIC_ENERGY_PIPE.get()));level.addFreshEntity(frame);
                var drop=new ItemEntity(level,25.5,200,6.5,new ItemStack(ModBlocks.BASIC_ENERGY_PIPE.get()));drop.setPickUpDelay(32767);level.addFreshEntity(drop);});
            camera(26,200,3,0,-2);stage++;tick=0;}
        else if(stage==7&&tick>60){shot("07_ground_and_frame.png");camera(10,205,-10,0,22);pack(true);stage++;tick=0;}
        else if(stage==8&&tick>120){shot("08_stress_before.png");measure("before");stage++;tick=0;}
        else if(stage==9&&tick>200){finishMeasure();pack(false);stage++;tick=0;}
        else if(stage==10&&tick>120){shot("09_stress_after.png");bakedModels();measure("after");stage++;tick=0;}
        else if(stage==11&&tick>200){finishMeasure();mc.options.mainHand().set(HumanoidArm.LEFT);mc.options.broadcastOptions();mc.options.hideGui=false;camera(26.5,201,6,0,12);stage++;tick=0;}
        else if(stage==12&&tick>50){shot("10_first_person_left.png");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);stage++;tick=0;}
        else if(stage==13&&tick>50){shot("11_third_person_left.png");check(serverDone,"FE regression stages completed");log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);stage++;tick=0;}
        else if(stage==14&&tick>20){mc.stop();stage++;}
        else if(stage==20&&tick>80){shot("01_third_person_right.png");mc.options.mainHand().set(HumanoidArm.LEFT);mc.options.broadcastOptions();stage++;tick=0;}
        else if(stage==21&&tick>60){shot("02_third_person_left.png");mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;
            mc.getSingleplayerServer().execute(()->{var level=mc.getSingleplayerServer().overworld();level.setBlockAndUpdate(new BlockPos(38,201,5),Blocks.SMOOTH_STONE.defaultBlockState());var frame=new ItemFrame(level,new BlockPos(38,201,4),Direction.NORTH);frame.setItem(new ItemStack(ModBlocks.BASIC_ENERGY_PIPE.get()));level.addFreshEntity(frame);});
            camera(38.5,200.5,2,0,0);stage++;tick=0;}
        else if(stage==22&&tick>70){shot("03_fixed_frame.png");log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);stage=14;tick=0;}
    }
}
