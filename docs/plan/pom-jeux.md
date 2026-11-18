# Poms pour les jeux : BOM, parent commun, parent des jeux

**Statut : validé.**

Décisions prises :
- Un **parent pour les jeux** (`mightylib-game-parent`) et un **BOM** (`mightylib-bom`) importable par un jeu qui a déjà son propre parent.
- Chaîne sans doublon : `mightylib-bom` ← `mightylib-base-parent` ← `mightylib-game-parent` ← démo et jeux ; `mightylib-base-parent` ← `mightylib-parent` (racine, développement) ← `mightylib`.
- La démo hérite du parent des jeux : elle le valide à chaque build.
- Six plateformes reconnues : windows, windows-arm64, linux, linux-arm64, macos, macos-arm64 (macOS non testé).
- Natives : celles de la machine par défaut ; un profil `all-natives` les ajoute toutes (jar multiplateforme). Le choix fin par plateforme livrée revient à l'objectif « Distribution ».
- Le profil `analyse` (Error Prone + SpotBugs) vit dans le parent commun : la bibliothèque, la démo et tout jeu en disposent.
- Le parent des jeux configure `exec`, le `Main-Class` et le `jar-with-dependencies` à `package`, à partir de la propriété `mightylib.mainClass`.
- Pas de générateur de projet (archetype) dans cet objectif.

## Pour démarrer
- Lire : `docs/architecture.md`, « Structure » et la fin de « Démarrage » (lancement sur le classpath et en module) ; `docs/debug-tools.md`, la section sur `tools/analyse.py` ; `pom.xml`, `mightylib/pom.xml`, `demo/pom.xml` ; `tools/analyse.py` (lance `-P analyse process-test-classes` depuis la racine, puis résume par module) et `tools/scenario.py` (`run_maven(... "dependency:build-classpath", "-pl", "demo", "-am")`).
- Branche de sujet depuis `dev` : `feature/game-poms`.
- Compiler et tester avec les commandes de `CLAUDE.md` (`compile`, `test`, et ici surtout `package`) ; vérifier en jeu avec `tools/scenario.py`, en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `pom.xml` (racine, `mightylib-parent`, `1.1-SNAPSHOT`) mélange :
  - ce dont un jeu a besoin : les propriétés de versions, le `dependencyManagement` (BOM LWJGL importé, `mightylib`, son `test-jar`, json, joml, jbox2d, opensimplex, jlayer, apiguardian, junit), `maven.compiler.release` 11, l'encodage UTF-8, le `pluginManagement` (surefire 3.2.5 avec `useModulePath=false`, jar 3.4.1, assembly 3.7.1, exec 1.2.1), maven-compiler-plugin 3.13.0 avec l'exécution `default-testCompile` sur le classpath, et deux profils d'OS (windows-amd64, linux-amd64) qui fixent `lwjgl.natives` ;
  - l'outillage du moteur : le profil `analyse` (Error Prone et SpotBugs ; `excludeFilterFile` = `${maven.multiModuleProjectDirectory}/tools/spotbugs-exclude.xml` ; la liste `-Xep:…:OFF` justifiée par le code du moteur), le profil `coverage` (JaCoCo, exclusions propres aux paquets du moteur), et les options `-Xlint:-exports` et `-Xlint:-requires-automatic` (propres au module du moteur, reprises par IntelliJ depuis le commit d95f162).
