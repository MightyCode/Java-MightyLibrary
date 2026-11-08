"""Collect the measurable facts of docs/bonnes-pratiques.md in one short text report (run from the repository root).

python collect.py [--since "1 week ago"]

The numbers in brackets are the points of docs/bonnes-pratiques.md. Judgment points (real game, API surface,
in-game checks) are not measured here: the review answers them from the user and the history.
"""
import os
import re
import subprocess
import sys
import time

sys.stdout.reconfigure(encoding="utf-8")

since = "1 week ago"
if "--since" in sys.argv:
    since = sys.argv[sys.argv.index("--since") + 1]

LEGACY = ("network/", "graphics/GUI/", "utils/DataFolder", "algorithms/", "resources/data/", "graphics/text/FontFile")


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, encoding="utf-8").stdout.strip()


# Integration branch: topic branches merge into dev, dev merges into master for a version.
BASE = "dev" if git("branch", "--list", "dev") else "master"


def section(title):
    print(f"\n## {title}")


section("[1] Branches et fusions")
print("branche courante:", git("branch", "--show-current"))
changes = [line for line in git("status", "--short").splitlines() if not line.startswith("??")]
print("modifications non commitées:", len(changes))
merged = [b for b in git("branch", "--merged", BASE, "--format=%(refname:short)").splitlines() if b not in (BASE, "master")]
print("fusionnées non supprimées:", ", ".join(merged) or "aucune")
for branch in git("branch", "--no-merged", BASE, "--format=%(refname:short)").splitlines():
    print(f"non fusionnée: {branch} (+{git('rev-list', '--count', BASE + '..' + branch)}) {git('log', '-1', '--format=%ad %s', '--date=short', branch)[:80]}")
direct = [line for line in git("log", "--first-parent", "--no-merges", "--since=" + since, "--format=%h %s", BASE).splitlines()]
print(f"commits directs sur {BASE} (hors fusion):", len(direct))
if BASE != "master":
    print(f"{BASE} en avance sur master: {git('rev-list', '--count', 'master..' + BASE)} commits")
for line in direct[:10]:
    print("  ", line[:100])

section(f"[2][3][8] Commits de {BASE} depuis « {since} »")
log = git("log", "--no-merges", "--since=" + since, "--name-only", "--format=@@%h %s", BASE)
commits = []
for block in log.split("@@")[1:]:
    lines = [line for line in block.splitlines() if line.strip()]
    commits.append((lines[0], lines[1:]))
fix_words = re.compile(r"\bfix|bug|no longer|crash|NullPointer|leak|stuck", re.I)
with_code = without_doc = 0
flagged = {"correctif sans test": [], "code ancien sans test": []}
for title, files in commits:
    code = [f for f in files if f.startswith("src/")]
    docs = [f for f in files if f.startswith("docs/") or f == "CLAUDE.md"]
    tests = [f for f in files if f.startswith("test/")]
    if not code:
        continue
    with_code += 1
    without_doc += not docs
    if fix_words.search(title) and not tests:
        flagged["correctif sans test"].append(title)
    if any(part in f for f in code for part in LEGACY) and not tests:
        flagged["code ancien sans test"].append(title)
print(f"commits: {len(commits)}, dont {with_code} touchent src/ ; {without_doc} sans doc "
      "(normal pour un changement sans effet sur l'architecture : regarder les plus gros)")
for flag, titles in flagged.items():
    print(f"{flag}: {len(titles)}")
    for title in titles[:12]:
        print("  ", title[:100])
    if len(titles) > 12:
        print(f"   … {len(titles) - 12} de plus")
print("(un drapeau n'est pas une faute : à juger, ex. un correctif impossible à tester sans GL)")

section("[3] Tests")
test_count = 0
for root, _, files in os.walk("test"):
    for name in files:
        if name.endswith(".java"):
            with open(os.path.join(root, name), encoding="utf-8") as file:
                test_count += file.read().count("@Test")
print("méthodes @Test:", test_count, "(lancer mvn -o -q test pour le résultat)")

section("[6][13] current.md")
with open("docs/current.md", encoding="utf-8") as file:
    current = file.read().splitlines()
print("lignes de points:", sum(1 for line in current if line.lstrip().startswith("- ")))
for number, line in enumerate(current, 1):
    if re.search(r"si les mesures|si un vrai jeu|seulement si|si besoin", line, re.I):
        print(f"  conditionnel l.{number}: {line.strip()[:110]}")

section("[9][10] Code")
stack_traces, opened, empty_catches = [], [], []
open_call = re.compile(r"new (FileInputStream|FileReader|FileWriter|FileOutputStream)\(|new Scanner\(new File")
for root, _, files in os.walk("src"):
    for name in files:
        if not name.endswith(".java"):
            continue
        path = os.path.join(root, name).replace("\\", "/")
        with open(path, encoding="utf-8") as file:
            text = file.read()
        lines = text.splitlines()
        for number, line in enumerate(lines, 1):
            if "printStackTrace()" in line:
                stack_traces.append(f"{path}:{number}")
            # A try-with-resources may open its resources on the next lines
            nearby = " ".join(lines[max(0, number - 3):number])
            if open_call.search(line) and "try (" not in nearby and "try(" not in nearby:
                opened.append(f"{path}:{number}: {line.strip()[:80]}")
        for match in re.finditer(r"catch \([^)]*\) \{\s*\}", text):
            empty_catches.append(f"{path}:{text.count(chr(10), 0, match.start()) + 1}")
print("printStackTrace():", ", ".join(stack_traces) or "aucun")
print("catch vides:", ", ".join(empty_catches) or "aucun")
print("ouvertures hors try-with-resources (vérifier qu'elles sont fermées, ex. try sur la ligne d'avant):")
for line in opened:
    print("  ", line)

section("[11] Analyse statique")
exports = [os.path.join("target/analyse", f) for f in os.listdir("target/analyse")] if os.path.isdir("target/analyse") else []
if exports:
    age = (time.time() - max(os.path.getmtime(f) for f in exports)) / 86400
    print(f"export IntelliJ: {len(exports)} fichiers, il y a {age:.0f} jours" + ("  <- plus d'un mois" if age > 31 else ""))
else:
    print("aucun export dans target/analyse")
