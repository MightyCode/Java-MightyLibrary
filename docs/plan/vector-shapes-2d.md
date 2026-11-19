# Formes 2D vectorielles : reprise d'`archive-dev-2025`, formes manquantes et contours

**Statut : validé.** Objectifs « Reprise d'`archive-dev-2025` » et « Formes manquantes et contours » de `docs/current.md`.

Décisions prises :
- Modèle hybride : formes analytiques en **quad + SDF** dans le fragment shader ; polylignes, flèches et polygones quelconques en **géométrie CPU** (module de tracé) avec frange d'anticrénelage.
- Shaders : un include commun `utils/shape2D.glsl` + un shader court par forme ; la fusion en un shader à type par instance est laissée à l'objectif « Regroupement des dessins ».
- Épaisseur : unités du monde par défaut (suit le zoom), option par renderer en pixels d'écran.
- `RectangleRenderer` (sprites) inchangé ; les formes vectorielles partagent sa base `Shape2DRenderer` (même API de position, taille, référence, rotation, couleur) et **peuvent toujours être remplies d'une texture** (teintée par la couleur, découpée par la forme).
- Contour : méthodes sur la base (`setStrokeWidth`, `setStrokeColor`, `setStrokeAlignment`, `setStrokeUnit`), épaisseur 0 = sans contour ; `StrokeModule` de l'archive non repris.
- Alignement du contour : intérieur / centré / extérieur, **centré par défaut** (Processing, SVG, Canvas).
- Jointures onglet (limite, repli en biseau) / arrondie / biseau ; extrémités plate / arrondie / carrée.
- `EllipseRenderer` migré en SDF, `HexagonRenderer` devient un `RegularPolygonRenderer` préréglé, `PolygonRenderer` reste géométrique avec contour et frange ; changements cassants assumés.
- Noms : base `VectorShape2DRenderer` ; `RoundedRectangleRenderer`, `CircleRenderer`, `EllipseRenderer`, `TriangleRenderer`, `ArcRenderer`, `RegularPolygonRenderer`, `HexagonRenderer`, `PointsRenderer`, `GridRenderer` ; `PolygonRenderer`, `PolylineRenderer`, `ArrowRenderer` ; module `Stroker`.
- `VectorShape2DRenderer` extensible (déclarée dans `ExtensibleClasses`) : un projet ajoute sa forme SDF ; formes concrètes `final`.
- Branches `archive-dev-2025` et `devShader` (locales et `origin`) supprimées à la fin, après confirmation.

