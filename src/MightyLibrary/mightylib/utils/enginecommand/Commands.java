package MightyLibrary.mightylib.utils.enginecommand;

import MightyLibrary.mightylib.scenes.SceneManager;
import MightyLibrary.mightylib.utils.enginecommand.general.ListGeneralCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * Registry of the commands.
 * Commands can be submitted from any thread (console, debug server...), they are executed on the main thread
 * at the beginning of the next tick, so the game is never blocked while waiting for a command.
 */
public class Commands {
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
    private Thread consoleThread;

    public Commands(SceneManager sceneManager) {
        pendingRequests = new ConcurrentLinkedQueue<>();

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

    // Main thread only
    public void processPendingCommands() {
        CommandRequest request;
        while ((request = pendingRequests.poll()) != null) {
            CommandResult result = execute(request.line);

            if (result.isDeferred()) {
                CompletableFuture<CommandResult> target = request.result;
                result.getDeferred().whenComplete((deferredResult, exception) -> {
                    if (exception != null)
                        target.complete(CommandResult.error(exception.toString()));
                    else
                        target.complete(deferredResult);
                });
            } else {
                request.result.complete(result);
            }
        }
    }

    // Main thread only
    public CommandResult execute(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty())
            return CommandResult.error("Empty command");

        String[] args = trimmed.split("\\s+");
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

                    CommandResult result = submit(line).get();

                    if (result.isSuccess())
                        System.out.println(result);
                    else
                        System.err.println(result);
                }
            } catch (IOException | ExecutionException e) {
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


    public void addCommand(BaseCommand command){
        commands.add(command);
    }


    public void removeSpecificCommand(){
        if (commands.size() > endGeneralCommand) {
            commands.subList(endGeneralCommand, commands.size()).clear();
        }
    }
}
