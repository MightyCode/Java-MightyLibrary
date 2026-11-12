package io.github.mightycode.mightylibdemo.scenes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

/**
 * When each scene of the demo was added and last modified, read from git: the first and last commit of its source
 * file (renames followed), or the date of the file on disk when it has changes not committed. Development only: the
 * demo runs from the repository; without git (or outside it) there are no dates and the menu sorts by name.
 */
final class SceneHistory {
    static final class Dates {
        private final long added;
        private final long modified;

        Dates(long added, long modified) {
            this.added = added;
            this.modified = modified;
        }

        /** Seconds since the epoch. */
        long added() { return added; }

        /** Seconds since the epoch. */
        long modified() { return modified; }
    }

    static final String SCENES_DIRECTORY = "src/io/github/mightycode/mightylibdemo/scenes/";

    private SceneHistory() {}

    /** The dates of the scenes, by the name the scene command takes; empty if git fails. Runs processes: not on the main thread. */
    static Map<String, Dates> Read() {
        String log = Git("log", "--reverse", "-M", "--format=@%ct", "--name-status", "--", SCENES_DIRECTORY);
        if (log == null)
            return Collections.emptyMap();

        Map<String, Dates> byPath = ParseLog(log);
        String status = Git("status", "--porcelain", "--untracked-files=all", "--", SCENES_DIRECTORY);
        if (status != null)
            ApplyStatus(byPath, status, path -> new File(path).lastModified() / 1000);

        return ByCommand(byPath);
    }

    /**
     * Reads "git log --reverse -M --format=@%ct --name-status": the dates of the files still there, by path from the
     * root of the repository.
     */
    static Map<String, Dates> ParseLog(String log) {
        Map<String, Dates> byPath = new HashMap<>();
        long time = 0;
        for (String line : log.split("\n")) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            if (line.startsWith("@")) {
                time = Long.parseLong(line.substring(1));
                continue;
            }

            String[] parts = line.split("\t");
            char kind = parts[0].charAt(0);
            switch (kind) {
                case 'A':
                    byPath.put(parts[1], new Dates(time, time));
                    break;
                case 'M':
                case 'T': {
                    Dates old = byPath.get(parts[1]);
                    byPath.put(parts[1], new Dates(old == null ? time : old.added, time));
                    break;
                }
                case 'D':
                    byPath.remove(parts[1]);
                    break;
                case 'R':
                case 'C': {
                    // R keeps the history of the file under its new name; a copy is a new file
                    Dates old = kind == 'R' ? byPath.remove(parts[1]) : null;
                    byPath.put(parts[2], new Dates(old == null ? time : old.added, time));
                    break;
                }
                default:
                    break;
            }
        }

        return byPath;
    }

    /**
     * Reads "git status --porcelain": a file with changes not committed was modified when it was last written
     * (lastModified, in seconds); one never committed was also added then.
     */
    static void ApplyStatus(Map<String, Dates> byPath, String status, ToLongFunction<String> lastModified) {
        for (String line : status.split("\n")) {
            if (line.length() < 4)
                continue;

            String code = line.substring(0, 2);
            String path = line.substring(3).trim();
            int arrow = path.indexOf(" -> ");
            String oldPath = null;
            if (arrow >= 0) {
                oldPath = path.substring(0, arrow);
                path = path.substring(arrow + 4);
            }

            if (code.indexOf('D') >= 0) {
                byPath.remove(path);
                continue;
            }

            Dates old = byPath.get(oldPath != null && byPath.containsKey(oldPath) ? oldPath : path);
            if (oldPath != null)
                byPath.remove(oldPath);

            long time = lastModified.applyAsLong(path);
            byPath.put(path, new Dates(old == null ? time : old.added, Math.max(time, old == null ? 0 : old.modified)));
        }
    }

    /** The paths of the scene sources become the names the scene command takes (Platformer2D.PlatformerBox2D). */
    static Map<String, Dates> ByCommand(Map<String, Dates> byPath) {
        Map<String, Dates> byCommand = new HashMap<>();
        for (Map.Entry<String, Dates> entry : byPath.entrySet()) {
            String path = entry.getKey();
            if (!path.startsWith(SCENES_DIRECTORY) || !path.endsWith(".java"))
                continue;

            String relative = path.substring(SCENES_DIRECTORY.length(), path.length() - ".java".length());
            byCommand.put(relative.replace('/', '.'), entry.getValue());
        }

        return byCommand;
    }

    // The output of a git command run from the working directory, null if it fails
    private static String Git(String... arguments) {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);

        try {
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = process.getInputStream()) {
                input.transferTo(output);
            }

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                return null;
            }

            return process.exitValue() == 0 ? output.toString(StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            // No git: the menu sorts by name only
            System.err.println("--Menu: git unavailable, no dates for the scenes (" + e.getMessage() + ")");
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
