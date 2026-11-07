package io.github.mightycode.mightylib.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A choice among the registered data: all, or a type, optionally narrowed to some names and/or a regex.
 * Used wherever data must be picked (ResourceWatcher today; batchs, reload and unload of groups later).
 * When names and a regex are both given, a data is selected if it is in the names OR matches the regex
 * (the regex must match the whole data name).
 */
public final class ResourceSelection {
    private final Class<? extends DataType> type;
    private final Set<String> names;
    private final Pattern regex;

    private ResourceSelection(Class<? extends DataType> type, Set<String> names, Pattern regex) {
        this.type = type;
        this.names = names;
        this.regex = regex;
    }

    public static ResourceSelection all() {
        return new ResourceSelection(null, Collections.emptySet(), null);
    }

    public static ResourceSelection ofType(Class<? extends DataType> type) {
        return new ResourceSelection(type, Collections.emptySet(), null);
    }

    public ResourceSelection withNames(String... dataNames) {
        return new ResourceSelection(type, new LinkedHashSet<>(Arrays.asList(dataNames)), regex);
    }

    /**
     * @throws IllegalArgumentException if the regex is invalid
     */
    public ResourceSelection withRegex(String pattern) {
        try {
            return new ResourceSelection(type, names, Pattern.compile(pattern));
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex '" + pattern + "': " + e.getDescription());
        }
    }

    /**
     * Selection written as words, for the commands: all|TypeName, then any of "name1,name2" (names) and
     * "re:regex" (no space in the regex), starting at args[from].
     *
     * @throws IllegalArgumentException unknown type, invalid regex, or nothing at args[from]
     */
    public static ResourceSelection parse(String[] args, int from) {
        if (args.length <= from)
            throw new IllegalArgumentException("Missing type: all or the name of a resource type");

        ResourceSelection selection;
        if (args[from].equalsIgnoreCase("all")) {
            selection = all();
        } else {
            Class<? extends DataType> type = Resources.getClassFromName(args[from]);
            if (type == null)
                throw new IllegalArgumentException("Unknown resource type ->" + args[from] + "<-");
            selection = ofType(type);
        }

        for (int i = from + 1; i < args.length; ++i) {
            if (args[i].startsWith("re:"))
                selection = selection.withRegex(args[i].substring(3));
            else
                selection = selection.withNames(args[i].split(","));
        }
        return selection;
    }

    public Class<? extends DataType> getType() {
        return type;
    }

    public boolean matches(Class<? extends DataType> registeredType, DataType data) {
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
