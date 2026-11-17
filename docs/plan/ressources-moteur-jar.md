# Ressources du moteur dans le jar

**Statut : validé.**

Décisions prises :
- Le moteur livre les shaders qu'il nomme et les shaders de base. Politique durable : **le moteur fournit beaucoup de shaders**, ce sera à terme sa ressource la plus fournie.
- Les polices Microsoft (Arial, Bahnschrift) sont **supprimées du dépôt**. Le moteur livre **Liberation Sans** pour l'interface et **Liberation Mono** pour la console (**Liberation 2.x, SIL OFL 1.1** ; pas les 1.x, sous GPL v2 avec exception).
- En bonus pour les utilisateurs, chaque famille est aussi livrée en **Bold, Italic et BoldItalic** : 8 atlas en tout, chacun déclaré comme une police à part. Le moteur n'utilise que les deux Regular. La gestion des styles (variantes liées logiquement à une famille) et les polices sur plusieurs fichiers (pages, tailles) viendront plus tard.
- **Claude génère les atlas** avec BMFont : `C:\Users\Maxence\Downloads\bmfont64_1.14b_beta\bmfont64.exe`, en ligne de commande.
- Un `Text` sans `setFont` prend la police d'interface du moteur.
- Les ressources du moteur sont chargées par un **batch du moteur**, toujours chargé avant le premier batch du jeu.
- **Noms simples**, sans espace de noms. Un jeu remplace une ressource du moteur en déclarant le même nom ou le même chemin. Les noms utilisés par le moteur sont des constantes d'une seule classe, vérifiées par un test.
- Les `.ttf` et leur licence sont dans le dépôt, hors du jar. Le jar contient l'atlas BMFont et la licence.
- `gamecontrollerdb.txt` (SDL, zlib) entre dans le jar du moteur.

## Pour démarrer
- Lire : `docs/current.md` (première ligne), `docs/architecture.md` sections « Ressources CPU » (Sources, Listes fusionnées, Placeholders) et « Console », `docs/decisions.md` (pile de sources), `licence.md` (sections Ressources et « à faire avant distribution »), `docs/debug-tools.md` (ligne `-Dmightylib.resources.engine`).
- Branche de sujet depuis `dev` : `feature/engine-resources`.
- Compiler et tester avec les commandes de `CLAUDE.md`. Lancer `package` à la fin, car la frontière bibliothèque/démo change. Vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).
- Déplacer les fichiers avec `git mv` pour garder l'historique.

## Contexte : ce qui existe
- **Pile de sources** (`resources/source`) : source classpath du moteur `assets/mightylib/` < dossier du jeu `resources/` < plugins. Elle est déclarée par `EnginePlugin` (`registry.resources().classpath()`, ou `.folder(...)` avec `-Dmightylib.resources.engine`) et elle est **vide** aujourd'hui : `mightylib/src/main/resources/` n'existe pas. `EngineSourcesTest` vérifie le nom `classpath:assets/mightylib/`.
- **Listes fusionnées par entrée**, la source la plus haute gagne : `shaders/3_30/shaders.json`, `textures/fonts/fonts.json`, `textures/textures.json`, batchs (listes réunies). Un `#include` se résout dans la pile : un shader du jeu peut inclure un fichier du moteur.
- **Ressources nommées par la bibliothèque mais fournies par le jeu** (`resources/`, chargées par `init.batch` de la démo avec `.*`) :
  - shaders : `colorShape2D` (`RectangleRenderer`/`MeshRenderer` : console, GUI, debug), `multiColoredShape2D`, `colorEllipse2D`, `coloredText` (`Text`), `postProcessing` (`VirtualSceneRenderer`), `skybox`, `multiColoredLighting3D`, `textureMultipleLights3D` (`ModelRenderer.DEFAULT_SHADER`). Ils incluent `utils/colorsManipulation.glsl`.
  - polices : `bahnschrift` (`EngineConfig.consoleFont`, `ValueSelector`) et `arial` (`DebugInfoCamera2D/3D`). Leurs atlas sont déclarés dans le groupe `fonts` de `resources/textures/textures.json` et dans `resources/textures/fonts/fonts.json`.
