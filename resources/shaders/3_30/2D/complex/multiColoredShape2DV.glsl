#version 330 core

layout (location = 0) in vec2 aPos;
layout (location = 1) in vec4 aColor; // Vertex attribute for color

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec4 vertexColor; // Output color to fragment shader

void main() {
    gl_Position = projection * view * model * vec4(aPos, 0.0f, 1.0f);
    vertexColor = aColor; // Pass the color attribute to fragment shader
}