package io.github.mightycode.mightylib.main.monitors;

import org.json.JSONObject;

/**
 * Description of a connected screen, read with GLFW (Monitors.List).
 * Positions are in the virtual desktop, the work area excludes the task bar.
 */
public class MonitorInfo {
    private final int index;
    private final long handle;
    private final String name;
    private final boolean primary;

    private final int x, y;
    private final int width, height;
    private final int refreshRate;
    private final int workX, workY, workWidth, workHeight;

    MonitorInfo(int index, long handle, String name, boolean primary, int x, int y, int width, int height,
                int refreshRate, int workX, int workY, int workWidth, int workHeight) {
        this.index = index;
        this.handle = handle;
        this.name = name;
        this.primary = primary;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
        this.workX = workX;
        this.workY = workY;
        this.workWidth = workWidth;
        this.workHeight = workHeight;
    }

    public int getIndex() { return index; }
    public long getHandle() { return handle; }
    public String getName() { return name; }
    public boolean isPrimary() { return primary; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getRefreshRate() { return refreshRate; }
    public int getWorkX() { return workX; }
    public int getWorkY() { return workY; }
    public int getWorkWidth() { return workWidth; }
    public int getWorkHeight() { return workHeight; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("index", index);
        json.put("name", name);
        json.put("primary", primary);
        json.put("position", x + "," + y);
        json.put("resolution", width + "x" + height);
        json.put("refreshRate", refreshRate);
        json.put("workArea", workX + "," + workY + " " + workWidth + "x" + workHeight);
        return json;
    }

    @Override
    public String toString() {
        return index + " : " + name + " " + width + "x" + height + "@" + refreshRate + "Hz at (" + x + ", " + y + ")"
                + (primary ? " (primary)" : "");
    }
}
