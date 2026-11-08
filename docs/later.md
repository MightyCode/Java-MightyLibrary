# Plus tard

Feuille de route, dans l'ordre (suite : `techniquegraphique.md`). Gros point en cours : `current.md`. Quand il est fini, **discuter** du point en tête de ce fichier avec l'utilisateur, puis le déplacer dans `current.md`.

## Gamma : rendu linéaire activable et réglable (en premier)
LearnOpenGL « Gamma Correction ». En premier car il change toutes les couleurs à l'écran.
- Constat : aucun `GL_SRGB*`, calculs en sRGB → éclairage 3D trop sombre, fondus et dégradés ternes (peu visible en pixel art).
- Règle : `Color4f` et textures du projet sont en sRGB.
- Activation : option `EngineConfig` + par scène, désactivée par défaut, activée par défaut en 3D éclairée. Active : textures de couleur en `GL_SRGB8_ALPHA8` (pas les cartes de données : normales, spéculaire, hauteurs), couleurs uniformes converties en linéaire, `GL_FRAMEBUFFER_SRGB` ou conversion dans la passe finale si framebuffer intermédiaire (`VirtualSceneRenderer`).
- Réglage joueur : gamma/luminosité dans une passe finale (`pow(c, 1/gamma)`), modifiable à l'exécution, sauvegardé.
- `TestGamma` : dégradés, fondus alpha, éclairage, activé/désactivé côte à côte.

## Tests de non-régression
À étendre (base JUnit 5 : `architecture.md`, liste dans `CLAUDE.md`) : JUnit pour la logique pure (`InputSimulation`, tilemaps, maths, algorithmes, parsing) ; scénarios en jeu via `tools/mighty.py` vérifiant du texte ; fuites par allers-retours de scènes. Filet avant les gros chantiers suivants.

## Bibliothèque réutilisable
`mightylib` = base des futurs jeux/logiciels ; `mightylibdemo` l'illustre. Cap validé :
- **Deux modules Maven** : `mightylib` (jar) et `demo`, qui en dépend comme un projet externe.
- **Jar léger** : dépendances (LWJGL et natives, JOML, org.json, JBox2D…) déclarées dans le pom, pas embarquées.
- **Distribution** : `mvn install`, puis JitPack ou GitHub Packages. **Versions** sémantiques par release.
- **Ressources** (principal chantier, tout est lu aujourd'hui depuis `resources/` sur disque) : celles du moteur (shaders, texture `error`, polices par défaut) dans le jar, lues depuis le classpath ; celles du jeu dans le dossier du projet ; un **paquet de démonstration** à part (modèles `backpack`, `basicShape`, `weapon`, `world`, non commités, dans `.gitignore`) : la démo fonctionne sans, avec un message clair.
- **API publique** : **à discuter avant toute décision** (périmètre, changements cassants comme les singletons avant une 1.0).

### Modèle d'extension validé : le projet injecte, le projet étend
- **Injection** : un point unique appelé à une phase précise du démarrage (avant l'init des ressources), ex. `register(EngineRegistry)`, registre figé ensuite. Remplace `IProjectLoading.init`/`contextLoading` et les singletons modifiés de partout.
- **Injectable** : sources de ressources (dossier du jeu, autres jars ; un nom identique remplace celui du moteur), loaders et `DataType`, commandes, scènes par nom, actions et touches par défaut, shaders, scène de chargement par défaut, crochets de cycle de vie (démarrage, changement de scène, arrêt), compteurs `stats`, plus tard composants/thèmes GUI.
- **Héritage** : classes extensibles **choisies explicitement** avec points d'accroche (`Shape2DRenderer`, `Scene`, `DataType`/`ResourceLoader`, `BaseCommand`, composants GUI…) ; les autres `final` ou package-private (tout `protected` devient un contrat).

### Modding (secondaire, ne rien décider qui l'empêche ; pas à implémenter maintenant)
- Le point d'injection accepte **plusieurs modules** : le jeu, puis chaque mod (jars de `mods/` via `ServiceLoader`, descripteur id/version/dépendances fixant l'ordre).
- **Identifiants à espace de noms** (`jeu:slime`) pour ressources, commandes, scènes, actions : aujourd'hui noms de ressources globaux → collisions (IDs d'action : `current.md`).
- La liste des scènes du menu de la démo (`MenuScene.createSceneEntries`) devra venir du registre de scènes par nom, pas d'une liste écrite en dur.
- Remplacement de ressources par id ; à terme **bus d'événements** (plusieurs modules réagissent, ex. ajouter un bouton au menu).
- **Rechargement à chaud des ressources des mods** : l'observateur de fichiers (`ResourceWatcher`) suit les ressources du jeu ; il devra suivre aussi les dossiers et ressources de chaque mod, et les remplacements de ressources par id.
- Mods de données (JSON/ressources) et mods de code (jars, sans sandbox : de confiance seulement).

