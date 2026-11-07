# Plus tard — Java-MightyLibrary

Feuille de route globale, dans l'ordre : ce fichier, puis `docs/techniquegraphique.md`. Le gros point en cours est dans `docs/current.md`. Quand il est terminé, le gros point suivant (en tête de ce fichier) est **discuté avec l'utilisateur** pour s'assurer que tout est prévu, puis déplacé dans `docs/current.md`.

## Gamma : rendu linéaire activable et réglable (en premier)
LearnOpenGL « Gamma Correction ». À faire en premier : l'activer change toutes les couleurs à l'écran, mieux vaut le décider avant d'accumuler du contenu.
- **Principe** : les écrans affichent `couleur^2.2` environ ; les images et les couleurs choisies à l'œil sont donc stockées en sRGB (déjà « pré-éclaircies »). Les calculs (éclairage, mélange alpha, filtrage, dégradés, particules additives) ne sont justes qu'en espace linéaire. Chaîne correcte : textures sRGB converties en linéaire à la lecture → calculs en linéaire → conversion en sRGB à l'écriture à l'écran.
- **Constat** : aucune gestion (pas de `GL_SRGB*`) : le moteur calcule directement en sRGB. Peu visible en 2D sans éclairage ni fondus (pixel art) ; éclairage 3D trop sombre/contrasté, fondus et dégradés ternes.
- **Règle** : couleurs (`Color4f`) et textures fournies par le projet sont en sRGB.
- **Activation simple** : option globale (`EngineConfig`) et par scène, désactivée par défaut, activée par défaut pour la 3D éclairée. Une fois activée : textures de couleur chargées en `GL_SRGB8_ALPHA8` (pas les cartes de données : normales, spéculaire, hauteurs), couleurs uniformes converties en linéaire, `GL_FRAMEBUFFER_SRGB` (ou conversion dans la passe finale si un framebuffer intermédiaire est utilisé, ex. `VirtualSceneRenderer`).
- **Réglage** : valeur de gamma/luminosité (réglage joueur classique) appliquée dans une passe finale (`pow(couleur, 1/gamma)`), modifiable à l'exécution et sauvegardable.
- Scène `TestGamma` : dégradés, fondus alpha, éclairage, option activée/désactivée côte à côte.

## Tests de non-régression
En place : JUnit 5 (`test/`, `mvn -o -q test`), premier ensemble `ResourcesLifecycleTest` (cycles charger/décharger de `Resources` avec des `FakeData` sans fichier ni GL, instance neuve par `Resources.createForTests`). À étendre : JUnit pour le reste de la logique pure (`InputSimulation`, tilemaps, maths, algorithmes, parsing des ressources) ; scénarios en jeu pilotés par `tools/mighty.py` (`scene`, `input`, `gl`, `stats`) qui vérifient des valeurs texte, sans capture d'écran ; contrôle des fuites par allers-retours de scènes. Filet de sécurité avant les gros chantiers suivants.

## Bibliothèque réutilisable

