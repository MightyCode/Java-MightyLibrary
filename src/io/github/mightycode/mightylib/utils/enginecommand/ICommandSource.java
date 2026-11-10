package io.github.mightycode.mightylib.utils.enginecommand;

import java.util.function.Supplier;

/**
 * Who sends a command: the in-game console, the standard input, a client of the debug server...
 * Shown in front of the command in the console log. Its level decides the commands it sees.
 */
public interface ICommandSource {
    ICommandSource API = Named("api", ECommandLevel.DEV);

    String name();

    // Can change (the in-game console follows the level of the session)
    ECommandLevel level();

    /**
     * Asked by "console follow": send every new line of the console log to this source.
     * @return false when the source can't receive them
     */
    default boolean setFollowLog(boolean follow) {
        return false;
    }

    static ICommandSource Named(String name, ECommandLevel level) {
        return Named(name, () -> level);
    }

    static ICommandSource Named(String name, Supplier<ECommandLevel> level) {
        return new ICommandSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public ECommandLevel level() {
                return level.get();
            }
        };
    }
}
