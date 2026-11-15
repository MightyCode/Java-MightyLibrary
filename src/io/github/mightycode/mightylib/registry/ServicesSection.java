package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.inputs.IInputManagerFactory;
import io.github.mightycode.mightylib.scenes.ISceneManagerFactory;
import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.services.IServiceFactory;
import io.github.mightycode.mightylib.services.ServiceDeclaration;
import io.github.mightycode.mightylib.services.ServiceKey;

import java.util.Objects;

/**
 * The services of a plugin (EngineRegistry.services): unique systems of the engine, created once at the start in the
 * order of their dependencies (ServiceOrder). Each register or replace gives the declaration where to chain requires,
 * after and fallback. Also the factories of the managers of each context (scenes) and each window (inputs), given by
 * the engine and replaced by a plugin: the last one wins.
 */
public final class ServicesSection {
    private final Registrations registrations;
    private final EngineRegistry registry;

    ServicesSection(Registrations registrations, EngineRegistry registry) {
        this.registrations = registrations;
        this.registry = registry;
    }

    /**
     * @throws IllegalArgumentException if the key is outside the namespace of the plugin
     * @throws IllegalStateException    if a service has this key: the message names both plugins
     */
    public <T extends IEngineService> ServiceDeclaration<T> register(ServiceKey<T> key,
                                                                     IServiceFactory<? extends T> factory) {
        registrations.checkOpen();
        ServiceDeclaration<T> declaration = new ServiceDeclaration<>(key, factory, registry.namespace(), null);
        registry.checkNamespace(key.id());
        registrations.services.register(registry.namespace(), key, declaration);
        return declaration;
    }

    /**
     * Replaces the service of a previous plugin (the engine included) with this key: the new declaration states its
     * own dependencies and fallback, it keeps none of the replaced one.
     * @throws IllegalArgumentException if the contract of the key differs from the registered one
     * @throws IllegalStateException    if no service has this key
     */
    public <T extends IEngineService> ServiceDeclaration<T> replace(ServiceKey<T> key,
                                                                    IServiceFactory<? extends T> factory) {
        registrations.checkOpen();
        Objects.requireNonNull(key, "key");
        ServiceDeclaration<?> replaced = registrations.services.get(key);
        if (replaced != null && replaced.key().type() != key.type())
            throw new IllegalArgumentException("The service " + key + " has the contract "
                    + replaced.key().type().getName() + ", not " + key.type().getName());

        ServiceDeclaration<T> declaration = new ServiceDeclaration<>(key, factory, registry.namespace(),
                replaced == null ? null : replaced.plugin());
        registrations.services.replace(registry.namespace(), key, declaration);
        return declaration;
    }

    // The scene manager of each context, created by this factory (the engine's: SceneManager::new)
    public ServicesSection sceneManager(ISceneManagerFactory factory) {
        registrations.checkOpen();
        registrations.sceneManagerFactory.set(registry.namespace(), factory);
        return this;
    }

    // The input manager of each window, created by this factory (the engine's: InputManager::new)
    public ServicesSection inputManager(IInputManagerFactory factory) {
        registrations.checkOpen();
        registrations.inputManagerFactory.set(registry.namespace(), factory);
        return this;
    }
}
