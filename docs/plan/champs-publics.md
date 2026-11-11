# Champs publics modifiables → méthodes

**Statut : validé.**

Décisions prises :
- Règle : **une classe avec de la logique n'expose aucun champ public modifiable** (ni collection, ni drapeau, ni statique non `final`) ; **une structure de données pure** (pas de logique, lue une fois ou à chaque usage sans invariant) **garde ses champs publics**.
- Loaders : liste **donnée par le projet** à la construction de `Resources`, puis **non modifiable**.
- `GUIList.GUIs` / `ShouldLoop` : privés, méthodes.
- Dossier des ressources : `final` et sûr entre threads.
- `network/ClientThread` : hors périmètre.

## Pour démarrer
- Lire : `docs/architecture.md` sections « Démarrage », « Moteur et vue courante » (statiques gardés), « Ressources CPU » ; `docs/later.md` section « Modèle d'extension validé » (point d'injection, registre figé) ; `docs/debug-tools.md` ligne « Erreurs GL » ; `docs/bonnes-pratiques.md` points 7 et 14.
- Branche de sujet depuis `dev` : `champs-publics`.
- Compiler et tester avec les commandes de `CLAUDE.md` à chaque étape ; vérifier en jeu avec `tools/scenario.py` (une fenêtre s'ouvre : prévenir l'utilisateur).

## Contexte : ce qui existe
Champs publics non constants de `mightylib` (hors `network`) et leur sort :

