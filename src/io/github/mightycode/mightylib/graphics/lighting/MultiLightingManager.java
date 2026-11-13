package io.github.mightycode.mightylib.graphics.lighting;

import io.github.mightycode.mightylib.graphics.lighting.lights.DirectionalLight;
import io.github.mightycode.mightylib.graphics.lighting.lights.PointLight;
import io.github.mightycode.mightylib.graphics.lighting.lights.SpotLight;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;

import java.util.ArrayList;
import java.util.List;

public class MultiLightingManager {
    private final List<Renderer> renderers;

    private DirectionalLight directionalLight;
    private final String directionalStructName;

    private final List<PointLight> pointLights;
    private final String pointStructName;

    private SpotLight spotLight;

    private final String spotStructName;

    public MultiLightingManager(String directionalStructName, String pointStructName, String spotStructName){
        renderers = new ArrayList<>();
        pointLights = new ArrayList<>();

        this.directionalStructName = directionalStructName;
        this.pointStructName = pointStructName;
        this.spotStructName = spotStructName;
    }

    public MultiLightingManager setDirectionalLight(DirectionalLight directionalLight){
        this.directionalLight = directionalLight;

        return this;
    }

    public MultiLightingManager setSpotLight(SpotLight spotLight){
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
