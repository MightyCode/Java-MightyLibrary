# Feuille de route — Java-MightyLibrary

Objectifs dans l'ordre ; retirer chaque objectif une fois atteint.

## Feuille de route (dans l'ordre ; retirer chaque objectif de cette section une fois atteint)

### 0. Discussions en cours (maintenant)
Discuter avec l'utilisateur des sujets de `docs/discussions.md`, dans l'ordre : TODO restants, chargement de modèles 3D (Assimp), pistes LearnOpenGL. Reporter chaque décision (code ou roadmap), puis supprimer ce fichier et cette étape.

### 1. API : corrections urgentes (après l'étape 0)
Propositions commentées, **à valider avec l'utilisateur avant d'appliquer**. Les changements cassants se font maintenant, avant toute version publiée ; marquées (U) = les plus urgentes.
- (U) **Noms de packages** `MightyLibrary.mightylib` en majuscules (contraire aux conventions Java) → `mightylib` ou un nom de domaine inversé, à faire avant la première version publiée.
- (U) **Multithreading** : `ThreadManager` ne fait que compter les threads et, s'il n'y en a plus, `RunThread` exécute la tâche dans le thread appelant (parfois le thread GL) ; chaque chargement crée de nouveaux `ResourceWorker extends Thread` ; réseau, `DebugServer`, console et `Screenshot` créent leurs threads chacun de leur côté. **Bug** : `Resources.mainContextHandleResource` (`LOAD`) attend avec `Thread.sleep(10)` sur le thread GL tant que `canBeLoad()` est faux, ce qui fige le rendu → remettre l'entrée en file. Cible :
  - un `ExecutorService` de taille fixe possédé par le moteur (arrêté à la sortie), tâches renvoyant des `CompletableFuture` ; suppression de `ThreadManager`/`ThreadOrCurrent` et de `LoadingScene`/`LoadingContent` (inutilisées) ;
  - une file `runOnMainThread(Runnable)` vidée à chaque frame avec un budget de temps, seule façon de revenir sur le thread GL ;
  - une vérification du thread GL dans `GenDeleteResources` (sous `Logger.DO_CHECK`) pour détecter tout appel GL hors du thread principal ;
  - threads réseau/debug nommés et daemon.
- **Chargement GL, suite** (le chargement par tranches avec le reste de la frame est fait, mesuré par `TestGLLoading`) : PBO, ou contexte GL partagé dans un second thread (fenêtre GLFW cachée + `glFenceSync`), seulement si des mesures montrent que les bandes ne suffisent pas (dépendant des pilotes, complexe). Les modèles 3D (`SimpleModelRenderer.init(int)`, `HeightMapRenderer`, `TerrainRenderer`) envoient encore leur géométrie d'un bloc.
- **Uniforms absents d'un shader** : le compilateur GLSL retire les uniforms inutilisés, et `updateShaderValue` sur un tel uniform lève une exception (contournée sur la branche `dev` par un `if (false)`) → avertissement unique par shader/uniform plutôt qu'exception.
- **Singletons statiques** (`Resources`, `GLResources`, `ShaderManager`, `ContextManager`, `SoundManager`) et `Resources.getInstance()` qui crée une instance par défaut en silence → accès via un contexte moteur, ou au minimum échec explicite. Lié à l'injection (étape 3).
- **IDs d'action `int` statiques** (`InputManager.getAndIncrementId`) → identifiants texte avec espace de noms enregistrés au registre. Lié au remappage (étape 7) et au modding.
- **Champs publics modifiables** (`Resources.Loaders`, `GUIList.GUIs`, `ShouldLoop`, `WindowCreationInfo.Size`…) → méthodes.
- **Fautes et noms trompeurs** : package `lightning` (= `lighting`), `getCurrentTIme`, `canBeLoad`…
- **`Resources.addResourceToProcess`** (TODO `Resources.java`) : « ajoute » mais exécute tout de suite les données `IMMEDIATELY_IN_CURRENT_CONTEXT` → traiter ce cas en amont, ou renommer (`processOrQueue`).

