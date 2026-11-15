package io.github.mightycode.mightylib.main.monitors;

/**
 * A resolution and refresh rate a screen supports (MonitorInfo.getVideoModes), to choose a fullscreen resolution.
 */
public final class VideoMode {
    private final int width, height;
    private final int refreshRate;

    public VideoMode(int width, int height, int refreshRate) {
        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getRefreshRate() { return refreshRate; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VideoMode))
            return false;

        VideoMode mode = (VideoMode) other;
        return width == mode.width && height == mode.height && refreshRate == mode.refreshRate;
    }

    @Override
    public int hashCode() {
        return (width * 31 + height) * 31 + refreshRate;
    }

    @Override
    public String toString() {
        return width + "x" + height + "@" + refreshRate;
    }
}
