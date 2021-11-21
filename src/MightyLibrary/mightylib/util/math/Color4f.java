package MightyLibrary.mightylib.util.math;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class Color4f{
    private float r;
    private float g;
    private float b;
    private float a;

    public Color4f() {
        this.r = 0.0f;
        this.g = 0.0f;
        this.b = 0.0f;
        this.a = 0.0f;
    }

    public Color4f(float color) {
        this.r = color;
        this.g = color;
        this.b = color;
        this.a = 1.0f;
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

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public float getA() {
        return a;
    }

    public void setA(float a) {
        this.a = a;
    }

    public void setColor(Vector4f color){
        this.r = color.x;
        this.g = color.y;
        this.b = color.z;
        this.a = color.w;
    }

    public void setColor(Vector3f color){
        setColor(new Vector4f(color, 1.0f));
        this.r = color.x;
        this.g = color.y;
        this.b = color.z;
        this.a = 1.0f;
    }

    public void setColor(float f){
        setColor(new Vector3f(f));
    }

    public Color4f copy(){
        return new Color4f(r, g, b, a);
    }
}
