# Gros point actuel — API : corrections urgentes

Seul le gros point en cours est ici. La feuille de route globale est la chaîne `docs/later.md` → `docs/techniquegraphique.md`. Retirer chaque objectif une fois atteint ; quand ce fichier est vide, prendre le gros point suivant en tête de `docs/later.md`, **en discuter d'abord avec l'utilisateur** pour s'assurer que tout est prévu, puis l'écrire ici.

Propositions commentées, **à valider avec l'utilisateur avant d'appliquer**. Les changements cassants se font maintenant, avant toute version publiée ; marquées (U) = les plus urgentes.

**Ensuite :**
- **Chargement GL, suite** (le chargement par tranches avec le reste de la frame est fait, mesuré par `TestGLLoading`) : PBO, ou contexte GL partagé dans un second thread (fenêtre GLFW cachée + `glFenceSync`), seulement si des mesures montrent que les bandes ne suffisent pas (dépendant des pilotes, complexe). `ModelRenderer` envoie un mesh par frame ; `HeightMapRenderer` et `TerrainRenderer` envoient encore leur géométrie d'un bloc.
- **Uniforms absents d'un shader** : le compilateur GLSL retire les uniforms inutilisés, et `updateShaderValue` sur un tel uniform lève une exception (contournée sur la branche `dev` par un `if (false)`) → avertissement unique par shader/uniform plutôt qu'exception.
- **Singletons statiques** (`Resources`, `GLResources`, `ShaderManager`, `ContextManager`, `SoundManager`) et `Resources.getInstance()` qui crée une instance par défaut en silence → accès via un contexte moteur, ou au minimum échec explicite. Lié à l'injection (`later.md` : Bibliothèque réutilisable).
- **IDs d'action `int` statiques** (`InputManager.getAndIncrementId`) → identifiants texte avec espace de noms enregistrés au registre. Lié au remappage (`later.md` : Entrées) et au modding.
- **Champs publics modifiables** (`Resources.Loaders`, `GUIList.GUIs`, `ShouldLoop`, `WindowCreationInfo.Size`…) → méthodes.
- **Fautes et noms trompeurs** : package `lightning` (= `lighting`), `getCurrentTIme`, `canBeLoad`…
- **Shaders découpés comme les textures** (TODO `Shader.java`) : aujourd'hui `Shader` (`MAIN_CONTEXT`) lit les `.glsl`, ajoute la bibliothèque GLSL (`ShaderLoader.completeWithLibraryFile`), compile (`setShaderContent`) et lie (`internLoad`) d'un bloc sur le thread principal. Cible : lecture et prétraitement (`#include`, `#define`, variantes) par les workers ; programme GL dans un `GLElement` de `GLResources` (compilation par tranches, références, suivi `gl`) ; avec `GL_KHR_parallel_shader_compile`, compiler sans attendre et interroger `GL_COMPLETION_STATUS_KHR` aux frames suivantes. Prépare l'injection de shaders par le projet (`later.md` : Bibliothèque réutilisable) et le rechargement à chaud.
- **`Resources.addResourceToProcess`** (TODO `Resources.java`) : « ajoute » mais exécute tout de suite les données `IMMEDIATELY_IN_CURRENT_CONTEXT` → traiter ce cas en amont, ou renommer (`processOrQueue`).
