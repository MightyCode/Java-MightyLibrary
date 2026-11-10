---
name: planification-ml
description: Prépare un gros point (passage de later.md à current.md, changement d'API, nouveau système) par une discussion en questions-réponses, écrit un plan autonome dans docs/plan/<sujet>.md, puis donne les deux commandes à copier pour les discussions suivantes (implémentation, puis `/pr-ml`), chacune avec le modèle et l'effort recommandés. Ne code rien. À utiliser quand l'utilisateur dit /planification-ml <sujet>, ou demande un plan avant de commencer.
disable-model-invocation: true
---

# Planification

Un plan n'est pas un cahier des charges rédigé seul : il naît d'une discussion. Le travail se fait ici en trois temps, et le code est écrit dans **une autre discussion** à partir du fichier produit. Rien n'est commité par ce skill : le plan reste un fichier non suivi dans le dossier du dépôt principal jusqu'à la discussion d'implémentation, qui le commite en premier sur sa branche.

## 1. Comprendre l'existant (sans rien modifier)
- Lire les extraits utiles : `docs/current.md`, la section de `docs/later.md` qui concerne le sujet, les sections de `docs/architecture.md` et `docs/debug-tools.md` touchées, `docs/decisions.md` s'il y a une décision proche. Puis le code concerné, par extraits (Grep, Read avec offset/limit).
- Relever : ce qui existe déjà et peut être réutilisé (modèles du moteur : `Identifier`, propriétaire des fenêtres, vue courante, `Workers`…), le défaut ou le manque précis, les tests qui couvrent la zone.
- Vérifier si `later.md` ou `futur-discussion.md` prévoit déjà le sujet, et à quelle étape : le dire à l'utilisateur, avec ce qui est prévu et ce qui ne l'est pas.

## 2. Discuter (questions-réponses, une passe à la fois)
- Donner d'abord un avis court : le modèle proposé, ce qui serait durable à l'échelle d'un vrai jeu (milliers de ressources, mods, projets tiers), ce qui changerait plus tard et à quel coût. Préférer corriger le modèle plutôt qu'un pansement.
- Poser les questions **par petits lots** (3 à 5), avec pour chacune une recommandation et son pourquoi. Attendre les réponses avant le lot suivant. `AskUserQuestion` convient pour des choix fermés ; une question ouverte se pose en texte.
- Après chaque lot, reformuler les décisions prises en une ligne chacune, et signaler les conséquences sur le reste (une décision en entraîne souvent une autre : touches, docs, démo, tests).
- Quand une décision de l'utilisateur ouvre un cas nouveau, le dire tout de suite plutôt que de le découvrir pendant l'implémentation.
- Terminer par « Des dernières remarques ? » : lister ce que l'utilisateur n'a pas mentionné (sécurité, mode release, autre contexte/fenêtre, scripts, docs, place dans `current.md`).

## 3. Écrire le plan : `docs/plan/<sujet-en-kebab>.md`
Le plan **se lit seul** : une discussion neuve, sans cet historique, doit pouvoir l'exécuter. Il est mis à jour à chaque lot de réponses, pas seulement à la fin. Structure :

1. **Titre, statut** (à valider / validé), et la liste des décisions prises.
2. **Pour démarrer** : sections de doc à lire, nom suggéré de la branche de sujet, rappel « compiler et tester avec les commandes de `CLAUDE.md`, vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur ».
3. **Contexte : ce qui existe**, avec les classes et fichiers concernés (chemins), le défaut précis, les sources et modèles à réutiliser, les tests existants.
4. **Décisions** : une ligne par décision, avec son pourquoi quand il n'est pas évident.
5. **Étapes** numérotées, une par commit : fichiers touchés, comportement attendu, tests JUnit à écrire (logique sans GL), vérification en jeu. Dire si une étape peut être fusionnée seule.
6. **Démo** : scène de démo ou exemple à ajouter (nouvelle scène inscrite dans `MenuScene` plutôt qu'une existante modifiée).
7. **Documents à mettre à jour à la fin** : `architecture.md` (nouvel état, au présent), `debug-tools.md`, `decisions.md`, `later.md` (ce qui reste et ce qui est préparé pour la suite), `futur-discussion.md`, `CLAUDE.md` (tests couverts, nouvelle règle), `current.md`. Et : « supprimer ce plan ».
8. **Hors périmètre** : ce qui est volontairement laissé à plus tard, avec l'endroit où c'est noté.
9. **Questions restantes** à trancher au début de l'implémentation (idéalement une ou zéro).
10. **Modèles recommandés** : pour la discussion A, le modèle et l'effort (`low`/`medium`/`high`/`xhigh`/`max`) par groupe d'étapes, avec la raison en quelques mots (conception d'une API ou d'un modèle durable, logique délicate, gros fichier → modèle le plus capable et effort élevé ; migration mécanique, docs → modèle plus économique, effort moyen) ; pour la discussion B (`/pr-ml`), un modèle et un effort (relire un gros diff au regard du plan demande un modèle capable). Ils sont recopiés dans les deux commandes de l'étape 4. Utiliser les modèles les plus récents disponibles au moment du plan.
11. **Déroulé** : recopier le bloc ci-dessous en adaptant `<branche>` et la liste des vérifications manuelles. C'est lui qui pilote les deux discussions suivantes, dont les commandes tiennent en une ligne.

```markdown
## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/<sujet>.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `<branche>`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (<scènes, touches, configs>). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml <branche>`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».
```

Pas d'historique de la discussion dans le plan, seulement l'état des décisions. Réponses courtes dans la discussion : le détail va dans le fichier.

## 4. Donner les commandes des discussions suivantes
Quand l'utilisateur est satisfait du plan, lui donner les deux commandes, chacune dans son bloc de code, **à copier telles quelles en une seule fois** : le modèle et l'effort recommandés (« Modèles recommandés ») sont dans la commande, pas dans un message à part. Le déroulé de A est dans la section « Déroulé » du plan, celui de B dans le skill `/pr-ml`.

Discussion A (implémentation) :
```text
Implémente le fichier docs/plan/<sujet>.md — modèle recommandé : <modèle>, effort <effort> pour les premières étapes (<raison courte>).
```

Discussion B (relecture, PR, fusion) :
```text
/pr-ml <branche> — modèle recommandé : <modèle>, effort <effort> (<raison courte>).
```

Rappeler en une ligne que la discussion B se lance une fois la branche poussée par la discussion A.

## Règles
- Ce skill ne modifie que `docs/plan/<sujet>.md`. Pas de code, pas de commit, pas de changement des autres docs : ils sont listés dans le plan pour la fin de l'implémentation.
- Un plan existant dans `docs/plan/` sur le même sujet : le reprendre et le mettre à jour, pas en créer un second.
- Le plan ne doit pas décrire ce qui est déjà vrai comme à faire : relire l'existant avant chaque étape écrite.
