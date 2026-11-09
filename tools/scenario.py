#!/usr/bin/env python3
"""Run a scenario on the game in one call: compile, launch, send commands, summarize, exit.

Usage:
    python tools/scenario.py "scene Scene2D" wait-loaded gl "input ENTER" stats
    python tools/scenario.py --attach "stats"          # use an already running game
    python tools/scenario.py --no-compile "scene" ...  # skip compilation

Steps are game commands (see `help` in the game) plus:
    wait-loaded                     wait until the current scene has finished loading
    sleep:SECONDS                   wait, for example sleep:0.5
    leakcheck SCENE [N] [VIA]       load SCENE, do N round trips (default 2) through VIA (default MenuScene),
                                    print "no leak" or only the OpenGL counters that changed
    log PATTERN [N]                 last N lines (default 10) of the game log matching the regex PATTERN

Output is short: a successful step prints "ok" only (and the file of a screenshot); prefix a step with "?" to
see its whole answer, for example "?gl summary". Failures always print the whole answer.
Then the problems found in the game log, identical lines counted once.
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
if os.name == "nt":
    _DEFAULT_JAVA = "C:/Autres/## Librairies ##/javajdk/bin/java"
    _DEFAULT_MVN = "D:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2024.2.3/plugins/maven/lib/maven3/bin/mvn.cmd"
    _DEFAULT_MAVEN_JDK = os.path.expanduser("~/.jdks/ms-21.0.9")
else:
    # Linux: java and mvn from the PATH, maven keeps the JAVA_HOME of the environment
    _DEFAULT_JAVA = "java"
    _DEFAULT_MVN = "mvn"
    _DEFAULT_MAVEN_JDK = os.environ.get("JAVA_HOME")
JAVA = os.environ.get("MIGHTY_JAVA", _DEFAULT_JAVA)
MVN = os.environ.get("MIGHTY_MVN", _DEFAULT_MVN)
MAVEN_JDK = os.environ.get("MIGHTY_MAVEN_JDK", _DEFAULT_MAVEN_JDK)
MAIN_CLASS = "io.github.mightycode.mightylibdemo.main.Main"
CLASSPATH_FILE = os.path.join(ROOT, "target", "classpath.txt")
# The launched game writes there the port of its debug server (the first free one from 7777):
# one file per scenario, so that several games and scenarios can run at the same time
PORT_FILE = os.path.join(ROOT, "target", f"scenario-{os.getpid()}.port")
PORT_FILE_PROPERTY = "mightylib.debugServer.portFile"
MONITOR_PROPERTY = "mightylib.window.monitor"
PLATFORM_PROPERTY = "mightylib.glfw.platform"
STRICT_RESOURCES_PROPERTY = "mightylib.resources.strict"
NO_CURSOR_CAPTURE_PROPERTY = "mightylib.input.noCursorCapture"
PROBLEM_PATTERN = re.compile(r"Exception|Exit with error|^\s+at io\.github\.mightycode|OpenGL debug|Error:"
                             r"|^\[LWJGL\] .*error|^\s+Description :")


def run_maven(*goals):
    environment = dict(os.environ)
    if MAVEN_JDK:
        environment["JAVA_HOME"] = MAVEN_JDK
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


def launch(log_path, monitor, strict=False, capture_mouse=False, platform=None):
    with open(CLASSPATH_FILE, encoding="utf-8") as file:
        classpath = os.path.join(ROOT, "target", "classes") + os.pathsep + file.read().strip()

    if os.path.exists(PORT_FILE):
        os.remove(PORT_FILE)

    log = open(log_path, "w", encoding="utf-8")
    options = [f"-D{PORT_FILE_PROPERTY}={PORT_FILE}"]
    if monitor:
        options.append(f"-D{MONITOR_PROPERTY}={monitor}")
    if platform:
        options.append(f"-D{PLATFORM_PROPERTY}={platform}")
    if strict:
        options.append(f"-D{STRICT_RESOURCES_PROPERTY}=true")
    if not capture_mouse:
        # The mouse of the user stays free while the game runs (3D scenes would capture it)
        options.append(f"-D{NO_CURSOR_CAPTURE_PROPERTY}=true")

    return subprocess.Popen([JAVA, *options, "-cp", classpath, MAIN_CLASS], cwd=ROOT,
                            stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT)


def wait_port(process, timeout):
    # Port chosen by the launched game, None if it exited or did not start its server in time
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline and process.poll() is None:
        try:
            with open(PORT_FILE, encoding="utf-8") as file:
                text = file.read().strip()
            if text:
                return int(text)
        except (OSError, ValueError):
            pass
        time.sleep(0.2)
    return None


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


def short(answer):
    # Successful answer reduced to what is needed to go on: the file of a screenshot
    data = answer.get("data", {})
    text = "ok"
    if "path" in data:
        text += " " + data["path"]
    return text


def gl_snapshot(connection, reader, timeout):
    # OpenGL counters once the waiting GL elements are loaded; names lose their "#id" suffix (renderer ids change)
    deadline = time.monotonic() + timeout
    while True:
        data = mighty.send(connection, reader, "gl").get("data", {})
        shared = data.get("sharedElements", {})
        if shared.get("waitingToLoad", 0) == 0 or time.monotonic() > deadline:
            break
        time.sleep(0.25)

    counters = {}
    for name, count in data.get("glObjects", {}).items():
        counters["gl." + name] = count
    for kind, elements in shared.items():
        if isinstance(elements, dict):
            for name, references in elements.items():
                key = f"{kind}.{re.sub(r'#[0-9]+$', '', name)}"
                counters[key] = counters.get(key, 0) + references
    return counters


def change_scene(connection, reader, timeout, scene):
    answer = mighty.send(connection, reader, "scene " + scene)
    if not answer.get("success"):
        return answer, False
    answer, timed_out = wait_loaded(connection, reader, timeout, scene)
    return answer, not timed_out


def leak_check(connection, reader, timeout, words):
    if len(words) < 2:
        print("> leakcheck\n  ERR usage: leakcheck SCENE [N] [VIA]")
        return False
    scene = words[1]
    trips = int(words[2]) if len(words) > 2 else 2
    via = words[3] if len(words) > 3 else "MenuScene"

    answer, loaded = change_scene(connection, reader, timeout, scene)
    if not loaded:
        print(f"> leakcheck {scene}\n  ERR {compact(answer)}")
        return False
    before = gl_snapshot(connection, reader, timeout)

    for _ in range(trips):
        for target in (via, scene):
            answer, loaded = change_scene(connection, reader, timeout, target)
            if not loaded:
                print(f"> leakcheck {scene}\n  ERR loading {target}: {compact(answer)}")
                return False
    after = gl_snapshot(connection, reader, timeout)

    changes = [f"{key} {before.get(key, 0)}->{after.get(key, 0)}"
               for key in sorted(set(before) | set(after)) if before.get(key, 0) != after.get(key, 0)]
    if changes:
        print(f"> leakcheck {scene}\n  LEAK after {trips} round trip(s) via {via}: " + ", ".join(changes))
        return False
    print(f"> leakcheck {scene}  no leak ({trips} round trip(s) via {via})")
    return True


def log_lines(log_path, words):
    if log_path is None or len(words) < 2:
        print("> log\n  ERR usage: log PATTERN [N], not available with --attach")
        return False
    pattern = re.compile(words[1])
    count = int(words[2]) if len(words) > 2 else 10
    with open(log_path, encoding="utf-8", errors="replace") as file:
        lines = [line.rstrip() for line in file if pattern.search(line)]
    print(f"> log {words[1]}  {len(lines)} line(s)" + ("" if len(lines) <= count else f", last {count}"))
    for line in lines[-count:]:
        print("  " + line)
    return True


def run_steps(port, steps, timeout, log_path=None):
    success = True
    expected_scene = None
    connection = mighty.connect(port, timeout)
    with connection:
        reader = connection.makefile("r", encoding="utf-8")
        for step in steps:
            if step.startswith("sleep:"):
                time.sleep(float(step[len("sleep:"):]))
                continue

            verbose = step.startswith("?")
            if verbose:
                step = step[1:]

            words = step.split()
            if words and words[0] == "leakcheck":
                success = leak_check(connection, reader, timeout, words) and success
                continue
            if words and words[0] == "log":
                success = log_lines(log_path, words) and success
                continue

            if len(words) >= 2 and words[0] == "scene":
                # Class name relative to the scenes package, for example Platformer2D.PlatformerBox2D
                expected_scene = words[1]

            if step == "wait-loaded":
                answer, timed_out = wait_loaded(connection, reader, timeout, expected_scene)
                if timed_out:
                    print(f"> {step}\n  BLOCKED: still loading after {timeout:g}s, scenario stopped: {compact(answer)}")
                    print("  " + compact(mighty.send(connection, reader, "gl summary")))
                    return False
            else:
                answer = mighty.send(connection, reader, step)

            if answer.get("success") and not verbose:
                print(f"> {step}  {short(answer)}")
            else:
                print(f"> {step}\n  {compact(answer)}")
            success = success and answer.get("success", False)
    return success


def stop(process, port):
    try:
        if port is None:
            raise ConnectionError("no port")
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

    # A repeated exception is shown once with its number of occurrences
    counts = {}
    for line in problems:
        counts[line] = counts.get(line, 0) + 1

    print(f"log: {len(problems)} problem line(s), {len(counts)} distinct ({log_path})")
    for line, count in list(counts.items())[:20]:
        print("  " + line + (f"  (x{count})" if count > 1 else ""))
    return not problems


def main():
    # The Windows console code page can't print every character of the game log
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(errors="replace")

    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=7777,
                        help="port of the game for --attach (a launched game gives its own port)")
    parser.add_argument("--timeout", type=float, default=20,
                        help="seconds without result (start, answer, loading) before the game is considered blocked")
    parser.add_argument("--max-duration", type=float, default=300,
                        help="seconds after which the game is killed, whatever it is doing")
    parser.add_argument("--monitor", default="secondary",
                        help="screen of the game window: secondary (default, falls back to the primary one), primary,"
                             " largest, an index or a part of a name; empty to keep the choice of the project")
    parser.add_argument("--platform", default=None,
                        help="GLFW platform: x11, wayland (Linux; Wayland can't place windows), win32, cocoa;"
                             " by default GLFW chooses")
    parser.add_argument("--no-compile", action="store_true", help="skip the compilation")
    parser.add_argument("--strict", action="store_true",
                        help="a resource that fails stops the game instead of using its fallback (debug mode only)")
    parser.add_argument("--capture-mouse", action="store_true",
                        help="let the game capture the mouse (3D scenes hide and lock it); by default it stays free")
    parser.add_argument("--attach", action="store_true", help="use an already running game, don't launch or close it")
    parser.add_argument("--keep-open", action="store_true", help="don't close the game at the end")
    parser.add_argument("--log", default=os.path.join(ROOT, "target", "scenario.log"),
                        help="game log, give another one to run several scenarios at the same time")
    parser.add_argument("steps", nargs="*")
    args = parser.parse_args()
    mighty.ANSWER_TIMEOUT = args.timeout

    if args.attach:
        return 0 if run_steps(args.port, args.steps, args.timeout) else 1

    prepare(not args.no_compile)
    process = launch(args.log, args.monitor, args.strict, args.capture_mouse, args.platform)

    # A blocked game (main thread stuck, no answer) must not block the caller forever
    def kill_blocked_game():
        if process.poll() is None:
            print(f"Game killed: scenario longer than {args.max_duration:g} seconds")
            process.kill()

    watchdog = threading.Timer(args.max_duration, kill_blocked_game)
    watchdog.daemon = True
    watchdog.start()
    port = None
    try:
        port = wait_port(process, args.timeout)
        if port is None:
            print(f"BLOCKED: the game gave no debug server port after {args.timeout:g}s")
            success = False
        else:
            print(f"game: debug server on port {port}")
            success = run_steps(port, args.steps, args.timeout, args.log)
    except OSError as error:
        print(f"Can't talk to the game: {error}")
        success = False
    finally:
        watchdog.cancel()
        if not args.keep_open:
            stop(process, port)

    if os.path.exists(PORT_FILE):
        os.remove(PORT_FILE)

    log_ok = report_log(args.log)
    return 0 if success and log_ok else 1


if __name__ == "__main__":
    sys.exit(main())
