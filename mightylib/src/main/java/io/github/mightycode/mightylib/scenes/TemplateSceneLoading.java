package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.utils.LoadingElement;

import java.util.ArrayList;

/**
 * A scene displayed while another one loads (Scene.createLoadingScene, or the default loading scene of the game).
 * It is launched at once, without loading scene itself. Its hooks are the ones of Scene; it lasts until the
 * resources of the loaded scene and the elements added with addElementToLoad are loaded.
 */
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

    public final void addElementToLoad(LoadingElement element) {
        elementToLoad.add(element);
    }

    public final boolean isFinished() {
        for (LoadingElement element : elementToLoad) {
            if (!element.isLoaded()) {
                return false;
            }
        }

        return resources().finishedCurrentLoading();
    }
}
