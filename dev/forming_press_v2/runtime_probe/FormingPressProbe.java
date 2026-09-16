package com.wasted.domesurvival.formingprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.machine.forming.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.recipe.*;
import com.wasted.domesurvival.forge.item.EngineerWrenchItem;
import com.wasted.domesurvival.forge.itempipe.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class FormingPressProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.formingPressReview");
    static final Path OUT=Path.of("../../dev/forming_press_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures,age;
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String text){System.out.println("[PRESS_REVIEW] "+text);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String text){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+text);}
    static FormingPressBlockEntity press(ServerLevel level,BlockPos pos){return (FormingPressBlockEntity)level.getBlockEntity(pos);}
    static FormingPressBlockEntity place(ServerLevel level,BlockPos pos){level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,FormingPressRegistry.FORMING_PRESS.get().defaultBlockState());return press(level,pos);}
    static void mode(FormingPressBlockEntity p,RelativeSide side,SideMode m){for(int i=0;i<3&&p.getDataAccess().get(FormingPressBlockEntity.DATA_SIDES_START+side.resolve(p.getMachineFacing()).ordinal())!=m.ordinal();i++)p.cycleSideMode(side);}
    static void energy(FormingPressBlockEntity p,int amount){var tag=p.saveWithoutMetadata();tag.putInt("Energy",amount);p.load(tag);}
    static void modules(FormingPressBlockEntity p,ItemStack first,ItemStack second){p.getModules().setStackInSlot(0,first);p.getModules().setStackInSlot(1,second);}
    static List<ItemEntity> drops(ServerLevel level,BlockPos pos){return level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));}
    static int count(List<ItemEntity> entities,Item item){return entities.stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
    static void clearDrops(ServerLevel level,BlockPos pos){drops(level,pos).forEach(ItemEntity::discard);}
    static ItemStack input(FormingPressRecipe recipe){var a=recipe.getIngredient().getItems();return a[0].copyWithCount(recipe.getInputCount());}
    static void processChecks(ServerLevel level){
        var recipes=level.getRecipeManager().getAllRecipesFor(ModRecipes.FORMING_TYPE.get());
        for(FormingOperation operation:FormingOperation.values()){
            var recipe=recipes.stream().filter(r->r.getOperation()==operation&&r.getIngredient().getItems().length>0).findFirst().orElseThrow();
            for(int variant=0;variant<3;variant++){
                var p=place(level,TEST);p.setSelectedOperation(operation);
                if(variant>0)modules(p,new ItemStack(variant==1?MachineModuleItems.EFFICIENCY.get():MachineModuleItems.OVERDRIVE.get()),new ItemStack(MachineModuleItems.BUFFER.get()));
                p.getInventory().setStackInSlot(0,input(recipe));energy(p,p.getDataAccess().get(FormingPressBlockEntity.DATA_CAPACITY));
                int before=p.getDataAccess().get(0),expectedCost=p.getModules().modifiers().applyEnergyCost(recipe.getEnergy());
                int duration=p.getModules().modifiers().applyProcessingTicks(recipe.getProcessingTime());
                for(int i=0;i<duration-1;i++)FormingPressBlockEntity.serverTick(level,TEST,p.getBlockState(),p);
                check(p.getInventory().getStackInSlot(1).isEmpty(),operation+" no early output module "+variant);
                var menu=new FormingPressMenu(77,level.players().get(0).getInventory(),p);menu.setTab(FormingPressMenu.MODULE_TAB);
                if(variant>0)check(!menu.getSlot(38).mayPickup(level.players().get(0)),"Module locked during cycle "+operation+"/"+variant);
                FormingPressBlockEntity.serverTick(level,TEST,p.getBlockState(),p);
                check(ItemStack.matches(p.getInventory().getStackInSlot(1),recipe.getResult()),operation+" exact product module "+variant);
                check(before-p.getDataAccess().get(0)==expectedCost,operation+" exact recipe energy module "+variant);
                check(p.getInventory().getStackInSlot(0).isEmpty(),operation+" input consumed once module "+variant);
            }
        }
        var p=place(level,TEST);modules(p,new ItemStack(MachineModuleItems.EFFICIENCY.get()),ItemStack.EMPTY);
        check(!p.getModules().insertItem(1,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty(),"Incompatible module rejected");
        check(!p.getModules().insertItem(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()),false).isEmpty(),"Duplicate module rejected");
        modules(p,new ItemStack(MachineModuleItems.BUFFER.get()),ItemStack.EMPTY);energy(p,35000);
        var menu=new FormingPressMenu(78,level.players().get(0).getInventory(),p);menu.setTab(FormingPressMenu.MODULE_TAB);
        check(p.getDataAccess().get(1)==35000&&!menu.getSlot(38).mayPickup(level.players().get(0)),"Expanded buffer protected against energy loss");
        var saved=p.saveWithoutMetadata();var clone=new FormingPressBlockEntity(TEST,p.getBlockState());clone.load(saved);
        check(clone.getModules().getStackInSlot(0).is(MachineModuleItems.BUFFER.get())&&clone.getDataAccess().get(0)==35000,"Modules and expanded energy survive NBT");
        saved.remove("Modules");var legacy=new FormingPressBlockEntity(TEST,p.getBlockState());legacy.load(saved);
        check(legacy.getModules().getSlots()==2&&legacy.getModules().getStackInSlot(0).isEmpty(),"Legacy save retains two empty module slots");
    }
    static void ports(ServerLevel level){
        for(Direction facing:Direction.Plane.HORIZONTAL){
            var p=place(level,TEST);level.setBlockAndUpdate(TEST,p.getBlockState().setValue(FormingPressBlock.FACING,facing));
            for(RelativeSide side:RelativeSide.values())for(SideMode m:new SideMode[]{SideMode.INPUT,SideMode.OUTPUT,SideMode.DISABLED}){
                mode(p,side,m);Direction d=side.resolve(facing);boolean front=side==RelativeSide.FRONT;
                var items=p.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fe=p.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);
                check((items!=null)==(!front&&m!=SideMode.DISABLED),"Sided items "+facing+"/"+side+"/"+m);
                check((fe!=null)==(!front&&m==SideMode.INPUT),"Sided energy "+facing+"/"+side+"/"+m);
                if(fe!=null)check(fe.canReceive()&&!fe.canExtract()&&fe.receiveEnergy(999,true)==128&&p.getDataAccess().get(0)==0,"FE simulation is input-only");
                if(items!=null){p.getInventory().setStackInSlot(1,new ItemStack(Items.IRON_INGOT,2));check(items.extractItem(1,1,true).isEmpty()==(m==SideMode.INPUT),"Output permission "+side);p.getInventory().setStackInSlot(1,ItemStack.EMPTY);}
            }
            check(!p.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent()&&!p.getCapability(ForgeCapabilities.ENERGY,null).isPresent(),"No unsided bypass "+facing);
        }
    }
    static void survival(ServerLevel level,ServerPlayer player){
        player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
        for(Item tool:new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}){
            clearDrops(level,TEST);var p=place(level,TEST);p.getInventory().setStackInSlot(0,new ItemStack(Items.IRON_INGOT,3));modules(p,new ItemStack(MachineModuleItems.BUFFER.get()),ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));player.gameMode.destroyBlock(TEST);var drops=drops(level,TEST);
            check(count(drops,FormingPressRegistry.FORMING_PRESS_ITEM.get())==(tool==Items.IRON_PICKAXE?1:0),"Survival tool requirement "+tool);
            check(count(drops,Items.IRON_INGOT)==3&&count(drops,MachineModuleItems.BUFFER.get())==1,"Survival contents drop once "+tool);
        }
        clearDrops(level,TEST);var p=place(level,TEST);p.getInventory().setStackInSlot(0,new ItemStack(Items.IRON_INGOT,7));modules(p,new ItemStack(MachineModuleItems.BUFFER.get()),ItemStack.EMPTY);energy(p,31000);p.setSelectedOperation(FormingOperation.WIRE);
        Item wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));
        check(wrench instanceof EngineerWrenchItem&&wrench.getClass().getSuperclass()==Item.class,"Native wrench has no Thermal superclass");
        player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));player.setShiftKeyDown(false);
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(p.getMachineFacing()==Direction.EAST,"Native wrench rotates machine through real item-use path");
        check(!p.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST).isPresent(),"Rotated front remains blocked");
        player.setShiftKeyDown(true);player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        ItemStack portable=ItemStack.EMPTY;int count=0;for(ItemStack stack:player.getInventory().items)if(stack.is(FormingPressRegistry.FORMING_PRESS_ITEM.get())){portable=stack;count+=stack.getCount();}
        check(level.isEmptyBlock(TEST)&&count==1&&drops(level,TEST).isEmpty(),"Shift wrench returns exactly one machine, no loose duplicate contents");
        check(portable.hasTag()&&portable.getTag().getCompound("BlockEntityTag").getInt("Energy")==31000,"Wrench preserves expanded charge");
        player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,portable.copy());
        var placeHit=new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false);
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,placeHit);
        var restored=press(level,TEST);check(restored!=null&&restored.getInventory().getStackInSlot(0).getCount()==7&&restored.getModules().getStackInSlot(0).is(MachineModuleItems.BUFFER.get())&&restored.getSelectedOperation()==FormingOperation.WIRE,"Replaced machine restores contents, module and operation");
        player.setGameMode(GameType.ADVENTURE);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));player.setShiftKeyDown(true);player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(level.getBlockEntity(TEST)==restored,"Adventure mode cannot bypass dismantle permission");
        player.setGameMode(GameType.SURVIVAL);for(int i=0;i<player.getInventory().items.size();i++)player.getInventory().items.set(i,new ItemStack(Items.COBBLESTONE,64));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(count(drops(level,TEST),FormingPressRegistry.FORMING_PRESS_ITEM.get())==1&&count(drops(level,TEST),Items.IRON_INGOT)==0,"Full inventory drops one portable machine without loose contents");
        clearDrops(level,TEST);player.setShiftKeyDown(false);player.getInventory().clearContent();player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        BlockPos pipePos=TEST.offset(0,0,3);
        level.setBlockAndUpdate(pipePos,ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(pipePos.east(),Blocks.CHEST.defaultBlockState());
        var pipe=(ItemPipeBlockEntity)level.getBlockEntity(pipePos);var before=pipe.getConnectorMode(Direction.EAST);
        var pipeHit=new BlockHitResult(Vec3.atCenterOf(pipePos).add(.49,0,0),Direction.EAST,pipePos,false);
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,pipeHit);
        check(pipe.getConnectorMode(Direction.EAST)!=before,"Native key configures pipe connector via normal click");
        level.setBlockAndUpdate(pipePos.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
        player.setShiftKeyDown(true);player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,pipeHit);
        check(pipe.isManuallyDisconnected(Direction.EAST),"Shift key splits pipe boundary without removing pipe");
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,pipeHit);
        check(!pipe.isManuallyDisconnected(Direction.EAST),"Shift key reconnects pipe boundary");
        player.setShiftKeyDown(false);
    }
    static final class CancelBreak {
        @SubscribeEvent public void cancel(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
            if (event.getPos().equals(TEST)) event.setCanceled(true);
        }
    }
    static void wrenchSafety(ServerLevel level, ServerPlayer player) {
        var wrench=(EngineerWrenchItem)ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));player.setShiftKeyDown(false);
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
        BlockState[] protectedStates={Blocks.OAK_DOOR.defaultBlockState(),Blocks.RED_BED.defaultBlockState(),
                Blocks.END_PORTAL_FRAME.defaultBlockState(),Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.CHEST_TYPE,ChestType.LEFT),
                Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.EXTENDED,true)};
        for(var state:protectedStates){
            level.setBlock(TEST,state,2);var before=level.getBlockState(TEST);
            wrench.onItemUseFirst(player.getMainHandItem(),context);
            check(level.getBlockState(TEST).equals(before),"Key preserves composite/extended block "+state.getBlock());
        }
        for(Block block:new Block[]{Blocks.FURNACE,Blocks.HOPPER,Blocks.OAK_LOG}){
            level.setBlock(TEST,block.defaultBlockState(),2);var before=level.getBlockState(TEST);
            player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
            check(!level.getBlockState(TEST).equals(before)&&level.getBlockState(TEST).getBlock()==block,"Key rotates supported vanilla block "+block);
        }
        var p=place(level,TEST);p.getInventory().setStackInSlot(0,new ItemStack(Items.IRON_INGOT,9));
        player.setGameMode(GameType.SURVIVAL);player.setShiftKeyDown(true);
        var protection=new CancelBreak();net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protection);
        try{player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);}
        finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protection);}
        check(level.getBlockEntity(TEST)==p&&p.getInventory().getStackInSlot(0).getCount()==9,"Protection break event prevents portable extraction");
        var marked=new ItemStack(wrench);marked.getOrCreateTag().putString("Marker","kept");
        check(ItemStack.matches(marked,wrench.getCraftingRemainingItem(marked)),"Crafting returns the original wrench including NBT");
        player.setShiftKeyDown(false);player.setGameMode(GameType.CREATIVE);
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("forming-press-review"))throw new IllegalStateException("Isolated directory required");
        var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(!fixture){fixture=true;try{
            level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            player.teleportTo(level,.5,101,-4,0,16);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            processChecks(level);ports(level);survival(level,player);wrenchSafety(level,player);
            var p=place(level,DISPLAY);modules(p,new ItemStack(MachineModuleItems.BUFFER.get()),ItemStack.EMPTY);place(level,DISPLAY.offset(3,0,0));mode(p,RelativeSide.LEFT,SideMode.INPUT);mode(p,RelativeSide.RIGHT,SideMode.OUTPUT);
            level.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());level.setBlockAndUpdate(DISPLAY.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            var recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.FORMING_TYPE.get()).stream().filter(r->r.getOperation()==FormingOperation.PRESS).findFirst().orElseThrow();p.getInventory().setStackInSlot(0,input(recipe).copyWithCount(64));energy(p,35000);
            ready=true;
        }catch(Throwable ex){check(false,"Probe exception "+ex);ex.printStackTrace();ready=true;}return;}
        age++;if(age%80==0&&level.getBlockEntity(DISPLAY) instanceof FormingPressBlockEntity p){energy(p,p.getDataAccess().get(1));p.getInventory().setStackInSlot(1,ItemStack.EMPTY);}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);image.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static FormingPressScreen screen(){return (FormingPressScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.west(),Direction.EAST),"Client blue input collar visible");
        check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.east(),Direction.WEST),"Client orange output collar visible");
        add(20,()->shot("01_machine_working"));add(8,()->shot("02_machine_motion"));
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,press(p.serverLevel(),DISPLAY),DISPLAY);});});
        add(25,()->{check(mc.screen instanceof FormingPressScreen,"Real networked press GUI opened");check(screen().getMenu().slots.size()==40,"40 slots including two appended module slots");check(screen().getMenu().energyCapacity()==35000&&screen().getMenu().energyStored()>32767,"Networked expanded buffer retains values above signed short range");shot("03_main_gui");tab(FormingPressMenu.SIDE_TAB);});
        add(20,()->{check(!screen().getMenu().getSlot(0).isActive(),"Machine slots hidden and disabled under settings");shot("04_side_gui");tab(FormingPressMenu.MODULE_TAB);});
        add(20,()->{check(screen().getMenu().getSlot(38).isActive()&&!screen().getMenu().getSlot(0).isActive(),"Only module sockets active on module tab");shot("05_modules_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("06_scale3");log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.createWorldOpenFlows().createFreshLevel("forming_press_review_"+System.currentTimeMillis(),new LevelSettings("Press review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>80){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client step "+ex);log("RESULT FAIL");mc.stop();}}
    }
}
