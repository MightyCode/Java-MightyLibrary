package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Usage lines generated from the syntax: "watch mode <poll|service> [--interval <ms>]".
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
}
