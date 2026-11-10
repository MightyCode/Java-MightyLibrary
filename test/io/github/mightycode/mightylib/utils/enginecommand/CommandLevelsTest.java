package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.console.CommandEcho;
import io.github.mightycode.mightylib.console.ConsoleLog;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ICommandHandler;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;
import static org.junit.jupiter.api.Assertions.*;

class CommandLevelsTest {
    private static final ICommandSource PLAYER = ICommandSource.Named("game", ECommandLevel.USER);
    private static final ICommandSource CHEATER = ICommandSource.Named("game", ECommandLevel.CHEAT);
    private static final ICommandSource DEVELOPER = ICommandSource.Named("server", ECommandLevel.DEV);

    // public, other, and secret reserved to the developer
    private static class LevelCommand extends BaseCommand {
        LevelCommand(String name, ECommandLevel level) {
            super(TestCommands.Id(name), level, "Answers the level of its source");
        }

        @Override
        protected void syntax(CommandNode root) {
            ICommandHandler answer = context -> CommandResult.ok(getName() + " at " + context.currentLevel());
            root.then(literal("public").executes(answer))
                    .then(literal("other").executes(answer))
                    .then(literal("secret", "Reserved").level(ECommandLevel.DEV).executes(answer));
        }
    }

    // Answers at the next tick: the next line of a script runs later, in a tick task
    private static class LaterCommand extends BaseCommand {
        private final Commands commands;

        LaterCommand(Commands commands) {
            super(TestCommands.Id("later"), ECommandLevel.USER, "Answers at the next tick");
            this.commands = commands;
        }

        @Override
        protected void syntax(CommandNode root) {
            root.executes(context -> {
                CompletableFuture<CommandResult> future = new CompletableFuture<>();
                long start = commands.getTick();
                commands.addTickTask(() -> {
                    if (commands.getTick() <= start)
                        return false;
                    future.complete(CommandResult.ok("later"));
                    return true;
                });
                return CommandResult.later(future);
            });
        }
    }

    // Starts a script of the given lines, as exec does
    private static class ScriptCommand extends BaseCommand {
        private final Commands commands;

        ScriptCommand(Commands commands) {
            super(TestCommands.Id("script"), ECommandLevel.USER, "Runs the given lines");
            this.commands = commands;
        }

        @Override
        protected void syntax(CommandNode root) {
            root.then(argument("lines", word(), "The lines").variadic().executes(context -> {
                CommandScript script = new CommandScript(commands, "test", context.<String>getList("lines"));
                commands.addTickTask(script::step);
                return CommandResult.later(script.getResult());
            }));
        }
    }

    private static CommandResult run(Commands commands, String line, ICommandSource source) throws Exception {
        CompletableFuture<CommandResult> future = commands.submit(line, source);
        for (int tick = 0; tick < 10 && !future.isDone(); ++tick)
            commands.processPendingCommands();

        assertTrue(future.isDone(), "not finished");
        return future.get(1, TimeUnit.SECONDS);
    }

    private static Commands withLevels() {
        Commands commands = new Commands();
        TestCommands.add(commands, new LevelCommand("zzuser", ECommandLevel.USER));
        TestCommands.add(commands, new LevelCommand("zzcheat", ECommandLevel.CHEAT));
        TestCommands.add(commands, new LevelCommand("zzdev", ECommandLevel.DEV));
        return commands;
    }

    @Test
    void helpOnlyListsTheVisibleCommands() throws Exception {
        Commands commands = withLevels();

        String player = run(commands, "help", PLAYER).getMessage();
        assertTrue(player.contains("test:zzuser"));
        assertTrue(player.contains("mightylib:help"));
        assertTrue(player.contains("mightylib:screenshot"));
        assertFalse(player.contains("test:zzcheat"));
        assertFalse(player.contains("test:zzdev"));
        assertFalse(player.contains("mightylib:exec"));
        assertFalse(player.contains("mightylib:gl"));

        assertTrue(run(commands, "help", CHEATER).getMessage().contains("test:zzcheat"));
        assertTrue(run(commands, "help", DEVELOPER).getMessage().contains("mightylib:exec"));

        // help of a hidden command: as if it did not exist
        assertEquals(run(commands, "help zznone", PLAYER).getMessage().replace("zznone", "zzdev"),
                run(commands, "help zzdev", PLAYER).getMessage());
        assertTrue(run(commands, "help zzdev", DEVELOPER).isSuccess());
    }

