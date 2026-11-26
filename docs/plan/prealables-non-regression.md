# Préalables des tests de non-régression : dossier de données des tests, `Animation` testable, bugs de caméra et de tilemap

**Statut : validé.**

Trois objectifs de `docs/current.md` (gros point « Tests de non-régression ») réunis dans un plan : ils préparent le test de fumée de toutes les scènes et la comparaison d'images (plans suivants), et sont petits chacun.

Décisions prises :
- Périmètre : « Dossier de données du joueur des tests », « `Animation` / `Animator` testables sans moteur », « Bugs connus reproduits et corrigés » (caméras, `TileMapRenderer`).
- Dossier de données : **un par moteur de test**, temporaire, supprimé à `close` ; une configuration qui donne déjà son dossier le garde (le kit n'y touche pas).
- `utils/DataFolder` supprimée (code mort, ignore la configuration).
- `Animation(Resources)`, `Animator(Resources)` et `Animation.update(float secondes)` en plus des appels actuels.
- Bugs d'`Animation` trouvés en préparant le plan corrigés ici : `Animator.lateUpdate` n'appelle pas `Animation.lateUpdate` ; durée de frame ≤ 0 (ou NaN) refusée au chargement.
- `Camera` : projections remplacées (`setOrtho`/`setPerspective`) ; drapeaux `shouldUpdate*` et `superUpdate*` supprimés.
- `TileMapRenderer` : décalage de référence calculé par un outil partagé avec `Text` (`Direction.AnchorOffset`), appliqué aux deux bords et au culling, sans `position()`.

## Pour démarrer
- Lire : `docs/current.md` (le point entier), `docs/architecture.md` sections « Kit de test (`mightylib-test`) », « Ressources CPU (`Resources`) » (paragraphe « Configurations en couches »), « Rendu » ; `docs/decisions.md` (philosophie) ; `docs/debug-tools.md` section Commandes si une commande est touchée (aucune prévue).
- Branche de sujet depuis `dev` : `feature/regression-groundwork`.
- Compiler et tester avec les commandes de `CLAUDE.md` (Windows : Maven d'IntelliJ) ; `package` à l'étape 1 (le kit change). Vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
### Dossier de données du joueur
- `EngineConfig.setPlayerDataFolder(String)` / `getPlayerDataFolder()` (`mightylib/.../main/EngineConfig.java`, défaut `DataFolder.DATA_FOLDER_PATH` = `"data/"`, relatif au dossier de travail). `Engine` (≈ l. 350) en fait la couche du joueur : `new FilteredSource(new FolderSource(config.getPlayerDataFolder()), ConfigDataLoader.FOLDER)` au sommet de la pile. Seul `ConfigData.set`/`remove` y écrit (sur un worker, fichier temporaire puis renommage ; la future se complète à l'écriture). Commandes qui écrivent par là : `picture`, `colorspace` (via `IColorService.config()`).
- Le kit (`mightylib-test/.../test/TestEngine.java`) modifie la configuration au `start` (plugin des sources du test, scène de départ, affichage) mais pas le dossier du joueur : un test qui écrit une config crée `data/` dans le dossier du module (`.gitignore` n'ignore que `/data/` à la racine). `close` : `KitEngineAccess.shutdown`, puis `resetProcessState`, `KitTimeAccess.reset`… ; une configuration ne sert qu'à un moteur (`STARTED_CONFIGS`).
- `@MightyEngineTest` : un moteur par test ; `@MightySceneTest` : moteur avec fenêtre gardé entre les tests de la même configuration (le dossier est donc partagé par ces tests).
- `utils/DataFolder` : utilitaire statique (`saveFile`, `getFileContent`…) qui écrit dans `"data/"` en dur ; utilisé seulement par `DataFolderTest`. Mentions : `CLAUDE.md` (liste des tests), `docs/bonnes-pratiques.md` point 8, `docs/futur-discussion.md` (« dossier de données utilisateur »), `docs/later.md` (« Réglages d'affichage du joueur » : « JSON dans `DataFolder` »), `.claude/skills/revue-pratiques/collect.py` (`LEGACY`).
- Modèle à suivre : JUnit `@TempDir` (dossier temporaire du système, supprimé après).

### `Animation` / `Animator` (`mightylib/.../resources/animation/`)
- `Animation` : champ `private final Resources resources = Views.engine().resources();` (seule dépendance au moteur) ; `update()` lit `GameTime.DeltaTime()` (piloté dans les tests sans moteur par `GameTimeControl`, `mightylib/src/test/.../main/utils/`).
- `update()` : `while (elapsedTime >= frameTime && (currentFrame <= frameNumber() || looping))` — la seconde condition est toujours vraie ; une frame de durée 0 boucle à l'infini (et une durée négative aussi).
- `Animator` : `new Animation()` dans `addAndInitAnimation` ; `lateUpdate()` remet `animationChanged` mais n'appelle pas `currentAnimation.lateUpdate()`. `Animation.lateUpdate` n'est appelée nulle part dans la bibliothèque : `lastFrame` reste à `-1` (mis par `restart`), `isCurrentFrameChanged()` est toujours vrai, et `Animation2DRenderer.update` appelle `updateInfoForFrame()` à chaque image. `clear()` ne remet pas `currentAnimationName`.
- `FrameData.init` lit la durée par `TextFormat.decimal(parts, 6, path, line, "frame time")` (erreur d'une ligne avec fichier et ligne via `TextFormat.error`).
- `FallbackAnimationData(Resources)` : dépendance à la texture de repli ; testable avec une `Resources` construite à la main (modèle : `ColorServiceConfigTest`, `new Resources(new Resources.AllResourcesMethod(), 2, sources, List.of(loaders…))`).
- Tests existants : `AnimationDataTest`, `FrameTexturePositionTest` (sans GL), `Animation2DBatchGLTest` (kit, `new Animation()` sous moteur). Couverture de `resources/animation` : 42 % des lignes.

### Caméras (`mightylib/.../scenes/camera/`)
- `Camera2D.writeProjection` : `projection.ortho(…)` ; `Camera3D.writeProjection` : `projection.perspective(…)` / `ortho(…)`. JOML `ortho`/`perspective` **multiplient** la matrice existante ; `setOrtho`/`setPerspective` la remplacent. Un second calcul (`Camera2D.invertView`, `Camera3D.setViewAngle`, `setViewMode`, ou tout `updateProjection()`) compose donc les projections. `ConsoleView.PixelCamera` utilise déjà `setOrtho`.
- `Camera` : `shouldUpdateProjection`/`shouldUpdateView` (vrais à la construction, jamais remis à faux par le moteur), `superUpdateProjection()` (appelée par `Camera3D.setViewAngle` et `setViewMode`), `superUpdateView()` (teste `shouldUpdateProjection`, appelée nulle part), setters et getters publics. Aucun autre appelant dans la bibliothèque, la démo ou le kit.
- Aucune scène de la démo n'appelle `setViewAngle`, `setViewMode` ou `invertView` après la construction : la projection n'y est calculée qu'une fois, la correction ne doit rien changer (à vérifier).
- Tests : `CameraVersionTest` (construit les caméras sans GL avec un `WindowInfo` par réflexion, taille virtuelle 0), `Camera2DZoomTest`.

### `TileMapRenderer` (`mightylib/.../graphics/game/TileMapRenderer.java`)
- `loadTileMapInformation` : `leftUpPosition` = `position()`, puis selon `reference` x = `-w/2` ou `-w` et y = `+h/2` (`None`, `Left`, `Right`) ou `-h` (`LeftDown`, `Down`, `RightDown`) — valeurs absolues qui écrasent `position()`.
- `getTilePosition(x, y)` : `x * ts` à `(x + 1) * ts - (leftUp.x - position.x)` : seul le bord de fin est décalé, les tuiles changent de taille. `tileEndX/Y` (même formule) alimentent `TileVisibility.Range(…, start = 0, end = tileEnd…)` : le culling ne décale que la fin.
- La position du renderer passe par la matrice du modèle (`modelMatrix()`), les sommets sont locaux : le décalage de référence ne doit pas lire `position()`.
- Signe : `Text` (convention du moteur, y vers le bas) décale de `-h/2` pour `None`/`Left`/`Right` et `-h` pour le bas (`Text.java` ≈ l. 355–388, `textReference` soustrait) ; la tilemap met `+h/2` : second bug.
- `setReference` n'est appelé sur une tilemap par aucune scène de la démo.
- Tests : `TileMapRendererTest` (`setTexturePosition`, sans GL), `TileVisibilityTest`, `TileMapBatchGLTest` (démo, carte au pixel, caméra zoomée, tournée, hors champ, tuiles comptées).
- `utils/math/geometry/Direction` : utilitaires statiques sur `EDirection` (pas encore de décalage d'ancre).

## Décisions
- Le kit crée le dossier du joueur par `Files.createTempDirectory("mightylib-player-")` (dossier temporaire du système, comme `@TempDir`) au `start`, si la configuration a encore le dossier par défaut (`EngineConfig.DEFAULT_PLAYER_DATA_FOLDER`, comparaison de chaîne) ; sinon il garde celui donné, sans jamais l'effacer. Pourquoi : un test qui relit ce qu'il a écrit, ou qui veut garder son dossier, passe son propre `@TempDir`.
- Suppression à `close`, après l'arrêt du moteur (écritures des workers terminées) ; un échec de suppression écrit une ligne `--` et n'échoue pas le test.
- `TestEngine.playerDataFolder()` → `Path` du dossier utilisé (créé ou donné).
- Moteur gardé par `@MightySceneTest` : dossier partagé par ses tests ; un test de scène qui change une config la remet (écrit dans `architecture.md`).
- `DataFolder` supprimée sans remplaçant ; `DATA_FOLDER_PATH` devient `EngineConfig.DEFAULT_PLAYER_DATA_FOLDER` (`"data/"`). Les données du joueur autres que `configs/` (sauvegardes) n'ont pas d'API : noté dans `later.md`.
- `Animation(Resources)` et `Animator(Resources)` : constructeurs ajoutés ; `Animation()`/`Animator()` gardés (ressources du moteur de la vue courante, comme aujourd'hui) ; `Animator(Resources)` passe sa `Resources` à chaque `Animation` créée.
- `Animation.update(float seconds)` : avance de ce temps (× vitesse) ; `update()` = `update(GameTime.DeltaTime())`. `Animator.update(float)` de même.
- Condition morte de la boucle d'`update` retirée (comportement inchangé à durée > 0).
- `Animator.lateUpdate()` appelle `currentAnimation.lateUpdate()` (si non nulle) : `isFrameChanged()` n'est vrai qu'à l'image du changement. `clear()` remet aussi `currentAnimationName` à `null`.
- Durée de frame : `FrameData.init` refuse une durée ≤ 0 ou non finie par `TextFormat.error` (« frame time must be greater than 0, got … », fichier et ligne) : l'animation échoue au chargement comme pour une autre erreur de format (repli, ou exception en mode strict). Pas de garde à la lecture.
- `Camera2D`/`Camera3D.writeProjection` : `setOrtho`/`setPerspective`.
- `Camera` : `shouldUpdateProjection`, `shouldUpdateView`, leurs setters et getters, `superUpdateProjection`, `superUpdateView` supprimés ; `setViewAngle`/`setViewMode` appellent `updateProjection()`. Changement cassant sans alias (règle du projet).
- `Direction.AnchorOffset(EDirection reference, float width, float height, Vector2f out)` : décalage du coin haut gauche par rapport au point de référence, convention de `Text` (x : `0`, `-w/2`, `-w` ; y : `0`, `-h/2`, `-h` ; `None` = centre), sans allocation. Utilisé par `Text` (comportement identique, vérifié par ses tests et une scène) et `TileMapRenderer`.
- `TileMapRenderer` : décalage local `offset = AnchorOffset(reference, w, h)` ; tuile `[x * ts + offset.x, (x + 1) * ts + offset.x]` (idem en y) ; `TileVisibility.Range(…, start = offset, end = ts + offset, …)`. Le décalage de la carte reste en unités entières, comme aujourd'hui (`(int)`, vers zéro : une carte de largeur impaire n'est pas posée à un demi-pixel). Le champ privé `leftUpPosition` (lu seulement dans la classe) est remplacé par un champ `referenceOffset`, et `tileEndX/Y` par le départ et la fin du décalage.

## Étapes (un commit chacune)
1. **Kit : dossier du joueur temporaire par moteur** — `TestEngine` (création au `start`, avant `KitEngineAccess.create` ; suppression récursive à `close`, dans le `finally`, après `shutdown`), `playerDataFolder()`, `EngineConfig.DEFAULT_PLAYER_DATA_FOLDER`. Tests (`HeadlessTestEngineTest` ou nouveau `PlayerDataFolderKitTest`, sans GL) : `ConfigData.set(…).get()` écrit sous `playerDataFolder()` et pas dans `data/` du module ; dossier supprimé après `close` ; dossier donné par la configuration (`@TempDir`) utilisé et gardé après `close` ; deux moteurs successifs ont deux dossiers. `KitApiTest` si l'API du kit y est listée. `mvn -o -q package`. Fusionnable seule.
2. **Suppression de `DataFolder`** — `utils/DataFolder.java`, `DataFolderTest`, `EngineConfig` (constante), `collect.py` (`LEGACY`). Compilation et tests ; `PublicFieldsTest` (le champ public disparaît : liste à mettre à jour si elle le nomme). Fusionnable seule.
3. **`Animation` / `Animator` sans moteur** — constructeurs à `Resources`, `update(float)`, condition morte retirée. Tests sans GL (`AnimationTest`, `AnimatorTest` dans `resources/animation`, avec une `Resources` construite à la main, `AnimationDataLoader` et le loader des textures, ou des `AnimationData` déjà chargées) : avance frame par frame, plusieurs frames dans un même `update`, vitesse, boucle (retour à 0, jamais fini), sans boucle (fini sur la dernière frame, plus d'avance), `restart`, `setFrame` (bornes, reprise), repli tant que la donnée n'est pas chargée puis redémarrage au chargement (`FallbackAnimationData`), `Animator` (première animation courante, nom en double refusé, `setCurrentAnimation` avec et sans remise à zéro, nom inconnu, vitesses). Vérification en jeu : `python tools/scenario.py "scene BatchRenderers2D" wait-loaded "?gl summary"` (animations jouées).
4. **Bugs d'`Animation`** — chacun avec son test écrit d'abord (échoue avant la correction) : `Animator.lateUpdate` → `isFrameChanged()` faux à l'image suivante sans changement, vrai au changement ; `clear()` remet le nom ; `FrameData` refuse `0`, `-1`, `NaN` (`AnimationDataTest`). `Animation2DBatchGLTest` : vérifier qu'il passe toujours (il appelle `lateUpdate` lui-même). Vérification en jeu : `BatchRenderers2D` et `BatchStress2D` (animations), `?gl summary`.
5. **Projection des caméras remplacée, drapeaux supprimés** — `Camera`, `Camera2D`, `Camera3D`. Tests (`CameraProjectionTest`, sans GL ; un `WindowInfo` de taille virtuelle non nulle, par réflexion comme `CameraVersionTest`, sinon la projection contient des infinis) : deux `updateProjection()` donnent la matrice d'un seul calcul (`setOrtho` attendu) pour `Camera2D`, `Camera3D` perspective et orthographique ; `invertView()` deux fois = matrice de départ ; `setViewAngle(60)` = perspective de 60° seule ; `setViewMode` aller-retour. `CameraVersionTest` mis à jour. Vérification en jeu : les quatre scènes 3D (`Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`) et `Scene2D` identiques avant/après (`scenario.py "scene Scene3D" wait-loaded "?camera" "screenshot --rect …"` sur `dev` puis sur la branche, une capture recadrée par scène au plus ; ou matrices de `?camera` comparées en texte si elles y figurent).
6. **Référence de `TileMapRenderer`** — `Direction.AnchorOffset`, `Text` migré, `TileMapRenderer` corrigé (sommets et culling). Tests : `DirectionTest` (ou test existant de `utils/math`) : les neuf `EDirection` ; un test sans GL des sommets si un calcul pur peut être extrait (par ex. méthode statique package-private des bords d'une tuile, testée pour les neuf références : taille constante, point de référence au bon coin/centre) ; `TileVisibilityTest` : plage avec un départ non nul ; `TileMapBatchGLTest` (démo) : carte en `None` = carte en `LeftUp` déplacée de `(-w/2, -h/2)` au pixel près, et tuiles comptées sous une caméra qui n'en voit qu'une partie. Tests de `Text` (`DrawnTexts`, `Scene2DTest`, `TextBatchGLTest`) inchangés. Vérification en jeu : `TextPaddingScene` et `CommandsScene2D` (textes en `None`/`Up`) identiques ; scène de la démo ci-dessous.
7. **Scène de démo `TileMapReferences2D`** (voir Démo) — inscrite dans `DemoScenes` ; `DemoScenesTest`. Vérification : `python tools/scenario.py "scene TileMapReferences2D" wait-loaded "?gl summary" "input key hold D" "input scroll 3" "?camera"` et une capture recadrée.
8. **Docs** — voir la liste ci-dessous (ne pas supprimer ce plan).

## Démo
- `TileMapReferences2D` : la même petite carte (tileset existant de la démo) dessinée neuf fois, une par `EDirection`, chaque `TileMapRenderer` à une position de grille marquée par une croix (forme vectorielle ou dessin de debug) et le nom de la référence (texte) ; la croix doit tomber au coin ou au centre attendu, toutes les cartes de même taille. `DemoCamera.free(this)` ; textes fixes en `setReferenceCamera`. Servira de scène de référence à la comparaison d'images.

## Documents à mettre à jour à la fin
- `architecture.md` : « Kit de test » (dossier du joueur temporaire par moteur, dossier donné gardé, `playerDataFolder()`, partagé par les tests d'un `@MightySceneTest`) ; « Configurations en couches » (`EngineConfig.DEFAULT_PLAYER_DATA_FOLDER`) ; caméras (projection recalculée sans composition, plus de drapeaux) ; animation (`Resources` en paramètre, `update(secondes)`, `isFrameChanged` à l'image du changement, durée > 0) ; références des tilemaps (`Direction.AnchorOffset`, même convention que `Text`).
- `debug-tools.md` : rien de prévu (à vérifier si `?camera` est utilisé pour la comparaison).
- `later.md` : « Réglages d'affichage du joueur » (`ConfigData` au lieu de `DataFolder`) ; données du joueur hors `configs/` (sauvegardes : chemin du dossier du joueur exposé par le moteur, écriture atomique) à prévoir.
- `futur-discussion.md` : « dossier de données utilisateur (`utils/DataFolder`) » → couche du joueur de `ConfigData`.
- `bonnes-pratiques.md` point 8 : retirer `DataFolder`.
- `.claude/skills/revue-pratiques/collect.py` : `LEGACY` sans `utils/DataFolder` (étape 2).
- `CLAUDE.md` : liste des tests (retirer `utils/DataFolder`, ajouter dossier du joueur du kit, `AnimationTest`/`AnimatorTest`, `CameraProjectionTest`, `AnchorOffset`, référence de `TileMapBatchGLTest`).
- `current.md` : supprimer les trois objectifs (ou n'en garder que le reste) ; mettre à jour la couverture de `resources/animation` si elle est citée ailleurs.
- Supprimer ce plan (discussion B).

## Hors périmètre
- Test de fumée de toutes les scènes, couverture réelle, comparaison d'images, test d'acceptation : plans suivants (`current.md`).
- API des données du joueur hors `configs/` (sauvegardes) : notée dans `later.md` (étape 8).
- Plusieurs moteurs de test en parallèle : `later.md`, « Kit de test : suites ».
- Autres usages de `EDirection` (`Animation2DRenderer.setShiftRotation`, collisions) : non migrés vers `AnchorOffset` (autre sens : rotation, fractions).

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1 à 4 (API du kit et d'`Animation`, tests de lecture) : Opus 5.5, effort `high` (API durable, cas de fin de vie du dossier et de fin d'animation).
- Étapes 5 à 7 (caméras, référence et culling des tilemaps, scène) : Opus 5.5, effort `high` (culling et rendu au pixel près, scènes 3D à ne pas casser).
- Étape 8 (docs) : Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`) : Opus 5.5, effort `high` (diff sur le kit, les caméras et les renderers, à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/prealables-non-regression.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/regression-groundwork`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` identiques ; animations de `BatchRenderers2D` et `BatchStress2D` ; textes centrés de `CommandsScene2D` et `TextPaddingScene` inchangés ; `TileMapReferences2D` : croix au bon point, cartes de même taille, à fort zoom ; aucun dossier `data/` créé dans `mightylib/`, `mightylib-test/`, `demo/` après `mvn test`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/regression-groundwork`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
### Fait (un commit par étape)
1. Kit : dossier du joueur temporaire par moteur (`TestEngine.playerDataFolder()`, `EngineConfig.DEFAULT_PLAYER_DATA_FOLDER`), dossier donné gardé ; `PlayerDataFolderKitTest` (4 cas).
2. `utils/DataFolder` et `DataFolderTest` supprimés, `collect.py` (`LEGACY`) mis à jour.
3. `Animation(Resources)`, `Animator(Resources)`, `update(float)` ; condition morte retirée ; `AnimationTest`, `AnimatorTest` sur une `Resources` construite à la main (`AnimationResources` : textures, animations, replis, batch chargé après coup).
4. Bugs d'`Animation`, chaque test vu en échec avant la correction : `Animator.lateUpdate` → `Animation.lateUpdate` ; `clear()` remet le nom ; `FrameData` refuse 0, -1, -0.0, NaN, Infinity.
5. `setOrtho`/`setPerspective`, drapeaux et `superUpdate*` supprimés ; `CameraProjectionTest` (5 cas en échec sur l'ancien code).
6. `Direction.AnchorOffset`, `Text` migré, `TileMapRenderer` corrigé (`ReferenceOffset`, `TileEdges` package-private, culling) ; `DirectionTest`, `TileMapRendererTest`, `TileVisibilityTest`, `TileMapBatchGLTest` (nouveau cas en échec sur l'ancien code : 23 598 canaux différents).
7. `TileMapReferences2D` inscrite dans `DemoScenes`.
8. Docs (`architecture.md`, `current.md`, `later.md`, `futur-discussion.md`, `bonnes-pratiques.md`, `CLAUDE.md`).

### Écarts au plan, et pourquoi
- Étape 4, deux bugs de plus trouvés par les tests de l'étape 3, corrigés avec leur test : le temps au-delà de la fin d'une **boucle** était perdu (`restart()` remettait le temps à 0 : une longue mise à jour ou un tick en retard décalait la boucle) → `currentFrame = 0` en gardant le reste ; la **première mise à jour après `init`** redémarrait l'animation (première résolution de la donnée utilisable dans `data()`), donc le premier changement de frame était vu deux fois → `init` prend la donnée utilisable.
- `Animation.update(float)` lit la donnée utilisable **avant** d'ajouter le temps : à l'image où la donnée devient chargée, l'animation redémarre puis avance de ce temps (avant : le temps de cette image était perdu).
- `TileMapRenderer.setReference` avant `setTilemap` ne lève plus de `NullPointerException` (la carte est placée par `setTilemap`).
- `TileMapBatchGLTest` : le dernier contrôle « plus de tuiles laissées qu'écrites » est devenu « des tuiles laissées et des tuiles écrites » (la carte de la démo n'a que 4 × 4 tuiles).
- Rien de changé pour `CameraVersionTest` (il ne lisait pas les drapeaux supprimés) ni pour `PublicFieldsTest` (aucune liste ne nommait `DATA_FOLDER_PATH`).
- Bugs relevés, non corrigés, notés dans `later.md` : angle de vue 3D de 120 donné en radians à JOML ; `.anim` de 0 frame chargé puis `NullPointerException` à `update`.

### Vérifié, et comment
- `mvn -o -q package` (tous les modules, tests GL compris) : vert ; aucun dossier `data/` dans `mightylib/`, `mightylib-test/`, `demo/` ensuite.
- `scenario.py` : `BatchRenderers2D` et `BatchStress2D` (après les étapes 3 et 4), `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`, `Scene2D` (étape 5), `TextPaddingScene`, `CommandsScene2D` (étape 6), `TileMapReferences2D` (étape 7 : `?gl summary`, `input key hold D`, `input scroll 3`, `?camera`, une capture, `leakcheck` sans fuite) : 0 ligne de problème.
- Scènes 3D identiques par construction : JOML `ortho`/`perspective` sur une matrice identité passent par `setOrtho`/`setPerspective`, et aucune scène ne recalcule la projection après la construction (le premier contrôle de `CameraProjectionTest` passait déjà sur l'ancien code). `?camera` ne donne pas les matrices : pas de comparaison en texte ni de capture.
- `Text` identique par construction (`a - b` = `a + (-b)` en flottants) ; tests de texte (`DrawnTexts`, `TextWrap`, `Scene2DTest`, `TextBatchGLTest`) verts.
- `TileMapReferences2D` (capture) : croix au coin, au milieu du côté ou au centre attendu pour les neuf références, cartes de même taille.

### À vérifier à la main
- Scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` identiques à `dev`.
- Animations de `BatchRenderers2D` et `BatchStress2D` : même rythme, pas de saut au bouclage.
- Textes centrés de `CommandsScene2D` et `TextPaddingScene` inchangés.
- `TileMapReferences2D` : croix au bon point, cartes de même taille, aussi à fort zoom (molette).
- Aucun dossier `data/` créé dans `mightylib/`, `mightylib-test/`, `demo/` après `mvn test`.
