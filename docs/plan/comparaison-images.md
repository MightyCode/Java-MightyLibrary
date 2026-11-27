# Comparaison d'images et test d'acceptation du rendu

**Statut : validé.**

Ce plan couvre deux des trois points de `docs/current.md` : la comparaison d'images et le test d'acceptation. La couverture réelle a son propre plan.

Décisions prises :
- Métrique : un pixel est différent si l'un de ses canaux R, G ou B s'écarte de plus de N niveaux. L'échec dépend de la part de pixels différents.
- Tolérance par défaut stricte : ±2 niveaux, 0 pixel différent. Un test peut l'élargir, en donnant sa raison.
- Références : une série par clé de pilote = OS + famille du pilote (`linux-mesa-llvmpipe`, `windows-nvidia`…). La clé est lue de `GL_VENDOR`/`GL_RENDERER` et peut être surchargée.
- Capture : l'image finale de la fenêtre (tampon arrière après l'affichage, avant l'échange), en mémoire et de façon synchrone.
- Scènes de référence dédiées et figées, en 640×360 :
  - sprites, animation et tuiles 2D ;
  - texte et formes 2D ;
  - modes de couleur, avec une correction `picture` non neutre ;
  - une scène 3D.
- API publique dans le kit `mightylib-test` (statut `EXPERIMENTAL`). `BatchPictures` passe dessus.
- Régénération :
  - la propriété `-Dmightylib.test.updateReferences=true` ;
  - l'outil `tools/references.py`, dont l'option `--from-ci` récupère la série Mesa depuis l'artefact du CI.
- Runner du CI fixé à `ubuntu-24.04`.

## Pour démarrer
- Lire :
  - `docs/architecture.md`, sections « Kit de test (`mightylib-test`) », « Boucle » et « Rendu » (passe finale de la fenêtre, `VirtualSceneRenderer`, correction `picture`) ;
  - `docs/debug-tools.md`, `screenshot` et « Comparer des rendus » ;
  - `docs/decisions.md` : le lot 2D (`off` = référence du rendu).
- Code à lire :
  - `mightylib/src/main/java/io/github/mightycode/mightylib/graphics/utils/Screenshot.java` ;
  - `Context.java:267` (appel de `Screenshot.captureIfRequested`) ;
  - `mightylib-test/src/main/java/io/github/mightycode/mightylib/test/SceneRunner.java`, `TestEngine.java` ;
  - `mightylib-test/src/test/java/io/github/mightycode/mightylib/test/BatchPictures.java`, `GLContextTest.java` ;
  - `demo/src/test/java/io/github/mightycode/mightylibdemo/main/DemoTestConfig.java`, `scenes/SmokeCases.java`, `DemoScenesSmokeGLTest.java` ;
  - `.github/workflows/ci.yml`.
- Branche de sujet : `feature/image-comparison`, depuis `dev`.
- Compiler et tester avec les commandes de `CLAUDE.md`. Vérifier en jeu avec `tools/scenario.py`, en prévenant l'utilisateur : une fenêtre s'ouvre chez lui.

## Contexte : ce qui existe

**`Screenshot`**
- Lit `GL_BACK` de la fenêtre à la fin de son affichage, depuis `Context`, avant `swapBuffers`.
- Encode le PNG sur un `new Thread`, ce qui enfreint la règle « `Workers` pour une tâche courte ».
- Aucune lecture en mémoire n'est offerte aux tests.
- Elle sert la commande `screenshot` (`--rect`, `--corners`, `--scale`).

**`BatchPictures` (tests GL du kit)**
- Dessine dans un FBO à la taille virtuelle et le relit.
- Compare deux images canal par canal, avec un écart permis de 1 et au plus `capacité / 2000` canaux différents.
- Sert à vérifier que les modes du lot rendent comme `off`.
- C'est le germe de la comparaison, mais il reste privé, ne compare pas à une référence enregistrée et n'écrit pas d'image en cas d'échec.