Faire de `mightylib` une base de code utilisée par les futurs logiciels et jeux de l'utilisateur ; le package `mightylibdemo` en est l'illustration. Cap validé :
- **Deux modules Maven** : `mightylib` (la bibliothèque, produit le jar) et `demo` (l'actuel `mightylibdemo`), qui dépend de `mightylib` comme un projet externe et prouve qu'elle s'utilise de l'extérieur.
- **Jar léger**, sans dépendances embarquées (pas de jar-with-dependencies pour la bibliothèque) : LWJGL (natives comprises), JOML, org.json, JBox2D… sont déclarées dans le pom et récupérées par Maven.
- **Distribution** : `mvn install` (dépôt local) au départ, puis JitPack ou GitHub Packages pour l'utiliser depuis n'importe quelle machine.
- **Versions** sémantiques par release (`1.2.0`…) : chaque projet fixe la version qu'il utilise.
- **Ressources** : séparer les ressources du moteur (shaders, texture `error`, polices par défaut), à embarquer dans le jar et lire depuis le classpath, des ressources du jeu, qui restent dans le dossier du projet utilisateur. Aujourd'hui tout est lu depuis le dossier `resources/` du disque : c'est le principal chantier. Un troisième ensemble : un **paquet de ressources de démonstration** (tout ce qui ne sert qu'à la démo, par exemple les modèles `resources/models/backpack`, `basicShape`, `weapon`, `world`, non commités, ignorés par `.gitignore`), distribué à part, pour montrer ce que fait le moteur ; la démo doit fonctionner, avec un message clair, quand il est absent.
- **API publique** : pas encore définie, **à discuter avec l'utilisateur avant toute décision** (ce qui sera public, changements cassants comme les singletons à faire avant une 1.0).

### Modèle d'extension validé : le projet injecte, le projet étend
- **Injection** : le projet fournit au moteur ce dont il a besoin via **un point d'injection unique**, appelé pendant une phase précise du démarrage (avant l'initialisation des ressources) : par exemple `register(EngineRegistry registry)`. Après le démarrage, le registre est figé. Remplace à terme `IProjectLoading.init`/`contextLoading` et les singletons modifiés depuis n'importe où.
- **Ce qui s'injecte** : sources de ressources (dossier du jeu, autres jars, en plus des ressources du moteur ; une ressource de même nom remplace celle du moteur), resource loaders et types de `DataType`, commandes, scènes enregistrées par nom, actions d'entrée et touches par défaut, shaders, scène de chargement par défaut, crochets de cycle de vie (démarrage, changement de scène, arrêt), compteurs pour `stats`, plus tard composants/thèmes GUI.
- **Extension par héritage** : les classes du projet héritent de celles du moteur (`Shape2DRenderer`, `Scene`, `DataType`/`ResourceLoader`, `BaseCommand`, composants GUI…). Les classes extensibles sont **choisies explicitement**, avec des points d'accroche prévus ; les autres sont fermées (`final` ou package-private), car tout membre `protected` devient un contrat envers les projets.

### Modding (secondaire, à garder en tête dans les choix de conception)
- Le point d'injection doit accepter **plusieurs modules** : le jeu est le premier, chaque mod en est un autre (jars d'un dossier `mods/` découverts par `ServiceLoader`, descripteur avec identifiant, version et dépendances qui fixent l'ordre d'enregistrement).
- **Identifiants avec espace de noms** (`jeu:slime`, `monmod:epee`) pour ressources, commandes, scènes et actions : aujourd'hui les noms de ressources sont globaux et les IDs d'action sont des `int` statiques (`InputManager.getAndIncrementId`), ce qui ferait entrer des mods en collision.
- **Remplacement de ressources** par identifiant, et à terme un **bus d'événements** pour que plusieurs modules puissent réagir et modifier (ex. ajouter un bouton au menu).
- Deux niveaux : mods de données (ressources/JSON, sans code) et mods de code (jars Java, sans sandbox possible : à n'installer que s'ils sont de confiance).
- Ne pas implémenter le modding maintenant, mais ne pas prendre de décision qui l'empêcherait.

## Effets 2D : particules et post-traitement
Inspiré de LearnOpenGL « In Practice / 2D Game » (Breakout). Un jeu sans particules n'est pas complet.
- **Particules** (absentes du moteur) : émetteurs (position, forme, débit, rafales), particules avec durée de vie, vitesse, gravité/forces, couleur/taille/rotation/opacité évoluant dans le temps, texture ou région d'atlas ; mélange additif ou alpha ; recyclage des particules (pool, aucune allocation par frame) ; émetteurs décrits en JSON (`DataType`) et injectables par le projet ; option de mise à jour en espace monde ou local.
- **Rendu** : une première version simple, puis passage à l'instancing quand le regroupement des dessins (Rendu 2D) sera fait (des milliers de particules en un appel).
- **Post-traitement** : effets sur l'image entière via un framebuffer (base : `VirtualSceneRenderer`) : tremblement, flou, inversion, fondus, vignette ; enchaînables et activables par scène. La passe finale du gamma (section Gamma) en fait partie.
- Scène `TestParticles` : plusieurs émetteurs, compteur de particules et temps dans `stats`.

## Physique 2D : remplacer JBox2D
Exigences de l'utilisateur : **fonctionnalités 2D primordiales**, au pixel près, cas complexes compris ; **3D secondaire mais compatible** (ne rien concevoir qui l'empêche). Une boîte poussée par le joueur doit **rester collée à lui** (aucun écart, aucun tremblement). Un projet C++ précédent a été abandonné faute d'avoir réussi le cas ci-dessous à la main : c'est le test d'acceptation.

