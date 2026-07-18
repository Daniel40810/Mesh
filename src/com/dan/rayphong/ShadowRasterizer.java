package com.dan.rayphong;

/**
 * Rasterisiert nur Tiefe (keine Farbe, kein Shader) aus Sicht einer Licht-Kamera —
 * der Depth-Pass für {@link ShadowMap}. Bewusst als eigene, schlanke Schleife statt
 * {@link Rasterizer} wiederzuverwenden: kein BufferedImage, kein Fragment-Shader-Aufruf,
 * nur Z-Test + Schreiben.
 */
public final class ShadowRasterizer {

    private ShadowRasterizer() {
    }

    /**
     * Rendert die Tiefe eines Meshes in den {@code depthBuffer} (Länge width*height, vorab mit
     * {@link Float#POSITIVE_INFINITY} befüllt). Gleiche Backface-Culling-Logik wie der
     * Farb-Rasterizer, hier aus Sicht des Lichts statt der Kamera.
     */
    public static void renderDepth(Mesh mesh, Mat4 model, Mat4 lightView, Mat4 lightProjection,
                                    int width, int height, float[] depthBuffer) {
        int vCount = mesh.positions.length;
        Vec3[] lightSpacePos = new Vec3[vCount];
        Vec4[] clipPos = new Vec4[vCount];

        for (int i = 0; i < vCount; i++) {
            Vec3 worldPos = model.transform(mesh.positions[i], 1f).toVec3PerspectiveDivide();
            lightSpacePos[i] = lightView.transform(worldPos, 1f).toVec3PerspectiveDivide();
            clipPos[i] = lightProjection.transform(lightSpacePos[i], 1f);
        }

        int[] faces = mesh.faces;
        int triCount = faces.length / 3;

        for (int t = 0; t < triCount; t++) {
            int i0 = faces[t * 3];
            int i1 = faces[t * 3 + 1];
            int i2 = faces[t * 3 + 2];

            Vec3 v0 = lightSpacePos[i0];
            Vec3 v1 = lightSpacePos[i1];
            Vec3 v2 = lightSpacePos[i2];
            Vec3 faceNormal = v1.sub(v0).cross(v2.sub(v0));
            if (faceNormal.dot(v0) >= 0f) {
                continue; // vom Licht abgewandt
            }

            Vec4 c0 = clipPos[i0];
            Vec4 c1 = clipPos[i1];
            Vec4 c2 = clipPos[i2];
            if (c0.w <= 0f || c1.w <= 0f || c2.w <= 0f) {
                continue;
            }

            Vec3 ndc0 = c0.toVec3PerspectiveDivide();
            Vec3 ndc1 = c1.toVec3PerspectiveDivide();
            Vec3 ndc2 = c2.toVec3PerspectiveDivide();

            float sx0 = (ndc0.x * 0.5f + 0.5f) * width;
            float sy0 = (1f - (ndc0.y * 0.5f + 0.5f)) * height;
            float sx1 = (ndc1.x * 0.5f + 0.5f) * width;
            float sy1 = (1f - (ndc1.y * 0.5f + 0.5f)) * height;
            float sx2 = (ndc2.x * 0.5f + 0.5f) * width;
            float sy2 = (1f - (ndc2.y * 0.5f + 0.5f)) * height;

            float area = edgeFunction(sx0, sy0, sx1, sy1, sx2, sy2);
            if (Math.abs(area) < 1e-6f) {
                continue;
            }

            int minX = clamp((int) Math.floor(Math.min(sx0, Math.min(sx1, sx2))), width);
            int maxX = clamp((int) Math.ceil(Math.max(sx0, Math.max(sx1, sx2))), width);
            int minY = clamp((int) Math.floor(Math.min(sy0, Math.min(sy1, sy2))), height);
            int maxY = clamp((int) Math.ceil(Math.max(sy0, Math.max(sy1, sy2))), height);

            for (int py = minY; py < maxY; py++) {
                for (int px = minX; px < maxX; px++) {
                    float sampleX = px + 0.5f;
                    float sampleY = py + 0.5f;

                    float w0 = edgeFunction(sx1, sy1, sx2, sy2, sampleX, sampleY) / area;
                    float w1 = edgeFunction(sx2, sy2, sx0, sy0, sampleX, sampleY) / area;
                    float w2 = edgeFunction(sx0, sy0, sx1, sy1, sampleX, sampleY) / area;

                    if (w0 < 0f || w1 < 0f || w2 < 0f) {
                        continue;
                    }

                    float depth = w0 * ndc0.z + w1 * ndc1.z + w2 * ndc2.z;
                    int idx = py * width + px;
                    if (depth < depthBuffer[idx]) {
                        depthBuffer[idx] = depth;
                    }
                }
            }
        }
    }

    private static float edgeFunction(float ax, float ay, float bx, float by, float cx, float cy) {
        return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }
}
