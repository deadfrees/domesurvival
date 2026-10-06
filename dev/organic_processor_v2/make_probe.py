from pathlib import Path
import json
root=Path(__file__).resolve().parents[2]
out=root/'dev/organic_processor_v2/runtime_probe';out.mkdir(exist_ok=True)
old=(root/'dev/filter_regeneration_v2/runtime_probe/FilterRegenerationProbe.java').read_text()
s=old.replace('filterprobe','organicprobe').replace('FilterRegeneration','OrganicProcessor').replace('machine.filter','machine.organic').replace('filter_regeneration_v2','organic_processor_v2').replace('filter-regeneration-review','organic-processor-review').replace('filterRegeneration','organicProcessor').replace('FILTER_REGENERATION_STATION','ORGANIC_PROCESSOR').replace('FILTER_REGENERATION','ORGANIC_PROCESSOR').replace('FILTER_REVIEW','ORGANIC_REVIEW').replace('filter_review','organic_review').replace('DomeSurvivalJeiPlugin','ProcessingMachinesJeiPlugin')
a=s.index('    static ItemStack filter()');b=s.index('    static void ports(',a)
s=s[:a]+'''    static ItemStack filter(){return new ItemStack(Items.WHEAT,1);}
    static ItemStack media(int n){return new ItemStack(Items.BONE_MEAL,n);}
    static void prepare(OrganicProcessorBlockEntity f){f.getInventory().setStackInSlot(0,new ItemStack(Items.WHEAT,32));f.getInventory().setStackInSlot(1,media(16));energy(f,50000);f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException("water port")).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),4000),IFluidHandler.FluidAction.EXECUTE);}
    static void process(ServerLevel l,ServerPlayer p){
        check(!OrganicProcessorRegistry.ORGANIC_PROCESSOR.get().defaultBlockState().canOcclude(),"Recessed shell does not hide adjacent faces");
        var recipes=l.getRecipeManager().getAllRecipesFor(com.wasted.domesurvival.forge.recipe.ModRecipes.ORGANIC_PROCESSOR_TYPE.get());check(recipes.size()==3,"Three unchanged biosynthesis recipes");
        for(var r:recipes)for(int variant=0;variant<3;variant++){
            var f=place(l,TEST);prepare(f);
            var first=r.getPrimary().getItems()[0].copy();first.setCount(r.getPrimaryCount());var second=r.getAdditive().getItems()[0].copy();second.setCount(r.getAdditiveCount());f.getInventory().setStackInSlot(0,first);f.getInventory().setStackInSlot(1,second);
            if(variant>0)f.getModules().setStackInSlot(0,new ItemStack((variant==1?MachineModuleItems.EFFICIENCY:MachineModuleItems.OVERDRIVE).get()));
            int duration=f.requiredTicks(),cost=f.getDataAccess().get(7);tick(l,f,duration-1);check(f.getInventory().getStackInSlot(2).isEmpty(),"No early output "+r.getId()+"/"+variant);tick(l,f,1);
            check(ItemStack.matches(f.getInventory().getStackInSlot(2),r.getResult()),"Correct product "+r.getId()+"/"+variant);
            check(f.getDataAccess().get(0)==50000-cost&&f.getDataAccess().get(2)==4000-r.getWaterMb()&&f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).isEmpty(),"Exact resources "+r.getId()+"/"+variant);
        }
        var f=place(l,TEST);prepare(f);tick(l,f,40);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(f.progressTicks()==40&&f.requiredTicks()==140&&f.getDataAccess().get(7)==3600,"Module change preserves paid cycle");var saved=f.saveWithoutMetadata();f.load(saved);tick(l,f,100);
        check(f.getDataAccess().get(0)==46400&&f.getDataAccess().get(2)==3750&&f.requiredTicks()==156&&f.getDataAccess().get(7)==2880,"Saved cycle finishes at old cost; next uses efficiency");tick(l,f,156);check(f.getDataAccess().get(0)==43520&&f.getInventory().getStackInSlot(2).getCount()==2,"Next cycle uses module cost exactly");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get())),"Duplicate and conflicting modules denied");
        f=place(l,TEST);prepare(f);tick(l,f,30);energy(f,0);tick(l,f,20);check(f.progressTicks()==30&&!f.getBlockState().getValue(OrganicProcessorBlock.ACTIVE),"No energy pauses rotor and paid cycle");
        energy(f,50000);saved=f.saveWithoutMetadata();saved.put("PurifiedWater",new FluidStack(ModFluids.PURIFIED_WATER.get(),100).writeToNBT(new net.minecraft.nbt.CompoundTag()));f.load(saved);tick(l,f,20);check(f.progressTicks()==30&&f.getDataAccess().get(0)==50000,"Insufficient water pauses without consumption");
        f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException()).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),4000),IFluidHandler.FluidAction.EXECUTE);
        f.getInventory().setStackInSlot(2,new ItemStack(Items.STONE,64));tick(l,f,20);check(f.progressTicks()==30&&f.getDataAccess().get(0)==50000,"Blocked output pauses without consumption");f.getInventory().setStackInSlot(2,ItemStack.EMPTY);tick(l,f,1);check(f.progressTicks()==31,"Unblocked cycle resumes");
        saved=f.saveWithoutMetadata();saved.remove("CycleTicks");saved.remove("CycleEnergy");saved.remove("PortFacing");f.load(saved);tick(l,f,109);check(f.progressTicks()==0&&f.getInventory().getStackInSlot(2).getCount()==1,"Legacy NBT cycle completes once");
        f=place(l,TEST);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,87500);var menu=new OrganicProcessorMenu(1,p.getInventory(),f);menu.setTab(200);
        check(menu.energyCapacity()==87500&&!menu.getSlot(3).mayPickup(p)&&menu.quickMoveStack(p,3).isEmpty(),"Full buffer removal blocked");energy(f,50000);check(menu.getSlot(3).mayPickup(p),"Safe buffer removal permitted");menu.setTab(202);for(int i=0;i<5;i++)check(!menu.getSlot(i).isActive(),"Hidden machine slot "+i);
        f=place(l,TEST);prepare(f);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);tick(l,f,100);check(f.getDataAccess().get(0)==50000&&f.getDataAccess().get(2)==4000,"Missing ingredient consumes nothing");
    }
'''+s[b:]
# Keep the existing exhaustive 4-facing x 6-side x 3-mode checks, adapting energy and fluid rules.
s=s.replace('fe.receiveEnergy(1000,true)==64','fe.receiveEnergy(1000,true)==256').replace('fe.receiveEnergy(64,false)','fe.receiveEnergy(256,false)')
s=s.replace('var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);','var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);var fluid=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElse(null);')
s=s.replace('(fe!=null)==input,','(fe!=null)==input&&(fluid!=null)==input,')
s=s.replace('check(items.insertItem(0,filter(),true)', '''var water=new FluidStack(ModFluids.PURIFIED_WATER.get(),250);
                    int before=f.getDataAccess().get(2);check(fluid.fill(water,IFluidHandler.FluidAction.SIMULATE)==Math.min(250,4000-before)&&f.getDataAccess().get(2)==before,"Water simulation");
                    check(fluid.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0&&fluid.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Purified-only input, no draining");fluid.fill(water,IFluidHandler.FluidAction.EXECUTE);
                    check(items.insertItem(0,filter(),true)''')
