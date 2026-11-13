#!/usr/bin/env python3
"""Code coverage of the JUnit tests in one call: JaCoCo through the Maven profile `coverage`, then a text summary.
Only logic tested without OpenGL is covered: read the numbers per package, not as one global percentage.

Usage:
    python tools/coverage.py                         # run the tests, coverage per package (lines, branches)
    python tools/coverage.py --classes PART          # classes of the packages whose name contains PART (dotted or slashed)
    python tools/coverage.py --lines CLASS           # uncovered line numbers of a class (simple or full name)
    python tools/coverage.py --no-run                # summarize the last run
The HTML report is target/site/jacoco/index.html. Machine specific paths: see tools/scenario.py.
"""
import argparse
import os
import subprocess
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import scenario  # noqa: E402

ROOT = scenario.ROOT
REPORT = os.path.join(ROOT, "target", "site", "jacoco", "jacoco.xml")
LOG = os.path.join(ROOT, "target", "coverage.log")


def run_maven():
    environment = dict(os.environ)
    if scenario.MAVEN_JDK:
        environment["JAVA_HOME"] = scenario.MAVEN_JDK
    result = subprocess.run([scenario.MVN, "-B", "-P", "coverage", "test"], cwd=ROOT, env=environment,
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    with open(LOG, "w", encoding="utf-8") as log:
        log.write(result.stdout + result.stderr)
    if result.returncode != 0:
        print("Maven failed (tests red or build error, full log: target/coverage.log):\n"
              + (result.stdout + result.stderr)[-3000:])
        sys.exit(1)


def counter(element, kind):
    node = element.find(f"counter[@type='{kind}']")
    if node is None:
        return 0, 0
    covered = int(node.get("covered"))
    return covered, covered + int(node.get("missed"))


def percent(covered, total):
    return f"{100 * covered // total:3d}%" if total else "  - "


def row(name, element):
    lines, line_total = counter(element, "LINE")
    branches, branch_total = counter(element, "BRANCH")
    return f"  {percent(lines, line_total)} {lines:5d}/{line_total:<5d} lines  " \
           f"{percent(branches, branch_total)} {branches:4d}/{branch_total:<4d} branches  {name}"


def main():
    parser = argparse.ArgumentParser(description="JUnit code coverage (JaCoCo) with a text summary")
    parser.add_argument("--no-run", action="store_true", help="summarize the last run instead of running the tests")
    parser.add_argument("--classes", metavar="PART", help="list the classes of the packages whose name contains PART")
    parser.add_argument("--lines", metavar="CLASS", help="list the uncovered lines of a class")
    arguments = parser.parse_args()

    if not arguments.no_run:
        run_maven()
    if not os.path.exists(REPORT):
        print("No previous result: run without --no-run")
        return
    root = ET.parse(REPORT).getroot()
    packages = root.findall("package")

    if arguments.lines:
        wanted = arguments.lines.replace(".", "/")
        for package in packages:
            for source in package.findall("sourcefile"):
                full = package.get("name") + "/" + source.get("name")[:-len(".java")]
                if full == wanted or full.endswith("/" + wanted):
                    missed = [line.get("nr") for line in source.findall("line") if int(line.get("ci")) == 0
                              and int(line.get("mi")) > 0]
                    print(row(full, source))
                    print("  uncovered lines: " + (", ".join(missed) if missed else "none"))
        return

    if arguments.classes:
        prefix = arguments.classes.replace(".", "/")
        for package in packages:
            if prefix in package.get("name"):
                for source in package.findall("sourcefile"):
                    print(row(package.get("name") + "/" + source.get("name"), source))
        return

    print("== Coverage per package (tests without OpenGL)")
    for package in sorted(packages, key=lambda p: p.get("name")):
        print(row(package.get("name"), package))
    print(row("TOTAL", root))


if __name__ == "__main__":
    main()
