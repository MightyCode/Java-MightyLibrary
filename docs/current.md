# Gros point actuel — API : corrections urgentes

Retirer chaque objectif atteint ; fichier vide → **discuter** avec l'utilisateur du point en tête de `later.md`, puis l'écrire ici. Propositions **à valider avec l'utilisateur avant d'appliquer** ; changements cassants maintenant, avant toute version publiée. (U) = le plus urgent.

- (U) **Pipeline de `Resources`** (propriétaire unique, réconciliation, dépendances, rechargement à chaud : faits, voir `architecture.md`). Reste :
  - rechargement à chaud :
    - **observateur de fichiers** (et ressources des mods) qui appelle `reload(donnée)` ; `reload all` reste un outil de debug ;
    - **mémoire** : ancienne et nouvelle version coexistent, un `reload` de milliers de données crée toutes les copies d'un coup (les workers bornent les lectures, pas les copies qui attendent leurs dépendances) → limiter les copies en cours ;
    - relecture abandonnée (nouveau `reload` pendant la lecture) : son étape en cours va au bout (CPU/I/O gaspillés, deux copies en mémoire un moment) → interruption coopérative (drapeau lu par les loaders longs) avec l'item **annulation** ci-dessous ; l'observateur de fichiers devra regrouper les événements d'un même enregistrement ;
  - **échecs attendus** (`ResourceException` : fait pour fichiers, JSON, CSV, textures, sons, modèles) : les formats maison (`TileSetLoader`, `TextureDataAtlasLoader`, `AnimationDataLoader`, reste de `TileMapLoader`) lèvent encore des `NumberFormatException`/`IndexOutOfBounds` affichées comme bugs → valider et lever `ResourceException` avec fichier et ligne ;
  - `leaves.obj` (Test3DScene) refusé par Assimp (`OBJ: vertex index out of range`) : cube de secours (une ligne d'erreur), `--strict` arrête le jeu ;
  - **priorités** (scène courante avant préchargement de la suivante) et **annulation** des lectures inutiles : étapes encore en file sautées (fait, `skippedSteps`) ; reste l'interruption des étapes déjà commencées (drapeau `cancelled` + `checkCancelled()` aux points naturels des loaders longs, `try/finally` pour la mémoire native ; lecture du fichier par blocs séparée du décodage) et la limite de copies en cours ;
  - fusionner les files (`inbox` et étapes `MAIN_CONTEXT` de `Resources`, `MainThread`, `GLResources`) sous un même budget de frame ;
  - **cycle** de dépendances → attente sans fin : le détecter et le faire échouer ;
  - **voir à l'avance** ce qu'un batch charge (dépendances comprises) et son **poids** (taille estimée par `DataType`, mémoire CPU/GPU, totaux par batch et dans `resources`) ;
  - `DataType` lit `Resources.getInstance()` dans son constructeur (cf. singletons).
- **Chargement GL** (bandes dans le reste de la frame : fait, `TestGLLoading`) : PBO ou contexte partagé (fenêtre cachée + `glFenceSync`) seulement si les mesures l'exigent. `HeightMapRenderer` et `TerrainRenderer` envoient encore leur géométrie d'un bloc.
- **Singletons statiques** (`Resources`, `GLResources`, `ShaderManager`, `ContextManager`, `SoundManager`) et instance par défaut silencieuse de `Resources.getInstance()` → contexte moteur, ou au minimum échec explicite. Lié à l'injection (`later.md`).
- **IDs d'action `int` statiques** → identifiants texte à espace de noms enregistrés au registre (lié au remappage et au modding).
- **Champs publics modifiables** (`Resources.Loaders`, `GUIList.GUIs`, `ShouldLoop`, `WindowCreationInfo.Size`…) → méthodes.
- **Noms fautifs** : package `lightning` (= `lighting`), `getCurrentTIme`, `canBeLoad`…
- **Shaders découpés comme les textures** (TODO `Shader.java`) : aujourd'hui lecture, ajout de la bibliothèque GLSL (`ShaderLoader.completeWithLibraryFile`), compilation (`setShaderContent`) et liaison (`internLoad`) d'un bloc sur le thread principal. Cible : lecture et prétraitement (`#include`, `#define`, variantes) par les workers ; programme dans un `GLElement` (compilation par tranches, références, suivi `gl`) ; avec `GL_KHR_parallel_shader_compile`, interroger `GL_COMPLETION_STATUS_KHR` aux frames suivantes. Prépare l'injection de shaders et leur rechargement à chaud.
