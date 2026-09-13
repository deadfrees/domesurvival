"""Editable Blender review of the twelve configurable item-pipe connector appearances."""
from pathlib import Path
import sys,bpy
from mathutils import Vector
sys.path.insert(0,str(Path(__file__).parent))
import item_pipe_material_helpers as h
g=h.g
g.reset_scene();g.camera_setup(1900,1400,66)
g.label('DOMESURVIVAL / CONFIGURABLE PIPE CONNECTORS',-30,23,1.0)
ids=['copper_item_pipe','steel_item_pipe','desh_item_pipe','filtering_item_pipe']
def material(id,mode):
    mat=g.material(id+'_'+mode,(.5,.5,.5));bsdf=mat.node_tree.nodes.get('Principled BSDF');bsdf.inputs['Roughness'].default_value=.72
    tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(h.OUT/'textures'/f'{id}_connector_{mode}.png'));tex.image.pack();tex.interpolation='Closest'
    mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color']);return mat
def uv(obj):
    layer=obj.data.uv_layers.new(name='Connector_UV')
    for poly in obj.data.polygons:
        for loop,coord in zip(poly.loop_indices,[(0,0),(1,0),(1,1),(0,1)]):layer.data[loop].uv=coord
for col,id in enumerate(ids):
    x=(col-1.5)*16
    for row,mode in enumerate(('input','output','disabled')):
        y=12-row*13;offset=g.screen_point(x,y)
        spec={'name':id+'_'+mode,'minimum':[-2.75,-.825,-2.75],'maximum':[2.75,.825,2.75],'faces':['bottom','top','near','far','right','left']}
        frame=g.mesh_box(spec,id+'_'+mode+'_frame',material(id,'frame'),offset=offset);uv(frame)
        spec={'name':id+'_'+mode+'_plate','minimum':[-1.85,-.845,-1.85],'maximum':[1.85,-.845,1.85],'faces':['near']}
        face=g.mesh_box(spec,id+'_'+mode+'_plate',material(id,mode),offset=offset);uv(face)
        g.label(mode.upper(),x-2.8,y-5,.65)
    g.label(['COPPER / T1','STEEL / T2','DESH / T3','FILTER'][col],x-5,18,.65)
g.label('ORIGINAL COLLAR DIMENSIONS / TIER MATERIALS / VISIBLE MODE PLATES',-30,-23,.62,secondary=True)
bpy.context.scene['description']='Exact 5.5 x 5.5 x 1.65 MC-unit collars; original clickable game shape retained.'
g.save_render('02_configurable_connectors',True)
