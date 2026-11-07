package io.github.mightycode.mightylib.utils.enginecommand;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Run lines of commands one after the other, on the main thread.
 * Lines run in the same tick until one has a deferred result (wait, screenshot, exec...): the next line waits for it.
 * Blank lines and lines starting with # are ignored. The script stops at the first failed line.
 */
public class CommandScript {
    private final Commands commands;
    private final String name;
    private final List<String> lines;
    private final CompletableFuture<CommandResult> result;

    private final JSONArray report;
    private int nextLine;

    private CompletableFuture<CommandResult> waited;
    private CommandResult waitedCommand;
    private String waitedLine;
    private int waitedLineNumber;

    public CommandScript(Commands commands, String name, List<String> lines) {
        this.commands = commands;
        this.name = name;
        this.lines = lines;
        this.result = new CompletableFuture<>();
        this.report = new JSONArray();
        this.nextLine = 0;
    }

    // Completed when the script ends, successfully or at its first failed line
    public CompletableFuture<CommandResult> getResult() {
        return result;
    }

    /**
     * Run the lines that can run now.
     * @return true when the script is finished (or stopped from outside, by its timeout for example)
     */
    public boolean step() {
        while (!result.isDone()) {
            if (waited != null) {
                if (!waited.isDone())
                    return false;

                CommandResult lineResult = waited.handle((value, exception) ->
                        Commands.ResolveDeferred(waitedCommand, value, exception)).join();
                waited = null;

                if (!record(waitedLineNumber, waitedLine, lineResult))
                    return true;
            }

            if (nextLine >= lines.size()) {
                finish(true, "Script " + name + " : " + report.length() + " command(s) run");
                return true;
            }

            int lineNumber = nextLine + 1;
            String line = lines.get(nextLine++).trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;

            CommandResult lineResult = commands.execute(line);
            if (lineResult.isDeferred()) {
                waited = lineResult.getDeferred();
                waitedCommand = lineResult;
                waitedLine = line;
                waitedLineNumber = lineNumber;
            } else if (!record(lineNumber, line, lineResult)) {
                return true;
            }
        }

        return true;
    }

    // false if the line failed (the script is then finished)
    private boolean record(int lineNumber, String line, CommandResult lineResult) {
        JSONObject entry = new JSONObject();
        entry.put("line", lineNumber);
        entry.put("command", line);
        entry.put("success", lineResult.isSuccess());
        entry.put("message", lineResult.getMessage() == null ? "" : lineResult.getMessage());
        if (lineResult.getData() != null)
            entry.put("data", lineResult.getData());
        report.put(entry);

        if (!lineResult.isSuccess()) {
            finish(false, "Script " + name + " stopped at line " + lineNumber + " : " + line);
            return false;
        }

        return true;
    }

    private void finish(boolean success, String message) {
        JSONObject data = new JSONObject();
        data.put("lines", report);

        result.complete(success ? CommandResult.ok(message, data) : CommandResult.error(message, data));
    }
}
