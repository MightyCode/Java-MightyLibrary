# Décisions prises — Java-MightyLibrary

Décisions de conception durables, valables pour toute la feuille de route.

- **Placeholders pour les seules données primaires** (texture, son, shader, police, animation, modèle) : une erreur de ressource ne doit pas tout faire planter, ni en debug ni chez le joueur. Les données complexes (cartes, JSON, règles…) renvoient `null` : un placeholder y cacherait l'erreur sous un état faux. Placeholder visible et audible (damier magenta, bip strident) plutôt que discret, aussi pendant le chargement. Le mode strict (debug) transforme tout échec en exception pour les tests et `scenario.py`.
- **Mode `DEBUG` / `RELEASE`** (`EEngineMode`) plutôt qu'un état « admin ».
- **Package `network` conservé** bien qu'inutilisé (`ServerThread.run()` vide) : utilisable un jour, ne pas le supprimer.
- **Pas de Lombok** : gain de tokens faible (peu d'accesseurs triviaux), méthodes générées introuvables par recherche dans le source, processeur d'annotations à configurer (Maven + IntelliJ) et API publique implicite, contraire au choix d'une API publique explicite.
- **Packages `io.github.mightycode.mightylib` (bibliothèque) et `io.github.mightycode.mightylibdemo` (démo)**, en minuscules : préfixe gratuit et vérifiable (compte GitHub `MightyCode`, accepté par Maven Central). Passer à `fr.<domaine>.mightylib` quand l'utilisateur aura une entreprise et un produit fini, avant toute publication sur Maven Central.
