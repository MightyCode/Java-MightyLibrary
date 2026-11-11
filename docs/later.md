# Plus tard

Feuille de route, dans l'ordre (suite : `techniquegraphique.md`). Gros point en cours : `current.md`. Quand il est fini, **discuter** du point en tête de ce fichier avec l'utilisateur, puis le déplacer dans `current.md`.

## Philosophie du moteur : bases simples, implémentations proposées, injection (en premier)
Philosophie de `decisions.md` : usage courant facile, réimplémentation (presque) complète possible. En premier : chaque commit écrit sans elle ajoute du code à reprendre.

### Écarts relevés dans le code
- **`Renderer` crée son `Shape`** (`shape = new Shape(shaderName, useEbo)` dans son constructeur) : impossible de lui donner un `Shape` hérité. Ajouter un constructeur qui reçoit un `Shape`, en gardant l'actuel pour l'usage simple.
- **`Shape` expose son état interne en `protected`** (une quinzaine de champs : `vbos`, `vbosStorage`, `eboStorage`, `vboCount`, `in2D`…) : tout son fonctionnement devient un contrat. Le remplacer par quelques méthodes redéfinissables et sortir de la base l'hypothèse 2D/3D (`in2D`). Même revue pour `Renderer` (`model`, `shaderValues`, `textures`… en `protected`).
- **Systèmes de l'`Engine` créés en dur** : `Console`, `SceneManager`, `SoundManager`, `ShaderManager` (`Engine.setup`) et `InputManager` (`ContextManager`) ; la configuration ne règle que leurs paramètres, un projet ne peut pas fournir les siens. Les rendre injectables (ci-dessous) ; à décider à la planification : interfaces ou classes de base, et ce qui reste fixe.
- **Rien n'est choisi comme extensible** (au 2026-11-11 : 247 classes publiques, 49 `final`, du `protected` dans 87 fichiers) : tout est ouvert par défaut (voir **Héritage**).
- **Aucune démo n'étend le rendu** : une scène avec son propre `Shape`/`Renderer` (ex. `CustomRenderer2D`) valide le contrat et fait apparaître ce qui bloque.

### Modèle d'extension validé : le projet injecte, le projet étend
- **Injection** : un point unique appelé à une phase précise du démarrage (avant l'init des ressources), ex. `register(EngineRegistry)`, registre figé ensuite. Remplace `IProjectLoading.contextLoading` et les points provisoires `IProjectLoading.loaders` (liste de loaders figée à la construction de `Resources`, les mods fourniront les leurs à la même phase) et `IProjectLoading.registerCommands` ; les commandes du moteur et du jeu sont figées après le démarrage, celles des scènes et des vues restent dynamiques. Absorbe `InputRegistry` : actions et axes enregistrés pendant cette phase (aujourd'hui à l'initialisation des classes de constantes, sans gel), avec leurs touches par défaut.
- **Injectable** : sources de ressources (dossier du jeu, autres jars ; un nom identique remplace celui du moteur), loaders et `DataType`, commandes, scènes par nom, actions et touches par défaut, shaders, scène de chargement par défaut, systèmes de l'`Engine` (console, gestionnaires de scènes, de sons, de shaders, d'entrées), crochets de cycle de vie (démarrage, changement de scène, arrêt), compteurs `stats`, plus tard composants/thèmes GUI.
- **Héritage** : classes extensibles **choisies explicitement** avec points d'accroche (`Shape`, `Renderer`, `Shape2DRenderer`, `Scene`, `DataType`/`ResourceLoader`, `BaseCommand`, composants GUI…) ; les autres `final` ou package-private (tout `protected` devient un contrat).
- **API publique** : **à discuter avant toute décision** (périmètre, changements cassants avant une 1.0) ; même tri que l'héritage (`bonnes-pratiques.md`, point 7).

