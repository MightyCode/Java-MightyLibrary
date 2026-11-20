# Plan — Regroupement des dessins 2D (cœur + sprites)

**Statut : validé.**

Premier objectif de `docs/current.md` (« Regroupement des dessins »), limité au cœur et à `RectangleRenderer` comme premier client. Le passage de `Text`, `TileMapRenderer`, `Animation2DRenderer`, des formes vectorielles et le dessin immédiat de debug (deuxième objectif) ainsi que la scène d'acceptation complète (troisième objectif) feront l'objet d'un plan suivant.

Décisions prises :
- Périmètre : le lot, son API, ses modes de remplissage, ses mesures, et `RectangleRenderer` regroupable.
- Un lot par vue (`DrawBatch2D`, `view.batch2D()`), rempli automatiquement par les renderers 2D regroupables **et** par une API explicite de dessin direct.
- Ordre : fonction de clé de tri injectable par vue (`IDrawSortKey`, un `long` par entrée, pas un comparateur d'objets) ; par défaut l'ordre des appels, **aucune clé calculée, aucun tri, coût constant par dessin** ; une fonction réimplémentée (couche + profondeur, Y…) fait trier un `long[]` à l'envoi, coût n log n accepté. Le tri est borné par les barrières, dit dans la Javadoc et visible dans la commande `batch`.
- Fusion de deux dessins voisins quand leur **état est égal** (comparaison exacte, pas un hash).
- Sommets au format fixe (position 2D transformée sur CPU, UV, couleur RGBA8) ; un shader de lot porte le tag `batch2D` ; un shader ordinaire déclare son équivalent de lot par un champ `"batch"` dans `shaders.json`.
- `texture2D` et `colorShape2D` → `"batch": "sprite2D"` ; le mode couleur utilise une texture blanche 1×1 du moteur : rectangles colorés et sprites d'une même texture (atlas) fusionnent.
- Nom : `DrawBatch2D` ; la scène de mesure s'appelle `BatchStress2D` (elle deviendra la scène d'acceptation).
- Modes : `off` (dessin immédiat d'aujourd'hui, référence du rendu), `orphan` (défaut), `ring`, `instanced` ; réglés par `EngineConfig`, `-Dmightylib.batch2D=…` et la commande DEV `batch`, à chaud.
- L'instancing est gardé comme mode : quads de sprites seuls, shader tagué `batch2DInstanced` ; un lot qui ne s'y prête pas passe par le batch CPU.
- `stats` gagne `batchFlushesPerFrame`, `batchedDrawsPerFrame`, `batchBarriersPerFrame` et `renderCpuMs`.

