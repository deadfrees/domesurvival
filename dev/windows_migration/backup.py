"""Verified local Windows migration snapshot; never deletes source or destination files."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import shutil
import sqlite3
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]
USER = Path.home()
CODEX_SKIP = {'.sandbox', '.sandbox-bin', '.sandbox-secrets', '.tmp', 'tmp',
              'browser', 'node_repl', 'thread-writer-locks', 'cache'}
CODEX_FILES_SKIP = {'auth.json', 'cap_sid', '.sqlite-maintenance.lock'}


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def inventory(source, label):
    result, skipped, links = [], [], []
    for base, dirs, files in os.walk(source, followlinks=True):
        for name in list(dirs):
            p = Path(base) / name
            if label == 'codex-home' and name in CODEX_SKIP:
                dirs.remove(name)
                skipped.append(str(p))
                continue
            if p.is_symlink() or p.is_junction():
                target = p.resolve()
                if target in [Path(base).resolve(), *Path(base).resolve().parents]:
                    raise RuntimeError(f'Circular directory link: {p}')
                links.append({'link': str(p), 'target': str(target), 'action': 'materialized'})
        for name in files:
            p = Path(base) / name
            if label == 'codex-home' and (name in CODEX_FILES_SKIP or
                    name.endswith(('-wal', '-shm', '.lock', '.guard')) or name.startswith('..codex-global-state')):
                skipped.append(str(p))
                continue
            if p.is_symlink():
                links.append({'link': str(p), 'target': str(p.resolve()), 'action': 'materialized'})
            result.append((p, Path(label) / p.relative_to(source), label == 'codex-home'))
    return result, skipped, links


def copy_verified(job, destination):
    source, relative, live = job
    target = destination / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    before = source.stat()
    if live and source.suffix in ('.sqlite', '.db'):
        with sqlite3.connect(source.as_uri() + '?mode=ro', uri=True, timeout=30) as original:
            with sqlite3.connect(target) as snapshot:
                deadline = time.monotonic() + 120
                def progress(status, remaining, total):
                    if time.monotonic() > deadline:
                        raise TimeoutError(f'SQLite snapshot timed out: {source.name}')
                original.backup(snapshot, pages=1024, progress=progress)
                check = snapshot.execute('PRAGMA quick_check').fetchone()[0]
                if check != 'ok':
                    raise RuntimeError(f'SQLite integrity error: {source.name}: {check}')
        return {'path': relative.as_posix(), 'size': target.stat().st_size,
                'sha256': sha(target), 'method': 'sqlite_online_backup', 'integrity': 'ok'}
    digest = hashlib.sha256()
    remaining = before.st_size
    with source.open('rb') as reader, target.open('xb') as writer:
        while remaining:
            chunk = reader.read(min(2 * 1024 * 1024, remaining))
            if not chunk:
                raise RuntimeError(f'Source shortened during backup: {source}')
            writer.write(chunk)
            digest.update(chunk)
            remaining -= len(chunk)
    after = source.stat()
    if not live and (before.st_size, before.st_mtime_ns) != (after.st_size, after.st_mtime_ns):
        raise RuntimeError(f'Source changed during backup: {source}')
    os.utime(target, ns=(before.st_atime_ns, before.st_mtime_ns))
    value = digest.hexdigest()
    if sha(target) != value:
        raise RuntimeError(f'Hash mismatch: {target}')
    return {'path': relative.as_posix(), 'size': before.st_size, 'sha256': value,
            'method': 'live_file_snapshot' if live else 'copy_verified'}


def verify(destination):
    manifest = json.loads((destination / 'manifest.json').read_text(encoding='utf8'))
    errors = []
    for index, record in enumerate(manifest['files'], 1):
        p = destination / record['path']
        if not p.is_file() or p.stat().st_size != record['size'] or sha(p) != record['sha256']:
            errors.append(record['path'])
        if index % 5000 == 0:
            print(f'Verified {index}/{len(manifest["files"])}', flush=True)
    print(json.dumps({'verified_files': len(manifest['files']), 'errors': errors}))
    return bool(errors)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('destination', type=Path)
    parser.add_argument('--verify', action='store_true')
    args = parser.parse_args()
    destination = args.destination.resolve()
    if args.verify:
        return verify(destination)
    if destination.drive.upper() != 'F:' or destination == Path('F:/'):
        raise RuntimeError('Backup must use a dedicated subdirectory on F:')
    if destination.exists():
        raise RuntimeError('Refusing to overwrite an existing backup directory')
    sources = [(ROOT, 'project'), (USER / '.gradle', 'toolchain/gradle-home'),
               (Path('C:/Program Files/Java/jdk-17.0.12'), 'toolchain/jdk-17.0.12'),
               (USER / '.codex', 'codex-home')]
    jobs, skipped, links = [], [], []
    for source, label in sources:
        if not source.is_dir():
            raise RuntimeError(f'Required source missing: {source}')
        files, omissions, materialized = inventory(source, label)
        jobs.extend(files)
        skipped.extend(omissions)
        links.extend(materialized)
    total = sum(job[0].stat().st_size for job in jobs)
    if shutil.disk_usage(destination.parent).free < total * 1.2 + 2 * 1024**3:
        raise RuntimeError('Insufficient free space')
    destination.mkdir(parents=True)
    print(f'COPY_START files={len(jobs)} GiB={total / 1024**3:.2f}', flush=True)
    records, errors = [], []
    last_progress = time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        pending = {executor.submit(copy_verified, job, destination): job for job in jobs}
        for future in concurrent.futures.as_completed(pending):
            try:
                records.append(future.result())
            except Exception as error:
                errors.append({'source': str(pending[future][0]), 'error': str(error)})
            if time.monotonic() - last_progress > 15:
                print(f'COPY_PROGRESS {len(records)}/{len(jobs)} errors={len(errors)}', flush=True)
                last_progress = time.monotonic()
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    bundle = destination / 'DomeSurvival-all-branches.bundle'
    if not errors:
        subprocess.run(['git', 'bundle', 'create', str(bundle), '--all'], cwd=ROOT, check=True)
        subprocess.run(['git', 'bundle', 'verify', str(bundle)], cwd=ROOT, check=True,
                       stdout=subprocess.DEVNULL)
        records.append({'path': bundle.name, 'size': bundle.stat().st_size,
                        'sha256': sha(bundle), 'method': 'git_bundle_verified'})
    for name in ('RESTORE.md', 'BUILD.cmd', 'RUN.cmd', 'VERIFY.cmd', 'RESTORE-CODEX.ps1', 'LINK-OLD-PATH.ps1'):
        source = ROOT / 'dev/windows_migration' / name
        records.append(copy_verified((source, Path(name), False), destination))
    manifest = {'created': time.strftime('%Y-%m-%dT%H:%M:%S%z'), 'commit': commit,
                'sources': [str(s) for s, _ in sources], 'old_user_home': str(USER),
                'materialized_links': links, 'excluded_codex_paths': skipped,
                'files': records, 'errors': errors}
    (destination / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf8')
    report = {'complete': not errors, 'commit': commit, 'files': len(records),
              'bytes': sum(r['size'] for r in records), 'errors': errors,
              'sqlite_snapshots': sum(r['method'] == 'sqlite_online_backup' for r in records)}
    (destination / 'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf8')
    print(json.dumps(report, ensure_ascii=False), flush=True)
    return bool(errors)


if __name__ == '__main__':
    raise SystemExit(main())
