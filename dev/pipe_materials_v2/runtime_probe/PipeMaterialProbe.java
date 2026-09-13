package com.wasted.domesurvival.materialprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.storage.tank.*;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.side.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;
import com.wasted.domesurvival.forge.transport.fluid.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.decoration.ItemFrame;
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

/** Only loaded by the isolated development init script. No production registration. */
@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class PipeMaterialProbe {
    private static final boolean ENABLED=Boolean.getBoolean("dome.pipeMaterialsV2");
    private static final Path OUT=Path.of("../../dev/pipe_materials_v2/runtime").toAbsolutePath().normalize();
    private static final String PACK="file/pipe_materials_v2_before";
    private static final List<BlockPos> pipes=new ArrayList<>();
    private static final List<Step> steps=new ArrayList<>();
    private static final List<Double> frames=new ArrayList<>();
    private static List<String> packs;
    private static boolean started,fixture,planned;
    private static volatile boolean ready,reloading,serverDone;
    private static int age,ticks,step,failures;
    private static String sampling="";
    private static long lastFrame;
    private record Step(int ticks,Runnable action){}
    private static Block block(int tier){return switch(tier){case 1->FluidPipeRegistry.BASIC_FLUID_PIPE.get();case 2->FluidPipeRegistry.REINFORCED_FLUID_PIPE.get();default->FluidPipeRegistry.HIGH_PRESSURE_FLUID_PIPE.get();};}
    private static Block energy(int tier){return switch(tier){case 1->ModBlocks.BASIC_ENERGY_PIPE.get();case 2->ModBlocks.REINFORCED_ENERGY_PIPE.get();default->ModBlocks.HIGH_VOLTAGE_ENERGY_PIPE.get();};}
    private static int zone(int tier){return (tier-1)*12;}
    private static BlockPos source(int tier){return new BlockPos(35,200,zone(tier)+8);}
    private static BlockPos sink(int tier){return new BlockPos(40,200,zone(tier)+8);}
    private static BlockPos cut(int tier){return new BlockPos(37,200,zone(tier)+8);}
    private static IFluidHandler tank(ServerLevel level,BlockPos pos){return level.getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER,null).orElseThrow(()->new IllegalStateException("Tank capability missing"));}
    private static void tankMode(ServerLevel level,BlockPos pos,Direction dir,SideMode mode){var tank=(UniversalTankBlockEntity)level.getBlockEntity(pos);for(var relative:RelativeSide.values())if(UniversalTankBlockEntity.resolveRelativeSide(relative,tank.getFacing())==dir){for(int i=0;i<4&&tank.getSideMode(dir)!=mode;i++)tank.cycleSideMode(relative);}check(tank.getSideMode(dir)==mode,"Tank side "+dir+"="+mode);}
    private static synchronized void log(String message){System.out.println("[PIPE_MATERIAL_V2] "+message);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),message+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    private static synchronized void check(boolean ok,String message){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+message);}
    private static int connections(BlockState state){return (int)state.getValues().entrySet().stream().filter(e->e.getKey() instanceof BooleanProperty&&Boolean.TRUE.equals(e.getValue())).count();}
    private static void refresh(ServerLevel level,BlockPos pos){level.setBlockAndUpdate(pos,FluidPipeBlock.refreshConnections(level,pos,level.getBlockState(pos)));}
    private static void pipe(ServerLevel level,BlockPos pos,int tier){level.setBlockAndUpdate(pos,block(tier).defaultBlockState());pipes.add(pos);}
    private static void pipe(ServerLevel level,int x,int y,int z,int tier){pipe(level,new BlockPos(x,y,z),tier);}
    private static void place(ServerPlayer player,BlockPos pos,int tier){ItemStack saved=player.getMainHandItem();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block(tier)));player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos.below(),false)));player.setItemInHand(InteractionHand.MAIN_HAND,saved);}

    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("pipe-materials-v2"))throw new IllegalStateException("Isolated directory required");
        var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(!fixture){
            fixture=true;level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
            for(int x=-5;x<=46;x++)for(int z=-8;z<=58;z++)level.setBlockAndUpdate(new BlockPos(x,199,z),Blocks.SMOOTH_STONE.defaultBlockState());
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            player.teleportTo(level,10,214,-20,0,30);
            for(int tier=1;tier<=3;tier++)player.getInventory().setItem(tier-1,new ItemStack(block(tier),64));
            for(int tier=1;tier<=3;tier++){
                int z=zone(tier);
                for(int x=0;x<20;x++)for(int row=0;row<5;row++)pipe(level,x,200,z+row*2,tier);
                pipe(level,25,200,z,tier);
                for(int y=200;y<=203;y++)pipe(level,28,y,z,tier);
                pipe(level,32,200,z,tier);pipe(level,33,200,z,tier);pipe(level,32,200,z+1,tier);
                pipe(level,37,200,z,tier);pipe(level,36,200,z,tier);pipe(level,38,200,z,tier);pipe(level,37,200,z+1,tier);
                pipe(level,25,200,z+6,tier);for(Direction d:Direction.Plane.HORIZONTAL)pipe(level,new BlockPos(25,200,z+6).relative(d),tier);
                pipe(level,31,202,z+6,tier);for(Direction d:Direction.values())pipe(level,new BlockPos(31,202,z+6).relative(d),tier);
                level.setBlockAndUpdate(source(tier),UniversalTankRegistry.UNIVERSAL_TANK.get().defaultBlockState());level.setBlockAndUpdate(sink(tier),UniversalTankRegistry.UNIVERSAL_TANK.get().defaultBlockState());
                for(int x=36;x<=39;x++)pipe(level,x,200,z+8,tier);
                tankMode(level,source(tier),Direction.EAST,SideMode.OUTPUT);tankMode(level,sink(tier),Direction.WEST,SideMode.INPUT);
                tank(level,source(tier)).fill(new FluidStack(Fluids.WATER,4000),IFluidHandler.FluidAction.EXECUTE);
                var placed=new BlockPos(43,200,z);place(player,placed,tier);check(level.getBlockState(placed).is(block(tier)),"Tier "+tier+" actual placement");check(player.gameMode.destroyBlock(placed)&&level.isEmptyBlock(placed),"Tier "+tier+" actual break");
            }
            for(BlockPos pos:pipes)refresh(level,pos);
            for(int tier=1;tier<=3;tier++){
                int z=zone(tier);check(connections(level.getBlockState(new BlockPos(25,200,z)))==0,"Tier "+tier+" isolated");check(connections(level.getBlockState(new BlockPos(28,201,z)))==2,"Tier "+tier+" vertical");
                check(connections(level.getBlockState(new BlockPos(32,200,z)))==2,"Tier "+tier+" corner");check(connections(level.getBlockState(new BlockPos(37,200,z)))==3,"Tier "+tier+" T");check(connections(level.getBlockState(new BlockPos(25,200,z+6)))==4,"Tier "+tier+" cross");check(connections(level.getBlockState(new BlockPos(31,202,z+6)))==6,"Tier "+tier+" six-way");
                check(connections(level.getBlockState(new BlockPos(36,200,z+8)))==2,"Tier "+tier+" machine port connection");
            }
            int pairs=0;var a=new BlockPos(44,205,40);
            for(int first=1;first<=3;first++)for(int second=1;second<=3;second++)for(Direction d:Direction.values()){
                var b=a.relative(d);level.setBlockAndUpdate(a,block(first).defaultBlockState());level.setBlockAndUpdate(b,block(second).defaultBlockState());refresh(level,a);refresh(level,b);
                if(connections(level.getBlockState(a))==1&&connections(level.getBlockState(b))==1)pairs++;
                level.removeBlock(a,false);level.removeBlock(b,false);
            }
            check(pairs==54,"All 54 ordered tier/direction pairs connect");
            for(int tier=1;tier<=3;tier++)for(int x=0;x<8;x++)pipe(level,x+3,200,38+tier*2,tier);
            for(int tier=1;tier<=3;tier++){
                int x=34+(tier-1)*4;level.setBlockAndUpdate(new BlockPos(x,201,46),Blocks.SMOOTH_STONE.defaultBlockState());var frame=new ItemFrame(level,new BlockPos(x,201,45),Direction.NORTH);frame.setItem(new ItemStack(block(tier)));level.addFreshEntity(frame);
                var drop=new ItemEntity(level,x+.5,200,44,new ItemStack(block(tier)));drop.setPickUpDelay(32767);level.addFreshEntity(drop);
            }
            for(int tier=1;tier<=3;tier++){
                player.getInventory().setItem(tier+2,new ItemStack(energy(tier),64));
                for(int x=0;x<8;x++)level.setBlockAndUpdate(new BlockPos(x+3,200,48+tier*2),energy(tier).defaultBlockState());
                var pos=new BlockPos(22,200,48+tier*2);level.setBlockAndUpdate(pos,energy(tier).defaultBlockState());
                for(Direction d:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(pos.relative(d),energy(tier).defaultBlockState());
            }
            ready=true;log("Fixture ready: 100 pipes per tier, nodes and real fluid networks");
        }
        age++;
        for(int tier=1;tier<=3;tier++){
            int limit=tier==1?128:tier==2?512:2048;
            var src=tank(level,source(tier));var dst=tank(level,sink(tier));int received=dst.getFluidInTank(0).getAmount();
            if(age>=3&&age<=12){check(received==limit,"Tier "+tier+" exact mB/t="+received);check(received+src.getFluidInTank(0).getAmount()==4000,"Tier "+tier+" fluid conserved before fixture refill");}
            if(age==30){var be=(FluidPipeBlockEntity)level.getBlockEntity(cut(tier));be.setSideMode(Direction.EAST,FluidPipeSideMode.DISABLED);var tag=be.saveWithoutMetadata();var detached=new FluidPipeBlockEntity(cut(tier),be.getBlockState());detached.load(tag);check(detached.getSideMode(Direction.EAST)==FluidPipeSideMode.DISABLED,"Tier "+tier+" SideModes NBT round trip");}
            if(age==40){check(received==0,"Tier "+tier+" disabled stops fluid");((FluidPipeBlockEntity)level.getBlockEntity(cut(tier))).setSideMode(Direction.EAST,FluidPipeSideMode.AUTO);}
            if(age==50){check(received==limit,"Tier "+tier+" enabled resumes fluid");player.gameMode.destroyBlock(cut(tier));}
            if(age==60){check(received==0,"Tier "+tier+" break stops fluid");place(player,cut(tier),tier);}
            if(age==70)check(received==limit,"Tier "+tier+" real replacement resumes fluid");
            if(age==80){level.setBlockAndUpdate(cut(tier),block(1).defaultBlockState());refresh(level,cut(tier));}
            if(age==90)check(received==128,"Tier "+tier+" mixed network basic bottleneck 128 mB/t");
            if(age==100){level.setBlockAndUpdate(cut(tier),block(tier).defaultBlockState());refresh(level,cut(tier));}
            if(age==110)check(received==limit,"Tier "+tier+" full throughput restored");
            if(age==130)check(received==limit&&dst.getFluidInTank(0).getFluid()==Fluids.LAVA,"Tier "+tier+" lava transfer at tier rate");
            if(age==150)check(src.getFluidInTank(0).getAmount()==4000&&received==2000&&dst.getFluidInTank(0).getFluid()==Fluids.WATER,"Tier "+tier+" incompatible fluids do not mix or disappear");
            // Test-only replenishment outside the measured network transfer: source starts each tick at 4000 mB.
            if(age<140||age>=150){dst.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);if(age==120)src.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);src.fill(new FluidStack(age>=120?Fluids.LAVA:Fluids.WATER,4000),IFluidHandler.FluidAction.EXECUTE);}
            if(age==140){dst.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);dst.fill(new FluidStack(Fluids.WATER,2000),IFluidHandler.FluidAction.EXECUTE);src.fill(new FluidStack(Fluids.LAVA,4000),IFluidHandler.FluidAction.EXECUTE);}
        }
        if(age==160)serverDone=true;
    }

    private static void camera(double x,double y,double z,float yaw,float pitch){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{var s=mc.getSingleplayerServer();s.getPlayerList().getPlayers().get(0).teleportTo(s.overworld(),x,y,z,yaw,pitch);});}
    private static void shot(String name){var mc=Minecraft.getInstance();check(name.contains("inventory")?(mc.screen instanceof InventoryScreen||mc.screen instanceof CreativeModeInventoryScreen):mc.screen==null,"Capture context "+name);try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(OUT);image.writeToFile(OUT.resolve(name));log("Screenshot "+name);}catch(Exception e){check(false,"Capture "+e);}}
    private static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    private static void baked(){var mc=Minecraft.getInstance();int count=0;for(int family=0;family<2;family++)for(int tier=1;tier<=3;tier++)for(var state:(family==0?block(tier):energy(tier)).getStateDefinition().getPossibleStates()){var quads=mc.getBlockRenderer().getBlockModel(state).getQuads(state,null,RandomSource.create(1));int expected=family==0?6+(tier==1?15:tier==2?25:20)*connections(state):36+26*connections(state);if(quads.size()==expected&&quads.stream().noneMatch(q->q.getSprite().contents().name().toString().contains("missingno")))count++;}check(count==384,"All 384 energy/fluid baked states: unchanged quads and valid atlas sprites");}
    private static void pack(boolean before){var mc=Minecraft.getInstance();var repo=mc.getResourcePackRepository();repo.reload();var selected=new ArrayList<>(packs);if(before)selected.add(PACK);repo.setSelected(selected);check(repo.getSelectedIds().contains(PACK)==before,"Comparison pack="+before);reloading=true;mc.reloadResourcePacks().whenComplete((v,error)->{if(error!=null)check(false,"Reload failed "+error);reloading=false;});}
    private static void measure(String name){frames.clear();lastFrame=0;sampling=name;}
    private static void finish(){String name=sampling;sampling="";var sorted=new ArrayList<>(frames);Collections.sort(sorted);check(sorted.size()>100,"Frame sample "+name+"="+sorted.size());if(sorted.isEmpty())return;double mean=sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);String row=String.format(Locale.ROOT,"%s,%d,%.4f,%.4f,%.4f%n",name,sorted.size(),mean,sorted.get(sorted.size()/2),sorted.get((int)(sorted.size()*.95)));try{Files.writeString(OUT.resolve("frame_times.csv"),row,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}log("FRAME_METRICS name,frames,mean_ms,median_ms,p95_ms: "+row.trim());}
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){if(!ENABLED||e.phase!=TickEvent.Phase.END||sampling.isEmpty())return;long now=System.nanoTime();if(lastFrame!=0)frames.add((now-lastFrame)/1_000_000.0);lastFrame=now;}
    private static void plan(){
        var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;packs=new ArrayList<>(mc.getResourcePackRepository().getSelectedIds());baked();
        add(80,()->{shot("01_long_lines.png");camera(31,205,-6,0,25);});
        add(60,()->{shot("02_tier1_junctions.png");camera(31,205,6,0,25);});
        add(60,()->{shot("03_tier2_junctions.png");camera(31,205,18,0,25);});
        add(60,()->{shot("04_tier3_junctions.png");camera(37.5,201,28,0,15);});
        add(60,()->{shot("05_machine_connection.png");camera(7,202,37,0,18);});
        add(60,()->{shot("06_family_lines.png");camera(38.5,200.5,40,0,8);});
        add(60,()->{shot("07_ground_and_frames.png");mc.options.hideGui=false;mc.setScreen(new InventoryScreen(mc.player));});
        add(50,()->{shot("08_inventory.png");mc.player.closeContainer();mc.setScreen(null);camera(44,201,-4,0,10);});
        for(int tier=1;tier<=3;tier++){
            final int t=tier;
            add(30,()->{mc.player.getInventory().selected=t-1;mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.broadcastOptions();mc.options.setCameraType(CameraType.FIRST_PERSON);});
            add(45,()->{shot("tier"+t+"_first_right.png");mc.options.mainHand().set(HumanoidArm.LEFT);mc.options.broadcastOptions();});
            add(45,()->{shot("tier"+t+"_first_left.png");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
            add(50,()->{shot("tier"+t+"_third_left.png");mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.broadcastOptions();});
            add(50,()->shot("tier"+t+"_third_right.png"));
        }
        add(20,()->{mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);camera(7,202,47,0,18);});
        add(60,()->{shot("10_energy_family.png");camera(22,203,47,0,30);});
        add(60,()->{shot("11_energy_junctions.png");mc.options.hideGui=false;camera(44,201,-4,0,10);});
        for(int tier=1;tier<=3;tier++){
            final int t=tier;
            add(20,()->{mc.player.getInventory().selected=t+2;mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.broadcastOptions();mc.options.setCameraType(CameraType.FIRST_PERSON);});
            add(40,()->{shot("energy_tier"+t+"_first_right.png");mc.options.mainHand().set(HumanoidArm.LEFT);mc.options.broadcastOptions();});
            add(40,()->{shot("energy_tier"+t+"_first_left.png");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
            add(40,()->{shot("energy_tier"+t+"_third_left.png");mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.broadcastOptions();});
            add(40,()->shot("energy_tier"+t+"_third_right.png"));
        }
        add(20,()->{mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);camera(10,214,-20,0,30);pack(true);});
        add(120,()->{shot("08_stress_before.png");measure("before");});
        add(200,()->{finish();pack(false);});
        add(120,()->{shot("09_stress_after.png");baked();measure("after");});
        add(200,()->{finish();check(serverDone,"Fluid regression finished");log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);});
        add(30,mc::stop);
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;log("Creating fresh family review world");mc.options.pauseOnLostFocus=false;mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.guiScale().set(2);mc.options.fov().set(60);mc.options.renderDistance().set(6);mc.options.bobView().set(false);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(260);
            mc.createWorldOpenFlows().createFreshLevel("pipe_material_v2_"+System.currentTimeMillis(),new LevelSettings("Pipe material V2 test",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null||reloading)return;ticks++;
        if(!planned&&ticks>120){planned=true;ticks=0;plan();}
        else if(planned&&step<steps.size()&&ticks>=steps.get(step).ticks()){ticks=0;steps.get(step++).action().run();}
    }
}
