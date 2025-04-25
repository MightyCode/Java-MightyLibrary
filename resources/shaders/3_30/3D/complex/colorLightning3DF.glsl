#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;

uniform int color;

uniform vec3 viewPos;
uniform vec3 worldColor;
uniform vec3 lightPos;
uniform vec3 lightColor;

vec4 decodeColor(int colorPacked) {
    float r = float((colorPacked >> 24) & 0xFF) / 255.0;
    float g = float((colorPacked >> 16) & 0xFF) / 255.0;
    float b = float((colorPacked >> 8) & 0xFF) / 255.0;
    float a = float(colorPacked & 0xFF) / 255.0;

    return vec4(r, g, b, a);
}

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

    vec4 decodedColor = decodeColor(color);

    vec3 result = (ambient + diffuse + specular) * decodedColor.xyz;
    FragColor = vec4(result, decodedColor.w);
}
