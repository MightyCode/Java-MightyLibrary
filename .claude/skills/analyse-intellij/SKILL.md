---
name: analyse-intellij
description: Analyse l'export des inspections IntelliJ (XML dans target/analyse), trie les warnings en "immédiat / demande réflexion / ne pas faire" et les corrige proprement. À utiliser quand l'utilisateur parle des warnings ou inspections d'IntelliJ, ou d'un export dans target/analyse.
---

# Analyse des inspections IntelliJ

## 1. Obtenir l'export
L'inspecteur en ligne de commande (`inspect.bat`) d'IntelliJ 2024.2 plante au démarrage de la JVM (accès à `sun.nio.fs`) : ne pas le relancer. Demander l'export à l'utilisateur : *Code → Inspect Code…*, portée « Whole project », bouton *Export* de la fenêtre de résultats, format XML, dossier `target/analyse` (un fichier par inspection).

## 2. Résumer sans lire les XML
- `python .claude/skills/analyse-intellij/summarize.py` : nombre de problèmes par inspection.
- `… summarize.py --details Inspection1 Inspection2` : `fichier:ligne | élément | description`.
- `… summarize.py --unused` : `unused.xml` groupé par nature et par zone (lib / demo / test), puis le détail de tout sauf les méthodes publiques inutilisées de la bibliothèque.

Lire ensuite seulement les extraits de code concernés (Read avec offset/limit, Grep).

## 3. Trier avant de toucher quoi que ce soit
Présenter trois listes à l'utilisateur et attendre son accord :
- **Immédiat** (mécanique, sans changement de comportement) : imports inutilisés, `isEmpty()`, initialiseurs/redéfinitions/suppressions redondants, diamant, `final` sur un champ privé jamais réaffecté, `StandardCharsets`, assertions simplifiables.
- **Demande réflexion** (lire chaque cas) :
  - `DataFlowIssue` : vrai risque ou faux positif ? Un vrai bug reçoit un test qui le reproduit (vérifier qu'il échoue sans le correctif).
  - `NonAtomicOperationOnVolatileField` : trouver tous les écrivains ; un seul thread → documenter l'invariant (méthode annotée ou `//noinspection`), sinon atomique.
  - Champs écrits jamais lus, code mort, code commenté : supprimer ce qui est mort ; garder les interrupteurs de debug.
  - `ThrowablePrintStackTrace` → `Logger.LogException` ; échec attendu de ressource → `ResourceException` ; `InterruptedException` → remettre le drapeau et s'arrêter.
  - `EmptyStatementBody`, `LossyConversionCompoundAssignment`, `DefaultNotLastCaseInSwitch`, `UNCHECKED_WARNING` (un helper annoté par tableau générique), `ClassEscapesItsScope` (visibilité).
- **Ne pas faire** : orthographe (`SpellCheckingInspection`, Grazie), `PackageName` (renommer des packages), méthodes/classes publiques « inutilisées » de `mightylib` (API de la bibliothèque), `BusyWait` volontaires, `CanBeFinal` sur des champs publics, cosmétique (`IfCanBeSwitch`, Javadoc, tableaux Markdown), 3D au-delà du strictement mécanique, faux positifs connus (format construit dynamiquement, buffers Assimp bornés par leurs compteurs).

## 4. Corriger
- Branche dédiée depuis `dev` (jamais dev ni master), un commit par thème, `Co-Authored-By` demandé par le système.
- Les numéros de ligne de l'export sont ceux d'avant les modifications : éditer par contenu (Edit), et supprimer les lignes d'un même fichier du bas vers le haut.
- Après chaque thème : compiler, `mvn -o -q test`, et `tools/scenario.py` sur les scènes touchées (prévenir l'utilisateur qu'une fenêtre s'ouvre).
- Un bug existant découvert mais hors sujet : le noter dans `docs/later.md` ou `docs/futur-discussion.md`, pas le corriger en douce.
- Docs dans le même commit (`architecture.md`, conventions de `CLAUDE.md` si une règle naît).

## 5. Rendre compte
Par thème : ce qui est corrigé, les vrais bugs trouvés (avec leur test), ce qui est laissé volontairement et pourquoi, ce qui n'a pas été vérifié.
