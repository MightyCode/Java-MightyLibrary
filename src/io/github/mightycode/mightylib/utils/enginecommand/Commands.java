package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.console.LogCapture;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.general.ListGeneralCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registry of the commands.
 * Commands can be submitted from any thread (console, debug server...), they are executed on the main thread
 * at the beginning of the next tick, so the game is never blocked while waiting for a command.
 */
public class Commands {
    // Default time given to a deferred result before it becomes an error
    public static final long COMMAND_TIMEOUT_SECONDS = 30;

    // Only read in debug mode
    private static final ICommandSource STDIN = ICommandSource.Named("stdin", ECommandLevel.DEV);

    private static class CommandRequest {
        private final String line;
        private final ICommandSource source;
        private final CompletableFuture<CommandResult> result;
        // Level of the source when the command runs: the command and its answer are told with it, even when the
        // command changes the level of its source (cheats off)
        private ECommandLevel level;

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

    // The level of the session never goes above the one of the build
    private final ECommandLevel maxLevel;
    private volatile ECommandLevel sessionLevel;

    // Among the views having the same command, the one the commands target (Engine.commandWindow), null if none
    private Function<List<IView>, IView> viewChooser = views -> null;

    // Everything allowed, without the cheats command: the tests, a tool
    public Commands() {
        this(ECommandLevel.DEV, ECommandLevel.DEV, false);
    }

    /**
     * One per engine (Engine.commands), with the general commands.
     * @param maxLevel     the highest level of the build (EngineConfig.setMaxCommandAccess)
     * @param sessionLevel level of the player at the start, bounded by maxLevel (EngineConfig.setCommandAccess)
     * @param cheatCommand adds the command "cheats on|off" when maxLevel allows the cheats
     */
    public Commands(ECommandLevel maxLevel, ECommandLevel sessionLevel, boolean cheatCommand) {
        pendingRequests = new ConcurrentLinkedQueue<>();
        tickTasks = new ArrayList<>();
        listeners = new CopyOnWriteArrayList<>();

        this.maxLevel = maxLevel;
        this.sessionLevel = ECommandLevel.Min(sessionLevel, maxLevel);

        commands = new ArrayList<>();
        ListGeneralCommand.addCommands(registryFor(CommandOwner.ENGINE), this,
                cheatCommand && maxLevel.includes(ECommandLevel.CHEAT));
    }

    /**
     * Main thread. Chooses, among the views having a command with the same identifier (a scene shown in two windows),
     * the one a typed name runs: returns one of the given views, or null to leave the name ambiguous.
     */
    public void setViewChooser(Function<List<IView>, IView> viewChooser) {
        this.viewChooser = viewChooser == null ? views -> null : viewChooser;
    }

    // ---- Levels ----

    public ECommandLevel getMaxLevel() {
        return maxLevel;
    }

    // Level of the player (the in-game console): what it sees and what the console shows
    public ECommandLevel getSessionLevel() {
        return sessionLevel;
    }

    /**
     * Main thread. Change the level of the player during the game (cheats allowed or not), bounded by the level of
     * the build.
     * @return the level set
     */
    public ECommandLevel setSessionLevel(ECommandLevel level) {
        sessionLevel = ECommandLevel.Min(level, maxLevel);
        return sessionLevel;
    }

    // Level of the source of the command being run or completed, DEV for a direct call (api)
    public ECommandLevel getCurrentLevel() {
        return getCurrentSource().level();
    }

    public boolean isVisible(BaseCommand command) {
        return getCurrentLevel().includes(command.getLevel());
    }

    // The commands the current source sees, in registration order
    public List<BaseCommand> getVisibleCommands() {
        List<BaseCommand> visible = new ArrayList<>();
        for (BaseCommand command : commands) {
            if (isVisible(command))
                visible.add(command);
        }
        return visible;
    }

