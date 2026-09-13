/**
 * Independent, read-only verification of the first filter-regenerator prototype.
 * Node standard library only. Writes JSON/Markdown reports under dev/visual_overhaul.
 * Usage: node dev/visual_overhaul/verify_prototype.mjs [workspace-root]
 * Optional: VISUAL_AUDIT_MINECRAFT_JAR=/absolute/path/to/1.20.1/client.jar
 *
 * The baseline is the pre-existing DIRTY working tree, not Git HEAD. Do not regenerate
 * the baseline to make this check pass. This validator does not launch Minecraft.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import zlib from 'node:zlib';

const root = path.resolve(process.argv[2] || process.cwd());
const reportDir = path.join(root, 'dev/visual_overhaul');
const baselinePath = path.join(reportDir, 'preexisting_src_hashes.json');
const BASELINE_SHA256 = 'f7b50db7484bbf021092ff09f9cf3f07b212a8081633a3e76dbce2a78e49e663';
const assetsPrefix = 'src/main/resources/assets/domesurvival/';
const inactivePath = assetsPrefix + 'models/block/filter_regeneration_station.json';
const activePath = assetsPrefix + 'models/block/filter_regeneration_station_active.json';
const itemPath = assetsPrefix + 'models/item/filter_regeneration_station.json';
const blockstatePath = assetsPrefix + 'blockstates/filter_regeneration_station.json';
const screenPath = 'src/main/java/com/wasted/domesurvival/forge/machine/filter/FilterRegenerationScreen.java';
const allowed = new Set([inactivePath, activePath, itemPath, screenPath]);
const inactiveId = 'domesurvival:block/filter_regeneration_station';
const activeId = 'domesurvival:block/filter_regeneration_station_active';
const itemId = 'domesurvival:item/filter_regeneration_station';
const displayContexts = ['gui', 'ground', 'fixed', 'thirdperson_righthand',
  'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand'];
const faceDirections = ['down', 'up', 'north', 'south', 'west', 'east'];
const issues = [];
const checks = [];
const models = [];
const references = new Map();
const slash = value => value.replaceAll('\\', '/');
const hash = value => crypto.createHash('sha256').update(value).digest('hex');
const read = filename => fs.readFileSync(filename, 'utf8').replace(/^\uFEFF/, '');
const finite = value => typeof value === 'number' && Number.isFinite(value);
const object = value => value !== null && typeof value === 'object' && !Array.isArray(value);
const vector = (value, length) => Array.isArray(value) && value.length === length && value.every(finite);
const near = (a, b) => Math.abs(a - b) < 1e-7;
const issue = (severity, code, file, message) => issues.push({ severity, code, file, message });
function assert(condition, code, file, message) {
  if (!condition) issue('error', code, file, message);
  return Boolean(condition);
}
function section(name, action) {
  const before = issues.filter(value => value.severity === 'error').length;
  try { action(); } catch (error) { issue('error', 'CHECK_EXCEPTION', name, error.message); }
  checks.push({ name, passed: issues.filter(value => value.severity === 'error').length === before });
}
function parse(text, filename) {
  const data = JSON.parse(text.replace(/^\uFEFF/, ''));
  if (!object(data)) throw new Error(`${filename}: JSON root must be an object`);
  return data;
}

// Deliberately include every source file and reject symlinks rather than following
// them outside the source tree. No filtering by language, registry or extension.
function sourceFiles(directory, list = []) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const filename = path.join(directory, entry.name);
    const relative = slash(path.relative(root, filename));
    if (entry.isSymbolicLink()) {
      issue('error', 'SOURCE_SYMLINK', relative, 'Symlink is outside this verifier contract.');
    } else if (entry.isDirectory()) sourceFiles(filename, list);
    else if (entry.isFile()) list.push(relative);
  }
  return list;
}

let baseline = new Map();
const sourceDiff = { unchanged: 0, changed: [], added: [], removed: [], allowedChanged: [] };
section('Pinned baseline and all src file hashes', () => {
  const data = fs.readFileSync(baselinePath);
  assert(hash(data) === BASELINE_SHA256, 'BASELINE_CHANGED', slash(path.relative(root, baselinePath)),
    'Baseline bytes do not match the hash recorded before prototype work.');
  const entries = JSON.parse(data.toString('utf8').replace(/^\uFEFF/, ''));
  if (!Array.isArray(entries) || entries.length === 0) throw new Error('Baseline is empty or malformed.');
  for (const entry of entries) {
    if (!object(entry) || typeof entry.path !== 'string' || typeof entry.hash !== 'string') {
      throw new Error('Malformed baseline entry.');
    }
    const normalized = slash(entry.path);
    if (!normalized.startsWith('src/') || normalized.split('/').includes('..') || !/^[a-f0-9]{64}$/i.test(entry.hash)) {
      throw new Error(`Invalid baseline path or hash: ${normalized}`);
    }
    if (baseline.has(normalized)) throw new Error(`Duplicate baseline entry: ${normalized}`);
    baseline.set(normalized, entry.hash.toLowerCase());
  }
  const current = new Set(sourceFiles(path.join(root, 'src')));
  for (const [filename, previousHash] of baseline) {
    if (!current.has(filename)) {
      sourceDiff.removed.push(filename);
      issue('error', 'SOURCE_REMOVED', filename, 'No source removals are allowed, including prototype files.');
      continue;
    }
    if (hash(fs.readFileSync(path.join(root, filename))) === previousHash) sourceDiff.unchanged++;
    else {
      sourceDiff.changed.push(filename);
      if (allowed.has(filename)) sourceDiff.allowedChanged.push(filename);
      else issue('error', 'SOURCE_CHANGED', filename, 'Source changed outside the four-file visual allowlist.');
    }
  }
  for (const filename of current) {
    if (baseline.has(filename)) continue;
    sourceDiff.added.push(filename);
    issue('error', 'SOURCE_ADDED', filename, 'No new src files are allowed by this prototype contract.');
  }
  for (const filename of allowed) assert(baseline.has(filename), 'ALLOWLIST_NOT_IN_BASELINE', filename,
    'Allowed file must be present in the pre-existing baseline.');
});

// ZIP central-directory reader for a local vanilla jar; no archive extraction,
// downloads or third-party modules. ZIP64/encrypted archives are not supported.
function openZip(filename) {
  const bytes = fs.readFileSync(filename);
  if (bytes.length < 22) throw new Error('Vanilla jar is not a ZIP archive.');
  let end = bytes.length - 22;
  const minimum = Math.max(0, bytes.length - 65557);
  while (end >= minimum && bytes.readUInt32LE(end) !== 0x06054b50) end--;
  if (end < minimum) throw new Error('ZIP end-of-central-directory not found.');
  const count = bytes.readUInt16LE(end + 10);
  let cursor = bytes.readUInt32LE(end + 16);
  if (count === 0xffff || cursor === 0xffffffff) throw new Error('ZIP64 is unsupported.');
  const entries = new Map();
  for (let index = 0; index < count; index++) {
    if (cursor + 46 > bytes.length || bytes.readUInt32LE(cursor) !== 0x02014b50) {
      throw new Error('Malformed ZIP central directory.');
    }
    const flags = bytes.readUInt16LE(cursor + 8);
    const method = bytes.readUInt16LE(cursor + 10);
    const compressedSize = bytes.readUInt32LE(cursor + 20);
    const size = bytes.readUInt32LE(cursor + 24);
    const nameLength = bytes.readUInt16LE(cursor + 28);
    const extraLength = bytes.readUInt16LE(cursor + 30);
    const commentLength = bytes.readUInt16LE(cursor + 32);
    const offset = bytes.readUInt32LE(cursor + 42);
    const next = cursor + 46 + nameLength + extraLength + commentLength;
    if (next > bytes.length) throw new Error('Truncated ZIP entry.');
    const name = bytes.subarray(cursor + 46, cursor + 46 + nameLength).toString('utf8');
    entries.set(name, { flags, method, compressedSize, size, offset });
    cursor = next;
  }
  return {
    filename,
    get(name) {
      const entry = entries.get(name);
      if (!entry) return null;
      if (entry.flags & 1) throw new Error('Encrypted ZIP entry is unsupported.');
      if (entry.offset + 30 > bytes.length || bytes.readUInt32LE(entry.offset) !== 0x04034b50) {
        throw new Error('Invalid local ZIP header.');
      }
      if (entry.size > 32 * 1024 * 1024) throw new Error('Model resource exceeds 32 MiB safety limit.');
      const start = entry.offset + 30 + bytes.readUInt16LE(entry.offset + 26) + bytes.readUInt16LE(entry.offset + 28);
      const end = start + entry.compressedSize;
      if (end > bytes.length) throw new Error('Truncated ZIP payload.');
      const compressed = bytes.subarray(start, end);
      const result = entry.method === 0 ? compressed
        : entry.method === 8 ? zlib.inflateRawSync(compressed, { maxOutputLength: 32 * 1024 * 1024 })
        : null;
      if (!result || result.length !== entry.size) throw new Error('Unsupported or corrupted ZIP payload.');
      return result;
    }
  };
}

const vanillaPath = process.env.VISUAL_AUDIT_MINECRAFT_JAR || path.join(
  process.env.USERPROFILE || process.env.HOME || '',
  '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar');
let vanilla;
section('Local vanilla resource archive', () => { vanilla = openZip(vanillaPath); });

function normalizedId(value) {
  if (typeof value !== 'string') throw new Error('Resource identifier must be a string.');
  const result = value.includes(':') ? value : 'minecraft:' + value;
  if (!/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(result) || result.split(':')[1].split('/').includes('..')) {
    throw new Error(`Invalid resource identifier ${value}`);
  }
  return result;
}
function resource(value, folder, suffix) {
  const id = normalizedId(value);
  const [namespace, location] = id.split(':');
  for (const base of ['src/main/resources', 'src/generated/resources']) {
    const relative = `${base}/assets/${namespace}/${folder}/${location}${suffix}`;
    const filename = path.join(root, relative);
    if (fs.existsSync(filename)) {
      const data = fs.readFileSync(filename);
      references.set(`${folder}:${id}`, { id, folder, source: relative, sha256: hash(data) });
      return { data, source: relative };
    }
  }
  if (namespace === 'minecraft' && vanilla) {
    const entry = `assets/${namespace}/${folder}/${location}${suffix}`;
    const data = vanilla.get(entry);
    if (data) {
      const source = `${vanilla.filename}!/${entry}`;
      references.set(`${folder}:${id}`, { id, folder, source, sha256: hash(data) });
      return { data, source };
    }
  }
  throw new Error(`Unresolved ${folder} reference: ${id}`);
}
const modelCache = new Map();
function model(value, chain = []) {
  const id = normalizedId(value);
  if (chain.includes(id)) throw new Error(`Cyclic model parent: ${[...chain, id].join(' -> ')}`);
  if (modelCache.has(id)) return modelCache.get(id);
  if (chain.length > 30) throw new Error('Model parent chain exceeds 30 levels.');
  const found = resource(id, 'models', '.json');
  const declared = parse(found.data.toString('utf8'), found.source);
  const parent = declared.parent ? model(declared.parent, [...chain, id]) : null;
  const effective = {
    ...parent?.effective, ...declared,
    textures: { ...parent?.effective.textures, ...declared.textures },
    display: { ...parent?.effective.display, ...declared.display }
  };
  const result = { id, source: found.source, declared, effective, chain: [id, ...(parent?.chain || [])] };
  modelCache.set(id, result);
  return result;
}
function resolvedTexture(value, textures, chain = []) {
  if (typeof value !== 'string') throw new Error('Face texture must be a string.');
  if (!value.startsWith('#')) return normalizedId(value);
  const key = value.slice(1);
  if (chain.includes(key)) throw new Error(`Cyclic texture variable: ${[...chain, key].join(' -> ')}`);
  if (!Object.hasOwn(textures, key)) throw new Error(`Unresolved texture variable #${key}`);
  return resolvedTexture(textures[key], textures, [...chain, key]);
}
function faceVertices(element, direction) {
  const [x0, y0, z0] = element.from;
  const [x1, y1, z1] = element.to;
  return {
    down: [[x0,y0,z0],[x1,y0,z0],[x1,y0,z1],[x0,y0,z1]],
    up: [[x0,y1,z0],[x0,y1,z1],[x1,y1,z1],[x1,y1,z0]],
    north: [[x0,y0,z0],[x0,y1,z0],[x1,y1,z0],[x1,y0,z0]],
    south: [[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]],
    west: [[x0,y0,z0],[x0,y0,z1],[x0,y1,z1],[x0,y1,z0]],
    east: [[x1,y0,z0],[x1,y1,z0],[x1,y1,z1],[x1,y0,z1]]
  }[direction];
}
function transformedVertex(vertex, rotation) {
  if (!rotation) return vertex;
  const translated = vertex.map((coordinate, index) => coordinate - rotation.origin[index]);
  const axisIndex = { x: 0, y: 1, z: 2 }[rotation.axis];
  const first = (axisIndex + 1) % 3;
  const second = (axisIndex + 2) % 3;
  const angle = rotation.angle * Math.PI / 180;
  const result = [...translated];
  result[first] = translated[first] * Math.cos(angle) - translated[second] * Math.sin(angle);
  result[second] = translated[first] * Math.sin(angle) + translated[second] * Math.cos(angle);
  if (rotation.rescale) {
    result[first] /= Math.cos(angle);
    result[second] /= Math.cos(angle);
  }
  return result.map((coordinate, index) => coordinate + rotation.origin[index]);
}
function normalOf(vertices) {
  const a = vertices[1].map((value, index) => value - vertices[0][index]);
  const b = vertices[2].map((value, index) => value - vertices[0][index]);
  const cross = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]];
  const length = Math.hypot(...cross);
  return cross.map(value => value / length);
}
const vertexKey = vertex => vertex.map(value => Math.round(value * 1e7) / 1e7).join(',');
function implicitUv(element, direction) {
  const [x0,y0,z0] = element.from, [x1,y1,z1] = element.to;
  return {
    down:[x0,16-z1,x1,16-z0], up:[x0,z0,x1,z1],
    north:[16-x1,16-y1,16-x0,16-y0], south:[x0,16-y1,x1,16-y0],
    west:[z0,16-y1,z1,16-y0], east:[16-z1,16-y1,16-z0,16-y0]
  }[direction];
}
function validateModel(id) {
  const resolved = model(id);
  const data = resolved.effective;
  const stats = { id, source: resolved.source, parentChain: resolved.chain, cuboids: 0, quads: 0,
    triangles: 0, duplicateFaces: [], sharedInternalFaces: [], textures: [], display: data.display };
  assert(!data.loader, 'CUSTOM_LOADER', resolved.source, 'Prototype must remain a standard JSON cuboid model.');
  assert(Array.isArray(data.elements) && data.elements.length > 0, 'MODEL_EMPTY', resolved.source,
    'Effective model must have cuboid elements.');
  for (const context of displayContexts) {
    const transform = data.display?.[context];
    if (!assert(object(transform), 'DISPLAY_MISSING', resolved.source,
      `Required display context ${context} is not explicit or inherited.`)) continue;
    for (const component of ['rotation', 'translation', 'scale']) {
      if (transform[component] === undefined) continue; // Vanilla identity/default component.
      assert(vector(transform[component], 3), 'DISPLAY_VECTOR', resolved.source,
        `${context}.${component} must have three finite numeric values.`);
      if (component === 'scale' && vector(transform[component], 3)) assert(
        transform.scale.every(value => value > 0), 'DISPLAY_SCALE', resolved.source,
        `${context} has zero or negative scale.`);
    }
  }
  const textureIds = new Set();
  for (const key of Object.keys(data.textures || {})) {
    try {
      const textureId = resolvedTexture('#' + key, data.textures);
      const found = resource(textureId, 'textures', '.png');
      assert(found.data.length >= 24 && found.data.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])),
        'TEXTURE_NOT_PNG', found.source, 'Referenced texture has no valid PNG signature/header.');
      textureIds.add(textureId);
    } catch (error) { issue('error', 'TEXTURE_REFERENCE', resolved.source, error.message); }
  }
  const planes = new Map();
  for (const [index, element] of (Array.isArray(data.elements) ? data.elements : []).entries()) {
    const name = `${resolved.source}#elements[${index}]`;
    if (!assert(object(element) && vector(element.from,3) && vector(element.to,3), 'CUBOID_VECTOR', name,
      'from/to must contain three finite coordinates.')) continue;
    stats.cuboids++;
    assert([...element.from,...element.to].every(value => value >= 0 && value <= 16),
      'CUBOID_BOUNDS', name, 'All unrotated cuboid bounds must lie in 0..16.');
    if (!assert(element.from.every((value, axis) => value < element.to[axis]), 'CUBOID_DEGENERATE', name,
      'Every cuboid axis must have strictly positive extent; inverted/zero bounds are forbidden.')) continue;
    if (element.rotation !== undefined) {
      const rotation = element.rotation;
      if (!assert(object(rotation) && vector(rotation.origin,3) && ['x','y','z'].includes(rotation.axis)
        && [-45,-22.5,0,22.5,45].includes(rotation.angle), 'ELEMENT_ROTATION', name,
        'Rotation must use a vanilla axis, finite origin and supported angle.')) continue;
    }
    if (!assert(object(element.faces) && Object.keys(element.faces).length > 0, 'CUBOID_FACES', name,
      'Cuboid must define at least one face.')) continue;
    for (const [direction, face] of Object.entries(element.faces)) {
      if (!assert(faceDirections.includes(direction) && object(face), 'FACE_INVALID', name,
        `Unknown or malformed face: ${direction}`)) continue;
      stats.quads++;
      const uv = face.uv ?? implicitUv(element, direction);
      assert(vector(uv,4) && uv.every(value => value >= 0 && value <= 16), 'FACE_UV', name,
        `${direction} UV coordinates must lie in 0..16, including inherited/default UV.`);
      assert(face.rotation === undefined || [0,90,180,270].includes(face.rotation), 'FACE_ROTATION', name,
        `${direction} UV rotation must be 0, 90, 180 or 270.`);
      assert(face.cullface === undefined || faceDirections.includes(face.cullface), 'FACE_CULL', name,
        `${direction} cullface must be a direction.`);
      try {
        const textureId = resolvedTexture(face.texture, data.textures);
        resource(textureId, 'textures', '.png');
        textureIds.add(textureId);
      } catch (error) { issue('error','FACE_TEXTURE',name,`${direction}: ${error.message}`); }
      const vertices = faceVertices(element,direction).map(vertex => transformedVertex(vertex,element.rotation));
      assert(vertices.flat().every(value => value >= -1e-7 && value <= 16+1e-7), 'ROTATED_BOUNDS', name,
        `${direction}: transformed face extends beyond the block envelope 0..16.`);
      const normal = normalOf(vertices);
      const key = vertices.map(vertexKey).sort().join('|');
      for (const previous of planes.get(key) || []) {
        const pair = { element:index, face:direction, previousElement:previous.index, previousFace:previous.direction };
        const sameNormal = normal.every((value,axis) => near(value,previous.normal[axis]));
        if (sameNormal) {
          stats.duplicateFaces.push(pair);
          issue('error','DUPLICATE_FACE',name,`${direction} duplicates outward face ${previous.direction} of element ${previous.index}.`);
        } else {
          stats.sharedInternalFaces.push(pair);
          issue('warning','SHARED_INTERNAL_FACE',name,`${direction} coincides with opposite face ${previous.direction} of element ${previous.index}; inspect/removing internal faces may reduce overdraw.`);
        }
      }
      const entries = planes.get(key) || [];
      entries.push({index,direction,normal});
      planes.set(key,entries);
    }
  }
  stats.triangles = stats.quads * 2;
  stats.textures = [...textureIds].sort();
  models.push(stats);
}

section('Inactive, active and item JSON inheritance contract', () => {
  const inactive = model(inactiveId), active = model(activeId), item = model(itemId);
  assert(sourceDiff.allowedChanged.includes(inactivePath), 'PROTOTYPE_NOT_APPLIED', inactivePath,
    'Inactive model is unchanged from baseline; this is a prototype check, not a baseline acceptance.');
  assert(active.declared.parent === inactiveId, 'ACTIVE_PARENT', activePath,
    'Active model must inherit the inactive model directly.');
  assert(Object.keys(active.declared).every(key => ['parent','textures'].includes(key)), 'ACTIVE_GEOMETRY_OVERRIDE', activePath,
    'Active model may only declare parent and textures; geometry/display must be inherited.');
  assert(object(active.declared.textures) && Object.keys(active.declared.textures).length > 0
    && Object.keys(active.declared.textures).every(key => ['mesh','lamp'].includes(key)), 'ACTIVE_TEXTURE_OVERRIDE', activePath,
    'Active model must override only mesh and/or lamp texture keys.');
  assert(JSON.stringify(active.effective.elements) === JSON.stringify(inactive.effective.elements), 'ACTIVE_GEOMETRY_MISMATCH', activePath,
    'Active/inactive effective geometry must be identical.');
  assert(item.declared.parent === inactiveId, 'ITEM_PARENT', itemPath,
    'Item must inherit the inactive machine model.');
  assert(!Object.hasOwn(item.declared,'elements'), 'ITEM_GEOMETRY_OVERRIDE', itemPath,
    'Item geometry should inherit the same machine model.');
});
for (const id of [inactiveId,activeId,itemId]) section(`Geometry / UV / references / display: ${id}`, () => validateModel(id));

section('Unchanged facing/active blockstate mapping', () => {
  const bytes = fs.readFileSync(path.join(root,blockstatePath));
  assert(baseline.has(blockstatePath) && hash(bytes) === baseline.get(blockstatePath), 'BLOCKSTATE_CHANGED', blockstatePath,
    'Blockstate bytes must match baseline exactly.');
  const state = parse(bytes.toString('utf8'),blockstatePath);
  assert(object(state.variants) && Object.keys(state.variants).length === 8 && !state.multipart, 'BLOCKSTATE_COUNT', blockstatePath,
    'Exactly eight facing/active variants are required.');
  const seen = new Set();
  for (const [key, entry] of Object.entries(state.variants || {})) {
    const properties = Object.fromEntries(key.split(',').map(part => part.split('=')));
    const facing = properties.facing, active = properties.active;
    const expectedRotation = {north:0,east:90,south:180,west:270}[facing];
    const identity = `${facing}/${active}`;
    assert(key.split(',').length === 2 && Object.keys(properties).length === 2
      && expectedRotation !== undefined && ['false','true'].includes(active) && !seen.has(identity),
      'BLOCKSTATE_VARIANT', blockstatePath, `Unexpected or repeated variant ${key}`);
    seen.add(identity);
    assert(object(entry) && entry.model === (active === 'true' ? activeId : inactiveId)
      && (entry.y ?? 0) === expectedRotation && (entry.x ?? 0) === 0, 'BLOCKSTATE_MODEL', blockstatePath,
      `Model/rotation mismatch in ${key}`);
  }
});

const limitations = [
  'Hash comparison covers all existing/additional/removed src files against the pinned pre-existing dirty-tree baseline. Files outside src, runtime resource packs, mods, build settings and already existing gameplay defects are not certified.',
  'FilterRegenerationScreen.java is the sole allowed Java change. Hash allowlisting cannot prove that an arbitrary edit to that screen is purely visual; compilation and human review of its diff remain necessary.',
  'Seven display contexts are checked after JSON inheritance. Head is an optional eighth context. This verifies presence and finite/nonzero transforms, not actual in-game size or visual quality.',
  'Quads and triangles are declared effective faces before runtime culling (triangles = quads * 2), not GPU draw calls or measured performance.',
  'Duplicate-face check finds identical four-vertex polygons, including supported element rotations. It does not detect partial coplanar overlaps, cuboid intersections or near-coplanar depth fighting; opposite internal coincident faces are reported as warnings.',
  'PNG signatures and resource references are checked; this does not decode texture pixels, assess atlas UV seams, texture resolution quality or effective resource-pack overrides.',
  'Vanilla resources are read from the local 1.20.1 client jar. Missing archives or unresolved external resources fail rather than being silently assumed valid.',
  'No Minecraft launch, screenshots, multiplayer test, chunk-reload test, Embeddium test, interaction test or performance benchmark is performed by this script.'
];
const errors = issues.filter(value => value.severity === 'error');
const warnings = issues.filter(value => value.severity === 'warning');
const report = { generatedAt: new Date().toISOString(), root, passed: errors.length === 0,
  baseline: { path:slash(path.relative(root,baselinePath)), pinnedSha256:BASELINE_SHA256, files:baseline.size },
  allowlist:[...allowed], vanillaArchive:vanillaPath, sourceDiff, checks, models,
  references:[...references.values()], issues, limitations };
fs.mkdirSync(reportDir,{recursive:true});
fs.writeFileSync(path.join(reportDir,'prototype_verification.json'),JSON.stringify(report,null,2)+'\n','utf8');
const cell = value => String(value).replaceAll('|','\\|').replaceAll('\n',' ');
const lines = [
  '# Проверка визуального эталона', '',
  `Результат: **${report.passed ? 'PASS' : 'FAIL'}**. Ошибок: ${errors.length}; предупреждений: ${warnings.length}.`, '',
  `Источник: неизменяемый снимок текущего рабочего дерева до прототипа (${baseline.size} файлов src), а не Git HEAD.`,
  `SHA256 baseline: \`${BASELINE_SHA256}\`.`, '',
  `Совпадают: ${sourceDiff.unchanged}; изменены: ${sourceDiff.changed.length}; добавлены: ${sourceDiff.added.length}; удалены: ${sourceDiff.removed.length}.`, '',
  '## Разрешённые изменения', '', ...[...allowed].map(filename=>`- \`${filename}\``), '',
  '## Выполненные проверки', '', ...checks.map(check=>`- ${check.passed?'PASS':'FAIL'} — ${check.name}`), '',
  '## Геометрия до runtime culling', '',
  '| Model | Cuboids | Quads | Triangles | Duplicate faces | Shared internal faces |',
  '|---|---:|---:|---:|---:|---:|',
  ...models.map(value=>`| ${value.id} | ${value.cuboids} | ${value.quads} | ${value.triangles} | ${value.duplicateFaces.length} | ${value.sharedInternalFaces.length} |`), '',
  '## Замечания', '',
  ...(issues.length ? ['| Severity | Code | File | Details |','|---|---|---|---|',
    ...issues.map(value=>`| ${cell(value.severity)} | ${cell(value.code)} | ${cell(value.file)} | ${cell(value.message)} |`)]
    : ['Статических нарушений заявленного контракта не обнаружено.']), '',
  '## Ограничения', '', ...limitations.map(value=>`- ${value}`), '',
  'Полный отчёт с эффективными display transforms, parent chains и хешами ссылочных ресурсов: `prototype_verification.json`.', ''
];
fs.writeFileSync(path.join(reportDir,'prototype_verification.md'),lines.join('\n'),'utf8');
console.log(JSON.stringify({ passed:report.passed, errors:errors.length, warnings:warnings.length,
  unchanged:sourceDiff.unchanged, changed:sourceDiff.changed, added:sourceDiff.added, removed:sourceDiff.removed,
  geometry:models.map(({id,cuboids,quads,triangles})=>({id,cuboids,quads,triangles})),
  reports:['dev/visual_overhaul/prototype_verification.json','dev/visual_overhaul/prototype_verification.md'] },null,2));
process.exitCode = report.passed ? 0 : 1;
