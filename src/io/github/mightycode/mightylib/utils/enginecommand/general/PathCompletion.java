package io.github.mightycode.mightylib.utils.enginecommand.general;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Files and folders that can follow a path being typed, relative to the working directory or absolute.
 * Folders end with a slash.
 */
final class PathCompletion {
    private static final int MAX_ENTRIES = 500;

    private PathCompletion() {}

    static List<String> Complete(String typed) {
        List<String> values = new ArrayList<>();
        String normalized = typed.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String folderPart = slash < 0 ? "" : typed.substring(0, slash + 1);

        Path folder;
        try {
            folder = Paths.get(folderPart.isEmpty() ? "." : folderPart);
        } catch (RuntimeException e) {
            return values;
        }

        if (!Files.isDirectory(folder))
            return values;

        try (Stream<Path> entries = Files.list(folder)) {
            entries.limit(MAX_ENTRIES).forEach(entry -> {
                String name = entry.getFileName().toString();
                values.add(folderPart + name + (Files.isDirectory(entry) ? "/" : ""));
            });
        } catch (IOException | RuntimeException e) {
            // Unreadable folder: nothing to propose
        }

        return values;
    }
}
