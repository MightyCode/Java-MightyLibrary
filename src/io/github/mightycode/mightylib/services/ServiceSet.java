package io.github.mightycode.mightylib.services;

import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The services of the engine: created once in their order (ServiceOrder), driven by the engine through their phases
 * (IEngineService), released in the reverse order. Main thread only.
 */
public final class ServiceSet {
    private final List<RunningService> running = new ArrayList<>();
    private final Map<ServiceKey<?>, RunningService> byKey = new HashMap<>();
    // By overlay layer, then order of creation
    private final List<IEngineService> overlays = new ArrayList<>();
    // Factories used instead of the declared ones (launch options)
    private final Map<ServiceKey<?>, IServiceFactory<?>> overrides = new HashMap<>();
    private boolean failed;

    /**
     * Before create: the service of this key is created by this factory, whatever plugin declared it, without
     * fallback (-Dmightylib.sound=silent).
     */
    public <T extends IEngineService> void override(ServiceKey<T> key, IServiceFactory<? extends T> factory) {
        overrides.put(key, factory);
    }

    /**
     * Creates the services in the given order (sorted). A factory that throws or returns null is replaced by the
     * fallback of its key; without fallback (or if the fallback fails too) the services that require it are not
     * created, the others are.
     * @return false if a service could not be created (the engine stops: ListError.SERVICE_FAIL)
     */
    public boolean create(Engine engine, List<ServiceDeclaration<?>> declarations) {
        for (ServiceDeclaration<?> declaration : declarations) {
            ServiceKey<?> missing = firstMissingRequirement(declaration);
            if (missing != null) {
                System.err.println("--Service " + declaration + " not created: it requires " + missing
                        + ", not created");
                failed = true;
                continue;
            }

            IServiceFactory<?> override = overrides.get(declaration.key());
            if (override != null)
                System.out.println("--Service " + declaration + " overridden by a launch option");

            IEngineService service = tryCreate(engine, declaration,
                    override != null ? override : declaration.factory(), "");
            boolean fallback = false;
            if (service == null && override == null && declaration.fallbackFactory() != null) {
                service = tryCreate(engine, declaration, declaration.fallbackFactory(), "fallback of ");
                fallback = service != null;
                if (fallback)
                    System.out.println("--Service " + declaration.key() + ": fallback to "
                            + service.getClass().getSimpleName());
            }

            if (service == null) {
                System.err.println("--Service " + declaration + " failed"
                        + (override != null || declaration.fallbackFactory() == null ? ", without fallback"
                        : ", its fallback too"));
                failed = true;
                continue;
            }

            RunningService entry = new RunningService(declaration, service, fallback);
            running.add(entry);
            byKey.put(declaration.key(), entry);
        }

        for (RunningService entry : running)
            overlays.add(entry.service());
        // Stable: the order of creation among a layer
        overlays.sort(Comparator.comparingInt(IEngineService::overlayLayer));

        return !failed;
    }

    private ServiceKey<?> firstMissingRequirement(ServiceDeclaration<?> declaration) {
        for (ServiceKey<?> required : declaration.requiredKeys()) {
            if (!byKey.containsKey(required))
                return required;
        }

        return null;
    }

    // Null if the factory failed (reported)
    private static IEngineService tryCreate(Engine engine, ServiceDeclaration<?> declaration,
                                            IServiceFactory<?> factory, String what) {
        try {
            IEngineService service = factory.create(engine);
            if (service == null)
                System.err.println("--The " + what + "factory of the service " + declaration + " returned null");
            return service;
        } catch (RuntimeException e) {
            Logger.LogException("The " + what + "factory of the service " + declaration + " failed", e);
            return null;
        }
    }

    // A service could not be created
    public boolean hasFailed() {
        return failed;
    }

    public boolean has(ServiceKey<?> key) {
        return byKey.containsKey(key);
    }

    /**
     * @throws IllegalArgumentException if no service has this key (Engine.hasService tests an optional one), or its
     *                                  contract is not the one of the key
     */
    public <T extends IEngineService> T get(ServiceKey<T> key) {
        RunningService entry = byKey.get(key);
        if (entry == null)
            throw new IllegalArgumentException("No service " + key + " (test an optional service with hasService)");
        if (entry.key().type() != key.type())
            throw new IllegalArgumentException("The service " + key + " has the contract "
                    + entry.key().type().getName() + ", not " + key.type().getName());

        return key.type().cast(entry.service());
    }

    // The services created, in their order of creation
    public List<RunningService> running() {
        return Collections.unmodifiableList(running);
    }

    public void update() {
        for (RunningService entry : running)
            entry.service().update();
    }

    public void lateUpdate() {
        for (RunningService entry : running)
            entry.service().lateUpdate();
    }

    public void displayOverlays() {
        for (IEngineService service : overlays)
            service.displayOverlay();
    }

    // Reverse order; a service that fails does not prevent the others from being released
    public void unload() {
        for (int i = running.size() - 1; i >= 0; --i) {
            RunningService entry = running.get(i);
            try {
                entry.service().unload();
            } catch (RuntimeException e) {
                Logger.LogException("The unload of the service " + entry.declaration() + " failed", e);
            }
        }
    }

    // Reverse order, after unload; the set is then empty
    public void close() {
        for (int i = running.size() - 1; i >= 0; --i) {
            RunningService entry = running.get(i);
            try {
                entry.service().close();
            } catch (RuntimeException e) {
                Logger.LogException("The close of the service " + entry.declaration() + " failed", e);
            }
        }

        running.clear();
        byKey.clear();
        overlays.clear();
    }
}
