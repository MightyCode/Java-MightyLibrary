# Couverture réelle

**Statut : validé.**

Ce plan couvre le point « Couverture réelle » de `docs/current.md`. La comparaison d'images (`feature/image-comparison`) et les régions de texture (`feature/texture-regions`) sont fusionnées dans `dev` : leurs tests sont mesurés eux aussi.

Décisions prises :
- Ce qui est mesuré : le code de `mightylib` et du kit `mightylib-test`, par tous les tests des trois modules, tests GL compris. Le code de la démo n'est pas mesuré.
- Les exclusions des paquets GL sont retirées.
- Le CI mesure la couverture à chaque push. Il publie le rapport en artefact et écrit un résumé par paquet dans le résumé du job.
- Un plancher global sur les lignes, vérifié par le CI. Il vaut la valeur mesurée, arrondie à l'unité inférieure, et se relève à la main.
- `tools/coverage.py` lance par défaut tout ce que le CI lance. `--quick` garde le mode actuel.
- Zones à tester, par leurs chemins principaux, sans pourcentage visé :
  - caméras 2D et leurs composants ;
  - `graphics/game` (tuiles) ;
  - GUI et texte ;
  - cycle de vie des scènes.
- Audit des corrections de novembre 2026 : chaque bug corrigé a un test qui le reproduit, ou une ligne dans `later.md` qui explique pourquoi il n'en a pas.

## Pour démarrer
- Lire :
  - `docs/architecture.md`, sections « Kit de test (`mightylib-test`) » et « Boucle » ;
  - `docs/debug-tools.md`, partie couverture ;
  - `docs/bonnes-pratiques.md`, le point sur la couverture s'il existe.
- Fichiers à lire :
  - `pom.xml`, profil `coverage` (JaCoCo 0.8.16) ;
  - `tools/coverage.py` ;
  - `.github/workflows/ci.yml` ;
  - `demo/pom.xml`, `game-parent/pom.xml` (le kit en portée `test`).
- Branche de sujet : `feature/real-coverage`, depuis `dev`.
- Compiler et tester avec les commandes de `CLAUDE.md`. Vérifier en jeu avec `tools/scenario.py`, en prévenant l'utilisateur.

## Contexte : ce qui existe
- **Profil `coverage` du pom racine** : `prepare-agent`, puis `report` en phase `test`.
  - `coverage.py` le lance seulement sur `-pl mightylib`.
  - Il exclut les paquets liés à GL, OpenAL ou GLFW : `graphics/renderer`, `shader`, `surface`, `lighting`, `debug`, `GenDeleteResources`, `Screenshot`, `SoundManager`, `SoundListener`, `SoundSource`, `main/monitors`.
  - Aucun `argLine` n'est déclaré dans les poms : celui de `prepare-agent` est donc repris par Surefire. Garder cette condition (`@{argLine}` si un `argLine` est ajouté un jour).
- **Défaut** : les tests GL du kit et de la démo, qui sont ceux qui exercent le rendu, les caméras pilotées et les scènes, ne sont jamais comptés. Par exemple, `cameraComponents` est à 1 % alors que `FreeCamera2DComponentTest` existe.
- **Chiffres au 2026-11-26** (sans GL, `mightylib` seul) : `scenes` 14 %, `scenes/camera` 31 %, `cameraComponents` 1 %, `graphics/game` 14 %, `graphics/gui` 35 %, `graphics/text` 45 %, `resources/models` 9 %, `network` 12 %, total 59 %.
- **Exécution des tests de la démo** : en `verify`/`package`, la démo est testée contre les jars de la bibliothèque et du kit. JaCoCo identifie une classe par l'empreinte de ses octets : celles du jar sont les mêmes que celles de `target/classes`, donc l'agrégation reste correcte.
- **Agrégation multi-module** : la pratique usuelle est le but `report-aggregate` (depuis JaCoCo 0.7.7). Il est lancé dans le module qui dépend des autres, ici `demo`, et lit les fichiers `target/*.exec` de chaque module dont il dépend. `jacoco:check` ne vérifie pas un rapport agrégé : le plancher est donc vérifié par `coverage.py`.
- **Tests existants des zones visées** :
  - `Camera2DZoomTest`, `CameraVersionTest`, `CameraProjectionTest`, `FreeCamera2DComponentTest` ;
  - `TileVisibilityTest`, `TileMapRendererTest` (régions des tuiles, min puis max, et bords pour chaque référence), `TileMapBatchGLTest`, `TextureRegionsGLTest` (régions des tuiles et des animations au pixel) ;
  - `GUI`/`GUIList`/`TextFieldModel`/`ListSelection`, l'arrondi de `Slider` ;
  - `FontFile`, `DrawnTexts`/`TextWrap` ;
  - `SceneRegistry`, `SceneRunnerTest`, le test de fumée.

