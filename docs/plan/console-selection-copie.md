# Console : sélection, copie, défilement ancré, filtre des logs

**Statut : validé.**

Décisions prises (détail dans « Décisions ») :
- Raccourcis Ctrl+lettre reconnus par la lettre de la disposition du système (`glfwGetKeyName`), pas par la position physique.
- Défilement ancré sur une ligne du journal (position temporelle) : les nouvelles lignes ne déplacent plus une vue remontée ; cacher/réafficher les logs garde la position.
- Shift+↑/↓ : défiler d'une rangée ; Ctrl+Début/Fin : haut/bas du journal.
- Sélection dans la saisie (clavier et souris) et dans le journal (souris), une seule à la fois.
- Ctrl+A : 1er appui la saisie (ou le journal si la saisie est vide), 2e appui tout le journal visible.
- Ctrl+C copie la sélection (rien sans sélection) ; Ctrl+X coupe dans la saisie, copie dans le journal.
- Ctrl+V de plusieurs lignes : une commande par ligne complète, la dernière reste dans la saisie.
- Double clic : un mot ; triple clic : la ligne. Défilement automatique en glissant hors du panneau.
- Indicateur « ↓ N lignes » quand la vue n'est pas en bas.
- Nouvelle palette de couleurs par type.
- `console logs [on|off]` (DEV) cache stdout/stderr ; état initial par `EngineConfig` et `-Dmightylib.console.logs`. Modèle interne : ensemble de types cachés.
- `console copy [n] [--all]` (USER) : copie ce que montre la console.

## Pour démarrer
- Lire : `docs/architecture.md` section **Console** (touches, tick, journal, rendu) et **Entrées** (capture, entrées brutes) ; `docs/debug-tools.md` sections Commandes et `console` ; `docs/later.md` section « Console et GUI en jeu ».
- Branche de sujet depuis `dev` : `console-selection`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `src/io/github/mightycode/mightylib/console/` :
  - `Console` : modes `CLOSED`/`OVERLAY`/`DETACHED`, `update()` → `type(fenêtre, panneau)` lit `getRawTypedText`/`getRawKeyEvents`, la molette brute, et applique les `ConsoleInput.Request` (`SUBMIT`, `CLOSE`, `SCROLL`, `SCROLL_END`, `CLEAR`). Presse-papiers par `WindowContext.getSystemInfo().getClipboardContent()` / `setClipboardContent`.
  - `ConsoleInput` : touches → éditions de `CommandLineEditor` et requêtes ; compare `GLFW_KEY_V`/`GLFW_KEY_L` (positions QWERTY : Ctrl+A en AZERTY arrive comme `GLFW_KEY_Q`). ↑/↓ = historique, PageHaut/Bas = une page, Tab = complétion bash.
  - `CommandLineEditor` : texte + curseur, mots (`wordLeft`/`wordRight`, `isSeparator`), historique avec brouillon. Pas de sélection.
  - `ConsolePanel` (GL) : un `Text` par rangée visible ; `lastRows` lit `log.last(scroll + visibles, niveau)`, coupe à la largeur (`wrap`, `Text.charAdvance`) ; `scrollOffset` = **rangées depuis le bas** : une vue remontée dérive à chaque nouvelle ligne (défaut corrigé ici). Couleurs `colorOf(type)` : OUT gris 0.85, ERR et ERROR même rouge, COMMAND jaune, RESULT/MESSAGE blanc. Reconstruit si `log.version(niveau)`, défilement ou taille changent.
  - `ConsoleLog` : anneau thread-safe `{id, type, source, texte, niveau}`, `last(n[, niveau])`, `since(id)`, `version([niveau])`, `clear([niveau])` ; `Line.toString()` = texte affiché (préfixe `[source] > ` des commandes).
  - `ConsoleWindowView` : fenêtre détachée, même `ConsolePanel`.
