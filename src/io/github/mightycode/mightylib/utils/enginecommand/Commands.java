package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.utils.enginecommand.general.ListGeneralCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * Registry of the commands.
 * Commands can be submitted from any thread (console, debug server...), they are executed on the main thread
 * at the beginning of the next tick, so the game is never blocked while waiting for a command.
 */
public class Commands {
    // Default time given to a deferred result before it becomes an error
    public static final long COMMAND_TIMEOUT_SECONDS = 30;

    private static class CommandRequest {
        private final String line;
        private final CompletableFuture<CommandResult> result;

        private CommandRequest(String line) {
            this.line = line;
            this.result = new CompletableFuture<>();
        }
    }

    private final ArrayList<BaseCommand> commands;
    private final int endGeneralCommand;

    private final ConcurrentLinkedQueue<CommandRequest> pendingRequests;
    private final ArrayList<BooleanSupplier> tickTasks;
    private static final int MAX_TASK_PASSES = 16;
    private long tick;
    private Thread consoleThread;

    public Commands(SceneManager sceneManager) {
        pendingRequests = new ConcurrentLinkedQueue<>();
        tickTasks = new ArrayList<>();

        // Add commands -> General
        commands = new ArrayList<>();
        ListGeneralCommand.addCommands(this, sceneManager);
        // Size
        endGeneralCommand = commands.size();
    }

    /**
     * Can be called from any thread.
     * @return Completed on the main thread once the command is executed
     */
    public CompletableFuture<CommandResult> submit(String line) {
        CommandRequest request = new CommandRequest(line);
        pendingRequests.add(request);

        return request.result;
    }

    /**
     * Run a task at each tick (main thread, before the submitted commands) until it returns true.
     * Used by commands whose result needs several ticks (wait, exec).
     */
    public void addTickTask(BooleanSupplier task) {
        tickTasks.add(task);
    }

    // Number of calls to processPendingCommands, one per tick: tasks compare it instead of counting their calls
    public long getTick() {
        return tick;
    }

    // Main thread only
    public void processPendingCommands() {
        ++tick;

        // A finished task can unblock another one (a script waiting for a wait): run them again in the same tick.
        // Tasks must depend on getTick(), not on the number of times they are run.
        boolean someFinished = true;
        for (int pass = 0; someFinished && pass < MAX_TASK_PASSES; ++pass) {
            someFinished = false;

            ArrayList<BooleanSupplier> running = new ArrayList<>(tickTasks);
            for (BooleanSupplier task : running) {
                boolean finished;
                try {
                    finished = task.getAsBoolean();
                } catch (RuntimeException e) {
                    System.err.println("--Command task failed : " + e);
                    finished = true;
                }

                if (finished) {
                    tickTasks.remove(task);
                    someFinished = true;
                }
            }
        }

        CommandRequest request;
        while ((request = pendingRequests.poll()) != null) {
            CommandResult result = execute(request.line);

            if (result.isDeferred()) {
                CompletableFuture<CommandResult> target = request.result;
                result.getDeferred().whenComplete((deferredResult, exception) ->
                        target.complete(ResolveDeferred(result, deferredResult, exception)));
            } else {
                request.result.complete(result);
            }
        }
    }

    // Final result of a deferred command once its future is completed
    public static CommandResult ResolveDeferred(CommandResult deferredCommand, CommandResult value, Throwable exception) {
        if (exception == null)
            return value == null ? CommandResult.error("Command answered nothing") : value;

        Throwable cause = exception instanceof CompletionException && exception.getCause() != null ? exception.getCause() : exception;
        if (cause instanceof TimeoutException)
            return CommandResult.error("No answer after " + deferredCommand.getTimeoutSeconds() + " seconds");

        return CommandResult.error(cause.toString());
    }

    /**
     * Main thread only.
     * A deferred result is completed with an error after its timeout, so it always ends.
     */
    public CommandResult execute(String line) {
        CommandResult result = executeNow(line);

        if (result.isDeferred())
            result.getDeferred().orTimeout(result.getTimeoutSeconds(), TimeUnit.SECONDS);

        return result;
    }

    private CommandResult executeNow(String line) {
        String[] args;
        try {
            args = CommandLine.Split(line);
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }

        if (args.length == 0)
            return CommandResult.error("Empty command");

        BaseCommand command = findCommand(args[0]);

        if (command == null)
            return CommandResult.error("No command with its name ->" + args[0] + "<-");

        if (command.checkArgumentHelp(args))
            return CommandResult.ok(command.help());

        try {
            return command.execute(args);
        } catch (RuntimeException e) {
            return CommandResult.error("Command " + command.getName() + " failed : " + e);
        }
    }

    /**
     * Read commands typed in the console from a separate thread, without blocking the game.
     */
    public void startConsole() {
        if (consoleThread != null)
            return;

        consoleThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty())
                        continue;

                    CommandResult result;
                    try {
                        // Always completed once executed: deferred results have their own timeout
                        result = submit(line).get();
                    } catch (ExecutionException e) {
                        result = CommandResult.error(e.getCause().toString());
                    }

                    if (result.isSuccess())
                        System.out.println(result);
                    else
                        System.err.println(result);
                }
            } catch (IOException e) {
                System.err.println("Console commands stopped : " + e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "MightyLibrary-Console");

        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    public BaseCommand findCommand(String name) {
        for (BaseCommand command : commands) {
            if (command.isCommand(name))
                return command;
        }

        return null;
    }

    public ArrayList<BaseCommand> getCommands(){
        return commands;
    }


    /**
     * Commands added after the general ones belong to the current scene and are removed when it changes.
     * @throws IllegalArgumentException if a command with the same name already exists
     */
    public void addCommand(BaseCommand command){
        if (findCommand(command.getName()) != null)
            throw new IllegalArgumentException("A command named ->" + command.getName() + "<- already exists");

        commands.add(command);
    }

    public boolean isSceneCommand(BaseCommand command) {
        return commands.indexOf(command) >= endGeneralCommand;
    }

    public void removeSceneCommands(){
        if (commands.size() > endGeneralCommand) {
            commands.subList(endGeneralCommand, commands.size()).clear();
        }
    }
}
