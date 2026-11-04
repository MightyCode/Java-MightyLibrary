# Futures discussions — Java-MightyLibrary

Sujets de moteur de jeu absents du code et de la feuille de route (`docs/later.md`, `docs/techniquegraphique.md`), relevés en fin de séance. **Aucun ordre** : l'utilisateur décide quand en discuter. Après discussion, rattacher chaque sujet à un gros point existant de `docs/later.md` ou en créer un, puis le retirer d'ici.

Déjà présents dans le moteur (ne pas reproposer) : A* et Dijkstra (`algorithms`), tweens, OpenAL positionné (`SoundListener`/`SoundSource`), dossier de données utilisateur (`utils/DataFolder`), plein écran et vsync partiels (`Window`).

## Fonctionnalités de jeu 2D
- **Rendu « pixel perfect » et résolutions** : résolution virtuelle agrandie par un facteur entier, bandes noires si le format d'écran diffère, caméra calée sur les pixels (pas de scintillement du pixel art) ; réglages plein écran / fenêtré sans bordure / vsync. Cohérent avec l'exigence « au pixel près ».
- **Sauvegardes et réglages** : emplacements, numéro de version et migration des anciennes sauvegardes, écriture sûre (fichier temporaire puis renommage, pas de corruption si plantage), dossier propre à chaque système ; persistance des réglages (volumes, touches, gamma, langue). L'API Lua `save.get/set` en dépend.
- **Import Tiled** (`.tmx` / `.tmj`) : le moteur a son propre format de tilemap (`resources/map`), alors que le projet C++ de l'utilisateur plaçait les entités depuis Tiled ; outil de level design standard.
- **Transitions et pile de scènes** : fondus et volets entre scènes ; menu pause par-dessus le jeu sans le décharger.
- **Caméra 2D de jeu** : suivi avec zone morte et anticipation, limites de la carte, zoom, tremblement ; vérifier d'abord ce que fait `Camera2D`.
- **Son** : groupes de volume (musique, effets, voix), fondus enchaînés entre musiques, légère variation de hauteur sur les effets répétés (le streaming est déjà prévu dans `later.md`, Son).
- **Animations** : machine à états (repos → course → saut) ; import Aseprite (outil de référence du pixel art).

## Production et distribution
- **Livrer le jeu** : `jpackage`/`jlink` pour un exécutable par système avec un Java réduit intégré (le joueur n'installe pas Java) ; macOS : `-XstartOnFirstThread` pour GLFW ; prérequis pour Steam.
- **Journal dans un fichier et rapports de plantage** : comprendre un bug chez un joueur.
- **Intégration continue** (GitHub Actions) : compiler et tester à chaque push, publier le jar ; à relier à « Bibliothèque réutilisable ».
- **Documentation de la bibliothèque** : démarrage rapide, exemples, Javadoc de l'API publique.
- **Licences et crédits** des dépendances et des assets (notamment le paquet de démonstration : sac à dos Sketchfab).

## Qualité
- **Pas d'allocation à chaque frame** : règle générale en Java contre les saccades du ramasse-miettes (pools, objets réutilisés) ; aujourd'hui notée seulement pour les particules.
- **Texte avancé** : accents et écritures CJK (polices de secours), texte enrichi (couleurs, icônes de touches dans le texte), boîtes de dialogue avec texte qui s'écrit lettre par lettre ; lié aux langues.
- **Rechargement à chaud de toutes les ressources** (textures, cartes, shaders), pas seulement des scripts Lua.
- **Accessibilité** : remappage (déjà prévu), sous-titres, taille du texte, modes daltoniens.