## Pour démarrer
- Lire : `docs/current.md` ; `docs/architecture.md`, sections « Rendu » (Renderer, `Material`, `RenderState`, cache d'état GL, couleurs et gamma, service des shaders), « Ressources GPU », « API publique » ; `docs/decisions.md` (formes 2D, philosophie) ; `docs/debug-tools.md`, section Commandes et `stats`.
- Branche de sujet depuis `dev` : `feature/draw-batch-2d`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `python tools/scenario.py …` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Dessin immédiat** : `Renderer.display()` (`graphics/renderer/Renderer.java`) = `applyRenderState()`, programme utilisé, `updateShader()` (uniforms `model`, `color`, surcharges), `draw()` → `Mesh.draw` qui compte l'appel (`EngineStats.countDrawCall`, `main/utils/EngineStats.java`). Chaque renderer fait un appel.
- **`RectangleRenderer`** (`graphics/renderer/_2D/shape/RectangleRenderer.java`, extensible) hérite de `Shape2DRenderer` (`MeshRenderer`) : 4 sommets locaux (`calculatePosition`, `calculateTexturePosition`, point de référence, retournement, rotation de texture), transformés par l'uniform `model`. Usages : 17 avec `colorShape2D`, 10 avec `texture2D` (démo + bibliothèque).
- **`Material`** (`graphics/renderer/Material.java`) : programme, valeurs communes, textures, `RenderState` ; `version()` incrémentée à chaque changement ; `sortKeyPart()` (hash, collisions possibles : ne sert qu'au tri). Un renderer créé par nom de shader a son matériau propre.
- **Cache d'état** `GLStateCache` (`graphics/internal`) : tout changement d'état y passe ; `RenderState.invalidateContextState()` après du GL brut.
- **Vues** : `Scene.displayByManager` → `display0` → `display()` (`scenes/Scene.java:384`), `WindowView.displayByContext` (`main/WindowView.java:304`), dans `Views.enter(this)` ; `clear()`, `setScissor`/`clearScissor` (Scene et WindowView), `FrameBuffer` (`graphics/renderer/_2D/FrameBuffer.java`, `setVirtualScene`/`setAndDisplayRealScene`).
- **Mode de couleur** résolu par dessin (`Renderer.resolveColorSpace`) : uniform `mlLinearColors`.
- **Shaders** : `mightylib/src/main/resources/assets/mightylib/shaders/shaders.json`, lu et fusionné par `ShaderService` (`graphics/shader`), `ShaderData` (tags `2D`/`none`, `defines`).
- **Textures** : `ColorTextureData(argb, espace)` (`resources/texture`), `TextureDataAtlas`.
- **Options** : modèle `EngineConfig.COLOR_SPACE_PROPERTY` (`main/EngineConfig.java:205`).
- **Commandes** : `utils/enginecommand/internal/*GenCom.java` (modèle : `StatsGenCom`, `GLGenCom`).
- **Tests de la zone** : `ShaderServiceTest`, `MaterialTest`, `GLStateCacheTest`/`GLStateCacheGLTest`, `UniformValuesTest`, `PublicFieldsTest`, `ExtensionFieldsTest`, `ApiStatusTest`, `ModuleDescriptorTest`, `DemoScenes`.

## Décisions
1. **Un `DrawBatch2D` par vue** (Scene, WindowView), exposé par `batch2D()` ; les tampons GL (VBO, EBO, VAO par contexte) appartiennent au contexte GL, partagés par ses vues (une vue vide le sien avant qu'une autre ne dessine).
2. **Dessin différé enregistré** : chaque dessin regroupé écrit ses sommets **au moment de l'appel** (copie : un renderer redessiné en boucle à plusieurs positions reste correct) et ajoute une entrée (identifiant d'état, couche, profondeur, indice d'appel, plage de sommets). À l'envoi, les sommets ne bougent pas : seul le tampon d'indices est écrit dans l'ordre choisi ; les entrées voisines de même état → un seul `glDrawElements`.
3. **Ordre par défaut = ordre des appels, aucun tri** (ajout et fusion en O(1), aucune allocation une fois les frames stables). **Clé de tri injectable** `IDrawSortKey` (interface, une méthode : `long key(int layer, float depth, int stateSortPart)`), posée par `view.setDrawSortKey(...)` (`null` = ordre des appels). Quand elle est posée, chaque entrée reçoit sa clé ; à l'envoi, le moteur trie un `long[]` de primitives (sans objet ni allocation) dont les bits bas portent l'indice d'appel dans le segment : tri **stable et déterministe** même si `Arrays.sort(long[])` ne l'est pas, à condition que la fonction laisse ces bits libres (nombre de bits réservés fixé par le lot, documenté ; la fonction renvoie la partie haute). Fonctions fournies : `DrawSortKeys.layerThenDepth()` et `layerThenState()` (regroupe par état à l'intérieur d'une couche, plus de fusions au prix de l'ordre entre dessins d'une même couche).
4. **Barrières** : un dessin non regroupable (le `Renderer.display()` de base vide le lot avant de dessiner), `clear()`, `setScissor`/`clearScissor`, liaison d'un `FrameBuffer`, fin de `display()` de la vue, `batch2D().flush()` explicite (à appeler avant du GL brut, comme `invalidateContextState`). **Le tri ne traverse jamais une barrière** : tant que `Text`, tuiles, animations et formes ne sont pas regroupés (plan suivant), une couche ne fait pas passer un sprite au-dessus d'eux. Dit dans la Javadoc d'`IDrawSortKey` et de `setLayer` ; la commande `batch` compte les barrières tombées dans une frame triée (`barriersWhileSorted`, avec la classe du renderer) pour que le développeur voie quand son tri est coupé.
5. **État d'un lot** = programme (et son `programStamp`, pour le rechargement et le repli), textures par unité (celle réellement liée, placeholder compris), blend, profondeur, culling, mode de couleur résolu, caméra (projection × vue), valeurs communes du matériau. **Interné** dans une table (contenu → identifiant entier) ; le matériau garde son identifiant tant que `version()` et `programStamp` ne changent pas : comparer deux dessins = comparer deux entiers. Table remise à zéro quand elle dépasse un seuil (pas de fuite sur de longues parties).
6. **Format de sommet fixe** : position 2D (déjà transformée par `model`), UV, couleur RGBA8 normalisée ; projection × vue en uniform par lot. Un renderer est regroupable si son shader a un équivalent de lot et si ses surcharges se limitent à `model` et `color` ; sinon il dessine seul (barrière).
7. **Lien de shader** : champ `"batch": "<nom>"` d'une entrée de `shaders.json` ; la cible doit porter le tag `batch2D` (sinon une ligne d'erreur, entrée traitée comme non regroupable). `sprite2D` (moteur) = texture × couleur du sommet, mode de couleur par `mlLinearColors`. `texture2D` et `colorShape2D` y renvoient. Un jeu rend son shader regroupable de la même façon.
8. **Texture blanche 1×1** du moteur (`EngineResources.WHITE_TEXTURE`, `ColorTextureData`, chargée par le batch `mightylib`) : le mode couleur d'un renderer regroupé la lie en unité 0.
9. **Modes** (`EBatch2DMode` : `OFF`, `ORPHAN`, `RING`, `INSTANCED`) : `EngineConfig.setBatch2DMode`, `-Dmightylib.batch2D=off|orphan|ring|instanced` (défaut `orphan`), commande `batch mode <mode>` à chaud (le lot est vidé avant la bascule). Remplissage par une interface interne (`IBatchUploader`) : orphelinage = `glBufferData(null)` + `glBufferSubData` ; anneau = `glMapBufferRange` `UNSYNCHRONIZED` sur un tampon découpé en régions, `glFenceSync`/`glClientWaitSync` par région. `INSTANCED` : quads de sprites seuls (affine 2×3, rectangle UV, couleur par instance, `glVertexAttribDivisor`, `glDrawElementsInstanced`), shader `sprite2DInstanced` tagué `batch2DInstanced` (lien par `"batchInstanced"`) ; un lot sans ce shader ou fait d'autre chose que des quads passe par le chemin CPU (`ORPHAN`).
10. **API explicite** sur `DrawBatch2D` : dessin d'une région de texture (position, taille, rotation, point de référence, couleur, retournement), d'un rectangle coloré, et d'un quad quelconque (4 sommets + UV + couleur) avec un `Material` ; variantes avec couche et profondeur. Même chemin que les renderers.
11. **Tri** : `Renderer.setLayer(int)`, `setSortDepth(float)` (0 par défaut), ignorés par l'ordre par défaut ; `IDrawSortKey` reçoit couche, profondeur et `sortKeyPart` de l'état (l'indice d'appel est ajouté par le lot).
12. **Mesures** dans `stats` : `batchFlushesPerFrame` (appels issus du lot), `batchedDrawsPerFrame` (dessins regroupés), `batchBarriersPerFrame`, `renderCpuMs` (temps CPU de `Engine.displayWindows`, sans le swap). `drawCallsPerFrame` reste le total.
13. **3D hors du lot** : `MeshRenderer` 3D, `ModelRenderer` et les autres 2D non migrés passent par la barrière ; rien ne change pour eux.
14. **Package** `graphics/renderer/_2D/batch` (`EXPERIMENTAL`, exporté), détails GL dans `graphics/renderer/_2D/batch/internal` (`INTERNAL`).

## Étapes (un commit chacune)
1. **Plan** : ce fichier tel quel.
2. **`renderCpuMs` et scène de mesure** — `EngineStats`, `MainLoop` (mesure autour de `engine.displayWindows()`), scène démo `BatchStress2D` (inscrite dans `DemoScenes`) : N `RectangleRenderer` (mélange `texture2D` d'un atlas et `colorShape2D`, rotations, tailles), N réglable au clavier (×2 / ÷2), affichage de N et des stats. Donne les chiffres de référence du mode actuel (notés dans le Bilan). Fusionnable seule.
3. **Lien de shader et texture blanche** — `ShaderService`/`ShaderData` : champs `batch`, `batchInstanced`, tags `batch2D`/`batch2DInstanced` (validation : cible existante et tagée, sinon ligne d'erreur) ; shaders `sprite2D` et `sprite2DInstanced` (fichiers dans `shaders/2D/batch/`, includes de `utils/`), `shaders.json` ; `EngineResources.WHITE_TEXTURE` dans le batch `mightylib`. Tests : `ShaderServiceTest` (lien lu, cible absente, cible sans tag, fusion entre sources), `EngineResourcesTest` (texture blanche).
4. **Cœur sans GL** — `DrawBatch2D` (entrées, sommets dans un tableau réutilisé, fusion, barrières, construction des indices), table d'états internés (`BatchStateTable`), `IDrawSortKey` + `DrawSortKeys.layerThenDepth()`/`layerThenState()` (profondeur `float` convertie en bits ordonnés, négatifs compris), tri du `long[]` des clés, `IBatchUploader` (implémentation de test). Tests `DrawBatch2DTest` : fusion des voisins égaux, coupure sur état différent, barrière, aucune clé calculée ni tri par défaut (ordre exact), tri par clé stable à clés égales et limité à un segment, profondeurs négatives et égales, compteur des barrières en frame triée, indices construits sans déplacer les sommets, état ré-interné après `version()`/`programStamp` changé, remise à zéro de la table, aucune allocation après réchauffement (taille des tableaux stable).
5. **GL et vues** — tampons par contexte (`GenDeleteResources`), uploaders `ORPHAN` et `RING` (fences), `batch2D()` sur `Scene`/`WindowView`, vidage aux barrières (fin de `display`, `clear`, scissor, `FrameBuffer`), `Renderer.display()` de base vide le lot, compteurs de `stats`, `EBatch2DMode` + `EngineConfig`/option. Libération des tampons à la destruction du contexte. Test GL du kit (tag `gl`) : N quads de même état = 1 appel, deux états alternés = N appels, `ring` sur plusieurs frames sans corruption (lecture de pixels). Vérif. : `scenario.py "scene BatchStress2D" wait-loaded "?stats"` puis `leakcheck BatchStress2D`.
6. **`RectangleRenderer` regroupable et API explicite** — accroche de `Renderer` (ex. `protected boolean submitToBatch(DrawBatch2D batch)`, Javadoc : appelée par `display()` quand le mode n'est pas `OFF`, renvoie `false` pour dessiner seul) ; `Shape2DRenderer`/`RectangleRenderer` : sommets transformés par `model` sur CPU, couleur par sommet, texture blanche en mode couleur ; `setLayer`/`setSortDepth` ; méthodes de dessin direct de `DrawBatch2D`. Tests sans GL : sommets transformés (référence, rotation, retournement, rotation de texture) égaux à ce que fait le shader actuel ; renderer avec surcharge en plus → non regroupable. **Rendu identique** : captures ciblées de `Scene2D` et `BatchStress2D` en `off` puis `orphan` (et `ring`), comparées pixel à pixel (script local dans `tools/scripts/`). `ExtensibleClasses`, `PublicFieldsTest`, `ApiStatusTest`, `ModuleDescriptorTest` à jour.
7. **Commande `batch`** (DEV, `mightylib:batch`, syntaxe déclarée) — `batch` : mode, compteurs de la dernière frame par vue (lots, entrées, barrières et classe du renderer qui les a causées, `barriersWhileSorted`, clé de tri posée ou non, états internés) ; `batch mode <off|orphan|ring|instanced>`. Test `BatchGenComTest` (syntaxe, complétion, bascule sans GL si possible).
8. **Mode `INSTANCED`** — uploader d'instances, chemin quads seuls, repli CPU par lot. Test GL : N sprites = 1 appel instancié, lot mixte → repli. Mesures dans `BatchStress2D` pour les 4 modes à plusieurs N (1 000, 10 000, 50 000) : chiffres dans le Bilan et dans `later.md` (« Effets 2D », particules).
9. **Docs** (voir plus bas).

## Démo
- `BatchStress2D` (nouvelle, dans `DemoScenes` avec sa description) : N sprites et rectangles colorés, N au clavier, mode du lot au clavier (cycle), affichage des stats ; base de la scène d'acceptation du plan suivant (texte, formes, tuiles, animations s'y ajouteront).

## Documents à mettre à jour à la fin
- `architecture.md` : section Rendu (lot 2D, état interné, barrières, ordre, modes, lien de shader, texture blanche, `setLayer`/`setSortDepth`), au présent.
- `debug-tools.md` : commande `batch`, nouveaux champs de `stats`, option `-Dmightylib.batch2D`, comparaison de captures `off`/`orphan`.
- `decisions.md` : dessin 2D différé dans un lot par vue, ordre des appels par défaut et clé de tri `long` injectable (pas de comparateur : coût, primitives, stabilité par l'indice d'appel ; tri borné par les barrières), barrière pour le non regroupable, format fixe + lien de shader, quatre modes gardés.
- `current.md` : réduire le premier objectif à son reste (formes vectorielles en attributs par instance) ; le deuxième objectif reste (Text, tuiles, animations, formes, debug immédiat) ; le troisième renommé `BatchStress2D` avec les seuils à fixer depuis les mesures.
- `later.md` : résultats de l'instancing pour les particules ; réordonnancement avec test de chevauchement (comme Godot) comme suite possible.
- `CLAUDE.md` : tests couverts (`DrawBatch2DTest`, lien de shader, `BatchGenComTest`, test GL du lot).
- Supprimer ce plan.

## Hors périmètre
- `Text`, console, `TileMapRenderer`, `Animation2DRenderer`, formes vectorielles, `Stroker`, dessin immédiat de debug : deuxième objectif de `current.md`.
- Format de sommet extensible (paramètres des formes par distance par sommet/instance) : premier objectif de `current.md` (reste).
- Réordonnancement automatique avec test de chevauchement : `later.md` (à ajouter à la fin).
- 3D : jamais regroupée dans ce point (`current.md`).
- Seuils chiffrés de la scène d'acceptation : troisième objectif.

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1–3 : Claude Opus 5.5, effort `medium` (mesure, shaders, lecture de JSON : peu de conception).
- Étapes 4–6 : Claude Opus 5.5, effort `xhigh` (modèle durable du lot, internement des états, anneau et fences, accroche d'une classe extensible).
- Étapes 7–8 : Claude Opus 5.5, effort `high` (commande au modèle connu ; instancing délicat mais borné).
- Étape 9 : Claude Sonnet 5.5, effort `medium` (docs).
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (gros diff GL à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/regroupement-des-dessins.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/draw-batch-2d`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`BatchStress2D` dans les quatre modes : rendu, N au clavier, stats ; `Scene2D` et les scènes de formes en `off` puis `orphan` : rendu identique ; scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` inchangées ; console ouverte par-dessus une scène regroupée). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/draw-batch-2d`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
