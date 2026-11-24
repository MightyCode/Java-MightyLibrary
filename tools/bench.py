#!/usr/bin/env python3
"""Run the CPU benches of the 2D batch and print a table of the ns per draw.

Usage:
    python tools/bench.py              # both benches, about 1 minute
    python tools/bench.py --no-gl      # only the batch without OpenGL (DrawBatch2DBenchTest)
    python tools/bench.py --isolated   # one JVM per case of the display bench (a JIT profile of one kind of
                                       # renderer, like a scene full of rectangles), about 1 minute more
    python tools/bench.py --repeat 3   # each run 3 times, the median of the runs per case (JIT varies by JVM)
    python tools/bench.py --json       # the JSON lines instead of the table

The benches are JUnit tests (DrawBatch2DBenchTest in mightylib, Batch2DCostBenchTest in mightylib-test, a hidden
window) enabled only by -Dmightylib.bench=true: `mvn test` does not run them. They measure, they check nothing.
The numbers of a case depend a little on the cases run before it in the same JVM (the JIT inlines the display() of
the kinds seen so far): --isolated gives each case a fresh JVM. A JVM also differs from the next one by its JIT
decisions (+-20 % on some cases): compare with --isolated --repeat 3. Each line: the case, the draws per pass, the ns
per draw (median of the passes after a warm-up); `send` = the ns per draw of the end of the view (sort, upload, draw
calls), apart from the display() calls.
"""
import argparse
import json
import os
import re
import statistics
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import scenario  # noqa: E402


GL_BENCH = os.path.join(scenario.ROOT, "mightylib-test", "src", "test", "java", "io", "github", "mightycode",
                        "mightylib", "test", "Batch2DCostBenchTest.java")


def run_maven(modules, tests):
    environment = dict(os.environ)
    if scenario.MAVEN_JDK:
        environment["JAVA_HOME"] = scenario.MAVEN_JDK
    result = subprocess.run([scenario.MVN, "-o", "-q", "test", "-pl", modules, "-am", "-Dtest=" + tests,
                             "-Dsurefire.failIfNoSpecifiedTests=false", "-Dmightylib.bench=true"],
                            cwd=scenario.ROOT, env=environment, capture_output=True, text=True, encoding="utf-8",
                            errors="replace")
    output = result.stdout + result.stderr
    rows = []
    for line in output.splitlines():
        start = line.find('{"bench"')
        if start >= 0:
            rows.append(json.loads(line[start:]))
    return result.returncode, rows, output


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--no-gl", action="store_true", help="only the bench without OpenGL")
    parser.add_argument("--isolated", action="store_true", help="one JVM per case of the display bench")
    parser.add_argument("--repeat", type=int, default=1, help="runs of each bench, median per case (default 1)")
    parser.add_argument("--json", action="store_true", help="print the JSON lines")
    args = parser.parse_args()

    runs = [("mightylib", "DrawBatch2DBenchTest")]
    if not args.no_gl:
        if args.isolated:
            with open(GL_BENCH, encoding="utf-8") as file:
                methods = re.findall(r"void (bench\w+)\(SceneRunner", file.read())
            runs += [("mightylib,mightylib-test", "Batch2DCostBenchTest#" + method) for method in methods]
        else:
            runs.append(("mightylib,mightylib-test", "Batch2DCostBenchTest"))

    by_case = {}
    for _ in range(max(1, args.repeat)):
        for modules, tests in runs:
            code, found, output = run_maven(modules, tests)
            if code != 0 or not found:
                print(output[-3000:])
                print(f"bench failed: {tests}" if code != 0 else f"no result: {tests}")
                return 1
            for row in found:
                by_case.setdefault((row["bench"], row["case"]), []).append(row)

    # The median of the runs of each case, in the order of the first run
    rows = []
    for runs_of_case in by_case.values():
        row = dict(runs_of_case[0])
        row["nsPerDraw"] = statistics.median(r["nsPerDraw"] for r in runs_of_case)
        row["sendNsPerDraw"] = statistics.median(r["sendNsPerDraw"] for r in runs_of_case)
        row["runs"] = [r["nsPerDraw"] for r in runs_of_case]
        rows.append(row)

    if args.json:
        for row in rows:
            print(json.dumps(row))
        return 0

    print(f"{'case':40} {'n':>6} {'ns/draw':>9} {'send ns':>8} {'drawn':>6} {'culled':>6}"
          + ("  runs" if args.repeat > 1 else ""))
    for row in rows:
        runs_text = "  " + " ".join(f"{value:.0f}" for value in row["runs"]) if args.repeat > 1 else ""
        print(f"{row['bench'] + ': ' + row['case']:40} {row['n']:6} {row['nsPerDraw']:9.1f} "
              f"{row['sendNsPerDraw']:8.1f} {row.get('drawn', ''):>6} {row.get('culled', ''):>6}" + runs_text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
