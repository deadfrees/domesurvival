"""Restore a verified Codex snapshot before launching the newly installed app."""
import json
from pathlib import Path
import shutil
import sqlite3
import sys

root = Path(sys.argv[1]).resolve()
manifest = json.loads((root / 'manifest.json').read_text(encoding='utf8'))
source = root / 'codex-home'
target = Path.home() / '.codex'
if target.exists():
    raise SystemExit('Existing .codex found. Close Codex and rename that folder first; nothing was overwritten.')
shutil.copytree(source, target)
old = manifest['old_user_home']
new = str(Path.home())
if old != new:
    for name in ('config.toml', '.codex-global-state.json', '.codex-global-state.json.bak'):
        p = target / name
        if p.is_file():
            text = p.read_text(encoding='utf8')
            for a, b in ((old.replace('\\', '\\\\'), new.replace('\\', '\\\\')),
                         (old.replace('\\', '/'), new.replace('\\', '/')), (old, new)):
                text = text.replace(a, b)
            p.write_text(text, encoding='utf8')
    for p in target.rglob('*'):
        if p.suffix not in ('.sqlite', '.db'):
            continue
        with sqlite3.connect(p) as db:
            tables = [row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")]
            for table in tables:
                quoted = '"' + table.replace('"', '""') + '"'
                for row in db.execute(f'PRAGMA table_info({quoted})').fetchall():
                    column = row[1]
                    if column not in ('cwd', 'path', 'workspace_root', 'workspace_dir') and not column.endswith('_path'):
                        continue
                    field = '"' + column.replace('"', '""') + '"'
                    db.execute(f'UPDATE {quoted} SET {field}=replace({field}, ?, ?) WHERE typeof({field})=\'text\'', (old, new))
            if db.execute('PRAGMA quick_check').fetchone()[0] != 'ok':
                raise RuntimeError(f'Database check failed: {p.name}')
print(f'Codex history/settings restored to {target}. Sign in again after opening the app.')
