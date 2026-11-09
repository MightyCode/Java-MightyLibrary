package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.console.LogCapture;
import io.github.mightycode.mightylib.utils.enginecommand.general.ListGeneralCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private static final ICommandSource STDIN = ICommandSource.Named("stdin");

    private static class CommandRequest {
        private final String line;
        private final ICommandSource source;
        private final CompletableFuture<CommandResult> result;

        private CommandRequest(String line, ICommandSource source) {
            this.line = line;
            this.source = source;
            this.result = new CompletableFuture<>();
        }
    }

    private final ArrayList<BaseCommand> commands;

    private final ConcurrentLinkedQueue<CommandRequest> pendingRequests;
    private final ArrayList<BooleanSupplier> tickTasks;
    private static final int MAX_TASK_PASSES = 16;
    private long tick;
    private Thread consoleThread;

    private final CopyOnWriteArrayList<ICommandListener> listeners;
    // Source of the submitted command being executed (main thread), null outside
    private ICommandSource currentSource;

    // One per engine (Engine.commands), with the general commands
    public Commands() {
        pendingRequests = new ConcurrentLinkedQueue<>();
        tickTasks = new ArrayList<>();
        listeners = new CopyOnWriteArrayList<>();

        commands = new ArrayList<>();
        ListGeneralCommand.addCommands(registryFor(CommandOwner.ENGINE), this);
    }

    /**
     * Can be called from any thread.
     * @return Completed on the main thread once the command is executed
     */
    public CompletableFuture<CommandResult> submit(String line) {
        return submit(line, ICommandSource.API);
    }

    /**
     * Can be called from any thread. The listeners are told of the command and of its result.
     * @return Completed on the main thread once the command is executed
     */
    public CompletableFuture<CommandResult> submit(String line, ICommandSource source) {
        CommandRequest request = new CommandRequest(line, source == null ? ICommandSource.API : source);
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
            for (ICommandListener listener : listeners)
                listener.executed(request.source, request.line);

            CommandResult result;
            currentSource = request.source;
            try {
                result = execute(request.line);
            } finally {
                currentSource = null;
            }

            CommandRequest answered = request;
            if (result.isDeferred()) {
                result.getDeferred().whenComplete((deferredResult, exception) ->
                        answer(answered, ResolveDeferred(result, deferredResult, exception)));
            } else {
                answer(answered, result);
            }
        }
    }

    private void answer(CommandRequest request, CommandResult result) {
        for (ICommandListener listener : listeners)
            listener.answered(request.source, request.line, result);

        request.result.complete(result);
    }

    public void addListener(ICommandListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ICommandListener listener) {
        listeners.remove(listener);
    }

    /** Source of the submitted command being executed, ICommandSource.API when executed directly (main thread). */
    public ICommandSource getCurrentSource() {
        return currentSource == null ? ICommandSource.API : currentSource;
    }

    /**
     * Completion of the word being typed (main thread).
     * @param beginning the text of the line before the cursor
     */
    public CommandCompletion complete(String beginning) {
        CommandLine.Partial partial = CommandLine.SplitPartial(beginning);
        String[] words = partial.getWords();
        String typed = partial.getLast();

        Collection<String> values = new ArrayList<>();
        if (words.length == 1) {
            for (BaseCommand command : commands)
                values.add(command.getName());
        } else {
            BaseCommand command = findCommand(words[0]);
            if (command != null) {
                try {
                    values.addAll(command.getOwner().run(() -> command.completeArgument(words)));
                } catch (RuntimeException e) {
                    // A completion never stops the typing: no value
                }

                if (words.length == 2)
                    values.add("help");
            }
        }

        return new CommandCompletion(partial.getLastStart(), typed, CommandCompletion.Filter(typed, values));
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
            return command.getOwner().run(() -> command.execute(args));
        } catch (CommandException e) {
            return CommandResult.error(e.getMessage());
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
                        result = submit(line, STDIN).get();
                    } catch (ExecutionException e) {
                        result = CommandResult.error(e.getCause().toString());
                    }

                    // Already in the console log: written for the terminal only
                    if (result.isSuccess())
                        LogCapture.rawOut().println(result);
                    else
                        LogCapture.rawErr().println(result);
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

    // In registration order, read only
    public List<BaseCommand> getCommands(){
        return Collections.unmodifiableList(commands);
    }

    /**
     * Main thread. The command belongs to the owner: removed with removeOwnedBy, run under its view.
     * @throws IllegalArgumentException if a command with the same name already exists, or the command is registered
     */
    public void register(BaseCommand command, CommandOwner owner) {
        if (owner == null)
            throw new IllegalArgumentException("A command needs an owner");
        if (findCommand(command.getName()) != null)
            throw new IllegalArgumentException("A command named ->" + command.getName() + "<- already exists");

        command.setOwner(owner);
        commands.add(command);
    }

    // The registry of an owner: what it registers belongs to it
    public CommandRegistry registryFor(CommandOwner owner) {
        return command -> register(command, owner);
    }

    // Main thread. A scene, a window view... removes its commands when it is left
    public void removeOwnedBy(CommandOwner owner) {
        commands.removeIf(command -> command.getOwner().equals(owner));
    }
}
