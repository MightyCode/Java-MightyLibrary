# Plan — Commandes : propriétaires, niveaux d'accès, espaces de noms

Statut : **validé dans ses grandes lignes**, à implémenter. Les questions restantes sont à la fin : les trancher avec l'utilisateur au début du travail.
Ce plan se lit seul. À la fin du travail, il est supprimé : son contenu vit alors dans `architecture.md`, `decisions.md` et `later.md` (voir « Documents »).

## Pour démarrer
- Lire `CLAUDE.md` (build, tests, règles git, nettoyage des docs), puis dans `docs/architecture.md` les sections **Console**, **Entrées** et **Moteur et vue courante**, et dans `docs/debug-tools.md` la section **Commandes**.
- Branche de sujet depuis `dev` (par exemple `command-registry`), un commit par étape, fusion `--no-ff` dans `dev`.
- Compiler et tester avec les commandes de `CLAUDE.md`. Vérifier en jeu avec `tools/scenario.py` (une fenêtre s'ouvre chez l'utilisateur : le prévenir).

## Contexte : ce qui existe
- **`Commands`** (`src/.../utils/enginecommand/Commands.java`) :
  - créé par le `SceneManager` du contexte principal seulement (`SceneManager(…, withCommands, …)`) ; les autres contextes n'en ont pas ;
  - `submit(line, ICommandSource)` depuis tout thread, exécution au début du tick suivant (`processPendingCommands`, appelé par `SceneManager.updateMain` en mode debug ou si la console existe) ;
  - `getCurrentSource()`, `addListener(ICommandListener)` (avant/après chaque commande soumise), `complete(début de ligne)` → `CommandCompletion`, `addTickTask` (commandes sur plusieurs ticks).
- **Séparation figée** : `endGeneralCommand` (index fixé à la construction, après `ListGeneralCommand.addCommands`) sépare les commandes « générales » des commandes « de scène ». `SceneManager.changeScene` appelle `removeSceneCommands` à **chaque** changement de scène.
- **Défaut** : une commande ajoutée par le jeu au démarrage (`IProjectLoading.init`) est effacée au premier changement de scène. Il n'y a donc **aucun moyen** d'avoir des commandes de jeu.
- **Scènes** : pas d'API dédiée (il faut passer par `sceneManager.getCommands()`), aucune scène de la démo ne le fait. Toutes les commandes s'exécutent sous la vue de l'`Engine` (`Views`), même celles d'une scène.
- **`BaseCommand(String name)`** : `execute(args)`, `help()`, `completeArgument(args)` (complétion, implémentée par les 21 commandes de `general/`, dont `ConsoleGenCom`). Noms globaux en texte, donc collisions possibles.
- **Sources** (`ICommandSource`) :
  - `game` : la console en jeu (`console/Console.java`) ;
  - `stdin` : `Commands.startConsole`, mode debug ;
  - `server#N` : un client de `DebugServer`, mode debug ;
  - `api` : le défaut.
