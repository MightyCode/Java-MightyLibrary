package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.resources.ResourceWatcher;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.ResourceSelection;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

public class WatchGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "watch";

    public WatchGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public CommandResult execute(String[] args) {
        ResourceWatcher watcher = Resources.getInstance().watcher();

        if (args.length < 2 || args[1].equalsIgnoreCase("status"))
            return CommandResult.ok(watcher.status());

        if (args[1].equalsIgnoreCase("off")) {
            watcher.stop();
            return CommandResult.ok("Resource watcher off");
        }

        if (args[1].equalsIgnoreCase("mode"))
            return setMode(watcher, args);

        ResourceSelection selection;
        try {
            selection = ResourceSelection.parse(args, 1);
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }

        watcher.watch(selection);
        return CommandResult.ok("Watching " + selection + "\n" + watcher.status());
    }

    private CommandResult setMode(ResourceWatcher watcher, String[] args) {
        if (args.length < 3)
            return noEnoughArgumentError();

        ResourceWatcher.Mode mode;
        if (args[2].equalsIgnoreCase("poll"))
            mode = ResourceWatcher.Mode.POLLING;
        else if (args[2].equalsIgnoreCase("service"))
            mode = ResourceWatcher.Mode.SERVICE;
        else
            return unknownArgumentError(args[2]);

        long interval = ResourceWatcher.DEFAULT_INTERVAL_MS;
        if (args.length > 3) {
            try {
                interval = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                return unknownArgumentError(args[3]);
            }
        }

        watcher.configure(mode, interval);
        return CommandResult.ok(watcher.status());
    }

    public String help() {
        return super.help() +
                "\nReload a resource when its file changes. Off by default, polling the files unless told otherwise." +
                "\n1. watch [status] : state of the watcher" +
                "\n2. watch all [name1,name2] [re:regex] : watch every loaded resource (starts the watcher)" +
                "\n3. watch [type] [name1,name2] [re:regex] : watch a type, only some names and/or names matching the" +
                " regex (a name in the list OR matching the regex), for example watch TextureData grass,sand re:tile.*" +
                "\n4. watch mode poll|service [interval ms] : compare the files each interval (default, works everywhere)" +
                " or ask the system (cheaper, not recursive, not on every file system)" +
                "\n5. watch off : stop watching and forget the selections" +
                "\n6. watch help : display tips about the command";
    }
}
