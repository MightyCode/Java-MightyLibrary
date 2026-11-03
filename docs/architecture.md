# Architecture — Java-MightyLibrary

Structure, concepts clés du moteur et pièges connus.

## Structure

```
src/MightyLibrary/
  mightylib/            # la bibliothèque (ne doit PAS dépendre de project/)
    main/               # MainLoop (statique), Window, Context/ContextManager, ThreadManager, GameTime
    scenes/             # Scene, SceneManager, SceneManagerInterface, LoadingScene, caméras 2D/3D
    resources/          # Resources (singleton), DataType, ResourceLoader, ResourceWorker, batchs
    graphics/           # GLResources, GLElement, renderer/ (Shape, Renderer, _2D, _3D), shader/, text/, GUI/, lightning/
    inputs/             # InputManager (actions), KeyboardManager, MouseManager, dispositions clavier
    sounds/             # SoundManager (singleton OpenAL), arbre de gains
    physics/            # collisions maison, tweenings, B2Utils (pont JBox2D)
    algorithms/         # A*, Dijkstra, graphes, Wave Function Collapse
    utils/              # commandes moteur/terminal, JSON, maths, Logger
  project/              # jeu de démo : Main, ProjectLoading, ActionId, scènes Test*
resources/              # assets ; batchs/*.batch décrit les groupes de ressources
```

## Concepts clés

