package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import org.joml.Vector2i;

public class WindowInfo {
    public boolean isWindowCreated() { return windowId != 0; }

    long windowId;
    public long getWindowId() { return windowId; }

    Vector2i size;
    public Vector2i getSizeRef() { return size; }
    public Vector2i getSizeCopy() { return new Vector2i(size); }

    float ratio;
    public float getRatio() { return ratio; }

    Vector2i virtualSize;
    public Vector2i getVirtualSizeRef() { return virtualSize; }
    public Vector2i getVirtualSizeCopy() { return new Vector2i(virtualSize); }

    float virtualRatio;
    public float getVirtualRatio() { return virtualRatio; }

    String windowName;
    public String getWindowName() { return windowName; }

    EWindowMode mode;
    public EWindowMode getWindowMode() { return mode; }

    // Screen of the display settings, null for the primary one
    MonitorInfo monitor;
    public MonitorInfo getMonitor() { return monitor; }

    WindowInfo(){
        windowId = 0;
        size = new Vector2i();
        ratio = 0;
        virtualSize = new Vector2i();
        virtualRatio = 0;
        windowName = "";
        mode = EWindowMode.WINDOWED;
    }
}
