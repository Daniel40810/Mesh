package com.dan.rayphong.demo;

import com.dan.rayphong.*;
import com.dan.rayphong.texture.Texture;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Headless-Verifikation für Phase 12 (Mipmapping gegen Moiré). Ein stark gekachelter
 * Karo-Boden, der weit in die Ferne reicht — genau das Szenario, in dem hochfrequente
 * Texturen ohne Mipmap-Filterung zu Moiré/Flackern in der Distanz führen.
 *
 * <p>Zwei Renderings derselben Szene: einmal mit einem minimalen Shader, der
 * {@link Texture#sample} (kein Mip-Filter, wie vor Phase 12) direkt nutzt, einmal mit dem
 * normalen {@link PhongShader} (der seit Phase 12 automatisch über den Rasterizer die
 * Bildschirmraum-UV-Ableitungen bekommt und trilinear mipfiltert).</p>
 */
public final class Phase11MipmapDemo {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;

    public static void main(String[] args) throws IOException {
        Texture checker = buildCheckerTexture(256, 256, 32); // feine Karos -> hohes Aliasing-Risiko

        Mesh ground = MeshFactory.plane(80f, 80f, 40f, 40f); // hohes Tiling: 40 Wiederholungen
        Mat4 groundModel = Mat4.identity();

        Vec3 cameraPos = new Vec3(0f, 3.5f, 8f);
        Mat4 view = Mat4.lookAt(cameraPos, new Vec3(0, 0, -30f), new Vec3(0, 1, 0)); // flacher Winkel
        Mat4 projection = Mat4.perspective((float) Math.toRadians(55), (float) WIDTH / HEIGHT, 0.1f, 200f);

        renderWithoutMipmap(ground, groundModel, view, projection, checker, cameraPos, "phase11_no_mipmap.png");
        renderWithMipmap(ground, groundModel, view, projection, checker, cameraPos, "phase11_with_mipmap.png");

        compareDistantNoise();
    }

    /** Minimaler Shader OHNE Mip-Filter — nutzt bewusst {@link Texture#sample}, nicht sampleWithDerivatives. */
    private static void renderWithoutMipmap(Mesh ground, Mat4 groundModel, Mat4 view, Mat4 projection,
                                             Texture checker, Vec3 cameraPos, String outFile) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Vec3 lightDir = new Vec3(-0.3f, 0.9f, 0.3f);
        Rasterizer.FragmentShader shader = new Rasterizer.FragmentShader() {
            @Override
            public int shade(Vec3 worldPos, Vec3 worldNormal) {
                return shade(worldPos, worldNormal, Vec2.ZERO);
            }

            @Override
            public int shade(Vec3 worldPos, Vec3 worldNormal, Vec2 uv) {
                Vec3 texel = checker.sample(uv.u, uv.v); // KEIN Mip-Filter
                float ndotl = Math.max(0.15f, worldNormal.normalize().dot(lightDir.normalize()));
                float r = texel.x * ndotl;
                float g = texel.y * ndotl;
                float b = texel.z * ndotl;
                return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
            }

            // Bewusst KEINE Überladung mit UV-Ableitungen — Default-Kette landet bei shade(pos,normal,uv).

            private int clamp(float v) {
                int i = Math.round(v * 255f);
                return i < 0 ? 0 : Math.min(i, 255);
            }
        };

        Rasterizer.render(ground, groundModel, view, projection, image, zBuffer, shader);
        write(image, outFile);
        System.out.println(outFile + ": ohne Mip-Filter (Texture.sample direkt)");
    }

    /** Normale PhongShader-Pipeline — sampelt seit Phase 12 automatisch mipgefiltert. */
    private static void renderWithMipmap(Mesh ground, Mat4 groundModel, Mat4 view, Mat4 projection,
                                          Texture checker, Vec3 cameraPos, String outFile) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        PointLight light = new PointLight(new Vec3(-4f, 10f, 4f), Color.WHITE, 3.5f);
        List<PointLight> lights = Arrays.asList(light);
        PhongMaterial material = PhongMaterial.matte(Color.WHITE).withDiffuseMap(checker);
        PhongShader shader = new PhongShader(material, cameraPos, lights, new Color(0x30, 0x30, 0x38), 0.35f);

        Rasterizer.render(ground, groundModel, view, projection, image, zBuffer, shader);
        write(image, outFile);
        System.out.println(outFile + ": mit trilinearem Mip-Filter (PhongShader/Texture.sampleWithDerivatives)");
    }

    /**
     * Misst die hochfrequente Pixel-zu-Pixel-Varianz im FERNEN Bilddrittel (wo die Karos stark
     * verkleinert erscheinen) für beide Bilder — Moiré äußert sich als hohe lokale Varianz
     * (benachbarte Pixel springen zwischen hell/dunkel statt sanft ineinander überzugehen).
     */
    private static void compareDistantNoise() throws IOException {
        for (String name : new String[]{"phase11_no_mipmap.png", "phase11_with_mipmap.png"}) {
            BufferedImage img = ImageIO.read(new File("/home/claude/rayphong/out_render/" + name));
            int w = img.getWidth();
            double sumAbsDiff = 0;
            int n = 0;
            // Direkt unterhalb des Horizonts (empirisch ermittelt bei dieser Kamera: y~270..300) —
            // dort ist der Boden am staerksten verkleinert, also am anfaelligsten fuer Moire.
            for (int yBand = 272; yBand < 300; yBand += 2) {
                for (int x = 1; x < w - 1; x++) {
                    int g0 = (img.getRGB(x - 1, yBand) >> 16) & 0xFF;
                    int g1 = (img.getRGB(x + 1, yBand) >> 16) & 0xFF;
                    sumAbsDiff += Math.abs(g1 - g0);
                    n++;
                }
            }
            System.out.printf("%s: durchschn. |Grauwert-Sprung| zw. Nachbar-Pixeln in der Ferne = %.2f%n",
                    name, sumAbsDiff / n);
        }
        System.out.println("(Hoher Wert = Pixel springen erratisch zw. hell/dunkel = Moire. Niedriger Wert = sanfter Verlauf.)");
    }

    private static Texture buildCheckerTexture(int width, int height, int squares) {
        Texture tex = new Texture("checker", width, height);
        tex.setWrapMode(Texture.WrapMode.REPEAT);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int cx = (x * squares) / width;
                int cy = (y * squares) / height;
                boolean light = (cx + cy) % 2 == 0;
                if (light) {
                    tex.setPixel(x, y, 0.92f, 0.92f, 0.95f);
                } else {
                    tex.setPixel(x, y, 0.05f, 0.08f, 0.12f);
                }
            }
        }
        return tex;
    }

    private static BufferedImage newCanvas() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int bg = new Color(0x12, 0x14, 0x1E).getRGB();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                image.setRGB(x, y, bg);
            }
        }
        return image;
    }

    private static void write(BufferedImage image, String fileName) throws IOException {
        File outDir = new File("/home/claude/rayphong/out_render");
        outDir.mkdirs();
        File outFile = new File(outDir, fileName);
        ImageIO.write(image, "png", outFile);
        System.out.println("Geschrieben: " + outFile.getAbsolutePath());
    }
}
