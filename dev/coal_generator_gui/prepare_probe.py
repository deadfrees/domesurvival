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
    static void plan(){var mc=Minecraft.getInstance();mc.options.hideGui=false;baked();
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
(new/'runtime_probe/CoalGeneratorProbe.java').write_text(s,encoding='utf-8')
