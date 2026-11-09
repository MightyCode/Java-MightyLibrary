package io.github.mightycode.mightylib.registry;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Unique namespaced name of a registered element ("demo:move_right"), stable between launches
 * (saved bindings, commands, mods) unlike a generated number.
 * Namespace : [a-z0-9_.-], path : [a-z0-9_.-/].
 */
public final class Identifier implements Comparable<Identifier> {
    public static final String ENGINE_NAMESPACE = "mightylib";

    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    private final String namespace;
    private final String path;

    private Identifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static Identifier of(String namespace, String path) {
        if (namespace == null || !NAMESPACE.matcher(namespace).matches())
            throw new IllegalArgumentException("Invalid namespace ->" + namespace + "<- (allowed : a-z 0-9 _ . -)");

        if (path == null || !PATH.matcher(path).matches())
            throw new IllegalArgumentException("Invalid path ->" + path + "<- (allowed : a-z 0-9 _ . - /)");

        return new Identifier(namespace, path);
    }

    // "namespace:path", the namespace is mandatory
    public static Identifier parse(String text) {
        if (text == null)
            throw new IllegalArgumentException("Null identifier");

        int separator = text.indexOf(':');
        if (separator < 0)
            throw new IllegalArgumentException("Identifier ->" + text + "<- has no namespace (namespace:path)");

        return of(text.substring(0, separator), text.substring(separator + 1));
    }

    public String namespace() { return namespace; }
    public String path() { return path; }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Identifier))
            return false;

        Identifier identifier = (Identifier) other;
        return namespace.equals(identifier.namespace) && path.equals(identifier.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    @Override
    public int compareTo(Identifier other) {
        int byNamespace = namespace.compareTo(other.namespace);
        return byNamespace != 0 ? byNamespace : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
