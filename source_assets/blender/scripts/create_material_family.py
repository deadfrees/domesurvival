"""Keep original material designs at 32x32. Recolor vanilla gunpowder for dust.
Solarite is excluded. Blender image API preserves the exact source pixel grid.
"""
from pathlib import Path
import bpy,json,hashlib,shutil,zipfile
ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
SOURCE=ROOT/'source_assets/blender/material_family'
DEV=ROOT/'dev/materials_visual'
METALS=['tin','lead','silver','nickel','goteium','voltarium','steel','neosteel']
PALETTE={'iron':'B6A99C','copper':'B97549','gold':'EDBC44','tin':'B7C7CC','lead':'77788F','silver':'D9E3E8','nickel':'C0B597','goteium':'49B9B5','voltarium':'9582CE'}
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
for folder in (SOURCE,DEV/'baseline'):folder.mkdir(parents=True,exist_ok=True)
manifest={'items':[],'ores':[],'storage_blocks':[],'files':[]}
baseline={}
def backup(path):
    if not path.exists():return
    rel=path.relative_to(ROOT);target=DEV/'baseline'/rel
    if not target.exists():target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,target)
    baseline[rel.as_posix()]=hashlib.sha256(target.read_bytes()).hexdigest()
def record(path):manifest['files'].append(path.relative_to(ROOT).as_posix())
def dump(path,value):
    backup(path);path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8');record(path)
def texture(source,path,metal=None):
    image=bpy.data.images.load(str(source),check_existing=False);image.pack()
    w,h=image.size;src=list(image.pixels[:]);pixels=[]
    tone=[int(PALETTE[metal][i:i+2],16)/255 for i in (0,2,4)] if metal else None
    for y in range(32):
        for x in range(32):
            i=4*((h-1-(31-y)*h//32)*w+x*w//32);c=src[i:i+3]
            if tone:c=[min(1,v*max(c)*1.65) for v in tone]
            pixels.extend((*c,src[i+3]))
    backup(path);path.parent.mkdir(parents=True,exist_ok=True)
    out=bpy.data.images.new(path.stem+'_32',width=32,height=32,alpha=True)
    out.colorspace_settings.name='sRGB';out.pixels.foreach_set(pixels)
    out.filepath_raw=str(path);out.file_format='PNG';out.save();out.pack();record(path)
    dump(path.with_suffix('.png.mcmeta'),{'texture':{'blur':False,'clamp':False}})
def export(name,category,kind,metal):
    modelpath=ASSETS/'models'/category/(name+'.json')
    original_model=SOURCE/'references/models'/category/(name+'.json')
    if not original_model.exists():
        backup(modelpath);original_model.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(DEV/'baseline'/modelpath.relative_to(ROOT),original_model)
    model=json.loads(original_model.read_text())
    original=ASSETS/'textures'/(next(iter(model['textures'].values())).split(':')[1]+'.png')
    if kind=='powder':original=SOURCE/'references/gunpowder.png'
    target=ASSETS/'textures'/category/'materials_v2'/(name+'.png')
    texture(original,target,metal if kind=='powder' else None)
    model['textures']={k:'domesurvival:'+category+'/materials_v2/'+name for k in model['textures']}
    dump(modelpath,model)
    entry={'id':name,'kind':kind,'metal':metal,'size':32,'source':original.relative_to(ROOT).as_posix()}
    if category=='item':manifest['items'].append(entry)
    elif kind=='ore':manifest['ores'].append(dict(entry,texture=name,host='deepslate' if name.startswith('deepslate') else 'stone'))
    else:manifest['storage_blocks'].append(name)
client=Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'
reference=SOURCE/'references/gunpowder.png';reference.parent.mkdir(exist_ok=True)
if not reference.exists():
    with zipfile.ZipFile(client) as archive:reference.write_bytes(archive.read('assets/minecraft/textures/item/gunpowder.png'))
for metal in METALS[:6]:
    for prefix in ('','deepslate_'):export(prefix+metal+'_ore','block','ore',metal)
for metal in METALS:
    export(metal+'_block','block','storage',metal)
    export(metal+'_ingot','item','ingot',metal)
    if metal!='neosteel':export(metal+'_nugget','item','nugget',metal)
for metal in METALS[:6]:export('raw_'+metal,'item','raw',metal)
for metal in ['iron','copper','gold']+METALS[:6]:export('crushed_'+metal+'_ore','item','powder',metal)
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'material_family.blend'))
(DEV/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
(DEV/'baseline_hashes.json').write_text(json.dumps(baseline,indent=2)+'\n')
print('MATERIAL_FAMILY_COMPLETE',len(manifest['items']),len(manifest['ores']),len(manifest['storage_blocks']),flush=True)