### 2. Tests de non-régression
Méthode à définir avec l'utilisateur. Pistes : JUnit pour la logique pure (ressources, références, `InputSimulation`, tilemaps, maths, algorithmes) ; scénarios en jeu pilotés par `tools/mighty.py` (`scene`, `input`, `gl`, `stats`) qui vérifient des valeurs texte, sans capture d'écran ; contrôle des fuites par allers-retours de scènes. Filet de sécurité avant les gros chantiers suivants.

### 3. Bibliothèque réutilisable

Faire de `mightylib` une base de code utilisée par les futurs logiciels et jeux de l'utilisateur ; le dossier `project` en est l'illustration. Cap validé :
- **Deux modules Maven** : `mightylib` (la bibliothèque, produit le jar) et `demo` (l'actuel `project`), qui dépend de `mightylib` comme un projet externe et prouve qu'elle s'utilise de l'extérieur.
- **Jar léger**, sans dépendances embarquées (pas de jar-with-dependencies pour la bibliothèque) : LWJGL (natives comprises), JOML, org.json, JBox2D… sont déclarées dans le pom et récupérées par Maven.
- **Distribution** : `mvn install` (dépôt local) au départ, puis JitPack ou GitHub Packages pour l'utiliser depuis n'importe quelle machine.
- **Versions** sémantiques par release (`1.2.0`…) : chaque projet fixe la version qu'il utilise.
- **Ressources** : séparer les ressources du moteur (shaders, texture `error`, polices par défaut), à embarquer dans le jar et lire depuis le classpath, des ressources du jeu, qui restent dans le dossier du projet utilisateur. Aujourd'hui tout est lu depuis le dossier `resources/` du disque : c'est le principal chantier.
- **API publique** : pas encore définie, **à discuter avec l'utilisateur avant toute décision** (ce qui sera public, changements cassants comme les singletons à faire avant une 1.0).

#### Modèle d'extension validé : le projet injecte, le projet étend
- **Injection** : le projet fournit au moteur ce dont il a besoin via **un point d'injection unique**, appelé pendant une phase précise du démarrage (avant l'initialisation des ressources) : par exemple `register(EngineRegistry registry)`. Après le démarrage, le registre est figé. Remplace à terme `IProjectLoading.init`/`contextLoading` et les singletons modifiés depuis n'importe où.
- **Ce qui s'injecte** : sources de ressources (dossier du jeu, autres jars, en plus des ressources du moteur ; une ressource de même nom remplace celle du moteur), resource loaders et types de `DataType`, commandes, scènes enregistrées par nom, actions d'entrée et touches par défaut, shaders, scène de chargement par défaut, crochets de cycle de vie (démarrage, changement de scène, arrêt), compteurs pour `stats`, plus tard composants/thèmes GUI.
- **Extension par héritage** : les classes du projet héritent de celles du moteur (`Shape2DRenderer`, `Scene`, `DataType`/`ResourceLoader`, `BaseCommand`, composants GUI…). Les classes extensibles sont **choisies explicitement**, avec des points d'accroche prévus ; les autres sont fermées (`final` ou package-private), car tout membre `protected` devient un contrat envers les projets.

#### Modding (secondaire, à garder en tête dans les choix de conception)
- Le point d'injection doit accepter **plusieurs modules** : le jeu est le premier, chaque mod en est un autre (jars d'un dossier `mods/` découverts par `ServiceLoader`, descripteur avec identifiant, version et dépendances qui fixent l'ordre d'enregistrement).
- **Identifiants avec espace de noms** (`jeu:slime`, `monmod:epee`) pour ressources, commandes, scènes et actions : aujourd'hui les noms de ressources sont globaux et les IDs d'action sont des `int` statiques (`InputManager.getAndIncrementId`), ce qui ferait entrer des mods en collision.
- **Remplacement de ressources** par identifiant, et à terme un **bus d'événements** pour que plusieurs modules puissent réagir et modifier (ex. ajouter un bouton au menu).
- Deux niveaux : mods de données (ressources/JSON, sans code) et mods de code (jars Java, sans sandbox possible : à n'installer que s'ils sont de confiance).
- Ne pas implémenter le modding maintenant, mais ne pas prendre de décision qui l'empêcherait.

