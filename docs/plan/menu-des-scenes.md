# Menu des scènes : liste filtrée, description, commandes

**Statut : validé.**

Décisions prises :
- La liste du menu vient de la complétion de la commande `scene` (`commands().complete("scene ")`), triée par ordre alphabétique ; un JSON de la démo cache certaines scènes.
- Les descriptions (carré bleu) sont dans ce même JSON de ressources ; une scène sans description affiche un texte par défaut.
- Les boutons du menu exécutent des commandes (`scene <nom>`, `exit`) au lieu d'appeler `setNewScene`/`exit` : démonstration de `Commands.execute` depuis le code.
- Une commande exécutée depuis le code vise la fenêtre de la vue courante ; la console garde la fenêtre choisie par `window`.
- Un système de **focus** simple par fenêtre remplace la capture booléenne : la scène est le détenteur par défaut, un `TextField` ou la console le prennent par-dessus elle.
- Le champ texte prend le focus au clic. Entrée y lance la première scène filtrée, Échap lui retire le focus ; Échap quand la scène a le focus quitte (`exit`).
- Filtre : « contient », sans tenir compte des majuscules, sur le nom court.
- Nom affiché : le nom court (`PlatformerBox2D`) ; le nom relatif (`Platformer2D.PlatformerBox2D`) seulement si deux scènes ont le même nom court. Le nom relatif reste celui de la commande et la clé du JSON.
- Une commande en échec affiche son message en rouge dans le carré bleu (et part dans le journal).
- Retour à la ligne par mots dans `Text` (`setMaxWidth`), dans la bibliothèque ; `TextField` aussi dans la bibliothèque.
- Les raccourcis Shift+F2/F3/F4 du menu sont supprimés.

## Pour démarrer
- Lire : `docs/architecture.md`, sections Entrées (**Texte et touches tapées**, **Capture**), Console (**Tick**, **Mise en page et défilement**, **Sélection**), Commandes (source, niveau, fenêtre cible) ; `docs/debug-tools.md`, sections Commandes, Plusieurs fenêtres, Simuler des entrées ; `docs/later.md`, sections Modding (registre de scènes) et Console et GUI en jeu.
- Branche de sujet depuis `dev` : `scene-menu`.
- Compiler et tester avec les commandes de `CLAUDE.md` ; vérifier en jeu avec `tools/scenario.py` en prévenant l'utilisateur (une fenêtre s'ouvre).

## Contexte : ce qui existe
- `src/io/github/mightycode/mightylibdemo/scenes/MenuScene.java` : titre `Text`, trois boutons en dur (Scene2D, Scene3D, Scene3DBis) et Quit dans un `GUIList`, une `ScrollList` à droite remplie par `createSceneEntries()` (liste en dur, ordre quelconque, fabriques `Supplier<Scene>`), `setNewScene`/`exit` appelés à la main, raccourcis Shift+F2/F3/F4, son `selectogg` au changement de sélection, rotation du texte de Quit.
- `mightylib/graphics/GUI/ScrollList.java` : liste à molette, réserve fixe de `BackgroundlessButton` re-remplie au défilement, `setItems`, `getHovered`, `getActivated` (clic), `scrollTo`. Pas de sélection au clavier ni de défilement automatique vers une ligne.
- `mightylib/graphics/GUI/GUIList.java` (test `GUIListTest`) : navigation haut/bas (`setupActionInputValues`), activation, souris.
- Pas de champ texte dans le GUI. La logique d'édition existe : `mightylib/console/CommandLineEditor.java` (curseur, sélection, insertion, collage, mots) et sa lecture des touches `ConsoleInput` (testés).
- `Text` (`graphics/text/Text.java`) : pas de retour à la ligne automatique. `console/ConsoleLayout` coupe par caractère avec une mesure injectée (modèle à suivre pour une logique sans GL).
- Capture : `InputManager.setCaptured(boolean)` (posée par `Console` lignes ~288/299, propagée à `KeyboardManager`/`MouseManager`) cache touches, texte, souris, actions et axes à la scène, sauf les actions de l'espace `mightylib` ; le détenteur lit les valeurs brutes (`getRawKeyEvents`, `getRawTypedText`, `getRawKeyState`, `MouseManager.getRawPos`, `rawButtonPressed`…). Un bouton de souris tenu à la fin de la capture ne compte pas comme un nouvel appui. Tests : `InputManager` (capture comprise), `MouseRawTest`.
- Commandes : `Commands.execute(line)` depuis le code s'exécute comme `ICommandSource.API` (niveau `DEV`). `SceneGenCom` (`scene`) trouve les scènes par `SceneClasses.Find` (classes non abstraites dérivées de `Scene` sous le paquet de la première scène, noms relatifs) et les crée par réflexion ; `ListGeneralCommand.targetScenes()` = `Views.engine().commandContext()`, c'est-à-dire la fenêtre choisie par `window`, pas celle de l'appelant. Les commandes qui visent la cible : `input`, `screenshot`, `monitors`, `scene`, `camera`, `wait-loaded`, `exit`. `Commands.complete(début)` rend un `CommandCompletion` (`getCandidates()`).
- JSON de la démo : `resources/data/*.json` lus par `JSONLoader` (exemple : `documentationScene.json`).

