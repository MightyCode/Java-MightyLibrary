# Plan — Test de fumée de toutes les scènes et canal des problèmes

**Statut** : validé.

Décisions prises :
- Périmètre : test de fumée de toutes les scènes de `DemoScenes`, canal des problèmes du moteur, et la part « fuite / exception » du test d'acceptation de `current.md`. La couverture réelle et la comparaison d'images ont chacune leur plan, plus tard.
- Le moteur a un **canal typé des problèmes** (`Logger`), que le kit écoute. Aucune regex sur `System.err`.
- **Tout test du kit** (`@MightySceneTest`, `@MightyEngineTest`, `TestEngine` seul) échoue sur un ERROR non annoncé, comme `LogAssert` d'Unity.
- Les `System.err.println` de la bibliothèque (88) et de la démo passent par `Logger.Error` / `Logger.Warn`, chacun classé par gravité. Une règle et un test interdisent ensuite un `System.err` direct.
- Le test de fumée est paramétré sur `DemoScenes.ALL` : toute scène est couverte par défaut. Les cas particuliers sont déclarés dans une table, avec leur raison.
- Deux chargements par scène : scène → vide → scène → vide. Les fuites sont comparées sur la scène vide.
- Le CI télécharge le paquet de démonstration, avec un cache. En local, une scène qui en a besoin est ignorée (`assume`) s'il manque.
- Budget : le test de fumée prend au plus 2 min en CI. On le mesure avant de régler les pas.
- Annonce d'un problème voulu : `expectProblem(niveau, motif)`. Le test échoue aussi si le problème annoncé n'arrive jamais.
- Les WARNING ne font pas échouer un test. Ils sont gardés, listés dans le message d'un échec et lisibles par le test.
- Débogage GL, par le callback du moteur : type ERROR ou sévérité HIGH → ERROR ; MEDIUM, LOW et type PERFORMANCE → WARNING ; NOTIFICATION ignorée.
- Commande DEV `problems` (compteurs, derniers problèmes, `clear`). `scenario.py` s'en sert pour son bilan et garde sa regex seulement pour ce qui échappe à `Logger`.
- Un bug révélé par le canal ou par la fumée est **corrigé dans ce point**, même hors du sujet, dans son propre commit avec un test qui le reproduit. C'est une exception voulue à la règle « noter dans `later.md` » de `CLAUDE.md` : le but du point est d'avoir un filet vert.
- Le canal reste actif en release : un jeu pourra y brancher son rapport de plantage. Sans écouteur inscrit, un message ne coûte qu'un parcours de liste vide, sans allocation.
- Les fenêtres ouvertes entrent dans le contrôle des fuites du kit (`leakSnapshot`), pour `MultiWindowScene` et toute scène qui ouvre une fenêtre.