## Pour démarrer
- Lire : `docs/current.md` ; `docs/architecture.md`, section « Rendu » (valeurs d'uniform du `Renderer`, bibliothèque GLSL et `#include`, couleurs et gamma) ; `docs/decisions.md` (philosophie, champs publics) ; `docs/later.md`, section « Rendu SVG » (réutilisera `Stroker`) ; `docs/debug-tools.md` (`scenario.py`, `screenshot --rect`, commande `camera`).
- Branche de sujet depuis `dev` : `feature/vector-shapes`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `mightylib/.../graphics/renderer/_2D/shape/` : `Shape2DRenderer` (abstraite, extensible : `MeshRenderer` avec VBO positions (canal 0) et UV (canal 1), géométrie envoyée au `display` suivant par `getIndices`/`calculatePosition`/`calculateTexturePosition`, direction et point de référence, `setSizePix`, `setTexturePosition`), `RectangleRenderer` (extensible, quad, sprites, 45 usages), `EllipseRenderer` (`final`, hérite de `RectangleRenderer`, `ShaderEllipseProperty` dans la liste de `PublicFieldsTest`), `HexagonRenderer`, `PolygonRenderer` (polygone convexe en éventail, sinon `Polygon.earClipIndices`).
- Shaders : `mightylib/src/main/resources/assets/mightylib/shaders/` ; `shaders.json` (`colorShape2D`, `colorEllipse2D`, `texture2D`, `multiColoredShape2D`…) ; `utils/colorSpace.glsl` (`mlLinearColors`, `colorIn`, `textureIn`) ; `utils/colorsManipulation.glsl` (`decodeColor(int)` : couleur `Color4f` empaquetée, uniform générique `color`).
- Défauts actuels : l'ellipse découpe au pixel (`discard`, aucun anticrénelage) et calcule en pixels d'écran depuis `view[0][0]`/`projection[1][1]` → fausse avec une caméra tournée ; rectangle, hexagone, polygone sans anticrénelage (MSAA ×4 demandé sur la fenêtre, mais les vues dessinent dans des FBO non multi-échantillonnés) ; aucun contour, aucune ligne épaisse.
- `ShaderValue` ne gère pas `Boolean` : passer les drapeaux en `float`/`int`.
- Archive (à lire, pas à fusionner) : `git show 437b7ea` (contour d'ellipse en % du rayon, `StrokeModule`) ; `git show 983c18c` (`AdvancedRectangle` et `2D/complex/advancedRectangle2D{V,F}.glsl` : SDF de rectangle arrondi `length(max(q,0)) + min(max(q.x,q.y),0) - r`, anticrénelage `fwidth` + `smoothstep`). Ne pas reprendre ses changements de `Shader.java` ni de `Resources.java`.
- Usages des formes migrées : `demo/.../scenes/Scene2D.java` (hexagones `colorShape2D` et `texture2D`, ellipse, deux polygones). GUI et console n'utilisent que `RectangleRenderer`.
- Tests proches : `PublicFieldsTest`, `ExtensionFieldsTest`/`ExtensibleClasses`, `ApiStatusTest`, `ShaderPreprocessor` (includes), géométrie (`utils/math`, `Polygon`).

## Décisions
- **Repère local** : le fragment shader travaille dans le repère de la forme (origine au centre, unités du monde), transmis par le vertex shader depuis les sommets du quad ; aucune dépendance à la vue → zoom, rotation de la forme et de la caméra justes. Les uniforms `ellipse.center/radius/rotation` disparaissent (la rotation passe par la matrice `model`).
- **Include `utils/shape2D.glsl`** (inclut `colorsManipulation.glsl`) : fonctions de distance (`sdRoundedBox`, `sdCircle`, `sdEllipse` approchée par f/|∇f|, `sdTriangle`, `sdArc`, `sdPie`, `sdRegularPolygon`), taille d'un pixel dans le repère local (`fwidth`), et une fonction de composition `shapeColor(distance, …)` : couverture du remplissage et du contour selon l'alignement, anticrénelage d'un pixel par `smoothstep`, remplissage couleur ou texture (`textureIn`, UV locales mappées sur `texturePosition`) teinté par la couleur, sortie avec alpha × couverture, `discard` à couverture nulle. Les formules sont réécrites (source citée en commentaire, aucun code copié).
- **Paramètres lus via des `in`** posés par le vertex shader à partir d'uniforms : le regroupement les remplacera par des attributs par instance sans toucher à l'include.
- **Marge du quad** : le quad s'étend de la moitié du contour extérieur + 1 pixel d'anticrénelage ; calculée dans le vertex shader (la taille du pixel vient des matrices), donc juste aussi en unités d'écran et au zoom, sans recalcul CPU.
- **Unité d'épaisseur** : `EStrokeUnit.WORLD` (défaut) / `SCREEN_PIXELS`, pour les contours et les lignes ; le rayon des coins et la taille des points restent en unités du monde.
- **Anticrénelage** : couverture dans l'alpha, mélange dans le mode de couleur du dessin (linéaire ou gamma), aucune correction propre à chaque mode ; option `setAntialiasing(false)` par forme (bords nets, pixel art).
- **Remplissage et contour** : couleur de remplissage = couleur générique (`setColorMode`) ; alpha 0 = contour seul ; texture par `setMainTextureChannel` comme les autres renderers.
- **Formes SDF** : `RoundedRectangleRenderer` (rayon par coin ou commun ; 0 = rectangle net avec contour), `CircleRenderer`, `EllipseRenderer`, `TriangleRenderer` (trois sommets locaux), `ArcRenderer` (angle de départ, ouverture, rayon intérieur : arc épais, secteur, anneau), `RegularPolygonRenderer` (côtés, `HexagonRenderer` = 6 côtés, garde `STRETCH_RATIO`), `PointsRenderer` (N points rond ou carré, un maillage de quads, un appel), `GridRenderer` (un quad, lignes périodiques avec pas, origine, épaisseur, couleur).
- **`Stroker`** (Java pur, sans GL, package `graphics/renderer/_2D/shape` ou sous-package, à placer selon `ApiStatusTest`) : une polyligne ouverte ou fermée → sommets (position de base, vecteur d'extrusion, côté, couverture) et indices ; jointures et extrémités ; jointures sans recouvrement (onglet, biseau, éventail arrondi) ; points dupliqués et segments nuls ignorés, demi-tour géré. Le **vertex shader extrude** `position + extrusion × demi-épaisseur` (épaisseur convertie du monde ou de l'écran) et ajoute la frange d'un pixel : pas de recalcul CPU au zoom. Réutilisé par le futur `SVGRenderer`.
- **Formes géométriques** : `PolylineRenderer` (points, fermée ou non, jointure, extrémité, limite d'onglet), `ArrowRenderer` (polyligne + pointe : longueur, largeur, pleine ou ouverte), `PolygonRenderer` (remplissage par éventail/oreilles + frange par le tracé du bord avec couverture, contour par `Stroker`).
- **Classes** : `VectorShape2DRenderer extends Shape2DRenderer`, extensible, accroches documentées (envoi des paramètres de la forme, marge propre si la forme dépasse son rectangle) ; toutes les formes concrètes `final`. `ShaderEllipseProperty` supprimée (retirée de `PublicFieldsTest`).

## Étapes (un commit chacune)
1. **Base et rectangle arrondi** : `utils/shape2D.glsl`, `VectorShape2DRenderer` (contour, alignement, unité, anticrénelage, marge), `RoundedRectangleRenderer` + `2D/shape/roundedRectangle2D{V,F}.glsl` déclaré dans `shaders.json` (reprise de la SDF de `983c18c`). `ExtensibleClasses`, `package-info` si nouveau package. Scène `VectorShapes2D` (voir Démo) avec rectangles : plein, contour seul, trois alignements, unités monde et écran, texturé, tourné. Tests : logique sans GL extraite (marge du quad selon alignement et unité, valeurs envoyées). Vérification : `python tools/scenario.py "scene VectorShapes2D" wait-loaded "?gl summary"` + `screenshot --rect` sur un coin arrondi, puis caméra zoomée et tournée (`camera`).
2. **Formes SDF** : `CircleRenderer`, `EllipseRenderer` migré (suppression de `colorEllipse2D`), `TriangleRenderer`, `ArcRenderer`, `RegularPolygonRenderer`, `HexagonRenderer` migré ; `Scene2D` mis à jour (hexagone texturé par `setMainTextureChannel`). Ajouts à `VectorShapes2D`. Vérification en jeu de `Scene2D` et `VectorShapes2D`, caméra tournée.
3. **Points et grille** : `PointsRenderer`, `GridRenderer` ; ajouts à la scène. Peut être fusionnée seule après 1.
4. **`Stroker`** : génération des sommets, Java pur. Tests JUnit : nombre de sommets et indices par jointure et extrémité, repli de l'onglet au-delà de la limite, polyligne fermée, points dupliqués, segments colinéaires, demi-tour, extrusions unitaires, aucun triangle dégénéré.
5. **Polylignes, flèches, polygone** : `PolylineRenderer`, `ArrowRenderer`, shader d'extrusion `2D/shape/stroke2D{V,F}.glsl`, `PolygonRenderer` migré (frange + contour). Ajouts à la scène : toutes jointures et extrémités, ligne fine en pixels d'écran sous zoom, polygone concave avec contour, polygone de `Scene2D`.
6. **Vérifications d'ensemble** : modes de couleur (bords des formes en linéaire et en gamma : `ColorSpaceSplit2D` ou commande `colorspace` sur `VectorShapes2D`), fuites (`leakcheck VectorShapes2D`), scènes 3D lancées (`Scene3D`, `Game3D2D`), `mvn -o -q test`, `package`.
7. **Docs et branches** : documents ci-dessous ; montrer à l'utilisateur ce qu'`archive-dev-2025` et `devShader` contiennent encore hors de `dev` (`git log dev..archive-dev-2025 --oneline`, idem `devShader`), puis, sur sa confirmation, les supprimer en local et sur `origin`.

## Démo
Nouvelle scène `VectorShapes2D` inscrite dans `DemoScenes` (description) : grille de toutes les formes, chacune pleine / contour seul / texturée ; ligne des trois alignements ; polylignes avec toutes jointures et extrémités ; flèches ; points ; grille de fond par `GridRenderer` ; touches pour zoomer et tourner la caméra et basculer l'unité des contours (monde / écran) et l'anticrénelage.

## Documents à mettre à jour à la fin
- `architecture.md`, « Rendu » : formes vectorielles (SDF sur quad, repère local, include, contour, unités, marge), `Stroker` et extrusion dans le vertex shader, liste des formes, `RectangleRenderer` = sprites.
- `debug-tools.md` : vérifier une forme (scène, `camera`, capture ciblée) si utile.
- `decisions.md` : modèle hybride SDF / géométrie et pourquoi ; contour centré par défaut.
- `later.md` : « Rendu SVG » s'appuie sur `Stroker` ; recouvrement des contours semi-transparents sur une polyligne qui se coupe (stencil, avec le SVG) ; dessin immédiat de debug (déjà dans `current.md`).
- `CLAUDE.md` : tests couverts (`Stroker`, marge des formes).
- `current.md` : supprimer les lignes « Reprise d'`archive-dev-2025` » et « Formes manquantes et contours » ; garder dans « Regroupement » la fusion possible des shaders de formes en un shader à type par instance.
- `licence.md` : rien si aucun code tiers n'est copié.
- Supprimer ce plan (discussion B).

## Hors périmètre
- Regroupement et instancing des formes, shader unique à type par instance : objectif « Regroupement des dessins » (`current.md`).
- API de dessin immédiat de debug : objectif « Passage des renderers 2D au regroupement » (`current.md`).
- Courbes de Bézier, dégradés, tirets : « Rendu SVG » (`later.md`) ; tirets à ajouter là.
- Recouvrement d'un contour semi-transparent sur lui-même (polyligne qui se coupe) : `later.md`, avec le SVG.
- Formes en 3D : aucune.

## Questions restantes
Aucune.

## Modèles recommandés
- Discussion A, étapes 1 à 5 : Claude Opus 5.5, effort `high` (API extensible et include durables, shaders SDF, géométrie du tracé délicate).
- Discussion A, étapes 6 et 7 : Claude Sonnet 5.5, effort `medium` (vérifications, docs).
- Discussion B : Claude Opus 5.5, effort `high` (gros diff Java + GLSL à relire au regard du plan).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/vector-shapes-2d.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `feature/vector-shapes`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (`VectorShapes2D` : bords lisses au zoom et caméra tournée, contours monde/écran, jointures ; `Scene2D` : hexagones, ellipse, polygones ; `ColorSpaceSplit2D` : bords en linéaire et gamma). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml feature/vector-shapes`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
