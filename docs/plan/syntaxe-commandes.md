# Plan — Syntaxe déclarative des commandes (arguments typés, options `--nom`)

**Statut : validé.**

Décisions prises :
- Une **syntaxe déclarée** par commande (arbre maison, sans dépendance) d'où sortent analyse, complétion, aide, erreurs, niveaux et conditions par branche.
- Options nommées `--nom valeur` et `--nom=valeur`, drapeaux `--nom` ; `--` termine les options ; pas d'alias courts.
- Un handler par branche, qui reçoit des valeurs déjà typées.
- Aide générée depuis le schéma, avec une description par commande, branche, argument et option ; `--help` ajouté à toute commande, `help <cmd>` gardé ; `<cmd> help` disparaît.
- Schéma exposé en JSON dans les `data` de `help <cmd>`.
- Les 21 commandes (moteur et démo) sont migrées, puis l'ancien chemin est supprimé.
- Sélection de ressources : `reload TextureData grass sand --regex tile.*` (fin de `re:` et des listes `a,b`).
- Conditions par branche dans le schéma (`requires`) : refus avec message ou silencieux ; remplacent `requireLaunched` et couvrent le `isAvailable()` prévu dans `later.md`.
- Démo : la commande `spawn` de `CommandsScene2D` est enrichie (pas de nouvelle scène).
- Commandes `as` et `complete` (niveau `DEV`) pour vérifier sans l'utilisateur ce que voit le joueur.