## Pour démarrer
- Lire : `docs/current.md` ; `docs/architecture.md`, sections « Kit de test », « Commandes », « Console », « Ressources CPU » (échecs) et « Pièges connus » ; `docs/debug-tools.md`, sections « Commandes », « `scenario.py` », « Intégration continue », « Paquet de démonstration » ; `docs/bonnes-pratiques.md`, points 3, 4, 9 et 14 ; `CLAUDE.md`, « Conventions de code » (logs).
- Branche de sujet depuis `dev` : `feature/scene-smoke-tests`.
- Compiler et tester avec les commandes de `CLAUDE.md` (Windows : Maven et JDK d'IntelliJ). Vérifier en jeu avec `tools/scenario.py`, en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Kit** (`mightylib-test/src/main/java/io/github/mightycode/mightylib/test/`) :
  - `SceneRunner` : `start(nom)` par le registre des scènes (`SceneRegistry.match`), `untilLoaded()`, `toEmptyScene()`, `frames(n)`, entrées simulées, `leakSnapshot()` / `assertNoLeak()` (objets GL, `retainedObjects`, éléments partagés, ressources référencées, `sound.ownedSources`).
  - `MightySceneExtension` : un moteur à fenêtre cachée par configuration, gardé dans le store racine. `beforeEach` passe sur la scène vide et prend l'instantané ; `afterEach` revient à la scène vide et compare.
  - `MightySceneTest` : `config`, `sources`, `checkLeaks`.
  - `TestEngine` : ressources strictes par défaut (`allowResourceFailures()` avant `start`), son muet, périphériques réels ignorés, fenêtre en `WINDOWED`.
- **Le manque** : le kit ne voit que les exceptions qui remontent de `step()` et les fuites. Il ne voit pas :
  - `Logger.LogException` (`mightylib/src/main/java/io/github/mightycode/mightylib/utils/Logger.java`, 23 appels : crochets isolés, workers, chargements). Il écrit seulement dans `System.err`.
  - Les messages de débogage GL : `GLContext.setupDebugMessages` → `GLUtil.setupDebugMessageCallback(System.err)`, dans `graphics/internal/GLContext.java`.
  - Les erreurs OpenAL : `OpenALSoundService.DisplayError`, dans `sounds/OpenALSoundService.java`.
  - 88 `System.err.println` de la bibliothèque, dont de vrais bugs : « Uniform … not found » (`Shader`), « GL element released more than referenced » (`GLResources`), « No animation named » (`Animator`), « Batch … unloaded but not loaded » (`BatchManager`). D'autres sont des données ou des réglages invalides : propriétés inconnues d'`EngineConfig`, mappings de manettes, `ShaderService`. La démo en a 21 (dont `Scene3D`, « Model 'backpack' missing »).
  - Seul `Logger.CheckOpenGLError` lève une exception.
- **`scenario.py`** repère les problèmes par la regex `PROBLEM_PATTERN` sur le journal. `leakcheck SCENE [N] [VIA]` fait des allers-retours, hors CI.
- **Scènes** : `demo/src/main/java/io/github/mightycode/mightylibdemo/scenes/DemoScenes.java`, 39 entrées dans `ALL` (package-private, `RelativeName(type)` donne le nom de `start`). Quatre ont déjà un test GL : `Scene2DTest`, `VectorShapes2DTest`, `BatchStress2DGLTest`, `TileMapBatchGLTest`. Configuration des tests : `demo/src/test/java/io/github/mightycode/mightylibdemo/main/DemoTestConfig.java`.
- **Scènes à cas particulier probable** (à confirmer par la mesure) :
  - `FallbackScene` et `LayeredTexture2D` : ressources en échec voulu, demandent un moteur non strict. `ResourceSourcesScene2D` est à vérifier.
  - `ConsoleFeedScene` écrit exprès dans `System.err`. Ces lignes restent hors du canal : rien à faire.
  - `MultiWindowScene` : fenêtres et second contexte.
  - `DisplayModesScene` : modes d'affichage. Le kit impose `WINDOWED`.
  - `PreloadScene` : passe à une autre scène sur Entrée.
  - `Scene3D` : paquet de démonstration.
  - Scènes lourdes : `BatchStress2D`, `TerrainGeneration3D` et `GLLoading` (envoi par bandes).
  - `GamepadScene`, `SoundFormatsScene` et `GUIWidgetsScene` : aucune manette, son muet.
- **CI** : `.github/workflows/ci.yml`, `xvfb-run -a mvn -B verify -Dmightylib.test.requireGL=true`, environ 2 min 30 au 2026-11-26. `tools/demo_assets.py` télécharge depuis une release GitHub, avec contrôle sha256 ; `--check` fonctionne sans réseau.
- **Ailleurs** :
  - Bevy lance chaque exemple en CI sous Xvfb, avec une configuration qui quitte après N frames (`bevy_dev_tools::ci_testing`).
  - Unreal fournit `Maps.LoadAllInGame`, rangé dans les tests lourds, pas dans les tests « smoke ».
  - Unity fait échouer un test sur toute erreur ou exception du journal, sauf `LogAssert.Expect`. Un message annoncé qui n'arrive pas fait échouer le test aussi.

## Décisions
- **`Logger` reste statique** (nature de processus, comme `Workers`). Le code sans accès au moteur (algorithmes, loaders sur les workers) y écrit. Les écouteurs sont enregistrés par le moteur (commande `problems`) et par le kit. Le noter dans `decisions.md`.
- **Nouveaux types dans `io.github.mightycode.mightylib.utils`** (`STABLE`, comme `Logger`), noms à confirmer à l'étape 1 :
  - `ELogLevel { WARNING, ERROR }`, ouvert à `INFO` plus tard.
  - `LogEntry` (`final`, champs privés, accesseurs) : niveau, catégorie, message, `Throwable` éventuel, nom du thread, instant.
  - `ILogListener { void onLog(LogEntry entry); }`.
- **API de `Logger`**, en PascalCase comme `LogException` :
  - `Error(String category, String message)` et `Warn(String category, String message)`. `LogException(message, e)` donne un ERROR avec la pile.
  - `AddListener` / `RemoveListener`, sans danger entre threads : `CopyOnWriteArrayList`.
  - Un écouteur qui lève une exception est retiré du flux : son erreur va dans `System.err` brut, jamais dans le canal (sinon boucle).
  - Catégories courtes en constantes : `gl`, `sound`, `resources`, `shaders`, `engine`, `input`, `config`. Un jeu donne la sienne.
- **Sortie inchangée** : chaque appel écrit la même ligne qu'aujourd'hui dans `System.err`, avec le préfixe `--`. La console (`LogCapture`) la voit donc toujours.
- **Gravité** : ERROR = bug (état incohérent, uniform absent, exception inattendue, erreur GL ou OpenAL). WARNING = donnée ou réglage invalide attendu (propriété inconnue, mappings de manettes invalides, ressource en repli hors mode strict, avertissement GL). Les messages d'information (`System.out`) restent tels quels.
- **`ResourceException`** : en mode strict, elle remonte comme aujourd'hui. Hors mode strict, son échec donne un WARNING (catégorie `resources`).
- **Débogage GL** : un callback du moteur (`GLDebugMessageCallback`) remplace `GLUtil`. Correspondance : voir les décisions prises. La ligne écrite garde la forme lisible d'aujourd'hui (source, type, sévérité, message).
- **Kit** :
  - `TestEngine` écoute le canal de `start` à `close`, et sépare ce qui arrive pendant chaque test.
  - `TestEngine.expectProblem(ELogLevel, String regex)` vaut pour le test en cours.
  - Contrôle à la fin de chaque test, par les deux extensions, et à `close` pour un `TestEngine` seul. Le test échoue s'il y a un ERROR non annoncé ou une annonce jamais satisfaite. Le message nomme chaque ERROR (la pile du premier en entier, les autres en une ligne) et liste les WARNING.
  - Un ERROR pendant `start` fait échouer le démarrage, avec le message.
  - `TestEngine.problems()` rend ce que le test en cours a reçu, WARNING compris.
- **`@MightySceneTest(strictResources = false)`** : nouvel attribut, qui fait partie de la clé du moteur gardé (comme `config` et `sources`). Il évite qu'une classe appelle `allowResourceFailures` à la main.
- **Commande `problems`** (DEV, propriétaire moteur) :
  - Le moteur garde les 200 derniers dans un anneau et leurs compteurs par niveau et par catégorie depuis le démarrage.
  - `problems` : compteurs et 20 derniers ; `--level error|warning` et `--last N` pour filtrer ; `problems clear`.
  - Message lisible, détail (piles comprises) dans les `data`.
- **Test de fumée** : `DemoScenesSmokeGLTest`, dans `demo/src/test/java/.../scenes/` (accès à `ALL`).
  - `@ParameterizedTest` + `@MethodSource` sur les entrées non lenientes de `ALL`.
  - Par scène : `start(nom, args)`, `untilLoaded()`, N pas (`step`, défaut 30), `toEmptyScene()`, de nouveau `start`, `untilLoaded()` et N pas. L'extension fait le retour final à la scène vide et compare les fuites.
  - Seconde classe `DemoScenesLenientSmokeGLTest`, `@MightySceneTest(strictResources = false)`, pour les scènes déclarées lenientes. Les deux classes partagent la même méthode d'exécution.
- **Table `SmokeCases`** (dans le dossier des tests de la démo), une entrée par scène à cas particulier :
  - pas, arguments de scène, entrées simulées par pas, problèmes annoncés ;
  - `lenient` ; besoin du paquet de démonstration ;
  - bug connu : l'invocation est ignorée (`assume` avec raison), et le bug est inscrit dans `later.md`.
  
  Un test sans GL, `SmokeCasesTest`, vérifie que chaque entrée nomme une scène de `ALL` et que chaque raison est remplie. Une scène sans entrée reçoit le traitement par défaut.
- **Bug révélé** par le canal ou par la fumée, dans la démo comme dans la bibliothèque : il est corrigé dans son propre commit (`Fix: …`), avec un test qui le reproduit (sans GL si possible, sinon un test GL du kit), et il est listé dans le « Bilan ». Si un bug demande un changement de conception (pas un correctif), s'arrêter et demander. Le statut « bug connu » de `SmokeCases` ne sert alors que pour ce que l'utilisateur choisit de laisser, avec le bug inscrit dans `later.md`.

## Étapes (un commit chacune)
1. **Canal des problèmes dans `Logger`**
   - Fichiers : `utils/Logger.java`, `ELogLevel`, `LogEntry`, `ILogListener`.
   - `LogException` donne un ERROR. `OpenALSoundService.DisplayError` donne un ERROR `sound`. `CheckOpenGLError` donne un ERROR `gl`, puis lève son exception comme aujourd'hui.
   - Callback de débogage GL du moteur dans `GLContext.setupDebugMessages` ; libération inchangée.
   - Tests sans GL (`LoggerTest`) : écouteurs ajoutés et retirés, niveaux, `LogException` porte l'exception, entrées venues de plusieurs threads, écouteur qui lève une exception isolé (pas de boucle), même texte qu'avant dans `System.err` (flux capturé dans le test).
   - Test de la correspondance du débogage GL : fonction pure `(source, type, sévérité) → niveau ou ignoré`.
   - En jeu : `scenario.py "scene Scene2D" wait-loaded` donne un journal identique.
2. **Migration des `System.err.println`**
   - Bibliothèque (88) et démo (21) passent à `Logger.Error` / `Logger.Warn`, classés par gravité. Le classement de chaque message (fichier, ERROR ou WARNING, raison) est donné dans le message du commit.
   - Exceptions voulues : `LogCapture`, `Logger` lui-même, `ConsoleFeedScene` (sortie brute voulue), le message « GLFW fail to initialize » d'avant le moteur s'il ne peut pas passer par `Logger`.
   - `ResourceException` hors mode strict donne un WARNING `resources`.
   - Test sans GL `NoDirectStderrTest` : il parcourt `mightylib/src/main/java` et `demo/src/main/java` et refuse `System.err.print` hors d'une liste d'exceptions (chacune avec sa raison). Comme `PublicFieldsTest`, mais par lecture des sources (fichiers lus depuis la racine du module).
   - En jeu : `scenario.py "scene FallbackScene" wait-loaded` donne les mêmes lignes qu'avant.
3. **Commande `problems`**
   - Service ou état du moteur qui écoute le canal à `Engine` start et le retire à l'arrêt : anneau de 200, compteurs. Commande dans `utils/enginecommand/internal/ProblemsGenCom.java`, avec syntaxe déclarée, niveau DEV et description.
   - `scenario.py` : à la fin, `problems` est interrogé par le serveur de debug et son bilan est ajouté au résumé. Le code de sortie vaut 1 s'il y a un ERROR. La regex reste pour ce qui échappe à `Logger` : pile JVM, `[LWJGL]`, plantage avant le moteur.
   - Tests : `ProblemsGenComTest` (compteurs, filtres, `clear`, anneau plein, `data`), sur le moteur sans GL du kit.
   - En jeu : `scenario.py "scene FallbackScene" wait-loaded "?problems"`.
   - Peut être fusionnée avec l'étape 1 si elle reste petite.
4. **Kit : un test échoue sur un ERROR non annoncé**
   - `TestEngine` (écoute, séparation par test, `expectProblem`, `problems()`, contrôle à `close`).
   - `MightySceneExtension` et `MightyEngineExtension` : contrôle en `afterEach`, après celui des fuites. Les deux échecs sont réunis dans un seul message si les deux arrivent.
   - `MightySceneTest.strictResources`.
   - Tests du kit (`ProblemsKitTest`) :
     - un ERROR non annoncé fait échouer le contrôle ;
     - un ERROR annoncé passe ;
     - une annonce jamais satisfaite échoue ;
     - un WARNING passe et figure dans `problems()` ;
     - un ERROR d'un worker est compté dans le test en cours ;
     - un ERROR au démarrage fait échouer `start` ;
     - un moteur `strictResources = false` est distinct du moteur strict.
     
     On teste la méthode de contrôle directement. `junit-platform-testkit` n'est ajouté que si un test de l'extension elle-même est nécessaire ; dans ce cas, le dire et ajouter la dépendance à `licence.md`.
   - Puis `mvn -o -q test` sur tous les modules. Chaque test existant qui échoue sur un problème jusqu'ici muet est corrigé (bug, même hors du sujet) ou reçoit son annonce avec une raison (comportement voulu). Chaque correction de bug a son propre commit et son test.
5. **Test de fumée de toutes les scènes**
   - `DemoScenesSmokeGLTest`, `DemoScenesLenientSmokeGLTest`, `SmokeCases`, `SmokeCasesTest`.
   - Mesure d'abord : temps de chaque scène en local (sortie du test, `System.nanoTime` par invocation), puis en CI (durée du job, poussée de la branche). Ajuster les pas, les arguments des scènes lourdes et la table pour tenir le budget (fumée ≤ 2 min en CI).
   - Si une scène lourde n'a pas d'argument de taille, en ajouter un à la scène (`launch(args)`, documenté dans sa description) plutôt que de l'exclure.
   - `MultiWindowScene` : vérifier que ses fenêtres et son second contexte sont fermés au retour à la scène vide. Ajouter les fenêtres ouvertes (nombre par contexte, nombre de contextes, lus par la commande `windows`) à `SceneRunner.leakSnapshot`, avec un test du kit qui ouvre une fenêtre dans une scène sans la fermer et voit la fuite (`SceneRunnerTest`).
   - `PreloadScene` : un `press ENTER` à la fin de ses pas, pour couvrir le changement préchargé.
   - Chaque scène qui échoue : bug corrigé (un commit, avec un test). « Bug connu » seulement sur décision de l'utilisateur.
6. **CI : paquet de démonstration**
   - `ci.yml` : étape `actions/cache` sur `resources/models/backpack` (clé = sha256 de `tools/demo_assets.json`), puis `python tools/demo_assets.py`, avant Maven.
   - Dans la table, `Scene3D` est marquée « paquet requis » : `assume` avec la raison « run python tools/demo_assets.py » quand `demo_assets.py --check` échoue. Ce contrôle se fait par la présence des fichiers du manifeste, sans lancer Python.
   - En CI, avec `mightylib.test.requireGL=true`, un paquet manquant fait échouer au lieu d'ignorer : nouvelle propriété `mightylib.test.requireDemoAssets=true`, posée par le CI, propre à la démo (rien dans le kit).
   - `licence.md` : le CI télécharge le modèle sans le redistribuer. Le vérifier et le noter.
7. **Acceptation (fuite, exception, ERROR)**
   - Une fois, sur une branche temporaire `tmp/smoke-acceptance` jamais fusionnée : trois défauts volontaires dans trois scènes de la démo, en trois commits.
     - Un objet GL créé au `launch` et jamais libéré.
     - Une exception au `launch`.
     - Un `Logger.Error` dans l'`update`.
   - `mvn -o -q test` doit échouer en local pour chacun, avec un message qui nomme la scène et le défaut. Pousser la branche : le CI doit échouer aussi. L'utilisateur lit le résultat du CI.
   - Supprimer la branche (locale et distante). Noter le résultat dans le « Bilan ». Aucun commit sur la branche de sujet, sauf le bilan.
8. **Documents** (voir plus bas).

## Démo
Pas de nouvelle scène : le test de fumée porte sur toutes les scènes existantes. `FallbackScene` montre déjà les WARNING de repli ; `problems` s'essaie dessus.

## Documents à mettre à jour à la fin
- `architecture.md` :
  - nouvelle section « Journal et problèmes » (`Logger`, niveaux, catégories, écouteurs, sortie, débogage GL, commande `problems`) ;
  - « Kit de test » : contrôle des problèmes, `expectProblem`, `problems()`, `strictResources`, test de fumée et table `SmokeCases` ;
  - « Ressources CPU » : échec hors mode strict = WARNING.
- `debug-tools.md` :
  - commande `problems` ;
  - `scenario.py` : bilan par `problems`, regex restante ;
  - « Intégration continue » : paquet de démonstration téléchargé, `mightylib.test.requireDemoAssets`, test de fumée ;
  - « Paquet de démonstration » : retirer « La CI n'en a pas besoin ».
- `decisions.md` : `Logger` statique et canal typé plutôt qu'une regex ; tout test du kit échoue sur un ERROR non annoncé (modèle d'Unity).
- `bonnes-pratiques.md` :
  - point 9 : `Logger.Error` / `Logger.Warn`, jamais `System.err` direct (vérifié par `NoDirectStderrTest`) ;
  - point 4 : journal sans problème = `problems` sans ERROR.
