#version 330 core

out vec3 FragPos;
out vec3 Normal;
out vec4 Color;

layout (location = 0) in vec3 aPos;
layout (location = 1) in int aColor;
layout (location = 2) in vec3 aNormal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

vec4 decodeColor(int colorPacked) {
    float r = float((colorPacked >> 24) & 0xFF) / 255.0;
    float g = float((colorPacked >> 16) & 0xFF) / 255.0;
    float b = float((colorPacked >> 8) & 0xFF) / 255.0;
    float a = float(colorPacked & 0xFF) / 255.0;

    return vec4(r, g, b, a);
}

void main() {
    FragPos = vec3(model * vec4(aPos, 1.0));

    Normal = mat3(transpose(inverse(model))) * aNormal;
    Color = decodeColor(aColor);
    gl_Position = projection * view  * vec4(FragPos, 1.0);
}