    @Test
    void completionOnlyGivesTheVisibleCommands() {
        Commands commands = withLevels();

        assertEquals(List.of("zzuser"), commands.complete("zz", PLAYER).getCandidates());
        assertEquals(List.of("zzcheat", "zzuser"), commands.complete("zz", CHEATER).getCandidates());
        assertEquals(List.of("zzcheat", "zzdev", "zzuser"), commands.complete("zz", DEVELOPER).getCandidates());
        // The default source is the api: everything
        assertEquals(3, commands.complete("zz").getCandidates().size());

        assertFalse(commands.complete("e", PLAYER).getCandidates().contains("exec"));
        assertTrue(commands.complete("help zz", PLAYER).getCandidates().contains("zzuser"));
        assertFalse(commands.complete("help zz", PLAYER).getCandidates().contains("zzdev"));
        // The arguments of a hidden command are not completed either
        assertTrue(commands.complete("zzdev ", PLAYER).isEmpty());
    }

    @Test
    void hiddenCommandIsRefusedLikeAnUnknownOne() throws Exception {
        Commands commands = withLevels();

        CommandResult hidden = run(commands, "zzdev public", PLAYER);
        CommandResult unknown = run(commands, "zzzz public", PLAYER);
        assertFalse(hidden.isSuccess());
        assertEquals(unknown.getMessage().replace("zzzz", "zzdev"), hidden.getMessage());
        // Its help argument and its identifier too
        assertEquals(hidden.getMessage(), run(commands, "zzdev --help", PLAYER).getMessage());
        assertEquals(unknown.getMessage().replace("zzzz", "test:zzdev"), run(commands, "test:zzdev", PLAYER).getMessage());
        assertEquals(unknown.getMessage().replace("zzzz", "exec"), run(commands, "exec a.txt", PLAYER).getMessage());

        assertTrue(run(commands, "zzdev public", DEVELOPER).isSuccess());
    }

    @Test
    void ambiguityOnlyCountsTheVisibleCommands() throws Exception {
        Commands commands = new Commands();
        TestCommands.add(commands, new LevelCommand("help", ECommandLevel.DEV));

        // The player does not see test:help: help is the one of the engine
        assertTrue(run(commands, "help", PLAYER).getMessage().startsWith("Available commands"));
        assertEquals(List.of("help"), commands.complete("hel", PLAYER).getCandidates());

        CommandResult developer = run(commands, "help", DEVELOPER);
        assertFalse(developer.isSuccess());
        assertEquals("Ambiguous command ->help<-, write one of : mightylib:help, test:help", developer.getMessage());
    }

    @Test
    void reservedBranchHidesASubCommand() throws Exception {
        Commands commands = withLevels();

        CommandResult secret = run(commands, "zzuser secret", PLAYER);
        assertFalse(secret.isSuccess());
        assertEquals(run(commands, "zzuser zzzz", PLAYER).getMessage().replace("zzzz", "secret"), secret.getMessage());
        assertTrue(secret.getMessage().startsWith("Unknown argument ->secret<-"));
        assertFalse(secret.getMessage().contains("zzuser secret"));
        assertFalse(run(commands, "zzuser", PLAYER).isSuccess());
        assertEquals("zzuser at USER", run(commands, "zzuser public", PLAYER).getMessage());
        assertEquals("zzuser at DEV", run(commands, "zzuser secret", DEVELOPER).getMessage());

        assertEquals(List.of("other", "public"), commands.complete("zzuser ", PLAYER).getCandidates());
        assertTrue(commands.complete("zzuser s", DEVELOPER).getCandidates().contains("secret"));

        // The help too: nothing of the branch, in the text and the JSON
        CommandResult help = run(commands, "help zzuser", PLAYER);
        assertFalse(help.getMessage().contains("secret"));
        assertFalse(help.getData().toString().contains("secret"));
        assertEquals(help.getMessage(), run(commands, "zzuser --help", PLAYER).getMessage());
        assertTrue(run(commands, "zzuser --help", DEVELOPER).getMessage().contains("zzuser secret : Reserved"));
    }

