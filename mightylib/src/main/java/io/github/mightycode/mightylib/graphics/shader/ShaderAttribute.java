package io.github.mightycode.mightylib.graphics.shader;

import java.util.Objects;

/** An active vertex attribute of a linked program (glGetActiveAttrib): its name, location and GL type. */
public final class ShaderAttribute {
    // GL types of the attributes (GL20, GL30): floats, ints, unsigned ints
    public static final int GL_FLOAT = 0x1406, GL_FLOAT_VEC2 = 0x8B50, GL_FLOAT_VEC3 = 0x8B51, GL_FLOAT_VEC4 = 0x8B52;
    public static final int GL_INT = 0x1404, GL_INT_VEC2 = 0x8B53, GL_INT_VEC3 = 0x8B54, GL_INT_VEC4 = 0x8B55;
    public static final int GL_UNSIGNED_INT = 0x1405, GL_UNSIGNED_INT_VEC2 = 0x8DC6, GL_UNSIGNED_INT_VEC3 = 0x8DC7,
            GL_UNSIGNED_INT_VEC4 = 0x8DC8;

    private final String name;
    private final int location;
    private final int glType;

    public ShaderAttribute(String name, int location, int glType) {
        this.name = Objects.requireNonNull(name, "name");
        this.location = location;
        this.glType = glType;
    }

    public String name() {
        return name;
    }

    public int location() {
        return location;
    }

    public int glType() {
        return glType;
    }

    // Components of a float or int scalar or vector, 0 for any other type (matrices...)
    public int components() {
        switch (glType) {
            case GL_FLOAT:
            case GL_INT:
            case GL_UNSIGNED_INT:
                return 1;
            case GL_FLOAT_VEC2:
            case GL_INT_VEC2:
            case GL_UNSIGNED_INT_VEC2:
                return 2;
            case GL_FLOAT_VEC3:
            case GL_INT_VEC3:
            case GL_UNSIGNED_INT_VEC3:
                return 3;
            case GL_FLOAT_VEC4:
            case GL_INT_VEC4:
            case GL_UNSIGNED_INT_VEC4:
                return 4;
            default:
                return 0;
        }
    }

    // A float scalar or vector
    public boolean isFloat() {
        return glType == GL_FLOAT || (glType >= GL_FLOAT_VEC2 && glType <= GL_FLOAT_VEC4);
    }

    // A signed or unsigned int scalar or vector
    public boolean isInt() {
        return glType == GL_INT || glType == GL_UNSIGNED_INT || (glType >= GL_INT_VEC2 && glType <= GL_INT_VEC4)
                || (glType >= GL_UNSIGNED_INT_VEC2 && glType <= GL_UNSIGNED_INT_VEC4);
    }

    @Override
    public String toString() {
        return name + "@" + location + " (0x" + Integer.toHexString(glType) + ")";
    }
}
