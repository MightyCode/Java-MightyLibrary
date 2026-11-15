# Plan — Services de l'`Engine` injectables

**Statut : validé.**

## Décisions prises (résumé)
- Les systèmes uniques du moteur deviennent des **services** injectés par le **registre des plugins** (section `services()`), remplaçables par `replace` par tout plugin.
- Un service = une **interface** (contrat) + une implémentation proposée ; un projet peut **remplacer** les services du moteur et **en ajouter**.
- Désignés par une **clé typée** `ServiceKey<T>` (`Identifier` + type du contrat) ; récupérés par `engine.service(clé)` ; raccourcis typés `engine.sound()`, `engine.console()`.
- **Dépendances déclarées** : `requires(clé)` (absente → erreur au démarrage) et `after(clé)` (ordre seulement si présente) ; tri topologique, cycle → erreur, ex aequo dans l'ordre d'enregistrement.
- **Échec** : une clé peut déclarer une fabrique de repli (son → muet) ; sans repli, arrêt avant la première scène (code `ListError` dédié), les dépendants ne sont pas créés. Jamais `null` pour le jeu (sauf la console sans accès aux commandes, comme aujourd'hui).
- Son : `ISoundService`, `ISoundSource`, `ISoundListener`, implémentations OpenAL et muettes ; propriété `-Dmightylib.sound=silent`.
- Gestionnaire de scènes (un par contexte) et d'entrées (un par fenêtre) : **fabriques** remplaçables dans la même section, pilotés comme aujourd'hui par `Context`/`WindowContext`.
- `ShaderManager` reste tel quel (chantier des shaders, `later.md`).
- Outils : commande `services` (DEV) et scène de démo `EngineServices2D`.

## Pour démarrer
- Lire : `docs/current.md` (ligne « Systèmes de l'`Engine` injectables »), `docs/decisions.md` (philosophie, point d'injection unique), `docs/architecture.md` (Démarrage, Boucle, Moteur et vue courante, Plugins et registre, Contextes et fenêtres, Entrées, Console), `docs/debug-tools.md` (Commandes).
- Branche de sujet depuis `dev` : `services-injectables`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `python tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `main/Engine.java`, `setup()` (l. 170) : crée en dur `ShaderManager` (l. 238), `SoundManager` (l. 258, échec d'`init` → `ListError.LIBRARIES_LOAD_FAIL`), `Commands`, `SceneManager` (l. 282, et l. 340 dans `openContext` pour chaque autre contexte), `Console` (l. 286, `final`, seulement si `config.getCommandAccess() != null`).
- Boucle (`Engine.update`, ~l. 460) : `console.update()` (avant les contextes : capture des entrées, commandes tapées) → `update` des contextes → `soundManager.lateUpdate()` → `contexts.postUpdate()`. `displayConsoleOverlay()` après les vues, sur la fenêtre principale.
- Arrêt (`Engine.shutdown`, l. 511) : `lifecycle().shutdown()` → `contexts.unloadViews()` → `soundManager.unloadSoundSource()` → `console.unload()` (fenêtre principale liée) → `resources.unload()`, `glResources.unload()` → `MainThread.processAll()` → `contexts.unload()` → `Workers.stop()` → `glfwTerminate` → `soundManager.unload()` (périphérique OpenAL). Il libère « ce que `setup` a atteint ».
- `main/ContextManager.java` (`final`, l. 94-96) : crée `KeyboardManager`, `MouseManager`, `InputManager` de chaque fenêtre (`setInputRegistry` reçoit le registre des entrées depuis `Engine.setup`).
- `scenes/SceneManager` (181 l.) : utilisé par `Context` (update/postUpdate/display/unload, `isExitRequested`), `MainLoop` (l. 56, 81, 160), lit `engine.soundManager()` (l. 36) ; `SceneManagerInterface` (côté scène) reste tel quel.
- `inputs/InputManager` (390 l.) : `WindowContext.getInputManager()` (12 usages bibliothèque, 61 démo) ; tests (`InputFocusTest`, `ActionValueTest`, `AxisTest`, `InputRegistryTest`, `GUIListTest`, `TextFieldModelTest`) le construisent directement.
- Son (`sounds/`) : `SoundManager` (264 l.), `SoundSource` (293 l.), `SoundListener` (70 l.), concrets OpenAL ; usages : `SceneManager`, `ChangeSoundGainCom`, `SoundsGenCom`, démo (`FallbackScene`, `GUIWidgetsScene`, `MenuScene`, `SoundFormatsScene`).
- Console : `ConsoleGenCom` et `ConsoleFeedScene` lisent `engine.console()` (null sans accès).
- Registre (`registry/`) : `EngineRegistry` (sections `resources`, `commands`, `inputs`, `scenes`, `lifecycle`), `Entries<K, V>` (collisions nommant les deux plugins, `replace`, espace de noms), `Registrations` (résultat gelé), `EnginePlugin` (package-private dans `main/`, espace `mightylib`, premier ; reçoit `config`). Tests : `EngineRegistryTest`, `LifecycleHooksTest`.
- `ListError` : `NO_ERROR` 0, `LIBRARIES_LOAD_FAIL` 2, `WINDOW_CREATION_FAIL` 3, `PLUGIN_REGISTRATION_FAIL` 4, `LIFECYCLE_HOOK_FAIL` 5.
- Propriétés système du moteur : `mightylib.<domaine>.<nom>` (`EngineConfig.*_PROPERTY`) ; `scenario.py -D clé=valeur` les transmet.
- Autres moteurs (modèle commun : choix une fois au démarrage ; retirer, remplacer, ajouter ; moteur non privilégié) :
  - Bevy : `DefaultPlugins.build().disable::<T>()` / `.set(...)` ([plugin group](https://bevy.org/examples/application/plugin-group/), [cheatbook](https://bevy-cheatbook.github.io/programming/plugins.html)).
  - jMonkeyEngine : `SimpleApplication(AppState... initialStates)` ([javadoc](https://javadoc.jmonkeyengine.org/v3.9.0-beta1/com/jme3/app/SimpleApplication.html)).
  - Godot : servers choisis au démarrage par priorité, jamais pendant le jeu ([PhysicsServer2DManager](https://docs.godotengine.org/en/stable/classes/class_physicsserver2dmanager.html)).
  - MonoGame/XNA : `Game.Services` par type, doublon = erreur ([GameServiceContainer](https://docs.monogame.net/api/Microsoft.Xna.Framework.GameServiceContainer.html)).
  - « Service » plutôt que « system » : réservé au sens ECS (entités, `later.md`).

## Décisions
1. **Package `services/`** (nouveau) : `IEngineService`, `ServiceKey<T>`, `IServiceFactory<T>`, `EngineServices` (clés du moteur : `SOUND`, `CONSOLE`), le conteneur des services créés et le tri.
2. **`ServiceKey<T extends IEngineService>`** : `Identifier` + `Class<T>` du contrat ; égalité par identifiant ; `engine.service(key)` rend `T` sans cast ; clé inconnue → `IllegalArgumentException` (une clé facultative se teste par `engine.hasService(key)`).
3. **`IEngineService`** : phases en méthodes `default` vides, appelées par l'`Engine` sur le thread principal :
   - `update()` : avant l'`update` des contextes (la console y capture les entrées et lance les commandes tapées) ;
   - `lateUpdate()` : après l'`update` des contextes, avant `contexts.postUpdate()` (le son) ;
   - `displayOverlay()` : après les vues, sur la fenêtre principale liée ; ordre par **couche** (`overlayLayer()`, `int`, défaut 0 ; console 1000, toujours au-dessus), ex aequo dans l'ordre de création ;
   - `unload()` : à l'arrêt, après `contexts.unloadViews()`, fenêtre principale liée, avant `resources.unload()` (sources de son, GL de la console) ;
   - `close()` : à la toute fin, après `glfwTerminate` (périphérique OpenAL).
   `update`/`lateUpdate` dans l'ordre de création ; `unload`/`close` dans l'ordre inverse.
4. **Fabrique** `IServiceFactory<T>` : `T create(Engine engine)` ; appelée pendant `Engine.setup`, **après** `Resources`, `GLResources`, `Commands` (et le `ConsoleLog`), **avant** les gestionnaires de scènes et la première scène ; un service lit ceux dont il dépend par `engine.service(...)` (déjà créés). Une fabrique de repli a la même forme.
5. **Section `registry.services()`** (`ServicesSection`, sur `Entries`) :
   - `register(key, factory)` → renvoie une déclaration où chaîner `.requires(key...)`, `.after(key...)`, `.fallback(factory)` ; `replace(key, factory)` idem (le remplaçant déclare **ses** dépendances et son repli ; il ne reprend pas ceux du remplacé) ;
   - la clé doit être de l'espace du plugin (sinon `IllegalArgumentException`), sauf `replace` ; doublon → `IllegalStateException` nommant les deux plugins ; `replace` d'une clé absente → erreur ; ligne `--… replaced by …` ;
   - `sceneManager(ISceneManagerFactory)` et `inputManager(IInputManagerFactory)` : **remplacement seulement** (le moteur fournit les fabriques par défaut), même journalisation, le dernier plugin gagne.
6. **Tri** (pur, à l'enregistrement, donc avant toute fenêtre, code `PLUGIN_REGISTRATION_FAIL`) : graphe `requires` + `after` présents ; dépendance `requires` absente → erreur nommant le service, son plugin et la clé manquante ; cycle → erreur listant le cycle ; ordre stable (Kahn, ex aequo par ordre d'enregistrement, moteur d'abord).
7. **Création** (`Engine.setup`) : dans l'ordre trié ; exception de la fabrique → `Logger.LogException` puis repli s'il existe (une ligne `--Service mightylib:sound: fallback to SilentSoundService`) ; sinon nouveau code `ListError.SERVICE_FAIL` (6), les services qui en dépendent (`requires`) ne sont pas créés, et `shutdown` libère ceux déjà créés (ordre inverse). Une fabrique ne rend jamais `null` (→ traité comme un échec).
8. **Son** :
   - `ISoundService` (contrat : `createSoundSource(SoundSourceCreationInfo) → ISoundSource`, `getListener() → ISoundListener`, liste des sources, arbre de gains : `getGainNodes`, `getNodeGain`, `changeGain`, texte de l'arbre ; `clearAwaitedSong` pour le changement de scène ; `lateUpdate`/`unload`/`close` des phases) ;
   - `OpenALSoundService` (ex-`SoundManager`), `OpenALSoundSource` (ex-`SoundSource`), `OpenALSoundListener` (ex-`SoundListener`) ; `SilentSoundService`, `SilentSoundSource`, `SilentSoundListener` (sans OpenAL : les sources existent, gardent position/gain/état « joué » mais ne sortent rien ; arbre de gains réel, lu depuis le même fichier) ;
   - clé `mightylib:sound`, enregistrée par `EnginePlugin` avec repli muet ; un `init` OpenAL qui échoue n'arrête plus le moteur ;
   - `-Dmightylib.sound=silent` (`EngineConfig.SOUND_PROPERTY`) : force le service muet, quel que soit le plugin qui fournit le son (une ligne de log) ; autre valeur → avertissement, ignorée.
9. **Console** : `IConsoleService` (ce qu'utilisent les commandes et la démo : `print`, `open/close/toggle`, `detach/attach`, `getMode`, `setLogsShown/areLogsShown`, `copyLast`, `stateJson`, `getLog`) ; `Console` l'implémente (reste `final`) ; clé `mightylib:console` enregistrée par `EnginePlugin` **seulement** si `config.getCommandAccess() != null` ; `engine.console()` rend `null` sinon ; un plugin qui veut la console déclare `after(CONSOLE)` et teste `hasService`.
10. **Gestionnaire de scènes** : `ISceneManager` (méthodes publiques actuelles de `SceneManager`) ; `ISceneManagerFactory.create(Engine, Context, boolean main, Supplier<TemplateSceneLoading> loadingScene)` ; utilisée par `setup` et `openContext` ; `Context.getSceneManager()`, `engine.sceneManager()`, `MainLoop` passent à l'interface.
11. **Gestionnaire d'entrées** : `IInputManager` (méthodes publiques actuelles d'`InputManager`) ; `IInputManagerFactory.create(KeyboardManager, MouseManager, GamepadManager, InputRegistry)` ; `ContextManager` la reçoit comme le registre des entrées ; `WindowContext.getInputManager()` rend `IInputManager`. Clavier, souris et manettes restent fixes (GLFW).
12. **Accesseurs** : `engine.sound()` remplace `soundManager()` (pas d'alias ni de dépréciation : aucun projet ne dépend encore du moteur) ; `engine.console()`, `engine.sceneManager()`, `engine.service(key)`, `engine.hasService(key)`, `engine.services()` (liste ordonnée pour la commande `services`).
13. **Héritage** : les implémentations gardent leur visibilité actuelle (non `final`, sauf `Console`), sans nouveau `protected` ; le choix des points d'accroche relève de l'objectif « Héritage et API publique ».

## Étapes (un commit chacune)
1. **Modèle des services, sans branchement** (fusionnable seule) : `services/` (`IEngineService`, `ServiceKey`, `IServiceFactory`), `registry/ServicesSection` (+ déclarations `requires/after/fallback`), tri (classe pure, ex. `services/ServiceOrder`), `Registrations.services()` (liste triée gelée), `ListError.SERVICE_FAIL`.
   Tests JUnit (`registry/ServicesSectionTest`, `services/ServiceOrderTest`) : enregistrement, espace de noms, doublon nommant les deux plugins, `replace` (dépendances du remplaçant), `replace` absent, gel ; `requires` absent (message), `after` absent ignoré, cycle (message), ordre stable, moteur d'abord.
2. **L'`Engine` crée et pilote les services** : conteneur (ex. `services/ServiceSet` : création dans l'ordre, repli, dépendants non créés, phases, `unload`/`close` inverses, liste des créés) ; `Engine.setup`/`update`/`displayConsoleOverlay` (renommé `displayOverlays`)/`shutdown` ; `service`, `hasService`, `services`. Aucun service enregistré encore : comportement identique.
   Tests JUnit (`ServiceSetTest`, services factices, `Engine` null) : ordre de création et des phases, couches d'overlay, repli, échec sans repli → dépendants absents, statut, ordre inverse à l'arrêt, fabrique qui rend `null`.
3. **Son en service** : interfaces et renommages (décision 8), `SilentSound*`, enregistrement par `EnginePlugin` avec repli, propriété `mightylib.sound`, retrait de `soundManager` de l'`Engine` (`unloadSoundSource`/`unload` → phases `unload`/`close`), `SceneManager`, `SoundsGenCom`, `ChangeSoundGainCom`, démo. Noter dans `later.md` le bug connu de `clearAwaitedSong`/`lateUpdate` sous le nouveau nom (ne pas le corriger ici).
   Tests JUnit : `SilentSoundService` (création de sources, gains, `changeGain` d'un nœud absent), lecture de la propriété.
   Vérifier : `scenario.py "scene MenuScene" wait-loaded "sounds"` (son OpenAL), puis la même avec `-D mightylib.sound=silent` (service muet, aucun appel AL, log) ; `leakcheck SoundFormatsScene`.
4. **Console en service** : `IConsoleService`, enregistrement conditionnel, phases `update`/`displayOverlay` (couche 1000)/`unload`, `ConsoleGenCom`, `ConsoleFeedScene`.
   Vérifier : `scenario.py -D mightylib.commands.access=user "console open" "console" "?gl summary"` ; sans accès : pas de service `mightylib:console`.
5. **Gestionnaire de scènes injectable** : `ISceneManager`, `ISceneManagerFactory`, section `sceneManager(...)`, `setup`, `openContext`, `Context`, `MainLoop`, `DemoPlugin`.
   Tests : `EngineRegistryTest` (remplacement de la fabrique, journalisation).
   Vérifier : `scenario.py "scene Scene2D" wait-loaded "scene MenuScene" wait-loaded` et une scène à plusieurs contextes (`openContext`) de la démo.
6. **Gestionnaire d'entrées injectable** : `IInputManager`, `IInputManagerFactory`, section `inputManager(...)`, `ContextManager`, `WindowContext`, usages bibliothèque ; la démo compile sans changement de nom de méthode.
   Tests : les tests d'entrées existants passent tels quels ; test du remplacement de la fabrique.
   Vérifier : `scenario.py "scene Scene2D" wait-loaded "input ENTER"` et une scène avec champ de texte (`GUIWidgetsScene`).
7. **Commande `services` et démo** : commande `mightylib:services` (niveau `DEV`, syntaxe déclarée, message lisible + `data` JSON : ordre, clé, contrat, implémentation, plugin, remplacé par, `requires`/`after`, repli utilisé, couche d'overlay ; fabriques de scènes et d'entrées) ; scène `EngineServices2D` (décrite dans `DemoScenes`) ; `DemoPlugin` ajoute `mightylibdemo:tick_counter` (`requires(SOUND)`, `after(CONSOLE)`, compte les ticks en `update`, petit overlay de couche 0) ; la scène affiche la liste des services, l'état du son (OpenAL ou muet) et le compteur.
   Tests : `DemoScenes` (déjà couvert), syntaxe/JSON de la commande si la forme des tests de commandes s'y prête.
   Vérifier : `scenario.py "scene EngineServices2D" wait-loaded "services"` ; puis avec `-D mightylib.sound=silent`.
8. **Docs** (liste ci-dessous) ; ne pas supprimer ce plan.

## Démo
- Nouvelle scène `EngineServices2D` (étape 7), inscrite dans `DemoScenes` ; service `mightylibdemo:tick_counter` ajouté par `DemoPlugin`.

## Documents à mettre à jour à la fin
- `architecture.md` : section « Services » (clés, phases, ordre, repli, couches), Démarrage, Boucle, Arrêt, « Moteur et vue courante » (accesseurs), Plugins et registre (section `services()`), Entrées et Console (interfaces), son (muet).
- `debug-tools.md` : commande `services`, propriété `mightylib.sound=silent`.
- `decisions.md` : services par le registre, clé typée, dépendances déclarées, repli par clé plutôt qu'arrêt, « service » plutôt que « system ».
- `later.md` : `ShaderManager` à refondre avec le chantier des shaders (section Shaders) ; services par contexte/fenêtre ajoutés par les plugins ; bug connu du son sous son nouveau nom ; découverte des plugins de mods (ordre des dépendances de services).
- `CLAUDE.md` : tests couverts (`services/`, `ServicesSection`, `SilentSoundService`).
- `current.md` : supprimer la ligne « Systèmes de l'`Engine` injectables ».
- Supprimer ce plan (discussion B).

## Hors périmètre
- `ShaderManager` comme service : `later.md`, Shaders.
- Services ajoutés par contexte ou par fenêtre : `later.md`.
- Points d'accroche `protected` des implémentations, `final` : objectif « Héritage et API publique » (`current.md`).
- Remplacement de `ContextManager`, `Resources`, `GLResources`, `Commands`, clavier/souris/manettes : fixes.
- Correction du bug connu de `clearAwaitedSong` : `later.md`.

## Questions restantes
- Aucune.

## Modèles recommandés
- Étapes 1-2 (conception de l'API des services, tri, cycle de vie) : Claude Opus 5.5, effort `high`.
- Étapes 3-6 (migration derrière des interfaces décidées, ordre d'arrêt du son à respecter) : Claude Sonnet 5.5, effort `high`.
- Étapes 7-8 (commande, démo, docs) : Claude Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`, gros diff à relire au regard du plan) : Claude Opus 5.5, effort `high`.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/systemes-injectables.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `services-injectables`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (son audible dans `MenuScene` et `SoundFormatsScene` ; lancement avec `-Dmightylib.sound=silent` ; console ouverte par-dessus `EngineServices2D`, au-dessus de l'overlay du compteur ; une scène à second contexte ; saisie dans un champ de texte de `GUIWidgetsScene`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml services-injectables`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
