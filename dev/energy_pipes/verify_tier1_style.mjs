import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const assets=path.join(root,'src/main/resources/assets/domesurvival');
const baselineRoot=path.join(root,'source_assets/baseline/energy_pipes/tier1');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const files=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(path.join(p,e.name)):[path.join(p,e.name)]);
const id='basic_energy_pipe';
const changedAllowed=['models/block/'+id+'_core.json','models/block/'+id+'_arm.json','models/block/'+id+'_inventory.json','models/item/'+id+'.json','blockstates/'+id+'.json'];
const addedAllowed=['body','core','panel','rail','end','edge'].map(r=>'textures/block/energy_pipe/basic_'+r+'.png');
const prefix='src/main/resources/assets/domesurvival/';
const baseline=read(path.join(root,'dev/energy_pipes/tier1_baseline_hashes.json'));
const errors=[];const check=(ok,message)=>{if(!ok)errors.push(message);};
const current=Object.fromEntries(files(path.join(root,'src/main')).map(p=>[path.relative(root,p).replaceAll('\\','/'),hash(p)]));
const changed=Object.keys(baseline).filter(p=>current[p]!==baseline[p]);
const added=Object.keys(current).filter(p=>!(p in baseline));
const outsideChanged=changed.filter(p=>!changedAllowed.includes(p.slice(prefix.length))||!p.startsWith(prefix));
const outsideAdded=added.filter(p=>!addedAllowed.includes(p.slice(prefix.length))||!p.startsWith(prefix));
check(addedAllowed.every(p=>added.includes(prefix+p)),'Missing expected pipe texture');
check(![...outsideChanged,...outsideAdded].some(p=>/energy_pipe/.test(p)),'Unexpected energy pipe changes');
const geometry=m=>m.elements.map(e=>({name:e.name,from:e.from,to:e.to,rotation:e.rotation,faces:Object.keys(e.faces).sort()}));
for(const part of ['core','arm']){
 const relative=`models/block/${id}_${part}.json`,before=read(path.join(baselineRoot,relative)),after=read(path.join(assets,relative));
 check(JSON.stringify(geometry(before))===JSON.stringify(geometry(after)),`${part} world geometry changed`);
 for(const e of after.elements)for(const f of Object.values(e.faces)){
  check(f.uv.length===4&&f.uv.every(n=>Number.isFinite(n)&&n>=0&&n<=16),'Invalid UV');
  const texture=after.textures[f.texture.slice(1)];
  check(texture&&fs.existsSync(path.join(assets,'textures',texture.split(':')[1]+'.png')),'Missing texture');
 }
}
const state=read(path.join(assets,`blockstates/${id}.json`));
const previousState=read(path.join(baselineRoot,`blockstates/${id}.json`));
const ignoreUV=m=>m.multipart.map(r=>({when:r.when,apply:{model:r.apply.model,x:r.apply.x,y:r.apply.y}}));
check(JSON.stringify(ignoreUV(state))===JSON.stringify(ignoreUV(previousState)),'Connection selectors or model rotation changed');
check(state.multipart.filter(r=>r.when).every(r=>r.apply.uvlock===false),'Arm UV must follow geometry');
for(const relative of addedAllowed){const bytes=fs.readFileSync(path.join(assets,relative));check(bytes.readUInt32BE(16)===16&&bytes.readUInt32BE(20)===16,'Unexpected pixel texture resolution');}
const item=read(path.join(assets,`models/item/${id}.json`));
check(['gui','ground','fixed','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'].every(k=>item.display[k]),'Missing item display');
const inventory=read(path.join(assets,`models/block/${id}_inventory.json`));
check(inventory.elements.length===17,'Item should contain core and two arms');
const quads=inventory.elements.reduce((n,e)=>n+Object.keys(e.faces).length,0);check(quads===88,'Wrong item face count');
for(const relative of changedAllowed)check(hash(path.join(baselineRoot,relative))===hash(path.join(root,'run/energy-pipe-visual/resourcepacks/energy_pipe_before/assets/domesurvival',relative)),'Before-pack differs from baseline');
const result={status:errors.length?'FAIL':'PASS',scope:'energy pipe asset validation',workspace_scope_status:outsideChanged.length||outsideAdded.length?'DRIFT_OUTSIDE_PIPE_SCOPE':'PASS',changed:changed.filter(p=>!outsideChanged.includes(p)),added:added.filter(p=>!outsideAdded.includes(p)),outsideChanged,outsideAdded,unchanged:Object.keys(baseline).length-changed.length,world_geometry_unchanged:true,item_quads:quads,errors};
fs.writeFileSync(path.join(root,'dev/energy_pipes/tier1_static_checks.json'),JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify(result,null,2));if(errors.length)process.exitCode=1;

// Editable Blockbench item master, retaining all explicit face UV and display transforms.
if(!errors.length){
 const uuid=text=>{const h=crypto.createHash('sha256').update(text).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-8${h.slice(17,20)}-${h.slice(20,32)}`;};
 const roles=['body','core','panel','rail','end','edge'];
 const textures=roles.map((role,index)=>{const p=path.join(root,`source_assets/blender/energy_pipes/textures/basic_${role}.png`);return {path:p,name:`basic_${role}.png`,id:String(index),uuid:uuid('texture/'+role),mode:'bitmap',visible:true,source:'data:image/png;base64,'+fs.readFileSync(p).toString('base64')};});
 const elements=inventory.elements.map((e,index)=>({...e,uuid:uuid('element/'+index),type:'cube',box_uv:false,autouv:0,origin:[8,8,8],faces:Object.fromEntries(['north','south','east','west','up','down'].map(d=>{const f=e.faces[d];return [d,f?{...f,texture:roles.indexOf(f.texture.slice(1))}:{uv:[0,0,0,0],texture:null}];}))}));
 const groups=[['junction',0,7],['north_connection',7,12],['south_connection',12,17]].map(([name,a,b])=>({name,origin:[8,8,8],uuid:uuid(name),children:elements.slice(a,b).map(e=>e.uuid)}));
 const model={meta:{format_version:'4.10',model_format:'java_block',box_uv:false},name:'Basic energy pipe — DOMESURVIVAL',model_identifier:id,resolution:{width:16,height:16},elements,outliner:groups,textures,display:item.display};
 const output=path.join(root,'source_assets/blockbench/energy_pipes/energy_pipe_tier_1.bbmodel');fs.mkdirSync(path.dirname(output),{recursive:true});fs.writeFileSync(output,JSON.stringify(model,null,2)+'\n');
}