- `src/io/github/mightycode/mightylib/inputs/` : `KeyEvent(key, mods, repeat)` (pas de scancode) créé dans le callback de `KeyboardManager` (ligne ~90) et par la simulation (`simulatedMods()`, ~226) ; `MouseManager` : seule la molette a un accès brut (`getRawMouseScroll`) ; position et boutons sont masqués par la capture ; simulation `simulateMove`/`simulateButton`/`simulateButtonRelease`.
- Commande : `utils/enginecommand/general/ConsoleGenCom.java` (`console open|close|toggle|detach|attach|clear|log|follow`). `EngineConfig` : `setConsoleFont`, `setConsoleLogCapacity`, options `-D` lues comme `COMMAND_ACCESS_PROPERTY`.
- Simulation pour `scenario.py` : `input mouse <x> <y>`, `input mouse hold|release`, `input click`, touches avec Ctrl/Shift tenus (`InputGenCom`).
- Tests : `test/.../console/CommandLineEditorTest`, `ConsoleInputTest`, `ConsoleLogTest`, `CompletionListingTest` ; `test/.../inputs/` (capture).

## Décisions
- **Lettre des raccourcis** : `KeyEvent` porte le scancode et `letter()` (minuscule, `glfwGetKeyName(key, scancode)` lu dans le callback, thread principal ; `0` si la touche n'a pas de nom). Touches simulées : `glfwGetKeyName(key, 0)`. `ConsoleInput` compare des lettres pour Ctrl+A/C/X/V/L. Les touches non imprimables (flèches, Début…) restent comparées par code.
- **Ancre de défilement** : `null` = suivre la fin ; sinon `(id de la ligne du bas, rangées de cette ligne cachées en dessous)`. Défiler déplace l'ancre de rangée en rangée. Ligne d'ancre cachée (filtre) : l'affichage se cale sur la ligne visible précédente la plus proche, l'ancre ne change pas (réafficher les logs revient au même endroit). Ligne d'ancre oubliée (anneau) : la plus ancienne gardée ; journal vidé : la fin. Entrée/validation ramène à la fin (comme aujourd'hui).
- **Mise en page sans GL** : nouvelle classe `ConsoleLayout` (coupe, ancre, rangées visibles, passage x/y → (id de ligne, caractère) et inverse), avec une fonction de largeur de caractère injectée (`charAdvance` en jeu, largeur fixe en test). `ConsolePanel` ne fait plus que dessiner.
- **Positions dans le journal** : `(id de ligne, indice de caractère dans le texte affiché)` (= `toString()`, tabulations remplacées par 4 espaces, comme à l'écran). Copie = ce qui est affiché, lignes d'origine jointes par `\n` (sans les coupures dues à la largeur) ; lignes cachées par le filtre ou le niveau dans l'intervalle non copiées.
- **Une seule sélection** : sélectionner dans le journal efface celle de la saisie et inversement ; taper, coller, effacer ou valider efface celle du journal. La sélection du journal tombe si ses lignes sont toutes oubliées ; si une seule borne l'est, elle est ramenée à la plus ancienne ligne gardée.
- **Saisie** : `CommandLineEditor` gagne une ancre de sélection (`selectionStart/End`, `hasSelection`, `selectedText`) ; Shift+←/→/Début/Fin et Ctrl+Shift+←/→ étendent ; taper/coller remplace la sélection ; Retour arrière/Suppr l'effacent ; ↑/↓ (historique), Tab et la validation l'effacent ; un déplacement sans Shift la retire.
- **Ctrl+A** : saisie non vide et pas déjà entièrement sélectionnée → toute la saisie ; sinon (2e appui, ou saisie vide) → tout le journal visible (filtre et niveau appliqués).
- **Ctrl+C** : copie la sélection (journal ou saisie), rien sans sélection, la sélection reste. **Ctrl+X** : saisie → coupe ; journal → copie.
- **Collage multiligne** : `\r\n`/`\r` normalisés ; texte = avant + collé + après ; chaque ligne complète (avant un `\n`) part en `SUBMIT` dans l'ordre (vides ignorées, chacune dans l'historique), la dernière reste dans la saisie, curseur après la partie collée. Exécuté au niveau de la session (aucune élévation).
- **Souris** : accès bruts dans `MouseManager` (`getRawPos`, `getRawButton`, `rawButtonPressed/Released`), qui suivent la simulation. `ConsoleInput` reçoit un instantané souris en coordonnées du panneau (position, bouton gauche appuyé/tenu/relâché, nombre de clics). Nombre de clics par une petite classe pure (`ClickCounter` : même endroit à 4 px près, moins de 500 ms, temps injecté). Clic dans la saisie : place le curseur ; glisser : sélectionne ; dans le journal : idem sur le journal. Double clic : mot (mêmes séparateurs que Ctrl+←) ; triple clic : ligne entière (ligne du journal d'origine, pas la rangée). Shift+clic étend la sélection.
- **Défilement auto** : glisser au-dessus/au-dessous de la zone du journal défile d'une rangée toutes les 50 ms et étend la sélection.
- **Défilement clavier** : Shift+↑/↓ = une rangée (↑/↓ seuls restent l'historique) ; Ctrl+Début/Fin = plus ancienne ligne / fin (Début/Fin seuls restent dans la saisie).
- **Indicateur** : vue pas en bas → « ↓ N lignes » à droite de la dernière rangée (N = lignes visibles, filtre et niveau appliqués, sous la vue ; affiché « 999+ » au-delà), recalculé seulement quand le journal ou l'ancre changent ; un clic dessus ramène à la fin.
- **Couleurs** (constantes regroupées dans une classe `ConsoleColors`, thème plus tard) : OUT gris terne (0.55) ; ERR orange terne (0.85, 0.5, 0.35) ; COMMAND jaune (1, 0.85, 0.35) ; RESULT blanc ; ERROR rouge vif (1, 0.35, 0.35) ; MESSAGE cyan clair (0.55, 0.85, 1). Sélection : fond bleu (0.25, 0.45, 0.85, 0.6).
- **Filtre** : la console (pas le journal) garde un ensemble de types cachés ; `console logs off` = cacher {OUT, ERR}. Le panneau ne se reconstruit pas pour une ligne cachée par le filtre (comme pour le niveau). État partagé entre overlay et fenêtre détachée.
- **`console logs [on|off]`** : niveau `DEV` (les logs ne sont visibles qu'en `DEV`), sans argument bascule ; message « Logs shown/hidden ». État initial : `EngineConfig.setConsoleLogsShown(boolean)` (défaut `true`), écrasé par `-Dmightylib.console.logs=shown|hidden` (valeur inconnue : une ligne au journal, défaut gardé).
- **`console copy [n] [--all]`** : niveau `USER` ; les `n` dernières lignes (toutes les lignes gardées sans `n`) visibles au niveau de la source, filtre de la console appliqué ; `--all` (`.level(DEV)`) ignore le filtre. Message « N lines copied » ; `data` `{lines, chars}`. Même fonction de copie que Ctrl+C.
- **Vérification sans capture** : le JSON de `console` (sans argument) ajoute `scroll` (`"end"` ou `{id, row}`), `logsShown`, `selection` (`null`, `{in: "input", start, end}` ou `{in: "log", from: {id, char}, to: {id, char}}`), `newLinesBelow`.

## Étapes (un commit chacune)
1. **Lettre des touches** — `inputs/KeyEvent` (scancode, `letter()`), `KeyboardManager` (callback, simulation), `console/ConsoleInput` (Ctrl+V/L par lettre). Tests : `ConsoleInputTest` avec des `KeyEvent` porteurs de lettre (Ctrl + lettre `v` sur un code `GLFW_KEY_Q` → collage). Vérif en jeu : `scenario.py` avec Ctrl+V/L simulés. Fusionnable seule.
2. **`ConsoleLayout` et ancre** — nouvelle `console/ConsoleLayout` (coupe, ancre, rangées, conversions), `ConsoleLog` (lecture des lignes jusqu'à un id et après un id, au niveau donné, prédicat de filtre), `ConsolePanel` l'utilise ; `ConsoleInput` : Shift+↑/↓ (`SCROLL` ±1), Ctrl+Début/Fin (`SCROLL_TOP`/`SCROLL_END`) ; JSON `scroll`. Tests : `ConsoleLayoutTest` (largeur fixe) : coupe, ancre stable quand des lignes arrivent, défilement d'une rangée dans une ligne coupée, ancre oubliée/effacée, conversions x/y ↔ (id, caractère) ; `ConsoleLogTest` pour les nouvelles lectures. Vérif en jeu : remonter (`input` Shift+↑), écrire des lignes (`console` / `echo`), `?console` → `scroll` inchangé.
3. **Filtre et `console logs`** — `Console` (types cachés), `ConsolePanel`/`ConsoleLayout` (prédicat), `ConsoleGenCom` (`logs [on|off]`), `EngineConfig` (`setConsoleLogsShown`, propriété). Tests : layout avec ancre sur une ligne cachée puis réaffichée ; analyse de la propriété ; syntaxe et niveau de `console logs` (invisible en USER). Vérif : `console logs off`, `?console`, `console logs on` → même `scroll`.
4. **Couleurs** — `console/ConsoleColors`, `ConsolePanel.colorOf`. Pas de test (GL) ; une capture `--rect` de l'overlay si l'utilisateur le demande. Fusionnable seule.
5. **Sélection dans la saisie (clavier)** — `CommandLineEditor` (sélection, remplacement, couper/copier), `ConsoleInput` (Shift+flèches/Début/Fin, Ctrl+Shift+←/→, Ctrl+A 1er appui, Ctrl+C/X/V, collage multiligne → plusieurs `SUBMIT`), `Console` (presse-papiers en écriture, `setClipboardContent`), `ConsolePanel` (fond de sélection derrière le texte de la saisie, qui défile avec elle). Tests : `CommandLineEditorTest` (extension, remplacement, effacement, historique efface la sélection), `ConsoleInputTest` (Ctrl+A/C/X/V, collage de 3 lignes → 2 `SUBMIT` + reste, `\r\n`). Vérif : `?console` → `selection`.
6. **Souris : accès bruts et saisie** — `MouseManager` (accès bruts suivant la simulation), `console/ClickCounter`, `ConsoleInput` (instantané souris), `ConsoleLayout` (x → caractère de la saisie), `Console` (coordonnées du panneau dans l'overlay et la fenêtre détachée). Tests : `ClickCounterTest`, `ConsoleInputTest` (clic place le curseur, glisser, double/triple clic, Shift+clic), test des accès bruts sous capture dans les tests d'entrées. Vérif : `input mouse x y`, `input click` deux fois, `?console`.
7. **Sélection du journal** — `console/LogSelection` (bornes `(id, caractère)`, texte copié via `ConsoleLayout`), `ConsoleInput`/`Console` (glisser, double/triple clic, Shift+clic, défilement auto, Ctrl+A 2e appui, Ctrl+C/X, règle d'une seule sélection), `ConsolePanel` (rectangles de surlignage par rangée, réserve réutilisée comme les `Text`, libérée dans `unload`). Tests : `LogSelectionTest` (sens inverse, lignes coupées rejointes, lignes cachées non copiées, bornes oubliées), règle d'une seule sélection dans `ConsoleInputTest`. Vérif : glisser simulé sur deux lignes, Ctrl+C, presse-papiers lu après coup (Windows : `Get-Clipboard`) ; `leakcheck` sur une scène avec la console ouverte puis fermée.
8. **Indicateur « ↓ N lignes »** — `ConsoleLayout` (compte), `ConsolePanel` (texte à droite de la dernière rangée), clic → fin ; JSON `newLinesBelow`. Tests : compte avec filtre, niveau et plafond.
9. **`console copy`** — `ConsoleGenCom`, fonction de copie partagée. Tests : texte copié (n, filtre, `--all`, niveau USER ne voit pas les logs), syntaxe et niveau de `--all`. Vérif : `console copy 5` puis `Get-Clipboard`.
10. **Démo et docs** — scène de démo (ci-dessous), documents (ci-dessous).

## Démo
Nouvelle scène `ConsoleFeedScene` (démo, inscrite dans `MenuScene`) : écrit périodiquement (toutes les 0,5 s, réglable par une touche) des lignes de chaque type : `System.out`, `System.err`, `console.print` (MESSAGE), une longue ligne qui se coupe, une ligne avec tabulation. Sert à vérifier l'ancre (la vue remontée ne bouge pas), le filtre, la sélection pendant que le journal avance et l'indicateur.

## Documents à mettre à jour à la fin
- `architecture.md`, section **Console** : touches (nouveaux raccourcis, souris), ancre de défilement, filtre, sélection, couleurs, `ConsoleLayout`/`LogSelection`/`ClickCounter` ; section **Entrées** : `KeyEvent.letter()`, accès bruts de la souris.
- `debug-tools.md` : `console logs`, `console copy`, champs du JSON de `console`, vérifier une sélection par `scenario.py`.
- `later.md`, « Console et GUI en jeu » : retirer « sélection et copie du journal » ; garder police monospace, thème (y mettre la palette de `ConsoleColors`), suggestions ; ajouter ce qui reste (voir Hors périmètre). Section Entrées : liaisons du jeu par lettre de disposition.
- `CLAUDE.md` : tests couverts (`console/ConsoleLayout`, `LogSelection`, `ClickCounter`, `inputs/KeyEvent`, accès bruts de `MouseManager`).
- `decisions.md` : raccourcis par lettre de la disposition du système, pas par position.
- `current.md` : rien (sujet hors du gros point en cours).
- Supprimer ce plan.

## Hors périmètre
- Annuler/refaire (Ctrl+Z/Y) dans la saisie, recherche dans le journal (Ctrl+F), menu clic droit, sélection rectangulaire : à noter dans `later.md` (Console).
- Filtres par source et autres types exposés par commande : modèle prêt, cf. « Journal à grande échelle » de `later.md`.
- Mémoriser l'état des logs entre deux lancements : avec les réglages du joueur (`later.md`, Fenêtres / réglages).
- Liaisons d'actions du jeu par lettre de disposition (`ActionInput`) : `later.md`, Entrées.
- Un `RectangleRenderer` par rangée surlignée : à reprendre avec le batching 2D (`later.md`, Texte de la console).

## Questions restantes
Aucune.

## Modèles recommandés
- Discussion A :
  - Étapes 1 à 3 (lettres, `ConsoleLayout`, ancre, filtre) : **Opus 5.5, effort high** — modèle durable de mise en page et d'ancre, logique délicate.
  - Étapes 5 à 7 (sélections, souris) : **Opus 5.5, effort high** — beaucoup de cas croisés (une seule sélection, lignes coupées, bornes oubliées).
  - Étapes 4, 8, 9, 10 (couleurs, indicateur, `console copy`, démo, docs) : **Sonnet 5.5, effort medium** — mécanique, sur un modèle déjà posé.
- Discussion B : **Opus 5.5, effort high** — gros diff à relire au regard du plan.

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/console-selection-copie.md »)
- Commencer par donner à l'utilisateur, en une ou deux lignes, le modèle et l'effort recommandés (« Modèles recommandés ») pour les premières étapes ; si la discussion tourne sur un autre modèle ou un autre effort, le dire et attendre qu'il change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `console-selection`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (dans `ConsoleFeedScene`, overlay puis fenêtre détachée F2 : Ctrl+A deux fois, Shift+flèches, sélection et double/triple clic à la souris dans la saisie et le journal, glisser hors du panneau, Ctrl+C/X/V dans un autre logiciel, collage de plusieurs lignes, Shift+↑/↓ et Ctrl+Début/Fin, vue remontée qui ne bouge pas, `console logs off/on` qui garde la position, indicateur et clic dessus, couleurs, raccourcis en AZERTY). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — PR, vérification, fusion (« Review la PR console-selection »)
- Commencer par donner à l'utilisateur le modèle et l'effort recommandés pour cette discussion (« Modèles recommandés ») ; si la discussion tourne sur un autre, le dire et attendre qu'il change ou confirme.
- Lire ce plan et son « Bilan ».
- Relire le diff de la branche par rapport à `dev` au regard du plan et de `CLAUDE.md` ; signaler ce qui manque ou dévie avant d'aller plus loin.
- Dernier commit : supprimer ce plan (et `docs/plan/` s'il est vide) ; vérifier que les docs ne gardent aucune trace de ce qui est fini.
- Pousser, ouvrir une PR vers `dev` (pas `master`) dont la description reprend le Bilan et la liste des vérifications manuelles.
- Attendre le feu vert de l'utilisateur, puis fusionner par un commit de fusion (pas de squash), mettre `dev` à jour en local, supprimer la branche en local et sur GitHub.
