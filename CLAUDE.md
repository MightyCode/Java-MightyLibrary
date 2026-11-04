# CLAUDE.md — Java-MightyLibrary

Moteur de jeu 2D/3D en Java 11 (LWJGL 3.3.6, OpenGL 3.3 core, GLFW, OpenAL, STB, Assimp ; JOML, org.json, JBox2D). `src/MightyLibrary/mightylib` = la bibliothèque, `src/MightyLibrary/project` = la démo qui sert de banc d'essai. Objectif : en faire une bibliothèque réutilisable (jar) pour les futurs jeux et logiciels de l'utilisateur.

## Documentation (à lire seulement quand la tâche le demande)
- `docs/roadmap.md` : feuille de route ordonnée (prochaine étape : discussions de `docs/discussions.md`, puis corrections urgentes de l'API à valider avec l'utilisateur), modèle d'extension, modding.
- `docs/discussions.md` : sujets à discuter et planifier avec l'utilisateur (chargement de modèles 3D, pistes LearnOpenGL).
- `docs/architecture.md` : structure, démarrage, boucle, cycle de vie des scènes, ressources CPU/GPU, rendu, entrées, temps, pièges connus.
- `docs/debug-tools.md` : commandes, serveur de debug, simulation d'entrées, traque des fuites, profilage (JFR, RenderDoc).

## Priorités
- **La 2D est prioritaire** (rendu 2D, tilemaps, texte, GUI, Camera2D, animations, physique 2D).
- **La 3D est secondaire** : ne pas l'étendre ni la refactorer sans demande, mais ne pas la casser (`Shape`/`Renderer`/`Shader` sont communs ; les scènes `Test3D*` doivent rester fonctionnelles).

## Build et vérification
- Maven avec `sourceDirectory` = `src`. Pas de tests automatisés. `mvn`/`java` ne sont pas dans le PATH : compiler avec les outils d'IntelliJ, après chaque modification :
  ```bash
  JAVA_HOME="$HOME/.jdks/ms-21.0.9" "/d/Program Files/JetBrains/IntelliJ IDEA Community Edition 2024.2.3/plugins/maven/lib/maven3/bin/mvn" -o -q compile
  ```
- Vérifier en jeu avec **un seul appel** : `python tools/scenario.py "scene Test2DScene" wait-loaded "gl" "input ENTER"` (compile, lance le jeu, enchaîne les commandes, résume en texte, ferme le jeu ; une fenêtre s'ouvre chez l'utilisateur : le prévenir). Détails : `python tools/scenario.py --help` et `docs/debug-tools.md`.
- Répertoire de travail = racine du dépôt (ressources lues depuis `resources/`). Point d'entrée : `MightyLibrary.project.main.Main`.

## Règles pour Claude
- **Économiser les tokens** : lire des extraits (Grep, Read avec offset/limit) plutôt que des fichiers entiers ; filtrer les journaux ; réponses courtes ; pas de capture d'écran sauf rendu impossible à vérifier autrement (une seule à la fois) ; si une information manque, ajouter une commande qui la renvoie en texte/JSON.
- Ne jamais importer `MightyLibrary.project.*` depuis `mightylib` (passer les IDs en paramètre).
- Tout code GL s'exécute sur le thread principal ; les workers de `Resources` ne font que de l'I/O/CPU. Toujours créer/détruire les objets GL via `GenDeleteResources`.
- Toute ressource GL créée dans une scène est libérée dans `unload()` ; vérifier les fuites avec `gl` (voir `docs/debug-tools.md`).
- Fins de ligne normalisées par `.gitattributes` (LF dans le dépôt) : `git add` normal. Ne pas réécrire de fichier source avec `sed` (utiliser Edit).
- Git : travailler sur une branche, un commit par changement logique, avec la ligne `Co-Authored-By` demandée par le système. Ne pas modifier/commiter `target/` ni `.idea/workspace.xml`.
- Décrire comment vérifier manuellement ce qui n'a pas pu l'être (quelle scène, quoi observer).

## Conventions de code
- Java 11 (pas de `record`, ni de `switch` expression). Accolades K&R, 4 espaces, commentaires en anglais, peu de Javadoc.
- Enums préfixées `E`, interfaces préfixées `I` ; utilitaires statiques souvent en PascalCase (`GenDeleteResources.GenBuffers`, `Logger.CheckOpenGLError`).
- Appel GL sensible suivi de `Logger.CheckOpenGLError("message")` ; dans le code appelé à chaque dessin, l'entourer de `if (Logger.DO_CHECK)`.
- Logs moteur : `System.out/err.println` avec préfixe `--`.
- Pour tester une fonctionnalité, ajouter une scène `Test*` plutôt que modifier une existante.
