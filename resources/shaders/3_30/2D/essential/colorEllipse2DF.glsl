#version 330
struct Ellipse
{
    vec2 center;
    vec2 radius;
    float rotation;
};

uniform int color;

in Ellipse transformedEllipse;
in float screenSizeY;

out vec4 FragColor;

vec4 decodeColor(int colorPacked) {
    float r = float((colorPacked >> 24) & 0xFF) / 255.0;
    float g = float((colorPacked >> 16) & 0xFF) / 255.0;
    float b = float((colorPacked >> 8) & 0xFF) / 255.0;
    float a = float(colorPacked & 0xFF) / 255.0;

    return vec4(r, g, b, a);
}


bool pointInsideEllipse(vec2 point, Ellipse ellipse)
{
    vec2 p = point - ellipse.center;
    float cosTheta = cos(ellipse.rotation);
    float sinTheta = sin(ellipse.rotation);
    vec2 q = vec2(cosTheta * p.x + sinTheta * p.y, -sinTheta * p.x + cosTheta * p.y);

    return ((q.x * q.x) / (ellipse.radius.x * ellipse.radius.x) + (q.y * q.y) / (ellipse.radius.y * ellipse.radius.y)) <= 1.0;
}

void main()
{
    if (pointInsideEllipse(vec2(gl_FragCoord.x, screenSizeY - gl_FragCoord.y), transformedEllipse))
    {
        FragColor = decodeColor(color);
    }
    else
    {
        //FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        discard;
    }
}