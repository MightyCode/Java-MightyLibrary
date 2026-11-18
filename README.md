# Java-MightyLibrary
A library to create a game.

## Paquet de démonstration
Le modèle 3D de `Scene3D` (`resources/models/backpack/`, environ 36 Mo) n'est pas dans le dépôt : il est distribué à part, dans une release GitHub (`demo-assets-N`). Pour l'installer : `python tools/demo_assets.py` (bibliothèque standard seulement, empreintes sha256 vérifiées ; `--check` vérifie sans réseau). Sans lui, la démo fonctionne et `Scene3D` affiche un message.

Crédits : « Survival guitar backpack » par Berk Gedik ([Sketchfab](https://sketchfab.com/3d-models/survival-guitar-backpack-799f8c4511f84fab8c3f12887f7e6b36)), licence CC BY 4.0, matériau adapté par Joey de Vries (LearnOpenGL).

## Checklist 

**Ressources :**
- Chargement automatique des ressources présentes dans le dossier ressources. ✅
- Chargement et déchargement de ressources par bloc. ✅
- Support des fichiers pratiques comme json, csv. ✅
- Chargement de fichiers atlas et de fichiers map (format propre au moteur). ✅
- Import de cartes Tiled (`.tmx`/`.tmj`). ❌
- Chargement threadé des ressources + parallélisation. ✅
- Rechargement à chaud des ressources, placeholders en cas d'erreur, préchargement de la scène suivante. ✅

**Graphiques :**
- Support shaders. ✅
- Support geometry shaders. ✅
- Envoie des informations aux shader seulement quand nécessaire. ✅
- Forme avec contours. ❌
- Chargement de SVG. ❌

**2D :**
- Camera complexe 2D ✅
- Framebuffer utilisable comme texture ✅
- Texture 2D ✅
- Animation 2D, animator et chargement du fichier associé ✅

**3D :**
- Cube texturé paramétrable, utilisation normale ✅
- Sphere normalisé ✅
- Texture 3D ✅
- Utilisation model simple 3D .obj ✅
- Chargement modèle plus complexe avec matériaux. ❌
- Camera complexe 3D ❌
 
**GUI :**
- Texte / Bouton textuel ✅
- Gestion menu (ensemble de boutons) ✅
- Sliders ✅
- Champs textuels ✅

**Entrées :**
- Interface clavier / souris complète ✅
- Interface manette ✅
- Chargement des inputs utilisés par l'app via fichier ❌
- Prefab scene pour changer les touches ❌

**Son :**
- Chargement fichier ogg, wav, mp3, flac ✅ (un format de plus : un `SoundDecoder` ajouté par `SoundLoader.addDecoder`)
- Gestion simple des sources 2D et 3D ✅
- Arbre de gain + chargement via fichier ✅

**Utilitaires :**
- Support toute résolution ✅
- Fonctions tweenings ✅
- Chargement des ressources par workers et préchargement de la scène suivante ✅
- Système de detection de collision basique ✅
- Intégration de box2D pour une utilisation potentielle ✅
