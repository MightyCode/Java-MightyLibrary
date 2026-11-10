package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandCompletion;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import org.json.JSONArray;
import org.json.JSONObject;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.enumOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.commandLine;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

/**
 * "complete --as user \"stats \"": what Tab gives in the console for the beginning of a line, to check without the
 * player what he is proposed. The level can only go down: Min(asked, level of the source).
 */
public class CompleteGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "complete";
    private final Commands commands;

    public CompleteGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "What the completion (Tab) proposes for the beginning of a command line");
        this.commands = commands;
    }

    @Override
    protected void syntax(CommandNode root) {
        root.option(Option.of("as", "Level of the completion, never above the one of the source")
                        .value("level", enumOf(ECommandLevel.class)))
                .then(argument("line", commandLine(), "Beginning of the line, between quotes for a final space : \"stats \"")
                        .optional()
                        .executes("The words Tab proposes for the last word of the line", context -> {
                            ICommandSource source = context.has("as")
                                    ? ICommandSource.Lowered(context.source(), context.get("as")) : context.source();
                            return answer(commands.complete(context.getOr("line", ""), source));
                        }));
    }

    // {start, typed, candidates, replacement}
    private static CommandResult answer(CommandCompletion completion) {
        JSONObject data = new JSONObject();
        data.put("start", completion.getStart());
        data.put("typed", completion.getTyped());
        data.put("candidates", new JSONArray(completion.getCandidates()));
        String replacement = completion.replacement();
        data.put("replacement", replacement == null ? JSONObject.NULL : replacement);

        String message = completion.isEmpty() ? "No completion" : String.join(" ", completion.getCandidates());
        return CommandResult.ok(message, data);
    }
}
