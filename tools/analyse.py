#!/usr/bin/env python3
"""Static analysis in one call: Error Prone (inside javac) and SpotBugs (bytecode) through the Maven profile
`analyse`, then a text summary. About 30 seconds; the game's target/classes is not touched.

Usage:
    python tools/analyse.py                          # run both tools, count findings per pattern
    python tools/analyse.py --details ReferenceEquality NP_NULL_ON_SOME_PATH   # file:line | pattern | message
    python tools/analyse.py --details-all            # every finding
    python tools/analyse.py --no-run                 # summarize the output of the last run

Findings the project accepts on purpose are filtered in tools/spotbugs-exclude.xml (SpotBugs) and by the
-Xep:...:OFF options of the profile in pom.xml (Error Prone). Machine specific paths: see tools/scenario.py.
"""
import argparse
import collections
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import scenario  # noqa: E402

ROOT = scenario.ROOT
OUTPUT = os.path.join(ROOT, "target", "static-analysis")
LOG = os.path.join(ROOT, "target", "static-analysis.log")
SPOTBUGS_XML = os.path.join(OUTPUT, "spotbugsXml.xml")
JAVAC_WARNING = re.compile(r"^\[WARNING\] (.+?\.java):\[(\d+),\d+\] (?:\[(\w+)\] )?(.*)$")
# Patterns that match the risk categories of docs/bonnes-pratiques.md (point 11): shown first
RISK = {
    "NP_", "OBL_", "OS_OPEN_STREAM", "ODR_", "VO_VOLATILE", "AT_", "IS2_", "DL_", "RV_", "EC_", "EQ_", "HE_",
    "NonAtomicVolatileUpdate", "EmptyCatch", "EmptyIf", "FallThrough", "EqualsHashCode", "FutureReturnValueIgnored",
    "MissingCasesInEnumSwitch", "StringCaseLocaleUsage", "DefaultCharset", "DM_DEFAULT_ENCODING",
}


def run_maven():
    environment = dict(os.environ)
    if scenario.MAVEN_JDK:
        environment["JAVA_HOME"] = scenario.MAVEN_JDK
    # English messages from SpotBugs
    environment["MAVEN_OPTS"] = (environment.get("MAVEN_OPTS", "") + " -Duser.language=en").strip()
    # A recompilation is forced: javac would otherwise skip the sources and Error Prone would report nothing
    shutil.rmtree(OUTPUT, ignore_errors=True)
    result = subprocess.run([scenario.MVN, "-B", "-P", "analyse", "process-classes"], cwd=ROOT, env=environment,
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    with open(LOG, "w", encoding="utf-8") as log:
        log.write(result.stdout + result.stderr)
    if result.returncode != 0:
        print("Maven failed (full log: target/static-analysis.log):\n" + (result.stdout + result.stderr)[-3000:])
        sys.exit(1)


def relative(path):
    path = path.replace("\\", "/")
    root = ROOT.replace("\\", "/") + "/"
    return path[len(root):] if path.startswith(root) else path


def read_error_prone():
    findings = []
    if not os.path.exists(LOG):
        return findings
    with open(LOG, encoding="utf-8", errors="replace") as log:
        for line in log:
            match = JAVAC_WARNING.match(line.rstrip("\n"))
            if match:
                path, line_number, name, message = match.groups()
                findings.append(("ErrorProne", name or "javac", relative(path), int(line_number), message))
    return findings


def read_spotbugs():
    findings = []
    if not os.path.exists(SPOTBUGS_XML):
        return findings
    for bug in ET.parse(SPOTBUGS_XML).getroot().findall("BugInstance"):
        source = bug.find("SourceLine[@primary='true']")
        if source is None:
            source = bug.find("Class/SourceLine")
        if source is None:
            source = bug.find("Method/SourceLine")
        path = "src/" + source.get("sourcepath") if source is not None else "?"
        line = int(source.get("start", 0)) if source is not None else 0
        findings.append(("SpotBugs", bug.get("type"), path, line, bug.findtext("LongMessage", "")))
    return findings


def is_risk(name):
    return any(name.startswith(prefix) for prefix in RISK)


def zone(path):
    if "/mightylibdemo/" in path:
        return "demo"
    if "/mightylib/" in path:
        return "lib"
    return "other"


def summarize(findings):
    for tool in ("SpotBugs", "ErrorProne"):
        selected = [f for f in findings if f[0] == tool]
        print(f"== {tool}: {len(selected)} findings (risk patterns marked *)")
        counts = collections.Counter(f[1] for f in selected)
        for name, count in sorted(counts.items(), key=lambda item: (not is_risk(item[0]), -item[1], item[0])):
            zones = collections.Counter(zone(f[2]) for f in selected if f[1] == name)
            where = " ".join(f"{z}:{n}" for z, n in sorted(zones.items()))
            print(f"  {'*' if is_risk(name) else ' '} {count:4d}  {name}  ({where})")


def main():
    parser = argparse.ArgumentParser(description="Static analysis (Error Prone + SpotBugs) with a text summary")
    parser.add_argument("--no-run", action="store_true", help="summarize the last run instead of running Maven")
    parser.add_argument("--details", nargs="+", metavar="PATTERN",
                        help="list findings whose pattern starts with one of these names")
    parser.add_argument("--details-all", action="store_true", help="list every finding")
    arguments = parser.parse_args()

    if not arguments.no_run:
        run_maven()
    findings = read_spotbugs() + read_error_prone()
    if not findings and arguments.no_run:
        print("No previous result: run without --no-run")
        return
    summarize(findings)
    if arguments.details or arguments.details_all:
        print()
        for tool, name, path, line, message in sorted(findings, key=lambda f: (f[2], f[3])):
            if arguments.details_all or any(name.startswith(prefix) for prefix in arguments.details):
                print(f"{path}:{line} | {tool} {name} | {message.splitlines()[0][:160] if message else ''}")


if __name__ == "__main__":
    main()
