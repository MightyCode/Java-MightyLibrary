"""Summarize an IntelliJ inspection export (one XML file per inspection) in a few lines of text.

python summarize.py [DIR]                       count of problems per inspection (default DIR: target/analyse)
python summarize.py [DIR] --details NAME...     file:line | highlighted element | description, for these inspections
python summarize.py [DIR] --unused              unused.xml grouped by kind and area (lib / demo / test),
                                                then every group except "method never used" in the library
"""
import collections
import html
import os
import re
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8")


def problems(path):
    for problem in ET.parse(path).getroot().iter("problem"):
        def text(tag):
            node = problem.find(tag)
            return "" if node is None or node.text is None else node.text
        description = re.sub(r"<[^>]+>", "", html.unescape(text("description"))).replace("\xa0", " ")
        entry = problem.find("entry_point")
        yield {
            "file": text("file").replace("file://$PROJECT_DIR$/", ""),
            "line": text("line"),
            "element": " ".join(text("highlighted_element").split())[:60],
            "description": " ".join(description.split())[:140],
            "type": "" if entry is None else entry.get("TYPE", ""),
            "fqname": "" if entry is None else entry.get("FQNAME", ""),
        }


def area(file):
    if file.startswith("test/"):
        return "test"
    return "demo" if "mightylibdemo" in file else "lib"


def counts(folder):
    rows = []
    for name in sorted(os.listdir(folder)):
        if name.endswith(".xml") and not name.startswith("."):
            rows.append((sum(1 for _ in problems(os.path.join(folder, name))), name[:-4]))
    for count, name in sorted(rows, reverse=True):
        print(f"{count:5} {name}")
    print(f"{sum(c for c, _ in rows):5} total, {len(rows)} inspections")


def details(folder, names):
    for name in names:
        print("##", name)
        for p in problems(os.path.join(folder, name + ".xml")):
            print(f"{p['file']}:{p['line']} | {p['element']} | {p['description']}")


def unused(folder):
    groups = collections.defaultdict(list)
    for p in problems(os.path.join(folder, "unused.xml")):
        d = p["description"]
        if "never assigned" in d:
            kind = "never assigned"
        elif "assigned but never accessed" in d:
            kind = "assigned, never read"
        elif "instantiat" in d or "not implemented" in d:
            kind = "class never instantiated"
        elif "never used" in d:
            kind = "never used"
        else:
            kind = "other (parameters, unreachable constructors)"
        groups[(kind, area(p["file"]), p["type"])].append(p)

    for key, items in sorted(groups.items(), key=lambda item: -len(item[1])):
        print(f"{len(items):5} {key[0]} / {key[1]} / {key[2]}")
    for key, items in groups.items():
        if key[0] == "never used" and key[1] == "lib" and key[2] == "method":
            continue  # public API of the library, kept on purpose
        print("##", " / ".join(key))
        for p in items:
            print(f"{p['file'].split('/')[-1]}:{p['line']} {p['fqname'].split(' ')[-1][:50]} :: {p['description'][:80]}")


if __name__ == "__main__":
    args = sys.argv[1:]
    folder = "target/analyse"
    if args and not args[0].startswith("--"):
        folder = args.pop(0)
    if not os.path.isdir(folder):
        sys.exit(f"No export in {folder}: Code > Inspect Code > Whole project, then Export (XML) to this folder")
    if args and args[0] == "--details":
        details(folder, args[1:])
    elif args and args[0] == "--unused":
        unused(folder)
    else:
        counts(folder)
