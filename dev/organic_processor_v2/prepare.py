from pathlib import Path
import hashlib,json,shutil
root=Path(__file__).resolve().parents[2];out=Path(__file__).resolve().parent
paths=list((root/'src/main/java/com/wasted/domesurvival/forge/machine/organic').glob('*.java'))
paths+=list((root/'src/main/resources/data/domesurvival/recipes/organic_processing').glob('*.json'))
paths += [root/p for p in ['src/main/resources/assets/domesurvival/blockstates/organic_processor.json','src/main/resources/assets/domesurvival/models/block/organic_processor.json','src/main/resources/assets/domesurvival/models/block/organic_processor_active.json','src/main/resources/assets/domesurvival/models/item/organic_processor.json','src/main/java/com/wasted/domesurvival/forge/client/jei/OrganicProcessorRecipeCategory.java']]
if not (out/'baseline.json').exists():
    hashes={}
    for p in paths:
        rel=p.relative_to(root);dest=out/'baseline'/rel;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest);hashes[str(rel).replace('\\','/')]=hashlib.sha256(p.read_bytes()).hexdigest()
    (out/'baseline.json').write_text(json.dumps(hashes,indent=2)+'\n')
print('Organic baseline preserved')
