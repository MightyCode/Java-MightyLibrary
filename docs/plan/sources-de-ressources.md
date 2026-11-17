# Sources de ressources et plusieurs racines

**Statut : validé.**

Décisions prises :
- Pile de sources (`IResourceSource`, `FolderSource`, `ClasspathSource`) sous une pile `ResourceSources`, lue par `resources.sources()` ; `resources.folder()` disparaît.
- Chemins **logiques** (`textures/gui/a.png`) dans les données, résolus à la lecture dans la pile.
- Ordre (du moins au plus prioritaire) : source classpath du moteur < dossier du jeu (`EngineConfig.setResourcesFolder`) < sources des plugins, dans l'ordre d'`addPlugin`.
- Fichiers ordinaires (images, sons, GLSL…) : la racine la plus haute remplace le fichier en entier. Fichiers-listes (`textures.json`, `fonts.json`, `shaders.json`, descriptions de batchs) : **fusion par entrée**, la racine la plus haute gagne pour une entrée de même nom.
- Même nom de donnée, chemins différents dans deux racines : la racine la plus haute gagne, une ligne d'avertissement (exception en mode strict).
- Source classpath : **préfixe unique** (un seul jar le contient), listée en ouvrant ce jar comme `FileSystem` zip (ou le dossier, dans l'IDE). Pas d'étape de build.
- Rechargement à chaud : seules les sources disque sont observées ; une propriété remplace la source classpath du moteur par un dossier (édition à chaud des ressources du moteur depuis le dépôt).
- Modèles 3D (Assimp lit par chemin) : depuis une source disque seulement, sinon `ResourceException` claire.
- Le plan englobe l'objectif « Lectures en dur hors de `Resources` » (mappings de manettes, configurations réseau).
- Préfixe d'une source classpath : `assets/<espace du plugin>/` (`assets/mightylib/`, `assets/demo/`).
- Démo : source classpath de `DemoPlugin` + scène `ResourceSourcesScene2D`.
- Commande `resources sources [chemin]`, niveau `DEV`.

## Pour démarrer
- Lire : `docs/architecture.md` (« Ressources CPU », « Plugins et registre », « API publique »), `docs/debug-tools.md` (section Commandes, commande `resources`, observateur), `docs/decisions.md` (placeholders, propriétaire unique, plugins).
- Branche de sujet depuis `dev` : `feature/resource-sources`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `resources/Resources` : dossier unique sur disque, `final`, donné au constructeur (`Resources.NormalizeFolder`, `folder()`), construit dans `main/Engine.setup` (`new Resources(…, config.getResourcesFolder()…)`, l. ~280). Deux `Resources` sur deux dossiers ne se voient pas (`ResourcesFolderTest`).
- Lectures qui concatènent `resources().folder()` et lisent le disque directement :
  - exploration : `ResourceLoader.exploreResourcesFile` (`java.io.File`, récursif, dossiers ignorés), appelée par `JSONLoader`, `CSVLoader`, `TileMapLoader`, `TileSetLoader`, `AnimationDataLoader`, `WaveCollapseRulesLoader`, `BatchLoader`, `SoundLoader`, `IconLoader`, `TextureDataAtlasLoader`, `ModelLoader`, `network/NetworkConfigurationLoader` (sur `resources/networkConfigs`, **hors du dossier**) ;
  - listes : `TextureDataLoader` (`textures.json`, `discover` par `Files.walk` de `textures/`), `FontLoader` (`textures/fonts/fonts.json`), `ShaderManager` (`shaders/<version>/shaders.json`, reçoit le dossier à sa construction, créé avec la fenêtre) ;
  - binaires : `TextureData` (`FileInputStream`, `loadBufferedImage`), `TextureDataLoader` (`FileChannel`, l. ~187), `CubeMapLoader` (`listFiles` des dossiers de `cubemaps/`, `FileChannel`), `IconLoader` (`FileInputStream`), décodeurs de son (`FlacDecoder` `readAllBytes`, `Mp3Decoder` `newInputStream`, `WavDecoder` `AudioSystem.getAudioInputStream(File)`), `SoundDataType.ioResourceToByteBuffer` (disque, puis repli ad hoc sur `getResourceAsStream`) ;
  - texte : `FileMethods.readFileAsString` / `readJSONObject` ;
  - shaders : `ShaderLoader.readIfFile` (`new File(path).isFile()`), injecté dans `ShaderPreprocessor` (déjà une fonction de lecture : point d'accroche à réutiliser) ;
  - modèles : `ModelLoader` (Assimp par chemin, textures du `.mtl` résolues par `new File(folder, file)`) ;
  - divers : `EnginePlugin.GainTreeFile` (arbre des gains du son), `DataType.estimatedCpuBytes` (`File.length`), démo : `LayeredTextureLoader`, `FallbackScene`.
- `GamepadManager.MAPPINGS_FILE = "resources/gamecontrollerdb.txt"`, lu par `ContextManager` (l. ~190, `Paths.get`) **hors du dossier** ; le fichier n'existe pas dans le dépôt (facultatif).
- Observateur (`ResourceWatcher`, `FileChangeDetector`) : chemins absolus tirés de `DataType.sourcePaths()` ; dossiers suivis par `Files.walkFileTree(resources.folder())` ; découverte par `ResourceLoader.discover`. Fichiers manquants déjà suivis (shaders : includes absents).
- Données : `SingleSourceDataType.path()`, `MultiSourceDataType` (chemins fixes), `DataType.sourcePaths()` ; `ResourceDescriber` montre `sourceFiles`.
- Registre : `EngineRegistry.resources()` (loaders, clé = type de donnée), plugins dans l'ordre d'`addPlugin`, `EnginePlugin` d'abord, gel après `register`.
- Tests de la zone : `Resources*Test` (`ResourcesTestBase`), `ResourcesFolderTest`, `FileWatchTest`, `MultiSourceDataTest`, `CubeMapLoaderTest`, `ResourceSelectionTest`, `ResourceLoadersTest`, `EngineRegistryTest`, `ShaderPreprocessorTest`, tests de `GamepadManager`, `TestFiles` (fichiers de test par le classpath).
- Ailleurs (modèle commun : pile de sources, la plus prioritaire gagne) : PhysFS/LÖVE (chemin de recherche), jMonkeyEngine (`AssetLocator` essayés dans l'ordre), Godot (PCK chargés plus tard remplacent les fichiers), Minecraft (resource packs empilés, certains fichiers fusionnés), Bevy (sources nommées, `embedded://` pour le moteur). Écueil : lister un dossier du classpath dans un jar ne marche pas dans libGDX (un « dossier » peut s'étaler sur plusieurs jars) ; d'où le préfixe unique par source.

## Décisions
1. `IResourceSource` (package `resources/source`, `@API(EXPERIMENTAL)`) : `name()` (debug : `classpath:assets/mightylib/`, `folder:resources/`), `exists(chemin)`, `isFolder(chemin)`, `open(chemin)` → `InputStream`, `size(chemin)`, `list(dossier)` (enfants directs, nom + dossier ou non), `localFile(chemin)` → `Optional<Path>` (vide pour le classpath dans un jar ; sert à l'observateur et à Assimp). Thread-safe, sans état modifiable après construction (lue par les workers et le détecteur).
2. `FolderSource(dossier)` (disque) ; `ClasspathSource(classLoader, préfixe)` : `getResource(préfixe)` → `file:` (dossier, IDE, `target/classes`) ou `jar:` (ouvert en `FileSystem` zip, réutilisé s'il est déjà ouvert, fermé à l'arrêt s'il a été ouvert par la source) ; préfixe absent = source vide (pas d'erreur). Le préfixe est **unique** par source : `assets/<espace du plugin>/` (l'espace est unique dans le registre), donc un seul jar le contient. Les raccourcis `classpath()` du registre le fixent à partir de l'espace du plugin ; un préfixe libre reste possible par `source(new ClasspathSource(…))`.
3. `ResourceSources` : pile **immuable**, construite dans `Engine.setup` après le gel du registre et avant le `ShaderManager` et `Resources` ; `resolve(chemin)` (source la plus haute qui l'a), `exists`, `open`, `readString`, `readBytes`, `readNative` (buffer natif à libérer par l'appelant, pour STB/OpenAL), `size`, `list(dossier)` (union, la plus haute gagne pour un même chemin), `layers(chemin)` (toutes les sources qui l'ont, de la plus basse à la plus haute : fusion), `localFile`, `localCandidates(chemin)` (fichiers disque possibles dans chaque source disque, existants ou non : observateur).
4. Chemin logique : relatif, séparateur `/`, **refusé** s'il est absolu ou contient `..` (une source ne sort jamais de sa racine : mods, commandes). Normalisé une fois (`ResourceSources.normalize`).
5. Construction de la pile : `EnginePlugin` déclare la source classpath du moteur (`assets/mightylib/`, vide tant que l'objectif « Ressources du moteur dans le jar » n'a rien déplacé) ; puis le dossier du jeu (`EngineConfig.setResourcesFolder`, `"resources/"` par défaut) ; puis les sources des plugins, `registry.resources().source(IResourceSource)` et raccourcis `classpath()` (préfixe `assets/<espace du plugin>/`, chargeur de classes du plugin, pour les futurs mods en jar) et `folder(chemin)`, dans l'ordre d'`addPlugin` puis d'appel.
6. Dossier dev : `-Dmightylib.resources.engine=<dossier>` (et `EngineConfig.setEngineResourcesFolder`) remplace la source classpath du moteur par un `FolderSource` (ex. `mightylib/src/main/resources/assets/mightylib`), observable.
7. Données : `path()`/`sourcePaths()` = chemins logiques ; la résolution se fait à chaque lecture (un fichier ajouté plus haut est pris au rechargement suivant). La commande `resources <Type> <nom>` montre, par fichier, la source qui l'a servi (`source` dans `sourceFiles`).
8. Exploration (`exploreResourcesFile`) : par `ResourceSources.list`, chemins logiques ; collision de nom (même type, chemins différents) → la racine la plus haute gagne, une ligne `--… overrides …`, exception en mode strict. Collision dans une même racine : comportement actuel (avertissement).
9. Fichiers-listes fusionnés par entrée (`layers`, de bas en haut, une entrée plus haute remplace) : `textures.json`, `fonts.json`, `shaders.json` (par dossier de version, tant que la refonte des shaders ne l'a pas changé), batchs (deux racines qui décrivent le même batch : listes réunies, sans doublon ; pas de retrait possible, cf. Hors périmètre). Rechargement : un changement dans n'importe quelle couche relit la liste.
10. Observateur : pour chaque chemin logique suivi, `localCandidates` (toutes les sources disque) ; un fichier créé dans une racine plus haute recharge la donnée ; dossiers suivis = ceux de chaque source disque. Une source classpath dans un jar n'est jamais observée.
11. Modèles : `ModelLoader` exige `localFile` ; sinon `ResourceException("model … must be in a folder source")`. AIFileIO noté dans `later.md`.
12. Mappings de manettes : fichier logique facultatif `gamecontrollerdb.txt` (racine de la pile), lu par `ContextManager` via `ResourceSources` (absent → rien, comme aujourd'hui) ; `GamepadManager` reçoit le contenu (texte), plus de chemin. La couche la plus haute gagne (pas de fusion : le format SDL est déjà une base complète).
13. Configurations réseau : `NetworkConfigurationLoader` explore `networkConfigs/` de la pile, comme les autres loaders.
14. Commande `resources sources [chemin]` (`DEV`, syntaxe déclarée) : la pile (ordre, nom, propriétaire : moteur, jeu ou plugin, disque/classpath, observable) ; avec un chemin, ses couches et celle qui gagne. Message lisible, détail dans `data`.

## Étapes
Un commit par étape ; chaque étape compile, passe les tests et laisse le jeu fonctionnel.

1. **Sources et pile** (fusionnable seule). Nouveau package `mightylib/…/resources/source` (`package-info.java`, `@API(EXPERIMENTAL)`) : `IResourceSource`, `FolderSource`, `ClasspathSource`, `ResourceSources` (décisions 1 à 4). Classes `final`. Tests JUnit : `FolderSourceTest` (dossier temporaire : exists, open, size, list, localFile, chemins `..`/absolus refusés), `ClasspathSourceTest` (dossier de `src/test/resources` par le classpath ; jar écrit dans un dossier temporaire et chargé par un `URLClassLoader` : list, open, size, localFile vide, préfixe absent = vide, `FileSystem` déjà ouvert réutilisé), `ResourceSourcesTest` (priorité, union de `list`, ordre de `layers`, `localCandidates`, `readNative`).
2. **Construction et déclaration.** `EngineRegistry.resources().source/classpath/folder` (ordre, gel ; `EngineRegistryTest`), `EnginePlugin` (source du moteur), `EngineConfig` (dossier du jeu inchangé, `setEngineResourcesFolder`, propriété `mightylib.resources.engine`), `Engine.setup` construit la pile et la donne à `Resources` (nouveau constructeur `Resources(méthode, workers, ResourceSources, loaders)`) et au `ShaderManager`. `resources.sources()`. `folder()` reste pour l'instant (= dossier du jeu) pour que les loaders compilent. Commande `resources sources` (décision 14). Tests : `ResourcesFolderTest` → `ResourcesSourcesTest` (deux `Resources` sur deux piles ne se voient pas), dossier dev remplaçant la source du moteur. Vérif. en jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "resources sources" "?gl summary"`.
3. **Exploration et chemins logiques ; loaders texte.** `ResourceLoader.exploreResourcesFile` sur la pile (dossiers ignorés gardés, règle de collision, décision 8), chemins logiques dans les données ; `FileMethods.readFileAsString`/`readJSONObject` remplacés par `ResourceSources.readString` pour les ressources ; portés : JSON, CSV, tilemaps, tilesets, animations, règles WFC, batchs, arbre des gains (`EnginePlugin.GainTreeFile`), `DataType.estimatedCpuBytes` (`size`), `ResourceDescriber` (source par fichier). Tests : `ResourceLoader.exploreResourcesFile` sur deux racines (dossiers ignorés, collision, strict) ; tests existants adaptés (`TestFiles` donne une pile).
4. **Loaders binaires.** Textures (`TextureData`, `TextureDataLoader`, `loadBufferedImage`), atlas, icônes, polices (`FontLoader`, `FontFace`), cubemaps (`CubeMapLoader` : dossiers de `cubemaps/` par `list`), sons (décodeurs sur flux ou octets ; `WavDecoder` avec `BufferedInputStream` ; repli ad hoc de `SoundDataType.ioResourceToByteBuffer` supprimé). Démo : `LayeredTextureLoader`. Tests : `CubeMapLoaderTest` sur deux racines (une face remplacée), `MultiSourceDataTest`, lecture d'un son et d'une texture depuis une `ClasspathSource` de test (sans GL : données lues, pas envoyées). Vérif. en jeu : Scene2D, scène de sons, `TerrainGeneration3D` (cubemap).
5. **Shaders et modèles.** `ShaderManager` lit `shaders.json` par la pile ; `ShaderLoader.readIfFile` → `ResourceSources` (le `ShaderPreprocessor` garde sa fonction de lecture) ; `ModelLoader` exige `localFile` (décision 11), textures du `.mtl` en chemins logiques. Tests : `ShaderPreprocessorTest` avec include remplacé dans une racine plus haute. Vérif. en jeu : `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`, `?gl summary`.
6. **Fusion des listes** (décision 9) : `textures.json`, `fonts.json`, `shaders.json`, batchs. Tests : une entrée ajoutée et une remplacée par une racine plus haute pour chaque liste ; rechargement d'une couche.
7. **Observateur et découverte sur toutes les sources disque** (décision 10) ; `TextureDataLoader.discover` sur la pile ; suppression de `Resources.folder()` et de ses derniers appels (démo : `FallbackScene`). Tests : `FileWatchTest` (fichier créé dans la racine haute → donnée rechargée avec le nouveau contenu ; source classpath non observée).
8. **Mappings de manettes** (décision 12) : `ContextManager`/`GamepadManager` ; `MAPPINGS_FILE` devient un chemin logique. Tests : `GamepadManager` reçoit le texte ; absent → rien.
9. **Configurations réseau** (décision 13) : `NetworkConfigurationLoader` sur `networkConfigs/` de la pile. Test : exploration sur une pile de test.
10. **Démo** (voir Démo), puis **documents** (voir plus bas ; ne pas supprimer ce plan), puis Bilan.

## Démo
- `DemoPlugin` (espace `demo`) déclare une source classpath `assets/demo/` (fichiers dans `demo/src/main/resources/assets/demo/`) : au-dessus du dossier du jeu, comme le fera un mod.
- Nouvelle scène `ResourceSourcesScene2D` (inscrite dans `DemoScenes` avec sa description) : une texture présente seulement dans la source de la démo, une texture du dossier du jeu remplacée par la source de la démo (même chemin), une entrée ajoutée par fusion à `textures.json`, et en texte les couches de chacune (`ResourceSources.layers`). Vérif. : `python tools/scenario.py "scene ResourceSourcesScene2D" wait-loaded "resources sources textures/…" "?gl summary"` (une capture seulement si le rendu ne peut pas être vérifié autrement).
- La démo est construite par `package` contre le jar : la source classpath de la démo est lue depuis le jar `demo` (`target/classes` dans l'IDE). Vérifier les deux.

## Documents à mettre à jour à la fin
- `architecture.md` : « Ressources CPU » (pile, chemins logiques, fusion, collisions, observateur), « Plugins et registre » (sources), « Démarrage » (construction de la pile avant le `ShaderManager`), « API publique » (nouveau package).
- `debug-tools.md` : `resources sources`, `-Dmightylib.resources.engine`, source par fichier dans `resources <Type> <nom>`.
- `decisions.md` : pile de sources (ordre moteur < jeu < plugins, fusion des listes, préfixe unique classpath, chemins logiques sans `..`).
- `later.md` : AIFileIO pour les modèles depuis un jar ; retrait d'entrée dans une liste fusionnée (mods) ; index des sources classpath dans le manifeste généré à la compilation ; observateur des ressources des mods (déjà prévu, à reformuler) ; module `jdk.zipfs` nécessaire au runtime pour JPMS/jlink (objectif « Packages internes et modules Java »).
- `current.md` : retirer les objectifs « Sources de ressources et plusieurs racines » et « Lectures en dur hors de `Resources` » ; préciser dans « Ressources du moteur dans le jar » que la source `assets/mightylib/` existe.
- `CLAUDE.md` : tests couverts (`resources/source`, fusion des listes, collisions, `resources sources`).
- `licence.md` : rien (aucune dépendance).
- Supprimer ce plan (discussion B).

## Hors périmètre
- Déplacer les ressources du moteur dans le jar : objectif suivant de `current.md`.
- Espaces de noms des ressources (`jeu:slime`) : `later.md`, Modding, et objectif « Ressources du moteur dans le jar ».
- Retrait d'une entrée d'une liste fusionnée, découverte des mods : `later.md`, Modding.
- Lecture Assimp depuis un jar (AIFileIO) : `later.md`.
- Index généré des sources classpath : manifeste de `later.md`.
- Source réseau ou archive zip hors classpath : l'interface le permet, rien de prévu.

## Questions restantes
- Aucune. La propriété `mightylib.resources.engine` reste active en release (inoffensive, utile pour déboguer un jeu livré).

## Modèles recommandés
- Discussion A, étapes 1 et 2 (API durable, threads, zip FS) : Opus 5.5, effort `high`.
- Étapes 3 à 7 (portage large, observateur, fusion : logique délicate sur beaucoup de fichiers) : Opus 5.5, effort `high`.
- Étapes 8 à 10 (petits portages, démo, docs) : Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`, gros diff à relire au regard du plan) : Opus 5.5, effort `high`.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/sources-de-ressources.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/resource-sources`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`ResourceSourcesScene2D` : textures remplacée et ajoutée ; scènes 3D ; scène de sons ; rechargement à chaud d'une texture du dossier du jeu et d'un fichier créé dans une racine plus haute avec `-Dmightylib.resources.watch=poll` ; démo lancée depuis le jar construit par `package`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/resource-sources`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
### Fait
Étapes 1 à 10, un commit par étape (8 et 9 séparés, la démo et les documents à part) : package `resources/source` (`IResourceSource`, `FolderSource`, `ClasspathSource`, `ResourceSources`, `SourceEntry`), déclaration par `registry.resources().source/classpath/folder`, pile construite par `Engine.setup` et donnée à `Resources`, au `ShaderManager` et au `ContextManager` ; exploration et lecture de tous les loaders par la pile (chemins logiques), règle de collision entre sources ; fusion par entrée de `textures.json`, `fonts.json`, `shaders.json` et des batchs ; observateur sur toutes les sources disque ; mappings de manettes et configurations réseau dans la pile ; commande `resources sources [chemin]` ; `-Dmightylib.resources.engine` ; source `assets/demo/` et `ResourceSourcesScene2D` ; documents.

### Écarts au plan
- `ShaderManager` reçoit la pile à l'étape 5 (pas 2) : il ne lit `shaders.json` qu'avec le `ShaderLoader`, porté à l'étape 5 ; évite un double dossier provisoire.
- Pour que chaque commit reste fonctionnel, les étapes 3 à 6 ont gardé des passerelles provisoires : `ResourceLoader.exploreDiskFolder` (loaders pas encore portés), replis sur le disque de `DataType.estimatedCpuBytes`, de l'observateur et de `resources <Type>`, `Resources.folder()` déduit du dossier du jeu. Toutes retirées aux étapes 7 et 9.
- `TextureDataLoader.discover` porté sur la pile dès l'étape 4 (plan : 7) : sa comparaison avec les chemins logiques des textures ne marchait plus.
- Constructeur `Resources(…, String dossier, …)` supprimé avec `folder()` (étape 7) : un seul constructeur, la pile ; les tests passent par `LoaderTestAccess.sources(dossier)` (nouvelle aide de test, qui attache aussi un loader ou une donnée à une pile).
- Changements d'API induits : `SoundDecoder.decode(sources, chemin, …)`, `SoundDataType.readVorbis(ByteBuffer, …)` (repli `getResourceAsStream` et `ioResourceToByteBuffer` supprimés), `FontFile.load(sources)`, `SilentSoundService`/`OpenALSoundService.Open` reçoivent le JSON de l'arbre des gains (lu par `EnginePlugin` dans la pile), `GamepadManager.loadMappingsFile` supprimé (`ContextManager.ReadMappings` lit le fichier, `addMappings` reçoit le texte), `FileMethods.readString/readJSONObject(sources, …)`, `readJSONObjectLayers`, `mergeEntries`.
- `resources <Type> <nom>` : `sourceFiles` pour toute donnée (avant : multi-fichiers seulement ; la clé `sources` disparaît) ; `source` = la source qui sert le fichier au moment de la commande (résolue à la demande, pas mémorisée à la lecture).
- Propriétaire du dossier du jeu : `ResourceSources.GAME_OWNER = "(game)"` (pas un espace de noms valide, aucune collision).
- `ClasspathSource` exige que le jar contienne les entrées de ses dossiers (Maven et Gradle les écrivent) ; noté dans sa Javadoc.
- Bug relevé hors sujet (non corrigé) : configuration réseau mal formée → exception brute (`later.md`, Bugs relevés en passant).
- Étapes 8 à 10 faites sur Opus 5.5 (recommandé : Sonnet 5.5), dans la même discussion.

### Vérifié
- `mvn -o -q test` : 1081 tests, 0 échec (nouveaux : `FolderSourceTest`, `ClasspathSourceTest` avec un jar écrit par le test, `ResourceSourcesTest`, `ResourcesSourcesTest`, `EngineSourcesTest`, `ListMergeTest`, `NetworkConfigurationLoaderTest`, cas ajoutés à `EngineRegistryTest`, `ResourceLoaderExploreTest`, `CubeMapLoaderTest`, `ShaderPreprocessorTest`, `FileWatchTest`, `ContextManagerTest`, `TextureDataLoaderTest` et `SoundDecodersTest` lus par une `ClasspathSource`). `package` : la démo compile et passe ses tests contre le jar.
- `python tools/analyse.py` : 0 constat (un `IdentityHashMapUsage` corrigé) ; aucun import inutilisé dans le diff.
- En jeu, `--strict` : `Scene2D`, `WaveFunctionCollapse2D`, `DocumentationScene` (batch, `sounds gains`), `SoundFormatsScene` (4 formats), `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` (cubemap), `ResourceSourcesScene2D` ; sans `--strict` (replis voulus) : `LayeredTexture2D`, `FallbackScene` avec `-Dmightylib.resources.watch=poll` (101 fichiers suivis). `leakcheck ResourceSourcesScene2D` : aucune fuite. `resources sources textures/sources/replaced.png` : deux couches, servie par `classpath:assets/demo/`.
- Démo lancée depuis `demo/target/mightylib-demo-1.1-SNAPSHOT-jar-with-dependencies.jar` et pilotée par `scenario.py --attach` : source de la démo `classpath jar`, non observable, les deux textures servies par elle, sortie sans erreur.

### À vérifier à la main
- `ResourceSourcesScene2D` : à gauche rayures **vertes** (la texture du dossier du jeu, rouge, est remplacée par celle du jar de la démo au même chemin), au milieu rayures **bleues** (texture ajoutée par le `textures.json` de la démo), dessous les couches de chaque fichier.
- Scènes 3D (`Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`) : modèles texturés et ciel comme avant.
- `SoundFormatsScene` : les quatre sons s'entendent.
- Rechargement à chaud, lancé avec `-Dmightylib.resources.watch=poll` : modifier une texture de `resources/textures/` (rechargée) ; créer dans `demo/target/classes/assets/demo/` (source de la démo en dossier, dans l'IDE) un fichier au chemin d'une ressource du dossier du jeu, ex. `textures/gui/…` : la donnée est relue depuis la source plus haute.
- Démo depuis le jar : `package`, puis `java -cp demo/target/mightylib-demo-1.1-SNAPSHOT-jar-with-dependencies.jar io.github.mightycode.mightylibdemo.main.Main` depuis la racine, menu → `ResourceSourcesScene2D`.
