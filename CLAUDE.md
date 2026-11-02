# CLAUDE.md — Java-MightyLibrary

Moteur de jeu 2D/3D en Java 11 (LWJGL 3.3.2 : OpenGL 3.3 core / GLSL 1.40 en secours, GLFW, OpenAL, STB, Assimp), avec JOML, org.json, JBox2D et OpenSimplex.
Le dépôt contient **la bibliothèque** (`mightylib`) et **un projet de démonstration** (`project`) qui sert de banc d'essai.

## Priorités

- **La 2D est prioritaire** : `renderer/_2D`, `graphics/game` (tilemaps), `graphics/text`, `graphics/GUI`, `Camera2D`, animations, physique 2D (JBox2D, collisions, tweenings). Les choix de conception, optimisations et corrections vont d'abord là.
- **La 3D est secondaire pour l'instant** (`renderer/_3D`, `Camera3D`, `lightning/`, modèles OBJ/Assimp) : ne pas l'étendre ni la refactorer sans demande explicite, mais ne pas la casser (le code commun `Shape`/`Renderer`/`Shader` sert aux deux ; les scènes `Test3D*` doivent rester fonctionnelles).

## Build et exécution

- Maven, mais `sourceDirectory` = `src` (pas de `src/main/java`). Aucun test (pas de `src/test`, pas de JUnit).
- `mvn` et `java` ne sont **pas** dans le PATH du shell : le projet se lance depuis IntelliJ (`.idea/`). Pour compiler en ligne de commande (bash), utiliser le JDK et le Maven fournis avec IntelliJ (dépendances déjà en cache dans `~/.m2`, d'où `-o`) :
  ```bash
  JAVA_HOME="$HOME/.jdks/ms-21.0.9" "/d/Program Files/JetBrains/IntelliJ IDEA Community Edition 2024.2.3/plugins/maven/lib/maven3/bin/mvn" -o -q compile
  ```
  Compiler après chaque modification. La compilation ne vérifie pas le rendu : le dire, et indiquer quelle scène lancer pour vérifier à la main.
- Point d'entrée : `MightyLibrary.project.main.Main` (aussi `exec-maven-plugin` et `maven-assembly-plugin`).
- **Le répertoire de travail doit être la racine du dépôt** : les chemins de ressources sont relatifs (`resources/`, `resources/shaders/`).
- Natives LWJGL sélectionnées par profil Maven (Windows/Linux amd64 seulement, pas de macOS).

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
`Main` fournit un `IStartLibraryProcedure` (taille fenêtre, FPS/TPS, méthode de chargement, nb de threads…) → `MainLoop.run()` :
GLFW + son → contexte `"Main"` (fenêtre + inputs) → `ShaderManager.init` → `Resources.createInstance` → `GLResources.createInstance` → `IProjectLoading.init` (ajout des loaders projet) → `contextLoading` (bindings d'input) → `resources.init()` puis `resources.load()` (bloquant) → première scène.

### Boucle principale (`MainLoop.run`)
Pas fixe découplé : chaque itération fait **soit** un tick (`sceneManager.update()` + `dispose()`), **soit** une frame (`display()` + `window.dispose()` = swap + poll). Les ticks en retard sont abandonnés (pas de rattrapage). `GameTime.DeltaTime()` = temps réel mesuré entre deux ticks.

### Cycle de vie d'une scène
Ordre : `init(args)` → (scène de chargement tant que les batchs chargent) → `launch(args)` → boucle `update()` / `dispose()` / `display()` → `unload()`.
- Convention : `init` appelle `super.init(args, new LoadingSceneImplementation())`, `launch` appelle `super.launch(args)` **avant** de créer des renderers (les caméras et le framebuffer virtuel sont créés dans `launch`).
- Les ressources nécessaires se déclarent en surchargeant `getInvolvedBatch()` (noms de fichiers dans `resources/batchs/`).
- `display()` typique : `setVirtualScene(); clear(); …draw…; setAndDisplayRealScene();`
- Changer de scène : `sceneManagerInterface.setNewScene(new XScene(), args)`.
- ⚠️ Nommage trompeur : **`dispose()` = fin de tick** (reset des états d'input « pressed/released »), **`unload()` = libération** des ressources. Ne pas confondre.

### Ressources (CPU) — `Resources`
- Chaque type est un `DataType` avec un `ResourceLoader` (enregistré dans `Resources.Loaders`; loaders projet dans `ProjectLoading.init`).
- États : créé → **preload** (lecture disque, déclaration des dépendances) → **load** (finalisation, souvent GL) → unload.
- `TYPE_SET_UP` : `THREAD_CONTEXT` (workers), `MAIN_CONTEXT` (file traitée par `process(ms)` sur le thread GL), `IMMEDIATELY_IN_CURRENT_CONTEXT`.
- Références par nom de batch (max 5 par ressource, tableau fixe).
- Le nom d'une ressource = nom de fichier sans extension → **les noms doivent être uniques par type**.
- Accès : `Resources.getInstance().getResource(TextureData.class, "nom")`.

### Ressources GPU — `GLResources` / `GLElement`
- Objets GL partagés et comptés par référence (ex. `Texture`). `getResource()` **incrémente** la référence ; tout `get`/`addElement…` doit être équilibré par un `releaseResource()`.
- `GenDeleteResources` enveloppe toutes les créations/destructions GL (VAO, VBO, EBO, textures, FBO, RBO) pour traquer les fuites (`PrintAll()` au changement de scène). **Toujours passer par lui**, jamais `glGen*`/`glDelete*` directement.

### Rendu
- `Shape` = VAO + VBOs (+ EBO) + `Shader` ; canaux : 0 position, 1 UV, 2 normales.
- `Renderer` = `Shape` + transform (position/scale/rotation → matrice `model`) + map de `ShaderValue` envoyées à chaque `display()`.
- `Shader.sendValueToShader` met en cache la dernière valeur par uniform et ne renvoie que si elle a changé.
- Uniforms génériques : `projection`, `view`, `model`, `color` (constantes dans `ShaderManager`). Les shaders sont listés dans `resources/shaders/<3_30|1_40>/shaders.json` ; **un nouveau shader doit exister dans les deux versions**.
- Un 2D/3D est déterminé par le shader (`isDimension2DShader`) et active/désactive le depth test à chaque draw.

### Inputs
- Les IDs d'action sont des `int` alloués statiquement via `InputManager.getAndIncrementId()` (0 et 1 réservés : `COMMAND`, `RELOAD_TEXTURE`). Projet : `ActionId` + bindings dans `ProjectLoading.contextLoading`.
- La disposition clavier vient de `IStartLibraryProcedure.returnDefaultKeyboardLanguage()` (QWERTY si un contexte est créé sans `WindowCreationInfo`).

### Temps
- `GameTime.DeltaTime()` : temps réel mesuré entre deux ticks (variable). `GameTime.FixedDeltaTime()` : durée fixe d'un tick (1/TPS), **à utiliser pour la physique** (`world.step` de JBox2D).

## Conventions de code

- Java 11 : pas de `record`, pas de `switch` expression, pas de `var` dans les lambdas… reste compatible 11.
- Accolades K&R, 4 espaces, commentaires en anglais, peu de Javadoc.
- Champs publics « config » en PascalCase (`wci.Size`, `resources.Loaders`), méthodes statiques utilitaires souvent en PascalCase (`GenDeleteResources.GenBuffers`, `Logger.CheckOpenGLError`). Enums préfixées `E` (`EDrawMode`, `EInputType`). Interfaces préfixées `I`.
- Après un appel GL sensible : `Logger.CheckOpenGLError("message")`.
- Logs via `System.out/err.println` avec préfixe `--` pour les étapes moteur.
- Les scènes de test (`Test*Scene`) servent de démo/régression manuelle : en ajouter une plutôt que de modifier une existante pour tester une fonctionnalité.

## Règles pour Claude

- Ne jamais importer `MightyLibrary.project.*` depuis `mightylib` : passer les IDs d'action en paramètre (ex. `ZoomingCameraComponent.initActionId`).
- **Fins de ligne** : le dépôt mélange des fichiers stockés en LF et en CRLF, avec `core.autocrlf=true`. Ne jamais réécrire un fichier source avec `sed`/redirection shell (le `sed` de Git Bash retire les CR) : utiliser l'outil Edit. Avant de commiter, vérifier `git diff --cached --stat` : un fichier entier modifié signale un changement de fins de ligne. Pour un fichier stocké en CRLF (`git ls-files --eol <fichier>` → `i/crlf`), l'ajouter avec `git -c core.autocrlf=false add <fichier>`.
- Tout code GL s'exécute sur le thread principal. Les workers de `Resources` ne doivent faire que de l'I/O/CPU.
- Toute ressource GL créée dans une scène doit être libérée dans `unload()` (renderers, shapes, textures via `releaseResource`).
- Pas de test automatisé : décrire comment vérifier manuellement (quelle scène lancer, quoi observer).
- Le dossier `target/` et `.idea/workspace.xml` ne doivent pas être modifiés/commités.

## Pièges connus (à garder en tête, non corrigés)

- Recharger une `TextureData` relit les pixels mais ne met pas à jour les `Texture` GL déjà créées à partir d'elle : le rechargement à chaud ne se voit pas à l'écran.
- `GLResources` : `PriorityQueue<GLElement>` alors que `GLElement` n'implémente pas `Comparable` ; `addElementToLoadOrReturnIfPresent` lit `resources.get(type)` avant `putIfAbsent`.
- `Resources.getInstance()` crée silencieusement une instance par défaut s'il est appelé avant `createInstance`.

### Spécifiques 2D
- Pas de batching : chaque sprite/forme (`Shape2DRenderer`, `Animation2DRenderer`, `Text`) a son propre VAO/VBO/EBO et son draw call, avec `glUseProgram` + uniforms + toggle du depth test à chaque dessin.
- `Shape.updateVbo` fait un `glBufferData` (réallocation) et les `calculate*()` allouent un nouveau `float[]` : appelé à chaque changement de frame d'animation ou de texte.
- `TileMapRenderer.update()` : dès qu'une tuile animée change de frame, **toute la carte** (positions, UV, indices) est recalculée et réuploadée.
- `Shape2DRenderer` alloue ses VBO en `STATIC_STORE` alors qu'ils sont mis à jour à l'exécution.
- La boucle principale abandonne les ticks en retard : sous forte charge le jeu ralentit au lieu de rattraper (la physique à pas fixe reste stable, mais le temps de jeu dérive du temps réel).
