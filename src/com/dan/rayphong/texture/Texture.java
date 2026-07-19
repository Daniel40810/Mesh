package com.dan.rayphong.texture;

import com.dan.rayphong.Vec2;
import com.dan.rayphong.Vec3;

/**
 * Textur mit RGB-Pixeldaten in float[0..1] (linear, nicht sRGB-kodiert — für diesen
 * Software-Renderer reicht das; Gamma-Korrektur wäre ein separater, späterer Schritt).
 * {@link #sample(float, float)} liefert bilinear gefilterte Farbwerte, {@code sample()}-
 * Überladungen mit einem einzelnen Kanal sind für Grayscale-Maps (Specular, Roughness) gedacht.
 *
 * <p>Adressierung: {@code repeat} (Standard, für Wiederholungs-Texturen wie Boden-Material)
 * oder {@code clamp} (Rand-Pixel wird über den Rand hinaus wiederholt, für Einzel-Objekte).</p>
 *
 * <p><b>Mipmapping (seit Phase 12):</b> {@link #sampleWithDerivatives} nimmt zusätzlich die
 * Bildschirmraum-Ableitungen der UV-Koordinaten entgegen (wie stark ändert sich u/v von einem
 * Pixel zum nächsten) und wählt darüber trilinear zwischen einer lazily aufgebauten Kette
 * box-gefilterter Verkleinerungsstufen — behebt Moiré/Aliasing bei stark verkleinerten,
 * gekachelten Texturen (z. B. Boden-Material mit hohem Tiling-Faktor aus der Ferne).</p>
 */
public final class Texture {

    public enum WrapMode { REPEAT, CLAMP }

    public final String name;
    public final int width;
    public final int height;

    // Flach abgelegte RGB-Kanäle, je width*height Einträge, Reihenfolge: row-major (y*width+x)
    private final float[] r;
    private final float[] g;
    private final float[] b;

    private WrapMode wrapMode = WrapMode.REPEAT;

    // Lazily aufgebaute Mip-Kette: Index 0 = diese Textur selbst (volle Auflösung), jede weitere
    // Stufe halbiert Breite/Höhe (Box-Filter) bis 1x1. Siehe ensureMipmaps().
    private Texture[] mipLevels;

