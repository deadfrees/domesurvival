from pathlib import Path
import json
root=Path(__file__).resolve().parents[2]/'src/main/resources/assets/domesurvival/lang'
ru={'charge':'Заряд: %s%%','charging':'Зарядка','flow_in':'Вход: %s FE/т','flow_out':'Выход: %s FE/т','input':'Вход','input_role':'Приём FE','output':'Выход','output_role':'Передача FE','front':'Лицевая сторона закрыта для подключения.','off':'Подключение отключено.','input_help':'Приём энергии с этой стороны.','output_help':'Выдача энергии через эту сторону.','energy_help':'Запас: %s / %s FE. Ёмкость увеличивается зачарованием «Ёмкость» I–IV.','rate_help':'Общий лимит: приём %s FE/т, выдача %s FE/т. Зарядка предмета использует тот же лимит выдачи.','charging_help':'Зарядка предмета из внутреннего запаса энергии.'}
en={'charge':'Charge: %s%%','charging':'Charge','flow_in':'In: %s FE/t','flow_out':'Out: %s FE/t','input':'Input','input_role':'Receive FE','output':'Output','output_role':'Send FE','front':'The front has no external connection.','off':'Connection disabled.','input_help':'Receive energy through this side.','output_help':'Send energy through this side.','energy_help':'Stored: %s / %s FE. Capacity I–IV enchantments increase storage.','rate_help':'Shared limits: receive %s FE/t, send %s FE/t. Item charging shares the output allowance.','charging_help':'Charge an item from the stored energy.'}
for locale,entries in [('ru_ru',ru),('en_us',en)]:
    p=root/(locale+'.json');data=json.loads(p.read_text(encoding='utf-8'))
    data.update({'gui.domesurvival.steel_buffer_v2.'+k:v for k,v in entries.items()})
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
