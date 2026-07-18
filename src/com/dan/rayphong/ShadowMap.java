package com.dan.rayphong;

import java.util.List;

/**
 * Tiefenkarte aus Sicht eines Lichts, plus PCF-Lookup ({@code sampleShadowFactor}) für den
 * Beleuchtungspass. Die Licht-View/Projection-Matrizen werden zusammen mit dem Depth-Buffer
 * gehalten, damit eine beliebige Weltposition später ins Licht-Clip-Space projiziert werden kann.
 */
public final class ShadowMap {

    private final int width;
    private final int height;
    private final float[] depth;
    private final Mat4 lightView;
    private final Mat4 lightProjection;

    /** PCF-Kernel-Radius in Texeln (2 => 5x5-Kernel). */
    private static final int PCF_RADIUS = 2;

    private ShadowMap(int width, int height, float[] depth, Mat4 lightView, Mat4 lightProjection) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.lightView = lightView;
        this.lightProjection = lightProjection;
    }

    /** Rendert den Depth-Pass für alle übergebenen Szenen-Objekte aus Sicht des Lichts. */
    public static ShadowMap render(List<SceneNode> casters, Mat4 lightView, Mat4 lightProjection,
                                    int width, int height) {
        float[] depthBuffer = new float[width * height];
        java.util.Arrays.fill(depthBuffer, Float.POSITIVE_INFINITY);

        for (int i = 0; i < casters.size(); i++) {
            SceneNode node = casters.get(i);
            ShadowRasterizer.renderDepth(node.mesh, node.model, lightView, lightProjection,
                    width, height, depthBuffer);
        }

        return new ShadowMap(width, height, depthBuffer, lightView, lightProjection);
    }

    /**
     * Schattenfaktor für einen Weltpunkt: 1.0 = voll beleuchtet, 0.0 = voll verschattet,
     * Zwischenwerte an weichen PCF-Rändern. {@code ndotl} (Normale·Lichtrichtung) steuert einen
     * neigungsabhängigen Bias, der Shadow-Acne (Selbstschatten-Flimmern) ohne sichtbares
     * Peter-Panning vermeidet.
     */
    public float sampleShadowFactor(Vec3 worldPos, float ndotl) {
        Vec3 lightSpacePos = lightView.transform(worldPos, 1f).toVec3PerspectiveDivide();
        Vec4 clip = lightProjection.transform(lightSpacePos, 1f);

        if (clip.w <= 0f) {
            return 1f; // hinter der Licht-Kamera — keine Aussage möglich, nicht verschatten
        }

        Vec3 ndc = clip.toVec3PerspectiveDivide();
        if (ndc.x < -1f || ndc.x > 1f || ndc.y < -1f || ndc.y > 1f || ndc.z < -1f || ndc.z > 1f) {
            return 1f; // außerhalb der Shadow-Map-Abdeckung — nicht verschatten
        }

        float u = (ndc.x * 0.5f + 0.5f) * width;
        float v = (1f - (ndc.y * 0.5f + 0.5f)) * height;

        float bias = Math.max(0.0025f * (1f - ndotl), 0.0008f);
        float currentDepth = ndc.z - bias;

        int cx = (int) u;
        int cy = (int) v;

        int litSamples = 0;
        int totalSamples = 0;
        for (int dy = -PCF_RADIUS; dy <= PCF_RADIUS; dy++) {
            for (int dx = -PCF_RADIUS; dx <= PCF_RADIUS; dx++) {
                int sx = cx + dx;
                int sy = cy + dy;
                if (sx < 0 || sx >= width || sy < 0 || sy >= height) {
                    continue;
                }
                totalSamples++;
                float occluderDepth = depth[sy * width + sx];
                if (currentDepth <= occluderDepth) {
                    litSamples++; // nichts Näheres als unser Punkt gefunden -> beleuchtet
                }
            }
        }

        if (totalSamples == 0) {
            return 1f;
        }
        return (float) litSamples / totalSamples;
    }
}
