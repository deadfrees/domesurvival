"""Validate sieve presentation changes while preserving the retained frame and behavior."""
from pathlib import Path
import copy,json,hashlib,zipfile,sys
R=Path(__file__).resolve().parents[2];O=Path(__file__).resolve().parent
p=R/'src/main/resources/assets/domesurvival/models/block/sand_sieve.json'
old=json.loads((O/'original_model.json').read_text(encoding='utf-8'));new=json.loads(p.read_text(encoding='utf-8'))
def geometry(m):
    m=copy.deepcopy(m);m.pop('textures')
    for element in m['elements']:
        for face in element['faces'].values():face.pop('texture')
    return m
# User explicitly requested removal of the two rear connector cuboids.
assert len(old['elements'])==15 and len(new['elements'])==13
old['elements']=old['elements'][:13]
assert geometry(old)==geometry(new),'Geometry, faces, UV or transforms of retained frame changed'
baseline=json.loads((O/'baseline.json').read_text())
# User subsequently requested a new GUI; its presentation class is intentionally editable.
baseline.pop('src/main/java/com/wasted/domesurvival/forge/machine/sieve/SandSieveScreen.java')
# Subsequent feedback explicitly changes mesh animation, particles and activation sound.
baseline.pop('src/main/java/com/wasted/domesurvival/forge/machine/sieve/SandSieveBlock.java')
baseline.pop('src/main/java/com/wasted/domesurvival/forge/client/render/SandSieveBlockEntityRenderer.java')
for name,digest in baseline.items():assert hashlib.sha256((R/name).read_bytes()).hexdigest()==digest,name
for element in new['elements']:
    for face in element['faces'].values():assert face['texture'][1:] in new['textures']
report={'result':'PASS','unchanged_frame_elements':len(new['elements']),'removed_rear_connector_elements':2,'unchanged_behavior_and_resources':len(baseline),'materials':new['textures']}
if '--release' in sys.argv:
    jar=R/'build/libs/domesurvival-0.2.0.jar'
    with zipfile.ZipFile(jar) as z:
        assert z.read('assets/domesurvival/models/block/sand_sieve.json')==p.read_bytes()
        gui=R/'src/main/resources/assets/domesurvival/textures/gui/sand_sieve_v2/panel.png'
        assert z.read('assets/domesurvival/textures/gui/sand_sieve_v2/panel.png')==gui.read_bytes()
        sound='assets/domesurvival/sounds/machines/sand_sieve_process.ogg'
        assert z.read(sound)==(R/'src/main/resources'/sound).read_bytes()
        assert 'com/wasted/domesurvival/forge/client/particle/SandSieveParticles.class' in z.namelist()
        assert not any('probe/' in n.lower() for n in z.namelist())
        assert 'com/wasted/domesurvival/forge/integration/customnpcs/JosephScriptCommand.class' in z.namelist()
    checks=(O/'runtime/checks.txt').read_text(encoding='utf-8');assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
    for name in ['client.log','build.log']:
        raw=(O/name).read_bytes();s=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8');assert 'BUILD SUCCESSFUL' in s,name
    assert not list((R/'run/sand-sieve-texture-review/production_mod_hold').glob('*.jar'))
    assert len(list((R/'run/mods').glob('*.jar')))==85
    report.update(jar=str(jar),sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),runtime_checks=sum(s.startswith('PASS ') for s in checks.splitlines()))
(O/('release_validation.json' if '--release' in sys.argv else 'validation.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
