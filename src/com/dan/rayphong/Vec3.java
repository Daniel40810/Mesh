package com.dan.rayphong;

/**
 * Unveränderlicher 3D-Vektor (auch für Punkte genutzt).
 * Java 8 kompatibel — keine Records.
 */
public final class Vec3 {

    public final float x, y, z;

    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 sub(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 scale(float s) {
        return new Vec3(x * s, y * s, z * s);
    }

    public float dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(
                y * o.z - z * o.y,
                z * o.x - x * o.z,
                x * o.y - y * o.x
        );
    }

    public float length() {
        return (float) Math.sqrt(dot(this));
    }

    public Vec3 normalize() {
        float len = length();
        if (len < 1e-8f) {
            return ZERO;
        }
        return scale(1f / len);
    }

    public Vec3 negate() {
        return new Vec3(-x, -y, -z);
    }

    /**
     * Spiegelt diesen Vektor (als Richtung "zur Lichtquelle", normiert) an der Normalen {@code n}
     * (ebenfalls normiert). R = 2*(N·L)*N - L — Standard-Reflexionsformel für Phong-Specular.
     */
    public Vec3 reflect(Vec3 n) {
        float d = 2f * this.dot(n);
        return n.scale(d).sub(this);
    }

    /** Lineare Interpolation zwischen zwei Vektoren, t in [0,1]. */
    public static Vec3 lerp(Vec3 a, Vec3 b, float t) {
        return new Vec3(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }

    @Override
    public String toString() {
        return "Vec3(" + x + ", " + y + ", " + z + ")";
    }
}
