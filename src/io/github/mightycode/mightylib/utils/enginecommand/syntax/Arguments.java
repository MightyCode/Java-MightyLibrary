package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.registry.Identifier;

import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The usual argument types. A game or a mod writes its own ArgumentType for its values.
 */
public final class Arguments {
    // Plain decimal: no NaN, Infinity, hexadecimal or "f" suffix
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");
    // Values listed in an error message, a longer list is only described
    private static final int MAX_LISTED = 10;

    private Arguments() {}

    // An argument type from functions
    private static final class Simple<T> implements ArgumentType<T> {
        private final String description;
        private final BiFunction<String, CommandContext, T> parser;
        private final Supplier<? extends Collection<String>> completion;
        private final List<String> fixed;
        private final boolean greedy;

        private Simple(String description, BiFunction<String, CommandContext, T> parser,
                       Supplier<? extends Collection<String>> completion, List<String> fixed, boolean greedy) {
            this.description = description;
            this.parser = parser;
            this.completion = completion;
            this.fixed = fixed;
            this.greedy = greedy;
        }

        @Override
        public T parse(String word, CommandContext context) {
            return parser.apply(word, context);
        }

        @Override
        public Collection<String> complete(String start, CommandContext context) {
            if (completion == null)
                return Collections.emptyList();

            Collection<String> values = completion.get();
            return values == null ? Collections.emptyList() : values;
        }

        @Override
        public String describe() {
            return description;
        }

        @Override
        public List<String> fixedValues() {
            return fixed;
        }

        @Override
        public boolean isGreedy() {
            return greedy;
        }
    }

    // ---- Numbers ----

