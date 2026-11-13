#!/usr/bin/env python3
"""Send commands to a running MightyLibrary program through its local debug server.

Usage:
    python tools/mighty.py stats
    python tools/mighty.py scene Platformer2D.PlatformerBox2D
    python tools/mighty.py --wait 60 help        # wait until the program accepts connections
    python tools/mighty.py --json stats          # raw JSON answer
    python tools/mighty.py                       # interactive mode, one command per line
    python tools/mighty.py --follow              # interactive mode showing the whole console log of the program

Exit code: 0 if every command succeeded, 1 otherwise, 2 if the program can't be reached.
"""
import argparse
import json
import socket
import sys
import threading
import time


def connect(port, wait_seconds):
    deadline = time.monotonic() + wait_seconds
    while True:
        try:
            return socket.create_connection(("127.0.0.1", port), timeout=5)
        except OSError:
            if time.monotonic() >= deadline:
                raise
            time.sleep(0.5)


# Seconds to wait for an answer before considering the program blocked (socket.timeout is raised)
ANSWER_TIMEOUT = 35


def send(connection, reader, command, answer_timeout=None):
    connection.sendall((command + "\n").encode("utf-8"))
    # Commands can wait for the next frames (screenshot, loading...)
    connection.settimeout(ANSWER_TIMEOUT if answer_timeout is None else answer_timeout)
    while True:
        line = reader.readline()
        if not line:
            raise ConnectionError("Connection closed by the program")
        answer = json.loads(line)
        # Lines of a followed console log come between the answers
        if answer.get("event") != "log":
            return answer


def show(answer, raw):
    if raw:
        print(json.dumps(answer))
        return

    stream = sys.stdout if answer.get("success") else sys.stderr
    if answer.get("message"):
        print(answer["message"], file=stream)
    if "data" in answer:
        print(json.dumps(answer["data"], indent=2, ensure_ascii=False), file=stream)


def show_log_line(event):
    kind = event.get("type")
    text = event.get("text", "")
    if kind == "COMMAND":
        text = f"[{event.get('source')}] > {text}"
    stream = sys.stderr if kind in ("ERR", "ERROR") else sys.stdout
    print(text, file=stream, flush=True)


def follow(connection, reader, raw):
    """Show the console log of the program as it is written, send the lines typed here as commands."""
    def read_all():
        for line in reader:
            message = json.loads(line)
            if message.get("event") == "log":
                show_log_line(message)
            elif raw:
                print(json.dumps(message), flush=True)
        print("Connection closed by the program", file=sys.stderr, flush=True)

    # Commands and their results are in the log: the answers are only shown with --json
    connection.sendall(b"console follow on\n")
    connection.settimeout(None)
    threading.Thread(target=read_all, daemon=True).start()

    for line in sys.stdin:
        if line.strip():
            connection.sendall((line.strip() + "\n").encode("utf-8"))
    return 0


def main():
    parser = argparse.ArgumentParser(description="Send commands to a running MightyLibrary program.")
    parser.add_argument("--port", type=int, default=7777, help="debug server port (default 7777)")
    parser.add_argument("--wait", type=float, default=0, help="seconds to wait for the program to start")
    parser.add_argument("--json", action="store_true", help="print raw JSON answers")
    parser.add_argument("--follow", action="store_true",
                        help="show the console log of the program (its output and the commands of every source)")
    parser.add_argument("command", nargs=argparse.REMAINDER, help="command to send, interactive mode if empty")
    args = parser.parse_args()

    try:
        connection = connect(args.port, args.wait)
    except OSError as error:
        print(f"Can't reach the program on port {args.port}: {error}", file=sys.stderr)
        print("The program must be running in debug mode, its console shows "
              f"'--Debug server listening on 127.0.0.1:{args.port}'. "
              "Use --wait SECONDS to wait for it to start.", file=sys.stderr)
        return 2

    with connection:
        reader = connection.makefile("r", encoding="utf-8")

        if args.follow:
            return follow(connection, reader, args.json)

        if args.command:
            # The shell removed the quotes: put them back around words with spaces
            words = [f'"{word}"' if (not word or any(c.isspace() for c in word)) else word
                     for word in args.command]
            answer = send(connection, reader, " ".join(words))
            show(answer, args.json)
            return 0 if answer.get("success") else 1

        success = True
        for line in sys.stdin:
            if not line.strip():
                continue
            answer = send(connection, reader, line.strip())
            show(answer, args.json)
            success = success and answer.get("success", False)
        return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
