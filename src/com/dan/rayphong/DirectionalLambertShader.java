package com.dan.rayphong;

import java.awt.Color;

/**
 * Minimaler Shader für Phase 1: Ambient + Lambert-Diffus über ein einzelnes Richtungslicht.
 * Dient dem Rasterizer-Test, bevor in Phase 2 der volle Phong-Shader (mehrere Punktlichter,
 * Specular) hinzukommt.
 */
public final class DirectionalLambertShader implements Rasterizer.FragmentShader {

    private final Color albedo;
    private final Vec3 lightDirToSource; // zeigt VOM Objekt ZUR Lichtquelle, normiert
    private final Color lightColor;
    private final float ambient;

    public DirectionalLambertShader(Color albedo, Vec3 lightDirToSource, Color lightColor, float ambient) {
        this.albedo = albedo;
        this.lightDirToSource = lightDirToSource.normalize();
        this.lightColor = lightColor;
        this.ambient = ambient;
    }

    @Override
    public int shade(Vec3 worldPos, Vec3 worldNormal) {
        float ndotl = Math.max(0f, worldNormal.dot(lightDirToSource));

        float r = albedo.getRed() / 255f * (ambient + ndotl * (lightColor.getRed() / 255f));
        float g = albedo.getGreen() / 255f * (ambient + ndotl * (lightColor.getGreen() / 255f));
        float b = albedo.getBlue() / 255f * (ambient + ndotl * (lightColor.getBlue() / 255f));

        int ri = clamp(r);
        int gi = clamp(g);
        int bi = clamp(b);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private static int clamp(float v) {
        int i = Math.round(v * 255f);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }
}
