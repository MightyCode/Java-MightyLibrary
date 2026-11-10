package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Usage lines, help and JSON generated from the syntax, with what the source sees:
 * "watch mode <poll|service> [--interval <ms>]".
 */
final class CommandHelp {
    // Fixed values shown in place of the name up to this count: <poll|service>
    private static final int MAX_SHOWN_VALUES = 5;

    private CommandHelp() {}

    /**
     * The executable branches from the end of the path on, each one being the nodes from the root.
     * @param visible the nodes the source sees
     */
    static List<List<CommandNode>> Branches(List<CommandNode> path, Predicate<CommandNode> visible) {
        List<List<CommandNode>> branches = new ArrayList<>();
        AddBranches(new ArrayList<>(path), visible, branches);
        return branches;
    }

    private static void AddBranches(List<CommandNode> path, Predicate<CommandNode> visible, List<List<CommandNode>> branches) {
        CommandNode last = path.get(path.size() - 1);
        if (last.handler() != null)
            branches.add(new ArrayList<>(path));

        for (CommandNode child : last.children()) {
            if (!visible.test(child))
                continue;

            path.add(child);
            AddBranches(path, visible, branches);
            path.remove(path.size() - 1);
        }
    }

    // The words of the branch, then its options
    static String Usage(String commandName, List<CommandNode> branch, Predicate<Option> visibleOption) {
        StringBuilder line = new StringBuilder(commandName);
        for (CommandNode node : branch) {
            if (node.isLiteral())
                line.append(' ').append(node.name());
            else if (node.isArgument())
                line.append(' ').append(ArgumentLabel(node));
        }

        for (CommandNode node : branch) {
            for (Option option : node.options()) {
                if (visibleOption.test(option))
                    line.append(' ').append(OptionLabel(option));
            }
        }

        return line.toString();
    }

    // "<name>", "[<name>]", "<names>..."
    static String ArgumentLabel(CommandNode argument) {
        String label = "<" + ValueLabel(argument.name(), argument.type()) + ">";
        if (argument.isVariadic())
            label += "...";

        return argument.isOptional() ? "[" + label + "]" : label;
    }

    // "[--rect <x> <y> <w> <h>]", "[--flag]", "[--tag <tag>]..."
    static String OptionLabel(Option option) {
        return "[" + OptionWords(option) + "]" + (option.isRepeatable() ? "..." : "");
    }

    // "--rect <x> <y> <w> <h>"
    static String OptionWords(Option option) {
        StringBuilder words = new StringBuilder("--").append(option.getName());
        for (int i = 0; i < option.valueCount(); ++i)
            words.append(" <").append(ValueLabel(option.valueName(i), option.valueType(i))).append('>');

        return words.toString();
    }

    // The fixed values when few (poll|service), the name otherwise
    static String ValueLabel(String name, ArgumentType<?> type) {
        List<String> values = type.fixedValues();
        if (!values.isEmpty() && values.size() <= MAX_SHOWN_VALUES)
            return String.join("|", values);

        return name;
    }

    // "Usage :" followed by one line per branch
    static String UsageBlock(String commandName, List<List<CommandNode>> branches, Predicate<Option> visibleOption) {
        StringBuilder block = new StringBuilder("Usage :");
        for (List<CommandNode> branch : branches)
            block.append("\n  ").append(Usage(commandName, branch, visibleOption));

        return block.toString();
    }

    /**
     * What the source sees of the command: its description, a usage line per branch with its description, the
     * arguments and the options with theirs.
     */
    static String HelpText(String id, CommandNode root, CommandContext context) {
        Predicate<Option> visibleOption = option -> option.isVisible(context);
        List<List<CommandNode>> branches = Branches(List.of(root), node -> node.isVisible(context));

        StringBuilder text = new StringBuilder(id);
        if (root.description() != null)
            text.append(" : ").append(root.description());

        text.append("\nUsage :");
        for (List<CommandNode> branch : branches) {
            text.append("\n  ").append(Usage(root.name(), branch, visibleOption));
            String description = branch.get(branch.size() - 1).branchDescription();
            if (description != null)
                text.append(" : ").append(description);
        }

        List<CommandNode> arguments = new ArrayList<>();
        List<Option> options = new ArrayList<>();
        for (List<CommandNode> branch : branches) {
            for (CommandNode node : branch) {
                if (node.isArgument() && node.description() != null && !arguments.contains(node))
                    arguments.add(node);
                for (Option option : node.options()) {
                    if (visibleOption.test(option) && !options.contains(option))
                        options.add(option);
                }
            }
        }

        if (!arguments.isEmpty()) {
            text.append("\nArguments :");
            for (CommandNode argument : arguments) {
                text.append("\n  <").append(argument.name()).append("> : ").append(argument.description())
                        .append(" (").append(argument.type().describe()).append(')');
            }
        }

        text.append("\nOptions :");
        for (Option option : options) {
            text.append("\n  ").append(OptionWords(option));
            if (option.getDescription() != null)
                text.append(" : ").append(option.getDescription());

            List<String> notes = new ArrayList<>();
            if (option.hasDefault())
                notes.add("default " + TextValue(option.getDefault()));
            if (option.isRepeatable())
                notes.add("repeatable");
            if (!notes.isEmpty())
                text.append(" (").append(String.join(", ", notes)).append(')');
        }
        text.append("\n  --").append(Option.HELP).append(" : this help");

        return text.toString();
    }

