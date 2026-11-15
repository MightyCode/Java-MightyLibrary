package io.github.mightycode.mightylib.services;

/**
 * A service created by the engine, with the declaration it comes from (Engine.services, command services).
 */
public final class RunningService {
    private final ServiceDeclaration<?> declaration;
    private final IEngineService service;
    private final boolean fallback;

    RunningService(ServiceDeclaration<?> declaration, IEngineService service, boolean fallback) {
        this.declaration = declaration;
        this.service = service;
        this.fallback = fallback;
    }

    public ServiceDeclaration<?> declaration() { return declaration; }

    public ServiceKey<?> key() { return declaration.key(); }

    public IEngineService service() { return service; }

    // Created by the fallback of the key, its factory having failed
    public boolean isFallback() { return fallback; }
}