### Constat
- **JBox2D 2.2.1.1** n'est plus maintenu ; utilisé seulement par `physics/B2Utils`, `TestBox2D` et `Platformer2D`.
- **Collisions maison** (`physics/collision`, ~500 lignes) : AABB `CollisionRectangle`, `CollisionBoundedVolume2D`, grille `Collision2DGrid`, `replaceComparedTo` (replacement selon une direction), `CollisionVisualisation`. Base de la physique arcade.

### Test d'acceptation : `TestPhysicsCrates`
Trois caisses empilées sur une plateforme mobile (horizontale et verticale) ; le joueur pousse la caisse du bas (la pile suit), puis celle du haut (elle seule bouge, puis tombe du bord) ; une caisse écrasée contre un mur. Aucun écart d'un pixel, aucun retard d'une frame, aucun tremblement ; la boîte poussée reste collée au joueur.

### Physique arcade maison (physique principale du moteur)
Modèle « solids / actors / riders » (Celeste, TowerFall) étendu aux objets poussables empilés ; c'est elle que l'API Lua expose (`physics.move` renvoie la position corrigée, comme `_PhysicsReplace` du projet C++).
- **Au pixel près** : positions entières + reste sous-pixel, déplacement axe par axe ; deux objets se touchent ou non, jamais d'écart d'arrondi. Pas fixe (déterministe).
- **Graphe d'appui** reconstruit à chaque pas : A est porté par B si le bas de A touche le haut de B (plateforme → caisse → caisse → joueur).
- **Déplacement en deux phases** : calculer jusqu'où toute la chaîne peut aller (une poussée se propage aux objets devant et s'arrête au premier mur), puis appliquer le même déplacement à toute la chaîne. Le pousseur et l'objet poussé bougent dans le même pas, d'où le contact permanent.
- **Les porteurs emportent ce qu'ils portent** (même delta, récursivement, du bas vers le haut) : pas de retard d'une frame.
- **Écrasement** : objet coincé entre un porteur et un mur → événement ; le jeu décide (mort, arrêt de la plateforme).
- Aussi : collisions avec les tilemaps, plateformes traversables par le dessous, pentes (option), zones de déclenchement, catégories de collision par nom (déclarées par le projet), raycasts, événements de contact, visualisation de debug.

### Corps rigides (bibliothèque, derrière une interface)
Pour ce qui tourne, s'empile librement ou s'articule (ponts, cordes, ragdolls, puzzles physiques). À la main : des mois pour faire moins bien.
- Interface `IPhysicsWorld2D` ; candidats : **box2d-jni** (fabmax, Box2D v3 natif, Maven Central, Windows/Linux/macOS ; compatibilité Java 11 et activité à vérifier), **gdx-box2d** (libGDX, Box2D v3.1), **dyn4j** (pur Java, BSD-3, 5.0.2 de mars 2024, Java 1.6+). Trancher en refaisant `TestBox2D` avec chacun.
- Coexistence avec la physique arcade (joueur arcade poussant des caisses simulées) : point délicat, à prototyper.
- **Au début du gros point** : prototype rapide de `TestPhysicsCrates` avec Box2D v3 pour comparer avant de s'engager.

### Compatibilité 3D
Concevoir les interfaces (monde, corps, requêtes, événements) pour qu'un monde 3D puisse s'y brancher plus tard (ex. Jolt via bindings Java) sans changer l'API des entités ni des scripts ; ne rien implémenter en 3D maintenant.

## Entités et scripts Lua
Besoin : dans un jeu (ex. plateformer), le moteur et le jeu exposent des fonctions que des scripts Lua utilisent pour faire agir les entités, modifiables sans relancer le moteur. L'utilisateur a déjà utilisé Lua ainsi dans un projet C++ avec un ami et en est très satisfait ; s'en inspirer (résumé ci-dessous) en corrigeant ses défauts.

### Prérequis : modèle d'entités
- **ECS et/ou graphe de scène** (déplacé de « Réflexions de conception ») : aujourd'hui une scène gère de simples listes d'objets à mettre à jour/afficher. Décider du modèle d'entités **avant** de les scripter ; à discuter au début de ce gros point.

