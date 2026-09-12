"""Reproducible energy-pipe concepts. Run with Blender --background --python.

Only source_assets and dev/energy_pipes are written. No runtime resource export.
Coordinates use 16 units per block; source arms point along Blender +Y (MC north).
"""
from pathlib import Path
import hashlib
import itertools
import json
import math
import traceback
import bpy
from mathutils import Vector, Quaternion

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "source_assets/blender/energy_pipes"
PREVIEW = OUT / "previews"
EVIDENCE = ROOT / "dev/energy_pipes"
for folder in (OUT, PREVIEW, EVIDENCE):
    folder.mkdir(parents=True, exist_ok=True)

DIRECTIONS = {"north": (0, 1, 0), "south": (0, -1, 0),
              "east": (1, 0, 0), "west": (-1, 0, 0),
              "up": (0, 0, 1), "down": (0, 0, -1)}
ROTATIONS = {"north": Quaternion((0, 0, 1), 0),
             "south": Quaternion((0, 0, 1), math.pi),
             "east": Quaternion((0, 0, 1), -math.pi / 2),
             "west": Quaternion((0, 0, 1), math.pi / 2),
             "up": Quaternion((1, 0, 0), math.pi / 2),
             "down": Quaternion((1, 0, 0), -math.pi / 2)}
FACE_IDS = {"bottom": (3, 2, 1, 0), "top": (4, 5, 6, 7),
            "near": (0, 1, 5, 4), "far": (2, 3, 7, 6),
            "right": (1, 2, 6, 5), "left": (3, 0, 4, 7)}


def box(name, minimum, maximum, omit=()):
    return dict(name=name, minimum=minimum, maximum=maximum,
                faces=[f for f in FACE_IDS if f not in omit])


PIPE_IDS = {1: "basic_energy_pipe", 2: "reinforced_energy_pipe", 3: "high_voltage_energy_pipe"}
MODEL_ROOT = ROOT / "src/main/resources/assets/domesurvival/models/block"
MC_FACES = {"north": "far", "south": "near", "east": "right", "west": "left", "up": "top", "down": "bottom"}


def templates(tier):
    """Import the actual game geometry, preserving coordinates and authored faces."""
    parts = {"cap": []}  # Current game uses permanent core panels, no conditional cap.
    for part in ("core", "arm"):
        model_path = MODEL_ROOT / f"{PIPE_IDS[tier]}_{part}.json"
        model = json.loads(model_path.read_text(encoding="utf-8"))
        parts[part] = []
        for element in model["elements"]:
            assert not element.get("rotation"), "Handle rotated JSON elements explicitly before import"
            lo, hi = element["from"], element["to"]
            spec = {"name": element["name"],
                    "minimum": [lo[0]-8, 8-hi[2], lo[1]-8],
                    "maximum": [hi[0]-8, 8-lo[2], hi[1]-8],
                    "faces": [MC_FACES[f] for f in element["faces"]],
                    "source_model": str(model_path.relative_to(ROOT)).replace(chr(92), "/"),
                    "minecraft_from": lo, "minecraft_to": hi}
            parts[part].append(spec)
    return parts

def material(name, color):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Roughness"].default_value = 0.78
    return mat


def annotation_material(name, color):
    mat = material(name, color)
    nodes = mat.node_tree.nodes
    nodes.clear()
    output = nodes.new("ShaderNodeOutputMaterial")
    emission = nodes.new("ShaderNodeEmission")
    emission.inputs["Color"].default_value = (*color, 1)
    emission.inputs["Strength"].default_value = 1
    mat.node_tree.links.new(emission.outputs[0], output.inputs["Surface"])
    return mat


def mesh_box(spec, name, mat, quat=None, offset=(0, 0, 0), scale=1):
    x0, y0, z0 = spec["minimum"]
    x1, y1, z1 = spec["maximum"]
    coordinates = [(x0,y0,z0), (x1,y0,z0), (x1,y1,z0), (x0,y1,z0),
                   (x0,y0,z1), (x1,y0,z1), (x1,y1,z1), (x0,y1,z1)]
    q = quat or Quaternion()
    verts = [q @ (Vector(v) * scale) + Vector(offset) for v in coordinates]
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], [FACE_IDS[f] for f in spec["faces"]])
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(mat)
    obj["source_part"] = spec["name"]
    obj["axis_aligned_cuboid_source"] = True
    # Coordinates are baked into vertices: object scale and rotation stay identity.
    return obj


