# Passage des renderers 2D au regroupement et dessin immédiat de debug

**Statut : validé.**

Décisions prises :
- Atlas des polices en `GL_TEXTURE_2D` (UV normalisées) : `Text` passe au lot par `sprite2D`, comme les sprites.
- `TileMapRenderer` : chaque frame, seules les tuiles visibles dans la caméra vont au lot (plage de la grille), pas toute la carte.
- Format extensible **déclaré en Java par programme de lot** (`BatchFormat` : attributs par sommet et/ou par instance), vérifié au lien du programme ; il fait partie de l'état d'un dessin.
- Formes à distance signée : **une instance par forme** (les uniforms de `shape2DV` deviennent des attributs d'instance), un programme de lot par type de forme ; formes géométriques (`stroke2D`) : leurs attributs par sommet.
- 3D : **hors du lot** (dessinée seule, barrière), mais le format ne suppose pas la 2D (taille et type d'attribut libres) pour qu'une 3D puisse s'y brancher plus tard.
- `MeshRenderer` reçoit un `submitToBatch` générique pour les maillages 2D en triangles : `GraphRenderer`, `NodesRenderer`, `MatrixRenderer` et les maillages d'un jeu passent au lot sans réécriture.
- Dessin immédiat de debug : `DebugDraw2D` **par vue**, dessiné **par-dessus** à la fin du `display` de la vue (monde ou écran), vidé au déchargement de la scène, actif en release, interrupteur global.
- Durée de vie d'un dessin de debug : interface `IDebugLifetime` vérifiée une fois par frame ; fournies : une frame (défaut), N frames, N secondes (**temps du jeu**, `GameTime` : figé en pause), tant qu'une condition est vraie, jusqu'au `remove()` de la poignée rendue.
- **Deux branches** : A1 (format fixe : animations, maillages, texte, tuiles), puis A2 (format extensible, formes, debug). Chacune a son `/pr-ml`.

## Pour démarrer
- Lire : `docs/architecture.md` sections **Rendu** (Formes 2D, Lot de dessins 2D, Service des shaders) et **Ressources GPU** ; `docs/debug-tools.md` section Commandes et la commande `batch` ; `docs/current.md`.
- Code : `graphics/renderer/Renderer.java` (`display`, `submitToBatch`, `batchState`, `batchColor`), `graphics/renderer/_2D/shape/Shape2DRenderer.java` (`submitToBatch`, `CanBatch`, `TransformPositions`), `graphics/renderer/_2D/batch/DrawBatch2D.java` et `batch/internal/` (uploaders, `BatchStateTable`), `shaders.json` du moteur.
- Branches depuis `dev` : `feature/batch-renderers-2d` (A1), puis `feature/batch-format-2d` (A2, depuis `dev` après la fusion de A1).
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Lot** (`DrawBatch2D`, un par vue) : format de sommet fixe (position 2, UV 2, couleur RGBA8 ; `VERTEX_BYTES` = 20), état interné par `BatchStateTable` (part de matériau, caméra, mode de couleur), modes `OFF`/`ORPHAN`/`RING`/`INSTANCED`. Le mode `INSTANCED` convertit les séries de quads d'une couleur en instances d'un quad unité (`InstancedUploader`, attributs pointés sur la première instance de chaque série : pas de base instance en 3.3). Lien de shader `"batch"`/`"batchInstanced"` dans `shaders.json`, vérifié par `ShaderService.batchShader` ; seuls `texture2D` et `colorShape2D` en ont un (→ `sprite2D`).
- **`Shape2DRenderer.submitToBatch`** : déjà générique (positions de `calculatePosition` transformées par le modèle sur CPU, UV, indices) pour toute sous-classe dont le shader a un lien de lot ; `RectangleRenderer` est donc regroupé. Les formes vectorielles ne le sont pas : `shape2DV.glsl` (include `utils/shape2DVertex.glsl`) lit une quinzaine d'uniforms (`shapeSize`, `shapeReference`, `color`, `strokeColor`, `strokeWidth`, `strokeAlignment`, `strokeUnit`, `antialiasing`, `shapeMargin`, `shapeTexturePosition`, `shapeTextured`, `shapeParams0/1`, `mlViewportSize`) et passe tout au fragment shader par des `out` `flat` ; une forme = un fragment shader (`2D/shape/<forme>2DF.glsl`). `PointsRenderer` (`points2DV.glsl`, un quad par point) et les formes géométriques (`stroke2DV.glsl` : `aBase`, `aExtrusion`, `aCoverage`, `aPaint`) ont leur vertex shader.
- **`Animation2DRenderer`** (`MeshRenderer`, `texture2D`) : un quad, positions et UV en VBO dynamiques.
- **`Text`** (`MeshRenderer`, `coloredText`) : un quad par caractère ; `coloredTextF.glsl` lit un `sampler2DRect` (coordonnées en texels, `HALF_TEXEL`) et la couleur en uniform `int`. Les atlas sont des `GL_TEXTURE_RECTANGLE` (`FontLoader`, repli `FallbackTextureData(…, GL_TEXTURE_RECTANGLE)`, `Texture` : `fallbackRectangleId`, `TextureParameters` : branche rectangle). La console dessine un `Text` par ligne visible.
- **`TileMapRenderer`** (`graphics/game`, `MeshRenderer`) : une catégorie de couches par renderer (`LayersTileMapRenderer` dans la démo), tableaux compacts des tuiles non vides (`tilesPosition`, `tilesTexturePosition`, `tilesIndices`), tuiles animées qui réécrivent les UV ; un appel par renderer.
- **Renderers de debug** (`graphics/debug`) : `GraphRenderer`, `NodesRenderer` (`colorShape2D`), `MatrixRenderer` (`multiColoredShape2D`, couleur par sommet) enveloppent un `MeshRenderer`. `Mesh` ne garde aucune copie CPU de ses VBO.
- **Temps** : `GameTime` (statique, `DeltaTime()`).
- Tests existants de la zone : `DrawBatch2DTest`, `BatchStateTableTest`, `RingBlocksTest`, `Batch2DOptionTest`, `Shape2DBatchGeometryTest`, `BatchGenComTest`, `ShaderServiceTest` (lien `batch`), `StrokerTest`, `ShapeStrokeTest`, `FontFile`/`DrawnTexts`/`TextWrap`, `TileMapRenderer.setTexturePosition`, et dans le kit `DrawBatch2DGLTest`, `RectangleBatchGLTest` (rendu `off` = `orphan` = `ring` = `instanced` au pixel près), `EngineShadersGLTest`.
- Scènes de démo : `Scene2D` (tuiles, slime animé, textes), `VectorShapes2D`, `BatchStress2D`, `Game3D2D`.

## Décisions
1. Les règles de `CanBatch` (pas de test de profondeur, liste de triangles, lien de lot, valeurs propres limitées à caméra, modèle, couleur) valent pour tous les renderers ; un renderer qui ne les remplit pas est dessiné seul, comme aujourd'hui.
2. Le rendu en mode `OFF` reste la référence : chaque migration se vérifie au pixel près contre lui (tests GL du kit).
3. Atlas des polices en `GL_TEXTURE_2D` : UV divisées par la taille de l'atlas au placement des caractères (le demi-texel devient `0.5 / taille`) ; `coloredTextF.glsl` lit un `sampler2D` ; `coloredText` reçoit `"batch": "sprite2D"` (et `"batchInstanced"`). La texture rectangle de repli et les branches rectangle qui ne servent plus sont supprimées (pas d'alias : aucun produit dessus).
4. Tuiles : la boîte de la vue (coins du viewport par l'inverse de projection × vue × modèle de la carte) donne la plage de colonnes et de rangées, bornée à la carte ; seules ces tuiles sont écrites, coût proportionnel aux tuiles visibles (pas à la carte). Une couche reste un seul dessin (une couche, une profondeur de tri). Le calcul de la plage est une fonction pure testable.
5. `MeshRenderer` 2D générique : `Mesh` garde une copie CPU des VBO float sur demande (`keepCpuData`, activée par les renderers 2D qui veulent le lot ; `updateVbo`/`updateSubVbo` la tiennent à jour) ; le lot lit le canal de position (taille 2), le canal de texture s'il existe, le canal de couleur par sommet (taille 4, `multiColoredShape2D` → lien vers `sprite2D`) sinon la couleur du renderer.
6. `BatchFormat` (immuable, `final`, construit par un builder) : attributs nommés (nom GLSL, nombre de composantes 1–4, type float / octets normalisés / int, par sommet ou par instance) ; taille en octets calculée ; le format de `sprite2D` est un `BatchFormat` comme les autres (pas de cas spécial). Un programme de lot vérifie au lien que chaque attribut existe avec la bonne taille (`glGetActiveAttrib`) : écart → une ligne d'erreur (`ResourceException`), le renderer est dessiné seul. Changer de format entre deux dessins est un changement d'état (série coupée).
7. Écriture d'un dessin au format étendu : `beginDraw(état, couche, profondeur)` puis des écritures typées (`put(float)`, `putColor(int)`…) dans l'ordre du format, vérifiées en debug (nombre de valeurs par sommet/instance). Aucune allocation une fois les frames stables (mêmes tableaux réutilisés que pour le format fixe).
8. Formats à attributs d'instance : appel `glDrawElementsInstanced` dans **tous** les modes non `OFF` (les modes ne changent que la façon d'envoyer les tampons : `ORPHAN` ou `RING` pour le tampon d'instances) ; attributs pointés sur la première instance de chaque série (comme `InstancedUploader`). La conversion de `INSTANCED` (quads de sprites → instances) reste propre aux sprites.
9. Formes à distance signée : vertex shader de lot `2D/shape/shape2DInstancedV.glsl` (même calcul que `shapeVertex`, mêmes `out`, les uniforms devenus attributs d'instance, le modèle passé en lignes affines 2×3), réutilise le fragment shader de chaque forme sans le changer ; une entrée de lot par forme dans `shaders.json` (`"batch": "circle2DBatch"`…). Une forme d'un projet garde son accroche `writeShapeParameters` ; pour être regroupée elle déclare aussi son entrée de lot (documenté dans `architecture.md`). `PointsRenderer` : une instance par point. `mlViewportSize` reste un uniform (commun au lot).
10. Formes géométriques : format par sommet de `stroke2D` (base, extrusion, couverture, peinture) écrit après transformation par le modèle sur CPU ; l'épaisseur, la couleur et les paramètres communs passent par sommet (pas d'uniform propre), pour regrouper des polylignes d'épaisseurs différentes. Rendu identique au pixel exigé.
11. `DebugDraw2D` (un par vue, `IView.debugDraw()`) : `line`, `polyline`, `rect`, `circle`, `cross`, `arrow`, `text`, en coordonnées du monde (caméra 2D de la vue) ou de l'écran (taille virtuelle), épaisseur en pixels d'écran par défaut ; il écrit directement dans le lot avec les formats existants (pas de renderer par dessin) ; dessiné à la fin du `display` de la vue, après une barrière, par-dessus tout. Vidé au `unload` de la scène et à la fermeture d'une `WindowView`. Interrupteur global : `EngineConfig`, `-Dmightylib.debugDraw=on|off` (défaut `on`), commande `debugdraw` (`DEV`).
12. `IDebugLifetime` : `boolean alive()` appelée une fois par frame avant le dessin (false → retiré) ; fournies par une classe utilitaire : `oneFrame()` (défaut), `frames(n)`, `seconds(s)` (`GameTime`), `whileTrue(BooleanSupplier)`, `untilRemoved()` (avec la poignée `DebugDrawHandle.remove()`). Toute méthode de dessin rend une poignée (réutilisable, sans allocation pour `oneFrame`).
13. 3D : `ModelRenderer` et les renderers 3D gardent `submitToBatch` = false ; rien ne change pour `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`.

## Étapes

### Branche A1 — `feature/batch-renderers-2d` (format fixe)
1. **Premier commit : ce plan tel quel.**
2. **`Animation2DRenderer` au lot** : `submitToBatch` (quad transformé par le modèle, UV de la frame courante, retournements) ; factoriser avec `Shape2DRenderer` ce qui peut l'être (`TransformPositions`, `CanBatch`) sans changer l'API publique. Tests : géométrie sans GL (comme `Shape2DBatchGeometryTest`) ; GL du kit : rendu `off` = modes regroupés. En jeu : `Scene2D` (slime), `stats` (dessins regroupés).
3. **`MeshRenderer` 2D générique** : `Mesh.keepCpuData`, `submitToBatch` de `MeshRenderer` (décision 5), lien `multiColoredShape2D` → `sprite2D` ; `GraphRenderer`, `NodesRenderer`, `MatrixRenderer` l'activent. Tests : copie CPU tenue par `updateVbo`/`updateSubVbo` (sans GL si possible), canaux lus ; GL du kit : rendu au pixel. En jeu : scène qui montre graphe/nœuds/matrice (à trouver dans `DemoScenes`), `stats`.
4. **Atlas des polices en textures 2D** : `FontLoader`, `FontFile`/placement des caractères dans `Text` (UV normalisées), `coloredTextF.glsl`, repli de police, suppression des branches rectangle mortes (`Texture`, `TextureParameters`, `FallbackTextureData`, `GLStateCache` si inutile). Aucun changement visible. Tests : `FontFile` et placement (UV normalisées, demi-texel) ; `EngineResourcesTest`. En jeu : `Scene2D`, console ouverte, capture ciblée d'un texte avant/après.
5. **`Text` au lot** : lien `coloredText` → `sprite2D`, `submitToBatch` (quads des caractères transformés par le modèle, couleur du texte). Tests : GL du kit, rendu au pixel d'un texte ; `DrawnTexts` inchangé. En jeu : console ouverte, `stats` (appels : une ligne de console ne coûte plus un appel).
6. **Tuiles visibles au lot** : fonction pure de plage visible (décision 4), index des tuiles par rangée de chaque couche dans les tableaux compacts, `submitToBatch` qui n'écrit que la plage ; tuiles animées incluses. Tests : plage (bords, carte déplacée, zoom, rotation de la caméra, hors champ = vide), index ; GL du kit : rendu au pixel d'une carte. En jeu : `Scene2D` (déplacement, zoom), `stats`.
7. **Démo et docs de A1** : scène `BatchRenderers2D` (texte, tuiles, animations, graphe de debug ; `stats` montre les appels), inscrite dans `DemoScenes` ; documents de la liste plus bas pour ce qui est fait ; « Bilan A1 » dans ce plan. Pousser, pas de PR.

### Branche A2 — `feature/batch-format-2d` (format extensible)
8. **`BatchFormat`** : classe, vérification au lien (décision 6), le format fixe réécrit comme le `BatchFormat` de `sprite2D`, format dans l'état (`BatchStateTable`). Aucun changement de rendu. Tests : construction, taille, refus (nom vide, doublon, taille hors 1–4), état distinct par format, vérification d'un programme sans GL (faux attributs). Peut être fusionnée seule.
9. **Écriture au format étendu et attributs d'instance** : API d'écriture (décision 7), envoi des instances dans `ORPHAN`/`RING` (décision 8), commande `batch` qui affiche le format des séries. Tests : `DrawBatch2DTest` (écritures, vérification du nombre de valeurs, séries coupées au changement de format, aucune allocation), GL du kit : un format d'essai dessiné au pixel près dans chaque mode.
10. **Formes à distance signée au lot** : `shape2DInstancedV.glsl`, entrées de lot de chaque forme, `VectorShape2DRenderer.submitToBatch` (décision 9), `PointsRenderer`, `GridRenderer`. Tests : valeurs d'instance écrites (sans GL), GL du kit : chaque forme `off` = regroupée (contours, alignements, unités, rotation, miroir, texture). En jeu : `VectorShapes2D` (survol, glisser, molette), `stats`.
11. **Formes géométriques au lot** : format de `stroke2D` (décision 10), `PolylineRenderer`, `ArrowRenderer`, `PolygonRenderer`. Tests : sommets écrits (sans GL), GL du kit au pixel (jointures, extrémités, zoom). En jeu : `VectorShapes2D`.
12. **`DebugDraw2D` et durées de vie** : décisions 11 et 12, option et commande `debugdraw` (`on`, `off`, `clear`, `list` : nombre de dessins vivants par durée de vie). Tests : file et durées de vie sans GL (une frame, N frames, secondes sur `GameTime` manuel, condition, poignée), vidage au déchargement, interrupteur ; commande (`DebugDrawComTest`). En jeu : scène de démo.
13. **Démo et docs de A2** : scène `DebugDrawing2D` (trajectoire gardée N secondes, ligne tant qu'une touche est tenue, texte en coordonnées d'écran), inscrite dans `DemoScenes` ; documents de la liste plus bas ; « Bilan A2 ». Pousser, pas de PR.

## Démo
- `BatchRenderers2D` (A1) : texte (dont un long paragraphe), carte de tuiles plus grande que l'écran avec caméra mobile, plusieurs animations, graphe/matrice de debug ; touche pour passer de `batch mode off` aux autres et comparer.
- `DebugDrawing2D` (A2) : chaque durée de vie fournie, monde et écran.
- `BatchStress2D` n'est pas modifiée ici (objectif « Test d'acceptation »).

## Documents à mettre à jour à la fin (chaque branche pour sa part)
- `architecture.md` : Lot de dessins 2D (renderers regroupés, `BatchFormat`, attributs d'instance, règles), Formes 2D (entrée de lot d'une forme de projet), texte (atlas 2D), tuiles (plage visible), `DebugDraw2D` ; retirer le piège « 2D sans batching » de « Pièges connus ».
- `debug-tools.md` : commande `debugdraw`, option `-Dmightylib.debugDraw`, format dans `batch`.
- `decisions.md` : 3D hors du lot mais format prêt pour elle ; dessins de debug par vue avec durée conditionnelle.
- `later.md` : particules (l'API d'instances s'appuie sur `BatchFormat`) ; tri par tuile (y-sort) si demandé.
- `current.md` : supprimer la ligne « Passage des renderers 2D… » ; dans la ligne « reste », retirer le format extensible (garder le shader unique à type par instance, le profilage, le réordonnancement).
- `CLAUDE.md` : nouveaux tests couverts.
- `licence.md` : rien (aucune dépendance).
- Supprimer ce plan (par `/pr-ml` de A2).

## Hors périmètre
- Shader unique de toutes les formes (type par instance), profilage JFR du coût par dessin, réordonnancement avec test de chevauchement : ligne « reste » de `current.md`.
- Culling général et compteurs (dessins éliminés, octets) : objectif « Culling 2D et statistiques du lot » de `current.md` (seules les tuiles ont ici leur plage visible).
- Seuils d'acceptation : objectif `BatchStress2D` de `current.md`.
- Regroupement 3D : préparé par le format, non fait ; `docs/later.md` (Rendu).
- Tri par tuile (y-sort des tuiles avec les sprites) : à noter dans `later.md` si utile.

## Questions restantes
- Texte de `DebugDraw2D` : écrire les caractères par une fonction de placement extraite de `Text` (sans renderer) ou garder une petite réserve de `Text` réutilisés d'une frame à l'autre ? Recommandation : fonction de placement extraite (aucun objet par texte, réutilisée par `Text`), à confirmer à l'étape 12.

## Modèles recommandés
- A1, étapes 1–6 : Opus 5.5, effort `high` (logique de lot existante à respecter, rendu au pixel, index des tuiles).
- A1, étape 7 et A2, étape 13 : Sonnet 5.5, effort `medium` (démo, docs).
- A2, étapes 8–12 : Opus 5.5, effort `xhigh` (conception durable du format, instances dans les uploaders, shaders des formes).
- `/pr-ml` (A1 et A2) : Opus 5.5, effort `high` (gros diff à relire au regard du plan).

## Déroulé
### Discussion A1 — implémentation (« Implémente la branche A1 du fichier docs/plan/batch-renderers-2d.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/batch-renderers-2d`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut pour ce qui est fait mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan A1 » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`Scene2D` : tuiles en déplacement et zoom, slime ; console ouverte : texte identique ; `BatchRenderers2D` : `batch mode off` contre les autres modes). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B1. Donner le nom de la branche.

### Discussion B1 — relecture, PR, fusion (`/pr-ml feature/batch-renderers-2d`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan A1 », discussion des erreurs, corrections, docs (**sans supprimer ce plan** : A2 reste à faire, le plan arrive dans `dev` avec la fusion), PR vers `dev`, fusion, retour sur `dev` à jour. Vérifications manuelles : celles du « Bilan A1 ».

### Discussion A2 — implémentation (« Implémente la branche A2 du fichier docs/plan/batch-renderers-2d.md »)
- Même déroulé que A1, branche `feature/batch-format-2d` depuis `dev` à jour (A1 fusionnée), étapes 8 à 13 ; le plan est déjà dans `dev` (fusionné avec A1, « Bilan A1 » compris) : pas de premier commit du plan.
- « Bilan A2 », à vérifier à la main : `VectorShapes2D` (toutes les formes, contours, zoom, rotation de la caméra) en `batch mode off` contre les autres modes ; `DebugDrawing2D` (chaque durée de vie, pause du jeu, `debugdraw off`).

### Discussion B2 — relecture, PR, fusion (`/pr-ml feature/batch-format-2d`)
- Le skill `/pr-ml` décrit tout le processus ; il supprime ce plan. Vérifications manuelles : celles du « Bilan A2 ».
