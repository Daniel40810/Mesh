package com.dan.rayphong.demo;

import com.dan.rayphong.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Kein DFrame, kein setVisible — reines Headless-Rendering für die Pixel-Verifikation
 * von Phase 1 (Mathematik + Rasterizer). Rendert Kugel und Würfel mit einfachem
 * Richtungslicht (Lambert) und speichert PNGs.
 */
public final class Phase1Demo {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    public static void main(String[] args) throws IOException {
        renderScene(MeshFactory.sphere(1.5f, 32, 48), "phase1_sphere.png", 0f);
        renderScene(MeshFactory.cube(2.2f), "phase1_cube.png", 0.6f);
    }

    private static void renderScene(Mesh mesh, String fileName, float yRotationRadians) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        // Hintergrund: dunkles Labor-Grau
        int bg = new Color(0x12, 0x14, 0x1E).getRGB();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                image.setRGB(x, y, bg);
            }
        }

        Mat4 model = Mat4.rotationY(yRotationRadians).multiply(Mat4.rotationX(0.3f));
        Mat4 view = Mat4.lookAt(new Vec3(0, 1.2f, 6f), Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = Mat4.perspective((float) Math.toRadians(50), (float) WIDTH / HEIGHT, 0.1f, 100f);

        Vec3 lightDir = new Vec3(-0.5f, 0.8f, 0.6f); // von oben-vorne-links
        Rasterizer.FragmentShader shader = new DirectionalLambertShader(
                Color.WHITE, lightDir, Color.WHITE, 0.12f);

        Rasterizer.render(mesh, model, view, projection, image, zBuffer, shader);

        File outDir = new File("/home/claude/rayphong/out");
        outDir.mkdirs();
        File outFile = new File(outDir, fileName);
        ImageIO.write(image, "png", outFile);
        System.out.println("Geschrieben: " + outFile.getAbsolutePath()
                + " (" + mesh.triangleCount() + " Dreiecke)");
    }
}
