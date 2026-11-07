package io.github.mightycode.mightylib.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Which data the ResourceWatcher watches: all, or a type, optionally narrowed to some names and/or a regex.
 * When names and a regex are both given, a data is selected if it is in the names OR matches the regex
 * (the regex must match the whole data name).
 */
public final class WatchSelection {
    private final Class<? extends DataType> type;
    private final Set<String> names;
    private final Pattern regex;

    private WatchSelection(Class<? extends DataType> type, Set<String> names, Pattern regex) {
        this.type = type;
        this.names = names;
        this.regex = regex;
    }

    public static WatchSelection all() {
        return new WatchSelection(null, Collections.emptySet(), null);
    }

    public static WatchSelection ofType(Class<? extends DataType> type) {
        return new WatchSelection(type, Collections.emptySet(), null);
    }

    public WatchSelection withNames(String... dataNames) {
        return new WatchSelection(type, new LinkedHashSet<>(Arrays.asList(dataNames)), regex);
    }

    /**
     * @throws IllegalArgumentException if the regex is invalid
     */
    public WatchSelection withRegex(String pattern) {
        try {
            return new WatchSelection(type, names, Pattern.compile(pattern));
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex '" + pattern + "': " + e.getDescription());
        }
    }

    public Class<? extends DataType> getType() {
        return type;
    }

    boolean matches(Class<? extends DataType> registeredType, DataType data) {
        if (type != null && !type.isAssignableFrom(registeredType) && !type.isAssignableFrom(data.getClass()))
            return false;

        if (names.isEmpty() && regex == null)
            return true;

        return names.contains(data.getDataName()) || (regex != null && regex.matcher(data.getDataName()).matches());
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(type == null ? "all" : type.getSimpleName());
        if (!names.isEmpty())
            text.append(" names=").append(String.join(",", names));
        if (regex != null)
            text.append(" regex=").append(regex.pattern());
        return text.toString();
    }
}
