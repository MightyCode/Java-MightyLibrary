# Point d'injection unique (`IPlugin` / `EngineRegistry`)

**Statut** : validé.

## Décisions prises
- Interface `IPlugin` (`namespace()`, `register(EngineRegistry)`), ajoutée par `EngineConfig.addPlugin` (ordre d'ajout = ordre d'enregistrement) ; `IProjectLoading` et `EngineConfig.setProjectLoading` supprimés.
- Le moteur enregistre son propre contenu par le même registre, comme un premier plugin interne (espace `mightylib`).
- Une seule phase d'enregistrement, au tout début de `Engine.setup` (avant GLFW et la fenêtre), puis registre gelé ; crochets de cycle de vie enregistrés pendant cette phase.
- Périmètre : loaders/`DataType`, commandes du moteur et des plugins, actions et axes avec touches par défaut, registre de scènes par nom, crochets de cycle de vie. Hors périmètre : shaders, sources de ressources, compteurs `stats` (notés dans `later.md`).
- Collisions : identifiant déjà pris → exception au démarrage nommant les deux plugins ; `replace()` explicite par un plugin plus tardif (moteur compris), signalé en une ligne.
- Espace de noms imposé : un plugin n'enregistre que des identifiants de son espace (sauf `replace()`) ; il est le propriétaire de ses commandes.
- Loaders : le moteur n'enregistre que les obligatoires ; le jeu ajoute ceux qu'il utilise ; raccourci `LibraryLoaders.all()`.
- Actions/axes : clés statiques (`InputAction.of(id)`), déclarées par `register()` avec leurs touches par défaut ; registre propre à l'`Engine`.
- Scènes : enregistrement facultatif (le code peut toujours créer une scène) ; la commande `scene` et les menus lisent le registre ; la réflexion quitte la bibliothèque ; la démo enregistre explicitement ses scènes, comme un vrai jeu.
- Crochets : un lambda par événement (démarrage, changement de scène, arrêt, ouverture/fermeture de fenêtre).