    public static ArgumentType<Integer> integer() {
        return integer(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static ArgumentType<Integer> integer(int min, int max) {
        String bounds = Bounds(min == Integer.MIN_VALUE ? null : String.valueOf(min),
                max == Integer.MAX_VALUE ? null : String.valueOf(max));
        return new Simple<>("integer" + Range(bounds), (word, context) -> {
            int value;
            try {
                value = Integer.parseInt(word);
            } catch (NumberFormatException e) {
                throw CommandSyntaxException.expected("an integer" + bounds, word);
            }
            if (value < min || value > max)
                throw CommandSyntaxException.expected("an integer" + bounds, word);
            return value;
        }, null, Collections.emptyList(), false);
    }

    public static ArgumentType<Long> longValue() {
        return longValue(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static ArgumentType<Long> longValue(long min, long max) {
        String bounds = Bounds(min == Long.MIN_VALUE ? null : String.valueOf(min),
                max == Long.MAX_VALUE ? null : String.valueOf(max));
        return new Simple<>("integer" + Range(bounds), (word, context) -> {
            long value;
            try {
                value = Long.parseLong(word);
            } catch (NumberFormatException e) {
                throw CommandSyntaxException.expected("an integer" + bounds, word);
            }
            if (value < min || value > max)
                throw CommandSyntaxException.expected("an integer" + bounds, word);
            return value;
        }, null, Collections.emptyList(), false);
    }

    public static ArgumentType<Double> decimal() {
        return decimal(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public static ArgumentType<Double> decimal(double min, double max) {
        String bounds = Bounds(Double.isInfinite(min) ? null : Format(min), Double.isInfinite(max) ? null : Format(max));
        return new Simple<>("decimal" + Range(bounds), (word, context) -> {
            if (!DECIMAL.matcher(word).matches())
                throw CommandSyntaxException.expected("a number" + bounds, word);

            double value = Double.parseDouble(word);
            if (Double.isInfinite(value) || value < min || value > max)
                throw CommandSyntaxException.expected("a number" + bounds, word);
            return value;
        }, null, Collections.emptyList(), false);
    }

    // " between 1 and 100", " >= 1", " <= 5" or ""
    private static String Bounds(String min, String max) {
        if (min != null && max != null)
            return " between " + min + " and " + max;
        if (min != null)
            return " >= " + min;
        if (max != null)
            return " <= " + max;
        return "";
    }

    // " 1..100" from " between 1 and 100", for the short description
    private static String Range(String bounds) {
        if (bounds.isEmpty())
            return "";
        if (bounds.startsWith(" between "))
            return " " + bounds.substring(" between ".length()).replace(" and ", "..");
        return bounds;
    }

    // 0.5, 4 (not 4.0)
    static String Format(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    // ---- Words ----

    public static ArgumentType<Boolean> bool() {
        List<String> values = List.of("true", "false");
        return new Simple<>("boolean", (word, context) -> {
            if (word.equalsIgnoreCase("true"))
                return true;
            if (word.equalsIgnoreCase("false"))
                return false;
            throw CommandSyntaxException.expected("true or false", word);
        }, () -> values, values, false);
    }

    // Any word
    public static ArgumentType<String> word() {
        return new Simple<>("word", (word, context) -> word, null, Collections.emptyList(), false);
    }

    /**
     * The rest of the line, options and "--" included, as one value: the last argument of its branch. A single word
     * is given as it is (input text "a b" gives a b), several words are joined so that splitting the value gives them
     * back (as user screenshot "a b.png" gives screenshot "a b.png").
     */
    public static ArgumentType<String> text() {
        return new Simple<>("text", (word, context) -> word, null, Collections.emptyList(), true);
    }

    // One of the values, case ignored: the value as declared
    public static ArgumentType<String> choice(String... values) {
        List<String> list = List.of(values);
        Set<String> lower = new HashSet<>();
        for (String value : list) {
            if (!lower.add(value.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Two choices ->" + value + "<-");
        }

        return new Simple<>("choice", (word, context) -> Match(word, list, true), () -> list, list, false);
    }

    // A constant of the enum, case ignored, proposed in lowercase
    public static <E extends Enum<E>> ArgumentType<E> enumOf(Class<E> type) {
        List<String> names = new ArrayList<>();
        for (E constant : type.getEnumConstants())
            names.add(constant.name().toLowerCase(Locale.ROOT));
        List<String> list = Collections.unmodifiableList(names);

        return new Simple<>("choice", (word, context) -> {
            String name = Match(word, list, true);
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(name))
                    return constant;
            }
            throw CommandSyntaxException.expected(OneOf(list), word);
        }, () -> list, list, false);
    }

    /**
     * One of the values given now (windows, cameras, actions...), case ignored: the value as given. Strict: another
     * word is refused. Asked at each parse and completion (main thread).
     */
    public static ArgumentType<String> oneOf(Supplier<? extends Collection<String>> values) {
        return new Simple<>("value", (word, context) -> {
            Collection<String> current = values.get();
            return Match(word, current == null ? Collections.emptyList() : current, false);
        }, values, Collections.emptyList(), false);
    }

    // Any word, the values are only proposed
    public static ArgumentType<String> suggest(Supplier<? extends Collection<String>> values) {
        return new Simple<>("word", (word, context) -> word, values, Collections.emptyList(), false);
    }

    // namespace:path
    public static ArgumentType<Identifier> identifier() {
        return new Simple<>("identifier", (word, context) -> {
            try {
                return Identifier.parse(word.toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw CommandSyntaxException.expected("an identifier (namespace:path)", word);
            }
        }, null, Collections.emptyList(), false);
    }

    // A file or a folder, relative to the working directory or absolute, completed from the disk
    public static ArgumentType<Path> path() {
        return path("");
    }

    // A file or a folder, relative to the folder ("" for the working directory) or absolute, completed from the disk
    public static ArgumentType<Path> path(String folder) {
        return new ArgumentType<Path>() {
            @Override
            public Path parse(String word, CommandContext context) {
                if (word.isEmpty())
                    throw CommandSyntaxException.expected("a path", word);
                try {
                    return PathCompletion.Resolve(folder, word);
                } catch (InvalidPathException e) {
                    throw CommandSyntaxException.expected("a path", word);
                }
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                return PathCompletion.Complete(folder, start);
            }

            @Override
            public String describe() {
                return "path";
            }
        };
    }

    // The value matching the word: the same, else the same ignoring case
    private static String Match(String word, Collection<String> values, boolean listed) {
        for (String value : values) {
            if (value.equals(word))
                return value;
        }
        for (String value : values) {
            if (value.equalsIgnoreCase(word))
                return value;
        }

        throw CommandSyntaxException.expected(listed || values.size() <= MAX_LISTED ? OneOf(values) : "a known value", word);
    }

    private static String OneOf(Collection<String> values) {
        return values.isEmpty() ? "a known value (none now)" : "one of : " + String.join(", ", values);
    }
}
