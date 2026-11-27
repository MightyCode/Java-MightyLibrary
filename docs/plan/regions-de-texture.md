# Régions de texture limitées dans le shader

Statut : **validé**

Décisions prises :
- Les frames d'une feuille (et les tuiles d'un tileset) peuvent se toucher sans marge : le shader limite la lecture de la texture à la **région** du dessin (comme `region_filter_clip_enabled` de Godot). Ni retrait d'un demi-texel, ni extrusion.
- Régions posées automatiquement par `Animation2DRenderer` et `TileMapRenderer`, explicitement par `Shape2DRenderer.setTextureRegion` et par l'API directe `DrawBatch2D.drawRegion`. `setTexturePosition` reste sans limite (une texture répétée au-delà de 0..1 reste possible).
- La région est une valeur de **chaque sommet du format `SPRITE`**, pour tous les dessins (un seul format, séries inchangées). « Pas de limite » = région de largeur nulle ou négative.
- Région stockée en **4 floats** (+16 octets par sommet, 20 → 36). Si `tools/stress.py` dépasse ses seuils : s'arrêter et en parler (repli prévu : entiers 16 bits normalisés).
- Pas d'interrupteur de debug, pas de nouvelle scène : la preuve est un test GL qui place les bords sur des centres de pixels, plus les images de référence (rangée du bord entière) et une vérification à la main dans `BatchRenderers2D`.

## Pour démarrer
- Lire `docs/architecture.md`, « Lot de dessins 2D » et « Formes 2D » ; `docs/later.md`, ligne « Débordement d'un demi-texel hors des régions d'atlas » ; `docs/debug-tools.md`, « Références d'images ».
- Branche : `feature/texture-regions` depuis `dev`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Défaut** : `Animation.FrameTexturePosition` (`mightylib/.../resources/animation/Animation.java`, `FRAME_INSET = 0.5f`) met les UV d'un demi-texel à l'intérieur de chaque côté de la frame. Ça cache un filet de la frame voisine, mais la première et la dernière rangée (et colonne) de la frame sont dessinées deux fois plus fines : c'est très visible à fort zoom (bas des slimes de `Scene2D`). Test : `FrameTexturePositionTest`.
- **Cause probable du filet** (jamais reproduit en jeu) : en `GL_NEAREST` (`"qualityType": "pixelart"`, `TextureParameters.pixelArtParameters`, en `GL_REPEAT`), un fragment dont le centre tombe pile sur le bord haut ou gauche du quad est dessiné (règle de remplissage), et son UV interpolée vaut la limite de la frame, arrondie juste en dessous : on lit le texel voisin. Ça n'arrive que quand un bord tombe exactement sur un centre de pixel (zoom, position), d'où un défaut intermittent, toujours au-dessus des slimes. Les UV exactes de `TileMapRenderer.getTileTextureRect` peuvent avoir le même défaut.
- **Chemins de rendu** :
  - Lot (`graphics/renderer/_2D/batch`) : `BatchFormats.SPRITE` (`aPos`, `inTexCoords`, `inColor`) lu par `2D/batch/sprite2DV.glsl` + `sprite2DF.glsl` ; `DrawBatch2D.vertex(x, y, u, v, rgba)`, `quad`, `triangles` (via `BatchGeometry2D.WriteTriangles`), `drawRegion`/`drawRectangle`/`drawQuad` ; `BatchFormat.hasSpriteValues` (formats de projet aux mêmes valeurs).
  - Mode `INSTANCED` : `DrawBatch2D.buildInstances`/`writeInstance` transforment les quads `SPRITE` en instances `SPRITE_INSTANCED` (`aRow0`, `aRow1`, `aTexOrigin`, `aTexAxes`, `aColor`), lues par `sprite2DInstancedV.glsl`.
  - Mode `OFF` : chaque renderer dessine son `Mesh` avec `texture2D` (`2D/essential/texture2DV.glsl` + `texture2DF.glsl`, positions canal 0, UV canal 1). `TileMapRenderer` met toutes ses tuiles dans un seul mesh : la région doit donc être **par sommet**, pas un uniform.
  - Renderers : `Animation2DRenderer` (`updateInfoForFrame`, UV avec retournements l. ~223, `submitToBatch`), `TileMapRenderer` (`submitToBatch` l. ~309 : `batch.vertex` par coin, `getTileTextureRect`, VBO `textureIndex`), `Shape2DRenderer` (`setTexturePosition`, `submitToBatch`, VBO `textureIndex`).
