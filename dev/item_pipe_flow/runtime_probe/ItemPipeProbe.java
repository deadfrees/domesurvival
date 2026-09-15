package com.wasted.domesurvival.itempipeprobe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.coal.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.itempipe.*;
import com.wasted.domesurvival.forge.client.itempipe.ItemPipeTravelVisuals;
import io.netty.buffer.Unpooled;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import java.nio.file.*;
import java.util.*;

/** Isolated test source set; excluded from release JAR. */
@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class ItemPipeProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.itemPipeReview");
    static final Path OUT=Path.of("../../dev/item_pipe_flow/runtime").toAbsolutePath().normalize();
    static boolean started,fixture,planned; static volatile boolean ready; static int age,ticks,step,failures;
    static final List<Step> steps=new ArrayList<>();
    record Step(int delay,Runnable action){}
    static Block block(int i){return switch(i){case 0->ItemPipeRegistry.COPPER_PIPE.get();case 1->ItemPipeRegistry.STEEL_PIPE.get();case 2->ItemPipeRegistry.DESH_PIPE.get();default->ItemPipeRegistry.FILTERING_PIPE.get();};}
    static BlockPos pos(int x,int i){return new BlockPos(x,100,i*3);}
    static IItemHandlerModifiable inventory(ServerLevel level,BlockPos pos){return (IItemHandlerModifiable)level.getBlockEntity(pos).getCapability(ForgeCapabilities.ITEM_HANDLER,null).orElseThrow(()->new IllegalStateException("Missing chest"));}
    static int total(IItemHandlerModifiable h){int n=0;for(int i=0;i<h.getSlots();i++)n+=h.getStackInSlot(i).getCount();return n;}
    static ItemPipeBlockEntity pipe(ServerLevel level,int x,int i){return (ItemPipeBlockEntity)level.getBlockEntity(pos(x,i));}
    static synchronized void log(String s){System.out.println("[ITEM_PIPE_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    static void check(boolean b,String s){if(!b)failures++;log((b?"PASS ":"FAIL ")+s);}
    static final BlockPos GENERATOR=new BlockPos(12,101,0);
    static final int[] arrivals=new int[3];
    static volatile int portPhase;
    static void mode(CoalGeneratorBlockEntity g,RelativeSide side,SideMode mode){
        for(int i=0;i<3&&g.getDataAccess().get(CoalGeneratorBlockEntity.DATA_SIDES_START+side.resolve(g.getMachineFacing()).ordinal())!=mode.ordinal();i++)g.cycleSideMode(side);
    }
    static void lane(ServerLevel level,int row,int tier,int count){
        for(int x=0;x<8;x++)level.setBlockAndUpdate(pos(x,row),block(tier).defaultBlockState());
        level.setBlockAndUpdate(pos(-1,row),Blocks.CHEST.defaultBlockState());level.setBlockAndUpdate(pos(8,row),Blocks.CHEST.defaultBlockState());
        for(int x=0;x<8;x++)level.setBlockAndUpdate(pos(x,row),ItemPipeBlock.refreshConnections(level,pos(x,row),level.getBlockState(pos(x,row))));
        pipe(level,0,row).setConnectorMode(Direction.WEST,ItemConnectorMode.OUTPUT);pipe(level,7,row).setConnectorMode(Direction.EAST,ItemConnectorMode.INPUT);
        ItemStack stack=new ItemStack(Items.DIAMOND,count);stack.getOrCreateTag().putInt("lane",row);inventory(level,pos(-1,row)).setStackInSlot(0,stack);
    }
    static int diamonds(IItemHandlerModifiable inv){int n=0;for(int s=0;s<inv.getSlots();s++)if(inv.getStackInSlot(s).is(Items.DIAMOND))n+=inv.getStackInSlot(s).getCount();return n;}
    static int pending(ServerLevel level,int row){int n=0;var list=ItemPipeTransitData.get(level).save(new CompoundTag()).getList("Packets",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var e=list.getCompound(i);if(BlockPos.of(e.getLong("Source")).equals(pos(0,row)))n+=ItemStack.of(e.getCompound("Stack")).getCount();}return n;}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("item-pipe-flow-review"))throw new IllegalStateException("Isolated directory required");
        var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(!fixture){
            fixture=true;level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-3;x<17;x++)for(int z=-4;z<22;z++)level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,3.5,102,-5,0,24);
            for(int i=0;i<4;i++)lane(level,i,i<3?i:1,16);
            level.setBlockAndUpdate(GENERATOR,ModBlocks.COAL_GENERATOR.get().defaultBlockState());
            var generator=(CoalGeneratorBlockEntity)level.getBlockEntity(GENERATOR);
            for(RelativeSide side:RelativeSide.values())mode(generator,side,SideMode.INPUT);
            for(Direction side:Direction.values())level.setBlockAndUpdate(GENERATOR.relative(side),block(1).defaultBlockState());
            ready=true;return;
        }
        age++;
        if(age==2)for(int i=0;i<3;i++){
            check(total(inventory(level,pos(8,i)))==0,"No instant insertion tier "+i);
            check(16-total(inventory(level,pos(-1,i)))==(1<<i),"Batch size tier "+i);
        }
        if(age==5){
            for(int i=0;i<3;i++)pipe(level,0,i).setConnectorMode(Direction.WEST,ItemConnectorMode.DISABLED);
            var dst=inventory(level,pos(8,3));for(int s=0;s<dst.getSlots();s++)dst.setStackInSlot(s,new ItemStack(Items.COBBLESTONE,64));
        }
        if(age==20){
            var data=ItemPipeTransitData.get(level);var saved=data.save(new CompoundTag());var loaded=ItemPipeTransitData.load(saved);
            check(loaded.save(new CompoundTag()).equals(saved),"Transit NBT preserves UUID, stack NBT, path and elapsed time");
            level.getDataStorage().set("domesurvival_item_pipe_transit_v1",loaded);
        }
        if(age==30)ItemPipeNetworkManager.markDirty(level);
        for(int i=0;i<3;i++)if(arrivals[i]==0&&total(inventory(level,pos(8,i)))>0){arrivals[i]=age;check(Math.abs(age-new int[]{199,133,100}[i])<=2,"Physical arrival tier "+i+" at tick "+age);}
        if(age%20==0&&age<=240)for(int i=0;i<4;i++)check(diamonds(inventory(level,pos(-1,i)))+diamonds(inventory(level,pos(8,i)))+pending(level,i)==16,"Conservation lane "+i+" tick "+age);
        if(age==60||age==100||age==140){
            var g=(CoalGeneratorBlockEntity)level.getBlockEntity(GENERATOR);SideMode mode=age==60?SideMode.OUTPUT:age==100?SideMode.DISABLED:SideMode.INPUT;
            for(RelativeSide side:RelativeSide.values())mode(g,side,mode);portPhase=age;
        }
        if(age==200){
            // A full inventory stops new dispatch; the already extracted packet waits.
            check(pending(level,3)==2&&diamonds(inventory(level,pos(-1,3)))==14,"Full receiver retains owned packet and stops extraction");
            pipe(level,0,3).setConnectorMode(Direction.WEST,ItemConnectorMode.DISABLED);
        }
        if(age==220){var dst=inventory(level,pos(8,3));for(int s=0;s<dst.getSlots();s++)dst.setStackInSlot(s,ItemStack.EMPTY);}
        if(age==245){check(diamonds(inventory(level,pos(8,3)))==2&&pending(level,3)==0,"Waiting packet delivered exactly once after receiver frees");check(arrivals[2]<arrivals[1]&&arrivals[1]<arrivals[0],"Tier speeds ordered");}
        if(age==250)lane(level,4,0,16);
        if(age==270)level.destroyBlock(pos(4,4),false);
        if(age==280)check(diamonds(inventory(level,pos(-1,4)))==16&&pending(level,4)==0&&total(inventory(level,pos(8,4)))==0,"Broken route refunds packet without duplication");
        if(age==300){lane(level,5,1,2);level.setBlockAndUpdate(pos(4,5),block(3).defaultBlockState());var f=pipe(level,4,5);f.ghostFilters().setStackInSlot(0,new ItemStack(Items.DIAMOND));f.setFilterRoute(0,FilterRoute.EAST);f.ghostFilters().setStackInSlot(1,new ItemStack(Items.GOLD_INGOT));f.setFilterRoute(1,FilterRoute.NONE);inventory(level,pos(-1,5)).setStackInSlot(1,new ItemStack(Items.GOLD_INGOT,2));}
        if(age==301)for(int x=0;x<8;x++)level.setBlockAndUpdate(pos(x,5),ItemPipeBlock.refreshConnections(level,pos(x,5),level.getBlockState(pos(x,5))));
        if(age==460){check(diamonds(inventory(level,pos(8,5)))==2&&inventory(level,pos(-1,5)).getStackInSlot(1).getCount()==2,"Filtering preserves routing and inherits steel travel speed");}
        if(age==480){check(ItemPipeTransitData.get(level).pendingItems()==0,"No orphaned in-flight stacks at completion");}
    }
    static void clientPorts(boolean input,String stage){
        var level=Minecraft.getInstance().level;
        for(Direction side:Direction.values()){
            var p=GENERATOR.relative(side);boolean expected=input&&side!=Direction.NORTH;
            check(ItemPipeBlock.hasObjectConnector(level,p,side.getOpposite())==expected,"Client collar "+side+" "+stage);
            check(ItemPipeBlock.canConnect(level,p,side.getOpposite())==expected,"Client arm "+side+" "+stage);
        }
        check(!level.getBlockEntity(GENERATOR).getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided fuel bypass remains closed "+stage);
    }
    static void network(){
        var mc=Minecraft.getInstance();var item=new ItemStack(Items.DIAMOND,2);item.getOrCreateTag().putString("probe","tag");
        var original=new ItemPipeVisualNetwork.Travel(UUID.randomUUID(),Level.OVERWORLD.location(),List.of(new Vec3(1,2,3),new Vec3(5,2,3)),item,100,25,mc.level.getGameTime(),true);
        var buf=new FriendlyByteBuf(Unpooled.buffer());try{ItemPipeVisualNetwork.Travel.encode(original,buf);var copy=ItemPipeVisualNetwork.Travel.decode(buf);check(copy.id().equals(original.id())&&copy.points().equals(original.points())&&copy.duration()==100&&copy.elapsed()==25&&ItemStack.matches(copy.stack(),item),"Network timing, UUID and NBT round trip");}finally{buf.release();}
        try{
            var field=ItemPipeTravelVisuals.class.getDeclaredField("VISUALS");field.setAccessible(true);var visuals=(Map<?,?>)field.get(null);
            check(!visuals.isEmpty(),"Real server transfers visible on client");int before=visuals.size();
            for(int i=0;i<5;i++)ItemPipeTravelVisuals.accept(original);check(visuals.size()==before+1,"UUID updates do not duplicate visible items");
            var sample=ItemPipeTravelVisuals.class.getDeclaredMethod("sample",long.class,float.class);sample.setAccessible(true);sample.invoke(null,original.serverTime(),.5F);
            var sf=ItemPipeTravelVisuals.class.getDeclaredField("SAMPLES");sf.setAccessible(true);var samples=(Map<?,?>)sf.get(null);
            var entries=(List<?>)samples.get(new BlockPos(2,2,3));var point=entries.get(0).getClass().getDeclaredMethod("point");point.setAccessible(true);
            check(Math.abs(((Vec3)point.invoke(entries.get(0))).x-2.02)<1e-6,"Smooth sub-tick position follows physical distance");
            ItemPipeTravelVisuals.accept(new ItemPipeVisualNetwork.Travel(original.id(),original.dimension(),List.of(),ItemStack.EMPTY,1,0,original.serverTime(),false));check(visuals.size()==before,"Delivered packet removed by UUID");
        }catch(Exception ex){check(false,"Client interpolation "+ex);}
    }
    static void plan(){
        var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);baked();network();clientPorts(true,"initial blue");
        add(10,()->{shot("01_moving_items");camera(10,102,-3, -25,22);});
        add(35,()->clientPorts(false,"orange"));
        add(40,()->clientPorts(false,"off"));
        add(40,()->clientPorts(true,"blue restored"));
        add(10,()->shot("02_generator_connectors"));
        add(350,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
    static void add(int n,Runnable r){steps.add(new Step(n,r));}
    static void camera(double x,double y,double z,float yaw,float pitch){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{var s=mc.getSingleplayerServer();s.getPlayerList().getPlayers().get(0).teleportTo(s.overworld(),x,y,z,yaw,pitch);});}
    static void shot(String name){try(NativeImage image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);image.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static void baked(){
        var mc=Minecraft.getInstance();int good=0;
        for(int i=0;i<4;i++)for(BlockState state:block(i).getStateDefinition().getPossibleStates()){
            var model=mc.getBlockRenderer().getBlockModel(state);var layers=model.getRenderTypes(state,RandomSource.create(1),ModelData.EMPTY);
            var metal=model.getQuads(state,null,RandomSource.create(1),ModelData.EMPTY,RenderType.cutout());
            var glass=model.getQuads(state,null,RandomSource.create(1),ModelData.EMPTY,RenderType.translucent());
            if(layers.contains(RenderType.cutout())&&layers.contains(RenderType.translucent())&&!metal.isEmpty()&&!glass.isEmpty()&&java.util.stream.Stream.concat(metal.stream(),glass.stream()).noneMatch(q->q.getSprite().contents().name().toString().contains("missingno")))good++;
        }
        check(good==256,"All 256 baked connection states contain valid separate metal and glass layers: "+good);
        for(int i=0;i<4;i++){var stack=new ItemStack(block(i));var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);check(model!=mc.getModelManager().getMissingModel()&&!model.getRenderPasses(stack,false).isEmpty(),"Inventory model "+i);}
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);
            mc.createWorldOpenFlows().createFreshLevel("item_pipe_flow_review_"+System.currentTimeMillis(),new LevelSettings("Item pipe review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>25){planned=true;ticks=0;plan();}
        else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;steps.get(step++).action().run();}
    }
}
