from pathlib import Path
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/com/wasted/domesurvival/forge'
s=(J/'machine/oxygen/OxygenElectrolyzerMenu.java').read_text(encoding='utf-8').replace('OxygenElectrolyzer','OxygenFiller').replace('OXYGEN_ELECTROLYZER','OXYGEN_FILLER')
s=s.replace('new ItemStackHandler(0),furnace.getDataAccess()','furnace.getInventory(),furnace.getDataAccess()')
needle='        for(int i=0;i<2;i++)addSlot(new SlotItemHandler'
i=s.index(needle)
s=s[:i]+'''        addSlot(new SlotItemHandler(container,0,52,103){
            public boolean isActive(){return isMainPanelOpen()&&getOperatingMode()==OxygenFillerMode.TANK_FILLING;}
            public boolean mayPickup(Player player){return isActive();}
            public boolean mayPlace(ItemStack stack){return isActive()&&OxygenFillerBlockEntity.acceptsTank(stack);}
            public int getMaxStackSize(){return 1;}
        });
        addSlot(new SlotItemHandler(container,1,180,103){
            public boolean isActive(){return isMainPanelOpen()&&getOperatingMode()==OxygenFillerMode.TANK_FILLING;}
            public boolean mayPickup(Player player){return isActive();}
            public boolean mayPlace(ItemStack stack){return false;}
        });
'''+s[i:]
a=s.index('    public int energyStored()');b=s.index('    public SideMode getSideMode',a)
s=s[:a]+'''    public int energyStored(){return data.get(0);}public int energyCapacity(){return data.get(1);}
    public int oxygen(){return data.get(2);}public int oxygenCapacity(){return data.get(3);}
    public int tankOxygen(){return data.get(4);}public int tankCapacity(){return data.get(5);}
    public int status(){return data.get(6);}public int fillEnergyCost(){return data.get(18);}
    public OxygenFillerMode getOperatingMode(){return OxygenFillerMode.byOrdinal(data.get(7));}
    public int roomVolume(){return data.get(9);}public int roomOxygen(){return data.get(10);}public int roomCapacity(){return data.get(11);}
'''+s[b:]
s=s.replace('data.get(9+side.resolve(facing).ordinal())','data.get(12+side.resolve(facing).ordinal())')
s=s.replace('        if(id==MAIN_TAB','        if(id==50 && isMainPanelOpen()){if(furnace!=null)furnace.cycleOperatingMode();return true;}\n        if(id==MAIN_TAB')
s=s.replace('moveItemStackTo(stack,36,38,false)','moveItemStackTo(stack,38,40,false)')
s=s.replace('        else if(index<27)', '        else if(OxygenFillerBlockEntity.acceptsTank(stack)&&isMainPanelOpen()&&getOperatingMode()==OxygenFillerMode.TANK_FILLING){if(!moveItemStackTo(stack,36,37,false))return ItemStack.EMPTY;}\n        else if(index<27)')
(J/'machine/oxygen/OxygenFillerMenu.java').write_text(s,encoding='utf-8')
