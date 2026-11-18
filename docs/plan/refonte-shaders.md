# Refonte des shaders : service, `#version` injecté, defines

**Statut : validé.**

Décisions prises :
- `ShaderManager` devient un service : contrat `IShaderService` (clé `EngineServices.SHADERS` = `mightylib:shaders`) et implémentation par défaut `ShaderService`, **sans GL**, remplaçable par `registry.services().replace` comme le son. Raccourci `engine.shaders()`.
- Plus de dossier de version : `shaders/shaders.json` et `shaders/2D/…` à plat, dans la source du moteur et dans `resources/`. `resources/shaders/1_40/`, la branche GLSL 140 et `EngineConfig.setShaderVersion` sont supprimés.
- Les fichiers GLSL n'ont plus de `#version` : le moteur injecte un **prélude**. Un `#version` dans un fichier → `ResourceException` (repli magenta, exception en mode strict).
- Prélude, dans cet ordre : `#version 330 core`, defines globaux **triés par nom**, defines de l'entrée de `shaders.json` (gagnent sur un global de même nom), `#line 1 0`.
- Defines globaux : `setDefine(nom, valeur)`/`removeDefine(nom)` sur le service (thread principal, nom = identifiant GLSL vérifié) ; un changement recharge tous les `ShaderData`. Commandes DEV `shaders` et `shaders define`.
- Defines par entrée : clé `"defines"` d'une entrée de `shaders.json` (deux entrées peuvent partager leurs fichiers avec des defines différents).
- Constantes des uniforms génériques dans une classe `ShaderUniforms` (hors du service : un service remplacé ne les change pas).
- `shaders.json` reste lu une fois au démarrage ; son rechargement à chaud va dans `later.md`.
- Variantes demandées par chaque renderer (clé shader + defines, cache de programmes) : hors périmètre, dans `later.md` avec les matériaux.

