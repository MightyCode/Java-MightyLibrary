# Démo en module Maven séparé

**Statut : validé.**

Décisions prises :
- Disposition Maven standard : pom parent (agrégateur) à la racine, modules `mightylib/` et `demo/`, sources en `src/main/java`, tests en `src/test/java`.
- `resources/` reste à la racine ; le jeu se lance depuis la racine (comme `assets/` d'un projet libGDX). Le tri moteur / démo est le chantier suivant (`later.md`, « Bibliothèque réutilisable »).
- Les tests de la bibliothèque ne lisent plus `resources/` : leurs fichiers sont copiés dans les ressources de test du module et lus par le classpath.
- Natives LWJGL déclarées par l'application (la démo) ; la bibliothèque ne déclare que les jars Java de LWJGL, et les natives en portée `test` si ses tests en ont besoin.
- Pas de JPMS (`module-info`) dans cet objectif : `ApiStatusTest` reste la garde de `INTERNAL`.
- artifactId : parent `mightylib-parent`, bibliothèque `mightylib`, démo `mightylib-demo` ; groupId `io.github.mightycode` et version `1.1-SNAPSHOT` inchangés.
- Test d'acceptation : `mvn -o -q package` depuis la racine (avec la phase `package`, le réacteur compile la démo contre le jar de la bibliothèque, pas contre `mightylib/target/classes`). La CI passe de `test` à `verify`.
- `tools/analyse.py` analyse les deux modules, résumé séparé par module ; `tools/coverage.py` couvre la bibliothèque seule.
- `SceneHistory` lit l'historique git de l'ancien et du nouveau dossier des scènes, pour que les dates d'ajout survivent au déplacement.

## Pour démarrer
- Lire : `docs/architecture.md` (« Structure », « API publique »), `docs/debug-tools.md` (sections `scenario.py`, analyse statique, couverture), `CLAUDE.md` (« Build et vérification »), `.github/workflows/ci.yml`, `pom.xml`.
- Branche de sujet depuis `dev` : `demo-module-maven`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Un seul `pom.xml`** (artifactId `Java-MightyLibrary`) : `sourceDirectory` = `src`, `testSourceDirectory` = `test`. Il contient :
  - les profils d'OS qui fixent `lwjgl.natives`, et les profils `analyse` (Error Prone + SpotBugs, sortie `target/static-analysis`) et `coverage` (JaCoCo, qui exclut `mightylibdemo/**` et le code lié à GL) ;
  - `maven-assembly-plugin` (jar-with-dependencies) et `exec-maven-plugin`, tous deux sur `io.github.mightycode.mightylibdemo.main.Main` ;
  - une `<resource>` qui copie `resources/` dans `target/resources`.
- **Dépendances** : opensimplex et jlayer ne servent qu'à la bibliothèque ; JBox2D, JOML et org.json servent aux deux. La bibliothèque utilise STB (`OggDecoder`) et `MemoryUtil` (`PcmBuilder`) : les tests des décodeurs ont besoin des natives.
- **Package-private** : la démo est déjà dans d'autres packages, donc ses accès package-private sont déjà refusés à la compilation. Le découpage prouve autre chose : la bibliothèque compile et se teste sans la démo, son pom déclare tout ce qu'il faut, et la démo ne voit que l'artefact. Il ne cache pas `INTERNAL` (JPMS ne filtrerait que par package, et des types `INTERNAL` sont dans des packages `STABLE`).
- **Liens actuels entre bibliothèque et démo** :
  - `test/.../mightylib/ApiStatusTest.java` : `DEMO_SOURCES = Paths.get("src/io/github/mightycode/mightylibdemo")`. La bibliothèque vérifie que la démo n'utilise aucun type ni package `INTERNAL`.
  - Tests de la bibliothèque qui lisent `resources/` (dossier de travail = racine) : `FontFileTest` (`fonts/missing.fnt`, absent volontairement), `FlacStreamTest`, `SoundDataEstimateTest`, `SoundDecodersTest`, `SoundDecodingTest` (`sounds/selectogg.ogg`, `selectwav.wav`, `selectflac.flac`, `missing.ogg`, absent volontairement), `SilentSoundServiceTest` (`sounds/sounds.gaintree`), `TextureDataLoaderTest` (`textures/world/container2.png`). Le dossier de travail de Surefire devient celui du module après le découpage.
  - `mightylibdemo/scenes/SceneHistory.java` : `SCENES_DIRECTORY = "src/io/github/mightycode/mightylibdemo/scenes/"`, `git log --reverse -M -- <dossier>`. Avec un seul chemin, le nouveau dossier ne verrait que le commit de déplacement : toutes les scènes prendraient sa date d'ajout. Tests : `SceneHistoryTest`.
- **Tests de la démo**, à déplacer : `DemoScenesTest`, `SceneChoicesTest`, `SceneHistoryTest`, `LayeredTextureLoaderTest`.
- **Outils** :
  - `tools/scenario.py` : `target/classpath.txt` produit par `dependency:build-classpath`, puis `target/classes` + ce classpath, lancé depuis la racine.
  - `tools/coverage.py` : `target/site/jacoco/jacoco.xml`.
  - `tools/analyse.py` : `target/static-analysis`, et le préfixe `"src/" + sourcepath` des constats SpotBugs.
- **CI** (`.github/workflows/ci.yml`) : `mvn -B test`.
- **IntelliJ** : `.idea/compiler.xml` est commité et nomme le module `Java-MightyLibrary`. `.gitignore` contient `src/META-INF/` ; `target/` y est déjà ignoré à toute profondeur.

## Décisions
- Pom parent à la racine (`packaging` `pom`, `modules` `mightylib` puis `demo`) : propriétés de versions, `dependencyManagement` (BOM LWJGL et versions des autres dépendances), `pluginManagement`, profils d'OS (`lwjgl.natives`) et profils `analyse`/`coverage`. Chaque module ne déclare que ce qu'il utilise.
- `mightylib/pom.xml` : jar léger (aucune dépendance embarquée), sans `mainClass`, sans assembly. Natives LWJGL en portée `test` seulement (les tests de décodage de son les chargent).
- `demo/pom.xml` : dépend de `mightylib` (`${project.version}`), déclare les natives LWJGL en portée `runtime` et ses propres dépendances directes (JOML, org.json, JBox2D). Il porte assembly et exec (`mainClass` de la démo).
- La `<resource>` qui copie `resources/` dans `target/resources` disparaît si rien ne la lit (le jeu lit `resources/` depuis la racine) ; à vérifier au début de l'étape 4.
- Les fichiers de test de la bibliothèque sont lus par le classpath, au moyen d'une petite aide de test (ex. `TestFiles.path("sounds/selectogg.ogg")`, `Path` obtenu depuis l'URL de la ressource), donc indépendants du dossier de travail de Maven comme d'IntelliJ. Les fichiers absents volontairement (`missing.*`) se résolvent depuis le dossier des fichiers de test.
- La vérification « la démo n'utilise rien d'`INTERNAL` » passe dans les tests de la démo, qui lisent leurs propres sources. Les annotations `@API` se lisent par réflexion sur les classes de la bibliothèque (rétention `RUNTIME`) ; on ne crée pas de test-jar de `mightylib`. `ApiStatusTest` garde ses autres vérifications.
- `SceneHistory` reçoit la liste de ses dossiers (le nouveau, puis les anciens) et lance `git log -M -- <tous>` : le renommage est vu, car les deux côtés sont dans les chemins demandés. Les chemins rendus sont ceux du dossier actuel.
- Commit de déplacement : les fichiers Java déplacés gardent un contenu identique (sauf les constantes de chemin), pour que git détecte les renommages à 100 %.

