# Décisions durables

- **Placeholders pour les seules données primaires** (texture, son, shader, police, animation, modèle) : une erreur de ressource ne fait pas tout planter, en debug comme chez le joueur. Données complexes (cartes, JSON, règles…) → `null`, un placeholder y cacherait l'erreur. Placeholder voyant/audible (damier magenta, bip), aussi pendant le chargement. Mode strict = exception, pour les tests et `scenario.py`.
- **Mode `DEBUG`/`RELEASE`** (`EEngineMode`) plutôt qu'un état « admin ».
- **Package `network` conservé** malgré `ServerThread.run()` vide.
- **Pas de Lombok** : peu d'accesseurs triviaux, méthodes générées introuvables dans le source, processeur d'annotations à configurer, API publique implicite.
- **Packages `io.github.mightycode.mightylib` / `…mightylibdemo`** (compte GitHub `MightyCode`, accepté par Maven Central). Passer à `fr.<domaine>.mightylib` avec une entreprise et un produit fini, avant toute publication sur Maven Central.
