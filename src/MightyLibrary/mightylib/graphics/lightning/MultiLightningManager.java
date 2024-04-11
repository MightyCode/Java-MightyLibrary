package MightyLibrary.mightylib.graphics.lightning;

import MightyLibrary.mightylib.graphics.lightning.lights.DirectionalLight;
import MightyLibrary.mightylib.graphics.lightning.lights.PointLight;
import MightyLibrary.mightylib.graphics.lightning.lights.SpotLight;
import MightyLibrary.mightylib.graphics.renderer.Renderer;

import java.util.ArrayList;
import java.util.List;

public class MultiLightningManager {
    private final List<Renderer> renderers;

    private DirectionalLight directionalLight;
    private final String directionalStructName;

    private final List<PointLight> pointLights;
    private final String pointStructName;

    private SpotLight spotLight;

    private final String spotStructName;

    public MultiLightningManager(String directionalStructName, String pointStructName, String spotStructName){
        renderers = new ArrayList<>();
        pointLights = new ArrayList<>();

        this.directionalStructName = directionalStructName;
        this.pointStructName = pointStructName;
        this.spotStructName = spotStructName;
    }

    public MultiLightningManager setDirectionalLight(DirectionalLight directionalLight){
        this.directionalLight = directionalLight;

        return this;
    }

    public MultiLightningManager setSpotLight(SpotLight spotLight){
        this.spotLight = spotLight;

        return this;
    }

    public void addPointLight(PointLight pointLight){
        pointLights.add(pointLight);

        pointLight.setLightCurrentNumber(pointLights.size() - 1);
        for (Renderer renderer : renderers) {
            pointLight.addToRenderer(renderer, directionalStructName);

            renderer.updateShaderValue(pointStructName + ".numberPointLights", pointLights.size());
        }
    }

    public void addRenderer(Renderer renderer){
        renderers.add(renderer);

        directionalLight.addToRenderer(renderer, directionalStructName);
        spotLight.addToRenderer(renderer, spotStructName);

        renderer.addShaderValue("numberPointLights", Integer.class, pointLights.size());
        for (int i = 0; i < pointLights.size(); ++i) {
            pointLights.get(i).setLightCurrentNumber(i);
            pointLights.get(i).addToRenderer(renderer, pointStructName);
        }
    }

    public void update() {
        for (int i = 0; i < pointLights.size(); ++i)
            pointLights.get(i).setLightCurrentNumber(i);

        for (Renderer renderer : renderers){
            directionalLight.updateRenderer(renderer, directionalStructName);

            spotLight.updateRenderer(renderer, spotStructName);

            for (PointLight pointLight : pointLights)
                pointLight.updateRenderer(renderer, pointStructName);
        }
    }

    public SpotLight getSpotLight() {
        return spotLight;
    }
}