- `Text` n'a pas de police tant que `setFont` n'est pas appelé (`font = null` : rien n'est dessiné). La démo appelle `setFont("bahnschrift")` 28 fois et `setFont("arial")` 3 fois, plus 3 littéraux `"bahnschrift"`.
- La texture `error` n'est pas un fichier : chaque placeholder est généré par `ResourceLoader.createFallback`. Il n'y a rien à déplacer pour elle.
- `Resources.load()` charge les replis, puis tous les `BatchResources`, puis `firstBatch` (`BatchResourcesMethod`) ou tous les types (`AllResourcesMethod`).
- `GamepadManager.MAPPINGS_FILE = "gamecontrollerdb.txt"` est lu à la racine de la pile, facultatif, la source la plus haute gagne (`ContextManager.ReadMappings`). Aucun fichier n'est fourni aujourd'hui. GLFW embarque une base plus ancienne.
- `resources/shaders/1_40/` (4 fichiers, incompatible avec le contexte 3.3 core) relève de la refonte des shaders : **on n'y touche pas**.
- `resources/textures/fonts/svbasicmanual.*` : `.fnt` vide, absent de `fonts.json`, inutilisé.
- Tests proches : `EngineSourcesTest`, `ClasspathSourceTest`, `ListMergeTest`, `ShaderPreprocessorTest`, `ContextManager` (mappings lus dans les sources), `FontFile`.

## Décisions
- **Shaders du moteur** (`assets/mightylib/shaders/3_30/`, mêmes sous-dossiers qu'aujourd'hui) : `colorShape2D`, `colorEllipse2D`, `texture2D`, `multiColoredShape2D`, `coloredText`, `postProcessing`, `colorShape3D`, `texture3D`, `skybox`, `multiColoredLighting3D`, `textureMultipleLights3D`, et `utils/colorsManipulation.glsl`. `textureMultipleLights3D` utilise `3D/complex/textureMaterial3DV.glsl` : ce fichier passe au moteur, et `textureMaterial3D` (démo) le lit à travers la pile. Le `shaders.json` du moteur déclare ces entrées, celui du jeu ne garde que les siennes (`customStar2D`, `fallbackDemo`, displacement, material…).
- **Polices du moteur** : `liberationSans` (interface, `EngineConfig.setDefaultFont`) et `liberationMono` (console, `EngineConfig.setConsoleFont`). En bonus : `liberationSansBold`, `liberationSansItalic`, `liberationSansBoldItalic`, et les trois mêmes pour `liberationMono…`. Ce sont des atlas BMFont de même taille que Bahnschrift (`fontSize=-32`), sur une seule page (1024×1024, ou 2048×2048 si le jeu de caractères ne tient pas : `FontFile` ne lit qu'une page), avec le jeu de caractères de `bahnschrift.bmfc` plus les flèches U+2190–2195 (la console pourra afficher `↓`).
- **Les 8 atlas sont déclarés** dans le `fonts.json` et le `textures.json` du moteur, mais **le batch du moteur ne charge que les deux Regular**. Un jeu charge une variante dans son propre batch : chaque atlas RGBA de 1024² occupe environ 4 Mo sur la carte, donc les charger tous coûterait 32 Mo. Les constantes de `EngineResources` couvrent les 8 noms.
- **Fichiers source des polices** : l'utilisateur a déposé une archive 1.05 dans `mightylib/font/liberation_sans/` et `mightylib/font/liberation_mono/`. Elle est remplacée par les `.ttf` 2.1.4 installés (voir le prérequis de l'étape 2). L'implémentation peut déplacer et nettoyer ces dossiers, par exemple vers `mightylib/fonts/liberation-sans/` et `mightylib/fonts/liberation-mono/` : garder les `.ttf`, la licence (`OFL.txt` ou `LICENSE`), `AUTHORS` et `README`, et ranger les `.bmfc` qui produisent les atlas à côté. Ce dossier est hors de `src/`, donc hors du jar. Le jar contient `assets/mightylib/textures/fonts/` : `.fnt`, `_0.png` et la licence (l'OFL demande de joindre la licence aux polices distribuées, même converties).
- **Classe des noms du moteur** : `EngineResources` (package `resources`, publique, `final`) regroupe des constantes `public static final String` par type (shaders, polices, batch `mightylib`, fichier de mappings). Le code de la bibliothèque n'écrit plus ces noms en littéraux. `ModelRenderer.DEFAULT_SHADER` et `GamepadManager.MAPPINGS_FILE` renvoient à ces constantes, ou disparaissent au profit d'elles s'il n'y a pas d'autre utilisateur.
- **Batch du moteur** : `assets/mightylib/batchs/mightylib.batch` (shaders du moteur, atlas et `FontFace` des deux polices). `Resources.load()` le charge **avant** le premier batch du jeu, avec les deux méthodes de chargement (`AllResourcesMethod` charge déjà tout, mais le batch garde la référence). Il n'est jamais déchargé. Un jeu ou un plugin peut y ajouter des entrées en déclarant un batch `mightylib` dans sa source (fusion des batchs).
- **Remplacement** : même nom (entrée de `shaders.json`/`fonts.json`/`textures.json`) ou même chemin dans une source plus haute. C'est déjà le comportement de la pile ; il est documenté pour le jeu.
- **`Text` sans police** : le constructeur prend la police par défaut de l'`Engine` (`EngineConfig.getDefaultFont()`, `EngineResources.DEFAULT_FONT` par défaut). `setFont` la remplace.
- **`gamecontrollerdb.txt`** : à la racine d'`assets/mightylib/`, version figée (commit de SDL_GameControllerDB noté dans `licence.md`). Le jeu le remplace en mettant le sien à la racine de `resources/`.
- **Pas d'espace de noms des ressources** dans ce point. Les identifiants `mightylib:nom` iront avec le modding (`later.md`).

