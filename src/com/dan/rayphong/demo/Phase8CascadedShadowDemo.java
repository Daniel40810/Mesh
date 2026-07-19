package com.dan.rayphong.demo;

import com.dan.rayphong.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Headless-Verifikation für Phase 8 (Cascaded Shadow Maps). Eine Reihe von Würfeln in
 * zunehmendem Kamera-Abstand (3, 10, 20, 34 Einheiten) auf einer langen Bodenebene — direkter
 * Vergleich zwischen einer einzelnen Shadow-Map über die komplette Szene und 3 Kaskaden mit
 * derselben Auflösung PRO Kaskade. Erwartung: der nahe Würfel (Kamera-Abstand ~3-6) wirft in
 * der Kaskaden-Variante einen deutlich schärferen Schatten, weil dessen Kaskade eng an sein
 * Frustum-Segment angepasst ist statt sich die Auflösung mit der gesamten 40-Einheiten-Szene
 * teilen zu müssen.
 */
public final class Phase8CascadedShadowDemo {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final float FOV_Y = (float) Math.toRadians(50);
    private static final float CAMERA_NEAR = 0.1f;
    private static final float CAMERA_FAR = 60f;

    public static void main(String[] args) throws IOException {
        Mesh cubeMesh = MeshFactory.cube(1.4f);
        // Großzügige Bodenebene, zentriert im Ursprung — ragt bewusst weit hinter die Kamera
        // (z bis +25). Seit Phase 11 (echtes Near-Plane-Clipping im Rasterizer) kein Problem
        // mehr: der über-Kamera-liegende Teil des Dreiecks wird sauber weggeschnitten statt
        // dass das ganze Mesh verschwindet (siehe Rasterizer-Klassendoku).
        Mesh groundMesh = MeshFactory.plane(50f, 50f);
        Mat4 groundModel = Mat4.identity();

        float[] cubeDistancesAlongZ = {3f, 10f, 20f, 34f}; // Kamera blickt -Z
        List<Mat4> cubeModels = new ArrayList<Mat4>();
        for (float d : cubeDistancesAlongZ) {
            cubeModels.add(Mat4.translation(0f, 0.7f, -d));
        }

        Vec3 cameraPos = new Vec3(0f, 2.5f, 6f);
        Vec3 cameraTarget = new Vec3(0f, 0.5f, -20f);
        Vec3 worldUp = new Vec3(0, 1, 0);
        Mat4 view = Mat4.lookAt(cameraPos, cameraTarget, worldUp);
        float aspect = (float) WIDTH / HEIGHT;
        Mat4 projection = Mat4.perspective(FOV_Y, aspect, CAMERA_NEAR, CAMERA_FAR);

        Vec3 lightPos = new Vec3(-8f, 14f, -6f);
        Vec3 sceneCenter = new Vec3(0f, 1f, -17f); // ungefähre Mitte der Würfel-Reihe
        PointLight light = new PointLight(lightPos, Color.WHITE, 8.0f, 1.0f, 0.02f, 0.002f);

        List<SceneNode> casters = new ArrayList<SceneNode>();
        for (Mat4 model : cubeModels) {
            casters.add(new SceneNode(cubeMesh, model));
        }

        // --- Variante A: eine einzelne Shadow-Map über die komplette Szene (wie Phase 3/5) ---
        ShadowMap singleMap = buildSingleShadowMap(casters, lightPos, sceneCenter, 1024);
        renderScene(cubeMesh, cubeModels, groundMesh, groundModel, view, projection, cameraPos, light,
                singleMap, "phase8_single_shadowmap.png");

        // --- Variante B: 3 Kaskaden, dieselbe Auflösung PRO Kaskade ---
        Vec3[] basis = CascadedShadowMap.cameraBasis(cameraPos, cameraTarget, worldUp);
        CascadedShadowMap cascaded = CascadedShadowMap.build(casters, lightPos, sceneCenter,
                cameraPos, basis[0], basis[1], basis[2], FOV_Y, aspect, CAMERA_NEAR, CAMERA_FAR, 3, 1024);
        renderScene(cubeMesh, cubeModels, groundMesh, groundModel, view, projection, cameraPos, light,
                cascaded, "phase8_cascaded_shadowmap.png");

        System.out.println("Kaskaden-Splits (Kamera-Abstand): " + Arrays.toString(cascaded.splitDistances()));
    }

    private static ShadowMap buildSingleShadowMap(List<SceneNode> casters, Vec3 lightPos, Vec3 sceneCenter, int res) {
        Vec3 dir = sceneCenter.sub(lightPos).normalize();
        Vec3 up = Math.abs(dir.y) > 0.98f ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        Mat4 lightView = Mat4.lookAt(lightPos, sceneCenter, up);
        Mat4 lightProjection = Mat4.perspective((float) Math.toRadians(65), 1f, 0.5f, 45f);
        return ShadowMap.render(casters, lightView, lightProjection, res, res);
    }

    private static void renderScene(Mesh cubeMesh, List<Mat4> cubeModels, Mesh groundMesh, Mat4 groundModel,
                                     Mat4 view, Mat4 projection, Vec3 cameraPos, PointLight light,
                                     ShadowSource shadowSource, String outFile) throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        List<PointLight> lights = Arrays.asList(light);
        List<ShadowSource> shadowSources = Arrays.asList(shadowSource);
        Color ambient = new Color(0x20, 0x22, 0x2C);

        PhongMaterial groundMat = PhongMaterial.matte(new Color(0xB0, 0xBA, 0xC4));
        PhongShader groundShader = new PhongShader(groundMat, cameraPos, lights, shadowSources, ambient, 0.3f);
        Rasterizer.render(groundMesh, groundModel, view, projection, image, zBuffer, groundShader);

        Color[] cubeColors = {
                new Color(0x00, 0xB5, 0xAD), new Color(0x1E, 0x7A, 0xFF),
                new Color(0x8B, 0x00, 0x24), new Color(0xE8, 0xB4, 0x30)
        };
        for (int i = 0; i < cubeModels.size(); i++) {
            PhongMaterial mat = PhongMaterial.standard(cubeColors[i % cubeColors.length]);
            PhongShader shader = new PhongShader(mat, cameraPos, lights, shadowSources, ambient, 0.3f);
            Rasterizer.render(cubeMesh, cubeModels.get(i), view, projection, image, zBuffer, shader);
        }

        write(image, outFile);
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
