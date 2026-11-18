# Kit de test pour les jeux

**Statut : validé.**

But : un jeu fait avec MightyLib doit pouvoir tester son propre code (loaders, commandes, services, scènes) avec JUnit, sans lancer le jeu à la main : un moteur de test qui démarre comme le jeu (mêmes plugins), sans GL ou avec une fenêtre cachée, qui avance frame par frame avec un temps fixé, simule les entrées et vérifie qu'aucune ressource ne fuit. Le moteur et la démo l'utilisent pour leurs propres tests (il est éprouvé avant d'être publié). C'est l'objectif « Kit de test pour les jeux » de `docs/current.md`.

Décisions prises :
- Module Maven à part `mightylib-test` (artefact `mightylib-test`), qui remplace le jar `tests` de `mightylib`.
- API du kit dans `io.github.mightycode.mightylib.test`, en `@API(status = EXPERIMENTAL)`.
- `Engine.setup()` découpé en un cœur sans GL et une partie fenêtre/GL ; le mode sans GL du kit lance le cœur seul.
- Les tests de scènes (vrai GL, fenêtre cachée) font partie de ce plan.
- API utilisable sans JUnit, plus une extension JUnit 5 ; JUnit en dépendance `optional` du kit.
- Les tests du moteur et de la démo migrent vers le kit (fin du jar `tests` et de la réflexion de `GameTimeControl`).
- Test avec GL sans écran ni contexte GL 3.3 core : ignoré, avec sa raison ; la CI Ubuntu les lance sous `xvfb` avec Mesa.
- Tag JUnit `gl` sur les tests avec GL (`-Dgroups=gl`, `-DexcludedGroups=gl`) ; `mvn test` les lance par défaut.
- `GameTime` lit une horloge injectable (temps réel par défaut, horloge manuelle pour le kit).
- Le corps de la boucle de `MainLoop` devient un tick et une frame appelables : le jeu et le kit passent par le même code.
- Sans GL, un puits d'éléments GL qui les enregistre sans les envoyer.
- Tests de scènes : GLFW et une fenêtre cachée une fois par JVM ; contrôle de fuites après chaque test.
- Le moteur de test part de l'`EngineConfig` du jeu ; le kit force : son muet, ni serveur de debug ni entrée standard, ressources strictes.
- Ressources d'un test : la pile du jeu, plus un dossier du test au-dessus (`addSource`, avant le démarrage).
- Entrées : API typée qui passe par les mêmes appels que la commande `input` (`InputSimulation`, `simulateKey`, `simulateMove`, `simulateText`).
- Noms : façade `MightyTest` (`headless(config)`, `withWindow(config)`), `TestEngine`, `SceneRunner` ; annotations `@MightyEngineTest` (sans GL) et `@MightySceneTest` (méta-annotée `@Tag("gl")`).
- `bom` gère `mightylib-test` ; `game-parent` l'ajoute d'office en portée `test`.
- La démo ne gagne pas de scène : elle gagne des tests qui servent d'exemples (sans GL, et `SceneRunner` sur `Scene2D`).
- Le kit n'est jamais dans le jar de production ; l'horloge injectable reste un accès package-private.

## Pour démarrer
- Lire : `docs/architecture.md` (structure des modules, « API publique », démarrage et boucle, ressources CPU/GPU, entrées), `docs/decisions.md` (modules JPMS, jar `tests`), `docs/debug-tools.md` (CI, `input`, `leakcheck`, `gl`).
- Branche de sujet depuis `dev` : `feature/test-kit`.
- Compiler et tester avec les commandes de `CLAUDE.md` (`package` dès que les poms changent) ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- Poms : `mightylib/pom.xml`, exécution `test-helpers` du `maven-jar-plugin` (`test-jar`, seulement `**/*TestAccess.class`) ; `bom/pom.xml` gère `mightylib` et son `test-jar` ; `demo/pom.xml` dépend du `test-jar` ; `base-parent` met `useModulePath=false` (Surefire, `default-testCompile`) ; pom racine : modules `bom`, `base-parent`, `game-parent`, `mightylib`, `demo`.
- Aides du jar `tests` : `main/MainThreadTestAccess` (`setCurrentAsMain`, `processAll`), `main/WorkersTestAccess` (`ensureThreads`).
- Aides restées dans les tests du moteur (`mightylib/src/test/java/...`) : `resources/LoaderTestAccess` (une `Resources` sur un dossier), `resources/DataTypeTestAccess` (`markLoaded`), `graphics/GLElementTestAccess` (`named`), `main/TestWindows` (`WindowContext` sans GLFW), `main/utils/GameTimeControl` (durée de frame par **réflexion** sur `GameTime`, utilisé par `TimerTest`, `FloatTweeningTest`, `Vector2fTweeningTest`), `resources/ResourcesTestBase` (`pump`, `frame`, capture de `System.err`).
- Test de la démo écrit à la main : `demo/src/test/.../resources/LayeredTextureLoaderTest`.
- `main/Engine.setup()` (un seul bloc) : validation, `MainThread`, `Workers`, mode, plugins (`EngineRegistry.Collect`), sources (`registrations.resourceSources`), bibliothèques natives, `GameTime.setFixedDeltaTime`, contexte principal (fenêtre, GL), `GenDeleteResources.Init`, `Resources` (puits `GLResources`, `IGLElementSink` dans `resources/internal`), commandes, services (`SilentSound` si `EngineConfig.SOUND_PROPERTY`), gestionnaire de scènes, `resources.init/load`, `lifecycle().started()`, scène de départ, console et serveur de debug (mode debug), icône.
- `main/MainLoop.loop` : branche tick (`GameTime.update`, `engine.update`) et branche frame (`DrawnTexts.beginFrame`, `displayWindows`, `resources.processUntil`, `MainThread.process`, `glResources.process`, `swapWindows`), stats `EngineStats`, événements JFR.
- `main/utils/GameTime` : statique, `System.nanoTime`, `fixedDeltaTime`.
- `scenes/Scene` crée caméra et `VirtualSceneRenderer` : une scène demande un contexte GL.
- État statique global : `MainThread`, `Workers`, `Views`, `GameTime`, `LogCapture`, `MainLoop` (mode), `Logger` (vérif GL), `TextureUploader` (mode).
- Simulation d'entrées : `utils/enginecommand/internal/InputGenCom` (`inputs/internal/InputSimulation`, `KeyboardManager.simulateKey/simulateKeyRelease/simulateText`, `MouseManager.simulateMove`…).
- Fuites : `scenario.py leakcheck` compare les compteurs de la commande `gl` (noms sans `#id`) et `sound.ownedSources`.
- CI : `.github/workflows/ci.yml`, `mvn -B verify` sur Ubuntu, JDK 21, sans affichage.
- Modèles extérieurs : libGDX (`gdx-backend-headless`), Bevy (`MinimalPlugins`, `app.update()`, `TimeUpdateStrategy::ManualDuration`), Godot (`--headless`, `SceneRunner` de gdUnit4), Unity Test Framework (EditMode/PlayMode). Point commun : artefact séparé, application sans rendu, avance par frame, temps manuel, entrées simulées ; écueil commun : l'état statique partagé entre les tests (d'où la remise à zéro par le kit).

