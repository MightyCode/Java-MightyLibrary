# Licences de la bibliothèque

État au 2026-11-08, pour la branche `sound-decoding`. Vérifié dans les `pom.xml` des dépendances (dépôt Maven local), les dépôts officiels et les fichiers du projet. **Ce document n'est pas un avis juridique** : les points marqués « à vérifier » le sont réellement.

## En bref

- **Licence du code du projet : GPL v3** (fichier `LICENSE`). C'est le choix qui a le plus de conséquences, voir « Licence du projet ».
- **Aucune dépendance copyleft forte.** Toutes les bibliothèques Java sont permissives (BSD, MIT, Apache 2.0, domaine public) : elles imposent de garder leur avis de copyright, rien d'autre. Elles sont compatibles avec la GPL v3 comme avec une licence propriétaire.
- **Une seule composante LGPL, une bibliothèque native** : OpenAL Soft, livrée par LWJGL (`OpenAL.dll`). Elle reste remplaçable, voir plus bas.
- **Le point le plus risqué n'est pas le code mais les ressources** : polices Microsoft (Arial, Bahnschrift) et modèles, textures et sons dont l'origine n'est pas écrite dans le dépôt.

## Dépendances Java (livrées dans le jar `jar-with-dependencies`)

| Composant | Version | Licence | Rôle | Obligation |
|---|---|---|---|---|
| LWJGL (core, glfw, opengl, openal, stb, assimp) | 3.3.6 | BSD-3-Clause | fenêtre, GL, son, décodage ogg, modèles | garder l'avis de copyright et la licence |
| JOML | 1.10.3 | MIT | mathématiques 3D | garder l'avis |
| org.json | 20231013 | Domaine public (selon le `pom.xml`) | JSON | aucune ; pas la vieille licence « Good, not Evil » de json.org |
| JBox2D (`jbox2d-library`) | 2.2.1.1 | BSD-2-Clause (Daniel Murphy) | physique 2D | garder l'avis |
| opensimplex (`com.raylabz`) | 1.0.3 | Apache 2.0 | bruit | garder l'avis, le texte de la licence et un éventuel fichier `NOTICE` |
| JavaMP3 (`fr.delthas`) | 1.0.1 | MIT | décodage mp3 | garder l'avis ; dépôt archivé |

Code écrit pour le moteur, sans dépendance : décodeur FLAC (`FlacStream`), `SoundDecoder` et les autres décodeurs. Le wav utilise `javax.sound.sampled` (JDK). `SoundDataType.readVorbis` et `ioResourceToByteBuffer` ressemblent à l'exemple audio de LWJGL (BSD-3) : si elles en viennent, l'avis de LWJGL couvre ce cas, à vérifier si la provenance compte.

### Remarque sur les versions
Les `pom.xml` présents dans le dépôt Maven local pour LWJGL sont en 3.3.2 alors que le projet utilise la 3.3.6 : la licence (BSD-3-Clause) est la même d'une version à l'autre.

## Bibliothèques natives livrées par LWJGL

Les jars `natives-windows` / `natives-linux` embarquent des bibliothèques C :