## Décisions (détail)
- **Focus par fenêtre** (un `InputFocus` porté par l'`InputManager`) : pile de détenteurs (objets quelconques) ; pile vide = la scène. Le sommet reçoit les entrées (lectures brutes), la scène ne voit plus rien tant que la pile n'est pas vide, sauf les actions `mightylib` (comportement actuel de la capture). `request(owner)` empile (ou remonte) le détenteur, `release(owner)` le retire où qu'il soit : la console ouverte par-dessus un `TextField` rend le focus au champ en se fermant. `setCaptured` disparaît (la console passe par le focus).
- **Pas de fuite d'appui au changement de focus** : une touche ou un bouton appuyé pendant que le focus appartenait à un autre détenteur ne compte pas comme un appui pour le nouveau (généralise la règle actuelle des boutons de souris). Exemple : Échap qui retire le focus du champ ne quitte pas le menu dans le même tick.
- **Fenêtre de l'appelant** : `Commands.execute` appelé depuis le code (source `API`, hors d'une commande) vise le contexte de la vue courante (`Views.current()`) ; la console, l'entrée standard et le serveur de debug gardent `Engine.commandContext()`. Les commandes lisent leur cible par un seul accès (aujourd'hui `ListGeneralCommand.targetScenes()` et équivalents) qui tient compte de cette cible d'exécution.
- **Retour à la ligne** : logique sans GL (`TextWrap`, mesure de largeur injectée) : coupe aux espaces, coupe un mot plus long que la ligne, garde les sauts de ligne explicites. `Text.setMaxWidth(largeur)` (0 = pas de coupe) l'applique. La console garde sa coupe par caractère.
- **`TextField`** (GUI de la bibliothèque) : `CommandLineEditor` + lecture des touches tirée de `ConsoleInput` (partager la logique plutôt que la copier), prend le focus au clic dedans, le rend au clic dehors, à Échap ou à Entrée ; texte d'invite gris quand il est vide (« Filtrer… ») ; curseur clignotant ; `isSubmitted()` (Entrée) et `isChanged()` lus par la scène. Logique de touches testée sans GL.
- **`ScrollList`** : sélection au clavier (actions haut/bas comme `GUIList`), défilement automatique pour garder la ligne sélectionnée visible, `getSelected()`, `setSelected`, activation par Entrée sur la ligne sélectionnée ; `setItems` garde la sélection si l'élément existe encore, sinon la première ligne. Logique de sélection/défilement sans GL et testée.
- **Description affichée** : celle de la ligne survolée par la souris, sinon celle de la ligne sélectionnée.
- **JSON du menu** : `resources/data/menuScenes.json`, `{ "hidden": ["MenuScene", …], "descriptions": { "Platformer2D.PlatformerBox2D": "…" } }`, clés = noms relatifs de la commande ; à terme clés de langue.