## Pour démarrer
- Lire : `docs/current.md` (ligne « Point d'injection unique »), `docs/architecture.md` (sections Démarrage, Ressources CPU (début), Entrées, Commandes, Contextes et fenêtres), `docs/decisions.md` (philosophie, commandes à propriétaire, champs publics), `docs/later.md` (Bibliothèque réutilisable, Modding).
- Branche de sujet depuis `dev` : `point-injection`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `main/procedures/IProjectLoading` : `loaders()` (lu à la construction de `Resources`, liste figée), `registerCommands(CommandRegistry)` (appelé après `SceneManager`, propriétaire `CommandOwner.GAME`), `contextLoading(WindowContext)` (liaisons d'entrées ; rappelé par `Engine.initWindowInputs` pour chaque contexte ouvert dont `WindowConfig.hasProjectInputs()`, faux pour la fenêtre détachée de la console). Démo : `mightylibdemo/main/ProjectLoading`.
- `main/Engine.setup` (≈ l. 180-300) : contexte principal → `ShaderManager` → `Resources(…, projectLoading.loaders())` → `GLResources` → `SoundManager` → `Commands` (commandes générales par `ListGeneralCommand.addCommands(registryFor(ENGINE))`) → `SceneManager` (+ `Console`) → `registerCommands` + `contextLoading(mainWindow)` → `resources.init()`/`load()` → `sceneManager.init(startScene)`. `Engine.shutdown(int)` (≈ l. 490).
- `resources/Resources` : loaders obligatoires ajoutés à la construction (≈ l. 100 : icônes, textures, animations, polices, JSON, batchs, shaders) puis ceux du projet ; un loader par type de donnée (doublon → `IllegalArgumentException`). Loaders facultatifs de la bibliothèque, enregistrés aujourd'hui par la démo : `TileSetLoader`, `TileMapLoader`, `SoundLoader`, `ModelLoader`, `TextureDataAtlasLoader`, `WaveCollapseRulesLoader`. Hors de leur paquet, `SoundSource` et `ModelRenderer` lisent `SoundData`/modèles : sans leur loader, une erreur claire est attendue.
- `inputs/InputRegistry` : **statique**, `action(id)`/`axis(id)` créent la poignée avec un index (`nextIndex`, utilisé par la simulation d'`InputManager` : `axisSimulation`, `simulation`), à l'initialisation des classes de constantes (`EngineActions`, démo `ActionId`/`AxisId`), sans gel ; `matchActions`/`matchAxes` (identifiant complet ou chemin seul, sans casse) lus par `InputManager` (l. 190, 302). Touches par défaut du moteur en dur dans `InputManager.initLibraryInputs` (`TOGGLE_CONSOLE` = accent grave, `DETACH_CONSOLE` = F2, `RELOAD_TEXTURE` = F5). `AxisButtons(IAction, IAction)` reçoit des `ActionInput` (démo : `find(configurations, …)`).
- `utils/enginecommand/CommandOwner` : `ENGINE`, `GAME`, `Of(view, kind)` ; `Commands.registryFor(owner)`. Commandes des scènes et des vues : dynamiques, inchangées.
- Scènes : `SceneManager.scenesPackage` = paquet de la première scène ; `general/SceneGenCom` (`scene <nom>`) et `general/SceneClasses.Find` cherchent les classes par réflexion. Démo : `MenuScene`, `SceneChoices` (lit la complétion de `scene`), `SceneHistory` (dates git par classe), `resources/…/menuScenes.json` (`hidden`, `descriptions`, `defaultDescription`). Commande `demo:menu` dans `ProjectLoading`.
- `registry/Identifier` : espace `[a-z0-9_.-]+`, chemin `[a-z0-9_./-]+`, `ENGINE_NAMESPACE = "mightylib"`.
- Contextes et fenêtres : `ContextManager.createContext`/`closeContext` (l. 204), `Context.openWindow`/`closeWindow`/`destroySubWindow`, `SceneManager.changeScene` (l. 86).
- Tests existants : `InputRegistryTest`, `ActionValueTest`, `AxisTest`, `InputFocusTest`, `GUIListTest` (utilisent `InputRegistry`), `CommandsTest`/`TestCommands` (`CommandOwner.GAME`), `SceneChoicesTest`/`SceneHistory` (démo), `EngineConfigTest`, `PublicFieldsTest`, `Resources*Test`.
- Ailleurs : Bevy (`Plugin::build`, puis `finish`/`cleanup` ; `DefaultPlugins` remplaçables par `set`/`disable`), Forge/Fabric (registres gelés après la phase d'enregistrement, constantes = identifiants, DeferredRegister), Unity (`RuntimeInitializeLoadType` : phases fixes, ordre non garanti dans une phase).

## Décisions
1. `IPlugin` : `String namespace()` (validé comme espace d'`Identifier`), `void register(EngineRegistry registry)`. `EngineConfig.addPlugin(IPlugin)` ; deux plugins du même espace → erreur. Espace `mightylib` réservé au moteur.
2. `EngineRegistry` = collecteur de données pur (aucun GL, aucun `Engine`), testable en JUnit : sections `resources()` (loaders), `commands()`, `inputs()`, `scenes()`, `lifecycle()`. Chaque section sait quel plugin enregistre (l'`EngineRegistry` passé à un plugin porte son espace). Gel après le dernier plugin : tout appel → `IllegalStateException`. L'`Engine` lit ensuite le registre gelé pour créer ses systèmes.
3. Ordre : plugin interne du moteur (`EnginePlugin`, package-private) puis plugins dans l'ordre d'`addPlugin`. `register()` est appelé au début d'`Engine.setup`, avant GLFW : une erreur de plugin arrête avant d'ouvrir une fenêtre.
4. Collisions : même identifiant (commande, action/axe, scène) ou même type de donnée (loader) → `IllegalStateException` « <id> registered by <plugin A> and <plugin B> ». `replace(...)` remplace une entrée d'un plugin précédent (y compris d'un autre espace et du moteur), avec une ligne `--`; `replace` d'une entrée absente → erreur.
5. Espace imposé : `register` d'un identifiant hors de l'espace du plugin → erreur. Les loaders (clé = classe de donnée) n'ont pas d'espace.
6. Commandes : propriétaire = le plugin (`CommandOwner.plugin(namespace)` ou équivalent, `kind` = "plugin", `help` montre l'espace) ; `CommandOwner.GAME` supprimé ; `ENGINE` reste pour le plugin du moteur. Commandes du moteur et des plugins figées après le démarrage ; celles des scènes et des vues restent dynamiques (`registryFor(view)`).
7. Loaders : le plugin du moteur enregistre les obligatoires (ceux de `Resources` aujourd'hui) ; `LibraryLoaders.all()` rend une liste neuve des loaders facultatifs de la bibliothèque, `registry.resources().addAll(...)`. Un type sans loader utilisé par `SoundSource`, `ModelRenderer`, etc. → message clair nommant le loader à enregistrer.
8. Entrées : `InputAction.of(String|Identifier)` / `InputAxis.of(...)` = clés (identifiant seul, `equals` par identifiant) ; `registry.inputs().action(clé, IAction défaut)` / `.axis(clé, AxisBinding défaut)` (défaut facultatif). L'index de simulation passe de la clé au registre de l'`Engine` (attribué à l'enregistrement). `InputRegistry` devient une instance de l'`Engine` (`engine.inputs()` ou équivalent), lue par chaque `InputManager`. Une clé non enregistrée utilisée (`getState`, simulation, commande `input`) → erreur claire. `AxisButtons` peut référencer des actions par clé (résolues dans l'`InputManager` de la fenêtre), pour ne plus passer par les `ActionInput` de la démo. Liaisons : celles du plugin du moteur dans toutes les fenêtres, celles des autres plugins dans les fenêtres `hasProjectInputs()` ; `initLibraryInputs` et `IProjectLoading.contextLoading` disparaissent.
9. Scènes : `registry.scenes().register(Identifier, Supplier<? extends Scene>, String description)` ; `SceneRegistry` lu par `scene` (nom = identifiant complet ou chemin seul, sans casse ; ambigu → erreur listant les candidats ; complétion = chemins, ou identifiants complets en cas d'ambiguïté). `SceneClasses`, `SceneManager.scenesPackage`/`getScenesPackage` et la réflexion retirés de la bibliothèque. Une scène non enregistrée reste utilisable par le code ; `scene` (sans argument) montre l'identifiant si la scène courante est enregistrée, sinon sa classe. `EngineConfig.setStartScene` et `setLoadingScene` inchangés (un seul propriétaire, le jeu ; remplacement par un mod : `later.md`, Modding).
10. Identifiants des scènes de la démo = nom de classe relatif au paquet `scenes`, en minuscules (`demo:scene2d`, `demo:platformer2d.platformerbox2d`) : tous les `scene <Nom>` des scripts et des docs restent valides.
11. Crochets : `registry.lifecycle().onStarted(Runnable)` (après `resources.load()`, juste avant `sceneManager.init` de la première scène), `onSceneChanged(ISceneChanged : (Context, Scene ancienne ou null, Scene nouvelle))` (quand la nouvelle scène est lancée, après son chargement, dans chaque contexte), `onShutdown(Runnable)` (début d'`Engine.shutdown`, avant toute libération), `onWindowOpened`/`onWindowClosed(WindowContext)` (fenêtre principale de chaque contexte et sous-fenêtres ; fermeture avant destruction). Appel sur le thread principal, dans l'ordre des plugins ; une exception d'un crochet → `Logger.LogException`, les suivants sont appelés (en mode strict : relancée).

## Étapes (un commit chacune)
0. Commit du plan tel quel.
1. **`IPlugin`, `EngineRegistry`, loaders et commandes.** Nouveau paquet `main/plugin` (ou `registry`) : `IPlugin`, `EngineRegistry` et ses sections ressources/commandes, `EnginePlugin` (loaders obligatoires, commandes générales de `ListGeneralCommand`), `LibraryLoaders`. `EngineConfig.addPlugin` ; `Resources` reçoit la liste complète des loaders (plus d'ajout des obligatoires dans son constructeur, ou constructeur de test conservé : à voir avec `ResourcesTestBase`). `Commands` reçoit les commandes du registre gelé ; propriétaire par plugin, `CommandOwner.GAME` retiré. `IProjectLoading` ne garde que `contextLoading` (retiré à l'étape 2). Démo : `DemoPlugin` (espace `demo`, loaders un par un, commande `demo:menu`). Tests : `EngineRegistryTest` (ordre, collision nommant les plugins, `replace`, `replace` absent, espace refusé, gel, deux plugins même espace), `CommandsTest`/`TestCommands` adaptés. Vérif : `scenario.py "scene Scene2D" wait-loaded "?help menu" "?resources"`. Peut être fusionnée seule.
2. **Entrées par le registre.** Clés `InputAction.of`/`InputAxis.of`, section `inputs()`, `InputRegistry` d'instance (index dans le registre), `InputManager` lit le registre de son `Engine` et ses liaisons par défaut ; `EngineActions` = clés déclarées par `EnginePlugin` avec F2/F5/accent grave ; `AxisButtons` par clés ; démo : `ActionId`/`AxisId` en clés, liaisons dans `DemoPlugin` ; `IProjectLoading` et `Engine.initWindowInputs`/`contextLoading` supprimés. Tests : `InputRegistryTest` réécrit (instance, collision, clé non enregistrée, `match*`), `ActionValueTest`/`AxisTest`/`InputFocusTest`/`GUIListTest` adaptés. Vérif : `scenario.py "scene Scene2D" wait-loaded "input --hold demo:move_right 10" "?camera"` (ou équivalent selon la syntaxe d'`input`), `?input list`, ouverture d'un second contexte (scène multi-fenêtre de la démo) et console détachée (F2) : les touches marchent dans chaque fenêtre.
3. **Registre de scènes.** Section `scenes()`, `SceneRegistry` de l'`Engine`, `SceneGenCom` sur le registre, retrait de `SceneClasses`/`scenesPackage`. Démo : liste `DemoScenes` (classe, fabrique, description ; descriptions reprises de `menuScenes.json`, supprimé), enregistrée par `DemoPlugin` ; `MenuScene`/`SceneChoices` lisent le registre (dates de `SceneHistory` par la classe de `DemoScenes`). Tests : `SceneRegistryTest` (correspondance, ambiguïté, complétion), `SceneChoicesTest` adapté, test démo : toute sous-classe concrète de `Scene` du paquet `mightylibdemo.scenes` (hors scènes de chargement) est dans `DemoScenes` (réflexion permise dans un test). Vérif : `scenario.py "scene Platformer2D.PlatformerBox2D" wait-loaded "?scene" "?complete \"scene Sce\""`, menu (`scene MenuScene`, liste et descriptions).
4. **Crochets de cycle de vie.** Section `lifecycle()`, appels aux cinq endroits (setup, `SceneManager` au lancement, `Engine.shutdown`, `ContextManager`/`Context` à l'ouverture et à la fermeture). Démo : `DemoPlugin` compte les événements et les rend par une commande `demo:lifecycle` (`DEV`, `data` JSON). Tests : répartiteur pur (ordre, exception isolée, strict relance). Vérif : `scenario.py "scene Scene2D" wait-loaded "?demo:lifecycle"`, puis avec une scène multi-fenêtre.
5. **Docs** (liste ci-dessous) et section « Bilan ».

## Démo
Pas de nouvelle scène : le sujet est le démarrage. `DemoPlugin` (remplace `ProjectLoading`) est l'exemple : loaders choisis un par un, commande `demo:menu`, actions et touches, liste de scènes, crochets (commande `demo:lifecycle`). `Main` : `config.addPlugin(new DemoPlugin())`.

## Documents à mettre à jour à la fin
- `architecture.md` : Démarrage (phase `register`, gel, ordre), Ressources CPU (loaders par le registre, `LibraryLoaders`), Entrées (clés, registre d'instance, liaisons par défaut), Commandes (propriétaire par plugin), nouvelle section courte « Plugins et registre » (sections, collisions, `replace`, espaces, crochets).
- `debug-tools.md` : commande `scene` (identifiants, chemins), `demo:lifecycle`.
- `decisions.md` : « Point d'injection unique : `IPlugin`, une phase, registre gelé, espace imposé, collision = erreur sauf `replace` ; le moteur passe par le même registre » ; « Scènes : registre facultatif, pas de réflexion dans la bibliothèque ».
- `later.md` : Bibliothèque réutilisable → shaders injectés par les plugins (`registry.shaders()`) et sources de ressources par plugin (racines moteur/jeu/mods), une fois les sources multiples faites ; compteurs `stats` injectables (nom, unité, remise à zéro) ; Modding → découverte des plugins de mods par `ServiceLoader` (`addPlugin` en est la base), remplacement de la scène de chargement par un mod, phase `finish` si des mods dépendent d'autres mods ; Entrées → remappage sur les clés du registre ; supprimer la phrase « registre de scènes par nom… remplacera la recherche par réflexion… et `menuScenes.json` ».
- `futur-discussion.md` : rien, sauf question ouverte apparue.
- `CLAUDE.md` : tests couverts (`EngineRegistry`, `SceneRegistry`, crochets, test démo des scènes enregistrées) ; règle des scènes de démo : « l'ajouter à `DemoScenes` (un test le vérifie) » au lieu de « apparaît seule dans le menu ; `menuScenes.json` ne sert qu'à masquer ».
- `current.md` : supprimer la ligne « Point d'injection unique » et ses sous-points.
- Supprimer ce plan (discussion B).

## Hors périmètre
- Shaders injectés, sources de ressources multiples : point « Bibliothèque réutilisable » (`later.md`).
- Compteurs `stats` injectables : `later.md`.
- Mods (`ServiceLoader`, descripteur, dépendances), bus d'événements, phase `finish` : `later.md`, Modding.
- Systèmes de l'`Engine` injectables, repli GL, héritage et API publique, démo en module Maven : lignes suivantes de `current.md`.

## Questions restantes
- Aucune (à confirmer à la lecture : nom du paquet, `main/plugin` ou `registry`).

## Modèles recommandés
- Discussion A, étapes 0 à 3 : Opus 5.5, effort `high` (conception d'une API durable : registre, clés d'entrées, scènes ; nombreux fichiers et tests touchés).
- Discussion A, étapes 4 et 5 : Opus 5.5, effort `medium` (crochets simples, docs).
- Discussion B (`/pr-ml`) : Opus 5.5, effort `high` (gros diff à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/point-injection.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `point-injection`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (menu de la démo : liste et descriptions ; touches dans Scene2D, une scène 3D et un second contexte ; console détachée par F2 ; fermeture du jeu sans erreur). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml point-injection`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
### Fait
- Étapes 1 à 5 sur la branche `point-injection`, un commit chacune (plan, loaders et commandes, entrées, scènes, crochets, docs). Paquet choisi : `registry` (`IPlugin`, `EngineRegistry` et ses sections `resources()`, `commands()`, `inputs()`, `scenes()`, `lifecycle()`, `Registrations` = registre gelé, `LifecycleHooks`, `ISceneChanged`) ; `EnginePlugin` package-private dans `main`.
- Accès : `engine.registrations()`, `engine.inputs()` (`InputRegistry`), `engine.scenes()` (`SceneRegistry`), `engine.lifecycle()`.
- Démo : `DemoPlugin` (loaders un par un, `demo:menu`, actions et axes avec touches, `DemoScenes`, crochets et `demo:lifecycle`) ; `ProjectLoading`, `IProjectLoading`, `menuScenes.json` et le batch `menu` supprimés.

### Écarts au plan et raisons
- **Scènes enregistrées avec leur classe** : `register(Identifier, Class<T>, Supplier<? extends T>, description)` au lieu de `(Identifier, Supplier, description)`. Sans la classe, `scene` sans argument ne peut pas nommer la scène courante créée par le code (`demo:menu`, scène de départ) sans appeler la fabrique. Plusieurs identifiants pour une classe : le premier la nomme.
- **Loader des shaders hors du registre** : il dépend du `ShaderManager` (version GLSL connue après la fenêtre), donc `Resources` le crée toujours en tête ; `LibraryLoaders.mandatory()` = les autres obligatoires (cohérent avec « shaders hors périmètre », noté dans `later.md`). `BatchLoader` garde le `JSONLoader` créé avec lui : remplacer le loader JSON ne change pas celui des batchs.
- **Commandes générales sans `Commands` au constructeur** : `help`, `wait`, `exec`, `console`, `as`, `complete`, `cheats` lisent `BaseCommand.commands()` (leur enregistrement) pour être créées par `EnginePlugin` avant l'`Engine`. `Commands(max, session, cheat)` (avec les commandes générales) reste pour les tests et les outils ; l'`Engine` utilise `Commands(max, session)` puis `freezePluginCommands()` (après : seules les vues enregistrent).
- **Erreur d'enregistrement** : journalisée (`Logger.LogException`) et code `ListError.PLUGIN_REGISTRATION_FAIL` (4), arrêt propre avant GLFW.
- **Entrées** : `InputManager.bindDefaults(pluginInputs)` à la création de chaque fenêtre ; `init(...)` garde les défauts du moteur. `AxisButtons(InputAction, InputAction)` lit les actions dans l'`InputManager` de la fenêtre, simulation comprise : simuler `demo:move_right` déplace maintenant aussi l'axe `move_right_3d` (avant, l'axe lisait la liaison brute). `SceneRegistry.declare` n'est pas générique (le lien classe/fabrique est vérifié par `ScenesSection.register`).
- **Crochets** : « mode strict » = `EngineConfig.setStrictResources` (seul mode strict existant). `onSceneChanged` part une fois la nouvelle scène chargée (première frame complète) ; l'« ancienne » est la dernière scène chargée du contexte (une scène quittée pendant son chargement n'est jamais signalée).
- **Menu** : le menu se masque lui-même par sa classe (plus de liste `hidden`) ; `CustomRenderer2D`, qui n'avait que la description par défaut, en a une ; `help` montre « plugin demo » pour les commandes de la démo ; la complétion de `scene` donne les chemins en minuscules (`scene Sce` → `scene2d`, `scene3d`, `scene3dbis`).

### Vérifié
- `mvn -o -q test` : 943 tests, 0 échec (nouveaux : `EngineRegistryTest`, `LifecycleHooksTest`, `SceneRegistryTest`, `InputRegistryTest` réécrit, `DemoScenesTest`, cas ajoutés dans `CommandsTest` et `ResourceLoadersTest`).
- `scenario.py` : `help menu`, `resources` (tous les types chargés) ; `Scene3D` : `input press demo:move_right 20` déplace la caméra ; `input list` dans le second contexte de `MultiWindowScene` (toutes les liaisons) et dans la console détachée (les 3 actions `mightylib` seulement) ; F2 dans la fenêtre de la console la rattache ; `scene Platformer2D.PlatformerBox2D` puis `?scene` (identifiant), `complete "scene Sce"`, `scene MenuScene`, `demo:menu`, `scene nothing` (erreur claire) ; menu (capture : liste et description), lancement de `Box2D` depuis le menu ; `demo:lifecycle` (démarrage 1, changements de scène par contexte, 3 fenêtres ouvertes puis 1 fermée) et ligne du crochet d'arrêt dans le journal. Aucune ligne de problème dans les journaux.

### À vérifier à la main
- Menu de la démo : liste complète, descriptions de chaque scène, filtre et tris, lancement d'une scène à la souris.
- Touches : `Scene2D` (déplacements), une scène 3D (`Scene3D` : ZQSD/WASD, souris, Maj, espace/Ctrl), un second contexte (`MultiWindowScene`, touche C) où les touches du jeu marchent.
- Console : ² ouvre/ferme dans la fenêtre principale, F2 la détache, F2 dans sa fenêtre la rattache ; dans la fenêtre détachée, les touches du jeu n'agissent pas.
- Fermeture du jeu sans erreur (journal : `--Demo: shutdown after …`, `Exit without error`).
