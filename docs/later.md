# Plus tard

Feuille de route, dans l'ordre (suite : `techniquegraphique.md`). Gros point en cours : `current.md`. Quand il est fini, **discuter** du premier point de la feuille de route (la section qui suit « Écrire `current.md` ») avec l'utilisateur, puis le déplacer dans `current.md` (format ci-dessous).

## Écrire `current.md`

### But
`current.md` est le **seul gros point en cours**, découpé en objectifs qu'on peut terminer. Il dit ce qui reste à faire, rien d'autre : l'état présent est dans `architecture.md`, les raisons durables dans `decisions.md`, l'historique dans git, le détail d'un objectif en cours d'implémentation dans `docs/plan/`. Il sert de point d'entrée aux skills : `/planification-ml` part d'un de ses objectifs, `/implement-next-objective` prend sa première puce (ou celle marquée (U)), `/pr-ml` et `/revue-pratiques` vérifient qu'il ne garde aucune trace de ce qui est fini.

### Cycle de vie
1. `current.md` vide → discuter avec l'utilisateur du point en tête de `later.md` (et des autres documents : un point plus urgent peut exister ailleurs, un prérequis peut manquer).
2. Écrire le point dans `current.md` (format ci-dessous) et retirer sa section de `later.md` ; ce qui n'entre pas dans le point reste dans `later.md`, rattaché à la section qui le traitera. Mettre à jour les renvois (`CLAUDE.md`, `bonnes-pratiques.md`, autres sections de `later.md`).
3. Chaque objectif passe par `/planification-ml <objectif>` → `docs/plan/<sujet>.md`, puis implémentation et `/pr-ml`. Un objectif simple (peu de fichiers, aucun choix de conception ni d'API ouvert) peut aller directement à `/implement-next-objective`.
4. Objectif atteint → supprimer sa puce dans la même branche (ou la réduire à son reste), décrire le nouvel état dans `architecture.md`, supprimer le plan.
5. Dernière puce supprimée → le fichier ne garde que son en-tête : retour en 1.

### Format
- **Titre** : `# Gros point actuel — <nom du point>` (le nom de la section de `later.md`).
- **Paragraphe de règles** (fixe, recopié d'un point à l'autre) : ce qui reste seulement, pas d'historique, propositions à valider avant d'appliquer, changements cassants maintenant, un `/planification-ml` par objectif, signification de (U).
- **Paragraphe de philosophie** (fixe, recopié d'un point à l'autre) : la philosophie de `decisions.md`, qui s'applique à tout point ; elle n'a pas de fin.
- **Paragraphe de cadrage** (trois ou quatre phrases) : pourquoi ce point passe avant les autres (ce qui en dépend), et le **critère de fin** du point entier, vérifiable (ex. « la démo compile contre la seule API publique »).
- **Objectifs** : une puce de premier niveau par objectif, `- **Nom court** : …`, dans l'ordre d'exécution (du moins risqué au plus cassant, prérequis d'abord). Une puce = un objectif planifiable et fusionnable seul (une branche, une PR), ni une tâche de dix minutes ni un point entier. `(U)` sur le plus urgent seulement.
- **Sous-puces** d'un objectif : les constats précis (classe, méthode, champ, chiffre daté « au AAAA-MM-JJ »), la cible, et ce qui reste **à décider** (marqué « à décider » ou « à discuter avant toute décision ») : la planification part de là. Assez de détail pour que `/planification-ml` n'ait pas à relire `later.md`, pas de solution détaillée (elle va dans le plan).
- **Dernier objectif = test d'acceptation** du point quand c'est possible (une démo, un module, une scène qui doit fonctionner).

### Ce qui n'y va pas
- Rien de conditionnel (« si les mesures l'exigent », « si un vrai jeu le demande ») : ces lignes restent dans `later.md` (`bonnes-pratiques.md`, point 6).
- Aucun « fait », « déjà », « aujourd'hui fait X », aucune date de fin.
- Pas de bug découvert hors du point : il va dans « Bugs relevés en passant » ou dans `futur-discussion.md`.
- Pas de second gros point : un objectif qui grossit au point d'en devenir un retourne dans `later.md`.

## Tests de non-régression
À étendre (liste des tests : `CLAUDE.md`) : scénarios en jeu via `tools/mighty.py` vérifiant du texte ; fuites par allers-retours de scènes. Filet avant les gros chantiers suivants.
- **Logique pure encore sans test** (`python tools/coverage.py`) : `Animation` / `Animator` (le constructeur de `Animation` lit `Views.engine().resources()` : lui passer `Resources` en paramètre, en gardant le constructeur actuel pour l'usage courant, permettrait de les tester sans moteur) et `FallbackAnimationData` (demande une `Resources`) ; `physics/B2Utils` (conversions JBox2D, à voir avec la physique 2D).

## Kit de test : suites
- **Statut `STABLE`** du package `io.github.mightycode.mightylib.test` avant la première version publiée (« Distribution et versions »), une fois éprouvé par un vrai jeu.
- **Tests sur le chemin de modules** : le kit est classpath seulement (ses ponts partagent des packages du moteur) ; un jeu en module teste sur le classpath (`useModulePath=false`, déjà dans `mightylib-base-parent`). Le passage au chemin de modules demanderait des ponts publics `INTERNAL` dans la bibliothèque, ou `--patch-module`.
- **macOS** : les tests `gl` exigent `-XstartOnFirstThread` dans l'`argLine` de Surefire (non testé, comme les autres natives macOS).
- **Comparaison d'images** (tests de rendu au pixel près) : capture de la fenêtre cachée (`Screenshot`) comparée à une image de référence, avec tolérance (pilotes différents, Mesa en CI).
- **Serveur dédié** sur le cœur sans GL (`Engine.setupHeadless`) : boucle sans fenêtre (`MainLoop.tick` seul, ou `frame` sans affichage), réseau ; préparé, pas fait.
- **Couche du joueur des configs** : un moteur de test garde le dossier de données par défaut (`data/` du dossier du module) ; un test qui appelle `picture` ou `ConfigData.set` y écrirait. Le kit devrait le rediriger vers un dossier temporaire (`EngineConfig.setPlayerDataFolder`).
- **Plusieurs moteurs de test en parallèle** : impossible tant que `MainThread`, `Workers`, `GameTime`, `Views`… sont statiques (le kit refuse un second moteur) ; l'exécution parallèle de JUnit reste à éteindre pour les tests du kit.

## Bugs relevés en passant (non corrigés)
- **`network` n'est pas sûr entre threads** : les drapeaux sont `volatile` et le compteur de messages atomique, mais `receivedMessages` et `messageToSend` (listes et table ordinaires) sont lus et écrits par le thread réseau et par le moteur sans verrou. À reprendre avec le réseau (file concurrente entre les deux), pas avant.
- Sans démo : `ReducedGraph` / `GraphRenderer` / `NodesRenderer`, `Screenshot`, `network` (`ClientTcp`/`ServerTcp`), `InputSimulation` (hors commandes).
- **Configuration réseau mal formée** (`NetworkConfigurationLoader.initWithFile`) : plus de cinq lignes utiles (`ArrayIndexOutOfBoundsException`), une ligne sans `= ` ou un port manquant ou non numérique (`NumberFormatException`) donnent une exception brute au lieu d'une `ResourceException` nommant le fichier et la ligne (comme `TextFormat`) ; un autre type de donnée affiche `ERROR LOAD` sans préfixe `--`. À reprendre avec le réseau.

## Manuel et documentation de l'API (cap proposé, **points précis à discuter avant de commencer**)
- **But** : un site HTML pour l'utilisateur du jar, distinct de `docs/` (notes internes) : **manuel d'utilisation et d'explication** en **français et anglais** (comment se servir du moteur, et pourquoi il est construit ainsi) ; la documentation de l'API publique est à côté, sur le même site, et liée depuis le manuel.
- **Moment** : l'API publique est classée (statuts `@API`, classes extensibles : `architecture.md`) ; outillage et squelette d'abord, démarrage rapide une fois la démo compilée contre la seule API publique, puis une page par objectif.
- **Outil proposé** : sources Markdown `manual/fr/…` et `manual/en/…`, site généré par MkDocs Material + mkdocs-static-i18n (sélecteur de langue, recherche, thème sombre), versions par tag `vX.Y.Z` (`mike`), publication GitHub Pages par la CI (`futur-discussion.md`). Autre option : petit générateur Python dans `tools/`.
- **Membres internes exposés, avant la Javadoc publique** : des membres publics de types exportés exposent un type d'un package `internal` (`Engine.consoleLog`, `IConsoleService.getLog`, constructeur de `Console`, `Window.getGLContext`/`createNewWindow`, `GLResources.shared`, `Resources.setGLElementSink`/`loaders`, `GenDeleteResources.Delete…(GLContext, …)`, `Icon.getBuffer` et `SoundDataType.readVorbis` (types LWJGL)), et des types publics implémentent une interface interne (`GLResources` : `IGLElementSink` ; `CubeMapPlaceholders`, `TexturePlaceholders` : `IGLSharedState`) ; les retirer de l'API par une passerelle interne (accès réservé aux packages du moteur), puis réactiver l'alerte `-Xlint:exports` du pom.
- **API** : Javadoc de l'API publique (anglais seulement, commune aux deux langues) générée par `mvn javadoc:javadoc`, copiée dans `site/api/` et liée depuis la navigation ; style rapproché par `-stylesheetfile`. Les accroches des classes extensibles ont déjà leur Javadoc courte ; le reste de l'API publique suppose de revenir sur la convention « peu de Javadoc ».
- **Contre la péremption** : exemples de code inclus depuis des fichiers Java compilés (jamais recopiés) ; pages générées (commandes depuis leur syntaxe déclarée, scènes depuis `DemoScenes`) ; liens `[[api:Classe#méthode]]` résolus par un hook MkDocs, build en échec si la cible n'existe plus ; script de vérification FR/EN (mêmes pages, titres, inclusions), ajouté à `bonnes-pratiques.md` et à la règle de fin d'objectif de `CLAUDE.md`.
- **À discuter** : MkDocs ou générateur maison ; langue de rédaction de référence et traduction ; plan du manuel (démarrage, concepts, extension, référence, recettes) ; étendue de la Javadoc requise ; versions publiées (toutes ou la dernière) ; texte des descriptions de commandes dans les deux langues (lié aux clés de langue).

## Modding (secondaire, ne rien décider qui l'empêche ; pas à implémenter maintenant)
- Le point d'injection accepte **plusieurs modules** (`EngineConfig.addPlugin`, ordre d'ajout) : reste la découverte des plugins des mods (jars de `mods/` via `ServiceLoader`, sur le classpath ou dans une couche de modules (`ModuleLayer`) quand le jeu est un module, descripteur id/version/dépendances fixant l'ordre d'`addPlugin`), et une phase `finish` après tous les `register` si des mods dépendent du contenu d'autres mods.
- **Ordre des services des mods** : la découverte des plugins fixe l'ordre d'`addPlugin` ; les services se trient déjà par leurs dépendances (`requires`/`after`), mais un mod qui remplace un service (`replace`) dépend de l'ordre des plugins (le dernier gagne) : le descripteur des mods devra le rendre explicite.
- **Services par contexte ou par fenêtre ajoutés par les plugins** : les services sont uniques pour l'`Engine` ; un plugin ne peut ajouter qu'une fabrique de gestionnaire de scènes ou d'entrées (remplaçant celle du moteur), pas un système par contexte ou par fenêtre à côté (ex. une mini-carte par fenêtre).
- **Remplacement de la scène de chargement ou de départ par un mod** : `EngineConfig.setStartScene`/`setLoadingScene` n'ont qu'un propriétaire, le jeu.
- **Identifiants à espace de noms** (`jeu:slime`, classe `registry/Identifier`, déjà utilisée par les actions, les axes, les commandes et les scènes) pour les ressources : aujourd'hui noms globaux → collisions ; les noms du moteur (`EngineResources` : `colorShape2D`, `engineSans`…) deviendront `mightylib:nom`, remplaçables par un jeu comme aujourd'hui.
- **Commandes d'un mod** : le mod (son plugin) est leur propriétaire, niveau `DEV` ; insérer `MODDER` entre `CHEAT` et `DEV` si les outils de mod doivent être séparés de ceux du moteur.
- **Scènes enregistrées** : clé de langue de la description (aujourd'hui texte anglais, `SceneRegistry`).
- Remplacement de ressources par id ; à terme **bus d'événements** (plusieurs modules réagissent, ex. ajouter un bouton au menu).
- **Rechargement à chaud des ressources des mods** : l'observateur de fichiers (`ResourceWatcher`) suit chaque source sur le disque (dossiers du jeu et des plugins, classpath en dossier), jamais un jar ; reste un mod livré en jar (le recharger entier, ou le remplacer par son dossier comme `-Dmightylib.resources.engine` pour le moteur) et les remplacements de ressources par id.
- **Observateur à l'échelle de nombreuses sources** : chaque fichier suivi l'est dans *chaque* source disque (`ResourceSources.localCandidates`), existant ou non ; quand son dossier manque dans une source, `FileChangeDetector` le sonde. Suffit pour la démo (3 sources) ; avec des milliers de ressources et plusieurs mods, le sondage grossit en ressources × sources. Cible : observer la création des dossiers (premier dossier existant de chaque candidat) et ne sonder que ce qui ne peut pas être observé.
- **Retrait d'une entrée d'une liste fusionnée** : une source plus haute ajoute ou remplace une entrée de `textures.json`, `fonts.json`, `shaders.json`, ou ajoute à un batch (`ResourceSources.layers`, `FileMethods.mergeEntries`, `BatchLoader.Merge`), mais ne peut pas en retirer (syntaxe à choisir : entrée `null`, liste `remove`).
- **Rechargement à chaud de `shaders.json`** : la liste des shaders est lue une fois, à la création du service `mightylib:shaders` ; une entrée ajoutée, retirée, ou des `defines` d'entrée changés demandent de relancer le jeu (les fichiers GLSL, eux, se rechargent à chaud). Cible : observer les couches de `shaders.json`, créer ou retirer les `ShaderData` et relire ceux dont l'entrée a changé.
- Mods de données (JSON/ressources) et mods de code (jars, sans sandbox : de confiance seulement).

## Chargement : optimisations seulement si les mesures ou un vrai jeu l'exigent

- **Shaders** : `GL_KHR_parallel_shader_compile` (lancer toutes les compilations, puis interroger `GL_COMPLETION_STATUS_KHR` aux frames suivantes ; une étape = un `glCompileShader` bloquant) ; pic de frame de ~58 ms au chargement, jugé acceptable.
- **Priorité plus fine** que courant/préchargement (rechargement à chaud, ressources du premier affichage) ; une donnée garde sa priorité urgente si la référence urgente est retirée avant la préchargée.
- **Chargement GL** : PBO ou contexte partagé (fenêtre cachée + `glFenceSync`). `HeightMapRenderer` et `TerrainRenderer` envoient encore leur géométrie d'un bloc.
- **Rechargement partiel d'une donnée multi-fichiers** (`MultiSourceDataType`) : aujourd'hui un fichier modifié relit toute la donnée (six faces d'une cubemap, tous les calques) ; ne relire que le fichier changé, avec l'état par fichier déjà tenu.
- **Données composées de dépendances** à côté de `MultiSourceDataType` (fichiers fixes) : planche de sprites (image + JSON Aseprite/TexturePacker) et matériau (albedo + normal + rugosité ; avec lui, les **variantes de shaders par renderer** : un renderer ou un matériau demande une clé shader + defines, normalisée (defines triés), un cache de programmes par clé évite de compiler deux fois la même variante, une garde limite l'explosion des variantes ; aujourd'hui seules les entrées de `shaders.json` et les defines globaux en créent), chaque partie une donnée à part entière (partageable, avec son repli), l'ensemble une donnée qui en dépend.
- **Modèles 3D depuis un jar** : Assimp lit un fichier du disque (`aiImportFile`), donc un modèle doit être dans une source sur le disque (sinon `ResourceException`), et son `.mtl` à côté de lui dans la même source ; lire par les sources avec `AIFileIO` (fonctions d'ouverture et de lecture données à Assimp), le `.mtl` compris.
- **Préchargement GL des modèles 3D** : `MeshUpload` est propre à chaque `ModelRenderer` (créé dans `launch`, exige le modèle chargé) ; le rendre partageable par modèle pour qu'un batch l'envoie pendant le chargement comme les textures (`DataType.createGLElement`).

## Budget mémoire et chargement en continu (quand un vrai jeu l'exige)

Les estimations de `DataType.estimatedCpuBytes`/`estimatedGpuBytes` ne servent aujourd'hui qu'au debug (`resources`, `resources batch`) ; aucune décision du moteur ne s'en sert.
- **Budget** : vérifier qu'un lot tient en mémoire (carte comprise), choisir quoi décharger pendant un chargement en continu, prévenir quand un mod est trop gros.
- **Manifeste généré à la compilation** (prérequis) : pour chaque ressource, ses dépendances et sa taille décodée, sans lire le fichier. Aujourd'hui les dépendances d'une donnée non lue ne sont connues qu'après sa lecture (`dependenciesKnown` dans `describeBatch`) et les estimations avant lecture sont des ratios par format (pansements). Il servirait aussi d'index des sources classpath : aujourd'hui une source dans un jar est listée en ouvrant le jar comme système de fichiers zip (`ClasspathSource`), ce qui exige un préfixe tenu par un seul jar.

## Effets 2D : particules et post-traitement
D'après LearnOpenGL « 2D Game » (Breakout).
- **Particules** (absentes) : émetteurs (position, forme, débit, rafales) ; durée de vie, vitesse, forces, couleur/taille/rotation/opacité dans le temps, texture ou région d'atlas ; mélange additif ou alpha ; pool sans allocation par frame ; émetteurs en JSON (`DataType`), injectables ; espace monde ou local.
- **Rendu** : simple d'abord, instancing une fois le regroupement des dessins fait (`current.md`).
- **Post-traitement** sur framebuffer (base `VirtualSceneRenderer`) : tremblement, flou, inversion, fondus, vignette ; enchaînables, par scène. La passe finale réglable existe (`IColorService`, `VirtualSceneRenderer`) : la chaîne s'y branche. Pistes liées au gamma : HDR et tone mapping (framebuffer `RGBA16F`), `EXT_texture_sRGB_decode` (lire une texture sRGB sans décodage pour un dessin gamma, au lieu de la réencoder dans le shader).
- `ParticlesScene` : plusieurs émetteurs, compteur et temps dans `stats`.

## Physique 2D : remplacer JBox2D
Exigences : 2D au pixel près, cas complexes compris ; 3D compatible plus tard. Une boîte poussée **reste collée** au joueur (aucun écart, aucun tremblement). Un projet C++ précédent a échoué sur le test ci-dessous.
- Constat : JBox2D 2.2.1.1 non maintenu, sans nom de module (module automatique `jbox2d.library`) (seulement `physics/B2Utils`, `Box2D`, `platformer2d`). Collisions maison (`physics/collision`, ~500 lignes : `CollisionRectangle`, `CollisionBoundedVolume2D`, `Collision2DGrid`, `replaceComparedTo`), base de la physique arcade.

### Unité du monde (à trancher au début du point, avec `/planification-ml`)
Aujourd'hui 1 unité du monde 2D = 1 pixel virtuel, en dur (`Camera2D.updateProjection` : `ortho` sur la taille virtuelle ; tailles en pixels : `setSizePix`, `setSizeToTexture`, tuiles de `TileMapRenderer`) ; `B2Utils.setB2ToEngineVector` est une échelle **statique globale** (un seul monde possible ; seul `PlatformerBox2D` l'utilise). Suffit à un pixel-art (comme Godot, Bevy), mais pas aux mètres que veut Box2D (objets mobiles de 0,1 à 10 m), ni à des images de résolutions différentes (images HD d'un mod).
- Cible : trois échelles séparées, défaut 1 (rien ne change pour l'existant). **Caméra** : pixels virtuels par unité, défaut du projet dans `EngineConfig`, valeur propre possible par `Camera2D`, distincte du zoom du joueur (le zoom fait déjà ce travail aujourd'hui) ; souris convertie en unités du monde. **Image** : taille d'un sprite en unités = pixels / PPU, `"pixelsPerUnit"` par texture dans `textures.json` et les tilesets (défaut du projet sinon), comme Unity. **Physique** : unités par mètre propres à chaque monde (fin du statique de `B2Utils`).
- GUI et console restent en pixels virtuels (espace écran).
- 3D : unité déjà libre (perspective, modèles dans leurs unités) ; viser une même unité du monde pour 2D et 3D (≈ 1 m, au choix du projet), le PPU donnant la taille d'un sprite ou d'un texte posé dans un monde 3D (`Game3D2D`). Rien à changer en 3D.
- À fixer au plus tard avant l'API Lua des entités (les distances du gameplay s'y figent) ; lien avec « Pixel perfect » (`futur-discussion.md`) et « Tailles automatiques ».

### Test d'acceptation : `PhysicsCratesScene`
Trois caisses empilées sur une plateforme mobile (horizontale et verticale) ; pousser celle du bas (la pile suit), puis celle du haut (elle seule bouge et tombe du bord) ; une caisse écrasée contre un mur. Aucun écart d'un pixel, aucun retard d'une frame, aucun tremblement.

### Physique arcade maison (principale)
Modèle solids/actors/riders (Celeste, TowerFall) étendu aux poussables empilés ; exposée en Lua (`physics.move` renvoie la position corrigée, comme `_PhysicsReplace` du C++).
- Positions entières + reste sous-pixel, déplacement axe par axe, pas fixe déterministe.
- **Graphe d'appui** reconstruit à chaque pas (A porté par B si le bas de A touche le haut de B).
- **Deux phases** : calculer jusqu'où la chaîne peut aller (la poussée se propage, s'arrête au premier mur), puis déplacer toute la chaîne du même delta dans le même pas.
- Les porteurs emportent ce qu'ils portent (récursif, du bas vers le haut).
- **Écrasement** → événement, le jeu décide.
- Aussi : tilemaps, plateformes traversables par-dessous, pentes (option), zones de déclenchement, catégories par nom (déclarées par le projet), raycasts, événements de contact, visualisation.

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
- **Thème** : `Theme` en JSON (couleurs, polices, tailles, espacements, styles par composant) géré par `ThemeManager`, lu par les composants GUI au lieu de valeurs en dur (`setFontSize(20)`, `new Color4f(0.5f)`, police d'interface choisie par `EngineConfig.setDefaultFont`).
- **Tailles automatiques** : unité relative à la résolution virtuelle, ancres et pourcentages, recalcul au redimensionnement.
- **Padding du texte** : utiliser `Text.setPadding` dans les composants GUI et l'exposer au thème.
- **Polices** : gestion des styles (une famille liée logiquement à ses variantes Regular, Bold, Italic, BoldItalic, `setFont(famille, style)` ; le moteur livre déjà les 4 atlas de chaque famille), polices sur plusieurs fichiers (pages, plusieurs tailles), texte net à toutes les tailles (atlas SDF/MSDF, ou atlas généré au lancement depuis le `.ttf`) ; à décider avec l'utilisateur.

## Rendu SVG
Après le rendu 2D (`current.md`). Contours épais, jointures, extrémités et anticrénelage par frange existent : `Stroker` (`StrokeGeometry`) et le shader `stroke2D` (`architecture.md`, Formes 2D), à réutiliser.
- Viser le noyau des profils légers (SVG Tiny/Native, NanoSVG) : chemins (segments, Bézier) remplis ou tracés ; texte converti en chemins à l'export.
- Sous-ensemble : chemins et formes de base, remplissage et contour unis, opacité, transformations, `viewBox` ; plus tard dégradés. Hors champ : texte, CSS, filtres, masques, SMIL.
- Approche (à confirmer) : `SVGData` (`DataType`, workers, NanoSVG de LWJGL) aplatit les chemins et triangule les remplissages, contours par `Stroker` ; `SVGRenderer extends Renderer` + shader `svg` (sur le modèle de `stroke2D`) ; un appel par SVG ; anticrénelage par frange comme `PolygonRenderer`. Plus tard si besoin : courbes dans le fragment shader (Loop-Blinn, SDF).
- `SVGScene` : icônes à plusieurs échelles.
- Avec le SVG : courbes de Bézier aplaties avant `Stroker`, dégradés, **tirets** (découpe de la polyligne avant `Stroker`).
- **Recouvrement d'un contour semi-transparent sur lui-même** : une polyligne qui se coupe (ou la pointe ouverte d'une flèche sur sa ligne) mélange deux fois la couleur aux croisements (les jointures, elles, ne se recouvrent pas) ; cible : stencil (forme dans le stencil, puis une passe de couleur) ou couverture maximale, à décider avec le SVG.

## Entrées : remappage et manettes
- Touches modifiables à l'exécution, sauvegardées/chargées depuis un fichier, sur les clés du registre (identifiants stables, `InputRegistry` ; liaisons par défaut déclarées par les plugins) ; scène prefab de changement des touches.
- **Liaisons des actions du jeu par lettre de la disposition** : la console reconnaît ses raccourcis par `KeyEvent.letter()` ; les liaisons du jeu (`ActionInput`, `ActionId`) restent par position physique (`EngineConfig.setKeyboardLanguage`) : à faire évoluer avec le remappage (lettre de la disposition ou position, au choix de l'action).
- **Manettes** (base faite : `GamepadManager`, joueurs, `InputGamepadButton`/`InputGamepadAxis`, virtuelles, commande `gamepad`). Reste :
  - mettre à jour `gamecontrollerdb.txt` du moteur de temps en temps (SDL_GameControllerDB ; le commit est noté dans `licence.md`), à la main ou par un script de `tools/`.
  - **Actions par joueur** (multijoueur local) : aujourd'hui joueur fixe ou `ANY_PLAYER` ; cible `inputManager.getState(action, joueur)`, clavier/souris attribués à un joueur comme une manette.
  - **Ensembles d'actions activables** (« jeu », « menu » : un même stick déplace et navigue, `MOVE_*_2D` et `SELECT_UP/DOWN`) au lieu de verrous par composant (`FreeCamera3DComponent.setLookLocked`).
  - Sensibilité du regard réglable (aujourd'hui `LOOK_SENSITIVITY` de `DemoPlugin`), séparable souris/manette, avec inversion verticale (`scale` négatif).
  - Remappage manette sauvegardé avec le clavier (`Gamepad.parseButton`/`buttonName`).
  - Vibrations, gyroscope : absents de GLFW (SDL ou XInput si besoin).
  - Icônes de boutons selon le modèle (Xbox, PlayStation, Switch) d'après nom/GUID.

## Son : streaming
Streaming des musiques longues (tout est décodé en mémoire).

## Console et GUI en jeu
- **Focus de la bibliothèque GUI** : `InputFocus` (pile de détenteurs par fenêtre) est la base ; reste la navigation Tab, le focus par composant (un seul `TextField` est prévu : avec plusieurs, un clic sur un champ pendant que l'autre a le focus doit passer de l'un à l'autre sans perdre le clic, aujourd'hui l'ordre de mise à jour décide), le focus au clavier et à la manette (aujourd'hui `MenuScene` code sa navigation champ → liste → Quit, parce que `GUIList` ne sait pas déléguer les flèches à un composant qui en a l'usage, comme `ScrollList` ou un curseur de texte), `ScrollList` qui devienne un `GUI`.
- **Overlay de debug** (stats, scène, entrées) à côté de la console. S'appuie sur thème, tailles et langues.
- **Compteurs `stats` injectables** par les plugins (nom, unité, remise à zéro), aujourd'hui fixés par le moteur.
- **Console** : thème (couleurs, hauteur de l'overlay, taille du texte ; la palette de `ConsoleColors` y passera) ; suggestions affichées sous la saisie en plus du Tab bash ; glyphe flèche dans l'indicateur des lignes sous la vue (les polices du moteur ont les flèches U+2190–2195 : il écrit encore « v N lines » au lieu de `↓`) ; annuler/refaire (Ctrl+Z/Y) dans la saisie, recherche dans le journal (Ctrl+F), menu clic droit, sélection rectangulaire ; mémoriser l'état de `console logs` et `console data` avec les réglages du joueur (Fenêtres / réglages) ; filtres par source et autres types exposés par commande (le modèle d'ensemble de types cachés existe, cf. Journal à grande échelle).
- **Commandes, suite** :
  - complétion dans `mighty.py` à partir du JSON de `help <cmd>`, et documentation des commandes générée depuis les schémas ;
  - conditions de multijoueur (commande réservée à l'hôte) : le mécanisme `requires(CommandCondition.hidden|check)` existe, les conditions viendront avec le réseau (permissions par joueur, `futur-discussion.md`) ;
  - commandes Lua : elles déclareront leur syntaxe par la même API (voir Entités et scripts Lua) ;
  - traduction des messages et aides des commandes du joueur (système de langues) : les descriptions de la syntaxe deviendront des clés de langue.
- **Journal à grande échelle** : l'anneau de `ConsoleLog` oublie au-delà de sa capacité ; cible durable : journal fichier tournant (cf. `futur-discussion.md`) relu par la console, filtres par type/source, niveaux de log au lieu de `System.out` préfixé `--`.

## Fenêtres
- **Sous-fenêtres natives hors Windows** : propriétaire de fenêtre sous Linux (X11 `XSetTransientForHint`, Wayland `xdg_toplevel.set_parent`) et macOS (`addChildWindow`) à la place du simple `GLFW_FLOATING` de `NativeWindowOwner`.
- **Réglages d'affichage du joueur** : `DisplaySettings` ↔ JSON dans `DataFolder`, menu d'options (modes, résolutions de `MonitorInfo.getVideoModes`, écran), position fenêtrée réglable ; écrans branchés/débranchés en cours de jeu (`glfwSetMonitorCallback` : une fenêtre dont l'écran disparaît revient sur le principal, aujourd'hui son `MonitorInfo` garde un handle invalide).
- **Fenêtre redimensionnée** : recalcul optionnel de la surface virtuelle (elle reste fixe, avec bandes noires) ; le jeu se fige pendant qu'on tire un bord ou déplace la fenêtre sous Windows (boucle modale dans `glfwPollEvents`, seule l'image est redessinée) : la cible durable est une boucle de jeu et de rendu sur un thread à elle, le thread principal ne lisant que les événements (change la règle « tout le GL sur le thread principal », à discuter). Icône par fenêtre, renvoi du focus au dialogue modal quand on clique sur la fenêtre parente, manettes bloquées par un dialogue modal.
- **`EngineStats` par fenêtre** (compteurs de frames et de swaps) et `Screenshot` par fenêtre dans la boucle.

## Distribution et versions (avant le premier jeu publié)
Pas avant : publier fige le dépôt, les versions et les statuts `@API` d'un moteur encore changé librement. D'ici là, un jeu sur la même machine dépend du moteur par `mvn install`.
- **Publication** : JitPack ou GitHub Packages ; versions sémantiques par release ; à partir de la première version publiée, japicmp compare l'API de chaque version à la précédente (les statuts `@API` disent ce qui peut changer). À décider : dépôt de publication, numéro de la première version, contrôle japicmp dans la CI ou en local.
- **Modules automatiques** : le moteur requiert trois modules nommés d'après leur jar (`jbox2d.library`, `jlayer`, `opensimplex`), que Maven déconseille de publier (le nom changerait avec le jar) : les remplacer, les embarquer ou fixer leur nom avant la première publication.
- **Image `jlink`** du jeu : doit contenir `jdk.zipfs` (requis par le module du moteur, résolu par `jlink` à partir du module du jeu).
- **Premier jeu hors du dépôt** : il vérifie ce que la démo ne couvre pas (ressources lues depuis le jar du jeu, aucun chemin relatif au dépôt).

## Poms des jeux : suites
- **Générateur de projet** (archetype Maven) : à partir du premier jeu hors du dépôt (`mightylib-game-parent`, `mightylib`, `mightylib.mainClass`, une scène 2D vide).
- **macOS** : le lancement exige `-XstartOnFirstThread`, que `exec:java` (même JVM) ne permet pas : prévoir `exec:exec` ou un lanceur (« Distribution et versions ») ; les natives macOS ne sont pas testées.
- **Plateforme non reconnue** : si aucun profil d'OS de `mightylib-base-parent` ne s'active (Linux x86 32 bits, FreeBSD…), `${lwjgl.natives}` reste tel quel et la résolution des natives de `mightylib-game-parent` échoue avec un message obscur. Prévoir un profil de repli (`activeByDefault` ou `!lwjgl.natives`) qui arrête le build avec un message clair (« Distribution et versions »).
- **`exec-maven-plugin` 1.2.1** (2011), transmis à tout jeu par `mightylib-base-parent` : passer à une version 3.x (vérifier `exec:java` et les arguments de la démo), en même temps que la question macOS ci-dessus.
- **Maven 4** : ses mixins pourraient simplifier la chaîne de parents.

## Intégrations plateformes
- **Discord** : présence enrichie (SDK Discord, bindings Java à évaluer).
- **Steam** : Steamworks (succès, overlay, statistiques), bindings Java à évaluer.
