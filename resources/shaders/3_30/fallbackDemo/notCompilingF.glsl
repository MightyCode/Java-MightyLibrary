#version 330 core
out vec4 FragColor;

// Does not compile on purpose (FallbackScene): the fallback shader is drawn instead
void main() {
    FragColor = vec4(0.0, 1.0, 0.0, 1.0)
}
