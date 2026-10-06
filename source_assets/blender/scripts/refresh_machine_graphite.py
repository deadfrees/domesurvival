"""Rebake the shared procedural metal without changing existing model UVs."""
from pathlib import Path
script=Path(__file__).with_name('create_coal_generator_v2.py')
exec(compile(script.read_text().split("base=body();ports=")[0],str(script),'exec'))
base=body();ports={mode:port(mode) for mode in ('input','output')}
# Compare layout before writing the atlas: current exported models are the source
# of truth for geometry and every baked face must keep its original coordinates.
current=read(MODEL/'coal_generator.json')
expected={e['name']:e for e in current['elements']}
size=atlas(base+ports['input']+ports['output'])
for e in base:
    assert e['faces']==expected[e['name']]['faces'],e['name']
save_image('particle',16,16,[c for y in range(16) for x in range(16) for c in material_pixel(x,y,16,16,'panel')])
blockbench(read(MODEL/'coal_generator_inventory.json'),size)
# Refresh the packed atlas in editable generator scenes without rebuilding them.
for path in sorted(OUT.glob('*.blend')):
    bpy.ops.wm.open_mainfile(filepath=str(path))
    for im in bpy.data.images:
        if Path(im.filepath).name=='satin_atlas.png':
            im.filepath=str(TEX/'satin_atlas.png');im.reload();im.pack()
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(path))
print('MACHINE_GRAPHITE_REBAKE_COMPLETE',size,flush=True)
