# Test d'acceptation du lot 2D : `BatchStress2D`

**Statut : validé.**

Décisions prises :
- `BatchStress2D` garde ses modes rectangles et formes ; F passe en cycle rectangles → formes → **mélange** ; les seuils d'acceptation portent sur le mélange.
- Mélange : par 16 objets, 8 sprites animés, 4 rectangles de l'atlas, 3 formes vectorielles (dont 1 avec contour), 1 court texte.
- Sprites animés et rectangles lus dans **un atlas de démo généré** (tileset + frames du slime dans une seule texture) par un script versionné.
- Vérification en deux parties : un **test GL de la démo** (`mvn test` : rendu de chaque mode = `off` au pixel près, bornes d'appels, aucune allocation qui croît avec N) et un outil versionné **`tools/stress.py`** (temps CPU, médiane de 3, contre des seuils ; hors `mvn test`, dépend de la machine).
- Seuils : tout est remesuré au début ; ceux de `current.md` restent si les mesures n'ont pas bougé ; ceux du mélange = mesure + ~15 %, validés par l'utilisateur.
- Allocations : le test GL compare les octets alloués par le thread principal sur k frames à N et à 4N (égaux à une tolérance près).
- Changement de frame d'une animation : corrigé dans le **modèle de `Mesh`** (envoi au GPU différé d'un VBO tenu en copie CPU) et sans allocation dans `Animation2DRenderer`.
- Mélange : borne d'appels sur le mélange **trié** (O) ; temps mesurés et seuillés trié et non trié.

