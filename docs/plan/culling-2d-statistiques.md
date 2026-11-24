# Plan — Culling 2D et statistiques du lot

**Statut : validé.**

Décisions prises :
- Culling des dessins 2D hors de la vue **activé par défaut** (comme Bevy, Godot, Unity) ; retrait par renderer ; option globale (`EngineConfig`, propriété système, commande `batch cull on|off`).
- Testés : les **dessins regroupés** (au moment de `submitToBatch`, avant d'écrire les sommets) et l'**API directe** du lot (`drawRegion`, `drawRectangle`, `drawQuad`). Pas les dessins seuls (3D, profondeur, shader sans lien de lot) ni `DebugDraw2D`.
- Compteurs : dessins testés, éliminés, sans boîte (par classe), octets envoyés (par format), taille moyenne d'un appel (dessins et octets par appel), tuiles éliminées par `TileMapRenderer`.
- Outils de vérification : zone de test réduite (`inset`), boîtes dessinées (`show`), liste JSON des éliminés (`list`), zone de test figée (`freeze`).
- Noms : `Renderer.setViewCulling(boolean)`, accroche `viewCullBounds`, `-Dmightylib.viewCulling2D=on|off`, commande `batch cull` (`setFaceCulling` désigne déjà le culling des faces GL).
- Marge des sommets déplacés par un shader : par renderer (`setViewCullMargin`) et par entrée de `shaders.json` (`"cullMargin"`, unités du monde).
- Démo : nouvelle scène `ViewCulling2D` ; `BatchStress2D` reste pour le test d'acceptation.
- Mesure : celle du culling seul (coût du test, gain) ; le profilage JFR du coût par dessin reste dans la première ligne de `current.md`.

## Pour démarrer
- Lire : `CLAUDE.md` ; `docs/architecture.md`, sections « Rendu » (puces Formes 2D, Lot de dessins 2D, Dessins de debug) ; `docs/debug-tools.md`, section Commandes (commande `batch`, `stats`) ; `docs/decisions.md`, puces « Dessin 2D différé dans un lot par vue » et « Format du lot ».
- Branche de sujet depuis `dev` : `feature/culling-2d`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `python tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Lot 2D** (`mightylib/.../graphics/renderer/_2D/batch/DrawBatch2D.java`) : `beginDraw(état, couche, profondeur)`, valeurs au format, `index`, `endDraw`. Un dessin coûte ~260 ns de CPU (écriture des sommets, mesuré à 128 000 rectangles sur `BatchStress2D`), qu'il soit visible ou non : **aucun test de visibilité** aujourd'hui.
- **Soumission** : `Renderer.display()` appelle `submitToBatch(batch)` (accroche) quand le mode n'est pas `OFF`. Implémentations : `Shape2DRenderer` (positions locales `calculatePosition` gardées en cache, `BatchGeometry2D.WriteTriangles` avec la matrice modèle), `VectorShape2DRenderer` (une instance `SHAPE`, `final`), `PointsRenderer` (`POINT`), `GeometricShape2DRenderer` (`STROKE`), `Animation2DRenderer` (quad + UV de la frame), `MeshRenderer` (maillage avec `keepCpuData()` : `Text`, `GraphRenderer`, `NodesRenderer`, `MatrixRenderer`), `TileMapRenderer` (déjà ses tuiles visibles seulement : `graphics/game/TileVisibility`, boîte de la vue dans l'espace de la carte).
- **État d'un dessin** : `Renderer.batchState(batch)` → `BatchStateTable.state(part, caméra, linéaire)` ; la table (`batch/internal/BatchStateTable`) garde une **place par caméra** avec ses matrices (`camera(projection, view)`, `projectionOf`, `viewOf`, `cameraOf(state)`) : l'endroit naturel pour la boîte de la vue, calculée une fois par place.
- **Marges des formes** : `ShapeStroke.quadMargin(worldPerPixel, antialiasing, extraMargin)` (copie Java du calcul de `utils/shape2DVertex.glsl`, testée par `ShapeStrokeTest`) ; `VectorShape2DRenderer.extraMargin()` ; contour `EStrokeUnit.SCREEN_PIXELS` (marge en pixels, dépend du zoom). Formes géométriques : `Stroker`/`StrokeGeometry` (points, jointures avec `setMiterLimit`, extrémités).
- **Nom pris** : `Renderer.setFaceCulling` / `RenderState.isFaceCulling` = culling des faces (GL) ; le nouveau réglage doit s'en distinguer.
- **Options** : modèle de `EngineConfig.setBatch2DMode` + `-Dmightylib.batch2D` + `Engine.setBatch2DMode` + commande `batch mode` (`utils/enginecommand/internal/BatchGenCom`, test `BatchGenComTest`).
- **Compteurs** : `main/utils/EngineStats` (`batchFlushesPerFrame`, `batchedDrawsPerFrame`, `batchBarriersPerFrame`, `renderCpuMs`, JSON de `stats`) ; `DrawBatch2D.lastFrame*` et `describe()` (commande `batch` : appels par format, barrières par cause).
- **Debug** : `DebugDraw2D` (`IView.debugDraw()`, `rect`, `polyline`… en coordonnées du monde ou de l'écran, durées de vie).
- **Tests existants** : `DrawBatch2DTest`, `BatchStateTableTest`, `BatchGenComTest`, `ShapeStrokeTest`, `TileVisibilityTest` ; tests GL du kit `DrawBatch2DGLTest`, `VectorShapeBatchGLTest`, `Animation2DBatchGLTest`, `TextBatchGLTest`, `TileMapBatchGLTest`, `MeshBatchGLTest`, `RectangleBatchGLTest` (rendu `off` = regroupé au pixel près).
- **Ailleurs** : Bevy calcule un `Aabb` par sprite/`Mesh2d`, retrait par `NoFrustumCulling`, exemple de debug qui dessine les boîtes (le texte 2D n'avait pas de boîte : écueil à éviter) ; Godot élimine chaque `CanvasItem` par son rectangle, rectangle propre par `canvas_item_set_custom_rect` ; libGDX, raylib, MonoGame n'éliminent rien dans leur lot.

## Décisions
- **Test dans `submitToBatch`, avant l'écriture** : c'est l'écriture qui coûte ; un dessin éliminé ne prend ni sommets, ni indice d'appel, ni place dans la série (il ne coupe rien).
- **Boîte de la vue en coordonnées du monde, par place de caméra** (`BatchStateTable`) : calculée une fois quand la place est créée ou mise à jour (inverse de projection × vue sur les coins du clip, puis boîte englobante), avec la taille d'un pixel en unités du monde (pour les marges en pixels). Test = 4 comparaisons. Une caméra tournée donne une boîte plus grande (moins d'éliminés, jamais un visible perdu). Caméra non affine 2D (perspective, `Game3D2D`) : pas de boîte, aucun dessin éliminé.
- **Boîte du dessin = accroche de `Renderer`** redéfinissable : boîte locale (espace du modèle, transformée par la matrice modèle 2D affine : boîte des 4 coins) + marge en unités du monde + marge en pixels d'écran ; ou directement une boîte du monde. Aucune boîte = jamais éliminé (et compté « sans boîte »). Écrite dans un objet réutilisé passé par le lot (aucune allocation).
- **Boîtes fournies par le moteur** : `Shape2DRenderer` (boîte de `calculatePosition`, recalculée avec elle), `Animation2DRenderer` (quad), `MeshRenderer` à données CPU (boîte des positions, recalculée quand le VBO de positions change), donc `Text` ; `VectorShape2DRenderer` (rectangle + `ShapeStroke.quadMargin`, contour `SCREEN_PIXELS` en marge pixels) ; `PointsRenderer` (boîte des centres + taille des points) ; `GeometricShape2DRenderer` (boîte des points + demi-épaisseur × limite d'onglet, extrémités, frange d'un pixel) ; API directe (coins connus). `TileMapRenderer` garde son test par tuile.
- **Activé par défaut**, retrait par renderer `setViewCulling(false)` (comme `NoFrustumCulling` de Bevy) ; option globale `EngineConfig.setViewCulling2D`, `-Dmightylib.viewCulling2D=on|off`, `Engine.setViewCulling2D` (à chaud), commande `batch cull on|off`. Actif en release.
- **Marge des sommets déplacés** (ondulation, vent dans un programme de lot) : `Renderer.setViewCullMargin(unitésDuMonde)` et champ optionnel `"cullMargin"` (nombre ≥ 0, sinon `ResourceException` comme un define invalide) d'une entrée de `shaders.json` ; la marge d'un dessin = celle du renderer + le maximum de celles de l'entrée de son shader et de son lien `"batch"`, rangée dans la part de matériau de la table (aucune recherche par dessin).
- **Taille d'un pixel** : calculée avec le viewport du lot (lu à son début), pas stockée dans la place de caméra (une caméra peut servir dans deux fenêtres de tailles différentes).
- **Compteurs** (`stats` et `batch`) : dessins testés, éliminés, sans boîte (par classe dans `batch`), octets envoyés par frame (sommets + instances + indices ; par format dans `batch`), dessins et octets par appel, tuiles éliminées et dessinées par `TileMapRenderer`.
- **Outils** (sous-commandes de `batch cull`, niveau `DEV`) : `inset <pixels>` (zone de test réduite de N pixels de chaque côté : les dessins disparaissent au bord, vérifiable par capture ciblée) ; `show` (boîte de chaque dessin testé par `DebugDraw2D` : dessinés et éliminés de deux couleurs, sans boîte non dessinés mais comptés) ; `list` (éliminés de la dernière frame en JSON : classe, boîte ; enregistrés seulement quand demandé) ; `freeze` / `unfreeze` (la boîte de vue de chaque caméra est figée, clé = matrice de vue de la caméra : on déplace la caméra pour voir ce qui est éliminé).

## Étapes
Chaque étape = un commit ; compiler, `mvn -o -q test`, vérifier en jeu quand l'étape se voit.

1. **Boîte de vue et test, sans renderer** (fusionnable seule : rien ne l'utilise encore).
   - `batch/internal` : calcul de la boîte du monde d'une place de caméra (inverse de projection × vue sur les 4 coins du clip, boîte englobante ; non affine 2D → aucune boîte) et de la taille d'un pixel pour un viewport ; stocké dans `BatchStateTable` par place, recalculé quand la place est créée ou mise à jour sur place.
   - `batch` (public, `EXPERIMENTAL`) : `ViewCullBounds` (`final`, réutilisé, aucune allocation) : `local(minX, minY, maxX, maxY)` (espace du modèle, transformé par une matrice modèle 2D affine : boîte des 4 coins ; non affine → jamais éliminé) ou `world(…)`, `margin(monde, pixels)`.
   - `DrawBatch2D.isCulled(état, bounds)` : vrai si la boîte (marges comprises) est entièrement hors de la boîte de vue de la caméra de l'état ; compteurs de frame testés / éliminés / sans boîte. Bord : une boîte qui touche la vue n'est pas éliminée.
   - Option : `EngineConfig.setViewCulling2D(boolean)`, `-Dmightylib.viewCulling2D=on|off` (constante de propriété comme `BATCH_2D_PROPERTY`), `Engine.setViewCulling2D`/`isViewCulling2D` ; coupée → `isCulled` rend toujours faux sans compter.
   - Tests sans GL : `ViewCulling2DTest` (translation, zoom, rotation de la caméra, taille virtuelle, perspective → aucune élimination, marges monde et pixels, boîte touchant le bord, modèle tourné), `BatchStateTableTest` (boîte suivie quand la place est mise à jour sur place, remise à zéro), `DrawBatch2DTest` (option coupée, compteurs), `ViewCullingOptionTest` (`-D`).
2. **Accroche de `Renderer`, renderers simples, API directe, marge des shaders.**
   - `Renderer.setViewCulling(boolean)` / `isViewCulling()` (défaut vrai), `setViewCullMargin(float)` ; accroche `protected boolean viewCullBounds(ViewCullBounds out)` (Javadoc courte : appelée avant l'écriture du dessin regroupé, `false` = aucune boîte, jamais éliminé ; la base rend `false`) ; méthode `protected final boolean viewCulled(DrawBatch2D batch, int state)` appelée par chaque `submitToBatch` juste après `batchState` (retrait, accroche, marges du renderer et de la part de matériau, `isCulled`) : éliminé → `submitToBatch` rend `true` sans rien écrire.
   - Boîtes : `Shape2DRenderer` (boîte de `calculatePosition`, en cache avec elle), `Animation2DRenderer` (quad), `MeshRenderer` à données CPU (boîte des positions du canal 0, recalculée quand `MeshCpuData` change ; donc `Text`, `GraphRenderer`, `NodesRenderer`, `MatrixRenderer`). Calcul des boîtes dans des fonctions statiques sans GL.
   - API directe : `drawRegion`, `drawRectangle`, `drawQuad` testent leurs coins (rendent `true` quand éliminés : le dessin est « fait »).
   - `shaders.json` : champ `"cullMargin"` lu et vérifié par `ShaderService` (gardé dans `ShaderData`), marge de la part de matériau (`MaterialBatchPart`).
   - Tests : boîtes sans GL (`MeshCpuDataTest` complété, positions d'une forme avec point de référence), `ShaderServiceTest` (`cullMargin` lu, refusé si négatif ou non numérique), `DrawBatch2DTest` (un dessin éliminé n'écrit rien, ne coupe pas la série, ne prend pas d'indice d'appel avec une clé de tri) ; kit GL `ViewCullingGLTest` : rectangles, animations, textes, maillages au bord de la vue, juste dedans et juste dehors, caméra zoomée et tournée → rendu culling activé = coupé au pixel près (`BatchPictures`), compteur d'éliminés attendu.
3. **Formes vectorielles, points, formes géométriques.**
   - `VectorShape2DRenderer` : rectangle de la forme + `ShapeStroke.quadMargin` (contour `WORLD` en marge du monde, `SCREEN_PIXELS` et pixel d'anticrénelage en marge pixels, `extraMargin`). `PointsRenderer` : boîte des centres + demi-taille des points + pixel. `GeometricShape2DRenderer` : boîte des points de `StrokeGeometry` + demi-épaisseur × max(1, limite d'onglet) (jointures `MITER`), extrémités `SQUARE`/`ROUND`, frange d'un pixel ; contour en pixels selon son unité.
   - Tests sans GL : `ShapeCullBoundsTest` (chaque alignement de contour, unité pixel à deux zooms, onglet limité, extrémités) ; kit GL : `VectorShapeBatchGLTest` ou `ViewCullingGLTest` complété (chaque forme vectorielle et géométrique au bord, contour `OUTER` et `SCREEN_PIXELS`, zoom 0,25 et 4, rotation) → culling activé = coupé au pixel près.
4. **Compteurs.**
   - `EngineStats` / `stats` : `viewCullTestedPerFrame`, `viewCulledPerFrame`, `viewCullNoBoundsPerFrame`, `batchBytesPerFrame` (sommets + instances + indices réellement envoyés, comptés à l'envoi), `batchDrawsPerCall`, `batchBytesPerCall`, `tilesCulledPerFrame`, `tilesDrawnPerFrame` (`TileMapRenderer` : tuiles de la carte hors de la plage visible / écrites).
   - Commande `batch` (`describe`) : par vue, les mêmes compteurs ; par format : appels, octets ; dessins sans boîte par classe (10 premières classes).
   - Tests : `DrawBatch2DTest` (octets = tailles du format × sommets/instances + indices), `TileVisibilityTest` ou test de `TileMapRenderer` sans GL (tuiles comptées), `BatchGenComTest` (nouvelles clés).
5. **Outils de vérification** (`batch cull …`, syntaxe déclarée, niveau `DEV` sauf `on|off` au niveau de `batch mode`, message lisible, détail dans les `data`).
   - `on|off` ; `inset <pixels>` (0 = aucun ; zone de test réduite de chaque côté, en pixels du viewport) ; `show [on|off]` (boîte de chaque dessin testé par le `DebugDraw2D` de la vue, en coordonnées du monde, une frame : dessinés en vert, éliminés en rouge ; `DebugDraw2D` n'est pas testé, donc ses boîtes ne s'éliminent pas) ; `list` (éliminés de la prochaine frame complète en JSON : classe, boîte du monde, caméra ; enregistrés seulement pour cette frame, 1 000 au plus, `truncated`) ; `freeze` / `unfreeze` (boîte de vue de chaque caméra figée, clé = objet matrice de vue de la place ; une caméra nouvelle après le gel garde sa vraie boîte).
   - Tests : `BatchGenComTest` (ou `BatchCullComTest`) : analyse, complétion, effets sur l'état sans GL (inset appliqué au test, freeze garde la boîte quand la caméra bouge).
   - Vérification en jeu : `python tools/scenario.py "scene ViewCulling2D" wait-loaded "batch cull inset 200" "?stats" "screenshot --rect …"` : les dessins disparaissent à 200 pixels du bord, rien à l'intérieur.
6. **Démo `ViewCulling2D` et mesure.**
   - Scène (voir Démo), inscrite dans `DemoScenes` avec sa description ; `leakcheck ViewCulling2D`.
   - Mesure sur `BatchStress2D` (sans la modifier : caméra déplacée ou zoomée par la commande `camera`) : `renderCpuMs` culling activé puis coupé, tout visible (coût du test) et ~90 % hors champ (gain), à deux tailles (16 000 et 128 000 rectangles) ; chiffres datés, gardés pour `current.md`.
7. **Documents** (liste plus bas) et section « Bilan ».

## Démo
`ViewCulling2D` (nouvelle scène, `demo/.../scenes/`) : un monde d'environ 20 × l'écran rempli de tous les renderers regroupés (rectangles d'un atlas, animations, formes vectorielles avec contours monde et pixels, polylignes et polygones, points, textes, une tilemap, un maillage), dont quelques-uns retirés du test (`setViewCulling(false)`) et un avec `setViewCullMargin`. Caméra au clavier (flèches : déplacer, molette ou +/− : zoom, Q/E : rotation), touche pour couper/activer le culling, touche pour `inset` 200 pixels, touche pour `show`. Texte d'aide et compteurs (testés, éliminés, sans boîte, appels, octets, `renderCpuMs`) dans un coin, par une caméra d'écran.

## Documents à mettre à jour à la fin
- `architecture.md`, puce « Lot de dessins 2D » : culling (boîte de vue par caméra, accroche, renderers, option, compteurs), au présent.
- `debug-tools.md` : `batch cull …`, nouveaux compteurs de `stats` et de `batch`, `-D` de l'option.
- `decisions.md`, puce du lot 2D : culling par boîte avant l'écriture, activé par défaut, retrait par renderer.
- `current.md` : supprimer la ligne « Culling 2D et statistiques du lot » ; reporter dans la ligne du test d'acceptation les mesures faites ici.
- `later.md` : structure spatiale (grille, arbre) si un jeu a des centaines de milliers d'objets statiques ; culling des dessins seuls 2D.
- `CLAUDE.md` : tests couverts (`ViewCulling2DTest`, `ViewCullingOptionTest`, `ShapeCullBoundsTest`, kit `ViewCullingGLTest`, compléments de `DrawBatch2DTest`, `BatchStateTableTest`, `ShaderServiceTest`, `BatchGenComTest`).
- `licence.md` : rien (aucune dépendance ni ressource nouvelle).
- Supprimer ce plan.

## Hors périmètre
- Dessins seuls (3D, profondeur, shaders sans lien de lot) et `DebugDraw2D` : non testés (`later.md`).
- Structure spatiale (grille, quadtree) : `later.md`.
- Seuils du test d'acceptation : objectif `BatchStress2D` de `current.md`.
- Profilage JFR du coût par dessin : première ligne de `current.md`.
- Événements de visibilité pour le jeu (entrée/sortie de l'écran, comme `VisibleOnScreenNotifier2D`) : à noter dans `later.md` si utile (logique de jeu, pas de rendu).

## Questions restantes
Aucune.

## Modèles recommandés
- Discussion A, étapes 1 à 3 (modèle de la boîte de vue, accroche d'une classe extensible, marges exactes des formes, rendu identique au pixel) : **Claude Opus 5.5, effort `high`**.
- Discussion A, étapes 4 à 7 (compteurs, sous-commandes à syntaxe déclarée, scène de démo, mesure, docs) : **Claude Sonnet 5.5, effort `medium`**.
- Discussion B (`/pr-ml`) : **Claude Opus 5.5, effort `high`** (gros diff à relire au regard du plan, rendu au pixel près).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/culling-2d-statistiques.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/culling-2d`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (scène `ViewCulling2D` : déplacer, zoomer et tourner la caméra, aucun dessin ne disparaît au bord de l'écran, contours `SCREEN_PIXELS` à fort et faible zoom ; `batch cull inset 200`, `show`, `freeze` puis déplacer la caméra ; `Scene2D`, `VectorShapes2D`, `BatchRenderers2D`, `DebugDrawing2D` identiques ; scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` toujours fonctionnelles ; `-Dmightylib.viewCulling2D=off`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/culling-2d`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
**Fait** (sept commits sur `feature/culling-2d`, un par étape) : boîte de vue par place de caméra (`BatchStateTable`), `ViewCullBounds`, `DrawBatch2D.isCulled`, option (`EngineConfig`, `-Dmightylib.viewCulling2D`, `Engine`) ; accroche `Renderer.viewCullBounds` / `viewCulled`, `setViewCulling`, `setViewCullMargin`, boîtes de `Shape2DRenderer`, `Animation2DRenderer`, `MeshRenderer` (donc `Text`), formes vectorielles, points, formes géométriques, API directe, `"cullMargin"` de `shaders.json` ; compteurs (`stats`, `batch`, tuiles) ; `batch cull on|off|inset|show|freeze|unfreeze|list` ; scène `ViewCulling2D` ; mesure ; docs (`architecture.md`, `debug-tools.md`, `decisions.md`, `current.md`, `later.md`, `CLAUDE.md`).

**Écarts au plan**
- Marges des formes vectorielles : la marge en pixels ne s'ajoute pas à la boîte du monde (le shader l'applique le long des axes de la forme, tournés) ; la boîte reçoit la taille d'un pixel (`ViewCullBounds.worldPerPixel` / `localPerPixel`, par `DrawBatch2D.cullBounds(état, modèle, classe)`) et chaque forme calcule sa marge exacte dans son espace (`ShapeCullBounds`). `ViewCullBounds.margin(monde, pixels)` reste pour les renderers d'un projet.
- Octets par format : ceux que les dessins ont écrits (sommets × taille + 4 × indices, ou instances), pas ceux envoyés (en mode `instanced`, les quads convertis en instances envoient moins) ; le total `bytes` est exact (compté à l'envoi).
- `batch cull list` : le premier appel lance l'enregistrement (une commande rend tout de suite, elle ne peut pas attendre la frame suivante), les suivants rendent la dernière frame complète ; `list off` l'arrête.
- `batch cull show` : seuls les dessins de la caméra 2D de la vue sont montrés (les boîtes sont en coordonnées du monde) ; avec `inset`, le texte de l'aide de `ViewCulling2D` disparaît aussi (il est dans la zone réduite).
- `ViewCullTools` est dans `batch/internal` (le moteur le garde, la commande le pilote) ; `IBatchBackend` et `IBatchTarget` ont des méthodes par défaut (`viewCulling`, `cullTools`, `viewportWidth/Height`) pour les tests.
- Mesure : script local `tools/scripts/measure_culling.py` (non versionné) ; chiffres dans `current.md`.
- La carte de `TileMapBatchGLTest` n'a que 16 tuiles : le test vérifie que tuiles écrites + éliminées est constant et que tout est éliminé hors de la carte.

**Vérifié** : `mvn -o -q test` et `mvn -o -q package` verts ; kit GL : rendu avec culling = sans culling au pixel près (sept vues de caméra : translation, zoom de 0,25 à 4, rotation ; rectangles, animations, textes, maillages, formes vectorielles et géométriques, dessins sans renderer), formes au bord (contour en pixels, contour extérieur, frange) dessinées ou éliminées comme attendu ; en jeu : `ViewCulling2D` (746 dessins éliminés sur 797 testés au départ, 782 avec `inset 200`, 0 avec `off`), `batch cull list`, `show` (une capture : boîtes vertes dedans et rouges au bord, rien de dessiné à moins de 200 pixels du bord), `freeze`, `leakcheck ViewCulling2D` sans fuite, `BatchRenderers2D`, `Scene2D`, `VectorShapes2D`, `DebugDrawing2D`, `Game3D2D` (caméra perspective : 0 éliminé sur 4 testés) et les scènes 3D sans erreur au journal.

**À vérifier à la main** : scène `ViewCulling2D` : déplacer, zoomer et tourner la caméra, aucun dessin ne disparaît au bord de l'écran, contours `SCREEN_PIXELS` à fort et faible zoom ; `batch cull inset 200`, `show`, `freeze` puis déplacer la caméra ; `Scene2D`, `VectorShapes2D`, `BatchRenderers2D`, `DebugDrawing2D` identiques ; scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` toujours fonctionnelles (chargées sans erreur au journal ici, rendu non regardé) ; `-Dmightylib.viewCulling2D=off`.
