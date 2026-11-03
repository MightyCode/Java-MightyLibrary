package MightyLibrary.mightylib.main.utils;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Java Flight Recorder event around an update tick.
 * Nearly free when no recording is running. See CLAUDE.md to record and read it.
 */
@Name("mightylib.Tick")
@Label("Tick")
@Category("MightyLibrary")
@Description("Update of the scene (commands, resources, scene update and postUpdate)")
public class TickEvent extends jdk.jfr.Event {
}
