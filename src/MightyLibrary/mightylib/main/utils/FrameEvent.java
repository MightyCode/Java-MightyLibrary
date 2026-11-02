package MightyLibrary.mightylib.main.utils;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Java Flight Recorder event around the display of a frame (buffer swap excluded).
 * Nearly free when no recording is running. See CLAUDE.md to record and read it.
 */
@Name("mightylib.Frame")
@Label("Frame")
@Category("MightyLibrary")
@Description("Display of a frame, buffer swap excluded")
public class FrameEvent extends jdk.jfr.Event {
}
