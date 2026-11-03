# Outils de debug et de mesure — Java-MightyLibrary

Commandes, serveur de debug, lancement du jeu par Claude et profilage.

## Outils de debug et de mesure

### Commandes
- Une commande hérite de `BaseCommand` et implémente `CommandResult execute(String[] args)` (`args[0]` = nom). Elle **renvoie** son résultat (`CommandResult.ok(message, data JSON)` / `error(...)`), elle n'écrit pas dans la console. Résultat connu plus tard (ex. capture après la frame suivante) : `CommandResult.later(future)`.
- Les commandes générales sont enregistrées dans `ListGeneralCommand` ; une scène peut ajouter les siennes (`Commands.addCommand`, retirées au changement de scène).
- `Commands.submit(line)` est appelable depuis n'importe quel thread ; l'exécution a lieu sur le thread principal au début du tick suivant (`processPendingCommands`). La console est lue dans un thread à part : le jeu n'est jamais bloqué.
- Commandes disponibles uniquement en mode admin.

### Serveur de debug (piloter le jeu depuis un autre processus)
- En mode admin, le jeu écoute sur `127.0.0.1:7777` (`EngineConfig.setDebugServerPort()`, `-1` pour désactiver). Protocole : une commande par ligne, une ligne JSON en réponse `{"success", "message", "data"}`.
- Client : `python tools/mighty.py <commande>` (`--wait N` pour attendre le démarrage, `--json` pour la réponse brute, sans argument = mode interactif).
- Commandes utiles : `help`, `stats` (temps, appels de dessin, changements de shader/texture, uniforms, octets envoyés, objets GL vivants, mémoire), `scene` (scène courante et `loading`), `scene <Nom>` (nom relatif au package de la première scène, ex. `Platformer2D.Test2DPlatformerBox2D`), `screenshot [chemin]` (PNG de la frame suivante, par défaut dans `screenshots/`), `gl` (objets OpenGL vivants et éléments partagés avec leurs références), `input` (voir ci-dessous), `reload`, `exit`.
- **Simuler des entrées** : `input SELECT_UP` (appui d'un tick), `input <ACTION> <ticks>`, `input hold <ACTION>` / `input release <ACTION>`, `input list` (actions et ids). Une action se désigne par son nom (insensible à la casse) ou son id. Les événements sont stockés dans `InputSimulation` et appliqués quand `InputManager.postUpdate()` calcule les entrées du tick suivant : un appui simulé est vu par tout ce tick, comme un vrai appui (déterministe). La simulation agit sur les **actions** : le code qui lit directement une touche (`getKeyState(GLFW_KEY_…)`) ne la voit pas.
- **Traquer une fuite** : relever `gl` sur une scène, faire plusieurs allers-retours vers une autre scène, relever à nouveau sur la même scène. Les compteurs et les références doivent être identiques à chaque passage.

### Lancer et vérifier le jeu soi-même (Claude)
Un seul appel avec `tools/scenario.py` (une fenêtre s'ouvre sur l'écran de l'utilisateur : le prévenir) :
```bash
python tools/scenario.py "scene Test2DScene" wait-loaded gl "input ESCAPE" sleep:0.5 scene
```
- Compile (`--no-compile` pour sauter), construit `target/classpath.txt` si besoin, lance le jeu (journal dans `target/scenario.log`), enchaîne les étapes, envoie `exit`, puis liste les lignes suspectes du journal (exceptions, erreurs OpenGL). Code de sortie 0 si tout a réussi.
- Étapes en plus des commandes du jeu : `wait-loaded` (attend la fin du chargement et la scène demandée par le dernier `scene X`), `sleep:SECONDS`.
- `--attach` utilise un jeu déjà lancé (par l'utilisateur) sans le lancer ni le fermer ; `--keep-open` laisse le jeu ouvert.
- `stats` décrit la dernière seconde **complète** : attendre au moins une seconde de jeu (`sleep:1.5`) avant de la lire.
- Fluidité d'un chargement : `stats reset` remet à zéro les pires valeurs `maxFrameMsSinceReset` (travail d'une frame), `maxFrameIntervalMsSinceReset` (écart entre deux frames, à garder < 100 ms), `maxTickMsSinceReset` et `maxSwapMsSinceReset`. Ex. : `python tools/scenario.py "scene TestGLLoading" "stats reset" wait-loaded stats` (le `reset` après le changement de scène exclut le tick qui exécute `init`).
- Une capture (`screenshot <chemin>`) seulement si un rendu ne peut pas être vérifié par le texte.

### Profilage
- **JFR** : événements `mightylib.Frame` et `mightylib.Tick` (durée de chaque frame/tick), presque gratuits hors enregistrement. Enregistrer : `jcmd <pid> JFR.start duration=30s filename=rec.jfr` (pid via `jcmd -l`), ou lancer avec `-XX:StartFlightRecording=duration=30s,filename=rec.jfr`. Lire : `jfr print --events mightylib.Frame rec.jfr`, ou JDK Mission Control / VisualVM pour CPU, allocations et GC.
- **GPU** : RenderDoc ou NVIDIA Nsight Graphics (contexte OpenGL 3.3 core) pour inspecter une frame et compter les appels de dessin.
- **Erreurs OpenGL** : `returnOpenGLErrorCheckState()` à `true` active `glGetError` **et** un contexte de debug : le pilote signale les erreurs avec un message lisible (notifications filtrées).
