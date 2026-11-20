package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Every shader of the engine compiles and links on an OpenGL 3.3 core context: a material draws with its own
// program, not with the fallback
@MightySceneTest(config = SceneRunnerTest.Config.class)
class EngineShadersGLTest {
    @Test
    void everyEngineShaderLinks(SceneRunner runner) {
        runner.frames(1);
        List<Material> materials = new ArrayList<>();
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            for (String name : EngineResources.shaders()) {
                Material material = new Material(name);
                material.retain();
                materials.add(material);
            }
        }

        runner.frames(3);

        try (Views.Scope ignored = Views.enter(runner.scene())) {
            for (int i = 0; i < materials.size(); ++i) {
                String name = EngineResources.shaders().get(i);
                assertEquals(name, materials.get(i).getShader().getData().getDataName(), name + " uses the fallback");
            }

            for (Material material : materials)
                material.release();
        }
    }
}
