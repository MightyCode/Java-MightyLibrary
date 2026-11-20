---
name: analyse-intellij
description: Analyse l'export des inspections IntelliJ (XML dans target/analyse), trie les warnings en "immédiat / demande réflexion / ne pas faire" et les corrige proprement. À utiliser quand l'utilisateur parle des warnings ou inspections d'IntelliJ, ou d'un export dans target/analyse.
---

# Analyse des inspections IntelliJ

Error Prone et SpotBugs, eux, se lancent sans l'utilisateur : `python tools/analyse.py` (voir `docs/debug-tools.md`) ; le tri et la correction ci-dessous valent aussi pour leurs constats.

## 1. Obtenir l'export
L'inspecteur en ligne de commande (`inspect.bat`) d'IntelliJ 2024.2 plante au démarrage de la JVM (accès à `sun.nio.fs`) : ne pas le relancer. Demander l'export à l'utilisateur : *Code → Inspect Code…*, portée « Whole project », bouton *Export* de la fenêtre de résultats, format XML, dossier `target/analyse` (un fichier par inspection).

## 2. Résumer sans lire les XML
- `python .claude/skills/analyse-intellij/summarize.py` : nombre de problèmes par inspection. **Attention** : un export ne réécrit pas les inspections tombées à zéro, leur XML reste d'un ancien export. Avant de trier, comparer les dates (`ls -l target/analyse`) et ignorer les fichiers plus anciens que les autres.
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
- **Déjà réglé par configuration** (ne doit plus apparaître ; s'il apparaît, le profil n'a pas été pris : le signaler) : membres publics et protégés « inutilisés » (API de la bibliothèque, `unused` limité au package-private), `SameParameterValue`, `BooleanMethodIsAlwaysInverted`, `UnusedReturnValue` (API fluide), `BusyWait` et `DataFlowIssue` dans les tests (un `null` y fait échouer le test), numérotation et tableaux Markdown (les numéros de `bonnes-pratiques.md` sont des identifiants cités ailleurs). Profil : `.idea/inspectionProfiles/Project_Default.xml`, au format exact d'IntelliJ (voir `docs/debug-tools.md`, section IntelliJ) : le changer de préférence dans l'IDE, sinon garder ce format.
- **Orthographe** : un mot du projet (identifiant, nom de bibliothèque, terme technique, mot français des docs) va dans `.idea/dictionaries/project.xml` (en minuscules, ordre alphabétique) ; une vraie faute se corrige. **Grammaire** (Grazie) : corriger le texte, sauf faux positif évident sur du code cité.
- **Cas isolé voulu** (sommeil d'une boucle de sondage, rappel GLFW libéré avec sa fenêtre, buffers Assimp nuls seulement avec un compteur à 0) : `//noinspection Nom` précédé d'un commentaire qui dit pourquoi. Certaines inspections ont un autre identifiant de suppression que leur nom d'export : `AutoCloseableResource` → `//noinspection resource`.
- **Ne pas faire** : 3D au-delà du strictement mécanique.

## 4. Corriger
- Branche dédiée depuis `dev` (jamais dev ni master), un commit par thème, `Co-Authored-By` demandé par le système.
- Ne jamais lancer `mvn clean` à la racine : il efface `target/analyse` (l'export). Nettoyer les modules seuls : `-pl mightylib,demo clean`.
- Les numéros de ligne de l'export sont ceux d'avant les modifications : éditer par contenu (Edit), et supprimer les lignes d'un même fichier du bas vers le haut.
- Après chaque thème : compiler, `mvn -o -q test`, et `tools/scenario.py` sur les scènes touchées (prévenir l'utilisateur qu'une fenêtre s'ouvre).
- Un bug existant découvert mais hors sujet : le noter dans `docs/later.md` ou `docs/futur-discussion.md`, pas le corriger en douce.
- Docs dans le même commit (`architecture.md`, conventions de `CLAUDE.md` si une règle naît). Une nouvelle sorte de constat corrigée devient une règle du point 14 de `docs/bonnes-pratiques.md` (pour ne pas la réécrire) ; une sorte acceptée devient une ligne de configuration avec sa raison.

## 5. Rendre compte
Par thème : ce qui est corrigé, les vrais bugs trouvés (avec leur test), ce qui est laissé volontairement et pourquoi, ce qui n'a pas été vérifié.
