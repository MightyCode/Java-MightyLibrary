# Outils de debug et de mesure

## Commandes (mode debug seulement : `EngineConfig.setMode(EEngineMode.DEBUG)`)
- `BaseCommand.execute(String[] args)` (`args[0]` = nom) **renvoie** `CommandResult.ok(message, data JSON)`/`error(...)`, sans écrire en console ; résultat différé : `CommandResult.later(future)` (abandon après `Commands.COMMAND_TIMEOUT_SECONDS` = 30 s).
- Découpage (`CommandLine.Split`) : espaces ; guillemets doubles pour un mot avec espaces ; `\` gardés (chemins Windows) ; guillemet non fermé = erreur. Dans `scenario.py` : `'screenshot "target/a b.png"'`.
- Générales dans `ListGeneralCommand` ; une scène ajoute les siennes (`Commands.addCommand`, retirées au changement de scène, `(scene)` dans `help`) ; nom pris → `IllegalArgumentException`.
- `Commands.submit(line)` depuis tout thread, exécutée au début du tick suivant (`processPendingCommands`) ; console lue dans un thread à part.
- Plusieurs ticks : `Commands.addTickTask` (comparer `Commands.getTick()`, une tâche peut être appelée plusieurs fois par tick).

## Serveur de debug
- `127.0.0.1:7777` (`EngineConfig.setDebugServerPort()`, `-1` = désactivé), sinon port libre suivant ; affiché (`--Debug server listening on …`) et écrit dans `-Dmightylib.debugServer.portFile=<fichier>` (utilisé par `scenario.py` : plusieurs jeux/scénarios simultanés, un `--log` chacun). `--attach`/`mighty.py` : `--port` si pas 7777.
- Protocole : une commande par ligne → une ligne JSON `{"success", "message", "data"}`. Client : `python tools/mighty.py <commande>` (`--wait N`, `--json`, sans argument = interactif).

## Commandes utiles
- `help` ; `scene` (courante et `loading`) / `scene <Nom>` (relatif au package de la première scène, ex. `Platformer2D.Test2DPlatformerBox2D`) ; `wait <ticks>` / `wait loaded [délai]` ; `reload <all|Type> [noms,…] [re:regex]` (sélection vide = 0 donnée, pas une erreur) ; `resources [all|Type] [noms,…] [re:regex]` (détail des données choisies, 100 au plus) ; `resources batch <nom>` (ce que charge un batch : dépendances connues, chargées, mémoire CPU/GPU estimée) ; `resources preload|unpreload <batch>` (précharge un batch, ou annule) ; `watch [status|<all|Type> [noms,…] [re:regex]|mode poll|service [ms]|off]` (rechargement à chaud sur changement de fichier, éteint par défaut, voir `architecture.md`) ; `exit` ; `resources` (voir `architecture.md`).
- `stats [champs…]` (chemins pointés, ex. `fps glObjects.Texture` ; sinon temps, appels de dessin, changements de shader/texture, uniforms, octets envoyés, objets GL, mémoire). Décrit la dernière seconde **complète** (attendre `sleep:1.5`). `stats reset` remet à zéro `maxFrameMsSinceReset`, `maxFrameIntervalMsSinceReset` (< 100 ms visé), `maxTickMsSinceReset`, `maxSwapMsSinceReset`.
- `gl` (objets GL vivants, éléments partagés et références) ; `gl summary` (par type `éléments/références`).
- `textureupload` (mode d'envoi des grosses textures à la carte et coût depuis le réglage) ; `textureupload direct|pbo` ; `textureupload bench` (compare les façons d'envoyer une texture sur cette carte, médiane de 5 essais, bloque ~1 s). Réglage au démarrage : `EngineConfig.setTextureUpload` ou `-Dmightylib.texture.upload=pbo`. Mesuré sur RTX 4080 SUPER : `pbo` n'est pas plus rapide (voir `decisions.md`).
- `screenshot [chemin]` (PNG de la frame suivante, défaut `screenshots/`) ; `scale 0.5` ; `rect x y l h` ou `corners x y x2 y2` (pixels fenêtre, origine en haut à gauche). Seulement si le texte ne suffit pas.
- `camera` (liste : position, direction/yaw/pitch/fov en 3D, zoom/rotation en 2D) ; `camera <id|nom>`. 3D : `camera main3D move x y z` / `lookat x y z`. 2D : `camera main2D move x y`, `zoom n` ou `zoom nx ny` (borné, garde le point de référence), `rotation degrés`. Une scène expose ses caméras par `registerCamera(camera, nom)` (`main3D`, `main2D` d'office).
- `monitors [full]` : écrans (résolution, position ; `full` : zone de travail, fréquence), index de celui de la fenêtre. Choix de l'écran au démarrage (liste `--Monitors :` au journal) : `-Dmightylib.window.monitor=primary|secondary|largest|<index>|<nom>`, sinon `EngineConfig.setMonitorSelector(MonitorSelectors.Primary()|Secondary()|Index(i)|Name(t)|Largest())` ; introuvable → principal ; fenêtre centrée dans la zone de travail.

## Simuler des entrées (`input`)
Appliqué par `InputManager.postUpdate()` au tick suivant, vu par tout ce tick (déterministe). Action = nom (insensible à la casse) ou id.
- Actions : `input <ACTION> [ticks]`, `input hold|release <ACTION>`, `input list` (actions puis axes).
- Souris (pixels fenêtre ; bouton `left|right|middle|<n>`) : `input mouse x y` (gardé jusqu'à un vrai mouvement), `input click [bouton] [x y]`, `input mouse hold|release [bouton]`, `input scroll y [x]`.
- Clavier (fin de `GLFW_KEY_…` ou nombre) : `input key <TOUCHE> [ticks]`, `input key hold|release <TOUCHE>`, `input text "texte"`. Passe par `KeyboardManager`/`MouseManager`, donc par les actions liées.
- Texte : lire `KeyboardManager.getTypedText()` (rappel `glfwSetCharCallback` + texte simulé), pas `getCorrespondingCharMainKeyPressed`.
- Axes : `input axis <AXE> <valeur> [ticks]`, `… hold <valeur>`, `… release` (remplace les liaisons ; ex. `input axis LOOK_RIGHT 2 30`).
- `input state` : ce que voit le tick (souris virtuelle, boutons, défilement, touches, texte, actions, axes non nuls, `cursorShown`, `cursorMode`).
- Manettes (`gamepad` ; boutons `A`, `DPAD_UP`, `START`… ; axes `LEFT_X`, `RIGHT_TRIGGER`…) : `gamepad list` (connectées, boutons tenus, axes hors zone morte, joueurs), `gamepad connect` / `disconnect <id>` (virtuelle 16+), `gamepad <id> button <B> [ticks]`, `gamepad <id> hold|release <B>`, `gamepad <id> axis <AXE> <valeur> [ticks]` / `hold <valeur>` / `release`, `gamepad assign <id> <joueur>`, `gamepad autoassign on|off`. Scène : `TestGamepadScene`.

## Scripts (`exec <fichier> [délai]`)
- Une commande par ligne, chemin relatif au répertoire de travail ; vides et `#` ignorées ; arrêt au premier échec. Réponse `{"lines": [{line, command, success, message, data}]}`. Scripts ponctuels dans `tools/scripts/` (ignoré par git) ; seul un script permanent est versionné, ailleurs.
- Les lignes s'enchaînent dans le même tick jusqu'à une réponse différée (`wait`, `screenshot`, `exec`). Entrée simulée vue au tick suivant (`input click`, `wait 1`, `input state`) ; après `scene X`, `wait loaded`.
- Délai par réponse différée : 30 s (`exec` 300 s). Script récursif refusé. Script long avec `scenario.py` : augmenter `--timeout` (`python tools/scenario.py --timeout 60 "?exec tools/scripts/x.txt"`).

