"""Validate the tested organic processor resources and the production JAR."""
from pathlib import Path
import hashlib,json,zipfile,math,struct
root=Path(__file__).resolve().parents[2];out=Path(__file__).resolve().parent
resources=root/'src/main/resources';assets=resources/'assets/domesurvival'
checks=(out/'runtime/checks.txt').read_text(encoding='utf-8')
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ['client.log','build.log']:
 raw=(out/name).read_bytes();log=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8');assert 'BUILD SUCCESSFUL' in log,name
baseline=json.loads((out/'baseline.json').read_text())
recipes=[root/p for p in baseline if '/recipes/' in p]
for p in recipes:assert hashlib.sha256(p.read_bytes()).hexdigest()==baseline[p.relative_to(root).as_posix()],p
body=json.loads((assets/'models/block/organic_processor.json').read_text())
rotor=json.loads((assets/'models/block/organic_processor_rotor.json').read_text())
inventory=json.loads((assets/'models/block/organic_processor_inventory.json').read_text())
assert len(inventory['elements'])==len(body['elements'])+len(rotor['elements'])
for model in (body,rotor,inventory):
 for cube in model['elements']:
  for lo,hi in zip(cube['from'],cube['to']):assert 0<=lo<hi<=16
  for face in cube['faces'].values():assert all(0<=v<=16 for v in face['uv'])
 for texture in model['textures'].values():
  if not texture.startswith('#'):
   namespace,path=texture.split(':');assert (resources/f'assets/{namespace}/textures/{path}.png').exists()
# Sample the entire rotation: blades must stay inside the vat's four walls.
for cube in rotor['elements']:
 for x in (cube['from'][0],cube['to'][0]):
  for z in (cube['from'][2],cube['to'][2]):
   for degrees in range(360):
    a=math.radians(degrees);rx=8+(x-8)*math.cos(a)-(z-5)*math.sin(a);rz=5+(x-8)*math.sin(a)+(z-5)*math.cos(a)
    assert 3.6<rx<12.4 and 1.8<rz<8.1,(cube['name'],degrees,rx,rz)
for name,dim in [('panel',(880,1064)),('configuration',(816,444)),('modules',(816,444)),('jei',(720,512))]:
 assert struct.unpack('>II',(assets/f'textures/gui/organic_processor_v2/{name}.png').read_bytes()[16:24])==dim
for locale in ('ru_ru','en_us'):
 data=json.loads((assets/f'lang/{locale}.json').read_text(encoding='utf-8'))
 assert not any('?' in v for k,v in data.items() if k.startswith('gui.domesurvival.organic_processor_v2.'))
files=recipes+[resources/'data/minecraft/tags/blocks/needs_stone_tool.json',resources/'data/minecraft/tags/blocks/mineable/pickaxe.json']
files+=list((assets/'textures/gui/organic_processor_v2').iterdir())+list((assets/'models/block').glob('organic_processor*.json'))
files+=list((assets/'models/block').glob('coal_generator_*_port_*.json'))
files+=[assets/'models/item/organic_processor.json',assets/'blockstates/organic_processor.json',assets/'textures/block/coal_generator_v2/satin_atlas.png',assets/'lang/ru_ru.json',assets/'lang/en_us.json']
jar=root/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as z:
 names=z.namelist();assert not any('probe/' in n.lower() for n in names)
 assert not any(n.endswith('/compat/jei/OrganicProcessorJeiCategory.class') for n in names)
 for p in files:assert z.read(p.relative_to(resources).as_posix())==p.read_bytes(),p
 for name in ['machine/organic/OrganicProcessorBlockEntity','machine/organic/OrganicProcessorMenu','machine/organic/OrganicProcessorScreen','client/render/OrganicProcessorRenderer','client/render/OrganicProcessorPreview','client/jei/OrganicProcessorRecipeCategory','integration/customnpcs/JosephScriptCommand']:
  assert f'com/wasted/domesurvival/forge/{name}.class' in names,name
assert len(list((root/'run/mods').glob('*.jar')))==85
assert not list((root/'run/organic-processor-review/production_mod_hold').glob('*.jar'))
report={'result':'PASS','runtime_assertions':sum(x.startswith('PASS ') for x in checks.splitlines()),'packaged_resources':len(files),'unchanged_recipes':len(recipes),'body_cuboids':len(body['elements']),'moving_cuboids':len(rotor['elements']),'rotor_clearance_angles':360,'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'bytes':jar.stat().st_size}
(out/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
