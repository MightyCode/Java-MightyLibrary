package MightyLibrary.mightylib.scenes;

import java.lang.reflect.ParameterizedType;

public abstract class LoadingScene <T extends LoadingContent, K extends LoadingContent.Result> extends Scene {
    private T loadingContent;
    protected K loadingResult;

    private float oldPercentage;

    public LoadingScene(T T){
        this.loadingContent = T;
        this.loadingResult = null;
        this.oldPercentage = -1;
    }

    protected abstract void initialize(String [] args);

    public final void init(String[] args) {
        super.init(args);
        loadingContent.start();

        initialize(args);
    }

    protected boolean hasLoadingAdvance(){
        return oldPercentage != percentage();
    }
    protected float percentage(){ return loadingContent.percentage(); }

    protected String stepName(){ return loadingContent.getStep(); }

    protected abstract void updateBeforeLoading();

    protected abstract void updateAfterLoading();

    protected abstract void endLoading();

    private Class<K> getLoadingResultType() {
        @SuppressWarnings("unchecked")
        Class<K> loadingResultType = (Class<K>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[1];
        return loadingResultType;
    }

    @Override
    public final void update(){
        super.update();

        if (loadingResult == null){
            if (loadingContent.finished()){
                loadingResult = loadingContent.getResult(getLoadingResultType());

                try {
                    loadingContent.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                loadingContent = null;

                endLoading();
                updateAfterLoading();
            } else {
                float before = percentage();

                updateBeforeLoading();

                oldPercentage = before;
            }
        } else {
            updateAfterLoading();
        }
    }


    protected abstract void displayBeforeLoading();

    protected abstract void displayAfterLoading();

    @Override
    public final void display(){
        if (loadingResult == null) {
            displayBeforeLoading();
        } else {
            displayAfterLoading();
        }
    }


    protected abstract void unloadBeforeLoading();

    protected abstract void unloadAfterLoading();

    @Override
    public final void unload(){
        super.unload();
        if (loadingResult == null) {
            loadingContent.interrupt();
            unloadBeforeLoading();

            System.out.println(Thread.activeCount());
        } else {
            unloadAfterLoading();
        }
    }
}
