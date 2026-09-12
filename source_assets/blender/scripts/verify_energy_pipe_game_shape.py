"""Read saved .blend masters and compare their source meshes to production JSON."""
from pathlib import Path
import hashlib
import itertools
import json
import traceback
import bpy

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "dev/energy_pipes/game_shape_verification.json"
IDS = ("basic_energy_pipe", "reinforced_energy_pipe", "high_voltage_energy_pipe")
NORMALS = {"east": (1,0,0), "west": (-1,0,0), "up": (0,1,0),
           "down": (0,-1,0), "south": (0,0,1), "north": (0,0,-1)}


def main():
    results = []
    for tier, pipe_id in enumerate(IDS, 1):
        master = ROOT / f"source_assets/blender/energy_pipes/energy_pipe_tier_{tier}.blend"
        bpy.ops.wm.open_mainfile(filepath=str(master), load_ui=False, use_scripts=False)
        collection = bpy.data.collections["SOURCE_MODULES_current_game_core_arm"]
        assert len(collection.objects) == 12
        for part in ("core", "arm"):
            source = ROOT / f"src/main/resources/assets/domesurvival/models/block/{pipe_id}_{part}.json"
            elements = json.loads(source.read_text(encoding="utf-8"))["elements"]
            objects = {o["source_part"]: o for o in collection.objects if o["module"] == part}
            assert set(objects) == {e["name"] for e in elements}
            for element in elements:
                obj = objects[element["name"]]
                assert all(abs(v-1) < 1e-6 for v in obj.scale)
                assert all(abs(v) < 1e-6 for v in obj.rotation_euler)
                expected = {tuple(round(v,5) for v in vertex)
                            for vertex in itertools.product(*zip(element["from"], element["to"]))}
                actual = set()
                for vertex in obj.data.vertices:
                    v = obj.matrix_world @ vertex.co
                    actual.add(tuple(round(n,5) for n in (v.x+8, v.z+8, 8-v.y)))
                assert actual == expected, f"Vertex mismatch: {pipe_id}/{element['name']}"
                normals = {(round(p.normal.x), round(p.normal.z), round(-p.normal.y))
                           for p in obj.data.polygons}
                assert normals == {NORMALS[f] for f in element["faces"]}, f"Face mismatch: {pipe_id}/{element['name']}"
                assert len(obj.data.polygons) == len(element["faces"])
            results.append({"tier": tier, "part": part, "elements": len(elements),
                            "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
                            "vertices_and_face_normals_match": True})
    OUT.write_text(json.dumps({"status": "PASS", "masters_checked": 3,
                               "elements_checked": 36, "results": results}, indent=2), encoding="utf-8")


try:
    main()
except Exception:
    OUT.write_text(json.dumps({"status": "FAIL", "error": traceback.format_exc()}, indent=2), encoding="utf-8")
    raise
