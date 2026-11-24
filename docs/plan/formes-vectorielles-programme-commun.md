# Formes vectorielles : un programme de lot commun

**Statut : validé.**

Décisions prises :
- Les 7 formes par distance du moteur (rectangle arrondi, cercle, ellipse, triangle, arc, polygone régulier/hexagone, grille) partagent **un seul programme de lot** `shape2DBatch`, qui choisit la forme par un `switch` sur un type par instance.
- Dessinée seule (mode `off`, test de profondeur, modèle hors du plan), chaque forme **garde son shader** (`circle2D`…) ; le corps de chaque forme devient une fonction d'include appelée par les deux : rendu identique par construction.
- Le type passe par un **nouvel attribut d'instance `aShapeType` (INT)** de `BatchFormats.SHAPE` (instance de 120 → 124 octets).
- Numéros des types : **constantes Java package-private + `#define` dans un include GLSL**, un test sans GL vérifie qu'ils coïncident.
- Type inconnu du programme commun (0, ou forme d'un projet liée par erreur à `shape2DBatch`) : **quad entier en magenta**.
- Une forme d'un projet garde **son propre programme de lot** (format `SHAPE`, type ignoré). Un programme commun généré depuis des types enregistrés (mods) est noté dans `later.md`.
- `PointsRenderer` garde son format `POINT` ; les formes géométriques gardent `STROKE`.
- Coût GPU : pas de mesure dédiée ; temps de frame avec vsync coupée avant/après, et le temps GPU reste dans `later.md` avec cette raison de plus.
- Démo et mesure : un **mode « formes »** dans `BatchStress2D`.

## Pour démarrer
- Lire `docs/architecture.md`, sections « Rendu » : **Formes 2D** et **Lot de dessins 2D** ; `docs/decisions.md`, « Formes 2D : modèle hybride » et « Format du lot déclaré en Java » ; `docs/later.md`, puce « Temps GPU » (section Effets 2D, rendu).
- Branche de sujet depuis `dev` : `feature/shape-batch-program`.
- Compiler et tester avec les commandes de `CLAUDE.md` (Windows : Maven d'IntelliJ) ; vérifier en jeu avec `python tools/scenario.py …` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Formes par distance** (`mightylib/src/main/java/io/github/mightycode/mightylib/graphics/renderer/_2D/shape/`) : `VectorShape2DRenderer` (extensible) et ses sous-classes `RoundedRectangleRenderer`, `CircleRenderer`, `EllipseRenderer`, `TriangleRenderer`, `ArcRenderer`, `RegularPolygonRenderer` (`HexagonRenderer` en hérite), `GridRenderer` ; `PointsRenderer` (format `POINT`). `submitToBatch` (final) : `batchState`, format attendu `batchFormat()` (`SHAPE`), culling, puis `writeBatchDraw` → `WriteModelRows`, taille/référence, `writeInstanceValues` (couleurs, contour, marge/texturé, position de texture, `aParams0/1`). `writeInstanceValues` sert aussi à `POINT`.
- **Shaders** (`mightylib/src/main/resources/assets/mightylib/shaders/`) : `2D/shape/shape2DV.glsl` (seul) et `shape2DInstancedV.glsl` (lot, `#define SHAPE_BATCH`, attributs `layout(location = 0..10)`, `utils/shape2DInstance.glsl` remplit les globales de `utils/shape2DVertex.glsl`) ; un fragment shader par forme `2D/shape/<forme>2DF.glsl`, qui inclut `utils/shape2D.glsl` et donne sa distance à `shapeColor(d)` (la grille compose elle-même sa couleur avec `shapeFragmentPixel`, `shapeFillColor`, `shapeCoverage`). `shapeColor` lit `dFdx/dFdy(shapeLocal)` et `texture()` (commentaire « read in uniform control flow »).
- **`shaders.json` du moteur** : chaque forme (`circle2D`…) a `"batch": "<forme>2DBatch"` ; 7 entrées `*2DBatch` (`shape2DInstancedV.glsl` + le fragment de la forme, tags `2D`, `batch2D`). `EngineResources` : constantes `ROUNDED_RECTANGLE_2D_BATCH`… `GRID_2D_BATCH` et la liste `SHADERS` ; `BatchFormats.engineFormats()` associe ces 7 noms à `SHAPE`.
- **Défaut** : le programme de lot fait partie de la part de matériau (`MaterialBatchPart.equals` : `shader`, `format`…) : deux formes de types différents ont deux états, donc **chaque changement de type coupe la série**. Dans l'ordre des appels (défaut), un mélange cercle / rectangle arrondi / cercle coûte un appel par alternance ; avec `DrawSortKeys.layerThenState()`, un appel par type et par couche.
- **Tests existants** : `BatchFormatTest` (formats des formes, `engineFormats`), `ShapeCullBoundsTest`, `ShapeStrokeTest`, `RegularPolygonShapeTest`, `EngineResourcesTest` (noms) ; kit : `VectorShapeBatchGLTest` (chaque forme `off` = regroupée au pixel près, formes égales = un appel, zoom ; `requestBatchPrograms` charge les programmes de lot), `DrawBatch2DGLTest` (programmes du moteur conformes à leur format), `EngineShadersGLTest` (tous les shaders du moteur se lient), `ViewCullingGLTest`.
- **Mesure** : `demo/.../scenes/BatchStress2D.java` (rectangles, PgUp/PgDn, M mode, O tri par état ; compteurs `stats`) ; `window vsync off` coupe la vsync ; `stats` donne `frameMs`, `renderCpuMs`, `drawCallsPerFrame`, `batchFlushesPerFrame`.
- Aucune forme de projet n'existe dans la démo ni dans les tests.

## Décisions
1. Programme commun `shape2DBatch` = `2D/shape/shape2DInstancedV.glsl` + nouveau `2D/shape/shape2DBatchF.glsl` ; les 7 entrées `*2DBatch` et leurs constantes disparaissent (pas d'alias : aucun produit dessus), remplacées par `EngineResources.SHAPE_2D_BATCH`.
2. Corps des formes : un include `utils/shapeKinds2D.glsl` (après `utils/shape2D.glsl`) avec une fonction par forme qui **rend sa distance** (`roundedRectangleDistance()`, `circleDistance()`, …) et `vec4 gridColor(float pixel, vec4 fill)` pour la grille. Chaque `<forme>2DF.glsl` devient `FragColor = shapeColor(<forme>Distance());`.
3. Dérivées et texture **hors du `switch`** : `utils/shape2D.glsl` gagne `vec4 shapeColorWith(float d, float pixel, vec4 fill)` ; `shapeColor(d)` = `shapeColorWith(d, shapeFragmentPixel(), shapeFillColor())` (API inchangée pour les formes de projet et pour une source plus haute qui garde les noms). `shape2DBatchF.glsl` calcule `pixel` et `fill` d'abord, puis `switch (shapeType)` donne la distance (ou la couleur de la grille). Pourquoi : `dFdx` et `texture()` implicite sont indéfinis en flot non uniforme selon la spécification GLSL ; le type est constant par primitive, mais on ne dépend pas de ce détail.
4. Type par instance : `layout(location = 11) in int aShapeType` dans `shape2DInstancedV.glsl`, passé en `flat out int shapeType` (`flat in int shapeType` lu seulement par `shape2DBatchF.glsl` ; un fragment de projet ne le lit pas, l'attribut peut être retiré par le compilateur, ce que `BatchFormat.check` accepte). Attribut ajouté à `SHAPE` seulement (pas à `POINT`) : il est écrit dans `writeBatchDraw` après `writeInstanceValues`.
5. Numéros : `utils/shapeTypes2D.glsl` (`#define SHAPE_TYPE_NONE 0`, `ROUNDED_RECTANGLE 1`, `CIRCLE 2`, `ELLIPSE 3`, `TRIANGLE 4`, `ARC 5`, `REGULAR_POLYGON 6`, `GRID 7`), classe package-private `ShapeTypes` du package `shape` avec les mêmes valeurs ; accroche package-private `int shapeType()` de `VectorShape2DRenderer` (défaut `ShapeTypes.NONE`), redéfinie par les 7 formes du moteur (`HexagonRenderer` hérite de celle du polygone régulier). Package-private : un projet ne peut pas se donner un type du programme commun.
6. Type inconnu : `default:` du `switch` → `FragColor = vec4(1, 0, 1, 1)` sur tout le quad (pas de `discard`).
7. Ce qui coupe encore une série de formes : texture, blend, programme d'une forme de projet, format (`POINT`, `STROKE`), caméra, mode de couleur. Inchangé et voulu.

## Étapes
### 1. Corps des formes en fonctions (aucun changement de rendu)
- Fichiers : `utils/shape2D.glsl` (`shapeColorWith`, `shapeColor` qui l'appelle), nouveau `utils/shapeKinds2D.glsl` (les 7 fonctions, la grille en `gridColor(pixel, fill)`), les 7 `2D/shape/<forme>2DF.glsl` réduits à un appel (`points2DF.glsl` peut garder son code : hors du programme commun).
- Tests : `EngineShadersGLTest`, `VectorShapeBatchGLTest` (`off` = regroupé au pixel près) et `ViewCullingGLTest` passent tels quels ; `mvn -o -q test`.
- En jeu : `python tools/scenario.py "scene VectorShapes2D" wait-loaded "?gl summary" "?stats drawCallsPerFrame batchFlushesPerFrame"` : rien ne change, aucune erreur de shader.
- Fusionnable seule.

### 2. Mode « formes » de `BatchStress2D`, mesure avant changement
- `BatchStress2D` : touche **F** qui alterne rectangles / formes mêlées : les 7 formes du moteur dans l'ordre des appels (un type différent à chaque dessin), tailles et rotations comme les rectangles, contour sur une partie d'entre elles (largeurs, alignements et unités variés), couleurs aléatoires, sans texture (une texture couperait la série) ; PgUp/PgDn, M et O restent valables ; aide et Javadoc de la classe mises à jour ; `unload` libère les formes. La caméra libre (`DemoCamera`) est déjà là.
- Mesure **avant** l'étape 3, à noter pour le « Bilan » : en mode formes, à N = 1 000, 16 000 et 128 000 : `drawCallsPerFrame`, `batchFlushesPerFrame`, `renderCpuMs`, et `frameMs` avec `window vsync off`, en mode `orphan`, sans tri puis avec O ; plus une mesure limitée par le GPU (zoom avant : grandes formes superposées). `?stats frameMs renderCpuMs drawCallsPerFrame batchFlushesPerFrame` (médiane de 3 échantillons).
- Test : `DemoScenesTest` inchangé (scène déjà enregistrée) ; vérifier en jeu `input key press F`, `?stats`, `input key hold D`, `input scroll 3`.

### 3. Programme commun `shape2DBatch`
- `utils/shapeTypes2D.glsl`, `ShapeTypes` (package-private), accroche `shapeType()` et ses 7 redéfinitions.
- `BatchFormats.SHAPE` : `.instance("aShapeType", 1, EBatchAttributeType.INT)` en dernier (location 11) ; Javadoc du format. `VectorShape2DRenderer.writeBatchDraw` : `batch.putInt(shapeType())` après `writeInstanceValues` (rien pour `POINT`).
- `shape2DInstancedV.glsl` : `aShapeType`, `flat out int shapeType`. Nouveau `2D/shape/shape2DBatchF.glsl` (inclut `utils/shape2D.glsl`, `utils/shapeKinds2D.glsl`, `utils/shapeTypes2D.glsl` ; `pixel` et `fill` avant le `switch` ; `default` magenta).
- `shaders.json` du moteur : entrée `shape2DBatch` (tags `2D`, `batch2D`), les 7 formes `"batch": "shape2DBatch"`, suppression des 7 entrées `*2DBatch`. `EngineResources` : `SHAPE_2D_BATCH` remplace les 7 constantes (liste `SHADERS`). `BatchFormats.engineFormats()` : `SHAPE_2D_BATCH → SHAPE`.
- Javadoc de `VectorShape2DRenderer` (forme d'un projet : son propre programme de lot `shape2DInstancedV.glsl` + son fragment, format `SHAPE`, `aShapeType` ignoré ; jamais lié à `shape2DBatch`, qui la dessinerait en magenta).
- Tests JUnit sans GL :
  - `ShapeTypesTest` (package `shape`) : lit `utils/shapeTypes2D.glsl` par le classpath, chaque `#define SHAPE_TYPE_*` = la constante Java de même nom, valeurs uniques, `NONE` = 0 ; chaque forme du moteur rend un type ≠ `NONE` et tous différents (sauf l'hexagone = le polygone régulier).
  - `BatchFormatTest` : `SHAPE` finit par `aShapeType` INT, taille de l'instance 124 octets ; `engineFormats` contient `shape2DBatch` et plus aucun `*2DBatch` de forme.
  - `EngineResourcesTest` : noms à jour.
  - Test d'écriture (`DrawBatch2DTest` ou nouveau test du package `shape` si un renderer s'écrit sans GL ; sinon dans le test GL) : le type écrit est celui de la forme.
- Tests GL du kit (`VectorShapeBatchGLTest`) :
  - `mixedShapesAreOneDrawCall` : les 7 formes alternées (avec contours, dont `SCREEN_PIXELS`, `setAntialiasing(false)`, rotation, zoom) = **un** appel de lot, et le rendu `off` = regroupé au pixel près.
  - Forme de projet : une sous-classe de test avec son fragment shader et son entrée de lot propre (source de ressources du test : voir comment `TestEngine` ajoute une source ; dossier temporaire sinon) dessinée entre deux cercles = 3 séries, rendu = `off` au pixel près ; la même liée à `shape2DBatch` → pixels magenta.
  - `DrawBatch2DGLTest` (programmes conformes à leur format) et `EngineShadersGLTest` couvrent `shape2DBatch` sans changement ; les vérifier.
- En jeu : `python tools/scenario.py "scene VectorShapes2D" wait-loaded "?stats drawCallsPerFrame batchFlushesPerFrame" "batch mode off" "?stats drawCallsPerFrame"` (moins d'appels regroupés qu'à l'étape 1, aucune erreur) ; capture ciblée de `VectorShapes2D` `orphan` contre `off` seulement si un écart est signalé par les tests.
- Mesure **après** dans `BatchStress2D` (mêmes conditions qu'à l'étape 2) : appels attendus ~1 par frame pour les formes sans tri ; `frameMs` limité par le GPU comparable à l'étape 2 (un écart notable est à signaler à l'utilisateur, pas à corriger en douce).

### 4. Documents
- Voir « Documents à mettre à jour à la fin ». Ne pas supprimer ce plan.

## Démo
- `BatchStress2D` : mode formes (touche F), étape 2. Pas de nouvelle scène : `VectorShapes2D` montre déjà chaque forme, `BatchStress2D` est la scène de mesure que le test d'acceptation complétera.

## Documents à mettre à jour à la fin
- `architecture.md` : **Formes 2D** (corps des formes dans `utils/shapeKinds2D.glsl`, `shapeColorWith`, une forme de projet = son fragment + son programme de lot propre) ; **Lot de dessins 2D** (un programme commun `shape2DBatch` pour les 7 formes du moteur, `aShapeType`, `ShapeTypes`/`shapeTypes2D.glsl`, magenta pour un type inconnu, ce qui coupe encore une série) ; `BatchStress2D` (touche F) là où la scène est citée.
- `decisions.md` : « Formes 2D : modèle hybride » : remplacer « leur fusion … attend le regroupement des dessins » par la décision (un programme de lot commun à type par instance pour les formes du moteur, shader propre gardé seul, formes de projet à part) et son pourquoi (une série par alternance de types).
- `later.md` : puce « Temps GPU » : ajouter le coût du `switch` de `shape2DBatch` (mesuré seulement par le temps de frame) ; nouvelle puce (section Effets 2D, rendu, ou Modding) : programme commun **généré** depuis des types de formes enregistrés (identifiant → numéro, fonction GLSL) pour que les formes des projets et des mods rejoignent la même série.
- `current.md` : retirer la partie « formes vectorielles : un programme de lot par forme… fusion possible… » de la première puce ; garder le reste (profilage JFR, réordonnancement).
- `CLAUDE.md` : tests couverts : `ShapeTypesTest`, `VectorShapeBatchGLTest` (formes mêlées = un appel, forme de projet, magenta), `BatchFormatTest` (`aShapeType`).
- `debug-tools.md` : rien, sauf si une commande change.
- Supprimer ce plan (discussion B).

## Hors périmètre
- Fusion de `POINT` dans `SHAPE` et des formes géométriques (`STROKE`, autre format) : non prévu.
- Programme commun généré pour les formes de projets et de mods : `later.md` (ci-dessus).
- Temps GPU dans `stats` : `later.md`, « Temps GPU ».
- Textures différentes qui coupent la série (atlas) : `later.md`, même puce.
- Profilage JFR du coût CPU par dessin, test d'acceptation : `current.md`.

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1 et 3 (GLSL partagé au pixel près, format, tests GL d'une forme de projet) : **Claude Opus 5.5, effort `high`** (logique de shader délicate, rendu identique exigé).
- Discussion A, étape 2 (mode de `BatchStress2D`, mesures) et étape 4 (docs) : **Claude Sonnet 5.5, effort `medium`** (code de démo et docs).
- Discussion B (`/pr-ml`) : **Claude Opus 5.5, effort `high`** (relire shaders, format et tests au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/formes-vectorielles-programme-commun.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/shape-batch-program`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, mesures avant/après de `BatchStress2D`, à vérifier à la main par l'utilisateur (`BatchStress2D` : touche F, formes mêlées, compteurs d'appels, M et O ; `VectorShapes2D` : chaque forme identique en `batch mode off` et `orphan`, contours, zoom). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/shape-batch-program`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan » : `BatchStress2D` (F, M, O, compteurs), `VectorShapes2D` (`off` = `orphan`).
