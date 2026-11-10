---
name: implement-next-objective
description: Implémente un objectif donné par l'utilisateur, ou à défaut le prochain de docs/current.md, quand il est assez petit pour ne pas demander de plan : branche depuis dev, code, tests, vérification en jeu, docs, puis s'arrête avant la PR et propose la commande /pr-ml à copier. Redirige vers /planification-ml si le point est gros ou demande des choix de conception. À utiliser quand l'utilisateur dit /implement-next-objective [objectif].
disable-model-invocation: true
---

# Implémenter le prochain objectif

Le travail d'un développeur qui prend la prochaine ligne de `docs/current.md`, la fait proprement, et la rend prête pour `/pr-ml`. **Ce skill n'ouvre jamais de PR et ne touche jamais à `master`.**

## 0. Choisir l'objectif
- Argument optionnel : **un objectif en texte libre** (il n'a pas besoin d'être dans `docs/current.md`), ou le numéro d'une ligne du fichier (dans l'ordre). Sans argument : **la première ligne de `current.md`**, ou la ligne marquée (U) si elle existe. Dire en une phrase l'objectif retenu avant de commencer.
- `current.md` vide : le dire, indiquer le point en tête de `docs/later.md`, proposer `/planification-ml <sujet>` et s'arrêter.
- Un plan existe-t-il (`docs/plan/<sujet>.md`) ? Le lire en entier et le suivre à la place du cadrage de l'étape 2.

## 1. État
- `git status --short` propre (sinon demander quoi en faire), `git fetch`, être sur `dev` à jour de `origin/dev` (`git pull`). Jamais de travail directement sur `dev` ni `master`.
- Si la dernière revue de bonnes pratiques date de 7 jours ou plus (`CLAUDE.md`), le rappeler en une ligne.

## 2. Cadrer (rien n'est modifié)
- Lire les extraits utiles : la ligne de `current.md`, la section de `later.md` ou `architecture.md` qui la concerne, `decisions.md` s'il y a une décision proche, puis le code concerné (Grep, Read avec offset/limit, pas de fichiers entiers).
- **Le point est-il « simple » ?** Oui si : peu de fichiers, aucune décision de conception ouverte, aucune API publique à choisir ou casser sans accord, pas de nouveau système. Sinon : le dire, dire pourquoi, proposer `/planification-ml <sujet>` et **s'arrêter** (jamais de gros point sans plan, règle de `CLAUDE.md`).
- Écrire un cadrage de quelques lignes : fichiers touchés, approche, tests à ajouter, comment vérifier, ce qui reste hors périmètre. `current.md` demande de **valider les propositions avec l'utilisateur avant de les appliquer** : attendre son accord dès qu'un choix existe (nom, signature, comportement) ; pour une correction purement mécanique (renommage, suppression de code mort), l'annoncer et continuer.

## 3. Implémenter
- Branche de sujet depuis `dev`, nom court en kebab-case tiré de l'objectif. Si un plan non suivi existe, le commiter en premier.
- Un commit par changement logique, message en anglais au style de `git log --oneline -10`, terminé par la ligne `Co-Authored-By` demandée par le système. Ne pas commiter `target/` ni `.idea/workspace.xml`.
- Respecter `CLAUDE.md` : Java 11, conventions, commandes déclarées avec syntaxe et niveau, thread principal pour le GL, ressources libérées dans `unload`/`release`, jamais d'import de `mightylibdemo` depuis `mightylib`, 3D seulement au strict nécessaire (mais jamais cassée).
- Éditer avec Edit (jamais `sed` sur un source) ; à la fin de chaque changement, relire son diff comme l'analyseur IntelliJ (`bonnes-pratiques.md`, point 14 : imports devenus inutiles, diamant, `isEmpty()`, redondances).
- Compiler après chaque modification (commande de `CLAUDE.md` selon l'OS).
- Rien en douce : un bug existant hors sujet est noté dans `later.md` ou `futur-discussion.md` et signalé, pas corrigé ; pas de refactor non demandé (en proposer, avec autorisation, voir la mémoire « refactors sur demande »). Si une vraie question de conception apparaît en cours de route : s'arrêter et demander.

## 4. Vérifier
- Tests : se demander si le changement touche du code couvert (liste de `CLAUDE.md`) ou couvrable (logique sans GL) ; ajouter ou compléter le test, et pour un bug un test qui le reproduit (vérifier qu'il échoue sans le correctif). `mvn -o -q test` doit être vert.
- En jeu, si rendu, entrées, fenêtre, scènes ou ressources sont touchés : **un seul appel** `python tools/scenario.py "scene <Scène>" wait-loaded "?gl summary" …` sur les scènes concernées (prévenir l'utilisateur qu'une fenêtre s'ouvre) ; `leakcheck <Scène>` si des ressources GL de scène ont changé. Journal : 0 ligne de problème. Sans résultat après 20 s, considérer le jeu bloqué.
- Noter ce qui n'a pas pu être vérifié et comment le vérifier à la main (quelle scène, quoi observer).

## 5. Docs (commit séparé sur la même branche)
- Règle « Fin de chaque objectif » de `CLAUDE.md` : supprimer la ligne de l'objectif dans `current.md` (ou la réduire à son reste), sans « fait » ni historique (un objectif donné en texte libre et absent du fichier : rien à retirer) ; décrire le nouvel état dans `architecture.md` au présent ; relire les fichiers modifiés.
- Si besoin : `debug-tools.md` (commande, outil, étape de `scenario.py`), `decisions.md` (décision durable), `CLAUDE.md` (liste du code couvert par les tests, conventions), `licence.md` (dépendance ou ressource).
- `current.md` devenu vide : le dire, c'est le moment de discuter du point suivant de `later.md`.

## 6. Rendre la main
Ne pas pousser, ne pas ouvrir de PR (`/pr-ml` est réservé à l'utilisateur). Répondre brièvement : ce qui a été fait (commits), ce qui a été vérifié (tests, scènes), ce qui ne l'a pas été et comment le vérifier à la main, ce qui a été noté hors sujet. Terminer par le prompt de PR **dans un bloc de code, à copier tel quel**, avec le modèle et l'effort recommandés pour la relecture (modèle et effort élevés si le diff touche une logique délicate ou un modèle durable, plus légers pour du mécanique) :

```
/pr-ml <branche> — modèle recommandé : <modèle>, effort <effort> (<raison courte>).
```