## Pour démarrer
- Lire : `docs/current.md` (objectif « Test d'acceptation »), `docs/architecture.md` (« Lot de dessins 2D », « Formes 2D », section des `Mesh`/`MeshCpuData`), `docs/debug-tools.md` (`stats`, `batch`, « JFR en un appel », « Bancs du coût CPU »), `docs/later.md` (« Culling 2D : suites », « Effets 2D », « Rendu »).
- Branche de sujet depuis `dev` : `feature/batch-acceptance`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `demo/.../scenes/BatchStress2D.java` : N rectangles (3 texturés d'une région de `tileset` pour 1 coloré, `Material` `texture2D`/`colorShape2D` retenus par la scène) ou, après F, N formes vectorielles mêlées (7 sortes, 1 sur 3 avec contour) ; tout tourne (`setRotationZ(time * speed)`) ; Page Up / Page Down (×2, /2, max 2^17), M (mode du lot), O (`DrawSortKeys.layerThenState()`), caméra libre (`DemoCamera.free`), compteurs de `EngineStats.toJson()` toutes les 0,5 s ; graine fixe 42 (même image pour le même N). Description dans `DemoScenes` (vérifiée par `DemoScenesTest`).
- Seule animation : `resources/animations/slime.anim` (feuille `slime.png` 84×90, 9 frames de 28×30, batch `anim`). `Animation` n'a pas de moyen de partir d'une frame donnée (`restart`, `update`, vitesse seulement).
- `Animation2DRenderer.updateInfoForFrame` (appelé à chaque changement de frame) : `Animation.currentTexturePosition()` alloue un `Vector4f`, `texturePos()` un `Vector4f` et un `float[]`, `updatePosition` un `Vector3f`, `setScale` un `Vector3f` (et `getFrameSize`/`currentHotPoint` sont à vérifier) ; `updateTextures` appelle `Mesh.updateVbo`, qui envoie tout de suite au GPU (`bind()` du VAO, `glBufferSubData`) même quand le renderer est dessiné par le lot (qui lit la copie CPU `MeshCpuData`).
- `Mesh` (`graphics/renderer/Mesh.java`) : `keepCpuData()` active une copie `MeshCpuData` (copiée dans son propre tableau, `version` incrémentée) ; `updateVbo`/`updateSubVbo` (float) écrivent la copie puis envoient ; `draw()` est le chemin « dessiné seul ». `Mesh` est commun à la 3D.
- Mesures : scripts locaux non versionnés `tools/scripts/stress_shapes_measure.py` (cas, médiane de 3 de `?stats`, vsync off, mode `orphan`, zoom par `input scroll`) et `tools/scripts/compare_batch_modes.py` (captures par mode comparées à `off`, lecteur PNG sans dépendance). `tools/bench.py` (bancs du coût d'un dessin) et `scenario.py --jfr` existent.
- Tests modèles : `demo/src/test/.../scenes/TileMapBatchGLTest.java` (capture dans un framebuffer, comparaison au canal près, `EngineStats.endSecond` pour lire les compteurs d'une frame), `Animation2DBatchGLTest` (kit), `DrawBatch2DTest` et `IdentifierTest` (allocations par `ThreadMXBean.getThreadAllocatedBytes`), `MeshCpuDataTest`, `SharedMeshTest`. `SceneRunner` : `start`, `untilLoaded`, `frames`, `key`, `execute`.
- `tools/generate_layers.py` : précédent de script versionné qui génère une ressource de démo.

## Décisions
- Mélange par motif de 16 (indices `i % 16`) : 0–7 sprites animés (frame de départ et sens tirés de la graine, retournement horizontal une fois sur deux, échelle 1 à 2), 8–11 rectangles de régions de l'atlas, 12–14 formes (14 avec contour, sortes tournantes comme le mode formes), 15 texte (~8 caractères, deux tailles, fixe, ne tourne pas). Tout le reste tourne comme aujourd'hui.
- Atlas : une PNG `stressAtlas` = tileset en haut, frames du slime en dessous, sans marge (comme les feuilles existantes ; marge et retrait du demi-texel restent dans `later.md`) ; une `.anim` `stressSlime` aux positions décalées ; nouveau batch `stress` pris par `BatchStress2D.getInvolvedBatch`. Les rectangles du mélange calculent leurs UV en pixels de l'atlas.
- `Animation.setFrame(int)` (public) : choisir la frame courante (frame de départ des sprites) ; borne vérifiée.
- `Mesh` : avec une copie CPU, `updateVbo(float[])`/`updateSubVbo(float[])` n'envoient plus rien, ils marquent le VBO sale dans `MeshCpuData` ; `draw()` (et tout autre chemin qui lit les VBO sur le GPU) envoie d'abord les VBO sales ; `EngineStats.countBufferUpload` compte l'envoi réel. Sans copie CPU : envoi immédiat inchangé. Le lot ne touche pas le GPU du `Mesh`.
- `stress.py` : cas nommés (rectangles, formes, mélange non trié, mélange trié ; tout visible et ~10 % visible) × N, mode `orphan`, vsync off, médiane de 3 de `renderCpuMs` et appels ; seuils dans le script, `PASS`/`FAIL` par cas, `--json`, `--case` pour en lancer un.
- Test GL : `demo/src/test/.../scenes/BatchStress2DGLTest.java`, N petit (≈ 2 048), temps manuel du kit (image reproductible).

## Étapes (un commit chacune)
1. **`tools/stress.py` versionné, mesure de départ** : reprendre la logique de `stress_shapes_measure.py` (pilotage de `scenario.py`, médiane de 3, cas rectangles et formes de `current.md`), seuils de `current.md` dans le script. Lancer, montrer les mesures à l'utilisateur, ajuster les seuils de rectangles et formes s'ils ont bougé (avec son accord). Fusionnable seule.
2. **`Mesh` : envoi différé des VBO tenus en copie CPU** : `MeshCpuData` garde un drapeau sale par VBO (posé par `setVbo`/`setSubVbo`, effacé à l'envoi) ; `Mesh.draw()` envoie les sales. Tests sans GL : `MeshCpuDataTest` (drapeaux, plusieurs mises à jour avant un envoi, sous-plage). Test GL du kit : un mesh mis à jour deux fois puis dessiné seul = les dernières valeurs ; dessiné par le lot, aucun envoi (`bufferUpload` de `stats`). Vérifier en jeu `Scene2D`, `BatchRenderers2D` et les scènes 3D (`Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`) : rendu inchangé, journal propre.
3. **Changement de frame sans allocation** : `Animation.currentTexturePosition(Vector4f out)` (lit `getFramePositionReference`), `texturePos` dans un `float[8]` réutilisé, vecteurs de travail réutilisés dans `updatePosition`/`setScale`, `currentHotPoint`/`getFrameSize` sans allocation ; `Animation.setFrame(int)`. Tests : `FrameTexturePositionTest` complété ; dans `Animation2DBatchGLTest`, k changements de frame d'un renderer dessiné par le lot n'allouent rien (`ThreadMXBean`) et n'envoient rien au GPU.
4. **Atlas de démo** : `tools/generate_stress_atlas.py` (versionné) produit la PNG et la `.anim` depuis `tileset` et `slime.png` ; entrées de `textures.json`, batch `stress`, animation. Vérifier `resources` en jeu (chargées, aucune erreur).
5. **Mode mélange de `BatchStress2D`** : F en cycle à trois modes (même image pour le même N et le même mode, graine remise), composition ci-dessus, sprites animés via l'atlas (`update` de chaque `Animation2DRenderer`), textes, aide et description de `DemoScenes` mises à jour, compteurs inchangés. Cas du mélange ajoutés à `stress.py`. Vérifier en jeu : `scene BatchStress2D`, `input key F` ×2, `batch`, `?stats`, caméra (`input key hold D`, `input scroll 3`, `?camera`), `leakcheck BatchStress2D`.
6. **`BatchStress2DGLTest`** : mélange à N ≈ 2 048, non trié et trié : chaque mode (`orphan`, `ring`, `instanced`) = `off` au canal près (tolérance de `TileMapBatchGLTest`) ; appels du mélange trié ≤ borne (fixée d'après la mesure : un appel par état, une poignée) ; formes mêlées = 3 appels ; octets alloués sur k frames à N et à 4N égaux (tolérance de quelques Ko pour les allocations fixes). Rien d'autre ne doit être `@Disabled`.
7. **Seuils et docs** : mesurer le mélange avec `stress.py`, proposer les seuils (mesure + ~15 %), les faire valider, les écrire dans le script ; documents ci-dessous ; section « Bilan ».

## Démo
Pas de nouvelle scène : `BatchStress2D` est la scène de mesure existante, à compléter (décision de `current.md`). Sa description dans `DemoScenes` mentionne le mode mélange.

## Documents à mettre à jour à la fin
- `architecture.md` : envoi différé des VBO d'un `Mesh` avec copie CPU ; `BatchStress2D` et ses trois modes ; `Animation.setFrame`.
- `debug-tools.md` : `tools/stress.py` (cas, seuils, options), `tools/generate_stress_atlas.py` ; renvois de « JFR en un appel » et « Bancs ».
- `later.md` : section « Rendu » (mesures à jour), ce qui reste de « Culling 2D : suites » ; noter ce que les mesures montrent de nouveau.
- `current.md` : supprimer la puce ; c'était la dernière : le fichier ne garde que son en-tête, et le gros point suivant se discute avec l'utilisateur (`later.md`, « Écrire `current.md` »).
- `CLAUDE.md` : tests couverts (`BatchStress2DGLTest`, envoi différé du `Mesh`, allocations du changement de frame, `Animation.setFrame`), outil `stress.py`.
- `licence.md` : atlas généré (dérivé de `tileset` et `slime.png`, mêmes licences).
- Supprimer ce plan.

## Hors périmètre
- Marge entre régions d'atlas et retrait du demi-texel (`later.md`, « Débordement d'un demi-texel »).
- Atlas assemblé automatiquement au chargement, temps GPU dans `stats` (`later.md`, « Rendu »).
- Structure spatiale, réordonnancement automatique, matrices tenues par composantes (`later.md`, « Culling 2D : suites »).
- Envoi différé pour les VBO `int[]` et les meshes sans copie CPU (inchangés).

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1 : Sonnet 5.5, effort medium (script de mesure repris d'un existant).
- Étapes 2–3 : Opus 5.5, effort high (modèle de `Mesh` commun à la 3D, allocations et GL).
- Étapes 4–5 : Sonnet 5.5, effort medium (ressource générée, scène de démo).
- Étapes 6–7 : Opus 5.5, effort medium (test GL délicat, seuils, docs).
- Discussion B (`/pr-ml`) : Opus 5.5, effort high (diff touchant `Mesh` et la démo, relu au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/batch-acceptance.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/batch-acceptance`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`BatchStress2D` : F ×2 jusqu'au mélange, slimes animés sans filet, M dans chaque mode sans changement d'image, O, Page Up jusqu'à 16 000 ; scènes 3D inchangées après l'envoi différé du `Mesh`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/batch-acceptance`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Vérifications manuelles : celles du « Bilan ».

## Bilan

### Fait (un commit par étape, plus un correctif)
1. `tools/stress.py` (`ab49dbf8`) : 12 cas (rectangles, formes, mélange ; 16 000 ou 128 000 ; tout visible ou ~10 % ; trié ou non), médiane de 3 échantillons de `stats`, seuils `renderCpuMs` et appels dans le script, `PASS`/`FAIL`, `--case`, `--list`, `--json`, `--no-compile`.
2. `Mesh` (`a0115f90`) : envoi différé des VBO float tenus en copie CPU ; `MeshCpuData` garde une plage sale par VBO ; nouveau test GL du kit `MeshUploadGLTest`.
3. `Animation` (`4949f795`) : `setFrame`, `getCurrentFrame`, variantes `out` de `currentTexturePosition`/`currentHotPoint`/`getFrameSize` ; `Animation2DRenderer` sans allocation au changement de frame.
4. Atlas (`f9228d59`) : `tools/generate_stress_atlas.py`, `stressAtlas.png` (448 × 666), `stressSlime.anim`, batch `stress`.
5. Mélange (`76ae20c1`) : F en cycle à trois modes, motif de 16, cas du mélange dans `stress.py`.
6. Correctif du lot (`0b32818d`) : `BatchLayouts.PointAttributes`/`EnableAttributes` allouaient un itérateur par appel de dessin (allocations qui croissaient avec les appels du mélange non trié), trouvé par le test de l'étape 6 et un profil JFR. Puis `BatchStress2DGLTest` (`a49ff31f`).
7. Seuils et documents (ce commit).

### Écarts au plan, et pourquoi
- **Seuils remesurés** (étape 1, validés) : rectangles inchangés ; formes plus lentes qu'à la fixation de leurs seuils (≈ 5,0 ms contre 3,9) → 5,7 et 2,6 ms ; cas triés ajoutés (rectangles 2,2 ms en 4 appels, formes 6,2 ms) ; bornes d'appels 8 002 / 64 002 / 4 / 3. Étape 7 : `rect-128k-10%` relevé de 25 à 32 ms (21,6 à 28,0 ms d'une passe à l'autre, 28,0 déjà avant tout changement de code : bruit, pas régression) ; mélange 3,6 / 3,4 / 2,2 / 1,9 ms, appels 3 005 et 5 (validés).
- **Envoi dans `Mesh.bind()`, pas dans `draw()`** : `draw()` est une accroche redéfinissable (une sous-classe aurait sauté l'envoi) ; `bind()` est final et précède tout dessin. Plage sale (début, fin) plutôt qu'un drapeau : un seul envoi de l'union des plages. `updateSubVbo` d'un VBO inconnu de la copie : envoi immédiat. `updateSubVbo` (float et int) compte maintenant ses octets dans `stats`.
- **`Animation2DRenderer` n'avait pas de copie CPU** (le plan le supposait : le lot lisait ses propres tableaux) : `keepCpuData()` ajouté, sinon chaque changement de frame envoyait 32 octets même dessiné par le lot.
- Tests de `setFrame` et des variantes `out` dans `Animation2DBatchGLTest` (`Animation` a besoin du moteur), pas dans `FrameTexturePositionTest` (inchangé). Test d'allocation du changement de frame : 1 000 contre 4 000 changements (un coût fixe de ~3 Ko existe).
- « Sens » des sprites du mélange : compris comme le sens de rotation (signe de la vitesse tirée de la graine).
- **`BatchStress2DGLTest`** : N = 2 000 ; trié, l'image ne peut pas égaler `off` (le dessin seul ne trie jamais : les objets qui se recouvrent changent d'ordre) → trié, chaque mode = `orphan` trié, même nombre de pixels dessinés que non trié ; **formes mêlées = 1 appel** sans les textes de l'écran (les 3 de `stats` comptaient le fond et les deux textes) ; allocations : le minimum de 6 fenêtres de 60 frames après chauffe (les premières fenêtres tournent du code pas encore compilé par le JIT), 4N ≤ N + 16 Ko. `BatchStress2D.displayObjects`/`objectCount` (package-private) pour le test ; `capture`, `countDifferent`, `drawnPixels` de `TileMapBatchGLTest` rendus visibles dans le package.

### Vérifié, et comment
- `mvn -o -q test` complet vert après chaque étape de code (2, 3, 6).
- Résultats de `tools/stress.py` au 2026-11-26 : tous les cas sous leurs seuils, sauf `rect-128k-10%` avant son relèvement. Mélange 16 000 : 2,9 à 3,6 ms en 3 002 appels (non trié), 2,8 à 3,0 ms en 5 (trié), ~10 % : 1,7 à 1,9 ms.
- `BatchStress2DGLTest` : mélange de 2 000, chaque mode = `off` (0 à 3 canaux différents), trié en 3 appels, 396 016 octets pour 60 frames à 2 000 comme à 8 000 objets.
- En jeu (`scenario.py`), journal propre : `Scene2D`, `BatchRenderers2D` (modes `off` et `orphan`), `ViewCulling2D`, `FallbackScene`, `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` ; `leakcheck Scene2D` et `leakcheck BatchStress2D` sans fuite ; `BatchStress2D` : F ×2, `batch`, `stats`, caméra (`input key hold D`, `input scroll 3`, `?camera`), O, M ×4 ; une capture zoomée du mélange (slimes, tuiles, formes, texte corrects) ; `resources` : `stressAtlas` et `stressSlime` chargés.
- Bancs `python tools/bench.py --isolated --repeat 3` : rectangle texturé visible 70 ns, coloré 73, animation 77 (≤ 90) ; dessins éliminés 38 à 52 ns (≤ 50 à l'arrondi près ; texte éliminé 101, comme sur `dev`). Texte visible 272 à 448 ns sur la branche contre 358 à 473 sur `dev` (`benchTexts` lancé 4 fois sur chacun, dans un worktree de `dev`) : écart du JIT d'une JVM à l'autre, pas de régression.

### À vérifier à la main
- `BatchStress2D` : F ×2 jusqu'au mélange ; slimes animés sans filet au-dessus (zoom fort) ; M dans chaque mode sans changement d'image ; O (l'ordre des objets qui se recouvrent change, rien ne disparaît) ; Page Up jusqu'à 16 000.
- Scènes 3D (`Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`) : rendu inchangé après l'envoi différé du `Mesh` (journal propre vérifié, image non regardée).
