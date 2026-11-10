package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.utils.enginecommand.CommandException;
import io.github.mightycode.mightylib.utils.enginecommand.CommandLine;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Reads the words of a command line with its declared syntax: the branch reached and the typed values, or an error
 * with the usage of the branch reached.
 * - literals before arguments at the same place, then the arguments in their order (the first one accepting the word);
 * - options "--name value", "--name=value", flags "--name", anywhere after the node declaring them;
 * - "--": the following words are positional only;
 * - "--help" anywhere (even after an error): the help of the command.
 */
public final class CommandParser {
    private static final String END_OF_OPTIONS = "--";

    private final String commandName;
    private final CommandNode root;

    /**
     * @param root checked at once (IllegalStateException for a mistake of the syntax)
     */
    public CommandParser(CommandNode root) {
        if (root == null || root.kind() != CommandNode.Kind.ROOT)
            throw new IllegalArgumentException("A parser needs the root of a syntax");

        root.validate();
        this.commandName = root.name();
        this.root = root;
    }

    public CommandNode getRoot() {
        return root;
    }

    /** What a line gives: the branch to run with its values, or the help. */
    public static final class Parsed {
        private final CommandNode node;
        private final CommandContext context;

        private Parsed(CommandNode node, CommandContext context) {
            this.node = node;
            this.context = context;
        }

        // --help was typed
        public boolean isHelp() {
            return node == null;
        }

        public CommandContext getContext() {
            return context;
        }

        // Null for the help
        public ICommandHandler getHandler() {
            return node == null ? null : node.handler();
        }
    }

    /**
     * @param words   the words after the name of the command
     * @param context filled with the values read
     * @throws CommandSyntaxException for a line that does not follow the syntax, with the usage of the branch reached
     */
    public Parsed parse(String[] words, CommandContext context) {
        Walk walk = new Walk(context, false);
        try {
            walk.start();
            while (walk.index < words.length && !walk.help)
                walk.read(words);

            if (walk.help)
                return new Parsed(null, context);

            walk.finish();
            return new Parsed(walk.node, context);
        } catch (CommandException e) {
            // "spawn 500 --help": the help is asked, not the error
            if (HelpAfter(words, walk.index))
                return new Parsed(null, context);
            if (!(e instanceof CommandSyntaxException))
                throw e;

            throw new CommandSyntaxException(e.getMessage() + "\n" + CommandHelp.UsageBlock(commandName,
                    CommandHelp.Branches(walk.path, node -> node.isVisible(context)), option -> option.isVisible(context)));
        }
    }

    /**
     * Words that can replace the word being typed, unfiltered (the caller keeps those starting with it): the literals
     * and values of the expected argument; the options not given yet and --help when the word starts with "-"; the
     * values of an option waiting for one. Nothing the source does not see. Checked conditions are not asked.
     * @param words the words after the name of the command, the last one being typed (possibly empty)
     */
    public List<String> complete(String[] words, CommandContext context) {
        if (words.length == 0)
            return Collections.emptyList();

        String[] before = Arrays.copyOf(words, words.length - 1);
        Walk walk = new Walk(context, true);
        try {
            walk.start();
            while (walk.index < before.length && !walk.help)
                walk.read(before);
        } catch (CommandException e) {
            // A word that can't be read: the branch is unknown
            return Collections.emptyList();
        }

        if (walk.help)
            return Collections.emptyList();
        return walk.candidates(words[words.length - 1]);
    }

    // The help of the command for the source: text and visible tree in JSON, the same for help <cmd> and <cmd> --help
    public CommandResult help(String id, ECommandLevel level, CommandContext context) {
        return CommandResult.ok(helpText(id, context), helpJson(id, level, context));
    }

    public String helpText(String id, CommandContext context) {
        return CommandHelp.HelpText(id, root, context);
    }

    public JSONObject helpJson(String id, ECommandLevel level, CommandContext context) {
        return CommandHelp.HelpJson(id, level, root, context);
    }

    /**
     * False when a hidden condition of the root is false: the command does not exist for the source, like a command
     * above its level.
     */
    public boolean isAvailable(CommandContext context) {
        return root.isVisible(context);
    }