    @Test
    void generalSubCommandsOfTheDeveloperAreHidden() throws Exception {
        Commands commands = new Commands();

        // Refused before reaching the engine: no GL needed
        assertTrue(run(commands, "stats glObjects", PLAYER).getMessage().startsWith("Unknown argument ->glObjects<-"));
        assertFalse(run(commands, "stats", PLAYER).isSuccess());
        assertTrue(run(commands, "screenshot my.png", PLAYER).getMessage().startsWith("Unknown argument ->my.png<-"));
        assertTrue(run(commands, "console log", PLAYER).getMessage().startsWith("Unknown argument ->log<-"));
        assertTrue(run(commands, "console follow", PLAYER).getMessage().startsWith("Unknown argument ->follow<-"));

        assertEquals(List.of("fps"), commands.complete("stats ", PLAYER).getCandidates());
        assertFalse(run(commands, "screenshot --rect 0 0 1 1 --corners 0 0 1 1", DEVELOPER).isSuccess());
        assertFalse(commands.complete("console ", PLAYER).getCandidates().contains("log"));
        assertTrue(commands.complete("console ", DEVELOPER).getCandidates().contains("log"));
    }

    @Test
    void scriptsKeepTheLevelOfTheirSourceEvenLater() throws Exception {
        Commands commands = withLevels();
        TestCommands.add(commands, new LaterCommand(commands));
        TestCommands.add(commands, new ScriptCommand(commands));

        // zzdev runs after later: in a tick task, with no submitted command around
        CommandResult player = run(commands, "script \"zzuser public\" later \"zzdev public\"", PLAYER);
        assertFalse(player.isSuccess());
        assertTrue(player.getMessage().contains("stopped at line 3"), player.getMessage());
        JSONObject failed = player.getData().getJSONArray("lines").getJSONObject(2);
        assertEquals("No command with its name ->zzdev<-", failed.getString("message"));

        CommandResult developer = run(commands, "script \"zzuser public\" later \"zzdev public\"", DEVELOPER);
        assertTrue(developer.isSuccess(), developer.getMessage());
        assertEquals("zzdev at DEV", developer.getData().getJSONArray("lines").getJSONObject(2).getString("message"));
    }

    @Test
    void sessionLevelIsBoundedByTheBuild() {
        Commands commands = new Commands(ECommandLevel.CHEAT, ECommandLevel.DEV, false);
        assertEquals(ECommandLevel.CHEAT, commands.getSessionLevel());

        ICommandSource console = ICommandSource.Named("game", commands::getSessionLevel);
        TestCommands.add(commands, new LevelCommand("zzcheat", ECommandLevel.CHEAT));

        assertEquals(ECommandLevel.USER, commands.setSessionLevel(ECommandLevel.USER));
        assertTrue(commands.complete("zz", console).isEmpty());

        assertEquals(ECommandLevel.CHEAT, commands.setSessionLevel(ECommandLevel.CHEAT));
        assertEquals(List.of("zzcheat"), commands.complete("zz", console).getCandidates());

        assertEquals(ECommandLevel.CHEAT, commands.setSessionLevel(ECommandLevel.DEV));
    }

    @Test
    void cheatsCommandOnlyExistsWhenAskedAndAllowed() throws Exception {
        assertNull(new Commands(ECommandLevel.DEV, ECommandLevel.USER, false).findCommand("cheats"));
        // The build does not allow the cheats: not even registered
        assertNull(new Commands(ECommandLevel.USER, ECommandLevel.USER, true).findCommand("cheats"));

        Commands commands = new Commands(ECommandLevel.CHEAT, ECommandLevel.USER, true);
        ICommandSource console = ICommandSource.Named("game", commands::getSessionLevel);
        TestCommands.add(commands, new LevelCommand("zzcheat", ECommandLevel.CHEAT));

        assertFalse(run(commands, "zzcheat public", console).isSuccess());
        assertEquals("Cheats on", run(commands, "cheats on", console).getMessage());
        assertEquals(ECommandLevel.CHEAT, commands.getSessionLevel());
        assertTrue(run(commands, "zzcheat public", console).isSuccess());

        assertEquals("Cheats off", run(commands, "cheats off", console).getMessage());
        assertEquals(ECommandLevel.USER, commands.getSessionLevel());
        assertFalse(run(commands, "cheats maybe", console).isSuccess());

        // The developer mode is never lowered by the cheats command
        Commands developer = new Commands(ECommandLevel.DEV, ECommandLevel.DEV, true);
        assertFalse(run(developer, "cheats off", PLAYER).isSuccess());
        assertEquals(ECommandLevel.DEV, developer.getSessionLevel());
    }

