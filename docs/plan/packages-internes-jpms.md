# Plan — Packages internes et modules Java

**Statut : à valider** (discussion terminée, en attente des dernières remarques).

Décisions prises :
- Types `INTERNAL` déplacés dans `<package>.internal`, à côté de leur propriétaire.
- Les deux packages déjà entièrement `INTERNAL` (`utils.enginecommand.general`, `physics.tweenings.tweeningmath`) sont renommés en `.internal` ; règle unique : un package nommé `internal` ⇔ statut `@API(INTERNAL)` (vérifiée par un test).
- Les membres publics de types `STABLE` qui exposent un type interne (`Engine.consoleLog()`, `Window.getGLContext()`/`createNewWindow`, `GLResources.shared`, `Resources.setGLElementSink`, `Console.getLog`, les `Delete…(GLContext, …)` de `GenDeleteResources`) restent, annotés `INTERNAL` ; l'alerte `-Xlint:exports` est acceptée (déclarée dans le pom avec sa raison).
- `module-info` pour `mightylib` (module `io.github.mightycode.mightylib`) et pour la démo (`io.github.mightycode.mightylibdemo`), la démo en dernière étape avec repli (la laisser sur le classpath et le noter) si les natives ou les modules automatiques bloquent.
- JBox2D (module automatique `jbox2d.library`) : `requires`, alerte `requires-automatic` acceptée et déclarée dans le pom ; `requires transitive` si ses types sont dans l'API publique de `physics`. `org.json` 20260719, JOML, LWJGL et apiguardian sont déjà modulaires.
- Ressources du moteur : `opens assets.mightylib` dans le module ; chaque module de jeu déclare `opens assets.<plugin>` (à documenter).
- Vérification : tests JUnit sur le classpath (`useModulePath=false`, accès package-private inchangé) ; `scenario.py` garde le classpath par défaut et gagne `--modulepath` ; la compilation modulaire de la démo valide les exports.

