package io.github.mightycode.mightylib.services;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A service as a plugin registered it (EngineRegistry.services): its key, its factory, and what it declares while the
 * plugin registers (requires, after, fallback). Frozen with the registry (ServiceOrder.Sort).
 */
public final class ServiceDeclaration<T extends IEngineService> {
    private final ServiceKey<T> key;
    private final IServiceFactory<? extends T> factory;
    private final String plugin;
    // The plugin whose declaration this one replaced, null if none
    private final String replaces;

    private final Set<ServiceKey<?>> requires = new LinkedHashSet<>();
    private final Set<ServiceKey<?>> after = new LinkedHashSet<>();
    private IServiceFactory<? extends T> fallback;
    private boolean frozen;

    // Created by the registry (ServicesSection)
    public ServiceDeclaration(ServiceKey<T> key, IServiceFactory<? extends T> factory, String plugin, String replaces) {
        this.key = Objects.requireNonNull(key, "key");
        this.factory = Objects.requireNonNull(factory, "factory");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.replaces = replaces;
    }

    private void checkOpen() {
        if (frozen)
            throw new IllegalStateException("The service " + key + " is frozen: declare it from IPlugin.register only");
    }

    /**
     * Services created before this one, mandatory: the engine does not start if one of them is not registered, and this
     * one is not created if one of them failed without fallback.
     */
    public ServiceDeclaration<T> requires(ServiceKey<?>... keys) {
        checkOpen();
        for (ServiceKey<?> required : keys)
            requires.add(Objects.requireNonNull(required, "key"));

        return this;
    }

    // Services created before this one when they are registered (order only)
    public ServiceDeclaration<T> after(ServiceKey<?>... keys) {
        checkOpen();
        for (ServiceKey<?> previous : keys)
            after.add(Objects.requireNonNull(previous, "key"));

        return this;
    }

    // Used when the factory fails (the silent sound when the device can't be opened)
    public ServiceDeclaration<T> fallback(IServiceFactory<? extends T> factory) {
        checkOpen();
        fallback = Objects.requireNonNull(factory, "factory");
        return this;
    }

    void freeze() {
        frozen = true;
    }

    public ServiceKey<T> key() { return key; }

    public IServiceFactory<? extends T> factory() { return factory; }

    // Null without fallback
    public IServiceFactory<? extends T> fallbackFactory() { return fallback; }

    // The namespace of the plugin that registered (or replaced) the service
    public String plugin() { return plugin; }

    // The plugin whose declaration this one replaced, null if none
    public String replaces() { return replaces; }

    public Set<ServiceKey<?>> requiredKeys() { return Collections.unmodifiableSet(requires); }

    public Set<ServiceKey<?>> afterKeys() { return Collections.unmodifiableSet(after); }

    @Override
    public String toString() {
        return key + " (plugin " + plugin + ")";
    }
}