    // Main thread. The code runs as if the source had sent a command (its level, its name)
    private <T> T runAs(ICommandSource source, Supplier<T> code) {
        ICommandSource previous = currentSource;
        currentSource = source;
        try {
            return code.get();
        } finally {
            currentSource = previous;
        }
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
            request.level = request.source.level();
            for (ICommandListener listener : listeners)
                listener.executed(request.source, request.level, request.line);

            CommandResult result = execute(request.line, request.source);

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
            listener.answered(request.source, request.level, request.line, result);

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
     * Completion of the word being typed (main thread), with the commands of the current source.
     * @param beginning the text of the line before the cursor
     */
    public CommandCompletion complete(String beginning) {
        return complete(beginning, getCurrentSource());
    }

    // Completion of the word being typed (main thread), with what the source sees
    public CommandCompletion complete(String beginning, ICommandSource source) {
        return runAs(source, () -> completeNow(beginning));
    }

    private CommandCompletion completeNow(String beginning) {
        CommandLine.Partial partial = CommandLine.SplitPartial(beginning);
        String[] words = partial.getWords();
        String typed = partial.getLast();

        Collection<String> values = new ArrayList<>();
        if (words.length == 1) {
            values.addAll(commandNames(typed));
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
     * Main thread only, as the current source (api outside a command): a command run by another one keeps its level.
     * A deferred result is completed with an error after its timeout, so it always ends.
     */
    public CommandResult execute(String line) {
        return execute(line, getCurrentSource());
    }

    /**
     * Main thread only, as the source (its level, its name): scripts run their lines as the source that started them.
     * Unlike submit, the listeners are not told.
     */
    public CommandResult execute(String line, ICommandSource source) {
        CommandResult result = runAs(source, () -> executeNow(line));

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

        List<BaseCommand> matching = matchCommands(args[0]);
        if (matching.isEmpty())
            return CommandResult.error("No command with its name ->" + args[0] + "<-");
        if (matching.size() > 1 && SameIdentifier(matching))
            return CommandResult.error("The command ->" + matching.get(0).getId()
                    + "<- exists in several windows, target one with -> window <- first");
        if (matching.size() > 1)
            return CommandResult.error("Ambiguous command ->" + args[0] + "<-, write one of : " + identifiers(matching));

        BaseCommand command = matching.get(0);
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

    /**
     * The commands a typed name gives among the ones the current source sees: the one with this identifier
     * (namespace:path), or those with this path. Case insensitive. When several views have the command, only the
     * one of the targeted view is kept (setViewChooser), all of them if none is targeted.
     */
    public List<BaseCommand> matchCommands(String name) {
        List<BaseCommand> result = new ArrayList<>();
        if (name == null)
            return result;

        List<BaseCommand> visible = getVisibleCommands();
        if (name.indexOf(':') >= 0) {
            Identifier id;
            try {
                id = Identifier.parse(name.toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // Not a valid identifier: nothing matches
                return result;
            }

            for (BaseCommand command : visible) {
                if (command.getId().equals(id))
                    result.add(command);
            }
            return keepTargeted(result);
        }

        for (BaseCommand command : visible) {
            if (command.getName().equalsIgnoreCase(name))
                result.add(command);
        }
        return keepTargeted(result);
    }

    // Commands with the same identifier (one per view): the one of the view the chooser gives, all of them otherwise
    private List<BaseCommand> keepTargeted(List<BaseCommand> matching) {
        if (matching.size() < 2)
            return matching;

        Map<Identifier, List<BaseCommand>> byId = new LinkedHashMap<>();
        for (BaseCommand command : matching)
            byId.computeIfAbsent(command.getId(), id -> new ArrayList<>()).add(command);

        List<BaseCommand> result = new ArrayList<>();
        for (List<BaseCommand> same : byId.values()) {
            BaseCommand kept = same.size() == 1 ? same.get(0) : targeted(same);
            if (kept != null)
                result.add(kept);
            else
                result.addAll(same);
        }
        return result;
    }

    private BaseCommand targeted(List<BaseCommand> same) {
        List<IView> views = new ArrayList<>();
        for (BaseCommand command : same)
            views.add(command.getOwner().view());

        IView chosen = viewChooser.apply(Collections.unmodifiableList(views));
        if (chosen == null)
            return null;

        for (BaseCommand command : same) {
            if (command.getOwner().view() == chosen)
                return command;
        }
        return null;
    }

    // Several commands, all with the same identifier: the command of several views, none targeted
    public static boolean SameIdentifier(List<BaseCommand> commands) {
        for (BaseCommand command : commands) {
            if (!command.getId().equals(commands.get(0).getId()))
                return false;
        }
        return true;
    }

    // The only command a typed name gives, null if unknown or ambiguous
    public BaseCommand findCommand(String name) {
        List<BaseCommand> matching = matchCommands(name);
        return matching.size() == 1 ? matching.get(0) : null;
    }

    /**
     * What can be typed to name a command, for the completion: the paths, and the identifiers of the paths several
     * commands share. All the identifiers once the namespace is being typed. Only the commands the source sees.
     */
    public Collection<String> commandNames(String typed) {
        boolean withNamespace = typed != null && typed.indexOf(':') >= 0;
        List<BaseCommand> visible = getVisibleCommands();

        // The identifiers of each path: the same command of several views counts once
        Map<String, Set<Identifier>> pathIds = new HashMap<>();
        for (BaseCommand command : visible)
            pathIds.computeIfAbsent(command.getName(), path -> new HashSet<>()).add(command.getId());

        Set<String> names = new LinkedHashSet<>();
        for (BaseCommand command : visible) {
            if (!withNamespace)
                names.add(command.getName());
            if (withNamespace || pathIds.get(command.getName()).size() > 1)
                names.add(command.getId().toString());
        }

        return names;
    }

    private static String identifiers(List<BaseCommand> commands) {
        Set<String> ids = new TreeSet<>();
        for (BaseCommand command : commands)
            ids.add(command.getId().toString());

        return String.join(", ", ids);
    }

    // In registration order, read only
    public List<BaseCommand> getCommands(){
        return Collections.unmodifiableList(commands);
    }

    /**
     * Main thread. The command belongs to the owner: removed with removeOwnedBy, run under its view.
     * Two views can have a command with the same identifier (a scene shown in two windows): the targeted one runs it
     * (setViewChooser).
     * @throws IllegalArgumentException if a command with the same identifier already exists (outside of another view),
     *                                  or the command is registered
     */
    public void register(BaseCommand command, CommandOwner owner) {
        if (owner == null)
            throw new IllegalArgumentException("A command needs an owner");
        for (BaseCommand existing : commands) {
            if (existing.getId().equals(command.getId()) && !OfOtherViews(existing.getOwner(), owner))
                throw new IllegalArgumentException("A command ->" + command.getId() + "<- already exists");
        }

        command.setRegistration(this, owner);
        commands.add(command);
    }

    private static boolean OfOtherViews(CommandOwner first, CommandOwner second) {
        return first.view() != null && second.view() != null && first.view() != second.view();
    }

    // The registry of an owner: what it registers belongs to it
    public CommandRegistry registryFor(CommandOwner owner) {
        return command -> register(command, owner);
    }

    // Main thread. A scene, a window view... removes its commands when it is left: they can be registered again
    public void removeOwnedBy(CommandOwner owner) {
        commands.removeIf(command -> {
            if (!command.getOwner().equals(owner))
                return false;

            command.clearRegistration();
            return true;
        });
    }
}
