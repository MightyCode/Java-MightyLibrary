#!/usr/bin/env python3
"""Measure the CPU time of BatchStress2D (the acceptance scene of the 2D batch) against fixed thresholds.

Usage:
    python tools/stress.py                     # every case, about 1 minute per game launch
    python tools/stress.py --case rect-16k     # only the cases whose name contains one of these words
    python tools/stress.py --json              # one JSON line per case instead of the table
    python tools/stress.py --list              # the cases and their thresholds, without launching the game
    python tools/stress.py --no-compile        # skip the compilation of scenario.py

Each case is a mode of the scene (F cycles rectangles -> shapes -> mix), a number of objects N (Page Up doubles the 1 000
of the start), a view (everything visible, or ~10 % visible with the main 2D camera zoomed x3.2) and a sort (O:
by state). The game runs with the vsync off and the batch in mode `orphan`; after a warm-up, 3 samples of `stats`
are taken 1.2 s apart and the median is compared with the thresholds of the case: renderCpuMs (CPU time of the
rendering per frame) and, when known, drawCallsPerFrame. The cases of one game launch share the mode, N and view
(sorted after unsorted). The thresholds are those of the user's machine: on another machine, only the draw calls
mean something. Exit code: 0 if every case passes, 1 otherwise. A window of the game opens for each launch.
"""
import argparse
import json
import os
import statistics
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import scenario  # noqa: E402

KEYS = ["frameMs", "renderCpuMs", "drawCallsPerFrame", "batchFlushesPerFrame"]
STATS = "?stats " + " ".join(KEYS)
SAMPLES = 3
START_COUNT = 1000
ZOOM_10_PERCENT = 3.2
# Presses of F from the start of the scene for each mode
MODES = {"rectangles": 0, "shapes": 1, "mix": 2}

# name, mode, doublings of N, ~10 % visible, sorted by state, max renderCpuMs, max drawCallsPerFrame (None: measured,
# not checked). Thresholds: measures of 2026-11-26 (median of 3 runs) + ~15 %; the 10 % cases have no bound of draw
# calls (they vary with the view: ~781 and ~6 350 calls)
CASES = [
    ("rect-16k", "rectangles", 4, False, False, 2.5, 8002),
    ("rect-16k-sorted", "rectangles", 4, False, True, 2.2, 4),
    ("rect-16k-10%", "rectangles", 4, True, False, 1.3, None),
    ("rect-128k", "rectangles", 7, False, False, 38.0, 64002),
    ("rect-128k-10%", "rectangles", 7, True, False, 25.0, None),
    ("shapes-16k", "shapes", 4, False, False, 5.7, 3),
    ("shapes-16k-10%", "shapes", 4, True, False, 2.6, 3),
    ("shapes-16k-sorted", "shapes", 4, False, True, 6.2, 3),
    ("mix-16k", "mix", 4, False, False, None, None),
    ("mix-16k-sorted", "mix", 4, False, True, None, None),
    ("mix-16k-10%", "mix", 4, True, False, None, None),
    ("mix-16k-10%-sorted", "mix", 4, True, True, None, None),
]


def case_dict(case):
    name, mode, doublings, zoomed, sort, max_ms, max_calls = case
    return {"case": name, "mode": mode, "n": START_COUNT << doublings, "visible": "10%" if zoomed else "all",
            "sorted": sort, "maxRenderCpuMs": max_ms, "maxDrawCalls": max_calls}


def launch_steps(mode, doublings, zoomed, sorts):
    steps = ["scene BatchStress2D", "wait-loaded", "window vsync off", "batch mode orphan"]
    for _ in range(MODES[mode]):
        steps += ["input key F", "sleep:0.3"]
    for _ in range(doublings):
        steps += ["input key PAGE_UP", "sleep:0.2"]
    if zoomed:
        steps += ["camera main2D zoom %s" % ZOOM_10_PERCENT]
    steps.append("sleep:2")
    sorted_now = False
    for sort in sorts:
        if sort != sorted_now:
            steps += ["input key O", "sleep:1"]
            sorted_now = sort
        for _ in range(SAMPLES):
            steps += ["sleep:1.2", STATS]
    return steps


