package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.utils.math.Color4f;
import org.joml.*;

public class ShaderValue implements Cloneable {
    protected final String name;
    protected Object object;

    protected final Class<?> type;
    private boolean shouldForceUpdate;

    public ShaderValue(final String a, final Class<?> type, final Object object){
        this.name = a;
        this.object = object;
        this.type = type;
    }

    public boolean equals(final ShaderValue value){
        if (value == null)
            return false;

        if (value.object == null || object == null) {
            return (value.object == null && object == null);
        }

        if (!value.name.equals(name))
            return false;

        if (type != value.type)
            return false;

        return testEquals(type, value);
    }

    private boolean testEquals(final Class<?> type, final ShaderValue b){
        /*if (type == Vector2f.class) {
            return new ShaderValue(name, type, new Vector2f((Vector2f) object));
        } else if (type == Vector3f.class) {
            return new ShaderValue(name, type, new Vector3f((Vector3f) object));
        } else if (type == Vector4f.class) {
            return new ShaderValue(name, type, new Vector4f((Vector4f) object));
        } else if (type == Vector2i.class) {
            return new ShaderValue(name, type, new Vector2i((Vector2i) object));
        } else if (type == Vector3i.class) {
            return new ShaderValue(name, type, new Vector3i((Vector3i) object));
        } else if (type == Vector4i.class) {
            return new ShaderValue(name, type, new Vector4i((Vector4i) object));
        } else if (type == FloatBuffer.class) {
            return new ShaderValue(name, type, ((FloatBuffer)object).duplicate());
        } else if (type == Integer.class) {
            return new ShaderValue(name, type, (Integer) object);
        }*/

        return getObjectTyped(type).equals(b.getObjectTyped(type));
    }

    public String getName() {
        return name;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void forceUpdate(){
        shouldForceUpdate = true;
    }

    public boolean shouldForceUpdate(){
        return shouldForceUpdate;
    }

    public void resetForceUpdate(){
        shouldForceUpdate = false;
    }

    public Class<?> getType() {
        return type;
    }

    public <T> T getObjectTyped(Class<T> objectType){
        return objectType.cast(object);
    }

    @Override
    public ShaderValue clone(){
        if (type == Float.class) {
            return new ShaderValue(name, type, getObjectTyped(Float.class));
        } else if (type == Vector2f.class) {
            return new ShaderValue(name, type, new Vector2f(getObjectTyped(Vector2f.class)));
        } else if (type == Vector3f.class) {
            return new ShaderValue(name, type, new Vector3f(getObjectTyped(Vector3f.class)));
        } else if (type == Vector4f.class) {
            return new ShaderValue(name, type, new Vector4f(getObjectTyped(Vector4f.class)));
        } else if (type == Color4f.class) {
            Color4f obj = getObjectTyped(Color4f.class);
            return new ShaderValue(name, type, new Color4f(obj.getR(), obj.getG(), obj.getB(), obj.getA()));
        } else if (type == Vector2i.class) {
            return new ShaderValue(name, type, new Vector2i(getObjectTyped(Vector2i.class)));
        } else if (type == Vector3i.class) {
            return new ShaderValue(name, type, new Vector3i(getObjectTyped(Vector3i.class)));
        } else if (type == Vector4i.class) {
            return new ShaderValue(name, type, new Vector4i(getObjectTyped(Vector4i.class)));
        } else if (type == Matrix4f.class) {
            return new ShaderValue(name, type, new Matrix4f(getObjectTyped(Matrix4f.class)));
        } else if (type == Integer.class) {
            return new ShaderValue(name, type, getObjectTyped(Integer.class));
        }

        return new ShaderValue(name, type, null);
    }
}
