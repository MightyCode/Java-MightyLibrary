# Gros point actuel — API : corrections urgentes

**Ce fichier ne contient que ce qui reste à faire, jamais l'historique.** Quand un objectif est atteint (même en partie) : supprimer sa ligne, ou la réécrire pour ne garder que le reste. Aucun « fait », « déjà », « aujourd'hui fait X » : l'état actuel est décrit dans `architecture.md`, l'historique est dans git. Fichier vide → **discuter** avec l'utilisateur du point en tête de `later.md`, puis l'écrire ici. Propositions **à valider avec l'utilisateur avant d'appliquer** ; changements cassants maintenant, avant toute version publiée. (U) = le plus urgent.

- (U) **Pipeline de `Resources`** (voir `architecture.md`). Reste :
  - **estimations de poids** par type pour les modèles et les sons (aujourd'hui : taille du fichier) ; les dépendances d'une donnée non lue ne sont connues qu'après sa lecture (`dependenciesKnown`) ;
  - `DataType` lit `Resources.getInstance()` dans son constructeur (cf. singletons).
- **Singletons statiques** (`Resources`, `GLResources`, `ShaderManager`, `ContextManager`, `SoundManager`) et instance par défaut silencieuse de `Resources.getInstance()` → contexte moteur, ou au minimum échec explicite. Lié à l'injection (`later.md`).
- **IDs d'action `int` statiques** → identifiants texte à espace de noms enregistrés au registre (lié au remappage et au modding).
- **Champs publics modifiables** (`Resources.Loaders`, `GUIList.GUIs`, `ShouldLoop`, `WindowCreationInfo.Size`…) → méthodes.
- **Noms fautifs** : package `lightning` (= `lighting`), `getCurrentTIme`, `canBeLoad`…
- **Shaders dans un `GLElement`** : programme avec références et suivi `gl` (prépare l'injection de shaders et leur rechargement à chaud) ; bibliothèque GLSL lue par les workers au lieu de `ShaderLoader.create`.