- `mightylib/pom.xml` : dépendances Java de LWJGL ; natives `lwjgl` et `lwjgl-stb` en portée `test` (avec `${lwjgl.natives}`) ; jar `tests` (`*TestAccess`).
- `demo/pom.xml` : déclare à la main les natives des 6 modules LWJGL (`lwjgl`, `assimp`, `glfw`, `openal`, `opengl`, `stb`) en portée `runtime` ; `maven-assembly-plugin` (`jar-with-dependencies`, `mainClass`) ; `exec-maven-plugin` (`mainClass`, arguments `-f false`).
- Défaut : un jeu hors du dépôt doit recopier les profils d'OS, les natives et la configuration des plugins de `demo/pom.xml`, et le parent actuel lui imposerait l'outillage du moteur.
- Modèle extérieur (Spring Boot) : `spring-boot-dependencies` (le BOM) est le **parent** de `spring-boot-starter-parent` ; un projet qui a déjà un parent importe le BOM et n'a que les versions. En Maven 3, seul un parent transmet les profils, les propriétés et les plugins. Un BOM du même dépôt utilisé comme **parent** (`relativePath`) se résout sans `install` ; un BOM **importé** depuis le même réacteur peut exiger un `install` préalable, ce qui casserait `-o` : c'est pour cela que la chaîne passe par des parents.
- LWJGL 3.3.6 : classifiers `natives-windows`, `natives-windows-arm64`, `natives-linux`, `natives-linux-arm64`, `natives-macos`, `natives-macos-arm64` ; activation des profils par `<os><family>…</family><arch>…</arch></os>` (`windows`/`unix`/`mac` ; `amd64` ou `aarch64`). Un profil `unix` doit exclure mac (`<family>unix</family>` est vrai sur macOS : utiliser `<name>Linux</name>` ou l'ordre des profils de LWJGL).
- Tests : aucun test JUnit ne couvre les poms. La vérification passe par `mvn package`, `dependency:tree` et un projet jetable hors du dépôt.

## Décisions
- **Nouveaux modules du réacteur**, dossiers à la racine : `bom/` (`mightylib-bom`), `base-parent/` (`mightylib-base-parent`), `game-parent/` (`mightylib-game-parent`). La racine reste l'agrégateur et le parent de développement (`mightylib-parent`), avec pour parent `mightylib-base-parent` (`relativePath` `base-parent/pom.xml`).
- **`mightylib-bom`** (parent : aucun) : seulement les versions (propriétés et `dependencyManagement` : BOM LWJGL importé, `mightylib`, son `test-jar`, json, joml, jbox2d, opensimplex, jlayer, apiguardian, junit). Pas de plugins ni de profils : un jeu qui l'importe n'a que les versions.
- **`mightylib-base-parent`** (parent : le BOM) : `maven.compiler.release` 11 (surchargeable, par exemple 21 pour un jeu), UTF-8, `pluginManagement` (compiler, surefire avec `useModulePath=false`, jar, assembly, exec), `default-testCompile` sur le classpath, les **six profils d'OS** (propriété `lwjgl.natives` seule : les tests de la bibliothèque s'en servent), le **profil `analyse`**. Ce que le profil `analyse` a de propre au moteur devient une propriété avec une valeur par défaut neutre :
  - `mightylib.errorprone.args`, pour la liste `-Xep:…:OFF` ;
  - `mightylib.spotbugs.exclude`, pour le fichier d'exclusions.

  `mightylib-parent` les fixe aux valeurs actuelles pour la bibliothèque. La démo n'hérite pas de la racine : elle fixe donc les mêmes valeurs dans son propre pom, à partir du fichier `tools/spotbugs-exclude.xml` du dépôt.
- **`mightylib-game-parent`** (parent : `mightylib-base-parent`) :
  - les natives de la machine des 6 modules LWJGL du moteur, en portée `runtime` avec `${lwjgl.natives}` ;
  - le profil `all-natives` : les 6 modules × 6 classifiers, en `runtime` ;
  - `exec` (`mainClass` = `${mightylib.mainClass}`) ;
  - `maven-assembly-plugin` (`jar-with-dependencies`, `Main-Class` = `${mightylib.mainClass}`, à `package`).

  Il ne déclare pas `mightylib` lui-même : le jeu le déclare (version venue du BOM). Un module LWJGL ajouté par un jeu (`lwjgl-nfd`…) se déclare dans son propre pom, avec `${lwjgl.natives}`.
- **`mightylib-parent`** (racine, développement) garde le profil `coverage`, les options `-Xlint` du module du moteur et les valeurs des propriétés d'analyse du moteur. `mightylib` en hérite toujours (son pom publié le référence : la racine, le parent commun et le BOM sont publiés avec lui).
- **Versions** : tous les poms à `1.1-SNAPSHOT`, comme aujourd'hui ; versions sémantiques et `${revision}` éventuel à l'objectif « Distribution ».

## Étapes
1. **BOM et parent commun** : créer `bom/pom.xml` et `base-parent/pom.xml`, y déplacer les versions, le `pluginManagement`, `release`, l'encodage, les profils d'OS (étendus aux six plateformes) et le profil `analyse` (avec les propriétés `mightylib.errorprone.args` et `mightylib.spotbugs.exclude`). La racine a pour parent `mightylib-base-parent` et liste les nouveaux modules. `mightylib` et la démo ne changent pas.
   - Vérifier d'abord, avec un essai de quelques minutes, le comportement des parents par `relativePath` en `-o`, et ce que fait SpotBugs d'un `excludeFilterFile` vide.
   - Puis `help:effective-pom -pl mightylib,demo` avant et après (script jetable dans `tools/scripts/`) : mêmes dépendances, mêmes plugins.
   - Lancer `compile`, `test`, `package` et `python tools/analyse.py` : mêmes nombres qu'avant, pour les deux modules.
   - Cette étape peut être fusionnée seule.