## Gamma : rendu linéaire activable et réglable
LearnOpenGL « Gamma Correction ». Tôt car il change toutes les couleurs à l'écran.
- Constat : aucun `GL_SRGB*`, calculs en sRGB → éclairage 3D trop sombre, fondus et dégradés ternes (peu visible en pixel art).
- Règle : `Color4f` et textures du projet sont en sRGB.
- Activation : option `EngineConfig` + par scène, désactivée par défaut, activée par défaut en 3D éclairée. Active : textures de couleur en `GL_SRGB8_ALPHA8` (pas les cartes de données : normales, spéculaire, hauteurs), couleurs uniformes converties en linéaire, `GL_FRAMEBUFFER_SRGB` ou conversion dans la passe finale si framebuffer intermédiaire (`VirtualSceneRenderer`).
- Réglage joueur : gamma/luminosité dans une passe finale (`pow(c, 1/gamma)`), modifiable à l'exécution, sauvegardé.
- `GammaScene` : dégradés, fondus alpha, éclairage, activé/désactivé côte à côte.

## Tests de non-régression
À étendre (liste des tests : `CLAUDE.md`) : scénarios en jeu via `tools/mighty.py` vérifiant du texte ; fuites par allers-retours de scènes. Filet avant les gros chantiers suivants.
- **Logique pure encore sans test** (`python tools/coverage.py`) : `Animation` / `Animator` (le constructeur de `Animation` lit `Views.engine().resources()` : lui passer `Resources` en paramètre, en gardant le constructeur actuel pour l'usage courant, permettrait de les tester sans moteur) et `FallbackAnimationData` (demande une `Resources`) ; `physics/B2Utils` (conversions JBox2D, à voir avec la physique 2D).

## Bugs relevés en passant (non corrigés)
- **Premier passage de `tools/analyse.py` (2026-11-11) à trier** (`python tools/analyse.py --details <motif>`), par ordre de risque : SpotBugs `AT_STALE_THREAD_WRITE_OF_PRIMITIVE` (15) et `AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE` (6), `VO_VOLATILE_INCREMENT` (2, aussi Error Prone `NonAtomicVolatileUpdate` : `DataType`, `ResourceWatcher`) ; `NP_*` (`FontFile.load`, `ExecGenCom.run`, `EngineConfig.ParseLogsShown`) ; Error Prone `MissingCasesInEnumSwitch` (23), `EmptyCatch` (5), `FutureReturnValueIgnored` (3), `ReferenceEquality` (41), `EnumOrdinal` (20). Corriger par thème, comme avec `/analyse-intellij` ; ce qui est voulu va dans `tools/spotbugs-exclude.xml` ou les `-Xep:…:OFF` du profil `analyse`.
- `Slider.setMaxPrecision` ne fait rien : `maxPrecision` vaut -1 au départ et la méthode n'écrit que si elle est déjà `>= 0` (condition à revoir).
- `StrSelector` / `EnumSelector` : position et taille écrites en dur (coin haut droit de la fenêtre), donc deux sélecteurs se superposent et `StrSelector` n'a aucune démo ; à rendre configurables (comme les sliders).
- `Collision2DGrid.init` journalise sans le préfixe `--` du moteur.
- Sans démo : `ReducedGraph` / `GraphRenderer` / `NodesRenderer`, `Screenshot`, `network` (`ClientTcp`/`ServerTcp`), `InputSimulation` (hors commandes).
- `DataFolder.getFileContent` concatène les lignes lues sans saut de ligne (`Scanner.nextLine`).
- `BasicMaterial.clone()` / `Material.clone()` partagent leurs `Vector3f` (copie superficielle) : modifier la couleur d'un clone change l'original (3D).
- `WaveCollapseRules.Rule.INITIAL_POS` est un `Vector2i` public partagé, modifiable par n'importe qui (champ `final`, objet mutable).
- `ResourceLoader.exploreResourcesFile` : les sous-dossiers sont explorés avec `IGNORE_FOLDERS` au lieu de la liste de dossiers ignorés reçue (sans effet aujourd'hui, seul `BatchLoader` en donne une).

## Bibliothèque réutilisable
`mightylib` = base des futurs jeux/logiciels ; `mightylibdemo` l'illustre. Cap validé :
- **Deux modules Maven** : `mightylib` (jar) et `demo`, qui en dépend comme un projet externe.
- **Jar léger** : dépendances (LWJGL et natives, JOML, org.json, JBox2D…) déclarées dans le pom, pas embarquées.
- **Distribution** : `mvn install`, puis JitPack ou GitHub Packages. **Versions** sémantiques par release.
- **Ressources** (principal chantier, tout est lu aujourd'hui depuis `resources/` sur disque) : celles du moteur (shaders, texture `error`, polices par défaut) dans le jar, lues depuis le classpath ; celles du jeu dans le dossier du projet ; un **paquet de démonstration** à part (modèles `backpack`, `basicShape`, `weapon`, `world`, non commités, dans `.gitignore`) : la démo fonctionne sans, avec un message clair.
  - **Plusieurs racines** (moteur, jeu, mods ; un nom identique remplace celui du moteur) : le dossier est déjà propre à chaque `Resources` (`EngineConfig.setResourcesFolder`, lu par `resources.folder()`) ; il deviendra une liste de sources.
  - Lisent encore `resources/` en dur, hors du dossier de `Resources` : `GamepadManager.MAPPINGS_FILE` (lu par `ContextManager`) et `network/NetworkConfigurationLoader` (`resources/networkConfigs`).
- Extension et API publique : section « Philosophie du moteur » en tête de ce fichier.

### Modding (secondaire, ne rien décider qui l'empêche ; pas à implémenter maintenant)
- Le point d'injection accepte **plusieurs modules** : le jeu, puis chaque mod (jars de `mods/` via `ServiceLoader`, descripteur id/version/dépendances fixant l'ordre).
- **Identifiants à espace de noms** (`jeu:slime`, classe `registry/Identifier`, déjà utilisée par les actions, les axes et les commandes) pour ressources et scènes : aujourd'hui noms globaux → collisions.
- **Commandes d'un mod** : le mod est leur propriétaire (`CommandOwner`), espace = identifiant du mod, niveau `DEV` ; insérer `MODDER` entre `CHEAT` et `DEV` si les outils de mod doivent être séparés de ceux du moteur.
- La liste des scènes du menu de la démo (`MenuScene.createSceneEntries`) devra venir du registre de scènes par nom, pas d'une liste écrite en dur.
- Remplacement de ressources par id ; à terme **bus d'événements** (plusieurs modules réagissent, ex. ajouter un bouton au menu).
- **Rechargement à chaud des ressources des mods** : l'observateur de fichiers (`ResourceWatcher`) suit les ressources du jeu ; il devra suivre aussi les dossiers et ressources de chaque mod, et les remplacements de ressources par id.
- Mods de données (JSON/ressources) et mods de code (jars, sans sandbox : de confiance seulement).

## Chargement : optimisations seulement si les mesures ou un vrai jeu l'exigent

- **Shaders** : `GL_KHR_parallel_shader_compile` (lancer toutes les compilations, puis interroger `GL_COMPLETION_STATUS_KHR` aux frames suivantes ; une étape = un `glCompileShader` bloquant) ; pic de frame de ~58 ms au chargement, jugé acceptable.
- **Priorité plus fine** que courant/préchargement (rechargement à chaud, ressources du premier affichage) ; une donnée garde sa priorité urgente si la référence urgente est retirée avant la préchargée.
- **Chargement GL** : PBO ou contexte partagé (fenêtre cachée + `glFenceSync`). `HeightMapRenderer` et `TerrainRenderer` envoient encore leur géométrie d'un bloc.
- **Préchargement GL des modèles 3D** : `MeshUpload` est propre à chaque `ModelRenderer` (créé dans `launch`, exige le modèle chargé) ; le rendre partageable par modèle pour qu'un batch l'envoie pendant le chargement comme les textures (`DataType.createGLElement`).

## Budget mémoire et chargement en continu (quand un vrai jeu l'exige)

Les estimations de `DataType.estimatedCpuBytes`/`estimatedGpuBytes` ne servent aujourd'hui qu'au debug (`resources`, `resources batch`) ; aucune décision du moteur ne s'en sert.
- **Budget** : vérifier qu'un lot tient en mémoire (carte comprise), choisir quoi décharger pendant un chargement en continu, prévenir quand un mod est trop gros.
- **Manifeste généré à la compilation** (prérequis) : pour chaque ressource, ses dépendances et sa taille décodée, sans lire le fichier. Aujourd'hui les dépendances d'une donnée non lue ne sont connues qu'après sa lecture (`dependenciesKnown` dans `describeBatch`) et les estimations avant lecture sont des ratios par format (pansements).

## Effets 2D : particules et post-traitement
D'après LearnOpenGL « 2D Game » (Breakout).
- **Particules** (absentes) : émetteurs (position, forme, débit, rafales) ; durée de vie, vitesse, forces, couleur/taille/rotation/opacité dans le temps, texture ou région d'atlas ; mélange additif ou alpha ; pool sans allocation par frame ; émetteurs en JSON (`DataType`), injectables ; espace monde ou local.
- **Rendu** : simple d'abord, instancing une fois le regroupement des dessins fait (Rendu 2D).
- **Post-traitement** sur framebuffer (base `VirtualSceneRenderer`) : tremblement, flou, inversion, fondus, vignette ; enchaînables, par scène. Inclut la passe finale du gamma.
- `ParticlesScene` : plusieurs émetteurs, compteur et temps dans `stats`.

## Physique 2D : remplacer JBox2D
Exigences : 2D au pixel près, cas complexes compris ; 3D compatible plus tard. Une boîte poussée **reste collée** au joueur (aucun écart, aucun tremblement). Un projet C++ précédent a échoué sur le test ci-dessous.
- Constat : JBox2D 2.2.1.1 non maintenu (seulement `physics/B2Utils`, `Box2D`, `Platformer2D`). Collisions maison (`physics/collision`, ~500 lignes : `CollisionRectangle`, `CollisionBoundedVolume2D`, `Collision2DGrid`, `replaceComparedTo`, `CollisionVisualisation`), base de la physique arcade.

### Test d'acceptation : `PhysicsCratesScene`
Trois caisses empilées sur une plateforme mobile (horizontale et verticale) ; pousser celle du bas (la pile suit), puis celle du haut (elle seule bouge et tombe du bord) ; une caisse écrasée contre un mur. Aucun écart d'un pixel, aucun retard d'une frame, aucun tremblement.

### Physique arcade maison (principale)
Modèle solids/actors/riders (Celeste, TowerFall) étendu aux poussables empilés ; exposée en Lua (`physics.move` renvoie la position corrigée, comme `_PhysicsReplace` du C++).
- Positions entières + reste sous-pixel, déplacement axe par axe, pas fixe déterministe.
- **Graphe d'appui** reconstruit à chaque pas (A porté par B si le bas de A touche le haut de B).
- **Deux phases** : calculer jusqu'où la chaîne peut aller (la poussée se propage, s'arrête au premier mur), puis déplacer toute la chaîne du même delta dans le même pas.
- Les porteurs emportent ce qu'ils portent (récursif, du bas vers le haut).
- **Écrasement** → événement, le jeu décide.
- Aussi : tilemaps, plateformes traversables par dessous, pentes (option), zones de déclenchement, catégories par nom (déclarées par le projet), raycasts, événements de contact, visualisation.

### Corps rigides (bibliothèque derrière `IPhysicsWorld2D`)
Pour rotation, empilement libre, articulations (ponts, cordes, ragdolls).
- Candidats : **box2d-jni** (fabmax, Box2D v3 natif, Maven Central, Windows/Linux/macOS ; Java 11 et activité à vérifier), **gdx-box2d** (libGDX, Box2D v3.1), **dyn4j** (pur Java, BSD-3, 5.0.2 de mars 2024, Java 1.6+). Trancher en refaisant `Box2D` avec chacun.
- Coexistence avec l'arcade (joueur arcade poussant des caisses simulées) : à prototyper.
- Au début du point : prototype de `PhysicsCratesScene` avec Box2D v3 pour comparer.

### Compatibilité 3D
Interfaces (monde, corps, requêtes, événements) prêtes pour un monde 3D (ex. Jolt) sans changer l'API des entités et scripts ; rien en 3D maintenant.

## Entités et scripts Lua
Le moteur et le jeu exposent des fonctions que des scripts Lua utilisent pour faire agir les entités, modifiables sans relancer. L'utilisateur en est très satisfait dans un projet C++ (avec un ami) : s'en inspirer en corrigeant ses défauts.
- **Prérequis** : modèle d'entités (ECS et/ou graphe de scène ; aujourd'hui de simples listes par scène), à décider au début du point.

