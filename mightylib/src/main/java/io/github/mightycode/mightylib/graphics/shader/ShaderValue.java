package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.*;

/**
 * A named value for the uniforms of a shader. Its version changes at each setObject (and copyValueFrom) and at each
 * markChanged: what keeps a copy of the content compares the version instead of the content when the value is tracked
 * (trackChanges: its owner calls markChanged after each change of the content in place, like Camera for its
 * matrices). An untracked value may be changed in place without notice: compare its content.
 */
public final class ShaderValue {
    private final String name;
    private Object object;

    private final Class<?> type;
    private boolean shouldForceUpdate;
    private int version;
    private boolean tracked;

    public ShaderValue(final String a, final Class<?> type, final Object object){
        this.name = a;
        this.object = object;
        this.type = type;
    }

    // Same name, type and value (not an override of equals: a value is mutable, it isn't a map key)
    public boolean hasSameValue(final ShaderValue value){
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
        ++version;
    }

    // Changes at each setObject, copyValueFrom and markChanged
    public int version() {
        return version;
    }

    // The content was changed in place (a matrix updated): a new version
    public void markChanged() {
        ++version;
    }

    /**
     * The owner promises to call markChanged after each change of the content in place: its version is then enough to
     * know whether the content changed (BatchStateTable.camera). Set once, by the owner of the content.
     */
    public ShaderValue trackChanges() {
        tracked = true;
        return this;
    }

    public boolean isTracked() {
        return tracked;
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

    /**
     * Copy the value of source into this value without allocating, for mutable types.
     * @return false if the copy can't be done in place, a copy must be used instead
     */
    public boolean copyValueFrom(ShaderValue source) {
        if (type != source.type || object == null || source.object == null)
            return false;

        if (type == Matrix4f.class) {
            getObjectTyped(Matrix4f.class).set(source.getObjectTyped(Matrix4f.class));
        } else if (type == Vector2f.class) {
            getObjectTyped(Vector2f.class).set(source.getObjectTyped(Vector2f.class));
        } else if (type == Vector3f.class) {
            getObjectTyped(Vector3f.class).set(source.getObjectTyped(Vector3f.class));
        } else if (type == Vector4f.class) {
            getObjectTyped(Vector4f.class).set(source.getObjectTyped(Vector4f.class));
        } else if (type == Float.class || type == float.class || type == Integer.class || type == int.class) {
            // Immutable boxed values can be shared
            object = source.object;
        } else {
            return false;
        }

        ++version;
        return true;
    }

    public ShaderValue copy() {
        if (type == Float.class || type == float.class) {
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
        } else if (type == Integer.class || type == int.class) {
            return new ShaderValue(name, type, getObjectTyped(Integer.class));
        }

        return new ShaderValue(name, type, null);
    }
}
