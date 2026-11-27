package io.github.mightycode.mightylib.test;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The key of the series of reference images of a machine: its OS and the family of its OpenGL driver, as
 * {@code windows-nvidia} or {@code linux-mesa-llvmpipe}. Two drivers round differently: each has its own series. A
 * driver that renders on the processor (llvmpipe) has one series whatever the maker of the processor: the few pixels
 * that Intel and AMD round otherwise are a tolerance of the view that shows them, with its reason (one run of the CI
 * gives the whole series, on any runner). The property {@value #PROPERTY} replaces the key read from the driver.
 */
public final class RendererKey {
    public static final String PROPERTY = "mightylib.test.referenceKey";

    // The drivers of Mesa named in its vendor or renderer strings, as "AMD Radeon RX 580 (radeonsi, polaris10, ...)"
    // (not iris: the renderer of Mesa for Intel does not name it, and "Iris" is a name of Intel cards on Windows)
    private static final List<String> MESA_DRIVERS = List.of("llvmpipe", "softpipe", "lavapipe", "radeonsi",
            "r600", "r300", "nouveau", "zink", "virgl", "d3d12", "panfrost", "freedreno", "v3d", "vc4", "etnaviv",
            "lima", "asahi");
    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private RendererKey() {}

    /** The key of a machine, from the name of its OS (os.name), the GL_VENDOR and GL_RENDERER of its driver; pure. */
    public static String of(String osName, String vendor, String renderer) {
        return os(osName) + "-" + family(vendor == null ? "" : vendor, renderer == null ? "" : renderer);
    }

    /**
     * The key of this machine: the property {@value #PROPERTY} if given, otherwise read from the driver of the
     * current OpenGL context (main thread, a window created).
     * @throws IllegalStateException without OpenGL context on this thread, or if the property is not a key
     */
    public static String current() {
        String property = System.getProperty(PROPERTY);
        if (property != null && !property.trim().isEmpty())
            return resolve(property, null, null, null);

        try {
            GL.getCapabilities();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("The key of the driver is read with an OpenGL context current: start a"
                    + " test engine with a window, or give -D" + PROPERTY, e);
        }
        return of(System.getProperty("os.name"), GL11C.glGetString(GL11C.GL_VENDOR),
                GL11C.glGetString(GL11C.GL_RENDERER));
    }

    // The property if given (checked), otherwise the key of the driver
    static String resolve(String property, String osName, String vendor, String renderer) {
        if (property != null && !property.trim().isEmpty()) {
            String key = property.trim();
            if (!VALID_KEY.matcher(key).matches())
                throw new IllegalStateException("-D" + PROPERTY + "=" + key + " is not a key (lower case letters,"
                        + " digits and dashes, as linux-mesa-llvmpipe)");
            return key;
        }
        return of(osName, vendor, renderer);
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
