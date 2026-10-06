"""Capture the unchanged sieve geometry/behavior and set up a visual-only review."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parents[2];O=Path(__file__).resolve().parent
model=R/'src/main/resources/assets/domesurvival/models/block/sand_sieve.json'
files=list((R/'src/main/java/com/wasted/domesurvival/forge/machine/sieve').glob('*.java'))
files += [R/'src/main/java/com/wasted/domesurvival/forge/client/render/SandSieveBlockEntityRenderer.java']
files += [R/'src/main/resources/assets/domesurvival'/p for p in ['blockstates/sand_sieve.json','models/item/sand_sieve.json','models/item/fiber_sieve_mesh.json','models/item/copper_sieve_mesh.json','models/item/steel_sieve_mesh.json','textures/item/sieve_mesh.png']]
files += list((R/'src/main/resources/data/domesurvival/recipes').glob('*sieve*.json'))
if not (O/'baseline.json').exists():
    (O/'original_model.json').write_bytes(model.read_bytes())
    (O/'baseline.json').write_text(json.dumps({str(p.relative_to(R)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in files},indent=2)+'\n')
for name in ['run_review.ps1','review.init.gradle','build_release.ps1']:
    s=(R/'dev/filter_regeneration_v2'/name).read_text(encoding='utf-8')
    for a,b in [('filter_regeneration_v2','sand_sieve_textures'),('filter-regeneration-review','sand-sieve-texture-review'),('filterRegeneration','sieveTexture'),('FilterRegeneration','SieveTexture')]:s=s.replace(a,b)
    (O/name).write_text(s,encoding='utf-8')
(O/'runtime_probe').mkdir(exist_ok=True)
# Suppress only review-world first-run overlays; production settings are untouched.
config=R/'run/sand-sieve-texture-review/config/fancymenu/options.txt'
if not config.exists():
    config.parent.mkdir(parents=True,exist_ok=True)
    config.write_bytes((R/'run/filter-regeneration-review/config/fancymenu/options.txt').read_bytes())
