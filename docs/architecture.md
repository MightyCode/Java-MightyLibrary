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
- **Réconciliation** (`Resources.update`) : voulue = référencée (sauf suppression ou sortie). Une donnée dont une étape tourne n'est jamais modifiée ; à la fin de l'étape, la comparaison voulue/état est refaite (déchargée si plus référencée, étape suivante sinon). Une donnée voulue et préchargée référence ses dépendances (`dependency:<Type>:<nom>`) : un batch n'a pas à les lister, et elles restent chargées tant que la donnée l'est (commande `resources` : `onlyAsDependency` liste celles qui ne sont voulues que par là). Elle s'inscrit auprès des dépendances non chargées et est réveillée quand l'une est chargée. Un échec (`Resources.fail`, un loader qui lève une exception, un `internLoad` qui renvoie `false`) ramène la donnée à l'état initial et rend ses dépendances ; il n'est pas retenté tant que la donnée reste voulue, sauf par `reloadAllOfType`. Ceux qui l'attendent continuent avec son placeholder si son type en a un, sinon échouent aussi, en chaîne.
- **Placeholders** (`ResourceLoader.createFallback`, nommés `error`, générés par le code, chargés au démarrage, jamais déchargés ni rechargés ; un placeholder qui échoue lève une exception) : types primaires seulement — texture (damier magenta/noir, plus une version rectangle), son (bip strident), shader (magenta uni), police (des carrés), animation (une image), modèle (un cube). Types complexes (JSON, CSV, tilemaps, tilesets, batchs…) : pas de placeholder, `null` et plantage accepté. `Resources.usable(type, donnée)` donne la donnée si chargée, sinon le placeholder (échec **ou chargement en cours**) ; `getResourceOrFallback` remplace un nom inconnu (signalé une fois). Les utilisateurs (`Texture`, `SoundSource`, `Shape`, `Text`, `Animation`, `ModelRenderer`) résolvent à l'usage : une donnée chargée plus tard ou relue avec succès remplace le placeholder. Le placeholder est signalé une fois, à l'échec ; la commande `resources` liste par type `usingFallback`.
- **Mode strict** (`EngineConfig.setStrictResources`, `-Dmightylib.resources.strict=true`, `scenario.py --strict`, mode debug seulement) : tout échec ou nom inconnu lève une exception au lieu d'utiliser le placeholder.
- Références par nom (batch, `"manually"`, `"all"` pour `loadAllOfType`, propriétaire pour les ressources possédées) ; une liste, un même nom peut apparaître plusieurs fois.
- **Rechargement à chaud** (`Resources.reload(donnée)`, `reloadAllOfType`, commande `reload`) :
  - une donnée **chargée** n'est jamais vidée : `DataType.createReloadCopy` crée une copie (même type, nom, source) que le pipeline lit et charge à part (`resources` la liste dans `reloading`) ; une fois chargée, `swapContent` échange les contenus, la donnée garde son identité (les références restent valides) et la copie décharge l'ancien contenu. Un **échec garde la version précédente** (exception en mode strict) ;
  - `DataType.getVersion()` augmente à chaque changement de contenu : les utilisateurs comparent la version qu'ils ont utilisée (`Texture`, `TileMapRenderer`, `Text`, `ModelRenderer`), sans liste d'écouteurs ;
  - les données chargées qui dépendent d'une donnée changée (`dependents`, tenus par `acquireDependencies`) sont relues à leur tour, en chaîne (texture → tileset → tilemap, atlas → police) ; une donnée attend que ses dépendances aient fini leur relecture (`dependencyReady`), donc chacune n'est calculée qu'une fois lors d'un `reload all` ;
  - pas relues : données sans source (`hasSource()` faux : générées par le code), préchargées par le code, placeholders ; elles sont seulement recalculées (sauf placeholders) quand une dépendance change (atlas construit par le code). Une donnée **en échec** ou en cours de premier chargement est relue sur place ;
  - un type sans copie (`createReloadCopy` → `null`, ex. `NetworkConfiguration`, `WaveCollapseRules`, types de mods qui ne l'implémentent pas) est vidé puis relu sur place (signalé une fois). Un sous-type n'hérite pas de la copie de son parent (il doit donner la sienne) ;
  - un nouveau type de donnée : implémenter `createReloadCopy` (copier aussi les réglages donnés par le code, ex. `TextureData.copySettingsFrom`) et `swapContent` (échanger, pas déplacer, tout ce que lisent `initWithFile` et `internLoad`).
- Commande `resources` : par type, chargées/préchargées/référencées, données chargées sans référence (fuite) ou référencées non chargées, état du pipeline.
- Tests : `test/.../resources/ResourcesLifecycleTest` (cycles avec des `FakeData` sans fichier ni GL).
- Le nom d'une ressource = nom de fichier sans extension → **les noms doivent être uniques par type**.
- Accès : `Resources.getInstance().getResource(TextureData.class, "nom")`.

### Ressources GPU — `GLResources` / `GLElement`
- Objets GL partagés et comptés par référence (ex. `Texture`). `addElementOrReturnIfPresent()` ajoute une référence (élément existant ou nouveau) et doit être équilibré par un `releaseResource()`. `getResource()` est une simple recherche, sans référence.
- **Jamais bloquant** : `addElementOrReturnIfPresent()` ne fait que mettre en file. Après chaque `display()`, `MainLoop` appelle `GLResources.process(deadline)` avec le **reste de la frame** (jusqu'au prochain tick ou à la prochaine frame), et toujours **au moins une étape**, même si le temps est dépassé ; plafond facultatif `EngineConfig.setMaxGLUploadMillisecondsPerFrame`. `GLElement.load(remainingMilliseconds)` renvoie `false` tant qu'il n'a pas fini : il reprend à la frame suivante.
- `Texture` : les petites textures sont envoyées d'un coup ; au-delà de 4 Mio (2D), par bandes `glTexSubImage2D` dimensionnées d'après le débit mesuré et le temps restant (256 Kio à 4 Mio par bande). Tant qu'une texture n'est pas chargée (en cours ou en échec), son `bind` utilise le placeholder (de même type, 2D ou rectangle ; transparent 1×1 tant que le placeholder lui-même n'est pas envoyé) ; `hasElementsToLoad` ignore les éléments dont une donnée a échoué (pas d'écran de chargement infini). Quand sa donnée est rechargée (version différente au `bind`), la texture est renvoyée par `GLResources` (`GLElement.requestReload`/`reload`, file à part dans le même budget) dans une seconde texture GL, qui remplace l'actuelle une fois complète : l'ancienne version reste affichée pendant l'envoi.
- L'atlas d'une `TileMap` (`TileSetAtlas`, texture GL `tilemap:<carte>`) est un contenu de la carte, construit dans son `internLoad`, pas une ressource ; au rechargement, son contenu est échangé et les objets restent ceux des renderers.
- Un son rechargé : `SoundData.internUnload` détache le buffer des sources (`SoundManager.DetachBuffer`) avant de le supprimer (OpenAL refuse sinon).
- Chargement d'une scène : l'écran de chargement reste affiché pendant le chargement des ressources, puis pendant l'envoi GL de ce que `launch()` a demandé ; la scène n'est affichée et mise à jour qu'une fois `GLResources` vide (avertissement après 10 s avec les éléments en attente).
- Renderers 2D (`Shape2DRenderer` et dérivés) : utilisables dès le constructeur, la géométrie est envoyée au premier `display()` puis à chaque `display()` qui suit une modification.
- `GenDeleteResources` enveloppe toutes les créations/destructions GL (VAO, VBO, EBO, textures, FBO, RBO) pour traquer les fuites (`PrintAll()` au changement de scène). **Toujours passer par lui**, jamais `glGen*`/`glDelete*` directement.

### Rendu
- `Shape` = VAO + VBOs (+ EBO) + `Shader` ; canaux : 0 position, 1 UV, 2 normales.
- `Renderer` = `Shape` + transform (position/scale/rotation → matrice `model`) + map de `ShaderValue` envoyées à chaque `display()`.
- `Shader.sendValueToShader` met en cache la dernière valeur par uniform et ne renvoie que si elle a changé. Un uniform absent du programme (retiré par le compilateur GLSL, ou absent du placeholder) est signalé une fois et ignoré.
- Uniforms génériques : `projection`, `view`, `model`, `color` (constantes dans `ShaderManager`). Les shaders sont listés dans `resources/shaders/<3_30|1_40>/shaders.json` ; **un nouveau shader doit exister dans les deux versions**.
- Un 2D/3D est déterminé par le shader (`isDimension2DShader`) et active/désactive le depth test à chaque draw.

### Inputs
- Les IDs d'action sont des `int` alloués statiquement via `InputManager.getAndIncrementId()` (0 et 1 réservés : `COMMAND`, `RELOAD_TEXTURE`). Projet : `ActionId` + bindings dans `ProjectLoading.contextLoading`.
- La disposition clavier vient de `EngineConfig.setKeyboardLanguage()` (QWERTY par défaut, et si un contexte est créé sans `WindowCreationInfo`).
- **Manettes** : un seul `GamepadManager` pour tous les contextes (`ContextManager.getGamepadManager()`, aussi `InputManager.getGamepadManager()`), mis à jour par `ContextManager.postUpdate` avant les contextes. Lecture GLFW (`glfwGetGamepadState` : disposition Xbox standard quel que soit le modèle, base SDL_GameControllerDB intégrée ; `addMappings` pour une manette inconnue) derrière `IGamepadSource`, remplacée par une fausse source dans les tests.
  - Emplacements : un par id de joystick GLFW (0 à 15), puis 8 manettes virtuelles (16+) pour les tests et le multijoueur avec une seule manette. Connexions (rappel `glfwSetJoystickCallback`, ou lecture échouée) appliquées dans `postUpdate` : un tick voit un ensemble stable ; `connectedThisTick()`/`disconnectedThisTick()` durent un tick.
  - Joueurs : une manette connectée prend le plus petit joueur libre, et reprend le sien en se reconnectant s'il est encore libre ; `setAutoAssign(false)` + `assignPlayer` pour un écran « appuyez pour rejoindre ». Un joystick sans mapping est connecté mais sans joueur.
  - `Gamepad` : boutons/axes par ids GLFW (`GLFW_GAMEPAD_BUTTON_A`, `GLFW_GAMEPAD_AXIS_LEFT_X`…) ; sticks de -1 à 1 (y positif vers le bas, comme la fenêtre) avec zone morte radiale remise à l'échelle ; gâchettes de 0 à 1.
  - Actions : `InputGamepadButton(bouton[, joueur])`, `InputGamepadAxis(axe, POSITIVE|NEGATIVE[, seuil, joueur])` (axe utilisé comme bouton), `InputSimple(bouton, EInputType.Controller)` ; sans joueur = `GamepadManager.ANY_PLAYER` (n'importe quelle manette).
- **Valeur d'une action** : `InputManager.getValue(action)` de 0 à 1, à côté de `getState`. 1 pour une touche, un bouton ou une action simulée ; progressive pour `InputGamepadAxis` (sans seuil, la zone morte s'applique) ; `InputOr` = la plus forte, `InputAnd` = la plus faible. Pour un déplacement : `getValue(droite) - getValue(gauche)`, puis limiter la longueur du vecteur à 1 (diagonale), comme `MovingCameraComponent`.
- **Axes** (`AxisInput(id, nom, IAxis)`, `inputManager.init(actions, axes)`, lus par `InputManager.getAxis(id)`) : valeur signée non bornée, dont le sens est donné par la liaison (direction -1..1, degrés pour ce tick…). Ids pris dans le même compteur que les actions (exemple : `AxisId` de la démo).
  - Liaisons : `AxisGamepad(axe[, joueur])`, `AxisMouse(X|Y|SCROLL_X|SCROLL_Y)` (déplacement du curseur en pixels de la fenêtre pendant le tick : `MouseManager.deltaX/Y`, indépendant de la taille virtuelle), `AxisButtons(négative, positive)` (deux actions, sans double comptage si elles réunissent touche et stick par `InputOr`), `AxisSum(...)` (sources additionnées : souris et stick ensemble).
  - Réglages de toute liaison, dans l'ordre : `curve(exposant)` (signe gardé ; 2 = précis près du centre), `scale(facteur)` (négatif pour inverser), `perSecond()` (× durée du tick : une vitesse devient un déplacement). Un `scale` sur un `AxisSum` est une sensibilité commune à ses sources : regard de la démo = `AxisSum(souris px, stick × 3000 px/s par seconde).scale(0.05 °/px)`.
  - `input axis NOM valeur [ticks]` remplace la valeur des liaisons (tests).
- **Caméra 3D** : `Camera3D` ne lit plus les entrées (`rotate(dYaw, dPitch)` en degrés, droite et haut positifs, tangage limité à ±89°). `FreeCamera3DComponent` (ajouté par `addUpdatable`) la déplace et la tourne à partir d'axes (déplacement -1..1 × `camera.getSpeed()`, regard en degrés), × `quickFactor` tant que l'action `QuickSpeed` est tenue, puis met la vue à jour avant le code de la scène ; `setLookLocked` bloque le regard de toutes les sources (menu, curseur affiché).

### Temps
- `GameTime.DeltaTime()` : temps réel mesuré entre deux ticks (variable). `GameTime.FixedDeltaTime()` : durée fixe d'un tick (1/TPS), **à utiliser pour la physique** (`world.step` de JBox2D).

## Pièges connus (à garder en tête, non corrigés)

- Rechargement à chaud : pendant l'envoi GL d'une texture relue, `Texture.getWidth/getHeight` donnent déjà la nouvelle taille ; un `ModelRenderer` n'affiche rien le temps de renvoyer ses meshes ; une `TileMap` relue perd les changements faits par le code (`setTileType`). Relire un batch chargé dont la liste a changé fausse les références retirées par `unloadBatch`.
- `Resources.getInstance()` crée silencieusement une instance par défaut s'il est appelé avant `createInstance`.

### Spécifiques 2D
- Pas de batching : chaque sprite/forme (`Shape2DRenderer`, `Animation2DRenderer`, `Text`) a son propre VAO/VBO/EBO et son draw call, avec `glUseProgram` + uniforms + toggle du depth test à chaque dessin.
- `Shape.updateVbo` réutilise le buffer (`glBufferSubData`) si la taille ne change pas, sinon il réalloue : préférer des tableaux de taille constante pour les données mises à jour souvent. Les `calculate*()` allouent encore un nouveau `float[]` à chaque appel.
- `TileMapRenderer` : une animation de tuile ne réécrit que les UV des tuiles concernées ; une modification de la carte (`TileMap.hasBeenUpdated()`) reconstruit toujours tout.
- `Shape2DRenderer` alloue ses VBO en `STATIC_STORE` même quand ils sont mis à jour à l'exécution.
- La boucle principale abandonne les ticks en retard : sous forte charge le jeu ralentit au lieu de rattraper (la physique à pas fixe reste stable, mais le temps de jeu dérive du temps réel).
