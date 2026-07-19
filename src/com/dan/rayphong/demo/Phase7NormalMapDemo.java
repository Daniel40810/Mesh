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
 * Headless-Verifikation für Phase 7 (Tangent-Space Normal Mapping). Erzeugt eine prozedurale
 * "Noppen"-Normal-Map (Gitter aus Höhenbuckeln, analytisch abgeleitet) und rendert damit eine
 * glatte Kugel sowie eine flache Ebene — Normal Mapping muss auf beiden Oberflächendetails
 * erzeugen, die es in der zugrundeliegenden Geometrie gar nicht gibt.
 */
public final class Phase7NormalMapDemo {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    public static void main(String[] args) throws IOException {
        Texture bumps = buildStuddedNormalMap(512, 512, 10, 1.4f);

        renderSphereComparison(bumps);
        renderPlaneComparison(bumps);
    }

    /** Kugel MIT und OHNE Normal Map nebeneinander (linke/rechte Bildhälfte). */
    private static void renderSphereComparison(Texture normalMap) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh sphere = MeshFactory.sphere(1.3f, 44, 60);
        Vec3 cameraPos = new Vec3(0, 0.6f, 6.5f);
        Mat4 view = Mat4.lookAt(cameraPos, Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        PointLight key = new PointLight(new Vec3(-2.5f, 2.5f, 4f), Color.WHITE, 3.2f);
        List<PointLight> lights = Arrays.asList(key);
        Color ambient = new Color(0x28, 0x2A, 0x34);

        PhongMaterial flatMat = PhongMaterial.matte(new Color(0xB0, 0xBA, 0xC4));
        PhongMaterial bumpMat = flatMat.withNormalMap(normalMap);

        Mat4 modelLeft = Mat4.translation(-1.6f, 0f, 0f);
        Mat4 modelRight = Mat4.translation(1.6f, 0f, 0f);

        Rasterizer.render(sphere, modelLeft, view, projection, image, zBuffer,
                new PhongShader(flatMat, cameraPos, lights, ambient, 0.35f));
        Rasterizer.render(sphere, modelRight, view, projection, image, zBuffer,
                new PhongShader(bumpMat, cameraPos, lights, ambient, 0.35f));

        write(image, "phase7_sphere_normalmap_comparison.png");
        System.out.println("phase7_sphere_normalmap_comparison.png: links glatt, rechts mit Noppen-Normal-Map");
    }

    /** Flache Ebene MIT Normal Map, aus flachem Winkel — Buckel müssen sichtbar Licht/Schatten werfen. */
    private static void renderPlaneComparison(Texture normalMap) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh plane = MeshFactory.plane(6f, 6f);
        Vec3 cameraPos = new Vec3(0, 2.2f, 4.5f);
        Mat4 view = Mat4.lookAt(cameraPos, new Vec3(0, 0, -0.5f), new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        PointLight key = new PointLight(new Vec3(-2f, 2.2f, 1.5f), Color.WHITE, 3.0f);
        List<PointLight> lights = Arrays.asList(key);
        Color ambient = new Color(0x20, 0x22, 0x2C);

        PhongMaterial bumpMat = PhongMaterial.matte(new Color(0x8B, 0x8F, 0x96)).withNormalMap(normalMap);
        Mat4 model = Mat4.identity();

        Rasterizer.render(plane, model, view, projection, image, zBuffer,
                new PhongShader(bumpMat, cameraPos, lights, ambient, 0.3f));

        write(image, "phase7_plane_normalmap.png");
        System.out.println("phase7_plane_normalmap.png: flache Ebene mit Noppen-Normal-Map, flacher Blickwinkel");
    }

    /**
     * Analytisches Höhenfeld h(u,v) = sin(u*2*pi*freq) * sin(v*2*pi*freq) (Noppen-Gitter).
     * Die Ableitungen dh/du, dh/dv ergeben direkt die Tangent-Space-Normale
     * (-dh/du, -dh/dv, 1)/|...|, ohne Sobel-Filter oder Nachbar-Pixel-Sampling nötig.
     */
    private static Texture buildStuddedNormalMap(int width, int height, int freq, float strength) {
        Texture tex = new Texture("studded_normal", width, height);
        tex.setWrapMode(Texture.WrapMode.CLAMP);

        double twoPiFreq = 2 * Math.PI * freq;
        for (int y = 0; y < height; y++) {
            float v = (y + 0.5f) / height;
            for (int x = 0; x < width; x++) {
                float u = (x + 0.5f) / width;

                double su = Math.sin(u * twoPiFreq);
                double cu = Math.cos(u * twoPiFreq);
                double sv = Math.sin(v * twoPiFreq);
                double cv = Math.cos(v * twoPiFreq);

                // h = su*sv  =>  dh/du = twoPiFreq*cu*sv,  dh/dv = twoPiFreq*su*cv
                double dhdu = twoPiFreq * cu * sv;
                double dhdv = twoPiFreq * su * cv;

                Vec3 n = new Vec3((float) (-dhdu * strength), (float) (-dhdv * strength), 1f).normalize();

                // Encode: [-1,1] -> [0,1]
                tex.setPixel(x, y, n.x * 0.5f + 0.5f, n.y * 0.5f + 0.5f, n.z * 0.5f + 0.5f);
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