## Étapes
Chaque étape compile, passe `mvn -o -q test` et peut être commitée seule. Les étapes 1 et 2 laissent la démo fonctionnelle.

### 0. Commit du plan
Ce fichier tel quel.

### 1. Classe `EngineResources`, batch du moteur et shaders
- Créer `mightylib/src/main/resources/assets/mightylib/` et y déplacer (`git mv`) les fichiers `.glsl` listés dans Décisions, plus `utils/colorsManipulation.glsl`.
- `assets/mightylib/shaders/3_30/shaders.json` : les entrées du moteur, copiées de celui du jeu. Retirer ces entrées du `resources/shaders/3_30/shaders.json` de la démo.
- `assets/mightylib/batchs/mightylib.batch` : `ShaderData` avec la liste explicite des shaders du moteur (pas de regex : le jeu garde son `.*` sur ses propres shaders).
- `resources/EngineResources` : constantes des shaders et du batch. Remplacer les littéraux de la bibliothèque (`"colorShape2D"` ×16, `multiColoredShape2D`, `colorEllipse2D`, `coloredText`, `postProcessing`, `skybox`, `multiColoredLighting3D`, `textureMultipleLights3D`). Les littéraux de la démo peuvent rester, ou passer aux constantes quand c'est plus clair.
- `Resources.load()` : charger `EngineResources.ENGINE_BATCH` après les descriptions de batchs et avant `firstBatch`. Un batch absent (moteur remplacé par un dossier incomplet) déclenche une `ResourceException` claire, ou une exception en mode strict.
- Le `init.batch` de la démo garde `ShaderData .*` : les noms du moteur y sont déjà chargés, la double référence est sans effet.
- **Tests** : `EngineResourcesTest` (dans `resources`) ouvre `ClasspathSource(…, "assets/mightylib/")` depuis le classpath de test et vérifie :
  - chaque constante de shader est une entrée du `shaders.json` du moteur, et chacun de ses fichiers existe ;
  - chaque `#include` des shaders du moteur se résout dans la source du moteur seule ;
  - le batch du moteur existe et liste exactement les constantes.

  Ajouter un test de `Resources` (base `ResourcesTestBase`) : le batch du moteur est chargé avant le premier batch du jeu, avec les deux méthodes de chargement.
