#!/usr/bin/env python3
"""Summarize a Java Flight Recorder file in text: hottest methods of the main thread, allocations, frame times.

Usage:
    python tools/jfr_summary.py target/jfr/scenario.jfr
    python tools/jfr_summary.py target/jfr/scenario.jfr --since 6 --root Renderer.display --top 30

Record with `python tools/scenario.py --jfr ...` (it calls this script at the end) or any JVM started with
-XX:StartFlightRecording (add -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints to keep the inlined frames).

Sections:
    methods      method samples (jdk.ExecutionSample, 20 ms) of the thread: "self" = the method is the top of the stack,
                 "total" = the method is anywhere in the stack, in percent of the selected samples
    allocations  object allocation samples (jdk.ObjectAllocationSample) of the thread, by class and by the first
                 frame of the library (weight = bytes allocated, estimated)
    frames       durations of the events mightylib.Frame, mightylib.Tick and mightylib.BatchSend (the send of a 2D
                 batch flush: sort, upload, draw calls), in the same window

--since / --until keep the events between those seconds after the start of the recording (the steady state, after
the setup of a scenario); --root keeps the samples whose stack holds a method containing that text (for example
--root Renderer.display for the display of the draws only). The binary `jfr` is searched next to the java of the
scenario.py (MIGHTY_JAVA) or in the PATH.
"""
import argparse
import json
import os
import re
import statistics
import subprocess
import sys
from collections import Counter
from datetime import datetime

LIBRARY_PREFIX = "io.github.mightycode"
EVENTS = ["jdk.ExecutionSample", "jdk.ObjectAllocationSample", "mightylib.Frame", "mightylib.Tick",
          "mightylib.BatchSend"]


def jfr_binary(java):
    folder = os.path.dirname(java) if os.path.dirname(java) else ""
    name = "jfr.exe" if os.name == "nt" else "jfr"
    candidate = os.path.join(folder, name)
    return candidate if folder and os.path.exists(candidate) else "jfr"


def read_events(path, java):
    command = [jfr_binary(java), "print", "--json", "--stack-depth", "64", "--events", ",".join(EVENTS), path]
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        sys.exit("jfr failed: " + (result.stderr or result.stdout)[-1000:])
    return json.loads(result.stdout)["recording"]["events"]


def parse_time(text):
    # 2026-11-24T10:20:30.123456789+01:00 : fromisoformat keeps microseconds at most
    text = re.sub(r"(\.\d{6})\d+", r"\1", text)
    return datetime.fromisoformat(text.replace("Z", "+00:00")).timestamp()


def parse_duration(text):
    if isinstance(text, (int, float)):
        return float(text)
    match = re.fullmatch(r"PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?", text)
    if not match:
        return 0.0
    hours, minutes, seconds = match.groups()
    return int(hours or 0) * 3600 + int(minutes or 0) * 60 + float(seconds or 0)


def method_name(frame):
    method = frame["method"]
    return full_owner(frame).split(".")[-1] + "." + method["name"]


def full_owner(frame):
    # Some JDKs print the owner with slashes
    return frame["method"]["type"]["name"].replace("/", ".")


def thread_name(values, key):
    thread = values.get(key) or {}
    return thread.get("javaName") or thread.get("osName") or ""


def percent(count, total):
    return f"{100.0 * count / total:5.1f}%" if total else "   -  "


def methods_section(samples, top):
    print(f"== methods: {len(samples)} samples")
    if not samples:
        return
    self_count = Counter()
    total_count = Counter()
    for frames in samples:
        self_count[method_name(frames[0])] += 1
        for name in {method_name(frame) for frame in frames}:
            total_count[name] += 1
    print(f"-- self (top {top})")
    for name, count in self_count.most_common(top):
        print(f"  {percent(count, len(samples))} {name}")
    print(f"-- total (top {top})")
    for name, count in total_count.most_common(top):
        print(f"  {percent(count, len(samples))} {name}")


def library_site(frames):
    for frame in frames:
        if full_owner(frame).startswith(LIBRARY_PREFIX):
            return method_name(frame) + ":" + str(frame.get("lineNumber", "?"))
    return method_name(frames[0]) if frames else "?"


