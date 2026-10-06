from pathlib import Path
R=Path(__file__).resolve().parents[2]; J=R/'src/main/java/com/wasted/domesurvival/forge'
s=(J/'machine/shaft/ShaftFurnaceMenu.java').read_text().replace('machine.shaft','machine.water').replace('ShaftFurnace','WaterPurifier')
s=s.replace('new ItemStackHandler(4)','new ItemStackHandler(2)')
a=s.index('        addSlot(new SlotItemHandler(container,0,')
b=s.index('        for(int row=0;',a)
s=s[:a]+'''        addSlot(new SlotItemHandler(container,0,46,111){
            public boolean isActive(){return isMainPanelOpen();}public boolean mayPickup(Player p){return isActive();}
            public boolean mayPlace(ItemStack s){return isActive()&&s.is(net.minecraft.world.item.Items.WATER_BUCKET);}
        });
        addSlot(new SlotItemHandler(container,1,182,111){
            public boolean isActive(){return isMainPanelOpen();}public boolean mayPickup(Player p){return isActive();}
            public boolean mayPlace(ItemStack s){return isActive()&&s.getItem() instanceof com.wasted.domesurvival.forge.item.WaterFilterItem;}
        });
'''+s[b:]
a=s.index('        addSlot(new SlotItemHandler(furnace==null')
b=s.index('    public boolean isMainPanelOpen()',a)
s=s[:a]+'''        for(int i=0;i<2;i++)addSlot(new SlotItemHandler(furnace==null?new ItemStackHandler(2):furnace.getModules(),i,22,67+i*30){
            public boolean isActive(){return isModulePanelOpen();}
            public boolean mayPickup(Player p){return isActive()&&(!(getItem().getItem() instanceof MachineModuleItem m)||m.module().type()!=MachineModuleType.BUFFER||energyStored()<=WaterPurifierBlockEntity.ENERGY_CAPACITY);}
            public int getMaxStackSize(){return 1;}
            public boolean mayPlace(ItemStack s){return isActive()&&s.getItem() instanceof MachineModuleItem&&super.mayPlace(s);}
        });
    }
'''+s[b:]
a=s.index('    public int burnRemaining()')
b=s.index('    public SideMode getSideMode',a)
s=s[:a]+'''    public int energyStored(){return data.get(0);} public int energyCapacity(){return data.get(1);}
    public int rawWater(){return data.get(2);}public int rawCapacity(){return data.get(3);}
    public int purifiedWater(){return data.get(4);}public int purifiedCapacity(){return data.get(5);}
    public int progress(){return data.get(6);}public int progressMax(){return data.get(7);}
    public int status(){return data.get(8);}public int cycleEnergy(){return data.get(15);}public int filterRemaining(){return data.get(16);}
'''+s[b:]
s=s.replace('data.get(4+side.resolve(facing).ordinal())','data.get(9+side.resolve(facing).ordinal())')
a=s.index('        if(index<4||index==40)')
b=s.index('        if(stack.getCount()==copy.getCount())',a)
s=s[:a]+'''        if(index<2||index>=38){if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof MachineModuleItem){if(!isModulePanelOpen()||!moveItemStackTo(stack,38,40,false))return ItemStack.EMPTY;}
        else if(isMainPanelOpen()&&stack.is(net.minecraft.world.item.Items.WATER_BUCKET)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(isMainPanelOpen()&&stack.getItem() instanceof com.wasted.domesurvival.forge.item.WaterFilterItem){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(index<29){if(!moveItemStackTo(stack,29,38,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,2,29,false))return ItemStack.EMPTY;
'''+s[b:]
s=s.replace('        if(index==2||index==3)slot.onQuickCraft(stack,copy);\n','')
(J/'machine/water/WaterPurifierMenu.java').write_text(s)
s=(J/'client/screen/ShaftFurnaceScreen.java').read_text().replace('machine.shaft','machine.water').replace('ShaftFurnace','WaterPurifier').replace('shaft_v2','purifier_v2').replace('shaft_furnace_v2','water_purifier_v2')
# Align overlay to y=29 while module wells are authored relative to it.
a=s.index('            int height=')
b=s.index('\n        }\n        if(menu.isModulePanelOpen()',a)
s=s[:a]+'''            int height=(int)Math.min(80,(long)menu.energyStored()*80/Math.max(1,menu.energyCapacity()));
            if(height>0)widget(g,17,126-height,10,height,64,0,12,47);
            fluid(g,45,46,22,50,menu.rawWater(),menu.rawCapacity(),net.minecraft.world.level.material.Fluids.WATER);
            fluid(g,181,46,22,50,menu.purifiedWater(),menu.purifiedCapacity(),com.wasted.domesurvival.forge.fluid.ModFluids.PURIFIED_WATER.get());
            com.wasted.domesurvival.forge.client.render.WaterPurifierPreview.draw(g,leftPos+123,topPos+70,25,menu.status()==1,menu.rawWater(),menu.purifiedWater());
            int width=(int)Math.min(85,(long)menu.progress()*85/Math.max(1,menu.progressMax()));
            if(width>0){g.enableScissor(leftPos+82,topPos+111,leftPos+82+width,topPos+119);widget(g,82,111,85,8,0,48,64,8);g.disableScissor();}
'''+s[b:]
a=s.index('            text(g,t("efficiency")')
b=s.index('        }else if(menu.isSidePanelOpen())',a)
s=s[:a]+'''            text(g,t("module_title"),59,54,145,COPPER);text(g,t("module_next"),59,72,145,TEXT);
            text(g,t("module_install"),59,89,145,MUTED);text(g,t("module_conflict"),59,106,145,MUTED);
'''+s[b:]
a=s.index('            text(g,t("smelting")')
b=s.index('\n        }\n        text(g,playerInventoryTitle',a)
s=s[:a]+'''            text(g,t("conversion"),44,33,162,TEXT);
            text(g,t("energy",menu.energyStored(),menu.energyCapacity()),14,132,192,MUTED);
'''+s[b:]
s=s.replace('module.module().type()==MachineModuleType.EFFICIENCY?"module_help":"unsupported_module"','"module_help"')
a=s.index('        }else if(inside(x,y,new Rect(14,58')
b=s.index('\n    }\n    private record Rect',a)
s=s[:a]+'''        }else if(inside(x,y,new Rect(14,43,16,86)))help(g,t("energy",menu.energyStored(),menu.energyCapacity()),x,y);
        else if(inside(x,y,new Rect(42,43,28,56)))help(g,t("raw",menu.rawWater(),menu.rawCapacity()),x,y);
        else if(inside(x,y,new Rect(178,43,28,56)))help(g,t("purified",menu.purifiedWater(),menu.purifiedCapacity()),x,y);
        else if(inside(x,y,new Rect(79,108,91,14)))help(g,t("process",menu.progress(),menu.progressMax(),menu.cycleEnergy()),x,y);
        else if(inside(x,y,new Rect(42,107,24,24)))help(g,t("bucket_help"),x,y);
        else if(inside(x,y,new Rect(178,107,24,24)))help(g,t("filter_help"),x,y);
'''+s[b:]
insert='''    private void fluid(GuiGraphics g,int x,int y,int w,int h,int amount,int capacity,net.minecraft.world.level.material.Fluid fluid){
        int filled=(int)((long)h*amount/Math.max(1,capacity));if(filled<=0)return;
        var ext=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
        int tint=ext.getTintColor();g.setColor(((tint>>16)&255)/255F,((tint>>8)&255)/255F,(tint&255)/255F,1);
        g.blit(leftPos+x,topPos+y+h-filled,0,w,filled,sprite);g.setColor(1,1,1,1);
    }
'''
s=s.replace('    private record Rect',insert+'    private record Rect')
(J/'client/screen/WaterPurifierScreen.java').write_text(s)
print('WATER_PURIFIER_UI_PREPARED')