- Shaders liés au lot `sprite2D` : `texture2D`, `colorShape2D`, `multiColoredShape2D`, `coloredText` (`shaders.json`). Les shaders de projet liés à `sprite2D` ne lisent pas de région : pas de limite.
- **Tests de la zone** : `DrawBatch2DTest`, `BatchFormatTest`, `FrameTexturePositionTest`, `EngineShadersGLTest`, `DrawBatch2DGLTest`/`RectangleBatchGLTest` (rendu `off` = `orphan` = `ring` = `instanced` au pixel près), `Animation2DBatchGLTest`, `TileMapBatchGLTest`, `BatchStress2DGLTest`, `ReferenceImagesGLTest` (`ReferenceSprites2D` et `TileMapReferences2D` dessinent des animations et des tuiles : **leurs références vont changer**).

## Décisions
- **Convention** : une région est un `Vector4f` (gauche, droite, haut, bas) en UV, comme `texturePosition`, toujours dans l'ordre min/max même quand le dessin est retourné (les retournements ne touchent que les UV). « Pas de limite » : `droite <= gauche`. La valeur par défaut d'un attribut non fourni (0, 0, 0, 1) veut donc dire « pas de limite » : un mesh de `texture2D` sans VBO de région n'est pas limité.
- **Shader** : nouvel include `utils/textureRegion.glsl`, avec une fonction `regionTexCoords(sampler2D, vec2 uv, vec4 region)` : si la région est active, `clamp(uv, min + demiTexel, max - demiTexel)` (demi-texel = `0.5 / textureSize(t, 0)`), et une région plus fine qu'un texel garde son centre (jamais un `clamp` avec min > max). La région passe du vertex au fragment shader en `flat`. Exact en `pixelart` (rangées du bord entières) et en `linear` sans mipmaps (le bord est prolongé, comme `CLAMP_TO_EDGE`).
- **Formats** : `SPRITE` = `aPos`, `inTexCoords`, `inTexRegion` (4 floats), `inColor` ; `SPRITE_INSTANCED` reçoit `aTexRegion` par instance. Un quad dont les quatre sommets n'ont pas la même région n'est pas instancié (il est dessiné par ses sommets). `hasSpriteValues` inclut la région. Les formats de projet aux valeurs de `SPRITE` changent aussi (changement cassant accepté).
- **API du lot** : région **collante** pour le dessin en cours, `DrawBatch2D.textureRegion(gauche, droite, haut, bas)`, remise à « pas de limite » à chaque `beginDraw`, écrite par `vertex(...)` (signature inchangée) ; `quad`, `triangles` et `BatchGeometry2D.WriteTriangles` reçoivent une région (surcharge ou paramètre, au choix de l'implémentation, sans allocation) ; `drawRegion` limite à sa région.
- **Mode `OFF`** : `texture2D` lit la région par sommet (attribut `inTexRegion`, location 2) ; `Animation2DRenderer`, `TileMapRenderer` et `Shape2DRenderer` ajoutent un VBO de régions (4 floats par sommet). Les autres utilisateurs de `texture2D` n'en ont pas : pas de limite.
- **`Animation.FrameTexturePosition`** rend les bords exacts de la frame (le retrait disparaît) ; la région du dessin = ces mêmes bords.
- **`Shape2DRenderer.setTextureRegion(Vector4fc)`** : pose la position de texture **et** la limite ; `setTexturePosition` retire la limite.
- Mipmaps (`"realistic"`) : la limite n'empêche pas les niveaux bas de mélanger les régions voisines ; seule une extrusion le peut (voir Hors périmètre).

## Étapes
1. **Plan** : commiter ce fichier tel quel.
2. **Région dans le lot et les shaders** (peut être fusionnée seule : aucun rendu ne change, rien ne pose encore de région) :
   - `utils/textureRegion.glsl` ; `sprite2DV/F`, `sprite2DInstancedV`, `texture2DV/F` l'utilisent ; `BatchFormats.SPRITE` et `SPRITE_INSTANCED` ; `DrawBatch2D` (région collante, `vertex`, `quad`, `triangles`, `writeInstance` : même région sur les quatre coins, `drawRegion`) ; `BatchGeometry2D.WriteTriangles` ; `hasSpriteValues`.
   - Tests sans GL : `DrawBatch2DTest` (région écrite, remise à zéro par `beginDraw`, quad aux régions différentes non instancié, octets, aucune allocation) ; `BatchFormatTest` (tailles 36 octets par sommet, format instancié).
   - Tests GL : `EngineShadersGLTest`, `DrawBatch2DGLTest` et `RectangleBatchGLTest` passent sans changement de référence.
   - Mesurer `python tools/bench.py --isolated --repeat 3` et `python tools/stress.py` avant et après (chiffres dans le « Bilan ») ; un dépassement de seuil arrête l'implémentation (voir les décisions).
3. **Renderers** :
   - `Animation.FrameTexturePosition` exact (`FRAME_INSET` supprimé), `FrameTexturePositionTest` adapté (bords exacts, frame d'un texel) ; `Animation2DRenderer` pose sa région (lot et VBO du mode `OFF`, retournements compris).
   - `TileMapRenderer` : une région par tuile (`getTileTextureRect`), dans le lot et dans un VBO de régions.
   - `Shape2DRenderer.setTextureRegion` (VBO de régions, lot), `setTexturePosition` retire la limite.
   - **Test de reproduction GL** (dans le kit, à côté de `Animation2DBatchGLTest`) : une feuille de taille non puissance de deux (par exemple 3 × 5 texels par frame), frames voisines de couleurs très contrastées, en `pixelart` et en `linear` ; un quad agrandi (×8) dont les bords haut et gauche tombent sur des centres de pixels (position en `k + 0.5` pixel), dessiné par `Animation2DRenderer`, `TileMapRenderer` et `Shape2DRenderer.setTextureRegion`, dans chaque mode (`off`, `orphan`, `ring`, `instanced`) : aucune couleur d'une frame voisine, chaque rangée et colonne du bord fait toute la largeur d'un texel (8 pixels, ±1 en `linear`), et tous les modes donnent la même image.
   - Vérification en jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "camera …" "screenshot --rect …"` sur le bas d'un slime à fort zoom (une capture, cadrée).
4. **Références d'images** : `ReferenceSprites2D` et `TileMapReferences2D` changent (rangées du bord entières). Régénérer les trois séries dans la branche (`python tools/references.py --update` pour la machine, puis `--from-ci` pour `linux-mesa-llvmpipe-intel` et `-amd`, une par exécution de la CI), relire les images avant le commit : le bas des slimes et le bord des tuiles ont leur rangée entière, aucun filet.
5. **Docs** (voir plus bas), puis le « Bilan ».

## Démo
Pas de nouvelle scène. Vérification à la main par l'utilisateur dans `BatchRenderers2D` (et `Scene2D`) : zoom élevé à la molette, déplacements lents (Z Q S D), plus aucun filet au-dessus des slimes ni entre les tuiles, et la rangée du bas des slimes entière.

## Documents à mettre à jour à la fin
- `architecture.md` : Lot de dessins 2D (format `SPRITE` avec sa région, `textureRegion` collante, `drawRegion`, instances, mode `OFF` par `texture2D`) ; `Animation2DRenderer` (le passage sur le demi-texel est remplacé par la région) ; Formes 2D (`setTextureRegion` contre `setTexturePosition`).
- `decisions.md` : régions limitées dans le shader plutôt qu'un retrait d'un demi-texel ou une extrusion, et pourquoi (frames qui se touchent, rangées du bord entières, aucun changement des images) ; limite : mipmaps.
- `later.md` : remplacer la ligne « Débordement d'un demi-texel… » par ce qui reste : extrusion des régions (au chargement ou par un futur empaqueteur d'atlas) pour les textures à mipmaps ; régions pour le texte (`FontFile.textureBounds`, demi-texel) si un atlas de police sans marge apparaît.
- `CLAUDE.md` : tests couverts (régions dans `DrawBatch2DTest`, `BatchFormatTest`, `FrameTexturePositionTest`, nouveau test GL de reproduction).
- `current.md` : rien (le gros point en cours reste celui de la couverture).
- Supprimer ce plan (dans `/pr-ml`).

## Hors périmètre
- Extrusion ou marge des régions pour les textures à mipmaps (`"realistic"`) : `later.md`.
- Texte : `Text` et `FontFile.textureBounds` gardent leur demi-texel (atlas `linear` avec marge entre glyphes) : `later.md`.
- 3D (`texture3D`) : inchangée.

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 2 et 3 : Claude Opus 5.5, effort `high` (format du lot, instances, rendu identique au pixel dans chaque mode, test GL délicat).
- Discussion A, étapes 4 et 5 : Claude Sonnet 5.5, effort `medium` (références à relire, docs).
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (diff du lot et des shaders à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/regions-de-texture.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/texture-regions`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`BatchRenderers2D` et `Scene2D` à fort zoom : aucun filet au-dessus des slimes ni entre les tuiles, rangée du bas des slimes entière). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/texture-regions`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan » : `BatchRenderers2D` et `Scene2D` à fort zoom.

## Bilan
### Fait
- Étape 2 : `utils/textureRegion.glsl` (`regionTexCoords`, région passée en `flat`), lue par `sprite2D`, `sprite2DInstanced` et `texture2D` ; `SPRITE` = `aPos`, `inTexCoords`, `inTexRegion`, `inColor` (36 octets), `SPRITE_INSTANCED` reçoit `aTexRegion` (68 octets) ; `DrawBatch2D.textureRegion` collante (remise à zéro par `beginDraw`), surcharges de `quad`, `triangles` et `BatchGeometry2D.WriteTriangles` avec région ; une instance par quad d'une seule région. Tests : `DrawBatch2DTest`, `BatchFormatTest`.
- Étape 3 : `Animation.FrameTexturePosition` exact (`FRAME_INSET` supprimé) ; régions dans `Animation2DRenderer`, `TileMapRenderer`, `Shape2DRenderer.setTextureRegion` (et `getTextureRegion`), `drawRegion` ; `Mesh.COMMON_TEXTURE_REGION_CHANNEL` et `Mesh.hasChannel`. Test de reproduction `TextureRegionsGLTest` (kit) ; `FrameTexturePositionTest`, `AnimationTest`, `RectangleBatchGLTest` adaptés.
- Étape 4 : séries `windows-nvidia`, `linux-mesa-llvmpipe-intel` et `-amd` de `ReferenceSprites2D` (trois vues) régénérées et relues (la série AMD à la quatrième exécution de la CI : les trois premières sur un runner Intel ; le jeton ne peut pas relancer une exécution, un commit vide en a relancé une).
- Étape 5 : `architecture.md`, `decisions.md`, `later.md`, `CLAUDE.md`, `licence.md` (fichiers du test).

### Écarts au plan
- La limite de `drawRegion` est passée de l'étape 2 à l'étape 3 : son test GL le compare à un `RectangleRenderer`, qui n'a la même limite qu'avec `setTextureRegion` ; l'étape 2 ne change ainsi aucune image.
- `Shape2DRenderer` ajoute son VBO de régions à la première `setTextureRegion` (pas de VBO pour les formes qui n'en ont pas), et seulement si le canal 2 est libre : `PointsRenderer` y a ses coins, les formes vectorielles ne lisent pas de région.
- `TileMapReferences2D` n'existe pas : les tuiles des références sont dans `ReferenceSprites2D`, seule série qui change.
- `TextureRegionsGLTest` a sa configuration (le kit n'enregistre pas les loaders des cartes) et ses fichiers (`mightylib-test/src/test/resources/texture-regions`) ; largeur de chaque rangée et colonne de texels à ±1 pixel aussi en `pixelart` : les bords étant sur des centres de pixels, les limites entre texels y tombent aussi et s'arrondissent d'un côté ou de l'autre. Restent exacts : aucun texel voisin, 24 × 40 pixels dessinés.

### Vérifié
- `mvn -o -q test` : tout passe (les références régénérées).
- `TextureRegionsGLTest` échoue sans la limite du shader (pixel bleu de la frame voisine au bord gauche, en `pixelart` et en `linear`) et passe avec.
- Références : bas du grand slime 6 pixels (une rangée entière) au lieu de 3.
- En jeu : `scenario.py "scene Scene2D"`, capture du slime : contour régulier, aucun filet.
- Coût CPU (`tools/stress.py`, machine plus lente que lors de la fixation des seuils : `dev` dépasse lui aussi plusieurs seuils ; mesures alternées branche / `dev` dans les mêmes conditions) : étape 2, `mix-16k` 3,91 / 4,27 ms contre 4,00 / 3,98, rectangles et formes identiques ; étape 3, `mix-16k` 4,09 / 4,15 contre 4,01 / 4,06, `mix-16k-sorted` 3,94 contre 3,68 (+7 %), cas à 10 % égaux. Pas de dépassement dû au changement : le repli en entiers 16 bits n'est pas nécessaire. `bench.py` : écarts dans le bruit (rectangle visible 81,5 → 84 ns, animation 77,6 → 81 ns).

### À vérifier à la main
- `BatchRenderers2D` et `Scene2D` à fort zoom (molette), déplacements lents (Z Q S D) : aucun filet au-dessus des slimes ni entre les tuiles, la rangée du bas des slimes entière.