### Bibliothèque
- **LuaJava de gudzpoz** (`party.iroiro.luajava`, MIT, Maven Central, ≥ 4.0.2) : Lua 5.1–5.4 ou LuaJIT natif par plateforme (Windows/Linux/macOS/Android), secours LuaJ. LuaJ seul n'est plus maintenu (~2015). Vérifier l'activité à l'adoption.
- Interface de script indépendante du langage ; LuaJava = première implémentation.
- API **explicite** (fonctions enregistrées une à une, pas de réflexion), bibliothèques Lua limitées (pas de `io`/`os` libres).

### Référence : le projet C++
- Un script par type d'entité, choisi depuis Tiled (propriété `LuaScript`) ; propriétés Tiled copiées dans la mémoire de l'entité (`tiled_tileId`, `tiled_positionX/Y`, `tiled_sizeX/Y`, image…).
- Rappels : `Init`, `Update(dt)`, `Draw`, `DrawLight`, `TakeDamage(damage, direction)`, `SetState(state)`.
- Drapeaux lus par le moteur : `__canTakeDamage`, `__needBeDestroy`, `__needDrawSprite`, `__haveFinishChangeState`…
- Fonctions `_Xxx(entityPtr, …)` : mémoire, sauvegarde permanente, sprite et animations (ajout, lecture, retournement, position, échelle, couleur, fin), sprite de lumière, collisions (catégories/masques, hitbox, déplacement corrigé, traversables, poussables, contact joueur), attaque et dégâts, signaux de logique (levier → porte), création d'entités, UI (vie du boss, textes de dégâts), formes de debug, tremblement, vibration, log, temps.

