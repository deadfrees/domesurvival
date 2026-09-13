from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'dev/pipe_materials_v2';A=ROOT/'src/main/resources/assets/domesurvival'
IDS=['basic_energy_pipe','reinforced_energy_pipe','high_voltage_energy_pipe','basic_fluid_pipe','reinforced_fluid_pipe','high_pressure_fluid_pipe']
baseline=OUT/'baseline_hashes.json'
assert not baseline.exists(),'Baseline already recorded; do not overwrite'
baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'src/main').rglob('*') if p.is_file()},indent=2)+'\n')
for id in IDS:
 for rel in [f'blockstates/{id}.json',f'models/item/{id}.json']+[f'models/block/{id}_{part}.json' for part in ('core','arm','inventory')]:
  target=OUT/'baseline'/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(A/rel,target)
for kind in ('energy_pipe','fluid_pipe'):
 shutil.copytree(A/'textures/block'/kind,OUT/'baseline/textures/block'/kind)
print('Baseline recorded for six unchanged pipe frames and all production files')