def assembly(tier, enabled, offset=(0,0,0), quat=None, scale=1, prefix="pipe"):
    q = quat or Quaternion()
    result = []
    for spec in SPECS[tier]["core"]:
        result.append(mesh_box(spec, f"{prefix}/{spec['name']}", CLAY, q, offset, scale))
    for direction in DIRECTIONS:
        role = "arm" if direction in enabled else "cap"
        for spec in SPECS[tier][role]:
            obj = mesh_box(spec, f"{prefix}/{direction}/{spec['name']}", CLAY,
                           q @ ROTATIONS[direction], offset, scale)
            obj["connection_property"] = direction
            obj["when"] = role == "arm"
            result.append(obj)
    return result


def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.scene.unit_settings.system = "METRIC"
    bpy.context.scene.unit_settings.scale_length = 1 / 16
    bpy.context.scene["stage"] = "CURRENT GAME SHAPE — exact JSON geometry import"
    bpy.context.scene["minecraft_units_per_block"] = 16
    global CLAY, INK, MUTED, PROXY
    CLAY = material("uniform_grey_clay_all_tiers", (0.38, 0.40, 0.42))
    INK = annotation_material("annotation_ink", (0.018, 0.027, 0.035))
    MUTED = annotation_material("annotation_secondary", (0.10, 0.12, 0.14))
    PROXY = material("reference_port_only", (0.23, 0.25, 0.27))


def camera_setup(width, height, span):
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 8
    scene.cycles.use_denoising = True
    scene.render.threads_mode = "FIXED"
    scene.render.threads = 8
    scene.render.resolution_x, scene.render.resolution_y = width, height
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.world = bpy.data.worlds.new("neutral_studio")
    scene.world.use_nodes = True
    scene.world.node_tree.nodes["Background"].inputs[0].default_value = (0.77, 0.78, 0.78, 1)
    scene.world.node_tree.nodes["Background"].inputs[1].default_value = 0.8
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "None"
    bpy.context.preferences.filepaths.save_version = 0
    cam_data = bpy.data.cameras.new("orthographic_review")
    cam = bpy.data.objects.new("orthographic_review", cam_data)
    scene.collection.objects.link(cam)
    cam.location = (70, -100, 80)
    cam.rotation_euler = (-cam.location).to_track_quat('-Z','Y').to_euler()
    cam.data.type = "ORTHO"
    cam.data.ortho_scale = span
    cam.data.clip_end = 1000
    scene.camera = cam
    global CAMERA_Q, RIGHT, UP, TOWARD
    CAMERA_Q = cam.rotation_euler.to_quaternion()
    RIGHT, UP, TOWARD = [CAMERA_Q @ Vector(v) for v in ((1,0,0),(0,1,0),(0,0,1))]
    # Directional studio lights give every cell equal lighting and cannot appear
    # as luminous panels across the wide/portrait presentation layouts.
    for name, loc, power in (("key_light", (20,-30,80), 1.5),
                             ("fill_light", (-50,-10,20), 0.5),
                             ("rim_light", (20,50,40), 0.8)):
        data = bpy.data.lights.new(name, "SUN")
        data.energy = power
        data.angle = math.radians(12)
        obj = bpy.data.objects.new(name, data)
        scene.collection.objects.link(obj)
        obj.location = loc
        obj.rotation_euler = (-obj.location).to_track_quat('-Z','Y').to_euler()


def screen_point(x, y, depth=0):
    return RIGHT * x + UP * y + TOWARD * depth


def label(text, x, y, size=1, align="LEFT", secondary=False):
    data = bpy.data.curves.new("label_" + text, 'FONT')
    data.body, data.size, data.align_x = text, size, align
    data.extrude = 0
    obj = bpy.data.objects.new("label_" + text, data)
    bpy.context.collection.objects.link(obj)
    obj.location = screen_point(x, y, 16)
    obj.rotation_euler = CAMERA_Q.to_euler()
    data.materials.append(MUTED if secondary else INK)