### 4. Thème, tailles automatiques et langues
Dépend du point d'injection (étape 3) : thèmes et langues sont fournis par le projet (et les mods). Dès l'étape 1, ne pas figer d'API `Text`/GUI qui empêcherait les clés de langue et le thème.
- **Langues (l10n, inspirée de Flutter)** : un fichier par langue (`resources/lang/fr.json`, `en.json`, équivalent des ARB) chargé comme un `DataType` ; paramètres `{nom}` et pluriels simples ; langue de repli et log des clés manquantes ; un script `tools/` génère une classe de constantes typées (autocomplétion, fautes de clé détectées à la compilation) ; `Text` liable à une clé, mis à jour au changement de langue ; clés avec espace de noms (mods).
- **Thème** : objet `Theme` chargé depuis du JSON (couleurs, polices, tailles, espacements, styles par composant), géré par un `ThemeManager` ; les composants GUI le lisent au lieu de valeurs en dur (ex. `setFontSize(20)`, `new Color4f(0.5f)` dans `EnumSelector`/`StrSelector`).
- **Tailles automatiques** : unité indépendante de la résolution (facteur d'échelle par rapport à la résolution virtuelle), ancres et pourcentages, recalcul au redimensionnement de la fenêtre.
- **Padding du texte** : à reprendre de la branche `dev` (commit `40f4d8a`, `Text.setPadding(left, up, right, down)` + scène `TestGUI`). À corriger en reprenant : le commentaire annonce l'ordre « gauche, droite, haut, bas » alors que le code utilise « gauche, haut, droite, bas » ; les cas Left/Up font `-= padding` en supposant `textReference` à 0, à vérifier.

### 5. Rendu 2D : formes manquantes et regroupement des dessins
- Existant : rectangle, ellipse, hexagone, polygone (`renderer/_2D/shape`), et des renderers de debug (graphes, nœuds, matrices). Le README indique « formes avec contours » non fait.
- À ajouter : segments et polylignes avec épaisseur, contours (outline) de toutes les formes, cercle/disque, triangle, rectangle arrondi, arc/secteur, points, flèches, grille ; et une API de dessin immédiat pour le debug (« dessine une ligne de A à B cette frame », utile pour la physique).
- **Point de départ sur la branche `dev`** (absent de `master`) :
  - `437b7ea` : `StrokeModule` réutilisable (activation, couleur, taille du contour via des valeurs de shader), contour dans le shader de l'ellipse, `Renderer.getShaderValue`/`hasShaderValue`, `Boolean`/`String` dans `ShaderValue`. Défauts : Java envoie `strokeSize` mais le shader lit `strokeSizePercent` (déclaré `in` et non `uniform`) → le contour ne fonctionne probablement pas ; `getSizePercent` lit la mauvaise clé ; `geEnabled` (faute) ; un contour en pourcentage du rayon n'a pas une épaisseur constante sur une ellipse aplatie → épaisseur en pixels.
  - `983c18c` (« Temporary ») : `AdvancedRectangle`, rectangle à coins arrondis par SDF (champ de distance signée) avec rotation et anticrénelage (`fwidth` + `smoothstep`). Le contour y est commenté. La technique SDF peut servir de base commune aux contours, cercles et rectangles arrondis. Ne pas reprendre les modifications de `Shader.java` (vérification des uniforms désactivée, voir étape 1 ; message de log à supprimer).
  - Une fois repris, supprimer les branches `dev` et `devShader` (local et `origin`) ; `devShader` (`b50dd27`, ajout de `CheckOpenGLError` dans `Texture`) est déjà couvert par `master`.
- **Regroupement des dessins** (style `SpriteBatch`) : accumuler les quads de même texture/shader dans un buffer dynamique et dessiner en un appel. Complémentaire de `TextureDataAtlas` (un atlas réduit les changements de texture, seul le batch réduit les appels de dessin). À décider sur mesures (`stats` : `drawCallsPerFrame`).

### 6. Rendu SVG
Après l'étape 5 : reprendre d'abord le travail des branches `dev` (`AdvancedRectangle`, `StrokeModule`, SDF), car les contours épais et l'anticrénelage y sont communs, et ce qui y changera conditionne cette étape.
- **Norme** : SVG (W3C) est trop vaste pour être géré en entier (texte, CSS, filtres, masques, scripts, animations). Viser le noyau commun aux profils légers (SVG Tiny, SVG Native, NanoSVG/VectorDrawable) : tout se ramène à des chemins (segments et Bézier) remplis ou tracés. Les SVG exportés d'Inkscape/Figma/Illustrator s'y ramènent si le texte est converti en chemins.
- **Sous-ensemble** : chemins et formes de base, remplissage et contour en couleur unie, opacité, transformations, `viewBox` ; plus tard dégradés linéaires et radiaux. Hors champ : texte, CSS, filtres, masques, animations SMIL.
- **Approche retenue (à confirmer)** : tessellation au chargement + shader simple. `SVGData` (`DataType`, workers) lit le fichier (NanoSVG, inclus dans LWJGL), aplatit les courbes et triangule remplissages et contours (épaisseur, jointures, extrémités) ; `SVGRenderer extends Renderer` + shader `svg` fourni par le moteur ; un appel de dessin par SVG, net à toute échelle ; anticrénelage par MSAA ou bande de fondu sur les bords.
- **Plus tard, si besoin** : courbes évaluées dans le fragment shader (Loop-Blinn, SDF), dans la lignée de `AdvancedRectangle`.
- Scène `TestSVG` : quelques icônes à plusieurs échelles.

### 7. Entrées : remappage des touches et manettes
- Touches modifiables à l'exécution et sauvegardées/chargées depuis un fichier (le README liste aussi : chargement des entrées par fichier, scène prefab de changement des touches).
- **Support des manettes** (GLFW gamepad : boutons et axes), intégré au même modèle d'actions et remappable comme le clavier.

### 8. Son : formats
Constat : `SoundLoader.filterFile` n'accepte que `.ogg` (décodé par STB Vorbis) ; **le WAV n'est plus chargé** malgré le README. Rétablir le WAV, envisager d'autres formats, et le streaming pour les musiques longues (aujourd'hui tout le fichier est décodé en mémoire).

