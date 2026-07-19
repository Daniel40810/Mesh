package com.dan.rayphong;

import com.dan.rayphong.texture.Texture;

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
    private final List<? extends ShadowSource> shadowMaps; // parallel zu lights, Einträge dürfen null sein
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
     *                   Jeder Eintrag kann eine einzelne {@link ShadowMap} ODER eine
     *                   {@link CascadedShadowMap} sein — dank {@link ShadowSource} macht das
     *                   für den Shader keinen Unterschied.
     */
    public PhongShader(PhongMaterial material, Vec3 cameraPos, List<PointLight> lights,
                        List<? extends ShadowSource> shadowMaps, Color ambientColor, float ambientIntensity) {
        this.material = material;
        this.cameraPos = cameraPos;
        this.lights = lights;
        this.shadowMaps = shadowMaps;
        this.ambientColor = ambientColor;
        this.ambientIntensity = ambientIntensity;
    }

    @Override
    public int shade(Vec3 worldPos, Vec3 worldNormal) {
        return shadeLit(worldPos, worldNormal, Vec2.ZERO, Vec2.ZERO, Vec2.ZERO);
    }

    /** Texturierte Variante ohne Normal-Mapping/Ableitungen — volle Auflösung, kein Mip-Filter. */
    @Override
    public int shade(Vec3 worldPos, Vec3 worldNormal, Vec2 uv) {
        return shadeLit(worldPos, worldNormal, uv, Vec2.ZERO, Vec2.ZERO);
    }

    /**
     * Normal-Map-fähige Variante ohne UV-Ableitungen (keine Mipmap-Auswahl möglich — volle
     * Auflösung). Wird nur noch erreicht, wenn jemand direkt diesen Overload aufruft statt
     * über den {@link Rasterizer}, der inzwischen immer die 6-Parameter-Variante nutzt.
     */
    @Override
    public int shade(Vec3 worldPos, Vec3 worldNormal, Vec2 uv, Vec3 worldTangent) {
        Vec3 perturbedNormal = perturbNormalIfNeeded(worldNormal, uv, worldTangent);
        return shadeLit(worldPos, perturbedNormal, uv, Vec2.ZERO, Vec2.ZERO);
    }

    /**
     * Volle Variante inkl. Bildschirmraum-UV-Ableitungen: normal-mappt wie oben, sampelt
     * {@link PhongMaterial#diffuseMap} aber trilinear-mipgefiltert (siehe
     * {@link com.dan.rayphong.texture.Texture#sampleWithDerivatives}) — behebt Moiré bei
     * stark verkleinerten, gekachelten Texturen (z. B. Boden aus der Ferne).
     */
    @Override
    public int shade(Vec3 worldPos, Vec3 worldNormal, Vec2 uv, Vec3 worldTangent, Vec2 uvDx, Vec2 uvDy) {
        Vec3 perturbedNormal = perturbNormalIfNeeded(worldNormal, uv, worldTangent);
        return shadeLit(worldPos, perturbedNormal, uv, uvDx, uvDy);
    }

    /**
     * Verzerrt {@code worldNormal} per Tangent-Space-TBN-Matrix, falls {@link PhongMaterial#normalMap}
     * gesetzt ist — sonst unverändert. Ausgelagert aus den beiden {@code shade}-Overloads, die
     * beide (mit und ohne UV-Ableitungen) dasselbe Normal-Mapping brauchen.
     *
     * <p>Vereinfachung wie in {@link Mesh#tangents}: keine explizite Bitangenten-Händigkeit —
     * die Bitangente wird stets als {@code normal × tangent} gebildet (rechtshändig, passend
     * zu unseren nicht gespiegelten UV-Layouts).</p>
     */
    private Vec3 perturbNormalIfNeeded(Vec3 worldNormal, Vec2 uv, Vec3 worldTangent) {
        Texture normalMap = material.normalMap;
        if (normalMap == null) {
            return worldNormal;
        }

        // Gram-Schmidt: Tangente nochmal orthogonal zur (interpolierten) Normale erzwingen —
        // nach der Interpolation über ein Dreieck hinweg sind beide i. A. nicht mehr exakt 90°.
        Vec3 t = worldTangent.sub(worldNormal.scale(worldNormal.dot(worldTangent)));
        float tLen = t.length();
        if (tLen < 1e-6f) {
            return worldNormal; // entartete Tangente — Normal Mapping überspringen
        }
        t = t.scale(1f / tLen);
        Vec3 bitangent = worldNormal.cross(t);

        Vec3 sampled = normalMap.sampleNormal(uv.u, uv.v); // Tangent-Space, Komponenten in [-1,1]
        float strength = material.normalMapStrength;
        // Nur X/Y (die "Auslenkung" quer zur Oberfläche) skalieren, Z bleibt 1 — danach neu normieren.
        // strength=0 => (0,0,1) => keine Verzerrung; strength=1 => wie kodiert; >1 => verstärkt.
        return t.scale(sampled.x * strength)
                .add(bitangent.scale(sampled.y * strength))
                .add(worldNormal.scale(sampled.z))
                .normalize();
    }

    /**
     * Kern-Beleuchtung: Ambient + Diffuse/Specular je Punktlicht (mit Schatten) + Fresnel-
     * Environment-Reflexion. {@link PhongMaterial#diffuseMap} wird trilinear-mipgefiltert
     * gesampelt ({@code uvDx}/{@code uvDy} == (0,0) fällt automatisch auf volle Auflösung
     * zurück, siehe {@link com.dan.rayphong.texture.Texture#sampleWithDerivatives}).
     */
    private int shadeLit(Vec3 worldPos, Vec3 worldNormal, Vec2 uv, Vec2 uvDx, Vec2 uvDy) {
        Vec3 viewDir = cameraPos.sub(worldPos).normalize();

        float baseR = material.baseColor.getRed() / 255f;
        float baseG = material.baseColor.getGreen() / 255f;
        float baseB = material.baseColor.getBlue() / 255f;

        Texture diffuseMap = material.diffuseMap;
        if (diffuseMap != null) {
            Vec3 texel = diffuseMap.sampleWithDerivatives(uv, uvDx, uvDy);
            baseR *= texel.x;
            baseG *= texel.y;
            baseB *= texel.z;
        }

        float specularK = material.specularK;
        Texture specularMap = material.specularMap;
        if (specularMap != null) {
            specularK *= specularMap.sampleGray(uv.u, uv.v);
        }

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
                    shadowFactor = shadowMaps.get(i).sampleShadowFactor(worldPos, ndotl, cameraPos);
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
                float spec = specularK * (float) Math.pow(rdotv, material.shininess) * atten * shadowFactor;
                r += specR * lr * spec;
                g += specG * lg * spec;
                b += specB * lb * spec;
            }
        }

        // Fresnel-Environment-Reflexion: bei streifendem Blickwinkel spiegelt jede Oberfläche
        // stärker (Schlick-Näherung), unabhängig von den Punktlichtern — simuliert Umgebungslicht
        // ohne echtes Raytracing. reflectivity=0 (Standard) => Verhalten exakt wie zuvor.
        if (material.reflectivity > 0f) {
            float ndotv = Math.max(0f, worldNormal.dot(viewDir));
            float fresnel = material.fresnelF0 + (1f - material.fresnelF0) * (float) Math.pow(1f - ndotv, 5.0);
            float reflectAmount = Math.max(0f, Math.min(1f, material.reflectivity * fresnel));
            if (reflectAmount > 0f) {
                Vec3 reflectDir = viewDir.reflect(worldNormal);
                Vec3 envColor = sampleEnvironment(reflectDir);
                r = r * (1f - reflectAmount) + envColor.x * reflectAmount;
                g = g * (1f - reflectAmount) + envColor.y * reflectAmount;
                b = b * (1f - reflectAmount) + envColor.z * reflectAmount;
            }
        }

        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /**
     * Liefert die Umgebungsfarbe für eine Reflexionsrichtung. Mit {@link PhongMaterial#environmentMap}
     * wird äquirektangular gesampelt (u = Azimut, v = Polarwinkel — dieselbe Konvention wie
     * {@link MeshFactory#sphere}); ohne Environment-Map ein einfacher prozeduraler
     * Himmel/Horizont/Boden-Farbverlauf als plausibler Standard-Fallback.
     */
    private Vec3 sampleEnvironment(Vec3 dir) {
        Texture envMap = material.environmentMap;
        if (envMap != null) {
            float u = (float) (Math.atan2(dir.z, dir.x) / (2.0 * Math.PI) + 0.5);
            float clampedY = Math.max(-1f, Math.min(1f, dir.y));
            float v = (float) (Math.acos(clampedY) / Math.PI);
            return envMap.sample(u, v);
        }
        return proceduralSky(dir);
    }

    private static final Vec3 SKY_COLOR = new Vec3(0.55f, 0.70f, 0.95f);
    private static final Vec3 HORIZON_COLOR = new Vec3(0.75f, 0.78f, 0.80f);
    private static final Vec3 GROUND_COLOR = new Vec3(0.25f, 0.24f, 0.22f);

    private static Vec3 proceduralSky(Vec3 dir) {
        float y = Math.max(-1f, Math.min(1f, dir.y));
        if (y >= 0f) {
            float t = (float) Math.pow(y, 0.5); // Quadratwurzel: schnellerer Übergang nahe Horizont
            return Vec3.lerp(HORIZON_COLOR, SKY_COLOR, t);
        } else {
            float t = (float) Math.pow(-y, 0.5);
            return Vec3.lerp(HORIZON_COLOR, GROUND_COLOR, t);
        }
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
