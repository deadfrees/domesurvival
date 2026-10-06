from pathlib import Path
R=Path(__file__).resolve().parents[2];out=Path(__file__).parent;src=R/'dev/shaft_furnace_v2'
(out/'runtime_probe').mkdir(exist_ok=True)
def rename(s):
    for a,b in [('ShaftFurnace','WaterPurifier'),('shaftFurnace','waterPurifier'),('shaft_furnace','water_purifier'),('shaft-furnace','water-purifier'),('shaftprobe','waterprobe'),('SHAFT_FURNACE','WATER_PURIFIER'),('SHAFT_REVIEW','WATER_REVIEW'),('machine.shaft','machine.water')]:s=s.replace(a,b)
    return s.replace('.facing()','.getMachineFacing()')
s=rename((src/'runtime_probe/ShaftFurnaceProbe.java').read_text())
s=s.replace('import java.nio.file.*;','import java.nio.file.*;\nimport net.minecraftforge.fluids.*;\nimport net.minecraftforge.fluids.capability.*;\nimport net.minecraft.world.level.material.Fluids;\nimport com.wasted.domesurvival.forge.fluid.ModFluids;\nimport com.wasted.domesurvival.forge.item.WaterFilterItem;')
a=s.index('    static void process(');b=s.index('    @SubscribeEvent public static void server',a)
s=s[:a]+'''    static void energy(WaterPurifierBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}
    static void water(WaterPurifierBlockEntity f,int raw,int pure){var n=f.saveWithoutMetadata();n.put("RawTank",new FluidStack(Fluids.WATER,raw).writeToNBT(new net.minecraft.nbt.CompoundTag()));n.put("PurifiedTank",new FluidStack(ModFluids.PURIFIED_WATER.get(),pure).writeToNBT(new net.minecraft.nbt.CompoundTag()));f.load(n);}
    static void process(ServerLevel l,ServerPlayer player){
        for(var filter:List.of(ModItems.WATER_FILTER_CARTRIDGE.get(),ModItems.IMPROVED_WATER_FILTER.get()))for(int variant=0;variant<3;variant++){
            var f=place(l,TEST);if(variant>0)f.getModules().setStackInSlot(0,new ItemStack(variant==1?MachineModuleItems.EFFICIENCY.get():MachineModuleItems.OVERDRIVE.get()));
            f.getInventory().setStackInSlot(1,new ItemStack(filter));water(f,1000,0);energy(f,20000);
            int time=f.getDataAccess().get(7),cost=f.getDataAccess().get(15);tick(l,f,time-1);check(f.purifiedAmount()==0,"No early fluid result "+filter+"/"+variant);
            tick(l,f,1);check(f.rawAmount()==750&&f.purifiedAmount()==200,"Exact 250 to 200 mB conversion "+filter+"/"+variant);
            check(20000-f.getDataAccess().get(0)==cost,"Exact energy cost "+filter+"/"+variant);
            check(f.getInventory().getStackInSlot(1).getDamageValue()==1,"One filter durability per cycle "+variant);
        }
        var f=place(l,TEST);f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));water(f,1000,0);energy(f,20000);tick(l,f,40);
        var menu=new WaterPurifierMenu(3,player.getInventory(),f);menu.setTab(200);player.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(!menu.quickMoveStack(player,2).isEmpty(),"Shift-click installs while working");check(menu.progressMax()==200&&menu.cycleEnergy()==3000,"Hot insertion preserves active cycle");
        var restored=new WaterPurifierBlockEntity(TEST,f.getBlockState());restored.load(f.saveWithoutMetadata());check(restored.getDataAccess().get(7)==200&&restored.getDataAccess().get(15)==3000,"Active cycle snapshot survives save");
        tick(l,f,160);check(f.purifiedAmount()==200&&menu.progressMax()==223&&menu.cycleEnergy()==2400,"Next cycle adopts efficiency");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty(),"Conflicting speed modules rejected");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()),false).isEmpty(),"Duplicate modules rejected");
        f.getModules().setStackInSlot(1,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,34000);check(menu.energyCapacity()==35000&&!menu.getSlot(39).mayPickup(player),"Charged buffer cannot be removed");
        energy(f,20000);check(menu.getSlot(39).mayPickup(player)&&!menu.quickMoveStack(player,39).isEmpty()&&menu.energyCapacity()==20000,"Safe buffer removal preserves energy");
        tick(l,f,20);int progress=menu.progress();water(f,750,3900);tick(l,f,20);check(menu.progress()==progress,"Full tank pauses unfinished cycle");water(f,750,0);tick(l,f,1);check(menu.progress()==progress+1,"Freed tank resumes cycle");
        energy(f,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No energy pauses cycle");energy(f,10000);tick(l,f,1);check(menu.progress()==progress+1,"Power restored resumes cycle");
        water(f,0,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No water pauses cycle");
        f.getInventory().setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));tick(l,f,1);check(f.rawAmount()==1000&&f.getInventory().getStackInSlot(0).is(Items.BUCKET),"Water bucket becomes one empty bucket");
        var spent=new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get());spent.setDamageValue(spent.getMaxDamage()-1);spent.getOrCreateTag().putInt("RegenerationCycles",2);f.getInventory().setStackInSlot(1,spent);energy(f,20000);tick(l,f,menu.progressMax());
        check(f.getInventory().getStackInSlot(1).getDamageValue()==spent.getMaxDamage()&&f.getInventory().getStackInSlot(1).getTag().getInt("RegenerationCycles")==2,"Exhausted cartridge retains regeneration NBT");
        int result=f.purifiedAmount();tick(l,f,300);check(f.purifiedAmount()==result,"Exhausted filter stops processing");
        menu.setTab(202);check(!menu.getSlot(0).isActive()&&!menu.getSlot(1).mayPickup(player)&&!menu.getSlot(38).isActive(),"Hidden slots protected");
        player.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(player,100+RelativeSide.FRONT.ordinal()),"Front menu button rejected");
        var legacy=f.saveWithoutMetadata();legacy.remove("PortFacing");var sides=legacy.getCompound("UnifiedSideConfig");for(Direction d:Direction.values())sides.putString(d.getName(),d==f.getMachineFacing()?"disabled":"output");f.load(legacy);check(f.sideMode(Direction.UP)==SideMode.INPUT,"Legacy all-output defaults gain functioning input");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(WaterPurifierBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fluids=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);
                boolean open=side!=RelativeSide.FRONT&&m!=SideMode.DISABLED;check((items!=null)==open&&(fluids!=null)==open&&(fe!=null)==(open&&m==SideMode.INPUT),"Capabilities "+facing+"/"+side+"/"+m);
                if(!open)continue;water(f,1000,1000);energy(f,1000);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);
                if(m==SideMode.INPUT){
                    check(fluids.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.SIMULATE)==250&&fluids.drain(250,IFluidHandler.FluidAction.SIMULATE).isEmpty(),"Blue water fill only "+side);
                    check(fluids.fill(new FluidStack(Fluids.LAVA,250),IFluidHandler.FluidAction.EXECUTE)==0&&fe.receiveEnergy(1000,true)==64&&fe.extractEnergy(1000,false)==0,"Blue FE limit and fluid validation "+side);
                    check(items.insertItem(0,new ItemStack(Items.WATER_BUCKET),true).isEmpty()&&items.insertItem(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()),true).isEmpty()&&!items.insertItem(1,new ItemStack(Items.COAL),true).isEmpty(),"Blue item validation "+side);
                    mode(f,side,SideMode.DISABLED);check(fe.receiveEnergy(64,false)==0&&fluids.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0&&!items.insertItem(0,new ItemStack(Items.WATER_BUCKET),false).isEmpty(),"Cached input obeys OFF "+side);
                }else{
                    check(fluids.drain(200,IFluidHandler.FluidAction.SIMULATE).getFluid()==ModFluids.PURIFIED_WATER.get()&&fluids.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0,"Orange drains purified water only "+side);
                    f.getInventory().setStackInSlot(0,new ItemStack(Items.BUCKET));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));check(items.extractItem(0,1,true).is(Items.BUCKET)&&items.extractItem(1,1,true).isEmpty(),"Orange protects working filter "+side);
                    var spent=f.getInventory().getStackInSlot(1);spent.setDamageValue(spent.getMaxDamage());check(!items.extractItem(1,1,true).isEmpty(),"Orange releases spent filter "+side);
                    mode(f,side,SideMode.DISABLED);check(fluids.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty()&&items.extractItem(0,1,false).isEmpty(),"Cached output obeys OFF "+side);
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,null).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided access denied "+facing);
        }
        var f=place(l,TEST);var neighbor=place(l,TEST.south());mode(neighbor,RelativeSide.FRONT,SideMode.DISABLED);l.setBlockAndUpdate(TEST.south(),neighbor.getBlockState().setValue(WaterPurifierBlock.FACING,Direction.SOUTH));tick(l,neighbor,1);mode(neighbor,RelativeSide.BACK,SideMode.INPUT);
        // A raw-water inlet must reject this machine's purified output.
        water(f,0,1000);tick(l,f,1);check(f.purifiedAmount()==1000&&neighbor.rawAmount()==0,"Automatic export cannot contaminate raw tank");l.removeBlock(TEST.south(),false);
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.WATER_PURIFIER.get().asItem())==(tool==Items.IRON_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,Items.WATER_BUCKET)==1&&count(l,ModItems.WATER_FILTER_CARTRIDGE.get())==1&&count(l,MachineModuleItems.BUFFER.get())==1,"Contents and module drop exactly once "+tool);
        }
        var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,3000,1000);energy(f,34000);tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.WATER_PURIFIER.get().asItem())==0,"Wrench creates one world drop");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Inventory","Modules","Energy","RawTank","PurifiedTank","Progress","CycleTicks","CycleEnergy","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(WaterPurifierBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.rawAmount()==3000&&f.purifiedAmount()==1000&&f.getDataAccess().get(0)==expected.getInt("Energy")&&f.getDataAccess().get(6)==40,"Actual placement restores fluid energy and cycle");
        check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,f.getMachineFacing()).isPresent(),"Placed ports follow orientation");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
'''+s[b:]
a=s.index('            var f=place(l,DISPLAY);');b=s.index('ready=true;',a)
s=s[:a]+'''            var f=place(l,DISPLAY);f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,3500,1000);energy(f,35000);
            l.setBlockAndUpdate(DISPLAY.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            p.teleportTo(l,1.5,102,-4,0,25);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));'''+s[b:]
