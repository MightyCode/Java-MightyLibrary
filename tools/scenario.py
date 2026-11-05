#!/usr/bin/env python3
"""Run a scenario on the game in one call: compile, launch, send commands, summarize, exit.

Usage:
    python tools/scenario.py "scene Test2DScene" wait-loaded gl "input ENTER" stats
    python tools/scenario.py --attach "stats"          # use an already running game
    python tools/scenario.py --no-compile "scene" ...  # skip compilation

Steps are game commands (see `help` in the game) plus:
    wait-loaded      wait until the current scene has finished loading
    sleep:SECONDS    wait, for example sleep:0.5

Output is one compact line per step, then the problems found in the game log.
Without result after --timeout seconds (20 by default) the game is considered blocked: the scenario stops.
The game is killed after --max-duration seconds whatever it is doing.
Exit code: 0 if every step succeeded and the log has no problem, 1 otherwise.

Machine specific paths can be changed with the environment variables MIGHTY_JAVA (java used to run the game),
MIGHTY_MVN (maven executable) and MIGHTY_MAVEN_JDK (JAVA_HOME used by maven).
"""
import argparse
import json
import os
import re
import subprocess
import sys
import threading
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mighty  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.environ.get("MIGHTY_JAVA", "C:/Autres/## Librairies ##/javajdk/bin/java")
MVN = os.environ.get("MIGHTY_MVN",
                     "D:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2024.2.3/plugins/maven/lib/maven3/bin/mvn.cmd")
MAVEN_JDK = os.environ.get("MIGHTY_MAVEN_JDK", os.path.expanduser("~/.jdks/ms-21.0.9"))
MAIN_CLASS = "MightyLibrary.project.main.Main"
CLASSPATH_FILE = os.path.join(ROOT, "target", "classpath.txt")
PROBLEM_PATTERN = re.compile(r"Exception|Exit with error|^\s+at MightyLibrary|OpenGL debug|Error:")


def run_maven(*goals):
    environment = dict(os.environ, JAVA_HOME=MAVEN_JDK)
    result = subprocess.run([MVN, "-q", *goals], cwd=ROOT, env=environment, capture_output=True, text=True)
    if result.returncode != 0:
        print("Maven failed:\n" + (result.stdout + result.stderr)[-3000:])
        sys.exit(1)


def prepare(compile_sources):
    if compile_sources:
        run_maven("-o", "compile")

    pom = os.path.join(ROOT, "pom.xml")
    if not os.path.exists(CLASSPATH_FILE) or os.path.getmtime(CLASSPATH_FILE) < os.path.getmtime(pom):
        run_maven("dependency:build-classpath", "-Dmdep.outputFile=" + CLASSPATH_FILE)


def launch(log_path):
    with open(CLASSPATH_FILE, encoding="utf-8") as file:
        classpath = os.path.join(ROOT, "target", "classes") + os.pathsep + file.read().strip()

    log = open(log_path, "w", encoding="utf-8")
    return subprocess.Popen([JAVA, "-cp", classpath, MAIN_CLASS], cwd=ROOT,
                            stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT)


def compact(answer):
    status = "ok " if answer.get("success") else "ERR"
    text = status + " " + answer.get("message", "").replace("\n", " | ")
    if "data" in answer:
        text += " " + json.dumps(answer["data"], separators=(",", ":"), ensure_ascii=False)
    return text


def wait_loaded(connection, reader, timeout, expected_scene):
    # The scene changes at the next tick: also wait for the requested scene, not only for the end of loading
    deadline = time.monotonic() + timeout
    while True:
        answer = mighty.send(connection, reader, "scene")
        data = answer.get("data", {})
        scene_ok = expected_scene is None or str(data.get("scene", "")).endswith(expected_scene)
        if scene_ok and not data.get("loading", True):
            return answer, False
        if time.monotonic() > deadline:
            return answer, True
        time.sleep(0.25)


def run_steps(port, steps, timeout):
    success = True
    expected_scene = None
    connection = mighty.connect(port, timeout)
    with connection:
        reader = connection.makefile("r", encoding="utf-8")
        for step in steps:
            if step.startswith("sleep:"):
                time.sleep(float(step[len("sleep:"):]))
                print(f"> {step}")
                continue

            words = step.split()
            if len(words) >= 2 and words[0] == "scene":
                # Class name relative to the scenes package, for example Platformer2D.Test2DPlatformerBox2D
                expected_scene = words[1]

            if step == "wait-loaded":
                answer, timed_out = wait_loaded(connection, reader, timeout, expected_scene)
                if timed_out:
                    print(f"> {step}\n  BLOCKED: still loading after {timeout:g}s, scenario stopped: {compact(answer)}")
                    print("  gl " + compact(mighty.send(connection, reader, "gl")))
                    return False
            else:
                answer = mighty.send(connection, reader, step)
            print(f"> {step}\n  {compact(answer)}")
            success = success and answer.get("success", False)
    return success


def stop(process, port):
    try:
        connection = mighty.connect(port, 2)
        with connection:
            mighty.send(connection, connection.makefile("r", encoding="utf-8"), "exit")
    except (OSError, ConnectionError, ValueError):
        pass

    try:
        process.wait(15)
    except subprocess.TimeoutExpired:
        process.kill()
        print("Game killed: it did not exit after 15 seconds")


def report_log(log_path):
    with open(log_path, encoding="utf-8", errors="replace") as file:
        problems = [line.rstrip() for line in file if PROBLEM_PATTERN.search(line)]

    print(f"log: {len(problems)} problem line(s) ({log_path})")
    for line in problems[:20]:
        print("  " + line)
    return not problems


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=7777)
    parser.add_argument("--timeout", type=float, default=20,
                        help="seconds without result (start, answer, loading) before the game is considered blocked")
    parser.add_argument("--max-duration", type=float, default=300,
                        help="seconds after which the game is killed, whatever it is doing")
    parser.add_argument("--no-compile", action="store_true", help="skip the compilation")
    parser.add_argument("--attach", action="store_true", help="use an already running game, don't launch or close it")
    parser.add_argument("--keep-open", action="store_true", help="don't close the game at the end")
    parser.add_argument("--log", default=os.path.join(ROOT, "target", "scenario.log"))
    parser.add_argument("steps", nargs="*")
    args = parser.parse_args()
    mighty.ANSWER_TIMEOUT = args.timeout

    if args.attach:
        return 0 if run_steps(args.port, args.steps, args.timeout) else 1

    # Another game already listening would receive the commands instead of the launched one
    try:
        mighty.connect(args.port, 0).close()
        print(f"A game already listens on port {args.port}: close it, or use --attach to drive it")
        return 1
    except OSError:
        pass

    prepare(not args.no_compile)
    process = launch(args.log)

    # A blocked game (main thread stuck, no answer) must not block the caller forever
    def kill_blocked_game():
        if process.poll() is None:
            print(f"Game killed: scenario longer than {args.max_duration:g} seconds")
            process.kill()

    watchdog = threading.Timer(args.max_duration, kill_blocked_game)
    watchdog.daemon = True
    watchdog.start()
    try:
        success = run_steps(args.port, args.steps, args.timeout)
    except OSError as error:
        print(f"Can't talk to the game: {error}")
        success = False
    finally:
        watchdog.cancel()
        if not args.keep_open:
            stop(process, args.port)

    log_ok = report_log(args.log)
    return 0 if success and log_ok else 1


if __name__ == "__main__":
    sys.exit(main())