### 9. Console et GUI en jeu
Une console affichée dans le jeu (base : `graphics/GUI/Terminal` et son système `guicommand`, à unifier avec `Commands`) et une GUI de base pour afficher des informations en jeu (overlay de debug : stats, scène, entrées). S'appuie sur le thème, les tailles automatiques et les langues (étape 4).

### 10. Réflexions de conception
- **ECS et/ou graphe de scène** : aujourd'hui une scène gère de simples listes d'objets à mettre à jour/afficher. Décider du modèle d'entités.
- **Alternative à Box2D** (secondaire) : JBox2D 2.2.1.1 n'est plus maintenu ; comparer une solution maison (le moteur a déjà des collisions maison) et des bibliothèques maintenues.

### 11. Intégrations plateformes
- **Discord** : présence enrichie (statut de jeu) via le SDK Discord, bindings Java à évaluer.
- **Steam** : Steamworks (succès, overlay, statistiques…) ; il existe des bindings Java à évaluer.

## Décisions prises
- **Package `network` conservé** bien qu'inutilisé (`ServerThread.run()` vide) : utilisable un jour, ne pas le supprimer.
- **Pas de Lombok** : gain de tokens faible (peu d'accesseurs triviaux), méthodes générées introuvables par recherche dans le source, processeur d'annotations à configurer (Maven + IntelliJ) et API publique implicite, contraire au choix d'une API publique explicite.
