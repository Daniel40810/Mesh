package com.dan.rayphong.demo;

import com.dan.rayphong.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Headless-Verifikation für Phase 5: Torus-Primitiv, OBJ-Loader (mit und ohne Normalen)
 * und eine Mehrobjekt-Szene, in der mehrere Meshes gemeinsam in EINE Shadow-Map einzahlen
 * (SceneNode-Liste aus Phase 3 zahlt sich hier aus).
 */
public final class Phase5Demo {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    public static void main(String[] args) throws IOException {
        renderStandaloneObj("/home/claude/rayphong/assets/gem.obj", "phase5_gem_no_normals.png", Color.WHITE);
        renderStandaloneObj("/home/claude/rayphong/assets/pyramid.obj", "phase5_pyramid_with_normals.png", Color.WHITE);
        renderTorus();
        renderMultiObjectScene();
    }

    private static void renderStandaloneObj(String path, String outFile, Color albedo) throws IOException {
        Mesh mesh = ObjLoader.load(new FileInputStream(new File(path)));

        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mat4 model = Mat4.rotationY(0.5f).multiply(Mat4.rotationX(0.15f));
        Mat4 view = Mat4.lookAt(new Vec3(0, 1.2f, 6f), Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = Mat4.perspective((float) Math.toRadians(50), (float) WIDTH / HEIGHT, 0.1f, 100f);

        Vec3 lightDir = new Vec3(-0.5f, 0.8f, 0.6f);
        Rasterizer.FragmentShader shader = new DirectionalLambertShader(albedo, lightDir, Color.WHITE, 0.15f);
        Rasterizer.render(mesh, model, view, projection, image, zBuffer, shader);

        write(image, outFile);
        System.out.println(outFile + ": " + mesh.positions.length + " Vertices, "
                + mesh.triangleCount() + " Dreiecke (geladen aus " + path + ")");
    }

    private static void renderTorus() throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh torus = MeshFactory.torus(1.4f, 0.5f, 36, 24);
        Mat4 model = Mat4.rotationX(0.9f);
        Vec3 cameraPos = new Vec3(0, 1.2f, 6f);
        Mat4 view = Mat4.lookAt(cameraPos, Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = Mat4.perspective((float) Math.toRadians(50), (float) WIDTH / HEIGHT, 0.1f, 100f);

        PointLight light = new PointLight(new Vec3(-3f, 4f, 4f), Color.WHITE, 3.0f);
        PhongShader shader = new PhongShader(PhongMaterial.glossy(new Color(0x8B, 0x00, 0x24)),
                cameraPos, Arrays.asList(light), new Color(0x30, 0x30, 0x38), 0.3f);

        Rasterizer.render(torus, model, view, projection, image, zBuffer, shader);
        write(image, "phase5_torus.png");
        System.out.println("phase5_torus.png: " + torus.triangleCount() + " Dreiecke");
    }

    /** Sphäre + Würfel + Torus gleichzeitig, alle drei werfen Schatten in EINE gemeinsame ShadowMap. */
    private static void renderMultiObjectScene() throws IOException {
        Mesh sphereMesh = MeshFactory.sphere(0.9f, 28, 40);
        Mat4 sphereModel = Mat4.translation(-2.1f, 1.1f, 0f);

        Mesh cubeMesh = MeshFactory.cube(1.5f);
        Mat4 cubeModel = Mat4.translation(0f, 0.9f, 0f).multiply(Mat4.rotationY(0.5f));

        Mesh torusMesh = MeshFactory.torus(0.85f, 0.32f, 28, 18);
        Mat4 torusModel = Mat4.translation(2.2f, 1.0f, 0f).multiply(Mat4.rotationX(1.2f));

        Mesh groundMesh = MeshFactory.plane(14f, 14f);
        Mat4 groundModel = Mat4.identity();

        Vec3 lightPos = new Vec3(2.5f, 5.5f, 4.5f);
        Vec3 sceneCenter = new Vec3(0f, 1.0f, 0f);
        PointLight light = new PointLight(lightPos, Color.WHITE, 6.0f);

        Mat4 lightView = Mat4.lookAt(lightPos, sceneCenter, new Vec3(0, 1, 0));
        Mat4 lightProjection = Mat4.perspective((float) Math.toRadians(65), 1f, 0.5f, 30f);

        // Alle drei Objekte sind Schatten-Werfer in EINER gemeinsamen ShadowMap.
        List<SceneNode> casters = Arrays.asList(
                new SceneNode(sphereMesh, sphereModel),
                new SceneNode(cubeMesh, cubeModel),
                new SceneNode(torusMesh, torusModel)
        );
        ShadowMap shadowMap = ShadowMap.render(casters, lightView, lightProjection, 1024, 1024);

        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Vec3 cameraPos = new Vec3(0f, 2.4f, 8.5f);
        Mat4 view = Mat4.lookAt(cameraPos, sceneCenter, new Vec3(0, 1, 0));
        Mat4 projection = Mat4.perspective((float) Math.toRadians(50), (float) WIDTH / HEIGHT, 0.1f, 100f);

        List<PointLight> lights = Arrays.asList(light);
        List<ShadowMap> shadowMaps = Arrays.asList(shadowMap);
        Color ambient = new Color(0x20, 0x22, 0x2C);

        PhongShader groundShader = new PhongShader(PhongMaterial.matte(new Color(0xB0, 0xBA, 0xC4)),
                cameraPos, lights, shadowMaps, ambient, 0.3f);
        PhongShader sphereShader = new PhongShader(PhongMaterial.glossy(new Color(0x00, 0xB5, 0xAD)),
                cameraPos, lights, shadowMaps, ambient, 0.3f);
        PhongShader cubeShader = new PhongShader(PhongMaterial.standard(new Color(0x1E, 0x7A, 0xFF)),
                cameraPos, lights, shadowMaps, ambient, 0.3f);
        PhongShader torusShader = new PhongShader(PhongMaterial.glossy(new Color(0x8B, 0x00, 0x24)),
                cameraPos, lights, shadowMaps, ambient, 0.3f);

        Rasterizer.render(groundMesh, groundModel, view, projection, image, zBuffer, groundShader);
        Rasterizer.render(sphereMesh, sphereModel, view, projection, image, zBuffer, sphereShader);
        Rasterizer.render(cubeMesh, cubeModel, view, projection, image, zBuffer, cubeShader);
        Rasterizer.render(torusMesh, torusModel, view, projection, image, zBuffer, torusShader);

        write(image, "phase5_multi_object_scene.png");
        System.out.println("phase5_multi_object_scene.png: 3 Objekte + Boden, gemeinsame ShadowMap ("
                + (sphereMesh.triangleCount() + cubeMesh.triangleCount() + torusMesh.triangleCount())
                + " Schatten-Dreiecke)");
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