## Pour démarrer
- Lire : `docs/architecture.md` sections « Démarrage » (`Engine.setup`), « Services », « Source du moteur », « Listes fusionnées par entrée », « Placeholders », `Shader`, « Bibliothèque GLSL » ; `docs/decisions.md` (`#include` par le préprocesseur, pile de sources, `Engine` unique) ; `docs/debug-tools.md`, section Commandes.
- Branche de sujet depuis `dev` : `feature/shader-service`.
- Compiler et tester avec les commandes de `CLAUDE.md` à chaque étape ; vérifier en jeu avec `python tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `mightylib/.../graphics/shader/ShaderManager.java` (classe finale, 75 lignes) : créé en dur par `Engine.setup` (`Engine.java` ~l. 291, après GLFW et `GenDeleteResources.Init`, avant `Resources`) ; `init(int)` lit `glGetString(GL_SHADING_LANGUAGE_VERSION)` pour choisir `1_40` ou `3_30` (seul usage du GL), puis fusionne `shaders/<version>/shaders.json` de la pile (`FileMethods.mergeEntries(FileMethods.readJSONObjectLayers(...), "shaders")`). Porte aussi `GENERIC_PROJECTION/VIEW/MODEL/COLOR_FIELD_NAME` (lus par `Renderer`) et `LIST_SHADER_FILE_NAME` (lu par `EngineResourcesTest`).
- `Engine.shaderManager()` ; `EngineConfig.setShaderVersion`/`getShaderVersion` (l. 73, 308, 534) ; `EnginePlugin(config, Supplier<ShaderManager>, console)` donne le fournisseur à `LibraryLoaders.mandatory(Supplier<ShaderManager>)`.
- `ShaderLoader` : lit le manager par un `Supplier` quand il travaille ; `create` parcourt `getShadersInfo().getJSONObject("shaders")` (clés `files`, `tags`) et préfixe les fichiers par `versionFolder()` ; `createFallback` → `new FallbackShaderData(version)` ; `preprocess` passe `lineOffset = -1` en `1_40`.
- `ShaderPreprocessor(reader, versionFolder, lineOffset)` (sans GL) : `#include` relatif au fichier puis au dossier de version ; exige le `#version` du fichier principal (« #include before #version »), refuse un `#version` dans un include, insère des `#line` (aucun avant `#version`), traduit le journal (`translateLog`). Testé par `ShaderPreprocessorTest` (chemins `shaders/3_30/…`).
- `FallbackShaderData(String version)` : génère ses sources avec `#version 330 core` ou `#version 140`.
- `ShaderData` : `isDimension2DShader` (tags `2D`/`none`), `sourcePaths`, rechargement par version (`Shader.use` recompile quand la version de la donnée change ; l'ancien programme reste jusqu'à la liaison).
- Fichiers : source du moteur `mightylib/src/main/resources/assets/mightylib/shaders/3_30/` (`shaders.json`, `2D/`, `3D/`, `font/`, `framebuffers/`, `utils/colorsManipulation.glsl`) ; démo `resources/shaders/3_30/` (`shaders.json`, `2D/`, `3D/`, `fallbackDemo/`) et `resources/shaders/1_40/` (4 fichiers). Les 34 fichiers principaux commencent par `#version 330` ou `#version 330 core`.
- Utilisateurs du dossier de version : `demo/.../scenes/FallbackScene.java` l. 49 (`engine().shaderManager().versionFolder()`), `EngineResourcesTest` (`SHADERS_FOLDER`), `ShaderPreprocessorTest` (`VERSION_FOLDER`).
- Services (`services/`) : `EngineServices` (clés `SOUND`, `CONSOLE`), `IEngineService` (phases `default` vides), `ServiceSet` crée les services dans `Engine.setup` **après** `Resources`/`GLResources`/`Commands` et **avant** `resources.init()`/`load()` : le `ShaderLoader` (lecture paresseuse) trouve donc le service créé. `EnginePlugin` enregistre `SOUND` et `CONSOLE` (`registry.services().register(clé, fabrique)`).
- `Resources.reloadAllOfType(Class)` existe (rechargement de tous les `ShaderData`).
- Ailleurs : jMonkeyEngine (version minimale déclarée, `#version` et `Defines` injectés), three.js (`defines` préfixés après `#version`, cache par clé), Bevy (`shader_defs`, cache par liste de defs : ordre différent = échec du cache), Unity (explosion des variantes, mots-clés globaux du code / locaux des matériaux).

