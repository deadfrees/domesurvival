from pathlib import Path
R=Path(__file__).resolve().parents[2];O=Path(__file__).resolve().parent
s=(R/'dev/oxygen_filler_v2/runtime_probe/OxygenFillerProbe.java').read_text(encoding='utf-8')
for a,b in [('fillerprobe','filterprobe'),('OxygenFiller','FilterRegeneration'),('oxygenFiller','filterRegeneration'),('oxygen_filler_v2','filter_regeneration_v2'),('oxygen-filler-review','filter-regeneration-review'),('FILLER_REVIEW','FILTER_REVIEW'),('ModBlocks.OXYGEN_FILLER','FilterRegenerationRegistry.FILTER_REGENERATION_STATION'),('FilterRegenerationBlock.LIT','FilterRegenerationBlock.ACTIVE'),('import com.wasted.domesurvival.forge.machine.oxygen.*;','import com.wasted.domesurvival.forge.machine.filter.*;'),('import com.wasted.domesurvival.forge.client.screen.FilterRegenerationScreen;','import com.wasted.domesurvival.forge.machine.filter.FilterRegenerationScreen;')]:s=s.replace(a,b)
a=s.index('    static void water(');b=s.index('    @SubscribeEvent public static void server(',a)
s=s[:a]+r'''
    static ItemStack filter(){var s=new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get());s.setDamageValue(s.getMaxDamage()/2);s.setHoverName(net.minecraft.network.chat.Component.literal("Kept filter"));return s;}
    static ItemStack media(int n){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","filter_regeneration_media")),n);}
    static void prepare(FilterRegenerationBlockEntity f){f.getInventory().setStackInSlot(0,filter());f.getInventory().setStackInSlot(1,media(8));energy(f,20000);}
    static void process(ServerLevel l,ServerPlayer p){
        check(!FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().defaultBlockState().canOcclude(),"Recessed shell keeps neighboring faces");
        for(var item:ForgeRegistries.ITEMS.getValues()){
            var input=new ItemStack(item);if(!FilterRegenerationBlockEntity.isEligibleFilter(input))continue;
            input.setDamageValue(Math.max(1,input.getMaxDamage()/4));input.setHoverName(net.minecraft.network.chat.Component.literal("Kept name"));
            for(int variant=0;variant<3;variant++){
                var f=place(l,TEST);prepare(f);f.getInventory().setStackInSlot(0,input.copy());
                if(variant>0)f.getModules().setStackInSlot(0,new ItemStack((variant==1?MachineModuleItems.EFFICIENCY:MachineModuleItems.OVERDRIVE).get()));
                int duration=f.processingTicks(),cost=f.processingEnergy();tick(l,f,duration-1);
                check(f.getInventory().getStackInSlot(2).isEmpty(),"No early repair "+item+"/"+variant);tick(l,f,1);
                var out=f.getInventory().getStackInSlot(2);
                check(out.is(item)&&out.getDamageValue()==0&&out.hasCustomHoverName()&&FilterRegenerationBlockEntity.getRegenerationCycles(out)==1,"Repair preserves filter NBT "+item+"/"+variant);
                check(f.getDataAccess().get(0)==20000-cost&&f.getInventory().getStackInSlot(1).getCount()==7,"Exact cycle FE and media "+item+"/"+variant);
            }
        }
        var f=place(l,TEST);prepare(f);tick(l,f,50);var menu=new FilterRegenerationMenu(1,p.getInventory(),f);
        check(!menu.getSlot(36).mayPickup(p)&&menu.quickMoveStack(p,36).isEmpty(),"Paid cycle locks filter removal");
        f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(f.processingTicks()==200&&f.processingEnergy()==8000,"Hot module keeps current cycle snapshot");
        var saved=f.saveWithoutMetadata();f.load(saved);tick(l,f,150);
        check(f.getDataAccess().get(0)==12000&&f.getInventory().getStackInSlot(1).getCount()==7&&f.processingEnergy()==6400,"Saved cycle finishes at old cost; next uses module");
        tick(l,f,f.processingTicks());check(f.getInventory().getStackInSlot(2).getDamageValue()==0&&f.getDataAccess().get(0)==5600,"Automatic second cycle at new cost");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get())),"Duplicate and conflicting modules rejected");
        f=place(l,TEST);prepare(f);tick(l,f,30);energy(f,0);tick(l,f,20);check(f.getDataAccess().get(2)==30&&!f.getBlockState().getValue(FilterRegenerationBlock.ACTIVE),"No energy pauses without losing paid work");
        energy(f,20000);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);tick(l,f,20);check(f.getDataAccess().get(2)==30&&f.getDataAccess().get(0)==20000,"Missing media pauses without spending");
        f.getInventory().setStackInSlot(1,media(8));tick(l,f,1);check(f.getDataAccess().get(2)==31,"Restoring media resumes");
        saved=f.saveWithoutMetadata();saved.remove("Modules");saved.remove("CycleTicks");saved.remove("CycleEnergy");saved.remove("CycleFilter");saved.remove("PortFacing");saved.getCompound("Inventory").putInt("Size",2);f.load(saved);
        check(f.getInventory().getSlots()==3&&f.getDataAccess().get(2)==31&&f.processingTicks()==200&&f.processingEnergy()==8000,"Legacy two-slot paid cycle migrates");
        tick(l,f,169);check(f.getInventory().getStackInSlot(1).getCount()==7&&FilterRegenerationBlockEntity.getRegenerationCycles(f.getInventory().getStackInSlot(0))==1,"Legacy cycle completes once");
        f=place(l,TEST);prepare(f);var limited=filter();limited.getOrCreateTag().putInt("DomeRegenCycles",7);f.getInventory().setStackInSlot(0,limited);tick(l,f,200);
        check(FilterRegenerationBlockEntity.getRegenerationCycles(f.getInventory().getStackInSlot(2))==8&&f.getInventory().getStackInSlot(2).getDamageValue()>0,"Eighth repair outputs still-damaged filter");
        var exhausted=f.getInventory().extractItem(2,1,false);f.getInventory().setStackInSlot(0,exhausted);tick(l,f,300);check(f.getDataAccess().get(0)==12000&&f.getInventory().getStackInSlot(1).getCount()==7,"Exhausted filter consumes no resources");
        f=place(l,TEST);prepare(f);var shallow=filter();shallow.setDamageValue(1);f.getInventory().setStackInSlot(0,shallow);f.getInventory().setStackInSlot(2,filter());tick(l,f,220);
        check(f.status()==FilterRegenerationBlockEntity.STATUS_OUTPUT_FULL&&f.getDataAccess().get(0)==12000&&f.getInventory().getStackInSlot(0).getDamageValue()==0,"Blocked output retains repair without repeat charge");
        f.getInventory().extractItem(2,1,false);tick(l,f,1);check(f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(2).getDamageValue()==0,"Cleared output receives repaired filter");
        f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);menu=new FilterRegenerationMenu(2,p.getInventory(),f);menu.setTab(200);
        check(menu.energyCapacity()==35000&&!menu.getSlot(39).mayPickup(p)&&menu.quickMoveStack(p,39).isEmpty(),"High-charge buffer removal blocked");energy(f,20000);check(menu.getSlot(39).mayPickup(p),"Safe buffer removal allowed");
        menu.setTab(202);check(!menu.getSlot(36).isActive()&&!menu.getSlot(37).isActive()&&!menu.getSlot(38).isActive()&&!menu.getSlot(39).isActive(),"Configuration hides all process and module slots");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(FilterRegenerationBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);energy(f,1000);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);f.getInventory().setStackInSlot(2,filter());
                var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((items!=null)==(input||output)&&(fe!=null)==input,"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(FilterRegenerationBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual agrees "+facing+"/"+side+"/"+m);
                if(input){
                    check(items.insertItem(0,filter(),true).isEmpty()&&f.getInventory().getStackInSlot(0).isEmpty(),"Filter simulation");
                    check(items.insertItem(0,filter(),false).isEmpty()&&items.insertItem(1,media(1),false).isEmpty()&&items.extractItem(0,1,false).isEmpty()&&items.extractItem(2,1,false).isEmpty(),"Blue takes filter and media, never extracts");
                    check(!items.isItemValid(0,new ItemStack(Items.COAL))&&!items.isItemValid(1,filter())&&!items.isItemValid(2,filter()),"Input whitelist");
                    check(fe.receiveEnergy(1000,true)==64&&f.getDataAccess().get(0)==1000&&fe.extractEnergy(100,false)==0,"FE simulation and limit");
                    items.getStackInSlot(0).setCount(0);check(!f.getInventory().getStackInSlot(0).isEmpty(),"Item view cannot mutate inventory");
                    mode(f,side,SideMode.DISABLED);check(fe.receiveEnergy(64,false)==0&&!items.isItemValid(0,filter()),"Cached input respects OFF");
                }else if(output){
                    check(items.extractItem(0,1,false).isEmpty()&&!items.insertItem(0,filter(),false).isEmpty(),"Orange denies insertion and unfinished filter");
                    check(!items.extractItem(2,1,true).isEmpty()&&!f.getInventory().getStackInSlot(2).isEmpty(),"Output simulation");
                    check(!items.extractItem(2,1,false).isEmpty()&&f.getInventory().getStackInSlot(2).isEmpty(),"Orange extracts only finished filter");
                    f.getInventory().setStackInSlot(2,filter());mode(f,side,SideMode.DISABLED);check(items.extractItem(2,1,false).isEmpty(),"Cached output respects OFF");
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided automation denied");
            var n=f.saveWithoutMetadata();f.load(n);for(var side:RelativeSide.values())check(f.sideMode(side.resolve(facing))==SideMode.DISABLED,"Side settings survive save "+side);
        }
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));f.getInventory().setStackInSlot(1,media(3));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().asItem())==(tool==Items.IRON_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,MachineModuleItems.BUFFER.get())==1&&count(l,media(1).getItem())==3,"Contents and modules drop once");
        }
        var f=place(l,TEST);clear(l);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().asItem())==0,"Wrench drops one machine in world");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Modules","Energy","Progress","CycleTicks","CycleEnergy","CycleFilter","Inventory","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(FilterRegenerationBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.getDataAccess().get(2)==40&&f.getDataAccess().get(0)==expected.getInt("Energy"),"Placement restores paid progress and FE");
        check(f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Placed sides follow new facing");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
''' +s[b:]
s=s.replace('process(l,p);ports(l);ventilation(l);dismantle(l,p);prepareNegativeCacheRoom(l);','process(l,p);ports(l);dismantle(l,p);')
a=s.index('            var f=place(l,DISPLAY);');b=s.index('            p.teleportTo(l,1.6',a)
s=s[:a]+'''            var f=place(l,DISPLAY);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode(f,RelativeSide.TOP,SideMode.DISABLED);
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
'''+s[b:]
s=s.replace('        check(OxygenPipeBlock.refreshConnections(mc.level,DISPLAY.east(),mc.level.getBlockState(DISPLAY.east())).getValue(OxygenPipeBlock.WEST),"Client orange oxygen connection");','')
s=s.replace('getSlot(39).hasItem()','getSlot(40).hasItem()').replace('DomeSurvivalJeiPlugin.OXYGEN_FILLER','DomeSurvivalJeiPlugin.FILTER_REGENERATION')
s=s.replace('==3,"Exactly three tank sizes, no duplicates"','==ForgeRegistries.ITEMS.getValues().stream().filter(i->FilterRegenerationBlockEntity.isEligibleFilter(new ItemStack(i))).count(),"One recipe per eligible filter, no duplicates"')
s=s.replace('        add(1,()->mc.getSingleplayerServer().execute(()->verifyNegativeCacheRoom(mc.getSingleplayerServer().overworld())));','')
s=s.replace('        add(20,()->{shot("03_main_scale3");', '''        add(1,()->mc.getSingleplayerServer().execute(()->{var f=(FilterRegenerationBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);f.getInventory().setStackInSlot(1,media(8));}));
        add(25,()->{check(screen().getMenu().status()==1,"Networked regeneration status is active");check(mc.level.getBlockState(DISPLAY).getValue(FilterRegenerationBlock.ACTIVE),"Client working state synchronized");shot("02_working_gui");});
        add(40,()->shot("02_carriage_motion"));
        add(20,()->{shot("03_main_scale3");''')
a=s.index('        add(10,()->{mc.setScreen(null);mc.getSingleplayerServer().execute(()->{var p=');b=s.index('        add(10,()->{mc.setScreen(null);log("RESULT ',a)
s=s[:a]+s[b:]
s=s.replace('Compressor','Cleaning carriage').replace('filler GUI','regenerator GUI').replace('filler JEI','regenerator JEI').replace('filler_review_','filter_review_')
s=s.replace('static void plan(){var mc=Minecraft.getInstance();', '''static void plan(){var mc=Minecraft.getInstance();
        check(mc.font.width(net.minecraft.network.chat.Component.translatable("gui.domesurvival.filter_regeneration_v2.media"))<=54,"Full media caption fits");
        check(mc.font.width(net.minecraft.network.chat.Component.translatable("gui.domesurvival.filter_regeneration_v2.input_short"))<=90,"Full input caption fits");''')
(O/'runtime_probe/FilterRegenerationProbe.java').write_text(s,encoding='utf-8')
