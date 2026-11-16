package io.github.mightycode.mightylib.graphics.lighting.lights;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

// A clone of a light is a light of the same class, with the same settings and its own vectors
// (SpotLight.clone gave a PointLight, the clones lost their position and shared their vectors)
class LightCopyTest {
    // The lights have no getter for every setting: read the fields
    private static Object field(Object light, String name) throws ReflectiveOperationException {
        Field field = light.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(light);
    }

    private static final LightDecrease DECREASE = new LightDecrease(1, 0.09f, 0.032f);

    @Test
    void spotLightCloneIsASpotLightWithItsSettings() throws ReflectiveOperationException {
        Vector3f position = new Vector3f(1, 2, 3);
        Vector3f direction = new Vector3f(0, -1, 0);
        SpotLight original = new SpotLight(new Vector3f(0.1f), new Vector3f(0.5f), new Vector3f(1), DECREASE)
                .setPosition(position).setDirection(direction).setCutOff(0.9f).setOuterCutOff(0.8f)
                .disableDistance().enable();

        SpotLight copy = original.copy();

        assertEquals(position, field(copy, "position"));
        assertNotSame(position, field(copy, "position"));
        assertEquals(direction, field(copy, "direction"));
        assertNotSame(direction, field(copy, "direction"));
        assertEquals(0.9f, field(copy, "cutOff"));
        assertEquals(0.8f, field(copy, "outerCutOff"));
        assertEquals(false, field(copy, "shouldUseDistance"));
        assertEquals(true, field(copy, "enabled"));
        assertSame(DECREASE, field(copy, "lightDecrease"));
        assertNotSame(original.Diffuse, copy.Diffuse);
    }

    @Test
    void pointLightCloneKeepsItsPositionAndItsDecrease() throws ReflectiveOperationException {
        PointLight original = new PointLight(new Vector3f(0.1f), new Vector3f(0.5f), new Vector3f(1), DECREASE)
                .setPosition(new Vector3f(4, 5, 6)).disableDistance();
        original.setLightCurrentNumber(2);

        PointLight copy = original.copy();

        assertEquals(new Vector3f(4, 5, 6), field(copy, "position"));
        assertNotSame(field(original, "position"), field(copy, "position"));
        assertEquals(false, field(copy, "shouldUseDistance"));
        assertEquals(2, field(copy, "currentNumber"));
        // The distance can be enabled again on the copy: it kept its decrease
        assertEquals(true, field(copy.enableDistance(), "shouldUseDistance"));
    }

    @Test
    void directionalLightCloneHasItsOwnDirection() {
        DirectionalLight original = new DirectionalLight(new Vector3f(0.1f), new Vector3f(0.5f), new Vector3f(1))
                .setDirection(new Vector3f(-0.2f, -1, -0.3f));

        DirectionalLight copy = original.copy();
        copy.getDirection().set(0);

        assertEquals(new Vector3f(-0.2f, -1, -0.3f), original.getDirection());
    }
}