### Bibliothèque
- **LuaJava de gudzpoz** (`party.iroiro.luajava`, MIT, Maven Central, ≥ 4.0.2) : vrai Lua (5.1 à 5.4) ou **LuaJIT** en natif par plateforme (comme LWJGL), Windows/Linux/macOS/Android ; secours LuaJ pur Java. **LuaJ** seul n'est plus maintenu (dernière version ~2015). Vérifier l'activité du projet au moment de l'adopter.
- Interface de script indépendante du langage côté moteur ; LuaJava en est la première implémentation.
- API **explicite** (fonctions enregistrées une par une), pas d'accès libre aux objets Java par réflexion : contrat clair, plus rapide, plus sûr pour les mods. Bibliothèques Lua limitées (pas de `io`/`os` libres).

### Modèle du projet C++ (référence)
- Un script par type d'entité, choisi depuis Tiled (propriété `LuaScript` de l'objet) ; propriétés Tiled copiées dans la mémoire de l'entité (`tiled_tileId`, `tiled_positionX/Y`, `tiled_sizeX/Y`, image…).
- Rappels appelés par le moteur : `Init`, `Update(dt)`, `Draw`, `DrawLight`, `TakeDamage(damage, direction)`, `SetState(state)` (signaux de logique).
- Drapeaux partagés lus par le moteur : `__canTakeDamage`, `__needBeDestroy`, `__needDrawSprite`, `__haveFinishChangeState`…
- Familles de fonctions `_Xxx(entityPtr, …)` : mémoire de l'entité, sauvegarde permanente (fichier de sauvegarde du joueur), sprite et animations (ajout, lecture, retournement, position, échelle, couleur, fin d'animation), sprite de lumière, collisions (catégories/masques, hitbox, déplacement physique qui renvoie la position corrigée, plateformes traversables, objets poussables, tests de contact avec le joueur), attaque et dégâts, logique (signaux entre objets activables : levier → porte), création d'entités, UI du jeu (vie du boss, textes de dégâts), dessin de formes de debug, tremblement d'écran, vibration manette, log, temps.

### Améliorations prévues par rapport au C++
- **Entité = objet Lua** (`self:playAnimation("run", true)`) plutôt que des fonctions globales `_Xxx(entityPtr, …)` ; fonctions rangées par module (`sprite`, `physics`, `save`, `ui`, `log`…).
- **Mémoire = simple table Lua** (`self.memory.hp = 3`) au lieu de `_AddField` / `_SetFloatValue` typés, ce qui supprime les erreurs int/float (« ajouter `.0` ») ; propriétés Tiled dans `self.props`.
- **Sauvegarde** : `save.get(clé)` renvoie `nil` si absente (au lieu de `{find, value}`) ; clés avec espace de noms par carte/entité.
- **Collisions par nom** (`"player"`, `"enemy"`, `"environment"`) déclarés par le projet, au lieu d'IDs entiers.
- **Signaux de logique** via le bus d'événements (voir Modding) plutôt que des IDs d'objets en dur.
- **Séparation moteur / jeu** : le moteur fournit le générique (sprites, animations, physique, sons, particules, caméra et tremblement, manettes, dessin de debug, log, temps, sauvegarde) ; le jeu enregistre ses propres fonctions (attaque du joueur, UI du boss…) par le point d'injection (Bibliothèque réutilisable).
- **Rechargement à chaud** : surveillance des fichiers `.lua`, rechargement du script en gardant la mémoire des entités ; commandes de debug (`lua reload`, `lua eval <code>`).
- **Erreurs** : une erreur de script est journalisée avec fichier:ligne et désactive l'entité fautive, sans arrêter le jeu.
- **Documentation de l'API** générée depuis les fonctions enregistrées (fichier comme celui du projet C++), pour ne jamais être en retard sur le code.
- À décider : un état Lua par scène (un environnement isolé par script) ou par entité ; coût des appels Java ↔ Lua mesuré (`stats`).
- Scène `TestLuaPlatformer` : joueur, ennemi scripté, levier et porte, rechargement à chaud démontré.