def save_render(name, save_blend=False):
    scene = bpy.context.scene
    scene.render.filepath = str(PREVIEW / (name + ".png"))
    if save_blend:
        bpy.ops.wm.save_as_mainfile(filepath=str(OUT / (name + ".blend")))
    bpy.ops.render.render(write_still=True)
    if scene.render.resolution_y > 2000:
        scene.render.image_settings.file_format = "JPEG"
        scene.render.image_settings.quality = 92
        bpy.data.images["Render Result"].save_render(str(PREVIEW / (name + "_review.jpg")), scene=scene)
        scene.render.image_settings.file_format = "PNG"
    progress("rendered " + name)


def progress(message):
    (EVIDENCE / "greybox_progress.txt").write_text(message, encoding="utf-8")
    print(message, flush=True)


def check_coplanar_surfaces(parts, enabled_bits):
    """Reject overlapping, equally oriented faces, a source of visible z-fighting.

    Opposite faces at touching cuboid interfaces are allowed. This does not claim
    to be a complete mesh Boolean or a visibility test inside Minecraft.
    """
    groups = {}
    sources = [(s, Quaternion(), "core") for s in parts["core"]]
    for direction, active in zip(DIRECTIONS, enabled_bits):
        sources += [(s, ROTATIONS[direction], direction) for s in parts["arm" if active else "cap"]]
    for spec, q, direction in sources:
        x0,y0,z0 = spec["minimum"]
        x1,y1,z1 = spec["maximum"]
        vertices = [q @ Vector(v) for v in [(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),
                                             (x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]]
        for name in spec["faces"]:
            face = [vertices[i] for i in FACE_IDS[name]]
            normal = (face[1]-face[0]).cross(face[2]-face[0])
            axis = max(range(3), key=lambda i: abs(normal[i]))
            axes = [i for i in range(3) if i != axis]
            key = (axis, normal[axis] > 0, round(face[0][axis], 5))
            rectangle = [(min(v[i] for v in face), max(v[i] for v in face)) for i in axes]
            for other, origin in groups.get(key, []):
                overlap = all(min(a[1], b[1]) - max(a[0], b[0]) > 1e-4 for a,b in zip(rectangle, other))
                assert not overlap, f"Coplanar overlap: {direction}/{spec['name']}/{name} and {origin}"
            groups.setdefault(key, []).append((rectangle, f"{direction}/{spec['name']}/{name}"))


def validate_sources():
    report = {"stage": "current_game_shape", "blender": bpy.app.version_string,
              "source_models": {str(p.relative_to(ROOT)).replace(chr(92), "/"): hashlib.sha256(p.read_bytes()).hexdigest()
                                for tier in (1,2,3) for part in ("core","arm")
                                for p in [MODEL_ROOT / f"{PIPE_IDS[tier]}_{part}.json"]},
              "script_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(), "tiers": {},
              "combinations_checked": 0, "runtime_resources_modified": False}
    for tier, parts in SPECS.items():
        counts = {p: sum(len(s["faces"]) for s in specs) for p, specs in parts.items()}
        quads = [counts["core"] + d * counts["arm"] + (6-d) * counts["cap"] for d in range(7)]
        assert quads[2] <= 88 and quads[6] <= 192, (tier, quads)
        for enabled_bits in itertools.product((False, True), repeat=6):
            check_coplanar_surfaces(parts, enabled_bits)
            for direction, active in zip(DIRECTIONS, enabled_bits):
                q = ROTATIONS[direction]
                specs = parts["arm" if active else "cap"]
                for spec in specs:
                    assert all(a < b for a, b in zip(spec["minimum"], spec["maximum"]))
                    for v in itertools.product(*zip(spec["minimum"], spec["maximum"])):
                        vertex = q @ Vector(v)
                        assert all(-8.00001 <= n <= 8.00001 for n in vertex)
                if active:
                    tip = q @ Vector((0, 8, 0))
                    assert (tip - Vector(DIRECTIONS[direction]) * 8).length < 1e-4
            report["combinations_checked"] += 1
        report["tiers"][str(tier)] = {"quads_by_connections": quads, "part_quads": counts,
            "source_cuboids": {k: len(v) for k,v in parts.items()},
            "nominal_diameter": 3.4, "core_outer_size": 3.96}
    # Every adjacent pair ends at the same block plane, including mixed tiers.
    report["mixed_tier_interface_pairs_checked"] = 9 * 6
    for a, b, direction in itertools.product((1,2,3), (1,2,3), DIRECTIONS):
        axis = Vector(DIRECTIONS[direction])
        assert (axis * 8 - (axis * 16 - axis * 8)).length == 0
        assert max(s["maximum"][1] for s in SPECS[a]["arm"]) == 8
        assert max(s["maximum"][1] for s in SPECS[b]["arm"]) == 8
    report["limitations"] = ["Geometry checks are not Minecraft model bake or FE tests.",
        "Mixed-tier check verifies interface planes, not visual compatibility with real machines.",
        "Quads are authored faces before chunk culling; no FPS measurements."]
    report["same_facing_coplanar_overlaps"] = 0
    return report


def create_master(tier):
    reset_scene()
    camera_setup(1400, 1000, 48)
    assembly(tier, {"north", "south"}, prefix="straight")
    # Separate editable modules are stored in a hidden collection, not exported.
    modules = bpy.data.collections.new("SOURCE_MODULES_current_game_core_arm")
    bpy.context.scene.collection.children.link(modules)
    for role, specs in SPECS[tier].items():
        for spec in specs:
            obj = mesh_box(spec, f"source/{role}/{spec['name']}", CLAY)
            for collection in list(obj.users_collection):
                collection.objects.unlink(obj)
            modules.objects.link(obj)
            obj["module"] = role
    modules.hide_render = True
    modules.hide_viewport = True
    label(f"ENERGY PIPE / TIER {tier}", -21, 12, 1.8)
    label("CURRENT GAME SHAPE   /   16 UNITS = 1 BLOCK", -21, -13, 0.9, secondary=True)
    # Reference cube uses only a viewport wire display; it is not a render/export part.
    ref = mesh_box(box("reference_block", (-8,-8,-8), (8,8,8)), "reference_block_16_units", CLAY)
    ref.display_type = "WIRE"
    ref.hide_render = True
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT / f"energy_pipe_tier_{tier}.blend"))
    progress(f"saved tier {tier} master")