## Décisions
1. **Agent sur les trois modules et rapport agrégé dans `demo`.**
   - `report-aggregate` est lancé en phase `test` de `demo` (dernier module du réacteur, donc `mvn -P coverage test` suffit) et en `verify`.
   - Il se limite à `io/github/mightycode/mightylib/**`, ce qui écarte le code de la démo. Le kit est dans ce paquet (`.../mightylib/test`).
   - Le `report` de chaque module reste disponible pour un module seul.
2. **Exclusions GL retirées.** Il n'y a plus d'exclusion, sauf si une classe ne peut vraiment être exécutée par aucun test (par exemple `main/monitors`, qui dépend des écrans réels) : la garder alors avec sa raison en commentaire du pom.
3. **`tools/coverage.py`**
   - Sans option : `mvn -B -P coverage test` sur tout le réacteur, tests GL compris, puis le résumé par paquet du rapport agrégé (`demo/target/site/jacoco-aggregate/jacoco.xml`).
   - `--quick` : l'ancien mode, `-pl mightylib`, sans GL (`-DexcludedGroups=gl`), rapport du module.
   - `--no-run`, `--classes` et `--lines` : inchangés, sur le rapport choisi.
   - `--markdown` : un tableau pour `$GITHUB_STEP_SUMMARY`.
   - `--check` : échec si le total des lignes passe sous le plancher.
   - `--from-ci [run-id]` : `gh run download` de l'artefact `coverage`, puis le résumé.
4. **Plancher** dans `tools/coverage_floor.json` (`{"lines": N}`), lu par `--check`. N = total mesuré par le CI, arrondi à l'unité inférieure. On le relève dans le commit qui ajoute des tests. Le plancher n'est pas vérifié par `mvn test` : seulement par le CI et par `coverage.py --check`.
5. **CI**
   - L'étape principale « Compile, test and package » lance `xvfb-run -a mvn -B -P coverage verify`, en gardant ses propriétés (`mightylib.test.requireGL`, `requireDemoAssets`, `requireReferences`), ses variables (`GALLIUM_OVERRIDE_CPU_CAPS`, `LP_NATIVE_VECTOR_WIDTH`) et l'artefact `references` en cas d'échec.
   - Ensuite `python3 tools/coverage.py --no-run --markdown >> $GITHUB_STEP_SUMMARY`, puis `python3 tools/coverage.py --no-run --check`.
   - Artefact `coverage` = `demo/target/site/jacoco-aggregate/`, avec `if: always()` quand le rapport existe.
6. **Tests par zone**, sur les chemins principaux, chacun à sa place :
   - logique sans GL dans `mightylib/src/test` ;
   - moteur ou fenêtre nécessaire dans les tests du kit (`mightylib-test/src/test`) ;
   - comportement d'une scène de démo dans `demo/src/test`.

   Les chemins que ce plan laisse sans test vont dans `later.md`, avec leurs chiffres.
