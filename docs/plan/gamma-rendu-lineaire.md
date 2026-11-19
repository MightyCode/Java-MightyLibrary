# Gamma : rendu linéaire activable et réglable

Statut : **validé**.

## Décisions prises
1. Contrôle à plusieurs niveaux : moteur (défaut du jeu) → vue (`Scene`, `WindowView`) → `Renderer`. Chaque niveau vaut *hérite* (défaut), *linéaire* ou *gamma* ; on le redéfinit pour diverger, on le remet à *hérite* pour revenir. Modifiable à l'exécution, à tout niveau.
2. Modèle par dessin : textures de couleur **toujours** en `GL_SRGB8_ALPHA8`, framebuffer de vue **toujours** `GL_SRGB8_ALPHA8`. Dessin linéaire : `GL_FRAMEBUFFER_SRGB` activé, couleurs données (uniformes, sommets) converties en linéaire, mélange en linéaire. Dessin gamma : `GL_FRAMEBUFFER_SRGB` désactivé, ce qui est lu des textures de couleur est réencodé en sRGB dans le shader, mélange comme aujourd'hui. Mode effectif passé par un uniform : ni variantes de shaders, ni renvoi de textures quand le mode change.
3. Texture de couleur ou carte de données : champ `"colorSpace": "srgb" | "linear"` dans `textures.json` (défaut `srgb`, comme le drapeau `linear` de LÖVE ou la case sRGB de Unity) ; les textures des modèles reçoivent le leur selon leur rôle (diffuse : sRGB, spéculaire/normales : linéaire) ; l'atlas des polices est linéaire.
4. Traitement modifiable à plusieurs niveaux : fonctions de conversion dans un include unique (`utils/colorSpace.glsl`) remplaçable par une source plus haute (jeu, mod) ; shader de passe finale et ses paramètres (gamma, luminosité, contraste) : défaut moteur, surchargés par vue. La chaîne d'effets reste au point « Effets 2D ».
5. Défauts : moteur en gamma (la 2D existante ne change pas) ; scènes 3D éclairées de la démo en linéaire, 2D en gamma (sauf future scène 2D éclairée).
6. Démo : **F7** (action déclarée par le plugin de la démo) bascule la vue courante à la volée, dans toute la démo ; nouvelles scènes `ColorSpaceSplit3D` (même caméra, linéaire à gauche, gamma à droite) et `ColorSpaceSplit2D` (dégradés, fondus alpha, texte anticrénelé, textures filtrées).
7. Configurations : un type de donnée en couches, `ConfigData`. Défauts `configs/<nom>.json` fusionnés clé par clé à travers les sources (moteur → jeu → mods), puis couche du joueur `data/configs/<nom>.json`, seule écrite (temporaire puis renommage), observée comme les autres ressources. Les réglages du joueur (gamma, luminosité, contraste) en sont le premier usage. Emplacement par système, versions et migration restent dans `futur-discussion.md`.
8. Commandes : `colorspace` niveau `DEV` (modes moteur/vue, bascule) ; `picture` niveau `USER` (gamma, luminosité, contraste du joueur, sauvegardés dans sa config).

