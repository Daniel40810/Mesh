package com.dan.rayphong;

/** Homogener 4D-Vektor, Ergebnis einer Mat4-Transformation. */
public final class Vec4 {

    public final float x, y, z, w;

    public Vec4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /** Perspektivische Division; liefert kartesischen Vec3. */
    public Vec3 toVec3PerspectiveDivide() {
        if (Math.abs(w) < 1e-8f) {
            return new Vec3(x, y, z);
        }
        float invW = 1f / w;
        return new Vec3(x * invW, y * invW, z * invW);
    }

    @Override
    public String toString() {
        return "Vec4(" + x + ", " + y + ", " + z + ", " + w + ")";
    }
}
