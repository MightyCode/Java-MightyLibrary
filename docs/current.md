# Gros point actuel — API : corrections urgentes

Retirer chaque objectif atteint ; fichier vide → **discuter** avec l'utilisateur du point en tête de `later.md`, puis l'écrire ici. Propositions **à valider avec l'utilisateur avant d'appliquer** ; changements cassants maintenant, avant toute version publiée. (U) = le plus urgent.

- (U) **Pipeline de `Resources`** (propriétaire unique, réconciliation, dépendances, rechargement à chaud : faits, voir `architecture.md`). Reste :
<<<<<<< HEAD
  - **priorités** (scène courante avant préchargement de la suivante) ; **annulation** faite (étapes en file sautées, `skippedSteps` ; étape commencée : `DataType.checkCancelled()`, placé dans les textures et les modèles ; sons : lecture et décodage dans un seul appel, à découper si de gros fichiers l'exigent) ;
  - fusionner les files (`inbox` et étapes `MAIN_CONTEXT` de `Resources`, `MainThread`, `GLResources`) sous un même budget de frame ;
=======
  - **priorités** (scène courante avant préchargement de la suivante) ; **annulation** faite (étapes en file sautées, `skippedSteps` ; étape commencée : `DataType.checkCancelled()`), reste à placer `checkCancelled()` dans les autres loaders longs aux frontières de phases (aujourd'hui : décodage des textures seulement ; modèles, sons : un seul appel natif) ;
  - files sous un même budget de frame : fait (`Resources.processUntil`, `MainThread` et `GLResources` reçoivent la même échéance dans `MainLoop`). Reste : part du budget réservée au chargement pendant le jeu (plus large sous un écran de chargement), réglable dans `EngineConfig` ; le pic de ~80 ms vu avec `TestGLLoading` vient de son `init` (6 images 2048² dessinées sur le thread principal), pas du moteur : un changement de scène coûte 3 ms de tick vers le menu et moins de 4 ms vers `Test2DScene`/`TestBox2D`. Restent des frames de 25 à 50 ms au chargement (une seule étape `MAIN_CONTEXT` ou GL peut dépasser l'échéance) ;
>>>>>>> frame-budget
  - **voir à l'avance** ce qu'un batch charge (dépendances comprises) et son **poids** (taille estimée par `DataType`, mémoire CPU/GPU, totaux par batch et dans `resources`) ;
  - `DataType` lit `Resources.getInstance()` dans son constructeur (cf. singletons).
- **Chargement GL** (bandes dans le reste de la frame : fait, `TestGLLoading`) : PBO ou contexte partagé (fenêtre cachée + `glFenceSync`) seulement si les mesures l'exigent. `HeightMapRenderer` et `TerrainRenderer` envoient encore leur géométrie d'un bloc.
- **Singletons statiques** (`Resources`, `GLResources`, `ShaderManager`, `ContextManager`, `SoundManager`) et instance par défaut silencieuse de `Resources.getInstance()` → contexte moteur, ou au minimum échec explicite. Lié à l'injection (`later.md`).
- **IDs d'action `int` statiques** → identifiants texte à espace de noms enregistrés au registre (lié au remappage et au modding).
- **Champs publics modifiables** (`Resources.Loaders`, `GUIList.GUIs`, `ShouldLoop`, `WindowCreationInfo.Size`…) → méthodes.
- **Noms fautifs** : package `lightning` (= `lighting`), `getCurrentTIme`, `canBeLoad`…
- **Shaders découpés comme les textures** (TODO `Shader.java`) : aujourd'hui lecture, ajout de la bibliothèque GLSL (`ShaderLoader.completeWithLibraryFile`), compilation (`setShaderContent`) et liaison (`internLoad`) d'un bloc sur le thread principal. Cible : lecture et prétraitement (`#include`, `#define`, variantes) par les workers ; programme dans un `GLElement` (compilation par tranches, références, suivi `gl`) ; avec `GL_KHR_parallel_shader_compile`, interroger `GL_COMPLETION_STATUS_KHR` aux frames suivantes. Prépare l'injection de shaders et leur rechargement à chaud.
