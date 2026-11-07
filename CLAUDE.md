# CLAUDE.md — Java-MightyLibrary

Moteur de jeu 2D/3D en Java 11 (LWJGL 3.3.6, OpenGL 3.3 core, GLFW, OpenAL, STB, Assimp ; JOML, org.json, JBox2D). `src/io/github/mightycode/mightylib` = la bibliothèque, `src/io/github/mightycode/mightylibdemo` = la démo qui sert de banc d'essai. Objectif : en faire une bibliothèque réutilisable (jar) pour les futurs jeux et logiciels de l'utilisateur.

## Documentation (à lire seulement quand la tâche le demande)
- `docs/current.md` : seul le gros point en cours (API : corrections urgentes, à commencer par le chargement de modèles par Assimp).
- `docs/later.md` : feuille de route globale, gros points suivants dans l'ordre (gamma, tests, bibliothèque réutilisable avec modèle d'extension et modding, effets 2D, physique 2D, entités et scripts Lua, thème, rendu 2D, SVG, entrées, son, console, plateformes).
- `docs/techniquegraphique.md` : suite de `later.md`, techniques de rendu 2D/3D de LearnOpenGL (Blinn-Phong, skybox, culling, ombres, normal mapping, animation squelettique, HDR/bloom…).
- `docs/futur-discussion.md` : sujets à discuter un jour, sans ordre (l'utilisateur décide quand).
- `docs/decisions.md` : décisions de conception durables (pas de Lombok, package `network` conservé…).
- `docs/architecture.md` : structure, démarrage, boucle, cycle de vie des scènes, ressources CPU/GPU, rendu, entrées, temps, pièges connus.
- `docs/debug-tools.md` : commandes, serveur de debug, simulation d'entrées, traque des fuites, profilage (JFR, RenderDoc).

## Priorités
- **La 2D est prioritaire** (rendu 2D, tilemaps, texte, GUI, Camera2D, animations, physique 2D).
- **La 3D est secondaire** : ne pas l'étendre ni la refactorer sans demande, mais ne pas la casser (`Shape`/`Renderer`/`Shader` sont communs ; les scènes `Test3D*` doivent rester fonctionnelles).

## Build et vérification
- Maven avec `sourceDirectory` = `src`, tests JUnit 5 dans `test/` (mêmes packages que le code testé, pour accéder au package-private). Compiler après chaque modification :
  - Linux : `mvn`/`java` (JDK 21) sont dans le PATH : `mvn -o -q compile`.
  - Windows : `mvn`/`java` ne sont pas dans le PATH, utiliser les outils d'IntelliJ :
    ```bash
    JAVA_HOME="$HOME/.jdks/ms-21.0.9" "/d/Program Files/JetBrains/IntelliJ IDEA Community Edition 2024.2.3/plugins/maven/lib/maven3/bin/mvn" -o -q compile
    ```
  - Tests : même commande avec `test` au lieu de `compile` (quelques secondes, sans fenêtre ni GL).
  - `scenario.py` choisit ces chemins selon l'OS (surchargeables par `MIGHTY_JAVA`, `MIGHTY_MVN`, `MIGHTY_MAVEN_JDK`). Sur une nouvelle machine, lancer une fois `mvn -q compile` (sans `-o`) pour télécharger les dépendances.
- **Tests automatisés, à chaque modification** : se demander si elle touche du code couvert par les tests (aujourd'hui : `resources/`, `main/MainThread`, `main/Workers`) ; si oui, relancer `mvn -o -q test` avant de commiter. Se demander aussi si elle entre dans ce que les tests peuvent couvrir (logique sans GL : états, références, files, parsing, maths…) ; si oui, ajouter ou compléter le test, et pour un bug corrigé, un test qui le reproduit. Un bug connu non corrigé reste en test `@Disabled` avec sa raison, pas supprimé.
- Vérifier en jeu avec **un seul appel** : `python tools/scenario.py "scene Test2DScene" wait-loaded "?gl summary" "input ENTER"` (compile, lance le jeu, enchaîne les commandes, résume en texte, ferme le jeu ; une fenêtre s'ouvre chez l'utilisateur : le prévenir). Détails : `python tools/scenario.py --help` et `docs/debug-tools.md`.
- Répertoire de travail = racine du dépôt (ressources lues depuis `resources/`). Point d'entrée : `io.github.mightycode.mightylibdemo.main.Main`.

## Règles pour Claude
- **Penser à long terme, à l'échelle d'un vrai jeu** (des milliers de ressources, chargement en continu, nombreuses scènes, mods, projets tiers), pas à celle de la démo : une solution qui suffit à la démo mais ne tiendra pas à cette échelle doit être signalée comme telle, avec la cible durable, et inscrite dans `docs/current.md` ou `docs/later.md`. Préférer corriger le modèle qui produit les bugs plutôt qu'empiler des pansements.
- **Économiser les tokens** : lire des extraits (Grep, Read avec offset/limit) plutôt que des fichiers entiers ; filtrer les journaux ; réponses courtes ; pas de capture d'écran sauf rendu impossible à vérifier autrement (une seule à la fois, et seulement la zone utile avec `screenshot rect x y largeur hauteur` ou `corners x y x2 y2` quand on sait où regarder ; cadrer avec la commande `camera`) ; si une information manque, ajouter une commande qui la renvoie en texte/JSON.
- Ne jamais importer `io.github.mightycode.mightylibdemo.*` depuis `mightylib` (passer les IDs en paramètre).
- Tout code GL s'exécute sur le thread principal (y revenir par `MainThread.execute`) ; les tâches de `Workers` (dont les workers de `Resources`) ne font que de l'I/O/CPU. Pas de `new Thread` pour une tâche courte : utiliser `Workers`. Toujours créer/détruire les objets GL via `GenDeleteResources`.
- Toute ressource GL créée dans une scène est libérée dans `unload()` (ce que `launch` a créé) ou `release()` (ce que `init` a créé : `unload` n'est pas appelé si la scène est quittée pendant son chargement) ; vérifier l'état CPU des ressources avec la commande `resources` ; vérifier les fuites avec l'étape `leakcheck <Scène>` de `scenario.py` (voir `docs/debug-tools.md`).
- Fins de ligne normalisées par `.gitattributes` (LF dans le dépôt) : `git add` normal. Ne pas réécrire de fichier source avec `sed` (utiliser Edit).
- Git : travailler sur une branche, un commit par changement logique, avec la ligne `Co-Authored-By` demandée par le système. Ne pas modifier/commiter `target/` ni `.idea/workspace.xml`.
- Avant de commencer un gros point (passage de `later.md` à `current.md`), en discuter avec l'utilisateur pour s'assurer que tout est prévu.
- Décrire comment vérifier manuellement ce qui n'a pas pu l'être (quelle scène, quoi observer).

## Conventions de code
- Java 11 (pas de `record`, ni de `switch` expression). Accolades K&R, 4 espaces, commentaires en anglais, peu de Javadoc.
- Enums préfixées `E`, interfaces préfixées `I` ; utilitaires statiques souvent en PascalCase (`GenDeleteResources.GenBuffers`, `Logger.CheckOpenGLError`).
- Appel GL sensible suivi de `Logger.CheckOpenGLError("message")` ; dans le code appelé à chaque dessin, l'entourer de `if (Logger.DO_CHECK)`.
- Logs moteur : `System.out/err.println` avec préfixe `--`.
- Pour tester une fonctionnalité, ajouter une scène `Test*` plutôt que modifier une existante.
