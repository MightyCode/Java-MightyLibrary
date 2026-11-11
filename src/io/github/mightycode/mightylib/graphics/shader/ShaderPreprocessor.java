package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.ResourceException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The #include of the engine (GLSL has none): #include "path" is replaced by the file, searched first next to the
 * file that includes it, then in the folder of the GLSL version. Without GL: the files are read by a given function.
 * A file is included once per source, a cycle or a missing file throws a ResourceException, a #version in an
 * included file is refused. "#line n id" directives are inserted (id: index of the file in files(), 0 the main one)
 * so the compiler log gives the real line.
 * One instance per source: files() and watchedFiles() describe the last process.
 */
public class ShaderPreprocessor {
    private static final String INCLUDE = "#include";
    private static final Pattern VERSION = Pattern.compile("^\\s*#\\s*version\\b");

    // Content of a file, null when there is no such file
    private final Function<String, String> reader;
    private final String versionFolder;
    // Added to the line given to #line: 0 when it is the number of the next line (GLSL 330),
    // -1 when the next line is that number plus one (GLSL 140)
    private final int lineOffset;

    // Files by id (#line), the main one first
    private final List<String> files = new ArrayList<>();
    // Files the source is made of, and the paths of a missing include: watched to load the source again
    private final Set<String> watched = new LinkedHashSet<>();
    private final Set<String> included = new HashSet<>();
    private final Deque<String> including = new ArrayDeque<>();
    // Content of the file found by the last resolve
    private String resolvedContent;

    public ShaderPreprocessor(Function<String, String> reader, String versionFolder, int lineOffset) {
        this.reader = reader;
        this.versionFolder = versionFolder;
        this.lineOffset = lineOffset;
    }

    // The source of the file with its includes; the file itself is read by the reader too
    public String process(String path) {
        files.clear();
        watched.clear();
        included.clear();
        including.clear();

        String main = normalize(path);
        watched.add(main);
        String content = reader.apply(main);
        if (content == null)
            throw new ResourceException("shader file not found: " + main);

        StringBuilder out = new StringBuilder(content.length());
        appendFile(out, main, content, true);
        return out.toString();
    }

    // Files by id of the #line directives (0: the main one)
    public List<String> files() {
        return Collections.unmodifiableList(files);
    }

    // Files read, and paths of a missing include (creating it loads the source again)
    public List<String> watchedFiles() {
        return new ArrayList<>(watched);
    }

    private void appendFile(StringBuilder out, String path, String content, boolean main) {
        int id = files.size();
        files.add(path);
        included.add(path);
        including.push(path);

        String[] lines = content.split("\n", -1);
        // No #line before the #version of the main file: an include must come after it
        boolean versionToCome = main && hasVersion(lines);

        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
            int lineNumber = i + 1;

            if (VERSION.matcher(line).find()) {
                if (!main)
                    throw new ResourceException("#version in the included file " + path + ":" + lineNumber
                            + " (only the main file gives it)");
                versionToCome = false;
            }

            String trimmed = line.trim();
            if (!trimmed.startsWith(INCLUDE)) {
                appendLine(out, line, i == lines.length - 1);
                continue;
            }

            String where = path + ":" + lineNumber;
            if (versionToCome)
                throw new ResourceException("#include before #version in " + where);

            String includePath = resolve(path, includeName(trimmed, where), where);
            if (including.contains(includePath))
                throw new ResourceException("#include cycle: " + cycle(includePath));

            // Once per source: the line stays (as a comment), so the numbering does not change
            if (included.contains(includePath)) {
                appendLine(out, "// " + trimmed + " (already included)", i == lines.length - 1);
                continue;
            }

            String includeContent = resolvedContent;
            out.append("#line ").append(1 + lineOffset).append(' ').append(files.size()).append('\n');
            appendFile(out, includePath, includeContent, false);
            if (out.length() > 0 && out.charAt(out.length() - 1) != '\n')
                out.append('\n');
            // Back to this file, at its next line
            out.append("#line ").append(lineNumber + 1 + lineOffset).append(' ').append(id).append('\n');
        }

        including.pop();
    }

    private static void appendLine(StringBuilder out, String line, boolean last) {
        out.append(line);
        if (!last)
            out.append('\n');
    }

    private static boolean hasVersion(String[] lines) {
        for (String line : lines) {
            if (VERSION.matcher(line).find())
                return true;
        }

        return false;
    }

    private static String includeName(String trimmed, String where) {
        String name = trimmed.substring(INCLUDE.length()).trim();
        if (name.length() < 3 || !name.startsWith("\"") || !name.endsWith("\""))
            throw new ResourceException("invalid #include in " + where + ", expected #include \"path\"");

        return name.substring(1, name.length() - 1);
    }

    // Next to the file that includes it, then in the folder of the version; missing: both are watched
    private String resolve(String includingPath, String name, String where) {
        Path parent = Paths.get(includingPath).getParent();
        String nextToIt = normalize(parent == null ? name : parent.resolve(name).toString());
        String inVersionFolder = normalize(Paths.get(versionFolder).resolve(name).toString());

        for (String candidate : new String[] { nextToIt, inVersionFolder }) {
            resolvedContent = reader.apply(candidate);
            if (resolvedContent != null) {
                watched.add(candidate);
                return candidate;
            }
        }

        watched.add(nextToIt);
        watched.add(inVersionFolder);
        throw new ResourceException("#include \"" + name + "\" not found (" + where + "), searched " + nextToIt
                + " and " + inVersionFolder);
    }

    private String cycle(String includePath) {
        List<String> chain = new ArrayList<>(including);
        Collections.reverse(chain);
        int start = chain.indexOf(includePath);
        List<String> loop = new ArrayList<>(chain.subList(start, chain.size()));
        loop.add(includePath);
        return String.join(" -> ", loop);
    }

    // Same file, same string: forward slashes, no "." or ".."
    static String normalize(String path) {
        try {
            return Paths.get(path).normalize().toString().replace('\\', '/');
        } catch (InvalidPathException e) {
            throw new ResourceException("invalid shader path " + path);
        }
    }
}
