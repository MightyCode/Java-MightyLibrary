# Techniques graphiques — Java-MightyLibrary

Techniques de rendu tirées de LearnOpenGL (« Advanced OpenGL », « Advanced Lighting », « PBR », articles invités), à faire **après `docs/later.md`**. Elles touchent la 2D et la 3D. Aucune n'existe encore dans le moteur. Pas d'ordre imposé au-delà des groupes ; retirer chaque technique une fois faite, ou la déplacer dans `docs/current.md` (après discussion avec l'utilisateur) quand elle devient actuelle. Chaque technique a sa scène `Test*`.

## 1. Simples et utiles
- **Blinn-Phong** : correction de l'éclairage Phong actuel (reflets justes aux angles rasants) ; quelques lignes dans les shaders d'éclairage.
- **Skybox (cubemaps)** : ciel en 6 images (`GL_TEXTURE_CUBE_MAP`) ; les cubemaps servent aussi aux reflets et à la réfraction.
- **Frustum culling** : ne pas dessiner ce qui est hors du champ de la caméra (boîtes englobantes). Même principe en 2D : ne pas dessiner ce qui est hors de l'écran de la `Camera2D`.

## 2. Rendu plus réaliste
- **Ombres (shadow mapping)** : rendu de la profondeur depuis la lumière ; lumières directionnelles, puis ponctuelles (cubemap de profondeur), puis cascades (CSM) pour les grandes scènes.
- **Normal mapping** : relief par texture de normales (espace tangent : `aiProcess_CalcTangentSpace` d'Assimp). Le sac à dos de démonstration fournit sa carte de normales.

## 3. Animation
- **Animation squelettique** : os et poids des sommets lus par Assimp, matrices des os envoyées au vertex shader, interpolation des images clés. Suite logique du chargement de modèles.

## 4. À la demande
- **HDR + bloom** : intensités au-delà de 1 ramenées à l'écran (tone mapping), halo autour des zones très lumineuses. Le bloom est aussi très utilisé en 2D (néons, sorts, particules) : il s'intègre au post-traitement de `later.md` (Effets 2D) et à la passe finale du gamma.
- **Deferred shading** : géométrie stockée d'abord (G-buffer), éclairage calculé une fois par pixel ; des centaines de lumières.
- **SSAO** : ombrage d'ambiance dans les recoins.
- **Parallax mapping** : relief marqué (pavés, briques) par texture de hauteur.
- **PBR** : éclairage physiquement réaliste (métal, rugosité, IBL) ; gros chantier, le standard actuel.
