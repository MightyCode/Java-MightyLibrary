# Futures discussions

Sujets absents du code et de la feuille de route, **sans ordre** : l'utilisateur décide quand. Après discussion, rattacher le sujet à un point de `later.md` (ou en créer un) et le retirer d'ici.

Déjà dans le moteur (ne pas reproposer) : A*, Dijkstra, tweens, son positionné (`ISoundListener`/`ISoundSource`, OpenAL ou muet), dossier de données du joueur (couche du joueur de `ConfigData`, `EngineConfig.setPlayerDataFolder`), modes d'affichage fenêtré / plein écran / sans bordure par fenêtre (`DisplaySettings`), rechargement à chaud des ressources et observateur de fichiers, préchargement de la scène suivante.

## Jeu 2D
- **Pixel perfect** : agrandissement entier de la résolution virtuelle, bandes noires, caméra calée sur les pixels.
- **Sauvegardes et réglages** : `ConfigData` (configs en couches, couche du joueur `data/configs/`, écriture sûre) existe ; reste l'emplacement par système (dossier de données de l'utilisateur), les versions et la migration, les sauvegardes de partie, un menu d'options, et les autres réglages persistants (volumes, touches, langue). Requis par `save.get/set` en Lua.
- **Import Tiled** (`.tmx`/`.tmj`) : le moteur a son propre format (`resources/map`), le projet C++ plaçait ses entités depuis Tiled.
- **Transitions et pile de scènes** : fondus, volets ; pause par-dessus le jeu sans le décharger.
- **Caméra 2D de jeu** : zone morte, anticipation, limites, zoom, tremblement (voir d'abord `Camera2D`).
- **Son** : groupes de volume (musique, effets, voix), fondus enchaînés, variation de hauteur des effets répétés.
- **Animations** : machine à états ; import Aseprite.

## Réseau
- **Permissions par joueur** : opérateur, administrateur, triche autorisée par le serveur ; le niveau des commandes (`ECommandLevel`) ne vaut qu'en local, en réseau, c'est le serveur qui décide.

## Production et distribution
- **Livrer** : installeur `jpackage` (Java intégré) ; prérequis Steam (l'image `jlink`, macOS et la publication du jar : `later.md`, « Distribution et versions »).
- **Journal fichier et rapports de plantage**.

## Rendu
- **Version d'OpenGL/GLSL au-delà de 3.3 core** : 4.3+ apporte compute shaders, SSBO, rendu indirect (particules, culling, batching sur GPU) mais exclut macOS (4.1 maximum, OpenGL déprécié) ; 4.1 n'apporte presque rien. Choix de plateformes : garder 3.3 partout, chemin optionnel 4.3 détecté au démarrage avec repli 3.3 (par remplacement du service des shaders : son prélude écrit le `#version`, `IShaderService.prelude` ; un define global peut signaler le chemin 4.3 aux fichiers), ou abandon de macOS en OpenGL (Vulkan/MoltenVK plus tard).

## Qualité
- **Aucune allocation par frame** (pools, réutilisation) comme règle générale, pas seulement pour les particules.
- **Texte avancé** : polices de secours (accents, CJK), texte enrichi (couleurs, icônes de touches), dialogues lettre par lettre.
- **Accessibilité** : sous-titres, taille du texte, modes daltoniens (remappage déjà prévu).