    private static boolean HelpAfter(String[] words, int from) {
        for (int i = Math.max(0, from); i < words.length; ++i) {
            if (words[i].equals(END_OF_OPTIONS))
                return false;
            if (words[i].equalsIgnoreCase("--" + Option.HELP))
                return true;
        }
        return false;
    }

    static boolean IsOptionWord(String word) {
        return word.length() > 2 && word.startsWith("--");
    }

    // The words of a greedy argument as one value: a single word as it is, several ones joined to split the same way
    static String JoinRest(String[] words, int from, int to) {
        if (to - from == 1)
            return words[from];

        List<String> rest = new ArrayList<>();
        for (int i = from; i < to; ++i)
            rest.add(CommandLine.Quote(words[i]));
        return String.join(" ", rest);
    }

    // Reading of a line, word after word
    private final class Walk {
        private final CommandContext context;
        // Completion: nothing is checked, what can't be read stops the walk
        private final boolean completing;
        // From the root to the node reached
        private final List<CommandNode> path;
        // Options valid at the node reached, in their order
        private final List<Option> active;
        private final List<Option> given;

        private CommandNode node;
        private int index;
        private boolean positionalOnly;
        private boolean help;
        // Completion only: the option waiting for its value of index pendingValue
        private Option pending;
        private int pendingValue;
        // Completion only: a greedy argument takes the word being typed
        private boolean inGreedy;

        private Walk(CommandContext context, boolean completing) {
            this.context = context;
            this.completing = completing;
            this.path = new ArrayList<>();
            this.active = new ArrayList<>();
            this.given = new ArrayList<>();
        }

        private void start() {
            enter(root);
        }

        // Its checked conditions are asked on the way, before the words that depend on them (cameras of a scene)
        private void enter(CommandNode next) {
            if (!completing)
                next.check(context);

            node = next;
            path.add(next);
            for (Option option : next.options()) {
                active.add(option);
                if (option.hasDefault())
                    context.setDefault(option.getName(), option.getDefault());
            }
        }

        // Reads the word at index and the values it needs
        private void read(String[] words) {
            String word = words[index];
            if (!positionalOnly && word.equals(END_OF_OPTIONS)) {
                positionalOnly = true;
                ++index;
            } else if (!positionalOnly && IsOptionWord(word)) {
                readOption(words);
            } else {
                readPositional(words);
            }
        }

        private void readOption(String[] words) {
            String typed = words[index].substring(2);
            String inline = null;
            int equals = typed.indexOf('=');
            if (equals >= 0) {
                inline = typed.substring(equals + 1);
                typed = typed.substring(0, equals);
            }

            String name = typed.toLowerCase(Locale.ROOT);
            if (name.equals(Option.HELP) && inline == null) {
                help = true;
                index = words.length;
                return;
            }

            Option option = findActive(name);
            if (option == null)
                throw new CommandSyntaxException("Unknown option ->--" + typed + "<-");
            if (given.contains(option) && !option.isRepeatable())
                throw new CommandSyntaxException("The option --" + option.getName() + " is given twice");
            for (Option other : given) {
                if (other != option && option.conflictsWith(other))
                    throw new CommandSyntaxException("The options --" + other.getName() + " and --" + option.getName()
                            + " can't be given together");
            }

            if (!completing)
                option.check(context);
            given.add(option);
            ++index;

            if (option.valueCount() == 0) {
                if (inline != null)
                    throw new CommandSyntaxException("The option --" + option.getName() + " takes no value");
                context.add(option.getName(), Boolean.TRUE);
                return;
            }

            List<Object> values = new ArrayList<>();
            for (int i = 0; i < option.valueCount(); ++i) {
                String value;
                if (i == 0 && inline != null) {
                    value = inline;
                } else if (index < words.length && !words[index].equals(END_OF_OPTIONS) && !IsOptionWord(words[index])) {
                    value = words[index++];
                } else if (completing && index >= words.length) {
                    pending = option;
                    pendingValue = i;
                    return;
                } else {
                    throw new CommandSyntaxException("The option --" + option.getName() + " needs its values : "
                            + CommandHelp.OptionWords(option));
                }

                values.add(parseValue(option.valueType(i), value, "--" + option.getName() + " <" + option.valueName(i) + ">"));
            }

            context.add(option.getName(), values.size() == 1 ? values.get(0) : Collections.unmodifiableList(values));
        }

