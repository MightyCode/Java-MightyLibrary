package io.github.mightycode.mightylib.services;

import io.github.mightycode.mightylib.main.Engine;

/**
 * Creates a service while the engine starts, on the main thread: after the resources, the GL elements and the commands,
 * before the scene managers and the first scene. The services it depends on (requires, after) are already created
 * (Engine.service). Never returns null: an exception or null is a failure (the fallback of the key is then used).
 */
@FunctionalInterface
public interface IServiceFactory<T extends IEngineService> {
    T create(Engine engine);
}
