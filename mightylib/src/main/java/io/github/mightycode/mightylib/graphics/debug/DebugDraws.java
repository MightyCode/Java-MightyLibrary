package io.github.mightycode.mightylib.graphics.debug;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The debug drawings of an engine (Engine.debugDraws): the global switch (EngineConfig.setDebugDraw,
 * -Dmightylib.debugDraw, command debugdraw) and the DebugDraw2D of each view (forgotten with their view). Main thread.
 */
public final class DebugDraws {
    private boolean enabled;
    private final Map<DebugDraw2D, String> views = new WeakHashMap<>();

    public DebugDraws(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Off: the drawings asked are not recorded, those waiting are dropped. On by default, in release too. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled)
            clear();
    }

    /** The drawings of every view are dropped. */
    public void clear() {
        for (DebugDraw2D draw : views.keySet())
            draw.clear();
    }

    // A view creates its DebugDraw2D (named for the command debugdraw)
    void register(DebugDraw2D draw, String viewName) {
        views.put(draw, viewName);
    }

    /** For the command debugdraw: on or off, and for each view the drawings alive by lifetime. */
    public JSONObject describe() {
        JSONArray viewArray = new JSONArray();
        int total = 0;
        for (Map.Entry<DebugDraw2D, String> view : views.entrySet()) {
            Map<String, Integer> lifetimes = view.getKey().countByLifetime();
            int count = view.getKey().size();
            total += count;
            viewArray.put(new JSONObject().put("view", view.getValue()).put("drawings", count)
                    .put("lifetimes", new JSONObject(lifetimes)));
        }

        return new JSONObject().put("enabled", enabled).put("drawings", total).put("views", viewArray);
    }
}
