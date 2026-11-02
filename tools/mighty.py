#!/usr/bin/env python3
"""Send commands to a running MightyLibrary program through its local debug server.

Usage:
    python tools/mighty.py stats
    python tools/mighty.py scene Platformer2D.Test2DPlatformerBox2D
    python tools/mighty.py --wait 60 help        # wait until the program accepts connections
    python tools/mighty.py --json stats          # raw JSON answer
    python tools/mighty.py                       # interactive mode, one command per line

Exit code: 0 if every command succeeded, 1 otherwise, 2 if the program can't be reached.
"""
import argparse
import json
import socket
import sys
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


def send(connection, reader, command):
    connection.sendall((command + "\n").encode("utf-8"))
    # Commands can wait for the next frames (screenshot, loading...)
    connection.settimeout(35)
    line = reader.readline()
    if not line:
        raise ConnectionError("Connection closed by the program")
    return json.loads(line)


def show(answer, raw):
    if raw:
        print(json.dumps(answer))
        return

    stream = sys.stdout if answer.get("success") else sys.stderr
    if answer.get("message"):
        print(answer["message"], file=stream)
    if "data" in answer:
        print(json.dumps(answer["data"], indent=2, ensure_ascii=False), file=stream)


def main():
    parser = argparse.ArgumentParser(description="Send commands to a running MightyLibrary program.")
    parser.add_argument("--port", type=int, default=7777, help="debug server port (default 7777)")
    parser.add_argument("--wait", type=float, default=0, help="seconds to wait for the program to start")
    parser.add_argument("--json", action="store_true", help="print raw JSON answers")
    parser.add_argument("command", nargs=argparse.REMAINDER, help="command to send, interactive mode if empty")
    args = parser.parse_args()

    try:
        connection = connect(args.port, args.wait)
    except OSError as error:
        print(f"Can't reach the program on port {args.port}: {error}", file=sys.stderr)
        return 2

    with connection:
        reader = connection.makefile("r", encoding="utf-8")

        if args.command:
            answer = send(connection, reader, " ".join(args.command))
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
