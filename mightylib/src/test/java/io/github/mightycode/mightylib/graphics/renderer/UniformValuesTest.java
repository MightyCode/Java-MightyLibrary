package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// The values of a material and of a renderer, sent by location (found once per program)
class UniformValuesTest {
    // A program whose uniforms are those of the map; records what it is asked and sent
    private static final class FakeProgram implements IUniformSink {
        final Map<String, Integer> uniforms = new HashMap<>();
        final List<String> sent = new ArrayList<>();
        final List<String> missing = new ArrayList<>();
        int lookups;
        long stamp = 1;

        FakeProgram(String... names) {
            for (int i = 0; i < names.length; ++i)
                uniforms.put(names[i], i);
        }

        @Override public long programStamp() { return stamp; }

        @Override
        public int location(String name) {
            ++lookups;
            return uniforms.getOrDefault(name, -1);
        }

        @Override
        public void send(int location, ShaderValue value) {
            sent.add(value.getName() + "@" + location + "=" + value.getObject());
        }

        @Override public void reportMissing(String name) { missing.add(name); }
    }

    private static ShaderValue value(String name, float object) {
        return new ShaderValue(name, Float.class, object);
    }

    @Test
    void theMaterialThenTheRendererWhichWins() {
        FakeProgram program = new FakeProgram("a", "b", "c");
        UniformValues material = new UniformValues();
        UniformValues renderer = new UniformValues();
        material.put(value("a", 1), false);
        material.put(value("b", 2), false);
        renderer.put(value("b", 20), false);
        renderer.put(value("c", 30), false);

        material.send(program, renderer);
        renderer.send(program, null);

        // b of the material is not sent: the one of the renderer is
        assertEquals(List.of("a@0=1.0", "b@1=20.0", "c@2=30.0"), program.sent);
    }

    @Test
    void anAbsentOptionalValueIsNeitherSentNorReported() {
        FakeProgram program = new FakeProgram("a");
        UniformValues values = new UniformValues();
        values.put(value("a", 1), false);
        values.put(value("optional", 2), true);
        values.put(value("absent", 3), false);

        values.send(program, null);
        values.send(program, null);

        assertEquals(List.of("a@0=1.0", "a@0=1.0"), program.sent);
        // Reported when found missing, once per program
        assertEquals(List.of("absent"), program.missing);
    }

    @Test
    void locationsAreFoundOncePerProgram() {
        FakeProgram program = new FakeProgram("a", "b");
        UniformValues values = new UniformValues();
        values.put(value("a", 1), false);
        values.put(value("b", 2), false);

        values.send(program, null);
        values.send(program, null);
        assertEquals(2, program.lookups);

        // A new program (hot reload): the locations are found again
        program.stamp = 2;
        program.uniforms.put("a", 5);
        program.sent.clear();
        values.send(program, null);
        assertEquals(4, program.lookups);
        assertEquals("a@5=1.0", program.sent.get(0));

        // Another program (the fallback) too
        FakeProgram fallback = new FakeProgram("b");
        values.send(fallback, null);
        assertEquals(List.of("b@0=2.0"), fallback.sent);
        assertEquals(2, fallback.lookups);
    }

    @Test
    void aValueAddedOrReplacedIsFound() {
        FakeProgram program = new FakeProgram("a", "b");
        UniformValues values = new UniformValues();
        values.put(value("a", 1), false);
        values.send(program, null);

        values.put(value("b", 2), false);
        values.put(value("a", 3), false);
        program.sent.clear();
        values.send(program, null);

        // Kept in its place when replaced
        assertEquals(List.of("a@0=3.0", "b@1=2.0"), program.sent);
        assertTrue(values.remove("a"));
        assertFalse(values.has("a"));
        assertFalse(values.remove("a"));
    }
}
