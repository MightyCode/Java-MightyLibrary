# Discussions à mener — Java-MightyLibrary

Sujets à discuter et planifier avec l'utilisateur **avant** de reprendre la feuille de route (`docs/roadmap.md`, étape 0). Ordre convenu : le chargement de modèles 3D, puis les pistes LearnOpenGL (les TODO du code sont traités ou reportés dans la roadmap). Pour chaque point : noter la décision, l'appliquer (ou la reporter dans la roadmap), puis retirer le point. Supprimer ce fichier et l'étape 0 quand il est vide.

## 1. Chargement de modèles 3D (LearnOpenGL « Model Loading »)

Objectif de l'utilisateur : finir les pages Assimp, Mesh et Model. La 3D reste secondaire, mais cette partie est à terminer.

### Constat
- `lwjgl-assimp` est dans le pom mais **inutilisé** ; le chargement passe par `ObjModelLoader` (OBJ maison) : triangles `v/vt/vn` uniquement, pas de `.mtl`, un seul mesh, bug des coutures (TODO `SimpleModelRenderer.java:113` : sommet identifié par sa seule position, UV/normales écrasées ; réglé par `JoinIdenticalVertices`).
- `renderer/_3D/Mesh` et `MeshEntry` ne sont utilisés nulle part.
- L'éclairage est prêt : lumières directionnelle/ponctuelle/spot, `MultiLightningManager`, shader `textureMultipleLights3D` (ce que le tutoriel utilise pour le sac à dos).

### Plan proposé
1. **`Model` (CPU, workers)** : `aiImportFile` avec `Triangulate | FlipUVs | GenSmoothNormals | JoinIdenticalVertices` ; parcours récursif des nœuds (`processNode`) ; par mesh : positions, normales, UV, indices, matériau.
2. **Textures des matériaux** : chemins lus par `GetTexture`, relatifs au dossier du modèle ; chacune enregistrée comme `TextureData` nommée `modèle/fichier` et déclarée en dépendance du modèle. Le registre de `Resources` remplace le cache `textures_loaded` du tutoriel.
3. **`ModelRenderer`** : une `Shape` par mesh, envoyée un mesh par frame dans le budget de `GLResources` ; textures sur `material.diffuse` / `material.specular` (noms des shaders existants, au lieu de `texture_diffuse1`) ; texture par défaut sans carte spéculaire.
4. **Remplacement** de `ObjModelLoader`, `ModelTriangle`, `SimpleModelRenderer`, `Mesh`, `MeshEntry` ; migration du stand de `Test3DScene`.
5. **Scène `Test3DModelLoading`** : sac à dos éclairé par plusieurs lumières, vérification des fuites `gl`.

### Décisions
- Assimp remplace complètement le lecteur OBJ (lecture native, plus de formats, matériaux, plusieurs meshes).
- Les transformations des nœuds sont appliquées.
- Face culling activé pour les modèles dans ce même travail.
- Le sac à dos (`resources/models/backpack`, 40 Mo) n'est pas commité (`.gitignore`) : il fera partie d'un paquet de ressources de démonstration du moteur (voir étape 3 de la roadmap). Vérifier la licence Sketchfab (fichier d'attribution présent) avant de distribuer ce paquet.

### Reste à décider
- Autres dossiers non suivis : `resources/models/basicShape`, `weapon`, `world` (8,4 Mo) : commiter, ignorer, ou paquet de démonstration.
- Quand implémenter ce plan : juste après l'étape 0 (avant l'étape 1), ou à une étape de la roadmap.

## 2. Pistes tirées du reste de LearnOpenGL

À placer (ou non) dans la roadmap. Déjà présents dans le moteur : MSAA, blending, test de profondeur, callback de debug OpenGL, éclairage multiple.

- **Instancing / Advanced Data** (`glMapBufferRange`, `glBufferSubData`) : base du regroupement des dessins → étape 5.
- **Gamma Correction** (textures sRGB, `GL_FRAMEBUFFER_SRGB`) : rien n'est fait ; concerne aussi les couleurs 2D. À trancher tôt : l'activer plus tard change toutes les couleurs.
- **Breakout** (« In Practice ») : système de particules (absent du moteur) et effets de post-traitement sur framebuffer (secousse, flou, inversion), utiles en 2D.
- **Face culling** : gain facile en 3D (voir section 1).
- **Plus tard en 3D** : ombres, normal mapping (textures du sac à dos disponibles), skeletal animation via Assimp, frustum culling.
