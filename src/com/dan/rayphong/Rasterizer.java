package com.dan.rayphong;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Software-Rasterizer: Dreiecke aus einem {@link Mesh} über Model/View/Projection-Matrizen
 * in ein {@link BufferedImage} rendern, mit Z-Buffer und perspektivisch korrekter
 * Attribut-Interpolation (Weltposition + Weltnormale) je Pixel.
 *
 * <p>Die eigentliche Farbberechnung liegt beim übergebenen {@link FragmentShader} —
 * der Rasterizer selbst kennt keine Beleuchtung. So kann Phase 2 (Phong) und Phase 3
 * (Shadow Maps) den Shader austauschen, ohne die Rasterisierung anzufassen.</p>
 *
 * <p><b>Near-Plane-Clipping (seit Phase 11):</b> Dreiecke, die die Near-Plane schneiden
 * (ein Teil vor, ein Teil hinter der Kamera), werden per Sutherland-Hodgman gegen die
 * Clip-Space-Ebene {@code z + w = 0} geschnitten und als 1-2 neue Dreiecke neu trianguliert,
 * statt (wie zuvor) komplett verworfen zu werden. Das behebt die "Objekt/Ebene ragt hinter
 * die Kamera und verschwindet komplett"-Falle dauerhaft (siehe RayPhongStudio-Debugging).</p>
 */
public final class Rasterizer {

    private Rasterizer() {
    }

    /** Berechnet die Fragmentfarbe aus interpolierter Weltposition, -normale (und UV). */
    public interface FragmentShader {
        /** @return ARGB-Farbwert (wie {@link java.awt.Color#getRGB()}) */
        int shade(Vec3 worldPos, Vec3 worldNormal);

        /**
         * Überladung mit interpolierten UV-Koordinaten für texturierte Shader.
         * Default delegiert an die 2-Parameter-Variante, damit bestehende Shader
         * ({@link DirectionalLambertShader} etc.) unverändert weiterlaufen.
         */
        default int shade(Vec3 worldPos, Vec3 worldNormal, Vec2 uv) {
            return shade(worldPos, worldNormal);
        }

        /**
         * Überladung mit interpolierter Welt-Tangente für Normal-Mapping-fähige Shader.
         * Default delegiert an die 3-Parameter-Variante, damit bestehende Shader ohne
         * Normal-Map-Support unverändert weiterlaufen.
         */
        default int shade(Vec3 worldPos, Vec3 worldNormal, Vec2 uv, Vec3 worldTangent) {
            return shade(worldPos, worldNormal, uv);
        }

        /**
         * Überladung mit Bildschirmraum-UV-Ableitungen ({@code dUVdx}, {@code dUVdy} — wie
         * stark sich u/v pro Bildschirm-Pixel ändern) für Mipmap-fähige Shader (siehe
         * {@link com.dan.rayphong.texture.Texture#sampleWithDerivatives}). Default delegiert
         * an die 4-Parameter-Variante (keine Mipmap-Auswahl), damit bestehende Shader
         * unverändert weiterlaufen.
         */
        default int shade(Vec3 worldPos, Vec3 worldNormal, Vec2 uv, Vec3 worldTangent, Vec2 uvDx, Vec2 uvDy) {
            return shade(worldPos, worldNormal, uv, worldTangent);
        }
    }

    /**
     * Alle interpolierbaren Daten EINES Dreieck-Vertex, gebündelt fürs Clipping. Wird für
     * Original-Mesh-Vertices aus den vorab transformierten Arrays gebaut und für neue,
     * durch Clipping entstandene Vertices per {@link #lerp} linear interpoliert — linear in
     * (noch nicht perspektivisch dividiertem) Clip-Space ist der einzig korrekte Zeitpunkt
     * für diese Interpolation, siehe Standard-Clipping-Literatur.
     */
    private static final class ClipVertex {
        final Vec4 clip;
        final Vec3 worldPos;
        final Vec3 worldNormal;
        final Vec2 uv;
        final Vec3 tangent;

        ClipVertex(Vec4 clip, Vec3 worldPos, Vec3 worldNormal, Vec2 uv, Vec3 tangent) {
            this.clip = clip;
            this.worldPos = worldPos;
            this.worldNormal = worldNormal;
            this.uv = uv;
            this.tangent = tangent;
        }

