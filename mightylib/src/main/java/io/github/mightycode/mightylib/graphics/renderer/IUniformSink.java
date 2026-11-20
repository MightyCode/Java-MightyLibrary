package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.shader.ShaderValue;

// Where UniformValues send: the current program of a Shader, a recorder in the tests
interface IUniformSink {
    // Changes with each new program: the locations found before are then read again
    long programStamp();

    // -1 when the program has no uniform of this name
    int location(String name);

    // Sent to the location (skipped by the sink when it is its last value there)
    void send(int location, ShaderValue value);

    // A value whose uniform the program does not have (reported once by the sink)
    void reportMissing(String name);
}
