from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parents[2]
J=R/'src/main/java/com/wasted/domesurvival/forge'
A=R/'src/main/resources/assets/domesurvival'
# Record authored assets before the GUI work; these must stay unchanged.
paths=list((A/'models/block').glob('*coke*'))+[A/'blockstates/coke_oven.json',A/'models/item/coke_oven.json']
paths+=list((A/'textures/block/metallurgy').rglob('*coke*'))
paths+=[A/'textures/block'/f'{n}.png' for n in ('bfbricks','bfbricksdark','bftoolst','firetools')]
baseline=Path(__file__).with_name('model_baseline.json')
if not baseline.exists(): baseline.write_text(json.dumps({str(p.relative_to(R)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths if p.is_file()},indent=2))

s=(J/'machine/copper/CopperFurnaceMenu.java').read_text()
s=s.replace('package com.wasted.domesurvival.forge.machine.copper;', 'package com.wasted.domesurvival.forge.machine.shaft;')
s=s.replace('CopperFurnace','CokeOven').replace('COPPER_FURNACE','COKE_OVEN')
s=s.replace('new SimpleContainer(3)','new ItemStackHandler(3)').replace('furnace,furnace,furnace.getDataAccess()', 'furnace,furnace.getInventory(),furnace.getDataAccess()')
s=s.replace('Container container,','IItemHandler container,')
s=s.replace('new Slot(container,','new SlotItemHandler(container,')
s=s.replace('new FurnaceResultSlot(inv.player,container,2,182,79)', 'new SlotItemHandler(container,2,182,79)')
s=s.replace('CokeOvenBlockEntity.acceptsInput(level,', 'CokeOvenBlockEntity.isValidCoal(')
s=s.replace('AbstractFurnaceBlockEntity.isFuel(', 'CokeOvenBlockEntity.isValidFuel(')
s=s.replace('public boolean mayPickup(Player p){return isActive();}\n        });\n        for(int row', 'public boolean mayPickup(Player p){return isActive();}\n            public boolean mayPlace(ItemStack s){return false;}\n        });\n        for(int row')
s=s.replace('public int burnRemaining(){return data.get(0);}public int burnTotal(){return data.get(1);}', 'public int burnRemaining(){return data.get(2);}public int burnTotal(){return data.get(3);}')
s=s.replace('public int progress(){return data.get(2);}public int progressMax(){return data.get(3);}', 'public int progress(){return data.get(0);}public int progressMax(){return data.get(1);}')
(J/'machine/shaft/CokeOvenMenu.java').write_text(s)
s=(J/'machine/copper/CopperFurnaceScreen.java').read_text()
s=s.replace('package com.wasted.domesurvival.forge.machine.copper;', 'package com.wasted.domesurvival.forge.client.screen;\nimport com.wasted.domesurvival.forge.machine.shaft.CokeOvenMenu;')
s=s.replace('CopperFurnace','CokeOven').replace('copper_v2','coke_v2').replace('copper_furnace_v2','coke_oven_v2').replace('CF-01 / Cu','CO-01 / C')
s=s.replace('0xFF211910','0xFFF1D7B9')
s=s.replace('else if(inside(x,y,new Rect(14,51,18,73)))help(g,t("burn",menu.burnRemaining(),menu.burnTotal()),x,y);', 'else if(inside(x,y,new Rect(14,51,18,73)))help(g,t("burn",menu.burnRemaining(),menu.burnTotal()),x,y);\n        else if(inside(x,y,new Rect(79,71,91,14)))help(g,t("progress",menu.progress(),menu.progressMax()),x,y);')
(J/'client/screen/CokeOvenScreen.java').write_text(s)

ru={'smelting':'Коксование угля','raw':'Уголь','fuel':'Топливо','result':'Кокс','duration':'Цикл: %s с',
'status.0':'Нужен каменный уголь','status.1':'Идёт коксование','status.2':'Нужно топливо','status.3':'Выход заполнен',
'efficiency':'Эффективность','fuel_bonus':'Горение топлива: +15%','same_speed':'Без ускорения цикла','next_fuel':'Со следующей порции',
'economy_on':'Горение топлива: +15%','economy_off':'Базовый расход топлива',
'input':'Синий — вход','input_short':'Уголь / топливо','output':'Оранж. — выход','output_short':'Кокс / тара',
'module_help':'Установите модуль эффективности в гнездо мышью или Shift+кликом из инвентаря на этой вкладке. Снимать можно так же, даже во время работы. Следующая порция топлива горит на 15% дольше; текущая не меняется. Цикл остаётся 80 секунд. Принимается один модуль.',
'unsupported_module':'Коксовая печь принимает только модуль эффективности. Она работает на топливе; разгон и энергетические модули не поддерживаются.',
'input_help':'Приём каменного угля в сырьё и печного топлива в топку. Уголь сначала заполняет слот сырья. Извлечение запрещено.',
'output_help':'Выдача готового кокса и негорючей тары из топки. Сырьё, горючее топливо и модули не извлекаются.',
'front':'Лицевая сторона: автоматизация отключена.', 'off':'Сторона отключена. Нажмите для переключения: вход → выход → выключено.',
'burn':'Топливо: %s / %s тиков','progress':'Коксование: %s / %s тиков','jei_time':'1 уголь → 1 кокс · 80 с','jei_base':'Без модулей · топливо'}
en={'smelting':'Coal coking','raw':'Coal','fuel':'Fuel','result':'Coke','duration':'Cycle: %s s',
'status.0':'Needs coal','status.1':'Coking','status.2':'Needs fuel','status.3':'Output blocked',
'efficiency':'Efficiency','fuel_bonus':'Fuel burn time: +15%','same_speed':'No processing speedup','next_fuel':'From the next fuel item',
'economy_on':'Fuel burn time: +15%','economy_off':'Base fuel consumption',
'input':'Blue — input','input_short':'Coal / fuel','output':'Orange — output','output_short':'Coke / containers',
'module_help':'Insert one efficiency module by clicking the socket or Shift-clicking it from your inventory on this tab. Remove it the same way, including during operation. The next fuel item burns 15% longer; current fuel is unchanged. The cycle remains 80 seconds.',
'unsupported_module':'The coke oven accepts only an efficiency module. It uses fuel; overdrive and energy modules are unsupported.',
'input_help':'Accepts coal as feedstock and furnace fuel. Coal fills the feedstock slot first. No extraction.',
'output_help':'Extracts finished coke and non-fuel containers. Feedstock, usable fuel and modules cannot be extracted.',
'front':'Front face: automation disabled.','off':'Disabled. Click to cycle input, output, disabled.',
'burn':'Fuel: %s / %s ticks','progress':'Coking: %s / %s ticks','jei_time':'1 coal → 1 coke · 80 s','jei_base':'Base mode · furnace fuel'}
for lang,strings in [('ru_ru',ru),('en_us',en)]:
    p=A/'lang'/f'{lang}.json';d=json.loads(p.read_text(encoding='utf-8'))
    d.update({'gui.domesurvival.coke_v2.'+k:v for k,v in strings.items()})
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Coke oven sources and asset baseline prepared')
