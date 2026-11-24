package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Java Flight Recorder event around the send of a batch flush: the sort of the runs, the upload of the buffers and the
 * draw calls, so that the profile separates the OpenGL part of a frame from the writing of the draws.
 * Nearly free when no recording is running. See docs/debug-tools.md to record and read it.
 */
@Name("mightylib.BatchSend")
@Label("Batch send")
@Category("MightyLibrary")
@Description("Send of the waiting draws of a 2D batch: sort, upload and draw calls")
public final class BatchSendEvent extends jdk.jfr.Event {
    @Label("Draws")
    private int draws;

    @Label("Draw calls")
    private int drawCalls;

    @Label("Bytes sent")
    @DataAmount
    private long bytes;

    public void setSent(int draws, int drawCalls, long bytes) {
        this.draws = draws;
        this.drawCalls = drawCalls;
        this.bytes = bytes;
    }
}
