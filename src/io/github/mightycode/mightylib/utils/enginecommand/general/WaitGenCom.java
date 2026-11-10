package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;

import java.util.concurrent.CompletableFuture;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.longValue;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

/**
 * Answer after some ticks, or once the current scene is loaded: used by scripts (exec) and tools
 * to wait in game time instead of real time.
 */
public class WaitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "wait";

    private final Commands commands;

    public WaitGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Answer after some ticks (game time, not real time) or once the current scene is loaded");
        this.commands = commands;
    }

    @Override
    protected void syntax(CommandNode root) {
        root.option(Option.of("timeout", "Seconds before the wait becomes an error (" + Commands.COMMAND_TIMEOUT_SECONDS
                        + " by default)").value("seconds", longValue(1, Long.MAX_VALUE)))
                .then(literal("loaded", "Answer once the current scene is loaded, checked from two ticks later so that"
                        + " a scene change asked just before (by a command or by a scene reacting to a simulated input) is waited")
                        .executes(context -> waitLoaded(context.getOr("timeout", -1L))))
                .then(argument("ticks", integer(1, Integer.MAX_VALUE), "Number of ticks to wait")
                        .executes("Answer after a number of ticks (game time, not real time)",
                                context -> waitTicks(context.get("ticks"), context.getOr("timeout", -1L))));
    }

    private CommandResult waitTicks(int ticks, long timeout) {
        CompletableFuture<CommandResult> future = new CompletableFuture<>();
        long endTick = commands.getTick() + ticks;
        // The tasks run at the beginning of each tick, after the tick counter is increased
        commands.addTickTask(() -> {
            if (future.isDone())
                return true;

            if (commands.getTick() < endTick)
                return false;

            future.complete(CommandResult.ok(ticks + " tick(s) waited"));
            return true;
        });

        // Ticks are faster than one per second
        return CommandResult.later(future, timeout > 0 ? timeout : Math.max(Commands.COMMAND_TIMEOUT_SECONDS, ticks));
    }

    private CommandResult waitLoaded(long timeout) {
        // Of the window targeted when the wait starts
        Engine engine = Views.engine();
        Context context = engine.commandContext();
        SceneManager sceneManager = context.getSceneManager();
        CompletableFuture<CommandResult> future = new CompletableFuture<>();
        // A simulated input of this tick is seen by the next tick, whose scene can then ask a change,
        // done at the beginning of the tick after: start checking two ticks later
        long startTick = commands.getTick();
        commands.addTickTask(() -> {
            if (future.isDone())
                return true;

            // Its scenes are unloaded: they will never be loaded
            if (!engine.contexts().getContexts().contains(context)) {
                future.complete(CommandResult.error("The window " + context.getName() + " closed during the wait"));
                return true;
            }

            if (commands.getTick() < startTick + 2 || sceneManager.isSceneChangePending()
                    || sceneManager.isCurrentSceneLoading())
                return false;

            future.complete(CommandResult.ok(sceneManager.getCurrentScene().getClass().getName() + " loaded"));
            return true;
        });

        return CommandResult.later(future, timeout > 0 ? timeout : Commands.COMMAND_TIMEOUT_SECONDS);
    }
}
