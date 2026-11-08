# Architecture

## Structure
```
src/io/github/mightycode/
  mightylib/       # bibliothèque (ne dépend PAS de mightylibdemo/)
    main/          # MainLoop (statique), Window, Context/ContextManager, MainThread, Workers, GameTime
    scenes/        # Scene, SceneManager, SceneManagerInterface, TemplateSceneLoading, caméras 2D/3D
    resources/     # Resources (singleton, pipeline), DataType, ResourceLoader, batchs
    graphics/      # GLResources, GLElement, renderer/ (Shape, Renderer, _2D, _3D), shader/, text/, GUI/, lightning/
    inputs/        # InputManager (actions), KeyboardManager, MouseManager, dispositions clavier
    sounds/        # SoundManager (singleton OpenAL), arbre de gains
    physics/       # collisions maison, tweenings, B2Utils (JBox2D)
    algorithms/    # A*, Dijkstra, graphes, Wave Function Collapse
    utils/         # commandes, JSON, maths, Logger
  mightylibdemo/   # démo : Main, ProjectLoading, ActionId, scènes Test*
resources/         # assets ; batchs/*.batch = groupes de ressources
```

## Démarrage
`Main` remplit un `EngineConfig` (setters enchaînables, seule la scène de départ est obligatoire) → `MainLoop.run(config)` : GLFW + son → contexte `"Main"` (fenêtre + entrées) → `ShaderManager.init` → `Resources.createInstance` → `GLResources.createInstance` → `IProjectLoading.init` (loaders projet) → `contextLoading` (liaisons d'entrées) → `resources.init()` + `load()` (bloquant) → première scène.

## Boucle (`MainLoop.run`)
Pas fixe découplé : chaque itération fait **soit** un tick (`update()` + `postUpdate()`), **soit** une frame (`display()` + `swapBuffersAndPollEvents()`). Ticks en retard abandonnés, sans rattrapage (sous charge le jeu ralentit, le temps de jeu dérive du temps réel). Arrêt : fenêtre fermée ou `sceneManagerInterface.exit(status)` (fin du tick). `run` libère tout et **renvoie** le code (`ListError`) sans `System.exit` (`Main` fait `System.exit(MainLoop.run(config))`).

## Temps
`GameTime.DeltaTime()` : temps réel entre deux ticks (variable). `FixedDeltaTime()` : 1/TPS, **pour la physique**.

## Threads
- **Principal** (`MainThread`) : seul à avoir le contexte GL. Y revenir par `MainThread.execute`/`run`/`supply` (→ `CompletableFuture`) ou `thenAcceptAsync(…, MainThread.EXECUTOR)`. File vidée après chaque `display()` dans le reste de la frame (au moins une tâche), avant `GLResources.process`, puis entièrement à la sortie.
- **`Workers`** : pool de `EngineConfig.maxThreads` (`MightyLibrary-Worker-N`, daemon) pour I/O et calcul courts (`Workers.run`/`supply`), **sans GL**. Les étapes `THREAD_CONTEXT` de `Resources` y tournent.
- Boucles longues/bloquantes (réseau, `DebugServer`, console, encodage de `Screenshot`) : threads dédiés `MightyLibrary-…`, daemon.
- Avec `Logger.DO_CHECK`, `GenDeleteResources` lève une exception si un objet GL est créé/détruit hors du thread principal.

## Cycle de vie d'une scène
`init(args)` → (scène de chargement pendant les batchs) → `launch(args)` → `update()`/`postUpdate()`/`display()` → `unload()` → `release()`.
- Exemple commenté : `mightylibdemo/scenes/DocumentationScene` (batch `documentation`, `resources/data/documentationScene.json`, texture créée dans `init`, supprimée dans `release`).
- Paires : `launch` ↔ `unload` (seulement si `launch` a eu lieu, avec `super.unload()`) ; `init` ↔ `release` (toujours, même si quittée pendant le chargement).
- `init`/`launch` facultatifs, **sans `super`** : le moteur (`initByManager`/`launchByManager`) crée la scène de chargement, puis caméras et framebuffer virtuel avant `launch`. `init` déclare (ressources préchargées), `launch` crée les renderers.
- Scène de chargement : `EngineConfig.setLoadingScene` (défaut `DefaultLoadingScene`, fond gris) ou `createLoadingScene(args)` (`null` = rien). Surface virtuelle : `createVirtualSceneBindable()`.
- Ressources : surcharger `getInvolvedBatch()` (fichiers de `resources/batchs/`).
- `display()` : `setVirtualScene(); clear(); …; setAndDisplayRealScene();`. Changer de scène : `sceneManagerInterface.setNewScene(new XScene(), args)`. `postUpdate()` remet à zéro les états pressed/released.

## Ressources CPU (`Resources`)
- Type = `DataType` + `ResourceLoader` (`Resources.Loaders` ; projet : `ProjectLoading.init`). Accès : `Resources.getInstance().getResource(TextureData.class, "nom")`. Nom = fichier sans extension → **unique par type**.
- États : créé → **preload** (`initWithFile` : disque, dépendances) → **load** (`internLoad`, dépendances chargées) → unload (`internUnload`, ou `internCancelPreload`).
- `TYPE_SET_UP` : `THREAD_CONTEXT` (workers, ≤ `maxThreads`), `MAIN_CONTEXT` (thread principal, échéance de la frame : `Resources.processUntil`, appelée par `MainLoop` avec `MainThread` et `GLResources`), `IMMEDIATELY_IN_CURRENT_CONTEXT` (dans l'appel, ex. descriptions de batchs). Unload toujours sur le thread principal. Une étape en file est vérifiée **au moment de démarrer** (`stepStillNeeded`) : donnée plus voulue (scène quittée, copie abandonnée) ou à relire → sautée (`skippedSteps` dans `resources`), puis `update` la vide ou la relance ; voulue à nouveau entre-temps → étape gardée. Une étape commencée va au bout. **Deux niveaux de priorité** : une donnée voulue maintenant (batch chargé, code, dépendance d'une donnée voulue) est *urgente* ; celle qui n'a que des références `preload:` (`preloadBatch`) passe après (file à deux niveaux dans `ResourceScheduler`, promue en O(1) quand elle devient urgente, un worker reste libre pour les urgentes). `finishedCurrentLoading` ne compte que l'urgent : un écran de chargement n'attend pas le préchargement. API : `Resources.preloadBatch`/`cancelPreloadBatch`, `SceneManagerInterface.preloadScene` (scène `TestPreloadScene`). **Budget de frame** : `MainLoop` donne une échéance commune à `Resources.processUntil`, `MainThread` et `GLResources` ; sous un écran de chargement le chargement a toute la frame, sinon `EngineConfig.setInGameLoadingShare` (30 % par défaut ; `MainThread` garde toute la frame). **Poids et progression** : `DataType.estimatedCpuBytes`/`estimatedGpuBytes`, commande `resources batch <nom>`, `Resources.wantedProgress()` ({fait, total} des données voulues maintenant, affiché par l'écran de chargement de la démo).
- **Propriétaire unique** : seul le thread principal change références et états ; les appels d'autres threads (`addOwnedResource` depuis un loader, `addPreLoadedResource` depuis `internLoad`) passent par une file vidée par `process`. Un worker ne fait que lire/calculer.
- **Découpage de `Resources`** (façade + singleton, cycle d'état `update`/`startStep`/`endStep`/`fail`) : `ResourceScheduler` (files, workers, budget), `DependencyGraph` (dépendances, attentes), `ReloadManager` (rechargement à chaud), `BatchManager` (lots), `FallbackRegistry` (données de repli), `ResourceDescriber` (JSON de debug). Tous package-private, thread principal.
- **Réconciliation** (`Resources.update`) : voulue = référencée (sauf suppression/sortie). Une donnée dont une étape tourne n'est jamais modifiée ; à la fin, voulu/état est recomparé.
- **Dépendances** : une donnée voulue et préchargée référence les siennes (`dependency:<Type>:<nom>`, pas à lister dans le batch, gardées tant qu'elle est chargée ; `onlyAsDependency` dans `resources`) ; elle est réveillée quand l'une est chargée.
- **Échec** (`Resources.fail`, exception du loader, `internLoad` → `false`) : retour à l'état initial, dépendances rendues, pas de nouvel essai tant que voulue (sauf `reloadAllOfType`). Ceux qui attendent prennent le placeholder du type, sinon échouent en chaîne. Échec **attendu** (fichier manquant/illisible, format inconnu, contenu mal formé) : le loader lève `ResourceException` (sans pile ; `FileMethods.readFileAsString`/`readJSONObject` le font) → une ligne ; toute autre exception = bug → pile complète.
- **Placeholders** (`ResourceLoader.createFallback`, nommés `error`, générés, chargés au démarrage, jamais déchargés/rechargés ; un placeholder en échec lève une exception) pour les types primaires : texture (damier magenta/noir, + version rectangle), son (bip), shader (magenta), police (carrés), animation (une image), modèle (cube). Types complexes (JSON, CSV, tilemaps, tilesets, batchs…) : `null`, plantage accepté. `Resources.usable(type, donnée)` = donnée si chargée, sinon placeholder (échec **ou chargement en cours**) ; `getResourceOrFallback` pour un nom inconnu (signalé une fois). Les utilisateurs (`Texture`, `SoundSource`, `Shape`, `Text`, `Animation`, `ModelRenderer`) résolvent à l'usage : une donnée chargée plus tard ou relue avec succès remplace le placeholder. Placeholder signalé une fois, à l'échec ; `resources` liste `usingFallback`.
- **Mode strict** (`EngineConfig.setStrictResources`, `-Dmightylib.resources.strict=true`, `scenario.py --strict`, debug seulement) : échec ou nom inconnu → exception.
- Références par nom (batch, `"manually"`, `"all"` pour `loadAllOfType`, propriétaire) ; liste, doublons permis.
- **Rechargement à chaud** (`Resources.reload(donnée)`, `reloadAllOfType`, commande `reload`) :
  - donnée **chargée** jamais vidée : `DataType.createReloadCopy` → copie lue à part (`reloading` dans `resources`), puis `swapContent` (identité et références gardées), la copie décharge l'ancien contenu. **Échec = version précédente gardée** (exception en strict). Nouveau `reload` pendant la lecture : nouvelle copie lue tout de suite, l'ancienne abandonnée (plus voulue : son étape en cours s'arrête au prochain `checkCancelled()` du loader, sinon va au bout et son résultat est jeté ; si elle attendait ses dépendances, libérée aussitôt) ;
  - **annulation** : `update` pose le drapeau `cancelled` d'une donnée dont l'étape en cours est inutile (plus voulue, relecture demandée) ; le loader l'interroge par `checkCancelled()` (`CancellationException`, silencieuse, comptée dans `skippedSteps`), à placer avant la phase coûteuse ;
  - **copies bornées** (`Resources.setMaxReloadCopies`, 32 par défaut) : au-delà, la donnée reste utilisable avec l'ancien contenu et attend une place (`ReloadManager.mustWait`/`wakeDeferred`) ;
  - `DataType.getVersion()` augmente à chaque changement ; les utilisateurs comparent leur version (`Texture`, `TileMapRenderer`, `Text`, `ModelRenderer`), pas d'écouteurs ;
  - les `dependents` (via `acquireDependencies`) sont relus en chaîne (texture → tileset → tilemap, atlas → police), chacun attend ses dépendances (`dependencyReady`) : un seul calcul par donnée lors d'un `reload all` ;
  - non relues : sans source (`hasSource()` faux), préchargées par le code, placeholders ; recalculées (sauf placeholders) si une dépendance change. Une donnée en échec ou en premier chargement est relue sur place ;
  - type sans copie (`createReloadCopy` → `null` : mods, sous-types) : vidé puis relu sur place (signalé une fois). Un sous-type n'hérite pas de la copie du parent ;
  - batch : `loadBatch` garde sa liste, `unloadBatch` libère exactement celle-là ; description relue pendant qu'il est chargé → ajouts chargés, retraits libérés ; description citant une donnée inconnue non appliquée ;
  - **observateur de fichiers** (`Resources.watcher()`, `ResourceWatcher`) : **éteint par défaut**, démarré explicitement par le code (`watchAll()`, `watchType(...)`, `watch(ResourceSelection)`), la commande `watch` ou `-Dmightylib.resources.watch=poll|service` (`EngineConfig.setWatchResources`, intervalle `…watch.interval` en ms). Une `ResourceSelection` (réutilisable ailleurs, ex. batchs) = tout, ou un type, + noms et/ou regex sur le nom (nom dans la liste OU regex) ; les sélections s'additionnent. Seules les données chargées (ou en échec) avec fichier (`DataType.sourcePaths()`), ni placeholders ni créées par le code. Modification ou création → `Resources.reload` (les dépendants suivent) ; suppression ignorée (version gardée, une ligne). Changement rapporté quand le fichier est stable 200 ms (éditeurs qui écrivent en plusieurs temps). Détection (`FileChangeDetector`, thread démon) : **scrutation** date/taille à chaque intervalle (défaut 1 s, marche partout) ou **WatchService** (moins coûteux, non récursif : un enregistrement par dossier, repli sur la scrutation d'un dossier non enregistrable). Liste des fichiers suivis rafraîchie chaque seconde. **Regroupement** : les changements d'une même donnée (description + sources écrites l'une après l'autre) sont rechargés une seule fois, 300 ms après le dernier. **Fichiers nouveaux** : les dossiers de `resources/` sont suivis aussi (date du dossier / `WatchService`) ; un dossier modifié lance `Resources.discoverNewFiles` (thread du détecteur, `ResourceLoader.discover`, par défaut `create`) puis enregistre sur le thread principal les données absentes que la sélection accepte, et rafraîchit les batchs chargés (motifs). `TextureDataLoader.discover` ajoute les images de `textures/` absentes de `textures.json`, avec les paramètres par défaut de `TextureData` (realistic, 2D) et un avertissement. Limite : fichier déposé dans un sous-dossier créé il y a moins d'une seconde, vu au prochain changement de dossier. Tests : `FileWatchTest` ;
  - nouveau type : implémenter `createReloadCopy` (copier les réglages donnés par le code, ex. `TextureData.copySettingsFrom`) et `swapContent` (échanger tout ce que lisent `initWithFile` et `internLoad`).
- Commande `resources` : par type, chargées/préchargées/référencées, chargées sans référence (fuite), référencées non chargées, état du pipeline. Tests : `Resources*Test` (base `ResourcesTestBase` ; batchs, annulation, dépendances, échecs et repli, ressources manuelles, rechargement, préchargement, cycles aléatoires).

## Ressources GPU (`GLResources` / `GLElement`)
- Objets GL partagés et comptés : `addElementOrReturnIfPresent()` ajoute une référence (à équilibrer par `releaseResource()`) ; `getResource()` cherche sans référencer.
- **Jamais bloquant** : l'ajout met en file ; après chaque `display()`, `GLResources.process(deadline)` avec le reste de la frame, au moins une étape ; plafond `EngineConfig.setMaxGLUploadMillisecondsPerFrame`. `GLElement.load(ms)` renvoie `false` tant qu'il n'a pas fini.
- `Texture` : > 4 Mio (2D) envoyée par bandes `glTexSubImage2D` (256 Kio–4 Mio selon débit mesuré et temps restant). Non chargée (en cours/échec) → `bind` du placeholder de même type (1×1 transparent tant que le placeholder n'est pas envoyé) ; `hasElementsToLoad` ignore les échecs. Donnée rechargée (version différente au `bind`) → renvoyée dans une seconde texture GL (`GLElement.requestReload`/`reload`, même budget) qui remplace l'ancienne une fois complète. `getWidth/getHeight` = taille affichée ; ce qui en dépend attend `Texture.isUpToDate()` (ex. `TileMapRenderer` garde ses tuiles, animations en pause).
- `ModelRenderer` : une version = un `MeshSet` (une `Shape` par mesh, ses textures), un mesh par frame ; une nouvelle version remplace l'affichée une fois complète. La `Shape` de base ne sert qu'au shader.
- Atlas d'une `TileMap` (`TileSetAtlas`, texture `tilemap:<carte>`) = contenu de la carte construit dans `internLoad`, échangé au rechargement.
- Son rechargé : `SoundData.internUnload` détache le buffer des sources (`SoundManager.DetachBuffer`) avant suppression.
- Chargement de scène : l'écran de chargement reste jusqu'à `GLResources` vide (ce que `launch` a demandé compris) ; avertissement après 10 s.
- Renderers 2D utilisables dès le constructeur ; géométrie envoyée au premier `display()` puis après chaque modification.
- **Toujours** `GenDeleteResources` (VAO, VBO, EBO, textures, FBO, RBO), jamais `glGen*`/`glDelete*` (`PrintAll()` au changement de scène).

## Rendu
- `Shape` = VAO + VBOs (+ EBO) + `Shader` ; canaux 0 position, 1 UV, 2 normales. `Renderer` = `Shape` + transform (→ `model`) + `ShaderValue` envoyées à chaque `display()`.
- `Shader.sendValueToShader` ne renvoie qu'une valeur changée ; uniform absent du programme signalé une fois et ignoré.
- Uniforms génériques : `projection`, `view`, `model`, `color` (`ShaderManager`). Shaders listés dans `resources/shaders/<3_30|1_40>/shaders.json` ; **un nouveau shader existe dans les deux versions**.
- 2D/3D décidé par le shader (`isDimension2DShader`), qui bascule le depth test à chaque dessin.

## Entrées
- IDs d'action `int` statiques (`InputManager.getAndIncrementId()`, 0 et 1 réservés : `COMMAND`, `RELOAD_TEXTURE`). Projet : `ActionId` + liaisons dans `ProjectLoading.contextLoading`. Disposition clavier : `EngineConfig.setKeyboardLanguage()` (QWERTY par défaut, et pour un contexte créé sans `WindowCreationInfo`).
- **Manettes** : un `GamepadManager` commun (`ContextManager.getGamepadManager()`, ou `InputManager.getGamepadManager()`), mis à jour par `ContextManager.postUpdate` avant les contextes ; GLFW (`glfwGetGamepadState`, disposition Xbox, SDL_GameControllerDB intégrée, `addMappings`) derrière `IGamepadSource` (fausse source en tests).
  - Emplacements 0–15 = joysticks GLFW, 16+ = 8 virtuelles. Connexions appliquées dans `postUpdate` (ensemble stable par tick ; `connectedThisTick()`/`disconnectedThisTick()` durent un tick).
  - Joueurs : plus petit libre, repris à la reconnexion s'il est libre ; `setAutoAssign(false)` + `assignPlayer` pour « appuyez pour rejoindre ». Sans mapping : connectée sans joueur.
  - `Gamepad` : ids GLFW ; sticks -1..1 (y vers le bas), zone morte radiale remise à l'échelle ; gâchettes 0..1.
  - Actions : `InputGamepadButton(bouton[, joueur])`, `InputGamepadAxis(axe, POSITIVE|NEGATIVE[, seuil, joueur])`, `InputSimple(bouton, EInputType.Controller)` ; sans joueur = `ANY_PLAYER`.
- **Valeur** : `InputManager.getValue(action)` 0..1 (1 pour touche/bouton/simulé, progressive pour `InputGamepadAxis` sans seuil ; `InputOr` = max, `InputAnd` = min). Déplacement : `getValue(droite) - getValue(gauche)`, longueur limitée à 1 (cf. `MovingCameraComponent`).
- **Axes** (`AxisInput(id, nom, IAxis)`, `inputManager.init(actions, axes)`, `getAxis(id)`) : valeur signée non bornée, sens donné par la liaison ; ids du même compteur (cf. `AxisId`).
  - Liaisons : `AxisGamepad(axe[, joueur])`, `AxisMouse(X|Y|SCROLL_X|SCROLL_Y)` (pixels de fenêtre pendant le tick, `MouseManager.deltaX/Y`, indépendant de la taille virtuelle), `AxisButtons(nég, pos)` (sans double comptage via `InputOr`), `AxisSum(...)`.
  - Réglages, dans l'ordre : `curve(exposant)` (signe gardé ; 2 = précis près du centre), `scale(facteur)` (négatif = inversion), `perSecond()` (× durée du tick : vitesse → déplacement). `scale` sur `AxisSum` = sensibilité commune : regard de la démo = `AxisSum(souris px, stick × 3000 px/s).scale(0.05 °/px)`.
- **Caméra 3D** : `Camera3D` ne lit pas les entrées (`rotate(dYaw, dPitch)` en degrés, droite et haut positifs, tangage ±89°). `FreeCamera3DComponent` (`addUpdatable`) la pilote par axes (déplacement -1..1 × `camera.getSpeed()`, regard en degrés, × `quickFactor` avec `QuickSpeed`) et met la vue à jour avant le code de la scène ; `setLookLocked` bloque le regard.

## Pièges connus (non corrigés)
- Une `TileMap` relue perd les changements faits par le code (`setTileType`).
- `Resources.getInstance()` crée silencieusement une instance avant `createInstance`.
- 2D sans batching : chaque `Shape2DRenderer`/`Animation2DRenderer`/`Text` a VAO/VBO/EBO et draw call, avec `glUseProgram`, uniforms et depth test à chaque dessin.
- `Shape.updateVbo` réutilise le buffer à taille égale (`glBufferSubData`), sinon réalloue ; les `calculate*()` allouent un `float[]` à chaque appel.
- `TileMapRenderer` : une animation de tuile ne réécrit que ses UV, mais toute modification de carte (`hasBeenUpdated()`) reconstruit tout.
- `Shape2DRenderer` alloue en `STATIC_STORE` même pour des VBO modifiés.
