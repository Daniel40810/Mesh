package com.dan.rayphong;

import java.util.List;

/**
 * Cascaded Shadow Maps (CSM): statt einer einzelnen Shadow-Map mit festem Near/Far über die
 * gesamte Szene wird das Kamera-Sichtfrustum in {@code cascadeCount} Tiefenbereiche gesplittet
 * und für jeden Bereich eine eigene, eng anliegende Shadow-Map gebaut. Ergebnis: nahe Objekte
 * bekommen viel mehr effektive Auflösung (Texel pro Weltfläche) als bei einer einzigen Map, die
 * die komplette Szene abdecken muss — klassischer Trade-off zwischen Nah- und Fernschärfe.
 *
 * <p><b>Vereinfachung gegenüber "Lehrbuch"-CSM:</b> Für gerichtete (Sonnen-)Lichter ist die
 * Licht-Projektion immer orthographisch. Unsere {@link PointLight}s sind aber echte Punktlichter
 * mit Position — hier wird die Licht-<em>Blickrichtung</em> aus {@code lightPos → sceneTarget}
 * fest für alle Kaskaden übernommen (eine Rotation), nur die orthographische Box (links/rechts/
 * oben/unten/nah/fern) wird pro Kaskade neu an das jeweilige Kamera-Frustum-Segment angepasst.
 * Das ist die im Spiele-Bereich übliche Näherung, wenn ein Licht primär als "Sonnenersatz"
 * für eine Szene mit begrenzter Ausdehnung dient (siehe {@code RayPhongScene}-Presets).</p>
 *
 * <p>Implementiert {@link ShadowSource}, wählt intern per Kamera-Abstand die passende Kaskade —
 * für {@link PhongShader} macht das keinen Unterschied zu einer einzelnen {@link ShadowMap}.</p>
 */
public final class CascadedShadowMap implements ShadowSource {

    private final ShadowMap[] cascades;
    private final float[] splitDistances; // Länge cascades.length+1, Kamera-Abstände der Kaskaden-Grenzen

    private CascadedShadowMap(ShadowMap[] cascades, float[] splitDistances) {
        this.cascades = cascades;
        this.splitDistances = splitDistances;
    }

    public int cascadeCount() {
        return cascades.length;
    }

    /** Kaskaden-Grenzen (Kamera-Abstand), z. B. für Debug-Visualisierung. */
    public float[] splitDistances() {
        return splitDistances.clone();
    }

    @Override
    public float sampleShadowFactor(Vec3 worldPos, float ndotl, Vec3 cameraPos) {
        float dist = worldPos.sub(cameraPos).length();
        int idx = cascades.length - 1;
        for (int i = 0; i < cascades.length; i++) {
            if (dist < splitDistances[i + 1]) {
                idx = i;
                break;
            }
        }
        return cascades[idx].sampleShadowFactor(worldPos, ndotl);
    }

    /** Liefert den Kaskaden-Index für einen Weltpunkt — nützlich für Debug-Einfärbung der Kaskaden. */
    public int cascadeIndexFor(Vec3 worldPos, Vec3 cameraPos) {
        float dist = worldPos.sub(cameraPos).length();
        for (int i = 0; i < cascades.length; i++) {
            if (dist < splitDistances[i + 1]) {
                return i;
            }
        }
        return cascades.length - 1;
    }

