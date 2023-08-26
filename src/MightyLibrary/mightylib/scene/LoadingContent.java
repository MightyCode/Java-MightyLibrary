package MightyLibrary.mightylib.scene;

public abstract class LoadingContent extends Thread {
    protected final Result result;
    public abstract static class Result {}
    protected float percentage;
    protected String step = "Set up";

    public LoadingContent(Result result){
        this.result = result;
    }

    protected abstract void init();

    public final void run() {
        init();
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
