package io.github.mightycode.mightylib.utils.enginecommand;

import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * Result of a command: returned to the console, the debug server or any other caller instead of being printed.
 */
public class CommandResult {
    private final boolean success;
    private final String message;
    private final JSONObject data;

    // Set when the result is only known later (for example a screenshot taken at the end of the next frame)
    private final CompletableFuture<CommandResult> deferred;

    private CommandResult(boolean success, String message, JSONObject data, CompletableFuture<CommandResult> deferred) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.deferred = deferred;
    }

    public static CommandResult ok(String message) {
        return new CommandResult(true, message, null, null);
    }

    public static CommandResult ok(String message, JSONObject data) {
        return new CommandResult(true, message, data, null);
    }

    public static CommandResult error(String message) {
        return new CommandResult(false, message, null, null);
    }

    public static CommandResult later(CompletableFuture<CommandResult> deferred) {
        return new CommandResult(true, null, null, deferred);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public JSONObject getData() {
        return data;
    }

    public boolean isDeferred() {
        return deferred != null;
    }

    public CompletableFuture<CommandResult> getDeferred() {
        return deferred;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("success", success);
        json.put("message", message == null ? "" : message);

        if (data != null)
            json.put("data", data);

        return json;
    }

    @Override
    public String toString() {
        String text = message == null ? "" : message;

        if (data != null)
            text += (text.isEmpty() ? "" : "\n") + data.toString(2);

        return text;
    }
}