- **Vérification en jeu** : `python tools/scenario.py "scene Scene2D" wait-loaded "?gl summary" "resources sources shaders/3_30/2D/essential/colorShape2DF.glsl"`. Le fichier doit être servi par `classpath:assets/mightylib/`, sans shader en repli. Puis `"scene Scene3D" wait-loaded "?gl summary"` et `Game3D2D` (skybox, éclairage). Ouvrir et fermer la console (fond `colorShape2D`).

### 2. Polices Liberation, police par défaut de `Text`
**Prérequis, à faire en premier** : l'archive déposée dans `mightylib/font/liberation_*/` est la **1.05** (table `name` : « Version 1.05 », `License.txt` : GPL v2 avec exception de police et contrat Red Hat). Elle ne sert pas. Les polices installées sur la machine sont la **2.1.4 sous SIL OFL 1.1** (`C:\Windows\Fonts\LiberationSans-*.ttf` et `LiberationMono-*.ttf`, table `name` : « Version 2.1.4 », « Licensed under the SIL Open Font License, Version 1.1 »). Il faut donc :
- copier ces 8 `.ttf` (Regular, Bold, Italic, BoldItalic des deux familles) dans le dossier source rangé (voir Décisions) ;
- supprimer les fichiers 1.05 (`.ttf`, `License.txt`, `COPYING`, `ChangeLog` de la 1.x) ;
- ajouter la licence : le fichier `LICENSE` de la release 2.1.4 (texte OFL 1.1 avec l'en-tête « Digitized data copyright (c) 2010 Google Corporation with Reserved Font Arimo, Tinos and Cousine. Copyright (c) 2012 Red Hat, Inc. with Reserved Font Name Liberation. »). Le télécharger depuis le dépôt GitHub `liberationfonts/liberation-fonts` **avec l'accord de l'utilisateur**, ou l'écrire à partir du texte officiel de l'OFL 1.1 et de cet en-tête.

Avant de générer, relire la version de chaque `.ttf` copié (table `name`, ID 5 et 13) : ne générer aucun atlas depuis une 1.x.

Ensuite :
- Ranger les fichiers source (voir Décisions, « Fichiers source des polices »).
- **Claude écrit un `.bmfc` par atlas** (8 en tout), copié de `bahnschrift.bmfc` :
  - `fontName=Liberation Sans` (ou `Liberation Mono`) et `fontFile=` vers le `.ttf` du style (BMFont charge une police non installée) ; pour les styles, vérifier que le `.fnt` produit a bien le bon visage (`info face=…`, `bold`, `italic`) et que les glyphes sont en gras ou en italique (une capture d'un atlas suffit). Si BMFont choisit le Regular, régler `isBold`/`isItalic` ;
  - les lignes `chars=` de `bahnschrift.bmfc`, plus `8592-8597` ;
  - même taille et même padding.
- **Claude génère les atlas** : `"/c/Users/Maxence/Downloads/bmfont64_1.14b_beta/bmfont64.exe" -c <style>.bmfc -o <dossier>/assets/mightylib/textures/fonts/<nom>.fnt` (`<dossier>` = `mightylib/src/main/resources`). Sortie attendue : `<nom>.fnt` + `<nom>_0.png` pour les 8 noms, une seule page chacun (sinon agrandir `outWidth`/`outHeight`), et le nom de page du `.fnt` doit correspondre au `.png`. Si la ligne de commande ne marche pas, demander à l'utilisateur de les générer depuis l'interface.

Ensuite, dans le code :
- `assets/mightylib/textures/textures.json` (groupe `fonts`, `realistic`, `rectangle`, comme aujourd'hui) et `assets/mightylib/textures/fonts/fonts.json` déclarent les 8 atlas, et la licence est copiée à côté des atlas. Le batch du moteur n'ajoute que les `TextureData` et `FontFace` des deux Regular.
- `EngineResources` : `DEFAULT_FONT = "liberationSans"`, `CONSOLE_FONT = "liberationMono"`, et les constantes des 6 variantes. `EngineConfig` : `consoleFont` par défaut `CONSOLE_FONT`, nouveau `setDefaultFont`/`getDefaultFont` (défaut `DEFAULT_FONT`).
- `Text()` : police par défaut de l'`Engine` (lue comme `resources()` est lue aujourd'hui). `ValueSelector`, `DebugInfoCamera2D/3D` : retirer `setFont(...)` (police par défaut).
- Démo : retirer les 31 `setFont("bahnschrift"|"arial")` (garder `setFontSize` et le reste de la chaîne) et remplacer les 3 littéraux `"bahnschrift"` par la police par défaut. Retirer arial et bahnschrift de `init.batch`, du groupe `fonts` de `resources/textures/textures.json` et de `resources/textures/fonts/fonts.json`. Supprimer (`git rm`) `arial.*`, `bahnschrift*` et `svbasicmanual.*`, puis le dossier `resources/textures/fonts/` s'il est vide.
- **Tests** : dans `EngineResourcesTest`, chaque police du moteur (les 8) est une entrée de `fonts.json`, son atlas est une entrée de `textures.json`, ses fichiers existent dans la source du moteur, et le `.fnt` se lit avec `FontFile` (page = `.png` présent). Ajouter un test de `EngineConfig` (police par défaut et police de la console) s'il y a de la logique. Vérifier aussi qu'aucun fichier sous `resources/` n'a plus `arial`/`bahnschrift` dans son nom : `DemoScenes` ou un test de la démo.
- **Vérification en jeu** : `MenuScene` (le texte tient dans les boutons, liste des scènes), `Scene2D` (`DebugInfoCamera2D`), `GUIWidgetsScene` (`ValueSelector`, `TextField`, `Slider`), la console (`²` : saisie, sélection, curseur aligné sur les caractères en monospace, indicateur des lignes sous la vue), l'écran de chargement. `"?gl text"` vérifie les textes ; faire au plus une capture cadrée si l'alignement de la console ne se vérifie pas autrement.

### 3. `gamecontrollerdb.txt` dans le moteur
- **Avec l'accord de l'utilisateur** (téléchargement d'un fichier) : récupérer `gamecontrollerdb.txt` du dépôt GitHub `mdqinc/SDL_GameControllerDB` à un commit donné (le noter). Le mettre à la racine d'`assets/mightylib/`, avec `LICENSE` (zlib) à côté sous le nom `gamecontrollerdb.LICENSE.txt`.
- `EngineResources.GAMEPAD_MAPPINGS` : `GamepadManager.MAPPINGS_FILE` y renvoie.
- **Tests** : `EngineResourcesTest` vérifie que le fichier est présent dans la source du moteur. Ajouter un test de `ContextManager` : un fichier du jeu à la racine remplace celui du moteur (s'il n'existe pas déjà).
- **Vérification en jeu** : `GamepadScene` démarre, le journal annonce les mappings ajoutés sans erreur de syntaxe (filtrer la sortie de `scenario.py` sur `mapping`).

### 4. Documents et bilan
Mettre à jour les documents listés plus bas (sans supprimer ce plan). Lancer la commande `package` de `CLAUDE.md` : la démo compile et passe ses tests contre le jar. Vérifier dans le jar (`jar tf mightylib/target/mightylib-*.jar | grep assets/mightylib`) que les shaders, les atlas, `OFL.txt` et `gamecontrollerdb.txt` y sont, et qu'aucun `.ttf` n'y est.

## Démo
Pas de nouvelle scène : le changement se voit dans toutes les scènes (polices) et dans `ResourceSourcesScene2D` / `resources sources`, qui montrent la source du moteur servant les shaders. Option pour l'implémentation, si c'est simple : dans `ResourceSourcesScene2D`, remplacer un shader du moteur depuis la source de la démo (`assets/demo/`) pour illustrer le remplacement par nom. Le demander avant de le faire.

## Documents à mettre à jour à la fin
- `architecture.md` : « Ressources CPU » (contenu de la source du moteur, batch du moteur chargé avant le premier batch, `EngineResources`, remplacement par nom ou chemin), « Console » (Liberation Mono), « Rendu »/texte (`Text` a une police par défaut), « Entrées » (mappings du moteur).
- `decisions.md` : le moteur livre beaucoup de shaders, les ressources du moteur sont sous licence libre (pas de police propriétaire), les noms des ressources du moteur sont simples et remplaçables (espace de noms avec le modding).
- `licence.md` : retirer Arial, Bahnschrift et SV Basic Manual. Ajouter Liberation Sans/Mono 2.1.4 (SIL OFL 1.1, fichiers, ce que l'OFL demande : licence jointe, pas de vente de la police seule, nom réservé « Liberation » pour une version modifiée ; vérifier dans la FAQ de l'OFL si un atlas bitmap compte comme une version modifiée, et si oui, renommer le `face` des `.fnt` et les noms de ressource) et SDL_GameControllerDB (zlib, commit). Mettre à jour « à faire avant distribution » : le point des polices est réglé.
- `debug-tools.md` : rappeler `-Dmightylib.resources.engine=mightylib/src/main/resources/assets/mightylib` pour éditer à chaud les shaders du moteur.
- `later.md` : retirer « police monospace livrée avec le moteur » et le glyphe flèche de la ligne Console s'ils sont réglés. Ajouter à la ligne du modding ou des ressources : identifiants `espace:nom` des ressources ; mise à jour périodique de `gamecontrollerdb.txt`.
- `current.md` : retirer la ligne « Ressources du moteur dans le jar » et son « À décider ».
- `CLAUDE.md` : tests couverts (`EngineResourcesTest` : contenu de la source du moteur ; chargement du batch du moteur).
- Supprimer ce plan (fait par `/pr-ml`).

## Hors périmètre
- `shaders/1_40/` et la forme de la déclaration des shaders (`ShaderManager` en service, `#version` injecté) : ligne « Refonte des shaders » de `current.md`.
- Identifiants `mightylib:nom` des ressources : avec le modding (`later.md`).
- Gestion des styles (famille + Regular/Bold/Italic/BoldItalic liés logiquement, `setFont(famille, style)`), polices sur plusieurs fichiers (pages, plusieurs tailles), texte net à toutes les tailles (atlas SDF/MSDF ou atlas généré au lancement depuis le `.ttf`) : à noter dans `later.md` (partie texte), décidés plus tard avec l'utilisateur.
- Ressources lourdes de la démo (modèles) : ligne « Paquet de démonstration » de `current.md`.
- Textures de GUI (`button-hover`/`button-unhover`) : la bibliothèque ne les nomme pas, elles restent dans la démo.

## Questions restantes
- Aucune. Les accords à demander en cours de route (téléchargement du `LICENSE` de Liberation et de `gamecontrollerdb.txt`) sont indiqués dans leurs étapes.

## Modèles recommandés
- Discussion A, étape 1 : **Opus 5.5, effort high** (chargement du batch du moteur dans `Resources`, nouvelle API publique `EngineResources`, tests de la source du moteur).
- Discussion A, étapes 2 et 3 : **Sonnet 5.5, effort medium** (migration mécanique des polices et de la démo, ajout d'un fichier).
- Discussion A, étape 4 (docs) : **Sonnet 5.5, effort medium**.
- Discussion B (`/pr-ml`) : **Opus 5.5, effort high** (gros diff de fichiers déplacés, à relire au regard du plan et des licences).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/ressources-moteur-jar.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre modèle, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/engine-resources`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur. Liste : `MenuScene` et la console avec les nouvelles polices (lisibilité, alignement du curseur), `Scene3D`/`Game3D2D` (skybox, éclairage), `GamepadScene` avec une manette, `GUIWidgetsScene`. La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/engine-resources`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