### Améliorations prévues
- **Entité = objet Lua** (`self:playAnimation("run", true)`), fonctions par module (`sprite`, `physics`, `save`, `ui`, `log`…).
- **Mémoire = table** (`self.memory.hp = 3`) au lieu de `_AddField`/`_SetFloatValue` typés (fini les erreurs int/float) ; propriétés Tiled dans `self.props`.
- `save.get(clé)` → `nil` si absente ; clés à espace de noms par carte/entité.
- Collisions par nom (`"player"`, `"enemy"`, `"environment"`) déclarées par le projet.
- Signaux de logique via le bus d'événements (Modding), pas d'IDs en dur.
- Moteur = générique (sprites, animations, physique, sons, particules, caméra et tremblement, manettes, debug, log, temps, sauvegarde) ; le jeu enregistre ses fonctions par le point d'injection.
- **Rechargement à chaud** des `.lua` en gardant la mémoire des entités ; commandes `lua reload`, `lua eval <code>` (niveau `DEV`).
- **Commandes écrites en Lua** : enregistrées par un script (propriétaire = le script, retirées à son rechargement).
- Erreur de script → log fichier:ligne, entité désactivée, le jeu continue.
- Documentation de l'API générée depuis les fonctions enregistrées.
- À décider : état Lua par scène (environnement isolé par script) ou par entité ; coût Java ↔ Lua mesuré dans `stats`.
- `LuaPlatformer2D` : joueur, ennemi scripté, levier et porte, rechargement à chaud.

