---
name: pr-ml
description: Relit une branche de sujet par rapport à dev, signale les erreurs, en discute avec l'utilisateur, les corrige, met à jour les docs, ouvre la PR sur GitHub, la fusionne, puis revient sur dev à jour. Seulement sur demande explicite (/pr-ml <branche> [modèle et effort recommandés]) ; sans branche, la branche courante.
disable-model-invocation: true
---

# PR : relire, corriger, documenter, fusionner

L'appel de `/pr-ml` vaut accord pour ouvrir la PR et la fusionner **une fois la discussion des erreurs terminée** (étape 3). S'arrêter et demander à la moindre anomalie imprévue : tests rouges, problème dans le journal du jeu, modifications sans rapport avec l'objectif, conflit, dépôt distant inaccessible.

L'argument est la branche (par défaut la branche courante), suivie éventuellement du modèle et de l'effort recommandés. Si la discussion tourne sur un autre modèle ou un autre effort, le dire en une ligne et attendre que l'utilisateur change ou confirme.

La PR est ouverte **après** l'analyse, les corrections et les docs, pas avant : sa description reflète l'état final, l'historique de la PR ne contient pas les allers-retours de correction, et ce qui n'a pas encore été relu n'est pas publié.

## 1. État
- `git status --short` (propre, sinon demander quoi en faire), `git fetch`, `git branch --show-current`. Jamais de PR depuis `dev` ni `master` ; sur `dev` avec des modifications : demander l'objectif, créer la branche de sujet d'après lui.
- `git log --oneline dev..<branche>` et `git diff --stat dev...<branche>`.
- Un plan existe-t-il (`docs/plan/`, sa section « Bilan ») ? Le lire en entier : il décrit ce que la branche devait faire et ce qui est à vérifier à la main.

## 2. Analyser le diff (rien n'est modifié ici)
- Lire `git diff dev...<branche>` par extraits (fichier par fichier) ; ne relire en entier que ce qui le demande.
- Chercher : bugs (états, cas limites, threads : tout GL sur le thread principal), ressources GL non libérées (`unload`/`release`), fuites, écarts au plan, règles de `CLAUDE.md` non respectées (conventions, commandes déclarées avec syntaxe/niveau, imports de `mightylibdemo` depuis `mightylib`), code mort ou doublon avec un modèle existant, tests manquants (logique sans GL non couverte ; pour un bug corrigé, un test qui le reproduit).
- Compiler, puis `mvn -o -q test` (commandes de `CLAUDE.md` selon l'OS).
- En jeu, si rendu, entrées, fenêtre, scènes ou ressources sont touchés : un seul appel `python tools/scenario.py` sur les scènes concernées (prévenir l'utilisateur qu'une fenêtre s'ouvre) ; `leakcheck <Scène>` si des ressources GL de scène ont changé. Journal : 0 ligne de problème.
- Rendre la liste des erreurs, du plus grave au moins grave : fichier:ligne, le problème, la correction proposée. « Rien trouvé » est une réponse valable, à dire telle quelle avec ce qui a été vérifié.

## 3. Discuter, puis corriger
- Attendre la réponse de l'utilisateur : il écarte, précise ou ajoute. Ne rien corriger avant.
- Corriger ce qui est retenu, un commit par changement logique sur la branche, message en anglais au style des précédents (`git log --oneline -10`), terminé par la ligne `Co-Authored-By` demandée par le système. Relancer compilation et tests, et la vérification en jeu des zones touchées.
- Un problème de conception découvert en corrigeant : s'arrêter et demander. Un bug hors du sujet de la branche : le noter (règle de `CLAUDE.md`), pas le corriger ici.

## 4. Docs (commit de docs sur la même branche)
- Appliquer la règle « Fin de chaque objectif » de `CLAUDE.md` (`current.md`/`later.md`, `architecture.md`, relecture).
- En plus : `debug-tools.md` si une commande, un outil ou une étape de `scenario.py` change ; `decisions.md` si une décision durable est née ; `CLAUDE.md` (liste du code couvert par les tests, conventions) ; `licence.md` pour une nouvelle dépendance ou ressource.
- Le plan de `docs/plan/` de ce sujet : le supprimer (et `docs/plan/` s'il est vide).

## 5. PR et fusion
- `git push -u origin <branche>`, puis `gh pr create --base dev` (jamais `master`) : titre court, description en anglais, structurée :
  - **Summary** : ce que fait la branche et pourquoi, par points ; le « Bilan » du plan s'il existe ;
  - **Fixes** : les erreurs trouvées à l'analyse et corrigées ;
  - **Test plan** : ce qui a été exécuté (tests, scènes) avec cases cochées, et ce que l'utilisateur doit vérifier à la main (quelle scène, quoi observer) ;
  - se termine par la ligne d'attribution des pull requests demandée par le système.
- Fusionner : `gh pr merge <n> --merge --delete-branch` (commit de fusion, pas de squash).
- `git checkout dev`, `git pull origin dev`, `git branch -d <branche>` si elle existe encore en local ; `git status --short` propre.

## 6. Compte rendu
Lien de la PR (`[owner/repo#n](url)`), les erreurs corrigées et celles écartées, ce qui a été vérifié (tests, scènes), ce qui ne l'a pas été et comment le vérifier à la main, l'état de `dev` (par rapport à `origin` et à `master`).

## Version
`master` ne reçoit jamais de fusion par ce skill : la procédure de version est dans `CLAUDE.md` (règle Git), seulement sur demande explicite de l'utilisateur.
