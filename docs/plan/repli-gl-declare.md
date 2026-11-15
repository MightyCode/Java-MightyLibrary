# Repli GL déclaré par chaque donnée, `MultiSourceDataType` utile, skybox

**Statut** : validé.

Décisions prises :
- La **donnée de repli** déclare si son élément GL doit être prêt avant tout dessin (méthode de `DataType`, faux par défaut, vrai pour `FallbackShaderData`).
- Élément de repli retrouvé **par type de donnée** (même règle de parent que `Resources.getFallback`), rangé à part dans `GLResources` : plus de collision sur le nom `error` entre deux replis d'un même type d'élément.
- `GLResources` ne connaît plus aucun type d'élément : `TexturePlaceholders` et `TextureUploader.releaseBuffers` passent par un **état partagé par type d'élément**, désigné par sa classe.
- `MultiSourceDataType` **gardé**, avec son **état par fichier** rendu utile (journal de l'échec, commande `resources`) ; rechargement partiel laissé à plus tard.
- `gl summary` liste les éléments de repli.
- Démo 2D : nouvelle scène, donnée de démo **multi-fichiers** (texture faite de calques superposés) avec son **propre repli GL**.
- **Skybox** dans `TerrainGeneration3D` : donnée cubemap à 6 fichiers (dossier + noms de faces fixes), élément `GL_TEXTURE_CUBE_MAP` avec son repli, renderer et shader GLSL 330 et 140 ; images générées par un script Python (aucune licence tierce).

## Pour démarrer
- Lire : `docs/current.md` (ligne « Repli GL déclaré par chaque élément ») ; `docs/architecture.md`, sections **Ressources** (Placeholders, découverte, observateur de fichiers) et **GLResources** (jamais bloquant, échecs, `Shader`, repli) ; `docs/decisions.md` (deux états d'échec d'un `GLElement`, valeurs d'uniform au `Renderer`) ; `docs/debug-tools.md` (`gl summary`, `resources`).
- Branche de sujet depuis `dev` : `repli-gl-declare`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `graphics/GLResources.fallbackDataLoaded()` (appelée par `Resources.finishLoading` via `IGLElementSink`) crée en dur `new Texture(cpuResources, "error")` (envoyée aux frames suivantes) et le `Shader` de `FallbackShaderData` (`addElementAndLoadNow`, arrêt s'il ne se lie pas). Imports de `graphics.shader` et `resources.texture` dans `GLResources`.
- Lecteurs du repli : `Renderer.getShader()` (à chaque dessin : `glResources.fallbackShader()`), `Texture.getFallbackId()` (`getResource(Texture.class, Resources.FALLBACK_NAME)`, sinon `placeholders().placeholderId()`), `Texture.bind` (`placeholders().fallbackRectangleId(resources)` pour une texture rectangle).
- Chaque donnée déclare déjà son élément : `DataType.glElementType()`, `glElementName()`, `createGLElement()` (`TextureData` → `Texture`, `ShaderData` → `Shader`, `FallbackShaderData` → `FallbackShader`, `TileMap` → `Texture` de son atlas, non géré par `Resources`).
- Données de repli : `resources/FallbackRegistry` (une par type de loader, toutes nommées `Resources.FALLBACK_NAME` = `error`, référencées tout le programme ; `get` remonte aux types parents). Celles qui ont un élément GL : `FallbackTextureData`, `FallbackShaderData`.
- **Défaut** : un type de donnée ajouté par un projet (ex. normal map, repli bleu plat) dont l'élément est une `Texture` aurait un élément `Texture`/`error` : il prendrait en silence le damier. Et un projet dont le repli doit être prêt avant tout dessin doit modifier `GLResources`.
- `GLResources` porte aussi `TexturePlaceholders` (1×1 transparent ; damier en `GL_TEXTURE_RECTANGLE` créé à la demande ; `delete` à la fin) et appelle `TextureUploader.releaseBuffers()` quand les files sont vides et à la libération.
- `resources/MultiSourceDataType` : chemins fixes, `validPaths`, `sourcePaths()`/`hasSource()` finaux, état par fichier `sourceLoaded` / `isPartLoaded(i)` / `setAllSourceUnloaded()` jamais lu ; aucun utilisateur, seule trace : la branche de `ResourceLoader.read`.
- Loaders du registre : `registry.resources().add(loader)`, clé = classe de donnée ; loaders optionnels de la bibliothèque dans `LibraryLoaders.all()`, ajoutés un à un par `DemoPlugin`. Découverte de fichiers : `ResourceLoader.discover` (`TextureDataLoader.discover` en modèle).
- Commandes : `utils/enginecommand/general/GLGenCom` (`gl summary`), `resources`.
- Tests : `GLResourcesTest` (éléments factices, files, échecs, `addElementAndLoadNow`), `Resources*Test` (base `ResourcesTestBase`, `FakeLoader` : échecs et repli), `PublicFieldsTest`, `ExtensionFieldsTest`.
- Ailleurs : Bevy a un repli par type de ressource du rendu (`FallbackImage`, une image par dimension) que le système central ne connaît pas ; Unreal arrête le moteur si le matériau par défaut ne compile pas, faute de repli pour les autres (même règle que notre programme de repli).

## Décisions
- **Déclaration** : `DataType.glElementNeededBeforeDrawing()`, `false` par défaut ; `FallbackShaderData` → `true`. Seule une donnée de repli s'en sert.
- **Liste des replis** : `IGLElementSink.fallbackDataLoaded(Collection<DataType> fallbacks)` reçoit les données de repli de `FallbackRegistry` (pas de nouvel accès public à `Resources`).
- **Enregistrement** : pour chaque repli avec `glElementType() != null`, `GLResources` crée l'élément, le range dans une table *type de donnée du loader → élément* (hors table par nom), lui garde une référence urgente tout le programme ; chargé pendant l'appel si la donnée le demande, sinon mis en file.
- **Échec** : un élément « avant tout dessin » qui échoue → `IllegalStateException` (arrêt, comme aujourd'hui, aussi en release) ; un élément envoyé aux frames suivantes qui échoue → une ligne, élément en échec, ses utilisateurs gardent le placeholder.
- **Accès** : `GLResources.fallback(Class<? extends DataType> type)` rend l'élément de repli du type ou de son plus proche parent avec repli, `null` sinon (même règle que `FallbackRegistry.get`). `acquire` d'une donnée de repli rend cet élément. Remplace `fallbackShader()` et la recherche de `Texture`/`error`.
- `Renderer` garde en cache son programme de repli (il ne change plus après le démarrage) : pas de recherche par dessin.
- **État partagé par type d'élément** : interface `IGLSharedState` (`delete()`, `default void loadingIdle() {}` appelé quand les files sont vides), `GLResources.shared(Class<T> type, Supplier<T> factory)` (créé à la première demande, un par classe, tous libérés à la libération de `GLResources`). `TexturePlaceholders` en devient un ; la libération des buffers de `TextureUploader` passe par son `loadingIdle`/`delete`. `GLResources` n'importe plus rien de `graphics.shader` ni de `resources.texture`.
- **`MultiSourceDataType`** : chemins en liste immuable ; état par fichier remis à zéro par `ResourceLoader.read` avant chaque lecture, marqué par le loader fichier par fichier ; un échec nomme les fichiers non lus ; `resources` (JSON de `ResourceDescriber`) montre l'état de chaque fichier. `hasSource`/`sourcePaths` inchangés (observateur de fichiers : un fichier modifié recharge la donnée entière).
- **`gl summary`** : champ `fallbacks` dans les `data` (type de donnée, type d'élément, prêt avant tout dessin, état) ; le `message` donne leur nombre et ceux en échec.
- **Cubemap** (bibliothèque, loader optionnel de `LibraryLoaders.all()`) : `resources/cubemaps/<nom>/right|left|top|bottom|front|back.png` (ordre de `GL_TEXTURE_CUBE_MAP_POSITIVE_X` à `NEGATIVE_Z`), découverts sans JSON ; donnée `CubeMapData extends MultiSourceDataType` (6 fichiers, décodés par STB comme les textures) ; élément `CubeMapTexture` (`GLElement`, une face envoyée par étape, `GenDeleteResources`) ; repli `FallbackCubeMapData` (6 damiers) avec son élément ; placeholder transparent par l'état partagé (second utilisateur du mécanisme).
- **Skybox** : `SkyboxRenderer` (cube, programme `skybox`, tag 3D) dessiné en premier, sans test ni écriture de profondeur, culling désactivé ; le shader ne garde que la rotation de la vue (`mat4(mat3(view))`) ; il lie lui-même sa `CubeMapTexture` (pas une `Texture`).

## Étapes (un commit chacune)
1. **`MultiSourceDataType` : état par fichier utile** — `resources/MultiSourceDataType.java`, `ResourceLoader.read`, `ResourceDescriber`. Liste immuable, remise à zéro avant lecture, échec nommant les fichiers non lus, état par fichier dans `resources`. Tests (`test/.../resources/`, loader factice multi-fichiers sur `ResourcesTestBase`) : `sourcePaths`/`hasSource`, lecture sautée si un chemin est vide, état remis à zéro à la relecture, message d'échec. Fusionnable seule.
2. **État partagé par type d'élément** — `graphics/IGLSharedState.java` (nouveau), `GLResources` (`shared`, `loadingIdle` quand les files sont vides, `delete` à la libération), `TexturePlaceholders`, `TextureUploader`, `Texture`. Tests `GLResourcesTest` : créé une fois par classe, `loadingIdle` appelé quand les files se vident, `delete` à la libération. Vérif en jeu : `scene Scene2D`, `wait-loaded`, `?gl summary` ; `leakcheck Scene2D`. Fusionnable seule.
3. **Repli déclaré par la donnée** — `DataType`, `FallbackShaderData`, `IGLElementSink`, `FallbackRegistry`/`Resources.finishLoading`, `GLResources` (table des replis, `fallback(type)`, `acquire` d'un repli, suppression de `fallbackShader()` et des imports shader/texture), `Renderer` (cache), `Texture` (`getFallbackId`, rectangle). Tests `GLResourcesTest` avec données factices : chargé pendant l'appel ou mis en file selon la déclaration, échec « avant tout dessin » → exception, échec différé → élément en échec, règle du parent, deux replis d'un même type d'élément sans collision, `acquire` d'un repli rend son élément. Vérif en jeu : `scene FallbackScene` (`?gl summary`, damier et magenta inchangés), `scene Scene2D`, `scene Scene3D`.
4. **`gl summary` liste les replis** — `GLGenCom`, syntaxe et description à jour. Vérif : `python tools/scenario.py "scene Scene2D" wait-loaded "?gl summary"` montre `fallbacks` (texture, shader).
5. **Démo 2D : texture en calques avec son propre repli** — dans `mightylibdemo` : `LayeredTextureData extends MultiSourceDataType` (calques `resources/layered/<nom>/*.png` triés par nom, superposés en une image ; élément : une `Texture` sur une `TextureData` possédée, modèle de `TileMap` et de son atlas), son loader (`DemoPlugin`), son repli (rayures bleues, élément à lui : montre qu'il ne prend pas le damier) ; images générées par le script de l'étape 7 (ou le sien) ; nouvelle scène `LayeredTexture2D` inscrite dans `DemoScenes` : personnage en calques, nom de calques inconnu (rayures bleues), texture inconnue (damier). Tests : découverte et ordre des calques, état par fichier (calque illisible nommé). Vérif : `scene LayeredTexture2D`, `?gl summary`, `?resources LayeredTextureData`, `reload LayeredTextureData <nom>` après modification d'un calque, `leakcheck LayeredTexture2D` ; une capture cadrée si le rendu ne se vérifie pas autrement.
6. **Cubemap** — `resources/cubemap/` (`CubeMapData`, `CubeMapLoader` avec `discover`, `FallbackCubeMapData`, `CubeMapTexture`, placeholder par état partagé), `LibraryLoaders.all()`, `DemoPlugin`. Tests : découverte des 6 faces, ordre des faces, face manquante → échec qui la nomme, repli présent. Vérif : `?resources CubeMapData`, `?gl summary` (repli cubemap listé).
7. **Skybox dans `TerrainGeneration3D`** — `SkyboxRenderer`, shaders `skybox` dans `resources/shaders/3_30` et `1_40` (+ `shaders.json` des deux), script `tools/generate_skybox.py` (versionné : régénère les images ; dégradé de ciel + repère lisible par face pour vérifier l'orientation) et images `resources/cubemaps/sky/`, `TerrainGeneration3D` (créé au `launch`, libéré dans `unload`). Vérif : `scene TerrainGeneration3D`, `wait-loaded`, `?gl summary`, `leakcheck TerrainGeneration3D`, une capture cadrée (horizon, faces dans le bon ordre) ; `reload CubeMapData sky` après modification d'une face. Les autres scènes 3D (`Scene3D`, `Scene3DBis`, `Game3D2D`) restent intactes.
8. **Documents** (liste ci-dessous), sans supprimer ce plan.

## Démo
- Nouvelle scène `LayeredTexture2D` (étape 5), inscrite dans `DemoScenes` avec sa description.
- Skybox ajoutée à `TerrainGeneration3D` (demande explicite de l'utilisateur, exception à la règle « nouvelle scène »).
- `FallbackScene` inchangée (damier et magenta).

## Documents à mettre à jour à la fin
- `architecture.md` : Placeholders (élément de repli par type de donnée, déclaration « avant tout dessin », `fallback(type)`), GLResources (état partagé par type d'élément, plus aucun type connu), `Shader` (repli lu par `fallback(ShaderData.class)`), `MultiSourceDataType` (état par fichier), cubemaps (dossier, faces, repli), skybox.
- `debug-tools.md` : `gl summary` (`fallbacks`), `resources` (état par fichier).
- `decisions.md` : repli GL déclaré par la donnée et retrouvé par type de donnée (pourquoi : extensibilité, pas de collision `error`).
- `later.md` : rechargement partiel d'une donnée multi-fichiers (seulement le fichier changé) ; données composées de dépendances pour la planche de sprites (image + JSON, Aseprite/TexturePacker) et le matériau (albedo + normal + rugosité), futurs utilisateurs à côté de `MultiSourceDataType`.
- `techniquegraphique.md` : retirer la skybox (garder les cubemaps de reflets et réfraction).
- `licence.md` : images de la skybox et des calques générées par le projet (`tools/generate_skybox.py`), aucune licence tierce.
- `CLAUDE.md` : tests couverts (état partagé et replis de `GLResources`, `MultiSourceDataType`, cubemaps, calques de la démo).
- `current.md` : supprimer la ligne « Repli GL déclaré par chaque élément ».
- Supprimer ce plan (discussion B).

## Hors périmètre
- Rechargement partiel d'une donnée multi-fichiers → `later.md`.
- Cubemaps de reflets et réfraction, cubemaps de profondeur → `techniquegraphique.md`.
- Planche de sprites et matériau en données composées → `later.md`.
- Conversion d'une image équirectangulaire (HDR) en cubemap → pas noté (à ajouter à `techniquegraphique.md` si l'utilisateur le souhaite).

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1 à 3 : Opus 5.5, effort `high` (modèle durable des replis et de l'état partagé, `GLResources` délicat).
- Étapes 4 et 5 : Opus 5.5, effort `medium` (commande, démo multi-fichiers sur un modèle existant).
- Étapes 6 et 7 : Opus 5.5, effort `high` (élément GL nouveau, orientation des faces, GLSL 330 et 140).
- Étape 8 : Sonnet 5.5, effort `medium` (docs).
- Discussion B : Opus 5.5, effort `high` (gros diff sur plusieurs couches à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/repli-gl-declare.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `repli-gl-declare`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`FallbackScene` : damier et magenta ; `LayeredTexture2D` : personnage, rayures bleues, damier, rechargement d'un calque ; `TerrainGeneration3D` : ciel, orientation des faces en tournant la caméra ; `Scene3D`, `Scene3DBis`, `Game3D2D` inchangées ; lancement en GLSL 1_40 si possible). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml repli-gl-declare`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
