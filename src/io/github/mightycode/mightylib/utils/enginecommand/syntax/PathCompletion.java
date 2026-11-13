package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Files and folders that can follow a path being typed, relative to a folder (the working directory by default) or
 * absolute. Folders end with a slash.
 */
public final class PathCompletion {
    private static final int MAX_ENTRIES = 500;

    private PathCompletion() {}

    // Relative to the working directory
    public static List<String> Complete(String typed) {
        return Complete("", typed);
    }

    // Relative to the folder ("" for the working directory), the values as typed (without the folder)
    public static List<String> Complete(String folder, String typed) {
        List<String> values = new ArrayList<>();
        String normalized = typed.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String folderPart = slash < 0 ? "" : typed.substring(0, slash + 1);

        Path directory;
        try {
            directory = Resolve(folder, folderPart.isEmpty() ? "." : folderPart);
        } catch (RuntimeException e) {
            return values;
        }

        if (!Files.isDirectory(directory))
            return values;

        try (Stream<Path> entries = Files.list(directory)) {
            entries.limit(MAX_ENTRIES).forEach(entry -> {
                String name = entry.getFileName().toString();
                values.add(folderPart + name + (Files.isDirectory(entry) ? "/" : ""));
            });
        } catch (IOException | RuntimeException e) {
            // Unreadable folder: nothing to propose
        }

        return values;
    }

    /**
     * The path typed, relative to the folder unless absolute.
     * @throws java.nio.file.InvalidPathException if it is not a valid path
     */
    public static Path Resolve(String folder, String typed) {
        Path path = Paths.get(typed);
        if (folder == null || folder.isEmpty() || path.isAbsolute())
            return path;

        return Paths.get(folder).resolve(path);
    }
}