## Décisions
1. `IShaderService extends IEngineService` : `declarations()` (JSON fusionné, objet `shaders`), `folder()` (`"shaders/"`), `prelude(JSONObject entrée)` ou équivalent (prélude complet pour une entrée, à partir d'un instantané des defines), `setDefine(String, String)`, `removeDefine(String)`, `defines()` (instantané immuable trié). Un service remplacé peut viser une autre version de GLSL : c'est lui qui écrit le prélude.
2. `ShaderService` (`final`) : construit avec la pile des sources (`engine.resources().sources()` dans la fabrique), lit `shaders.json` à la construction ; aucun GL. Defines globaux dans une `TreeMap` copiée à chaque changement et publiée par un champ `volatile` (le worker lit un instantané une fois par lecture de shader). `setDefine`/`removeDefine` vérifient le thread principal (`MainThread`), le nom (`[A-Za-z_][A-Za-z0-9_]*`, pas de préfixe `GL_`) et la valeur (sans retour à la ligne : une valeur venue d'une commande ou d'un mod ne doit pas glisser une directive dans le prélude), même règle pour les `defines` d'une entrée, ne font rien si la valeur ne change pas, sinon `resources.reloadAllOfType(ShaderData.class)`.
3. Clé `EngineServices.SHADERS`, enregistrée par `EnginePlugin` sans repli (ne peut échouer que sur un `shaders.json` illisible : le moteur s'arrête, `SERVICE_FAIL`). `Engine.shaders()` = `service(SHADERS)`.
4. Defines d'une entrée : objet `"defines": { "NOM": "valeur" }` (valeur chaîne, `""` pour un simple `#define NOM`) ; lus par le `ShaderLoader` et gardés dans la `ShaderData` (immuable).
5. `ShaderPreprocessor(reader, folder, prelude)` : plus de `lineOffset` ; un `#version` dans **tout** fichier → `ResourceException` (« #version is written by the engine ») ; sortie = prélude + `#line 1 0` + source, les `#line` des includes inchangés ; « #include before #version » disparaît.
6. `FallbackShaderData()` sans paramètre, sources en `#version 330 core` (ne passe pas par le prélude : il ne doit pas pouvoir échouer, même avec un service remplacé).
7. `ShaderUniforms` (`final`, constantes `PROJECTION`, `VIEW`, `MODEL`, `COLOR`) dans `graphics/shader`.
8. Commandes `mightylib:shaders` (DEV) : `shaders` (version écrite par le prélude, defines globaux, entrées avec fichiers, tags, defines) et `shaders define <nom> [valeur]` / `shaders undefine <nom>` ; message lisible, détail dans les `data`.

## Étapes
1. **Shaders à plat et sans `#version`** (fusionnable seule avec l'étape 2 seulement : à faire ensemble si le build casse entre les deux). Déplacer `shaders/3_30/*` vers `shaders/` (moteur et démo, `git mv`), supprimer `resources/shaders/1_40/`, retirer la ligne `#version` des 34 fichiers principaux (Edit, pas `sed`). `ShaderPreprocessor` : constructeur `(reader, folder, prelude)`, règles de la décision 5. `ShaderManager` provisoire : plus de branche 140, `versionFolder()` → `shaders/`, prélude fixe `#version 330 core\n`. `FallbackShaderData()` sans paramètre. Migrer `FallbackScene`, `EngineResourcesTest`, `ShaderPreprocessorTest`. Supprimer `EngineConfig.setShaderVersion`/`getShaderVersion`.
   - Tests : `ShaderPreprocessorTest` adapté (prélude en tête, `#line 1 0` ensuite, numéros du journal exacts, `#version` dans le fichier principal ou un include → `ResourceException`).
   - En jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "?gl summary"`, puis `Scene3D` et `FallbackScene` (aucun shader en repli hors de ceux voulus : `resources` → `usingFallback`).
2. **Service `IShaderService`/`ShaderService`** : clé `EngineServices.SHADERS`, enregistrement dans `EnginePlugin`, `Engine.shaders()`, suppression de `Engine.shaderManager()`, de la création en dur dans `Engine.setup` et de `ShaderManager` ; `ShaderLoader` et `LibraryLoaders.mandatory` prennent un `Supplier<IShaderService>` (lu dans `Engine` par `service`). `ShaderUniforms` remplace les constantes `GENERIC_*` (`Renderer`). Statut `@API` du contrat : `EXPERIMENTAL` ; `PublicFieldsTest`/`ExtensionFieldsTest`/`ApiStatusTest` à jour.
   - Tests : `ShaderServiceTest` (fusion des couches de `shaders.json` sur une pile de test, dossier, prélude d'une entrée sans define) ; `EngineRegistryTest`/`ServicesSection` si l'ordre des services du moteur y est vérifié.
   - En jeu : `services` liste `mightylib:shaders` ; `Scene2D`, `Scene3D` s'affichent.
3. **Defines globaux et par entrée** : `defines` de `shaders.json` gardés dans `ShaderData` ; prélude de la décision 1 (tri, entrée prioritaire) ; `setDefine`/`removeDefine` (thread principal, nom vérifié, rechargement des `ShaderData`) ; instantané immuable lu une fois par le worker.
   - Tests : `ShaderServiceTest` (ordre trié, entrée prioritaire, `#define NOM` sans valeur, nom invalide → `IllegalArgumentException`, même valeur → pas de rechargement, instantané non modifié par un `setDefine` ultérieur).
   - En jeu : étape 5.
4. **Commandes `shaders`** (décision 8) avec syntaxe déclarée, complétion des noms de defines posés.
   - Tests : syntaxe et complétion (modèle des tests `utils/enginecommand/syntax`).
   - En jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "?shaders" "shaders define DEMO 1" "?shaders"`.
5. **Scène de démo `ShaderDefines2D`** (voir Démo), inscrite dans `DemoScenes`.
   - En jeu : `python tools/scenario.py "scene ShaderDefines2D" wait-loaded "input D" "?shaders"` puis capture de la zone utile si le changement de couleur ne se lit pas autrement.
6. **Documents** (liste plus bas), sans supprimer ce plan.

## Démo
`ShaderDefines2D` : deux entrées de `resources/shaders/shaders.json` sur les mêmes fichiers (`definesDemo2D` sans define, `definesDemoTinted2D` avec `"defines": {"TINT": "vec3(0.2, 0.6, 1.0)"}`) dessinées côte à côte ; touche D = `shaders().setDefine("DEMO_INVERT", "")`/`removeDefine` (couleurs inversées partout où le shader de démo le lit, rechargement visible) ; texte d'aide à l'écran.

## Documents à mettre à jour à la fin
- `architecture.md` : « Démarrage » (plus de `ShaderManager.init`, service `mightylib:shaders`), « Services » (clé `SHADERS`), `Engine` (`shaders()`), « Source du moteur » (`shaders/` à plat), « Listes fusionnées » (`shaders/shaders.json`), « Bibliothèque GLSL » (prélude, `#version` interdit, plus de GLSL 140), uniforms génériques (`ShaderUniforms`), classes `final`/extensibles si concernées.
- `debug-tools.md` : commandes `shaders`.
- `decisions.md` : `#version` et defines injectés par le service (prélude trié), une seule version de GLSL par jeu de fichiers.
- `later.md` : rechargement à chaud de `shaders.json` (à côté de « Retrait d'une entrée d'une liste fusionnée ») ; variantes par renderer (clé shader + defines normalisée, cache de programmes, garde contre l'explosion des variantes) avec les matériaux ; gamma : utiliser un define global.
- `futur-discussion.md` : version GLSL au-delà de 3.3 → par remplacement du service (prélude).
- `CLAUDE.md` : tests couverts (`ShaderServiceTest`, préprocesseur avec prélude, commandes `shaders`).
- `current.md` : retirer la ligne « Refonte des shaders » ; le test d'acceptation garde « un shader remplacé ».
- Puis supprimer ce plan (discussion B).

## Hors périmètre
- Variantes à la demande par renderer et cache de programmes par clé → `later.md` (matériaux).
- Rechargement à chaud de `shaders.json` (ajout, retrait, defines d'une entrée) → `later.md`.
- `registry.shaders()` (déclaration par code) : `shaders.json` fusionné par source suffit ; à rouvrir avec le modding.
- `GL_KHR_parallel_shader_compile`, programmes hors `GenDeleteResources` : déjà dans `later.md`.

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 1 à 3 : Opus 5.5, effort `high` (contrat d'un service remplaçable, prélude et numéros de ligne, instantané entre threads).
- Étapes 4 à 6 : Sonnet 5.5, effort `medium` (commande sur un modèle existant, scène de démo, docs).
- Discussion B : Opus 5.5, effort `high` (gros diff : déplacement de fichiers + contrat d'API à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/refonte-shaders.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan est déjà le premier commit de la branche `feature/shader-service` (créée depuis `dev`, poussée) : récupérer cette branche (`git fetch` puis `git switch feature/shader-service`) et travailler dessus.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`Scene2D`, `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`, `FallbackScene` : rendu identique à avant ; `ShaderDefines2D` : touche D ; modifier un fichier GLSL en jeu : rechargement à chaud). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

## Bilan (discussion A)
### Fait, un commit par étape
1. `a2d4dcf` : `shaders/` à plat (moteur et démo), `1_40` supprimé, `#version` retiré des 34 fichiers ; `ShaderPreprocessor(lecteur, dossier, prélude)` (sortie = prélude + `#line 1 0` + source, `#version` refusé partout) ; `FallbackShaderData()` ; `EngineConfig.setShaderVersion` supprimé.
2. `652b850` : `IShaderService` (`EXPERIMENTAL`), `ShaderService` (`final`, sans GL), `EngineServices.SHADERS`, `Engine.shaders()`, `ShaderUniforms` ; `ShaderManager` supprimé.
3. `cf17d8a` : defines globaux (`setDefine`/`removeDefine`/`defines`) et `defines` d'entrée (gardés dans `ShaderData`, recopiés par `createReloadCopy`) ; prélude calculé une fois par lecture d'un shader, le même pour ses étapes.
4. `b793a06` : commande `shaders`, `shaders define <nom> [valeur]`, `shaders undefine <nom>`.
5. `e3fa17f` : scène `ShaderDefines2D` (entrées `definesDemo2D`/`definesDemoTinted2D`, fichier `resources/shaders/2D/defines/definesDemo2DF.glsl`, vertex du moteur `colorShape2DV.glsl`).
6. `e71ba3c` : documents.

### Écarts au plan et raisons
- Pas de `shaders.json` dans aucune source : `ShaderService` échoue (comme `ShaderManager` avant), le moteur s'arrête (`SERVICE_FAIL`) ; le moteur en a toujours un.
- `IShaderService.prelude(Map<String, String>)` plutôt que `prelude(JSONObject entrée)` : la `ShaderData` garde déjà ses defines (immuables, triés), le loader n'a plus le JSON au moment de lire.
- Validation des defines : `ShaderService.CheckDefine` (public statique), appelée par le service et par le `ShaderLoader` pour ceux d'une entrée (define invalide → `ResourceException`, repli, avant la lecture : les fichiers restent suivis). Une valeur JSON non chaîne (nombre, booléen) est gardée telle qu'écrite.
- `shaders undefine <nom>` en branche sœur de `define` (le plan citait les deux formes) ; `undefine` n'accepte qu'un define posé.
- `ShaderDefines2D` retire `DEMO_INVERT` dans `unload` : un define global survit à la scène sinon.
- Commande du plan `"input D"` → `"input key D"` (syntaxe de `input`).
- `ShaderPreprocessorTest` et deux lignes d'autres tests ont été réécrits par script (Python, `sed`) au lieu d'Edit ; fins de ligne LF vérifiées.

### Vérifié et comment
- `mvn -o -q test` et `package` : verts (dont `ShaderServiceTest`, `ShadersGenComTest`, `ShaderPreprocessorTest`, `EngineResourcesTest` qui préprocesse chaque shader du moteur avec le prélude, gardes d'API).
- `scenario.py` : `Scene2D`, `Scene3D` (`gl summary` : `failed` vide) ; `FallbackScene` (seuls les replis voulus ; journal `notCompilingF.glsl:6`, ligne exacte sans `#version`) ; `services` liste `mightylib:shaders` ; `shaders define DEMO 1` puis `?shaders` ; `ShaderDefines2D` : `input key D` → `#define DEMO_INVERT`, capture : couleurs inversées des deux carrés, carré de droite teinté par `TINT` ; retour au menu → 0 define global ; `leakcheck ShaderDefines2D` : no leak.

### À vérifier à la main
- `Scene2D`, `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`, `FallbackScene` : rendu identique à avant (seules `Scene2D`, `Scene3D` et `FallbackScene` ont été lancées, sans capture).
- `ShaderDefines2D` : touche D plusieurs fois (inversion puis retour), texte d'aide à jour.
- Rechargement à chaud : `watch on`, modifier un fichier GLSL (par exemple `definesDemo2DF.glsl`) pendant `ShaderDefines2D` : le shader se recharge avec le prélude et les defines courants ; y écrire un `#version` : l'ancienne version reste affichée (rechargement échoué, pas de repli) et le journal donne `#version in …definesDemo2DF.glsl:1: the #version is written by the engine` ; le retirer recharge le shader.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/shader-service`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Vérifications manuelles : celles du « Bilan ».