    @Test
    void logLinesAreSeenFromTheirLevel() throws Exception {
        ConsoleLog log = new ConsoleLog(20);
        Commands commands = new Commands();
        commands.addListener(new CommandEcho(log));
        TestCommands.add(commands, TestCommands.Syntax("detailed", ECommandLevel.USER, "Readable and detailed",
                root -> root.executes(context -> CommandResult.ok("Readable", new JSONObject().put("detail", 42)))));

        log.add(ConsoleLog.EType.OUT, null, "--engine line");
        run(commands, "detailed", PLAYER);

        List<String> player = new ArrayList<>();
        for (ConsoleLog.Line line : log.last(10, ECommandLevel.USER))
            player.add(line.getText());
        assertEquals(List.of("detailed", "Readable"), player);

        List<ConsoleLog.Line> developer = log.last(10, ECommandLevel.DEV);
        assertEquals("--engine line", developer.get(0).getText());
        assertTrue(developer.get(developer.size() - 1).getText().contains("\"detail\""));
        assertEquals(ECommandLevel.DEV, developer.get(developer.size() - 1).getLevel());

        // Only the visible lines count in the last ones
        assertEquals(List.of("Readable"), Arrays.asList(log.last(1, ECommandLevel.USER).get(0).getText()));
    }

    // Regression: "cheats off" was logged at CHEAT (before) and its answer at USER (after): the player saw the answer
    // without the command
    @Test
    void aCommandAndItsAnswerAreLoggedAtTheSameLevel() throws Exception {
        ConsoleLog log = new ConsoleLog(20);
        Commands commands = new Commands(ECommandLevel.CHEAT, ECommandLevel.CHEAT, true);
        commands.addListener(new CommandEcho(log));
        ICommandSource console = ICommandSource.Named("game", commands::getSessionLevel);

        run(commands, "cheats off", console);
        run(commands, "cheats on", console);

        // Without the data (DEV): the command and its message, twice
        List<ConsoleLog.Line> lines = log.last(10, ECommandLevel.CHEAT);
        assertEquals(4, lines.size());
        assertEquals(lines.get(0).getLevel(), lines.get(1).getLevel());
        assertEquals(ECommandLevel.CHEAT, lines.get(1).getLevel());
        assertEquals(lines.get(2).getLevel(), lines.get(3).getLevel());
        assertEquals(ECommandLevel.USER, lines.get(3).getLevel());
    }

    // Regression: the open console rebuilt its rows for every line of program output the player does not see
    @Test
    void hiddenLinesDoNotChangeTheVersionOfTheViewer() {
        ConsoleLog log = new ConsoleLog(20);
        long player = log.version(ECommandLevel.USER);
        long developer = log.version(ECommandLevel.DEV);

        log.add(ConsoleLog.EType.OUT, null, "--engine line");
        assertEquals(player, log.version(ECommandLevel.USER));
        assertNotEquals(developer, log.version(ECommandLevel.DEV));

        log.add(ConsoleLog.EType.MESSAGE, null, "for the player", ECommandLevel.USER);
        assertNotEquals(player, log.version(ECommandLevel.USER));
    }

    // Regression: the clear of the player erased the log the developer reads (console log, debug server)
    @Test
    void theClearOfThePlayerOnlyHidesItsLines() {
        ConsoleLog log = new ConsoleLog(20);
        log.add(ConsoleLog.EType.OUT, null, "--engine line");
        log.add(ConsoleLog.EType.MESSAGE, null, "old", ECommandLevel.USER);
        long player = log.version(ECommandLevel.USER);

        log.clear(ECommandLevel.USER);
        assertNotEquals(player, log.version(ECommandLevel.USER));
        assertTrue(log.last(10, ECommandLevel.USER).isEmpty());
        assertEquals(2, log.last(10, ECommandLevel.DEV).size());

        log.add(ConsoleLog.EType.MESSAGE, null, "new", ECommandLevel.USER);
        assertEquals("new", log.last(10, ECommandLevel.USER).get(0).getText());
        // Cleared for the levels below too, not above
        List<String> cheater = new ArrayList<>();
        for (ConsoleLog.Line line : log.last(10, ECommandLevel.CHEAT))
            cheater.add(line.getText());
        assertEquals(List.of("old", "new"), cheater);

        log.clear(ECommandLevel.DEV);
        assertEquals(0, log.size());
    }