## `scenario.py` (lancer et vérifier le jeu, Claude)
Une fenêtre s'ouvre chez l'utilisateur (sur l'écran secondaire, `--monitor` vide = choix du projet) : le prévenir.
```bash
python tools/scenario.py "scene Test2DScene" wait-loaded "?gl summary" "input ESCAPE" sleep:0.5 scene
```
- Compile (`--no-compile`), construit `target/classpath.txt`, lance (journal `target/scenario.log`), enchaîne, envoie `exit`, liste les lignes suspectes du journal (dédoublonnées `(xN)`). Code 0 si tout réussit.
- Étapes en plus : `wait-loaded` (fin de chargement de la scène du dernier `scene X`), `sleep:S`, `leakcheck <Scène> [n=2] [via=MenuScene]` (allers-retours ; affiche `no leak` ou les compteurs `gl` modifiés, noms sans suffixe `#id`), `log <regex> [n=10]`.
- Sortie : `ok` par étape réussie ; `?` devant une étape = réponse complète ; un échec affiche tout.
- Blocage : rien après `--timeout` s (20) → bloqué ; `wait-loaded` trop long → `BLOCKED` + état `gl`, arrêt. Jeu tué après `--max-duration` (300 s).
- `--strict` : échec ou nom inconnu arrête le jeu (sinon log `… its fallback is used` et `usingFallback` dans `resources` ; démo : `TestFallbackScene` sans `--strict`). `--attach` (jeu déjà lancé, ni lancé ni fermé), `--keep-open`.
- Souris jamais capturée (`-Dmightylib.input.noCursorCapture=true`, `MouseManager.NO_CAPTURE_PROPERTY` : curseur visible et libre pour l'utilisateur) : regard 3D testé par `input mouse`/`input axis`/`gamepad` ; `--capture-mouse` pour la capture.
- Fluidité d'un chargement : `python tools/scenario.py "scene TestGLLoading" "stats reset" wait-loaded stats` (`reset` après `scene` exclut le tick qui exécute `init`).
- Fuite à la main : `gl` sur une scène, allers-retours, `gl` à nouveau : compteurs et références identiques.

## Profilage
- **JFR** : événements `mightylib.Frame`/`mightylib.Tick`. `jcmd <pid> JFR.start duration=30s filename=rec.jfr` (pid : `jcmd -l`) ou `-XX:StartFlightRecording=…` ; `jfr print --events mightylib.Frame rec.jfr`, ou JMC/VisualVM.
- **GPU** : RenderDoc ou Nsight Graphics (OpenGL 3.3 core).
- **Erreurs GL** : `returnOpenGLErrorCheckState()` à `true` = `glGetError` + contexte de debug (messages lisibles du pilote).