- `CLAUDE.md` :
  - « Conventions de code », logs : `Logger.Error` / `Logger.Warn` (catégorie, préfixe `--` ajouté par `Logger`), `System.out` pour l'information seulement ;
  - liste des tests couverts : `LoggerTest`, `NoDirectStderrTest`, `ProblemsGenComTest`, `ProblemsKitTest`, `SmokeCasesTest`, `DemoScenesSmokeGLTest` (toute scène de `DemoScenes` chargée deux fois sans fuite ni ERROR) ;
  - règle : une nouvelle scène de la démo passe le test de fumée ; un cas particulier va dans `SmokeCases` avec sa raison.
- `later.md` :
  - bugs que l'utilisateur a choisi de laisser (s'il y en a) ;
  - suites du canal : niveau `INFO`, sortie dans un fichier du dossier du joueur, filtre de la console par niveau ;
  - retrait de la regex de `scenario.py` quand plus rien n'y échappe.
- `current.md` :
  - supprimer la ligne « Test de fumée » ;
  - réduire « Test d'acceptation » à son reste (rendu changé dans une scène de référence, avec la comparaison d'images) ;
  - mettre à jour le compte des scènes couvertes dans le critère de fin si besoin.
- **Supprimer ce plan** (fait par `/pr-ml`).

