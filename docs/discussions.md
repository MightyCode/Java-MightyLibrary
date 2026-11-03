# Discussions à mener — Java-MightyLibrary

Sujets à discuter et planifier avec l'utilisateur **avant** de reprendre la feuille de route (`docs/roadmap.md`, étape 0). Ordre convenu : d'abord les TODO, puis le chargement de modèles 3D, puis les pistes LearnOpenGL. Pour chaque point : noter la décision, l'appliquer (ou la reporter dans la roadmap), puis retirer le point. Supprimer ce fichier et l'étape 0 quand il est vide.

## 1. TODO restants dans le code

Déjà traités : `GLResources.addElementToUnload` (supprimé), `Renderer.unload()` (seul `unload()` reste).

### 1.1 `SimpleModelRenderer.java:113` — sommets en double
- Constat : `init()` identifie un sommet par sa seule position ; aux coutures, une même position a plusieurs UV/normales qui s'écrasent (texture et éclairage faux).
- Proposition : réglé par le passage à Assimp (`aiProcess_JoinIdenticalVertices`, sommets uniques par combinaison position/UV/normale), voir section 2. Rien à faire séparément.

### 1.2 `Shader.java:48` — séparer la partie OpenGL du shader
- Constat : `Shader` est un `DataType` en `MAIN_CONTEXT` qui lit les sources **et** compile/lie le programme GL (`internLoad`). Les textures, elles, sont déjà séparées : `TextureData` (CPU, workers) et `Texture` (`GLElement`, `GLResources`).
- Proposition : même découpage pour les shaders : sources lues par les workers (`ShaderSource` ou similaire), programme GL dans un `GLElement` géré par `GLResources` (chargement par tranches, compteur de références, suivi des fuites `gl`). Lié à l'étape 1 de la roadmap (chargement GL) et à l'injection de shaders par le projet (étape 3).
- À décider : faire maintenant, en étape 1, ou plus tard ; les noms des classes.

### 1.3 `ServerThread.java:24` — `run()` vide
- Constat : tout le package `mightylib/network` (`ServerTcp`, `ClientTcp`, threads, `NetworkConfiguration(Loader)`, ~650 lignes) n'est utilisé nulle part : aucune référence hors du package, le loader n'est pas enregistré. Le serveur de debug (`utils/enginecommand/DebugServer`) est indépendant.
- Options : (a) le supprimer (récupérable dans l'historique git) ; (b) le garder et le terminer (quel besoin : multijoueur, outils ?) ; (c) le sortir dans un module à part.
- Recommandation : (a), sauf besoin réseau prévu à court terme.

### 1.4 `Resources.java:365` — traitement immédiat mal placé
- Constat : `addResourceToProcess` traite tout de suite (`mainContextHandleResource`) les données `IMMEDIATELY_IN_CURRENT_CONTEXT` (`BatchResources`, atlas de `TileSetAtlas`, `TileMap` de `Test2DWaveFunctionCollapseScene`) au lieu de les mettre en file : la méthode dit « ajouter » mais exécute.
- Proposition : sortir ce cas en amont (là où l'entrée est créée), ou renommer la méthode (`processOrQueue`). Petit refactor, à faire avec l'étape 1 (noms trompeurs).

## 2. Chargement de modèles 3D (LearnOpenGL « Model Loading »)

Objectif de l'utilisateur : finir les pages Assimp, Mesh et Model. La 3D reste secondaire, mais cette partie est à terminer.

### Constat
- `lwjgl-assimp` est dans le pom mais **inutilisé** ; le chargement passe par `ObjModelLoader` (OBJ maison) : triangles `v/vt/vn` uniquement, pas de `.mtl`, un seul mesh, bug des coutures (1.1).
- `renderer/_3D/Mesh` et `MeshEntry` ne sont utilisés nulle part.
- L'éclairage est prêt : lumières directionnelle/ponctuelle/spot, `MultiLightningManager`, shader `textureMultipleLights3D` (ce que le tutoriel utilise pour le sac à dos).

### Plan proposé
1. **`Model` (CPU, workers)** : `aiImportFile` avec `Triangulate | FlipUVs | GenSmoothNormals | JoinIdenticalVertices` ; parcours récursif des nœuds (`processNode`) ; par mesh : positions, normales, UV, indices, matériau.
2. **Textures des matériaux** : chemins lus par `GetTexture`, relatifs au dossier du modèle ; chacune enregistrée comme `TextureData` nommée `modèle/fichier` et déclarée en dépendance du modèle. Le registre de `Resources` remplace le cache `textures_loaded` du tutoriel.
3. **`ModelRenderer`** : une `Shape` par mesh, envoyée un mesh par frame dans le budget de `GLResources` ; textures sur `material.diffuse` / `material.specular` (noms des shaders existants, au lieu de `texture_diffuse1`) ; texture par défaut sans carte spéculaire.
4. **Remplacement** de `ObjModelLoader`, `ModelTriangle`, `SimpleModelRenderer`, `Mesh`, `MeshEntry` ; migration du stand de `Test3DScene`.
5. **Scène `Test3DModelLoading`** : sac à dos éclairé par plusieurs lumières, vérification des fuites `gl`.

### Choix à valider
- Remplacer complètement le lecteur OBJ (recommandé) ou garder les deux.
- Appliquer les transformations des nœuds (recommandé : FBX/glTF en ont besoin ; le tutoriel les ignore).
- Activer le face culling pour les modèles dans ce même travail.
- Ressources non suivies : `resources/models/backpack` (40 Mo), `basicShape`, `weapon`, `world` (8,4 Mo). Proposition : ne pas commiter le sac à dos (`.gitignore`) sauf Git LFS ; vérifier la licence Sketchfab (fichier d'attribution présent) avant toute publication. Décider aussi pour les autres dossiers.

## 3. Pistes tirées du reste de LearnOpenGL

À placer (ou non) dans la roadmap. Déjà présents dans le moteur : MSAA, blending, test de profondeur, callback de debug OpenGL, éclairage multiple.

- **Instancing / Advanced Data** (`glMapBufferRange`, `glBufferSubData`) : base du regroupement des dessins → étape 5.
- **Gamma Correction** (textures sRGB, `GL_FRAMEBUFFER_SRGB`) : rien n'est fait ; concerne aussi les couleurs 2D. À trancher tôt : l'activer plus tard change toutes les couleurs.
- **Breakout** (« In Practice ») : système de particules (absent du moteur) et effets de post-traitement sur framebuffer (secousse, flou, inversion), utiles en 2D.
- **Face culling** : gain facile en 3D (voir section 2).
- **Plus tard en 3D** : ombres, normal mapping (textures du sac à dos disponibles), skeletal animation via Assimp, frustum culling.
