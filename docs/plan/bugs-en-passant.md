# Bugs relevés en passant : corrections

**Statut : validé.**

Décisions prises :
- Les langues du clavier (`KeyboardLanguage` et tout ce qui en dépend) sont supprimées ; une étiquette de touche donnée par la disposition du système les remplace (`KeyboardManager.keyLabel`, nom anglais fixe pour les touches non imprimables), montrée par une nouvelle scène `KeyboardLayoutScene`.
- `DebugInfoCamera2D` : ordre d'ajout, précision de la rotation, WARNING sur un nom inconnu.
- La carte (`TileMap`) porte une version au lieu d'un journal vidé à la main ; à son rechargement, le fichier gagne et les changements perdus sont signalés.
- Un test du kit couvre `Texture.unload` après le déchargement des données CPU.
- `ColorSpaceSplit3D` (affichage différent selon la scène précédente) : ligne retirée de `later.md`, jamais reproduite (`scenario.py` au 2026-11-27 par quatre chemins, image identique au pixel près ; captures de l'utilisateur sans différence visible). S'il réapparaît, le noter avec le chemin qui le montre.
- Restent où ils sont, liés à un autre point : `network` entre threads (réseau), `Material` (3D, UBO), extrusion des régions de texture (empaqueteur d'atlas), atlas de police coloré (thème), Box2D (« Physique 2D »), tests d'`OpenALSoundService` (périphérique audio), Wayland (limite de GLFW).

## Pour démarrer
- Lire : `docs/later.md`, « Bugs relevés en passant » et « Entrées : remappage et manettes » ; `docs/architecture.md`, « Entrées », « API publique » (ligne des classes extensibles), « Pièges connus » (cartes) ; `docs/debug-tools.md`, « Rechargement » si présent.
- Branche de sujet depuis `dev` : `fix/bugs-en-passant`.
- Compiler et tester avec les commandes de `CLAUDE.md` après chaque étape ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Langues du clavier** (`mightylib/.../inputs/keyboardlanguage/` : `KeyboardLanguage`, `AZERTYKeyboardLanguage`, `QWERTKeyboardLanguage`, `package-info.java` ; `inputs/CharInputEntry`) : des tables de caractères pour les seules touches A à Z, singletons (`getInstance`) qui gardent **un** `KeyboardManager` (`setManager`). Le constructeur de `KeyboardManager` ne donne pas le gestionnaire à sa langue : `getKeyByLanguage` lève une `NullPointerException` (`translateKeyTo` lit `manager.isCapsLock()`). Avec deux fenêtres, la langue partagée lit l'état de la dernière.
  - **Code mort** : `getKeyByLanguage`, `getCorrespondingCharMainKeyPressed`, `getMainKeyPressed`, `getNumberKeyPressed`, `isCapsLock` n'ont aucun appelant (moteur, démo, tests). La touche principale (`mainKey`, `wasMain`, `numberKeyPressed`, `MIN/MAX_LANGUAGE_KEY`) et le Verr. Maj. maison (`capsLockActivated`, faux s'il était actif au démarrage) ne servent qu'à eux.
  - `EngineConfig.setKeyboardLanguage` / `WindowConfig.setKeyboardLanguage` (passés par `Engine` ligne ~438 et `ContextManager.createWindow` ligne ~104) ne changent rien d'observable : les actions se lisent par position physique (`GLFW_KEY_*`), le texte vient du rappel de caractères de GLFW, la console lit la lettre de la disposition par `glfwGetKeyName` (`KeyEvent.LetterOf`, `KeyboardManager.letterOf`).
  - Références : `demo/.../main/Main.java:57` (`setKeyboardLanguage(AZERTY)`), `module-info.java:67` (`exports …inputs.keyboardlanguage`), `ExtensibleClasses` (ligne de `KeyboardLanguage`), `architecture.md` (lignes 197, 247, 256), test désactivé `mightylib-test/.../KeyboardLanguageGLTest.java`.
  - Ailleurs : aucun moteur n'embarque ses tables de disposition ; Godot distingue `physical_keycode` (position), `keycode` et `key_label` (étiquette de la disposition courante, `DisplayServer.keyboard_get_label_from_physical`) ; SDL distingue scancode et keycode (`SDL_GetKeyFromScancode`, disposition courante) ; GLFW donne `glfwGetKeyName(key, scancode)` pour les touches imprimables seulement (null sinon).
- **`DebugInfoCamera2D`** (`mightylib/.../scenes/camera/cameraComponents/DebugInfoCamera2D.java`) : `Set<String> info = new HashSet<>()`, ordre d'affichage selon le hachage ; `"rotation"` écrit `camera.getRotation()` sans `prec` ; `"id"` est ajouté au constructeur et à `unload` mais n'a pas de `case` (l'en-tête montre toujours l'identifiant) ; un nom inconnu est ignoré en silence. Le texte est construit dans `postUpdate`. `Text` est un `MeshRenderer` (GL) ; `Camera2D` se construit sans GL (`Camera2DTest`).
- **Cartes** (`mightylib/.../resources/map/TileMap.java`) : `setTileType` ajoute un `TileChange` à `tileChanges` (si chargée et si la valeur change) ; `hasBeenUpdated()` = journal non vide ; `TileMapRenderer.update` (ligne ~406) reconstruit tout tant que `hasBeenUpdated()`. Personne dans le moteur ne vide le journal : le jeu doit appeler `clearTileChanges()` après la mise à jour de ses renderers (`Scene2D:218`, `WaveFunctionCollapse2D:112`, `TileMapLayersGLTest`, `TileMapTest`). Oubli → reconstruction à chaque frame et journal qui grossit sans fin ; appel trop tôt → changement jamais dessiné. Personne ne lit le détail des `TileChange` (`getTileChanges`) hors tests. `swapContent` (rechargement à chaud) remplace les calques par ceux du fichier et vide le journal : les changements du code sont perdus sans message (`architecture.md`, « Pièges connus »).
  - Modèle à réutiliser : les versions suivies (`CameraVersionTest`, `ShaderValue`, version de `Material`) : chaque lecteur garde la version qu'il a vue, aucun appel de remise à zéro.
  - Écritures de cartes par le moteur : `WaveFunctionCollapseGrid` (lignes 36, 73, 95).
- **`Texture.unload`** (`mightylib/.../resources/texture/Texture.java`) : depuis `c106751e`, supprime la texture GL dès que `textureId != -1`, que les données CPU (`TextureData`) soient chargées ou non ; non testé (il faut décharger les données d'une ressource partagée sous un élément GL vivant). Le suivi des textures supprimées (`GenDeleteResources.DeleteTexture`) est vu par chaque contrôle de fuites du kit.

## Décisions
1. **Langues du clavier supprimées** : package `inputs.keyboardlanguage`, `CharInputEntry`, `setKeyboardLanguage`/`getKeyboardLanguage` d'`EngineConfig` et `WindowConfig`, et dans `KeyboardManager` la langue, la touche principale et le Verr. Maj. maison avec leurs getters. Changement cassant sans alias (moteur sans produit). Pourquoi : code mort, tables incomplètes, état partagé entre fenêtres ; le système connaît la disposition.
2. **`KeyboardManager.keyLabel(int key)`** : l'étiquette d'une touche dans la disposition du système (`glfwGetKeyName`, en majuscule : `"Z"` sur la touche `GLFW_KEY_W` d'un clavier AZERTY), pour les textes d'aide et le futur remappage. Touche non imprimable (`glfwGetKeyName` → null) : nom anglais fixe d'une table du moteur (`"Escape"`, `"Enter"`, `"F1"`, `"Left"`, `"Left Shift"`, `"Keypad 1"`…) ; touche hors bornes (`BEGIN_KEYS`..`KEYS`) → `IllegalArgumentException`, comme `simulateKey`. Traduction des noms : avec les langues.
7. **Démo** : nouvelle scène `KeyboardLayoutScene` (écran de test d'entrées, donc sans caméra pilotée, `CLAUDE.md`).
3. **`DebugInfoCamera2D`** : `LinkedHashSet` (ordre d'ajout), `prec(rotation)`, `Logger.Warn(CATEGORY_ENGINE, …)` quand `addInfo` reçoit un nom inconnu (noms connus : `position`, `rotation`, `zoom`) ; `"id"` retiré de l'ensemble (l'en-tête le montre déjà).
4. **Version de la carte** : `TileMap.version()` augmente à chaque tuile changée et à chaque rechargement ; `TileMapRenderer` (et chaque renderer d'une catégorie de `LayersTileMapRenderer`) garde la version construite. `tileChanges`, `TileChange`, `clearTileChanges`, `getTileChanges`, `hasBeenUpdated` supprimés (aucun lecteur du détail). Pourquoi : plus de protocole manuel, plusieurs lecteurs, pas de journal qui grossit.
5. **Rechargement d'une carte changée par le code** : le fichier gagne (comme le rechargement d'une ressource ailleurs), avec un `Logger.Warn(CATEGORY_RESOURCES, "<carte> reloaded: N tiles changed by the code are lost")` ; un compteur des tuiles changées depuis le chargement suffit.
6. **Test de `Texture.unload`** : test GL du kit, sans nouvel outil si l'API le permet.

## Étapes
1. **Plan** : commiter ce fichier tel quel.
2. **`DebugInfoCamera2D`** (`DebugInfoCamera2D.java`) : décision 3. Le texte est construit par une méthode package-private statique (caméra, informations, format → `String`), testable sans GL. Test `DebugInfoCamera2DTest` (sans GL, `Camera2D` construite comme dans `Camera2DTest`) : ordre d'ajout gardé quel que soit l'ordre des noms, rotation à la précision réglée, nom inconnu → WARNING (écouteur du `Logger`, comme `LoggerTest`) et rien d'affiché, nom en majuscules accepté. Vérification en jeu : une scène qui l'utilise (`grep DebugInfoCamera2D demo/`), `?gl summary` sans ERROR. Fusionnable seule.
3. **Langues du clavier supprimées** (décision 1) : fichiers listés dans « Contexte », démo `Main.java`, `ExtensibleClasses`, `module-info.java` ; `KitApiTest`, `PublicFieldsTest`, `ApiStatusTest`, `ModuleDescriptorTest` à relancer (listes à mettre à jour s'ils listent ces types). Supprimer `KeyboardLanguageGLTest` (il est remplacé à l'étape 4). Les scripts locaux de `tools/scripts/` (non versionnés) ne comptent pas. Fusionnable seule.
4. **`keyLabel`** (décision 2) dans `KeyboardManager` ; la table des noms des touches non imprimables dans une classe package-private testable sans GL. Tests : sans GL, la table (chaque `GLFW_KEY_*` non imprimable a un nom, touche hors bornes refusée) ; GL du kit `KeyLabelGLTest` : une touche non imprimable a son nom fixe, une lettre a une étiquette d'un caractère, quelle que soit la disposition de la machine (ne pas exiger une lettre précise). Démo : `KeyboardLayoutScene` (section « Démo »). Vérification en jeu : `python tools/scenario.py "scene KeyboardLayoutScene" wait-loaded "input key hold W" "?scene"` puis `leakcheck KeyboardLayoutScene` ; avec la touche simulée, la ligne affichée doit montrer l'étiquette de la disposition de la machine (texte vérifié par une commande qui renvoie le texte affiché si `?scene` ne le donne pas, sinon capture de la zone utile). Fusionnable seule (après l'étape 3).
5. **Version de la carte** (décisions 4 et 5) : `TileMap`, `TileMapRenderer`, `LayersTileMapRenderer`, `WaveFunctionCollapseGrid` si besoin, `Scene2D`, `WaveFunctionCollapse2D`, tests. Tests sans GL (`TileMapTest`) : la version augmente à chaque tuile changée, pas pour une même valeur, augmente au rechargement (`swapContent` d'une copie), compteur des tuiles changées remis à zéro par le rechargement. Test GL du kit (`TileMapLayersGLTest` adapté) : changement dessiné sans appel de remise à zéro, deux catégories d'une même carte voient le même changement, aucune reconstruction sans changement (envois du mesh comptés, comme `MeshUploadGLTest`). Test du WARNING du rechargement : par le rechargement d'une carte changée dans un test du kit (`problems()`), ou sans GL si `swapContent` s'appelle seul. Vérification en jeu : `python tools/scenario.py "scene Scene2D" wait-loaded "input key press A" "?gl summary"` (la tuile 0,0 change, aucune ERROR) puis `leakcheck Scene2D`. Fusionnable seule.
6. **Test de `Texture.unload`** (décision 6) : test GL du kit `TextureUnloadGLTest` : une texture GL vivante dont la `TextureData` partagée est déchargée (chercher le chemin réel : `Resources.unloadBatch` d'un lot qui la contient pendant qu'un élément GL la garde, ou rechargement), puis l'élément déchargé : la texture GL est supprimée (compte des textures vivantes de `GenDeleteResources` revenu à son niveau d'avant, contrôle de fuites du kit), aucune ERROR. Si l'API publique ne le permet pas, ajouter au kit l'outil le plus petit (pont package-private du kit, comme les autres ponts) et le dire dans le « Bilan ». Fusionnable seule.
7. **Docs** (liste ci-dessous) ; ne pas supprimer ce plan.

## Démo
- `KeyboardLayoutScene` (`demo/.../scenes/`, inscrite dans `DemoScenes` avec sa description) : pour chaque touche tenue, une ligne « position `GLFW_KEY_W` → étiquette `Z` » (nom de la constante GLFW lu par une petite table de la scène ou par `keyLabel` pour l'étiquette seulement), et le texte tapé de la frame (`getTypedText`) ; aide fixe « ESC : menu ». Écran de test d'entrées : pas de `DemoCamera.free`. Passe le test de fumée (`DemoScenesSmokeGLTest`) ; aucun cas dans `SmokeCases`.

## Documents à mettre à jour à la fin
- `architecture.md` : « Entrées » (position physique pour les actions, `keyLabel` pour l'étiquette, plus de langue du clavier), lignes 197, 247 et 256 (`KeyboardLanguage` retiré des classes extensibles), « Pièges connus » (ligne des cartes relues : version, fichier gagnant et WARNING ; ligne de `TileMapRenderer` : la reconstruction suit la version).
- `later.md` : retirer de « Bugs relevés en passant » les lignes de `KeyboardManager`, `DebugInfoCamera2D`, `ColorSpaceSplit3D` (jamais reproduit), et `Texture.unload` de la ligne des corrections de novembre (le reste de cette ligne demeure) ; « Entrées : remappage » : la liaison par lettre de la disposition s'appuie sur `keyLabel` (plus sur `setKeyboardLanguage`) ; ajouter la cible durable de l'état des cartes (voir « Hors périmètre »).
- `CLAUDE.md` : tests couverts (retirer `KeyboardLanguageGLTest` désactivé ; ajouter `DebugInfoCamera2DTest`, `KeyLabelGLTest` et la table des noms, la version de `TileMap`, `TextureUnloadGLTest`) ; règle de la caméra 2D : « Z Q S D en AZERTY » reste vrai (touches physiques), rien à changer sauf si la démo affiche les étiquettes.
- `debug-tools.md` : rien sauf nouvel outil du kit (étape 6).
- `decisions.md` : une ligne « Disposition du clavier : celle du système (`glfwGetKeyName`), jamais de table du moteur ; actions par position physique ».
- `current.md` : rien (vide).
- Supprimer ce plan (discussion B).

## Hors périmètre
- **État d'une carte partagé entre scènes** : une carte est une ressource partagée et modifiable ; les changements d'une scène survivent ou non selon que la ressource reste chargée (autre référence, lot). Cible durable : la ressource est le modèle, le jeu travaille sur une instance (copie des calques) liée aux sauvegardes. À noter dans `later.md` (avec « Sauvegardes » et « Import Tiled » de `futur-discussion.md`).
- Étiquettes traduites des touches non imprimables (« Échap ») : avec « Thème, tailles automatiques et langues ».
- Textes d'aide de la démo construits depuis les liaisons : avec le remappage (« Entrées »).
- Le reste de « Bugs relevés en passant » (voir les décisions prises).

## Questions restantes
- Aucune.

## Modèles recommandés
- Discussion A, étapes 2 à 4 (`DebugInfoCamera2D`, suppression des langues, `keyLabel` et sa scène) : Claude Opus 5.5, effort `medium` (changements bornés, API simple).
- Discussion A, étapes 5 et 6 (version de `TileMap` et ses renderers, test de `Texture.unload` dont le chemin réel est à trouver) : Claude Opus 5.5, effort `high` (modèle de données partagé par plusieurs renderers, tests GL du kit).
- Discussion A, étape 7 (docs) : Claude Sonnet 5.5, effort `medium` (mise à jour mécanique).
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (diff étendu sur plusieurs zones, à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/bugs-en-passant.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `fix/bugs-en-passant`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`KeyboardLayoutScene` : appuyer sur des lettres, Échap exclu, Maj, AltGr, touches du pavé numérique, et vérifier que les étiquettes suivent la disposition AZERTY de la machine ; `Scene2D` : la tuile 0,0 change à l'appui de la touche de gauche et la carte n'est pas reconstruite à chaque frame ; rechargement à chaud d'une carte après un changement : WARNING dans la console ; une scène avec `DebugInfoCamera2D` : lignes dans l'ordre d'ajout, rotation arrondie).
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml fix/bugs-en-passant`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
### Fait
- Étape 2 : `DebugInfoCamera2D` en `LinkedHashSet`, `prec(rotation)`, WARNING (`engine`) sur un nom inconnu (ignoré), `"id"` retiré ; texte construit par `Describe` et noms ajoutés par `AddInfo` (statiques, package-private), testés sans GL par `DebugInfoCamera2DTest`.
- Étape 3 : package `inputs.keyboardlanguage`, `CharInputEntry`, `setKeyboardLanguage`/`getKeyboardLanguage` d'`EngineConfig` et `WindowConfig`, touche principale et Verr. Maj. maison de `KeyboardManager` supprimés (constructeur `KeyboardManager(WindowInfo)`) ; démo, `module-info`, `ExtensibleClasses`, dictionnaire d'IntelliJ mis à jour ; `KeyboardLanguageGLTest` supprimé.
- Étape 4 : `KeyboardManager.keyLabel(key)`, table `KeyNames` (package-private), `KeyNamesTest` (sans GL : chaque `GLFW_KEY_*` du clavier nommée, par réflexion sur `GLFW`), `KeyLabelGLTest` (kit), scène `KeyboardLayoutScene` (dans `DemoScenes`, passe le test de fumée).
- Étape 5 : `TileMap.tilesVersion()` (tuile changée une fois chargée, rechargement) et compteur `changedTiles()` (package-private, remis à zéro par le rechargement et le déchargement) ; `TileChange`, `clearTileChanges`, `getTileChanges`, `hasBeenUpdated` supprimés ; `TileMapRenderer` garde la version construite ; appels de remise à zéro retirés de `Scene2D` et `WaveFunctionCollapse2D`. `WaveFunctionCollapseGrid` et `LayersTileMapRenderer` sans changement (chaque renderer de catégorie suit la version seul).
- Étape 6 : `TextureUnloadGLTest` par le chemin réel : un lot (`BatchResourcesMethod`) charge la donnée et sa texture, le code partage la texture (`addElementOrReturnIfPresent`), `unloadBatch` décharge la donnée, puis `releaseResource` supprime la texture GL (compte `GenDeleteResources` revenu). Aucun outil ajouté au kit. Vérifié : le test échoue avec la condition d'avant `c106751e` (6 textures au lieu de 5).
- Étape 7 : `architecture.md`, `later.md`, `decisions.md`, `CLAUDE.md`, `licence.md` (fichiers de test du kit `tile-maps/` et `texture-unload/`).

### Écarts au plan
- Nom `TileMap.tilesVersion()` au lieu de `version()` : `DataType.getVersion()` existe déjà (version du contenu lu, chargement et rechargement) et `TileMapRenderer` lit les deux (le rechargement attend la texture de l'atlas, une tuile changée non) ; deux méthodes `version()`/`getVersion()` de sens différents auraient été un piège.
- Le pavé numérique garde toujours son nom fixe (« Keypad 1 ») : GLFW le nomme comme les chiffres de la rangée du haut (« 1 »), ce qui rendrait les textes d'aide ambigus.
- Un code sans touche GLFW entre les bornes (33 par exemple) est nommé « Key 33 » au lieu d'une exception.
- Le WARNING du rechargement est testé sans GL (`TileMapTest`, `swapContent` appelé seul).

### Vérifié
- `mvn -o -q test` complet (GL compris) vert après chaque étape.
- `scenario.py` : `Scene2D` (`?gl summary`, `?problems`, caméra pilotée, `input key press A`, `leakcheck Scene2D`) sans problème ; `KeyboardLayoutScene` : `GLFW_KEY_W -> Z` et `GLFW_KEY_KP_1 -> Keypad 1` sur la machine AZERTY (`?gl text`), `leakcheck` sans fuite.

### À vérifier à la main
- `KeyboardLayoutScene` : appuyer sur des lettres, Maj, AltGr, des touches du pavé numérique (Échap ramène au menu) ; les étiquettes suivent la disposition AZERTY de la machine, le texte tapé s'affiche (accents compris : `scenario.py` les abîme par la ligne de commande Windows).
- `Scene2D` : la tuile 0,0 change à l'appui de la touche de gauche (Q en AZERTY) et la carte n'est pas reconstruite à chaque frame.
- Rechargement à chaud d'une carte après un changement (`Scene2D`, touche de gauche, puis modifier le fichier de la carte) : WARNING « … reloaded: N tiles changed by the code are lost » dans la console.
- Une scène avec `DebugInfoCamera2D` (`Scene2D`, `Box2D`, `RopeSimulation2D`) : lignes dans l'ordre d'ajout (position, rotation, zoom), rotation arrondie.