## Pour démarrer
- Lire : `docs/debug-tools.md` section **Commandes** ; `docs/architecture.md` sections **Commandes** et **Console** ; `docs/decisions.md` (commandes à propriétaire) ; `docs/later.md` section **Console et GUI en jeu**, point *Commandes, suite*.
- Code : `src/io/github/mightycode/mightylib/utils/enginecommand/` (`BaseCommand`, `Commands`, `CommandLine`, `CommandCompletion`, `CommandResult`, `CommandException`, `general/*`), `resources/ResourceSelection`, `mightylibdemo/scenes/CommandsScene2D`, `mightylibdemo/main/ProjectLoading`.
- Branche de sujet : `command-syntax`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `BaseCommand.execute(String[] args)` reçoit les mots de `CommandLine.Split` (`args[0]` = nom tapé). Chaque commande analyse à la main : ~200 tests `args[n].equals…`/`args.length`/`Integer.parseInt`, surtout dans `InputGenCom` (463 lignes), `GamepadGenCom`, `ConsoleGenCom`, `CameraGenCom`, `WindowGenCom`, `WatchGenCom`.
- Pour chaque commande, quatre choses sont écrites séparément et dérivent : `execute`, `completeArgument(String[])` (valeurs du dernier mot, filtrées par `CommandCompletion.Filter`), `help()` (texte libre numéroté) et `requireLevel`/`hasLevel` (sous-commande réservée : même erreur qu'un argument inconnu, masquée de la complétion).
- Aides communes : `checkArgumentHelp` (`<cmd> help`), `noEnoughArgumentError`, `unknownArgumentError`, `requireLaunched(Scene)` (lance une `CommandException` « Scene still loading », utilisée par `CameraGenCom` et `CommandsScene2D`).
- Options de fait, écrites à la main : `screenshot [chemin] [rect x y l h | corners x y x2 y2] [scale f]` avec `isOptionKeyword` (un fichier nommé `rect` est mal lu) ; argument facultatif en fin de ligne : `watch mode poll [interval]`, `wait loaded [timeout]`.
- `ResourceSelection.parse(resources, args, from)` : `all|Type`, puis `a,b` et `re:regex` (utilisé par `reload`, `resources`, `watch` ; testé par `ResourceSelectionTest`).
- `JsonSelect.Select(data, chemins)` : `stats [chemins…]` et `camera` ne renvoient qu'une partie des `data`.
- `Commands` : `execute(line, source)` trouve la commande (nom ou identifiant), vérifie le niveau, appelle `execute` sous le propriétaire (`CommandOwner.run`), transforme une `CommandException` en erreur. `complete(début, source)` : noms des commandes, `help` en deuxième mot, puis `completeArgument`. La console (`console/CommandLineEditor`, `CompletionListing`) passe par `Commands.complete` et `CommandCompletion` : leur API ne change pas.
- `HelpGenCom` : liste des commandes visibles, `help <cmd>` → `command.help()`.
- `CommandMethods.indexFirstWord` : inutilisé.
- Tests : `test/.../utils/enginecommand/CommandsTest`, `CommandLevelsTest`, `CommandScriptTest`, `TestCommands` (commandes de test), `console/ConsoleInputTest`, `resources/ResourceSelectionTest`.
- Clients : chaque étape de `scenario.py` est une chaîne entière et `mighty.py` prend la commande en `argparse.REMAINDER` : les `--` des commandes ne sont pas pris par leurs propres options (à vérifier une fois en étape 5).

## Décisions
- **Package** `utils/enginecommand/syntax` : `CommandNode` (nœud : littéral ou argument, enfants, options, handler, niveau, condition, description), `ArgumentType<T>` (interface : `parse(String mot, CommandContext)` → T ou `CommandSyntaxException` ; `complete(String début, CommandContext)` → valeurs ; `describe()` pour l'usage et le JSON), `Option` (nom, description, valeurs typées 0..n, défaut, répétable, niveau, condition), `CommandContext` (valeurs lues : `get(nom)`, `getOr(nom, défaut)`, `has(nom)`, `getList(nom)` pour un variadique ou une option répétable ; `currentLevel()`), `CommandParser` (mots → branche + contexte, ou erreur), `Arguments` (types fournis).
- **Types fournis** : `integer(min, max)`, `decimal(min, max)`, `longValue`, `bool`, `word`, `text` (le reste de la ligne, un seul mot pour le handler), `choice(String…)` et `enumOf(Class)` (sans casse), `oneOf(Supplier<Collection<String>>)` (valeurs dynamiques : fenêtres, caméras, actions ; stricte), `suggest(Supplier)` (proposée mais libre), `identifier`, `path(dossier)` (reprend `PathCompletion`). Un jeu ou un mod écrit son propre `ArgumentType` (ex. `ResourceArguments.type(resources)`).
- **Construction** : `BaseCommand(Identifier, ECommandLevel, String description)` et `protected abstract void syntax(CommandNode racine)`, appelée une fois à l'enregistrement. Style chaîné Java 11 : `racine.then(literal("mode", "…").then(argument("mode", choice("poll", "service")).option(Option.of("interval", "…").value("ms", longValue(1, …))).executes(ctx -> …)))`.
- **Convention** : positionnels = ce que la commande vise (sous-commande, cible, valeur principale) ; options = réglages facultatifs. Donc `watch mode poll --interval 500`, `wait loaded --timeout 10`, `screenshot [chemin] --rect x y l h --corners x y x2 y2 --scale f`.
- **Analyse** : littéraux essayés avant les arguments au même niveau (un argument égal à un littéral prend le littéral) ; littéraux et choix sans casse, noms d'options en minuscules kebab ; une option est valable dans le sous-arbre du nœud qui la déclare, à n'importe quelle place après lui ; option inconnue, répétée (non répétable), sans sa valeur, ou valeur invalide → erreur ; `--` : la suite n'est que positionnels. Un argument variadique ne peut être que le dernier.
- **Erreurs** : message précis (« Expected an integer for <x>, got ->abc<- ») suivi de l'usage de la branche atteinte. Une branche ou option hors niveau, ou dont la condition est silencieuse et fausse, donne la même erreur qu'un mot inconnu (comme `requireLevel` aujourd'hui).
- **Niveaux** : `.level(ECommandLevel)` sur un nœud ou une option : masqué de la complétion, de l'aide et du JSON pour les niveaux inférieurs. Le niveau de la commande reste le minimum pour la voir. `currentLevel()` reste disponible au handler pour adapter la réponse.
- **Conditions** : `.requires(CommandCondition)` sur un nœud ou une option. `CommandCondition.hidden(predicate)` (silencieuse : la branche n'existe pas, ni complétion ni aide) ; `CommandCondition.check(predicate, message)` (visible, refus avec message à l'exécution). Fournie : `CommandCondition.launched(Scene)` (« Scene still loading »). Les conditions sont évaluées à chaque exécution et complétion (sur le thread principal).
- **Aide** : `--help` est une option de chaque commande ajoutée par le moteur ; `help <cmd>` et `<cmd> --help` donnent la même réponse : description, une ligne d'usage par branche exécutable visible (`watch mode <poll|service> [--interval <ms>]`) avec sa description, puis les options et leurs descriptions. Les `data` contiennent l'arbre visible en JSON (`id`, `level`, `description`, branches : `usage`, `description`, arguments `{name, type, optional, variadic, values?}`, options `{name, values, repeatable, default?}`).
- **Complétion** : `Commands.complete` demande à la commande ses candidats depuis l'arbre : littéraux et valeurs de l'argument attendu ; options visibles non encore données (et `--help`) quand le mot commence par `-` ; valeurs de l'option qui attend sa valeur. `help` n'est plus proposé en deuxième mot.
- **Sélection de ressources** : un type positionnel (`all` ou un type déclaré, complété), puis des noms en variadique (complétés parmi les ressources chargées du type), et `--regex`. `ResourceSelection` garde ses constructeurs (`all`, `ofType`, `withNames`, `withRegex`) ; `parse(String[])` est supprimé.
- **Tester un niveau sans l'utilisateur** : commande `as <user|cheat|dev> <ligne>` (niveau `DEV`) qui exécute la ligne avec une source au niveau `Min(niveau demandé, niveau de la source)` : elle ne peut que baisser l'accès, jamais l'élever. Commande `complete [--as <niveau>] <début de ligne>` (niveau `DEV`) qui renvoie dans ses `data` le résultat de `Commands.complete` (`start`, `typed`, `candidates`, `replacement`), y compris un espace final (`complete --as user "spawn "`). Ainsi Claude vérifie seul, par le serveur de debug, ce que voit le joueur (exécution, erreurs, aide, complétion), sans piloter la console par `input`.
- **Sécurité** : rien de nouveau exposé (pas de réflexion, pas d'évaluation). Le serveur de debug et l'entrée standard sont des outils de développement, absents d'un build release : la syntaxe ne change rien à leur accès.

## Étapes
Chaque étape compile, passe `mvn -o -q test` et laisse le jeu fonctionnel. Les étapes 1 à 3 peuvent être fusionnées seules (rien n'utilise encore le schéma).

1. **Noyau du schéma et analyse** — `syntax/CommandNode`, `ArgumentType`, `Arguments`, `Option`, `CommandContext`, `CommandParser`, `CommandSyntaxException`. Tests `test/.../enginecommand/syntax/CommandParserTest` : littéraux (sans casse), littéral prioritaire sur l'argument, chaque type fourni (bornes, valeur invalide), argument facultatif, variadique, `text`, options `--n v`, `--n=v`, drapeau, n valeurs (`--rect 0 0 10 10`), option à n'importe quelle place, portée d'une option, inconnue/répétée/sans valeur/répétable, `--`, branche sans handler (erreur « commande incomplète » + usage).
2. **Niveaux et conditions** — `.level`, `CommandCondition` (`hidden`, `check`, `launched`). Tests : branche et option réservées absentes pour un niveau inférieur avec l'erreur d'un mot inconnu ; condition silencieuse ; condition avec message.
3. **Complétion, aide et JSON générés** — depuis l'arbre et le niveau courant. Tests : candidats par position, options après `-` (sans celles déjà données), valeurs d'une option, rien de masqué ne fuit ; lignes d'usage et descriptions ; JSON (forme, branches masquées absentes).
4. **Branchement dans `BaseCommand` et `Commands`** — nouveau constructeur avec description, `syntax(racine)` ; `Commands.execute` analyse puis appelle le handler sous le propriétaire (`CommandException` toujours acceptée dans un handler) ; `--help` ajouté ; `HelpGenCom` utilise l'aide générée et met le JSON dans les `data`. Pendant la migration seulement : une commande qui ne déclare pas de syntaxe garde l'ancien chemin (`execute(String[])`, `completeArgument`) ; le noter dans le code comme provisoire. Mettre à jour `CommandsTest`/`CommandLevelsTest`/`TestCommands` (une commande de test passe au schéma ; `<cmd> help` → `--help`).
5. **Commandes `as` et `complete`** — écrites avec le schéma (argument `text` pour la ligne) ; source enveloppée (`ICommandSource` au niveau abaissé, même nom suivi de `(as user)` dans le journal). Tests : `as` n'élève jamais le niveau ; une commande `DEV` est inconnue sous `as user` ; `complete --as user` ne propose ni commande ni branche `DEV`. À partir d'ici, chaque vérification de niveau des étapes suivantes se fait par `as`/`complete` dans `scenario.py`.
6. **Commandes simples** — `exit`, `gl`, `monitors`, `textureupload`, `help`, `exec` (`path`), `scene`, `wait` (`--timeout`), `sound gain`, `cheats`, `windows`, `window`. Vérifier en jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "?help window" "?window --help" "?wait loaded --timeout 5" "?gl summary"` ; et une fois `python tools/mighty.py --wait 10 screenshot --help` sur un jeu lancé (`--help` transmis au jeu, pas lu par `mighty.py`).
7. **Sélection de ressources** — `ResourceArguments` (type, noms, `--regex`) ; `reload`, `resources`, `watch` (`watch mode poll|service --interval ms`) ; supprimer `ResourceSelection.parse`, adapter `ResourceSelectionTest`. Vérifier : `python tools/scenario.py "scene Scene2D" wait-loaded "?resources TextureData" "?reload TextureData --regex .*" "?watch mode poll --interval 500" "?watch off"`.
8. **`screenshot`, `stats`, `console`** — `screenshot [chemin (DEV)] --rect x y l h | --corners x y x2 y2 --scale f` (rect et corners exclusifs : erreur si les deux) ; `stats [chemins…]` avec `fps` au niveau `USER` et le reste `DEV` ; `console` et ses niveaux. Vérifier au niveau joueur que les branches `DEV` sont absentes : `"?complete --as user \"stats \""`, `"?as user help stats"`, `"?as user screenshot target/x.png"` (erreur d'argument inconnu). Vérifier : `"?screenshot target/s.png --rect 0 0 200 100 --scale 0.5"`.
9. **`camera`** (`requires(launched)` au lieu de `requireLaunched`), **`gamepad`**. Vérifier dans `Scene2D` et une scène 3D (`Scene3D`) que `camera` répond comme avant.
10. **`input`** (la plus grosse) — garder toutes ses sous-commandes ; `text` pour `input text "…"`. Vérifier : la commande d'exemple de `CLAUDE.md` (`"input ENTER"`) et `input key`/`input mouse`/`input axis` dans `Scene3D`.
11. **Démo** — `CommandsScene2D` : `spawn` avec arguments et options (voir **Démo**) et condition `launched` ; commande `demo:menu` de `ProjectLoading`. Vérifier par `complete`/`as user` (Tab, options, `spawn list` masqué au joueur).
12. **Suppression de l'ancien chemin** — retirer `execute(String[])`, `completeArgument`, `checkArgumentHelp`, `noEnoughArgumentError`, `unknownArgumentError`, `requireLevel`, `hasLevel` s'il n'est plus utilisé hors du schéma, `requireLaunched`, `CommandMethods`, le code provisoire de l'étape 4 ; `syntax` devient obligatoire. Plus aucun `args[` dans `enginecommand/general` (`grep`).
13. **Documents** (liste plus bas).

## Démo
`CommandsScene2D`, commande `spawn` : `spawn <nombre 1..100> [--color red|green|blue] [--size 0.5..4] [--at x y]`, `spawn clear`, `spawn list` (niveau `DEV`), condition `launched`. Vérification manuelle : ouvrir la scène, console (`²`/F1), `spawn` + Tab (sous-commandes et `--`), `spawn 5 --c` + Tab, `spawn --help`, `spawn 500` (erreur de borne avec usage).

## Documents à mettre à jour à la fin
- `architecture.md`, section **Commandes** : la syntaxe déclarée, le parcours analyse → handler, niveaux et conditions par branche (au présent).
- `debug-tools.md` : **Écrire une commande** (constructeur avec description, `syntax`, types, options, `level`, `requires`, convention positionnels/options) ; **Complétion** ; toutes les syntaxes changées (`screenshot --rect/--corners/--scale`, `watch mode … --interval`, `wait loaded --timeout`, sélection de ressources avec `--regex`, `help <cmd>`/`--help`) et les exemples `scenario.py` ; `as` et `complete` (tester le niveau joueur par le serveur), qui remplacent la manipulation de la console par `input` dans la section **Accès**.
- `CLAUDE.md` : `screenshot --rect x y largeur hauteur` ou `--corners x y x2 y2` (règle *Économiser les tokens*) ; convention « Toute nouvelle commande déclare… » : sa syntaxe et ses descriptions au lieu de `completeArgument` ; ajouter `utils/enginecommand/syntax` et la sélection de ressources aux tests couverts.
- `decisions.md` : syntaxe maison plutôt que Brigadier (pas d'options nommées, messages et niveaux à adapter, dépendance) ; options `--nom` sans alias courts (collisions entre moteur, jeu et mods).
- `later.md` : retirer « arguments déclaratifs » et `isAvailable()` (faits par le schéma et `requires`) ; garder la traduction ; ajouter ce qui est préparé (voir **Hors périmètre**).
- `licence.md` : rien (pas de dépendance).
- Supprimer ce plan.

## Hors périmètre
- Traduction des descriptions et messages (déjà dans `later.md`, *Commandes, suite* : les descriptions seront des clés de langue).
- Conditions de multijoueur (réservé à l'hôte) : le mécanisme `requires` existe, les conditions viendront avec le réseau (`futur-discussion.md`, permissions par joueur).
- Complétion dans `mighty.py` à partir du JSON de `help`, doc des commandes générée : à noter dans `later.md`.
- Commandes Lua : elles déclareront leur syntaxe par la même API (`later.md`, Entités et scripts Lua).

## Questions restantes
- Aucune.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/syntaxe-commandes.md »)
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `command-syntax`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (console de `CommandsScene2D` : Tab sur `spawn` et ses options, `spawn --help`, erreurs ; console au niveau joueur avec `-Ddemo.release=true` : branches `DEV` absentes de Tab et de `help` ; `camera` dans `Scene3D`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — PR, vérification, fusion (« Review la PR command-syntax »)
- Lire ce plan et son « Bilan ».
- Relire le diff de la branche par rapport à `dev` au regard du plan et de `CLAUDE.md` ; signaler ce qui manque ou dévie avant d'aller plus loin.
- Dernier commit : supprimer ce plan (et `docs/plan/` s'il est vide) ; vérifier que les docs ne gardent aucune trace de ce qui est fini.
- Pousser, ouvrir une PR vers `dev` (pas `master`) dont la description reprend le Bilan et la liste des vérifications manuelles.
- Attendre le feu vert de l'utilisateur, puis fusionner par un commit de fusion (pas de squash), mettre `dev` à jour en local, supprimer la branche en local et sur GitHub.

## Bilan
### Fait (étapes 1 à 13)
- Noyau (`utils/enginecommand/syntax` : `CommandNode`, `ArgumentType`, `Arguments`, `Option`, `CommandContext`, `CommandParser`, `CommandSyntaxException`, `CommandCondition`, `CommandHelp`), niveaux et conditions par branche, complétion, aide et JSON générés, branchement dans `BaseCommand`/`Commands`, commandes `as` et `complete`.
- Les 21 commandes générales et celles de la démo (`demo:spawn`, `demo:menu`) déclarent leur syntaxe ; `ResourceArguments` (type, noms, `--regex`) sert `reload`, `resources`, `watch` ; `ResourceSelection.parse` supprimé.
- Ancien chemin supprimé : `execute(String[])`, `completeArgument`, `checkArgumentHelp`, `noEnoughArgumentError`, `unknownArgumentError`, `requireLevel`, `hasLevel`, `currentLevel()`, `requireLaunched`, `CommandMethods`, tout le code `Provisional` ; `syntax` est abstraite, une commande sans syntaxe est refusée à l'enregistrement. Plus aucun `args[` dans `enginecommand/general`.
- Documents mis à jour (`CLAUDE.md`, `architecture.md`, `debug-tools.md`, `decisions.md`, `later.md`) ; `licence.md` inchangé.

### Écarts au plan, et pourquoi
- Étapes 1 à 5 : `Option.excludes(...)` (`--rect`/`--corners` : erreur d'analyse, retirés de la complétion), `executes(description, handler)`, section « Arguments » dans l'aide, `CommandContext.source()`/`commandName()`, `CommandCondition.launched(Supplier)` (la scène visée change avec la fenêtre), `ArgumentType.fixedValues()`/`isGreedy()` ; une condition `hidden` sur la racine rend la commande inexistante (couvre `isAvailable()`).
- « `sound gain` » : la commande s'appelle `changegain` ; son nom est inchangé (`changegain list`, `changegain <nœud> <valeur>`).
- `input click` : `input click [x y]` ou `input click left|right|middle [x y]`. Les arguments d'un même niveau sont essayés dans l'ordre sans retour en arrière : un bouton numérique serait lu comme `x`. Le bouton numérique reste accepté par `input mouse hold|release [bouton]`.
- `camera` : les actions (`move`, `lookat`, `zoom`, `rotation`) sont proposées pour toute caméra (une condition ne lit pas les valeurs de la ligne) ; une action de l'autre dimension répond « The camera x is 2D : move, zoom, rotation ». Un nom ou id inconnu est maintenant une erreur de type (« Expected one of … ») au lieu de « Unknown camera ».
- Messages d'erreur d'analyse (« Unknown argument ->x<- », « Expected … for <arg>, got ->x<- », « Incomplete command » suivis de l'usage) à la place des anciens « Unknown argument(s) ->x<- » / « No argument ! ». `stats` seul est « Incomplete command » pour le joueur.
- `gamepad <id> axis … <valeur>` : valeur bornée à -1..1 (documenté, non vérifié avant) ; `scene <nom> args…` : un argument de scène qui commence par `--` est lu comme une option.
- `demo:spawn` : ajout de `--shape square|wide|tall` (défaut `square`) pour garder les formes de l'ancienne commande ; `spawn clear` décharge les renderers créés.
- `help` : type maison (`commandNames` pour la complétion) ; l'erreur d'une commande inconnue devient « Unknown command ->x<- ».
- Tests : `CommandsTest`, `CommandLevelsTest`, `CommandScriptTest`, `ConsoleInputTest` migrés au schéma (commandes de test par `TestCommands.Syntax` ou classes qui surchargent `syntax`) ; test `commandWithoutSyntaxKeepsItsHelp` supprimé avec le code provisoire ; `ResourceSelectionTest` passe par `ResourceArguments`. Scripts locaux non versionnés `tools/scripts/*.txt` : `wait loaded 60` → `wait loaded --timeout 60`.

### Vérifié, et comment
- `mvn -o -q test` passe à chaque étape (dont `CommandLevelsTest.generalSubCommandsOfTheDeveloperAreHidden` adapté : `stats`/`screenshot`/`console` du joueur, `screenshot --rect … --corners …` refusé).
- `scenario.py` à chaque étape : `help <cmd>`/`<cmd> --help`, `wait loaded --timeout`, `gl summary`, `monitors modes`, `textureupload`, `exec --help`, `scene`, `window mode`, `cheats` (étape 6) ; `resources TextureData`, `reload TextureData --regex .*`, `watch mode poll --interval 500`, `watch TextureData --regex tile.*`, types et regex invalides, complétion de `reload` (étape 7) ; `complete --as user "stats "` (`fps` seul), `as user help stats`, `as user screenshot target/x.png` (erreur d'argument inconnu), `as user console log`, `screenshot target/s.png --rect 0 0 200 100 --scale 0.5`, `--rect` avec `--corners` refusé (étape 8) ; `camera` dans `Scene2D` et `Scene3D`, `gamepad` complet avec une manette virtuelle (étape 9) ; `input` : action, `key`, `key hold|release`, `mouse`, `click` (3 formes), `scroll`, `text`, `state`, erreurs, complétion, dans `Scene3D` (étape 10) ; `spawn` (bornes, options, `--help`, `spawn list` masqué au joueur, `complete --as cheat`, `leakcheck CommandsScene2D` : pas de fuite) (étape 11).
- Non refait en étape 6 : `python tools/mighty.py --wait 10 screenshot --help` sur un jeu lancé (le prédécesseur avait vérifié que `mighty.py` et `scenario.py` transmettent les `--`).

### À vérifier à la main par l'utilisateur
- Console de `CommandsScene2D` (`²` ou F1) : `cheats on` ; `spawn` + Tab (sous-commandes, `clear`) ; `spawn 5 --c` + Tab (`--color`, puis `blue green red`) ; `spawn 5 --color red --size 2 --at 300 200` ; `spawn --help` ; `spawn 500` (erreur de borne avec usage) ; `spawn clear`.
- Console au niveau joueur avec `-Ddemo.release=true` : Tab sur `stats ` (seulement `fps`), `console ` (pas de `log`/`follow`), `screenshot ` ; `help stats` et `help` sans branches `DEV` ; `screenshot a.png` refusé comme argument inconnu ; `spawn list` absent.
- Tab sur `reload `, `reload TextureData ` (noms des ressources), `watch mode `, `input key `, `window mode `, `gamepad ` en développeur ; `help <cmd>` d'une commande et `<cmd> --help`.
- `camera` dans `Scene3D` : `camera`, `camera main3D lookat 1 2 3`, `camera main3D move 1 2 3` ; une action 2D sur la caméra 3D donne l'erreur lisible.
- Une scène en chargement : `scene CommandsScene2D slow` puis `spawn 1` tout de suite : « Scene still loading ».
