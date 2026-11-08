# Futures discussions

Sujets absents du code et de la feuille de route, **sans ordre** : l'utilisateur décide quand. Après discussion, rattacher le sujet à un point de `later.md` (ou en créer un) et le retirer d'ici.

Déjà dans le moteur (ne pas reproposer) : A*, Dijkstra, tweens, son positionné (`SoundListener`/`SoundSource`), dossier de données utilisateur (`utils/DataFolder`), plein écran et vsync partiels (`Window`), rechargement à chaud des ressources et observateur de fichiers, préchargement de la scène suivante.

## Jeu 2D
- **Pixel perfect** : agrandissement entier de la résolution virtuelle, bandes noires, caméra calée sur les pixels ; plein écran / fenêtré sans bordure / vsync. `Window.setFullscreen` en cours de jeu est cassé (sans appelant aujourd'hui) : il détruit la fenêtre, donc le contexte GL et les callbacks d'entrées, sans la recréer ; à refaire avec `glfwSetWindowMonitor`, qui garde le contexte.
- **Sauvegardes et réglages** : emplacements, version et migration, écriture sûre (temporaire puis renommage), dossier par système ; réglages persistants (volumes, touches, gamma, langue). Requis par `save.get/set` en Lua.
- **Import Tiled** (`.tmx`/`.tmj`) : le moteur a son propre format (`resources/map`), le projet C++ plaçait ses entités depuis Tiled.
- **Transitions et pile de scènes** : fondus, volets ; pause par-dessus le jeu sans le décharger.
- **Caméra 2D de jeu** : zone morte, anticipation, limites, zoom, tremblement (voir d'abord `Camera2D`).
- **Son** : groupes de volume (musique, effets, voix), fondus enchaînés, variation de hauteur des effets répétés.
- **Animations** : machine à états ; import Aseprite.

## Production et distribution
- **Livrer** : `jpackage`/`jlink` (Java intégré) ; macOS `-XstartOnFirstThread` ; prérequis Steam.
- **Journal fichier et rapports de plantage**.
- **CI** (GitHub Actions) : compiler, tester, publier le jar.
- **Documentation de la bibliothèque** : démarrage rapide, exemples, Javadoc de l'API publique.

## Qualité
- **Aucune allocation par frame** (pools, réutilisation) comme règle générale, pas seulement pour les particules.
- **Texte avancé** : polices de secours (accents, CJK), texte enrichi (couleurs, icônes de touches), dialogues lettre par lettre.
- **Accessibilité** : sous-titres, taille du texte, modes daltoniens (remappage déjà prévu).
