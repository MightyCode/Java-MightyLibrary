package MightyLibrary.mightylib.scenes;

import MightyLibrary.mightylib.utils.ThreadOrCurrent;

public abstract class LoadingContent extends ThreadOrCurrent {
    protected final Result result;
    public abstract static class Result {}
    protected float percentage;
    protected String step = "Set up";

    public LoadingContent(Result result){
        this.result = result;
    }

    /** Between 0 and 1**/
    public final float percentage () { return percentage; }
    public final String getStep() { return step; }

    public final boolean finished() { return percentage == 1; }

    public final <T extends Result> T getResult(Class<T> type) {
        if (percentage != 1)
            return null;

        return type.cast(result);
    }
}
