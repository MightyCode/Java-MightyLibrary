#version 330

struct Rectangle {
    vec2 center;
    vec2 size;
    float rotation;
    float cornerRoundSize;
};

uniform vec4 color;

in Rectangle transformedRectangle;
in vec2 screenSize;  // Viewport dimensions in pixels [width, height]

out vec4 FragColor;

float roundedRectangleSDF(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    // Convert fragment position to NDC [-1, 1] range
    vec2 ndcPos = (gl_FragCoord.xy / screenSize) * 2.0 - 1.0;

    // Translate to rectangle's coordinate system
    vec2 pos = ndcPos - transformedRectangle.center;

    // Apply rotation
    float cosA = cos(transformedRectangle.rotation);
    float sinA = sin(transformedRectangle.rotation);
    pos = vec2(
    pos.x * cosA + pos.y * sinA,
    -pos.x * sinA + pos.y * cosA
    );

    // Convert corner radius from pixels to NDC
    float radius = transformedRectangle.cornerRoundSize * 2.0 / screenSize.y;
    vec2 halfSize = transformedRectangle.size * 0.5 - radius;

    // Calculate signed distance to rounded rectangle
    float distance = roundedRectangleSDF(pos, halfSize, radius);

    // Anti-aliasing using distance field
    float edgeWidth = fwidth(distance);
    float alpha = smoothstep(-edgeWidth, edgeWidth, -distance);

    if (alpha <= 0.0) discard;

    FragColor = vec4(color.rgb, color.a * alpha);
}