### Démarrage
`Main` remplit un `EngineConfig` (setters enchaînables avec valeurs par défaut ; seule la scène de départ est obligatoire : taille fenêtre, FPS/TPS, méthode de chargement, nb de threads…) → `MainLoop.run(config)` :
GLFW + son → contexte `"Main"` (fenêtre + inputs) → `ShaderManager.init` → `Resources.createInstance` → `GLResources.createInstance` → `IProjectLoading.init` (ajout des loaders projet) → `contextLoading` (bindings d'input) → `resources.init()` puis `resources.load()` (bloquant) → première scène.

### Boucle principale (`MainLoop.run`)
Pas fixe découplé : chaque itération fait **soit** un tick (`sceneManager.update()` + `postUpdate()`), **soit** une frame (`display()` + `window.swapBuffersAndPollEvents()`). Les ticks en retard sont abandonnés (pas de rattrapage). `GameTime.DeltaTime()` = temps réel mesuré entre deux ticks.
La boucle s'arrête quand la fenêtre est fermée ou qu'une scène demande à quitter (`sceneManagerInterface.exit(status)`, fin du tick en cours). `run` libère tout et **renvoie** le code de sortie (`ListError`) sans appeler `System.exit` : c'est l'application qui décide (`Main` fait `System.exit(MainLoop.run(config))`).

### Cycle de vie d'une scène
Ordre : `init(args)` → (scène de chargement tant que les batchs chargent) → `launch(args)` → boucle `update()` / `postUpdate()` / `display()` → `unload()`.
- `init` et `launch` sont facultatifs et **sans appel `super`** : le moteur (`initByManager` / `launchByManager`) crée la scène de chargement, puis, une fois les ressources chargées, les caméras et le framebuffer virtuel avant d'appeler `launch`. `init` déclare (ressources préchargées), `launch` crée les renderers.
- Scène de chargement : `EngineConfig.setLoadingScene(...)` pour tout le jeu (`DefaultLoadingScene`, fond gris sans ressource, par défaut), ou surcharge de `createLoadingScene(args)` dans une scène (`null` = rien d'affiché). Surface du framebuffer virtuel : surcharge de `createVirtualSceneBindable()`.
- Les ressources nécessaires se déclarent en surchargeant `getInvolvedBatch()` (noms de fichiers dans `resources/batchs/`).
- `display()` typique : `setVirtualScene(); clear(); …draw…; setAndDisplayRealScene();`
- Changer de scène : `sceneManagerInterface.setNewScene(new XScene(), args)`.
- `postUpdate()` = fin de tick (reset des états d'input « pressed/released »), `unload()` = libération des ressources.

### Ressources (CPU) — `Resources`
- Chaque type est un `DataType` avec un `ResourceLoader` (enregistré dans `Resources.Loaders`; loaders projet dans `ProjectLoading.init`).
- États : créé → **preload** (lecture disque, déclaration des dépendances) → **load** (finalisation, souvent GL) → unload.
- `TYPE_SET_UP` : `THREAD_CONTEXT` (workers), `MAIN_CONTEXT` (file traitée par `process(ms)` sur le thread GL), `IMMEDIATELY_IN_CURRENT_CONTEXT`.
- Références par nom de batch (liste, un même nom peut apparaître plusieurs fois). Les maps de ressources sont concurrentes et les états de chargement `volatile` : les workers et le thread principal y accèdent.
- Le nom d'une ressource = nom de fichier sans extension → **les noms doivent être uniques par type**.
- Accès : `Resources.getInstance().getResource(TextureData.class, "nom")`.

### Ressources GPU — `GLResources` / `GLElement`
- Objets GL partagés et comptés par référence (ex. `Texture`). `addElementOrReturnIfPresent()` ajoute une référence (élément existant ou nouveau) et doit être équilibré par un `releaseResource()`. `getResource()` est une simple recherche, sans référence.
- **Jamais bloquant** : `addElementOrReturnIfPresent()` ne fait que mettre en file. Après chaque `display()`, `MainLoop` appelle `GLResources.process(deadline)` avec le **reste de la frame** (jusqu'au prochain tick ou à la prochaine frame), et toujours **au moins une étape**, même si le temps est dépassé ; plafond facultatif `EngineConfig.setMaxGLUploadMillisecondsPerFrame`. `GLElement.load(remainingMilliseconds)` renvoie `false` tant qu'il n'a pas fini : il reprend à la frame suivante.
- `Texture` : les petites textures sont envoyées d'un coup ; au-delà de 4 Mio (2D), par bandes `glTexSubImage2D` dimensionnées d'après le débit mesuré et le temps restant (256 Kio à 4 Mio par bande). Tant qu'une texture n'est pas chargée, son `bind` utilise une texture transparente 1×1 ; des données introuvables sont remplacées par la texture `error`.
- Chargement d'une scène : l'écran de chargement reste affiché pendant le chargement des ressources, puis pendant l'envoi GL de ce que `launch()` a demandé ; la scène n'est affichée et mise à jour qu'une fois `GLResources` vide (avertissement après 10 s avec les éléments en attente).
- Renderers 2D (`Shape2DRenderer` et dérivés) : utilisables dès le constructeur, la géométrie est envoyée au premier `display()` puis à chaque `display()` qui suit une modification.
- `GenDeleteResources` enveloppe toutes les créations/destructions GL (VAO, VBO, EBO, textures, FBO, RBO) pour traquer les fuites (`PrintAll()` au changement de scène). **Toujours passer par lui**, jamais `glGen*`/`glDelete*` directement.

### Rendu
- `Shape` = VAO + VBOs (+ EBO) + `Shader` ; canaux : 0 position, 1 UV, 2 normales.
- `Renderer` = `Shape` + transform (position/scale/rotation → matrice `model`) + map de `ShaderValue` envoyées à chaque `display()`.
- `Shader.sendValueToShader` met en cache la dernière valeur par uniform et ne renvoie que si elle a changé.
- Uniforms génériques : `projection`, `view`, `model`, `color` (constantes dans `ShaderManager`). Les shaders sont listés dans `resources/shaders/<3_30|1_40>/shaders.json` ; **un nouveau shader doit exister dans les deux versions**.
- Un 2D/3D est déterminé par le shader (`isDimension2DShader`) et active/désactive le depth test à chaque draw.

### Inputs
- Les IDs d'action sont des `int` alloués statiquement via `InputManager.getAndIncrementId()` (0 et 1 réservés : `COMMAND`, `RELOAD_TEXTURE`). Projet : `ActionId` + bindings dans `ProjectLoading.contextLoading`.
- La disposition clavier vient de `EngineConfig.setKeyboardLanguage()` (QWERTY par défaut, et si un contexte est créé sans `WindowCreationInfo`).

### Temps
- `GameTime.DeltaTime()` : temps réel mesuré entre deux ticks (variable). `GameTime.FixedDeltaTime()` : durée fixe d'un tick (1/TPS), **à utiliser pour la physique** (`world.step` de JBox2D).

## Pièges connus (à garder en tête, non corrigés)

- Recharger une `TextureData` relit les pixels mais ne met pas à jour les `Texture` GL déjà créées à partir d'elle : le rechargement à chaud ne se voit pas à l'écran.
- `Resources.getInstance()` crée silencieusement une instance par défaut s'il est appelé avant `createInstance`.

### Spécifiques 2D
- Pas de batching : chaque sprite/forme (`Shape2DRenderer`, `Animation2DRenderer`, `Text`) a son propre VAO/VBO/EBO et son draw call, avec `glUseProgram` + uniforms + toggle du depth test à chaque dessin.
- `Shape.updateVbo` réutilise le buffer (`glBufferSubData`) si la taille ne change pas, sinon il réalloue : préférer des tableaux de taille constante pour les données mises à jour souvent. Les `calculate*()` allouent encore un nouveau `float[]` à chaque appel.
- `TileMapRenderer` : une animation de tuile ne réécrit que les UV des tuiles concernées ; une modification de la carte (`TileMap.hasBeenUpdated()`) reconstruit toujours tout.
- `Shape2DRenderer` alloue ses VBO en `STATIC_STORE` même quand ils sont mis à jour à l'exécution.
- La boucle principale abandonne les ticks en retard : sous forte charge le jeu ralentit au lieu de rattraper (la physique à pas fixe reste stable, mais le temps de jeu dérive du temps réel).
