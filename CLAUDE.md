# CLAUDE.md — Java-MightyLibrary

Moteur de jeu 2D/3D en Java 11 (LWJGL 3.3.2 : OpenGL 3.3 core / GLSL 1.40 en secours, GLFW, OpenAL, STB, Assimp), avec JOML, org.json, JBox2D et OpenSimplex.
Le dépôt contient **la bibliothèque** (`mightylib`) et **un projet de démonstration** (`project`) qui sert de banc d'essai.

## Objectif en cours : bibliothèque réutilisable (section à supprimer une fois l'objectif atteint)

Faire de `mightylib` une base de code utilisée par les futurs logiciels et jeux de l'utilisateur ; le dossier `project` en est l'illustration. Cap validé :
- **Deux modules Maven** : `mightylib` (la bibliothèque, produit le jar) et `demo` (l'actuel `project`), qui dépend de `mightylib` comme un projet externe et prouve qu'elle s'utilise de l'extérieur.
- **Jar léger**, sans dépendances embarquées (pas de jar-with-dependencies pour la bibliothèque) : LWJGL (natives comprises), JOML, org.json, JBox2D… sont déclarées dans le pom et récupérées par Maven.
- **Distribution** : `mvn install` (dépôt local) au départ, puis JitPack ou GitHub Packages pour l'utiliser depuis n'importe quelle machine.
- **Versions** sémantiques par release (`1.2.0`…) : chaque projet fixe la version qu'il utilise.
- **Ressources** : séparer les ressources du moteur (shaders, texture `error`, polices par défaut), à embarquer dans le jar et lire depuis le classpath, des ressources du jeu, qui restent dans le dossier du projet utilisateur. Aujourd'hui tout est lu depuis le dossier `resources/` du disque : c'est le principal chantier.
- **API publique** : pas encore définie, **à discuter avec l'utilisateur avant toute décision** (ce qui sera public, changements cassants comme `dispose()`/singletons à faire avant une 1.0).

### Modèle d'extension validé : le projet injecte, le projet étend
- **Injection** : le projet fournit au moteur ce dont il a besoin via **un point d'injection unique**, appelé pendant une phase précise du démarrage (avant l'initialisation des ressources) : par exemple `register(EngineRegistry registry)`. Après le démarrage, le registre est figé. Remplace à terme `IProjectLoading.init`/`contextLoading` et les singletons modifiés depuis n'importe où.
- **Ce qui s'injecte** : sources de ressources (dossier du jeu, autres jars, en plus des ressources du moteur ; une ressource de même nom remplace celle du moteur), resource loaders et types de `DataType`, commandes, scènes enregistrées par nom, actions d'entrée et touches par défaut, shaders, scène de chargement par défaut, crochets de cycle de vie (démarrage, changement de scène, arrêt), compteurs pour `stats`, plus tard composants/thèmes GUI.
- **Extension par héritage** : les classes du projet héritent de celles du moteur (`Shape2DRenderer`, `Scene`, `DataType`/`ResourceLoader`, `BaseCommand`, composants GUI…). Les classes extensibles sont **choisies explicitement**, avec des points d'accroche prévus ; les autres sont fermées (`final` ou package-private), car tout membre `protected` devient un contrat envers les projets.

### Modding (secondaire, à garder en tête dans les choix de conception)
- Le point d'injection doit accepter **plusieurs modules** : le jeu est le premier, chaque mod en est un autre (jars d'un dossier `mods/` découverts par `ServiceLoader`, descripteur avec identifiant, version et dépendances qui fixent l'ordre d'enregistrement).
- **Identifiants avec espace de noms** (`jeu:slime`, `monmod:epee`) pour ressources, commandes, scènes et actions : aujourd'hui les noms de ressources sont globaux et les IDs d'action sont des `int` statiques (`InputManager.getAndIncrementId`), ce qui ferait entrer des mods en collision.
- **Remplacement de ressources** par identifiant, et à terme un **bus d'événements** pour que plusieurs modules puissent réagir et modifier (ex. ajouter un bouton au menu).
- Deux niveaux : mods de données (ressources/JSON, sans code) et mods de code (jars Java, sans sandbox possible : à n'installer que s'ils sont de confiance).
- Ne pas implémenter le modding maintenant, mais ne pas prendre de décision qui l'empêcherait.

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
- Références par nom de batch (liste, un même nom peut apparaître plusieurs fois). Les maps de ressources sont concurrentes et les états de chargement `volatile` : les workers et le thread principal y accèdent.
- Le nom d'une ressource = nom de fichier sans extension → **les noms doivent être uniques par type**.
- Accès : `Resources.getInstance().getResource(TextureData.class, "nom")`.

