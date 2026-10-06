from pathlib import Path
import json
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/com/wasted/domesurvival/forge';A=R/'src/main/resources/assets/domesurvival'
s=(J/'client/screen/OxygenElectrolyzerScreen.java').read_text(encoding='utf-8').replace('OxygenElectrolyzer','OxygenFiller').replace('oxygen_electrolyzer','oxygen_filler').replace('electrolyzer_v2','filler_v2')
s=s.replace('    @Override public boolean mouseClicked(double x,double y,int button){','''    private boolean ventilation(){return menu.getOperatingMode()==com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerMode.VENTILATION;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&menu.isMainPanelOpen()) {
            if(inside(x,y,new Rect(44,34,22,18))){if(ventilation())send(50);return true;}
            if(inside(x,y,new Rect(70,34,22,18))){if(!ventilation())send(50);return true;}
        }''')
s=s.replace('g.blit(PANEL,leftPos,topPos','g.blit(ventilation()?tex("ventilation"):PANEL,leftPos,topPos')
a=s.index('            fluid(g,');b=s.index('\n\n        }',a)
s=s[:a]+'''            gas(g,leftPos+48,topPos+56,24,40,menu.oxygen(),menu.oxygenCapacity());
            com.wasted.domesurvival.forge.client.render.OxygenFillerPreview.draw(g,leftPos+123,topPos+76,27,menu.status()==1||menu.status()==6,0,(int)(1000L*menu.oxygen()/Math.max(1,menu.oxygenCapacity())));
            if(ventilation())gas(g,leftPos+180,topPos+56,24,40,menu.roomOxygen(),menu.roomCapacity());
            else {
                int width=(int)Math.min(85,(long)menu.tankOxygen()*85/Math.max(1,menu.tankCapacity()));
                if(width>0){g.enableScissor(leftPos+82,topPos+111,leftPos+82+width,topPos+119);widget(g,82,111,85,8,0,48,64,8);g.disableScissor();}
            }
            modeIcon(g,44,34,false,!ventilation(),inside(mx,my,new Rect(44,34,22,18)));
            modeIcon(g,70,34,true,ventilation(),inside(mx,my,new Rect(70,34,22,18)));
'''+s[b:]
s=s.replace('            text(g,t("conversion"),44,33,162,TEXT);','')
a=s.index('        else if(inside(x,y,new Rect(42,43');b=s.index('\n\n    }',a)
s=s[:a]+'''        else if(inside(x,y,new Rect(44,34,22,18)))help(g,t("mode_tank"),x,y);
        else if(inside(x,y,new Rect(70,34,22,18)))help(g,t("mode_vent"),x,y);
        else if(inside(x,y,new Rect(46,54,28,44)))help(g,t("oxygen",menu.oxygen(),menu.oxygenCapacity()),x,y);
        else if(ventilation()&&inside(x,y,new Rect(178,54,28,44)))help(g,t("room",menu.roomOxygen(),menu.roomCapacity(),menu.roomVolume()),x,y);
        else if(!ventilation()&&inside(x,y,new Rect(79,108,91,14)))help(g,t("tank",menu.tankOxygen(),menu.tankCapacity()),x,y);
        else if(inside(x,y,new Rect(96,45,71,57)))help(g,Component.translatable("gui.domesurvival.oxygen_filler.status."+menu.status()),x,y);
'''+s[b:]
# The shared gas remains identical to the newly approved electrolyzer gauge.
a=s.index('    private void fluid(');b=s.index('    private record Rect',a)
s=s[:a]+'''    private void modeIcon(GuiGraphics g,int x,int y,boolean air,boolean selected,boolean hovered){
        int px=leftPos+x,py=topPos+y;
        g.blit(tex(selected?"mode_frame_selected":hovered?"mode_frame_hover":"mode_frame"),px,py,22,18,0,0,88,72,88,72);
        g.pose().pushPose();
        g.pose().translate(px+3F,py+2.6F,0);
        g.pose().scale(.8F,.8F,1F);
        g.blit(tex(air?"mode_vent":"mode_tank"),0,0,20,16,0,0,80,64,80,64);
        g.pose().popPose();
    }
'''+s[b:]
s=s.replace('    @Override protected void renderLabels(', '''    private Component roomPercent(){
        int capacity=menu.roomCapacity();
        if(capacity<=0)return Component.literal("—");
        int percent=(int)Math.min(100L,Math.round((double)menu.roomOxygen()*100/capacity));
        return Component.literal(percent+"%");
    }
    private Component statusText(){return Component.translatable("gui.domesurvival.oxygen_filler.status."+menu.status());}
    @Override protected void renderLabels(''')