    /**
     * The visible tree: {id, level, description, branches: [{usage, description, arguments:
     * [{name, type, optional, variadic, values?, description?}], options: [{name, values: [{name, type, values?}],
     * repeatable, default?, description?}]}]}
     */
    static JSONObject HelpJson(String id, ECommandLevel level, CommandNode root, CommandContext context) {
        Predicate<Option> visibleOption = option -> option.isVisible(context);

        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("level", level.name().toLowerCase(Locale.ROOT));
        if (root.description() != null)
            json.put("description", root.description());

        JSONArray branches = new JSONArray();
        for (List<CommandNode> branch : Branches(List.of(root), node -> node.isVisible(context))) {
            JSONObject entry = new JSONObject();
            entry.put("usage", Usage(root.name(), branch, visibleOption));
            String description = branch.get(branch.size() - 1).branchDescription();
            if (description != null)
                entry.put("description", description);

            JSONArray arguments = new JSONArray();
            JSONArray options = new JSONArray();
            for (CommandNode node : branch) {
                if (node.isArgument()) {
                    JSONObject argument = TypeJson(node.name(), node.type());
                    argument.put("optional", node.isOptional());
                    argument.put("variadic", node.isVariadic());
                    if (node.description() != null)
                        argument.put("description", node.description());
                    arguments.put(argument);
                }

                for (Option option : node.options()) {
                    if (visibleOption.test(option))
                        options.put(OptionJson(option));
                }
            }

            entry.put("arguments", arguments);
            entry.put("options", options);
            branches.put(entry);
        }
        json.put("branches", branches);

        return json;
    }

    private static JSONObject OptionJson(Option option) {
        JSONObject json = new JSONObject();
        json.put("name", option.getName());

        JSONArray values = new JSONArray();
        for (int i = 0; i < option.valueCount(); ++i)
            values.put(TypeJson(option.valueName(i), option.valueType(i)));
        json.put("values", values);
        json.put("repeatable", option.isRepeatable());
        if (option.hasDefault())
            json.put("default", JsonValue(option.getDefault()));
        if (option.getDescription() != null)
            json.put("description", option.getDescription());

        return json;
    }

    private static JSONObject TypeJson(String name, ArgumentType<?> type) {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("type", type.describe());
        if (!type.fixedValues().isEmpty())
            json.put("values", new JSONArray(type.fixedValues()));

        return json;
    }

    // A value as written on the line: 1 (not 1.0), poll
    private static String TextValue(Object value) {
        if (value instanceof Double || value instanceof Float)
            return Arguments.Format(((Number) value).doubleValue());
        if (value instanceof Collection) {
            List<String> words = new ArrayList<>();
            for (Object element : (Collection<?>) value)
                words.add(TextValue(element));
            return String.join(" ", words);
        }
        return String.valueOf(JsonValue(value));
    }

    // A value for the JSON: 0.5, poll, a list of them
    private static Object JsonValue(Object value) {
        if (value == null)
            return JSONObject.NULL;
        if (value instanceof Double || value instanceof Float)
            return Double.parseDouble(Arguments.Format(((Number) value).doubleValue()));
        if (value instanceof Number || value instanceof Boolean || value instanceof String)
            return value;
        if (value instanceof Enum)
            return ((Enum<?>) value).name().toLowerCase(Locale.ROOT);
        if (value instanceof Collection) {
            JSONArray array = new JSONArray();
            for (Object element : (Collection<?>) value)
                array.put(JsonValue(element));
            return array;
        }
        return String.valueOf(value);
    }
}
