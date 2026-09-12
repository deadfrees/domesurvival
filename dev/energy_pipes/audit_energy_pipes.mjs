// Read-only audit of production energy-pipe assets; writes evidence only to this directory.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const assets = path.join(root, 'src/main/resources/assets/domesurvival');
const read = p => JSON.parse(fs.readFileSync(path.join(assets, p), 'utf8'));
const ids = ['basic_energy_pipe', 'reinforced_energy_pipe', 'high_voltage_energy_pipe'];
const directions = {north: [0,0], east: [0,90], south: [0,180], west: [0,270], up: [270,0], down: [90,0]};
const errors = [];
const check = (ok, message) => { if (!ok) errors.push(message); };
const digest = value => crypto.createHash('sha256').update(JSON.stringify(value)).digest('hex');
const pipes = ids.map(id => {
  const state = read(`blockstates/${id}.json`);
  const item = read(`models/item/${id}.json`);
  const parts = Object.fromEntries(['core', 'arm', 'inventory'].map(part => {
    const model = read(`models/block/${id}_${part}.json`);
    const elements = model.elements;
    let faces = 0, cullfaces = 0, fullUVFaces = 0;
    for (const e of elements) {
      check(e.from.every((v,i) => v >= 0 && v < e.to[i] && e.to[i] <= 16), `${id}/${part}: invalid bounds`);
      for (const f of Object.values(e.faces)) {
        faces++; if (f.cullface) cullfaces++;
        if (JSON.stringify(f.uv) === '[0,0,16,16]') fullUVFaces++;
        const texture = model.textures[f.texture.slice(1)];
        check(Boolean(texture) && fs.existsSync(path.join(assets, 'textures', texture?.split(':')[1] + '.png')), `${id}/${part}: unresolved texture ${f.texture}`);
      }
    }
    return [part, {elements: elements.length, faces, cullfaces, fullUVFaces,
      geometryHash: digest(elements),
      bounds: {from: [0,1,2].map(i => Math.min(...elements.map(e => e.from[i]))), to: [0,1,2].map(i => Math.max(...elements.map(e => e.to[i])))},
      textures: model.textures}];
  }));
  check(state.multipart.length === 7, `${id}: unexpected multipart count`);
  const base = state.multipart.filter(p => !p.when);
  check(base.length === 1 && base[0].apply.model === `domesurvival:block/${id}_core`, `${id}: core reference`);
  for (const [dir, [x,y]] of Object.entries(directions)) {
    const rule = state.multipart.find(p => p.when?.[dir] === 'true');
    check(rule?.apply.model === `domesurvival:block/${id}_arm` && (rule.apply.x ?? 0) === x && (rule.apply.y ?? 0) === y && rule.apply.uvlock === true, `${id}: ${dir} mapping`);
  }
  for (let mask = 0; mask < 64; mask++) {
    const flags = Object.fromEntries(Object.keys(directions).map((dir,i) => [dir, !!(mask & (1 << i))]));
    const selected = state.multipart.filter(p => !p.when || Object.entries(p.when).every(([k,v]) => String(flags[k]) === v));
    check(selected.length === 1 + Object.values(flags).filter(Boolean).length, `${id}: state ${mask}`);
  }
  const displays = ['gui','ground','fixed','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'];
  check(displays.every(d => item.display[d]), `${id}: missing display transform`);
  check(fs.existsSync(path.join(assets, 'models', item.parent.split(':')[1] + '.json')), `${id}: missing item parent`);
  const textures = ['', '_core', '_detail', '_arm'].map(suffix => {
    const filename = `textures/block/${id}${suffix}.png`;
    const png = fs.readFileSync(path.join(assets, filename));
    return {filename, width: png.readUInt32BE(16), height: png.readUInt32BE(20), animated: fs.existsSync(path.join(assets, filename + '.mcmeta'))};
  });
  return {id: `domesurvival:${id}`, parts, textures, itemParent: item.parent, display: item.display,
    quadsByConnectionCount: Array.from({length:7}, (_,d) => parts.core.faces + d * parts.arm.faces)};
});
const result = {scope: 'Static production asset audit, not an in-game rendering or FPS test', statesChecked: 192,
  sameGeometryAcrossTiers: ['core','arm','inventory'].every(part => pipes.every(p => p.parts[part].geometryHash === pipes[0].parts[part].geometryHash)), pipes, errors};
fs.writeFileSync(path.join(root, 'dev/energy_pipes/audit_results.json'), JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify({statesChecked: result.statesChecked, sameGeometryAcrossTiers: result.sameGeometryAcrossTiers, errors}, null, 2));
if (errors.length) process.exitCode = 1;
