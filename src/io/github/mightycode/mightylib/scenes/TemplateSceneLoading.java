package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.utils.LoadingElement;

import java.util.ArrayList;

public abstract class TemplateSceneLoading extends Scene {
    private final ArrayList<LoadingElement> elementToLoad;

    public TemplateSceneLoading() {
        super();

        elementToLoad = new ArrayList<>();
    }

    // A loading scene is displayed at once, it has no loading scene itself
    @Override
    protected final TemplateSceneLoading createLoadingScene(String[] args) {
        return null;
    }

    public void addElementToLoad(LoadingElement element) {
        elementToLoad.add(element);
    }

    public boolean isFinished() {
        for (LoadingElement element : elementToLoad) {
            if (!element.isLoaded()) {
                return false;
            }
        }

        return resources.finishedCurrentLoading();
    }
}
