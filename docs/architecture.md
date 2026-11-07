# Architecture — Java-MightyLibrary

Structure, concepts clés du moteur et pièges connus.

## Structure

```
src/io/github/mightycode/
  mightylib/            # la bibliothèque (ne doit PAS dépendre de mightylibdemo/)
    main/               # MainLoop (statique), Window, Context/ContextManager, MainThread, Workers, GameTime
    scenes/             # Scene, SceneManager, SceneManagerInterface, TemplateSceneLoading, caméras 2D/3D
    resources/          # Resources (singleton, pipeline de chargement), DataType, ResourceLoader, batchs
    graphics/           # GLResources, GLElement, renderer/ (Shape, Renderer, _2D, _3D), shader/, text/, GUI/, lightning/
    inputs/             # InputManager (actions), KeyboardManager, MouseManager, dispositions clavier
    sounds/             # SoundManager (singleton OpenAL), arbre de gains
    physics/            # collisions maison, tweenings, B2Utils (pont JBox2D)
    algorithms/         # A*, Dijkstra, graphes, Wave Function Collapse
    utils/              # commandes moteur/terminal, JSON, maths, Logger
  mightylibdemo/        # jeu de démo : Main, ProjectLoading, ActionId, scènes Test*
resources/              # assets ; batchs/*.batch décrit les groupes de ressources
```

## Concepts clés

