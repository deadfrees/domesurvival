"""Blender CLI smoke test only. Never modifies Minecraft runtime resources.

blender --background --factory-startup --python-exit-code 1 --python <this file>
"""
from pathlib import Path
import json
import bpy

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "source_assets" / "blender" / "energy_pipes" / "pipeline_test"
OUT.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene
scene.unit_settings.system = "METRIC"
scene.unit_settings.scale_length = 1.0 / 16.0
scene["minecraft_units_per_block"] = 16
scene["purpose"] = "Pipeline verification; not a production energy pipe"

bpy.ops.mesh.primitive_cube_add(size=16, location=(0, 0, 0))
reference = bpy.context.object
reference.name = "reference_block_16_units"
reference.display_type = "WIRE"
reference.hide_render = True

bpy.ops.mesh.primitive_cylinder_add(vertices=8, radius=2, depth=16, end_fill_type="NGON")
segment = bpy.context.object
segment.name = "test_pipe_segment"
bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
for polygon in segment.data.polygons:
    polygon.use_smooth = False
assert all(abs(v - 1) < 1e-6 for v in segment.scale)
assert all(abs(v) < 1e-6 for v in segment.rotation_euler)
assert abs(segment.dimensions.z - 16) < 1e-6
assert tuple(reference.dimensions) == (16.0, 16.0, 16.0)
assert all(p.normal.dot(p.center) > 0 for p in segment.data.polygons), "Inverted normals"
bpy.ops.object.select_all(action="DESELECT")
segment.select_set(True)
bpy.context.view_layer.objects.active = segment
blend_path = OUT / "test_energy_pipe_pipeline.blend"
obj_path = OUT / "test_energy_pipe_pipeline.obj"
bpy.ops.wm.save_as_mainfile(filepath=str(blend_path))
bpy.ops.wm.obj_export(filepath=str(obj_path), export_selected_objects=True, export_materials=False)
assert blend_path.stat().st_size > 0
assert obj_path.stat().st_size > 0
report = {"status": "PASS", "blender_version": bpy.app.version_string,
          "units_per_block": 16, "reference_dimensions": list(reference.dimensions),
          "segment_dimensions": list(segment.dimensions),
          "vertices": len(segment.data.vertices), "faces": len(segment.data.polygons),
          "blend": str(blend_path), "intermediate_obj": str(obj_path),
          "runtime_resources_modified": False}
(OUT / "pipeline_result.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
print("ENERGY_PIPE_BLENDER_PIPELINE_PASS " + json.dumps(report))
