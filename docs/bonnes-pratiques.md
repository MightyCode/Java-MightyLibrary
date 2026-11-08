# Bonnes pratiques

Points que le projet doit continuer à respecter. **Revue hebdomadaire** : quand l'utilisateur demande si le projet les respecte toujours, Claude vérifie chaque point avec sa vérification, puis répond point par point : **respecté**, ou **dérive** (avec la preuve : commande, fichier, chiffre) et l'action proposée. Les corrections ne se font qu'après accord. Ce fichier contient les règles, jamais l'état ni l'historique : l'état est dans la réponse de la revue.

## Méthode
1. **Une branche par sujet, un commit par changement logique, fusion `--no-ff` dans master** (jamais de commit direct sur master). Branche fusionnée = branche supprimée ; une branche non fusionnée a une raison connue de l'utilisateur.
   *Vérification* : `git log --first-parent --oneline master` depuis la dernière revue (seulement des fusions) ; `git branch --merged master` (vide hors master) ; `git branch --no-merged master` (chaque branche expliquée).
2. **Docs à jour dans le même commit que le code.** `current.md` / `later.md` = ce qui reste, sans historique ; `architecture.md` = l'état présent.
   *Vérification* : les commits de la semaine qui changent un comportement touchent aussi la doc concernée (`git log --since="1 week ago" --stat master`) ; `current.md` ne contient aucune trace d'objectif terminé.
3. **Tests verts, et un test pour chaque bug corrigé** quand il peut s'écrire sans GL. La liste du code couvert dans `CLAUDE.md` est à jour.
   *Vérification* : `mvn -o -q test` ; pour chaque correctif de la semaine, le test qui le reproduit (ou la raison de son absence).
4. **Vérification en jeu** de ce que les tests ne couvrent pas (`tools/scenario.py`, journal sans problème), et description de la vérification manuelle restante.
   *Vérification* : les changements de rendu, d'entrées ou de fenêtre de la semaine ont été lancés en jeu.

## Cap
5. **Un vrai jeu guide les priorités** : un petit jeu, dans un projet séparé qui utilise le jar, révèle ce qui manque et teste la bibliothèque comme un tiers l'utiliserait.
   *Vérification* : ce jeu existe-t-il ? Les points de `current.md` viennent-ils d'un besoin réel (jeu, mesure) ou d'une envie de perfection ? Tant qu'il n'existe pas : le signaler.
6. **`current.md` = un seul gros point, qu'on peut terminer.** Rien de conditionnel (« si les mesures l'exigent », « si un vrai jeu le demande ») : ces lignes vont dans `later.md`.
   *Vérification* : `grep -n -i -E "si les mesures|si un vrai jeu|seulement si" docs/current.md` est vide ; le point en cours avance d'une revue à l'autre.
7. **Surface de l'API décidée avant le premier jar** : chaque package de `mightylib` est classé (API promise, expérimental, à retirer), après discussion (cf. `later.md`, « API publique »). Tout ce qui est public est un engagement.
   *Vérification* : la classification existe et couvre les packages ajoutés depuis ; tant qu'elle n'existe pas, le rappeler si un jar approche.
8. **Le code ancien gagne des tests quand on le touche** (`network/`, GUI, `DataFolder`, loaders maison, algorithmes) : c'est là que se cachent les bugs.
   *Vérification* : les fichiers anciens modifiés dans la semaine ont gagné un test ou une vérification en jeu.

## Qualité du code
9. **Exceptions** : jamais `printStackTrace()` ; exception inattendue gérée → `Logger.LogException` ; échec attendu d'une ressource → `ResourceException` ; `InterruptedException` → `Thread.currentThread().interrupt()` puis arrêt.
   *Vérification* : `grep -rn "printStackTrace()" src` est vide ; pas de `catch` vide ou qui avale l'erreur sans la signaler.
10. **Fichiers et flux fermés** (try-with-resources), y compris en cas d'erreur.
    *Vérification* : `grep -rn -E "new (FileInputStream|FileReader|FileWriter|Scanner|BufferedReader)" src`, chaque résultat dans un try-with-resources ou fermé dans un `finally`.
11. **Analyse statique régulière** : export de l'inspection IntelliJ (*Code → Inspect Code*, export XML dans `target/analyse`) au moins une fois par mois, ou un outil branché sur Maven (SpotBugs, Error Prone) s'il est adopté. Les catégories à risque sont traitées : `DataFlowIssue`, `AutoCloseableResource`, `NonAtomicOperationOnVolatileField`, `EmptyStatementBody`, `ThrowablePrintStackTrace`, `UNCHECKED_WARNING`.
    *Vérification* : date de l'export de `target/analyse` (demander un nouvel export s'il a plus d'un mois) ; nouvelles alertes de ces catégories.
12. **Régressions de rendu** : tant que la comparaison d'images (`later.md`, « Tests de non-régression ») n'existe pas, toute modification de rendu 2D est vérifiée par une capture ciblée.
    *Vérification* : la comparaison d'images existe-t-elle ? Sinon, les changements de rendu de la semaine ont-ils eu leur capture ?

## Rythme
13. **Prendre du recul régulièrement et limiter les chantiers ouverts** : finir avant d'ouvrir. La cadence est élevée ; la revue sert aussi à vérifier que l'infrastructure ne passe pas avant le jeu.
    *Vérification* : nombre de branches non fusionnées et de lignes de `current.md` (pas en hausse sans raison) ; part des commits de la semaine qui ferment un point plutôt qu'en ouvrir un.
