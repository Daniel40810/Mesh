package com.dan.rayphong;

/**
 * Unveränderlicher 2D-Vektor, ausschließlich für Textur-Koordinaten (UV) genutzt.
 * Java 8 kompatibel — keine Records.
 */
public final class Vec2 {

    public final float u, v;

    public Vec2(float u, float v) {
        this.u = u;
        this.v = v;
    }

    public static final Vec2 ZERO = new Vec2(0, 0);

    public Vec2 add(Vec2 o) {
        return new Vec2(u + o.u, v + o.v);
    }

    public Vec2 sub(Vec2 o) {
        return new Vec2(u - o.u, v - o.v);
    }

    public Vec2 scale(float s) {
        return new Vec2(u * s, v * s);
    }

    /** Lineare Interpolation zwischen zwei UV-Koordinaten, t in [0,1]. */
    public static Vec2 lerp(Vec2 a, Vec2 b, float t) {
        return new Vec2(a.u + (b.u - a.u) * t, a.v + (b.v - a.v) * t);
    }

    @Override
    public String toString() {
        return "Vec2(" + u + ", " + v + ")";
    }
}
