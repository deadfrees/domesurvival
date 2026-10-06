"""Update only connector wording, preserving unrelated localization edits."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[2]
entries={
'ru_ru':{'connector_title':'Коннектор транспортной трубы','connector_v2.rate':'%s предм./цикл · %s тиков','connector_v2.selected':'Режим: %s',
'connector_v2.help.input':'Подаёт предметы из трубы в соседний блок.',
'connector_v2.help.output':'Забирает предметы из соседнего блока в трубу.',
'connector_v2.help.disabled':'Обмен предметами на этой стороне отключён.'},
'en_us':{'connector_title':'Item Pipe Connector','connector_v2.rate':'%s items/cycle · %s ticks','connector_v2.selected':'Mode: %s',
'connector_v2.help.input':'Delivers items from the pipe into the adjacent block.',
'connector_v2.help.output':'Extracts items from the adjacent block into the pipe.',
'connector_v2.help.disabled':'Item transfer on this side is disabled.'}}
for lang,values in entries.items():
    p=root/f'src/main/resources/assets/domesurvival/lang/{lang}.json';s=p.read_text(encoding='utf-8')
    for key,value in values.items():
        key='gui.domesurvival.item_pipe.'+key;encoded=json.dumps(value,ensure_ascii=False)
        pattern=r'("'+re.escape(key)+r'"\s*:\s*)"(?:\\.|[^"\\])*"'
        if re.search(pattern,s):s=re.sub(pattern,lambda m:m.group(1)+encoded,s)
        else:s=s.rstrip()[:-1].rstrip()+',\n  '+json.dumps(key)+': '+encoded+'\n}\n'
    json.loads(s);p.write_text(s,encoding='utf-8')
