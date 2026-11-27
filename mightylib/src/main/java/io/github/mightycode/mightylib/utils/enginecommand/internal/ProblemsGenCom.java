package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylib.utils.LogEntry;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import io.github.mightycode.mightylib.utils.internal.ProblemLog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.enumOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

// The errors and warnings sent through the Logger channel since the start: counts and last ones
public final class ProblemsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "problems";
    public static final int DEFAULT_LAST = 20;

    private final Supplier<ProblemLog> log;

    public ProblemsGenCom() {
        this(() -> Views.engine().problems());
    }

    // The log given (tests)
    public ProblemsGenCom(Supplier<ProblemLog> log) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The errors and warnings of the engine since the start: counts by level and category, last ones");
        this.log = log;
    }

    @Override
    protected void syntax(CommandNode root) {
        root.option(Option.of("level", "Only the last problems of this level").value("level", enumOf(ELogLevel.class)))
                .option(Option.of("last", "How many last problems (" + DEFAULT_LAST + " by default)")
                        .value("count", integer(1, ProblemLog.DEFAULT_CAPACITY)))
                .executes("The counts and the last problems (their stack traces in the data)",
                        context -> list(context.getOr("level", null), context.getOr("last", DEFAULT_LAST)))
                .then(literal("clear", "Forget the problems received: counts and last ones")
                        .executes(context -> {
                            log.get().clear();
                            return CommandResult.ok("Problems cleared");
                        }));
    }

    private CommandResult list(ELogLevel level, int count) {
        ProblemLog problems = log.get();
        long errors = problems.count(ELogLevel.ERROR);
        long warnings = problems.count(ELogLevel.WARNING);

        StringBuilder message = new StringBuilder();
        message.append(errors).append(" error(s), ").append(warnings).append(" warning(s)");

        JSONObject categories = new JSONObject();
        for (Map.Entry<String, Map<ELogLevel, Long>> category : problems.categories().entrySet()) {
            JSONObject counts = new JSONObject();
            message.append("\n  ").append(category.getKey()).append(":");
            // The most severe first
            ELogLevel[] levels = ELogLevel.values();
            for (int i = levels.length - 1; i >= 0; --i) {
                Long levelCount = category.getValue().get(levels[i]);
                if (levelCount == null)
                    continue;
                counts.put(Name(levels[i]), levelCount);
                message.append(" ").append(levelCount).append(" ").append(Name(levels[i]));
            }
            categories.put(category.getKey(), counts);
        }

        List<LogEntry> last = problems.last(level, count);
        JSONArray entries = new JSONArray();
        if (!last.isEmpty())
            message.append("\nLast ").append(last.size()).append(level == null ? "" : " " + Name(level)).append(":");
        for (LogEntry entry : last) {
            message.append("\n  ").append(entry.level()).append(" [").append(entry.category()).append("] ")
                    .append(FirstLine(entry.message()));
            if (entry.error() != null)
                message.append(": ").append(entry.error());
            entries.put(Json(entry));
        }

        JSONObject data = new JSONObject();
        data.put("errors", errors);
        data.put("warnings", warnings);
        data.put("categories", categories);
        data.put("capacity", problems.capacity());
        data.put("kept", problems.size());
        data.put("last", entries);
        return CommandResult.ok(message.toString(), data);
    }

    private static JSONObject Json(LogEntry entry) {
        JSONObject json = new JSONObject();
        json.put("level", Name(entry.level()));
        json.put("category", entry.category());
        json.put("message", entry.message());
        json.put("thread", entry.thread());
        json.put("time", entry.time().toString());
        if (entry.error() != null) {
            json.put("exception", entry.error().toString());
            StringWriter stack = new StringWriter();
            entry.error().printStackTrace(new PrintWriter(stack));
            json.put("stackTrace", stack.toString());
        }
        return json;
    }

    private static String Name(ELogLevel level) {
        return level.name().toLowerCase(Locale.ROOT);
    }

    private static String FirstLine(String message) {
        int end = message.indexOf('\n');
        return end < 0 ? message : message.substring(0, end) + " (...)";
    }
}