def allocations_section(allocations, top):
    print(f"== allocations: {len(allocations)} samples")
    if not allocations:
        return
    total = sum(weight for _, _, weight in allocations)
    by_class = Counter()
    by_site = Counter()
    for object_class, frames, weight in allocations:
        by_class[object_class] += weight
        by_site[library_site(frames)] += weight
    print(f"  estimated {total / 1e6:.1f} MB allocated in the window")
    print(f"-- by class (top {top})")
    for name, weight in by_class.most_common(top):
        print(f"  {percent(weight, total)} {weight / 1e6:8.2f} MB  {name}")
    print(f"-- by site of the library (top {top})")
    for name, weight in by_site.most_common(top):
        print(f"  {percent(weight, total)} {weight / 1e6:8.2f} MB  {name}")


def durations_section(name, durations):
    if not durations:
        print(f"  {name}: no event")
        return
    ordered = sorted(durations)
    p95 = ordered[min(len(ordered) - 1, int(len(ordered) * 0.95))]
    print(f"  {name}: {len(ordered)} events, median {statistics.median(ordered) * 1000:.3f} ms, "
          f"p95 {p95 * 1000:.3f} ms, max {ordered[-1] * 1000:.3f} ms, total {sum(ordered) * 1000:.1f} ms")


def summarize(path, java="java", since=None, until=None, root=None, top=25, thread="main"):
    events = read_events(path, java)
    if not events:
        print("jfr: no event in the recording")
        return
    times = [parse_time(event["values"]["startTime"]) for event in events if "startTime" in event["values"]]
    start = min(times)
    low = start + since if since is not None else None
    high = start + until if until is not None else None

    samples = []
    allocations = []
    durations = {"mightylib.Frame": [], "mightylib.Tick": [], "mightylib.BatchSend": []}
    send_bytes = 0
    send_calls = 0
    for event in events:
        values = event["values"]
        moment = parse_time(values["startTime"])
        if (low is not None and moment < low) or (high is not None and moment > high):
            continue
        kind = event["type"]
        if kind in durations:
            durations[kind].append(parse_duration(values["duration"]))
            if kind == "mightylib.BatchSend":
                send_bytes += int(values.get("bytes") or 0)
                send_calls += int(values.get("drawCalls") or 0)
            continue

        key = "sampledThread" if kind == "jdk.ExecutionSample" else "eventThread"
        if thread and thread_name(values, key) != thread:
            continue
        frames = (values.get("stackTrace") or {}).get("frames") or []
        if not frames:
            continue
        if kind == "jdk.ExecutionSample":
            if root and not any(root in method_name(frame) or root in full_owner(frame) for frame in frames):
                continue
            samples.append(frames)
        else:
            allocations.append((values["objectClass"]["name"], frames, int(values.get("weight") or 0)))

    window = f"{since if since is not None else 0:g}s" + (f"-{until:g}s" if until is not None else "-end")
    print(f"jfr: {path}, window {window} of the recording, thread {thread or 'any'}"
          + (f", samples with {root}" if root else ""))
    methods_section(samples, top)
    allocations_section(allocations, top)
    print("== frames")
    durations_section("mightylib.Frame (display, swap excluded)", durations["mightylib.Frame"])
    durations_section("mightylib.Tick", durations["mightylib.Tick"])
    durations_section("mightylib.BatchSend (sort, upload, draw calls)", durations["mightylib.BatchSend"])
    if durations["mightylib.BatchSend"]:
        print(f"  BatchSend: {send_calls} draw calls, {send_bytes / 1e6:.1f} MB sent")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("file", help="recording (.jfr)")
    parser.add_argument("--since", type=float, default=None, help="seconds after the start of the recording")
    parser.add_argument("--until", type=float, default=None, help="seconds after the start of the recording")
    parser.add_argument("--root", default=None, help="keep the method samples whose stack holds this text")
    parser.add_argument("--top", type=int, default=25, help="lines per list (default 25)")
    parser.add_argument("--thread", default="main", help="thread of the samples (default main, '' for every thread)")
    args = parser.parse_args()
    # The java chosen for the scenarios (MIGHTY_JAVA, or the default of the OS): the jfr binary is next to it
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import scenario
    java = scenario.JAVA
    summarize(args.file, java, args.since, args.until, args.root, args.top, args.thread)


if __name__ == "__main__":
    main()