### Démarrage
`Main` remplit un `EngineConfig` (setters enchaînables avec valeurs par défaut ; seule la scène de départ est obligatoire : taille fenêtre, FPS/TPS, méthode de chargement, nb de threads…) → `MainLoop.run(config)` :
GLFW + son → contexte `"Main"` (fenêtre + inputs) → `ShaderManager.init` → `Resources.createInstance` → `GLResources.createInstance` → `IProjectLoading.init` (ajout des loaders projet) → `contextLoading` (bindings d'input) → `resources.init()` puis `resources.load()` (bloquant) → première scène.

### Boucle principale (`MainLoop.run`)
Pas fixe découplé : chaque itération fait **soit** un tick (`sceneManager.update()` + `postUpdate()`), **soit** une frame (`display()` + `window.swapBuffersAndPollEvents()`). Les ticks en retard sont abandonnés (pas de rattrapage). `GameTime.DeltaTime()` = temps réel mesuré entre deux ticks.
La boucle s'arrête quand la fenêtre est fermée ou qu'une scène demande à quitter (`sceneManagerInterface.exit(status)`, fin du tick en cours). `run` libère tout et **renvoie** le code de sortie (`ListError`) sans appeler `System.exit` : c'est l'application qui décide (`Main` fait `System.exit(MainLoop.run(config))`).

### Threads
- **Thread principal** (`MainThread`) : seul à posséder le contexte GL. Les autres threads y reviennent par `MainThread.execute(Runnable)` / `run` / `supply` (→ `CompletableFuture`), ou `future.thenAcceptAsync(…, MainThread.EXECUTOR)`. La file est vidée après chaque `display()` avec le reste de la frame (au moins une tâche), avant `GLResources.process`, puis entièrement à la sortie.
- **`Workers`** : pool fixe de `EngineConfig.maxThreads` threads (`MightyLibrary-Worker-N`, daemon), arrêté à la sortie, pour les tâches courtes d'I/O ou de calcul (`Workers.run` / `supply` → `CompletableFuture`), **sans appel GL**. Les étapes `THREAD_CONTEXT` de `Resources` y tournent (au plus `maxThreads` à la fois).
- Boucles longues ou bloquantes (réseau, `DebugServer`, console, encodage de `Screenshot`) : threads dédiés, nommés `MightyLibrary-…` et daemon.
- Avec `Logger.DO_CHECK`, `GenDeleteResources` lève une exception si un objet GL est créé ou détruit hors du thread principal.

### Cycle de vie d'une scène
Ordre : `init(args)` → (scène de chargement tant que les batchs chargent) → `launch(args)` → boucle `update()` / `postUpdate()` / `display()` → `unload()` → `release()`.
- Exemple de référence commenté méthode par méthode : `mightylibdemo/scenes/DocumentationScene` (batch `documentation`, configuration `resources/data/documentationScene.json`, texture générée dans `init` et supprimée dans `release`).
- Paires symétriques : `launch` ↔ `unload` (appelé seulement si `launch` l'a été, avec `super.unload()`), `init` ↔ `release` (toujours appelé, même si la scène est quittée pendant son chargement ; libère ce que `init` a ajouté, ex. `deleteResource`).
- `init` et `launch` sont facultatifs et **sans appel `super`** : le moteur (`initByManager` / `launchByManager`) crée la scène de chargement, puis, une fois les ressources chargées, les caméras et le framebuffer virtuel avant d'appeler `launch`. `init` déclare (ressources préchargées), `launch` crée les renderers.
- Scène de chargement : `EngineConfig.setLoadingScene(...)` pour tout le jeu (`DefaultLoadingScene`, fond gris sans ressource, par défaut), ou surcharge de `createLoadingScene(args)` dans une scène (`null` = rien d'affiché). Surface du framebuffer virtuel : surcharge de `createVirtualSceneBindable()`.
- Les ressources nécessaires se déclarent en surchargeant `getInvolvedBatch()` (noms de fichiers dans `resources/batchs/`).
- `display()` typique : `setVirtualScene(); clear(); …draw…; setAndDisplayRealScene();`
- Changer de scène : `sceneManagerInterface.setNewScene(new XScene(), args)`.
- `postUpdate()` = fin de tick (reset des états d'input « pressed/released »), `unload()` = libération des ressources.

### Ressources (CPU) — `Resources`
- Chaque type est un `DataType` avec un `ResourceLoader` (enregistré dans `Resources.Loaders`; loaders projet dans `ProjectLoading.init`).
- États : créé → **preload** (`initWithFile` : lecture disque, déclaration des dépendances) → **load** (`internLoad` : calcul, une fois les dépendances chargées) → unload (`internUnload`, ou `internCancelPreload` si seulement préchargée).
- `TYPE_SET_UP` = où tournent preload et load : `THREAD_CONTEXT` (workers, au plus `maxThreads` à la fois), `MAIN_CONTEXT` (thread principal, dans le budget de `process(ms)`), `IMMEDIATELY_IN_CURRENT_CONTEXT` (tout de suite, dans l'appel qui la demande ; ex. descriptions de batchs). L'unload tourne toujours sur le thread principal.
- **Propriétaire unique** : seul le thread principal modifie références et états. Les appels venant d'un autre thread (`addOwnedResource` depuis un loader, `addPreLoadedResource` depuis un `internLoad`) passent par une file vidée par `process`. Une étape de worker ne fait que lire/calculer la donnée ; son résultat est appliqué sur le thread principal.
- **Réconciliation** (`Resources.update`) : voulue = référencée (sauf suppression ou sortie). Une donnée dont une étape tourne n'est jamais modifiée ; à la fin de l'étape, la comparaison voulue/état est refaite (déchargée si plus référencée, étape suivante sinon). Une donnée qui attend ses dépendances s'inscrit auprès d'elles et est réveillée quand l'une est chargée. Un échec n'est pas retenté tant que la donnée reste voulue.
- Références par nom (batch, `"manually"`, `"all"` pour `loadAllOfType`, propriétaire pour les ressources possédées) ; une liste, un même nom peut apparaître plusieurs fois.
- `reloadAllOfType` relit depuis le disque les données chargées (sauf celles préchargées par le code) : la donnée est vidée pendant la relecture (voir `current.md`).
- Commande `resources` : par type, chargées/préchargées/référencées, données chargées sans référence (fuite) ou référencées non chargées, état du pipeline.
- Tests : `test/.../resources/ResourcesLifecycleTest` (cycles avec des `FakeData` sans fichier ni GL).
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
