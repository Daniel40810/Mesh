package com.dan.rayphong;

import java.awt.image.BufferedImage;

/**
 * Software-Rasterizer: Dreiecke aus einem {@link Mesh} über Model/View/Projection-Matrizen
 * in ein {@link BufferedImage} rendern, mit Z-Buffer und perspektivisch korrekter
 * Attribut-Interpolation (Weltposition + Weltnormale) je Pixel.
 *
 * <p>Die eigentliche Farbberechnung liegt beim übergebenen {@link FragmentShader} —
 * der Rasterizer selbst kennt keine Beleuchtung. So kann Phase 2 (Phong) und Phase 3
 * (Shadow Maps) den Shader austauschen, ohne die Rasterisierung anzufassen.</p>
 */
public final class Rasterizer {

    private Rasterizer() {
    }

    /** Berechnet die Fragmentfarbe aus interpolierter Weltposition und -normale. */
    public interface FragmentShader {
        /** @return ARGB-Farbwert (wie {@link java.awt.Color#getRGB()}) */
        int shade(Vec3 worldPos, Vec3 worldNormal);
    }

    /**
     * Rendert das Mesh additiv NICHT — überschreibt Pixel im Ziel, deren Tiefe näher ist
     * als der aktuelle Z-Buffer-Wert. zBuffer muss vor dem ersten Aufruf mit
     * {@link Float#POSITIVE_INFINITY} initialisiert werden (siehe {@link #clearZBuffer(float[])}).
     */
    public static void render(Mesh mesh, Mat4 model, Mat4 view, Mat4 projection,
                               BufferedImage target, float[] zBuffer, FragmentShader shader) {
        int width = target.getWidth();
        int height = target.getHeight();

        int[] faces = mesh.faces;
        int triCount = faces.length / 3;

        // Vorab: alle Vertices einmal in Welt-/View-/Clip-Raum transformieren (kein erneutes
        // Transformieren geteilter Vertices pro Dreieck).
        int vCount = mesh.positions.length;
        Vec3[] worldPos = new Vec3[vCount];
        Vec3[] worldNormal = new Vec3[vCount];
        Vec3[] viewPos = new Vec3[vCount];
        Vec4[] clipPos = new Vec4[vCount];

        for (int i = 0; i < vCount; i++) {
            worldPos[i] = model.transform(mesh.positions[i], 1f).toVec3PerspectiveDivide();
            worldNormal[i] = model.transformDirection(mesh.normals[i]).normalize();
            viewPos[i] = view.transform(worldPos[i], 1f).toVec3PerspectiveDivide();
            clipPos[i] = projection.transform(viewPos[i], 1f);
        }

        for (int t = 0; t < triCount; t++) {
            int i0 = faces[t * 3];
            int i1 = faces[t * 3 + 1];
            int i2 = faces[t * 3 + 2];

            // Backface-Culling im View-Raum: Kamera sitzt im Ursprung und blickt entlang -Z.
            // Ein Dreieck ist rückseitig, wenn seine Normale (aus der Wickelreihenfolge) nicht
            // zur Kamera zeigt.
            Vec3 v0 = viewPos[i0];
            Vec3 v1 = viewPos[i1];
            Vec3 v2 = viewPos[i2];
            Vec3 faceNormalView = v1.sub(v0).cross(v2.sub(v0));
            if (faceNormalView.dot(v0) >= 0f) {
                continue; // zeigt von der Kamera weg
            }

            Vec4 c0 = clipPos[i0];
            Vec4 c1 = clipPos[i1];
            Vec4 c2 = clipPos[i2];

            // Trivialer Verwerf, falls ein Vertex hinter der Kamera liegt (w <= 0) — echtes
            // Clipping folgt in einer späteren Phase.
            if (c0.w <= 0f || c1.w <= 0f || c2.w <= 0f) {
                continue;
            }

            float invW0 = 1f / c0.w;
            float invW1 = 1f / c1.w;
            float invW2 = 1f / c2.w;

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
                continue; // entartetes Dreieck
            }

            int minX = clampX((int) Math.floor(Math.min(sx0, Math.min(sx1, sx2))), width);
            int maxX = clampX((int) Math.ceil(Math.max(sx0, Math.max(sx1, sx2))), width);
            int minY = clampY((int) Math.floor(Math.min(sy0, Math.min(sy1, sy2))), height);
            int maxY = clampY((int) Math.ceil(Math.max(sy0, Math.max(sy1, sy2))), height);

            for (int py = minY; py < maxY; py++) {
                for (int px = minX; px < maxX; px++) {
                    float sampleX = px + 0.5f;
                    float sampleY = py + 0.5f;

                    float w0 = edgeFunction(sx1, sy1, sx2, sy2, sampleX, sampleY) / area;
                    float w1 = edgeFunction(sx2, sy2, sx0, sy0, sampleX, sampleY) / area;
                    float w2 = edgeFunction(sx0, sy0, sx1, sy1, sampleX, sampleY) / area;

                    if (w0 < 0f || w1 < 0f || w2 < 0f) {
                        continue; // außerhalb des Dreiecks
                    }

                    // Tiefe: ndc.z ist affin im Screen-Raum interpolierbar (Standard-Technik).
                    float depth = w0 * ndc0.z + w1 * ndc1.z + w2 * ndc2.z;

                    int zIdx = py * width + px;
                    if (depth >= zBuffer[zIdx]) {
                        continue; // etwas Näheres ist schon da
                    }

                    // Perspektivisch korrekte Interpolation von Weltposition & -normale.
                    float interpInvW = w0 * invW0 + w1 * invW1 + w2 * invW2;
                    float pw0 = w0 * invW0 / interpInvW;
                    float pw1 = w1 * invW1 / interpInvW;
                    float pw2 = w2 * invW2 / interpInvW;

                    Vec3 pWorld = worldPos[i0].scale(pw0)
                            .add(worldPos[i1].scale(pw1))
                            .add(worldPos[i2].scale(pw2));

                    Vec3 nWorld = worldNormal[i0].scale(pw0)
                            .add(worldNormal[i1].scale(pw1))
                            .add(worldNormal[i2].scale(pw2))
                            .normalize();

                    int argb = shader.shade(pWorld, nWorld);

                    zBuffer[zIdx] = depth;
                    target.setRGB(px, py, argb);
                }
            }
        }
    }

    public static void clearZBuffer(float[] zBuffer) {
        java.util.Arrays.fill(zBuffer, Float.POSITIVE_INFINITY);
    }

    private static float edgeFunction(float ax, float ay, float bx, float by, float cx, float cy) {
        return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
    }

    private static int clampX(int v, int width) {
        return Math.max(0, Math.min(width, v));
    }

    private static int clampY(int v, int height) {
        return Math.max(0, Math.min(height, v));
    }
}
