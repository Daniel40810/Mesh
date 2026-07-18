package com.dan.rayphong.editor;

import com.dan.rayphong.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Übersetzt eine {@link RayPhongSnapshot} in ein gerendertes Bild. Rendert seit Phase 6
 * eine beliebige Liste von Objekten (Mehrobjekt-Szene), alle als gemeinsame Schatten-Werfer
 * in EINER {@link ShadowMap} pro Licht (siehe Phase 5).
 *
 * <p>Der Renderer liest ausschliesslich aus der unveraenderlichen Momentaufnahme und
 * niemals aus dem lebenden {@link RayPhongScene} — dadurch ist der Durchlauf frei von
 * Data Races mit EDT-Mutationen (siehe {@link RayPhongSnapshot}).</p>
 */
public final class RayPhongRenderer {

    private static final float GROUND_SIZE = 14f;

    /** Ergebnis eines Renderdurchlaufs inkl. Statistiken für die Status-Anzeige. */
    public static final class Result {
        public final BufferedImage image;
        public final long renderMillis;
        public final int triangleCount;

        public Result(BufferedImage image, long renderMillis, int triangleCount) {
            this.image = image;
            this.renderMillis = renderMillis;
            this.triangleCount = triangleCount;
        }
    }

    // Einfacher Cache geladener OBJ-Meshes, damit ein Auto-Rotate-Timer nicht bei jedem Frame
    // dieselbe Datei neu parst.
    private static final Map<String, Mesh> OBJ_CACHE = new HashMap<String, Mesh>();

    private RayPhongRenderer() {
    }

    public static Result render(RayPhongSnapshot snap) {
        long t0 = System.currentTimeMillis();

        int width = snap.width;
        int height = snap.height;

        List<Mesh> meshes = new ArrayList<Mesh>();
        List<Mat4> models = new ArrayList<Mat4>();
        int triangleCount = 0;

        for (RayPhongSnapshot.Obj obj : snap.objects) {
            Mesh mesh = buildMesh(obj);
            Mat4 model = Mat4.translation(obj.position.x, obj.position.y, obj.position.z)
                    .multiply(Mat4.rotationY(snap.modelRotationY + obj.rotationY));
            meshes.add(mesh);
            models.add(model);
            triangleCount += mesh.triangleCount();
        }

        Mesh groundMesh = MeshFactory.plane(GROUND_SIZE, GROUND_SIZE);
        Mat4 groundModel = Mat4.identity();

        PointLight l1 = new PointLight(snap.light1.position, snap.light1.color, snap.light1.intensity);
        PointLight l2 = new PointLight(snap.light2.position, snap.light2.color, snap.light2.intensity);
        List<PointLight> lights = new ArrayList<PointLight>();
        lights.add(l1);
        lights.add(l2);

        Vec3 sceneCenter = averageCenter(snap.objects);
        List<ShadowMap> shadowMaps = new ArrayList<ShadowMap>();
        shadowMaps.add(buildShadowMapIfEnabled(snap.light1, meshes, models, sceneCenter, snap.shadowResolution));
        shadowMaps.add(buildShadowMapIfEnabled(snap.light2, meshes, models, sceneCenter, snap.shadowResolution));

        // Kamera-Orbit aus Yaw/Pitch/Distance um den Szenen-Mittelpunkt.
        float cy = (float) Math.cos(snap.cameraPitch);
        Vec3 offset = new Vec3(
                cy * (float) Math.sin(snap.cameraYaw) * snap.cameraDistance,
                (float) Math.sin(snap.cameraPitch) * snap.cameraDistance,
                cy * (float) Math.cos(snap.cameraYaw) * snap.cameraDistance
        );
        Vec3 cameraPos = sceneCenter.add(offset);
        Mat4 view = Mat4.lookAt(cameraPos, sceneCenter, new Vec3(0, 1, 0));
        Mat4 projection = Mat4.perspective((float) Math.toRadians(50), (float) width / height, 0.1f, 100f);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int bg = new Color(0x12, 0x14, 0x1E).getRGB();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, bg);
            }
        }
        float[] zBuffer = new float[width * height];
        Rasterizer.clearZBuffer(zBuffer);

        PhongMaterial groundMaterial = PhongMaterial.matte(new Color(0xB0, 0xBA, 0xC4));
        PhongShader groundShader = new PhongShader(groundMaterial, cameraPos, lights, shadowMaps,
                snap.ambientColor, snap.ambientIntensity);
        Rasterizer.render(groundMesh, groundModel, view, projection, image, zBuffer, groundShader);

        for (int i = 0; i < meshes.size(); i++) {
            RayPhongSnapshot.Obj obj = snap.objects.get(i);
            PhongMaterial material = new PhongMaterial(obj.baseColor, Color.WHITE,
                    obj.ambientK, obj.diffuseK, obj.specularK, obj.shininess);
            PhongShader shader = new PhongShader(material, cameraPos, lights, shadowMaps,
                    snap.ambientColor, snap.ambientIntensity);
            Rasterizer.render(meshes.get(i), models.get(i), view, projection, image, zBuffer, shader);
        }

        long t1 = System.currentTimeMillis();
        return new Result(image, t1 - t0, triangleCount);
    }

    private static Vec3 averageCenter(List<RayPhongSnapshot.Obj> objects) {
        if (objects.isEmpty()) {
            return new Vec3(0, 1f, 0);
        }
        float x = 0, y = 0, z = 0;
        for (RayPhongSnapshot.Obj obj : objects) {
            x += obj.position.x;
            y += obj.position.y;
            z += obj.position.z;
        }
        int n = objects.size();
        return new Vec3(x / n, y / n, z / n);
    }

    private static ShadowMap buildShadowMapIfEnabled(RayPhongSnapshot.Light light, List<Mesh> meshes,
                                                      List<Mat4> models, Vec3 sceneCenter, int resolution) {
        if (!light.shadowEnabled) {
            return null;
        }
        Vec3 dir = sceneCenter.sub(light.position).normalize();
        Vec3 up = Math.abs(dir.y) > 0.98f ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        Mat4 lightView = Mat4.lookAt(light.position, sceneCenter, up);
        Mat4 lightProjection = Mat4.perspective((float) Math.toRadians(65), 1f, 0.5f, 30f);

        List<SceneNode> casters = new ArrayList<SceneNode>();
        for (int i = 0; i < meshes.size(); i++) {
            casters.add(new SceneNode(meshes.get(i), models.get(i)));
        }
        return ShadowMap.render(casters, lightView, lightProjection, resolution, resolution);
    }

    private static Mesh buildMesh(RayPhongSnapshot.Obj slot) {
        switch (slot.meshKind) {
            case CUBE:
                return MeshFactory.cube(2.1f);
            case TORUS:
                return MeshFactory.torus(1.1f, 0.42f, 28, 18);
            case OBJ:
                return loadObjCached(slot.objPath);
            case SPHERE:
            default:
                return MeshFactory.sphere(1.3f, 28, 40);
        }
    }

    private static Mesh loadObjCached(String path) {
        if (path == null) {
            return MeshFactory.sphere(1.3f, 28, 40); // Fallback, falls noch keine Datei gewählt wurde
        }
        Mesh cached = OBJ_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            Mesh mesh = ObjLoader.load(new FileInputStream(new File(path)));
            OBJ_CACHE.put(path, mesh);
            return mesh;
        } catch (IOException ex) {
            throw new RuntimeException("OBJ konnte nicht geladen werden: " + path, ex);
        }
    }
}
