package MightyLibrary.project.scenes.loadingScenes;

import MightyLibrary.mightylib.scenes.TemplateSceneLoading;

public class FirstLoadingScene extends TemplateSceneLoading {
    public FirstLoadingScene() {
        super();
    }

    public void init(String[] args) {
        super.init(args, new LoadingSceneImplementation());
    }

    @Override
    public void launch(String[] args) {
        super.launch(args);
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void display() {
        super.setVirtualScene();
        clear();
        super.display();

        // Grey background
        setClearColor(0.2f, 0.2f, 0.2f, 1.0f);

        super.setAndDisplayRealScene();
    }

    public void unload() {
        super.unload();
    }
}