## Étapes
Chaque étape compile, passe `mvn -o -q test` (et `package` si un pom change) et se commite seule.

1. **Plan** : commiter ce fichier tel quel.
2. **Horloge de `GameTime`** : `GameTime` lit le temps par une source (`LongSupplier` de nanosecondes, `System::nanoTime` par défaut), changée par un accès package-private ; durée de frame fixable sans réflexion. `GameTimeControl` passe par cet accès (provisoirement, jusqu'à l'étape 7). Tests : temps manuel avancé → `DeltaTime`, `currentTime`, `Timer`. Vérif en jeu : `scenario.py "scene Scene2D" wait-loaded "?gl summary"`.
3. **Tick et frame de `MainLoop`** : extraire `tick(engine)` et `frame(engine, deadline)` (package-private, mêmes appels dans le même ordre, stats et événements JFR compris) ; `loop` ne fait plus que choisir lequel appeler. Comportement inchangé. Vérif en jeu : `Scene2D`, `MultiWindowScene` (D, T, C), `leakcheck Scene2D`.
4. **Cœur sans GL de `Engine`** : `setup()` = `setupCore(sink)` (validation, threads, plugins, sources, `Resources` avec le puits donné, commandes, services, `GameTime.setFixedDeltaTime`) puis `setupWindow()` (natives, fenêtre, GL, `GLResources`, gestionnaire de scènes, chargement, scène de départ, console, debug, icône). Le cœur seul : pas de gestionnaire de scènes (`sceneManager()` nul, `scenes()` disponible), vue des commandes = le moteur, `shutdown` libère seulement ce qui a été créé. Vérifier tôt si un `InputManager` peut exister sans fenêtre (actions sans GL) ; sinon, entrées seulement en mode fenêtre (le noter). Tests (dans `mightylib`, aides internes) : cœur démarré avec un plugin de test → commande exécutée, loader enregistré, service créé, son muet. Vérif en jeu : `Scene2D`, `scenario.py --modulepath "scene Scene2D" wait-loaded`.
5. **Module `mightylib-test`, mode sans GL** : nouveau module (pom racine, `bom`, `game-parent` en portée `test`, démo) ; supprimer l'exécution `test-helpers` et le `test-jar`. Package `io.github.mightycode.mightylib.test` (`package-info` `EXPERIMENTAL`) : `MightyTest.headless(EngineConfig)` → `TestEngine` (`addSource(Path)` avant `start()`, `start()`, `step()`/`step(n)` = tick + frame sans attente, temps manuel `advance(seconds)`, `execute(commande)`, `resources()`, `loadUntilDone()` avec délai, éléments GL enregistrés, `close()`) ; surcharges forcées (son muet, pas de debug ni d'entrée standard, ressources strictes) ; remise à zéro de l'état statique à `close()` (`MainThread`, `Workers`, `Views`, horloge, `LogCapture`, mode). Ponts vers le package-private dans les packages du moteur (packages coupés : classpath seulement, comme les tests). Extension JUnit 5 et `@MightyEngineTest` (moteur injecté en paramètre, fermé après chaque test). Pas de `module-info` pour le kit. Tests du kit dans `mightylib-test/src/test/java`. `mvn -o -q package`.
6. **Mode fenêtre : `SceneRunner`** : `MightyTest.withWindow(EngineConfig)` ; GLFW et une fenêtre cachée créés une fois par JVM (`GLFW_VISIBLE` faux), réutilisés ; sans écran ou sans contexte GL 3.3 core : `Assumptions` (test ignoré avec la raison). `SceneRunner` : `start(scène, args)`, `untilLoaded()`, `frames(n)`, entrées (`press(action, ticks)`, `hold/release`, `key`, `click`, `mouse`, `scroll`, `text`) par les appels de `InputGenCom`, `execute`. Après chaque test : retour à la scène vide, puis compteurs de `gl` (sans `#id`), ressources référencées et `sound.ownedSources` comparés à ceux d'avant (échec qui les nomme). `@MightySceneTest` (méta-annoté `@Tag("gl")`). Test : une scène de test du kit chargée, frames, touche simulée, aucune fuite.
7. **Migration des tests du moteur** : les aides `*TestAccess`, `TestWindows`, `GameTimeControl`, `ResourcesTestBase` passent par le kit ou restent des aides internes des tests du moteur selon la question restante ; plus de réflexion sur `GameTime`. Tous les tests passent, nombre de tests inchangé ou plus grand.
8. **Démo** : `LayeredTextureLoaderTest` réécrit avec le kit ; un test sans GL (commandes et ressources de la démo par `TestEngine`) ; un test `@MightySceneTest` sur `Scene2D` (chargement, quelques frames, une touche, aucune fuite). Ce sont les exemples pour un jeu.
9. **CI** : `ci.yml` installe `xvfb` et Mesa (`libgl1-mesa-dri`), lance `xvfb-run -a mvn -B verify` ; mettre à jour son commentaire (« sans fenêtre ni OpenGL »). Un test vérifie qu'un contexte GL 3.3 core est obtenu sous Mesa ; sinon les tests `gl` sont ignorés et le disent. `scenario.py` surveille le pom du nouveau module.
10. **Documents** (voir plus bas), sans supprimer ce plan.

