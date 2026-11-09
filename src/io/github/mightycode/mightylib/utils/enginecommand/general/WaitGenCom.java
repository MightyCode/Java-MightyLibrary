package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import java.util.Collection;
import java.util.Collections;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;

import java.util.concurrent.CompletableFuture;

/**
 * Answer after some ticks, or once the current scene is loaded: used by scripts (exec) and tools
 * to wait in game time instead of real time.
 */
public class WaitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "wait";

    private final Commands commands;

    public WaitGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME));
        this.commands = commands;
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        return args.length == 2 ? Collections.singletonList("loaded") : Collections.emptyList();
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return noEnoughArgumentError();

        long timeout;
        try {
            timeout = args.length > 2 ? Long.parseLong(args[2]) : -1;
        } catch (NumberFormatException e) {
            return CommandResult.error("Timeout must be a number of seconds : " + args[2]);
        }

        if (args[1].equalsIgnoreCase("loaded"))
            return waitLoaded(timeout);

        int ticks;
        try {
            ticks = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return unknownArgumentError(args[1]);
        }

        if (ticks <= 0)
            return CommandResult.error("Number of ticks must be positive : " + ticks);

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
        SceneManager sceneManager = ListGeneralCommand.targetScenes();
        CompletableFuture<CommandResult> future = new CompletableFuture<>();
        // A simulated input of this tick is seen by the next tick, whose scene can then ask a change,
        // done at the beginning of the tick after: start checking two ticks later
        long startTick = commands.getTick();
        commands.addTickTask(() -> {
            if (future.isDone())
                return true;

            if (commands.getTick() < startTick + 2 || sceneManager.isSceneChangePending()
                    || sceneManager.isCurrentSceneLoading())
                return false;

            future.complete(CommandResult.ok(sceneManager.getCurrentScene().getClass().getName() + " loaded"));
            return true;
        });

        return CommandResult.later(future, timeout > 0 ? timeout : Commands.COMMAND_TIMEOUT_SECONDS);
    }

    @Override
    public String help() {
        return super.help() +
                "\nwait [ticks] [timeout] : answer after a number of ticks (game time, not real time)" +
                "\nwait loaded [timeout] : answer once the current scene is loaded, checked from two ticks later so that" +
                " a scene change asked just before (by a command or by a scene reacting to a simulated input) is waited" +
                "\ntimeout : seconds before the wait becomes an error (" + Commands.COMMAND_TIMEOUT_SECONDS + " by default)";
    }
}
