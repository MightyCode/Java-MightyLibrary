package io.github.mightycode.mightylib.console;

import java.util.function.Consumer;

/**
 * Writes the choices of an ambiguous completion in the console log, once: a Tab that would write the same choices
 * again, with nothing written since, rings the bell instead (the console shows that the Tab was received).
 */
final class CompletionListing implements Consumer<String> {
    private final ConsoleLog log;
    private final Runnable bell;

    private String lastChoices;
    // Version of the log just after the choices were written: anything written since makes them worth writing again
    private long versionAfter = -1;

    CompletionListing(ConsoleLog log, Runnable bell) {
        this.log = log;
        this.bell = bell;
    }

    @Override
    public void accept(String choices) {
        if (choices.equals(lastChoices) && log.version() == versionAfter) {
            bell.run();
            return;
        }

        log.add(ConsoleLog.EType.RESULT, null, choices);
        lastChoices = choices;
        versionAfter = log.version();
    }
}