2. **Parent des jeux et démo** : créer `game-parent/pom.xml` (natives de la machine, `all-natives`, `exec`, jar complet). `demo/pom.xml` a pour parent `mightylib-game-parent` (`relativePath` `../game-parent/pom.xml`). On y retire les natives et la configuration de l'assembly, et on y fixe `mightylib.mainClass`, les arguments `exec` (`-f false`) et les propriétés d'analyse du moteur.
   - Vérifier : `package` (la démo contre le jar) ; `dependency:tree -pl demo` (natives de la machine) ; `-P all-natives package` (le jar complet contient les natives des six plateformes : un script jetable liste les `.dll`, `.so` et `.dylib`) ; `python tools/analyse.py` (les deux modules sont toujours analysés).
   - En jeu (prévenir l'utilisateur) : `python tools/scenario.py "scene Scene2D" wait-loaded "?gl summary"`, puis la même commande avec `--modulepath`, et le jar complet lancé avec `java -jar` depuis la racine.
3. **Essai hors du dépôt** (pas de commit, script dans `tools/scripts/`, non versionné) : `mvn install` (sans `-o`), puis un projet jetable dans le scratchpad. Son pom a pour parent `mightylib-game-parent` et ne déclare que `mightylib` et `mightylib.mainClass`, plus une classe `Main` qui crée un `EngineConfig` avec une scène 2D vide. Le lancer : `mvn -o package`, puis `java -jar`. Refaire l'essai avec un pom qui importe seulement `mightylib-bom` : il compile, mais sans les natives ni les plugins, ce qui est attendu. Cet essai prépare le test d'acceptation ; il ne le remplace pas.
4. **Docs** (liste plus bas), et IntelliJ : réimporter Maven et vérifier que `.idea/compiler.xml` garde les options `-Xlint` de la bibliothèque (commit d95f162). Commiter les fichiers `.idea` partagés s'ils changent, jamais `workspace.xml`.

## Démo
Pas de nouvelle scène : la démo est elle-même le jeu d'exemple du parent. Le `README.md` gagne un pom minimal de jeu (parent, dépendance `mightylib`, `mightylib.mainClass`) et la variante avec le BOM.

## Documents à mettre à jour à la fin
- `architecture.md`, « Structure » : la chaîne des cinq poms, ce que fixe chacun, les propriétés (`mightylib.mainClass`, `lwjgl.natives`, `mightylib.errorprone.args`, `mightylib.spotbugs.exclude`), le profil `all-natives` ; la démo hérite du parent des jeux. Au présent.
- `debug-tools.md` : `analyse` est dans le parent commun (un jeu peut lancer `mvn -P analyse process-test-classes`) ; `-P all-natives`.
- `decisions.md` : la chaîne de parents plutôt qu'un BOM importé ou un starter (profils et plugins transmis seulement par un parent, résolution par `relativePath` sans `install`) ; natives de la machine par défaut.
- `later.md` : un générateur de projet (archetype Maven) à partir du projet du test d'acceptation ; pour « Distribution », macOS a besoin de `-XstartOnFirstThread`, ce qu'`exec:java` (même JVM) ne permet pas : il faudra `exec:exec` ou un lanceur ; les mixins de Maven 4 pourraient simplifier la chaîne.
- `current.md` : supprimer la ligne « Pom starter » ; dans « Test d'acceptation », remplacer « pom starter » par `mightylib-game-parent` ; dans « Kit de test », rappeler que `useModulePath=false` est déjà dans le parent commun.
- `CLAUDE.md` : la phrase « deux modules Maven sous un pom parent » et la ligne `package` de « Build et vérification » (mentionner `bom`, `base-parent`, `game-parent`).
- `licence.md` : rien, car aucune dépendance n'est ajoutée (le vérifier).
- `README.md` : le pom minimal d'un jeu.
- Supprimer ce plan (discussion B).

## Hors périmètre
- Générateur de projet (archetype) : `later.md`.
- Publication (dépôt, numéros de version, japicmp), `jlink`, `jpackage`, choix des natives par plateforme livrée, lancement sur macOS : objectif « Distribution » de `current.md`.
- Kit de test pour les jeux : objectif de `current.md`.
- Projet de jeu complet hors du dépôt : objectif « Test d'acceptation ».

## Questions restantes
- Valeur par défaut de `mightylib.spotbugs.exclude` pour un jeu sans fichier d'exclusions : vide, si SpotBugs l'accepte (vérifié à l'étape 1), sinon un fichier vide livré dans le jar de `mightylib-base-parent` ou dans un dossier standard du jeu (`config/spotbugs-exclude.xml`). Le trancher avec l'utilisateur si l'essai échoue.

## Modèles recommandés
- Discussion A, étapes 1 à 3 : Opus 5.5, effort `high` (restructuration Maven délicate : héritage, profils d'OS, résolution hors ligne, effective-pom à comparer).
- Discussion A, étape 4 (docs) : Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`) : Opus 5.5, effort `high` (relire cinq poms au regard du plan et vérifier qu'aucune configuration n'est perdue).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/pom-jeux.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/game-poms`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (réimport Maven dans IntelliJ, lancement de la démo depuis IntelliJ, `java -jar` du jar complet, build sur un Mac ou une machine ARM si l'utilisateur en a une). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/game-poms`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan » : réimport Maven et lancement depuis IntelliJ, `java -jar` du jar complet, build sur Mac ou ARM si possible.

## Bilan (discussion A)
**Fait** : les étapes 1 à 4. `bom/`, `base-parent/`, `game-parent/` créés ; la racine hérite de `base-parent`, la démo de `game-parent`. Docs mises à jour (architecture, debug-tools, decisions, later, current, CLAUDE.md, README) ; `licence.md` inchangé (aucune dépendance ajoutée : mêmes artefacts, seules les natives sont déclarées ailleurs). `tools/scenario.py` surveille maintenant tous les poms du réacteur pour régénérer `target/classpath.txt`.

**Écarts au plan et raison**
- Jar complet dans un profil `game-jar` (activé si `${basedir}/src/main` existe) et non directement dans `build` : sinon l'assembly tournait aussi sur `game-parent` (packaging pom) et produisait un jar de natives inutile.
- Versions des natives écrites `${lwjgl.version}` dans `game-parent` : avec `-P all-natives`, les natives de la machine sont déclarées deux fois et Maven ne donne la version gérée qu'à l'une des deux (erreur « version missing »).
- `mightylib.version` écrit en dur dans le BOM : `${project.version}` y vaudrait la version du jeu qui l'hérite.
- `<name>Linux</name>` ajouté au profil d'OS Linux (`unix` est aussi vrai sur macOS).
- Question restante tranchée : SpotBugs accepte un `excludeFilterFile` vide ; défaut vide.
- `.idea/compiler.xml` : IntelliJ ne donne plus d'options `-Xlint` à `mightylib-demo` (elles viennent du pom racine, dont la démo n'hérite plus) ; `.idea/encodings.xml` : entrées des nouveaux modules.

**Vérifié** : `help:effective-pom` avant/après (mêmes dépendances et plugins pour `mightylib` ; la démo gagne les natives et l'assembly du parent) ; `mvn -o package` (1094 + 27 tests) ; `python tools/analyse.py` (mêmes nombres : mightylib 0 SpotBugs / 3 Error Prone, démo 0 / 0) ; parents résolus hors ligne sans `install` ; `dependency:tree -pl demo` (natives de la machine) ; `-P all-natives` (jar complet : natives des six plateformes) ; `scenario.py "scene Scene2D"` sur le classpath et avec `--modulepath` ; `java -jar` du jar complet ; jeu jetable hors du dépôt (parent `mightylib-game-parent` : `mvn -o package` puis `java -jar`) et variante BOM seule (compile, sans natives ni jar complet, comme attendu). Script de l'essai : `tools/scripts/game-poms-trial/` (non versionné).

**À vérifier à la main** : réimport Maven dans IntelliJ (modules `mightylib-bom`, `-base-parent`, `-game-parent` apparaissent, les options `-Xlint` de `mightylib` restent dans `.idea/compiler.xml`) ; lancement de la démo depuis IntelliJ ; `java -jar demo/target/mightylib-demo-1.1-SNAPSHOT-jar-with-dependencies.jar` depuis la racine ; build sur un Mac ou une machine ARM (profils d'OS et natives non testés).