## Pour démarrer
- Lire : `docs/architecture.md` (Rendu, Service des shaders et prélude, Ressources : sources, `DataType`/`ResourceLoader`, observateur de fichiers, `GLResources`, API publique), `docs/debug-tools.md` (Commandes), `docs/decisions.md` (philosophie), `docs/later.md` (« Gamma », « Effets 2D »), `docs/futur-discussion.md` (« Sauvegardes et réglages »), `docs/techniquegraphique.md` (HDR/bloom, pour ne rien empêcher).
- Branche de sujet depuis `dev` : `feature/gamma`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- **Textures** : toutes envoyées en `GL_RGBA8` — `resources/texture/Texture.java` (l. ~135, ~235), `TextureUploader` (PBO, `glTexSubImage2D` sur un stockage déjà créé), `TexturePlaceholders`, `resources/cubemap/CubeMapTexture.java`, `CubeMapPlaceholders`, `TextureUploadBench`. `TextureData` lit `textures.json` (`qualityType`…) via `TextureDataLoader` (qui découvre aussi les images absentes du JSON) ; `ModelLoader.registerTexture` (~l. 215) crée des `TextureData` possédées par le modèle (diffuse, spéculaire : `ModelMesh.getSpecularTexture`), `ColorTextureData` pour une couleur unie ; `FontFace.getTexture` donne l'atlas des polices.
- **Framebuffer de vue** : `graphics/renderer/_2D/FrameBuffer.java` en `GL_RGB` 8 bits sans alpha ; `VirtualSceneRenderer` (un par `Scene`, `Scene.java` ~l. 236 et 402-411, et par `WindowView` ~l. 228) dessine dans ce framebuffer puis le recopie dans la fenêtre avec le shader `EngineResources.POST_PROCESSING` (`shaders/framebuffers/postProcessingF.glsl`, simple copie, ancien code de noyau commenté).
- **Couleurs** : les couleurs uniformes `Color4f` partent en `int` RGBA (`Shader.java` ~l. 352) et sont décodées par `decodeColor` (`shaders/utils/colorsManipulation.glsl`) : point unique de conversion. Exceptions : couleurs par sommet (`multiColoredShape2D`, `multiColoredLighting3D`), `vec3` des lumières et des matériaux (`textureMultipleLights3DF`, `multiColoredLighting3DF`, shaders `colorLighting3D`/`colorMaterial3D`/`textureMaterial3D` de la démo), `glClearColor` côté CPU (`Scene.setClearColor`), texte (`font/coloredTextF.glsl`).
- **Shaders** : ceux du moteur dans `mightylib/src/main/resources/assets/mightylib/shaders/` (2D, 3D, font, framebuffers, utils), ceux de la démo dans `resources/shaders/` (dont `fallbackDemo/`, volontairement cassés). Le préprocesseur gère `#include` (remplaçable par une source plus haute) et un prélude (`#version`, defines globaux et d'entrée).
- **Renderer** : `graphics/renderer/Renderer.java` (`display` : `applyRenderState`, `use`, `updateShader`, `draw`, `restore` ; valeurs optionnelles `putOptionalValue` sautées si le shader n'a pas l'uniform) ; état GL par dessin dans `RenderState` (profondeur, culling ; `apply`/`restore`).
- **Vue courante** : `main/Views` (`current()`, `currentOrNull()`), `IView` (moteur, contexte, caméras).
- **Services** : `registry/ServicesSection`, `services/` (`ServiceOrder`, `ServiceSet`) ; exemple à suivre : `IShaderService`/`ShaderService` (`mightylib:shaders`).
- **Ressources** : pile de sources (moteur, jeu, mods), `MultiSourceDataType`, fusion de `shaders.json` par entrée (modèle pour la fusion des configs), `ResourceWatcher`/`FileChangeDetector` (observateur), `DataFolder` (`data/`, sans écriture sûre).
- **Actions** : `EnginePlugin` déclare F2 (console détachée) et F5 (rechargement des textures) ; le plugin de la démo déclare les siennes.
- **Tests existants** proches : `ShaderPreprocessor`, `ShaderServiceTest`, `GLResources*`, `MultiSourceDataTest`, `ListMergeTest`, `FileWatchTest`, `ResourcesSourcesTest`, `DataFolder`, `PublicFieldsTest`, `ExtensionFieldsTest`, `ApiStatusTest`, `ModuleDescriptorTest`, `DemoScenesTest`.

## Décisions de détail
- Noms : `ETextureColorSpace { SRGB, LINEAR }` (encodage d'une texture) ; `ERenderColorSpace { INHERIT, LINEAR, GAMMA }` (mode de dessin). Résolution : `Renderer` ≠ `INHERIT` sinon vue ≠ `INHERIT` sinon moteur (jamais `INHERIT`). La résolution est une fonction pure testée.
- Le mode du `Renderer` vit dans `RenderState` (`apply` active/désactive `GL_FRAMEBUFFER_SRGB`, `restore` remet l'état) ; `Renderer.display` envoie l'uniform `mlLinearColors` (valeur optionnelle : sautée si le shader ne l'a pas, donc un shader tiers non converti reste en gamma comme aujourd'hui).
- `utils/colorSpace.glsl` : `uniform bool mlLinearColors;`, `toLinear(vec3)`/`toSRGB(vec3)` (courbe sRGB exacte, remplaçable), `colorIn(vec4)` pour une couleur donnée en sRGB (décodée si linéaire), `textureIn(vec4)` pour une lecture de texture de couleur (réencodée si gamma) ; les cartes de données ne passent par aucune des deux. `decodeColor` appelle `colorIn` (toute couleur empaquetée est une couleur sRGB).
- Couleur d'effacement : effacée avec `GL_FRAMEBUFFER_SRGB` désactivé et la valeur sRGB brute ; le résultat stocké est le même dans les deux modes.
- Passe finale : lit le framebuffer sRGB (décodé en linéaire par le GPU), réencode à la main en sRGB (indépendant de la prise en charge sRGB du framebuffer par défaut), puis applique sur les valeurs encodées contraste `(x - 0.5) * contrast + 0.5`, luminosité `x * brightness`, gamma `pow(x, 1 / gamma)` ; défauts 1, 1, 1 (identité, rendu inchangé) ; bornes validées (par exemple [0.25, 4]). Écriture dans la fenêtre avec `GL_FRAMEBUFFER_SRGB` désactivé.
- Service `IColorService` (clé `mightylib:colors`, `engine.colors()`, implémentation par défaut `final`, remplaçable par `registry.services().replace`, sans GL) : mode du moteur, nom du shader de passe finale et paramètres par défaut (lus de la config du joueur et réécrits par elle) ; `IView` donne ses surcharges (mode, shader de passe, paramètres), `null`/`INHERIT` = hérite.
- `EngineConfig.setColorSpace(...)` et propriété `-Dmightylib.colorSpace=linear|gamma` pour le défaut au démarrage (le service garde ensuite la valeur courante).
- `ConfigData` : un JSON objet par config ; fusion profonde des objets, la source la plus haute gagne ; la couche du joueur ne contient que les clés qu'il a changées ; lecture typée avec défaut (`getFloat(clé, défaut)`…) ; écriture sur un worker (`Workers`, I/O seulement), temporaire puis renommage ; un rechargement provoqué par sa propre écriture est sans effet visible ; erreur de lecture → `ResourceException` nommant le fichier, les valeurs des couches valides restent.
- Couche du joueur des configs : `data/` déclaré comme source de ressources limitée au dossier `configs/` (filtre de chemin par source), donc observée et listée par `resources sources` sans code à part. Si le modèle des sources ne l'accepte pas sans détour, s'arrêter et en discuter (repli : lecture directe de `data/configs/` par le loader de `ConfigData`, l'observateur suivant ce fichier hors des sources).
- Rôle des textures de modèle : fixé à l'enregistrement par `ModelLoader` ; un même fichier utilisé dans deux rôles garde le premier (une ligne d'avertissement).
- Un mode de rendu (et les valeurs de la config) change sans rien recharger : seuls l'uniform et `GL_FRAMEBUFFER_SRGB` changent au dessin suivant.

## Étapes (un commit chacune)
1. **Encodage des textures, données seulement.** `ETextureColorSpace` ; champ `colorSpace` de `TextureData` (JSON, défaut `SRGB`, valeur inconnue → `ResourceException` une ligne) ; textures découvertes : `SRGB` ; `ModelLoader` : spéculaire `LINEAR` ; atlas des polices `LINEAR` ; visible dans `resources Texture <nom>`. Rien ne change encore côté GL. Tests : lecture du champ, défaut, valeur refusée, rôle d'un modèle (sans GL si possible). Fusionnable seule.
2. **Stockage sRGB, rendu inchangé.** Format interne `GL_SRGB8_ALPHA8` pour les textures `SRGB` (`Texture`, rechargement, `TextureUploader`, `CubeMapTexture`, placeholders), `GL_RGBA8` pour `LINEAR` ; `FrameBuffer` en `GL_SRGB8_ALPHA8` ; `utils/colorSpace.glsl` ; tous les shaders de couleur du moteur et de la démo passent par `colorIn`/`textureIn` ; `Renderer` envoie `mlLinearColors = false` ; passe finale qui réencode. Vérification : captures de `Scene2D`, `Scene3D`, `GUIWidgetsScene`, `TextPaddingScene` avant/après (commande `screenshot`, comparaison de pixels en Python dans le scratchpad) : identiques en *nearest*, écart minime sur les textures filtrées ; `leakcheck` sur une scène 2D et une 3D.
3. **Modes de dessin à trois niveaux.** `ERenderColorSpace`, résolution, `RenderState`/`Renderer` (`setColorSpace`), surcharge de vue (`Scene`, `WindowView`, via `IView`), défaut moteur (`EngineConfig`, propriété) ; couleurs des lumières et matériaux converties dans les shaders 3D ; effacement. Tests : résolution (toutes les combinaisons), `EngineConfig`/propriété, `PublicFieldsTest`/`ExtensionFieldsTest` à jour. En jeu : `-Dmightylib.colorSpace=linear` sur `Scene3D` (plus clair, éclairage plus doux) et `Scene2D` (fondus différents).
4. **Service des couleurs et passe finale réglable.** `IColorService`/implémentation par défaut, inscrit dans `EnginePlugin` ; paramètres gamma/luminosité/contraste en uniforms de la passe finale ; shader de passe et paramètres surchargés par vue. Tests sans GL : défauts, bornes, surcharges, service remplacé.
5. **Type `ConfigData` en couches.** Loader, fusion à travers les sources, couche du joueur dans `data/configs/`, écriture sûre, observation, état dans la commande `resources`. Config `mightylib/display` du moteur (`configs/mightylib/display.json` dans la source du moteur : gamma, luminosité, contraste) lue par le service. Tests : fusion profonde, couche du joueur, écriture (temporaire + renommage, contenu relu tel qu'écrit), fichier invalide, rechargement après modification (comme `FileWatchTest`).
6. **Commandes.** `colorspace` (`DEV`) : sans argument, modes moteur / vue courante / résolu ; `colorspace engine linear|gamma`, `colorspace view linear|gamma|inherit|toggle`. `picture` (`USER`) : `picture` affiche, `picture gamma|brightness|contrast <valeur>`, `picture reset` ; écrit la config du joueur. Syntaxe déclarée, messages lisibles, détail dans les `data`. Tests : analyse, complétion, valeurs refusées, effet sur le service (sans fenêtre : « No window » pour la vue).
7. **Démo : bascule et défauts.** Action `demo:toggle_color_space` sur F7 dans le plugin de la démo (bascule la vue courante, message dans le journal) ; `Scene3D`, `Scene3DBis`, `TerrainGeneration3D` en linéaire par leur vue ; `Game3D2D` : vue linéaire, renderers 2D en gamma (exemple du niveau `Renderer`). Vérifier que la 3D reste fonctionnelle.
8. **Démo : scènes de comparaison.** `ColorSpaceSplit3D` (même caméra, moitié gauche linéaire, droite gamma : la scène dessine deux fois avec `glScissor`/viewport en changeant le mode de sa vue entre les deux) et `ColorSpaceSplit2D` (dégradés, fondus alpha, texte, texture filtrée, mêmes moitiés) ; inscrites dans `DemoScenes` avec description ; libération dans `unload`/`release` ; `leakcheck` des deux.
9. **Documents** (liste ci-dessous), sans supprimer ce plan.

## Démo
- `ColorSpaceSplit3D`, `ColorSpaceSplit2D` (étape 8), F7 partout (étape 7), commandes `colorspace` et `picture`.

## Documents à mettre à jour à la fin
- `architecture.md` : encodage des textures (`colorSpace`), modes de dessin et leur résolution, framebuffer sRGB, `colorSpace.glsl`, passe finale et `IColorService`, `ConfigData` (couches, couche du joueur, écriture), au présent.
- `debug-tools.md` : commandes `colorspace` et `picture`, propriété `-Dmightylib.colorSpace`, F7 de la démo, comparaison de captures.
- `decisions.md` : modèle par dessin (textures et framebuffer toujours sRGB, mode par uniform) et son pourquoi ; configs en couches.
- `later.md` : retirer la section « Gamma » ; dans « Effets 2D », la passe finale réglable existe (la chaîne s'y branche) ; garder HDR/tone mapping (`RGBA16F`) et `EXT_texture_sRGB_decode` (éviter le réencodage) comme pistes.
- `futur-discussion.md` : « Sauvegardes et réglages » s'appuie sur `ConfigData` (reste : emplacement par système, versions, migration, menu d'options).
- `CLAUDE.md` : tests couverts (encodage des textures, résolution des modes, service des couleurs, `ConfigData`, commandes `colorspace`/`picture`).
- `licence.md` : rien de neuf sauf ressource ajoutée.
- `current.md` : rien (aucun gros point en cours après la fusion).
- Supprimer ce plan (discussion B).

## Hors périmètre
- HDR, tone mapping, bloom, framebuffer `RGBA16F` : `techniquegraphique.md` / `later.md` « Effets 2D ».
- Chaîne de post-traitement : `later.md` « Effets 2D ».
- `EXT_texture_sRGB_decode` (lire une texture sRGB sans décodage pour un dessin gamma) : optimisation, notée dans `later.md`.
- Menu d'options du joueur, emplacement des données par système, versions des configs : `futur-discussion.md`.
- Scène 2D éclairée : future scène (lumière 2D), en linéaire.

## Questions restantes
Aucune.

## Modèles recommandés
- Discussion A, étapes 1 à 6 : Claude Opus 5.5, effort `high` (modèle de rendu par dessin, API à trois niveaux, service et type de donnée durables, nombreux shaders).
- Discussion A, étapes 7 à 9 : Claude Sonnet 5.5, effort `medium` (démo et docs, sur un modèle déjà posé).
- Discussion B (`/pr-ml`) : Claude Opus 5.5, effort `high` (gros diff GL + API à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/gamma-rendu-lineaire.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/gamma`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (F7 dans `Scene2D`, `Scene3D`, `GUIWidgetsScene` ; `ColorSpaceSplit3D` et `ColorSpaceSplit2D` : moitiés différentes, éclairage plus doux à gauche ; `picture gamma 1.5` puis relance : valeur gardée ; modification à la main de `data/configs/mightylib/display.json` pendant le jeu avec `watch` actif ; `Game3D2D` : 2D inchangée). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/gamma`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan
- **Fait** : les étapes 1 à 9, un commit chacune (étape 9 : documents) sur `feature/gamma`.
- **Écarts au plan** :
  - Étape 3 : le service `IColorService` (`mightylib:colors`, paquet `graphics.color`) est créé dès l'étape 3 avec le seul mode du moteur ; l'étape 4 y ajoute la passe finale et `PictureSettings`.
  - Étape 4 : `VirtualSceneRenderer` dessine lui-même la passe par défaut (`postProcessing`) ; un autre shader (vue ou moteur) passe par un renderer interne recréé quand le nom résolu change. `resetPicture()` ajouté au contrat.
  - Étape 5 : une couche de config illisible n'échoue pas la donnée : elle est ignorée avec une ligne nommant le fichier, listée dans `invalidLayers` ; une couche du joueur illisible n'est jamais réécrite. `FilteredSource` expose `configs/` et ses parents (`localFile("")` = racine de `data/`, l'observateur parcourt donc ses dossiers) ; `configs` est ignoré par `exploreResourcesFile` ; `EngineConfig.setPlayerDataFolder`. Le service ne garde dans la couche du joueur que les valeurs qui diffèrent de celles du jeu.
  - Étape 7 : F7 est géré par un service de la démo (`ColorSpaceToggleService`, lit l'action chaque tick) pour valoir dans toute la démo, sans toucher chaque scène.
  - Étape 8 : `Scene.setScissor`/`clearScissor` (et `WindowView`) ajoutés à la bibliothèque (la démo n'a pas accès à `org.lwjgl.opengl`). Dans `ColorSpaceSplit2D`, la rampe de gris est un témoin : identique dans les deux modes (couleurs données en sRGB) ; les différences sont dans les fondus alpha, la texture filtrée et le texte.
- **Vérifié** : `mvn -o -q test` (tous modules) à chaque étape ; étape 2 : `TextPaddingScene` identique au pixel près, `GUIWidgetsScene` et `Scene2D`/`Scene3D` (animées) ne diffèrent que dans les zones animées ; `leakcheck` de `Scene2D`, `Scene3D`, `ColorSpaceSplit2D`, `ColorSpaceSplit3D` ; `-Dmightylib.colorSpace=linear` sur `Scene3D` (fond et éclairage plus clairs) ; `colorspace view toggle`, `picture gamma 1.5` gardé après relance, `picture reset` ; F7 enchaîne `Scene3D`, `Game3D2D`, `Scene3DBis`, `TerrainGeneration3D` sans erreur ; captures des deux scènes de comparaison (cube vert : spéculaire différent de part et d'autre de la coupure).
- **À vérifier à la main** : F7 dans `Scene2D`, `Scene3D`, `GUIWidgetsScene` ; `ColorSpaceSplit3D` et `ColorSpaceSplit2D` (moitiés différentes, éclairage plus doux à gauche) ; `picture gamma 1.5` puis relance : valeur gardée ; modifier à la main `data/configs/mightylib/display.json` pendant le jeu avec `watch` actif (la valeur change) ; `Game3D2D` : le jeu 2D (slime) garde ses couleurs.
- **À noter** : un moteur de test du kit (`mightylib-test`) garde la couche du joueur dans `data/` du dossier du module ; un test qui appelle `picture` y écrirait (rediriger vers un dossier temporaire dans le kit).