def stats_lines(output):
    found = []
    for line in output.splitlines():
        start = line.find('{"')
        if start >= 0 and '"renderCpuMs"' in line:
            found.append(json.loads(line[start:line.rfind("}") + 1]))
    return found


def run_launch(cases, compile_sources):
    _, mode, doublings, zoomed, _, _, _ = cases[0]
    sorts = [case[4] for case in cases]
    command = [sys.executable, os.path.join(scenario.ROOT, "tools", "scenario.py")]
    if not compile_sources:
        command.append("--no-compile")
    command += launch_steps(mode, doublings, zoomed, sorts)
    result = subprocess.run(command, cwd=scenario.ROOT, capture_output=True, text=True, encoding="utf-8",
                            errors="replace", timeout=900)
    found = stats_lines(result.stdout)
    if len(found) != SAMPLES * len(cases):
        print(result.stdout[-3000:] + result.stderr[-1000:])
        raise SystemExit("expected %d samples of stats, got %d" % (SAMPLES * len(cases), len(found)))
    rows = []
    for index, case in enumerate(cases):
        part = found[index * SAMPLES:(index + 1) * SAMPLES]
        row = case_dict(case)
        for key in KEYS:
            row[key] = statistics.median(sample.get(key, 0) for sample in part)
        row["samplesRenderCpuMs"] = [sample.get("renderCpuMs", 0) for sample in part]
        passed = row["maxRenderCpuMs"] is None or row["renderCpuMs"] <= row["maxRenderCpuMs"]
        if row["maxDrawCalls"] is not None:
            passed = passed and row["drawCallsPerFrame"] <= row["maxDrawCalls"]
        row["pass"] = passed
        rows.append(row)
    return rows


def print_table(rows):
    print(f"{'case':18} {'n':>7} {'cpu ms':>7} {'max':>6} {'calls':>7} {'max':>6} {'frame ms':>8}  result")
    for row in rows:
        max_ms = "" if row["maxRenderCpuMs"] is None else f"{row['maxRenderCpuMs']:.1f}"
        max_calls = "" if row["maxDrawCalls"] is None else row["maxDrawCalls"]
        checked = row["maxRenderCpuMs"] is not None or row["maxDrawCalls"] is not None
        print(f"{row['case']:18} {row['n']:7} {row['renderCpuMs']:7.2f} {max_ms:>6} "
              f"{row['drawCallsPerFrame']:7.0f} {max_calls:>6} {row['frameMs']:8.2f}  "
              + (("PASS" if row["pass"] else "FAIL") if checked else "-"))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--case", nargs="+", help="only the cases whose name contains one of these words")
    parser.add_argument("--json", action="store_true", help="one JSON line per case")
    parser.add_argument("--list", action="store_true", help="print the cases and their thresholds, run nothing")
    parser.add_argument("--no-compile", action="store_true", help="skip the compilation")
    args = parser.parse_args()

    cases = [case for case in CASES if not args.case or any(word in case[0] for word in args.case)]
    if not cases:
        print("no case matches; the cases: " + ", ".join(case[0] for case in CASES))
        return 1
    if args.list:
        for case in cases:
            print(json.dumps(case_dict(case)))
        return 0

    # One game launch per mode, N and view, unsorted cases first
    launches = {}
    for case in cases:
        launches.setdefault((case[1], case[2], case[3]), []).append(case)
    rows = []
    compile_sources = not args.no_compile
    for group in launches.values():
        group.sort(key=lambda case: case[4])
        group_rows = run_launch(group, compile_sources)
        compile_sources = False
        rows += group_rows
        if args.json:
            for row in group_rows:
                print(json.dumps(row))
            sys.stdout.flush()

    if not args.json:
        print_table(rows)
    return 0 if all(row["pass"] for row in rows) else 1


if __name__ == "__main__":
    sys.exit(main())
