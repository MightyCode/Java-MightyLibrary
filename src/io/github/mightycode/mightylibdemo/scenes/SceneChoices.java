package io.github.mightycode.mightylibdemo.scenes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The scenes the menu offers, without GL: the names the scene command suggests (relative to the package of the
 * scenes, for example Platformer2D.PlatformerBox2D), minus the hidden ones, sorted by the name shown. The name shown
 * is the short one (PlatformerBox2D), the relative one only when two scenes have the same short name. The relative
 * name stays what the command takes.
 */
final class SceneChoices {
    static final class Entry {
        private final String command;
        private final String label;

        private Entry(String command, String label) {
            this.command = command;
            this.label = label;
        }

        /** The name the scene command takes. */
        String command() { return command; }

        /** The name shown in the list. */
        String label() { return label; }

        /** The class name without its package. */
        String shortName() { return ShortName(command); }
    }

    private final List<Entry> entries;

    /**
     * @param candidates the names the scene command suggests
     * @param hidden     the names (as the command takes them) the menu does not offer
     */
    SceneChoices(Collection<String> candidates, Set<String> hidden) {
        Map<String, Integer> shortNameCount = new HashMap<>();
        List<String> visible = new ArrayList<>();
        for (String candidate : candidates) {
            if (hidden.contains(candidate))
                continue;

            visible.add(candidate);
            shortNameCount.merge(ShortName(candidate).toLowerCase(Locale.ROOT), 1, Integer::sum);
        }

        List<Entry> list = new ArrayList<>();
        for (String name : visible) {
            boolean shared = shortNameCount.get(ShortName(name).toLowerCase(Locale.ROOT)) > 1;
            list.add(new Entry(name, shared ? name : ShortName(name)));
        }

        list.sort(Comparator.comparing((Entry entry) -> entry.label.toLowerCase(Locale.ROOT))
                .thenComparing(entry -> entry.command));
        entries = list;
    }

    /** All the scenes offered, sorted. */
    List<Entry> entries() {
        return entries;
    }

    /** The scenes whose short name contains the text, case ignored; all of them if the text is empty. */
    List<Entry> filter(String text) {
        String wanted = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty())
            return entries;

        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.shortName().toLowerCase(Locale.ROOT).contains(wanted))
                result.add(entry);
        }

        return result;
    }

    private static String ShortName(String relativeName) {
        return relativeName.substring(relativeName.lastIndexOf('.') + 1);
    }
}
