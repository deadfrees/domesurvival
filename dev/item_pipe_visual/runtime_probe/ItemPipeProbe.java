package com.wasted.domesurvival.itempipeprobe;

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
    static final Path OUT=Path.of("../../dev/item_pipe_visual/runtime").toAbsolutePath().normalize();
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
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("item-pipe-review"))throw new IllegalStateException("Isolated directory required");
        var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(!fixture){
            fixture=true;level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-3;x<20;x++)for(int z=-4;z<15;z++)level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,3.5,102,-5,0,24);
            for(int i=0;i<4;i++){
                for(int x=0;x<8;x++)level.setBlockAndUpdate(pos(x,i),(i==3?block(1):block(i)).defaultBlockState());
                if(i==3)level.setBlockAndUpdate(pos(4,i),block(3).defaultBlockState());
                level.setBlockAndUpdate(pos(-1,i),Blocks.CHEST.defaultBlockState());level.setBlockAndUpdate(pos(8,i),Blocks.CHEST.defaultBlockState());
                for(int x=0;x<8;x++)level.setBlockAndUpdate(pos(x,i),ItemPipeBlock.refreshConnections(level,pos(x,i),level.getBlockState(pos(x,i))));
                pipe(level,0,i).setConnectorMode(Direction.WEST,ItemConnectorMode.OUTPUT);pipe(level,7,i).setConnectorMode(Direction.EAST,ItemConnectorMode.INPUT);
                var h=inventory(level,pos(-1,i));h.setStackInSlot(0,new ItemStack(Items.DIAMOND,64));h.setStackInSlot(1,new ItemStack(Items.GOLD_INGOT,64));h.setStackInSlot(2,new ItemStack(Items.REDSTONE,64));
                player.getInventory().setItem(i,new ItemStack(block(i),64));
                // Detached junction and vertical stack for shape review.
                for(Direction d:Direction.values())level.setBlockAndUpdate(new BlockPos(12,102,i*3).relative(d),block(i).defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(12,102,i*3),block(i).defaultBlockState());
                // Configurable collars: all four materials, INPUT / OUTPUT / DISABLED.
                BlockPos display=new BlockPos(16,101,i*3);
                level.setBlockAndUpdate(display,block(i).defaultBlockState());
                for(Direction d:List.of(Direction.WEST,Direction.EAST,Direction.UP))level.setBlockAndUpdate(display.relative(d),Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(display,ItemPipeBlock.refreshConnections(level,display,level.getBlockState(display)));
                var connector=(ItemPipeBlockEntity)level.getBlockEntity(display);
                connector.setConnectorMode(Direction.WEST,ItemConnectorMode.INPUT);
                connector.setConnectorMode(Direction.EAST,ItemConnectorMode.OUTPUT);
                connector.setConnectorMode(Direction.UP,ItemConnectorMode.DISABLED);
                check(connector.getConnectorMode(Direction.WEST)==ItemConnectorMode.INPUT&&connector.getConnectorMode(Direction.EAST)==ItemConnectorMode.OUTPUT&&connector.getConnectorMode(Direction.UP)==ItemConnectorMode.DISABLED,"All three configurable connector modes retained for material "+i);
            }
            var f=pipe(level,4,3);f.ghostFilters().setStackInSlot(0,new ItemStack(Items.DIAMOND));f.setFilterRoute(0,FilterRoute.EAST);
            f.ghostFilters().setStackInSlot(1,new ItemStack(Items.GOLD_INGOT));f.setFilterRoute(1,FilterRoute.NONE);
            check(f.allowsFilterExit(Direction.EAST,new ItemStack(Items.DIAMOND)),"Filter accepts diamond east");
            check(!f.allowsFilterExit(Direction.WEST,new ItemStack(Items.DIAMOND)),"Filter rejects diamond west");
            check(!f.allowsFilterExit(Direction.EAST,new ItemStack(Items.GOLD_INGOT)),"Filter blocks gold NONE");
            check(f.allowsFilterExit(Direction.EAST,new ItemStack(Items.REDSTONE)),"Unmatched redstone remains allowed");
            var clone=new ItemPipeBlockEntity(pos(4,3),f.getBlockState());clone.load(f.saveWithoutMetadata());
            check(!clone.allowsFilterExit(Direction.EAST,new ItemStack(Items.GOLD_INGOT)),"Filter configuration survives NBT round trip");
            ready=true;return;
        }
        age++;
        if(age==1||age==41||age==81||age==121||age==161)for(int i=0;i<4;i++){
            int received=total(inventory(level,pos(8,i))),remaining=total(inventory(level,pos(-1,i)));
            check(received+remaining==192,"Item conservation tier "+i+" age "+age);
            int expected=Math.min(192,(age/(i==0?40:i==2?10:20)+1)*(i==0?4:i==2?32:16));
            if(i<3)check(received==expected,"Throughput tier "+i+" age "+age+" expected "+expected+" actual "+received);
        }
        if(age==180){
            check(total(inventory(level,pos(8,3)))==128,"Filtering transferred diamond and redstone only");
            check(inventory(level,pos(-1,3)).getStackInSlot(1).getCount()==64,"Blocked gold remains at source");
            for(int i=0;i<4;i++)pipe(level,0,i).setConnectorMode(Direction.WEST,ItemConnectorMode.DISABLED);
        }
        if(age==200){
            check(total(inventory(level,pos(8,0)))==20,"Disabled input stops copper transfer");
            for(int i=0;i<4;i++)pipe(level,0,i).setConnectorMode(Direction.WEST,ItemConnectorMode.OUTPUT);
        }
        // After measured checks, replenish only for continuous visual review.
        if(age>240)for(int i=0;i<4;i++){
            var src=inventory(level,pos(-1,i));var dst=inventory(level,pos(8,i));
            for(int s=0;s<dst.getSlots();s++)dst.setStackInSlot(s,ItemStack.EMPTY);
            if(total(src)<64){src.setStackInSlot(0,new ItemStack(age%160<80?Items.DIAMOND:Items.REDSTONE,64));}
            if(i==3&&age==241)src.setStackInSlot(1,ItemStack.EMPTY);
        }
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
    static void network(){
        ItemStack item=new ItemStack(Items.DIAMOND,4);item.getOrCreateTag().putString("review","packet NBT");
        var original=new ItemPipeVisualNetwork.Travel(Level.OVERWORLD.location(),List.of(new Vec3(1,2,3),new Vec3(4,5,6)),item);
        var buf=new FriendlyByteBuf(Unpooled.buffer());try{ItemPipeVisualNetwork.Travel.encode(original,buf);var copy=ItemPipeVisualNetwork.Travel.decode(buf);check(copy.dimension().equals(original.dimension())&&copy.points().equals(original.points())&&ItemStack.matches(copy.stack(),item),"Travel packet preserves item, count, NBT and path");}finally{buf.release();}
        try{
            var field=ItemPipeTravelVisuals.class.getDeclaredField("VISUALS");field.setAccessible(true);var visuals=(Collection<?>)field.get(null);
            check(!visuals.isEmpty(),"Real server transfers reached client animation queue: "+visuals.size());
            if(!visuals.isEmpty()){
                var visual=visuals.iterator().next();var stackMethod=visual.getClass().getDeclaredMethod("stack");stackMethod.setAccessible(true);
                var stack=(ItemStack)stackMethod.invoke(visual);check(stack.is(Items.DIAMOND)||stack.is(Items.REDSTONE),"Animation contains the actual transferred item: "+stack);
            }
            int count=visuals.size();ItemPipeTravelVisuals.accept(new ItemPipeVisualNetwork.Travel(Level.NETHER.location(),original.points(),item));
            check(visuals.size()==count,"Visual packet for another dimension ignored");
            for(int i=0;i<70;i++)ItemPipeTravelVisuals.accept(original);
            check(visuals.size()==64,"Client queue bounded at 64 cosmetic transfers");
        }catch(Exception ex){check(false,"Visual queue "+ex);}
    }
    static void plan(){
        var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;baked();network();
        add(50,()->{shot("01_four_tiers");camera(3.5,100,-1.9,0,30);});
        for(int i=0;i<4;i++){
            final int row=i;
            add(50,()->camera(row==3?4.5:3.5,100,row*3-1.9,0,30));
            add(60,()->shot("tier"+row+"_moving_a"));add(4,()->shot("tier"+row+"_moving_b"));
        }
        add(20,()->camera(12,105,-4,0,25));add(50,()->shot("02_junctions"));
        for(int i=0;i<4;i++){
            final int row=i;
            add(10,()->camera(16.5,102,row*3-2.6,0,25));add(40,()->shot("connector_tier"+row));
        }
        add(10,()->{mc.options.hideGui=false;mc.setScreen(new InventoryScreen(mc.player));});add(30,()->shot("03_inventory"));
        add(10,()->{mc.player.closeContainer();mc.setScreen(null);mc.player.getInventory().selected=0;});add(30,()->shot("04_in_hand"));
        add(10,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);});add(20,mc::stop);
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);
            mc.createWorldOpenFlows().createFreshLevel("item_pipe_review_"+System.currentTimeMillis(),new LevelSettings("Item pipe review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>260){planned=true;ticks=0;plan();}
        else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;steps.get(step++).action().run();}
    }
}
