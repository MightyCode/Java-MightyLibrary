# Gros point actuel — API : corrections urgentes

**Ce fichier ne contient que ce qui reste à faire, jamais l'historique.** Quand un objectif est atteint (même en partie) : supprimer sa ligne, ou la réécrire pour ne garder que le reste. Aucun « fait », « déjà », « aujourd'hui fait X » : l'état actuel est décrit dans `architecture.md`, l'historique est dans git. Fichier vide → **discuter** avec l'utilisateur du point en tête de `later.md`, puis l'écrire ici. Propositions **à valider avec l'utilisateur avant d'appliquer** ; changements cassants maintenant, avant toute version publiée. (U) = le plus urgent.

- **Champs publics modifiables** (`Resources.Loaders`, `GUIList.GUIs`, `ShouldLoop`, `WindowCreationInfo.Size`…) → méthodes.
- **Shaders dans un `GLElement`** : programme avec références et suivi `gl` (prépare l'injection de shaders et leur rechargement à chaud) ; bibliothèque GLSL lue par les workers au lieu de `ShaderLoader.create`.
