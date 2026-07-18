package com.dan.rayphong.demo;

import com.dan.rayphong.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Headless-Verifikation für Phase 3 (Shadow Maps). Eine Kugel schwebt über einer Bodenebene;
 * ein einzelnes Punktlicht von schräg oben wirft einen weichen Schatten der Kugel auf den Boden.
 */
public final class Phase3Demo {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final int SHADOW_RES = 1024;

    public static void main(String[] args) throws IOException {
        long t0 = System.currentTimeMillis();

        Vec3 sphereCenter = new Vec3(0f, 1.4f, 0f);
        Mesh sphereMesh = MeshFactory.sphere(1.2f, 36, 48);
        Mat4 sphereModel = Mat4.translation(sphereCenter.x, sphereCenter.y, sphereCenter.z);

        Mesh planeMesh = MeshFactory.plane(14f, 14f);
        Mat4 planeModel = Mat4.identity();

        Vec3 lightPos = new Vec3(3.2f, 5.5f, 3.0f);
        PointLight light = new PointLight(lightPos, Color.WHITE, 6.0f);

        // Licht als Kamera: blickt auf den Kugelmittelpunkt, Frustum weit genug für Kugel+Boden.
        Mat4 lightView = Mat4.lookAt(lightPos, sphereCenter, new Vec3(0, 1, 0));
        Mat4 lightProjection = Mat4.perspective((float) Math.toRadians(60), 1f, 0.5f, 30f);

        List<SceneNode> shadowCasters = Arrays.asList(new SceneNode(sphereMesh, sphereModel));
        ShadowMap shadowMap = ShadowMap.render(shadowCasters, lightView, lightProjection,
                SHADOW_RES, SHADOW_RES);

        long t1 = System.currentTimeMillis();
        System.out.println("Shadow-Map-Depth-Pass: " + (t1 - t0) + " ms");

        // --- Color-Pass ---
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Vec3 cameraPos = new Vec3(0f, 2.2f, 8f);
        Mat4 view = Mat4.lookAt(cameraPos, new Vec3(0, 0.8f, 0), new Vec3(0, 1, 0));
        Mat4 projection = Mat4.perspective((float) Math.toRadians(50), (float) WIDTH / HEIGHT, 0.1f, 100f);

        List<PointLight> lights = Arrays.asList(light);
        List<ShadowMap> shadowMaps = Arrays.asList(shadowMap);
        Color ambient = new Color(0x20, 0x22, 0x2C);

        PhongShader groundShader = new PhongShader(
                PhongMaterial.matte(new Color(0xB8, 0xC4, 0xCE)), cameraPos, lights, shadowMaps, ambient, 0.35f);
        PhongShader sphereShader = new PhongShader(
                PhongMaterial.glossy(new Color(0x00, 0xB5, 0xAD)), cameraPos, lights, shadowMaps, ambient, 0.35f);

        Rasterizer.render(planeMesh, planeModel, view, projection, image, zBuffer, groundShader);
        Rasterizer.render(sphereMesh, sphereModel, view, projection, image, zBuffer, sphereShader);

        write(image, "phase3_shadow.png");

        // --- Referenzbild ohne Schatten (gleiche Szene, shadowMaps=null) zum Vorher/Nachher-Vergleich ---
        BufferedImage imageNoShadow = newCanvas();
        float[] zBuffer2 = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer2);
        PhongShader groundNoShadow = new PhongShader(
                PhongMaterial.matte(new Color(0xB8, 0xC4, 0xCE)), cameraPos, lights, ambient, 0.35f);
        PhongShader sphereNoShadow = new PhongShader(
                PhongMaterial.glossy(new Color(0x00, 0xB5, 0xAD)), cameraPos, lights, ambient, 0.35f);
        Rasterizer.render(planeMesh, planeModel, view, projection, imageNoShadow, zBuffer2, groundNoShadow);
        Rasterizer.render(sphereMesh, sphereModel, view, projection, imageNoShadow, zBuffer2, sphereNoShadow);
        write(imageNoShadow, "phase3_no_shadow_reference.png");

        long t2 = System.currentTimeMillis();
        System.out.println("Color-Pass (beide Bilder): " + (t2 - t1) + " ms, gesamt: " + (t2 - t0) + " ms");
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
        File outDir = new File("/home/claude/rayphong/out");
        outDir.mkdirs();
        File outFile = new File(outDir, fileName);
        ImageIO.write(image, "png", outFile);
        System.out.println("Geschrieben: " + outFile.getAbsolutePath());
    }
}