s=s.replace('''        }else{


        }
        text(g,playerInventoryTitle''', '''        }else if(ventilation()){
            g.drawCenteredString(font,Component.literal("O₂"),192,42,BLUE);
            text(g,roomPercent(),179,102,28,TEXT);
            text(g,statusText(),46,112,160,menu.status()==6||menu.status()==11?BLUE:menu.status()==0?MUTED:COPPER);
        }
        text(g,playerInventoryTitle''')
s=s.replace('else if(menu.isModulePanelOpen()&&inside(x,y,new Rect(18,49,185,73)))help(g,t("module_help"),x,y);', '''else if(menu.isModulePanelOpen()){
            if(inside(x,y,new Rect(18,49,185,73)))help(g,t("module_help"),x,y);
        }''')
s=s.replace('        else if(!ventilation()&&inside(x,y,new Rect(79,108,91,14)))', '        else if(ventilation()&&inside(x,y,new Rect(46,108,160,18)))help(g,statusText(),x,y);\n        else if(!ventilation()&&inside(x,y,new Rect(79,108,91,14)))')
s=s.replace('help(g,Component.translatable("gui.domesurvival.oxygen_filler.status."+menu.status()),x,y);','help(g,statusText(),x,y);')
s=s.replace('Rect(44,34,22,18)', 'Rect(150,34,22,18)').replace('Rect(70,34,22,18)', 'Rect(180,34,22,18)')
s=s.replace('modeIcon(g,44,34,', 'modeIcon(g,150,34,').replace('modeIcon(g,70,34,', 'modeIcon(g,180,34,')
s=s.replace('            g.drawCenteredString(font,Component.literal("O₂"),192,42,BLUE);\n','')
(J/'client/screen/OxygenFillerScreen.java').write_text(s,encoding='utf-8')
for locale in ('ru_ru','en_us'):
    p=A/f'lang/{locale}.json';data=json.loads(p.read_text(encoding='utf-8'));ru=locale=='ru_ru'
    for k,v in list(data.items()):
        if k.startswith('gui.domesurvival.electrolyzer_v2.'):
            data[k.replace('electrolyzer_v2','filler_v2')]=v
    values={
      'mode_tank':('Заправка баллонов','Tank filling'),'mode_vent':('Вентиляция помещения','Room ventilation'),
      'input_short':('O₂ / FE / баллоны','O₂ / FE / tanks'),'output_short':('Баллоны / O₂','Tanks / O₂'),
      'input_help':('Приём кислорода, энергии и неполных баллонов.','Accepts oxygen, energy and non-full tanks.'),
      'output_help':('Выдача полных баллонов и кислорода из буфера.','Outputs full tanks and buffered oxygen.'),
      'module_title':('Буфер / эффективность','Buffer / efficiency'),
      'module_next':('Без ускорения заправки','Filling speed unchanged'),
      'module_conflict':('По одному каждого типа','One of each type'),
      'module_help':('Два гнезда: энергобуфер и эффективность. Замена доступна во время работы.','Two sockets: energy buffer and efficiency. Can be changed while running.'),
      'buffer_help':('Буфер: 20 000 → 35 000 FE. Снятие при заряде ≤ 20 000 FE.','Buffer: 20,000 → 35,000 FE. Remove at ≤ 20,000 FE.'),
      'efficiency_help':('Заправка: 5 → 4 FE на единицу O₂. Скорость и вентиляция не меняются.','Filling: 5 → 4 FE per O₂. Speed and ventilation unchanged.'),
      'overdrive_help':('Разгон не поддерживается.','Overdrive is not supported.'),
      'unsupported_module':('Не подходит для наполнителя.','Not supported by the filler.'),
      'room':('Помещение: %s / %s O₂ · объём %s блоков','Room: %s / %s O₂ · volume %s blocks'),
      'tank':('Баллон: %s / %s O₂','Tank: %s / %s O₂'),
      'jei_details':('%s FE · %s с','%s FE · %s s')}
    for k,v in values.items():data['gui.domesurvival.filler_v2.'+k]=v[0 if ru else 1]
    statuses=['Ожидание','Идёт заправка','Нет баллона','Баллон заполнен','Нет кислорода','Нет энергии','Подача воздуха','Выпуск сверху закрыт','Помещение негерметично','Помещение слишком большое','Часть помещения не загружена','Помещение заполнено','Утечка кислорода','Помещение разгерметизировано','Выход занят'] if ru else ['Idle','Filling','No tank','Tank full','No oxygen','No energy','Ventilating','Top outlet blocked','Room is open','Room too large','Room unloaded','Room full','Oxygen leak','Room depressurized','Output occupied']
    for i,v in enumerate(statuses):data['gui.domesurvival.oxygen_filler.status.'+str(i)]=v
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