## Étapes (un commit chacune)
1. **Focus par fenêtre** — `inputs/InputFocus` (nouveau), `InputManager`, `KeyboardManager`, `MouseManager`, `console/Console`. Remplace `setCaptured` par la pile de focus ; règle de non-fuite des appuis étendue aux touches et actions. Tests : mettre à jour ceux de la capture (`InputManager`, `MouseRawTest`) ; nouveaux : pile (empiler, remonter, retirer au milieu), scène aveugle sauf actions `mightylib`, appui non transmis au détenteur suivant. En jeu : console ouverte/fermée dans `Scene2D` (`scenario.py "scene Scene2D" wait-loaded "input key GRAVE_ACCENT" …`), le jeu ne reçoit rien pendant la saisie. Fusionnable seule.
2. **Commandes depuis le code : fenêtre de l'appelant** — `utils/enginecommand/Commands`, `ListGeneralCommand` et les commandes qui lisent la cible. Tests : `Commands` (cible d'exécution selon la source, sans GL si possible via un contexte factice). En jeu : `MultiWindowScene`, une commande lancée par le code de la seconde fenêtre agit sur elle, la console continue de viser la fenêtre choisie par `window`. Fusionnable seule.
3. **Retour à la ligne dans `Text`** — `graphics/text/TextWrap` (nouveau), `Text.setMaxWidth`. Tests `TextWrapTest` (espaces, mot trop long, sauts explicites, largeur 0, texte vide). En jeu : `TextPaddingScene` ou le menu, `gl text`. Fusionnable seule.
4. **`TextField`** — `graphics/GUI/TextField` (nouveau), logique de touches partagée avec `ConsoleInput`. Tests : édition, Entrée/Échap, focus pris/rendu. Ajouter `TextField` à `PublicFieldsTest` s'il a des champs publics (éviter). Démo : un champ dans `GUIWidgetsScene` (ou nouvelle scène `TextFieldScene2D` inscrite au menu si `GUIWidgetsScene` est trop chargée).
5. **`ScrollList` : sélection au clavier** — `graphics/GUI/ScrollList` (+ logique sans GL testée : sélection, défilement automatique, `setItems` qui garde la sélection). En jeu : `MenuScene` actuel, flèches haut/bas.
6. **Nouveau `MenuScene`** — `mightylibdemo/scenes/MenuScene`, `resources/data/menuScenes.json`, une petite classe sans GL de la démo pour la liste (candidats de la complétion, scènes cachées, nom court/relatif, tri, filtre ; testée si le dossier `test/` accueille la démo, sinon logique placée dans la bibliothèque comme utilitaire de filtre). Mise en page de la maquette : titre en haut, à gauche le champ texte au-dessus de la liste, à droite le panneau de description (fond, texte coupé par `setMaxWidth`, erreur en rouge), Quitter en bas au centre. Navigation : `GUIList` champ → liste → Quitter (focus au clic pour le champ, Bas depuis le champ passe à la liste). Lancement : `engine().commands().execute("scene " + nomRelatif)` ; Quitter et Échap (scène au focus) : `execute("exit")` ; un `CommandResult` en erreur va dans le panneau. Supprimer `createSceneEntries`, les trois boutons en dur et les raccourcis Shift+F2/F3/F4. Garder le son de sélection. En jeu : `scenario.py "scene MenuScene" wait-loaded "?gl text" …` (filtre tapé par `input`, flèches, Entrée), `leakcheck MenuScene`.
7. **Docs** — voir plus bas.

## Démo
`MenuScene` est la démonstration (focus, champ texte, liste filtrée, commandes depuis le code). `TextField` seul : dans `GUIWidgetsScene` ou une nouvelle scène inscrite au menu (étape 4).

