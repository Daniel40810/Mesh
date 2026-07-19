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
 * Headless-Verifikation für Phase 6 (Texture-Support). Erzeugt zwei prozedurale Texturen
 * (Karo-Diffuse, Rausch-Specular-Maske) und rendert damit eine Kugel und einen Würfel,
 * um UV-Mapping, bilineare Filterung und Specular-Map-Modulation visuell zu prüfen.
 */
public final class Phase6TextureDemo {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    public static void main(String[] args) throws IOException {
        Texture checker = buildCheckerTexture(256, 256, 8);
        Texture specNoise = buildRadialSpecularMask(256, 256);

        renderTexturedSphere(checker, specNoise);
        renderTexturedCube(checker);
    }

    private static void renderTexturedSphere(Texture diffuse, Texture specMask) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh sphere = MeshFactory.sphere(1.6f, 40, 56);
        Mat4 model = Mat4.rotationY(0.4f).multiply(Mat4.rotationX(0.15f));
        Vec3 cameraPos = new Vec3(0, 1.0f, 6f);
        Mat4 view = Mat4.lookAt(cameraPos, Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        PointLight key = new PointLight(new Vec3(-3f, 3f, 4f), Color.WHITE, 3.0f);
        List<PointLight> lights = Arrays.asList(key);

        // Glossy-Basis, aber mit Diffuse- und Specular-Map moduliert.
        PhongMaterial material = PhongMaterial.glossy(Color.WHITE)
                .withDiffuseMap(diffuse)
                .withSpecularMap(specMask);

        PhongShader shader = new PhongShader(material, cameraPos, lights,
                new Color(0x30, 0x30, 0x38), 0.3f);

        Rasterizer.render(sphere, model, view, projection, image, zBuffer, shader);
        write(image, "phase6_textured_sphere.png");
        System.out.println("phase6_textured_sphere.png: Karo-Diffuse + radiale Specular-Maske auf UV-Kugel");
    }

    private static void renderTexturedCube(Texture diffuse) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh cube = MeshFactory.cube(2.4f);
        Mat4 model = Mat4.rotationY(0.6f).multiply(Mat4.rotationX(0.35f));
        Vec3 cameraPos = new Vec3(0, 1.2f, 6f);
        Mat4 view = Mat4.lookAt(cameraPos, Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        PointLight key = new PointLight(new Vec3(3f, 3.5f, 4f), new Color(0x00, 0xB5, 0xAD), 3.2f);
        List<PointLight> lights = Arrays.asList(key);

        PhongMaterial material = PhongMaterial.standard(Color.WHITE).withDiffuseMap(diffuse);
        PhongShader shader = new PhongShader(material, cameraPos, lights,
                new Color(0x20, 0x22, 0x2C), 0.3f);

        Rasterizer.render(cube, model, view, projection, image, zBuffer, shader);
        write(image, "phase6_textured_cube.png");
        System.out.println("phase6_textured_cube.png: Karo-Diffuse auf allen 6 Würfelseiten (Box-UV-Mapping)");
    }

    /** Klassisches Schachbrett — prüft UV-Zuordnung und Nahtlosigkeit an den Rändern. */
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

    /** Radiale Graustufen-Maske: Zentrum glänzt stark, Rand kaum — simuliert Verschmutzung/Abnutzung. */
    private static Texture buildRadialSpecularMask(int width, int height) {
        Texture tex = new Texture("spec_mask", width, height);
        tex.setWrapMode(Texture.WrapMode.CLAMP);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float u = (x + 0.5f) / width;
                float v = (y + 0.5f) / height;
                float dx = u - 0.5f;
                float dy = v - 0.5f;
                float dist = (float) Math.sqrt(dx * dx + dy * dy) * 2f; // 0 (Mitte) .. ~1.41 (Ecke)
                float mask = Math.max(0f, 1f - dist);
                tex.setPixel(x, y, mask, mask, mask);
            }
        }
        return tex;
    }

    private static Mat4 perspective() {
        return Mat4.perspective((float) Math.toRadians(50), (float) WIDTH / HEIGHT, 0.1f, 100f);
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
