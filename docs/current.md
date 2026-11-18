# Gros point actuel — Bibliothèque réutilisable

**Ce fichier ne contient que ce qui reste à faire, jamais l'historique.** Quand un objectif est atteint (même en partie) : supprimer sa ligne, ou la réécrire pour ne garder que le reste. Aucun « fait », « déjà », « aujourd'hui fait X » : l'état actuel est décrit dans `architecture.md`, l'historique est dans git. Fichier vide → **discuter** avec l'utilisateur du point en tête de `later.md`, puis l'écrire ici (format et cycle de vie : `later.md`, « Écrire `current.md` »). Propositions **à valider avec l'utilisateur avant d'appliquer** ; changements cassants maintenant, avant toute version publiée. Chaque objectif passe par `/planification-ml` avant son implémentation. (U) = le plus urgent.

Philosophie de `decisions.md` : usage courant facile (niveau Processing), réimplémentation (presque) complète possible par héritage + injection. Prérequis de la bibliothèque réutilisable, du modding, du thème et des langues, de Lua et de la physique injectable : chaque commit écrit sans elle ajoute du code à reprendre.

`mightylib` = base des futurs jeux et logiciels, `mightylibdemo` l'illustre. La démo compile contre le jar ; le moteur fournit ses shaders, ses polices et ses mappings de manettes dans sa source (`architecture.md`, « Source du moteur »), le reste de ce que lit la démo est dans `resources/` à la racine du dépôt. Passe avant le gamma, les mods, le thème, les langues et Lua (plusieurs sources de ressources). Fin du point : un projet de jeu hors du dépôt, dépendant du seul jar publié, lance une scène 2D.

- **Packages internes et modules Java** : déplacer les types `INTERNAL` des packages `STABLE` (`GLContext`, `ConsoleLog`, `BatchLoader`… : `architecture.md`, « API publique ») dans des packages à eux, puis `module-info` n'exportant que les packages non `INTERNAL` (`decisions.md`, JPMS).
  - À décider : nom des packages (`….internal`), dépendances non modulaires (JBox2D, org.json), modules des natives LWJGL ; `ClasspathSource` ouvre les jars en système de fichiers zip : le module `jdk.zipfs` doit être présent au runtime (`requires` ou image `jlink`).
- **Pom « starter » pour les jeux** : un jeu reçoit les jars Java de LWJGL par `mightylib`, mais recopie les profils d'OS (`lwjgl.natives`) et déclare lui-même les natives (comme `demo/pom.xml`).
  - À décider : pom parent ou pom « starter » (dépendance de type `pom`), ce qu'il fixe pour le jeu (Java, encodage, plugins).
- **Kit de test pour les jeux** : le jar `tests` de `mightylib` ne contient que les aides `*TestAccess` (thread principal, workers) des tests du moteur ; un jeu qui teste ses loaders, ses commandes ou ses scènes sans fenêtre a besoin d'un kit pensé pour lui (moteur sans GL, `Resources` de test, temps fixé).
  - À décider : artefact à part ou jar `tests`, statut `@API` du kit.
- **Distribution et versions** : `mvn install`, puis JitPack ou GitHub Packages ; versions sémantiques par release ; à partir de la première version publiée, japicmp compare l'API de chaque version à la précédente (les statuts `@API` disent ce qui peut changer).
  - À décider : dépôt de publication, numéro de la première version, contrôle japicmp dans la CI ou en local.
- **Test d'acceptation** : projet de jeu minimal hors du dépôt (pom starter, jar publié, ses propres ressources, un shader remplacé) qui lance une scène 2D.