## Démo
Pas de nouvelle scène : les tests de l'étape 8 (`demo/src/test/java`) servent d'exemples de tests d'un jeu.

## Documents à mettre à jour à la fin
- `architecture.md` : module `mightylib-test` (structure des modules), cœur sans GL et partie fenêtre de `Engine`, tick et frame de `MainLoop`, horloge de `GameTime`, kit (modes, remise à zéro, contrôle de fuites, tag `gl`) ; retirer le jar `tests`.
- `debug-tools.md` : CI sous `xvfb`, tests `gl` (`-Dgroups`, `-DexcludedGroups`), fin de « Les tests n'ont besoin ni de fenêtre ni de GL » et de `WorkersTestAccess`.
- `decisions.md` : remplacer « Aides de test de la bibliothèque dans un jar `tests` » par la décision du kit (module à part, packages coupés, classpath seulement, `EXPERIMENTAL`, horloge package-private).
- `later.md` : tests en module (chemin de modules), macOS (`-XstartOnFirstThread` pour les tests `gl`), comparaison d'images (tests de rendu), passage du kit en `STABLE` avant la première version publiée ; serveur dédié possible sur le cœur sans GL.
- `CLAUDE.md` : tests (« sans fenêtre ni GL » → tests `gl` en fenêtre cachée, `-DexcludedGroups=gl`), liste des zones couvertes (kit, cœur sans GL, tick/frame, horloge), dépendance de la démo, poms (`mightylib-test`).
- `README` (exemple de jeu) : dépendance de test fournie par `game-parent`, exemple de test.
- `licence.md` : rien de nouveau (JUnit déjà listé) ; à vérifier.
- `current.md` : supprimer la ligne « Kit de test pour les jeux ».
- Supprimer ce plan (discussion B).