## Chargement : optimisations seulement si les mesures ou un vrai jeu l'exigent

- **Shaders** : `GL_KHR_parallel_shader_compile` (lancer toutes les compilations, puis interroger `GL_COMPLETION_STATUS_KHR` aux frames suivantes ; une étape = un `glCompileShader` bloquant) ; pic de frame de ~58 ms au chargement, jugé acceptable.
- **Priorité plus fine** que courant/préchargement (rechargement à chaud, ressources du premier affichage) ; une donnée garde sa priorité urgente si la référence urgente est retirée avant la préchargée.
- **Chargement GL** : PBO ou contexte partagé (fenêtre cachée + `glFenceSync`). `HeightMapRenderer` et `TerrainRenderer` envoient encore leur géométrie d'un bloc.
- **Préchargement GL des modèles 3D** : `MeshUpload` est propre à chaque `ModelRenderer` (créé dans `launch`, exige le modèle chargé) ; le rendre partageable par modèle pour qu'un batch l'envoie pendant le chargement comme les textures (`DataType.createGLElement`).

## Effets 2D : particules et post-traitement
D'après LearnOpenGL « 2D Game » (Breakout).
- **Particules** (absentes) : émetteurs (position, forme, débit, rafales) ; durée de vie, vitesse, forces, couleur/taille/rotation/opacité dans le temps, texture ou région d'atlas ; mélange additif ou alpha ; pool sans allocation par frame ; émetteurs en JSON (`DataType`), injectables ; espace monde ou local.
- **Rendu** : simple d'abord, instancing une fois le regroupement des dessins fait (Rendu 2D).
- **Post-traitement** sur framebuffer (base `VirtualSceneRenderer`) : tremblement, flou, inversion, fondus, vignette ; enchaînables, par scène. Inclut la passe finale du gamma.
- `TestParticles` : plusieurs émetteurs, compteur et temps dans `stats`.

## Physique 2D : remplacer JBox2D
Exigences : 2D au pixel près, cas complexes compris ; 3D compatible plus tard. Une boîte poussée **reste collée** au joueur (aucun écart, aucun tremblement). Un projet C++ précédent a échoué sur le test ci-dessous.
- Constat : JBox2D 2.2.1.1 non maintenu (seulement `physics/B2Utils`, `TestBox2D`, `Platformer2D`). Collisions maison (`physics/collision`, ~500 lignes : `CollisionRectangle`, `CollisionBoundedVolume2D`, `Collision2DGrid`, `replaceComparedTo`, `CollisionVisualisation`), base de la physique arcade.

### Test d'acceptation : `TestPhysicsCrates`
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
- Candidats : **box2d-jni** (fabmax, Box2D v3 natif, Maven Central, Windows/Linux/macOS ; Java 11 et activité à vérifier), **gdx-box2d** (libGDX, Box2D v3.1), **dyn4j** (pur Java, BSD-3, 5.0.2 de mars 2024, Java 1.6+). Trancher en refaisant `TestBox2D` avec chacun.
- Coexistence avec l'arcade (joueur arcade poussant des caisses simulées) : à prototyper.
- Au début du point : prototype de `TestPhysicsCrates` avec Box2D v3 pour comparer.

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
- **Rechargement à chaud** des `.lua` en gardant la mémoire des entités ; commandes `lua reload`, `lua eval <code>`.
- Erreur de script → log fichier:ligne, entité désactivée, le jeu continue.
- Documentation de l'API générée depuis les fonctions enregistrées.
- À décider : état Lua par scène (environnement isolé par script) ou par entité ; coût Java ↔ Lua mesuré dans `stats`.
- `TestLuaPlatformer` : joueur, ennemi scripté, levier et porte, rechargement à chaud.