    public Texture(String name, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Textur-Dimensionen müssen positiv sein");
        }
        this.name = name;
        this.width = width;
        this.height = height;
        this.r = new float[width * height];
        this.g = new float[width * height];
        this.b = new float[width * height];
    }

    public void setWrapMode(WrapMode mode) {
        this.wrapMode = mode;
    }

    public void setPixel(int x, int y, float red, float green, float blue) {
        int idx = y * width + x;
        r[idx] = red;
        g[idx] = green;
        b[idx] = blue;
    }

    /** Bilinear gefilterte RGB-Farbe an normierten Koordinaten (u,v), typ. 0..1. */
    public Vec3 sample(float u, float v) {
        float fx = u * width - 0.5f;
        float fy = v * height - 0.5f;

        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float dx = fx - x0;
        float dy = fy - y0;

        int i00 = wrapIndex(x0, y0);
        int i10 = wrapIndex(x1, y0);
        int i01 = wrapIndex(x0, y1);
        int i11 = wrapIndex(x1, y1);

        float rr = bilerp(r[i00], r[i10], r[i01], r[i11], dx, dy);
        float gg = bilerp(g[i00], g[i10], g[i01], g[i11], dx, dy);
        float bb = bilerp(b[i00], b[i10], b[i01], b[i11], dx, dy);

        return new Vec3(rr, gg, bb);
    }

    public Vec3 sample(Vec2 uv) {
        return sample(uv.u, uv.v);
    }

    /**
     * Trilinear gefilterte RGB-Farbe: wählt anhand der Bildschirmraum-UV-Ableitungen
     * ({@code dUVdx}, {@code dUVdy} — wie stark sich u/v pro Bildschirm-Pixel ändern) die
     * passende Mip-Stufe und blendet bilinear-gefiltert zwischen den zwei nächstgelegenen
     * Stufen. Bei (nahezu) verschwindenden Ableitungen (Objekt sehr nah/frontal) identisch
     * zu {@link #sample(float, float)} auf der vollen Auflösung.
     *
     * <p>LOD-Formel (Standard-Mipmap-Heuristik): {@code rho = max(|dUVdx * (w,h)|, |dUVdy * (w,h)|)},
     * {@code lod = log2(rho)} — {@code rho} ist die ungefähre Texel-"Fußabdruckgröße" eines
     * Bildschirm-Pixels; wächst sie über 1 Texel hinaus, wird auf eine gröbere Mip-Stufe
     * ausgewichen, um Unterabtastung (Aliasing/Moiré) zu vermeiden.</p>
     */
    public Vec3 sampleWithDerivatives(float u, float v, Vec2 dUVdx, Vec2 dUVdy) {
        ensureMipmaps();

        float dxU = dUVdx.u * width;
        float dxV = dUVdx.v * height;
        float dyU = dUVdy.u * width;
        float dyV = dUVdy.v * height;
        float rhoSq = Math.max(dxU * dxU + dxV * dxV, dyU * dyU + dyV * dyV);

        int maxLevel = mipLevels.length - 1;
        if (rhoSq <= 1f) {
            return mipLevels[0].sample(u, v); // Fußabdruck <= 1 Texel -> volle Auflösung reicht
        }

        float lod = 0.5f * (float) (Math.log(rhoSq) / Math.log(2)); // 0.5*log2(rhoSq) = log2(rho)
        if (lod >= maxLevel) {
            return mipLevels[maxLevel].sample(u, v);
        }

        int lod0 = (int) Math.floor(lod);
        int lod1 = lod0 + 1;
        float t = lod - lod0;
        Vec3 c0 = mipLevels[lod0].sample(u, v);
        Vec3 c1 = mipLevels[lod1].sample(u, v);
        return Vec3.lerp(c0, c1, t);
    }

    public Vec3 sampleWithDerivatives(Vec2 uv, Vec2 dUVdx, Vec2 dUVdy) {
        return sampleWithDerivatives(uv.u, uv.v, dUVdx, dUVdy);
    }

    /** Bequemer Zugriff für Grayscale-Maps (z. B. Specular/Roughness) — nimmt den Rot-Kanal. */
    public float sampleGray(float u, float v) {
        Vec3 c = sample(u, v);
        return (c.x + c.y + c.z) / 3f;
    }

    /**
     * Dekodiert eine Standard-Tangent-Space-Normal-Map: RGB-Kanäle liegen 0..1-kodiert vor
     * ({@code encoded = normal * 0.5 + 0.5}), diese Methode macht das rückgängig und liefert
     * einen normierten Vektor mit Komponenten in [-1, 1]. Ein "neutraler" Normal-Map-Texel
     * (0.5, 0.5, 1.0) ergibt (0, 0, 1) — zeigt also unverändert entlang der Tangent-Space-Z-Achse.
     */
    public Vec3 sampleNormal(float u, float v) {
        Vec3 c = sample(u, v);
        Vec3 n = new Vec3(c.x * 2f - 1f, c.y * 2f - 1f, c.z * 2f - 1f);
        return n.normalize();
    }

    /**
     * Baut die Mip-Kette bei Bedarf einmalig auf (Box-Filter: jede Stufe = 2×2-Mittelwert der
     * vorherigen, ungerade Dimensionen klemmen am Rand statt out-of-bounds zu lesen). Wird nur
     * aufgerufen, wenn tatsächlich {@link #sampleWithDerivatives} genutzt wird — reine
     * Diffuse-lose Texturen (z. B. wenn nur {@link #sample} verwendet wird) bauen nie eine Kette.
     */
    private synchronized void ensureMipmaps() {
        if (mipLevels != null) {
            return;
        }
        java.util.List<Texture> levels = new java.util.ArrayList<Texture>();
        levels.add(this);
        Texture current = this;
        while (current.width > 1 || current.height > 1) {
            int nw = Math.max(1, current.width / 2);
            int nh = Math.max(1, current.height / 2);
            Texture next = new Texture(name + "_mip" + levels.size(), nw, nh);
            next.wrapMode = this.wrapMode;
            for (int y = 0; y < nh; y++) {
                int sy0 = Math.min(current.height - 1, y * 2);
                int sy1 = Math.min(current.height - 1, y * 2 + 1);
                for (int x = 0; x < nw; x++) {
                    int sx0 = Math.min(current.width - 1, x * 2);
                    int sx1 = Math.min(current.width - 1, x * 2 + 1);
                    float rr = (current.texelR(sx0, sy0) + current.texelR(sx1, sy0)
                            + current.texelR(sx0, sy1) + current.texelR(sx1, sy1)) / 4f;
                    float gg = (current.texelG(sx0, sy0) + current.texelG(sx1, sy0)
                            + current.texelG(sx0, sy1) + current.texelG(sx1, sy1)) / 4f;
                    float bb = (current.texelB(sx0, sy0) + current.texelB(sx1, sy0)
                            + current.texelB(sx0, sy1) + current.texelB(sx1, sy1)) / 4f;
                    next.setPixel(x, y, rr, gg, bb);
                }
            }
            levels.add(next);
            current = next;
        }
        mipLevels = levels.toArray(new Texture[0]);
    }

    private float texelR(int x, int y) {
        return r[y * width + x];
    }

    private float texelG(int x, int y) {
        return g[y * width + x];
    }

    private float texelB(int x, int y) {
        return b[y * width + x];
    }

    private int wrapIndex(int x, int y) {
        int wx, wy;
        if (wrapMode == WrapMode.REPEAT) {
            wx = Math.floorMod(x, width);
            wy = Math.floorMod(y, height);
        } else {
            wx = Math.max(0, Math.min(width - 1, x));
            wy = Math.max(0, Math.min(height - 1, y));
        }
        return wy * width + wx;
    }

    private static float bilerp(float v00, float v10, float v01, float v11, float dx, float dy) {
        float top = v00 + (v10 - v00) * dx;
        float bottom = v01 + (v11 - v01) * dx;
        return top + (bottom - top) * dy;
    }
}
