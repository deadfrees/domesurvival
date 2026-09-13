"""Record independently restored energy resources; never alter production files."""
from pathlib import Path
import hashlib, json, subprocess
root=Path(__file__).resolve().parents[2]
base=json.loads((root/'dev/fluid_pipes/baseline_hashes.json').read_text())
files={}
for p in (root/'src/main').rglob('*'):
    if not p.is_file():continue
    rel=p.relative_to(root).as_posix();digest=hashlib.sha256(p.read_bytes()).hexdigest()
    if digest==base.get(rel) or 'energy_pipe' not in rel:continue
    historic=subprocess.run(['git','show','a1ce5be:'+rel],cwd=root,capture_output=True,check=True).stdout
    equivalent=json.loads(historic.decode('utf-8-sig'))==json.loads(p.read_text(encoding='utf-8-sig')) if p.suffix=='.json' else hashlib.sha256(historic).hexdigest()==digest
    assert equivalent,rel+' differs from approved energy family'
    files[rel]=digest
assert len(files)==33
target=root/'dev/fluid_pipes/external_energy_snapshot.json'
assert not target.exists(),'Do not recapture a scope baseline silently'
target.write_text(json.dumps({'note':'Energy resources changed independently after initial fluid baseline (observed 2026-09-13 18:38). All 33 match approved commit a1ce5be: JSON structurally, PNG byte-for-byte. Fluid scripts never write these paths. Pin current byte hashes for subsequent scope checks.','historical_commit':'a1ce5be','files':files},indent=2)+'\n')
print('Pinned 33 independently restored energy resources; JSON/PNG match a1ce5be.')
