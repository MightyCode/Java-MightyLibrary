package io.github.mightycode.mightylib.inputs;

// Shortcuts of the input tests
public final class TestInputs {
    private TestInputs() {}

    // A frozen registry declaring the given keys (InputAction or InputAxis), without default binding
    public static InputRegistry Registry(Object... keys) {
        InputRegistry registry = new InputRegistry();
        for (Object key : keys) {
            if (key instanceof InputAction)
                registry.declareAction((InputAction) key, null);
            else
                registry.declareAxis((InputAxis) key, null);
        }

        return registry.freeze();
    }
}