| Champ | Classe | Problème | Sort |
|---|---|---|---|
| `Loaders` (`public final List`) | `resources/Resources` | ajout après `init()` jamais initialisé (`attachTo`/`create` seulement dans `init`), retrait possible du `ShaderLoader`, parcouru par les workers et les commandes (`ResourceArguments`) | liste figée à la construction |
| `FOLDER` (`public static String`) | `resources/Resources` | global, non `volatile`, lu par les workers (14 lectures : loaders, `ResourceWatcher`, `TextureDataLoader.LIST_TEXTURES` statique) ; les tests le réaffectent et le restaurent | `final` d'instance |
| `GUIs` (`public final Map`), `ShouldLoop` | `graphics/GUI/GUIList` | retirer l'élément sélectionné le laisse sélectionné (`selected`/`id` gardés à part) | méthodes |
| `DO_CHECK` (`public static boolean`) | `utils/Logger` | global modifiable, lu aussi hors thread principal (`GenDeleteResources` vérifie le thread appelant) | privé `volatile`, accesseurs |
| `DEFAULT_CATEGORY_NAME` (`public static String`) | `resources/map/TileLayer` | jamais réaffecté | `static final` |
| `Collisions` (`public final Map`) | `physics/collision/CollisionBoundedVolume2D` | collection d'une classe à logique | méthodes |
| `Text`, `OverlapsText` (`public final`) | `graphics/GUI/BackgroundlessButton` | accès public figé (le bouton relit le texte à chaque image : pas d'invariant cassé) | accesseurs `text()` / `overlapsText()` |
| `Text` (`public final`) | `scenes/camera/cameraComponents/DebugInfoCamera2D`, `DebugInfoCamera3D` | idem | accesseur `text()` |
| `ShaderProperty` | `graphics/renderer/_2D/shape/EllipseRenderer` | réaffecté après construction : uniformes ajoutés sous les anciens noms, mis à jour sous les nouveaux | `private final` |
| `windowInfo` (`public final`) | `inputs/KeyboardManager` | aucun usage extérieur | privé |
| `entries` | `inputs/CharInputEntry` | doublon de `addModifier`/`getModification` | `private final` |
| `value` | `utils/math/KeyTreeNode` | doublon de `getValue`/`setValue` | privé |
| `LayerCount`, `Lacunarity`, `Persistence`, `RidgeLayerStart`, `Jiggle` | `algorithms/TerrainGeneration` | réglages d'une classe à logique | setters chaînés + getters |
| `Channel`, `Buffer`, `SampleRate`, `nativeBuffer` | `sounds/SoundLoadInfo` | `Buffer` remplacé à la main sans `nativeBuffer` cohérent → fuite de mémoire native ou `memFree` d'un buffer Java | constructeur + getters + `release()` |

Structures de données pures qui **gardent leurs champs publics** : `sounds/SoundSourceCreationInfo`, `scenes/camera/Camera3DCreationInfo`, `utils/math/ID`, `FreeCamera3DComponent.Inputs`, `MovingCameraComponent.Inputs`, `EllipseRenderer.ShaderEllipseProperty`, `graphics/lighting/materials/BasicMaterial` et `Material` (valeurs envoyées au shader seulement par `addToRenderer`/`updateRenderer`, appel explicite ; 3D secondaire). `main/WindowCreationInfo` est package-private (hors API).

Modèles à réutiliser :
- `ResourceLoader.attachTo(Resources)` / `resources()` (protégé) : les loaders lisent déjà leur `Resources` ; le dossier passe par là.
- `EngineConfig` (setters enchaînables) pour le dossier.
- `IProjectLoading` : `init(Resources)` ne fait qu'ajouter des loaders dans la démo (`mightylibdemo/main/ProjectLoading`). `Engine` (vers ligne 213) crée `Resources` puis (vers ligne 253) appelle `projectLoading.init(resources)` avant `resources.init()`.
- `Resources` a déjà un constructeur package-private avec une liste de loaders (tests).
- `Resources.addCategoryAndLoader` : **aucun appel** → supprimé.

Tests existants touchés : `GUIListTest` (`GUIs.put`, `ShouldLoop`), `ResourcesTestBase`, `FileWatchTest`, `ResourceSelectionTest` (réaffectent `Resources.FOLDER`), autres tests de `resources/` (constructeur).

## Décisions
- **Loaders** : `IProjectLoading.init(Resources)` est remplacé par `default List<ResourceLoader> loaders() { return List.of(); }`, lu par `Engine` **avant** de créer `Resources`. `Resources` garde `private final List<ResourceLoader> loaders = List.copyOf(obligatoires + projet)`, dans cet ordre ; lecture interne par accesseur package-private (`ResourceArguments`). Deux loaders du même type de donnée → `IllegalArgumentException` à la construction (si le loader expose son type ; sinon, rien). Pourquoi : c'est le « registre figé » prévu par le point d'injection de `later.md`, qui l'absorbera (les mods fourniront leurs loaders à la même phase).
- **Dossier des ressources** : `private final String folder` dans `Resources` (normalisé avec `/` final), donné au constructeur ; `EngineConfig.setResourcesFolder(String)`, défaut `"resources/"` ; lu par `resources.folder()`. `final` + publication par le constructeur = sûr entre threads. Une constante `static final` est impossible : les tests changent de dossier. Plusieurs racines (moteur, jeu, mods) : plus tard (`later.md`, « Ressources »).
- **`GUIList`** : `put(int id, GUI gui)`, `remove(int id)` (désélectionne si c'était l'élément sélectionné), `get(int id)`, `ids()` (vue non modifiable) ; `setLooping(boolean)` / `isLooping()`.
- **`Logger`** : `private static volatile boolean glErrorCheck`, `Logger.isGLErrorCheck()`, `Logger.setGLErrorCheck(boolean)` (appelé par `Engine`). Le coût d'une lecture `volatile` est négligeable sur x86 ; la convention de `CLAUDE.md` devient `if (Logger.isGLErrorCheck())`.
- **Accesseurs** : nommage du projet (`getX`/`setX`/`isX`, ou `text()` comme `watcher()` quand il s'agit d'un sous-objet), setters chaînés qui renvoient `this` pour les réglages.
- **Test de garde** : un test JUnit échoue si une classe de `mightylib` (hors `network`) a un champ public non `final`, un champ public statique non `final`, ou un champ `public final` de type `Collection`/`Map`, sauf liste blanche écrite dans le test (les structures pures ci-dessus). La règle est ajoutée à `bonnes-pratiques.md` avec ce test comme vérification.

## Étapes
1. **Loaders figés** — `main/procedures/IProjectLoading`, `main/Engine`, `resources/Resources` (constructeurs, `init`, toutes les boucles sur `Loaders`, suppression d'`addCategoryAndLoader`), `resources/ResourceArguments`, `mightylibdemo/main/ProjectLoading`, tests de `resources/`. Tests : la liste n'est pas modifiable ; obligatoires puis projet dans l'ordre ; doublon de type refusé. En jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "resources" "input ENTER"` (les types du projet sont présents : tilemap, son, modèles) puis `"scene Scene3D"`.
2. **Dossier des ressources d'instance** — `Resources` (constructeur, `folder()`), `EngineConfig.setResourcesFolder`, tous les lecteurs de `Resources.FOLDER` (loaders dont `TextureDataLoader.LIST_TEXTURES` calculé dans `create`, `WaveCollapseRulesLoader`, `SoundLoader.gainTreePath`, `ResourceWatcher`), `SoundManager` s'il en dépend, tests (dossier temporaire passé au constructeur, plus de sauvegarde ni de restauration du statique). Test : deux `Resources` sur deux dossiers différents ne se voient pas. En jeu : `scenario.py` comme à l'étape 1, plus un rechargement à chaud (`docs/debug-tools.md`).
3. **`GUIList`** — méthodes ci-dessus ; `MenuScene` ; `GUIListTest` adapté. Nouveau test : retirer l'élément sélectionné le désélectionne (et il ne peut plus être activé). En jeu : `MenuScene`, navigation au clavier sans boucle (`ShouldLoop = false` aujourd'hui).
4. **Statiques** — `Logger` (accesseurs, 11 lectures dans la lib, `Engine`), `TileLayer.DEFAULT_CATEGORY_NAME` en `static final`. En jeu : `EngineConfig.setOpenGLErrorCheck(true)` toujours effectif (une scène 2D, pas d'erreur).
5. **Classes à logique** — `CollisionBoundedVolume2D` (`put`/`remove`/`get`, démo `CollisionSystem`), `BackgroundlessButton` (`text()`/`overlapsText()`, démo `MenuScene`), `DebugInfoCamera2D`/`3D` (`text()`), `EllipseRenderer`, `KeyboardManager`, `CharInputEntry`, `KeyTreeNode`, `TerrainGeneration`, `SoundLoadInfo` (et ses décodeurs). Migration mécanique ; en jeu : `MenuScene`, `CollisionSystem`, `TerrainGeneration3D`, une scène avec du son.
6. **Test de garde + docs** — test de réflexion (classes listées depuis `target/classes`, ou par un scan du classpath du package) avec liste blanche ; puis les documents ci-dessous.

Les étapes 1 et 2 peuvent être fusionnées seules (tout `Resources`) ; 3 à 5 sont indépendantes entre elles ; 6 vient en dernier.

## Démo
Pas de nouvelle scène : changement d'API sans nouvelle fonctionnalité. Adapter `ProjectLoading`, `MenuScene`, `CollisionSystem`.

## Documents à mettre à jour à la fin
- `architecture.md` : « Ressources CPU » (loaders donnés par `IProjectLoading.loaders()`, liste figée, dossier d'instance), « Moteur et vue courante » (`Logger.DO_CHECK` → `Logger.isGLErrorCheck()` dans la liste des statiques gardés), « Threads » (ligne `Logger.DO_CHECK`).
- `debug-tools.md` : ligne « Erreurs GL ».
- `decisions.md` : la règle classe à logique / structure de données pure.
- `bonnes-pratiques.md` : nouveau point « aucun champ public modifiable hors structures de données pures », vérification = le test de garde.
- `later.md` : point d'injection (il absorbe `IProjectLoading.loaders()`), plusieurs racines de ressources (dossier d'instance prêt) ; noter les bugs relevés hors périmètre (ci-dessous).
- `CLAUDE.md` : convention `if (Logger.isGLErrorCheck())`, test de garde dans la liste des tests couverts.
- `current.md` : supprimer la ligne « Champs publics modifiables ».
- Supprimer ce plan.

## Hors périmètre
- `network/` (dont `ClientThread`, `NetworkConfigurationLoader` qui lit `"resources/networkConfigs"` en dur, sans passer par le dossier des ressources) : à noter dans `later.md`.
- `BasicMaterial.clone()` / `Material.clone()` partagent les `Vector3f` (copie superficielle) : bug 3D à noter dans `later.md`, pas corrigé ici.
- Plusieurs racines de ressources, classement des packages en API promise / expérimentale (`later.md`, « API publique »).
- Champs `protected` (contrat d'héritage) : traités avec le modèle d'extension (`later.md`).

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1–2 : **Claude Opus 5.5, effort high** (modèle de `Resources`, ordre de démarrage, workers).
- Discussion A, étapes 3–6 : **Claude Sonnet 5.5, effort medium** (migration mécanique, test de réflexion, docs).
- Discussion B (`/pr-ml`) : **Claude Opus 5.5, effort high** (diff large réparti dans tout `mightylib`, à relire au regard de la règle).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/champs-publics.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `champs-publics`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`MenuScene` : navigation clavier et souris, boutons ; `CollisionSystem` ; `TerrainGeneration3D` ; une scène avec du son ; rechargement à chaud d'une texture). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml champs-publics`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
**Fait** (un commit par étape) :
1. Loaders figés : `IProjectLoading.loaders()` lu par `Engine` avant de créer `Resources` ; `List.copyOf(obligatoires + projet)`, doublon de type → `IllegalArgumentException` ; `addCategoryAndLoader` supprimé ; `ResourceArguments` lit `loaders()`.
2. Dossier d'instance : `Resources.folder()` (`final`, normalisé), `EngineConfig.setResourcesFolder`, `Resources.DEFAULT_FOLDER` ; loaders, `ResourceWatcher`, `TextureDataLoader` (liste calculée dans `create`), arbre des gains (chemin complet passé par `SoundManager`), `FallbackScene` ; tests sans statique sauvegardé/restauré.
3. `GUIList` : `put`/`remove`/`get`/`ids`, `setLooping`/`isLooping`.
4. `Logger.isGLErrorCheck()`/`setGLErrorCheck` (`volatile`) ; `TileLayer.DEFAULT_CATEGORY_NAME` `final`.
5. `CollisionBoundedVolume2D` (`put`/`remove`/`get`/`names`), `BackgroundlessButton` (`text()`/`overlapsText()`), `DebugInfoCamera2D`/`3D` (`text()`), `EllipseRenderer` (noms des uniformes copiés, `private final`), `KeyboardManager`, `CharInputEntry`, `KeyTreeNode`, `TerrainGeneration` (setters chaînés + getters), `SoundLoadInfo` (constructeur + getters + `release()`).
6. `PublicFieldsTest` (réflexion sur les classes compilées, sans initialisation de classe ; seules les classes publiques et leurs englobantes publiques ; liste blanche des structures pures, vérifiée elle aussi) ; documents.

**Écarts au plan** :
- Étape 2 (choix de l'utilisateur) : `FontLoader`/`FontFace`, `IconLoader`, `TextureDataAtlasLoader` et les shaders (`ShaderManager(String dossier)`, `versionFolder()` ; `SHADER_INFO_PATH` supprimé) passent aussi par le dossier, ils écrivaient `"resources/"` en dur. `GamepadManager.MAPPINGS_FILE` reste en dur (noté dans `later.md`). `Resources.NormalizeFolder` est public (utilisé par `ShaderManager`).
- Étape 3 : `put` sur l'id sélectionné reporte la sélection au clavier sur le nouvel élément ; `remove` de l'élément sélectionné annule aussi son `forceSelect`.
- Étape 6 : le test de garde a trouvé `WaveCollapseRules.Rule.INITIAL_POS` et `MightyMath.PI_FLOAT`, jamais réaffectés → `static final` (le `Vector2i` reste mutable : noté dans `later.md`).
- Relevé hors sujet, noté dans `later.md` : `wait-loaded` de `scenario.py` bloqué après un changement de scène lancé par le menu (reproduit sur `dev`).

**Vérifié** : `mvn -o -q test` à chaque étape (nouveaux tests `ResourceLoadersTest`, `ResourcesFolderTest`, `GUIListTest` étendu, `SoundDecodingTest` adapté, `PublicFieldsTest`) ; en jeu par `scenario.py` : types du projet présents (`?reload WaveCollapseRules`, `?resources`), `Scene2D`, `Scene3D`, `FallbackScene` (erreurs attendues seulement), `reload all`, rechargement à chaud d'une texture modifiée (`watch TextureData` + fichier touché → 1 rechargement), menu au clavier sans boucle (haut ×2 → `Scene2D`, bas ×5 → Quitter), `CollisionSystem`, `TerrainGeneration3D`, `SoundFormatsScene`, vérification GL activée temporairement (`setOpenGLErrorCheck(true)`) sans erreur.

**À vérifier à la main** :
- `MenuScene` : navigation clavier et souris, survol des boutons (texte « ->x<- »), clic et Entrée, liste défilante de droite.
- `CollisionSystem` : le rectangle du volume englobant suit et entre en collision comme avant.
- `TerrainGeneration3D` : terrain identique.
- Une scène avec du son (`SoundFormatsScene`, `Scene2D`) : sons audibles, gains inchangés.
- Rechargement à chaud d'une texture à l'œil (`watch TextureData`, modifier une image).
