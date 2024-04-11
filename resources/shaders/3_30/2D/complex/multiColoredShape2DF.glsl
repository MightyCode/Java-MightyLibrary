#version 330 core

in vec4 vertexColor; // Input color from vertex shader
out vec4 FragColor;

void main() {
    FragColor = vertexColor;
}