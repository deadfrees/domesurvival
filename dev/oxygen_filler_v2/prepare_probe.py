from pathlib import Path
R=Path(__file__).resolve().parents[2];D=Path(__file__).parent;OLD=D.with_name('oxygen_electrolyzer_v2')
def rename(s):
    for a,b in [('OxygenElectrolyzer','OxygenFiller'),('oxygenElectrolyzer','oxygenFiller'),('OXYGEN_ELECTROLYZER','OXYGEN_FILLER'),('oxygen_electrolyzer','oxygen_filler'),('oxygen-electrolyzer','oxygen-filler'),('electrolyzerprobe','fillerprobe'),('ELECTROLYZER_REVIEW','FILLER_REVIEW'),('electrolyzer_review','filler_review')]:s=s.replace(a,b)
    return s
for name in ('review.init.gradle','run_review.ps1','build_release.ps1','verify_visual_assets.py','verify_release.py'):
    (D/name).write_text(rename((OLD/name).read_text(encoding='utf-8-sig')),encoding='utf-8')
(D/'runtime_probe').mkdir(exist_ok=True)
(D/'runtime_probe/JeiCapture.java').write_text(rename((OLD/'runtime_probe/JeiCapture.java').read_text()))
s=rename((OLD/'runtime_probe/OxygenElectrolyzerProbe.java').read_text())
s=s.replace('import com.wasted.domesurvival.forge.item.ModItems;','import com.wasted.domesurvival.forge.item.ModItems;\nimport com.wasted.domesurvival.forge.item.OxygenTankItem;\nimport com.wasted.domesurvival.forge.oxygen.room.*;')
a=s.index('    static void water(');b=s.index('    static void dismantle(',a)
s=s[:a]+'''    static void water(OxygenFillerBlockEntity f,int ignored,int oxygen){var n=f.saveWithoutMetadata();n.putInt("Oxygen",oxygen);f.load(n);}
    static ItemStack tank(Item item,int amount){var stack=new ItemStack(item);((OxygenTankItem)item).setOxygen(stack,amount);return stack;}
    static void process(ServerLevel l,ServerPlayer player){
        check(!ModBlocks.OXYGEN_FILLER.get().defaultBlockState().canOcclude(),"Recessed shell preserves neighbor faces");
        for(Item item:List.of(ModItems.SMALL_OXYGEN_TANK.get(),ModItems.MEDIUM_OXYGEN_TANK.get(),ModItems.LARGE_OXYGEN_TANK.get()))for(boolean efficient:List.of(false,true)){
            var f=place(l,TEST);if(efficient)f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
            int capacity=((OxygenTankItem)item).capacity();water(f,0,6000);energy(f,20000);
            var stack=tank(item,0);stack.setHoverName(net.minecraft.network.chat.Component.literal("Preserved tank"));f.getInventory().setStackInSlot(0,stack);
            tick(l,f,capacity-1);check(f.getTankOxygen()==capacity-1&&f.getInventory().getStackInSlot(1).isEmpty(),"No early output "+item+"/"+efficient);
            tick(l,f,1);var output=f.getInventory().getStackInSlot(1);
            check(f.getInventory().getStackInSlot(0).isEmpty()&&output.is(item)&&((OxygenTankItem)item).getOxygen(output)==capacity&&output.hasCustomHoverName(),"Full tank and NBT moved to output "+item+"/"+efficient);
            check(f.oxygenAmount()==6000-capacity&&f.getDataAccess().get(0)==20000-capacity*(efficient?4:5),"Exact oxygen and FE cost "+item+"/"+efficient);
            var old=f.saveWithoutMetadata();tick(l,f,20);check(f.oxygenAmount()==old.getInt("Oxygen")&&f.getDataAccess().get(0)==old.getInt("Energy"),"Idle consumes nothing");
        }
        var f=place(l,TEST);water(f,0,6000);energy(f,20000);f.getInventory().setStackInSlot(0,tank(ModItems.SMALL_OXYGEN_TANK.get(),119));f.getInventory().setStackInSlot(1,tank(ModItems.SMALL_OXYGEN_TANK.get(),120));
        tick(l,f,5);check(f.getTankOxygen()==120&&f.oxygenAmount()==5999&&f.getDataAccess().get(0)==19995,"Blocked output retains completed tank without repeated consumption");
        f.getInventory().extractItem(1,1,false);tick(l,f,1);check(f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).is(ModItems.SMALL_OXYGEN_TANK.get()),"Output resumes when cleared");
        f.getInventory().setStackInSlot(0,tank(ModItems.LARGE_OXYGEN_TANK.get(),0));tick(l,f,10);
        var menu=new OxygenFillerMenu(3,player.getInventory(),f);menu.setTab(200);player.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(!menu.quickMoveStack(player,0).isEmpty(),"Efficiency installs while filling");int before=menu.energyStored();tick(l,f,1);check(before-menu.energyStored()==4&&f.getTankOxygen()==11,"Efficiency applies at next per-unit fill step");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get())),"Duplicate and unsupported module rejected");
        f.getModules().setStackInSlot(1,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);
        check(menu.energyCapacity()==35000&&!menu.getSlot(39).mayPickup(player)&&menu.quickMoveStack(player,39).isEmpty(),"High charge prevents buffer removal");
        energy(f,20000);check(menu.getSlot(39).mayPickup(player)&&!menu.quickMoveStack(player,39).isEmpty()&&menu.energyCapacity()==20000,"Safe buffer removal");
        energy(f,0);before=f.getTankOxygen();tick(l,f,10);check(before==f.getTankOxygen()&&!f.getBlockState().getValue(OxygenFillerBlock.LIT),"No power pauses pump and filling");
        energy(f,1000);water(f,0,0);tick(l,f,10);check(before==f.getTankOxygen(),"No oxygen pauses filling");water(f,0,100);tick(l,f,1);check(f.getTankOxygen()==before+1,"Restored oxygen resumes filling");
        var legacy=f.saveWithoutMetadata();legacy.remove("Modules");legacy.remove("PortFacing");var inventory=legacy.getCompound("Inventory");inventory.putInt("Size",1);inventory.getList("Items",10).removeIf(t->((net.minecraft.nbt.CompoundTag)t).getInt("Slot")!=0);f.load(legacy);
        check(f.getInventory().getSlots()==2&&!f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).isEmpty(),"Legacy one-slot inventory migrates without losing tank");
        menu.setTab(202);check(!menu.getSlot(36).isActive()&&!menu.getSlot(38).isActive(),"Side panel hides machine and module slots");
        player.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(player,100+RelativeSide.FRONT.ordinal()),"Front setting denied");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(OxygenFillerBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);water(f,0,3000);energy(f,1000);
                f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,tank(ModItems.SMALL_OXYGEN_TANK.get(),120));
                var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);var gas=f.getCapability(ModCapabilities.OXYGEN,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((items!=null)==(input||output)&&(fe!=null)==input&&(gas!=null)==(input||output),"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(OxygenFillerBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual agrees "+facing+"/"+side+"/"+m);
                if(input){
                    var empty=tank(ModItems.SMALL_OXYGEN_TANK.get(),0);
                    check(items.insertItem(0,empty,true).isEmpty()&&f.getInventory().getStackInSlot(0).isEmpty(),"Tank input simulation");
                    check(items.insertItem(0,empty,false).isEmpty()&&items.extractItem(0,1,false).isEmpty()&&items.extractItem(1,1,false).isEmpty(),"Blue accepts tanks, never extracts");
                    check(!items.isItemValid(0,tank(ModItems.SMALL_OXYGEN_TANK.get(),120))&&!items.isItemValid(0,new ItemStack(Items.COAL)),"Only non-full tanks accepted");
                    check(gas.receiveOxygen(1000,true)==120&&f.oxygenAmount()==3000&&gas.extractOxygen(100,false)==0&&fe.receiveEnergy(1000,true)==64&&fe.extractEnergy(100,false)==0,"Input gas and FE limits");
                    var copy=items.getStackInSlot(0);copy.setCount(0);check(!f.getInventory().getStackInSlot(0).isEmpty(),"External item view cannot mutate inventory");
                    mode(f,side,SideMode.DISABLED);check(fe.receiveEnergy(64,false)==0&&gas.receiveOxygen(100,false)==0&&!items.isItemValid(0,empty),"Cached blue respects OFF");
                }else if(output){
                    check(items.extractItem(0,1,false).isEmpty()&&!items.insertItem(0,tank(ModItems.SMALL_OXYGEN_TANK.get(),0),false).isEmpty(),"Orange denies input and unfinished tank extraction");
                    check(items.extractItem(1,1,true).is(ModItems.SMALL_OXYGEN_TANK.get())&&!f.getInventory().getStackInSlot(1).isEmpty(),"Output simulation preserves tank");
                    check(items.extractItem(1,1,false).is(ModItems.SMALL_OXYGEN_TANK.get())&&f.getInventory().getStackInSlot(1).isEmpty()&&gas.receiveOxygen(100,false)==0,"Orange yields finished tank, no gas input");
                    mode(f,side,SideMode.DISABLED);check(items.extractItem(1,1,false).isEmpty()&&gas.extractOxygen(100,false)==0,"Cached orange respects OFF");
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent()&&!f.getCapability(ModCapabilities.OXYGEN,null).isPresent(),"Unsided bypass denied "+facing);
        }
        var f=place(l,TEST);var pipePos=TEST.south();l.setBlockAndUpdate(pipePos,ModBlocks.OXYGEN_PIPE.get().defaultBlockState());
        var sourcePos=TEST.south(2);l.setBlockAndUpdate(sourcePos,ModBlocks.OXYGEN_ELECTROLYZER.get().defaultBlockState().setValue(OxygenElectrolyzerBlock.FACING,Direction.SOUTH));
        var source=(OxygenElectrolyzerBlockEntity)l.getBlockEntity(sourcePos);var n=source.saveWithoutMetadata();n.putInt("Oxygen",1000);source.load(n);
        while(source.sideMode(Direction.NORTH)!=SideMode.OUTPUT)source.cycleSideMode(RelativeSide.BACK);
        l.setBlockAndUpdate(pipePos,OxygenPipeBlock.refreshConnections(l,pipePos,l.getBlockState(pipePos)));
        for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
            mode(f,RelativeSide.BACK,m);water(f,0,0);tick(l,f,1);check(m==SideMode.INPUT?f.oxygenAmount()>0:f.oxygenAmount()==0,"Real oxygen pipe respects filler inlet "+m);
            var itemPipe=ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState();check(ItemPipeBlock.refreshConnections(l,pipePos,itemPipe).getValue(ItemPipeBlock.NORTH)==(m!=SideMode.DISABLED),"Item pipe connects to enabled socket "+m);
        }
        mode(f,RelativeSide.BACK,SideMode.OUTPUT);mode(f,RelativeSide.TOP,SideMode.OUTPUT);water(f,0,500);
        int first=f.getCapability(ModCapabilities.OXYGEN,Direction.SOUTH).orElseThrow(()->new IllegalStateException()).extractOxygen(100,false);
        int second=f.getCapability(ModCapabilities.OXYGEN,Direction.UP).orElseThrow(()->new IllegalStateException()).extractOxygen(100,false);
        check(first+second==120&&f.oxygenAmount()==380,"Gas relay shares one 120-per-tick budget across outputs");
        l.removeBlock(pipePos,false);l.removeBlock(sourcePos,false);
    }
    static void room(ServerLevel l,BlockPos pos,boolean build){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=4;y++){
            if(x==0&&y==0&&z==0)continue;
            l.setBlockAndUpdate(pos.offset(x,y,z),(build&&(Math.abs(x)==2||Math.abs(z)==2||y==0||y==4)?(y==4?Blocks.SMOOTH_STONE:Blocks.GLASS):Blocks.AIR).defaultBlockState());
        }
        SealedRoomManager.invalidateAround(l,pos.above());
    }
    static void ventilation(ServerLevel l){
        var f=place(l,TEST);room(l,TEST,true);water(f,0,6000);energy(f,20000);f.cycleOperatingMode();tick(l,f,1);
        log("Room diagnostic mode="+f.getOperatingMode()+" status="+f.getDataAccess().get(6)+" state="+f.getDataAccess().get(8)+" volume="+f.getDataAccess().get(9)+" gas="+f.getDataAccess().get(10)+" required="+f.getDataAccess().get(11)+" airtightMachine="+SealedRoomManager.isAirtightBoundary(l,TEST,f.getBlockState())+" airAbove="+l.getBlockState(TEST.above()));
        check(f.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal()&&f.getDataAccess().get(9)==27,"Vent discovers sealed 27-block room");
        check(!f.getCapability(ModCapabilities.OXYGEN,Direction.UP).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).isPresent(),"Top reserved for ventilation outlet");
        tick(l,f,100);check(f.getDataAccess().get(10)==270&&f.getDataAccess().get(11)==270&&f.oxygenAmount()==5730&&f.getDataAccess().get(0)==18650,"Exact room oxygen capacity and energy cost");
        var n=f.saveWithoutMetadata();tick(l,f,30);check(f.oxygenAmount()==n.getInt("Oxygen")&&!f.getBlockState().getValue(OxygenFillerBlock.LIT),"Full room stops gas and visual activity");
        var gap=TEST.offset(2,2,0);l.removeBlock(gap,false);SealedRoomManager.invalidateAround(l,gap);tick(l,f,5);check(f.oxygenAmount()==n.getInt("Oxygen")&&!f.getBlockState().getValue(OxygenFillerBlock.LIT),"Leak stops ventilation instead of compensating");
        room(l,TEST,false);f.cycleOperatingMode();check(f.getCapability(ModCapabilities.OXYGEN,Direction.UP).isPresent(),"Tank mode restores top connector");
        // Transparent roofs seal by geometry even while sky light remains bright.
        BlockPos glassPos=TEST.above(8);var glass=place(l,glassPos);room(l,glassPos,true);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.setBlockAndUpdate(glassPos.offset(x,4,z),Blocks.GLASS.defaultBlockState());
        water(glass,0,6000);energy(glass,20000);glass.cycleOperatingMode();tick(l,glass,1);
        check(glass.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal()&&glass.getDataAccess().get(9)==27,"Glass roof seals immediately without waiting for lighting");
        tick(l,glass,100);check(glass.getDataAccess().get(10)==270&&glass.oxygenAmount()==5730&&glass.getDataAccess().get(0)==18650,"Glass-roof room fills with exact oxygen and FE cost");
        BlockPos roofGap=glassPos.above(4);l.removeBlock(roofGap,false);SealedRoomManager.invalidateAround(l,roofGap);tick(l,glass,1);
        check(glass.getDataAccess().get(8)!=SealedRoomManager.RoomState.SEALED.ordinal(),"Real roof opening is not treated as sealed");
        l.setBlockAndUpdate(roofGap,Blocks.GLASS.defaultBlockState());SealedRoomManager.invalidateAround(l,glassPos.above());tick(l,glass,1);
        check(glass.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal(),"Resealed glass roof restores room immediately");
        room(l,glassPos,false);l.removeBlock(glassPos,false);
    }
    static final BlockPos CACHE_ROOM=TEST.above(16);
    static void prepareNegativeCacheRoom(ServerLevel l){
        var f=place(l,CACHE_ROOM);room(l,CACHE_ROOM,true);
        l.removeBlock(CACHE_ROOM.above(4),false);water(f,0,6000);energy(f,20000);f.cycleOperatingMode();tick(l,f,1);
        check(f.getDataAccess().get(8)==SealedRoomManager.RoomState.OPEN.ordinal(),"Unroofed room is open by actual geometry");
        // The new roof lies outside the early-OPEN dependency set; simulate a
        // structure change without an event and require bounded cache recovery.
        l.setBlock(CACHE_ROOM.above(4),Blocks.GLASS.defaultBlockState(),2);
        check(SealedRoomManager.getOrDiscover(l,CACHE_ROOM.above()).state()==SealedRoomManager.RoomState.OPEN,"Negative room result remains cached before retry deadline");
    }
    static void verifyNegativeCacheRoom(ServerLevel l){
        var f=(OxygenFillerBlockEntity)l.getBlockEntity(CACHE_ROOM);
        check(f!=null&&f.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal()&&f.getDataAccess().get(10)>0,"Closed distant roof recovers and fills without manual reset");
        room(l,CACHE_ROOM,false);l.removeBlock(CACHE_ROOM,false);
    }
'''+s[b:]
s=s.replace('water(f,3000,1000);energy(f,52000);tick(l,f,40);','water(f,0,1000);energy(f,35000);f.getInventory().setStackInSlot(0,tank(ModItems.LARGE_OXYGEN_TANK.get(),100));tick(l,f,40);')
s=s.replace('"Modules","Energy","PurifiedWater","Oxygen","Progress","CycleTicks","CycleEnergy","UnifiedSideConfig"','"Modules","Energy","Oxygen","Inventory","OperatingMode","UnifiedSideConfig"')
s=s.replace('f.waterAmount()==3000&&f.oxygenAmount()==1000&&f.getDataAccess().get(0)==expected.getInt("Energy")&&f.getDataAccess().get(6)==40','f.getTankOxygen()==140&&f.oxygenAmount()==960&&f.getDataAccess().get(0)==expected.getInt("Energy")')
s=s.replace('Actual placement restores fluid energy and cycle','Actual placement restores tank, oxygen and energy')
s=s.replace('process(l,p);ports(l);dismantle(l,p);','process(l,p);ports(l);ventilation(l);dismantle(l,p);prepareNegativeCacheRoom(l);')
s=s.replace('water(f,3500,1000);energy(f,52500);','water(f,0,6000);energy(f,35000);f.getInventory().setStackInSlot(0,tank(ModItems.LARGE_OXYGEN_TANK.get(),0));')
s=s.replace('l.setBlockAndUpdate(DISPLAY.west(),FluidPipeRegistry.BASIC_FLUID_PIPE.get().defaultBlockState());','l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());')
a=s.index('        var animated=');b=s.index('        add(20,()->shot',a)
s=s[:a]+'''        var animated=new OxygenFillerBlockEntity(BlockPos.ZERO,ModBlocks.OXYGEN_FILLER.get().defaultBlockState().setValue(OxygenFillerBlock.LIT,true));OxygenFillerBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);float angle=animated.animationPhase(1);check(angle>0,"Compressor moves when active");animated.setBlockState(animated.getBlockState().setValue(OxygenFillerBlock.LIT,false));OxygenFillerBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);check(animated.animationPhase(1)==angle,"Compressor freezes while paused");
        check(ItemPipeBlock.refreshConnections(mc.level,DISPLAY.west(),mc.level.getBlockState(DISPLAY.west())).getValue(ItemPipeBlock.EAST),"Client blue item connection");
        check(OxygenPipeBlock.refreshConnections(mc.level,DISPLAY.east(),mc.level.getBlockState(DISPLAY.east())).getValue(OxygenPipeBlock.WEST),"Client orange oxygen connection");
'''+s[b:]
s=s.replace('energyCapacity()==52500','energyCapacity()==35000').replace('energyStored()>30000','energyStored()>32000').replace('getSlot(37).hasItem()','getSlot(39).hasItem()').replace('Networked purifier GUI','Networked filler GUI')
s=s.replace('count()==1,"One electrolysis recipe"','count()==3,"Exactly three tank sizes, no duplicates"')
s=s.replace('        add(30,()->shot("08_jei"));', '        add(1,()->mc.getSingleplayerServer().execute(()->verifyNegativeCacheRoom(mc.getSingleplayerServer().overworld())));\n        add(30,()->shot("08_jei"));')
a=s.index('        add(10,()->{mc.setScreen(null);log(')
s=s[:a]+'''        add(10,()->{mc.setScreen(null);mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();room(l,DISPLAY,true);var f=(OxygenFillerBlockEntity)l.getBlockEntity(DISPLAY);water(f,0,6000);energy(f,35000);NetworkHooks.openScreen(p,f,DISPLAY);});});
        add(15,()->{screen().mouseClicked((mc.getWindow().getGuiScaledWidth()-220)/2+191,(mc.getWindow().getGuiScaledHeight()-266)/2+43,0);});
        add(15,()->{check(screen().getMenu().getOperatingMode()==OxygenFillerMode.VENTILATION,"Air pictogram switches mode over network");check(!screen().getMenu().getSlot(36).isActive(),"Vent hides item slots");shot("10_ventilation_gui");mc.player.closeContainer();mc.setScreen(null);mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.teleportTo(p.serverLevel(),1.4,101.1,-.8,35,47);});});
        add(10,()->shot("11_ventilation_vapour"));
        add(10,()->shot("12_vapour_motion"));
'''+s[a:]
# Deliberately do not wait for asynchronous lighting: airtightness is geometry.
s=s.replace('"Exactly one electrolyzer JEI category"','"Exactly one filler JEI category"')
(D/'runtime_probe/OxygenFillerProbe.java').write_text(s,encoding='utf-8')
# A production artifact must contain the new shared gauge and particle implementation too.
p=D/'verify_release.py';v=p.read_text().replace("'client/render/OxygenFillerRenderer'","'client/gui/OxygenGasGauge','client/particle/VentilationBubbleParticle','client/render/OxygenFillerRenderer'");v=v.replace("files=list(","files=[assets/'particles/ventilation_bubble.json',resources/'data/minecraft/tags/blocks/needs_stone_tool.json']\nfiles+=list(");v=v.replace("the electrolyzer's tested", "the oxygen filler's tested");p.write_text(v)
print('Filler runtime and release checks prepared')
