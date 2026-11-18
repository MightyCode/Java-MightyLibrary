# Paquet de démonstration

**Statut : validé.**

Décisions prises :
- Le paquet est une archive jointe à une release GitHub de ce dépôt, sous un tag à part (`demo-assets-N`), indépendant des versions du moteur.
- Il ne contient que `models/backpack/` (le seul modèle lourd utilisé). `weapon`, `world/tree` et `basicShape` ne servent à aucune scène et ont une licence inconnue : ils ne sont pas distribués et sortent de `.gitignore` et de `licence.md`.
- Récupération par un script Python, `tools/demo_assets.py`, piloté par un manifeste versionné, `tools/demo_assets.json`.
- Le manifeste donne l'empreinte sha256 de chaque fichier : le script ne télécharge que si un fichier manque ou diffère, et `--check` vérifie sans réseau.
- Les chemins sont relatifs à `resources/` : le paquet pourra accueillir d'autres ressources lourdes (sons, textures, modèles) sans changer le script.
- La discussion A construit l'archive et ne crée la release (`gh release create`, publique) qu'après l'accord explicite de l'utilisateur.
- Licence du backpack : CC BY 4.0 (« CC Attribution » de Sketchfab, page https://sketchfab.com/3d-models/survival-guitar-backpack-799f8c4511f84fab8c3f12887f7e6b36). L'attribution accompagne le modèle : `source_attribution.txt` dans l'archive, `licence.md`, crédits du README.

## Pour démarrer
- Lire : `docs/current.md` (ligne « Paquet de démonstration »), `licence.md` (tableau des ressources), `docs/debug-tools.md` (sections des outils Python), `docs/architecture.md` (passages sur `resources/` à la racine du dépôt et la démo).
- Branche de sujet depuis `dev` : `feature/demo-assets`.
- Compiler et tester avec les commandes de `CLAUDE.md`. Vérifier en jeu avec `tools/scenario.py`, en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `.gitignore` exclut déjà `resources/models/backpack/`, `basicShape/`, `weapon/` et `world/`, sous le commentaire « Demonstration resources, distributed apart from the repository ». Ces dossiers n'ont jamais été commités : l'historique git n'a rien à nettoyer.
- `resources/models/backpack/` (environ 40 Mo) : `backpack.obj`, `backpack.mtl`, `diffuse.jpg`, `specular.jpg`, `normal.png`, `roughness.jpg`, `ao.jpg`, `source_attribution.txt`. Ce modèle est chargé par le batch `resources/batchs/backpack3d.batch` (regex `backpack`) et utilisé par `demo/.../scenes/Scene3D.java` (`createBackpack`, vers la ligne 219).
- Quand le modèle manque, `Scene3D` écrit dans le journal et affiche en rouge « Model 'backpack' missing: copy the demonstration package into resources/models/backpack/ ». Le paquet n'est publié nulle part : la dernière release GitHub est `v0.1.3` (2022).
- `weapon/sabre1.obj`, `world/tree/{leaves,trunk}.obj` et `basicShape/cube.obj` ne sont référencés par aucun code, batch ni JSON.
- La bibliothèque a déjà `FallbackModel`, qui remplace un modèle manquant : ce plan ne touche pas la bibliothèque.
- Les outils Python (`tools/scenario.py`, `coverage.py`, `analyse.py`) utilisent `argparse` et la seule bibliothèque standard. Aucun outil Python n'a de tests automatisés.
- La CI (GitHub Actions, `docs/debug-tools.md`) ne lance aucune scène : elle n'a pas besoin du paquet.
- Usage courant ailleurs : Git LFS est évité pour ces ressources. Les archives « Source code » et « Download ZIP » de GitHub n'incluent pas les fichiers LFS, et le quota de bande passante peut rendre les fichiers inaccessibles. Une archive attachée à une release et récupérée par un script est le modèle répandu.

## Décisions
- Manifeste `tools/demo_assets.json` :
  ```json
  {
    "tag": "demo-assets-1",
    "archive": "mightylib-demo-assets-1.zip",
    "url": "https://github.com/MightyCode/Java-MightyLibrary/releases/download/demo-assets-1/mightylib-demo-assets-1.zip",
    "sha256": "<empreinte de l'archive>",
    "files": { "models/backpack/backpack.obj": "<sha256>", "...": "..." }
  }
  ```
  Les chemins de `files` sont relatifs à `resources/`, avec des `/`.
- `tools/demo_assets.py` utilise la seule bibliothèque standard (`urllib.request`, `zipfile`, `hashlib`, `argparse`) et a trois modes :
  - **sans option** : vérifie chaque fichier du manifeste. Si tous sont à jour, il affiche « up to date » et s'arrête sans réseau. Sinon, il télécharge l'archive de `url` (HTTPS seulement) dans un fichier temporaire, vérifie son sha256 avant d'ouvrir le zip, puis extrait **uniquement** les entrées listées dans `files`, en refusant les chemins absolus et ceux qui contiennent `..`. Chaque fichier est écrit à côté de sa destination puis renommé, et son sha256 est vérifié. Le script finit par un résumé d'une ligne : fichiers installés, fichiers déjà là.
  - **`--check`** : sans réseau. Liste les fichiers manquants ou différents. Code de sortie 0 si tout est à jour, 1 sinon.
  - **`--pack <tag>`** : construit `target/demo-assets/<archive>` à partir des fichiers de `files` présents dans `resources/` (entrées dans un ordre trié), recalcule les empreintes, écrit `tag`, `archive`, `url` et `sha256`. Il refuse de construire si un fichier n'est pas ignoré par git (`git check-ignore`) : une ressource du paquet ne doit jamais être commitée. Ajouter une ressource au paquet = l'ajouter à `files` et à `.gitignore`, puis faire un nouveau `--pack demo-assets-<N+1>`.
- Erreurs (pas de réseau, HTTP 404, empreinte fausse) : un message d'une ligne qui dit quoi faire, code de sortie 2, aucun fichier partiel laissé dans `resources/`.
- Le jeu ne télécharge jamais rien lui-même : pas de réseau ni de surface d'attaque dans la démo.
- Le message de `Scene3D` donne la commande : « Model 'backpack' missing: run python tools/demo_assets.py ». Aucun mécanisme générique dans la bibliothèque.

## Étapes
1. **Plan** : commiter ce fichier tel quel.
2. **Nettoyage des ressources non distribuées et licence** (fusionnable seule). Avant l'étape, l'utilisateur a supprimé ou sorti du dépôt `resources/models/weapon`, `world` et `basicShape` (voir « Questions restantes »).
   - `.gitignore` : ne garder que `resources/models/backpack/` sous le commentaire, en précisant que la liste suit `tools/demo_assets.json`.
   - `licence.md` : la ligne `models/backpack/*` passe à CC BY 4.0 (auteur Berk Gedik, adaptation Joey de Vries, URL Sketchfab ; obligation : attribution, indiquer les modifications). Supprimer la ligne `models/weapon`, `models/world/tree`, `models/basicShape`. Ajouter que le paquet de démonstration est distribué à part, avec son attribution.
   - Vérification : `git status` propre.
3. **Script et manifeste** : `tools/demo_assets.py` et `tools/demo_assets.json`, avec `files` rempli à partir de `resources/models/backpack/`. `url` et `sha256` restent vides jusqu'à l'étape 4.
   - Vérifications : `python tools/demo_assets.py --check` → 0 ; modifier un octet d'une copie temporaire d'un fichier (jamais l'original), puis vérifier la détection → 1. Une archive de test fabriquée dans le scratchpad avec une entrée `../x` ou non listée doit être refusée (fonction d'extraction appelée directement, ou petite option cachée de test : au choix de l'implémentation, à dire dans le Bilan).
4. **Publication du paquet**. `python tools/demo_assets.py --pack demo-assets-1`, puis montrer à l'utilisateur la commande `gh release create demo-assets-1 target/demo-assets/mightylib-demo-assets-1.zip --target dev --title "Demo assets 1" --notes "<contenu, licence CC BY 4.0 et attribution du backpack, comment l'installer>"`, en précisant qu'elle crée un tag et une release **publics**. Ne la lancer que sur son oui. Commiter le manifeste complet.
   - Vérification de bout en bout : **déplacer** (jamais supprimer) `resources/models/backpack/` dans le scratchpad, lancer `python tools/demo_assets.py` (téléchargement réel), puis `--check` → 0, et comparer avec la copie déplacée. En cas d'échec, remettre la copie en place. Relancer le script : « up to date » sans réseau.
5. **Démo et README** : message de `Scene3D` (commande à lancer) ; section « Paquet de démonstration » du README (ce qu'il contient, `python tools/demo_assets.py`, scènes concernées : `Scene3D`) ; crédits du backpack dans le README.
   - Vérification en jeu, sans le paquet (déplacé) : `python tools/scenario.py "scene Scene3D" wait-loaded "?resources"` → message dans le journal, aucune exception. Avec le paquet : la même commande charge `backpack`. Vérifier aussi `Scene3DBis`, `Game3D2D` et `TerrainGeneration3D` (elles ne dépendent pas du paquet : elles doivent rester intactes).
6. **Documents** (liste ci-dessous) et section « Bilan » de ce plan.

Aucun test JUnit : le changement ne touche pas de logique Java testable (seulement un message de `Scene3D`). Tests du moteur à relancer seulement si du Java change : `mvn -o -q test`.

## Démo
Pas de nouvelle scène : `Scene3D` est déjà la scène qui montre le modèle et son absence.

## Documents à mettre à jour à la fin
- `docs/architecture.md` : les ressources de la démo dans `resources/` et le paquet de démonstration, au présent (manifeste, script, ce qui se passe sans lui).
- `docs/debug-tools.md` : section `tools/demo_assets.py` (modes, ajouter une ressource au paquet, nouveau tag).
- `CLAUDE.md` : sous « Sur une nouvelle machine », ajouter `python tools/demo_assets.py` pour les scènes 3D.
- `licence.md` : fait à l'étape 2, à relire.
- `docs/current.md` : supprimer la ligne « Paquet de démonstration » et son « À décider ».
- `docs/later.md`, `docs/futur-discussion.md`, `docs/decisions.md` : rien de prévu. Ajouter dans `decisions.md` « ressources lourdes de la démo : release GitHub + script, pas de Git LFS », avec la raison.
- Supprimer ce plan (discussion B).

## Hors périmètre
- Paquets pour les jeux faits avec le moteur (téléchargement de contenu, DLC, mods) : relève du modding (`later.md`).
- Préchargement GL des modèles : déjà noté dans `later.md`.
- Retrouver la licence de `stall.obj` / `stallTexture.png` (versionnés, « inconnu » dans `licence.md`) : reste dans « à faire avant distribution » de `licence.md`.

## Questions restantes
- `resources/models/weapon`, `world` et `basicShape` sont-ils supprimés ou sortis du dépôt avant l'étape 2 ? (C'est à l'utilisateur de le faire : sinon, ils apparaîtront comme fichiers non suivis.)

## Modèles recommandés
- Discussion A, étapes 1 à 6 : Claude Sonnet 5.5, effort `medium`. Petit script en bibliothèque standard et docs, sans conception d'API. La partie délicate (extraction sûre, vérification des empreintes) est entièrement décrite ci-dessus.
- Discussion B (`/pr-ml`) : Claude Sonnet 5.5, effort `medium`. Diff court ; relire surtout la sécurité de l'extraction et la cohérence du manifeste.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/paquet-demonstration.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/demo-assets`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (sur une copie fraîche du dépôt : `python tools/demo_assets.py`, puis `Scene3D` affiche le backpack éclairé ; sans le paquet, message rouge avec la commande ; la release `demo-assets-1` visible sur GitHub avec ses notes de licence). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/demo-assets`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
Fait : étapes 1 à 6. Release publique `demo-assets-1` créée (archive de 36 Mo, 8 fichiers) ; manifeste complet commité.

Écarts au plan :
- `weapon`, `world` et `basicShape` sont toujours sur le disque de l'utilisateur (ignorés par l'ancien `.gitignore`, maintenant visibles comme non suivis) : à supprimer ou sortir du dépôt par lui.
- Pas d'option cachée de test : les tests de l'extraction ont appelé `demo_assets.extract`/`status` directement depuis un script du scratchpad (non versionné).

Vérifié :
- `--check` : 0 sur l'état actuel ; fichier modifié ou manquant détecté ; archives avec `../x`, `/abs`, `C:/x`, `models/../../x` refusées ; mauvaise empreinte refusée sans fichier `.part` ; entrée non listée non extraite.
- De bout en bout : dossier `backpack` déplacé, `python tools/demo_assets.py` (téléchargement réel) → 8 installés, `--check` 0, `diff -r` identique à la copie ; relance : « up to date » sans réseau.
- `mvn -o -q compile` OK. `scenario.py "scene Scene3D" wait-loaded "?resources"` : sans le paquet, ligne « Model 'backpack' missing: run python tools/demo_assets.py » dans le journal, 0 problème ; avec le paquet, 3 modèles chargés. `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` : aucune erreur dans le journal.

À vérifier à la main : sur une copie fraîche du dépôt, `python tools/demo_assets.py` puis `Scene3D` affiche le backpack éclairé ; sans le paquet, message rouge avec la commande ; release `demo-assets-1` visible sur GitHub avec ses notes de licence.
