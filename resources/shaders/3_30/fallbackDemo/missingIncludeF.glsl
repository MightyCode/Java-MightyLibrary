#version 330 core
out vec4 FragColor;

uniform int color;

// Missing on purpose (FallbackScene): the shader fails, the fallback shader is drawn instead.
// Creating the file loads the shader again (with the watch command on)
#include "missingLibrary.glsl"

void main() {
    FragColor = decodeColor(color);
}