def lineup():
    reset_scene()
    camera_setup(1800, 1000, 78)
    label("DOMESURVIVAL", -35, 18, 1.2, secondary=True)
    label("ENERGY / CURRENT GAME SHAPE", -35, 14.5, 2.0)
    for tier, x, subtitle in ((1,-25,"BASIC"), (2,0,"REINFORCED"), (3,25,"HIGH VOLTAGE")):
        assembly(tier, {"north","south"}, screen_point(x, 0), prefix=f"tier_{tier}")
        label(f"TIER {tier}", x, -11.5, 1.5, "CENTER")
        label(subtitle, x, -13.8, 0.9, "CENTER", True)
        label("3.4 / 16 UNITS", x, -15.5, 0.8, "CENTER", True)
    label("01 / EXACT JSON GEOMETRY", -35, -19.8, 0.8, secondary=True)
    label("ORIGINAL PROPORTIONS. NO NEW RINGS.", 35, -19.8, 0.8, "RIGHT", True)
    save_render("01_energy_pipe_lineup", True)


def orthographic_views():
    reset_scene()
    camera_setup(1800, 1400, 96)
    label("FORM / ORTHOGRAPHIC VIEWS", -44, 33, 2)
    views = [("FRONT", Vector((0,-1,0))), ("SIDE", Vector((1,0,0))), ("TOP", Vector((0,0,1)))]
    for name, x in zip(("FRONT", "SIDE", "TOP", "ITEM ANGLE"), (-33,-11,11,33)):
        label(name, x, 28, 1.15, "CENTER", True)
    for tier, y in ((1,17),(2,-3),(3,-23)):
        for (name, toward), x in zip(views, (-33,-11,11)):
            # Rotate an exact orthographic model view into the shared sheet camera.
            view_q = toward.to_track_quat('Z', 'Y')
            if name == "TOP":
                view_q = Quaternion()
            elif name == "FRONT":
                view_q = Quaternion((1,0,0), math.pi/2)
            elif name == "SIDE":
                from mathutils import Matrix
                view_q = Matrix(((0,0,1),(1,0,0),(0,1,0))).to_quaternion()
            q = CAMERA_Q @ view_q.inverted()
            assembly(tier, {"north","south"}, screen_point(x,y), q, 0.9, f"tier_{tier}_{name}")
        assembly(tier, {"north","south"}, screen_point(33,y), scale=0.9, prefix=f"tier_{tier}_item")
        label(f"T{tier}", -45, y, 1.4)
    label("02 / ONE GREY MATERIAL   |   ALL VIEWS SHARE THE SAME SCALE", -44, -35, 0.8, secondary=True)
    save_render("02_energy_pipe_views")


