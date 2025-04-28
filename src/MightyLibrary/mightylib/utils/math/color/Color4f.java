package MightyLibrary.mightylib.utils.math.color;

import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;

public class Color4f extends Vector4f {

    public Color4f() {
        this(1.0f);
    }

    public Color4f(float color) {
        this(color, color, color, 1.0f);
    }

    public Color4f(Vector3f color, float alpha) {
        this(color.x, color.y, color.z, alpha);
    }

    public Color4f(float r, float g, float b, float a) {
        super(r, g, b, a);
    }

    public Color4f(Color awtColor) {
        super(
                awtColor.getRed() / 255.0f,
                awtColor.getGreen() / 255.0f,
                awtColor.getBlue() / 255.0f,
                awtColor.getAlpha() / 255.0f
        );
    }

    public Color4f(String hexCode) {
        super();
        fromHex(hexCode);
    }

    private void fromHex(String hexCode) {
        if (hexCode.length() != 7 && hexCode.length() != 9) {
            throw new IllegalArgumentException("Invalid hex color code: " + hexCode);
        }

        x = Integer.parseInt(hexCode.substring(1, 3), 16) / 255f;
        y = Integer.parseInt(hexCode.substring(3, 5), 16) / 255f;
        z = Integer.parseInt(hexCode.substring(5, 7), 16) / 255f;

        if (hexCode.length() == 9) {
            w = Integer.parseInt(hexCode.substring(7, 9), 16) / 255f;
        } else {
            w = 1.0f;
        }
    }

    public float getR() {
        return x;
    }

    public void setR(float r) {
        this.x = r;
    }
    public void setR255(float r) { setR(r / 255); }

    public int getR255(){
        return (int) (x * 255);
    }

    public float getG() {
        return y;
    }

    public void setG(float g) {
        this.y = g;
    }
    public void setG255(float g) { setG(g / 255); }

    public int getG255(){
        return (int) (y * 255);
    }

    public float getB() {
        return z;
    }

    public void setB(float b) {
        this.z = b;
    }
    public void setB255(float b) { setB(b / 255); }

    public int getB255(){
        return (int) (z * 255);
    }

    public float getA() {
        return w;
    }

    public void setA(float a) {
        this.w = a;
    }
    public void setA255(float a) { setA(a / 255); }

    public int getA255(){
        return (int) (w * 255);
    }

    public void setColor(Vector4f color){
        set(color);
    }

    public void setColorBased256(Vector4f color){
        setColor(color.div(255f));
    }

    public void setColor(Vector3f color){
        setColor(new Vector4f(color, 1.0f));
    }

    public void setColorBased256(Vector3f color){
        setColor(color.div(255f));
    }

    public void setColor(float f){
        setColor(new Vector3f(f));
    }

    public void setColorBased256(float x){
        setColor(x / 255f);
    }

    public Color toColorAwt() {
        return new Color(
                clamp01(x) * 255.0f / 255.0f,
                clamp01(y) * 255.0f / 255.0f,
                clamp01(z) * 255.0f / 255.0f,
                clamp01(w) * 255.0f / 255.0f
        );
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public Color4f blend(Color4f otherColor, float amount){
        return new Color4f(
                getR() * (1 - amount) + otherColor.getR() * amount,
                getG() * (1 - amount) + otherColor.getG() * amount,
                getB() * (1 - amount) + otherColor.getB() * amount,
                getA() * (1 - amount) + otherColor.getA() * amount);
    }

    public String toHex() {
        return String.format("#%02X%02X%02X%02X",
                (int) (x * 255),
                (int) (y * 255),
                (int) (z * 255),
                (int) (w * 255));
    }

    public Vector3f toHSL() {
        float r = x;
        float g = y;
        float b = z;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float h, s, l = (max + min) / 2;

        if (max == min) {
            h = s = 0; // achromatic
        } else {
            float d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }

        return new Vector3f(h, s, l);
    }

    public int toIntRGB() {
        return ((int) (x * 255) << 16) | ((int) (y * 255) << 8) | (int) (z * 255);
    }

    public int toIntRGBA() {
        return ((int) (x * 255) << 24) | ((int) (y * 255) << 16) | ((int) (z * 255) << 8) | (int) (w * 255);
    }

    public Color4f copy(){
        return new Color4f(x, y, z, w);
    }

    @Override
    public String toString() {
        return String.format("Color4f[r=%.2f, g=%.2f, b=%.2f, a=%.2f]", getR(), getG(), getB(), getA());
    }
}