### Ressources GPU — `GLResources` / `GLElement`
- Objets GL partagés et comptés par référence (ex. `Texture`). `addElementOrReturnIfPresent()` ajoute une référence (élément existant ou nouveau) et doit être équilibré par un `releaseResource()`. `getResource()` est une simple recherche, sans référence. Un élément dont les dépendances ne sont pas encore chargées est réessayé à chaque tick par `GLResources.process()`.
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

## Outils de debug et de mesure

### Commandes
- Une commande hérite de `BaseCommand` et implémente `CommandResult execute(String[] args)` (`args[0]` = nom). Elle **renvoie** son résultat (`CommandResult.ok(message, data JSON)` / `error(...)`), elle n'écrit pas dans la console. Résultat connu plus tard (ex. capture après la frame suivante) : `CommandResult.later(future)`.
- Les commandes générales sont enregistrées dans `ListGeneralCommand` ; une scène peut ajouter les siennes (`Commands.addCommand`, retirées au changement de scène).
- `Commands.submit(line)` est appelable depuis n'importe quel thread ; l'exécution a lieu sur le thread principal au début du tick suivant (`processPendingCommands`). La console est lue dans un thread à part : le jeu n'est jamais bloqué.
- Commandes disponibles uniquement en mode admin.

### Serveur de debug (piloter le jeu depuis un autre processus)
- En mode admin, le jeu écoute sur `127.0.0.1:7777` (`IStartLibraryProcedure.returnDebugServerPort()`, `-1` pour désactiver). Protocole : une commande par ligne, une ligne JSON en réponse `{"success", "message", "data"}`.
- Client : `python tools/mighty.py <commande>` (`--wait N` pour attendre le démarrage, `--json` pour la réponse brute, sans argument = mode interactif).
- Commandes utiles : `help`, `stats` (temps, appels de dessin, changements de shader/texture, uniforms, octets envoyés, objets GL vivants, mémoire), `scene` (scène courante et `loading`), `scene <Nom>` (nom relatif au package de la première scène, ex. `Platformer2D.Test2DPlatformerBox2D`), `screenshot [chemin]` (PNG de la frame suivante, par défaut dans `screenshots/`), `gl` (objets OpenGL vivants et éléments partagés avec leurs références), `input` (voir ci-dessous), `reload`, `exit`.
- **Simuler des entrées** : `input SELECT_UP` (appui d'un tick), `input <ACTION> <ticks>`, `input hold <ACTION>` / `input release <ACTION>`, `input list` (actions et ids). Une action se désigne par son nom (insensible à la casse) ou son id. Les événements sont stockés dans `InputSimulation` et appliqués quand `InputManager.dispose()` calcule les entrées du tick suivant : un appui simulé est vu par tout ce tick, comme un vrai appui (déterministe). La simulation agit sur les **actions** : le code qui lit directement une touche (`getKeyState(GLFW_KEY_…)`) ne la voit pas.
- **Traquer une fuite** : relever `gl` sur une scène, faire plusieurs allers-retours vers une autre scène, relever à nouveau sur la même scène. Les compteurs et les références doivent être identiques à chaque passage.

### Lancer et vérifier le jeu soi-même (Claude)
```bash
JAVA_HOME="$HOME/.jdks/ms-21.0.9" "/d/Program Files/JetBrains/IntelliJ IDEA Community Edition 2024.2.3/plugins/maven/lib/maven3/bin/mvn" -q compile dependency:build-classpath -Dmdep.outputFile=<scratchpad>/cp.txt
# depuis la racine du dépôt, en arrière-plan (une fenêtre s'ouvre sur l'écran de l'utilisateur : le prévenir)
"/c/Autres/## Librairies ##/javajdk/bin/java" -cp "target/classes;$(cat <scratchpad>/cp.txt)" MightyLibrary.project.main.Main
python tools/mighty.py --wait 60 scene Test2DScene   # puis attendre "loading": false via `scene`
python tools/mighty.py screenshot <scratchpad>/capture.png   # puis lire l'image
python tools/mighty.py exit
```

### Profilage
- **JFR** : événements `mightylib.Frame` et `mightylib.Tick` (durée de chaque frame/tick), presque gratuits hors enregistrement. Enregistrer : `jcmd <pid> JFR.start duration=30s filename=rec.jfr` (pid via `jcmd -l`), ou lancer avec `-XX:StartFlightRecording=duration=30s,filename=rec.jfr`. Lire : `jfr print --events mightylib.Frame rec.jfr`, ou JDK Mission Control / VisualVM pour CPU, allocations et GC.
- **GPU** : RenderDoc ou NVIDIA Nsight Graphics (contexte OpenGL 3.3 core) pour inspecter une frame et compter les appels de dessin.
- **Erreurs OpenGL** : `returnOpenGLErrorCheckState()` à `true` active `glGetError` **et** un contexte de debug : le pilote signale les erreurs avec un message lisible (notifications filtrées).

## Conventions de code

- Java 11 : pas de `record`, pas de `switch` expression, pas de `var` dans les lambdas… reste compatible 11.
- Accolades K&R, 4 espaces, commentaires en anglais, peu de Javadoc.
- Champs publics « config » en PascalCase (`wci.Size`, `resources.Loaders`), méthodes statiques utilitaires souvent en PascalCase (`GenDeleteResources.GenBuffers`, `Logger.CheckOpenGLError`). Enums préfixées `E` (`EDrawMode`, `EInputType`). Interfaces préfixées `I`.
- Après un appel GL sensible : `Logger.CheckOpenGLError("message")`. Ces vérifications (`glGetError`) ne sont actives que si `IStartLibraryProcedure.returnOpenGLErrorCheckState()` renvoie `true` (désactivées par défaut). Dans le code appelé à chaque dessin, entourer l'appel de `if (Logger.DO_CHECK)` pour ne pas construire le message inutilement.
- Mesure de performance : en mode admin, le titre de la fenêtre affiche le temps CPU moyen par frame (`display()`, hors swap) et par tick. Le FPS étant plafonné, c'est ce chiffre qui sert à comparer avant/après une optimisation.
- Logs via `System.out/err.println` avec préfixe `--` pour les étapes moteur.
- Les scènes de test (`Test*Scene`) servent de démo/régression manuelle : en ajouter une plutôt que de modifier une existante pour tester une fonctionnalité.

## Règles pour Claude

- **Vérifier par le texte, pas par l'image** : une capture d'écran coûte bien plus cher à analyser qu'une réponse texte. Récupérer d'abord l'information par les commandes (`scene`, `stats`, `gl`, `input list`…) et la console du jeu. Ne faire de `screenshot` que pour vérifier un rendu qu'aucune donnée texte ne peut confirmer (aspect visuel, artefacts), et une seule à la fois. Quand une information utile manque, ajouter une commande (ou enrichir une existante) qui la renvoie en texte/JSON plutôt que de passer par une capture.
- Ne jamais importer `MightyLibrary.project.*` depuis `mightylib` : passer les IDs d'action en paramètre (ex. `ZoomingCameraComponent.initActionId`).
- **Fins de ligne** : le dépôt mélange des fichiers stockés en LF et en CRLF, avec `core.autocrlf=true`. Ne jamais réécrire un fichier source avec `sed`/redirection shell (le `sed` de Git Bash retire les CR) : utiliser l'outil Edit. Avant de commiter, vérifier `git diff --cached --stat` : un fichier entier modifié signale un changement de fins de ligne. Pour un fichier stocké en CRLF (`git ls-files --eol <fichier>` → `i/crlf`), l'ajouter avec `git -c core.autocrlf=false add <fichier>`.
- Tout code GL s'exécute sur le thread principal. Les workers de `Resources` ne doivent faire que de l'I/O/CPU.
- Toute ressource GL créée dans une scène doit être libérée dans `unload()` (renderers, shapes, textures via `releaseResource`). Les objets ajoutés avec `addDisplayable`/`addUpdatable` sont déchargés par `Scene.unload()` ; ceux gardés seulement dans un champ doivent l'être à la main. Vérifier avec la commande `gl`.
- Pas de test automatisé : décrire comment vérifier manuellement (quelle scène lancer, quoi observer).
- Le dossier `target/` et `.idea/workspace.xml` ne doivent pas être modifiés/commités.

## Pièges connus (à garder en tête, non corrigés)

- Recharger une `TextureData` relit les pixels mais ne met pas à jour les `Texture` GL déjà créées à partir d'elle : le rechargement à chaud ne se voit pas à l'écran.
- `Resources.getInstance()` crée silencieusement une instance par défaut s'il est appelé avant `createInstance`.

### Spécifiques 2D
- Pas de batching : chaque sprite/forme (`Shape2DRenderer`, `Animation2DRenderer`, `Text`) a son propre VAO/VBO/EBO et son draw call, avec `glUseProgram` + uniforms + toggle du depth test à chaque dessin.
- `Shape.updateVbo` réutilise le buffer (`glBufferSubData`) si la taille ne change pas, sinon il réalloue : préférer des tableaux de taille constante pour les données mises à jour souvent. Les `calculate*()` allouent encore un nouveau `float[]` à chaque appel.
- `TileMapRenderer` : une animation de tuile ne réécrit que les UV des tuiles concernées ; une modification de la carte (`TileMap.hasBeenUpdated()`) reconstruit toujours tout.
- `Shape2DRenderer` alloue ses VBO en `STATIC_STORE` même quand ils sont mis à jour à l'exécution.
- La boucle principale abandonne les ticks en retard : sous forte charge le jeu ralentit au lieu de rattraper (la physique à pas fixe reste stable, mais le temps de jeu dérive du temps réel).
