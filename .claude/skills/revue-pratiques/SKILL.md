---
name: revue-pratiques
description: Revue hebdomadaire des bonnes pratiques du projet (docs/bonnes-pratiques.md), point par point avec preuves. À utiliser quand l'utilisateur demande si le projet respecte toujours ses bonnes pratiques, ou lance /revue-pratiques (argument optionnel : période, ex. « depuis le 2026-11-01 »).
---

# Revue des bonnes pratiques

`docs/bonnes-pratiques.md` fait foi (points et vérifications) : ce skill ne donne que la procédure. Rien n'est corrigé pendant la revue (sauf la date de la dernière revue dans `CLAUDE.md`).

## 1. Collecter
- Lire `docs/bonnes-pratiques.md` en entier.
- Période : celle donnée par l'utilisateur, sinon la dernière semaine.
- `python .claude/skills/revue-pratiques/collect.py --since "<période>"` : branches et fusions, commits (correctifs sans test, code ancien sans test, commits sans doc), tests, lignes conditionnelles de `current.md`, `printStackTrace`, `catch` vides, fichiers ouverts hors try-with-resources, âge de l'export IntelliJ.
- `mvn -o -q test` (commande Windows/Linux de `CLAUDE.md`) : résultat réel des tests.
- Lire seulement ce que le rapport signale (extraits de code, commits avec `git show --stat`), jamais tout l'historique.
- Ne pas lancer le jeu : la revue juge si les vérifications en jeu ont été faites, elle ne les refait pas.

## 2. Juger
- Un drapeau du script n'est pas une faute : un correctif impossible à tester sans GL, un commit sans effet sur l'architecture sans doc, un fichier rendu ouvert à l'appelant sont acceptables. Le dire.
- Points de jugement (vrai jeu, surface de l'API, vérifications en jeu, recul) : répondre d'après l'historique et ce que l'utilisateur a dit ; à défaut, « à confirmer » avec la question précise, jamais une supposition.
- Comparer avec la revue précédente si elle est dans la conversation ou si l'utilisateur la rappelle ; l'état n'est jamais écrit dans `bonnes-pratiques.md`.

## 3. Répondre
- Tableau, un point par ligne : numéro, point (quelques mots), verdict (**respecté** / **dérive** / **à confirmer**), preuve (une ligne : chiffre, fichier, commit), action proposée.
- Puis les 2 ou 3 actions prioritaires, et une seule question : laquelle faire (les corrections suivent les règles de `CLAUDE.md` : branche, commits, tests, docs).
- Mettre à jour la date « Dernière revue » de `CLAUDE.md` (Point hebdomadaire) à la date du jour : seule modification faite par la revue.
- Si un point est devenu obsolète ou si l'utilisateur en ajoute un : proposer la modification de `docs/bonnes-pratiques.md` (règle et vérification, jamais l'état), et mettre `collect.py` à jour si le point est mesurable.
