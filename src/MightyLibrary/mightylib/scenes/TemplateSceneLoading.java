package MightyLibrary.mightylib.scenes;

import MightyLibrary.mightylib.utils.LoadingElement;

import java.util.ArrayList;

public abstract class TemplateSceneLoading extends Scene {
    private final ArrayList<LoadingElement> elementToLoad;

    public TemplateSceneLoading() {
        super();

        elementToLoad = new ArrayList<>();
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

        return true;
    }
}
