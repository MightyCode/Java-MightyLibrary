package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.ResourceArguments;
import io.github.mightycode.mightylib.resources.ResourceWatcher;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.choice;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.longValue;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class WatchGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "watch";

    public WatchGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Reload a resource when its file changes. Off by default, polling the files unless told otherwise");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("State of the watcher", context -> status())
                .then(literal("status", "State of the watcher").executes(context -> status()))
                .then(literal("off", "Stop watching and forget the selections").executes(context -> {
                    Views.engine().resources().watcher().stop();
                    return CommandResult.ok("Resource watcher off");
                }))
                .then(literal("mode", "Compare the files each interval (poll, works everywhere) or ask the system"
                        + " (service, cheaper, not recursive, not on every file system)")
                        .then(argument("mode", choice("poll", "service"), "How the changes are detected")
                                .option(Option.of("interval", "Milliseconds between two comparisons (poll)")
                                        .value("ms", longValue(1, Long.MAX_VALUE))
                                        .defaultValue(ResourceWatcher.DEFAULT_INTERVAL_MS))
                                .executes(context -> {
                                    ResourceWatcher watcher = Views.engine().resources().watcher();
                                    ResourceWatcher.Mode mode = context.<String>get("mode").equals("poll")
                                            ? ResourceWatcher.Mode.POLLING : ResourceWatcher.Mode.SERVICE;

                                    watcher.configure(mode, context.<Long>get("interval"));
                                    return CommandResult.ok(watcher.status());
                                })))
                .then(ResourceArguments.selection(() -> Views.engine().resources(),
                        "all or the name of a resource type: watch it (starts the watcher)", selection -> {
                            ResourceWatcher watcher = Views.engine().resources().watcher();
                            watcher.watch(selection);
                            return CommandResult.ok("Watching " + selection + "\n" + watcher.status());
                        }));
    }

    private static CommandResult status() {
        return CommandResult.ok(Views.engine().resources().watcher().status());
    }
}