        static ClipVertex lerp(ClipVertex a, ClipVertex b, float t) {
            Vec4 c = new Vec4(
                    a.clip.x + (b.clip.x - a.clip.x) * t,
                    a.clip.y + (b.clip.y - a.clip.y) * t,
                    a.clip.z + (b.clip.z - a.clip.z) * t,
                    a.clip.w + (b.clip.w - a.clip.w) * t
            );
            return new ClipVertex(c,
                    Vec3.lerp(a.worldPos, b.worldPos, t),
                    Vec3.lerp(a.worldNormal, b.worldNormal, t),
                    Vec2.lerp(a.uv, b.uv, t),
                    Vec3.lerp(a.tangent, b.tangent, t));
        }
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
        Vec2[] uvs = mesh.uvs; // gleiche Indizierung wie worldPos/worldNormal

        Vec3[] worldTangent = new Vec3[vCount];
        for (int i = 0; i < vCount; i++) {
            worldTangent[i] = model.transformDirection(mesh.tangents[i]).normalize();
        }

        for (int t = 0; t < triCount; t++) {
            int i0 = faces[t * 3];
            int i1 = faces[t * 3 + 1];
            int i2 = faces[t * 3 + 2];

            // Backface-Culling im View-Raum: Kamera sitzt im Ursprung und blickt entlang -Z.
            // Ein Dreieck ist rückseitig, wenn seine Normale (aus der Wickelreihenfolge) nicht
            // zur Kamera zeigt. Bewusst VOR dem Clipping (billiger früher Verwurf).
            Vec3 v0 = viewPos[i0];
            Vec3 v1 = viewPos[i1];
            Vec3 v2 = viewPos[i2];
            Vec3 faceNormalView = v1.sub(v0).cross(v2.sub(v0));
            if (faceNormalView.dot(v0) >= 0f) {
                continue; // zeigt von der Kamera weg
            }

            ClipVertex cv0 = new ClipVertex(clipPos[i0], worldPos[i0], worldNormal[i0], uvs[i0], worldTangent[i0]);
            ClipVertex cv1 = new ClipVertex(clipPos[i1], worldPos[i1], worldNormal[i1], uvs[i1], worldTangent[i1]);
            ClipVertex cv2 = new ClipVertex(clipPos[i2], worldPos[i2], worldNormal[i2], uvs[i2], worldTangent[i2]);

            boolean in0 = isInsideNear(cv0);
            boolean in1 = isInsideNear(cv1);
            boolean in2 = isInsideNear(cv2);

            if (in0 && in1 && in2) {
                // Fast-Path: komplett vor der Near-Plane, kein Clipping nötig (Normalfall).
                rasterizeTriangle(cv0, cv1, cv2, width, height, target, zBuffer, shader);
                continue;
            }
            if (!in0 && !in1 && !in2) {
                continue; // komplett hinter/auf der Near-Plane -> nichts sichtbar
            }

            // Gemischter Fall: Dreieck schneidet die Near-Plane -> sauber clippen und das
            // resultierende Polygon (3 oder 4 Ecken) als Fan neu triangulieren.
            List<ClipVertex> polygon = clipNearPlane(cv0, cv1, cv2);
            for (int k = 0; k + 2 < polygon.size(); k++) {
                rasterizeTriangle(polygon.get(0), polygon.get(k + 1), polygon.get(k + 2),
                        width, height, target, zBuffer, shader);
            }
        }
    }

    /** Innerhalb der Near-Plane (Clip-Space-Ebene {@code z + w = 0}), mit kleiner Toleranz gegen Flackern. */
    private static boolean isInsideNear(ClipVertex v) {
        return v.clip.z + v.clip.w > 1e-6f;
    }

    /** Parameter t entlang a->b, an dem {@code z + w} die Null-Ebene kreuzt. */
    private static float intersectNear(ClipVertex a, ClipVertex b) {
        float da = a.clip.z + a.clip.w;
        float db = b.clip.z + b.clip.w;
        return da / (da - db);
    }

    /**
     * Sutherland-Hodgman-Clipping eines einzelnen Dreiecks gegen die Near-Plane. Liefert ein
     * konvexes Polygon mit 0 (komplett verworfen), 3 oder 4 Ecken, in Wickelreihenfolge
     * kompatibel mit dem Original-Dreieck (Fan-Triangulierung ab Index 0 bleibt korrekt).
     */
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

