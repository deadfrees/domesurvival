from pathlib import Path
import json
R=Path(__file__).resolve().parents[2];O=Path(__file__).resolve().parent;A=R/'src/main/resources'
for name in ['needs_stone_tool','mineable/pickaxe']:
    p=A/f'data/minecraft/tags/blocks/{name}.json';d=json.loads(p.read_text(encoding='utf-8'))
    if 'domesurvival:filter_regeneration_station' not in d['values']:d['values'].append('domesurvival:filter_regeneration_station')
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for loc,text in [('ru_ru','Расход за цикл: −20%. Скорость: −10%. Несовместим с разгоном.'),('en_us','Cycle energy: −20%. Speed: −10%. Incompatible with overdrive.')]:
    p=A/f'assets/domesurvival/lang/{loc}.json';d=json.loads(p.read_text(encoding='utf-8'));d['gui.domesurvival.filter_regeneration_v2.efficiency_help']=text
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for name in ['run_review.ps1','review.init.gradle','build_release.ps1','verify_visual_assets.py']:
    s=(R/'dev/oxygen_filler_v2'/name).read_text(encoding='utf-8')
    s=s.replace('oxygen_filler_v2','filter_regeneration_v2').replace('oxygen-filler-review','filter-regeneration-review').replace('oxygenFiller','filterRegeneration').replace('OxygenFiller','FilterRegeneration').replace("body=model('oxygen_filler')","body=model('filter_regeneration_station')")
    (O/name).write_text(s,encoding='utf-8')
(O/'runtime_probe').mkdir(exist_ok=True)
s=(R/'dev/oxygen_filler_v2/runtime_probe/JeiCapture.java').read_text(encoding='utf-8').replace('fillerprobe','filterprobe').replace('shaft_review_probe','filter_review_probe')
(O/'runtime_probe/JeiCapture.java').write_text(s,encoding='utf-8')
