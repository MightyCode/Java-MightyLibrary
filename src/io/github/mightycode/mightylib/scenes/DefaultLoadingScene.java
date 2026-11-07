package io.github.mightycode.mightylib.scenes;

/**
 * Loading scene of the engine: a grey screen, it needs no resource.
 * Replace it with EngineConfig.setLoadingScene, or per scene with Scene.createLoadingScene.
 */
public class DefaultLoadingScene extends TemplateSceneLoading {
    @Override
    public void display() {
        setVirtualScene();
        setClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        clear();
        super.display();

        setAndDisplayRealScene();
    }
}
