package com.dan.rayphong;

import java.awt.Color;

/**
 * Punktlichtquelle mit klassischer quadratischer Abstandsdämpfung
 * (attenuation = 1 / (constant + linear*d + quadratic*d²)).
 */
public final class PointLight {

    public final Vec3 position;
    public final Color color;
    public final float intensity;
    public final float constantAtten;
    public final float linearAtten;
    public final float quadraticAtten;

    public PointLight(Vec3 position, Color color, float intensity) {
        this(position, color, intensity, 1.0f, 0.09f, 0.032f);
    }

    public PointLight(Vec3 position, Color color, float intensity,
                       float constantAtten, float linearAtten, float quadraticAtten) {
        this.position = position;
        this.color = color;
        this.intensity = intensity;
        this.constantAtten = constantAtten;
        this.linearAtten = linearAtten;
        this.quadraticAtten = quadraticAtten;
    }

    public float attenuationAt(Vec3 point) {
        float d = position.sub(point).length();
        float denom = constantAtten + linearAtten * d + quadraticAtten * d * d;
        return denom < 1e-6f ? 1f : 1f / denom;
    }
}
