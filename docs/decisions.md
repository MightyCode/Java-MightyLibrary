# Décisions prises — Java-MightyLibrary

Décisions de conception durables, valables pour toute la feuille de route.

- **Package `network` conservé** bien qu'inutilisé (`ServerThread.run()` vide) : utilisable un jour, ne pas le supprimer.
- **Pas de Lombok** : gain de tokens faible (peu d'accesseurs triviaux), méthodes générées introuvables par recherche dans le source, processeur d'annotations à configurer (Maven + IntelliJ) et API publique implicite, contraire au choix d'une API publique explicite.
