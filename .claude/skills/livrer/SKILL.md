---
name: livrer
description: Termine l'objectif en cours et le livre — compilation, tests, vérification en jeu, nettoyage des docs, commits, fusion --no-ff dans dev, push, suppression de la branche. Seulement sur demande explicite de l'utilisateur (/livrer ; argument « sans push » pour s'arrêter après la fusion, « sans fusion » pour s'arrêter aux commits).
disable-model-invocation: true
---

# Livrer

L'appel de `/livrer` vaut accord pour fusionner et pousser, sauf argument contraire. S'arrêter et demander à la moindre anomalie : tests rouges, problème dans le journal du jeu, modifications sans rapport avec l'objectif, conflit de fusion.

## 1. État
- `git branch --show-current`, `git status --short`, `git log --oneline dev..HEAD`.
- Sur dev ou master avec des modifications : créer une branche de sujet depuis `dev`, nommée d'après l'objectif (jamais de commit direct sur dev ni master).
- Des modifications sans rapport avec l'objectif : demander quoi en faire, ne pas les embarquer.

## 2. Vérifier
- Compiler, puis `mvn -o -q test` (commandes de `CLAUDE.md` selon l'OS).
- En jeu, si rendu, entrées, fenêtre, scènes ou ressources sont touchés : un seul appel `python tools/scenario.py` sur les scènes concernées (prévenir l'utilisateur qu'une fenêtre s'ouvre) ; `leakcheck <Scène>` si des ressources GL de scène ont changé. Journal : 0 ligne de problème.

## 3. Docs (dans le même commit que le code, ou un commit de docs sur la même branche)
- `current.md` / `later.md` : supprimer la ligne de ce qui est atteint, ou la réduire à son reste ; aucun « fait » ni historique.
- `architecture.md` : décrire le nouvel état, au présent.
- `CLAUDE.md` : liste du code couvert par les tests si de nouveaux tests couvrent une nouvelle zone ; conventions si une règle est née.
- `licence.md` : nouvelle dépendance ou ressource.
- Relire chaque fichier modifié : plus aucune trace de ce qui est fini.

## 4. Commits
Un commit par changement logique, message en anglais au style des précédents (`git log --oneline -10`), terminé par la ligne `Co-Authored-By` demandée par le système.

## 5. Fusionner et pousser
- `git checkout dev`, puis `git merge --no-ff <branche>` avec le message `Merge branch '<branche>'` suivi de la ligne `Co-Authored-By`.
- `mvn -o -q test` sur dev.
- `git push origin dev` (sauf « sans push »), puis `git branch -d <branche>`.

## 6. Compte rendu
Les commits livrés, ce qui a été vérifié (tests, scènes), ce qui ne l'a pas été et comment le vérifier à la main (quelle scène, quoi observer), l'état de dev par rapport à origin (et de dev par rapport à master).

## Version
La fusion de `dev` dans `master` (`git merge --no-ff dev`, tests, push) se fait seulement sur demande explicite de l'utilisateur : c'est une nouvelle version.