7. **Audit** des commits de correction de `dev` depuis le 2026-11-01 (`git log --since=2026-11-01 --oneline dev`, filtré sur fix, bug et corrige).
   - Pour chacun, citer le test qui le reproduit. Sinon, en écrire un, ou écrire une ligne dans `later.md` qui dit pourquoi ce n'est pas testable.
   - Les tests `@Disabled` existants sont listés avec leur raison.
   - Le résultat est un tableau dans le « Bilan ».

## Étapes (un commit chacune)

**1. Plan.** Commiter ce plan tel quel.

**2. Rapport agrégé**
- Fichiers : `pom.xml` (profil `coverage` : exclusions retirées, `report-aggregate` déclaré pour `demo`), et `demo/pom.xml` si l'exécution doit y vivre.
- **Vérifier d'abord** que `report-aggregate` prend bien `mightylib-test`, qui est en portée `test` dans la démo. Sinon, le déclarer aussi en portée `compile` dans un profil, ou agréger depuis un autre point. S'arrêter et demander si aucune solution propre n'existe.
- Résultat attendu : `mvn -P coverage test` produit `demo/target/site/jacoco-aggregate/jacoco.xml`, avec les paquets de `mightylib` et du kit, et sans `mightylibdemo`.
- Tous les tests restent verts sous l'agent, en particulier ceux qui comptent les allocations (`getThreadAllocatedBytes` : `DrawBatch2DTest`, `Animation2DBatchGLTest`, `BatchStress2DGLTest`…) et les images comparées aux références. Un écart dû à l'instrumentation : s'arrêter et demander (ne pas relâcher le test).
- Noter les vrais chiffres par paquet dans le « Bilan ».

**3. `coverage.py`**
- Le défaut complet, `--quick`, `--markdown`, `--check`, `--from-ci` ; `tools/coverage_floor.json`.
- Tests : à la main. `--quick` doit donner les anciens chiffres ; le défaut, les nouveaux ; `--check` doit échouer avec un plancher fictif à 99.

**4. CI**
- `ci.yml` : profil `coverage`, résumé, `--check`, artefact.
- Pousser la branche et vérifier que le résumé du job montre le tableau et que l'artefact contient le rapport HTML.
- Fixer le plancher au total mesuré par le CI.

**5. Caméras 2D et composants**
- Classes : `Camera2D` (conversions écran/monde et monde/écran sous zoom, rotation, translation, taille virtuelle ; `setReferenceCamera` si concerné), `DraggingCameraComponent`, `MovingCameraComponent`, `ZoomingCameraComponent` (avec `SceneRunner` et des entrées simulées), `DebugInfoCamera2D` si elle a une logique.
- Tests sans GL quand la classe le permet ; sinon dans le kit.
- `FreeCamera3DComponent` et `DebugInfoCamera3D` restent hors périmètre (la 3D est secondaire).

**6. `graphics/game` (tuiles)**
- Classes : `TileMapRenderer` et `LayersTileMapRenderer`.
- À tester : création depuis une carte et un tileset, calques (ordre, visibilité), changement de tuile puis mise à jour, libération sans fuite (le kit vérifie les fuites autour du test). Les régions et les bords des tuiles par référence sont déjà couverts (`TileMapRendererTest`, `TextureRegionsGLTest`) : ne pas les refaire.

**7. GUI et texte**
- `graphics/gui` : navigation entre widgets (clavier, souris), états (survol, sélection, désactivé), valeurs des widgets non couverts (`Slider` hors arrondi, cases, listes). Utiliser `SceneRunner` et les entrées simulées si une fenêtre est nécessaire.
- `graphics/text` : `Text` (mise en page, alignements, ancres, boîtes, changement de texte et de taille, retours à la ligne).
- Ne pas refaire ce que couvrent `GUI`/`GUIList`/`TextFieldModel`/`ListSelection` et `DrawnTexts`/`TextWrap`.

**8. Cycle de vie des scènes**
- Classes : `SceneManager` et `TemplateSceneLoading`.
- À tester :
  - changement de scène et ordre `init`/`launch`/`unload`/`release` ;
  - sortie pendant le chargement (`release` appelé, pas `unload`) ;
  - scène qui échoue au chargement ;
  - `exit(status)` ;
  - écran de chargement.
