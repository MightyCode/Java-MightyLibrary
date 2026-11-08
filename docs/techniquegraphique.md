# Techniques graphiques

LearnOpenGL (Advanced OpenGL, Advanced Lighting, PBR, articles invités), **après `later.md`**. 2D et 3D ; aucune n'existe encore. Ordre par groupe seulement ; une technique faite est retirée, une technique devenue actuelle passe dans `current.md` après discussion. Chacune a sa scène de démo.

## 1. Simples et utiles
- **Blinn-Phong** : corrige le Phong actuel (reflets aux angles rasants).
- **Skybox** (`GL_TEXTURE_CUBE_MAP`) ; cubemaps aussi pour reflets et réfraction.
- **Frustum culling** (boîtes englobantes) ; en 2D, ne pas dessiner hors de la `Camera2D`.

## 2. Rendu plus réaliste
- **Shadow mapping** : directionnelles, puis ponctuelles (cubemap de profondeur), puis cascades (CSM).
- **Normal mapping** (espace tangent : `aiProcess_CalcTangentSpace`) ; le sac à dos de démo a sa carte.

## 3. Animation
- **Squelettique** : os et poids via Assimp, matrices des os au vertex shader, interpolation des clés.

## 4. À la demande
- **HDR + bloom** (tone mapping) ; le bloom sert aussi en 2D (néons, sorts), intégré au post-traitement de `later.md` (Effets 2D) et à la passe finale du gamma.
- **Deferred shading** (G-buffer) : des centaines de lumières.
- **SSAO**, **parallax mapping**.
- **PBR** (métal, rugosité, IBL) : gros chantier.
