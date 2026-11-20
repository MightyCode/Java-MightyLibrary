# Plan — `Material` partageable et état de dessin

**Statut : validé.**

Décisions prises :
1. Nom : `Material` pour le nouvel objet partagé ; l'actuel matériau Phong (`graphics/lighting/materials/Material`, `BasicMaterial`) devient `PhongMaterial`, `BasicPhongMaterial`, sans alias.
2. Un renderer créé par nom de shader a son propre `Material` (comportement identique à aujourd'hui) ; le partage est explicite (`setMaterial`, constructeurs prenant un `Material`). Modifier un matériau partagé modifie tous ses renderers : jamais de copie implicite (écueil de `renderer.material` de Unity).
3. Propre au renderer : transform (`model`), couleur, visibilité, caméra, et des **surcharges** (valeurs d'uniform propres qui gagnent sur celles du matériau, comme le `MaterialPropertyBlock` de Unity). Les textures sont dans le matériau.
4. `Mesh` et `Material` partagés : **compteur de références**, un seul modèle pour les deux.
5. Blend par matériau (`EBlendMode` dans le `RenderState`) + **cache d'état GL par `GLContext`** ; tout changement d'état GL du moteur passe par lui.
6. Uniforms : locations **entières** résolues une fois par version de programme ; plus de recherche par nom à chaque dessin.
7. Le `Material` fournit **sa part de la clé de tri** (entier) et une **version** incrémentée à chaque changement ; la clé complète (couche, ordre, transparence, mode de couleur résolu) se décide avec le regroupement.
8. Un `Mesh` ne change plus ses canaux au dessin : le shader décide (le mode couleur et le mode texture sont déjà deux programmes, `colorShape2D`/`texture2D` ; un attribut non lu ne gêne pas).
9. 3D, version propre : `PhongMaterial` et les lumières écrivent dans une cible d'uniforms commune (`Material` ou `Renderer`) ; `ModelRenderer` a un `Material` de base et garde ses textures par sous-mesh (part de clé par sous-mesh).
10. Debug : compteurs dans `stats`, commande `gl materials`, `leakcheck` étendu aux `Material`/`Mesh` retenus.
11. Démo : nouvelle scène `SharedMaterial2D`.

## Pour démarrer
- Lire : `docs/current.md` (objectif « `Material` partageable et état de dessin »), `docs/architecture.md` sections « Rendu », « Ressources GPU », « API publique », `docs/decisions.md` (ligne « Rendu : `Mesh` = géométrie… »), `docs/debug-tools.md` (commandes `gl`, `stats`, `leakcheck`).
- Branche de sujet depuis `dev` : `feature/shared-material`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre). Scènes à revoir à chaque étape qui touche le rendu : `Scene2D`, `VectorShapes2D`, une scène de tilemap, une d'animation, la console, et les scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`.
- Prérequis : l'objectif « Programmes dans `GenDeleteResources` » (compteur `Program`) de `current.md` est fait avant (`/implement-next-objective`) ; sinon le faire en étape 0.

## Contexte : ce qui existe
- `graphics/renderer/Renderer.java` (abstraite, extensible) : tient le programme (`Shader` acquis par `GLResources.acquire(shaderData, true)`, rendu dans `unload`, repli `getShader()`), les valeurs d'uniform (`HashMap<String, ShaderValue> shaderValues` + `optionalValues`, envoyées **par nom** à chaque dessin par `sendShaderValues`), le `RenderState`, la transform, la visibilité, la caméra de référence. `display()` = `applyRenderState()` → `getShader().use()` → `updateShader()` → `draw()` → `renderState.restore()`. API de valeurs : `addShaderValue`, `updateShaderValue`, `removeShaderValue`, `copyShaderValuesTo`, `putOptionalValue`, `setColorMode` (uniform `color`) ; ~100 usages dans le dépôt.
- `graphics/renderer/RenderState.java` (`final`) : profondeur (test, écriture), culling, mode de couleur (`ERenderColorSpace`, `INHERIT` par défaut) ; `apply(linear)` refait tous les `glEnable`/`glDisable` à chaque dessin ; `restore()` remet le masque de profondeur et `GL_FRAMEBUFFER_SRGB`.
- `graphics/renderer/MeshRenderer.java` (extensible) : possède son `Mesh` (`unload` → `mesh.unload()`), texture principale (unité 0) et canaux (`addTextureChannel`, unités 1..30), liés dans `updateShader`. `setColorMode(c, true)`/`setNoColorNoTextureMode` désactivent le canal UV du `Mesh` (`disableVbo`).
- Blend : fixé une fois par `GLContext.applyDefaultState` (`GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA`). Seul cache d'état : `GLContext.currentProgram`. Appels d'état directs ailleurs : `Scene` et `WindowView` (scissor, `GL_FRAMEBUFFER_SRGB`), `Window.glEnable2D/glDisable2D`, liaisons de textures (`bindRenderTexture`), `FrameBuffer`.
- `Shader.sendValueToShader` : `lastValues` et `uniformLocations` en `HashMap<String, …>` ; version de programme (`programVersion`) changée au rechargement.
- Matériau Phong 3D : `graphics/lighting/materials/BasicMaterial`, `Material` (structures pures, champs publics, `addToRenderer(renderer, structure)`/`updateRenderer`), interface `IShaderUniforms` partagée avec les lumières (`DirectionalLight`, `PointLight`, `SpotLight`) ; utilisés par `Scene3D`, `Scene3DBis`, `TerrainGeneration3D`, `ColorSpaceSplit3D` ; tests `MaterialCopyTest`, `LightCopyTest`, listés dans `PublicFieldsTest`.
- `ModelRenderer` (`final`) : plusieurs sous-meshes (`MeshSet`), textures diffuse/spéculaire par sous-mesh, `MeshUpload` (`GLElement`), `setTextureUniforms`, `setDiffuseOverride`.
- Sous-classes de `Renderer` : `MeshRenderer` → formes 2D (`Shape2DRenderer`, `VectorShape2DRenderer`, `GeometricShape2DRenderer`…), `Text`, `TileMapRenderer`, `Animation2DRenderer`, 3D (`ModelRenderer`, `CubeRenderer`, `SkyboxRenderer`, `HeightMapRenderer`, `TerrainRenderer`), démo `CustomRenderer2D` (seul usage de `new MeshRenderer(mesh, shader)`).
- Garde-fous : `ExtensionFieldsTest`/`ExtensibleClasses`, `PublicFieldsTest`, `ApiStatusTest`, `ModuleDescriptorTest` ; `GLContextTest` (contexte caché, tag `gl`).

## Comparaison (sources)
- Unity : `sharedMaterial` partagé ; `renderer.material` copie silencieusement ; valeurs par objet par `MaterialPropertyBlock` ([doc](https://docs.unity3d.com/Manual/DrawCallBatching-Properties.html), [forum](https://discussions.unity.com/t/materialpropertyblock-settexture-breaks-batching/853876)).
- Godot 3 : un changement de texture ou de matériau coupe un lot ; propriétés par objet hors uniforms ([batching](https://docs.godotengine.org/en/3.5/tutorials/performance/batching.html)).
- three.js, Bevy, raylib : `Material` partagé (shader + valeurs + état).
- Clé de tri : Christer Ericson ([article](https://realtimecollisiondetection.net/blog/?p=86)) ; tri approximatif par profondeur ([Aras P.](https://aras-p.info/blog/2014/01/16/rough-sorting-by-depth/)).

## Décisions de détail
- `Material` : package `graphics/renderer` (exporté, statut `EXPERIMENTAL`), classe `final` (la réimplémentation passe par `Renderer`, pas par l'héritage du matériau). Contenu : `ShaderData` acquise (repli comme aujourd'hui), valeurs communes (nom → `ShaderValue`, optionnelles comprises), textures (principale + canaux 1..30), `RenderState`. Thread principal seulement.
- **Compteur de références** : classe interne sans GL (`graphics/renderer/internal/RefCount` ou équivalent) utilisée par `Mesh` et `Material` : `retain()`, `release()` (libère à zéro, `IllegalStateException` en dessous de zéro), `references()`. Un `Material`/`Mesh` créé par un renderer est retenu une fois par lui ; créé par le code du jeu, il commence à zéro et chaque renderer qui le prend le retient ; celui qui veut le garder après ses renderers le retient lui-même. `setMaterial` retient le nouveau avant de rendre l'ancien (pas de rechargement d'un programme commun). `MeshRenderer(Mesh, shader)` retient le `Mesh` au lieu de le posséder.
- **Cible d'uniforms** : interface (`IUniformTarget`, nom à confirmer) implémentée par `Material` et `Renderer` : `addShaderValue`, `updateShaderValue`, `removeShaderValue`, `hasShaderValue`. `IShaderUniforms.addTo(cible, structure)`/`update(cible, structure)` remplace `addToRenderer`/`updateRenderer`.
- **Renderer** : `material()` (le partagé), `setMaterial(Material)` ; l'API de valeurs du `Renderer` écrit dans ses **surcharges** ; `material().addShaderValue` écrit dans le matériau. Envoi au dessin : valeurs du matériau puis surcharges (la surcharge gagne). Les valeurs génériques (`model`, `projection`, `view`, `mlLinearColors`, `mlViewportSize`) et `color` restent dans le renderer. `copyShaderValuesTo` garde son sens (surcharges).
- **Délégations** : `setDepthTest`/`setDepthWrite`/`setFaceCulling`/`setColorSpace`/`setBlendMode` du `Renderer` et les textures de `MeshRenderer` (`setMainTextureChannel`, `addTextureChannel`) écrivent dans le matériau ; leur Javadoc dit que c'est partagé si le matériau l'est.
- **`EBlendMode`** : `NONE`, `ALPHA` (défaut, identique à aujourd'hui), `PREMULTIPLIED`, `ADDITIVE`, `MULTIPLY` ; dans `RenderState`.
- **Cache GL** : état mémorisé par `GLContext` (chaque contexte a le sien) : blend activé + fonction, test et masque de profondeur, culling, `GL_FRAMEBUFFER_SRGB`, scissor, programme (existant), texture liée par unité et unité active. Logique dans une classe sans GL (`GLStateCache`, appliqueur injecté) pour être testée en JUnit ; `GLContext` lui donne l'appliqueur réel. Invalidation complète (`invalidate()`) après un code qui touche l'état directement (ex. bibliothèque tierce) : méthode publique documentée. `restore()` du `RenderState` n'existe plus : le cache laisse l'état tel quel, et `clear()` d'une vue remet ce dont `glClear` dépend (masque de profondeur écrivable, sRGB désactivé) par le cache.
- **Uniforms par location** : `Shader` expose `location(nom)` (une fois) et `sendValue(int location, ShaderValue)` avec dernier envoi mémorisé par location (tableau) ; `Material` et `Renderer` gardent un tableau de locations par valeur, reconstruit quand `programVersion` ou le programme courant (repli) change. Absent = -1, signalé une fois comme aujourd'hui (sauf optionnelle).
- **Part de clé** : `Material.sortKeyPart()` = entier composé de l'identifiant du programme courant, des identifiants des textures, du blend et de la profondeur (bits précis à l'implémentation, documentés) ; `Material.version()` incrémentée à chaque changement (valeur, texture, état). Pour `ModelRenderer`, une part par sous-mesh (textures du sous-mesh).
- **Canaux du `Mesh`** : `setColorMode(c, true)`/`setNoColorNoTextureMode` ne touchent plus au `Mesh` ; vérifier pour chaque shader 2D et 3D concerné qu'il ne lit pas l'UV en mode couleur (sinon : define ou uniform du matériau).

## Étapes
Une étape = un commit. Les étapes 1 et 2 peuvent être fusionnées seules ; 3 à 6 forment l'objectif.

1. **Renommage Phong** : `BasicMaterial` → `BasicPhongMaterial`, `Material` → `PhongMaterial` (package inchangé), `MaterialCopyTest` → `PhongMaterialCopyTest`, `PublicFieldsTest`, scènes 3D de la démo, docs qui les nomment. Aucun changement de comportement. Vérif : `mvn test`, `Scene3D`.
2. **Cache d'état GL et blend** : `GLStateCache` (sans GL) + intégration à `GLContext` ; `EBlendMode` dans `RenderState` ; `RenderState.apply` passe par le cache ; `Scene`, `WindowView`, `Window`, liaisons de textures et `FrameBuffer` aussi ; compteurs (changements faits, évités) lus par `stats` (`glStateChangesPerFrame`, `glStateChangesSkippedPerFrame`, `textureBindsPerFrame`). Tests JUnit : un changement redondant n'appelle pas l'appliqueur, `invalidate` force le suivant, contextes indépendants, chaque mode de blend donne sa fonction. Test `gl` : état du cache = `glGet` après une suite de changements. Vérif en jeu : scènes listées, rendu identique, `stats` avant/après (noter les chiffres dans le Bilan).
3. **Compteur de références et `Mesh` partageable** : `RefCount` (sans GL) ; `Mesh.retain/release` (`unload` à zéro) ; `MeshRenderer(Mesh, …)` retient ; `setColorMode`/`setNoColorNoTextureMode` ne touchent plus aux canaux (vérification shader par shader). Tests : `RefCountTest` (retain/release, zéro, en dessous de zéro), deux `MeshRenderer` sur un `Mesh` (test `gl` ou `TestEngine` selon ce que `Mesh` exige). Vérif : `CustomRenderer2D`, `leakcheck` sur une scène 2D.
4. **`Material`, cible d'uniforms, locations entières** : `Material` (contenu ci-dessus), `IUniformTarget`, `Shader.location`/`sendValue(int, …)`, `Renderer` délègue (matériau propre par défaut, `material()`, `setMaterial`, surcharges), `MeshRenderer` met ses textures dans le matériau ; `IShaderUniforms.addTo/update` (Phong, lumières). Tests : ordre matériau puis surcharge, valeur optionnelle absente non envoyée, locations reconstruites à un changement de version, `version()` incrémentée par chaque modification, `sortKeyPart` égale pour deux matériaux de même état et différente sinon, `setMaterial` retient avant de rendre. Vérif : toutes les scènes listées (rendu identique), rechargement à chaud d'un shader (locations reconstruites), shader en échec (repli).
5. **Passage des renderers** : formes 2D, `Text`, `TileMapRenderer`, `Animation2DRenderer`, `CustomRenderer2D` ; 3D : `CubeRenderer`, `SkyboxRenderer`, `HeightMapRenderer`, `TerrainRenderer`, `ModelRenderer` (matériau de base + textures par sous-mesh, part de clé par sous-mesh), scènes 3D de la démo avec `PhongMaterial` écrit dans un `Material` partagé là où deux objets ont le même. Aucun changement visible. Vérif : scènes listées, `?gl summary`, captures ciblées comparées à `dev` sur `VectorShapes2D` et `Scene3D`.
6. **Debug** : commande `gl materials` (identifiant, niveau `DEV`, syntaxe déclarée ; `message` : nombre de matériaux vivants ; `data` : par matériau shader, références, blend, part de clé, version ; meshes partagés et références) ; `leakcheck` de `scenario.py` signale un `Material`/`Mesh` encore retenu après la sortie d'une scène (registre faible des vivants, côté moteur, lu par la commande). Tests : syntaxe et complétion de la commande (`ShadersGenComTest` comme modèle), registre des vivants sans GL.
7. **Démo `SharedMaterial2D`** (voir Démo) + inscription dans `DemoScenes`.
8. **Docs** (voir plus bas), sans supprimer ce plan, puis Bilan.

## Démo
`SharedMaterial2D` (dans `DemoScenes`, avec description) : une grille de sprites qui partagent un `Material` (touche : changer la texture du matériau → tous changent ; touche : changer le blend) ; quelques sprites avec une couleur en surcharge ; deux renderers sur un même `Mesh` ; les cinq `EBlendMode` côte à côte sur un fond coloré ; texte d'aide et compteurs `stats` affichés. Vérif : `leakcheck SharedMaterial2D`.

## Documents à mettre à jour à la fin
- `architecture.md` : Rendu (`Material`, surcharges, partage et compteurs, `EBlendMode`, cache d'état GL, locations entières, part de clé ; `Mesh` partageable ; Phong renommé et cible d'uniforms).
- `decisions.md` : ligne « Rendu » réécrite (le programme, les valeurs communes, les textures et l'état appartiennent au `Material` ; partage explicite sans copie implicite ; compteur de références).
- `debug-tools.md` : `gl materials`, nouveaux compteurs de `stats`, `leakcheck` étendu.
- `current.md` : supprimer l'objectif (garder ce qui ne serait pas fait).
- `later.md` : ce qui reste (attributs nommés si non faits, matériau par sous-mesh complet de `ModelRenderer` si réduit, UBO des lumières).
- `CLAUDE.md` : tests couverts (`GLStateCache`, `RefCount`, `Material`, commande `gl materials`, `PhongMaterialCopyTest`).
- Supprimer ce plan (discussion B).

## Hors périmètre
- Tri et regroupement des dessins, clé complète, instancing : objectif suivant de `current.md`.
- Attributs nommés (en plus des canaux) : seulement si une étape en a besoin ; sinon `later.md`.
- Lumières en uniform block partagé (UBO) : `later.md` (pas d'extension de la 3D ici).

## Questions restantes
- Aucune bloquante ; à confirmer au début : noms `IUniformTarget` et `RefCount`.

## Modèles recommandés
- Discussion A, étape 1 : Claude Sonnet 5.5, effort `medium` (renommage mécanique).
- Étapes 2 à 5 : Claude Opus 5.5, effort `high` (modèle durable, cache d'état et locations délicats, nombreux renderers à ne pas casser).
- Étapes 6 à 8 : Claude Sonnet 5.5, effort `medium` (commande sur un modèle existant, scène, docs).
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (gros diff transverse à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/material-partageable.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/shared-material`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`SharedMaterial2D` : touches de texture et de blend, surcharges de couleur, cinq modes de blend ; `VectorShapes2D`, tilemap, animation, console : rendu identique ; scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` : rendu identique ; rechargement à chaud d'un shader). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/shared-material`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Vérifications manuelles : celles du « Bilan ».

## Bilan

### Fait
Branche `feature/shared-material`, un commit par étape (voir `git log dev..`), plus l'étape 0 :
- 0. Compteur `Program` dans `GenDeleteResources` (prérequis, qui n'était pas fait).
- 1. Renommage `BasicPhongMaterial` / `PhongMaterial` (et `copyBasicPhongMaterial`, `PhongMaterialCopyTest`).
- 2. `GLStateCache` (+ `IGLStateApplier`, `GLStateApplier`) dans `GLContext` ; `EBlendMode` dans `RenderState` ; scissor, `clear()` des vues, liaisons de textures et `Shader.use` passent par lui ; `RenderState.restore` supprimé ; compteurs `glStateChangesPerFrame`, `glStateChangesSkippedPerFrame` (le compteur `textureBindsPerFrame` existait déjà et compte maintenant les liaisons envoyées).
- 3. `RefCount`, `Mesh.retain/release/references`, `MeshRenderer` retient son mesh ; `setColorMode`/`setNoColorNoTextureMode` ne touchent plus aux canaux (`MeshRenderer.isTextureEnabled`).
- 4. `Material`, `IUniformTarget`, `Renderer.material()/setMaterial`, surcharges, locations entières (`Shader.location`, `sendValue`, `programStamp`), `IShaderUniforms.addTo/update`.
- 5. Constructeurs `(Material)` des renderers (`MeshRenderer`, `CubeRenderer`, `Shape2DRenderer`, `RectangleRenderer`), cubes de `Scene3DBis` sur deux `Material` partagés, `ModelRenderer.sortKeyPart(mesh)`.
- 6. `SharedObjects` (registre faible), commande `gl materials`, `retainedObjects` dans `gl`, lu par `leakcheck` (`scenario.py`, `SceneRunner`).
- 7. Démo `SharedMaterial2D`. 8. Docs.

### Écarts au plan, et pourquoi
- Étape 0 faite ici : le prérequis (« Programmes dans `GenDeleteResources` ») n'était pas fait.
- Étape 5, passage « des renderers » : tous passent par `Renderer`/`MeshRenderer` (donc par un `Material`) sans changement de code ; seuls les constructeurs `(Material)` ont été ajoutés là où le partage sert (démo, `Scene3DBis`). `SkyboxRenderer` lie encore son cubemap lui-même (noté dans `later.md`). `Scene3D` n'a pas de partage de `PhongMaterial` entre deux objets de même valeur : seule `Scene3DBis` en a.
- `Window.glEnable2D`/`glDisable2D` ne sont pas passés par le cache : ils ne touchent que d'anciennes matrices fixes, aucun état mis en cache.
- `FrameBuffer` : liaisons de textures par le cache ; `bind`/`unbind` de framebuffers non mis en cache (hors plan).
- Les textures de `Material.setMainTexture(String)` sont acquises comme avant : une texture hors du batch du jeu donne le repli (c'est pourquoi `SharedMaterial2D` utilise `painting`, `hexagon`, `slime`, du batch `init`).
- `IUniformSink`/`UniformValues` (package-private) : ajoutés pour tester l'envoi par location sans GL.

### Vérifié, et comment
- `mvn -o test` (tous les modules) vert à chaque étape ; tests ajoutés : `GLStateCacheTest`, `GLStateCacheGLTest`, `RefCountTest`, `SharedMeshTest`, `UniformValuesTest`, `MaterialTest`, `LiveObjectsTest`, `GLMaterialsComTest`.
- Captures de `Scene2D`, `VectorShapes2D`, `ColorSpaceSplit2D`, `WaveFunctionCollapse2D`, `Box2D`, `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`, `ColorSpaceSplit3D` comparées pixel par pixel à celles de `dev` (script dans le scratchpad) : identiques, sauf les zones animées de `Scene2D` (0,3 %) et un compteur de `WaveFunctionCollapse2D`. `ColorSpaceSplit3D` diffère de `dev` quand on la lance après une autre scène (déjà sur `dev`, noté dans `later.md`).
- `leakcheck` sans fuite : `Scene2D`, `VectorShapes2D`, `CustomRenderer2D`, `Scene3D`, `Scene3DBis`, `Game3D2D`, `SharedMaterial2D`.
- Rechargement à chaud (`reload ShaderData texture2D colorShape2D` sur `ColorSpaceSplit2D`) : rendu identique.
- `stats` (état GL, par frame, envoyés/évités) : `Scene2D` 12/225, `VectorShapes2D` 17/546, `Scene3D` 121/344 environ.

### À vérifier à la main
- `SharedMaterial2D` : T change la texture de toute la grille, B son blend (ALPHA, ADDITIVE, MULTIPLY) ; rectangles de couleurs différentes (surcharge) ; deux rectangles sur un même `Mesh` ; les cinq blends côte à côte, distincts ; `gl materials` dans la console liste la grille à 18 références.
- `VectorShapes2D`, une tilemap (`Scene2D`), une animation, la console : rendu identique à `dev`.
- Scènes 3D `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` : rendu identique.
- Rechargement à chaud d'un shader pendant l'affichage (`watch` ou `reload`) : pas de scintillement ni d'erreur dans le journal.
