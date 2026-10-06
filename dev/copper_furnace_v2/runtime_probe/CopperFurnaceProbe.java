package com.wasted.domesurvival.copperprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.*;
import com.wasted.domesurvival.forge.machine.copper.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.itempipe.*;
import com.wasted.domesurvival.forge.client.jei.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
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
public final class CopperFurnaceProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.copperFurnaceReview");
    static final Path OUT=Path.of("../../dev/copper_furnace_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[COPPER_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static CopperFurnaceBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,ModBlocks.COPPER_FURNACE.get().defaultBlockState());return (CopperFurnaceBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,CopperFurnaceBlockEntity f,int n){for(int i=0;i<n;i++)CopperFurnaceBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(CopperFurnaceBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.facing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void smelting(ServerLevel l,ServerPlayer player){
        var f=place(l,TEST);f.setItem(0,new ItemStack(Items.RAW_IRON,16));f.setItem(1,new ItemStack(Items.COAL,2));
        check(f.fuelDuration(new ItemStack(Items.COAL))==2000,"Baseline coal lasts 2000 ticks");
        tick(l,f,159);check(f.getItem(2).isEmpty()&&f.getDataAccess().get(3)==160,"No output before 160 ticks");
        tick(l,f,1);check(f.getItem(2).is(Items.IRON_INGOT)&&f.getItem(2).getCount()==1&&f.getItem(0).getCount()==15,"Exact 8 second vanilla smelt");
        check(f.getBlockState().getLightEmission(l,TEST)==13,"Burning copper furnace illuminates its surroundings");
        check(ModBlocks.COPPER_FURNACE.get().defaultBlockState().getLightEmission(l,TEST)==0,"Idle furnace emits no light");
        check(!f.saveWithoutMetadata().getCompound("RecipesUsed").isEmpty(),"Vanilla recipe XP retained");
        var menu=new CopperFurnaceMenu(77,player.getInventory(),f);menu.setTab(CopperFurnaceMenu.MODULE_TAB);
        player.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        int burn=f.getDataAccess().get(0),duration=f.getDataAccess().get(1),progress=f.getDataAccess().get(2);
        check(!menu.quickMoveStack(player,3).isEmpty()&&f.hasEfficiency(),"Shift-click installs efficiency during burning");
        check(f.getDataAccess().get(0)==burn&&f.getDataAccess().get(1)==duration&&f.getDataAccess().get(2)==progress,"Hot installation cannot refill fuel or reset progress");
        check(f.fuelDuration(new ItemStack(Items.COAL))==2300,"Efficiency gives exactly 15 percent fuel bonus");
        check(!f.getModules().insertItem(0,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty(),"Overdrive rejected");
        var nbt=f.saveWithoutMetadata();nbt.putInt("DomeBurnTime",1);f.load(nbt);tick(l,f,1);
        check(f.getDataAccess().get(0)==2300&&f.getDataAccess().get(1)==2300,"Next coal receives efficiency bonus");
        int running=f.getDataAccess().get(2);f.getModules().extractItem(0,1,false);
        check(f.getDataAccess().get(0)==2300&&f.getDataAccess().get(1)==2300&&f.getDataAccess().get(2)==running,"Hot removal preserves burning fuel and cycle");
        check(f.fuelDuration(new ItemStack(Items.COAL))==2000,"Following fuel returns to base duration");
        nbt=f.saveWithoutMetadata();var clone=new CopperFurnaceBlockEntity(TEST,f.getBlockState());clone.load(nbt);
        check(clone.getDataAccess().get(0)==2300&&clone.getDataAccess().get(1)==2300&&clone.getDataAccess().get(2)==running,"Reload restores burn duration with empty fuel slot");
        nbt.putInt("DomeBurnTime",80000);nbt.putInt("DomeBurnDuration",90000);clone.load(nbt);
        check(clone.getDataAccess().get(0)==80000&&clone.saveWithoutMetadata().getInt("DomeBurnDuration")==90000,"Fuel persistence is not truncated to signed short");
        var legacy=f.saveWithoutMetadata();for(String key:new String[]{"Modules","UnifiedSideConfig","DomeBurnTime","DomeBurnDuration","DomeCookProgress","DomeCookTotal"})legacy.remove(key);
        clone.load(legacy);check(clone.getModules().getSlots()==1&&clone.getItem(0).getCount()==15,"Legacy furnace NBT remains compatible");
        f=place(l,TEST);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));f.setItem(0,new ItemStack(Items.RAW_IRON,2));f.setItem(1,new ItemStack(Items.COAL));
        tick(l,f,159);check(f.getItem(2).isEmpty(),"Efficiency does not accelerate smelting");tick(l,f,1);check(f.getItem(2).getCount()==1,"Efficiency still takes 160 ticks");
        f=place(l,TEST);f.setItem(0,new ItemStack(Items.RAW_IRON));f.setItem(1,new ItemStack(Items.COAL));f.setItem(2,new ItemStack(Items.IRON_INGOT,64));tick(l,f,180);
        check(f.getItem(0).getCount()==1&&f.getItem(1).getCount()==1&&f.getDataAccess().get(0)==0,"Blocked output never consumes fresh fuel or input");
        f=place(l,TEST);f.setItem(0,new ItemStack(Items.RAW_IRON));f.setItem(1,new ItemStack(Items.LAVA_BUCKET));tick(l,f,1);
        check(f.getItem(1).is(Items.BUCKET),"Vanilla fuel container remainder preserved");
    }
    static void ports(ServerLevel l){
        for(Direction facing:Direction.Plane.HORIZONTAL){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(CopperFurnaceBlock.FACING,facing));
            for(RelativeSide s:RelativeSide.values())for(SideMode m:new SideMode[]{SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT}){
                mode(f,s,m);Direction d=s.resolve(facing);boolean front=s==RelativeSide.FRONT;
                var cap=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);
                check((cap!=null)==(!front&&m!=SideMode.DISABLED),"Item port "+facing+"/"+s+"/"+m);
                check(!f.getCapability(ForgeCapabilities.ENERGY,d).isPresent(),"No electric capability "+facing+"/"+s+"/"+m);
                if(cap!=null){
                    f.setItem(0,ItemStack.EMPTY);f.setItem(1,ItemStack.EMPTY);f.setItem(2,new ItemStack(Items.IRON_INGOT,2));
                    if(m==SideMode.INPUT){check(cap.insertItem(0,new ItemStack(Items.RAW_IRON),true).isEmpty()&&cap.insertItem(1,new ItemStack(Items.COAL),true).isEmpty(),"Blue accepts material and fuel "+s);check(cap.insertItem(0,new ItemStack(Items.DIAMOND),true).getCount()==1&&cap.extractItem(0,1,true).isEmpty(),"Blue rejects invalid input and extraction "+s);}
                    else {check(cap.extractItem(0,1,true).is(Items.IRON_INGOT)&&!cap.insertItem(0,new ItemStack(Items.IRON_INGOT),true).isEmpty(),"Orange only extracts result "+s);f.setItem(1,new ItemStack(Items.COAL));check(cap.extractItem(1,1,true).isEmpty(),"Orange protects usable fuel "+s);f.setItem(1,new ItemStack(Items.BUCKET));check(cap.extractItem(1,1,true).is(Items.BUCKET),"Orange returns empty container "+s);}
                }
            }
            check(!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"No unsided automation bypass "+facing);
        }
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.STONE_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.setItem(0,new ItemStack(Items.RAW_IRON,3));f.setItem(1,new ItemStack(Items.COAL,2));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.COPPER_FURNACE.get().asItem())==(tool==Items.STONE_PICKAXE?1:0),"Survival tool level "+tool);
            check(count(l,Items.RAW_IRON)==3&&count(l,Items.COAL)==2&&count(l,MachineModuleItems.EFFICIENCY.get())==1,"Survival contents and module drop once "+tool);
        }
        var f=place(l,TEST);clear(l);f.setItem(0,new ItemStack(Items.RAW_IRON,7));f.setItem(1,new ItemStack(Items.COAL,3));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));tick(l,f,80);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);
        mode(f,RelativeSide.LEFT,SideMode.OUTPUT);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(f.facing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.facing()))==SideMode.OUTPUT,"Native wrench rotates side configuration");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.COPPER_FURNACE.get().asItem())==0,"Native wrench creates one world drop without inventory insertion");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        check(saved.getInt("DomeBurnTime")==expected.getInt("DomeBurnTime")&&saved.getInt("DomeCookProgress")==80&&saved.getCompound("Modules").equals(expected.getCompound("Modules")),"Portable furnace retains fuel progress and module");
        entity.playerTouch(p);check(entity.isAlive(),"Portable furnace uses normal pickup delay");clear(l);
        p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(CopperFurnaceBlockEntity)l.getBlockEntity(TEST);
        check(f!=null&&f.hasEfficiency()&&f.getItem(0).getCount()==7&&f.getDataAccess().get(2)==80,"BlockItem replacement restores actual furnace contents");
        for(Direction side:Direction.values())check(f.getBlockState().getValue(CopperFurnaceBlock.portProperty(side))==PortVisual.fromMode(f.sideMode(side)),"Replaced port visuals agree with automation "+side);
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.getBlockEntity(TEST)==f,"Adventure mode cannot dismantle furnace");
        p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("copper-furnace-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            p.teleportTo(l,.5,101,-4,0,16);smelting(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);f.setItem(0,new ItemStack(Items.RAW_IRON,64));f.setItem(1,new ItemStack(Items.COAL,64));
            mode(f,RelativeSide.LEFT,SideMode.INPUT);mode(f,RelativeSide.RIGHT,SideMode.OUTPUT);
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());l.setBlockAndUpdate(DISPLAY.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            l.setBlockAndUpdate(DISPLAY.offset(3,0,0),ModBlocks.COAL_GENERATOR.get().defaultBlockState());
            p.teleportTo(l,1.5,102,-4,0,25);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server probe exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static CopperFurnaceScreen screen(){return (CopperFurnaceScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.west(),Direction.EAST),"Client blue connector visible");check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.east(),Direction.WEST),"Client orange connector visible");
        add(20,()->shot("01_furnace_working"));add(9,()->shot("02_fire_animation"));
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var f=(CopperFurnaceBlockEntity)p.serverLevel().getBlockEntity(DISPLAY);NetworkHooks.openScreen(p,f,DISPLAY);});});
        add(25,()->{check(mc.screen instanceof CopperFurnaceScreen,"Real networked copper GUI opened");check(screen().getMenu().burnTotal()==2000&&screen().getMenu().progressMax()==160,"Fuel and processing values synchronized");shot("03_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("03b_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();tab(202);});
        add(20,()->{check(!screen().getMenu().getSlot(0).isActive()&&!screen().getMenu().getSlot(1).isActive(),"Hidden input slots disabled under settings");shot("04_side_gui");tab(200);});
        add(20,()->{check(screen().getMenu().getSlot(39).isActive(),"Module socket active on its tab");shot("05_module_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("06_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.closeContainer();jeiChecks();});
        add(25,()->{shot("07_jei_press_a");});add(12,()->{shot("08_jei_press_b");JeiCapture.runtime.getRecipesGui().showTypes(List.of(RefinedFuelMachinesJeiPlugin.COPPER));});
        add(25,()->{shot("09_jei_copper");JeiCapture.runtime.getRecipesGui().showTypes(List.of(RefinedFuelMachinesJeiPlugin.COAL));});
        add(25,()->{shot("10_jei_coal");mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.offset(-2,0,1);
                l.setBlockAndUpdate(pos,com.wasted.domesurvival.forge.machine.forming.FormingPressRegistry.FORMING_PRESS.get().defaultBlockState());
                var press=(com.wasted.domesurvival.forge.machine.forming.FormingPressBlockEntity)l.getBlockEntity(pos);
                var products=l.getRecipeManager().getAllRecipesFor(com.wasted.domesurvival.forge.recipe.ModRecipes.FORMING_TYPE.get());
                int i=9;for(var recipe:products)if(i<36)p.getInventory().setItem(i++,recipe.getResult().copy());
                NetworkHooks.openScreen(p,press,pos);
            });
        });
        add(25,()->{check(mc.screen instanceof com.wasted.domesurvival.forge.machine.forming.FormingPressScreen,"Press GUI and native pixel products displayed");shot("11_press_product_icons");mc.player.closeContainer();mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setDayTime(18000));});
        add(25,()->{shot("12_furnace_night");mc.options.hideGui=false;mc.setScreen(null);});
        materialReview();
        add(10,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
    static void materialReview(){
        var mc=Minecraft.getInstance();
        add(10,()->mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            p.serverLevel().setDayTime(6000);
            NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,player)->new ItemConnectorMenu(id,inv,DISPLAY.west(),Direction.EAST),
                net.minecraft.network.chat.Component.translatable("gui.domesurvival.item_pipe.connector_title")),buf->{buf.writeBlockPos(DISPLAY.west());buf.writeByte(Direction.EAST.get3DDataValue());});
        }));
        for(var mode:List.of(ItemConnectorMode.INPUT,ItemConnectorMode.OUTPUT,ItemConnectorMode.DISABLED)){
            add(20,()->{
                check(mc.screen instanceof com.wasted.domesurvival.forge.client.itempipe.ItemConnectorScreen,"Networked connector screen available");
                int column=mode==ItemConnectorMode.INPUT?0:mode==ItemConnectorMode.OUTPUT?1:2;
                mc.screen.mouseClicked((mc.screen.width-236)/2+12+73*column+33,(mc.screen.height-176)/2+104,0);
            });
            add(20,()->{
                var screen=(com.wasted.domesurvival.forge.client.itempipe.ItemConnectorScreen)mc.screen;
                check(screen.getMenu().mode()==mode,"Connector button synchronized "+mode);
                shot("13_connector_"+mode.id());
            });
        }
        add(10,()->{mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(15,()->{shot("14_connector_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.closeContainer();
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();
                try{
                    var manifest=com.google.gson.JsonParser.parseString(Files.readString(OUT.getParent().getParent().resolve("materials_visual/manifest.json"))).getAsJsonObject();
                    p.getInventory().clearContent();int i=0;
                    for(var entry:manifest.getAsJsonArray("items")){
                        var id=entry.getAsJsonObject().get("id").getAsString();var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival",id));
                        check(item!=null&&item!=Items.AIR,"Material item registered "+id);p.getInventory().setItem(i++,new ItemStack(item));
                    }
                    var pos=DISPLAY.offset(-2,0,1);NetworkHooks.openScreen(p,(com.wasted.domesurvival.forge.machine.forming.FormingPressBlockEntity)l.getBlockEntity(pos),pos);
                }catch(Exception ex){check(false,"Material inventory fixture "+ex);}
            });
        });
        add(25,()->{shot("15_material_inventory");mc.player.closeContainer();mc.options.hideGui=true;mc.options.fov().set(70);
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();
                String[] metals={"tin","lead","silver","nickel","goteium","voltarium"};
                for(int x=-3;x<=17;x++)for(int y=100;y<=104;y++)l.setBlockAndUpdate(new BlockPos(x,y,10),(y>=102?Blocks.STONE:Blocks.DEEPSLATE).defaultBlockState());
                for(int i=0;i<metals.length;i++)for(boolean deep:new boolean[]{false,true}){
                    String id=(deep?"deepslate_":"")+metals[i]+"_ore";
                    l.setBlockAndUpdate(new BlockPos(i*3-1,deep?101:103,10),ForgeRegistries.BLOCKS.getValue(new ResourceLocation("domesurvival",id)).defaultBlockState());
                }
                String[] blocks={"tin","lead","silver","nickel","goteium","voltarium","steel","neosteel"};
                for(int i=0;i<blocks.length;i++)l.setBlockAndUpdate(new BlockPos(i*2,100,7),ForgeRegistries.BLOCKS.getValue(new ResourceLocation("domesurvival",blocks[i]+"_block")).defaultBlockState());
                p.teleportTo(l,7.5,103,-3,0,9);
            });
        });
        add(35,()->{shot("16_material_blocks");mc.options.hideGui=false;mc.setScreen(null);});
    }
    static void jeiChecks(){
        var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
        for(var type:List.of(FormingPressJeiPlugin.FORMING_PRESS,RefinedFuelMachinesJeiPlugin.COPPER,RefinedFuelMachinesJeiPlugin.COAL))
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(type.getUid())).count()==1,"Single JEI category "+type.getUid());
        var press=manager.createRecipeLookup(FormingPressJeiPlugin.FORMING_PRESS).get().toList();
        check(press.size()==24&&press.stream().map(r->r.getId()).distinct().count()==24,"All press recipes exactly once");
        for(var recipe:press){
            var id=ForgeRegistries.ITEMS.getKey(recipe.getResult().getItem());
            var texture=new ResourceLocation(id.getNamespace(),"textures/item/press_products/"+id.getPath()+".png");
            try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open();var img=NativeImage.read(stream)){
                check(img.getWidth()==32&&img.getHeight()==32,"Restored 32px inventory/GUI sprite "+id);
            }catch(Exception ex){check(false,"Product texture "+id+": "+ex);}
        }
        check(manager.createRecipeLookup(RefinedFuelMachinesJeiPlugin.COAL).get().count()==3,"Exactly three supported generator fuels");
        long expected=Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING).size();
        check(manager.createRecipeLookup(RefinedFuelMachinesJeiPlugin.COPPER).get().count()==expected,"All actual smelting recipes available in copper category");
        var focus=runtime.getJeiHelpers().getFocusFactory().createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,mezz.jei.api.constants.VanillaTypes.ITEM_STACK,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","steel_gear"))));
        var focused=manager.createRecipeLookup(FormingPressJeiPlugin.FORMING_PRESS).limitFocus(List.of(focus)).get().toList();
        check(focused.size()==1&&focused.get(0).getResult().is(ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","steel_gear"))),"Gear lookup resolves its actual press recipe once");
        runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(FormingPressJeiPlugin.FORMING_PRESS),focused,List.of(focus));
    }
    @SubscribeEvent public static void mouse(TickEvent.RenderTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.START||!planned)return;
        try{var mc=Minecraft.getInstance();
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(MouseHandler.class,"f_91507_").setDouble(mc.mouseHandler,0);
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(MouseHandler.class,"f_91508_").setDouble(mc.mouseHandler,0);
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("copper_review_"+System.currentTimeMillis(),new LevelSettings("Copper review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client step "+ex);ex.printStackTrace();log("RESULT FAIL");mc.stop();}}
    }
}
