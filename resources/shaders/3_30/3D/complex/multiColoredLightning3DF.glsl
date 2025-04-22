#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec4 Color;

uniform vec3 viewPos;
uniform vec3 worldColor;
uniform vec3 lightPos;
uniform vec3 lightColor;

void main() {
    // ambient
    float ambientStrengh = 0.5f;
    vec3 ambient = worldColor * ambientStrengh;

    // diffuse
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0f);
    vec3 diffuse = diff * lightColor;

    float specularStrength = 0.5;
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 128);
    vec3 specular = specularStrength * spec * lightColor;

    vec3 result = (ambient + diffuse + specular) * Color.xyz;
    FragColor = vec4(result, Color.w);
}
