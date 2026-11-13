#version 330 core

layout (location = 0) in vec2 aPos;
layout (location = 3) in vec4 aColor;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform float time;

out vec4 vertexColor;

void main() {
    // The star breathes: the farther a vertex is from the center, the more it moves
    float pulse = 1.0 + 0.15 * sin(time * 2.0) * length(aPos);
    gl_Position = projection * view * model * vec4(aPos * pulse, 0.0, 1.0);

    // The colors rotate over time
    float shift = 0.5 + 0.5 * sin(time + aPos.x * 3.0);
    vertexColor = vec4(mix(aColor.rgb, aColor.gbr, shift), aColor.a);
}
