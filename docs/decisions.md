# Décisions durables

- **Placeholders pour les seules données primaires** (texture, son, shader, police, animation, modèle) : une erreur de ressource ne fait pas tout planter, en debug comme chez le joueur. Données complexes (cartes, JSON, règles…) → `null`, un placeholder y cacherait l'erreur. Placeholder voyant/audible (damier magenta, bip), aussi pendant le chargement. Mode strict = exception, pour les tests et `scenario.py`.
- **Mode `DEBUG`/`RELEASE`** (`EEngineMode`) plutôt qu'un état « admin ».
- **Package `network` conservé** malgré `ServerThread.run()` vide.
- **Pas de Lombok** : peu d'accesseurs triviaux, méthodes générées introuvables dans le source, processeur d'annotations à configurer, API publique implicite.
- **Envoi des textures : `DIRECT` par défaut, `PBO` en option** (`ETextureUpload`). Mesuré sur RTX 4080 SUPER (pilote 610.88), 4096x4096 : direct 8 ms au total ; PBO 3 ms dans les appels mais 36 ms jusqu'à la fin du transfert. Sur Test3DScene l'envoi entier (128 Mo) ne coûte que ~15 ms, rien à gagner là. Le résultat dépend de la carte : `textureupload bench` le mesure chez l'utilisateur.
- **Packages `io.github.mightycode.mightylib` / `…mightylibdemo`** (compte GitHub `MightyCode`, accepté par Maven Central). Passer à `fr.<domaine>.mightylib` avec une entreprise et un produit fini, avant toute publication sur Maven Central.
- **Décodeurs de sons : JLayer (mp3) et jFLAC (flac), purs Java, licence LGPL** (dépendances Maven, jar non fusionné dans le nôtre). LWJGL n'a pas de décodeur mp3/flac (STB ne fait que Vorbis). Wav par `javax.sound.sampled` (JDK). Si la licence gêne pour un jeu commercial : les remplacer par un décodeur à licence permissive (dr_libs via JNI, ou autre) derrière la même interface `SoundDecoder`.