## Hors périmètre
- Couverture réelle (`coverage.py` avec les tests GL, du kit et de la démo) et comparaison d'images : points suivants de `current.md`, chacun avec son plan.
- Niveau `INFO`, journal dans un fichier, filtres de la console par niveau : `later.md` (étape 8).
- Plusieurs moteurs de test en parallèle : déjà dans `later.md`, « Kit de test : suites ».

## Questions restantes
Aucune.

## Modèles recommandés
- **Discussion A**
  - Étapes 1, 3 et 4 (API du canal, kit, contrôle) : Claude Opus 5.5, effort `high`. API publique durable et logique de test délicate (threads, annonces).
  - Étape 2 (migration de 109 messages et leur classement) : Claude Sonnet 5.5, effort `medium`. Mécanique, mais chaque gravité demande du jugement.
  - Étapes 5 à 7 (fumée, scènes qui échouent, CI) : Claude Opus 5.5, effort `high`. Diagnostic de scènes et de fuites.
  - Étape 8 (docs) : Claude Sonnet 5.5, effort `medium`.
- **Discussion B** (`/pr-ml`) : Claude Opus 5.5, effort `high`. Gros diff, qui touche l'API publique et le kit.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/tests-fumee-scenes.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/scene-smoke-tests`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur. La commiter. À vérifier à la main :
  - journal de la console identique dans `FallbackScene` et `Scene2D`, lignes `--` en rouge comme avant ;
  - `problems` dans la console (touche à gauche de 1) après `FallbackScene` ;
  - pas de nouveau message de débogage GL sur le pilote de l'utilisateur dans `Scene2D`, `Scene3D` et `BatchStress2D` ;
  - durée du CI et résultat de la branche d'acceptation, lus par l'utilisateur.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/scene-smoke-tests`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
### Fait (branche `feature/scene-smoke-tests`)
- Étape 1 : canal typé dans `Logger` (`ELogLevel`, `LogEntry`, `ILogListener`, `Error`/`Warn`/`Log`, catégories), `LogException` = ERROR `engine`, OpenAL = ERROR `sound` (une ligne au lieu de trois), `CheckOpenGLError` = ERROR `gl`, callback de débogage GL du moteur ; `LoggerTest`, `GLDebugMessagesTest`.
- Étape 2 : 85 messages de la bibliothèque et 3 de la démo (la démo n'en avait que 3 directs, pas 21) migrés, classement dans le message du commit ; `NoDirectStderrTest` et `DemoNoDirectStderrTest`.
- Étape 3 : commande `problems` (`ProblemLog`, anneau de 200), `scenario.py` l'interroge et échoue sur un ERROR ; `ProblemsGenComTest`.
- Étape 4 : contrôle des problèmes du kit (`ProblemRecorder`, `expectProblem`, `problems()`, extensions, `close`, `start`), `@MightySceneTest(strictResources)` ; `ProblemsKitTest` (18 tests).
- Étape 5 : `DemoScenesSmokeGLTest`, `DemoScenesLenientSmokeGLTest`, `SmokeCases`, `SmokeRun`, `SmokeCasesTest`, `DemoAssets` ; contextes et fenêtres dans `leakSnapshot` (`SceneRunnerTest`).
- Étape 6 : CI avec le paquet de démonstration en cache et `-Dmightylib.test.requireDemoAssets=true` ; `licence.md`.
- Étape 7 : branche `tmp/smoke-acceptance` (fuite dans `TextPaddingScene`, exception au `launch` de `CollisionGridScene2D`, `Logger.Error` dans l'`update` de `RopeSimulation2D`) : `mvn test` échoue en local et le CI aussi (run 45917642066), exactement sur ces trois scènes, chaque message nommant la scène et le défaut ; branche supprimée (locale et distante).
- Étape 8 : documents.

### Bugs révélés et corrigés (un commit chacun)
- `MatrixRenderer` posait une couleur que `multiColoredShape2D` n'a pas (« Uniform color not found ») : vu par `MeshBatchGLTest` sous le contrôle du kit.
- `MultiWindowScene` laissait ses seconds contextes ouverts et réutilisait leurs noms (exception au second chargement) : vu par la fumée.

### Écarts au plan
- `CheckOpenGLError` écrivait dans `System.out` : son ERROR va dans `System.err`.
- Correspondance du débogage GL : fonction `(type, sévérité)`, la source ne change pas le niveau.
- Un élément GL en échec est un ERROR en mode strict (`GLResources.setStrict`), un WARNING sinon : sans cela, un shader qui ne compile pas passait les tests stricts.
- Une fabrique de service qui échoue avant son repli (pas de périphérique audio) donne un WARNING.
- Deux tests de sources au lieu d'un (`NoDirectStderrTest` pour la bibliothèque, `DemoNoDirectStderrTest` pour la démo) : chaque module lit ses propres sources.
- Une annonce couvre tous les problèmes qu'elle reconnaît (pas un seul, comme `LogAssert.Expect`) : une erreur répétée à chaque frame s'annonce une fois.
- Un moteur gardé par `@MightySceneTest` ne contrôle pas les problèmes de son arrêt (inscrit dans `later.md`).
- Ajouts au kit vus pendant la fumée : un test qui a levé une exception ferme le moteur gardé (sinon fuite négative au test suivant) ; l'échec après un test commence par `After <test>:` ; une ligne répétée est comptée `(xN)`.
- Aucune scène n'a eu besoin d'arguments de taille : la fumée dure environ 9 s en local, 26 s en CI (42 s avec le redémarrage du moteur après une exception) ; le job complet 2 min 06 (2 min 30 avant).
- `RectangleBatchGLTest` annonce son uniform absent voulu.

### Vérifié
- `mvn -o package` (1442 + 84 + 86 tests) vert ; CI vert sur la branche (runs 45917372579 et 45917645645).
- `scenario.py` : `Scene2D` et `FallbackScene`, journal identique, `problems` = 0 ERROR et 6 WARNING `resources` dans `FallbackScene`.
- `python tools/analyse.py` : rien de nouveau (reste `IM_BAD_CHECK_FOR_ODD` dans `BatchStress2D.createSlime`, antérieur, pour la revue hebdomadaire).
- Paquet absent : `Scene3D` ignorée, ou en échec avec `requireDemoAssets`.

### À vérifier à la main par l'utilisateur
- Journal de la console identique dans `FallbackScene` et `Scene2D`, lignes `--` en rouge comme avant.
- `problems` dans la console (touche à gauche de 1) après `FallbackScene`.
- Pas de nouveau message de débogage GL sur votre pilote dans `Scene2D`, `Scene3D` et `BatchStress2D`.
- Durée du CI et résultat de la branche d'acceptation (runs ci-dessus).
