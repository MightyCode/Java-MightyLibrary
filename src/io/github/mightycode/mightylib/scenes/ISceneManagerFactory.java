package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.Engine;

import java.util.function.Supplier;

/**
 * Creates the scene manager of each context (the main one at the start, the others by Engine.openContext). The engine
 * gives SceneManager::new; a plugin replaces it with registry.services().sceneManager.
 */
@FunctionalInterface
public interface ISceneManagerFactory {
    /**
     * @param main         the main context: its manager runs the pending commands of the engine at each tick
     * @param loadingScene displayed while a scene loads, unless the scene overrides createLoadingScene
     */
    ISceneManager create(Engine engine, Context context, boolean main, Supplier<TemplateSceneLoading> loadingScene);
}