    /**
     * Rasterisiert EIN (bereits near-geclipptes) Dreieck: Screen-Projektion, Bounding-Box,
     * Z-Test, perspektivisch korrekte Attribut-Interpolation, Fragment-Shader-Aufruf.
     * Enthält exakt die Logik, die vor dem Clipping-Support inline in {@link #render} stand.
     */
    private static void rasterizeTriangle(ClipVertex a, ClipVertex b, ClipVertex c,
                                           int width, int height,
                                           BufferedImage target, float[] zBuffer, FragmentShader shader) {
        Vec4 c0 = a.clip;
        Vec4 c1 = b.clip;
        Vec4 c2 = c.clip;

        // Nach dem Near-Clipping garantiert w > 0 für alle drei Ecken.
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
            return; // entartetes Dreieck
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

                Vec3 pWorld = a.worldPos.scale(pw0)
                        .add(b.worldPos.scale(pw1))
                        .add(c.worldPos.scale(pw2));

                Vec3 nWorld = a.worldNormal.scale(pw0)
                        .add(b.worldNormal.scale(pw1))
                        .add(c.worldNormal.scale(pw2))
                        .normalize();

                Vec2 uvInterp = new Vec2(
                        a.uv.u * pw0 + b.uv.u * pw1 + c.uv.u * pw2,
                        a.uv.v * pw0 + b.uv.v * pw1 + c.uv.v * pw2);

                Vec3 tWorld = a.tangent.scale(pw0)
                        .add(b.tangent.scale(pw1))
                        .add(c.tangent.scale(pw2));
                float tLen = tWorld.length();
                Vec3 tWorldNorm = tLen > 1e-6f ? tWorld.scale(1f / tLen) : tWorld;

                // UV-Bildschirmraum-Ableitungen per Finite-Differenzen (ein Pixel Versatz in
                // x bzw. y) — für Mipmap-Auswahl in Texture.sampleWithDerivatives(). Etwas
                // teurer (2x zusätzliche perspektivisch korrekte Interpolation je Pixel), aber
                // einfach korrekt statt eine fehleranfällige analytische Ableitung herzuleiten.
                Vec2 uvAtXPlus1 = interpolateUv(sx0, sy0, sx1, sy1, sx2, sy2, area,
                        invW0, invW1, invW2, a.uv, b.uv, c.uv, sampleX + 1f, sampleY);
                Vec2 uvAtYPlus1 = interpolateUv(sx0, sy0, sx1, sy1, sx2, sy2, area,
                        invW0, invW1, invW2, a.uv, b.uv, c.uv, sampleX, sampleY + 1f);
                Vec2 uvDx = new Vec2(uvAtXPlus1.u - uvInterp.u, uvAtXPlus1.v - uvInterp.v);
                Vec2 uvDy = new Vec2(uvAtYPlus1.u - uvInterp.u, uvAtYPlus1.v - uvInterp.v);

                int argb = shader.shade(pWorld, nWorld, uvInterp, tWorldNorm, uvDx, uvDy);

                zBuffer[zIdx] = depth;
                target.setRGB(px, py, argb);
            }
        }
    }

    /**
     * Wertet die perspektivisch korrekte UV-Interpolation an einem beliebigen Bildschirmpunkt
     * aus — genutzt, um per Finite-Differenzen (Punkt + 1px in x/y) die UV-Ableitungen für
     * Mipmap-Auswahl zu bestimmen (siehe Aufrufstelle in {@link #rasterizeTriangle}).
     */
    private static Vec2 interpolateUv(float sx0, float sy0, float sx1, float sy1, float sx2, float sy2,
                                       float area, float invW0, float invW1, float invW2,
                                       Vec2 uv0, Vec2 uv1, Vec2 uv2, float sampleX, float sampleY) {
        float w0 = edgeFunction(sx1, sy1, sx2, sy2, sampleX, sampleY) / area;
        float w1 = edgeFunction(sx2, sy2, sx0, sy0, sampleX, sampleY) / area;
        float w2 = edgeFunction(sx0, sy0, sx1, sy1, sampleX, sampleY) / area;

        float interpInvW = w0 * invW0 + w1 * invW1 + w2 * invW2;
        float pw0 = w0 * invW0 / interpInvW;
        float pw1 = w1 * invW1 / interpInvW;
        float pw2 = w2 * invW2 / interpInvW;

        return new Vec2(
                uv0.u * pw0 + uv1.u * pw1 + uv2.u * pw2,
                uv0.v * pw0 + uv1.v * pw1 + uv2.v * pw2);
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
