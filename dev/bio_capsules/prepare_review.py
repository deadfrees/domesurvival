from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
for name in ('run_review.ps1','build_release.ps1','review.init.gradle'):
    s=(root/'dev/bioincubator_v2'/name).read_text().replace('bioincubator_v2','bio_capsules').replace('bioincubator-review','bio-capsules-review').replace('bioincubatorProbe','bioCapsulesProbe').replace('bioincubatorReview','bioCapsulesReview').replace('BioincubatorReview','BioCapsulesReview')
    if not (out/name).exists():
        (out/name).write_text(s)
config=root/'run/bio-capsules-review/config/fancymenu'
if not config.exists():
    config.parent.mkdir(parents=True,exist_ok=True)
    shutil.copytree(root/'run/bioincubator-review/config/fancymenu',config)
