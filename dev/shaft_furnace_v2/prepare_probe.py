"""Prepare the isolated integration probe from the previously validated furnace harness."""
from pathlib import Path
import re
R=Path(__file__).resolve().parents[2];out=Path(__file__).parent
dest=out/'runtime_probe';dest.mkdir(exist_ok=True)
def convert(s):
    return s.replace('CokeOven','ShaftFurnace').replace('COKE_OVEN','SHAFT_FURNACE').replace('COKE_REVIEW','SHAFT_REVIEW').replace('cokeOven','shaftFurnace').replace('cokeprobe','shaftprobe').replace('coke_oven_v2','shaft_furnace_v2').replace('coke-oven-review','shaft-furnace-review').replace('coke_review','shaft_review').replace('Coke review','Shaft review')
s=convert((R/'dev/coke_oven_v2/runtime_probe/CokeOvenProbe.java').read_text())
start=s.index('    static void process(');end=s.index('    static void ports(',start)
s=s[:start]+'''    static void process(ServerLevel l,ServerPlayer p){
        var f=place(l,TEST);var inv=f.getInventory();var coke=ModItems.COAL_COKE.get();
        inv.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,16));inv.setStackInSlot(1,new ItemStack(coke,3));
        check(f.fuelDuration(new ItemStack(coke))==3200,"Base coke burn is unchanged");
        check(f.fuelDuration(new ItemStack(Items.COAL))==0&&!ShaftFurnaceBlockEntity.isValidCoke(new ItemStack(Items.CHARCOAL)),"Only coke fuels the furnace");
        tick(l,f,2999);check(inv.getStackInSlot(2).isEmpty()&&inv.getStackInSlot(3).isEmpty()&&f.getDataAccess().get(0)==2999,"No outputs before 150 second cycle");
        tick(l,f,1);check(inv.getStackInSlot(2).is(ModItems.STEEL_INGOT.get())&&inv.getStackInSlot(3).is(ModItems.SLAG.get())&&inv.getStackInSlot(0).getCount()==15,"One iron produces exactly one steel and one slag");
        var menu=new ShaftFurnaceMenu(7,p.getInventory(),f);menu.setTab(200);p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        int burn=menu.burnRemaining();check(!menu.quickMoveStack(p,4).isEmpty()&&f.hasEfficiency(),"Install efficiency with Shift-click while burning");
        check(menu.burnRemaining()==burn&&menu.burnTotal()==3200,"Installing preserves current fuel snapshot");
        tick(l,f,burn);check(menu.burnTotal()==3680,"Next coke burns 15 percent longer");
        check(menu.progressMax()==3000,"Module does not accelerate smelting");
        check(!menu.quickMoveStack(p,40).isEmpty()&&!f.hasEfficiency()&&menu.burnTotal()==3680,"Hot removal preserves already burning fuel");
        check(!f.getModules().insertItem(0,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty()&&f.getModules().getStackInSlot(0).isEmpty(),"Empty module socket rejects overdrive");
        check(!f.getModules().insertItem(0,new ItemStack(MachineModuleItems.BUFFER.get()),false).isEmpty(),"Energy buffer module rejected");
        for(int output:new int[]{2,3}){
            inv.setStackInSlot(output,new ItemStack(output==2?ModItems.STEEL_INGOT.get():ModItems.SLAG.get(),64));
            int progress=menu.progress(),raw=inv.getStackInSlot(0).getCount();tick(l,f,10);
            check(menu.progress()==progress&&inv.getStackInSlot(0).getCount()==raw,"Full output pauses safely slot "+output);
            inv.setStackInSlot(output,ItemStack.EMPTY);tick(l,f,1);check(menu.progress()==progress+1,"Clearing output resumes slot "+output);
        }
        inv.setStackInSlot(3,new ItemStack(Items.COBBLESTONE));int progress=menu.progress();tick(l,f,10);check(menu.progress()==progress,"Foreign item in slag output blocks processing");inv.setStackInSlot(3,ItemStack.EMPTY);
        var saved=f.saveWithoutMetadata();saved.putInt("BurnTime",0);saved.putInt("BurnTimeMax",0);saved.putInt("Progress",13);f.load(saved);
        inv.setStackInSlot(1,ItemStack.EMPTY);tick(l,f,10);check(menu.progress()==13,"No coke pauses unfinished cycle");
        inv.setStackInSlot(1,new ItemStack(coke));tick(l,f,1);check(menu.progress()==14,"Adding coke resumes unfinished cycle");
        inv.setStackInSlot(0,ItemStack.EMPTY);tick(l,f,1);check(menu.progress()==0,"Removing iron resets progress");
        inv.setStackInSlot(0,new ItemStack(Items.IRON_INGOT));
        f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));saved=f.saveWithoutMetadata();saved.putInt("BurnTime",70000);saved.putInt("BurnTimeMax",80000);f.load(saved);
        check(menu.burnRemaining()==70000&&menu.burnTotal()==80000,"Full-width fuel survives NBT");
        var restored=new ShaftFurnaceBlockEntity(TEST,f.getBlockState());restored.load(f.saveWithoutMetadata());check(restored.hasEfficiency()&&restored.getDataAccess().get(2)==70000,"Module and fuel survive save/reload");
        saved.remove("Modules");saved.remove("UnifiedSideConfig");saved.remove("PortFacing");restored.load(saved);check(!restored.hasEfficiency()&&restored.sideMode(restored.facing().getClockWise())==SideMode.INPUT,"Legacy save retains original feed side");
        menu.setTab(202);check(!menu.getSlot(0).isActive()&&!menu.getSlot(2).mayPickup(p)&&!menu.getSlot(3).mayPickup(p)&&!menu.getSlot(40).isActive(),"Hidden machine and module slots are protected");
        p.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(p,100+RelativeSide.FRONT.ordinal()),"Menu refuses front port");
        menu.setTab(201);p.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,2));p.getInventory().setItem(10,new ItemStack(coke,2));
        check(!menu.quickMoveStack(p,4).isEmpty()&&!menu.quickMoveStack(p,5).isEmpty(),"Shift-click routes iron and coke to their own slots");
    }
'''+s[end:]
# Adapt remaining coke harness sections to iron / coke / two outputs.
start=s.index('    static void ports(')
head,tail=s[:start],s[start:]
tail=tail.replace('ModItems.COAL_COKE.get()','ModItems.STEEL_INGOT.get()')
tail=re.sub(r'\bItems.COAL\b','Items.IRON_INGOT',tail)
tail=tail.replace('inv.setStackInSlot(1,new ItemStack(Items.IRON_INGOT,2))','inv.setStackInSlot(1,new ItemStack(ModItems.COAL_COKE.get(),2))')
tail=tail.replace('cap.insertItem(1,new ItemStack(Items.IRON_INGOT),true)','cap.insertItem(1,new ItemStack(ModItems.COAL_COKE.get()),true)')
tail=tail.replace('inv.setStackInSlot(1,new ItemStack(Items.BUCKET));check(cap.extractItem(1,1,true).is(Items.BUCKET),"Orange extracts empty bucket "+s);','inv.setStackInSlot(3,new ItemStack(ModItems.SLAG.get()));check(cap.extractItem(3,1,true).is(ModItems.SLAG.get()),"Orange extracts slag "+s);')
tail=tail.replace('Orange protects coal and fuel','Orange protects iron and coke')
tail=tail.replace('new ItemStack(Items.CHARCOAL,2)','new ItemStack(ModItems.COAL_COKE.get(),2)').replace('new ItemStack(Items.CHARCOAL,3)','new ItemStack(ModItems.COAL_COKE.get(),3)').replace('count(l,Items.CHARCOAL)','count(l,ModItems.COAL_COKE.get())')
tail=tail.replace('f.getInventory().setStackInSlot(1,new ItemStack(Items.IRON_INGOT,64))','f.getInventory().setStackInSlot(1,new ItemStack(ModItems.COAL_COKE.get(),64))')
tail=tail.replace('containerId,3,0,ClickType.QUICK_MOVE','containerId,4,0,ClickType.QUICK_MOVE').replace('getSlot(39)','getSlot(40)')
tail=tail.replace('coke GUI','shaft GUI').replace('coke JEI','shaft JEI').replace('one coke recipe','one shaft recipe')
tail=tail.replace('f.getInventory().setStackInSlot(2,new ItemStack(ModItems.STEEL_INGOT.get(),3));','f.getInventory().setStackInSlot(2,new ItemStack(ModItems.STEEL_INGOT.get(),3));f.getInventory().setStackInSlot(3,new ItemStack(ModItems.SLAG.get(),2));')
tail=tail.replace('chest.countItem(ModItems.STEEL_INGOT.get())==3,','chest.countItem(ModItems.STEEL_INGOT.get())==3&&chest.countItem(ModItems.SLAG.get())==2,')
# Only the current machine is under review.
tail='\n'.join(line for line in tail.split('\n') if not ('add(10,()->otherGui(' in line))
s=head+tail
(dest/'ShaftFurnaceProbe.java').write_text(s)
(dest/'JeiCapture.java').write_text(convert((R/'dev/coke_oven_v2/runtime_probe/JeiCapture.java').read_text()))
for old,new in [('review.init.gradle','review.init.gradle'),('run_review.ps1','run_review.ps1')]:
    text=convert((R/'dev/coke_oven_v2'/old).read_text(encoding='utf-8-sig'))
    (out/new).write_text(text,encoding='utf-8-sig' if new.endswith('.ps1') else 'utf-8')
# Avoid first-launch FancyMenu dialogs in this isolated test profile only.
target=R/'run/shaft-furnace-review/config/fancymenu';target.mkdir(parents=True,exist_ok=True)
config=R/'run/coke-oven-review/config/fancymenu/options.txt'
(target/'options.txt').write_bytes(config.read_bytes())
print('Shaft review harness ready')