## Étapes
1. **`SceneHistory` sur plusieurs dossiers** (avant le déplacement, fusionnable seule).
   - Fichiers : `SceneHistory`, `SceneHistoryTest`.
   - La liste ne contient d'abord que le dossier actuel.
   - Test : un journal `--name-status` avec un renommage `R100 ancien → nouveau` garde la date d'ajout du premier commit ; le statut porcelain du nouveau dossier est bien lu.
2. **Vérification `INTERNAL` de la démo dans ses propres tests** (avant le déplacement, fusionnable seule).
   - Nouveau test côté démo (ex. `test/.../mightylibdemo/DemoApiUsageTest`), qui reprend la lecture des sources (imports, imports statiques et `*`, noms qualifiés) et lit `@API` par réflexion.
   - `ApiStatusTest` perd `DEMO_SOURCES` et cette partie.
   - Vérifier qu'il échoue sur un import `INTERNAL` ajouté exprès, puis le retirer.
3. **Fichiers de test de la bibliothèque par le classpath** (avant le déplacement, fusionnable seule).
   - Copier les fichiers listés plus haut dans `test-resources/` (déclaré en `testResources` du pom actuel), ajouter l'aide `TestFiles`, puis faire passer les 7 tests par elle.
   - `licence.md` : les fichiers copiés ont la même origine que leurs originaux (vérifier qu'ils y figurent).
   - `mvn -o -q test` vert.
4. **Découpage en deux modules** (le cœur ; dépend de 1 à 3).
   - Pom parent + `mightylib/pom.xml` + `demo/pom.xml` comme décrit plus haut.
   - Déplacements par `git mv` : `src/.../mightylib` → `mightylib/src/main/java/...`, `test/.../mightylib` → `mightylib/src/test/java/...`, `test-resources/` → `mightylib/src/test/resources/`, `src/.../mightylibdemo` → `demo/src/main/java/...`, `test/.../mightylibdemo` → `demo/src/test/java/...`.
   - Constantes de chemin à mettre à jour :
     - `SceneHistory` : nouveau dossier `demo/src/main/java/io/github/mightycode/mightylibdemo/scenes/`, puis l'ancien `src/...`.
     - Test `INTERNAL` de la démo : il lit ses sources depuis le dossier du module.
   - Natives : runtime dans la démo, test dans la bibliothèque.
   - Autres fichiers : `.gitignore` (`src/META-INF/` → le chemin équivalent, ou retiré s'il ne sert plus), `.idea/compiler.xml` (noms des modules, ou laisser IntelliJ le régénérer), CI `mvn -B verify`.
   - Vérifier `mvn -o -q compile`, `mvn -o -q test` et `mvn -o -q package` depuis la racine : la démo est compilée contre `mightylib/target/mightylib-1.1-SNAPSHOT.jar`, ce qui se voit avec `-X` ou en supprimant `mightylib/target/classes` après le jar.
   - Vérifier que `SceneHistory` rend toujours les anciennes dates d'ajout : commande ou tri du menu en jeu.
5. **Outils**.
   - `scenario.py` : classpath = `demo/target/classes` + `mightylib/target/classes` + dépendances. Produire le classpath dans le réacteur, par exemple `-o -q compile dependency:build-classpath -pl demo -am` avec un `-Dmdep.outputFile` absolu : le module démo écrit en dernier, vérifier que le fichier final contient bien le classpath de la démo. Garder la règle de régénération (date du pom → des trois poms). Lancement toujours depuis la racine.
   - `coverage.py` : `-pl mightylib`, rapport `mightylib/target/site/jacoco/jacoco.xml` ; l'exclusion `mightylibdemo/**` devient inutile.
   - `analyse.py` : les deux modules, sorties `*/target/static-analysis`, préfixe de chemin par module, résumé séparé par module.
   - Vérifier `python tools/scenario.py "scene Scene2D" wait-loaded "?gl summary"`, un `leakcheck Scene2D`, `python tools/coverage.py` et `python tools/analyse.py`.
6. **Documents** (voir plus bas), sans supprimer ce plan.

## Démo
Pas de nouvelle scène : le test d'acceptation est la construction elle-même (`mvn package` de la démo contre le jar). Toutes les scènes doivent rester lancées par `scenario.py` comme avant, scènes 3D comprises (`Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D`).

## Documents à mettre à jour à la fin
- `architecture.md` : « Structure » (pom parent, `mightylib/`, `demo/`, `resources/` à la racine, ressources de test) ; « API publique » (la vérification de la démo est dans ses tests) ; menu de la démo (`SceneHistory` suit l'ancien dossier).
- `debug-tools.md` : chemins de `scenario.py`, `analyse.py` et `coverage.py` (sorties par module, `pom.xml` → pom parent / module), commande d'acceptation `mvn -o -q package`.
- `CLAUDE.md` :
  - « Build et vérification » : modules et dispositions, `mvn -o -q package` pour vérifier la démo contre le jar.
  - Phrase sur les tests : sources de test dans `mightylib/src/test/java` et `demo/src/test/java` ; fichiers de test par le classpath.
  - Liste des tests couverts : `SceneHistory` (anciens dossiers), test `INTERNAL` de la démo.
  - Point d'entrée et dossier de travail.
- `decisions.md` : natives déclarées par l'application et pourquoi ; pas de JPMS tant que des types `INTERNAL` sont dans des packages `STABLE`.
- `later.md` (« Bibliothèque réutilisable ») :
  - Retirer « Deux modules Maven… : `current.md` ».
  - Ajouter un pom parent ou un « starter » pour qu'un jeu reçoive les natives et leurs profils sans les recopier.
  - Préciser JPMS : déplacer d'abord les types `INTERNAL` dans des packages à eux (ex. `….internal`).
  - Ressources : les ressources du moteur vont dans `mightylib/src/main/resources`.
- `licence.md` : si les fichiers de test copiés ou les poms changent quelque chose.
- `README.md` : commandes de construction et de lancement s'il en donne.
- `current.md` : supprimer la puce. Le fichier ne garde que son en-tête, donc le point « Philosophie du moteur » est fini : discuter du point suivant de `later.md`.
- Supprimer ce plan (par `/pr-ml`).

## Hors périmètre
- Ressources du moteur lues depuis le classpath, et paquet de démonstration à part : `later.md`, « Bibliothèque réutilisable ».
- JPMS, distribution (JitPack, GitHub Packages), jars de sources et de Javadoc, japicmp : `later.md`, même section.
- Pom « starter » des natives pour les jeux : ajouté à `later.md` par l'étape 6.

## Questions restantes
Aucune.

## Modèles recommandés
- Discussion A, étapes 1 à 4 : **Opus 5.5, effort high**. Les poms et le découpage décident de la forme de la bibliothèque pour la suite ; il faut aussi lire l'historique git de `SceneHistory` et chercher les pièges de résolution du réacteur.
- Discussion A, étapes 5 et 6 : **Sonnet 5.5, effort medium** (chemins des outils et docs, mécanique).
- Discussion B (`/pr-ml`) : **Opus 5.5, effort medium**. Le diff est surtout fait de renommages ; les poms et les outils demandent une lecture attentive.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/demo-module-maven.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `demo-module-maven`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur :
  - IntelliJ : réimporter le projet Maven, puis vérifier la configuration de lancement (module `mightylib-demo`, dossier de travail = racine) et le lancement des tests depuis l'IDE ;
  - le menu de la démo trié par date d'ajout (dates d'avant le déplacement) ;
  - une scène 3D (`Scene3D`).
  
  La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml demo-module-maven`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Vérifications manuelles du « Bilan » : réimport IntelliJ et configuration de lancement, tri du menu par date d'ajout, une scène 3D.

## Bilan
### Fait
- Étape 1 (`ab50f87`) : `SceneHistory.SCENES_DIRECTORIES` (dossier actuel, puis anciens), `git log`/`git status` sur tous ; tests du renommage `R100` depuis un ancien dossier et du statut lu après déplacement.
- Étape 2 (`807382e`) : `DemoApiUsageTest` (démo) reprend la lecture des sources et lit `@API` par réflexion ; `ApiStatusTest` ne garde que ses deux autres vérifications. Un import de `ConsoleLog` ajouté exprès dans la démo le fait échouer (vérifié, puis retiré).
- Étape 3 (`1091cb9`) : fichiers de test copiés (`sounds/select*`, `sounds.gaintree`, `textures/world/container2.png`), aide `TestFiles` (`path`, `file`), les 7 tests passent par elle ; `licence.md` le signale.
- Étape 4 (`89ac4b2`) : pom parent `mightylib-parent`, modules `mightylib` et `mightylib-demo`, déplacements par `git mv` (670 renommages, tous à 100 % sauf `SceneHistory`, `DemoApiUsageTest` et `DemoScenesTest`, les trois fichiers qui portent des chemins). CI en `mvn -B verify`, `.idea/compiler.xml` renommé.
- Étape 5 (`1bf7c52`) : `scenario.py`, `coverage.py` et `analyse.py` adaptés.
- Étape 6 : documents ; et les scripts des skills `revue-pratiques/collect.py` et `analyse-intellij/summarize.py`, qui lisaient `src/` et `test/`.

### Écarts au plan, et pourquoi
- **Jar `tests` de `mightylib`, limité aux `*TestAccess`** (décidé avec l'utilisateur pendant l'implémentation) : `LayeredTextureLoaderTest` (démo) utilise `MainThreadTestAccess` et `WorkersTestAccess`, des sources de test de la bibliothèque. La démo en dépend en portée `test`. Le plan excluait un test-jar seulement pour la lecture de `@API`. Décision dans `decisions.md`, kit de test pour les jeux dans `later.md`.
- **`test-compile` au lieu de `compile`** pour le classpath de `scenario.py`, et **`process-test-classes` au lieu de `process-classes`** pour `analyse.py` : dans le réacteur de Maven 3, le jar `tests` ne se résout qu'une fois les tests de la bibliothèque compilés, et `dependency:build-classpath` comme SpotBugs résolvent les dépendances de test. `analyse.py` ignore les avertissements d'Error Prone sur les tests. Le classpath du jeu est pris en portée `runtime` (ni JUnit ni classes de test).
- **Filtre SpotBugs par `${maven.multiModuleProjectDirectory}`** : le pom parent (packaging `pom`) lance aussi SpotBugs, un chemin relatif au module ne lui convient pas. Les commandes se lancent donc depuis la racine (déjà la règle).
- **`DemoScenesTest`** lit les sources depuis le dossier du module : il retire `demo/` de `SceneHistory.SCENES_DIRECTORY` et vérifie ce préfixe, ce qui garde les deux chemins liés.
- **`src/META-INF/MANIFEST.MF` supprimé** (et sa ligne de `.gitignore`) : un manifeste d'artefact IntelliJ qui ne servait plus (aucun `.idea/artifacts`, l'assembly de la démo donne déjà `Main-Class`).
- **`<resource>` vers `target/resources` retirée** : rien ne la lisait (le jeu lit `resources/` depuis la racine).
- Natives de la bibliothèque en `test` : seulement `lwjgl` et `lwjgl-stb` (les seuls chargés par ses tests). Versions des plugins jar (3.4.1) et assembly (3.7.1) fixées dans le pom parent (avertissement de Maven sinon).

### Vérifié, et comment
- `mvn -o -q compile`, `mvn -o -q test` (1007 tests de la bibliothèque, 28 de la démo) et `mvn -o package` depuis la racine. Avec `-X`, le classpath de test de la démo contient `mightylib-1.1-SNAPSHOT.jar` et `mightylib-1.1-SNAPSHOT-tests.jar`, pas `mightylib/target/classes`. Le jar `tests` ne contient que les 4 classes `*TestAccess`.
- Les tests de la bibliothèque passent avec `resources/sounds` et `container2.png` retirés temporairement de `resources/` (fichiers bien lus par le classpath).
- Dates des scènes après le déplacement (même lecture que `SceneHistory`, sur l'historique réel) : 37 scènes, aucune n'a la date du commit de déplacement, et l'ancien dossier ne laisse rien.
- `scenario.py` : `Scene2D` (`gl summary`), `leakcheck Scene2D` (pas de fuite), `Scene3D`, `Scene3DBis`, `Game3D2D`, `TerrainGeneration3D` et `MenuScene` chargées, 0 ligne de problème dans le journal.
- `python tools/coverage.py` (bibliothèque seule) et `python tools/analyse.py` (mightylib : 81 SpotBugs, 189 Error Prone ; demo : 11 et 53).

### À vérifier à la main par l'utilisateur
- IntelliJ : réimporter le projet Maven (trois modules : `mightylib-parent`, `mightylib`, `mightylib-demo`). La configuration de lancement doit utiliser le module `mightylib-demo`, le dossier de travail = la racine du dépôt (sinon `resources/` est introuvable). Lancer les tests depuis l'IDE (dossier de travail = celui du module, la valeur par défaut avec Maven).
- Le menu de la démo trié par date d'ajout (Tab ou « Sort ») : les dates d'avant le déplacement (`Scene2D` parmi les plus anciennes, `LayeredTexture2D` et `EngineServices2D` le 2026-11-15).
- Une scène 3D (`Scene3D`) à l'œil.
