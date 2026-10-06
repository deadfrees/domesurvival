from pathlib import Path
import shutil,json,hashlib
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
files=list((root/'src/main/java/com/wasted/domesurvival/forge/machine/bio').glob('*.java'))
files+=list((root/'src/main/resources/assets/domesurvival/models/block').glob('bioincubator*.json'))
files+=[root/'src/main/resources/assets/domesurvival/blockstates/bioincubator.json',root/'src/main/resources/assets/domesurvival/models/item/bioincubator.json']
if not (out/'baseline.json').exists():
 data={}
 for p in files:
  rel=p.relative_to(root);dest=out/'baseline'/rel;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest);data[rel.as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
 (out/'baseline.json').write_text(json.dumps(data,indent=2))
for name in ('run_review.ps1','review.init.gradle','build_release.ps1'):
 s=(root/'dev/organic_processor_v2'/name).read_text().replace('organic_processor_v2','bioincubator_v2').replace('organic-processor-review','bioincubator-review').replace('organicProcessor','bioincubator').replace('OrganicProcessor','Bioincubator')
 (out/name).write_text(s)
