package com.dan.rayphong.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lädt Texturen von der Platte (PNG/JPG/... — alles was {@link ImageIO} versteht) und hält
 * sie in einem Cache, damit ein Auto-Rotate-Timer oder wiederholte Renders nicht dieselbe
 * Datei erneut dekodieren (gleiches Muster wie {@code RayPhongRenderer.OBJ_CACHE}).
 *
 * <p>Eine Instanz pro Anwendung/Editor-Session reicht; {@link #get(String)} liefert bei
 * unbekanntem Namen eine weiße 1x1-Fallback-Textur statt {@code null} — Fragment-Shader
 * müssen dadurch nicht auf {@code null} prüfen.</p>
 */
public final class TextureManager {

    private final Map<String, Texture> cache = new HashMap<String, Texture>();
    private final Texture fallback;

    public TextureManager() {
        this.fallback = createSolidTexture("__fallback_white", 1f, 1f, 1f);
    }

    /**
     * Lädt eine Textur von der angegebenen Datei, sofern noch nicht im Cache. Der Dateipfad
     * selbst dient als Cache-Schlüssel.
     */
    public Texture loadFromFile(String filePath) throws IOException {
        Texture cached = cache.get(filePath);
        if (cached != null) {
            return cached;
        }
        BufferedImage img = ImageIO.read(new File(filePath));
        if (img == null) {
            throw new IOException("Konnte Bilddatei nicht dekodieren: " + filePath);
        }
        Texture tex = fromBufferedImage(filePath, img);
        cache.put(filePath, tex);
        return tex;
    }

    /** Wandelt ein bereits geladenes {@link BufferedImage} in eine {@link Texture} um. */
    public Texture fromBufferedImage(String name, BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        Texture tex = new Texture(name, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                float rr = ((rgb >> 16) & 0xFF) / 255f;
                float gg = ((rgb >> 8) & 0xFF) / 255f;
                float bb = (rgb & 0xFF) / 255f;
                tex.setPixel(x, y, rr, gg, bb);
            }
        }
        return tex;
    }

    /** Einfarbige Textur — nützlich als Platzhalter oder für einfache getönte Flächen. */
    public Texture createSolidTexture(String name, float r, float g, float b) {
        Texture tex = new Texture(name, 1, 1);
        tex.setPixel(0, 0, r, g, b);
        cache.put(name, tex);
        return tex;
    }

    /** Liefert die Textur zum Namen/Pfad, oder die weiße Fallback-Textur falls nicht im Cache. */
    public Texture get(String name) {
        Texture tex = cache.get(name);
        return tex != null ? tex : fallback;
    }

    public Texture fallback() {
        return fallback;
    }

    public boolean isLoaded(String name) {
        return cache.containsKey(name);
    }

    public void clear() {
        cache.clear();
    }
}