**Kit (`mightylib-test`, `EXPERIMENTAL`)**
- `TestEngine` : temps manuel, fenêtre cachée, dossier du joueur temporaire (donc une correction `picture` neutre par défaut).
- `SceneRunner` : `start`, `untilLoaded`, `frames`, `execute` (commandes `camera`, `colorspace`, `picture`…), entrées simulées.
- Un moteur par configuration. La taille de fenêtre et la taille virtuelle viennent de l'`EngineConfig` (`setWindowSize`, `setVirtualSize`).

**Démo**
- `DemoTestConfig` = la configuration du jeu, avec les ressources lues à la racine.
- Le test de fumée charge deux fois chaque scène de `DemoScenes` (les nouvelles scènes de référence y passeront d'office).

**CI**
- `ubuntu-latest`, Xvfb, Mesa llvmpipe.
- Lancé par `mvn -B verify -Dmightylib.test.requireGL=true -Dmightylib.test.requireDemoAssets=true`.
- Aucun artefact n'est publié.

**Défaut** : un rendu changé ne se voit qu'à la main, en comparant des captures de `scenario.py`.

**Écueils connus**
- La relecture du tampon arrière d'une fenêtre cachée n'est pas garantie par la spécification (test de propriété des pixels). Mesa sous Xvfb la permet ; certains pilotes Windows peut-être pas : il faut un test qui le détecte.
- Le canal alpha du tampon arrière n'a pas de valeur définie : ne comparer que R, G et B.
- La mise à l'échelle DPI peut donner un framebuffer d'une autre taille que la fenêtre : la capture vérifie sa taille.
- Il est courant que llvmpipe et un pilote matériel diffèrent de 0,01 à 0,24 % des pixels. C'est pourquoi on garde une série par pilote, plutôt qu'une tolérance large commune.

## Décisions
1. **Capture en mémoire depuis la bibliothèque.**
   - `Screenshot` est découpé en deux :
     - une demande interne de pixels par fenêtre, lus au même point qu'aujourd'hui et rendus en mémoire (largeur, hauteur, RGBA de haut en bas) ;
     - l'encodage PNG de la commande `screenshot`, qui passe sur `Workers` au lieu de `new Thread`.
   - Le comportement de la commande ne change pas.
   - Le kit accède à la demande par un pont du kit, comme `KitEngineAccess`.
2. **`CapturedImage` (kit, public)** : largeur, hauteur, pixels RGB de haut en bas, `rgb(x, y)`, `crop(x, y, l, h)`, `writePng(Path)`, et la lecture d'un PNG (`CapturedImage.read(Path | InputStream)`).
3. **`SceneRunner.capture()`** : affiche une frame et rend l'image finale de la fenêtre principale de la scène. Une surcharge prend la fenêtre (sous-fenêtres). Échec clair si la taille capturée n'est pas celle de la fenêtre ou si la relecture d'une fenêtre cachée ne marche pas sur ce pilote.
4. **Comparaison (kit, sans GL)**
   - `ImageTolerance` : écart permis par canal, pixels différents permis, en nombre ou en part. Défaut `ImageTolerance.STRICT` = ±2, 0 pixel ; `withReason(String)` est obligatoire pour en élargir une.
   - `ImageComparison` : résultat avec le nombre et la part des pixels différents, l'écart maximal, la boîte englobante des différences et l'image de différence (référence assombrie, pixels différents en rouge).
   - Images de tailles différentes = échec immédiat, avec les deux tailles dans le message.
5. **Références (kit)** : `ReferenceImages.assertMatches(CapturedImage, String nom[, ImageTolerance])`.
   - Lecture sur le classpath du test, à `references/<clé>/<nom>.png`.
   - Écriture des références sous `src/test/resources/references/<clé>/`, par rapport au dossier du module (le dossier de travail des tests). La propriété `mightylib.test.referencesFolder` peut le changer.
   - En cas d'échec, trois fichiers sont écrits dans `target/references/<clé>/` : `<nom>.actual.png`, `<nom>.expected.png` et `<nom>.diff.png`. Le message donne les chiffres, la boîte des différences et le chemin.
6. **Clé de pilote** : `RendererKey.of(os, vendor, renderer)`, fonction pure testée sans GL.
   - OS : `windows`, `linux` ou `macos`.
   - Famille :
     - `mesa-llvmpipe` si le renderer contient `llvmpipe` ;
     - `mesa-<pilote>` pour un autre pilote Mesa (`radeonsi`, `iris`…) ;
     - `nvidia`, `amd` (ATI/AMD), `intel`, `apple` ;
     - sinon le vendeur réduit à `[a-z0-9-]`.
   - La propriété `mightylib.test.referenceKey` surcharge la clé. `GLContextTest` affiche la clé de la machine.
7. **Série absente ou image absente**
   - Série de la clé absente : le test est ignoré (`Assumptions`), avec un message qui donne la clé et la commande `python tools/references.py --update`. Avec `-Dmightylib.test.requireReferences=true` (le CI), c'est un échec.
   - Série présente mais image absente : toujours un échec, avec `.actual.png` écrit (c'est un oubli de régénération).
8. **Mise à jour** (`-Dmightylib.test.updateReferences=true`)
   - Chaque assertion écrit la capture comme référence de la série locale et passe.
   - Elle écrit une ligne `--Reference updated <clé>/<nom> (N pixels différents)`, ou `unchanged` si rien n'a bougé.
   - Le CI ne l'active jamais. On relit avec `git diff` avant de commiter.
9. **`tools/references.py`**
   - `--key` donne la clé locale, en lançant `GLContextTest`.
   - `--update [--test Classe]` lance les tests de référence de la démo avec la propriété de mise à jour, puis montre `git status` des références.
   - `--from-ci [run-id]` : `gh run download` de l'artefact `references` de la dernière exécution rouge de la branche courante. Il copie les `*.actual.png` de la série `linux-mesa-llvmpipe` dans la série du dépôt.
   - Les chemins viennent de `scenario.py`.
10. **CI**
    - `runs-on: ubuntu-24.04`. Une montée de version voulue régénère la série Mesa dans le même commit.
    - `-Dmightylib.test.requireReferences=true`.
    - `actions/upload-artifact@v4` `if: failure()` nommé `references`, sur `**/target/references/**`.
11. **Scènes de référence** (démo, 640×360), sans animation qui avance, sans hasard non fixé, sans texte d'aide ni compteur.
    - Elles restent pilotables par `DemoCamera.free(this)` (règle de la démo) ; les tests posent la caméra par la commande `camera`.
    - Elles sont inscrites dans `DemoScenes`, avec une description qui dit « scène de référence des tests d'images : ne pas la modifier sans régénérer ses références ».
12. **Test de la démo `ReferenceImagesGLTest`** (tag `gl`)
    - Configuration `ReferenceTestConfig` = `DemoTestConfig` avec `setWindowSize(640, 360)` et `setVirtualSize(640, 360)`.
    - Une méthode par scène. Plusieurs vues par scène (caméra par défaut, zoom, rotation), chacune une référence `<Scène>-<vue>`.
13. **`BatchPictures` utilise la comparaison du kit.**
    - Son seuil est converti en `ImageTolerance`, avec un écart par canal de 1 et un nombre de pixels proche de l'actuel `capacité / 2000` canaux.
    - Les tests restent verts sans relâcher la tolérance.
    - Il garde son FBO : il compare deux dessins, pas l'image de la fenêtre.

## Étapes (un commit chacune)

**1. Plan.** Commiter ce plan tel quel.

**2. Capture en mémoire dans la bibliothèque**
- Fichiers :
  - `Screenshot.java` : demande de pixels interne par fenêtre. L'encodage PNG passe par `Workers` et non par `new Thread`. Le recadrage et l'échelle sont inchangés.
  - le point d'appel dans `Context` ;
  - un pont du kit (`KitGraphicsAccess`, ou un ajout à `KitEngineAccess` dans le package du moteur concerné, classpath seulement).
- Comportement : la commande `screenshot` est inchangée (`--rect`, `--corners`, `--scale`, fenêtre minimisée = échec).
- Tests : GL du kit, `ScreenshotGLTest` (pixels d'une couleur de fond connue relus, taille).
- Vérification en jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "screenshot --scale 0.5"`. Le PNG est écrit ; regarder le journal plutôt que l'image.
- Peut être fusionnée seule.

**3. `CapturedImage` et `SceneRunner.capture()`**
- Fichiers : `mightylib-test/.../test/CapturedImage.java`, `SceneRunner.java`.
- Comportement :
  - relecture d'une fenêtre cachée vérifiée : remplir de couleurs connues et relire. Si la relecture échoue, le message dit que le pilote ne relit pas une fenêtre cachée, et `GLContextTest` le signale aussi ;
  - taille vérifiée.
- Tests :
  - sans GL : `CapturedImageTest` (PNG aller-retour, `crop`, orientation haut-bas) ;
  - GL : `SceneCaptureGLTest` (une scène de test qui remplit une couleur et un rectangle placé : position du rectangle, haut en haut).
- Mettre `KitApiTest` à jour pour la nouvelle API publique.

**4. Comparaison**
- Fichiers : `ImageTolerance.java`, `ImageComparison.java` (kit) ; `BatchPictures.java` passe dessus.
- Tests sans GL dans `ImageComparisonTest` :
  - identiques ;
  - un pixel à +3 détecté en `STRICT`, à +2 accepté ;
  - part et nombre ;
  - tailles différentes ;
  - boîte englobante ;
  - image de différence (pixel rouge à la bonne place) ;
  - élargissement sans raison refusé.
- Les tests GL du lot restent verts.

**5. Références et clé de pilote**
- Fichiers : `RendererKey.java`, `ReferenceImages.java` (kit) ; `GLContextTest` affiche la clé.
- Tests sans GL :
  - `RendererKeyTest` : chaînes réelles de llvmpipe, NVIDIA, AMD, Intel Windows et Linux, Mesa radeonsi/iris, Apple, inconnu, surcharge par propriété ;
  - `ReferenceImagesTest`, dans un dossier temporaire : série absente = ignoré, série absente avec `requireReferences` = échec, image absente = échec avec `.actual.png`, différence = échec avec les trois fichiers, mise à jour = référence écrite et test vert, `unchanged`.

**6. Scènes et test de référence de la démo**
- Fichiers :
  - `demo/.../scenes/` : `ReferenceSprites2D` (textures, animation à une frame fixée par `setFrame`, carte de tuiles), `ReferenceShapesText2D` (`Text` des polices du moteur, sept formes vectorielles, contours, points), `ReferenceColorSpace2D` (même contenu à gauche en linéaire, à droite en gamma), `Reference3D` (mesh primitif, `PhongMaterial`, une lumière, texture des ressources de la démo, sans le paquet de démonstration) ;
  - `DemoScenes` ;
  - `ReferenceTestConfig` ;
  - `ReferenceImagesGLTest`, avec en plus une vue de `ReferenceColorSpace2D` sous `picture gamma 1.4`, `brightness` et `contrast` non neutres ;
  - la série locale de l'utilisateur, générée par `--update`, dans `demo/src/test/resources/references/<clé>/`.
- Vérification en jeu : chaque scène, `input key hold D`, `input scroll 3`, `?camera`, et `leakcheck <Scène>`.
- Vérifier que deux exécutions de suite donnent exactement la même image (déterminisme).

**7. CI et outil**
- Fichiers : `.github/workflows/ci.yml` (runner, propriété, artefact), `tools/references.py`.
- Générer la série `linux-mesa-llvmpipe` : pousser la branche, CI rouge sur les images absentes, `python tools/references.py --from-ci`, relire les images, commiter, CI vert.

**8. Test d'acceptation (vérifié une fois, puis retiré)**
- Sur une branche temporaire `test/image-acceptance`, créée depuis la branche de sujet, changer volontairement un rendu. Par exemple, décaler d'un pixel un sprite de `ReferenceSprites2D`, ou changer une couleur de 3 niveaux dans un shader commun.
- Vérifier que `mvn -o -q test` échoue en local (message, trois fichiers) et que le CI échoue avec l'artefact `references` (image de différence lisible).
- Supprimer la branche (locale et distante), en demandant à l'utilisateur avant de la pousser et avant de la supprimer.
- Noter le résultat dans le « Bilan ».

**9. Documents** (liste plus bas).

## Démo
Quatre scènes de référence (étape 6), inscrites dans `DemoScenes`. Elles apparaissent dans le menu et passent le test de fumée. Aucune scène existante n'est modifiée.

## Documents à mettre à jour à la fin
- `architecture.md`, « Kit de test » :
  - capture (`SceneRunner.capture`, `CapturedImage`) ;
  - comparaison (`ImageTolerance`, `ImageComparison`) ;
  - références (`ReferenceImages`, `RendererKey`, dossiers, propriétés `updateReferences`, `requireReferences`, `referenceKey`, `referencesFolder`) ;
  - `Screenshot` en deux temps (pixels en mémoire, PNG sur `Workers`).
- `debug-tools.md` : section « Références d'images » :
  - régénérer (`tools/references.py --update`, `--from-ci`, `--key`) ;
  - lire un échec (`target/references/<clé>/*.diff.png`, artefact du CI) ;
  - déroulé d'un changement de rendu voulu : `--update` en local, push, CI rouge, `--from-ci`, commit.
- `decisions.md` : une série par pilote à tolérance stricte plutôt qu'une série commune à tolérance large ; image finale de la fenêtre plutôt que le FBO de la scène ; métrique par canal plutôt que perceptuelle.
- `later.md`, « Kit de test : suites » :
  - séries macOS et autres pilotes ;
  - comparaison perceptuelle (FLIP) si le bruit des pilotes l'exige ;
  - références pour les tests GL du kit (sans scène de démo) ;
  - repli par le FBO de la scène si un pilote ne relit pas une fenêtre cachée.
- `CLAUDE.md` :
  - tests couverts : `CapturedImageTest`, `ImageComparisonTest`, `RendererKeyTest`, `ReferenceImagesTest`, `ScreenshotGLTest`, `SceneCaptureGLTest`, `ReferenceImagesGLTest` ;
  - commande `tools/references.py` ;
  - règle : un changement de rendu voulu régénère les deux séries dans le même commit, et une scène `Reference*` ne se modifie pas sans régénérer.
- `bonnes-pratiques.md` : un point « références d'images à jour » (vérification : `mvn test` vert, aucune série orpheline d'une scène supprimée).
- `current.md` : supprimer les lignes « Comparaison d'images » et « Test d'acceptation », et réduire le critère de fin à ce qui reste (couverture).
- Supprimer ce plan (fait par `/pr-ml`).

## Hors périmètre
- Couverture réelle : plan à part (`docs/plan/couverture-reelle.md`).
- Séries macOS, comparaison perceptuelle, références des tests GL du kit : `later.md` (voir plus haut).
- Captures en plusieurs modes du lot : déjà comparées entre elles par les tests GL du lot. La référence n'est capturée que dans le mode par défaut.

## Questions restantes
- Si la relecture d'une fenêtre cachée échoue sur le pilote de l'utilisateur (étape 3) : s'arrêter et décider ensemble entre le repli par le FBO de la scène et une fenêtre visible hors écran.

## Modèles recommandés
- Discussion A :
  - étapes 2 à 5 (API publique du kit, découpage de `Screenshot`, logique de comparaison et de références) : Claude Opus 5.5, effort `high` ;
  - étapes 6 et 7 (scènes, test, CI, outil Python) : Claude Sonnet 5.5, effort `medium` ;
  - étapes 8 et 9 (acceptation, docs) : Claude Sonnet 5.5, effort `medium`.
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high`, pour relire un diff qui touche bibliothèque, kit, démo et CI.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/comparaison-images.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre modèle, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/image-comparison`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : ce qui a été fait, les écarts au plan et leur raison, ce qui a été vérifié et comment, ce que l'utilisateur doit vérifier à la main. Pour l'utilisateur :
  - ouvrir les quatre scènes `Reference*` depuis le menu (rendu attendu, caméra pilotable) ;
  - lire une image `*.diff.png` d'un échec ;
  - lancer `python tools/references.py --key` et `--update` sur sa machine.

  La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/image-comparison`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour.
- Vérifications manuelles à faire, reprises du « Bilan » : les quatre scènes `Reference*` dans le menu, une image de différence lisible dans l'artefact du CI, et `tools/references.py --key` et `--update` sur la machine de l'utilisateur.
