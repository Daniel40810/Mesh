package com.dan.rayphong;

import java.awt.Color;
import java.util.List;

/**
 * Voller Phong-Shader: globales Ambient-Licht + beliebig viele {@link PointLight}s
 * (Diffuse + Specular je Licht, mit Abstandsdämpfung). Ersetzt den einfachen
 * {@link DirectionalLambertShader} aus Phase 1 — der Rasterizer selbst ändert sich nicht,
 * da nur ein neuer {@link Rasterizer.FragmentShader} eingesetzt wird.
 */
public final class PhongShader implements Rasterizer.FragmentShader {

    private final PhongMaterial material;
    private final Vec3 cameraPos;
    private final List<PointLight> lights;
    private final List<ShadowMap> shadowMaps; // parallel zu lights, Einträge dürfen null sein
    private final Color ambientColor;
    private final float ambientIntensity;

    /** Ohne Schatten (wie Phase 2) — bequemer Konstruktor für Licht-Setups ohne Shadow-Map. */
    public PhongShader(PhongMaterial material, Vec3 cameraPos, List<PointLight> lights,
                        Color ambientColor, float ambientIntensity) {
        this(material, cameraPos, lights, null, ambientColor, ambientIntensity);
    }

    /**
     * @param shadowMaps parallele Liste zu {@code lights} (gleiche Länge oder {@code null}).
     *                   Ein {@code null}-Eintrag bedeutet: dieses Licht wirft keinen Schatten.
     */
    public PhongShader(PhongMaterial material, Vec3 cameraPos, List<PointLight> lights,
                        List<ShadowMap> shadowMaps, Color ambientColor, float ambientIntensity) {
        this.material = material;
        this.cameraPos = cameraPos;
        this.lights = lights;
        this.shadowMaps = shadowMaps;
        this.ambientColor = ambientColor;
        this.ambientIntensity = ambientIntensity;
    }

    @Override
    public int shade(Vec3 worldPos, Vec3 worldNormal) {
        Vec3 viewDir = cameraPos.sub(worldPos).normalize();

        float baseR = material.baseColor.getRed() / 255f;
        float baseG = material.baseColor.getGreen() / 255f;
        float baseB = material.baseColor.getBlue() / 255f;

        float specR = material.specularColor.getRed() / 255f;
        float specG = material.specularColor.getGreen() / 255f;
        float specB = material.specularColor.getBlue() / 255f;

        // Ambient-Term: globales Licht, unabhängig von Normale/Blickrichtung.
        float r = baseR * (ambientColor.getRed() / 255f) * ambientIntensity * material.ambientK;
        float g = baseG * (ambientColor.getGreen() / 255f) * ambientIntensity * material.ambientK;
        float b = baseB * (ambientColor.getBlue() / 255f) * ambientIntensity * material.ambientK;

        for (int i = 0; i < lights.size(); i++) {
            PointLight light = lights.get(i);

            Vec3 toLight = light.position.sub(worldPos);
            float dist = toLight.length();
            if (dist < 1e-6f) {
                continue;
            }
            Vec3 lightDir = toLight.scale(1f / dist); // normiert, Punkt -> Licht

            float atten = light.attenuationAt(worldPos) * light.intensity;
            if (atten <= 0f) {
                continue;
            }

            float lr = light.color.getRed() / 255f;
            float lg = light.color.getGreen() / 255f;
            float lb = light.color.getBlue() / 255f;

            // Diffuse (Lambert)
            float ndotl = Math.max(0f, worldNormal.dot(lightDir));
            if (ndotl > 0f) {
                float shadowFactor = 1f;
                if (shadowMaps != null && i < shadowMaps.size() && shadowMaps.get(i) != null) {
                    shadowFactor = shadowMaps.get(i).sampleShadowFactor(worldPos, ndotl);
                }
                if (shadowFactor <= 0f) {
                    continue; // voll verschattet — dieses Licht trägt nichts bei
                }

                float diff = material.diffuseK * ndotl * atten * shadowFactor;
                r += baseR * lr * diff;
                g += baseG * lg * diff;
                b += baseB * lb * diff;

                // Specular (Phong): nur relevant, wenn die Fläche überhaupt beleuchtet ist
                Vec3 reflectDir = lightDir.reflect(worldNormal);
                float rdotv = Math.max(0f, reflectDir.dot(viewDir));
                float spec = material.specularK * (float) Math.pow(rdotv, material.shininess) * atten * shadowFactor;
                r += specR * lr * spec;
                g += specG * lg * spec;
                b += specB * lb * spec;
            }
        }

        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
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
