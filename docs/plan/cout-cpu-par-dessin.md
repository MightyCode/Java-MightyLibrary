# Coût CPU par dessin du lot 2D : mesure et réduction

**Statut : validé.**

Décisions prises :
- Périmètre : mesurer **et** corriger ; les seuils d'acceptation de `BatchStress2D` (objectif suivant de `current.md`) sortent des mesures finales.
- Mesure : JFR lancé par `scenario.py --jfr` et résumé en texte, **plus** des bancs JUnit reproductibles (ns par dessin), sans JMH.
- Vérifications de l'écriture : une fois par dessin (écritures groupées pour les renderers du moteur), l'API valeur par valeur reste vérifiée pour les formats d'un projet.
- Cible chiffrée : fixée avec l'utilisateur après le rapport de mesure (étape 3) ; ordre de grandeur visé : rectangle visible ≤ ~130 ns (la moitié), dessin éliminé ≤ ~50 ns (le quart), mesurés par le banc, appels GL exclus.
- Changement de caméra : compteur de version des `ShaderValue`, suivi par la table au lieu de comparer les matrices.
- Ordre de `submitToBatch` : culling testé avant la part de matériau et l'espace de couleur.
- Partition spatiale / cache statique (ne pas appeler `display()` hors champ) : plus tard, noté dans `later.md`.
- Couverture des bancs et du rapport : rectangle texturé et coloré, animation, forme vectorielle (avec et sans contour), texte ; les tuiles à part.

## Pour démarrer
- Lire : `docs/current.md` (objectif « Regroupement des dessins : reste » et « Test d'acceptation »), `docs/architecture.md` section Rendu, paragraphe « Lot de dessins 2D » (long : le lire en entier, il décrit états, table, culling, formats, compteurs), `docs/debug-tools.md` sections Profilage et `scenario.py`, `docs/decisions.md` (philosophie, pas de changement cassant protégé).
- Branche de sujet depuis `dev` : `feature/batch-cpu-cost`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).
- Les mesures en jeu se font sur `BatchStress2D` (Page Up double N, F = formes, M = mode, `camera main2D zoom 3.2` pour ~10 % visibles, `batch cull on|off`, `?stats` → `renderCpuMs`). Le script local non versionné `tools/scripts/measure_culling.py` montre l'enchaînement (médiane de 3 `?stats`).

