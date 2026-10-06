from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent/'runtime_probe';out.mkdir(exist_ok=True)
s=(root/'dev/organic_processor_v2/runtime_probe/OrganicProcessorProbe.java').read_text()
s=s.replace('organicprobe','incubatorprobe').replace('OrganicProcessor','Bioincubator').replace('machine.organic','machine.bio').replace('organic_processor_v2','bioincubator_v2').replace('organic-processor-review','bioincubator-review').replace('organicProcessor','bioincubator').replace('ORGANIC_REVIEW','INCUBATOR_REVIEW').replace('organic_review','incubator_review')
s=s.replace('BioincubatorRegistry.ORGANIC_PROCESSOR','ModBlocks.BIOINCUBATOR').replace('BioincubatorBlock.ACTIVE','BioincubatorBlock.LIT')
s=s.replace('import com.wasted.domesurvival.forge.client.jei.ProcessingMachinesJeiPlugin;','import com.wasted.domesurvival.forge.client.jei.DomeSurvivalJeiPlugin;\nimport com.wasted.domesurvival.forge.bio.*;\nimport com.wasted.domesurvival.forge.item.BioModuleItem;\nimport com.wasted.domesurvival.forge.quest.QuestProgressService;')
a=s.index('    static ItemStack filter()');b=s.index('    static void ports(',a)
s=s[:a]+'''    static final ResourceLocation CHICKEN=new ResourceLocation("minecraft","chicken");
    static ItemStack filter(){return BioModuleItem.create(CHICKEN,false);}
    static ItemStack media(int n){return new ItemStack(ForgeRegistries.ITEMS.getValue(BioLootData.species(CHICKEN).feedItem()),n);}
    static void prepare(BioincubatorBlockEntity f){
        var species=BioLootData.species(CHICKEN);f.getInventory().setStackInSlot(0,filter());f.getInventory().setStackInSlot(1,new ItemStack(ForgeRegistries.ITEMS.getValue(species.feedItem()),species.feedCount()*3));energy(f,60000);
        mode(f,RelativeSide.TOP,SideMode.INPUT);f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException()).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),6000),IFluidHandler.FluidAction.EXECUTE);
    }
    static void repair(BioincubatorBlockEntity f){prepare(f);if(f.getDataAccess().get(8)!=1)f.toggleMode();f.getInventory().setStackInSlot(0,BioModuleItem.create(CHICKEN,true));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.BIO_REPAIR_KIT.get()));f.getInventory().setStackInSlot(2,new ItemStack(ModItems.BIOGEL.get()));f.getInventory().setStackInSlot(3,new ItemStack(ModItems.NUTRIENT_MIX.get()));}
    static List<net.minecraft.world.entity.AgeableMob> babies(ServerLevel l){return l.getEntitiesOfClass(net.minecraft.world.entity.AgeableMob.class,new AABB(TEST).inflate(4));}
    static long run(ServerLevel l,BioincubatorBlockEntity f,int ticks){
        long used=0;for(int i=0;i<ticks;i++){
            if(f.getDataAccess().get(0)<1000){var cap=f.getCapability(ForgeCapabilities.ENERGY,Direction.UP).orElseThrow(()->new IllegalStateException());while(cap.receiveEnergy(128,false)>0){}}
            int before=f.getDataAccess().get(0);tick(l,f,1);used+=before-f.getDataAccess().get(0);
        }return used;
    }
    static void process(ServerLevel l,ServerPlayer p){
        QuestProgressService.set(l,BioModuleData.IDENTIFICATION_FLAG,"incubator_probe");
        check(!ModBlocks.BIOINCUBATOR.get().defaultBlockState().canOcclude(),"Recessed chamber keeps adjacent faces");
        for(var species:BioLootData.allSpecies())for(int task=0;task<2;task++){
            var f=place(l,TEST);prepare(f);babies(l).forEach(net.minecraft.world.entity.Entity::discard);
            if(task==1)repair(f);
            f.getInventory().setStackInSlot(0,BioModuleItem.create(species.entityId(),task==1));
            if(task==0)f.getInventory().setStackInSlot(1,new ItemStack(ForgeRegistries.ITEMS.getValue(species.feedItem()),species.feedCount()));
            int ticks=f.getDataAccess().get(5),cost=f.getDataAccess().get(15),water=task==1?1000:species.waterMb();
            long used=run(l,f,ticks-1);check(f.getInventory().getStackInSlot(4).isEmpty()&&babies(l).isEmpty(),"No early result "+species.entityId()+"/"+task);used+=run(l,f,1);
            // Vanilla Frog.setBaby is a no-op: its juvenile form is a separate tadpole entity.
            // Preserve the machine's existing species behavior; all other supported species receive juvenile age.
            if(task==0)check(babies(l).size()==1&&(babies(l).get(0) instanceof net.minecraft.world.entity.animal.frog.Frog?babies(l).get(0).getAge()==0:babies(l).get(0).getAge()<0)&&ForgeRegistries.ENTITY_TYPES.getKey(babies(l).get(0).getType()).equals(species.entityId()),"Correct species and vanilla juvenile behavior "+species.entityId());
            else{var sample=BioModuleData.sample(f.getInventory().getStackInSlot(4));check(sample!=null&&!sample.damaged()&&sample.entityId().equals(species.entityId()),"Repair preserves species "+species.entityId());}
            check(used==cost&&f.getDataAccess().get(2)==6000-water&&f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).isEmpty(),"Exact resources "+species.entityId()+"/"+task);
        }
        for(var module:List.of(MachineModuleItems.EFFICIENCY,MachineModuleItems.OVERDRIVE))for(int task=0;task<2;task++){
            var f=place(l,TEST);if(task==1)repair(f);else prepare(f);babies(l).forEach(net.minecraft.world.entity.Entity::discard);
            f.getModules().setStackInSlot(0,new ItemStack(module.get()));int ticks=f.getDataAccess().get(5),cost=f.getDataAccess().get(15);
            check(run(l,f,ticks)==cost&&f.getDataAccess().get(4)==0&&(task==1?!f.getInventory().getStackInSlot(4).isEmpty():babies(l).size()==1),"Module cost and duration "+module.getId()+"/"+task);
        }
        var f=place(l,TEST);repair(f);tick(l,f,40);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(f.getDataAccess().get(4)==40&&f.getDataAccess().get(5)==1800&&f.getDataAccess().get(15)==144000,"Hot module preserves paid cycle");var saved=f.saveWithoutMetadata();f.load(saved);
        check(run(l,f,1760)==140800&&!f.getInventory().getStackInSlot(4).isEmpty(),"Saved cycle completes at original cost");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get())),"Conflicting and duplicate modules rejected");
        f=place(l,TEST);prepare(f);tick(l,f,30);energy(f,0);tick(l,f,20);check(f.getDataAccess().get(4)==30&&!f.getBlockState().getValue(BioincubatorBlock.LIT),"No FE freezes active cycle");energy(f,60000);
        l.setBlockAndUpdate(TEST.north(),Blocks.STONE.defaultBlockState());tick(l,f,20);check(f.getDataAccess().get(4)==30&&f.getDataAccess().get(0)==60000,"Blocked birth exit pauses without consuming");l.setBlockAndUpdate(TEST.north(),Blocks.AIR.defaultBlockState());
        var feed=f.getInventory().getStackInSlot(1).copy();f.getInventory().setStackInSlot(1,ItemStack.EMPTY);tick(l,f,20);check(f.getDataAccess().get(4)==30&&f.getDataAccess().get(0)==60000,"Missing feed pauses without consuming");f.getInventory().setStackInSlot(1,feed);
        saved=f.saveWithoutMetadata();saved.put("PurifiedWater",new FluidStack(ModFluids.PURIFIED_WATER.get(),1).writeToNBT(new net.minecraft.nbt.CompoundTag()));f.load(saved);tick(l,f,20);check(f.getDataAccess().get(4)==30&&f.getDataAccess().get(0)==60000,"Insufficient water pauses without consuming");
        f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException()).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),6000),IFluidHandler.FluidAction.EXECUTE);tick(l,f,1);check(f.getDataAccess().get(4)==31,"Restored resources resume cycle");
        saved=f.saveWithoutMetadata();for(String key:List.of("CycleTicks","CycleEnergy","CycleCapsule","CycleMode","Modules","PortFacing"))saved.remove(key);f.load(saved);tick(l,f,1);check(f.getDataAccess().get(4)==32&&f.getDataAccess().get(15)>0,"Legacy NBT restores running cycle");
        for(var side:RelativeSide.values())mode(f,side,SideMode.DISABLED);saved=f.saveWithoutMetadata();f.load(saved);for(var side:RelativeSide.values())check(f.sideMode(side.resolve(f.getMachineFacing()))==SideMode.DISABLED,"Saved OFF remains OFF "+side);
        f=place(l,TEST);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,105000);var menu=new BioincubatorMenu(1,p.getInventory(),f);menu.setTab(200);
        check(menu.getEnergyCapacity()==105000&&!menu.getSlot(7).mayPickup(p)&&menu.quickMoveStack(p,7).isEmpty(),"Full buffer cannot be removed");energy(f,60000);check(menu.getSlot(7).mayPickup(p),"Safe buffer removal permitted");menu.setTab(202);for(int i=0;i<9;i++)check(!menu.getSlot(i).isActive(),"Internal tabs hide machine slot "+i);
    }
'''+s[b:]
# Logical slots: four inputs and one output; preserve independent side/facing coverage.
a=s.index('    static void ports(');b=s.index('    static void dismantle(',a);part=s[a:b]
part=part.replace('setStackInSlot(2,filter())','setStackInSlot(4,filter())').replace('extractItem(2,','extractItem(4,').replace('getStackInSlot(2)','getStackInSlot(4)').replace('!items.isItemValid(2,filter())','!items.isItemValid(4,filter())').replace('==256','==128').replace('receiveEnergy(256','receiveEnergy(128').replace('4000-before','6000-before')
s=s[:a]+part+s[b:]
s=s.replace('energy(f,87500)','energy(f,105000)').replace('f.progressTicks()==40','f.getDataAccess().get(4)==40').replace('"PurifiedWater","Inventory"','"PurifiedWater","CycleCapsule","CycleMode","Inventory"')
s=s.replace('f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode','f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode')
a=s.index('    static void plan()');b=s.index('    @SubscribeEvent public static void mouse(',a)
s=s[:a]+'''    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        var animated=new BioincubatorBlockEntity(BlockPos.ZERO,ModBlocks.BIOINCUBATOR.get().defaultBlockState().setValue(BioincubatorBlock.LIT,true));BioincubatorBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);float phase=animated.animationPhase(1);check(phase>0,"Scanner moves when active");animated.setBlockState(animated.getBlockState().setValue(BioincubatorBlock.LIT,false));BioincubatorBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);check(animated.animationPhase(1)==phase,"Scanner stops while paused");
        add(40,()->shot("01_model"));
        add(1,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(BioincubatorBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(90,()->{check(mc.screen instanceof BioincubatorScreen,"Networked GUI opens");check(screen().getMenu().getEnergyCapacity()==105000&&screen().getMenu().getEnergy()>100000,"32-bit FE buffer synchronized");shot("02_incubation");mc.getSingleplayerServer().execute(()->{var f=(BioincubatorBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);f.getInventory().setStackInSlot(1,media(16));});});
        add(90,()->{check(screen().getMenu().getStatus()==1,"Incubation starts");shot("03_working");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(60,()->{shot("04_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();tab(202);});
        add(60,()->{shot("05_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(60,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Side button synchronizes");tab(200);});
        add(60,()->{mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,9,0,ClickType.QUICK_MOVE,mc.player);});
        add(60,()->{check(screen().getMenu().getSlot(8).hasItem(),"Shift-click installs compatible module");shot("06_modules");tab(201);mc.getSingleplayerServer().execute(()->{var f=(BioincubatorBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);repair(f);});});
        add(90,()->{check(screen().getMenu().getMode()==1&&screen().getMenu().getStatus()==1,"Repair mode and slots synchronized");shot("07_repair");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(60,()->{shot("08_repair_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.closeContainer();var runtime=JeiCapture.runtime;check(runtime!=null,"JEI available");
            var manager=runtime.getRecipeManager();int species=BioLootData.allSpecies().size();check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.BIO_REPAIR).get().count()==species,"One repair JEI recipe per species");check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.BIO_INCUBATION).get().count()==species,"One incubation JEI recipe per species");runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.BIO_REPAIR));});
        add(90,()->shot("09_jei_repair"));add(100,()->{shot("10_jei_repair_motion");JeiCapture.runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.BIO_INCUBATION));});add(90,()->shot("11_jei_incubation"));
        add(10,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
'''+s[b:]
# These validation labels originated from the previous machine; keep logs unambiguous.
s=s.replace('Filter simulation','Capsule simulation').replace('filter and media','capsule and feed').replace('finished filter','repaired capsule').replace('unfinished filter','unfinished capsule')
(out/'BioincubatorProbe.java').write_text(s,encoding='utf-8')
(out/'JeiCapture.java').write_text((root/'dev/organic_processor_v2/runtime_probe/JeiCapture.java').read_text().replace('organicprobe','incubatorprobe').replace('organic_review','incubator_review'))
run=root/'run/bioincubator-review';(run/'config/fancymenu').mkdir(parents=True,exist_ok=True);shutil.copy2(root/'run/machine-gauge-review/config/fancymenu/options.txt',run/'config/fancymenu/options.txt')
