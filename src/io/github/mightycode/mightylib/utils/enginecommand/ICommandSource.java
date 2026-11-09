package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Who sends a command: the in-game console, the standard input, a client of the debug server...
 * Shown in front of the command in the console log.
 */
public interface ICommandSource {
    ICommandSource API = Named("api");

    String name();

    /**
     * Asked by "console follow": send every new line of the console log to this source.
     * @return false when the source can't receive them
     */
    default boolean setFollowLog(boolean follow) {
        return false;
    }

    static ICommandSource Named(String name) {
        return () -> name;
    }
}
