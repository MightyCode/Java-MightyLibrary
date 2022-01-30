package MightyLibrary.mightylib.util.math;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class Color4f{
    private float r;
    private float g;
    private float b;
    private float a;

    public Color4f() {
        this(1.0f);
    }

    public Color4f(float color) {
        this(color, color, color, 1.0f);
    }

    public Color4f(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public float getR() {
        return r;
    }

    public void setR(float r) {
        this.r = r;
    }
    public void setR255(float r) { setR(r / 255); }

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }
    public void setG255(float g) { setG(g / 255); }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }
    public void setB255(float b) { setB(b / 255); }

    public float getA() {
        return a;
    }

    public void setA(float a) {
        this.a = a;
    }
    public void setA255(float a) { setA(a / 255); }

    public void setColor(Vector4f color){
        this.r = color.x;
        this.g = color.y;
        this.b = color.z;
        this.a = color.w;
    }

    public void setColorBased256(Vector4f color){
        setColor(color.div(255f));
    }

    public void setColor(Vector3f color){
        setColor(new Vector4f(color, 1.0f));
        this.r = color.x;
        this.g = color.y;
        this.b = color.z;
        this.a = 1.0f;
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

    public Color4f copy(){
        return new Color4f(r, g, b, a);
    }
}
