"""Extend the approved Tier 1 material language without changing world geometry."""
from pathlib import Path
import importlib.util, json, traceback
import bpy

HERE = Path(__file__).resolve().parent
def module(name, file):
    spec=importlib.util.spec_from_file_location(name,HERE/file)
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result)
    return result

p=module('pipe_materials','style_energy_pipe_tier1.py')
g=module('pipe_geometry','create_energy_pipe_greyboxes.py')
base_pixels=p.pixels
base_model=p.style_model
CONFIGS={
    2: ('reinforced', {'body':'59656a','shadow':'303a40','black':'171e24','light':'a0aaa6','steel':'84918e','amber':'c5a252','darkamber':'836d38'}),
    3: ('high_voltage', {'body':'343e42','shadow':'232d31','black':'11191c','light':'89958f','steel':'526360','amber':'d0aa58','darkamber':'866b38'}),
}
tier=2

def pixels(role):
    tile=base_pixels(role)
    c=p.PALETTE
    if role=='panel':
        # The tier badge uses an 8x8 UV island for legible II / III strokes.
        for y in range(16):
            for x in range(16):tile[y][x]=c['black']
        for n in range(8):
            tile[0][n]=tile[n][0]=c['steel']
            tile[7][n]=tile[n][7]=c['shadow']
        for x in ((2,5) if tier==2 else (1,3,5)):
            for y in range(2,6):tile[y][x]=c['amber']
    if role=='rail':
        # Reinforced uses broad steel rails; HV has small amber end collars in pixels.
        for y in (1,11):
            for x in range(1,4):tile[y][x]=c['shadow'] if tier==2 else c['darkamber']
        if tier==3:
            for y in (2,10):
                for x in range(1,4):tile[y][x]=c['amber']
    return tile

def style_model(part):
    result=base_model(part)
    for e in result['elements']:
        for face in e['faces'].values():
            if face['texture']=='#panel':face['uv']=[0,0,8,8]
    return result

def configure(number):
    global tier
    tier=number
    p.PREFIX,p.PALETTE=CONFIGS[number]
    p.ID=p.PREFIX+'_energy_pipe' if number==2 else 'high_voltage_energy_pipe'
    p.BACKUP=p.ROOT/f'source_assets/baseline/energy_pipes/tier{number}'
    p.FILES=[f'models/block/{p.ID}_{part}.json' for part in ('core','arm','inventory')]+[f'models/item/{p.ID}.json',f'blockstates/{p.ID}.json']
    p.BASELINE_NAME='tiers23_baseline_hashes.json'
    p.COMPARISON_PACK='energy_pipe_tiers23_before'
    p.pixels=pixels;p.style_model=style_model

def main():
    models={}
    for number in CONFIGS:
        configure(number)
        models[number]=p.generate_assets()
        bpy.ops.wm.open_mainfile(filepath=str(p.OUT/f'energy_pipe_tier_{number}.blend'),load_ui=False,use_scripts=False)
        for obj in list(bpy.data.objects):
            if obj.type=='MESH' and obj.get('source_part'):p.texture_objects([obj],models[number],g)
            if obj.type=='FONT' and 'CURRENT GAME SHAPE' in obj.data.body:obj.data.body=f'TIER {number} MATERIAL MASTER / ORIGINAL GEOMETRY'
        bpy.context.scene['stage']=f'TIER {number} TEXTURES / original geometry'
        bpy.context.preferences.filepaths.save_version=0
        bpy.ops.wm.save_as_mainfile(filepath=str(p.OUT/f'energy_pipe_tier_{number}.blend'))
    # A single controlled lineup includes approved Tier 1 without rewriting it.
    models[1]={part:json.loads((p.ASSETS/f'models/block/basic_energy_pipe_{part}.json').read_text()) for part in ('core','arm')}
    g.reset_scene();g.camera_setup(1800,1100,70)
    g.label('DOMESURVIVAL / ENERGY PIPE MATERIAL FAMILY',-32,17,1.45)
    for number,x in ((1,-22),(2,0),(3,22)):
        p.PREFIX={1:'basic',2:'reinforced',3:'high_voltage'}[number]
        for y,sides in ((6,{'north','south'}),(-9,{'north','east'})):
            offset=g.screen_point(x,y)
            objects=g.assembly(number,sides,offset,prefix=f'styled_{number}_{y}')
            p.texture_objects(objects,models[number],g,offset=offset)
        g.label(f'TIER {number}',x-5,-1.5,1.1)
    g.label('ORIGINAL GAME SHAPE / STEEL + GRAPHITE + AMBER',-32,-19,.85,secondary=True)
    g.save_render('06_energy_pipe_styled_lineup',True)
    p.dump(p.DEV/'tiers23_style_result.json',{'status':'PASS','runtime_tiers_changed':[2,3],'world_geometry_changed':False,'texture_count':12,'texture_size':[16,16],'body_texels_per_unit':2,'tier_badge_uv':[0,0,8,8]})

if __name__=='__main__':
    try:main()
    except Exception:
        p.dump(p.DEV/'tiers23_style_result.json',{'status':'FAIL','error':traceback.format_exc()})
        raise
