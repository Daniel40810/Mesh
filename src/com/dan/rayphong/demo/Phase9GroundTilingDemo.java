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
 * Headless-Verifikation für UV-Tiling der Bodenebene. Eine große Karo-Textur wird einmal
 * ohne Kachelung (uTiles=vTiles=1, wie {@link MeshFactory#plane(float, float)} vor dieser
 * Änderung) und einmal mit {@code plane(w, d, 8, 8)} gerendert — ohne Tiling wirkt das Karo
 * über die ganze Bodenfläche verzerrt/gestreckt, mit Tiling bleiben die Kacheln in einer
 * konstanten, plausiblen Weltgröße.
 */
public final class Phase9GroundTilingDemo {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;

    public static void main(String[] args) throws IOException {
        Texture checker = buildCheckerTexture(256, 256, 8);

        renderGround(MeshFactory.plane(20f, 20f, 1f, 1f), checker, "phase9_ground_no_tiling.png");
        renderGround(MeshFactory.plane(20f, 20f, 8f, 8f), checker, "phase9_ground_tiled_8x8.png");
    }

    private static void renderGround(Mesh groundMesh, Texture checker, String outFile) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mat4 model = Mat4.identity();
        // Ursprüngliche, natürlichere Kamera-Distanz — seit Phase 11 (echtes Near-Plane-
        // Clipping) kein Problem mehr, auch wenn die Ebene wieder näher an die Kamera heranreicht.
        Vec3 cameraPos = new Vec3(0f, 6f, 9f);
        Mat4 view = Mat4.lookAt(cameraPos, new Vec3(0, 0, -2f), new Vec3(0, 1, 0));
        Mat4 projection = Mat4.perspective((float) Math.toRadians(55), (float) WIDTH / HEIGHT, 0.1f, 100f);

        PointLight light = new PointLight(new Vec3(4f, 8f, 4f), Color.WHITE, 4.0f);
        List<PointLight> lights = Arrays.asList(light);
        Color ambient = new Color(0x28, 0x2A, 0x34);

        PhongMaterial material = PhongMaterial.matte(Color.WHITE).withDiffuseMap(checker);
        PhongShader shader = new PhongShader(material, cameraPos, lights, ambient, 0.4f);

        Rasterizer.render(groundMesh, model, view, projection, image, zBuffer, shader);
        write(image, outFile);
        System.out.println(outFile + " geschrieben.");
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
