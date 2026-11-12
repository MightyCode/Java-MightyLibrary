# Rendu extensible : `Mesh`, `Renderer`, `MeshRenderer`

**Statut : validé.**

Décisions prises :
- `Shape` devient `Mesh` = géométrie seule (VAO par contexte, VBO, EBO, mode de dessin) ; le programme (`Shader`) appartient au renderer.
- Un **état de dessin** du renderer (test de profondeur, écriture de profondeur, culling des faces) remplace `Shape.in2D` ; valeurs par défaut lues dans le tag du shader.
- Hiérarchie séparée : `Renderer` = base sans `Mesh` (abstraite), `MeshRenderer extends Renderer` = un `Mesh`. `ModelRenderer` hérite de `Renderer`, les autres renderers de `MeshRenderer`.
- Textures (`setMainTextureChannel`, `addTextureChannel`, coupure du canal UV) dans `MeshRenderer`.
- Le renderer possède le `Mesh` qu'on lui donne (son `unload` le libère) ; pas de partage de `Mesh` dans ce point.
- Canal (location) d'un attribut explicite à l'ajout d'un VBO, en plus de l'ajout au rang suivant.
- Pas encore de `Material` partageable : shader, valeurs d'uniform et état de dessin regroupés dans `Renderer` de façon à pouvoir en être extraits ; `Material` noté dans `later.md`.
- `Mesh`, `Renderer`, `MeshRenderer`, `Shape2DRenderer` : champs tous privés, quelques méthodes choisies et documentées ; un test de garde vérifie l'absence de champ `protected`.
- Matrices 3D croisées remises droites.
- Démo `CustomRenderer2D` écrite en premier sur l'API actuelle, simplifiée à chaque étape.