- Le kit (`SceneRunner`) est le bon niveau. Compléter `SceneRunnerTest` seulement pour ce qui est propre au kit.

**9. Audit des corrections de novembre 2026**
- Le tableau du « Bilan » ; les tests manquants ajoutés, ou les lignes écrites dans `later.md`.

**10. Plancher relevé et documents**
- Plancher au nouveau total mesuré par le CI, puis documents (liste plus bas).

Les étapes 2 à 4 peuvent être fusionnées seules. Chacune des étapes 5 à 8 aussi, une fois les étapes 2 à 4 faites.

## Démo
Aucune nouvelle scène prévue. Si une zone ne peut être testée qu'avec une scène dédiée, l'ajouter à `DemoScenes`, avec sa description et la caméra pilotée, plutôt que modifier une scène existante.

## Documents à mettre à jour à la fin
- `architecture.md`, « Kit de test » ou une section sur les tests : couverture agrégée, ce qui est mesuré, plancher.
- `debug-tools.md` : `coverage.py` (défaut complet, `--quick`, `--markdown`, `--check`, `--from-ci`), artefact `coverage`, plancher (`tools/coverage_floor.json`).
- `bonnes-pratiques.md` : le point sur la couverture lit le rapport agrégé ; le plancher se relève quand on ajoute des tests.
- `CLAUDE.md` :
  - la phrase sur `coverage.py` (« JaCoCo, 20 s » devient : complet par défaut, `--quick` ≈ 20 s) ;
  - les nouveaux tests dans la liste des tests couverts ;
  - la revue hebdomadaire lit le rapport agrégé.
- `later.md` : les chemins non testés des zones visées, avec leurs chiffres ; `network`, `resources/models` et la 3D, avec leurs chiffres réels ; les bugs non testables de l'audit.
- `current.md` : supprimer la ligne « Couverture réelle ». Si les deux plans sont fusionnés, le fichier est vide : rappeler alors de discuter du point en tête de `later.md` (kit de test).
- Supprimer ce plan (fait par `/pr-ml`).

## Hors périmètre
- `network`, `resources/models` et 3D (`FreeCamera3DComponent`…) : les chiffres sont notés dans `later.md`, sans test ajouté.
- Planchers par paquet et couverture des branches comme seuil : rapport seulement.
- Couverture du code de la démo.
- Audit des corrections avant novembre 2026 : couvert par la règle existante (un test pour tout bug corrigé).

## Questions restantes
- Aucune. Seul le cas de `report-aggregate` et de la portée `test` (étape 2) peut demander une décision, s'il n'y a pas de solution propre.

## Modèles recommandés
- Discussion A :
  - étapes 2 à 4 (Maven, Python, CI : configuration délicate mais courte) : Claude Opus 5.5, effort `medium` ;
  - étapes 5 à 9 (tests qui demandent de comprendre les comportements attendus des caméras, du GUI, du texte et du cycle de vie) : Claude Opus 5.5, effort `high` ;
  - étape 10 (docs) : Claude Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high`, pour juger si les tests vérifient vraiment un comportement et ne font pas que de la couverture.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/couverture-reelle.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre modèle, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/real-coverage`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : ce qui a été fait, les écarts au plan et leur raison, ce qui a été vérifié et comment, ce que l'utilisateur doit vérifier à la main. Y mettre aussi les chiffres avant et après par paquet, et le tableau de l'audit. Pour l'utilisateur :
  - lancer `python tools/coverage.py` et `--quick` sur sa machine ;
  - lire le résumé du job du CI et l'artefact `coverage`.

  La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/real-coverage`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour.
- Vérifications manuelles à faire, reprises du « Bilan » : `tools/coverage.py` (défaut et `--quick`) sur la machine de l'utilisateur, le résumé du job et l'artefact `coverage` du CI.
