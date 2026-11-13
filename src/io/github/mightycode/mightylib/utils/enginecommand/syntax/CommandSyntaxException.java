package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.utils.enginecommand.CommandException;

/**
 * A command line that does not follow the declared syntax. Thrown by an ArgumentType for a word it refuses
 * (expected(...): the parser adds the name of the argument and the word), and by the parser itself, with the usage of
 * the branch reached.
 */
public class CommandSyntaxException extends CommandException {
    // What the type expected ("an integer between 1 and 100"), null for a complete message
    private final String expected;

    public CommandSyntaxException(String message) {
        super(message);
        this.expected = null;
    }

    private CommandSyntaxException(String expected, String word) {
        super("Expected " + expected + ", got ->" + word + "<-");
        this.expected = expected;
    }

    /**
     * Refusal of a word by a type: the parser writes "Expected [expected] for [argument], got ->[word]<-".
     * @param expected what the word should have been, with its article: "an integer", "one of : poll, service"
     */
    public static CommandSyntaxException expected(String expected, String word) {
        return new CommandSyntaxException(expected, word);
    }

    // Null for a complete message
    public String getExpected() {
        return expected;
    }
}
