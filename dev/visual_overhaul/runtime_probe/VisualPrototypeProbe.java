package com.wasted.domesurvival.visualprobe;

import com.wasted.domesurvival.forge.machine.filter.FilterRegenerationBlock;
import com.wasted.domesurvival.forge.machine.filter.FilterRegenerationBlockEntity;
import com.wasted.domesurvival.forge.machine.filter.FilterRegenerationRegistry;
import com.wasted.domesurvival.forge.item.ModItems;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Test-only Minecraft integration probe. Compiled ONLY with the isolated Gradle init script.
 * No production sourceSet, JAR, world or resource-pack selection is changed.
 * Screenshots are read from Minecraft's real framebuffer, not a model mockup.
 */
@Mod.EventBusSubscriber(modid="domesurvival", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class VisualPrototypeProbe {
    private static final boolean ENABLED=Boolean.getBoolean("dome.visualProbe");
    private static final BlockPos MACHINE=new BlockPos(0,200,0);
    private static final Path OUT=Path.of("../../dev/visual_overhaul/runtime_verified").toAbsolutePath().normalize();
    private static boolean started, fixture;
    private static volatile boolean ready, working, workInstalled;
    private static int tick, stage, workTicks;
    private static volatile boolean reloading;
    private static List<String> packs;
    private static ItemStack originalFilter;
    private static boolean cycleChecked;
    private static int failures;
    private static void log(String text) {
        System.out.println("[VISUAL_PROBE] "+text);
        try { Files.createDirectories(OUT); Files.writeString(OUT.resolve("checks.txt"),text+"\n",
            StandardOpenOption.CREATE,StandardOpenOption.APPEND); } catch(Exception e) {throw new RuntimeException(e);}
    }
    private static synchronized void check(boolean ok,String text) {if(!ok)failures++;log((ok?"PASS ":"FAIL ")+text);}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e) {
        if(!ENABLED||e.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        var server=mc.getSingleplayerServer();
        if(server==null||server.getPlayerList().getPlayers().isEmpty()) return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("visual-prototype"))
            throw new IllegalStateException("Visual probe requires its isolated working directory");
        ServerLevel level=server.overworld();
        ServerPlayer player=server.getPlayerList().getPlayers().get(0);
        if(!fixture) {
            fixture=true;
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            level.setDayTime(6000);
            for(int x=-8;x<=14;x++)for(int z=-10;z<=9;z++)level.setBlockAndUpdate(new BlockPos(x,199,z),Blocks.SMOOTH_STONE.defaultBlockState());
            Direction[] facing={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
            for(int i=0;i<4;i++)level.setBlockAndUpdate(new BlockPos(i*2,200,0),
                FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().defaultBlockState().setValue(FilterRegenerationBlock.FACING,facing[i]));
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(level,2.8,200,-3.5,30,10);
            player.getInventory().setItem(0,new ItemStack(FilterRegenerationRegistry.FILTER_REGENERATION_STATION_ITEM.get()));
            player.getInventory().setItem(1,new ItemStack(Blocks.IRON_BLOCK));
            BlockPos placeTest=new BlockPos(8,200,4);
            new ItemStack(FilterRegenerationRegistry.FILTER_REGENERATION_STATION_ITEM.get()).useOn(
                new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(
                    new Vec3(8.5,200,4.5),Direction.UP,placeTest.below(),false)));
            check(level.getBlockState(placeTest).is(FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get()),"Actual BlockItem placement");
            check(player.gameMode.destroyBlock(placeTest)&&level.isEmptyBlock(placeTest),"Actual player break removes station");
            var be=(FilterRegenerationBlockEntity)level.getBlockEntity(MACHINE);
            var energyPipe=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("domesurvival","basic_energy_pipe"));
            // Use the real BlockItem placement path so directional states are calculated.
            // setBlock(defaultBlockState) would intentionally leave all six flags false.
            ItemStack held=player.getMainHandItem().copy();
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(energyPipe));
            player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(.5,200,1.5),Direction.UP,MACHINE.south().below(),false)));
            player.setItemInHand(InteractionHand.MAIN_HAND,held);
            check(level.getBlockState(MACHINE.south()).getValues().entrySet().stream().anyMatch(
                value->value.getKey().getName().equals("north")&&Boolean.TRUE.equals(value.getValue())),"Real energy pipe connects to rear input");
            for(Direction d:Direction.values()) {
                check(be.getCapability(ForgeCapabilities.ENERGY,d).isPresent()==(d!=Direction.NORTH&&d!=Direction.DOWN),"FE port "+d);
                check(be.getCapability(ForgeCapabilities.ITEM_HANDLER,d).isPresent()==(d!=Direction.NORTH),"Item port "+d);
            }
            check(level.getBlockState(MACHINE).getCollisionShape(level,MACHINE).bounds().getSize()==1.0,"Collision remains one full block");
            ready=true;log("Fixture ready: fresh superflat test world, four horizontal orientations.");
        }
        if(working&&!workInstalled) {
            var be=(FilterRegenerationBlockEntity)level.getBlockEntity(MACHINE);
            ItemStack filter=new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get());
            filter.setDamageValue(filter.getMaxDamage()-1); originalFilter=filter.copy();
            check(FilterRegenerationBlockEntity.isEligibleFilter(filter),"Test fixture uses registered eligible cartridge");
            be.getInventory().setStackInSlot(0,filter);
            be.getInventory().setStackInSlot(1,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","filter_regeneration_media")),64));
            workInstalled=true;
        }
        if(workInstalled) {
            var be=(FilterRegenerationBlockEntity)level.getBlockEntity(MACHINE);
            be.getCapability(ForgeCapabilities.ENERGY,Direction.UP).ifPresent(energy->energy.receiveEnergy(64,false));
            workTicks++;
            if(workTicks==80) check(level.getBlockState(MACHINE).getValue(FilterRegenerationBlock.ACTIVE),"Active state synchronized during regeneration");
            if(workTicks>=230&&!cycleChecked) {
                cycleChecked=true;
                ItemStack restored=be.getInventory().getStackInSlot(0);
                check(restored.getItem()==originalFilter.getItem()&&restored.getDamageValue()<originalFilter.getDamageValue(),"Real cycle restores filter in original slot");
                check(FilterRegenerationBlockEntity.getRegenerationCycles(restored)>0,"Original regeneration cycle NBT increments");
                check(be.getInventory().getStackInSlot(1).getCount()<64,"Original cycle consumes media");
            }
            if(workTicks==250)be.getInventory().setStackInSlot(1,ItemStack.EMPTY);
            if(workTicks==270)check(!level.getBlockState(MACHINE).getValue(FilterRegenerationBlock.ACTIVE),"Machine becomes inactive without media");
        }
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e) {
        if(!ENABLED||e.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
            started=true; log("Starting test-only automated world.");
            mc.options.guiScale().set(2);mc.options.fov().set(60);mc.options.renderDistance().set(6);mc.options.bobView().set(false);
            mc.createWorldOpenFlows().createFreshLevel("visual_filter_"+System.currentTimeMillis(),
                new LevelSettings("DOMESURVIVAL Visual Prototype",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null||reloading) return;
        tick++;
        if(stage==0&&tick>80) {mc.setScreen(null);mc.options.hideGui=true;stage=1;tick=0;}
        else if(stage==1&&tick>40) {shot(mc,"01_new_front.png");packs=new ArrayList<>(mc.getResourcePackRepository().getSelectedIds());pack(mc,true);stage=2;tick=0;}
        else if(stage==2&&tick>80) {shot(mc,"02_old_front.png");pack(mc,false);stage=3;tick=0;}
        else if(stage==3&&tick>80) {mc.options.hideGui=false;stage=4;tick=0;}
        else if(stage==4&&tick>40) {shot(mc,"03_new_in_hand.png");mc.setScreen(new InventoryScreen(mc.player));stage=5;tick=0;}
        else if(stage==5&&tick>30) {shot(mc,"04_new_inventory.png");mc.player.closeContainer();mc.setScreen(null);mc.options.hideGui=true;working=true;stage=6;tick=0;}
        else if(stage==6&&tick>90) {shot(mc,"05_new_working.png");mc.options.hideGui=false;
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,200.5,0),Direction.NORTH,MACHINE,false));stage=7;tick=0;}
        else if(stage==7&&tick>30) {check(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("FilterRegenerationScreen"),"Actual block interaction opens FilterRegenerationScreen");shot(mc,"06_new_gui.png");stage=8;tick=0;}
        else if(stage==8&&tick>140) {mc.player.closeContainer();mc.setScreen(null);mc.options.hideGui=true;
            mc.getSingleplayerServer().execute(()->{var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().get(0);p.teleportTo(s.overworld(),9,203,-9,30,18);});stage=9;tick=0;}
        else if(stage==9&&tick>60) {shot(mc,"07_four_orientations.png");
            mc.getSingleplayerServer().execute(()->{
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().get(0);
                p.teleportTo(s.overworld(),2.8,200,-3.5,30,10);
                var dropped=new net.minecraft.world.entity.item.ItemEntity(s.overworld(),.5,200,-1.5,
                    new ItemStack(FilterRegenerationRegistry.FILTER_REGENERATION_STATION_ITEM.get()));
                dropped.setPickUpDelay(32767);s.overworld().addFreshEntity(dropped);
            });stage=10;tick=0;}
        else if(stage==10&&tick>60) {shot(mc,"08_ground_item.png");check(cycleChecked,"Cycle assertions completed");
            log("RESULT "+(failures==0?"PASS":"FAIL")+"; failures="+failures+". Screenshots are real Minecraft framebuffer captures.");stage=11;tick=0;}
        else if(stage==11&&tick>30) {mc.stop();stage=12;}
    }
    private static void pack(Minecraft mc,boolean old) {
        var repo=mc.getResourcePackRepository();repo.reload();var selected=new ArrayList<>(packs);
        if(old)selected.add("file/visual_old");repo.setSelected(selected);
        check(repo.getSelectedIds().contains("file/visual_old")==old,"OLD comparison pack selection="+old);reloading=true;
        mc.reloadResourcePacks().whenComplete((nothing,error)->{if(error!=null)log("FAIL resource reload: "+error);reloading=false;});
    }
    private static void shot(Minecraft mc,String name) {
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Files.createDirectories(OUT);image.writeToFile(OUT.resolve(name));log("Screenshot "+name);
        }catch(Exception ex) {log("FAIL screenshot "+name+": "+ex);}
    }
}