    /**
     * Baut die Kaskaden. {@code cameraForward}/{@code cameraRight}/{@code cameraUp} müssen die
     * tatsächliche Kamera-Basis sein (siehe {@link #cameraBasis(Vec3, Vec3, Vec3)}), damit die
     * berechneten Frustum-Ecken exakt dem tatsächlich sichtbaren Bereich entsprechen.
     *
     * @param cascadeCount Anzahl Kaskaden (typ. 2–4)
     * @param resolution   Texel-Auflösung JEDER einzelnen Kaskaden-Map (z. B. 1024)
     */
    public static CascadedShadowMap build(List<SceneNode> casters,
                                           Vec3 lightPos, Vec3 sceneTarget,
                                           Vec3 cameraPos, Vec3 cameraForward, Vec3 cameraRight, Vec3 cameraUp,
                                           float fovYRadians, float aspect, float cameraNear, float cameraFar,
                                           int cascadeCount, int resolution) {
        if (cascadeCount < 1) {
            throw new IllegalArgumentException("cascadeCount muss >= 1 sein");
        }
        float[] splits = computeSplitDistances(cameraNear, cameraFar, cascadeCount);

        Vec3 lightDir = sceneTarget.sub(lightPos).normalize();
        Vec3 lightUpHelper = Math.abs(lightDir.y) > 0.98f ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        // Feste Rotation für ALLE Kaskaden — nur die Projektion (Box) variiert pro Kaskade.
        Mat4 lightView = Mat4.lookAt(lightPos, sceneTarget, lightUpHelper);

        ShadowMap[] maps = new ShadowMap[cascadeCount];
        for (int i = 0; i < cascadeCount; i++) {
            float splitNear = splits[i];
            float splitFar = splits[i + 1];
            Vec3[] corners = frustumCorners(cameraPos, cameraForward, cameraRight, cameraUp,
                    fovYRadians, aspect, splitNear, splitFar);

            float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            for (Vec3 corner : corners) {
                Vec3 ls = lightView.transform(corner, 1f).toVec3PerspectiveDivide();
                minX = Math.min(minX, ls.x);
                maxX = Math.max(maxX, ls.x);
                minY = Math.min(minY, ls.y);
                maxY = Math.max(maxY, ls.y);
                minZ = Math.min(minZ, ls.z);
                maxZ = Math.max(maxZ, ls.z);
            }

            // Puffer: Schatten-Werfer außerhalb der engen Kaskaden-Box (v. a. näher am Licht,
            // "hinter" der Kamera-nahen Kaskaden-Ebene) sollen trotzdem noch Schatten werfen können.
            float margin = 3f;
            float nearPlane = Math.max(0.05f, -maxZ - margin);
            float farPlane = -minZ + margin;

            Mat4 lightProjection = Mat4.orthographic(minX, maxX, minY, maxY, nearPlane, farPlane);
            maps[i] = ShadowMap.render(casters, lightView, lightProjection, resolution, resolution);
        }

        return new CascadedShadowMap(maps, splits);
    }

    /**
     * Kamera-Basisvektoren (forward/right/up), exakt konsistent mit {@link Mat4#lookAt} —
     * wichtig, damit {@link #build} dieselbe Blickrichtung annimmt wie die tatsächlich
     * verwendete View-Matrix der Hauptkamera.
     *
     * @return {@code [forward, right, up]}
     */
    public static Vec3[] cameraBasis(Vec3 eye, Vec3 target, Vec3 worldUp) {
        Vec3 forward = target.sub(eye).normalize();
        Vec3 right = forward.cross(worldUp).normalize();
        Vec3 up = right.cross(forward);
        return new Vec3[]{forward, right, up};
    }

    /**
     * "Practical Split Scheme" (Zhang et al.): Mischung aus logarithmischer und uniformer
     * Aufteilung. Rein logarithmisch macht nahe Kaskaden extrem schmal (Detailflut ohne Nutzen),
     * rein uniform verschenkt Auflösung nah an der Kamera — {@code lambda=0.5} balanciert beides.
     */
    private static float[] computeSplitDistances(float near, float far, int count) {
        float[] splits = new float[count + 1];
        splits[0] = near;
        float lambda = 0.5f;
        for (int i = 1; i < count; i++) {
            float uniform = near + (far - near) * i / count;
            float logarithmic = (float) (near * Math.pow(far / near, (double) i / count));
            splits[i] = lambda * logarithmic + (1 - lambda) * uniform;
        }
        splits[count] = far;
        return splits;
    }

    /** 8 Weltraum-Eckpunkte des Kamera-Frustum-Segments zwischen {@code near} und {@code far}. */
    private static Vec3[] frustumCorners(Vec3 cameraPos, Vec3 forward, Vec3 right, Vec3 up,
                                          float fovYRadians, float aspect, float near, float far) {
        Vec3[] corners = new Vec3[8];
        float tanHalfFovy = (float) Math.tan(fovYRadians / 2.0);
        int idx = 0;
        float[] distances = {near, far};
        for (float dist : distances) {
            float halfHeight = dist * tanHalfFovy;
            float halfWidth = halfHeight * aspect;
            Vec3 center = cameraPos.add(forward.scale(dist));
            corners[idx++] = center.add(right.scale(-halfWidth)).add(up.scale(-halfHeight));
            corners[idx++] = center.add(right.scale(halfWidth)).add(up.scale(-halfHeight));
            corners[idx++] = center.add(right.scale(halfWidth)).add(up.scale(halfHeight));
            corners[idx++] = center.add(right.scale(-halfWidth)).add(up.scale(halfHeight));
        }
        return corners;
    }
}