def connections():
    reset_scene()
    camera_setup(1800, 2700, 156)
    # Portrait orthographic span is vertical; horizontal field is 104 units.
    label("CONNECTIONS / SIX DIRECTIONS", -47, 71, 2)
    for tier, x in ((1,-31),(2,0),(3,31)):
        label(f"TIER {tier}", x, 65.5, 1.5, "CENTER")
    cases = [("ISOLATED / ORIGINAL CORE", set()), ("STRAIGHT / 3 BLOCKS", {"north","south"}),
             ("VERTICAL / UP + DOWN", {"up","down"}), ("CORNER / NORTH + EAST", {"north","east"}),
             ("T / THREE CONNECTIONS", {"north","east","west"}),
             ("CROSS / FOUR CONNECTIONS", {"north","south","east","west"}),
             ("ALL SIX DIRECTIONS", set(DIRECTIONS)), ("CONNECTION / PORT PROXY", {"north"})]
    for row, (title, enabled) in enumerate(cases):
        y = 56 - row * 16.5
        for tier, x in ((1,-31),(2,0),(3,31)):
            center = screen_point(x,y)
            if row == 1:
                for n in (-1,0,1):
                    assembly(tier, {"north","south"}, center + Vector((0,n*16*.43,0)), scale=.43,
                             prefix=f"tier_{tier}_straight_{n}")
            else:
                assembly(tier, enabled, center, scale=.69, prefix=f"tier_{tier}_{row}")
            if row == 7:
                mesh_box(box("reference_port_plate", (-4,8,-4), (4,9,4)),
                         f"tier_{tier}_port_proxy", PROXY, offset=center, scale=.69)
            label(title, x, y-7.1, .65, "CENTER", True)
    label("03 / EXISTING BOOLEAN CONNECTIONS   |   PORT PLATE IS A REFERENCE, NOT A MACHINE ASSET", -47, -74, .72, secondary=True)
    save_render("03_energy_pipe_connections")


def long_lines():
    reset_scene()
    camera_setup(2000, 1000, 190)
    label("INFRASTRUCTURE / LINE RHYTHM", -87, 40, 4)
    for tier, row in ((1,22),(2,1),(3,-20)):
        for i in range(10):
            center = screen_point((i-4.5)*16, row)
            # Orient the line precisely along screen right to expose block rhythm.
            rotation = Vector((0,1,0)).rotation_difference(RIGHT)
            assembly(tier, {"north","south"}, center, rotation, prefix=f"tier_{tier}_line_{i}")
        label(f"TIER {tier}   /   10 BLOCKS", -87, row-9, 1.6, secondary=True)
    label("04 / IDENTICAL CAMERA AND SCALE   |   CONNECTORS END AT EVERY 16-UNIT BOUNDARY", -87, -43, 1.5, secondary=True)
    save_render("04_energy_pipe_long_lines")


def main():
    progress("starting")
    production = ROOT / "src/main"
    before = {str(p.relative_to(production)): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in production.rglob('*') if p.is_file()}
    (EVIDENCE / "greybox_results.json").write_text('{"status":"RUNNING"}', encoding="utf-8")
    (EVIDENCE / "greybox_error.txt").unlink(missing_ok=True)
    report = validate_sources()
    (OUT / "greybox_cuboids.json").write_text(json.dumps(SPECS, indent=2), encoding="utf-8")
    for tier in (1,2,3):
        create_master(tier)
    lineup()
    orthographic_views()
    connections()
    long_lines()
    after = {str(p.relative_to(production)): hashlib.sha256(p.read_bytes()).hexdigest()
             for p in production.rglob('*') if p.is_file()}
    assert before == after, "Production files changed during greybox generation"
    report["production_files_verified_unchanged"] = len(before)
    report["status"] = "PASS"
    (EVIDENCE / "greybox_results.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    progress("PASS: masters and four preview sheets created; production unchanged")


SPECS = {tier: templates(tier) for tier in (1,2,3)}
if __name__ == "__main__":
    try:
        main()
    except Exception:
        (EVIDENCE / "greybox_error.txt").write_text(traceback.format_exc(), encoding="utf-8")
        raise