## Contexte : ce qui existe
Chemin d'un rectangle regroupé (`RectangleRenderer` → `Shape2DRenderer`), par dessin :
1. `Renderer.display()` (`mightylib/.../graphics/renderer/Renderer.java`) : `Views.current()` (ThreadLocal), `batch2D()`, `isEnabled()`, `submitToBatch(batch)`.
2. `Shape2DRenderer.submitToBatch` (`.../_2D/shape/Shape2DRenderer.java`) : `BatchGeometry2D.CanBatch`, `refreshBatchGeometry` (drapeaux), `batchState`, `viewCulled`, `BatchGeometry2D.WriteTriangles`.
3. `Renderer.batchState` : version des surcharges, `instanceof Matrix4f` ×2, `Material.batchPart(table)`, `table.camera(projection, view)`, `resolveColorSpace()` (rappelle `Views.current()`, puis `engine().colors().resolve(...)`), `table.state(...)`.
4. `Material.batchPart` (`.../graphics/renderer/Material.java`) : `shader.isLoaded()`, `Batches2D.of(glResources)` (recherche dans `GLResources.shared` par classe) et `batches.readsFormat(...)` (`HashMap` par programme) **avant** le chemin rapide mis en cache (`table == batchTable && generation && version && programStamp`).
5. `BatchStateTable.camera` (`.../_2D/batch/internal/BatchStateTable.java`) : même source que la dernière place, puis **deux `Matrix4f.equals`** (32 flottants) à chaque dessin, car `Camera` (`.../scenes/camera/Camera.java`) modifie ses `Matrix4f` sur place (`projectionShaderValue`, `viewShaderValue`) sans compteur.
6. `Renderer.viewCulled` → `DrawBatch2D.cullBounds` (`isCurrent`, taille d'un pixel) → `viewCullBounds` (accroche) → `DrawBatch2D.isCulled` (`start()`, `isCurrent`, compteurs, transformation de la boîte locale, marge de la part de matériau `table.cullMarginOf(state)`).
7. `BatchGeometry2D.WriteTriangles` (`.../_2D/batch/internal/BatchGeometry2D.java`) : `beginDraw` (`inDraw`, `MAX_DRAWS`, `start()`, `isCurrent`, format), 4 × `vertex` (vérifiés), 6 × `index` (cinq vérifications chacun), `endDraw`.

Autres `submitToBatch` qui appellent `batchState`/`viewCulled` : `MeshRenderer` (dont `Text`), `Animation2DRenderer`, `VectorShape2DRenderer`, `TileMapRenderer` (son test par tuile), et ceux qui écrivent des instances (`PointsRenderer`, `GeometricShape2DRenderer` passent par leurs parents ou `BatchGeometry2D` : vérifier par `Grep "batchState("`).

Mesures connues (2026-11-24, `renderCpuMs`, médiane de 3) : ~260 ns par rectangle visible hors appels GL ; 39,8 ms à 128 000 tout visible ; 27,7 ms à 128 000 dont 88 % éliminés (le coût avant le test n'est pas supprimé par le culling). `BatchStress2D.update` tourne tous les rectangles (`setRotationZ` : `model.identity().translate().rotate()`), hors `renderCpuMs`.

Profilage existant : événements JFR `mightylib.Frame`/`mightylib.Tick` (`main/internal/FrameEvent`, `TickEvent`), commande manuelle `jcmd … JFR.start` (`docs/debug-tools.md`, Profilage). `scenario.py` n'a pas d'option JFR (options : `--port`, `--timeout`, `--max-duration`, `-D`, `--attach`, `--keep-open`…).

Tests existants de la zone : `DrawBatch2DTest` (lot sans GL, faux `IBatchBackend` : un banc sans GL peut s'en servir), `BatchStateTableTest`, `ViewCulling2DTest`, `ShapeCullBoundsTest`, `MaterialTest`, `UniformValuesTest` ; kit : `DrawBatch2DGLTest`, `RectangleBatchGLTest`, `ViewCullingGLTest`, `VectorShapeBatchGLTest`, `Animation2DBatchGLTest`, `TextBatchGLTest` (rendu au pixel près entre modes : à garder verts à chaque étape).

Ailleurs : libGDX `SpriteBatch` écrit directement dans un `float[]` sans vérification par valeur, le remplissage CPU est son coût principal (~50 000 sprites à 60 fps, test informel) ; `SpriteCache` pour le statique. Godot (OpenGL) : coût par élément « prohibitif » à des milliers d'éléments, test de chevauchement bon marché mais pas gratuit. JFR : `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` pour garder les frames inlinées ; `jdk.ObjectAllocationSample` pour les allocations.

## Décisions
- **Banc** : deux bancs JUnit, actifs seulement avec `-Dmightylib.bench=true` (`@EnabledIfSystemProperty`, pas de `excludedGroups` : il ne doit pas entrer en conflit avec `-DexcludedGroups=gl`), nommés `…BenchTest` (motif de Surefire) :
  - `DrawBatch2DBenchTest` (`mightylib/src/test`, sans GL, faux backend comme `DrawBatch2DTest`) : écriture et culling seuls (`beginDraw`/`vertex`/`index`/`endDraw`, `isCulled`), ns par dessin visible et éliminé.
  - `Batch2DCostBenchTest` (`mightylib-test/src/test`, tag `gl`, fenêtre cachée) : le chemin complet `display()` → `submitToBatch` de N renderers de chaque type couvert, dans une vue au lot actif, chronométré **sans** l'envoi GL (temps des `display()` seuls ; l'envoi mesuré à part). Cas : tout visible, tout hors champ.
  - Chauffe (plusieurs passes jetées), puis médiane de plusieurs passes ; une ligne JSON par cas sur la sortie (`{"case":…,"nsPerDraw":…}`). Seuils non vérifiés par les bancs (ils mesurent, ils n'échouent pas, sauf erreur).
  - `tools/bench.py` lance les deux et imprime un tableau (Python versionné : usage permanent).
- **JFR** : `scenario.py --jfr` ajoute `-XX:StartFlightRecording=filename=target/jfr/scenario.jfr,settings=profile` et `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` ; `tools/jfr_summary.py <fichier>` (lancé aussi à la fin par `--jfr`) lit `jfr print --json` (binaire `jfr` à côté du `java` choisi par `scenario.py`) et imprime : méthodes les plus chaudes du thread principal (soi et cumulé, top 25), allocations par classe et site (`jdk.ObjectAllocationSample`), durées de `mightylib.Frame`. Option `--since <s>` pour ne garder que l'état stable (après les Page Up).
- **Événement JFR de l'envoi** : `mightylib.BatchSend` (dans `batch/internal`, comme `FrameEvent`) autour de l'envoi d'un lot (octets, appels) : sépare l'écriture CPU des appels GL dans le rapport.
- **Version des `ShaderValue`** : un compteur (`version()`), incrémenté par `setObject` et par `markChanged()` ; une valeur **suivie** (`trackChanges()` / `isTracked()`, posé par `Camera` sur ses deux valeurs, qui appelle `markChanged()` après chaque mise à jour de ses matrices) permet à `BatchStateTable.camera` de comparer source + version (un entier) au lieu des matrices. Une valeur non suivie (matrices brutes d'un projet modifiées sur place) garde la comparaison des matrices : aucun changement de caméra perdu.
- **Culling avant la part de matériau** : `batchState` est coupé en deux temps : place de caméra (surcharges, matrices, `table.camera`) puis, si le dessin est visible, part de matériau, espace de couleur et état. Le test de culling ne dépend que de la place de caméra et de la marge des shaders : la marge (`cullMargin` des shaders et programmes de lot) est mise en cache dans `Material` (recalculée avec sa version / le `programStamp`), plus lue dans la part de matériau. Les accroches `viewCulled`/`viewCullBounds`/`batchState` changent de signature si besoin, sans alias (moteur sans produit). Les compteurs (testés, éliminés, sans boîte) restent identiques.
- **Écritures groupées** : `DrawBatch2D` gagne une écriture d'un dessin de triangles en une fois (sommets `SPRITE` depuis des tableaux + indices depuis un `int[]`), vérifiée une fois par dessin (format, plage des indices `< vertexCount`, place) ; `BatchGeometry2D.WriteTriangles` et les renderers du moteur l'utilisent. `isCurrent` est vérifié une fois par dessin (l'état validé par `cullBounds`/`isCulled` n'est pas revérifié par `beginDraw` dans la même écriture). L'API `put`/`vertex`/`index` reste vérifiée valeur par valeur.
- Les autres pistes (`Views.current()` deux fois, `resolveColorSpace`, `Batches2D.of` et `readsFormat` avant le chemin rapide de `batchPart`, `instanceof`, `refreshBatchGeometry`, compteurs) ne sont corrigées que si le JFR ou le banc les montrent (un commit chacune, avec son gain mesuré).
- Aucune allocation par dessin, comme aujourd'hui : vérifiée par le résumé JFR (`jdk.ObjectAllocationSample` vide sur le chemin du lot).

## Étapes
Chaque étape garde verts les tests de rendu au pixel près du kit (`mvn -o -q test`, avec `gl`). Les étapes 4 à 7 se font dans l'ordre du poids mesuré à l'étape 3 si celui-ci diffère (le dire dans le Bilan) ; chacune note son gain (banc + `renderCpuMs` à 16 000 et 128 000, visible / ~10 %).

1. **Outils de profilage.** `scenario.py --jfr` (options ci-dessus, dossier `target/jfr/`), `tools/jfr_summary.py`, événement `mightylib.BatchSend`. `docs/debug-tools.md` (Profilage) dans ce commit. Vérification : `python tools/scenario.py --jfr "scene BatchStress2D" wait-loaded "input key PAGE_UP" "sleep:5"` produit le fichier et un résumé lisible ; `scenario.py --help` à jour. Fusionnable seule.
2. **Bancs.** `DrawBatch2DBenchTest`, `Batch2DCostBenchTest` (rectangle texturé, rectangle coloré, animation, forme vectorielle sans et avec contour, texte ; visible / hors champ ; N = 16 000), `tools/bench.py`. Vérifier que `mvn -o -q test` ne les lance pas et que `python tools/bench.py` les lance (durée < 1 min). Fusionnable seule.
3. **Rapport de mesure.** JFR sur `BatchStress2D` (rectangles 16 000 et 128 000, visible et ~10 %, puis formes F) et bancs ; écrire dans ce plan une section « Mesures » : répartition du temps par étape du chemin (pourcentages), allocations, ns par dessin par type, part des appels GL (`BatchSend`). **S'arrêter** : présenter le rapport à l'utilisateur, fixer avec lui la cible chiffrée et l'ordre des étapes 4 à 7, écrire la cible dans ce plan. Commit : ce plan mis à jour.
4. **Version des `ShaderValue` et caméra suivie.** `ShaderValue` (`version`, `markChanged`, `trackChanges`, `isTracked`), `Camera` (marque ses valeurs à chaque mise à jour des matrices ; repérer toutes les écritures de `projection`/`view`, y compris dans `Camera2D`, `Camera3D`), `BatchStateTable.camera` (source + version pour une valeur suivie, `equals` sinon). Comment la table reçoit la `ShaderValue` plutôt que la seule `Matrix4f` : par `Renderer.batchState` (il tient déjà `projectionValue`/`viewValue`). Tests : `ShaderValue` (version à `setObject`, `markChanged`), `BatchStateTableTest` (caméra suivie déplacée au milieu d'une frame : nouvelle place si des dessins attendent, mise à jour sur place sinon ; matrice brute non suivie modifiée sur place : vue par `equals`), `Camera2DZoomTest`/caméra : chaque setter incrémente la version.
5. **Culling avant la part de matériau.** `Renderer` (`batchState` en deux temps, `viewCulled`), `Material` (marge de culling en cache), `DrawBatch2D.isCulled`/`cullBounds` (marge lue du matériau / passée, plus de la part), `BatchStateTable` (`cullMarginOf` retiré si inutile), tous les `submitToBatch` listés au Contexte. Tests : `ViewCulling2DTest` (marges des shaders toujours appliquées, matériau changé de shader : marge mise à jour), `DrawBatch2DTest` (dessin éliminé : ni état ni part demandés, aucune écriture), `MaterialTest` (marge en cache invalidée par la version). Kit : `ViewCullingGLTest` vert (culling = sans culling au pixel près).
6. **Écritures groupées vérifiées une fois par dessin.** `DrawBatch2D` (nouvelle écriture d'un dessin de triangles, validation unique de l'état), `BatchGeometry2D.WriteTriangles`, renderers du moteur qui écrivent des sommets. Tests `DrawBatch2DTest` : mêmes octets qu'avec l'API valeur par valeur, indice hors plage refusé (`IllegalArgumentException`, rien d'écrit), écriture au milieu d'un dessin refusée, aucune allocation ; l'API valeur par valeur garde ses refus.
7. **Points chauds restants confirmés par la mesure.** Un commit par point (pistes listées aux Décisions), chacun avec son gain ; s'arrêter quand la cible fixée à l'étape 3 est atteinte, ou dire dans le Bilan pourquoi elle ne l'est pas (et ce qui la bloquerait : envoi GL, `display()` par objet → partition spatiale).
8. **Mesure finale et documents.** Bancs et `renderCpuMs` (mêmes cas qu'à l'étape 3, tableau avant / après dans le Bilan) ; seuils proposés pour l'acceptation (appels et ms par frame) écrits dans `current.md` à la ligne « Test d'acceptation » ; documents ci-dessous.

## Démo
Pas de nouvelle scène : `BatchStress2D` est la scène de mesure (et deviendra la scène d'acceptation). Si un cas manque au rapport (texte ou animations en nombre), l'ajouter aux bancs plutôt qu'à la scène, ce que fera l'objectif « Test d'acceptation ».

## Documents à mettre à jour à la fin
- `architecture.md`, « Lot de dessins 2D » : ordre du chemin d'un dessin (caméra, culling, puis matériau), version des `ShaderValue` et caméra suivie, écritures groupées, événement `mightylib.BatchSend` ; chiffres actuels par dessin si l'on en garde.
- `debug-tools.md`, Profilage : `scenario.py --jfr`, `tools/jfr_summary.py`, `tools/bench.py`, `-Dmightylib.bench=true`, `mightylib.BatchSend`.
- `decisions.md` : si la règle « une valeur suivie par version, sinon comparée » mérite une ligne durable.
- `later.md` : partition spatiale 2D / cache statique pour ne pas appeler `display()` hors champ (cible durable des scènes de dizaines de milliers d'objets, à relier à « Entités et scripts Lua ») ; coût de `update` des transformations (`setRotationZ` recalcule toute la matrice) s'il est vu dans le rapport ; réordonnancement avec test de chevauchement (déjà noté dans `current.md`, à déplacer s'il reste).
- `current.md` : réduire « Regroupement des dessins : reste » à ce qui reste (le réordonnancement, s'il y reste) ; ligne « Test d'acceptation » avec les seuils proposés et les mesures finales à jour.
- `CLAUDE.md` : tests couverts (`ShaderValue` versions, compléments de `BatchStateTableTest`, `ViewCulling2DTest`, `DrawBatch2DTest`, `MaterialTest`), bancs et leur commande (`python tools/bench.py`), `--jfr` de `scenario.py`.
- Supprimer ce plan (discussion B).

## Hors périmètre
- Partition spatiale / cache statique des objets hors champ (`later.md`).
- Réordonnancement automatique avec test de chevauchement (`current.md` → `later.md`).
- Seuils et contenu complets de la scène d'acceptation (objectif « Test d'acceptation » de `current.md`).
- Coût GL de l'envoi (pilote, `glBufferSubData`, modes) : mesuré par `BatchSend`, pas optimisé ici.
- Filet d'un pixel au-dessus des animations (objectif à part de `current.md`).
- 3D : rien à changer, ne pas casser (`Camera3D` reçoit seulement le marquage de version de l'étape 4 ; scènes 3D vérifiées à l'étape 4).

## Questions restantes
- (Étape 3) Cible chiffrée définitive et ordre des étapes 4 à 7, fixés avec l'utilisateur sur le rapport de mesure.

## Modèles recommandés
- Discussion A :
  - Étapes 1 à 3 (outils, bancs, rapport) : Claude Sonnet 5.5, effort `medium` (outillage et mesures, peu de conception).
  - Étapes 4 à 7 (version des valeurs, ordre du culling, écritures groupées, points chauds) : Claude Opus 5.5, effort `high` (modèle durable du lot, accroches publiques, risque de rendu faux).
  - Étape 8 (mesure finale, docs) : Claude Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (gros diff dans le cœur du lot 2D à relire au regard du plan et des mesures).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/cout-cpu-par-dessin.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/batch-cpu-cost`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`BatchStress2D` : rectangles et formes F à 16 000 et 128 000, `batch mode` dans les quatre modes, zoom et déplacement de caméra pendant le défilement ; `BatchRenderers2D`, `ViewCulling2D`, `Scene2D` : rendu inchangé ; une scène 3D, `Scene3D`, toujours fonctionnelle). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/batch-cpu-cost`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
