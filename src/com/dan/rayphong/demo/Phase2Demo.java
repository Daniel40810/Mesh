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
 * Headless-Verifikation für Phase 2 (Phong-Beleuchtung). Zwei Szenen:
 * 1) Eine glänzende Kugel, beleuchtet von einem türkisen und einem weinroten Punktlicht
 *    (ReagenzglasBar-Farbidentität) — zeigt Farbmischung + Specular-Glanzpunkt.
 * 2) Drei Kugeln mit unterschiedlichem Material (matt/standard/glänzend) unter identischer
 *    Beleuchtung — zeigt den Effekt der Material-Koeffizienten.
 */
public final class Phase2Demo {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final Vec3 CAMERA_POS = new Vec3(0, 1.0f, 6f);

    public static void main(String[] args) throws IOException {
        renderTwoLightScene();
        renderMaterialComparison();
    }

    private static void renderTwoLightScene() throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Mesh sphere = MeshFactory.sphere(1.6f, 40, 56);
        Mat4 model = Mat4.rotationX(0.15f);
        Mat4 view = Mat4.lookAt(CAMERA_POS, Vec3.ZERO, new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        PointLight turquoise = new PointLight(new Vec3(-3.5f, 2.5f, 3f),
                new Color(0x00, 0xB5, 0xAD), 3.0f);
        PointLight wineRed = new PointLight(new Vec3(3.5f, -1.0f, 2.5f),
                new Color(0x8B, 0x00, 0x24), 3.0f);
        List<PointLight> lights = Arrays.asList(turquoise, wineRed);

        PhongMaterial material = PhongMaterial.glossy(new Color(0xEA, 0xFB, 0xFF));
        PhongShader shader = new PhongShader(material, CAMERA_POS, lights,
                new Color(0x30, 0x30, 0x38), 0.25f);

        Rasterizer.render(sphere, model, view, projection, image, zBuffer, shader);
        write(image, "phase2_two_lights.png");
    }

    private static void renderMaterialComparison() throws IOException {
        BufferedImage image = newCanvas();
        float[] zBuffer = new float[WIDTH * HEIGHT];
        Rasterizer.clearZBuffer(zBuffer);

        Vec3 cameraPos = new Vec3(0, 0.6f, 7f);
        Mat4 view = Mat4.lookAt(cameraPos, new Vec3(0, 0, 0), new Vec3(0, 1, 0));
        Mat4 projection = perspective();

        // Ein einzelnes weißes Licht von schräg oben-vorne, damit die drei Kugeln
        // ausschließlich durch ihr Material unterschieden werden.
        PointLight key = new PointLight(new Vec3(-2f, 3f, 4f), Color.WHITE, 3.0f);
        List<PointLight> lights = Arrays.asList(key);
        Color ambient = new Color(0x30, 0x30, 0x38);

        Mesh sphere = MeshFactory.sphere(0.9f, 32, 44);

        renderAt(sphere, new Vec3(-2.1f, 0, 0), view, projection, image, zBuffer,
                new PhongShader(PhongMaterial.matte(new Color(0x1E, 0x7A, 0xFF)), cameraPos, lights, ambient, 0.3f));
        renderAt(sphere, new Vec3(0, 0, 0), view, projection, image, zBuffer,
                new PhongShader(PhongMaterial.standard(new Color(0x1E, 0x7A, 0xFF)), cameraPos, lights, ambient, 0.3f));
        renderAt(sphere, new Vec3(2.1f, 0, 0), view, projection, image, zBuffer,
                new PhongShader(PhongMaterial.glossy(new Color(0x1E, 0x7A, 0xFF)), cameraPos, lights, ambient, 0.3f));

        write(image, "phase2_material_comparison.png");
    }

    private static void renderAt(Mesh mesh, Vec3 offset, Mat4 view, Mat4 projection,
                                  BufferedImage image, float[] zBuffer, PhongShader shader) {
        Mat4 model = Mat4.translation(offset.x, offset.y, offset.z);
        Rasterizer.render(mesh, model, view, projection, image, zBuffer, shader);
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
        File outDir = new File("/home/claude/rayphong/out");
        outDir.mkdirs();
        File outFile = new File(outDir, fileName);
        ImageIO.write(image, "png", outFile);
        System.out.println("Geschrieben: " + outFile.getAbsolutePath());
    }
}
