package com.dan.rayphong.demo;

import com.dan.rayphong.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Headless-Verifikation für Phase 10 (Environment-Reflexion + Fresnel). Zwei Szenen:
 * 1) Vier Kugeln mit steigender {@code reflectivity} (0 / 0.3 / 0.6 / 1.0) unter identischer
 *    Beleuchtung — zeigt den prozeduralen Himmel/Horizont/Boden-Fallback und wie stärker
 *    reflektierende Materialien mehr Umgebungsfarbe statt Eigenfarbe zeigen.
 * 2) Eine einzelne Kugel mit niedrigem, dielektrischem F0 (0.04) aus einem Winkel, der Zentrum
 *    (Frontalblick, wenig Reflexion erwartet) und Rand (streifender Blick, viel Reflexion
 *    erwartet) gleichzeitig zeigt — der klassische Fresnel-"Rand-Glanz".
 */
public final class Phase10FresnelDemo {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 400;

    public static void main(String[] args) throws IOException {
        renderReflectivityComparison();
        renderFresnelRimSphere();
    }

    private static void renderReflectivityComparison() throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh sphere = MeshFactory.sphere(1.0f, 36, 48);
        Vec3 cameraPos = new Vec3(0, 0.5f, 7f);
        Mat4 view = Mat4.lookAt(cameraPos, Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        PointLight key = new PointLight(new Vec3(-3f, 3f, 4f), Color.WHITE, 3.0f);
        List<PointLight> lights = Arrays.asList(key);
        Color ambient = new Color(0x30, 0x30, 0x38);

        float[] reflectivities = {0f, 0.3f, 0.6f, 1.0f};
        float[] xPositions = {-3.3f, -1.1f, 1.1f, 3.3f};

        for (int i = 0; i < reflectivities.length; i++) {
            PhongMaterial material = PhongMaterial.standard(new Color(0x1E, 0x7A, 0xFF))
                    .withReflection(reflectivities[i]); // kein envMap -> prozeduraler Himmel, F0=0.04
            PhongShader shader = new PhongShader(material, cameraPos, lights, ambient, 0.3f);
            Mat4 model = Mat4.translation(xPositions[i], 0f, 0f);
            Rasterizer.render(sphere, model, view, projection, image, zBuffer, shader);
        }

        write(image, "phase10_reflectivity_comparison.png");
        System.out.println("phase10_reflectivity_comparison.png: reflectivity = 0 / 0.3 / 0.6 / 1.0 (links -> rechts)");
    }

    private static void renderFresnelRimSphere() throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh sphere = MeshFactory.sphere(1.8f, 44, 60);
        Vec3 cameraPos = new Vec3(0, 0f, 4.2f); // nah dran: Kugelrand ist stark streifender Blickwinkel
        Mat4 view = Mat4.lookAt(cameraPos, Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        PointLight key = new PointLight(new Vec3(2f, 3f, 3f), Color.WHITE, 2.5f);
        List<PointLight> lights = Arrays.asList(key);
        Color ambient = new Color(0x25, 0x27, 0x30);

        // Niedriges F0 (dielektrisch, z. B. lackierter Kunststoff): am Zentrum kaum Spiegelung,
        // am Rand (streifender Blick) durch Fresnel stark reflektierend.
        PhongMaterial material = PhongMaterial.glossy(new Color(0x20, 0x22, 0x28))
                .withReflection(null, 1.0f, 0.04f);
        PhongShader shader = new PhongShader(material, cameraPos, lights, ambient, 0.25f);
        Rasterizer.render(sphere, Mat4.identity(), view, projection, image, zBuffer, shader);

        write(image, "phase10_fresnel_rim.png");
        System.out.println("phase10_fresnel_rim.png: F0=0.04, Zentrum dunkel/Eigenfarbe, Rand hell/Himmel-Reflexion");
    }

    private static Mat4 perspective() {
        return Mat4.perspective((float) Math.toRadians(45), (float) WIDTH / HEIGHT, 0.1f, 100f);
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
