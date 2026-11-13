package io.github.mightycode.mightylib.main.monitors;

import java.util.Locale;

/**
 * Common monitor selectors. When the wanted screen doesn't exist, they return null: the primary screen is used.
 */
public final class MonitorSelectors {
    // System property overriding the selector of the project (tools/scenario.py opens the tests on another screen)
    public static final String PROPERTY = "mightylib.window.monitor";

    private MonitorSelectors() {}

    public static IMonitorSelector Primary() {
        return monitors -> {
            for (MonitorInfo monitor : monitors) {
                if (monitor.isPrimary())
                    return monitor;
            }
            return null;
        };
    }

    // First screen that is not the primary one
    public static IMonitorSelector Secondary() {
        return monitors -> {
            for (MonitorInfo monitor : monitors) {
                if (!monitor.isPrimary())
                    return monitor;
            }
            return null;
        };
    }

    // Index in Monitors.List() (order given by the system)
    public static IMonitorSelector Index(int index) {
        return monitors -> index >= 0 && index < monitors.size() ? monitors.get(index) : null;
    }

    // First screen whose name contains the text, ignoring case
    public static IMonitorSelector Name(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return monitors -> {
            for (MonitorInfo monitor : monitors) {
                if (monitor.getName().toLowerCase(Locale.ROOT).contains(lower))
                    return monitor;
            }
            return null;
        };
    }

    // Screen with the most pixels
    public static IMonitorSelector Largest() {
        return monitors -> {
            MonitorInfo largest = null;
            for (MonitorInfo monitor : monitors) {
                if (largest == null || (long) monitor.getWidth() * monitor.getHeight() > (long) largest.getWidth() * largest.getHeight())
                    largest = monitor;
            }
            return largest;
        };
    }

    /**
     * Selector described by a text : "primary", "secondary", "largest", an index ("1") or a part of a name.
     */
    public static IMonitorSelector FromText(String text) {
        String value = text.trim();
        switch (value.toLowerCase(Locale.ROOT)) {
            case "primary":
                return Primary();
            case "secondary":
                return Secondary();
            case "largest":
                return Largest();
        }

        try {
            return Index(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Name(value);
        }
    }
}
