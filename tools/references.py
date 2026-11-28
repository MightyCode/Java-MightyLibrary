#!/usr/bin/env python3
"""Reference images of the image tests (ReferenceImages of the test kit, ReferenceImagesGLTest of the demo).

Usage:
    python tools/references.py --key                    # the key of the series of this machine (runs GLContextTest)
    python tools/references.py --update                 # the captures of this machine become its series, then git status
    python tools/references.py --update --test Class    # the same for one test class of the demo
    python tools/references.py --from-ci [RUN_ID]       # the series of the CI (linux-mesa-llvmpipe, any processor of
                                                        # its runner) from the artifact "references" of the last
                                                        # failed run of the current branch
A window opens hidden for the tests. Read the changed images (git diff) before committing them.
Machine specific paths: see tools/scenario.py.
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import scenario  # noqa: E402

ROOT = scenario.ROOT
SERIES = os.path.join(ROOT, "demo", "src", "test", "resources", "references")
# The series of the CI: llvmpipe, one series for the Intel and AMD processors of the runners (RendererKey)
CI_KEY = "linux-mesa-llvmpipe"
ARTIFACT = "references"
DEFAULT_TEST = "ReferenceImagesGLTest"
KEY_LINE = re.compile(r"^--Reference key: (\S+)", re.MULTILINE)
REFERENCE_LINE = re.compile(r"^--Reference (updated|unchanged) .*$", re.MULTILINE)


def maven(*arguments):
    environment = dict(os.environ)
    if scenario.MAVEN_JDK:
        environment["JAVA_HOME"] = scenario.MAVEN_JDK
    result = subprocess.run([scenario.MVN, "-B", "-q", *arguments], cwd=ROOT, env=environment, capture_output=True,
                            text=True, encoding="utf-8", errors="replace")
    output = result.stdout + result.stderr
    if result.returncode != 0:
        print("Maven failed:\n" + output[-3000:])
        sys.exit(1)
    return output


def key():
    output = maven("test", "-pl", "mightylib-test", "-am", "-Dtest=GLContextTest",
                   "-Dsurefire.failIfNoSpecifiedTests=false")
    found = KEY_LINE.search(output)
    if not found:
        print("No key printed: GLContextTest skipped (no OpenGL here?)")
        sys.exit(1)
    print(found.group(1))


def update(test):
    output = maven("test", "-pl", "demo", "-am", "-Dtest=" + test, "-Dsurefire.failIfNoSpecifiedTests=false",
                   "-Dmightylib.test.updateReferences=true")
    lines = [line.group(0) for line in REFERENCE_LINE.finditer(output)]
    for line in lines:
        print(line)
    if not lines:
        print("No reference written: no test of " + test + " compared an image")
    git_status()


def git_status():
    status = subprocess.run(["git", "status", "--short", "--", os.path.relpath(SERIES, ROOT)], cwd=ROOT,
                            capture_output=True, text=True)
    print(status.stdout.strip() or "References unchanged in git")


def gh(*arguments):
    try:
        result = subprocess.run(["gh", *arguments], cwd=ROOT, capture_output=True, text=True)
    except FileNotFoundError:
        print("gh (GitHub CLI) not found")
        sys.exit(1)
    if result.returncode != 0:
        print("gh " + " ".join(arguments) + " failed:\n" + result.stderr.strip())
        sys.exit(1)
    return result.stdout.strip()


def last_failed_run():
    branch = subprocess.run(["git", "branch", "--show-current"], cwd=ROOT, capture_output=True,
                            text=True).stdout.strip()
    run = gh("run", "list", "--branch", branch, "--status", "failure", "--limit", "1", "--json", "databaseId",
             "--jq", ".[0].databaseId")
    if not run:
        print("No failed run of the CI on the branch " + branch)
        sys.exit(1)
    return run


def from_ci(run):
    run = run or last_failed_run()
    with tempfile.TemporaryDirectory(prefix="mightylib-references-") as folder:
        gh("run", "download", run, "--name", ARTIFACT, "--dir", folder)
        copied = 0
        # The artifact holds a folder per key: the series of the CI only
        for key in sorted(os.listdir(folder)):
            source = os.path.join(folder, key)
            if not os.path.isdir(source) or key != CI_KEY:
                continue
            target = os.path.join(SERIES, key)
            for name in sorted(os.listdir(source)):
                if not name.endswith(".actual.png"):
                    continue
                os.makedirs(target, exist_ok=True)
                reference = name[:-len(".actual.png")] + ".png"
                shutil.copyfile(os.path.join(source, name), os.path.join(target, reference))
                print("--Reference from the CI " + key + "/" + reference)
                copied += 1
    if not copied:
        print("No capture of the series " + CI_KEY + " in the artifact " + ARTIFACT + " of the run " + run)
        sys.exit(1)
    git_status()


def main():
    parser = argparse.ArgumentParser(description="Reference images of the image tests")
    actions = parser.add_mutually_exclusive_group(required=True)
    actions.add_argument("--key", action="store_true", help="print the key of the series of this machine")
    actions.add_argument("--update", action="store_true", help="the captures of this machine become its series")
    actions.add_argument("--from-ci", nargs="?", const="", metavar="RUN_ID",
                         help="the series " + CI_KEY + " from the artifact of a failed run of the CI")
    parser.add_argument("--test", default=DEFAULT_TEST, help="test class of the demo for --update")
    arguments = parser.parse_args()

    if arguments.key:
        key()
    elif arguments.update:
        update(arguments.test)
    else:
        from_ci(arguments.from_ci)


if __name__ == "__main__":
    main()