## Pour démarrer
- Lire : `docs/architecture.md` (« API publique »), `docs/decisions.md` (statut de l'API, JPMS), `docs/debug-tools.md` (`scenario.py`), `docs/current.md`.
- Branche de sujet depuis `dev` : `feature/internal-packages-jpms`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; `package` à lancer (la démo est compilée contre le jar) ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur.

## Contexte : ce qui existe
- Chemins de base : `mightylib/src/main/java/io/github/mightycode/mightylib/` (noté `ml/`).
- Types `INTERNAL` dans des packages `STABLE` (annotés un par un) : `ml/graphics/GLContext`, `IGLSharedState` ; `ml/resources/IGLElementSink`, `BatchLoader`, `BatchResources`, `ResourceArguments` ; `ml/main/WindowPlatform` ; `ml/inputs/InputSimulation` ; `ml/utils/enginecommand/DebugServer` ; `ml/console/ConsoleLog`, `ConsoleInput`, `LogCapture`, `CommandEcho`.
- Packages entièrement `INTERNAL` : `ml/utils/enginecommand/general` (les commandes générales `*GenCom`, `ListGeneralCommand`) et `ml/physics/tweenings/tweeningmath` (`Back`, `Bounce`…).
- Membres `INTERNAL` listés plus haut (`Engine`, `Window`, `GLResources`, `Resources`, `Console`, `GenDeleteResources`).
- Un `package-info.java` par package (statut `@API`), une cinquantaine de packages ; `ApiStatusTest` vérifie qu'aucune annotation n'est plus stable que son type ou son package.
- Tests de la bibliothèque (`mightylib/src/test/java`, mêmes packages que le code testé) qui nomment les types à déplacer : `GLResourcesTest` (`IGLSharedState`), `ResourcesGLElementsTest` (`IGLElementSink`), `WindowPlatformTest`, `AxisTest`/`GamepadManagerTest` (`InputSimulation`), `FileWatchTest`, `ListMergeTest`, `MultiSourceDataTest`, `ResourceLoadersTest`, `ResourceSelectionTest`, `ResourcesEngineBatchTest`, `ResourcesRandomTest`, `ResourcesTestBase`, `ResourcesBatchTest` (`BatchLoader`, `BatchResources`, `ResourceArguments`), `ConsoleInputTest`, `ConsoleLayoutTest`, `ConsoleLogTest`, `LogSelectionTest`, `CompletionListingTest`, `CommandLevelsTest` (console), `ExtensibleClasses` (`BatchResources`). Dans la démo : `DemoApiUsageTest`.
- `ClasspathSource` (`ml/resources/source/`) lit `assets/<plugin>/…` par `ClassLoader.getResource` et ouvre les jars par `FileSystems.newFileSystem` (module `jdk.zipfs`). Le pom parent compile en Java 11 (`maven.compiler.source/target`) avec Error Prone ; le module `demo` dépend du jar `tests` de `mightylib` (aides `*TestAccess` dans les packages du moteur : à laisser sur le classpath).
- `scenario.py` lance la démo avec `-cp` (classes de la démo + `target/classpath.txt`).

## Étapes (un commit chacune)
1. **Renommer les deux packages déjà internes** : `utils.enginecommand.general` → `utils.enginecommand.internal`, `physics.tweenings.tweeningmath` → `physics.tweenings.internal` (IDE ou `git mv` + imports ; ne pas réécrire avec `sed`). Mettre à jour les références dans les docs au moment de l'étape 6. Compiler, tester. Peut être fusionnée seule.
2. **Déplacer les 13 types** dans `console.internal`, `resources.internal`, `graphics.internal`, `main.internal`, `inputs.internal`, `utils.enginecommand.internal` (avec `DebugServer`, à côté des commandes générales). Chaque nouveau package : `package-info.java` `@API(status = INTERNAL)` ; retirer l'annotation de chaque type devenue redondante ; les membres `INTERNAL` des types publics gardent la leur. Rendre public ce qui était package-private et que l'ancien package utilisait (le relever à la compilation ; ne rien ouvrir de plus que nécessaire). Déplacer les tests de ces types dans le même package `.internal` (accès package-private conservé), mettre à jour `ExtensibleClasses` et `DemoApiUsageTest`. Tests JUnit existants à relancer en entier.
3. **Règle vérifiée** : `ApiStatusTest` — un package dont le dernier segment est `internal` est `INTERNAL`, et tout package `INTERNAL` est nommé `internal` ; la liste des types `INTERNAL` de `architecture.md` est relue au regard du test.
4. **`module-info` de `mightylib`** : `ml/../module-info.java` (`src/main/java`), `module io.github.mightycode.mightylib` : `exports` de tous les packages non `internal` (générer la liste puis la relire), `opens assets.mightylib` (si javac refuse un package sans classe, ajouter un marqueur ou ouvrir autrement : première expérience à faire), `requires transitive` pour ce qui figure dans l'API publique (`org.joml`, `org.json`, modules LWJGL concernés, `jbox2d.library` si exposé), `requires static org.apiguardian.api`, et `jdk.zipfs` (à confirmer par l'expérience : ce module doit être résolu à l'exécution). Pom parent : `-Xlint:-exports` et `-Xlint:-requires-automatic` avec leurs raisons, `maven-surefire-plugin` en `useModulePath=false` (parent et démo), vérifier que Error Prone, SpotBugs (`python tools/analyse.py`) et JaCoCo (`python tools/coverage.py`) marchent encore. Test `ModuleDescriptorTest` (lit `module-info.class` par `ModuleDescriptor.read`, classpath) : exports ⇔ packages non `INTERNAL`, `opens assets.mightylib`, `requires static` apiguardian. `mvn package`.
   - *Modèle : conception d'un contrat durable et pièges JPMS → modèle le plus capable.*
5. **Démo modulaire et lancement modulaire** : `demo/src/main/java/module-info.java` (`io.github.mightycode.mightylibdemo`, `requires` de la bibliothèque et de ce que la démo utilise directement, `opens assets.<plugin>` de la démo si elle a un plugin avec ressources dans le jar, `opens` des packages lus par réflexion le cas échéant). Retirer `DemoApiUsageTest` si le compilateur impose déjà la frontière (garder ce qu'il ne couvre pas : membres `INTERNAL`), sinon le garder. `scenario.py --modulepath` : `--module-path` (jars de `target/classpath.txt` + classes de la bibliothèque et de la démo) et `--module io.github.mightycode.mightylibdemo/…Main`, à la place de `-cp`. Vérifier en jeu (`python tools/scenario.py --modulepath "scene Scene2D" wait-loaded "?gl summary"`) : ressources du moteur trouvées (`opens`), jar ouvert (zipfs), natives chargées, démarrage de `Scene2D`. Repli si bloquant : démo sur le classpath, noter le blocage dans `later.md`.
   - *Modèle : problèmes de lancement et de natives à diagnostiquer → plus capable.*
6. **Documents** (liste ci-dessous) ; **ne pas supprimer ce plan**.

Tests JUnit à écrire : `ModuleDescriptorTest` (étape 4), règle `internal` de `ApiStatusTest` (étape 3) ; les tests existants déplacés (étape 2).

## Démo
Pas de nouvelle scène : la démo modulaire est l'acceptation (l'objectif suivant, « test d'acceptation », ajoute le projet de jeu externe).

## Documents à mettre à jour à la fin
- `architecture.md` : section « API publique » (types `INTERNAL` désormais dans des packages `internal`, plus de liste de types dans des packages `STABLE`, `module-info`, `opens`, exports, modules automatiques), démarrage modulaire.
- `decisions.md` : remplacer la décision « Pas de JPMS tant que… » par la décision prise (module, `internal`, membres exposés acceptés, `opens assets.<plugin>`, tests sur classpath).
- `debug-tools.md` : `scenario.py --modulepath`.
- `later.md` : ce qui reste (retirer les membres exposés de l'API publique par une passerelle interne ; `jlink`/image avec `jdk.zipfs` pour la distribution ; module automatique de JBox2D) ; rien d'historique.
- `current.md` : supprimer la ligne de l'objectif, et dans l'objectif « Distribution » ajouter le rappel de `jlink` si utile ; mettre à jour la liste « À décider » du test d'acceptation si besoin.
- `CLAUDE.md` : ajouter `ModuleDescriptorTest`, la règle `internal` d'`ApiStatusTest`, remplacer `DemoApiUsageTest` si retiré ; commande `scenario.py --modulepath`.
- `docs/bonnes-pratiques.md` : si un point cite `DemoApiUsageTest`.
- Supprimer ce plan (à la fusion, par `/pr-ml`).

## Hors périmètre
- Retirer de l'API publique les membres qui exposent un type interne (passerelle interne) : `later.md`.
- Image `jlink`, publication, versions, japicmp : objectif « Distribution et versions » de `current.md`.
- Pom starter, kit de test pour les jeux, projet d'acceptation externe : objectifs suivants de `current.md`.
- Mods en jars (`mods/`, `ServiceLoader`) sur le chemin de modules : `later.md` (modding).

## Questions restantes
- Aucune de bloquante. À constater au début de l'étape 4 : si `opens` d'un package sans classe est accepté par javac ; sinon l'expérience décide (marqueur ou ouverture au lancement).

## Modèles recommandés
- Discussion A : étapes 1 à 3 et 6 (déplacements mécaniques, tests, docs) : Sonnet 5.5, effort medium. Étapes 4 et 5 (contrat `module-info`, pièges de modules, lancement, natives) : Opus 5.5, effort high.
- Discussion B (`/pr-ml`) : Opus 5.5, effort high (relire un diff de déplacements au regard du plan et des exports).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/packages-internes-jpms.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change (avant l'étape 4).
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/internal-packages-jpms`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (démo lancée sur classpath puis avec `--modulepath` : `Scene2D`, une scène 3D, la console, une commande `resources`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/internal-packages-jpms`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
