"""Idempotently update only module guidance and the duplicated machine name."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[2]/'src/main/resources/assets'
for lang in ('ru_ru','en_us'):
    ru=lang=='ru_ru'
    updates={
        'tooltip.domesurvival.module.install': 'Установка: вкладка «Модули» в машине.' if ru else 'Install in the machine\'s Modules tab.',
        'tooltip.domesurvival.module.install_action': 'Перенесите в гнездо мышью или Shift+кликом.' if ru else 'Move into a socket, or Shift-click the item.',
        'gui.domesurvival.forming_press_v2.modules_idle': 'Без остановки работы' if ru else 'Install while running',
        'gui.domesurvival.forming_press_v2.modules_conflict': 'Эффект: со след. цикла' if ru else 'Effect: next cycle',
        'gui.domesurvival.forming_press_v2.module_slot_tooltip': 'Положите модуль в гнездо мышью или Shift+кликом из инвентаря. Можно менять во время работы: скорость и расход изменятся со следующего цикла, ёмкость — сразу. Разгон и эффективность несовместимы; дубликаты запрещены. Для снятия буфера снизьте заряд до 20 000 FE.' if ru else 'Place a module in a socket, or Shift-click it from your inventory. You can change modules while running: speed and cost apply next cycle, capacity applies immediately. Overdrive and efficiency cannot be combined; no duplicates. To remove the buffer, reduce charge to 20,000 FE.',
        'gui.domesurvival.coal_generator.module_effect': 'Установка: мышь / Shift+клик' if ru else 'Install: drag / Shift-click',
        'gui.domesurvival.coal_generator.module_generation': 'Без остановки работы' if ru else 'Install while running',
        'gui.domesurvival.coal_generator.module_slot_tooltip': 'Подходит один буферный модуль. Положите его в гнездо мышью или Shift+кликом из инвентаря на этой вкладке. Можно установить во время работы: ёмкость сразу вырастет на 75%, выработка не изменится. Для снятия снизьте заряд до 50 000 FE.' if ru else 'Accepts one buffer module. Place it in the socket, or Shift-click it from your inventory on this tab. Install while running: capacity immediately increases by 75%; generation stays unchanged. To remove it, reduce charge to 50,000 FE.'
    }
    path=root/'domesurvival/lang'/f'{lang}.json';raw=path.read_text(encoding='utf-8-sig')
    for key,value in updates.items():
        pattern=r'("'+re.escape(key)+r'"\s*:\s*)"(?:\\.|[^"\\])*"'
        if re.search(pattern,raw):raw=re.sub(pattern,lambda m:m[1]+json.dumps(value,ensure_ascii=False),raw)
        else:raw=raw.rstrip().removesuffix('}').rstrip()+',\n  '+json.dumps(key)+': '+json.dumps(value,ensure_ascii=False)+'\n}\n'
    json.loads(raw);path.write_text(raw,encoding='utf-8')
    for namespace in ('domesurvival','domesurvival_forming'):
        path=root/namespace/'lang'/f'{lang}.json';raw=path.read_text(encoding='utf-8-sig')
        raw=re.sub(r'("block.domesurvival.forming_press"\s*:\s*)"[^"]*"',lambda m:m[1]+json.dumps('Металлообрабатывающий станок' if ru else 'Metalworking Machine',ensure_ascii=False),raw)
        path.write_text(raw,encoding='utf-8')