    // ---- as and complete: what the player sees, checked without him ----

    @Test
    void asNeverRaisesTheLevel() throws Exception {
        Commands commands = withLevels();

        assertEquals("zzuser at USER", run(commands, "as user zzuser public", DEVELOPER).getMessage());
        assertEquals("zzuser at CHEAT", run(commands, "as cheat zzuser public", DEVELOPER).getMessage());
        // as itself is reserved to the developer: the player can't use it to climb
        assertEquals("No command with its name ->as<-", run(commands, "as dev zzuser secret", PLAYER).getMessage());
        assertEquals("No command with its name ->as<-", run(commands, "as cheat as dev zzdev public", DEVELOPER).getMessage());

        ICommandSource lowered = ICommandSource.Lowered(PLAYER, ECommandLevel.DEV);
        assertEquals(ECommandLevel.USER, lowered.level());
        assertEquals("game (as dev)", lowered.name());
    }

    @Test
    void developerCommandIsUnknownAsUser() throws Exception {
        Commands commands = withLevels();

        String unknown = run(commands, "as user zzzz public", DEVELOPER).getMessage();
        assertEquals(unknown.replace("zzzz", "zzdev"), run(commands, "as user zzdev public", DEVELOPER).getMessage());
        assertEquals(unknown.replace("zzzz", "exec"), run(commands, "as user exec a.txt", DEVELOPER).getMessage());
        // A reserved branch too, and the help of the player
        assertTrue(run(commands, "as user zzuser secret", DEVELOPER).getMessage().startsWith("Unknown argument ->secret<-"));
        assertFalse(run(commands, "as user help zzuser", DEVELOPER).getMessage().contains("secret"));
        assertTrue(run(commands, "as dev help zzuser", DEVELOPER).getMessage().contains("secret"));
    }

    @Test
    void asRunsWithTheLoweredSourceAndKeepsDeferredResults() throws Exception {
        Commands commands = withLevels();
        TestCommands.add(commands, TestCommands.Syntax("zzsource", ECommandLevel.USER, "Its source",
                root -> root.executes(context -> CommandResult.ok(context.source().name() + " " + context.currentLevel()))));
        TestCommands.add(commands, new LaterCommand(commands));

        assertEquals("server (as user) USER", run(commands, "as user zzsource", DEVELOPER).getMessage());
        assertEquals("later", run(commands, "as user later", DEVELOPER).getMessage());
        // One word between quotes: the whole line
        assertEquals("zzuser at USER", run(commands, "as user \"zzuser public\"", DEVELOPER).getMessage());
    }

    @Test
    void completeGivesWhatTheLevelIsProposed() throws Exception {
        Commands commands = withLevels();

        CommandResult player = run(commands, "complete --as user \"zzuser \"", DEVELOPER);
        assertTrue(player.isSuccess());
        assertEquals("other public", player.getMessage());
        assertEquals(List.of("other", "public"), player.getData().getJSONArray("candidates").toList());
        assertEquals(7, player.getData().getInt("start"));
        assertEquals("", player.getData().getString("typed"));
        assertTrue(player.getData().isNull("replacement"));

        assertTrue(run(commands, "complete \"zzuser \"", DEVELOPER).getMessage().contains("secret"));

        // Neither the commands nor the branches of the developer
        List<Object> names = run(commands, "complete --as user zz", DEVELOPER).getData().getJSONArray("candidates").toList();
        assertEquals(List.of("zzuser"), names);
        String all = run(commands, "complete --as user", DEVELOPER).getMessage();
        assertFalse(all.contains("exec") || all.contains("complete") || all.contains("zzdev"), all);
        assertTrue(all.contains("help"), all);

        assertEquals("zzuser ", run(commands, "complete --as user zzu", DEVELOPER).getData().getString("replacement"));
    }
}