## Thème, tailles automatiques et langues
Dépend du point d'injection (thèmes et langues fournis par le projet et les mods). Dès maintenant, ne pas figer d'API `Text`/GUI qui empêcherait clés de langue et thème.
- **Langues** (à la Flutter) : `resources/lang/<langue>.json` chargé comme `DataType` ; paramètres `{nom}`, pluriels simples ; langue de repli, log des clés manquantes ; script `tools/` générant des constantes typées ; `Text` liable à une clé, mis à jour au changement de langue ; clés à espace de noms.
- **Thème** : `Theme` en JSON (couleurs, polices, tailles, espacements, styles par composant) géré par `ThemeManager`, lu par les composants GUI au lieu de valeurs en dur (`setFontSize(20)`, `new Color4f(0.5f)` dans `EnumSelector`/`StrSelector`).
- **Tailles automatiques** : unité relative à la résolution virtuelle, ancres et pourcentages, recalcul au redimensionnement.
- **Padding du texte** : reprendre `40f4d8a` (branche `archive-dev-2025`) (`Text.setPadding(left, up, right, down)`, scène `TestGUI`). À corriger : le commentaire dit « gauche, droite, haut, bas », le code fait « gauche, haut, droite, bas » ; Left/Up font `-= padding` en supposant `textReference` à 0, à vérifier.

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
- `TestSVG` : icônes à plusieurs échelles.

## Entrées : remappage et manettes
- Touches modifiables à l'exécution, sauvegardées/chargées depuis un fichier ; scène prefab de changement des touches.
- **Manettes** (base faite : `GamepadManager`, joueurs, `InputGamepadButton`/`InputGamepadAxis`, virtuelles, commande `gamepad`). Reste :
  - **Actions par joueur** (multijoueur local) : aujourd'hui joueur fixe ou `ANY_PLAYER` ; cible `inputManager.getState(action, joueur)`, clavier/souris attribués à un joueur comme une manette.
  - **Ensembles d'actions activables** (« jeu », « menu » : un même stick déplace et navigue, `MOVE_*_2D` et `SELECT_UP/DOWN`) au lieu de verrous par composant (`FreeCamera3DComponent.setLookLocked`).
  - Sensibilité du regard réglable (aujourd'hui `LOOK_SENSITIVITY` de `ProjectLoading`), séparable souris/manette, avec inversion verticale (`scale` négatif).
  - `MovingCameraComponent` (2D) : passer aux axes comme `FreeCamera3DComponent`.
  - Remappage manette sauvegardé avec le clavier (`Gamepad.parseButton`/`buttonName`).
  - Charger un `gamecontrollerdb.txt` du projet (`GamepadManager.addMappings`).
  - Vibrations, gyroscope : absents de GLFW (SDL ou XInput si besoin).
  - Icônes de boutons selon le modèle (Xbox, PlayStation, Switch) d'après nom/GUID.

## Son : streaming
Streaming des musiques longues (tout est décodé en mémoire).

## Console et GUI en jeu
Console en jeu (base `graphics/GUI/Terminal` et `guicommand`, à unifier avec `Commands` ; lire le texte par `KeyboardManager.getTypedText()`, pas `KeyboardLanguage`) et overlay de debug (stats, scène, entrées). S'appuie sur thème, tailles et langues.

## Intégrations plateformes
- **Discord** : présence enrichie (SDK Discord, bindings Java à évaluer).
- **Steam** : Steamworks (succès, overlay, statistiques), bindings Java à évaluer.