## Pour démarrer
- Lire : `docs/decisions.md` (philosophie, « une valeur d'uniform appartient au `Renderer` »), `docs/architecture.md` (lignes sur `Shape`, `Renderer`, `ModelRenderer`, placeholders du shader, `FallbackShader`), `docs/bonnes-pratiques.md` point 7, `docs/current.md` (objectif « Rendu extensible »).
- Branche de sujet depuis `dev` : `renderer-extensible`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre). Scènes à revoir à chaque étape qui touche le rendu : `Scene2D`, `CustomRenderer2D`, et les quatre scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` (la 3D ne doit pas casser), `MultiWindowScene` (VAO par contexte), plus `leakcheck` sur `CustomRenderer2D` et `Scene3D`. Commandes utiles : `gl summary` (programmes et références), `stats drawCallsPerFrame` (`docs/debug-tools.md`).

## Contexte : ce qui existe
- `src/io/github/mightycode/mightylib/graphics/renderer/Shape.java` (503 lignes) :
  - géométrie : VAO par `GLContext` (`VaoEntry`, `layoutVersion`, reconstruit au `bind` par `rebuild`, qui prend le rang du VBO comme location), listes `vbos`, `vbosStorage`, `vbosSize`, `vbosEnable`, `vbosVertexSize`, `vbosInteger`, `vboCount`, `ebo`, `eboStorage`, `useEbo`, `indicesSize`, `verticesToDraw`, `drawMode` : tous `protected` sauf l'état VAO. Le canal d'un VBO = son rang d'ajout (`addVboFloat`/`addVboInt` appellent `glVertexAttribPointer(vboCount, …)`) ; `enableVbo(pos)`/`disableVbo(pos)` prennent ce rang, utilisé comme canal par `Shape.COMMON_TEXTURE_POSITION_CHANNEL` ;
  - programme : `new Shape(shaderName, useEbo)` prend le `Shader` par `glResources.acquire(resources.getResourceOrFallback(ShaderData.class, nom), true)`, `getShader()` (programme lié ou `glResources.fallbackShader()`), `applyShader()`, `getShaderId()`, libéré une fois dans `unload()` (`shaderReleased`) ;
  - 2D/3D : `in2D` (= `ShaderData.isDimension2DShader()`, vrai pour les tags `2D` et `none`, lu par `ShaderLoader`), `applyDimension()` (active/désactive `GL_DEPTH_TEST`), `is2DPurpose()`, `setDimensionTo2D` ;
  - `display()` = `applyDimension` + `applyShader` + `bind` + `draw`.
  - **Aucune sous-classe de `Shape`** ; aucun champ `protected` de `Shape` n'est lu hors de la classe. `Shape` apparaît dans 15 fichiers ; aucune classe ne s'appelle `Mesh` (`ModelMesh` = données CPU d'un mesh de modèle, garde son nom).
- `src/io/github/mightycode/mightylib/graphics/renderer/Renderer.java` (375 lignes) : `UUID implements IDisplayable` ; crée sa `Shape` dans son constructeur (`Renderer(String shaderName, boolean useEbo)`) ; transform (`position`, `scale`, `rotation`, `angle`, `model`), `shaderValues`/`optionalValues` (valeurs renvoyées à chaque dessin, optionnelles = seulement si le programme a l'uniform), textures (`mainTexture`, `textures[30]`, active le canal UV), `setColorMode`, `setReferenceCamera`, `setShape` (jamais appelé, ne libère pas l'ancienne), `sentToShader` (faute de frappe). Champs `protected` lus par les sous-classes : `shaderValues`/`optionalValues` (`ModelRenderer.display`), `mainTexture` (`Shape2DRenderer`), `referenceCamera` (`Text`), `glResources`/`resources` (`TileMapRenderer`, `ModelRenderer`, `Animation2DRenderer`, `HeightMapRenderer`, `TerrainRenderer`, `Text`), `shape` (presque tous).
- **Matrices croisées** : en 3D, le constructeur fait `putOptionalValue(GENERIC_PROJECTION_FIELD_NAME, camera3D().getView())` et `putOptionalValue(GENERIC_VIEW_FIELD_NAME, camera3D().getProjection())`. L'envoi utilise le nom de la `ShaderValue`, donc le bon uniform reçoit la bonne matrice ; mais la clé est fausse, et `copyShaderValuesTo` réindexe par nom de valeur.
- Sous-classes de `Renderer` : `Shape2DRenderer` (abstraite ; `RectangleRenderer` ← `EllipseRenderer`, `VirtualSceneRenderer` ; `HexagonRenderer`, `PolygonRenderer`), `Animation2DRenderer`, `TileMapRenderer`, `Text`, `CubeRenderer`, `HeightMapRenderer`, `TerrainRenderer`, `ModelRenderer`. Seuls `Text` et `Shape2DRenderer` redéfinissent `display()` (et finissent par `super.display()`) ; aucune ne touche au shader ni à la profondeur. `Shape2DRenderer` a `protected final int positionIndex, textureIndex` et `protected Vector4f texturePosition` (non lus par ses sous-classes) et trois méthodes abstraites `getIndices`, `calculatePosition`, `calculateTexturePosition`.
- `ModelRenderer` : `super(shaderName, true)` pour une `Shape` « qui ne sert qu'au shader », puis une `Shape(shaderName, true)` par mesh (`MeshSet.sendNext`, tableau `shapes`), chacune reprenant le programme ; `display()` appelle `sendShaderValues()` puis `shapes[i].display()` ; culling par son champ `faceCulling` (`glEnable/glDisable(GL_CULL_FACE)` autour des meshes).
- Utilisateurs externes : `Scene3D.java:298` envoie `sBlock.getShape().getShader().sendValueToShader(timeShaderValue)` (ne vaut que pour le programme courant) ; `GraphRenderer`, `MatrixRenderer`, `NodesRenderer` (debug) et `TerrainGeneration3D` passent par `getShape()` pour la géométrie ; `IShaderUniforms` (lumières, matériaux) ne prend que des valeurs de shader ; 6 `new Renderer(`.
- `GLContext` active `GL_BLEND` et `GL_DEPTH_TEST` globalement à sa création.
- Shaders : `resources/shaders/3_30/shaders.json` (`files`, `tags` `2D`/`3D`/`none`) ; GLSL 330 core, seule version utile (`GLContext` demande un contexte 3.3 core). `resources/shaders/1_40/` (un seul shader, `texture2D`) n'est choisi que pour un pilote GLSL 1.40, incompatible avec ce contexte : ne pas y ajouter de shader.
- Tests : rien ne couvre `Shape`/`Renderer` (GL). `test/io/github/mightycode/mightylib/PublicFieldsTest.java` : modèle de garde par réflexion sur les classes compilées (liste les `.class`, exclut la démo).
- Scènes de démo : `src/io/github/mightycode/mightylibdemo/scenes/`. Le menu est **automatique** : `MenuScene` liste les candidats de la complétion de `scene` (`SceneChoices`), `menuScenes.json` ne sert qu'à masquer. Une nouvelle scène n'a rien à inscrire ; `SceneChoicesTest` travaille sur des listes fixes. (`CLAUDE.md` dit encore « l'inscrire dans `MenuScene` » : périmé, voir « Documents ».)
- **Ailleurs** (comparaison faite à la planification) : jMonkeyEngine (`Mesh` + `VertexBuffer.Type`, `Material` + `RenderState`, `Geometry`), three.js (`BufferGeometry.setAttribute(nom)`, `Material.depthTest`/`depthWrite`/`side`, `Mesh` = géométrie + matériau + transform), raylib (`DrawMesh(mesh, material, transform)`, `Material.shader`), libGDX (`Mesh` + `VertexAttribute(Usage, n, alias)`, `Mesh.render(shader, primitive)` ; son `ShapeRenderer` est un dessin immédiat de formes, d'où le nom `MeshRenderer`), Unity (`MeshRenderer`, `ZTest`/`ZWrite`, `Cull`). Modèle commun : géométrie / matériau partageable (shader, paramètres, état GL) / objet affiché (transform). Notre écart volontaire : pas encore de matériau partagé.

## Décisions
- **`Mesh` = géométrie** (renommage de `Shape`, `getShape()` → `getMesh()`) : constructeur `Mesh(boolean useEbo)` ; plus de `Shader`, `in2D`, `applyDimension`, `applyShader`, `getShader`, `getShaderId`, `display`. Méthodes publiques : celles de géométrie existantes, `bind`, `unbind`, `draw`, `unload`, plus des lectures (`getVboCount`, `getVertexCount`, `getIndexCount`, `isUsingEbo`). Point d'accroche documenté : `draw()` (ex. dessin instancié). `Shape2DRenderer` et le paquet `_2D/shape` gardent leur nom (« forme » géométrique 2D, sens correct).
- **Canal explicite** : `addVboFloat(int channel, float[] data, int vertexSize, int storage)` et `addVboInt(int channel, …)` ; les formes actuelles gardent le rang suivant. Le `Mesh` retient le canal de chaque VBO (`rebuild` l'utilise) ; `enableVbo`/`disableVbo`/`isEnableVbo` prennent un **canal** ; `updateVbo`/`updateSubVbo` gardent l'indice renvoyé par `addVbo*`. Un canal déjà pris : erreur claire (`IllegalArgumentException`).
- **Le renderer tient le programme** : acquis dans le constructeur de `Renderer` (même chemin que `Shape` aujourd'hui), libéré une fois dans `unload`. `getShader()` (programme lié ou repli) devient une méthode publique de `Renderer`.
- **`Renderer` (abstraite)** : transform, programme, valeurs de shader, état de dessin, visibilité, caméra de référence. Constructeur `protected Renderer(String shaderName)`. `display()` = si visible : `applyRenderState()`, `getShader().use()`, `updateShader()`, `draw()`. Points d'accroche documentés : `draw()` (abstraite), `updateShader()`, `applyRenderState()`. Méthodes `protected` : `resources()`, `glResources()`, `getReferenceCamera()`, `putOptionalValue`, `hasUniform`, `sendShaderValues`, `sendToShader` (renommé depuis `sentToShader`). Publiques en plus de l'existant : `hasShaderValue`, `getShaderValue`, `removeShaderValue`. Programme, valeurs et état de dessin regroupés (champs voisins, aucune dépendance au transform) pour qu'un futur `Material` les reprenne sans changer l'API des sous-classes.
- **`MeshRenderer extends Renderer`** : `MeshRenderer(String shaderName, boolean useEbo)` (usage simple, crée son `Mesh`) et `MeshRenderer(Mesh mesh, String shaderName)` (injection ; le renderer possède le `Mesh`). `draw()` = `mesh.bind()` + `mesh.draw()`. Textures et canal UV ici, ainsi que la partie « coupe le canal UV » de `setColorMode` (la valeur de couleur reste dans `Renderer`). `getMesh()` publique ; `getMainTexture()` `protected`. `setShape` supprimé.
- **État de dessin** : petite classe `RenderState` (test de profondeur, écriture de profondeur, culling), champ privé du `Renderer`, accesseurs `setDepthTest`, `setDepthWrite`, `setFaceCulling` (et lectures). Défauts : tag `2D`/`none` → ni test ni écriture ; sinon les deux ; culling désactivé. `applyRenderState()` les applique à chaque dessin (`glEnable/glDisable(GL_DEPTH_TEST)`, `glDepthMask`, `GL_CULL_FACE`) ; `ModelRenderer.faceCulling` passe par cet état.
- **Caméra par défaut** : choisie au constructeur depuis le même tag (2D → `camera2D`, sinon `camera3D`), clés `projection`/`view` remises droites.
- **`ModelRenderer extends Renderer`** : plus de `Shape` de base ; ses `Mesh` de mesh sans shader ; `draw()` lie et dessine chaque mesh après l'utilisation du programme du renderer.
- **Test de garde** `ExtensionFieldsTest` (même paquet et même lecture des classes que `PublicFieldsTest`) : `Mesh`, `Renderer`, `MeshRenderer`, `Shape2DRenderer` n'ont aucun champ `protected` ; liste des classes concernées en tête du test, à étendre par le point « Héritage et API publique ».
- Les autres renderers concrets gardent leurs `protected` éventuels : point « Héritage et API publique ».

## Étapes (une par commit)
1. **Plan** : commiter ce fichier tel quel.
2. **Démo `CustomRenderer2D` sur l'API actuelle** (fusionnable seule) :
   - Shader `customStar2D` (tag `2D`, `#version 330 core`) dans `resources/shaders/3_30/` + son `shaders.json` : position 2D (canal 0), couleur par sommet (canal 3), uniform `time` pour une variation.
   - `src/io/github/mightycode/mightylibdemo/scenes/CustomRenderer2D.java` : une étoile (triangle fan, couleurs par sommet) par une sous-classe de `Renderer` qui remplit sa `Shape` et anime `time` ; un second renderer à `Shape` construite par la scène, sans texture ; touche `D` pour basculer la profondeur. Chaque contournement est commenté `// Workaround:` (canaux 1 et 2 remplis pour atteindre le canal 3, `Shape` imposée par le constructeur, `setShape` qui laisse fuir la première `Shape` et son shader, profondeur liée au tag du shader…).
   - Rien à inscrire dans le menu (automatique).
   - Vérif : `scenario.py "scene CustomRenderer2D" wait-loaded "?gl summary"`, `leakcheck CustomRenderer2D`.
3. **Matrices 3D croisées** (fusionnable seule) : `Renderer` met `getProjection()` sous `projection` et `getView()` sous `view`. Vérif : les quatre scènes 3D s'affichent comme avant.
4. **Renommage `Shape` → `Mesh`** (mécanique, sans changement de comportement) : classe, `getShape`/`setShape` → `getMesh`/`setMesh`, champ `shape` → `mesh`, `Shape.STATIC_STORE`… → `Mesh.…`, tableau `shapes` de `ModelRenderer`, 15 fichiers. Vérif : compilation, `Scene2D`, une scène 3D.
5. **Le programme passe de `Mesh` à `Renderer`** :
   - `Mesh(boolean useEbo)` ; retirer `shader`, `getShader`, `applyShader`, `getShaderId`, `shaderReleased` et le chemin d'acquisition ; `display()` retiré (appelants : `Renderer.draw`, `ModelRenderer`).
   - `Renderer` acquiert/libère le programme, `getShader()`, `hasUniform`/`sendToShader` sur son propre programme ; `display` utilise le programme avant `updateShader`.
   - `ModelRenderer` : `Mesh` de mesh sans shader ; programme utilisé une fois, puis par mesh : textures, brillance, `bind` + `draw`.
   - `Scene3D.java:298` : `addShaderValue`/`updateShaderValue` au lieu de l'envoi direct.
   - `in2D` reste encore dans `Mesh` (renseigné par le renderer) jusqu'à l'étape 6.
   - Vérif : `Scene2D`, quatre scènes 3D, `CustomRenderer2D` ; `leakcheck Scene3D` (programmes rendus une fois, `gl summary`).
6. **État de dessin** : `RenderState` (test et écriture de profondeur, culling) dans `Renderer`, défauts depuis le tag ; retirer `in2D`, `applyDimension`, `is2DPurpose`, `setDimensionTo2D` de `Mesh` ; caméra par défaut depuis le tag ; `ModelRenderer.faceCulling` → `setFaceCulling`. Démo : la touche utilise `setDepthTest`. Vérif : `Game3D2D` (2D par-dessus 3D), `TerrainGeneration3D`, `Scene3D` (culling).
7. **Hiérarchie `Renderer` / `MeshRenderer`** : `Renderer` abstraite (`draw()` abstraite), `MeshRenderer` avec le `Mesh`, les textures et les deux constructeurs ; `setMesh` supprimé ; `ModelRenderer extends Renderer`, toutes les autres sous-classes `extends MeshRenderer` ; les 6 `new Renderer(` → `new MeshRenderer(` ; les types des champs (`GraphRenderer`, `MatrixRenderer`, `NodesRenderer`…) qui appellent `getMesh()` passent à `MeshRenderer`. Démo : le second renderer utilise `new MeshRenderer(mesh, "customStar2D")`.
8. **Canal explicite des attributs** : `addVboFloat(channel, …)`/`addVboInt(channel, …)`, canal retenu par VBO et utilisé par `rebuild`, `enableVbo`/`disableVbo` par canal, erreur sur canal pris. Démo : couleur directement en canal 3, contournement supprimé. Vérif : toutes les scènes (le canal UV `COMMON_TEXTURE_POSITION_CHANNEL` doit rester juste), `MultiWindowScene`.
9. **Champs privés et méthodes choisies** : `Mesh`, `Renderer`, `MeshRenderer`, `Shape2DRenderer` sans champ `protected` (accesseurs listés dans « Décisions » ; `Shape2DRenderer` : `positionIndex`, `textureIndex`, `texturePosition` privés, accesseurs seulement si un usage existe) ; migrer `ModelRenderer` (`shaderValues` → `hasShaderValue`), `Text` (`getReferenceCamera`), `Shape2DRenderer` (`getMainTexture`), `resources`/`glResources` → méthodes. Court commentaire anglais sur chaque point d'accroche (`draw`, `updateShader`, `applyRenderState`, constructeurs) : ce qu'une sous-classe peut redéfinir et ce qu'elle doit appeler. Test `ExtensionFieldsTest`. Relancer `mvn -o -q test` (dont `PublicFieldsTest`). La démo ne contient plus aucun `// Workaround:`.
10. **Docs** (voir plus bas) et « Bilan ».

Chaque étape : compiler, `mvn -o -q test`, vérifier en jeu ; relire le diff comme l'analyseur d'IntelliJ (imports inutiles, etc.).

## Démo
`CustomRenderer2D` (nouvelle scène, apparaît seule dans le menu) : test d'acceptation du contrat d'extension ; à la fin, elle n'utilise que l'API publique et `protected` documentée, sans aucun contournement.

## Documents à mettre à jour à la fin
- `architecture.md` : `Mesh` = géométrie (canaux explicites) ; `Renderer` (programme, valeurs, état de dessin, caméra) / `MeshRenderer` (un `Mesh`, textures) / `ModelRenderer` ; points d'accroche ; ligne de `ModelRenderer` (« La `Shape` de base ne sert qu'au shader » disparaît) ; ligne des placeholders (`Shape` → renderer) ; ligne « 2D sans batching » (l'état de profondeur vient de l'état de dessin) ; toute autre mention de `Shape`.
- `decisions.md` : `Mesh` = géométrie, le programme et l'état GL appartiennent au renderer ; un renderer possède son `Mesh` ; noms `Mesh`/`MeshRenderer` (usuels, `ShapeRenderer` a un autre sens dans libGDX).
- `later.md` : `Material` partageable (programme, valeurs, `RenderState`) avec le regroupement des dessins (clé de tri) ; partage d'un `Mesh` entre renderers (possession, compteur) ; état de dessin à étendre (blend par renderer, cache des `glEnable` redondants dans `GLContext`) ; attributs nommés si besoin ; retrait du dossier `shaders/1_40` et de la branche GLSL 140 de `ShaderManager.init` (avec l'injection de `#version`, section Shaders).
- `current.md` : supprimer l'objectif « Rendu extensible » ; dans « Héritage et API publique », `Shape`/`Renderer` → `Mesh`/`Renderer`/`MeshRenderer` dans la liste des classes extensibles, et mentionner `ExtensionFieldsTest`.
- `CLAUDE.md` : ajouter `ExtensionFieldsTest` à la liste des tests couverts ; remplacer « l'inscrire dans `MenuScene` » (menu automatique, `menuScenes.json` pour masquer) ; toute mention de `Shape`/`Renderer` communs (« `Shape`/`Renderer`/`Shader` sont communs » → `Mesh`/`Renderer`/`Shader`).
- `bonnes-pratiques.md` point 7 si la garde y a sa place.
- **Supprimer ce plan** (fait par `/pr-ml`).