## Thème, tailles automatiques et langues
Dépend du point d'injection (Bibliothèque réutilisable) : thèmes et langues sont fournis par le projet (et les mods). Dès maintenant (`current.md`, corrections de l'API), ne pas figer d'API `Text`/GUI qui empêcherait les clés de langue et le thème.
- **Langues (l10n, inspirée de Flutter)** : un fichier par langue (`resources/lang/fr.json`, `en.json`, équivalent des ARB) chargé comme un `DataType` ; paramètres `{nom}` et pluriels simples ; langue de repli et log des clés manquantes ; un script `tools/` génère une classe de constantes typées (autocomplétion, fautes de clé détectées à la compilation) ; `Text` liable à une clé, mis à jour au changement de langue ; clés avec espace de noms (mods).
- **Thème** : objet `Theme` chargé depuis du JSON (couleurs, polices, tailles, espacements, styles par composant), géré par un `ThemeManager` ; les composants GUI le lisent au lieu de valeurs en dur (ex. `setFontSize(20)`, `new Color4f(0.5f)` dans `EnumSelector`/`StrSelector`).
- **Tailles automatiques** : unité indépendante de la résolution (facteur d'échelle par rapport à la résolution virtuelle), ancres et pourcentages, recalcul au redimensionnement de la fenêtre.
- **Padding du texte** : à reprendre de la branche `dev` (commit `40f4d8a`, `Text.setPadding(left, up, right, down)` + scène `TestGUI`). À corriger en reprenant : le commentaire annonce l'ordre « gauche, droite, haut, bas » alors que le code utilise « gauche, haut, droite, bas » ; les cas Left/Up font `-= padding` en supposant `textReference` à 0, à vérifier.

## Rendu 2D : formes manquantes et regroupement des dessins
- Existant : rectangle, ellipse, hexagone, polygone (`renderer/_2D/shape`), et des renderers de debug (graphes, nœuds, matrices). Le README indique « formes avec contours » non fait.
- À ajouter : segments et polylignes avec épaisseur, contours (outline) de toutes les formes, cercle/disque, triangle, rectangle arrondi, arc/secteur, points, flèches, grille ; et une API de dessin immédiat pour le debug (« dessine une ligne de A à B cette frame », utile pour la physique).
- **Point de départ sur la branche `dev`** (absent de `master`) :
  - `437b7ea` : `StrokeModule` réutilisable (activation, couleur, taille du contour via des valeurs de shader), contour dans le shader de l'ellipse, `Renderer.getShaderValue`/`hasShaderValue`, `Boolean`/`String` dans `ShaderValue`. Défauts : Java envoie `strokeSize` mais le shader lit `strokeSizePercent` (déclaré `in` et non `uniform`) → le contour ne fonctionne probablement pas ; `getSizePercent` lit la mauvaise clé ; `geEnabled` (faute) ; un contour en pourcentage du rayon n'a pas une épaisseur constante sur une ellipse aplatie → épaisseur en pixels.
  - `983c18c` (« Temporary ») : `AdvancedRectangle`, rectangle à coins arrondis par SDF (champ de distance signée) avec rotation et anticrénelage (`fwidth` + `smoothstep`). Le contour y est commenté. La technique SDF peut servir de base commune aux contours, cercles et rectangles arrondis. Ne pas reprendre les modifications de `Shader.java` (vérification des uniforms désactivée, voir `current.md`, uniforms absents ; message de log à supprimer).
  - Une fois repris, supprimer les branches `dev` et `devShader` (local et `origin`) ; `devShader` (`b50dd27`, ajout de `CheckOpenGLError` dans `Texture`) est déjà couvert par `master`.
- **Regroupement des dessins** : dessiner en un appel tout ce qui partage texture et shader. Complémentaire de `TextureDataAtlas` (un atlas réduit les changements de texture, seul le regroupement réduit les appels de dessin). Deux techniques à comparer sur mesures (`stats` : `drawCallsPerFrame`, temps CPU) :
  - **Batch** (style `SpriteBatch`) : le CPU transforme les sommets et les accumule dans un VBO dynamique, un `glDrawElements` ; pour le texte et les formes variées.
  - **Instancing** (OpenGL 3.3, LearnOpenGL « Instancing ») : un quad unitaire + des attributs par instance (position, taille, rotation, rectangle UV, couleur ; `glVertexAttribDivisor`), un `glDrawElementsInstanced`, transformation dans le vertex shader ; pour les sprites et les particules.
  - Dans les deux cas, remplir les buffers changeant à chaque frame efficacement (LearnOpenGL « Advanced Data » : `glMapBufferRange`, `glBufferSubData`).

## Rendu SVG
Après le rendu 2D : reprendre d'abord le travail des branches `dev` (`AdvancedRectangle`, `StrokeModule`, SDF), car les contours épais et l'anticrénelage y sont communs, et ce qui y changera conditionne cette étape.
- **Norme** : SVG (W3C) est trop vaste pour être géré en entier (texte, CSS, filtres, masques, scripts, animations). Viser le noyau commun aux profils légers (SVG Tiny, SVG Native, NanoSVG/VectorDrawable) : tout se ramène à des chemins (segments et Bézier) remplis ou tracés. Les SVG exportés d'Inkscape/Figma/Illustrator s'y ramènent si le texte est converti en chemins.
- **Sous-ensemble** : chemins et formes de base, remplissage et contour en couleur unie, opacité, transformations, `viewBox` ; plus tard dégradés linéaires et radiaux. Hors champ : texte, CSS, filtres, masques, animations SMIL.
- **Approche retenue (à confirmer)** : tessellation au chargement + shader simple. `SVGData` (`DataType`, workers) lit le fichier (NanoSVG, inclus dans LWJGL), aplatit les courbes et triangule remplissages et contours (épaisseur, jointures, extrémités) ; `SVGRenderer extends Renderer` + shader `svg` fourni par le moteur ; un appel de dessin par SVG, net à toute échelle ; anticrénelage par MSAA ou bande de fondu sur les bords.
- **Plus tard, si besoin** : courbes évaluées dans le fragment shader (Loop-Blinn, SDF), dans la lignée de `AdvancedRectangle`.
- Scène `TestSVG` : quelques icônes à plusieurs échelles.

## Entrées : remappage des touches et manettes
- Touches modifiables à l'exécution et sauvegardées/chargées depuis un fichier (le README liste aussi : chargement des entrées par fichier, scène prefab de changement des touches).
- **Manettes** : base faite (`GamepadManager`, plusieurs manettes, joueurs, actions `InputGamepadButton`/`InputGamepadAxis`, manettes virtuelles, commande `gamepad`). Reste :
  - **Actions par joueur** (cible durable pour le multijoueur local) : aujourd'hui une action se lie à un joueur fixe ou à `ANY_PLAYER`, il faudrait des liaisons `JUMP_P1`, `JUMP_P2`… Cible : évaluer une même table d'actions pour un joueur donné (`inputManager.getState(action, joueur)`), le clavier/souris étant attribué à un joueur comme une manette.
  - Actions d'axe : `InputManager.getValue` (0 à 1 par action) est fait ; reste une vraie action d'axe -1..1 (`MOVE_X` = stick ou A/D) au lieu de deux actions soustraites, à décider avec les actions par joueur.
  - Regarder autour en 3D au stick droit : sortir `Camera3D.rotate(dYaw, dPitch)` de `setToCursor` (vitesse en degrés/s pour le stick) ; idéalement un composant caméra libre 3D commun aux 5 scènes 3D qui recopient le code de déplacement.
  - Remappage des boutons de manette sauvegardé avec celui du clavier (`Gamepad.parseButton`/`buttonName` donnent déjà les noms).
  - Charger un `gamecontrollerdb.txt` du projet au démarrage (`GamepadManager.addMappings`) pour les manettes inconnues de la base intégrée à GLFW.
  - Vibrations et gyroscope : absents de GLFW (passer par SDL ou XInput directement si un jeu en a besoin).
  - Icônes de boutons selon le modèle de la manette (Xbox, PlayStation, Switch), d'après le nom/GUID.

## Son : formats
Constat : `SoundLoader.filterFile` n'accepte que `.ogg` (décodé par STB Vorbis) ; **le WAV n'est plus chargé** malgré le README. Rétablir le WAV, envisager d'autres formats, et le streaming pour les musiques longues (aujourd'hui tout le fichier est décodé en mémoire).

## Console et GUI en jeu
Une console affichée dans le jeu (base : `graphics/GUI/Terminal` et son système `guicommand`, à unifier avec `Commands` ; lire le texte par `KeyboardManager.getTypedText()` plutôt que par `KeyboardLanguage`, qui ignore la disposition réelle et les accents) et une GUI de base pour afficher des informations en jeu (overlay de debug : stats, scène, entrées). S'appuie sur le thème, les tailles automatiques et les langues (Thème, tailles automatiques et langues).

## Intégrations plateformes
- **Discord** : présence enrichie (statut de jeu) via le SDK Discord, bindings Java à évaluer.
- **Steam** : Steamworks (succès, overlay, statistiques…) ; il existe des bindings Java à évaluer.
