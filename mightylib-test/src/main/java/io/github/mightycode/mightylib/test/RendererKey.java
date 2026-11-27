package io.github.mightycode.mightylib.test;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The key of the series of reference images of a machine: its OS and the family of its OpenGL driver, as
 * {@code windows-nvidia}; for a driver that renders on the processor (llvmpipe, softpipe), the maker of the processor
 * too, as {@code linux-mesa-llvmpipe-intel}: Intel and AMD do not round the filtering of llvmpipe the same way.
 * Two drivers round differently: each has its own series. The property {@value #PROPERTY} replaces the key read from
 * the driver.
 */
public final class RendererKey {
    public static final String PROPERTY = "mightylib.test.referenceKey";

    // The drivers of Mesa named in its vendor or renderer strings, as "AMD Radeon RX 580 (radeonsi, polaris10, ...)"
    // (not iris: the renderer of Mesa for Intel does not name it, and "Iris" is a name of Intel cards on Windows)
    private static final List<String> MESA_DRIVERS = List.of("llvmpipe", "softpipe", "lavapipe", "radeonsi",
            "r600", "r300", "nouveau", "zink", "virgl", "d3d12", "panfrost", "freedreno", "v3d", "vc4", "etnaviv",
            "lima", "asahi");
    // The families that render on the processor
    private static final List<String> SOFTWARE = List.of("mesa-llvmpipe", "mesa-softpipe");
    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Path CPU_INFO = Paths.get("/proc/cpuinfo");

    private RendererKey() {}

    /**
     * The key of a machine, from the name of its OS (os.name), the GL_VENDOR and GL_RENDERER of its driver, and the
     * processor (its vendor id, as GenuineIntel or AuthenticAMD, or os.arch; only read for a software driver).
     * Pure: no GL.
     */
    public static String of(String osName, String vendor, String renderer, String processor) {
        String family = family(vendor == null ? "" : vendor, renderer == null ? "" : renderer);
        if (SOFTWARE.contains(family))
            family += "-" + processor(processor);
        return os(osName) + "-" + family;
    }

    /**
     * The key of this machine: the property {@value #PROPERTY} if given, otherwise read from the driver of the
     * current OpenGL context (main thread, a window created) and, for a software driver, from the processor.
     * @throws IllegalStateException without OpenGL context on this thread, or if the property is not a key
     */
    public static String current() {
        String property = System.getProperty(PROPERTY);
        if (property != null && !property.trim().isEmpty())
            return resolve(property, null, null, null, null);

        try {
            GL.getCapabilities();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("The key of the driver is read with an OpenGL context current: start a"
                    + " test engine with a window, or give -D" + PROPERTY, e);
        }
        return of(System.getProperty("os.name"), GL11C.glGetString(GL11C.GL_VENDOR),
                GL11C.glGetString(GL11C.GL_RENDERER), currentProcessor());
    }

    // The property if given (checked), otherwise the key of the driver
    static String resolve(String property, String osName, String vendor, String renderer, String processor) {
        if (property != null && !property.trim().isEmpty()) {
            String key = property.trim();
            if (!VALID_KEY.matcher(key).matches())
                throw new IllegalStateException("-D" + PROPERTY + "=" + key + " is not a key (lower case letters,"
                        + " digits and dashes, as linux-mesa-llvmpipe-intel)");
            return key;
        }
        return of(osName, vendor, renderer, processor);
    }

    // The vendor id of the processor of this machine (Linux, Windows), otherwise its architecture
    private static String currentProcessor() {
        String identifier = System.getenv("PROCESSOR_IDENTIFIER");
        if (identifier != null && !identifier.isEmpty())
            return identifier;

        if (Files.isReadable(CPU_INFO)) {
            try (Stream<String> lines = Files.lines(CPU_INFO)) {
                Optional<String> vendorId = lines.filter(line -> line.startsWith("vendor_id")).findFirst();
                if (vendorId.isPresent())
                    return vendorId.get().substring(vendorId.get().indexOf(':') + 1).trim();
            } catch (IOException | UncheckedIOException e) {
                // Unreadable: the architecture tells at least the family of processors
                return System.getProperty("os.arch");
            }
        }
        return System.getProperty("os.arch");
    }

    // The maker of the processor, from its vendor id or its architecture
    static String processor(String description) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);
        if (text.contains("intel"))
            return "intel";
        // "AMD64" is the architecture, Intel processors included: only the vendor id tells AMD
        if (text.replace("amd64", "").contains("amd"))
            return "amd";
        if (text.contains("aarch64") || text.contains("arm"))
            return "arm";
        return reduce(text);
    }

    static String os(String osName) {
        String name = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (name.startsWith("windows"))
            return "windows";
        if (name.startsWith("linux"))
            return "linux";
        if (name.startsWith("mac") || name.startsWith("darwin"))
            return "macos";
        return reduce(name);
    }

    static String family(String vendor, String renderer) {
        String vendorName = vendor.toLowerCase(Locale.ROOT);
        String rendererName = renderer.toLowerCase(Locale.ROOT);

        Matcher words = WORD.matcher(vendorName + " " + rendererName);
        while (words.find()) {
            String word = words.group();
            if (MESA_DRIVERS.contains(word))
                return "mesa-" + (word.equals("lavapipe") ? "llvmpipe" : word);
        }
        // Mesa without the name of its driver: "Mesa Intel(R) UHD Graphics 620", vendor "Mesa" or "X.Org"
        if (rendererName.startsWith("mesa ") || vendorName.contains("mesa") || vendorName.contains("x.org")) {
            String hardware = hardware(vendorName + " " + rendererName.replaceFirst("^mesa ", ""));
            return hardware.isEmpty() ? "mesa" : "mesa-" + hardware;
        }

        String hardware = hardware(vendorName + " " + rendererName);
        return hardware.isEmpty() ? (vendorName.trim().isEmpty() ? "unknown" : reduce(vendorName)) : hardware;
    }

    // The maker of the card, empty if unknown
    private static String hardware(String text) {
        if (text.contains("nvidia") || text.contains("geforce") || text.contains("quadro"))
            return "nvidia";
        if (text.contains("ati technologies") || text.contains("amd") || text.contains("radeon"))
            return "amd";
        if (text.contains("intel"))
            return "intel";
        if (text.contains("apple"))
            return "apple";
        return "";
    }

    // Lower case letters, digits and single dashes
    static String reduce(String text) {
        String reduced = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return reduced.isEmpty() ? "unknown" : reduced;
    }
}
