package com.dan.rayphong;

import java.util.ArrayList;
import java.util.List;

/**
 * Rasterisiert nur Tiefe (keine Farbe, kein Shader) aus Sicht einer Licht-Kamera —
 * der Depth-Pass für {@link ShadowMap}. Bewusst als eigene, schlanke Schleife statt
 * {@link Rasterizer} wiederzuverwenden: kein BufferedImage, kein Fragment-Shader-Aufruf,
 * nur Z-Test + Schreiben.
 *
 * <p>Näher an der Licht-Quelle als die Near-Plane liegende Dreieck-Anteile werden (wie im
 * Farb-Rasterizer, siehe {@link Rasterizer}) sauber weggeschnitten statt das ganze Dreieck
 * zu verwerfen — sonst könnten Schatten-Werfer nahe am Licht komplett aus der Shadow-Map
 * verschwinden.</p>
 */
public final class ShadowRasterizer {

    private ShadowRasterizer() {
    }

    /** Nur die für den Tiefen-Pass nötigen Daten: Clip-Position, sonst nichts zu interpolieren. */
    private static final class ClipVertex {
        final Vec4 clip;

        ClipVertex(Vec4 clip) {
            this.clip = clip;
        }

        static ClipVertex lerp(ClipVertex a, ClipVertex b, float t) {
            return new ClipVertex(new Vec4(
                    a.clip.x + (b.clip.x - a.clip.x) * t,
                    a.clip.y + (b.clip.y - a.clip.y) * t,
                    a.clip.z + (b.clip.z - a.clip.z) * t,
                    a.clip.w + (b.clip.w - a.clip.w) * t
            ));
        }
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

            ClipVertex cv0 = new ClipVertex(clipPos[i0]);
            ClipVertex cv1 = new ClipVertex(clipPos[i1]);
            ClipVertex cv2 = new ClipVertex(clipPos[i2]);

            boolean in0 = isInsideNear(cv0);
            boolean in1 = isInsideNear(cv1);
            boolean in2 = isInsideNear(cv2);

            if (in0 && in1 && in2) {
                rasterizeDepthTriangle(cv0, cv1, cv2, width, height, depthBuffer);
                continue;
            }
            if (!in0 && !in1 && !in2) {
                continue;
            }

            List<ClipVertex> polygon = clipNearPlane(cv0, cv1, cv2);
            for (int k = 0; k + 2 < polygon.size(); k++) {
                rasterizeDepthTriangle(polygon.get(0), polygon.get(k + 1), polygon.get(k + 2),
                        width, height, depthBuffer);
            }
        }
    }

    private static boolean isInsideNear(ClipVertex v) {
        return v.clip.z + v.clip.w > 1e-6f;
    }

    private static float intersectNear(ClipVertex a, ClipVertex b) {
        float da = a.clip.z + a.clip.w;
        float db = b.clip.z + b.clip.w;
        return da / (da - db);
    }

    private static List<ClipVertex> clipNearPlane(ClipVertex v0, ClipVertex v1, ClipVertex v2) {
        List<ClipVertex> input = new ArrayList<ClipVertex>(3);
        input.add(v0);
        input.add(v1);
        input.add(v2);

        List<ClipVertex> output = new ArrayList<ClipVertex>(4);
        int n = input.size();
        for (int i = 0; i < n; i++) {
            ClipVertex cur = input.get(i);
            ClipVertex next = input.get((i + 1) % n);
            boolean curInside = isInsideNear(cur);
            boolean nextInside = isInsideNear(next);

            if (curInside) {
                output.add(cur);
                if (!nextInside) {
                    output.add(ClipVertex.lerp(cur, next, intersectNear(cur, next)));
                }
            } else if (nextInside) {
                output.add(ClipVertex.lerp(cur, next, intersectNear(cur, next)));
            }
        }
        return output;
    }

    private static void rasterizeDepthTriangle(ClipVertex a, ClipVertex b, ClipVertex c,
                                                int width, int height, float[] depthBuffer) {
        Vec4 c0 = a.clip;
        Vec4 c1 = b.clip;
        Vec4 c2 = c.clip;

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
            return;
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

    private static float edgeFunction(float ax, float ay, float bx, float by, float cx, float cy) {
        return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }
}