## Thème, tailles automatiques et langues
Dépend du point d'injection (thèmes et langues fournis par le projet et les mods). Dès maintenant, ne pas figer d'API `Text`/GUI qui empêcherait clés de langue et thème.
- **Langues** (à la Flutter) : `resources/lang/<langue>.json` chargé comme `DataType` ; paramètres `{nom}`, pluriels simples ; langue de repli, log des clés manquantes ; script `tools/` générant des constantes typées ; `Text` liable à une clé, mis à jour au changement de langue ; clés à espace de noms.
- **Thème** : `Theme` en JSON (couleurs, polices, tailles, espacements, styles par composant) géré par `ThemeManager`, lu par les composants GUI au lieu de valeurs en dur (`setFontSize(20)`, `new Color4f(0.5f)` dans `EnumSelector`/`StrSelector`).
- **Tailles automatiques** : unité relative à la résolution virtuelle, ancres et pourcentages, recalcul au redimensionnement.
- **Padding du texte** : utiliser `Text.setPadding` dans les composants GUI et l'exposer au thème.

## Rendu 2D : formes manquantes et regroupement des dessins
- Existant : rectangle, ellipse, hexagone, polygone (`renderer/_2D/shape`), renderers de debug (graphes, nœuds, matrices).
- À ajouter : segments/polylignes épais, contours de toutes les formes, cercle, triangle, rectangle arrondi, arc/secteur, points, flèches, grille ; API de dessin immédiat pour le debug (ligne de A à B cette frame).
- **Point de départ sur `archive-dev-2025`** (absent de `dev`) :
  - `437b7ea` : `StrokeModule` (activation, couleur, taille du contour par valeurs de shader), contour dans le shader de l'ellipse, `Renderer.getShaderValue`/`hasShaderValue`, `Boolean`/`String` dans `ShaderValue`. Défauts : Java envoie `strokeSize` mais le shader lit `strokeSizePercent` (`in`, pas `uniform`) → contour probablement cassé ; `getSizePercent` lit la mauvaise clé ; faute `geEnabled` ; contour en % du rayon inégal sur une ellipse aplatie → épaisseur en pixels.
  - `983c18c` (« Temporary ») : `AdvancedRectangle`, rectangle arrondi par SDF avec rotation et anticrénelage (`fwidth` + `smoothstep`), contour commenté. Le SDF peut servir de base aux contours, cercles, rectangles arrondis. Ne pas reprendre ses changements de `Shader.java` (vérification des uniforms désactivée, log à supprimer).
  - Ensuite supprimer `archive-dev-2025` et `devShader` (local et `origin`) ; `devShader` (`b50dd27`) est déjà couvert par `dev`.
