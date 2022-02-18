#version 330 core
out vec4 FragColor;

in vec3 Normal;

uniform vec4 color;
uniform vec3 lightDir;
uniform vec3 worldColor;
uniform vec3 lightColor;

void main() {
    // ambient
    vec3 ambient = worldColor;

    // diffuse
    vec3 norm = normalize(Normal);
    float diff = max(dot(norm, lightDir), 0.0f);
    vec3 diffuse = diff * lightColor;

    vec3 result = (ambient + diffuse) * color.xyz;
    FragColor = vec4(result, color.w);
}
