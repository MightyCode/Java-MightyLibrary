package io.github.mightycode.mightylib.services;

import io.github.mightycode.mightylib.registry.Identifier;

import java.util.Objects;

/**
 * Names a service and its contract: Engine.service(key) gives the service as its contract, without cast. Two keys are
 * equal when their identifiers are.
 */
public final class ServiceKey<T extends IEngineService> {
    private final Identifier id;
    private final Class<T> type;

    private ServiceKey(Identifier id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    public static <T extends IEngineService> ServiceKey<T> of(Identifier id, Class<T> type) {
        return new ServiceKey<>(Objects.requireNonNull(id, "id"), Objects.requireNonNull(type, "type"));
    }

    public Identifier id() { return id; }

    // The contract of the service
    public Class<T> type() { return type; }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof ServiceKey && id.equals(((ServiceKey<?>) other).id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