## Documents à mettre à jour à la fin
- `architecture.md` : Entrées (focus à la place de **Capture**), Console (la console prend le focus), Commandes (cible d'une commande lancée par le code), Texte (`setMaxWidth`), GUI (`TextField`, sélection de `ScrollList`), menu de la démo.
- `debug-tools.md` : cible des commandes lancées par le code ; vérifier le menu avec `scenario.py`.
- `decisions.md` : les boutons de la démo passent par des commandes (démonstration) ; un vrai jeu appellera le registre de scènes.
- `later.md` : section Modding, la ligne « La liste des scènes du menu… » devient : le menu lit la complétion de `scene`, la cible durable reste le registre de scènes par nom (avec descriptions et clés de langue) ; section Console et GUI en jeu : focus de la bibliothèque GUI (navigation Tab, focus par composant, plusieurs champs, focus au clavier/manette) préparé par `InputFocus`.
- `CLAUDE.md` : tests couverts (`InputFocus`, `TextWrap`, `TextField`, sélection de `ScrollList`, liste du menu si testée).
- Supprimer ce plan.

## Hors périmètre
- Registre de scènes par nom (`later.md`, Modding) : le menu lit la complétion en attendant.
- Thème et langues des composants GUI (`later.md`, Thème) : couleurs et tailles en dur pour l'instant.
- Navigation Tab et focus complet de la bibliothèque GUI : noté dans `later.md` à la fin.
- Filtre approximatif (lettres dans l'ordre).

## Questions restantes
- Démo en release sans commandes (`setMaxCommandAccess(null)`) : `Commands.execute` depuis le code fonctionne-t-il encore ? Si non, le menu doit-il le signaler ou revenir à `setNewScene` ? À vérifier au début de l'étape 6.

## Modèles recommandés
- Discussion A, étapes 1 à 4 : Claude Opus 5.5, effort `high` (modèle du focus, cible des commandes et composant réutilisable : API durable).
- Discussion A, étapes 5 à 7 : Claude Sonnet 5.5, effort `medium` (logique de liste et scène de démo, docs).
- Discussion B : Claude Opus 5.5, effort `high` (gros diff qui touche entrées, commandes et GUI).

## Déroulé
### Discussion A — implémentation (« Implémente le fichier docs/plan/menu-des-scenes.md »)
- La commande donne le modèle et l'effort recommandés pour les premières étapes (« Modèles recommandés »). Si la discussion tourne sur un autre, le dire en une ligne et attendre que l'utilisateur change ou confirme. Le rappeler en une ligne avant chaque groupe d'étapes dont la recommandation change.
- Lire ce plan en entier, puis « Pour démarrer ». Poser les questions restantes et attendre les réponses.
- Ce plan n'est pas commité : il n'existe que dans le dossier du dépôt principal. Dans un worktree, le copier depuis ce dossier.
- Branche de sujet depuis `dev` : `scene-menu`. Premier commit : ce plan tel quel.
- Un commit par étape, dans l'ordre. Compiler, tester et vérifier en jeu (`scenario.py`, prévenir l'utilisateur) à chaque étape.
- Un problème de conception : s'arrêter et demander, ne pas dévier du plan.
- La dernière étape met à jour les documents listés plus haut mais **ne supprime pas ce plan**.
- Ajouter ici une section « Bilan » : fait, écarts au plan et raison, vérifié et comment, à vérifier à la main par l'utilisateur (menu : clic dans le champ, filtre, flèches, Entrée, Échap dans et hors du champ, survol et description, console ouverte pendant la saisie ; `MultiWindowScene` ; `GUIWidgetsScene`). La commiter.
- Pousser la branche. **Pas de PR** : c'est la discussion B. Donner le nom de la branche.

### Discussion B — relecture, PR, fusion (`/pr-ml scene-menu`)
- Le skill `/pr-ml` décrit tout le processus : relecture du diff par rapport à `dev` au regard de ce plan et de son « Bilan », discussion des erreurs, corrections, docs (dont la suppression de ce plan), PR vers `dev`, fusion, retour sur `dev` à jour. Rien à ajouter ici, sauf la liste des vérifications manuelles du « Bilan ».

## Bilan

### Fait
Les étapes 1 à 7, un commit chacune sur la branche `scene-menu` (modèle de la discussion : Sonnet 5.5 partout, sur demande de l'utilisateur, y compris pour les étapes 1 à 4 recommandées en Opus).
1. `InputFocus` (pile de détenteurs par fenêtre) remplace `setCaptured` ; la console passe par lui et rend le focus aussitôt (plus de tick de délai).
2. `Commands.execute(ligne)` et `complete(début)` appelés par le code d'une vue visent la fenêtre de cette vue (`Commands.getCallerWindow`, `Engine.targetWindow`/`targetContext`) ; les commandes générales lisent leur cible par là.
3. `TextWrap` et `Text.setMaxWidth`.
4. `TextField` (`TextFieldModel` sans GL, `console/LineEditKeys` partagé avec `ConsoleInput`), scène `TextFieldScene2D`.
5. `ScrollList` : `ListSelection` sans GL, flèches, `getSelected`/`setSelected`/`getActive`/`moveSelection`.
6. `MenuScene` reconstruit, `SceneChoices` sans GL, `menuScenes.json`, batch `menu`.
7. Docs (`architecture.md`, `debug-tools.md`, `decisions.md`, `later.md`, `CLAUDE.md`).

### Écarts au plan, et pourquoi
- **Clic qui rend le focus** : `InputFocus.releaseForClick` (non prévu). Sans lui, le clic dehors qui retire le focus du champ était effacé pour la scène : cliquer sur une ligne de la liste demandait deux clics. L'appui de la souris reste pour la scène dans ce cas seulement (les touches et le texte sont toujours effacés).
- **Appui effacé pour les actions du moteur aussi** : l'effacement des appuis du tick vaut pour toutes les actions, `mightylib` comprises (plus simple, la console lit son action avant tout changement).
- **Navigation du menu sans `GUIList`** : `GUIList` ne sait pas déléguer les flèches à un composant qui en a l'usage (liste, curseur du champ) ; `MenuScene` gère trois zones (champ, liste, Quit) elle-même. Noté dans `later.md` (focus de la bibliothèque GUI). Le champ rend aussi le focus sur Haut/Bas (`TextField.getLeftWith`) pour que Bas passe à la liste.
- **Entrée dans le champ** lance la première scène filtrée ; le filtre remet la sélection sur la première ligne (`setItems` garde pourtant la sélection si l'élément existe encore, utile hors filtre).
- Un cas non couvert : deux champs de texte (un clic sur B pendant que A a le focus : l'ordre de mise à jour décide). Noté dans `later.md`.
- `WindowContext` accepte une fenêtre `null` (tests) pour créer des fenêtres factices (`TestWindows`).
- `MouseManager.getRawVirtualPos` ajouté (le champ place son curseur de souris dans l'espace virtuel pendant qu'il a le focus).
- Les descriptions des scènes du menu sont écrites d'après les noms et le code survolé : à relire.

### Question restante (étape 6)
`Commands.execute` depuis le code marche en release sans commandes : `Engine.setup` crée toujours `Commands` et la source `api` a le niveau `DEV` ; `setMaxCommandAccess(null)` ne borne que la session. Vérifié en lisant le code (un build release ne démarre pas le serveur de debug, `scenario.py` ne peut pas le piloter).

### Vérifié, et comment
- `mvn -o -q test` : 864 tests, tous passent (nouveaux : `InputFocusTest`, `TextWrapTest`, `TextFieldModelTest`, `ListSelectionTest`, `SceneChoicesTest`, complétés : `MouseRawTest`, `CommandsTest`).
- Console : ouverture/fermeture par `²`, Échap, détachement (`scenario.py`), le jeu ne voit rien pendant la saisie.
- `TextFieldScene2D` : clic, saisie, Entrée (soumet, la scène ne la voit pas), Échap (rend le focus sans quitter), console ouverte par-dessus le champ (le texte va à la console, le focus revient au champ), clic dehors ; `leakcheck` : aucune fuite.
- `TextPaddingScene` : texte coupé aux espaces, mot long coupé, saut de ligne gardé.
- `ScrollList` sur l'ancien menu (touche S) : sélection, défilement jusqu'à la ligne 12.
- `MenuScene` : filtre (`pla`), Entrée lance la première, S/S/Entrée lance la troisième, capture d'écran de la mise en page, `leakcheck MenuScene 2 TextFieldScene2D` : aucune fuite.
- `MultiWindowScene` : les commandes de debug (`windows`, `input`, `scene`) marchent toujours.

### Non vérifié (à faire à la main)
- Souris : survol d'une ligne (description du panneau, surbrillance), clic sur une ligne, molette, clic dans le champ puis clic sur une ligne (un seul clic doit lancer), clic sur Quit.
- Menu : saisie réelle au clavier (accents, Ctrl+A/C/V, Maj+flèches), flèches W/S et Entrée, Échap dans le champ (ne quitte pas) puis hors du champ (quitte), console `²` ouverte pendant la saisie puis fermée.
- `MultiWindowScene` : une commande lancée par le code de la seconde fenêtre agit sur elle (testé en unitaire seulement, pas en jeu : aucune scène de la démo ne lance de commande dans une seconde fenêtre).
- `GUIWidgetsScene` : non modifiée, rien à voir normalement.
- Le menu d'un build release (accès aux commandes `none`).
- Erreur en rouge du panneau : pas de moyen de faire échouer `scene <nom>` depuis le menu.