- **Journal** : `CommandEcho` (`console/`) écrit chaque commande soumise et son résultat dans `ConsoleLog`. Les lignes ont un type (`OUT`, `ERR`, `COMMAND`, `RESULT`, `ERROR`) et une source. `LogCapture` y copie toute la sortie du programme (`System.out`/`err`). `ConsolePanel` affiche les dernières lignes.
- **Accès** : tout ou rien. `EngineConfig.setConsole(bool)` (défaut : mode debug) crée la console. Dans un jeu publié, **toutes** les commandes y sont alors disponibles, y compris `exec` (qui lit des fichiers), `input` et `gamepad`. La console s'ouvre par `EngineActions.TOGGLE_CONSOLE` (F1) et se détache par `DETACH_CONSOLE` (F2).
- **Scripts** : `ExecGenCom` / `CommandScript` exécutent les lignes par `commands.execute`, en partie plus tard (tâches de tick), où `currentSource` vaut `null`.
- **Modèles à réutiliser** :
  - résolution des noms des actions (`InputRegistry.matchActions` : identifiant complet, ou chemin seul s'il est unique) et `registry/Identifier` ;
  - propriétaire des fenêtres (`Context.openWindow` retient `Views.currentOrNull()`, `closeWindowsOpenedBy` à la sortie de la scène, `WindowTree.openedBy`).
- **Tests existants** : `test/.../utils/enginecommand/CommandsTest` (`new Commands(null)` suffit sans GL), `CommandScriptTest`, `test/.../console/*`, `inputs/InputCaptureTest`.

## Décisions
- **Trois niveaux** `USER` < `CHEAT` < `DEV`, chacun incluant ceux d'en dessous :
  - `USER` : commandes normales du joueur (chat, options, aide) ;
  - `CHEAT` : triches du joueur, quand le jeu les active ;
  - `DEV` : outils de création, pour le développeur comme pour le moddeur.

  `MODDER` n'existe pas pour l'instant. L'enum est ordonnée, on pourra l'insérer entre `CHEAT` et `DEV` le jour où les mods auront des besoins propres.
- **Pas de niveau par défaut** : chaque commande déclare son niveau dans son constructeur. Un oubli est une erreur de compilation, pas une commande invisible.
- **Refus silencieux** : une commande au-dessus du niveau de la source n'existe pas pour elle (rien ne révèle son existence).
- **La triche est choisie par le développeur du jeu.** Le plafond par défaut en release est `USER`. Le jeu relève le plafond à `CHEAT` s'il veut des triches, puis choisit comment le joueur les active :
  - la commande moteur `cheats on|off`, optionnelle et désactivée par défaut ;
  - ou son propre moyen (bouton caché dans les paramètres, réglage du monde) par l'API `setSessionLevel`.

  Le moteur n'impose aucune règle de sauvegarde.
- **Une commande vérifie elle-même l'état dont elle dépend** (scène pas encore chargée, pas de caméra…) et répond par une erreur lisible ; le moteur ne bloque pas les commandes d'une scène en chargement.
- **Les niveaux ne sont pas une sécurité.** En local, le joueur possède sa JVM et ses fichiers ; les niveaux délimitent ce qui est visible et évitent les erreurs. En réseau, c'est le serveur qui décidera (hors périmètre).
- **La sortie du programme n'est visible qu'en `DEV`.** Un joueur ne voit que ses commandes, leurs réponses et les messages que le jeu lui adresse.
- **Les réponses varient selon le niveau** : lisibles pour `USER`/`CHEAT`, complètes pour `DEV`.

## Étapes

### Étape 1 — Un registre par `Engine`, des commandes à propriétaire
Corrige seule le défaut des commandes de jeu ; peut être fusionnée sans les étapes suivantes.
- `Commands` passe à l'`Engine` (`engine.commands()`), créé dans `Engine.setup`. `SceneManager` ne le crée plus et ne fait plus que traiter les commandes en attente du contexte principal (`updateMain`). La cible des commandes générales reste `Engine.commandWindow()` / `commandContext()`.
- Chaque commande a un **propriétaire** :
  - le **moteur** (commandes générales) ;
  - le **projet** (le jeu) ;
  - un **`IView`** : une scène de n'importe quel contexte, ou une `WindowView`.
- `removeOwnedBy(owner)` remplace `endGeneralCommand`, `isSceneCommand` et `removeSceneCommands`.
- **Scène** : `Scene.addCommand(BaseCommand)` enregistre avec la scène comme propriétaire. Le retrait se fait dans `Scene.unloadByManager`, à côté de `closeWindowsOpenedBy` : il est appelé même si la scène est quittée pendant son chargement. `WindowView.addCommand` de même, avec retrait dans `unloadByContext`.
- **Exécution sous le propriétaire** : une commande de scène ou de vue s'exécute sous sa vue (`Views.enter`) avec le contexte GL de sa fenêtre, pour pouvoir créer des objets comme le code de la scène.
- **Scène en chargement** : la commande est acceptée et doit tester elle-même l'état dont elle a besoin. La scène expose cet état (`Scene.isLaunched()` public, ou équivalent), et une aide comme `requireLaunched()` renvoie l'erreur « scene still loading » (cameras et renderers n'existent qu'à partir du `launch`). Les commandes générales qui dépendent de la scène (`camera`…) font de même.
- **Jeu** : `IProjectLoading.registerCommands(CommandRegistry)`, méthode par défaut vide, appelée une fois dans `Engine.setup`. C'est un point d'entrée **provisoire**, à absorber plus tard dans `register(EngineRegistry)` (voir `later.md`, « Modèle d'extension »). `CommandRegistry` est une interface étroite (`register`), pas `Commands` en entier.
- `help` affiche le propriétaire (`engine`, `game`, `scene`) à la place de `(scene)`.
- Tests :
  - retrait par propriétaire ;
  - une commande de jeu survit aux changements de scène (test de régression du défaut) ;
  - deux propriétaires ne se retirent pas mutuellement leurs commandes.

### Étape 2 — Identifiants à espace de noms
- `BaseCommand(Identifier, …)`. Les 21 commandes générales passent dans l'espace `mightylib`. Les noms courts restent valables, donc les scénarios, scripts et docs existants continuent de marcher.
- Résolution comme pour les actions : identifiant complet, ou chemin seul s'il est unique parmi les commandes **visibles** (voir l'étape 3). Un nom ambigu est refusé avec la liste des identifiants visibles. Un identifiant déjà pris est refusé (`IllegalArgumentException`).
- Complétion : les chemins seuls, plus les identifiants complets en cas d'ambiguïté. `help` affiche l'identifiant.
- Tests : résolution par chemin seul ou complet, ambiguïté, collision, complétion.

### Étape 3 — Niveaux et accès
- **Niveau des commandes** : `ECommandLevel { USER, CHEAT, DEV }`, paramètre obligatoire du constructeur de `BaseCommand`. Classement des commandes générales :

  | Niveau | Commandes |
  |---|---|
  | `USER` | `help`, `console` (`open`, `close`, `toggle`, `detach`, `attach`, `clear`), `screenshot`, `stats fps`, `cheats` (si le jeu l'active) |
  | `CHEAT` | aucune : les triches sont des commandes du jeu |
  | `DEV` | toutes les autres : `reload`, `watch`, `resources`, `scene`, `camera`, `stats` (sauf `fps`), `gl`, `monitors`, `windows`, `window`, `changegain`, `wait`, `input`, `gamepad`, `textureupload`, `exec`, `exit`, `console log`, `console follow` |

  Descendre en `USER` d'autres commandes utiles au joueur si le besoin apparaît. `screenshot` en `USER` n'accepte que le dossier par défaut : un chemin choisi reste réservé à `DEV` (`requireLevel`).

- **Accès au système** : `NONE` ou un niveau. Il remplace `EngineConfig.setConsole(bool)` :
  - `EngineConfig.setCommandAccess(...)` : niveau de la session au démarrage ;
  - `EngineConfig.setMaxCommandAccess(...)` : plafond du build (un jeu compétitif bloque à `USER`) ;
  - `engine.commands().setSessionLevel(...)` : changement en cours de partie (activer la triche), toujours borné par le plafond, appelé par le jeu ;
  - `EngineConfig.setCheatCommand(true)` enregistre la commande moteur `cheats on|off` (niveau `USER`), qui appelle `setSessionLevel(CHEAT|USER)`. Sans effet si le plafond est sous `CHEAT` : la réponse est alors celle d'une commande inconnue, la commande n'étant même pas enregistrée ;
  - défauts : en debug, accès et plafond `DEV` ; en release, accès `NONE` (pas de console, le jeu ouvre l'accès explicitement) et plafond `USER` (le jeu le relève s'il veut des triches ou un mode développeur) ;
  - `DEV` dans un jeu publié (moddeur) : « mode développeur », par un réglage du jeu ou une option de lancement (`-Dmightylib.commands.access=dev`), borné par le plafond. Pas de commande pour passer en `DEV` en cours de partie.
- **Niveau d'une source** : `ICommandSource.level()`.
  - Console en jeu : le niveau de la session.
  - `stdin` et `server#N` : `DEV`, car ils n'existent qu'en debug.
  - `api` : `DEV`.

  `Commands.getCurrentLevel()` donne le niveau de la commande en cours.
- **Refus silencieux** :
  - une commande au-dessus du niveau courant est absente de `help`, de `help <nom>` et de la complétion ;
  - tapée quand même, elle reçoit **exactement** le message d'une commande inexistante ;
  - la résolution des noms courts et le message d'ambiguïté ignorent les commandes invisibles.
- **Sous-commandes** : `requireLevel(ECommandLevel)` dans une commande renvoie la même erreur qu'un argument inconnu, et masque la complétion de cette sous-commande. Exemples : `console log`, `console follow`.
- **Scripts** : `exec` capture la source qui l'a lancé et la rétablit pour chaque ligne, y compris les lignes jouées dans les tâches de tick. Un script ne peut pas élever son niveau.
- **Journal par niveau** : chaque ligne de `ConsoleLog` porte le niveau minimal pour la voir.
  - `OUT`/`ERR` (sortie du programme) : `DEV`.
  - Commande et réponse : le niveau de la source qui l'a soumise.
  - Messages du jeu : `console.print(texte, niveau)`, `USER` par défaut.

  `ConsolePanel` n'affiche que les lignes visibles au niveau de la session. Le serveur (`DEV`) voit tout.
- **Réponses selon le niveau** : `CommandEcho` écrit le `message` au niveau de la source, et les `data` (JSON) seulement au niveau `DEV`. Règle pour écrire une commande : le `message` est une phrase lisible par un joueur, les `data` sont le détail pour le développeur. Une commande peut aussi adapter sa réponse avec `getCurrentLevel()`.
- **Touches** : deux actions moteur ouvrent la même console.
  - `TOGGLE_CONSOLE` (F1) n'agit qu'au niveau `DEV`.
  - `TOGGLE_PLAYER_CONSOLE` (`mightylib:toggle_player_console`) est liée par défaut à `²`, la touche à gauche de 1, soit `GLFW_KEY_GRAVE_ACCENT` quelle que soit la disposition. Elle agit dès le niveau `USER`, donc aussi en `CHEAT`.

  Un jeu peut relier les deux comme toute action. En debug (`DEV`), les deux touches marchent : c'est ainsi qu'on teste.
- Tests :
  - filtrage de `help`, `help <nom>` et de la complétion ;
  - message de refus identique à celui d'une commande inexistante ; ambiguïté calculée sur les seules commandes visibles ;
  - `requireLevel` ;
  - niveau des scripts hérité, lignes différées comprises ;
  - plafond, et `setSessionLevel` qui fait apparaître ou disparaître les commandes `CHEAT` ;
  - commande `cheats` : absente sans `setCheatCommand` ou avec un plafond sous `CHEAT`, sinon bascule `USER`/`CHEAT` ;
  - visibilité des lignes du journal par niveau, `data` réservées à `DEV`.

### Étape 4 — Démo et documents
- **Démo** :
  - Dans `ProjectLoading.registerCommands`, au niveau `USER` : `demo:menu` (retour au menu).
  - La config de la démo active la commande moteur `cheats` (`setCheatCommand(true)`). Le plafond est `DEV` en debug, donc la commande est disponible.
  - Une nouvelle scène `CommandsScene2D`, inscrite dans `MenuScene` (`createSceneEntries`), avec `demo:spawn <forme>` au niveau `CHEAT`. La commande crée un objet (ce qui prouve qu'elle s'exécute sous la scène), complète ses formes, et répond « scene still loading » avant le `launch`.
- **Vérification en jeu** (`scenario.py`) :
  - commande de jeu présente avant et après `scene X` ;
  - commande de scène présente dans la scène, absente après, même si la scène est quittée pendant son chargement ;
  - `leakcheck CommandsScene2D`.
- **Release** (config de test : `RELEASE`, accès `USER`, plafond `CHEAT`, commande `cheats` active) :
  - `²` ouvre la console, F1 non ;
  - `help` ne montre que `USER`, et `exec` répond comme une commande inconnue ;
  - les lignes `--` du moteur n'apparaissent pas dans la console ;
  - `demo:spawn` est inconnue, puis disponible après `cheats on` ;
  - avec le plafond par défaut (`USER`), `cheats` est inconnue.
- **Documents** : voir ci-dessous. Supprimer ce plan.

## Documents à mettre à jour à la fin (préparation de la suite)
- `CLAUDE.md` :
  - liste des tests couverts ;
  - règle « toute nouvelle commande déclare son identifiant, son niveau, sa complétion, et répond par un message lisible pour son niveau » ;
  - mention de `docs/plan/` dans la liste des documents (plans d'un travail en cours, supprimés une fois réalisés).
- `architecture.md` : section Commandes (registre par `Engine`, propriétaires, exécution sous le propriétaire, niveaux, accès, identifiants, sources) ; section Console (journal et touches par niveau).
- `debug-tools.md` : titre et section Commandes (accès), sortie de `help`, niveau `DEV` du serveur et de stdin, comment écrire une commande (`completeArgument`, niveau, `requireLevel`, message et `data`).
- `decisions.md` : les décisions ci-dessus (trois niveaux qui ne sont pas une sécurité, moddeur = `DEV`, triche activée par le jeu, niveau obligatoire, refus silencieux, sortie du programme réservée à `DEV`, commandes à propriétaire calquées sur les fenêtres).
- `later.md` :
  - *Modèle d'extension* : `IProjectLoading.registerCommands` est provisoire, à absorber dans `register(EngineRegistry)` ; les commandes moteur et jeu sont à figer après le démarrage, celles des scènes restent dynamiques.
  - *Modding* : un mod est propriétaire de ses commandes (espace = identifiant du mod, niveau `DEV`) ; insérer `MODDER` entre `CHEAT` et `DEV` si les outils de mod doivent être séparés de ceux du moteur.
  - *Lua* : commandes enregistrées par script (propriétaire = script, retirées au rechargement), `lua reload` / `lua eval` au niveau `DEV`.
  - *Console* : retirer « complétion des commandes de scène existantes ».
  - Arguments déclaratifs : un schéma d'arguments par commande qui produirait `help`, la complétion et les niveaux par sous-commande.
  - Condition de disponibilité par commande (`isAvailable()`, même refus silencieux) : commande réservée à l'hôte en multijoueur, ou valable dans un certain état de la partie.
  - Traduction des messages et aides des commandes du joueur (système de langues).
- `futur-discussion.md` : permissions par joueur en réseau (opérateur, admin, triche autorisée par le serveur).
- `current.md` : à décider avec l'utilisateur au début (question 1).

## Hors périmètre
Registre figé `register(EngineRegistry)`, chargement de mods, Lua, permissions réseau, arguments déclaratifs, condition de disponibilité, traduction.

## Questions restantes (à trancher au début du travail)
1. Ce travail entre-t-il dans `current.md` (point en cours « API : corrections urgentes », qui accepte les changements cassants), ou passe-t-il avant ou après ?

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/commandes-proprietaires-niveaux.md »)
- Lire ce plan en entier, puis « Pour démarrer ». Poser la question restante et attendre la réponse.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `command-registry`. Premier commit : ce plan tel quel.
- Un commit par étape (1 à 4), dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- L'étape 4 met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (scène `CommandsScene2D`, touches F1 et ², commande `cheats`, config release). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — PR, vérification, fusion (« Review la PR command-registry »)
- Lire ce plan et son « Bilan ».
- Relire le diff de la branche par rapport à `dev` au regard du plan et de `CLAUDE.md` ; signaler ce qui manque ou dévie avant d'aller plus loin.
- Dernier commit : supprimer ce plan (et `docs/plan/` s'il est vide) ; vérifier que les docs ne gardent aucune trace de ce qui est fini.
- Pousser, ouvrir une PR vers `dev` (pas `master`) dont la description reprend le Bilan et la liste des vérifications manuelles.
- Attendre le feu vert de l'utilisateur, puis fusionner par un commit de fusion (pas de squash), mettre `dev` à jour en local, supprimer la branche en local et sur GitHub.

## Bilan (discussion A)

### Fait
- Question restante : travail traité **à part, avant** `current.md` (réponse de l'utilisateur) : `current.md` inchangé.
- Avant la branche, sur `dev` (demande de l'utilisateur) : commit de `CLAUDE.md` + skill `/planifier`, commit de `Main.java` (fenêtre redimensionnable).
- Branche `command-registry` : plan, puis un commit par étape (1 à 4) et un commit pour `scenario.py -D`.
- Étape 1 : `Engine.commands()`, `CommandOwner` (`ENGINE`, `GAME`, vue `scene`/`window`), `register`/`registryFor`/`removeOwnedBy`, `CommandRegistry`, `IProjectLoading.registerCommands`, `Scene.addCommand`/`WindowView.addCommand` (retrait dans `unloadByManager`/`unloadByContext`), exécution et complétion sous la vue du propriétaire avec son contexte GL, `Scene.isLaunched()`, `help` affiche le propriétaire.
- Étape 2 : `BaseCommand(Identifier, ECommandLevel)`, commandes générales dans `mightylib`, résolution chemin seul / identifiant complet, ambiguïté refusée avec la liste, complétion.
- Étape 3 : `ECommandLevel`, `ICommandSource.level()`, refus silencieux (help, complétion, message identique, ambiguïté sur les visibles), `requireLevel`/`hasLevel`/`currentLevel`, scripts au niveau de leur source, `EngineConfig.setCommandAccess`/`setMaxCommandAccess`/`setCheatCommand` + `-Dmightylib.commands.access` (remplacent `setConsole`), `Commands.setSessionLevel`, `cheats on|off`, journal par niveau (`data` en `DEV`), `ConsolePanel` filtré, touche `²` (`TOGGLE_PLAYER_CONSOLE`), F1 en `DEV` seulement.
- Étape 4 : `demo:menu` (jeu), `CommandsScene2D` + `demo:spawn` (`CHEAT`, argument `slow` pour un chargement de 3 s), `setCheatCommand(true)` dans la démo, options `-Ddemo.release=true` et `-Ddemo.commands.max=…`, documents.

### Écarts au plan et raisons
- `requireLevel(niveau, mot)` prend le mot tapé : il faut le nommer pour donner exactement l'erreur d'un argument inconnu (`null` : erreur d'argument manquant).
- `requireLaunched(scène)` est une aide statique de `BaseCommand` qui lève une `CommandException` (nouvelle : sa réponse est le message seul) ; `Scene` n'expose que `isLaunched()`.
- `cheats on|off` en session `DEV` répond une erreur sans rien changer : sinon `cheats off` ferait perdre le mode développeur sans retour possible (pas de commande pour revenir en `DEV`).
- Les commandes en attente sont traitées à chaque tick, plus seulement en debug ou avec console : un jeu publié sans console peut soumettre par l'`api`.
- Complétion : les identifiants des chemins ambigus n'apparaissent qu'avec un mot vide ou une fois `espace:` tapé (les choix sont filtrés par préfixe).
- Ajouts non prévus, utiles à la vérification et au jeu : `Console.print(texte[, niveau])` et type `MESSAGE` (messages du jeu, cités par le plan), champ `level` dans `console log`, option `-D clé=valeur` de `scenario.py`.
- `wait loaded` mémorise la cible au début de l'attente (au lieu de la relire à chaque tick).
- `setMaxCommandAccess(null)` = aucun accès, jamais (ajout demandé par l'utilisateur après la première version) : sans lui, `-Dmightylib.commands.access=user` rouvrait une console dans un jeu qui n'en voulait pas, le plafond ne descendant pas sous `USER`. L'option est alors ignorée (une ligne au journal), la commande `cheats` n'est pas enregistrée. Test de régression dans `CommandAccessTest`, vérifié en jeu (`-D demo.commands.max=none -D mightylib.commands.access=user` : console désactivée, ligne « ignored » au journal).

### Vérifié et comment
- `mvn -o -q test` : 301 tests, tous verts (nouveaux : `CommandsTest` propriétaires/identifiants, `CommandLevelsTest`, `main/CommandAccessTest`).
- `scenario.py` (debug) : `demo:menu` avant et après `scene Scene2D` ; `demo:spawn` dans `CommandsScene2D`, absente après `scene MenuScene`, « Scene still loading » pendant `scene CommandsScene2D slow` puis absente après l'avoir quittée pendant son chargement ; `leakcheck CommandsScene2D` : no leak ; `gl summary` identique au menu avant et après 3 formes créées ; scènes 3D chargées sans problème.
- Niveau joueur, automatisé en debug avec `-D mightylib.commands.access=user -D demo.commands.max=cheat` (console en jeu au niveau `USER`, serveur `DEV`, saisie par `input text` + `input key ENTER`) : F1 n'ouvre pas, `²` ouvre ; `help` liste `help`, `stats`, `screenshot`, `console`, `cheats`, `demo:menu` ; `exec a.txt` → « No command with its name ->exec<- » ; `demo:spawn square` inconnue puis disponible après `cheats on` ; lignes `--` et `data` au niveau `DEV` (champ `level` de `console log`). Avec `demo.commands.max=user` : `cheats` absente de `help`.

### À vérifier à la main par l'utilisateur
- **Vraie release** (le serveur de debug n'existe pas en release, donc pas automatisable) : lancer `Main` avec l'option VM `-Ddemo.release=true`. Attendu : `²` ouvre la console, F1 non ; `help` ne montre que les commandes `USER` (dont `cheats`, `demo:menu`) ; `exec a.txt` répond comme une commande inconnue ; aucune ligne `--` du moteur dans la console ; dans `CommandsScene2D`, `demo:spawn square` est inconnue, puis crée une forme après `cheats on`. Avec en plus `-Ddemo.commands.max=user` : `cheats` est inconnue. Avec `-Ddemo.commands.max=none -Dmightylib.commands.access=user` : `²` et F1 n'ouvrent rien.
- **Debug** : F1 et `²` ouvrent tous deux la console ; dans `CommandsScene2D` (menu), `demo:spawn square|wide|tall` dessine des formes colorées et le compteur du titre suit ; Tab complète `demo:spawn` et ses formes.
- Disposition : sur AZERTY, `²` ferme la console, ce caractère ne peut donc pas être tapé dedans (comme la touche ` en QWERTY).