## Hors périmètre
- `Material` partageable, partage d'un `Mesh` entre renderers, batching 2D : `later.md` (Rendu 2D, regroupement des dessins).
- Blend par renderer, cache des états GL, attributs nommés : `later.md`.
- Champs `protected` des autres renderers concrets, `final` des classes : objectif « Héritage et API publique » de `current.md`.
- `updateShaderValue` sur une valeur absente (NPE) : à noter dans `later.md` s'il n'y est pas.
- Retrait de `shaders/1_40` : `later.md`.
- OpenGL/GLSL 4.3+ : `futur-discussion.md` (section Rendu).

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1–2 (démo, shader) : Claude Sonnet 5.5, effort `medium` (code de démo, peu de conception).
- Discussion A, étape 4 (renommage) : Claude Sonnet 5.5, effort `medium` (mécanique).
- Discussion A, étapes 3, 5–9 (modèle et API de `Mesh`/`Renderer`, `ModelRenderer`, canaux, migration) : Claude Opus 5.5, effort `high` (contrat durable, gros fichiers, 3D à ne pas casser).
- Discussion A, étape 10 (docs) : Claude Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (gros diff à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/rendu-extensible.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `renderer-extensible`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`CustomRenderer2D` : étoile colorée animée, touche `D` de profondeur ; `Scene3D` : culling et lumières ; `Game3D2D` : 2D par-dessus la 3D ; `TerrainGeneration3D`, `Scene3DBis` ; `Scene2D` : formes, textes, tilemap ; `MultiWindowScene` : rendu dans chaque fenêtre). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml renderer-extensible`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
### Fait
Les 10 étapes, un commit chacune sur `renderer-extensible` : plan, démo `CustomRenderer2D` + shader `customStar2D`, matrices 3D droites, `Shape` → `Mesh`, programme dans `Renderer`, `RenderState`, `Renderer` abstraite / `MeshRenderer`, canaux explicites, champs privés + `ExtensionFieldsTest`, docs.

### Écarts au plan
- `Shape2DRenderer.texturePosition` était lu par `RectangleRenderer`, `EllipseRenderer`, `PolygonRenderer` (le plan le croyait inutilisé) : privé, avec `setTexturePosition`/`getTexturePosition`.
- `position`, `scale`, `rotation`, `angle`, `model` de `Renderer` sont aussi privés (accès par `position()`, `scale()`… existants).
- `Renderer.setColorMode(color)` reste dans `Renderer` (sans toucher aux UV) ; `MeshRenderer` ajoute `setColorMode(color, disableTexture)`.
- Démo : la première étoile avait un rapport intérieur de 1 (décagone), corrigé ; l'étape 6 oblige `setDepthWrite` en plus de `setDepthTest` (un shader 2D n'écrit pas la profondeur par défaut).
- Bugs existants notés dans `later.md` : `updateShaderValue` sur valeur absente (NPE), `setTexturePosition` sans renvoi de la texture.

### Vérifié
`mvn -o -q test` (903 tests, dont `ExtensionFieldsTest`, `PublicFieldsTest`) ; avec `scenario.py` : `Scene2D`, `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`, `CustomRenderer2D` (touche D), `MultiWindowScene` (outils et second contexte) chargées sans erreur de log ; captures de `Scene3D` et `Game3D2D` identiques avant/après ; `leakcheck CustomRenderer2D` et `Scene3D` sans fuite.

### À vérifier à la main
- `CustomRenderer2D` : deux étoiles colorées animées ; D : la petite étoile passe devant/derrière la grande.
- `Scene3D` : culling des modèles et lumières ; `Game3D2D` : 2D par-dessus la 3D ; `TerrainGeneration3D`, `Scene3DBis`.
- `Scene2D` : formes, textes, tilemap ; `MultiWindowScene` : rendu dans chaque fenêtre.
