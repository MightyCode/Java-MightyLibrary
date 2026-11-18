package io.github.mightycode.mightylib.resources.internal;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.ResourceSelection;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.resources.source.SourceEntry;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ArgumentType;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandSyntaxException;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

/**
 * The words of a command that choose registered data: a type (all or the name of a loader), names, and the option
 * --regex ("reload TextureData grass sand --regex tile.*"). A data is selected if it is in the names OR matches the
 * regex (see ResourceSelection).
 */
public final class ResourceArguments {
    public static final String TYPE = "type";
    public static final String NAMES = "names";
    public static final String REGEX = "regex";
    public static final String ALL = "all";

    private ResourceArguments() {}

    // "all" or the name of a resource type (the name of its loader, for example TextureData), case ignored
    public static ArgumentType<String> type(Supplier<Resources> resources) {
        return new ArgumentType<>() {
            @Override
            public String parse(String word, CommandContext context) {
                List<String> types = typeNames(resources.get());
                for (String type : types) {
                    if (type.equals(word))
                        return type;
                }
                for (String type : types) {
                    if (type.equalsIgnoreCase(word))
                        return type;
                }

                throw CommandSyntaxException.expected("all or a resource type (" + String.join(", ", types) + ")", word);
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                return typeNames(resources.get());
            }

            @Override
            public String describe() {
                return "resource type";
            }
        };
    }

    // A logical path of the sources of the resources, completed folder by folder (folders end with '/')
    public static ArgumentType<String> sourcePath(Supplier<Resources> resources) {
        return new ArgumentType<>() {
            @Override
            public String parse(String word, CommandContext context) {
                try {
                    return ResourceSources.normalize(word);
                } catch (IllegalArgumentException e) {
                    throw CommandSyntaxException.expected("a relative path, without \"..\"", word);
                }
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                String slashes = start.replace('\\', '/');
                int last = slashes.lastIndexOf('/');
                String folder = last < 0 ? "" : slashes.substring(0, last);
                String begin = slashes.substring(last + 1);

                List<String> paths = new ArrayList<>();
                try {
                    for (SourceEntry entry : resources.get().sources().list(folder)) {
                        if (entry.name().startsWith(begin))
                            paths.add(entry.path() + (entry.isFolder() ? "/" : ""));
                    }
                } catch (IllegalArgumentException e) {
                    // Absolute or with "..": nothing to propose
                    return List.of();
                }
                return paths;
            }

            @Override
            public String describe() {
                return "path";
            }
        };
    }

    // Names of the data declared for the type read before (the argument "type"), proposed and free
    public static ArgumentType<String> names(Supplier<Resources> resources) {
        return new ArgumentType<>() {
            @Override
            public String parse(String word, CommandContext context) {
                return word;
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                Resources current = resources.get();
                String type = context.getOr(TYPE, ALL);
                Set<String> names = new LinkedHashSet<>();

                for (ResourceLoader loader : current.loaders()) {
                    if (type.equals(ALL) || type.equals(loader.getResourceNameType()))
                        names.addAll(current.getResourceNames(loader.getType()));
                }
                return names;
            }

            @Override
            public String describe() {
                return "name";
            }
        };
    }

    // A regular expression that must match the whole name
    public static ArgumentType<String> regex() {
        return new ArgumentType<>() {
            @Override
            public String parse(String word, CommandContext context) {
                try {
                    Pattern.compile(word);
                } catch (PatternSyntaxException e) {
                    throw CommandSyntaxException.expected("a regular expression (" + e.getDescription() + ")", word);
                }
                return word;
            }

            @Override
            public String describe() {
                return "regex";
            }
        };
    }

    // --regex pattern: the names matching the whole pattern are selected too
    public static Option regexOption() {
        return Option.of(REGEX, "Also the data whose whole name matches the regular expression (no need to quote it"
                + " unless it has a space)").value("pattern", regex());
    }

    /**
     * The branches "type [names...] [--regex pattern]", the argument <type> being the node returned: it runs the
     * handler with the selection read from the line.
     * @param description of the type argument, shown with the usage
     */
    public static CommandNode selection(Supplier<Resources> resources, String description,
                                        Function<ResourceSelection, CommandResult> handler) {
        return argument(TYPE, type(resources), description)
                .option(regexOption())
                .executes("The whole type", context -> handler.apply(read(resources.get(), context)))
                .then(argument(NAMES, names(resources), "Names of the data, the data named or matching --regex are chosen")
                        .variadic()
                        .executes("Only the data of this type with these names, or matching --regex",
                                context -> handler.apply(read(resources.get(), context))));
    }

    // The selection of the words read: type, names and regex
    public static ResourceSelection read(Resources resources, CommandContext context) {
        String type = context.get(TYPE);
        ResourceSelection selection;
        if (type.equals(ALL)) {
            selection = ResourceSelection.all();
        } else {
            Class<? extends DataType> dataType = resources.getClassFromName(type);
            if (dataType == null)
                throw new IllegalStateException("Unknown resource type ->" + type + "<-");
            selection = ResourceSelection.ofType(dataType);
        }

        List<String> names = context.getList(NAMES);
        if (!names.isEmpty())
            selection = selection.withNames(names.toArray(new String[0]));
        if (context.has(REGEX))
            selection = selection.withRegex(context.get(REGEX));
        return selection;
    }

    private static List<String> typeNames(Resources resources) {
        List<String> names = new ArrayList<>();
        names.add(ALL);
        for (ResourceLoader loader : resources.loaders())
            names.add(loader.getResourceNameType());
        return names;
    }
}