- **Regroupement des dessins** (un appel par texture + shader ; complète `TextureDataAtlas`). Comparer sur mesures (`stats` : `drawCallsPerFrame`, temps CPU) :
  - **Batch** (`SpriteBatch`) : sommets transformés sur CPU dans un VBO dynamique ; texte et formes variées.
  - **Instancing** (quad + attributs par instance, `glVertexAttribDivisor`, `glDrawElementsInstanced`) : sprites et particules.
  - Remplissage par frame efficace (`glMapBufferRange`, `glBufferSubData`).

## Rendu SVG
Après le rendu 2D et la reprise de `archive-dev-2025` (contours épais et anticrénelage communs).
- Viser le noyau des profils légers (SVG Tiny/Native, NanoSVG) : chemins (segments, Bézier) remplis ou tracés ; texte converti en chemins à l'export.
- Sous-ensemble : chemins et formes de base, remplissage et contour unis, opacité, transformations, `viewBox` ; plus tard dégradés. Hors champ : texte, CSS, filtres, masques, SMIL.
- Approche (à confirmer) : `SVGData` (`DataType`, workers, NanoSVG de LWJGL) aplatit et triangule remplissages et contours (épaisseur, jointures, extrémités) ; `SVGRenderer extends Renderer` + shader `svg` ; un appel par SVG ; anticrénelage MSAA ou bande de fondu. Plus tard si besoin : courbes dans le fragment shader (Loop-Blinn, SDF).
- `SVGScene` : icônes à plusieurs échelles.

