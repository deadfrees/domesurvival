from pathlib import Path
import hashlib, json, struct, subprocess, zipfile

ROOT=Path(__file__).resolve().parents[2]
DEV=ROOT/'dev/coal_generator_gui'
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
checks=[]
def check(ok,name):
    checks.append({'pass':bool(ok),'check':name})

expected={'panel':(880,1064),'configuration':(816,400),'modules':(816,400),'widgets':(512,256)}
for name,size in expected.items():
    png=ASSETS/f'textures/gui/coal_generator_v2/{name}.png'
    check(struct.unpack('>II',png.read_bytes()[16:24])==size,name+' texture dimensions')
    check(json.loads(png.with_suffix('.png.mcmeta').read_text())['texture']=={'blur':True,'clamp':True},name+' filtering')

for locale in ('ru_ru','en_us'):
    data=json.loads((ASSETS/f'lang/{locale}.json').read_text(encoding='utf-8-sig'))
    check(all('gui.domesurvival.coal_generator.'+k in data for k in ['routing_title','input_tooltip','output_only_tooltip','side_letter.front']),locale+' labels')
    check(data['block.domesurvival.coal_generator']==('Угольный генератор' if locale=='ru_ru' else 'Coal Generator'),locale+' restored name')

module=json.loads((ASSETS/'models/item/buffer_module.json').read_text())
check(len(module['elements'])==37 and module['parent']=='minecraft:block/block','Buffer module uses 37 real 3D parts')
check(all(all(e['from'][i]<e['to'][i] for i in range(3)) for e in module['elements']),'Module has no zero-thickness elements')
check(all((ASSETS/('textures/'+ref.split(':')[1]+'.png')).is_file() for ref in module['textures'].values()),'All module materials resolve to approved textures')

def read_log(name):
    raw=(DEV/name).read_bytes()
    return raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8',errors='replace')
runtime=read_log('runtime/checks.txt')
check('RESULT PASS failures=0' in runtime and not any(l.startswith('FAIL ') for l in runtime.splitlines()),'Runtime integration tests')
check('BUILD SUCCESSFUL' in read_log('build.log'),'Production build and tests')
check(len(list((ROOT/'run/mods').glob('*.jar')))==85 and not list((ROOT/'run/coal-generator-gui-review/production_mod_hold').glob('*.jar')),'85 development mods restored')

jar=ROOT/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as z:
    for png in (ASSETS/'textures/gui/coal_generator_v2').iterdir():
        check(z.read('assets/domesurvival/'+png.relative_to(ASSETS).as_posix())==png.read_bytes(),png.name+' matches release')
    for locale in ('ru_ru','en_us'):
        check(z.read(f'assets/domesurvival/lang/{locale}.json')==(ASSETS/f'lang/{locale}.json').read_bytes(),locale+' matches release')
    check(not any('coalgeneratorprobe' in n for n in z.namelist()),'No test probe packaged')
    check(z.read('assets/domesurvival/models/item/buffer_module.json')==(ASSETS/'models/item/buffer_module.json').read_bytes(),'3D module model matches release')

allowed={'src/main/java/com/wasted/domesurvival/forge/client/screen/CoalGeneratorScreen.java',
         'src/main/java/com/wasted/domesurvival/forge/machine/coal/CoalGeneratorBlockEntity.java',
         'src/main/java/com/wasted/domesurvival/forge/machine/coal/CoalGeneratorBlock.java',
         'src/main/java/com/wasted/domesurvival/forge/machine/coal/CoalGeneratorMenu.java',
         'src/main/resources/assets/domesurvival/models/item/buffer_module.json',
         'src/main/resources/assets/domesurvival/lang/en_us.json',
         'src/main/resources/assets/domesurvival/lang/ru_ru.json'}
diff=subprocess.check_output(['git','diff','--name-only','b281823','--','src/main'],cwd=ROOT,text=True).splitlines()
check(all(p in allowed or p.startswith('src/main/resources/assets/domesurvival/textures/gui/coal_generator_v2/') for p in diff),'Only generator, its GUI, buffer item model and localization changed')
result={'pass':all(c['pass'] for c in checks),'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
        'bytes':jar.stat().st_size,'runtime_checks':sum(l.startswith('PASS ') for l in runtime.splitlines()),'checks':checks}
(DEV/'release_validation.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='checks'},indent=2))
assert result['pass'],[c for c in checks if not c['pass']]
