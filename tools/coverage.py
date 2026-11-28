#!/usr/bin/env python3
"""Code coverage of the tests in one call: JaCoCo through the Maven profile `coverage`, then a summary per package.

Usage:
    python tools/coverage.py                  # every test of the three modules, GL included (as the CI), engine report
    python tools/coverage.py --quick          # library tests without GL only (about 20 s), report of mightylib
    python tools/coverage.py --classes PART   # classes of the packages whose name contains PART (dotted or slashed)
    python tools/coverage.py --lines CLASS    # uncovered line numbers of a class (simple or full name)
    python tools/coverage.py --no-run         # summarize the last run (with --quick: the last quick run)
    python tools/coverage.py --markdown       # the summary as a Markdown table (the summary of the CI job)
    python tools/coverage.py --check          # fails when the total of the lines is under tools/coverage_floor.json
    python tools/coverage.py --from-ci [RUN]  # the report of the CI (artifact coverage, last run of the branch)
Measured: the code of mightylib and of the test kit, by the tests of the three modules (the code of the demo is not).
The default report is aggregated by the module coverage: coverage/target/site/jacoco-aggregate/index.html; the quick one
is mightylib/target/site/jacoco/index.html. Machine specific paths: see tools/scenario.py.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import scenario  # noqa: E402

ROOT = scenario.ROOT
REPORT = os.path.join(ROOT, "coverage", "target", "site", "jacoco-aggregate", "jacoco.xml")
QUICK_REPORT = os.path.join(ROOT, "mightylib", "target", "site", "jacoco", "jacoco.xml")
CI_FOLDER = os.path.join(ROOT, "target", "coverage-ci")
FLOOR = os.path.join(ROOT, "tools", "coverage_floor.json")
LOG = os.path.join(ROOT, "target", "coverage.log")
ARTIFACT = "coverage"
PREFIX = "io/github/mightycode/mightylib/"


def run_maven(quick):
    environment = dict(os.environ)
    if scenario.MAVEN_JDK:
        environment["JAVA_HOME"] = scenario.MAVEN_JDK
    command = [scenario.MVN, "-B", "-P", "coverage", "test"]
    if quick:
        command += ["-pl", "mightylib", "-DexcludedGroups=gl"]
    result = subprocess.run(command, cwd=ROOT, env=environment, capture_output=True, text=True, encoding="utf-8",
                            errors="replace")
    # The root target/ belongs to no module: Maven does not create it
    os.makedirs(os.path.dirname(LOG), exist_ok=True)
    with open(LOG, "w", encoding="utf-8") as log:
        log.write(result.stdout + result.stderr)
    if result.returncode != 0:
        print("Maven failed (tests red or build error, full log: target/coverage.log):\n"
              + (result.stdout + result.stderr)[-3000:])
        sys.exit(1)


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


def from_ci(run):
    """Downloads the artifact coverage of a run (by default the last completed run of the branch), returns its report."""
    if not run:
        branch = subprocess.run(["git", "branch", "--show-current"], cwd=ROOT, capture_output=True,
                                text=True).stdout.strip()
        run = gh("run", "list", "--branch", branch, "--workflow", "ci.yml", "--status", "completed", "--limit", "1",
                 "--json", "databaseId", "--jq", ".[0].databaseId")
        if not run:
            print("No completed run of the CI on the branch " + branch)
            sys.exit(1)
    shutil.rmtree(CI_FOLDER, ignore_errors=True)
    gh("run", "download", run, "--name", ARTIFACT, "--dir", CI_FOLDER)
    print("== Coverage of the CI run " + run)
    return os.path.join(CI_FOLDER, "jacoco.xml")


def counter(element, kind):
    node = element.find(f"counter[@type='{kind}']")
    if node is None:
        return 0, 0
    covered = int(node.get("covered"))
    return covered, covered + int(node.get("missed"))


def percent(covered, total):
    return f"{100 * covered // total:3d}%" if total else "  - "


def short(name):
    return name[len(PREFIX):] if name.startswith(PREFIX) else ("." if name + "/" == PREFIX else name)


def row(name, element):
    lines, line_total = counter(element, "LINE")
    branches, branch_total = counter(element, "BRANCH")
    return f"  {percent(lines, line_total)} {lines:5d}/{line_total:<5d} lines  " \
           f"{percent(branches, branch_total)} {branches:4d}/{branch_total:<4d} branches  {name}"


def markdown_row(name, element):
    lines, line_total = counter(element, "LINE")
    branches, branch_total = counter(element, "BRANCH")
    return f"| {name} | {percent(lines, line_total).strip()} | {lines}/{line_total} | " \
           f"{percent(branches, branch_total).strip()} | {branches}/{branch_total} |"


def groups(root):
    """(name, element) per module: the groups of an aggregated report, the root itself for the report of a module."""
    found = root.findall("group")
    return [(group.get("name"), group) for group in found] if found else [(root.get("name"), root)]


def read_floor():
    with open(FLOOR, encoding="utf-8") as file:
        return int(json.load(file)["lines"])


def check(root):
    lines, line_total = counter(root, "LINE")
    value = 100 * lines / line_total if line_total else 0.0
    floor = read_floor()
    if value < floor:
        print(f"Coverage of the lines {value:.2f}% under the floor {floor}% (tools/coverage_floor.json)")
        sys.exit(1)
    print(f"Coverage of the lines {value:.2f}%, floor {floor}% (tools/coverage_floor.json)")


def main():
    parser = argparse.ArgumentParser(description="Code coverage (JaCoCo) with a text summary")
    parser.add_argument("--quick", action="store_true", help="library tests without GL only, report of mightylib")
    parser.add_argument("--no-run", action="store_true", help="summarize the last run instead of running the tests")
    parser.add_argument("--from-ci", nargs="?", const="", metavar="RUN",
                        help="summarize the report of the CI (artifact coverage, last completed run of the branch)")
    parser.add_argument("--classes", metavar="PART", help="list the classes of the packages whose name contains PART")
    parser.add_argument("--lines", metavar="CLASS", help="list the uncovered lines of a class")
    parser.add_argument("--markdown", action="store_true", help="the summary per package as a Markdown table")
    parser.add_argument("--check", action="store_true", help="fail when the total of the lines is under the floor")
    arguments = parser.parse_args()

    if arguments.from_ci is not None:
        report = from_ci(arguments.from_ci)
    else:
        report = QUICK_REPORT if arguments.quick else REPORT
        if not arguments.no_run:
            run_maven(arguments.quick)
    if not os.path.exists(report):
        print("No report " + os.path.relpath(report, ROOT) + ": run without --no-run")
        sys.exit(1)
    root = ET.parse(report).getroot()
    modules = groups(root)
    packages = [package for _, group in modules for package in group.findall("package")]

    if arguments.lines:
        wanted = arguments.lines.replace(".", "/")
        for package in packages:
            for source in package.findall("sourcefile"):
                full = package.get("name") + "/" + source.get("name")[:-len(".java")]
                if full == wanted or full.endswith("/" + wanted):
                    missed = [line.get("nr") for line in source.findall("line") if int(line.get("ci")) == 0
                              and int(line.get("mi")) > 0]
                    print(row(short(full), source))
                    print("  uncovered lines: " + (", ".join(missed) if missed else "none"))
        return

    if arguments.classes:
        prefix = arguments.classes.replace(".", "/")
        for package in packages:
            if prefix in package.get("name"):
                for source in package.findall("sourcefile"):
                    print(row(short(package.get("name") + "/" + source.get("name")), source))
        return

    if arguments.markdown:
        print("### Coverage of the engine (packages of " + PREFIX.rstrip("/").replace("/", ".") + ")\n")
        print("| Package | Lines | | Branches | |")
        print("|---|--:|--:|--:|--:|")
        for name, group in modules:
            if len(modules) > 1:
                print(markdown_row(f"**{name}**", group))
            for package in sorted(group.findall("package"), key=lambda p: p.get("name")):
                print(markdown_row(short(package.get("name")), package))
        print(markdown_row("**Total**", root))
        print(f"\nFloor of the lines: {read_floor()}% (tools/coverage_floor.json)")
    elif not arguments.check:
        print("== Coverage per package (" + ("library tests without GL" if arguments.quick and
                                               arguments.from_ci is None else "every test, GL included") + ")")
        for name, group in modules:
            if len(modules) > 1:
                print("-- " + name)
            for package in sorted(group.findall("package"), key=lambda p: p.get("name")):
                print(row(short(package.get("name")), package))
            if len(modules) > 1:
                print(row(name, group))
        print(row("TOTAL", root))
    if arguments.check:
        check(root)


if __name__ == "__main__":
    main()
