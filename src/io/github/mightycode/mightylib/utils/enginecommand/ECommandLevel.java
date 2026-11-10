package io.github.mightycode.mightylib.utils.enginecommand;

import java.util.Locale;

/**
 * Who a command is for, each level including the ones below. A command above the level of its source does not exist
 * for it. Not a security: in a local game, the player owns the program; the levels keep the tools out of sight.
 */
public enum ECommandLevel {
    // Commands of the player: help, console, options...
    USER,
    // Cheats of the player, when the game allows them
    CHEAT,
    // Tools of the developer and of the modder
    DEV;

    public boolean includes(ECommandLevel level) {
        return ordinal() >= level.ordinal();
    }

    public static ECommandLevel Min(ECommandLevel first, ECommandLevel second) {
        return first.ordinal() <= second.ordinal() ? first : second;
    }

    /** "user", "cheat" or "dev", case insensitive. @throws IllegalArgumentException otherwise */
    public static ECommandLevel Parse(String text) {
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown command level ->" + text + "<- (user, cheat, dev)");
        }
    }
}
