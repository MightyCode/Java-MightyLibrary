package io.github.mightycode.mightylib.main.monitors;

import java.util.List;

/**
 * Decide on which screen the window opens, called before the window is created.
 * Return null to keep the primary screen.
 */
public interface IMonitorSelector {
    MonitorInfo select(List<MonitorInfo> monitors);
}
