# Plan — Héritage et API publique

**Statut** : validé.

Décisions prises :
1. Une classe publique concrète non choisie comme extensible devient `final`.
2. Statut de stabilité marqué par l'annotation `@API` d'apiguardian ; un test JUnit vérifie que tout package est classé et que la démo n'utilise rien d'interne.
3. Les champs `protected` de `Scene` deviennent des méthodes d'accès.
4. Portée : classer chaque package et traiter en entier les classes extensibles ou injectables ; la démo en module Maven séparé est un plan à part.
5. Classes extensibles, en quatre groupes : scènes et vues (`Scene`, `WindowView`, `TemplateSceneLoading`) ; rendu (`Mesh`, `Renderer`, `MeshRenderer`, `Shape2DRenderer`) ; ressources (`DataType`, `SingleSourceDataType`, `MultiSourceDataType`, `ResourceLoader`, `TextureData`, `GLElement`, `Texture`) ; commandes et GUI (`BaseCommand`, `GUI`, `Slider`, `ValueSelector`). Les interfaces publiques le sont d'office.
6. `STABLE` = promis pour la 1.0 : le classement sert de carte dès maintenant, les changements cassants restent permis avant la 1.0.
7. 3D, réseau et physique JBox2D : `EXPERIMENTAL`.
8. Statut posé par un `package-info.java` par package ; un type ou un membre ne s'annote que s'il est moins stable que son package.
9. Autres classes abstraites : hiérarchie fermée (constructeur package-private), sauf `KeyboardLanguage` et `Tweening`, ouvertes aux projets (`EXPERIMENTAL`), et `Box2DActor`, ouverte (la démo l'étend) et `EXPERIMENTAL`.
10. Javadoc courte (anglais) sur chaque point d'accroche des classes extensibles : quand la méthode est appelée, ce qu'elle peut faire, ce qu'elle doit appeler. Exception à « peu de Javadoc », limitée aux accroches.
11. Test de garde : toute classe publique concrète est `final`, sauf celles de la liste des classes extensibles (liste unique, partagée avec `ExtensionFieldsTest`).
12. Changements mécaniques (`final`, `package-info`, renommages dans la démo) par un script Python local dans `tools/scripts/` (non versionné, garde LF et UTF-8), diff relu et compilé : exception à « pas de `sed` sur les sources » pour ce seul chantier.

## Pour démarrer
- Lire : `docs/current.md` (objectif « Héritage et API publique »), `docs/decisions.md` (philosophie, champs publics, point d'injection), `docs/bonnes-pratiques.md` (points 7, 14 et 15), `docs/architecture.md` (scènes, rendu, ressources, son), `licence.md`.
- Branche de sujet depuis `dev` : `heritage-api-publique`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `mightylib` au 2026-11-15 : 417 fichiers, 388 types publics, 94 `final`, 37 abstraits, 34 interfaces ; `protected` 266 fois dans 98 fichiers (dont 25 `syntax(CommandNode)` des commandes générales, 28 dans `graphics/GUI`, 22 dans `Scene`).
- Rien n'est choisi comme extensible : tout est ouvert par défaut. Des classes doivent rester publiques sans être une API (utilisées d'un package à l'autre) : package-private ne suffit pas.
- `ExtensionFieldsTest` (`test/io/github/mightycode/mightylib/ExtensionFieldsTest.java`) interdit les champs `protected` de `Mesh`, `Renderer`, `MeshRenderer`, `Shape2DRenderer` (liste `EXTENSIBLE_CLASSES`) ; `PublicFieldsTest` garde les champs publics de toute la bibliothèque.
- `Scene` (`scenes/Scene.java`) : champs `protected` `resources`, `glResources`, `mainContext` (final), `main3DCamera`, `main2DCamera`, `sceneManagerInterface`, `args` (affectés seulement par `Scene`) ; la démo les lit (au 2026-11-15 : `mainContext` 141 fois, `resources` 77, `args` 46, `main3DCamera` 45, `main2DCamera` 39, `sceneManagerInterface` 30, `glResources` 4) sans jamais les affecter. `Scene` a déjà `engine()`. Méthodes `protected` : `openWindow`, `addCommand`, `createLoadingScene`, `createVirtualSceneBindable`, `setVirtualScene`, `setAndDisplayRealScene`, `clear`, `setClearColor`, `getInvolvedBatch`.
- Ce que la démo étend : `Scene` (28), `WindowView`, `TemplateSceneLoading`, `MeshRenderer`, `Texture`, `TextureData`, `MultiSourceDataType` (`LayeredTextureData`), `ResourceLoader`, `BaseCommand`, `Box2DActor` ; elle implémente `IPlugin`, `IEngineService`, `IView`, `IUpdatableDisplayable`. Elle importe 142 types de 47 packages.
- Abstraites qui ne sont que des utilitaires statiques : `GraphUtil`, `RendererUtils`, `GenDeleteResources`, `FileMethods`, `TextFormat`, `MightyMath`, `JSONObjectUtil`, `ListGeneralCommand`, `ListError`, `Direction`, `HeightMapColorFunctions`, `HeightMapHeightMappers`, `TextureParameters`.
- Autres abstraites hors des groupes : `Camera`, `Tweening`, `Collision2D`, `CollisionVisualisation`, `KeyboardLanguage`, `AxisBinding`, `ListNode`, `PositionListNode`, `CommunicationThread`, `ServerThread`.
- Bugs connus dans des classes qui deviennent un contrat (corrigés dans l'étape de leur groupe, retirés alors de `later.md`) : `Renderer.updateShaderValue` (NPE sur une valeur absente, `Renderer.java:233`) ; `MeshRenderer.addTextureChannel(nom, uniform, canal)` sans effet tant que l'autre surcharge n'a pas créé le tableau ; `Shape2DRenderer.setTexturePosition` ne marque pas la texture à renvoyer ; `ResourceLoader.IGNORE_FOLDERS` est un tableau `protected static` modifiable et l'exploration des sous-dossiers ignore la liste reçue (`ResourceLoader.java:105`). Hors groupes, mais défauts d'API : `WaveCollapseRules.Rule.INITIAL_POS` (`Vector2i` public partagé) ; `BasicMaterial.clone()`/`Material.clone()` (copie superficielle).
- apiguardian 1.1.2 est déjà dans le dépôt Maven local (dépendance de JUnit) : `@API` vise `TYPE`, `METHOD`, `CONSTRUCTOR`, `FIELD`, `PACKAGE`, rétention `RUNTIME` (lisible par réflexion, `package-info` compris).
- Références : *Effective Java*, item 19 (concevoir et documenter pour l'héritage, sinon l'interdire) ; apiguardian (`STABLE`, `MAINTAINED`, `EXPERIMENTAL`, `DEPRECATED`, `INTERNAL`, utilisé par JUnit 5 ; un statut sur un type vaut pour ses membres publics, un membre peut déclarer un statut moins stable) ; libGDX (tout ouvert, visibilités changées au fil des versions) ; japicmp (compatibilité binaire entre versions publiées, plus tard).

## Décisions
- `final` par défaut : retirer un `final` plus tard ne casse rien, l'ajouter casse les sous-classes des autres projets.
- apiguardian (`org.apiguardian:apiguardian-api:1.1.2`, Apache 2.0) en dépendance `compile` : nom connu des développeurs Java ; ajouté à `licence.md`.
- Statuts utilisés : `STABLE`, `EXPERIMENTAL`, `INTERNAL` (pas de `MAINTAINED` ; `DEPRECATED` seulement pour une API remplacée, aucune aujourd'hui). Pas de `since` avant la première version.
- `INTERNAL` : type public seulement parce qu'un autre package de la bibliothèque s'en sert ; un projet ne doit pas l'utiliser. Un package `INTERNAL` entier quand aucun de ses types n'est destiné aux projets.
- Classement des packages :
  - `STABLE` : `main`, `main.utils`, `main.monitors`, `scenes`, `scenes.camera`, `scenes.camera.cameraComponents`, `registry`, `services`, `graphics`, `graphics.renderer`, `graphics.renderer._2D`, `graphics.renderer._2D.shape`, `graphics.renderer.utils`, `graphics.shader`, `graphics.surface`, `graphics.text`, `graphics.GUI`, `graphics.game`, `graphics.utils`, `inputs`, `inputs.inputType`, `inputs.text`, `resources`, `resources.texture`, `resources.animation`, `resources.map`, `resources.data`, `resources.sound`, `sounds`, `utils`, `utils.json`, `utils.math`, `utils.math.color`, `utils.math.geometry`, `utils.enginecommand`, `utils.enginecommand.syntax`, `physics.tweenings`, `physics.tweenings.type`.
  - `EXPERIMENTAL` : 3D (`graphics.lighting`, `graphics.lighting.lights`, `graphics.lighting.materials`, `graphics.renderer._3D` et sous-packages, `resources.models`, `resources.cubemap`), `network`, `physics`, `physics.collision`, `algorithms` et sous-packages, `graphics.debug`, `utils.valueDebug`, `inputs.keyboardlanguage`, `resources.sound.decoder` (`SoundDecoder` : point d'extension des formats), `console` (un jeu peut remplacer le service `IConsoleService` ; composants internes en `INTERNAL`).
  - `INTERNAL` : `utils.enginecommand.general` (commandes du moteur), `physics.tweenings.tweeningmath`.
  - Exceptions `INTERNAL` dans les packages `STABLE` (candidates, à confirmer type par type à l'étape 2 avec le critère ci-dessus) : `main` (`Context`, `ContextManager`, `MainLoop`, `NativeWindowOwner`, `WindowTree`, `WindowPlatform`, `SystemInfo`), `graphics` (`GLContext`, `IGLSharedState`), `inputs` (`GlfwGamepadSource`, `InputSimulation`), `resources` (`BatchLoader`, `BatchResources`, `IGLElementSink`, `FileChangeDetector`, `ResourceWatcher`, `ResourceSelection`, `ResourceArguments` s'ils sont publics), `sounds` (`GainTree`), `console` (`ConsoleLog`, `ConsoleInput`, `ConsoleLayout`, `ConsolePanel`, `LogCapture`, `LogSelection`, `LogPosition`, `CompletionListing`, `CommandEcho`).
- Accesseurs de `Scene` (champs devenus `private`) : `resources()`, `glResources()`, `mainContext()`, `camera2D()`, `camera3D()`, `sceneManager()` (l'interface de changement de scène), `args()` (copie du tableau). Pas de setter : aucun usage.
- Une liste unique des classes extensibles (`ExtensibleClasses`, dans `test/io/github/mightycode/mightylib/`) : les quatre groupes, plus `KeyboardLanguage`, `Tweening`, `Box2DActor` (`EXPERIMENTAL`). Lue par `ExtensionFieldsTest` (aucun champ `protected`) et par le nouveau test de garde `final`.
- La démo n'est pas `INTERNAL`-libre par une simple convention : `ApiStatusTest` lit ses sources (imports, y compris statiques et `*`, et noms qualifiés) et échoue sur tout type ou package `INTERNAL`. `EXPERIMENTAL` est permis.
- La démo en module Maven séparé reste l'objectif suivant de `current.md`, avec son propre plan.

## Étapes
Un commit par étape. Après chaque étape : `mvn -o -q test`, puis `python tools/scenario.py "scene MenuScene" wait-loaded "scene Scene2D" wait-loaded "scene Scene3D" wait-loaded "?gl summary"` et un `leakcheck` sur une scène du groupe touché.

1. **Ce plan**, tel quel (premier commit de la branche).
2. **Statuts et test** : dépendance apiguardian dans `pom.xml` (version fixée, `licence.md` : licence, obligations) ; un `package-info.java` par package de `mightylib` avec `@API(status = …)` selon le classement ; annotations `INTERNAL` des types listés en exception (vérifier chacun : utilisé par un projet ? sinon `INTERNAL`) ; `ApiStatusTest` (tout package de `mightylib` contenant un type a un `package-info` annoté ; un type annoté n'est jamais plus stable que son package ; la démo n'utilise aucun type ou package `INTERNAL`). Si la démo utilise un type candidat à `INTERNAL`, s'arrêter et demander (le type est peut-être une API). Script local pour les `package-info`. Fusionnable seul.
3. **Scènes et vues** : champs de `Scene` en `private` + accesseurs (décision ci-dessus), migration de la démo (script local pour les renommages, puis compilation) ; revue des méthodes `protected`/redéfinissables de `Scene`, `WindowView`, `TemplateSceneLoading` (ce qui n'est pas une accroche devient `private` ou `final`) ; Javadoc des accroches ; ajout à `ExtensibleClasses` (le test `ExtensionFieldsTest` passe la liste unique). Vérification en jeu : toutes les scènes de la démo s'ouvrent (`scene` sur chacune, ou `leakcheck` sur un échantillon) et `LoadingSceneImplementation` s'affiche.
4. **Rendu** : `Mesh`, `Renderer`, `MeshRenderer`, `Shape2DRenderer` : accroches choisies, le reste `private`/`final`, Javadoc ; corrections : `updateShaderValue` sur une valeur absente (exception claire nommant la valeur, ou ajout : trancher en lisant les appelants), `addTextureChannel(nom, uniform, canal)` qui crée le tableau, `setTexturePosition` qui marque la texture ; tests sans GL quand la logique le permet (sinon vérification en jeu : `CustomRenderer2D`, `Scene2D`). Retirer ces bugs de `later.md`.
5. **Ressources** : `DataType`, `SingleSourceDataType`, `MultiSourceDataType`, `ResourceLoader`, `TextureData`, `GLElement`, `Texture` : accroches, Javadoc ; `IGNORE_FOLDERS` `private` (copie défensive ou liste immuable) et sous-dossiers explorés avec la liste reçue, test sur un dossier temporaire. Vérification : `LayeredTextureData` de la démo, `resources`, rechargement à chaud.
6. **Commandes, GUI et ouvertures expérimentales** : `BaseCommand` (`syntax` = l'accroche), `GUI` (`referenceCamera` → méthode), `Slider`, `ValueSelector` ; `KeyboardLanguage`, `Tweening`, `Box2DActor` : accroches documentées, annotées `EXPERIMENTAL` si leur package est `STABLE`. Vérification : `GUIWidgetsScene`, `MenuScene`, commandes de la console.
7. **Hiérarchies fermées et utilitaires** : constructeurs package-private des abstraites listées en décision 9 (sauf ouvertes) ; utilitaires statiques en `final` avec constructeur privé.
8. **`final` partout ailleurs et garde** : script local qui ajoute `final` à toute classe publique concrète hors `ExtensibleClasses` ; adapter les tests qui dérivaient d'une classe devenue `final` ; test de garde (dans `ExtensionFieldsTest` ou à côté) ; corriger `WaveCollapseRules.Rule.INITIAL_POS` (copie ou accesseur) et `Material.clone()`/`BasicMaterial.clone()` (copie des `Vector3f`, test). Retirer ces bugs de `later.md`. Vérification en jeu : les scènes 3D (`Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`) et la WFC.
9. **Documents** (liste ci-dessous) et section « Bilan » de ce plan.

Les étapes 2 à 6 sont fusionnables seules ; 7 et 8 supposent les groupes traités (sinon une classe extensible devient `final`).

## Démo
Pas de nouvelle scène : la démo entière est le test (elle compile, n'utilise rien d'`INTERNAL`, toutes ses scènes s'ouvrent). Ses sous-classes (`StarRenderer`, `LayeredTextureData`, `LayeredTextureLoader`, `LoadingSceneImplementation`, commandes, `PlayerManager`) servent d'exemples des accroches documentées.

## Documents à mettre à jour à la fin
- `architecture.md` : section « API publique » au présent : statuts et leur sens, `package-info`, liste des classes extensibles et de leurs accroches, accesseurs de `Scene`, tests de garde.
- `decisions.md` : `final` par défaut et classes extensibles choisies ; apiguardian et `package-info` ; `STABLE` = promis pour la 1.0 ; Javadoc des accroches.
- `bonnes-pratiques.md`, point 7 : la vérification devient `ApiStatusTest` + garde `final` ; point 14 ou nouveau : Javadoc des accroches.
- `CLAUDE.md` : tests couverts (`ApiStatusTest`, garde `final`, `ExtensibleClasses`, tests ajoutés aux étapes 4, 5, 8) ; conventions : nouvelle classe publique `final` sauf extensible déclarée, nouveau package → `package-info` avec statut, Javadoc des accroches (exception à « peu de Javadoc »).
- `later.md` : bugs corrigés retirés ; Manuel : la Javadoc des accroches existe ; Bibliothèque réutilisable : japicmp à la première version, JPMS.
- `current.md` : supprimer l'objectif « Héritage et API publique ».
- `licence.md` : apiguardian (étape 2).
- Supprimer ce plan (discussion B).

## Hors périmètre
- Démo en module Maven séparé (objectif suivant de `current.md`).
- Vérification de compatibilité binaire (japicmp) : à la première version publiée (`later.md`, Bibliothèque réutilisable).
- Modules Java (JPMS, `module-info`) : avec la bibliothèque réutilisable (`later.md`).
- Remplacement de JBox2D (`later.md`, Physique 2D) ; la 3D n'est ni étendue ni refactorée (seulement classée et verrouillée).
- Ressources lues depuis le classpath (`later.md`, Bibliothèque réutilisable).

## Questions restantes
- Aucune de conception ; à l'étape 2, chaque exception `INTERNAL` candidate est confirmée par son usage (une utilisée par la démo → demander).

## Modèles recommandés
- Discussion A, étapes 1 à 6 : Claude Opus 5.5, effort `high` (choix des accroches, contrat durable des classes extensibles, `Scene` touchée par 28 scènes).
- Discussion A, étapes 7 à 9 : Claude Sonnet 5.5, effort `medium` (mécanique par script, docs).
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (gros diff à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/heritage-api-publique.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `heritage-api-publique`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (toutes les scènes du menu s'ouvrent et se ferment ; `CustomRenderer2D` dessine l'étoile ; `GUIWidgetsScene` : curseurs et sélecteurs ; `FallbackScene` : texture et son de repli ; scènes 3D intactes). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml heritage-api-publique`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
