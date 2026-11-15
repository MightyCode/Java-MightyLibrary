package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.enginecommand.CommandException;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * When a branch or an option of a command can be used (CommandNode.requires, Option.requires), asked at each
 * execution and completion (main thread):
 * - hidden: false, the branch does not exist for the source (no completion, no help, the error of an unknown word),
 *   like a level above the source; on the root, the whole command does not exist;
 * - check: the branch is shown, refused with the message when false.
 * The predicate reads the level and the source of the context, and the state of the game, not the values of the line
 * (the help asks it without a line).
 */
public final class CommandCondition {
    public static final String SCENE_STILL_LOADING = "Scene still loading";

    private final Predicate<CommandContext> predicate;
    // Null for a hidden condition
    private final String message;

    private CommandCondition(Predicate<CommandContext> predicate, String message) {
        if (predicate == null)
            throw new IllegalArgumentException("A condition needs a predicate");

        this.predicate = predicate;
        this.message = message;
    }

    // Silent: false, the branch does not exist
    public static CommandCondition hidden(Predicate<CommandContext> predicate) {
        return new CommandCondition(predicate, null);
    }

    // Shown: false, the branch is refused with the message
    public static CommandCondition check(Predicate<CommandContext> predicate, String message) {
        if (message == null || message.isEmpty())
            throw new IllegalArgumentException("A checked condition needs a message");

        return new CommandCondition(predicate, message);
    }

    /**
     * "Scene still loading" until the scene is launched: its cameras, renderers and objects only exist from launch()
     * on, and a command of a scene can be typed while it loads.
     */
    public static CommandCondition launched(Scene scene) {
        return check(context -> scene != null && scene.isLaunched(), SCENE_STILL_LOADING);
    }

    // The scene asked at each use (the current scene of the targeted window), "Scene still loading" until launched
    public static CommandCondition launched(Supplier<? extends Scene> scene) {
        return check(context -> {
            Scene current = scene.get();
            return current != null && current.isLaunched();
        }, SCENE_STILL_LOADING);
    }

    public boolean isHidden() {
        return message == null;
    }

    // Null for a hidden condition
    public String getMessage() {
        return message;
    }

    public boolean test(CommandContext context) {
        return predicate.test(context);
    }

    // The level of the source reaches the level (null: no level of its own) and the hidden conditions are true
    static boolean Visible(ECommandLevel level, List<CommandCondition> conditions, CommandContext context) {
        if (level != null && !context.currentLevel().includes(level))
            return false;

        for (CommandCondition condition : conditions) {
            if (condition.isHidden() && !condition.test(context))
                return false;
        }
        return true;
    }

    // Stops with the message of the first checked condition that is false
    static void Check(List<CommandCondition> conditions, CommandContext context) {
        for (CommandCondition condition : conditions) {
            if (!condition.isHidden() && !condition.test(context))
                throw new CommandException(condition.getMessage());
        }
    }
}