## Entrées : remappage et manettes
- Touches modifiables à l'exécution, sauvegardées/chargées depuis un fichier ; scène prefab de changement des touches.
- **Liaisons des actions du jeu par lettre de la disposition** : la console reconnaît ses raccourcis par `KeyEvent.letter()` ; les liaisons du jeu (`ActionInput`, `ActionId`) restent par position physique (`EngineConfig.setKeyboardLanguage`) : à faire évoluer avec le remappage (lettre de la disposition ou position, au choix de l'action).
- **Manettes** (base faite : `GamepadManager`, joueurs, `InputGamepadButton`/`InputGamepadAxis`, virtuelles, commande `gamepad`). Reste :
  - **Actions par joueur** (multijoueur local) : aujourd'hui joueur fixe ou `ANY_PLAYER` ; cible `inputManager.getState(action, joueur)`, clavier/souris attribués à un joueur comme une manette.
  - **Ensembles d'actions activables** (« jeu », « menu » : un même stick déplace et navigue, `MOVE_*_2D` et `SELECT_UP/DOWN`) au lieu de verrous par composant (`FreeCamera3DComponent.setLookLocked`).
  - Sensibilité du regard réglable (aujourd'hui `LOOK_SENSITIVITY` de `ProjectLoading`), séparable souris/manette, avec inversion verticale (`scale` négatif).
  - Remappage manette sauvegardé avec le clavier (`Gamepad.parseButton`/`buttonName`).
  - Vibrations, gyroscope : absents de GLFW (SDL ou XInput si besoin).
  - Icônes de boutons selon le modèle (Xbox, PlayStation, Switch) d'après nom/GUID.

## Son : streaming
Streaming des musiques longues (tout est décodé en mémoire).

## Console et GUI en jeu
- **Overlay de debug** (stats, scène, entrées) à côté de la console. S'appuie sur thème, tailles et langues.
- **Console** : police monospace livrée avec le moteur (aujourd'hui `bahnschrift` du batch `init` du projet, proportionnelle) ; thème (couleurs, hauteur de l'overlay, taille du texte ; la palette de `ConsoleColors` y passera) ; suggestions affichées sous la saisie en plus du Tab bash ; glyphe flèche dans la police (l'indicateur des lignes sous la vue écrit « v N lines » faute de `↓`) ; annuler/refaire (Ctrl+Z/Y) dans la saisie, recherche dans le journal (Ctrl+F), menu clic droit, sélection rectangulaire ; mémoriser l'état de `console logs` avec les réglages du joueur (Fenêtres / réglages) ; filtres par source et autres types exposés par commande (le modèle d'ensemble de types cachés existe, cf. Journal à grande échelle).
- **Commandes, suite** :
  - complétion dans `mighty.py` à partir du JSON de `help <cmd>`, et documentation des commandes générée depuis les schémas ;
  - conditions de multijoueur (commande réservée à l'hôte) : le mécanisme `requires(CommandCondition.hidden|check)` existe, les conditions viendront avec le réseau (permissions par joueur, `futur-discussion.md`) ;
  - commandes Lua : elles déclareront leur syntaxe par la même API (voir Entités et scripts Lua) ;
  - traduction des messages et aides des commandes du joueur (système de langues) : les descriptions de la syntaxe deviendront des clés de langue.
- **Journal à grande échelle** : l'anneau de `ConsoleLog` oublie au-delà de sa capacité ; cible durable : journal fichier tournant (cf. `futur-discussion.md`) relu par la console, filtres par type/source, niveaux de log au lieu de `System.out` préfixé `--`.
- **Texte de la console** : un `Text` (VAO/VBO, draw call) par ligne visible, sans découpe GL (`glScissor`), et un `RectangleRenderer` par rangée surlignée : à reprendre avec le batching 2D.

## Fenêtres
- **Sous-fenêtres natives hors Windows** : propriétaire de fenêtre sous Linux (X11 `XSetTransientForHint`, Wayland `xdg_toplevel.set_parent`) et macOS (`addChildWindow`) à la place du simple `GLFW_FLOATING` de `NativeWindowOwner`.
- **Réglages d'affichage du joueur** : `DisplaySettings` ↔ JSON dans `DataFolder`, menu d'options (modes, résolutions de `MonitorInfo.getVideoModes`, écran), position fenêtrée réglable ; écrans branchés/débranchés en cours de jeu (`glfwSetMonitorCallback` : une fenêtre dont l'écran disparaît revient sur le principal, aujourd'hui son `MonitorInfo` garde un handle invalide).
- **Fenêtre redimensionnée** : recalcul optionnel de la surface virtuelle (elle reste fixe, avec bandes noires) ; le jeu se fige pendant qu'on tire un bord ou déplace la fenêtre sous Windows (boucle modale dans `glfwPollEvents`, seule l'image est redessinée) : la cible durable est une boucle de jeu et de rendu sur un thread à elle, le thread principal ne lisant que les événements (change la règle « tout le GL sur le thread principal », à discuter). Icône par fenêtre, renvoi du focus au dialogue modal quand on clique sur la fenêtre parente, manettes bloquées par un dialogue modal.
- **`EngineStats` par fenêtre** (compteurs de frames et de swaps) et `Screenshot` par fenêtre dans la boucle.

## Intégrations plateformes
- **Discord** : présence enrichie (SDK Discord, bindings Java à évaluer).
- **Steam** : Steamworks (succès, overlay, statistiques), bindings Java à évaluer.
