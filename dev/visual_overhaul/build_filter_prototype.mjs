/** Native cuboid model construction; existing pixel textures are reused unchanged.
 * Run with Node from the repository root. Baseline copies are never overwritten.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const root = path.resolve(process.argv[2] || process.cwd());
const assets = path.join(root, 'src/main/resources/assets/domesurvival');
const baseline = path.join(root, 'source_assets/baseline/filter_regeneration_station');
fs.mkdirSync(baseline, {recursive:true});
const files = ['models/block/filter_regeneration_station.json',
  'models/block/filter_regeneration_station_active.json', 'models/item/filter_regeneration_station.json'];
for (const file of files) {
  const dest = path.join(baseline,file);
  fs.mkdirSync(path.dirname(dest), {recursive:true});
  if (!fs.existsSync(dest)) fs.copyFileSync(path.join(assets,file),dest);
}
const textures = {
  metal:'domesurvival:block/hopper/steel_body',
  trim:'domesurvival:block/hopper/steel_rim',
  dark:'domesurvival:block/filter_regenerator_metal_dark',
  black:'domesurvival:block/filter_regenerator_black',
  mesh:'domesurvival:block/filter_regenerator_mesh',
  lamp:'domesurvival:block/filter_regenerator_black',
  energy:'domesurvival:block/filter_regenerator_hazard',
  vent:'domesurvival:block/filter_regenerator_vent',
  particle:'domesurvival:block/hopper/steel_body'
};
const elements = [];
const directions = {north:[2,-1],south:[2,1],west:[0,-1],east:[0,1],down:[1,-1],up:[1,1]};
function box(name, from, to, texture, overrides={}) {
  const [x,y,z]=from, [X,Y,Z]=to;
  const uv={north:[16-X,16-Y,16-x,16-y],south:[x,16-Y,X,16-y],
    west:[z,16-Y,Z,16-y],east:[16-Z,16-Y,16-z,16-y],
    up:[x,z,X,Z],down:[x,16-Z,X,16-z]};
  const faces={};
  for(const [face,[axis,sign]] of Object.entries(directions)) {
    faces[face]={uv:uv[face],texture:'#'+texture,...overrides[face]};
    if((sign<0?from[axis]:to[axis]) === (sign<0?0:16)) faces[face].cullface=face;
  }
  elements.push({name,from,to,faces});
}
// Structural core seals all recesses. Shell faces partition the full block envelope.
box('sealed_inner_chassis',[1,1,1],[15,15,15],'black');
// A side uses u/v coordinates on its outside face, d measured inward.
const maps={west:(u,v,d)=>[d,v,u],east:(u,v,d)=>[16-d,v,16-u],
  south:(u,v,d)=>[u,v,16-d],up:(u,v,d)=>[u,16-d,v],down:(u,v,d)=>[u,d,16-v]};
function sideBox(side,name,u,v,U,V,d,D,tex,faceUv) {
  const a=maps[side](u,v,d),b=maps[side](U,V,D);
  const from=a.map((n,i)=>Math.min(n,b[i])),to=a.map((n,i)=>Math.max(n,b[i]));
  box(side+'_'+name,from,to,tex,faceUv?{[side]:{uv:faceUv}}:{});
}
for(const side of Object.keys(maps)) {
  // Back owns full width/height; top/bottom stop at front/back; side walls stop at all rails.
  const u0=side==='west'||side==='east'?1:0, u1=side==='west'||side==='east'?15:16;
  const v0=side==='south'?0:1, v1=side==='south'?16:15;
  sideBox(side,'lower_shell',u0,v0,u1,5,0,1,'metal');
  sideBox(side,'upper_shell',u0,11,u1,v1,0,1,'metal');
  sideBox(side,'left_shell',u0,5,5,11,0,1,'metal');
  sideBox(side,'right_shell',11,5,u1,11,0,1,'metal');
  // 6x6 collar, recessed contact. Ports all meet the existing face centre (8,8).
  sideBox(side,'socket_lower',5,5,11,6,0,1,'dark');
  sideBox(side,'socket_upper',5,10,11,11,0,1,'dark');
  sideBox(side,'socket_left',5,6,6,10,0,1,'dark');
  sideBox(side,'socket_right',10,6,11,10,0,1,'dark');
  // A 4x4 inset and 2x2 contact communicate the physical recess without protrusion.
  sideBox(side,'socket_well',6,6,10,10,0.75,1,'black');
  if(side==='down') {
    sideBox(side,'output_chevron_left',6,7,7,8,0.5,0.75,'trim');
    sideBox(side,'output_chevron_right',9,7,10,8,0.5,0.75,'trim');
    sideBox(side,'output_chevron_tip',7,8,9,9,0.5,0.75,'trim');
  } else sideBox(side,'contact',7,7,9,9,0.5,0.75,'energy',[4,4,6,6]);
}
// NORTH: service facade, no automation capability. Two broad cassette windows.
box('base_plinth',[0,0,0],[16,2,1],'dark');
box('top_crossmember',[0,14,0],[16,16,1],'trim');
box('left_upright',[0,2,0],[2,14,1],'trim');
box('right_upright',[14,2,0],[16,14,1],'trim');
box('cassette_left_frame',[2,2,0],[3,14,1],'dark');
box('cassette_right_frame',[11,2,0],[12,14,1],'dark');
box('cassette_lower_frame',[3,2,0],[11,3,1],'dark');
box('cassette_separator',[3,7.5,0],[11,8.5,1],'metal');
box('cassette_upper_frame',[3,13,0],[11,14,1],'dark');
box('lower_filter_cassette',[3,3,0.5],[11,7.5,1],'mesh');
box('upper_filter_cassette',[3,8.5,0.5],[11,13,1],'mesh');
// Handles occupy a different plane than the mesh; hidden backs removed below.
box('lower_cassette_handle',[4,3.5,0],[10,4.5,0.5],'trim');
box('upper_cassette_handle',[4,9,0],[10,10,0.5],'trim');
box('control_column',[12,2,0.5],[14,14,1],'dark');
box('working_lamp',[12.5,11.5,0],[13.5,12.5,0.5],'lamp', {north:{uv:[6,6,10,10]}});
box('service_vent',[12.25,4,0.25],[13.75,9,0.5],'vent');
// Sparse warning stripe on lower plinth, inset within its surface to avoid coplanar overlays.
// All exposed detail is native geometry. No extra glowing or always-ticking renderer.

// Remove a face only if its whole rectangular surface is covered by another solid cuboid
// on the outward side. This removes backs of handles, buried socket walls, and internal rails.
let removed=0;
for(const a of elements) for(const [face,[axis,sign]] of Object.entries(directions)) {
  if(!a.faces[face]) continue;
  const plane=sign<0?a.from[axis]:a.to[axis], otherAxes=[0,1,2].filter(i=>i!==axis);
  const buried=elements.some(b=>b!==a && otherAxes.every(i=>b.from[i]<=a.from[i]&&b.to[i]>=a.to[i]) &&
    (sign<0 ? b.from[axis]<plane&&b.to[axis]>=plane : b.from[axis]<=plane&&b.to[axis]>plane));
  if(buried) {delete a.faces[face];removed++;}
}
const display={
  gui:{rotation:[30,225,0],translation:[0,0,0],scale:[0.625,0.625,0.625]},
  ground:{rotation:[0,0,0],translation:[0,3,0],scale:[0.25,0.25,0.25]},
  fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[0.5,0.5,0.5]},
  thirdperson_righthand:{rotation:[75,45,0],translation:[0,2.5,0],scale:[0.375,0.375,0.375]},
  thirdperson_lefthand:{rotation:[75,45,0],translation:[0,2.5,0],scale:[0.375,0.375,0.375]},
  firstperson_righthand:{rotation:[0,45,0],translation:[0,0,0],scale:[0.4,0.4,0.4]},
  firstperson_lefthand:{rotation:[0,225,0],translation:[0,0,0],scale:[0.4,0.4,0.4]}
};
const model={parent:'minecraft:block/block',ambientocclusion:true,textures,elements,display};
const active={parent:'domesurvival:block/filter_regeneration_station',textures:{
  mesh:'domesurvival:block/filter_regenerator_mesh_active',lamp:'domesurvival:block/filter_regenerator_lamp_green_active'}};
const item={parent:'domesurvival:block/filter_regeneration_station',display};
for(const [i,value] of [model,active,item].entries()) fs.writeFileSync(path.join(assets,files[i]),JSON.stringify(value,null,2)+'\n');

// Portable Blockbench master with original embedded PNGs; no texture image changes.
const keys=Object.keys(textures).filter(k=>k!=='particle');
const uid=name=>{const h=crypto.createHash('md5').update('domesurvival/filter/'+name).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
const bbElements=elements.map(e=>({...e,uuid:uid(e.name),type:'cube',origin:[8,8,8],box_uv:false,rescale:false,
  faces:Object.fromEntries(Object.keys(directions).map(f=>[f,e.faces[f]
    ? {...e.faces[f],texture:keys.indexOf(e.faces[f].texture.slice(1))}
    : {uv:[0,0,0,0],texture:null}]))}));
const master={meta:{format_version:'4.10',model_format:'java_block',box_uv:false},name:'filter_regeneration_station',
  model_identifier:'filter_regeneration_station',visible_box:[1,1,0],resolution:{width:16,height:16},
  elements:bbElements,outliner:bbElements.map(e=>e.uuid),textures:keys.map((k,i)=>({
    path:path.join(assets,'textures',textures[k].split(':')[1]+'.png'),name:textures[k].split('/').at(-1)+'.png',
    folder:'block',namespace:'domesurvival',id:String(i),uuid:uid('texture_'+k),width:16,height:16,uv_width:16,uv_height:16,
    source:'data:image/png;base64,'+fs.readFileSync(path.join(assets,'textures',textures[k].split(':')[1]+'.png')).toString('base64')})),display};
const masterPath=path.join(root,'source_assets/blockbench/machines/filter_regeneration_station.bbmodel');
fs.mkdirSync(path.dirname(masterPath),{recursive:true}); fs.writeFileSync(masterPath,JSON.stringify(master,null,2)+'\n');
const quads=elements.reduce((n,e)=>n+Object.keys(e.faces).length,0);
const summary={cuboids:elements.length,quads,triangles:quads*2,removedHiddenFaces:removed,bounds:[0,0,0,16,16,16],
  textures:keys.map(k=>textures[k]),newTextures:0,newBERs:0,master:path.relative(root,masterPath)};
fs.writeFileSync(path.join(root,'dev/visual_overhaul/prototype_geometry.json'),JSON.stringify(summary,null,2)+'\n');
console.log(JSON.stringify(summary,null,2));
