#!/usr/bin/env python3
"""Demonstration package: heavy resources of the demo, distributed apart from the repository.

  python tools/demo_assets.py           install the files that are missing or different
  python tools/demo_assets.py --check   list missing or different files, no network (exit 1 if any)
  python tools/demo_assets.py --pack T  build target/demo-assets/<archive> and update the manifest

The manifest tools/demo_assets.json lists the files (paths relative to resources/, with their sha256).
Exit codes: 0 ok, 1 --check found differences, 2 error (message on one line).
"""
import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESOURCES = os.path.join(ROOT, "resources")
MANIFEST = os.path.join(ROOT, "tools", "demo_assets.json")
PACK_DIR = os.path.join(ROOT, "target", "demo-assets")


class AssetError(Exception):
    pass


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def load_manifest():
    with open(MANIFEST, encoding="utf-8") as f:
        return json.load(f)


def save_manifest(manifest):
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        json.dump(manifest, f, indent=2)
        f.write("\n")


def safe_destination(root, name):
    """Absolute destination of a manifest path under root; refuses absolute paths and '..'."""
    if name.startswith("/") or name.startswith("\\") or os.path.isabs(name) or (len(name) > 1 and name[1] == ":"):
        raise AssetError("unsafe path in archive: " + name)
    if ".." in name.replace("\\", "/").split("/"):
        raise AssetError("unsafe path in archive: " + name)
    dest = os.path.abspath(os.path.join(root, *name.split("/")))
    if os.path.commonpath([os.path.abspath(root), dest]) != os.path.abspath(root):
        raise AssetError("unsafe path in archive: " + name)
    return dest


def status(manifest, root=RESOURCES):
    """Returns (missing, different, ok) lists of manifest paths."""
    missing, different, ok = [], [], []
    for name, digest in manifest["files"].items():
        path = safe_destination(root, name)
        if not os.path.isfile(path):
            missing.append(name)
        elif sha256_file(path) != digest:
            different.append(name)
        else:
            ok.append(name)
    return missing, different, ok


def extract(zip_path, manifest, names, root=RESOURCES):
    """Extracts only the listed manifest entries; each file is written beside its destination, checked, renamed."""
    wanted = manifest["files"]
    installed = 0
    with zipfile.ZipFile(zip_path) as z:
        entries = set(z.namelist())
        for entry in entries:
            safe_destination(root, entry)  # any unsafe entry refuses the whole archive
        for name in names:
            if name not in entries:
                raise AssetError("archive does not contain " + name)
            dest = safe_destination(root, name)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            fd, tmp = tempfile.mkstemp(dir=os.path.dirname(dest), suffix=".part")
            try:
                with os.fdopen(fd, "wb") as out, z.open(name) as src:
                    while True:
                        chunk = src.read(1 << 20)
                        if not chunk:
                            break
                        out.write(chunk)
                if sha256_file(tmp) != wanted[name]:
                    raise AssetError("wrong checksum for " + name + ": archive and manifest do not match")
                os.replace(tmp, dest)
            finally:
                if os.path.exists(tmp):
                    os.remove(tmp)
            installed += 1
    return installed


def download(url, expected_sha):
    if not url.startswith("https://"):
        raise AssetError("manifest url must be https: run --pack and publish the release first")
    fd, tmp = tempfile.mkstemp(suffix=".zip")
    os.close(fd)
    try:
        h = hashlib.sha256()
        with urllib.request.urlopen(url, timeout=60) as resp, open(tmp, "wb") as out:
            while True:
                chunk = resp.read(1 << 20)
                if not chunk:
                    break
                h.update(chunk)
                out.write(chunk)
        if h.hexdigest() != expected_sha:
            raise AssetError("downloaded archive has the wrong checksum: retry, or the release was changed")
        return tmp
    except urllib.error.HTTPError as e:
        os.remove(tmp)
        raise AssetError("download failed (HTTP %d) for %s: check that the release exists" % (e.code, url))
    except urllib.error.URLError as e:
        os.remove(tmp)
        raise AssetError("download failed (%s): check the network, then run again" % e.reason)
    except BaseException:
        if os.path.exists(tmp):
            os.remove(tmp)
        raise


def cmd_check(manifest):
    missing, different, ok = status(manifest)
    for n in missing:
        print("missing:   " + n)
    for n in different:
        print("different: " + n)
    if missing or different:
        print("-- demo assets: %d missing, %d different, %d ok" % (len(missing), len(different), len(ok)))
        return 1
    print("-- demo assets: up to date (%d files)" % len(ok))
    return 0


def cmd_install(manifest):
    missing, different, ok = status(manifest)
    todo = missing + different
    if not todo:
        print("-- demo assets: up to date (%d files)" % len(ok))
        return 0
    tmp = download(manifest.get("url", ""), manifest.get("sha256", ""))
    try:
        installed = extract(tmp, manifest, todo)
    finally:
        os.remove(tmp)
    print("-- demo assets: %d installed, %d already there" % (installed, len(ok)))
    return 0


def is_ignored(rel_path):
    r = subprocess.run(["git", "check-ignore", "-q", "resources/" + rel_path], cwd=ROOT)
    return r.returncode == 0


def cmd_pack(manifest, tag):
    names = sorted(manifest["files"])
    for n in names:
        path = safe_destination(RESOURCES, n)
        if not os.path.isfile(path):
            raise AssetError("missing file for the package: resources/" + n)
        if not is_ignored(n):
            raise AssetError("resources/" + n + " is not ignored by git: add it to .gitignore first")
    archive = "mightylib-" + tag + ".zip"
    os.makedirs(PACK_DIR, exist_ok=True)
    out = os.path.join(PACK_DIR, archive)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for n in names:
            info = zipfile.ZipInfo(n, date_time=(2020, 1, 1, 0, 0, 0))  # same bytes for the same content
            info.compress_type = zipfile.ZIP_DEFLATED
            with open(safe_destination(RESOURCES, n), "rb") as f:
                z.writestr(info, f.read())
    manifest["files"] = {n: sha256_file(safe_destination(RESOURCES, n)) for n in names}
    manifest["tag"] = tag
    manifest["archive"] = archive
    manifest["url"] = "https://github.com/MightyCode/Java-MightyLibrary/releases/download/%s/%s" % (tag, archive)
    manifest["sha256"] = sha256_file(out)
    save_manifest(manifest)
    print("-- built %s (%d files, %.1f MB), manifest updated" % (out, len(names), os.path.getsize(out) / 1e6))
    return 0


def main():
    parser = argparse.ArgumentParser(description="Install, check or build the demonstration package.")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--check", action="store_true", help="no network: list missing or different files (exit 1 if any)")
    group.add_argument("--pack", metavar="TAG", help="build the archive (tag like demo-assets-2) and update the manifest")
    args = parser.parse_args()
    try:
        manifest = load_manifest()
        if args.check:
            return cmd_check(manifest)
        if args.pack:
            return cmd_pack(manifest, args.pack)
        return cmd_install(manifest)
    except AssetError as e:
        print("-- demo assets: " + str(e), file=sys.stderr)
        return 2
    except (OSError, zipfile.BadZipFile, json.JSONDecodeError) as e:
        print("-- demo assets: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
