from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[2]
ru={
'efficiency':'Эффективность','fuel_bonus':'Топливо: +15%','same_speed':'Без ускорения','next_fuel':'Со следующего топлива',
'input':'Синий — вход','input_short':'Сырьё / топливо','output':'Оранжевый — выход','output_short':'Продукт / тара',
'smelting':'Плавильная камера','raw':'Сырьё','fuel':'Топливо','result':'Выход','duration':'Цикл: %s с',
'status.0':'Ожидание сырья','status.1':'Плавка','status.2':'Нужно топливо','status.3':'Нет рецепта','status.4':'Выход заполнен',
'economy_on':'Экономия топлива: +15%','economy_off':'Угольный нагрев · без FE',
'module_help':'Модуль эффективности: топливо горит на 15% дольше. Скорость плавки не меняется. Установка: перетащите модуль в гнездо или нажмите Shift+клик на этой вкладке. Можно менять во время работы; уже горящее топливо сохраняет свою длительность, новая настройка действует со следующей порции топлива.',
'unsupported_module':'Этой угольной печи подходит только модуль эффективности. Разгон и энергетические модули не поддерживаются.',
'front':'Лицевая сторона зарезервирована для дверцы. Порты недоступны.',
'input_help':'Принимает сырьё для плавки и топливо в соответствующие слоты. Электроэнергия не используется.',
'output_help':'Выдаёт готовый продукт и пустую тару из топливного слота. Сырьё и пригодное топливо извлечь нельзя.',
'off':'Сторона отключена','burn':'Горение: %s / %s тиков',
'jei.base':'Базовый режим · без модулей','jei.fuel':'Топливо: %s с','jei.coal':'Выработка: %s FE','jei.rate':'%s FE/т · %s с',
'jei.forming':'%s FE · %s с','jei.copper':'Плавка · %s с','jei.generator':'Угольный генератор'}
en={
'efficiency':'Efficiency','fuel_bonus':'Fuel duration: +15%','same_speed':'No speed increase','next_fuel':'Applies to next fuel',
'input':'Blue — input','input_short':'Material / fuel','output':'Orange — output','output_short':'Product / container',
'smelting':'Smelting chamber','raw':'Material','fuel':'Fuel','result':'Output','duration':'Cycle: %s s',
'status.0':'Awaiting input','status.1':'Smelting','status.2':'Fuel required','status.3':'No recipe','status.4':'Output full',
'economy_on':'Fuel economy: +15%','economy_off':'Solid fuel heat · no FE',
'module_help':'Efficiency module: fuel lasts 15% longer, with unchanged smelting speed. Drag it into the socket or Shift-click it on this tab. Installation and removal are allowed while running. Burning fuel retains its original duration; the new setting applies when the next fuel item ignites.',
'unsupported_module':'This fuel furnace only accepts the efficiency module. Overdrive and energy upgrades are not supported.',
'front':'The front is reserved for the door. No ports are available.',
'input_help':'Accepts smelting ingredients and fuel into their matching slots. Does not use electricity.',
'output_help':'Extracts finished products and empty containers from the fuel slot. Input materials and usable fuel cannot be extracted.',
'off':'Side disabled','burn':'Burn time: %s / %s ticks',
'jei.base':'Base mode · no modules','jei.fuel':'Fuel: %s s','jei.coal':'Generation: %s FE','jei.rate':'%s FE/t · %s s',
'jei.forming':'%s FE · %s s','jei.copper':'Smelting · %s s','jei.generator':'Coal Generator'}
for lang,entries in [('ru_ru',ru),('en_us',en)]:
    path=root/f'src/main/resources/assets/domesurvival/lang/{lang}.json'
    text=path.read_text(encoding='utf-8')
    for key,value in entries.items():
        key='gui.domesurvival.copper_v2.'+key
        encoded=json.dumps(value,ensure_ascii=False)
        pattern=r'("'+re.escape(key)+r'"\s*:\s*)"(?:\\.|[^"\\])*"'
        if re.search(pattern,text):text=re.sub(pattern,lambda m:m.group(1)+encoded,text)
        else:text=text.rstrip()[:-1].rstrip()+',\n  '+json.dumps(key)+': '+encoded+'\n}\n'
    json.loads(text);path.write_text(text,encoding='utf-8')
