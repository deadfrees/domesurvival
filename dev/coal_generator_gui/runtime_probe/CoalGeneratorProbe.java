package com.wasted.domesurvival.coalgeneratorprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.itempipe.*;
import com.wasted.domesurvival.forge.machine.coal.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlock;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import java.nio.file.*;
import java.util.*;

/** Isolated fixture; never included in the production source set. */
@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class CoalGeneratorProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.coalGeneratorReview");
    static final Path OUT=Path.of("../../dev/coal_generator_gui/runtime").toAbsolutePath().normalize();
    static boolean started,fixture,planned;static volatile boolean ready;static int age,ticks,step,failures;
    static final List<Step> steps=new ArrayList<>();record Step(int delay,Runnable action){}
    static BlockPos p(int x,int y,int z){return new BlockPos(x,y,z);}
    static CoalGeneratorBlockEntity gen(ServerLevel level,int x){return (CoalGeneratorBlockEntity)level.getBlockEntity(p(x,100,0));}
    static synchronized void log(String s){System.out.println("[COAL_GENERATOR_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static void refresh(ServerLevel level,BlockPos p){var s=level.getBlockState(p);if(s.getBlock() instanceof EnergyPipeBlock)level.setBlockAndUpdate(p,EnergyPipeBlock.refreshConnections(level,p,s));if(s.getBlock() instanceof ItemPipeBlock)level.setBlockAndUpdate(p,ItemPipeBlock.refreshConnections(level,p,s));}
    static void camera(double x,double y,double z,float yaw,float pitch){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();server.getPlayerList().getPlayers().get(0).teleportTo(server.overworld(),x,y,z,yaw,pitch);});}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!ENABLED||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("coal-generator-gui-review"))throw new IllegalStateException("Isolated directory required");
        var level=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        if(!fixture){fixture=true;level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-6;z<12;z++)level.setBlockAndUpdate(p(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,2,101,-5,0,16);
            for(int x:new int[]{0,3,7,18,20})level.setBlockAndUpdate(p(x,100,0),ModBlocks.COAL_GENERATOR.get().defaultBlockState());
            gen(level,18).getInventory().setStackInSlot(0,new ItemStack(Items.COAL,8));
            mode(gen(level,20),RelativeSide.LEFT,SideMode.INPUT);
            level.setBlockAndUpdate(p(19,100,0),ModBlocks.BASIC_ENERGY_PIPE.get().defaultBlockState());refresh(level,p(19,100,0));
            gen(level,3).getInventory().setStackInSlot(0,new ItemStack(Items.COAL,8));
            for(int x=8;x<11;x++)level.setBlockAndUpdate(p(x,100,0),ModBlocks.BASIC_ENERGY_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(p(11,100,0),ModBlocks.ENERGY_BUFFER.get().defaultBlockState());
            for(int x=8;x<11;x++)refresh(level,p(x,100,0));
            for(int y=101;y<103;y++)level.setBlockAndUpdate(p(7,y,0),ItemPipeRegistry.COPPER_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(p(7,103,0),Blocks.CHEST.defaultBlockState());
            var chest=(IItemHandlerModifiable)level.getBlockEntity(p(7,103,0)).getCapability(ForgeCapabilities.ITEM_HANDLER,null).orElseThrow(()->new IllegalStateException("chest capability"));chest.setStackInSlot(0,new ItemStack(Items.COAL,32));
            for(int y=101;y<103;y++)refresh(level,p(7,y,0));
            ((ItemPipeBlockEntity)level.getBlockEntity(p(7,102,0))).setConnectorMode(Direction.UP,ItemConnectorMode.OUTPUT);
            ((ItemPipeBlockEntity)level.getBlockEntity(p(7,101,0))).setConnectorMode(Direction.DOWN,ItemConnectorMode.INPUT);
            int i=0;for(Direction d:Direction.Plane.HORIZONTAL){level.setBlockAndUpdate(p(i*3,100,5),ModBlocks.COAL_GENERATOR.get().defaultBlockState().setValue(CoalGeneratorBlock.FACING,d));i++;}
            BlockPos buried=p(0,100,9);level.setBlockAndUpdate(buried,ModBlocks.COAL_GENERATOR.get().defaultBlockState());
            ((CoalGeneratorBlockEntity)level.getBlockEntity(buried)).getInventory().setStackInSlot(0,new ItemStack(Items.COAL,8));
            for(Direction d:Direction.values())level.setBlockAndUpdate(buried.relative(d),Blocks.DIRT.defaultBlockState());
            for(Direction d:Direction.values())check(Block.shouldRenderFace(Blocks.DIRT.defaultBlockState(),level,buried.relative(d),d.getOpposite(),buried),"Adjacent dirt face retained at recessed generator: "+d);
            for(int x=-1;x<=1;x++)for(int z=8;z<=10;z++){
                level.setBlockAndUpdate(p(x,101,z),Blocks.DIRT.defaultBlockState());
                if(x!=0)level.setBlockAndUpdate(p(x,100,z),Blocks.DIRT.defaultBlockState());
            }
            level.removeBlock(buried.north(),false);
            player.getInventory().setItem(0,new ItemStack(ModBlocks.COAL_GENERATOR.get()));player.getInventory().setItem(1,new ItemStack(ModBlocks.BASIC_ENERGY_PIPE.get()));player.getInventory().setItem(2,new ItemStack(ItemPipeRegistry.COPPER_PIPE.get()));ready=true;return;
        }
        age++;
        if(age==100){
            ports(level);
            check(gen(level,20).getDataAccess().get(0)>0&&gen(level,20).getDataAccess().get(2)==0,"Real energy pipe feeds blue input without fuel or combustion");
            check(level.getBlockState(p(19,100,0)).getValue(EnergyPipeBlock.EAST),"Energy pipe visually attaches to blue input");
            var fed=gen(level,7);mode(fed,RelativeSide.TOP,SideMode.OUTPUT);
            check(!level.getBlockState(p(7,101,0)).getValue(ItemPipeBlock.DOWN),"Item pipe detaches immediately when socket becomes orange");
            mode(fed,RelativeSide.TOP,SideMode.DISABLED);
            check(!level.getBlockState(p(7,101,0)).getValue(ItemPipeBlock.DOWN),"Item pipe cannot attach to OFF socket");
            check(!ItemPipeBlock.canConnect(level,p(7,100,-1),Direction.SOUTH),"Item pipe cannot attach to firebox front");
            mode(fed,RelativeSide.TOP,SideMode.INPUT);
            check(level.getBlockState(p(7,101,0)).getValue(ItemPipeBlock.DOWN),"Item pipe reconnects when socket returns to blue");
            check(!level.getBlockState(p(0,100,0)).getValue(CoalGeneratorBlock.LIT),"Idle generator remains unlit");
            check(level.getBlockState(p(3,100,0)).getValue(CoalGeneratorBlock.LIT),"Fueled generator uses lit model");
            check(level.getBlockState(p(7,100,0)).getValue(CoalGeneratorBlock.LIT),"Coal delivered by item pipes starts generator");
            check(level.getBlockState(p(7,101,0)).getValue(ItemPipeBlock.DOWN),"Item pipe meets top fuel socket");
            check(level.getBlockState(p(8,100,0)).getValue(EnergyPipeBlock.WEST),"Energy pipe meets side output socket");
            var buffer=level.getBlockEntity(p(11,100,0)).getCapability(ForgeCapabilities.ENERGY,Direction.WEST).orElseThrow(()->new IllegalStateException("buffer input"));
            check(buffer.getEnergyStored()>0,"Real energy reaches buffer through energy pipes");
            for(int x:new int[]{0,3,7}){
                var generator=gen(level,x);check(!generator.getCapability(ForgeCapabilities.ENERGY,Direction.NORTH).isPresent(),"Front energy capability remains disabled "+x);
                check(generator.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).isPresent(),"Top item input preserved "+x);
            }
            var generator=gen(level,0);check(generator.cycleSideMode(RelativeSide.RIGHT)==SideMode.DISABLED,"Output switches off");
            check(level.getBlockState(p(0,100,0)).getValue(CoalGeneratorBlock.PORT_EAST)==PortVisual.OFF,"OFF removes active connector overlay");
            check(generator.cycleSideMode(RelativeSide.RIGHT)==SideMode.INPUT,"OFF switches to input");
            check(level.getBlockState(p(0,100,0)).getValue(CoalGeneratorBlock.PORT_EAST)==PortVisual.INPUT,"Input activates blue socket");
            var saved=generator.saveWithoutMetadata();var restored=new CoalGeneratorBlockEntity(p(0,100,0),level.getBlockState(p(0,100,0)));restored.load(saved);
            check(restored.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST).isPresent()&&restored.getCapability(ForgeCapabilities.ENERGY,Direction.EAST).orElseThrow(IllegalStateException::new).canReceive(),"Configured socket survives NBT round trip");
        }
    }
    static void mode(CoalGeneratorBlockEntity g,RelativeSide side,SideMode mode){
        for(int i=0;i<3&&g.getDataAccess().get(CoalGeneratorBlockEntity.DATA_SIDES_START+side.resolve(g.getMachineFacing()).ordinal())!=mode.ordinal();i++)g.cycleSideMode(side);
    }
    static void upgrades(ServerLevel level){
        var state=ModBlocks.COAL_GENERATOR.get().defaultBlockState();
        var g=new CoalGeneratorBlockEntity(p(25,100,0),state);
        var bufferItem=com.wasted.domesurvival.forge.machine.module.MachineModuleItems.BUFFER.get();
        var legacy=g.saveWithoutMetadata();legacy.remove("Modules");g.load(legacy);
        check(g.getModules().getSlots()==1&&g.getDataAccess().get(1)==50000,"Legacy generator keeps empty module slot and base capacity");
        var module=new ItemStack(bufferItem);
        check(g.getModules().insertItem(0,module.copy(),true).isEmpty()&&g.getModules().getStackInSlot(0).isEmpty()&&g.getDataAccess().get(1)==50000,"Simulating module insertion has no effect");
        check(g.getModules().insertItem(0,module.copy(),false).isEmpty()&&g.getDataAccess().get(1)==87500,"Buffer module raises capacity by 75 percent");
        for(var item:com.wasted.domesurvival.forge.machine.module.MachineModuleItems.ITEMS.getEntries()){
            if(item.get()!=bufferItem)check(!g.getModules().isItemValid(0,new ItemStack(item.get())),"Reject unsupported module "+item.getId());
        }
        check(!g.getInventory().isItemValid(0,module)&&g.getModules().getSlotLimit(0)==1,"Module is not fuel and cannot stack");
        var saved=g.saveWithoutMetadata();saved.putInt("Energy",80000);saved.putInt("BurnTime",1200);saved.putInt("MaxBurnTime",1600);g.load(saved);
        var clone=new CoalGeneratorBlockEntity(g.getBlockPos(),state);clone.load(g.saveWithoutMetadata());
        check(clone.getDataAccess().get(0)==80000&&clone.getDataAccess().get(1)==87500&&clone.getDataAccess().get(2)==1200&&clone.getModules().getStackInSlot(0).is(bufferItem),"Installed module, energy and combustion persist together");
        var player=Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
        var menu=new CoalGeneratorMenu(42,player.getInventory(),g);menu.setModulePanelOpen(true);
        check(!menu.getSlot(37).mayPickup(player)&&menu.quickMoveStack(player,37).isEmpty()&&g.getDataAccess().get(0)==80000,"Cannot remove capacity module while surplus energy is stored");
        saved=g.saveWithoutMetadata();saved.putInt("Energy",40000);g.load(saved);
        check(menu.getSlot(37).mayPickup(player)&&!menu.quickMoveStack(player,37).isEmpty()&&g.getDataAccess().get(1)==50000&&g.getDataAccess().get(0)==40000&&g.getDataAccess().get(2)==1200,"Safe module removal preserves energy and fuel progress");
        level.setBlockAndUpdate(p(25,100,0),state);
        var placed=(CoalGeneratorBlockEntity)level.getBlockEntity(p(25,100,0));placed.getModules().insertItem(0,module.copy(),false);
        var blockItem=placed.getBlockState().getBlock().getCloneItemStack(placed.getBlockState(),new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(placed.getBlockPos()),Direction.NORTH,placed.getBlockPos(),false),level,placed.getBlockPos(),player);
        check(blockItem.getTagElement("BlockEntityTag")!=null&&blockItem.getTagElement("BlockEntityTag").contains("Modules"),"Dismantling clone includes installed module NBT");
        level.removeBlock(p(25,100,0),false);
        int drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(p(25,100,0)).inflate(1)).stream().filter(e->e.getItem().is(bufferItem)).mapToInt(e->e.getItem().getCount()).sum();
        check(drops==1,"Breaking the generator drops its module once");
    }
    static void ports(ServerLevel level){upgrades(level);
        for(Direction facing:Direction.Plane.HORIZONTAL){
            var state=ModBlocks.COAL_GENERATOR.get().defaultBlockState().setValue(CoalGeneratorBlock.FACING,facing);
            var g=new CoalGeneratorBlockEntity(p(24,100,0),state);
            for(RelativeSide side:RelativeSide.values()){
                Direction world=side.resolve(facing);
                if(side==RelativeSide.FRONT){
                    check(g.cycleSideMode(side)==SideMode.DISABLED&&!g.getCapability(ForgeCapabilities.ENERGY,world).isPresent()&&!g.getCapability(ForgeCapabilities.ITEM_HANDLER,world).isPresent(),"Front permanently excluded "+facing);continue;
                }
                mode(g,side,SideMode.INPUT);
                check(!g.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided item fallback cannot bypass port mode "+facing+"/"+side);var blue=g.getCapability(ForgeCapabilities.ENERGY,world);
                var input=blue.orElseThrow(IllegalStateException::new);
                int before=input.getEnergyStored();
                check(input.canReceive()&&!input.canExtract()&&input.receiveEnergy(1000,true)==128&&input.getEnergyStored()==before,"Blue FE simulation "+facing+"/"+side);
                check(input.receiveEnergy(1000,false)==128&&input.getEnergyStored()==before+128&&input.extractEnergy(128,false)==0,"Blue FE input only "+facing+"/"+side);
                var itemCap=g.getCapability(ForgeCapabilities.ITEM_HANDLER,world);
                var items=itemCap.orElseThrow(IllegalStateException::new);g.getInventory().setStackInSlot(0,ItemStack.EMPTY);
                check(items.insertItem(0,new ItemStack(Items.COAL,3),true).isEmpty()&&items.getStackInSlot(0).isEmpty(),"Fuel simulation "+facing+"/"+side);
                check(items.insertItem(0,new ItemStack(Items.COAL,3),false).isEmpty()&&items.getStackInSlot(0).getCount()==3&&items.extractItem(0,3,false).isEmpty()&&!items.isItemValid(0,new ItemStack(Items.IRON_INGOT)),"Blue accepts fuel only, no extraction "+facing+"/"+side);
                var copy=new CoalGeneratorBlockEntity(g.getBlockPos(),state);copy.load(g.saveWithoutMetadata());
                check(copy.getCapability(ForgeCapabilities.ENERGY,world).orElseThrow(IllegalStateException::new).canReceive()&&copy.getDataAccess().get(0)==g.getDataAccess().get(0)&&copy.getInventory().getStackInSlot(0).getCount()==3,"Blue data persists "+facing+"/"+side);
                mode(g,side,SideMode.OUTPUT);
                var output=g.getCapability(ForgeCapabilities.ENERGY,world).orElseThrow(IllegalStateException::new);
                before=output.getEnergyStored();
                check(!blue.isPresent()&&!itemCap.isPresent()&&!g.getCapability(ForgeCapabilities.ITEM_HANDLER,world).isPresent(),"Mode switch invalidates input capabilities; orange has no items "+facing+"/"+side);
                check(output.canExtract()&&!output.canReceive()&&output.receiveEnergy(1000,false)==0&&output.extractEnergy(1000,true)==128&&output.getEnergyStored()==before&&output.extractEnergy(1000,false)==128&&output.getEnergyStored()==before-128,"Orange FE output only "+facing+"/"+side);
                mode(g,side,SideMode.DISABLED);
                check(!g.getCapability(ForgeCapabilities.ENERGY,world).isPresent()&&!g.getCapability(ForgeCapabilities.ITEM_HANDLER,world).isPresent(),"Off seals both channels "+facing+"/"+side);
            }
            mode(g,RelativeSide.TOP,SideMode.INPUT);var saved=g.saveWithoutMetadata();saved.putInt("Energy",49950);g.load(saved);
            var input=g.getCapability(ForgeCapabilities.ENERGY,Direction.UP).orElseThrow(IllegalStateException::new);
            check(input.receiveEnergy(1000,false)==50&&input.getEnergyStored()==50000&&input.receiveEnergy(1,false)==0,"Input cannot overfill buffer "+facing);
        }
    }
    static void shot(String name){try(NativeImage image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);image.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void baked(){var mc=Minecraft.getInstance();int valid=0,oriented=0;var block=ModBlocks.COAL_GENERATOR.get();
        for(BlockState state:block.getStateDefinition().getPossibleStates()){
            int active=0;for(Direction d:Direction.values())if(state.getValue(CoalGeneratorBlock.portProperty(d))!=PortVisual.OFF)active++;
            var model=mc.getBlockRenderer().getBlockModel(state);var quads=model.getQuads(state,null,RandomSource.create(1),ModelData.EMPTY,RenderType.cutout());
            boolean lit=state.getValue(CoalGeneratorBlock.LIT);
            if(quads.size()==703+27*active+(lit?20:0)&&quads.stream().noneMatch(q->q.getSprite().contents().name().toString().contains("missingno")))valid++;
            var coal=quads.stream().filter(q->q.getSprite().contents().name().toString().endsWith(lit?"/coal_on":"/coal")).toList();
            int flameCount=(int)quads.stream().filter(q->q.getSprite().contents().name().toString().contains("/flame")).count();
            Direction facing=state.getValue(CoalGeneratorBlock.FACING);int axis=facing.getAxis().ordinal();double sum=0;int count=0;
            for(var quad:coal){var vertices=quad.getVertices();for(int k=0;k<vertices.length;k+=8){sum+=Float.intBitsToFloat(vertices[k+axis]);count++;}}
            boolean front=count>0&&(facing.getAxisDirection()==Direction.AxisDirection.POSITIVE?sum/count>.5:sum/count<.5);
            if(coal.size()==120&&flameCount==(lit?20:0)&&front)oriented++;
        }
        check(valid==5832,"All 5832 facing/lit/port states have correct quads and valid textures: "+valid);
        check(oriented==5832,"3D coals and crossed flame layers follow facing and lit state in all 5832 states: "+oriented);
        check(!block.defaultBlockState().canOcclude(),"Recessed model no longer hides neighbouring block faces");
        var stack=new ItemStack(block);check(mc.getItemRenderer().getModel(stack,mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Inventory model valid");
        check(block.defaultBlockState().getCollisionShape(mc.level,BlockPos.ZERO).bounds().getSize()==1,"Original full-block collision retained");
    }
    static void modelPlan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;baked();
        add(35,()->shot("01_idle_and_working"));add(5,()->camera(4.8,99.5,-1.8,32,17));add(35,()->shot("02_working_close"));add(9,()->shot("02b_animated_front"));
        add(5,()->camera(2.2,99.5,-1.8,-32,17));add(35,()->shot("02c_firebox_depth"));
        add(5,()->camera(11,102,-4,30,27));add(40,()->shot("03_connected_energy_and_items"));
        add(5,()->camera(2.5,102,-2.1,40,38));add(40,()->shot("04_input_socket"));
        add(5,()->camera(5,105,3,0,55));add(40,()->shot("05_four_orientations"));
        add(5,()->camera(.5,99,7.15,0,0));add(40,()->shot("05b_buried_no_missing_faces"));
        add(5,()->{mc.options.hideGui=false;mc.setScreen(new InventoryScreen(mc.player));});add(30,()->shot("06_inventory"));
        add(5,()->{mc.player.closeContainer();mc.setScreen(null);mc.player.getInventory().selected=0;});add(30,()->shot("07_in_hand"));
        add(5,()->log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures));add(20,mc::stop);
    }

    static com.wasted.domesurvival.forge.client.screen.CoalGeneratorScreen gui(){
        var screen=Minecraft.getInstance().screen;
        if(!(screen instanceof com.wasted.domesurvival.forge.client.screen.CoalGeneratorScreen))throw new IllegalStateException("Generator GUI not open: "+screen);
        return (com.wasted.domesurvival.forge.client.screen.CoalGeneratorScreen)screen;
    }
    static void openGui(int x){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().get(0);
        player.teleportTo(server.overworld(),x+.5,100,-2,0,0);
        net.minecraftforge.network.NetworkHooks.openScreen(player,gen(server.overworld(),x),p(x,100,0));
    });}
    static void click(int x,int y){var g=gui();g.mouseClicked((g.width-220)/2+x,(g.height-266)/2+y,0);}
    static void plan(){var mc=Minecraft.getInstance();mc.options.hideGui=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);baked();
        mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();
        add(100,()->openGui(0));
        add(40,()->{check(gui().getMenu().slots.size()==38,"One fuel, one module and 36 player slots");check(gui().getMenu().getEnergyCapacity()==50000,"Full 50000 FE capacity synchronized to client");shot("01_idle_gui");});
        add(5,()->{mc.player.closeContainer();openGui(3);});add(40,()->shot("02_working_gui"));
        add(5,()->click(202,16));add(20,()->shot("03_configuration"));
        add(5,()->click(56,81));add(15,()->check(gui().getMenu().getSideMode(RelativeSide.FRONT)==SideMode.DISABLED,"Front GUI click cannot enable port"));
        add(5,()->click(32,81));add(15,()->check(gui().getMenu().getSideMode(RelativeSide.RIGHT)==SideMode.DISABLED,"Viewer left click changes machine RIGHT over network"));
        add(5,()->click(32,81));add(15,()->{check(gui().getMenu().getSideMode(RelativeSide.RIGHT)==SideMode.INPUT,"Second click selects blue input and synchronizes");shot("04_blue_side");});
        add(5,()->{mc.options.guiScale().set(3);mc.resizeDisplay();});add(25,()->{check(gui().width>=220&&gui().height>=266,"Configuration fits viewport at scale 3");shot("05_gui_scale3");});
        add(5,()->click(202,16));add(20,()->shot("06_working_scale3"));
        add(5,()->{mc.getSingleplayerServer().execute(()->{var g=gen(mc.getSingleplayerServer().overworld(),3);var tag=g.saveWithoutMetadata();tag.putInt("Energy",50000);g.load(tag);g.setChanged();});});
        add(20,()->{check(gui().getMenu().getEnergyStored()==50000,"Full buffer value synchronized without overflow");shot("07_full_buffer");});
        add(5,()->{mc.player.closeContainer();mc.options.guiScale().set(2);mc.resizeDisplay();openGui(0);});
        add(25,()->{mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().get(0);player.getInventory().setItem(9,new ItemStack(Items.COAL,16));player.containerMenu.broadcastChanges();});});
        add(15,()->mc.gameMode.handleInventoryMouseClick(gui().getMenu().containerId,1,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player));
        add(20,()->{check(gui().getMenu().getSlot(0).getItem().getCount()==15,"Shift-click transfers fuel, generator consumes exactly one");shot("08_fuel_shift_click");});

        add(5,()->{mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);player.getInventory().setItem(9,new ItemStack(com.wasted.domesurvival.forge.machine.module.MachineModuleItems.BUFFER.get()));player.containerMenu.broadcastChanges();});});
        add(20,()->{check(!gui().getMenu().getSlot(37).isActive(),"Module slot hidden on main tab");mc.gameMode.handleInventoryMouseClick(gui().getMenu().containerId,1,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);});
        add(15,()->check(gui().getMenu().getSlot(37).getItem().isEmpty(),"Closed module tab cannot accept shift-click upgrades"));
        add(5,()->click(178,16));add(15,()->{check(gui().getMenu().getSlot(37).isActive(),"Module tab activates module slot");shot("09_modules_empty");});
        add(5,()->mc.gameMode.handleInventoryMouseClick(gui().getMenu().containerId,1,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player));
        add(20,()->{check(gui().getMenu().getSlot(37).hasItem()&&gui().getMenu().getEnergyCapacity()==87500,"Installing real module synchronizes 87500 FE capacity");
            var item=gui().getMenu().getSlot(37).getItem();var model=mc.getItemRenderer().getModel(item,mc.level,mc.player,0);
            check(model.isGui3d()&&!model.getQuads(null,null,RandomSource.create(2)).isEmpty(),"Buffer module uses a baked 3D model");shot("10_module_installed");});
        add(5,()->mc.getSingleplayerServer().execute(()->{var g=gen(mc.getSingleplayerServer().overworld(),0);var tag=g.saveWithoutMetadata();tag.putInt("Energy",80000);g.load(tag);g.setChanged();}));
        add(20,()->{check(gui().getMenu().getEnergyStored()>65535,"Expanded stored energy synchronizes above 16-bit range");mc.gameMode.handleInventoryMouseClick(gui().getMenu().containerId,37,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);});
        add(15,()->check(gui().getMenu().getSlot(37).hasItem(),"Networked removal refuses to discard surplus energy"));
        add(5,()->click(178,16));add(15,()->shot("11_expanded_energy"));
        add(5,()->{click(178,16);mc.getSingleplayerServer().execute(()->{var g=gen(mc.getSingleplayerServer().overworld(),0);var tag=g.saveWithoutMetadata();tag.putInt("Energy",0);g.load(tag);g.setChanged();});});
        add(20,()->mc.gameMode.handleInventoryMouseClick(gui().getMenu().containerId,37,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player));
        add(15,()->check(gui().getMenu().getSlot(37).getItem().isEmpty()&&gui().getMenu().getEnergyCapacity()==50000,"Module can be removed after buffer is drained"));
        add(5,()->log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures));add(20,mc::stop);
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!ENABLED||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);
            mc.createWorldOpenFlows().createFreshLevel("coal_generator_review_"+System.currentTimeMillis(),new LevelSettings("Coal generator review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>160){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;steps.get(step++).action().run();}
    }
}