        private Option findActive(String name) {
            for (Option option : active) {
                if (option.getName().equals(name) && option.isVisible(context))
                    return option;
            }
            return null;
        }

        private void readPositional(String[] words) {
            String word = words[index];

            // A variadic argument takes every following word
            if (node.isArgument() && node.isVariadic()) {
                context.add(node.name(), parseValue(node.type(), word, "<" + node.name() + ">"));
                ++index;
                return;
            }

            for (CommandNode child : node.children()) {
                if (child.isLiteral() && child.name().equalsIgnoreCase(word) && child.isVisible(context)) {
                    enter(child);
                    ++index;
                    return;
                }
            }

            CommandSyntaxException refused = null;
            for (CommandNode child : node.children()) {
                if (!child.isArgument() || !child.isVisible(context))
                    continue;

                if (child.isGreedy()) {
                    if (!completing)
                        context.add(child.name(), parseValue(child.type(), JoinRest(words, index, words.length), "<" + child.name() + ">"));
                    enter(child);
                    inGreedy = completing;
                    index = words.length;
                    return;
                }

                try {
                    Object value = parseValue(child.type(), word, "<" + child.name() + ">");
                    enter(child);
                    context.add(child.name(), value);
                    ++index;
                    return;
                } catch (CommandSyntaxException e) {
                    if (refused == null)
                        refused = e;
                }
            }

            throw refused != null ? refused : new CommandSyntaxException("Unknown argument ->" + word + "<-");
        }

        // A word read by its type: "Expected an integer for <count>, got ->abc<-"
        private Object parseValue(ArgumentType<?> type, String word, String label) {
            try {
                return type.parse(word, context);
            } catch (CommandSyntaxException e) {
                if (e.getExpected() == null)
                    throw e;

                throw new CommandSyntaxException("Expected " + e.getExpected() + " for " + label + ", got ->" + word + "<-");
            }
        }

        // Completion of the word being typed, once the words before it are read
        private List<String> candidates(String typed) {
            List<String> values = new ArrayList<>();
            if (pending != null) {
                values.addAll(pending.valueType(pendingValue).complete(typed, context));
                return values;
            }
            if (inGreedy) {
                values.addAll(node.type().complete(typed, context));
                return values;
            }

            if (!positionalOnly && typed.startsWith("-")) {
                int equals = typed.indexOf('=');
                if (typed.startsWith("--") && equals > 2) {
                    // --name=value: the values of the option
                    Option option = findActive(typed.substring(2, equals).toLowerCase(Locale.ROOT));
                    if (option != null && option.valueCount() > 0) {
                        String start = typed.substring(0, equals + 1);
                        for (String value : option.valueType(0).complete(typed.substring(equals + 1), context))
                            values.add(start + value);
                    }
                    return values;
                }

                for (Option option : active) {
                    if (option.isVisible(context) && (option.isRepeatable() || !given.contains(option)) && !conflicts(option))
                        values.add("--" + option.getName());
                }
                values.add("--" + Option.HELP);
                return values;
            }

            if (node.isArgument() && node.isVariadic())
                values.addAll(node.type().complete(typed, context));

            for (CommandNode child : node.children()) {
                if (!child.isVisible(context))
                    continue;

                if (child.isLiteral())
                    values.add(child.name());
                else
                    values.addAll(child.type().complete(typed, context));
            }
            return values;
        }

        private boolean conflicts(Option option) {
            for (Option other : given) {
                if (other != option && option.conflictsWith(other))
                    return true;
            }
            return false;
        }

        // End of the line: the branch reached must execute, left out optional arguments are skipped
        private void finish() {
            while (node.handler() == null) {
                CommandNode skipped = null;
                for (CommandNode child : node.children()) {
                    if (child.isArgument() && child.isOptional() && child.isVisible(context)) {
                        skipped = child;
                        break;
                    }
                }

                if (skipped == null)
                    throw new CommandSyntaxException("Incomplete command");
                enter(skipped);
            }
        }
    }
}
