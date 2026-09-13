import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const assets=path.join(root,'src/main/resources/assets/domesurvival');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const walk=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(p,e.name)):[path.join(p,e.name)]);
const roles=['body','core','panel','rail','end','edge'];
const ids=['basic_energy_pipe','reinforced_energy_pipe','high_voltage_energy_pipe'];
const errors=[];const check=(ok,message)=>{if(!ok)errors.push(message);};
const baseline=read(path.join(root,'dev/energy_pipes/tiers23_baseline_hashes.json'));
const current=Object.fromEntries(walk(path.join(root,'src/main')).map(p=>[path.relative(root,p).replaceAll('\\','/'),hash(p)]));
const assetPrefix='src/main/resources/assets/domesurvival/';
const modelPaths=id=>['core','arm','inventory'].map(p=>`models/block/${id}_${p}.json`).concat(`models/item/${id}.json`,`blockstates/${id}.json`);
const changedAllowed=ids.slice(1).flatMap(modelPaths).map(p=>assetPrefix+p);
const addedAllowed=ids.slice(1).flatMap(id=>roles.map(r=>assetPrefix+`textures/block/energy_pipe/${id.replace('_energy_pipe','')}_${r}.png`));
// high_voltage_energy_pipe follows the same suffix convention.
const changed=Object.keys(baseline).filter(p=>current[p]!==baseline[p]);
const added=Object.keys(current).filter(p=>!(p in baseline));
const outsideChanged=changed.filter(p=>!changedAllowed.includes(p));
const outsideAdded=added.filter(p=>!addedAllowed.includes(p));
check(addedAllowed.every(p=>added.includes(p)),'Missing expected new texture');
check(!outsideChanged.some(p=>/energy_pipe|EnergyPipe/.test(p)),'Unexpected changes to other pipe assets or code');
check(!outsideAdded.some(p=>/energy_pipe|EnergyPipe/.test(p)),'Unexpected new pipe asset or code');
const geometry=m=>m.elements.map(e=>({name:e.name,from:e.from,to:e.to,rotation:e.rotation,faces:Object.keys(e.faces).sort()}));
const results=[];
for(const [i,id] of ids.entries()){
 const tier=i+1,prefix=id.replace('_energy_pipe','');
 const backup=path.join(root,`source_assets/baseline/energy_pipes/tier${tier}`);
 for(const part of ['core','arm']){
  const relative=`models/block/${id}_${part}.json`,before=read(path.join(backup,relative)),after=read(path.join(assets,relative));
  check(JSON.stringify(geometry(before))===JSON.stringify(geometry(after)),`${id} ${part} geometry changed`);
 }
 const inventory=read(path.join(assets,`models/block/${id}_inventory.json`));
 for(const part of ['core','arm','inventory']){
  const model=read(path.join(assets,`models/block/${id}_${part}.json`));
  for(const element of model.elements)for(const face of Object.values(element.faces)){
   check(face.uv?.length===4&&face.uv.every(n=>Number.isFinite(n)&&n>=0&&n<=16),`${id} invalid UV`);
   check([0,90,180,270].includes(face.rotation??0),`${id} invalid face rotation`);
   const texture=model.textures[face.texture.slice(1)];
   check(texture&&fs.existsSync(path.join(assets,'textures',texture.split(':')[1]+'.png')),`${id} missing texture`);
  }
 }
 const state=read(path.join(assets,`blockstates/${id}.json`));
 const strip=m=>m.multipart.map(e=>({when:e.when,apply:{model:e.apply.model,x:e.apply.x,y:e.apply.y}}));
 check(JSON.stringify(strip(state))===JSON.stringify(strip(read(path.join(backup,`blockstates/${id}.json`)))),`${id} multipart changed`);
 check(state.multipart.filter(e=>e.when).every(e=>e.apply.uvlock===false),`${id} locked arm UV`);
 for(const role of roles){const p=path.join(assets,`textures/block/energy_pipe/${prefix}_${role}.png`);const bytes=fs.readFileSync(p);check(bytes.readUInt32BE(16)===16&&bytes.readUInt32BE(20)===16,`${id} texture resolution`);}
 const item=read(path.join(assets,`models/item/${id}.json`));
 check(item.parent===`domesurvival:block/${id}_inventory`,`${id} wrong item parent`);
 check(['gui','ground','fixed','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'].every(k=>item.display[k]),`${id} missing item display`);
 const quads=inventory.elements.reduce((n,e)=>n+Object.keys(e.faces).length,0);
 check(inventory.elements.length===17&&quads===88,`${id} wrong inventory geometry`);
 if(tier>1)for(const relative of modelPaths(id))check(hash(path.join(backup,relative))===hash(path.join(root,'run/energy-pipe-visual/resourcepacks/energy_pipe_tiers23_before/assets/domesurvival',relative)),`${id} comparison baseline mismatch`);
 results.push({tier,id,world_geometry_unchanged:true,item_quads:quads});
 if(tier>1){
  const uuid=text=>{const h=crypto.createHash('sha256').update(id+'/'+text).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-8${h.slice(17,20)}-${h.slice(20,32)}`;};
  const textures=roles.map((role,index)=>({name:`${prefix}_${role}.png`,id:String(index),uuid:uuid(role),mode:'bitmap',visible:true,source:'data:image/png;base64,'+fs.readFileSync(path.join(assets,`textures/block/energy_pipe/${prefix}_${role}.png`)).toString('base64')}));
  const elements=inventory.elements.map((e,index)=>({...e,uuid:uuid('element/'+index),type:'cube',box_uv:false,autouv:0,origin:[8,8,8],faces:Object.fromEntries(['north','south','east','west','up','down'].map(d=>{const f=e.faces[d];return[d,f?{...f,texture:roles.indexOf(f.texture.slice(1))}:{uv:[0,0,0,0],texture:null}];}))}));
  const outliner=[['junction',0,7],['north_connection',7,12],['south_connection',12,17]].map(([name,a,b])=>({name,origin:[8,8,8],uuid:uuid(name),children:elements.slice(a,b).map(e=>e.uuid)}));
  const model={meta:{format_version:'4.10',model_format:'java_block',box_uv:false},name:`Energy pipe Tier ${tier} — DOMESURVIVAL`,model_identifier:id,resolution:{width:16,height:16},elements,outliner,textures,display:item.display};
  if(!errors.length)fs.writeFileSync(path.join(root,`source_assets/blockbench/energy_pipes/energy_pipe_tier_${tier}.bbmodel`),JSON.stringify(model,null,2)+'\n');
 }
}
const result={status:errors.length?'FAIL':'PASS',workspace_scope_status:outsideChanged.length||outsideAdded.length?'DRIFT_OUTSIDE_PIPE_SCOPE':'PASS',changed:changed.filter(p=>changedAllowed.includes(p)),added:added.filter(p=>addedAllowed.includes(p)),outsideChanged,outsideAdded,tier1_unchanged:!outsideChanged.some(p=>p.includes('basic_energy_pipe')||p.includes('energy_pipe/basic_')),results,errors};
fs.writeFileSync(path.join(root,'dev/energy_pipes/tiers23_static_checks.json'),JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify(result,null,2));if(errors.length)process.exitCode=1;