s=s.replace('mode(f,side,SideMode.DISABLED);check(fe.receiveEnergy','mode(f,side,SideMode.DISABLED);check(fluid.fill(water,IFluidHandler.FluidAction.EXECUTE)==0,"Cached fluid respects OFF");check(fe.receiveEnergy')
s=s.replace('new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}','new Item[]{Items.WOODEN_PICKAXE,Items.GOLDEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE}')
s=s.replace('tool==Items.IRON_PICKAXE?1:0','tool!=Items.WOODEN_PICKAXE&&tool!=Items.GOLDEN_PICKAXE?1:0')
s=s.replace('"CycleFilter",','"PurifiedWater",').replace('f.getDataAccess().get(2)==40&&','f.progressTicks()==40&&')
s=s.replace('energy(f,35000)','energy(f,87500)').replace('f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode','f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode')
s=s.replace('check(mc.font.width(net.minecraft.network.chat.Component.translatable("gui.domesurvival.organic_processor_v2.media"))<=54,"Full media caption fits");','')
s=s.replace('energyCapacity()==35000','energyCapacity()==87500').replace('energyStored()>32000','energyStored()>85000')
s=s.replace('getSlot(40)','getSlot(4)').replace('containerId,0,0,ClickType.QUICK_MOVE','containerId,5,0,ClickType.QUICK_MOVE')
s=s.replace('ForgeRegistries.ITEMS.getValues().stream().filter(i->OrganicProcessorBlockEntity.isEligibleFilter(new ItemStack(i))).count()','3')
s=s.replace('"One recipe per eligible filter, no duplicates"','"Exactly three JEI recipes, no duplicates"')
s=s.replace('static int ticks,step,failures;','static int ticks,step,failures,frames;')
start=s.index('}else if(planned&&step<steps.size()');end=s.index('\n    }\n}',start)
s=s[:start]+'''}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(!ENABLED||!planned||e.phase!=TickEvent.Phase.END||step>=steps.size())return;
        if(++frames>=steps.get(step).delay()){
            frames=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");Minecraft.getInstance().stop();}
        }'''+s[end:]
# Render-frame timing: allow networking to catch up, without relying on a screenshot after client ticks.
s=s.replace('add(20,','add(60,').replace('add(25,','add(90,').replace('add(10,','add(45,').replace('add(30,','add(90,')
(out/'OrganicProcessorProbe.java').write_text(s,encoding='utf-8')
(out/'JeiCapture.java').write_text((root/'dev/filter_regeneration_v2/runtime_probe/JeiCapture.java').read_text().replace('filterprobe','organicprobe').replace('filter_review','organic_review'),encoding='utf-8')
for name in ('mineable/pickaxe','needs_stone_tool'):
 p=root/f'src/main/resources/data/minecraft/tags/blocks/{name}.json';d=json.loads(p.read_text());
 if 'domesurvival:organic_processor' not in d['values']:d['values'].append('domesurvival:organic_processor')
 p.write_text(json.dumps(d,indent=2)+'\n')
run=root/'run/organic-processor-review';run.mkdir(exist_ok=True)
options=root/'run/filter-regeneration-review/options.txt'
if options.exists():(run/'options.txt').write_bytes(options.read_bytes())