s=s.replace('screen().getMenu().burnTotal()==80000&&screen().getMenu().burnRemaining()>65535','screen().getMenu().energyCapacity()==35000&&screen().getMenu().energyStored()>20000')
s=s.replace('32-bit fuel value synchronized','Expanded FE buffer synchronized')
s=s.replace('containerId,4,0,ClickType.QUICK_MOVE','containerId,2,0,ClickType.QUICK_MOVE')
s=s.replace('screen().getMenu().efficiency()&&screen().getMenu().getSlot(40).hasItem()','screen().getMenu().getSlot(39).hasItem()')
s=s.replace('.get().count()==1,"Exactly one shaft recipe"','.get().count()==2,"Two distinct filter recipes"')
s=s.replace('Exactly one shaft JEI category','Exactly one purifier JEI category').replace('Networked shaft GUI opens','Networked purifier GUI opens')
a=s.index('        add(10,()->otherGui(ModBlocks.COKE_OVEN.get()));');b=s.index('        add(10,()->{mc.setScreen(null);',a);s=s[:a]+s[b:]
(out/'runtime_probe/WaterPurifierProbe.java').write_text(s)
for srcname,dstname in [('runtime_probe/JeiCapture.java','runtime_probe/JeiCapture.java'),('review.init.gradle','review.init.gradle'),('run_review.ps1','run_review.ps1')]: (out/dstname).write_text(rename((src/srcname).read_text()))
p=R/'run/water-purifier-review/config/fancymenu';p.mkdir(parents=True,exist_ok=True)
(p/'options.txt').write_bytes((R/'run/shaft-furnace-review/config/fancymenu/options.txt').read_bytes())
print('WATER_PURIFIER_PROBE_PREPARED')
