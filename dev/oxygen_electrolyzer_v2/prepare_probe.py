"""Isolated real-client integration probe for electrolysis, ports, modules and UI."""
from pathlib import Path
R=Path(__file__).resolve().parents[2];O=Path(__file__).parent;S=R/'dev/water_purifier_v2'
(O/'runtime_probe').mkdir(exist_ok=True)
def rename(s):
    for a,b in [('WaterPurifier','OxygenElectrolyzer'),('waterPurifier','oxygenElectrolyzer'),('water_purifier','oxygen_electrolyzer'),('water-purifier','oxygen-electrolyzer'),('waterprobe','electrolyzerprobe'),('WATER_PURIFIER','OXYGEN_ELECTROLYZER'),('WATER_REVIEW','ELECTROLYZER_REVIEW'),('machine.water','machine.oxygen')]:s=s.replace(a,b)
    return s
s=rename((S/'runtime_probe/WaterPurifierProbe.java').read_text())
s=s.replace('import java.nio.file.*;','import java.nio.file.*;\nimport com.wasted.domesurvival.forge.capability.ModCapabilities;\nimport com.wasted.domesurvival.forge.transport.fluid.*;')
a=s.index('    static void water(');b=s.index('    static void dismantle(',a)
s=s[:a]+'''    static void water(OxygenElectrolyzerBlockEntity f,int water,int oxygen){var n=f.saveWithoutMetadata();n.put("PurifiedWater",new FluidStack(ModFluids.PURIFIED_WATER.get(),water).writeToNBT(new net.minecraft.nbt.CompoundTag()));n.putInt("Oxygen",oxygen);f.load(n);}
    static void process(ServerLevel l,ServerPlayer player){
        check(!ModBlocks.OXYGEN_ELECTROLYZER.get().defaultBlockState().canOcclude(),"Recessed shell does not hide neighboring block faces");
        int[] times={200,223,149},costs={2400,1920,3840};
        for(int variant=0;variant<3;variant++){
            var f=place(l,TEST);if(variant>0)f.getModules().setStackInSlot(0,new ItemStack(variant==1?MachineModuleItems.EFFICIENCY.get():MachineModuleItems.OVERDRIVE.get()));
            water(f,1000,0);energy(f,30000);
            check(f.getDataAccess().get(7)==times[variant]&&f.getDataAccess().get(15)==costs[variant],"Expected module time/cost "+variant);
            tick(l,f,times[variant]-1);check(f.oxygenAmount()==0&&f.waterAmount()==1000,"No early result "+variant);
            tick(l,f,1);check(f.waterAmount()==800&&f.oxygenAmount()==96,"Exact water to oxygen "+variant);
            check(30000-f.getDataAccess().get(0)==costs[variant],"Exact charged energy "+variant);
        }
        var f=place(l,TEST);water(f,1000,0);energy(f,30000);tick(l,f,40);
        var menu=new OxygenElectrolyzerMenu(3,player.getInventory(),f);menu.setTab(200);player.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(!menu.quickMoveStack(player,0).isEmpty(),"Hot module insertion");check(menu.progressMax()==200&&menu.cycleEnergy()==2400,"Active cycle unchanged by insertion");
        var saved=f.saveWithoutMetadata();var restored=new OxygenElectrolyzerBlockEntity(TEST,f.getBlockState());restored.load(saved);
        check(restored.getDataAccess().get(6)==40&&restored.getDataAccess().get(7)==200&&restored.getDataAccess().get(15)==2400,"Cycle snapshot survives save");
        tick(l,f,160);check(f.oxygenAmount()==96&&menu.progressMax()==223&&menu.cycleEnergy()==1920,"Next cycle adopts efficiency");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty(),"Conflicting modules denied");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()),false).isEmpty(),"Duplicate modules denied");
        tick(l,f,20);check(!menu.quickMoveStack(player,36).isEmpty()&&menu.progressMax()==223&&menu.cycleEnergy()==1920,"Hot removal preserves current cycle");
        tick(l,f,203);check(menu.progressMax()==200&&menu.cycleEnergy()==2400,"Following cycle returns to base");
        f.getModules().setStackInSlot(1,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,52000);
        check(menu.energyCapacity()==52500&&!menu.getSlot(37).mayPickup(player)&&menu.quickMoveStack(player,37).isEmpty(),"Charged buffer removal blocked");
        energy(f,30000);check(menu.getSlot(37).mayPickup(player)&&!menu.quickMoveStack(player,37).isEmpty()&&menu.energyCapacity()==30000,"Safe buffer removal");
        tick(l,f,20);int progress=menu.progress();water(f,800,3950);tick(l,f,20);check(menu.progress()==progress&&!f.getBlockState().getValue(OxygenElectrolyzerBlock.LIT),"Full oxygen buffer pauses");
        water(f,800,0);tick(l,f,1);check(menu.progress()==progress+1,"Freed oxygen buffer resumes");
        energy(f,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No power pauses");energy(f,10000);tick(l,f,1);check(menu.progress()==progress+1,"Restored power resumes");
        water(f,0,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No water pauses");water(f,800,0);tick(l,f,1);check(menu.progress()==progress+1,"Restored water resumes");
        menu.setTab(202);check(!menu.getSlot(36).isActive()&&!menu.getSlot(36).mayPickup(player),"Hidden module slots protected");
        player.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(player,100+RelativeSide.FRONT.ordinal()),"Front button denied");
        var legacy=f.saveWithoutMetadata();legacy.remove("PortFacing");legacy.remove("CycleTicks");legacy.remove("CycleEnergy");legacy.putInt("Progress",70);
        var sides=legacy.getCompound("UnifiedSideConfig");for(Direction d:Direction.values())sides.putString(d.getName(),d==f.getMachineFacing()?"disabled":"output");
        f.load(legacy);check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.getDataAccess().get(6)==70&&f.getDataAccess().get(15)==2400,"Legacy defaults and paid cycle migrate");
        mode(f,RelativeSide.BACK,SideMode.DISABLED);var custom=f.saveWithoutMetadata();custom.remove("PortFacing");f.load(custom);check(f.sideMode(Direction.SOUTH)==SideMode.DISABLED,"Legacy custom OFF retained");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(OxygenElectrolyzerBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);water(f,1000,1000);energy(f,1000);
                var fluids=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);var gas=f.getCapability(ModCapabilities.OXYGEN,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((fluids!=null)==input&&(fe!=null)==input&&(gas!=null)==output&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).isPresent(),"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(OxygenElectrolyzerBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual port agrees "+facing+"/"+side+"/"+m);
                if(input){
                    check(fluids.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),250),IFluidHandler.FluidAction.SIMULATE)==250&&f.waterAmount()==1000,"Input simulation unchanged "+side);
                    check(fluids.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0&&fluids.fill(new FluidStack(Fluids.LAVA,250),IFluidHandler.FluidAction.EXECUTE)==0,"Only purified water accepted "+side);
                    check(fluids.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),250),IFluidHandler.FluidAction.EXECUTE)==250&&f.waterAmount()==1250&&fluids.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Water input only "+side);
                    check(fe.receiveEnergy(1000,true)==64&&fe.extractEnergy(1000,false)==0&&fe.canReceive()&&!fe.canExtract(),"FE direction and limit "+side);
                    var copy=fluids.getFluidInTank(0);copy.setAmount(1);check(f.waterAmount()==1250,"Tank view cannot mutate storage "+side);
                    mode(f,side,SideMode.DISABLED);check(!fe.canReceive()&&fe.receiveEnergy(64,false)==0&&fluids.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),100),IFluidHandler.FluidAction.EXECUTE)==0,"Cached input obeys OFF "+side);
                }else if(output){
                    check(gas.extractOxygen(1000,true)==120&&f.oxygenAmount()==1000&&gas.receiveOxygen(100,false)==0,"Oxygen simulation and direction "+side);
                    check(gas.extractOxygen(1000,false)==120&&f.oxygenAmount()==880,"Oxygen extraction amount "+side);
                    mode(f,side,SideMode.DISABLED);check(!gas.canExtract()&&gas.extractOxygen(100,false)==0,"Cached output obeys OFF "+side);
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,null).isPresent()&&!f.getCapability(ModCapabilities.OXYGEN,null).isPresent(),"Unsided bypass denied "+facing);
        }
        var f=place(l,TEST);var pipePos=TEST.south();var gasPipe=ModBlocks.OXYGEN_PIPE.get().defaultBlockState();l.setBlockAndUpdate(pipePos,gasPipe);
        for(var m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
            mode(f,RelativeSide.BACK,m);check(OxygenPipeBlock.refreshConnections(l,pipePos,gasPipe).getValue(OxygenPipeBlock.NORTH)==(m==SideMode.OUTPUT),"Gas pipe connects only to orange "+m);
            var fluidPipe=FluidPipeRegistry.BASIC_FLUID_PIPE.get().defaultBlockState();
            check(FluidPipeBlock.refreshConnections(l,pipePos,fluidPipe).getValue(FluidPipeBlock.NORTH)==(m==SideMode.INPUT),"Fluid pipe connects only to blue "+m);
        }
        var sinkPos=TEST.south(2);
        l.setBlockAndUpdate(sinkPos,ModBlocks.OXYGEN_FILLER.get().defaultBlockState().setValue(OxygenFillerBlock.FACING,Direction.SOUTH));
        var sink=l.getBlockEntity(sinkPos).getCapability(ModCapabilities.OXYGEN,Direction.NORTH).orElseThrow(()->new IllegalStateException("Missing filler inlet"));
        l.setBlockAndUpdate(pipePos,OxygenPipeBlock.refreshConnections(l,pipePos,l.getBlockState(pipePos)));
        water(f,1000,1000);
        for(var m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
            mode(f,RelativeSide.BACK,m);
            int before=f.oxygenAmount(),received=OxygenPipeTransferService.pull(l,sinkPos,sink,100,d->d==Direction.NORTH);
            check(m==SideMode.OUTPUT?received>0&&f.oxygenAmount()==before-received:received==0&&f.oxygenAmount()==before,"Real oxygen pipe transfer obeys port "+m);
        }
        check(sink.getOxygenStored()>0,"Filler receives oxygen through real pipe");
        l.removeBlock(pipePos,false);l.removeBlock(sinkPos,false);
    }
'''+s[b:]
# Strip item handling, retain the real mining and wrench interactions.
s=s.replace('f.getInventory().setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));','').replace('f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));','')
s=s.replace('count(l,Items.WATER_BUCKET)==1&&count(l,ModItems.WATER_FILTER_CARTRIDGE.get())==1&&','').replace('Contents and module drop','Module drops')
s=s.replace('"Inventory","Modules","Energy","RawTank","PurifiedTank"','"Modules","Energy","PurifiedWater","Oxygen"')
s=s.replace('f.rawAmount()','f.waterAmount()').replace('f.purifiedAmount()','f.oxygenAmount()').replace('energy(f,34000)','energy(f,52000)').replace('energy(f,35000)','energy(f,52500)')
s=s.replace('l.setBlockAndUpdate(DISPLAY.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());','l.setBlockAndUpdate(DISPLAY.west(),FluidPipeRegistry.BASIC_FLUID_PIPE.get().defaultBlockState());l.setBlockAndUpdate(DISPLAY.east(),ModBlocks.OXYGEN_PIPE.get().defaultBlockState());')
s=s.replace('animated.pumpAngle(1)','animated.animationPhase(1)').replace('Pump moves','Bubbles move').replace('Pump freezes','Bubbles freeze')
a=s.index('        check(ItemPipeBlock.hasObjectConnector');b=s.index('        add(20,()->shot',a)
s=s[:a]+'''        check(FluidPipeBlock.refreshConnections(mc.level,DISPLAY.west(),mc.level.getBlockState(DISPLAY.west())).getValue(FluidPipeBlock.EAST),"Client fluid connection");
        check(OxygenPipeBlock.refreshConnections(mc.level,DISPLAY.east(),mc.level.getBlockState(DISPLAY.east())).getValue(OxygenPipeBlock.WEST),"Client oxygen connection");
'''+s[b:]
s=s.replace('energyCapacity()==35000','energyCapacity()==52500').replace('energyStored()>20000','energyStored()>30000')
s=s.replace('containerId,2,0,ClickType','containerId,0,0,ClickType').replace('getSlot(39)','getSlot(37)')
s=s.replace('.count()==2,"Two distinct filter recipes"','.count()==1,"One electrolysis recipe"')
s=s.replace('shaft_review_','electrolyzer_review_').replace('Shaft review','Electrolyzer review')
s=s.replace('p.teleportTo(l,1.5,102,-4,0,25)','p.teleportTo(l,1.6,100.3,-2.7,19,23)')
s=s.replace('Exactly one purifier JEI category','Exactly one electrolyzer JEI category')
(O/'runtime_probe/OxygenElectrolyzerProbe.java').write_text(s)
for name in ('runtime_probe/JeiCapture.java','review.init.gradle','run_review.ps1'):
    (O/name).write_text(rename((S/name).read_text()))
s=(R/'dev/forming_press_v2/verify_visual_assets.py').read_text().replace("body=model('forming_press')","body=model('oxygen_electrolyzer')").replace("f'forming_press_{mode}_port_{direction}'","f'coal_generator_{mode}_port_{direction}'").replace('press_enclosure','electrolyzer_enclosure')
(O/'verify_visual_assets.py').write_text(s)
s=rename((S/'verify_release.py').read_text()).replace('machine/water/','machine/oxygen/').replace("purifier's tested", "electrolyzer's tested")
(O/'verify_release.py').write_text(s)
print('Electrolyzer isolated runtime probe prepared')