| Composant | Licence | Remarque |
|---|---|---|
| OpenAL Soft (`OpenAL.dll`) | **LGPL 2** | Seule composante copyleft. Chargée dynamiquement (DLL séparée dans le jar, extraite à l'exécution) : l'utilisateur peut la remplacer, ce que la LGPL demande. Il faut mentionner OpenAL Soft et sa licence et ne pas empêcher le remplacement de la DLL (ne pas la fusionner dans l'exécutable). |
| GLFW | zlib | avis non obligatoire dans un binaire, souhaitable |
| Assimp | BSD-3-Clause | garder l'avis |
| stb (stb_image, stb_vorbis…) | domaine public ou MIT | aucune obligation |
| LWJGL (jemalloc, libffi…) | BSD-2 / MIT | à confirmer sur https://www.lwjgl.org/license |

## Outils de test et de build (non distribués)

JUnit 5 (EPL 2.0, tests seulement), Maven et ses plugins (Apache 2.0). Rien à joindre à un jeu.

## Licence du projet : GPL v3

Les auteurs de commits du dépôt sont tous la même personne (plusieurs noms), ce qui permet de **changer de licence sans accord de tiers**, tant qu'il n'y a pas de contribution externe. Conséquences :

- **GPL v3 telle quelle** : tout jeu ou logiciel qui utilise la bibliothèque (liaison avec le jar) doit lui-même être publié sous GPL v3 avec ses sources. C'est incompatible avec un jeu fermé ou vendu sans sources, et avec les mods sous licence propriétaire.
- **LGPL v3** : un jeu fermé peut utiliser la bibliothèque, à condition de laisser l'utilisateur la remplacer (bibliothèque en jar séparé, pas fusionnée dans un seul jar) et de publier les modifications de la bibliothèque elle-même.
- **MIT ou Apache 2.0** : aucune contrainte pour les jeux, un avis de copyright à garder. Apache 2.0 ajoute une clause de brevet. C'est le plus simple pour « bibliothèque réutilisable, modding ».
- Toutes les dépendances Java ci-dessus acceptent n'importe lequel de ces trois choix.
- **La démo** (`mightylibdemo`) peut avoir une autre licence que la bibliothèque (`mightylib`) si on veut.

Choix à faire par l'auteur. Le projet vise des jeux et logiciels propres à l'auteur et un modèle de modding (`docs/later.md`) : MIT ou Apache 2.0 évitent de se limiter plus tard.

## Ressources (images, modèles, sons, polices)

| Ressource | Origine connue | Statut |
|---|---|---|
| `models/backpack/*` | Berk Gedik (Sketchfab), adapté par Joey de Vries (LearnOpenGL), voir `source_attribution.txt` | **à vérifier** : licence de la page Sketchfab (probablement une licence Creative Commons avec attribution) ; l'attribution est obligatoire |
| `textures/fonts/arial.*` | police Arial (Microsoft), atlas généré avec AngelCode BMFont | **risque** : police propriétaire ; distribuer un atlas de ses glyphes avec un produit demande de vérifier la licence de la police |
| `textures/fonts/bahnschrift.*` | police Bahnschrift (Microsoft, fournie avec Windows) | **risque** : même question ; c'est la police d'interface par défaut du moteur et de l'écran de chargement |
| `textures/fonts/svbasicmanual.*` | police SV Basic Manual | **à vérifier** : licence inconnue (souvent « gratuit pour usage personnel ») |
| `models/stand/stall.obj`, `textures/world/stallTexture.png` | probablement un tutoriel OpenGL | **inconnu** |
| `textures/world/container*.png/jpg` | probablement LearnOpenGL | **inconnu**, à vérifier |
| `models/weapon`, `models/world/tree`, `models/basicShape` | inconnu | **inconnu** |
| `textures/world/*.png`, `gui/*`, `shaderEffects/*`, `icon/icon.ico` | inconnu (peut-être créés par l'auteur) | **à confirmer** |
| `sounds/select*` (ogg, wav, mp3, flac) | inconnu (probablement créés par l'auteur) | **à confirmer** |
| shaders GLSL (`resources/shaders`) | écrits pour le projet, techniques de LearnOpenGL (`docs/techniquegraphique.md`) | les idées ne sont pas protégées ; vérifier tout code copié |

Conséquence pratique : tant que la provenance d'une ressource n'est pas écrite, on ne peut pas dire si elle est redistribuable. Pour une version publique (jar sur JitPack ou Maven Central), les ressources de la démo sont séparées de la bibliothèque : **la bibliothèque seule n'a besoin que des polices et textures dont elle a vraiment besoin au démarrage** (la police de l'écran de chargement, `error`).

## À faire

1. **Choisir la licence de la bibliothèque** (GPL v3, LGPL v3, MIT, Apache 2.0) et remplacer `LICENSE` en conséquence.
2. **Créer `THIRD-PARTY-LICENSES`** (ou une page « À propos » dans les jeux) : une entrée par composant des tableaux ci-dessus, avec le texte de la licence.
3. **OpenAL Soft** : le mentionner, ne pas fusionner sa DLL.
4. **Polices** : remplacer Bahnschrift et Arial par des polices sous licence libre (SIL OFL : Roboto, Open Sans, Noto, DejaVu…) avant toute distribution ; vérifier SV Basic Manual.
5. **Ressources** : écrire l'origine et la licence de chaque fichier de `resources/` (un fichier `resources/CREDITS.md`), supprimer ce qu'on ne peut pas redistribuer. Garder l'attribution du sac à dos.
6. **Build** : si une licence impose que l'utilisateur puisse remplacer une bibliothèque, ne plus distribuer qu'un jar de bibliothèque sans `jar-with-dependencies` (le jar fusionné reste possible pour la démo).
7. **Mods** : quand le modèle de modding existera, dire qui possède la licence des mods et des ressources qu'ils apportent.
