from pathlib import Path
import json,re
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/com/wasted/domesurvival/forge';A=R/'src/main/resources/assets/domesurvival'
def write(p,s):p.write_text(s,encoding='utf-8')
# Reuse the established port geometry and native dismantle save path.
s=(J/'machine/oxygen/OxygenFillerBlock.java').read_text(encoding='utf-8').replace('package com.wasted.domesurvival.forge.machine.oxygen;','package com.wasted.domesurvival.forge.machine.filter;').replace('OxygenFiller','FilterRegeneration').replace('LIT','ACTIVE')
s=s.replace('BlockStateProperties.ACTIVE','BlockStateProperties.LIT').replace('public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;','public static final BooleanProperty ACTIVE = BooleanProperty.create("active");')
s=s.replace('ModBlockEntities.OXYGEN_FILLER','FilterRegenerationRegistry.FILTER_REGENERATION_BLOCK_ENTITY')
s=s.replace('''            if (level instanceof ServerLevel serverLevel) {
                SealedRoomManager.forgetOutlet(serverLevel, pos.above());
            }
''','').replace('import com.wasted.domesurvival.forge.oxygen.room.SealedRoomManager;\n','')
write(J/'machine/filter/FilterRegenerationBlock.java',s)
p=J/'machine/filter/FilterRegenerationRegistry.java';s=p.read_text(encoding='utf-8')
if '.noOcclusion()' not in s:s=s.replace('.strength(4.0F, 8.0F)', '.strength(4.0F, 8.0F).noOcclusion()')
s=re.sub(r'return Component.literal\([^;]+;', 'return Component.translatable("block.domesurvival.filter_regeneration_station");',s);write(p,s)
s=(J/'machine/oxygen/OxygenFillerMenu.java').read_text(encoding='utf-8').replace('package com.wasted.domesurvival.forge.machine.oxygen;','package com.wasted.domesurvival.forge.machine.filter;').replace('OxygenFiller','FilterRegeneration').replace('new ItemStackHandler(2),new SimpleContainerData','new ItemStackHandler(3),new SimpleContainerData')
s=s.replace('ModMenuTypes.OXYGEN_FILLER','FilterRegenerationRegistry.FILTER_REGENERATION_MENU').replace('ModBlocks.OXYGEN_FILLER','FilterRegenerationRegistry.FILTER_REGENERATION_STATION')
s=s.replace('isMainPanelOpen()&&getOperatingMode()==FilterRegenerationMode.TANK_FILLING','isMainPanelOpen()')
s=s.replace('public boolean mayPickup(Player player){return isActive();}', 'public boolean mayPickup(Player player){return isActive()&&(furnace==null?progress()==0:furnace.canRemoveFilter());}',1)
s=s.replace('FilterRegenerationBlockEntity.acceptsTank','FilterRegenerationBlockEntity.isEligibleFilter')
s=s.replace('addSlot(new SlotItemHandler(container,1,180,103)', 'addSlot(new SlotItemHandler(container,2,180,103)')
marker='        addSlot(new SlotItemHandler(container,2,180,103)'
s=s.replace(marker,'''        addSlot(new SlotItemHandler(container,1,52,67){
            public boolean isActive(){return isMainPanelOpen();}
            public boolean mayPickup(Player player){return isActive();}
            public boolean mayPlace(ItemStack stack){return isActive()&&FilterRegenerationBlockEntity.isRegenerationMedia(stack);}
        });
'''+marker)
a=s.index('    public int energyStored()');b=s.index('    public SideMode getSideMode',a)
s=s[:a]+'''    public int energyStored(){return data.get(0);}public int energyCapacity(){return data.get(1);}
    public int progress(){return data.get(2);}public int progressMax(){return data.get(3);}
    public int status(){return data.get(4);}public int regenerationCycles(){return data.get(5);}
    public int maxRegenerationCycles(){return data.get(6);}public int cycleEnergy(){return data.get(7);}
'''+s[b:]
s=s.replace('data.get(12+side.resolve(facing).ordinal())','data.get(8+side.resolve(facing).ordinal())')
s=s.replace('        if(id==50 && isMainPanelOpen()){if(furnace!=null)furnace.cycleOperatingMode();return true;}\n','')
s=s.replace('moveItemStackTo(stack,38,40,false)','moveItemStackTo(stack,39,41,false)')
s=s.replace('        else if(index<27)', '        else if(FilterRegenerationBlockEntity.isRegenerationMedia(stack)&&isMainPanelOpen()){if(!moveItemStackTo(stack,37,38,false))return ItemStack.EMPTY;}\n        else if(index<27)')
write(J/'machine/filter/FilterRegenerationMenu.java',s)
# Moving carriage: 0..4 model pixels along its existing vertical guides.
s=(J/'client/render/OxygenFillerRenderer.java').read_text(encoding='utf-8').replace('OxygenFiller','FilterRegeneration').replace('machine.oxygen.*','machine.filter.*').replace('models/block/oxygen_filler_','models/block/filter_regeneration_station_')
s=s.replace('    private final List<Cube> needle=new ArrayList<>();\n','').replace('load("pump",cubes);load("needle",needle);','load("carriage",cubes);')
s=s.replace('com.wasted.domesurvival.forge.registry.ModBlockEntities.OXYGEN_FILLER','FilterRegenerationRegistry.FILTER_REGENERATION_BLOCK_ENTITY')
s=s.replace('0,(int)(1000L*press.oxygenAmount()/FilterRegenerationBlockEntity.OXYGEN_CAPACITY)','0,0')
a=s.index('        pose.pushPose();pose.translate(0,Math.sin');b=s.index('    private int alpha',a)
s=s[:a]+'''        pose.pushPose();pose.translate(0,(1-Math.cos(rotation*Math.PI*2))*.125,0);
        for(Cube cube:cubes)box(consumer,pose.last(),cube,light,overlay);
        pose.popPose();pose.popPose();
    }
'''+s[b:];write(J/'client/render/FilterRegenerationRenderer.java',s)
s=(J/'client/render/OxygenFillerPreview.java').read_text(encoding='utf-8').replace('OxygenFiller','FilterRegeneration').replace('machine.oxygen.*','machine.filter.*').replace('ModBlocks.OXYGEN_FILLER','FilterRegenerationRegistry.FILTER_REGENERATION_STATION').replace('FilterRegenerationBlock.LIT','FilterRegenerationBlock.ACTIVE')
write(J/'client/render/FilterRegenerationPreview.java',s)
# Existing screen class location remains stable for ClientModEvents registration.
s=(J/'client/screen/OxygenFillerScreen.java').read_text(encoding='utf-8').replace('package com.wasted.domesurvival.forge.client.screen;','package com.wasted.domesurvival.forge.machine.filter;').replace('import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerMenu;','').replace('OxygenFiller','FilterRegeneration').replace('filler_v2','filter_regeneration_v2').replace('oxygen_filter_regeneration_v2','filter_regeneration_v2')
a=s.index('    private boolean ventilation()');b=s.index('        if(button==0&&inside',a)
s=s[:a]+'''    @Override public boolean mouseClicked(double x,double y,int button){
'''+s[b:]
s=s.replace('g.blit(ventilation()?tex("ventilation"):PANEL','g.blit(PANEL')
a=s.index('            gas(g,');b=s.index('\n\n        }',a)
s=s[:a]+'''            com.wasted.domesurvival.forge.client.render.FilterRegenerationPreview.draw(g,leftPos+123,topPos+76,27,menu.status()==1,0,0);
            int width=(int)Math.min(85,(long)menu.progress()*85/Math.max(1,menu.progressMax()));
            if(width>0){g.enableScissor(leftPos+82,topPos+111,leftPos+82+width,topPos+119);widget(g,82,111,85,8,0,48,64,8);g.disableScissor();}
'''+s[b:]
a=s.index('    private Component roomPercent()');b=s.index('    @Override protected void renderLabels',a)
s=s[:a]+'''    private Component statusText(){return t("status."+menu.status());}
'''+s[b:]
a=s.index('        }else if(ventilation())');b=s.index('        text(g,playerInventoryTitle',a)
s=s[:a]+'''        }else{
            text(g,t("media"),46,49,54,MUTED);
        }
'''+s[b:]
a=s.index('        else if(inside(x,y,new Rect(150,34');b=s.index('    private record Rect',a)
s=s[:a]+'''        else if(inside(x,y,new Rect(79,108,91,14)))help(g,t("cycle",menu.cycleEnergy(),String.format(Locale.ROOT,"%.1f",menu.progressMax()/20.0),menu.regenerationCycles(),menu.maxRegenerationCycles()),x,y);
        else if(inside(x,y,new Rect(96,45,71,57)))help(g,statusText(),x,y);
    }
'''+s[b:]
write(J/'machine/filter/FilterRegenerationScreen.java',s)
for loc in ('ru_ru','en_us'):
    p=A/f'lang/{loc}.json';d=json.loads(p.read_text(encoding='utf-8'));ru=loc=='ru_ru'
    vals={
        'media':('Сорбент','Media'),'input':('Вход','Input'),'output':('Выход','Output'),
        'input_short':('Фильтр, сорбент','Filter / media'),'output_short':('Готовый фильтр','Ready filter'),
        'input_help':('Приём FE, фильтров и сорбента.','Accepts FE, filters and media.'),
        'output_help':('Выдача исправного фильтра или фильтра с исчерпанным лимитом восстановления.','Outputs a healthy filter or one that reached its regeneration limit.'),
        'front':('Лицевая сторона — рабочая камера.','Front face: treatment chamber.'),'off':('Сторона отключена.','Side disabled.'),
        'module_title':('Буфер / эффективность','Buffer / efficiency'),'module_next':('или разгон','or overdrive'),
        'module_conflict':('Разгон ≠ эффективность','Overdrive ≠ efficiency'),
        'module_help':('Два гнезда. Модули меняются во время работы; новый расход и скорость действуют со следующего цикла.','Two sockets. Modules can be changed while running; cost and speed apply from the next cycle.'),
        'buffer_help':('Буфер: 20 000 → 35 000 FE. Снятие при заряде ≤ 20 000 FE.','Buffer: 20,000 → 35,000 FE. Remove at ≤ 20,000 FE.'),
        'efficiency_help':('Расход за цикл: −20%. Скорость: −10%. Несовместим с разгоном.','Cycle energy: −20%. Speed: −10%. Incompatible with overdrive.'),
        'overdrive_help':('Скорость: +35%. Энергия за цикл: +60%. Несовместим с эффективностью.','Speed: +35%. Cycle energy: +60%. Incompatible with efficiency.'),
        'unsupported_module':('Модуль не поддерживается.','Unsupported module.'),
        'energy':('%s / %s FE','%s / %s FE'),
        'cycle':('%s FE · %s с за 25%% восстановления. Использовано циклов: %s / %s.','%s FE · %s s per 25%% restored. Regenerations used: %s / %s.'),
        'jei_details':('%s FE · %s с','%s FE · %s s'),
        'jei_note':('Один сорбент восстанавливает 25% ресурса. Максимум 8 циклов на фильтр; машина продолжает до полного восстановления или лимита.','One media restores 25% durability. Up to 8 cycles per filter; the machine repeats until healthy or exhausted.')}
    for k,v in vals.items():d['gui.domesurvival.filter_regeneration_v2.'+k]=v[0 if ru else 1]
    states=['Готов к работе','Регенерация','Нет энергии','Нет фильтра','Нет сорбента','Фильтр исправен','Лимит исчерпан','Выход занят'] if ru else ['Ready','Regenerating','No energy','No filter','No media','Filter healthy','Limit reached','Output occupied']
    for i,v in enumerate(states):d[f'gui.domesurvival.filter_regeneration_v2.status.{i}']=v
    write(p,json.dumps(d,ensure_ascii=False,indent=2)+'\n')
print('Filter integration prepared')
