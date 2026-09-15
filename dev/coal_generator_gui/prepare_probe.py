"""Derive an isolated integration fixture from the model review, without changing it."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
old=root/'dev/coal_generator_visual'
new=root/'dev/coal_generator_gui'
(new/'runtime_probe').mkdir(exist_ok=True)
for filename in ['run_review.ps1','coal_test.init.gradle']:
    s=(old/filename).read_text(encoding='utf-8-sig')
    s=s.replace('coal_generator_visual','coal_generator_gui').replace('coal-generator-review','coal-generator-gui-review')
    (new/filename).write_text(s,encoding='utf-8')
s=(old/'runtime_probe/CoalGeneratorProbe.java').read_text(encoding='utf-8-sig')
s=s.replace('coal_generator_visual','coal_generator_gui').replace('coal-generator-review','coal-generator-gui-review')
s=s.replace('isPresent()&&!restored.getCapability(ForgeCapabilities.ENERGY,Direction.EAST).isPresent()',
            'isPresent()&&restored.getCapability(ForgeCapabilities.ENERGY,Direction.EAST).orElseThrow(IllegalStateException::new).canReceive()')
s=s.replace('static void plan(){', 'static void modelPlan(){')
s=s.replace('check(!level.getBlockState(p(0,100,0))', 'ports(level);check(!level.getBlockState(p(0,100,0))')
s=s.replace('for(int x:new int[]{0,3,7})level.setBlockAndUpdate', 'for(int x:new int[]{0,3,7,18,20})level.setBlockAndUpdate')
s=s.replace('gen(level,3).getInventory().setStackInSlot', '''gen(level,18).getInventory().setStackInSlot(0,new ItemStack(Items.COAL,8));
            mode(gen(level,20),RelativeSide.LEFT,SideMode.INPUT);
            level.setBlockAndUpdate(p(19,100,0),ModBlocks.BASIC_ENERGY_PIPE.get().defaultBlockState());refresh(level,p(19,100,0));
            gen(level,3).getInventory().setStackInSlot''')
s=s.replace('ports(level);check(', '''ports(level);
            check(gen(level,20).getDataAccess().get(0)>0&&gen(level,20).getDataAccess().get(2)==0,"Real energy pipe feeds blue input without fuel or combustion");
            check(level.getBlockState(p(19,100,0)).getValue(EnergyPipeBlock.EAST),"Energy pipe visually attaches to blue input");
            var fed=gen(level,7);mode(fed,RelativeSide.TOP,SideMode.OUTPUT);
            check(!level.getBlockState(p(7,101,0)).getValue(ItemPipeBlock.DOWN),"Item pipe detaches immediately when socket becomes orange");
            mode(fed,RelativeSide.TOP,SideMode.DISABLED);
            check(!level.getBlockState(p(7,101,0)).getValue(ItemPipeBlock.DOWN),"Item pipe cannot attach to OFF socket");
            check(!ItemPipeBlock.canConnect(level,p(7,100,-1),Direction.SOUTH),"Item pipe cannot attach to firebox front");
            mode(fed,RelativeSide.TOP,SideMode.INPUT);
            check(level.getBlockState(p(7,101,0)).getValue(ItemPipeBlock.DOWN),"Item pipe reconnects when socket returns to blue");
            check(''')
s=s.replace('static void shot(String name)', '''static void mode(CoalGeneratorBlockEntity g,RelativeSide side,SideMode mode){
        for(int i=0;i<3&&g.getDataAccess().get(CoalGeneratorBlockEntity.DATA_SIDES_START+side.resolve(g.getMachineFacing()).ordinal())!=mode.ordinal();i++)g.cycleSideMode(side);
    }
    static void ports(ServerLevel level){
        for(Direction facing:Direction.Plane.HORIZONTAL){
            var state=ModBlocks.COAL_GENERATOR.get().defaultBlockState().setValue(CoalGeneratorBlock.FACING,facing);
            var g=new CoalGeneratorBlockEntity(p(24,100,0),state);
            for(RelativeSide side:RelativeSide.values()){
                Direction world=side.resolve(facing);
                if(side==RelativeSide.FRONT){
                    check(g.cycleSideMode(side)==SideMode.DISABLED&&!g.getCapability(ForgeCapabilities.ENERGY,world).isPresent()&&!g.getCapability(ForgeCapabilities.ITEM_HANDLER,world).isPresent(),"Front permanently excluded "+facing);continue;
                }
                mode(g,side,SideMode.INPUT);
                var blue=g.getCapability(ForgeCapabilities.ENERGY,world);
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
    static void shot(String name)''')
# GUI interactions go through the real client menu and normal network packets.
insertion='''
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
        add(40,()->{check(gui().getMenu().slots.size()==37,"Exactly one fuel slot and 36 player slots");check(gui().getMenu().getEnergyCapacity()==50000,"Full 50000 FE capacity synchronized to client");shot("01_idle_gui");});
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
        add(5,()->log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures));add(20,mc::stop);
    }
'''
s=s.replace('    @SubscribeEvent public static void client',insertion+'    @SubscribeEvent public static void client')
s=s.replace('gui().getMenu().slots.size()==37,"Exactly one fuel slot and 36 player slots"',
            'gui().getMenu().slots.size()==38,"One fuel, one module and 36 player slots"')
s=s.replace('static void ports(ServerLevel level){','''static void upgrades(ServerLevel level){
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
    static void ports(ServerLevel level){upgrades(level);''')
s=s.replace('var blue=g.getCapability(ForgeCapabilities.ENERGY,world);',
            'check(!g.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided item fallback cannot bypass port mode "+facing+"/"+side);var blue=g.getCapability(ForgeCapabilities.ENERGY,world);')
extra='''
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
'''
# Only the GUI plan needs these actions; the unused model plan is retained as a reference.
start=s.index('    static void plan(){')
s=s[:start]+s[start:].replace('        add(5,()->log("RESULT ',extra+'        add(5,()->log("RESULT ',1)
(new/'runtime_probe/CoalGeneratorProbe.java').write_text(s,encoding='utf-8')
