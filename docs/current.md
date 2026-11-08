# Gros point actuel — API : corrections urgentes

Retirer chaque objectif atteint ; fichier vide → **discuter** avec l'utilisateur du point en tête de `later.md`, puis l'écrire ici. Propositions **à valider avec l'utilisateur avant d'appliquer** ; changements cassants maintenant, avant toute version publiée. (U) = le plus urgent.

- (U) **Pipeline de `Resources`** (voir `architecture.md`). Reste :
  - **sons** : lecture et décodage dans un seul appel (`SoundDataType.readVorbis`), donc pas de `checkCancelled()` ; à découper si de gros fichiers l'exigent ;
  - **shaders** : lecture et prétraitement sur les workers, compilation en une étape par source puis liaison (fait) ; reste le premier shader ~65 ms et l'atlas de la police `bahnschrift` ~70-90 ms au démarrage (sans doute chauffe du pilote, une étape de compilation peut rester longue), et `GL_KHR_parallel_shader_compile` (voir « Shaders découpés » plus bas) ;
  - **précharger les éléments GL** (aujourd'hui seules les ressources CPU le sont : les `GLElement` n'existent qu'après le `launch` de la scène) ;
  - **priorité plus fine** que courant/préchargement (rechargement à chaud, ressources du premier affichage) si un vrai jeu le demande ; une donnée garde sa priorité urgente si la référence urgente est retirée avant la préchargée ;
  - **temps restant d'un chargement** : débits mesurés par type, et part GL dans la progression ;
  - **estimations de poids** par type pour les modèles et les sons (aujourd'hui : taille du fichier) ; les dépendances d'une donnée non lue ne sont connues qu'après sa lecture (`dependenciesKnown`) ;
  - `DataType` lit `Resources.getInstance()` dans son constructeur (cf. singletons).
- **Chargement GL** : PBO ou contexte partagé (fenêtre cachée + `glFenceSync`) seulement si les mesures l'exigent. `HeightMapRenderer` et `TerrainRenderer` envoient encore leur géométrie d'un bloc.
- **Singletons statiques** (`Resources`, `GLResources`, `ShaderManager`, `ContextManager`, `SoundManager`) et instance par défaut silencieuse de `Resources.getInstance()` → contexte moteur, ou au minimum échec explicite. Lié à l'injection (`later.md`).
- **IDs d'action `int` statiques** → identifiants texte à espace de noms enregistrés au registre (lié au remappage et au modding).
- **Champs publics modifiables** (`Resources.Loaders`, `GUIList.GUIs`, `ShouldLoop`, `WindowCreationInfo.Size`…) → méthodes.
- **Noms fautifs** : package `lightning` (= `lighting`), `getCurrentTIme`, `canBeLoad`…
- **Shaders découpés comme les textures** (reste) : programme dans un `GLElement` (références, suivi `gl`, injection de shaders), bibliothèque GLSL lue au démarrage par `create` (pas par les workers), et avec `GL_KHR_parallel_shader_compile` lancer toutes les compilations puis interroger `GL_COMPLETION_STATUS_KHR` aux frames suivantes (aujourd'hui une étape = un `glCompileShader` bloquant). Fait : lecture, `#include` et `#define` sur les workers (`THREAD_PRELOAD_MAIN_LOAD`), compilation et liaison en étapes séparées (`DataType.loadContinues`).
