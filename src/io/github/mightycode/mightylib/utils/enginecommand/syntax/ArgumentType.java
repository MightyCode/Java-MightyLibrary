package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Type of an argument or of an option value: reads a word into a value and proposes the words it accepts.
 * Arguments gives the usual ones; a game or a mod writes its own (main thread, like the commands).
 */
public interface ArgumentType<T> {
    /**
     * @param context the values read before this word (an argument can depend on a previous one)
     * @throws CommandSyntaxException when the word is refused, CommandSyntaxException.expected(...) for the usual
     *                                "Expected ... for <name>, got ->word<-"
     */
    T parse(String word, CommandContext context);

    /**
     * Words to propose for the word being typed (Tab in the console). Unfiltered values are fine: they are filtered
     * by what is typed.
     */
    default Collection<String> complete(String start, CommandContext context) {
        return Collections.emptyList();
    }

    // Short name of the type, for the help and the JSON: "integer 1..100", "path"
    String describe();

    // The only values accepted when they are fixed (shown in the usage and the JSON), empty otherwise
    default List<String> fixedValues() {
        return Collections.emptyList();
    }

    /**
     * True when the argument takes the rest of the line, options and "--" included, as one value (Arguments.text):
     * it can only be the last argument of a branch.
     */
    default boolean isGreedy() {
        return false;
    }
}