## Hors périmètre
- macOS (`-XstartOnFirstThread`) : non testé, comme les autres natives macOS (`later.md`).
- Tests sur le chemin de modules : le kit est classpath seulement (`later.md`).
- Comparaison d'images rendues (tests de rendu au pixel près) : `later.md`.
- Serveur dédié sur le cœur sans GL : préparé, pas fait (`later.md`).

## Questions restantes
- **Cycle Maven** : `mightylib-test` dépend de `mightylib`, donc les tests de `mightylib` ne peuvent pas dépendre du kit (cycle interdit dans le réacteur). Recommandation : les tests qui demandent le moteur entier (moteur de test, scènes) vivent dans `mightylib-test/src/test/java` ; les tests unitaires de `mightylib` gardent de petites aides internes dans leurs propres sources de test (non publiées). À confirmer avant l'étape 5.

## Modèles recommandés
- Étapes 2 à 6 (horloge, boucle, découpage de `Engine`, API du kit, fenêtre partagée et fuites) : Claude Opus 5.5, effort `high` (conception d'une API durable, cycle de vie délicat du moteur).
- Étapes 7 à 10 (migration des tests, démo, CI, docs) : Claude Sonnet 5.5, effort `medium` (travail mécanique guidé par le plan).
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (gros diff qui touche le démarrage du moteur et les poms).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/kit-de-test.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/test-kit`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`Scene2D`, `MultiWindowScene` (D, T, C), `leakcheck Scene2D`, lancement `--modulepath`, `mvn test` sans fenêtre visible, CI verte avec les tests `gl` lancés et non ignorés).
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/test-kit`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
