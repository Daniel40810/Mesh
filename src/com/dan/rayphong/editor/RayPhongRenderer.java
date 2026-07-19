package com.dan.rayphong.editor;

import com.dan.rayphong.*;
import com.dan.rayphong.texture.Texture;
import com.dan.rayphong.texture.TextureManager;

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

    // Analog: TextureManager cached bereits intern nach Dateipfad (siehe TextureManager.loadFromFile),
    // eine einzige Instanz über alle Frames/Objekte hinweg reicht daher aus.
    private static final TextureManager TEXTURE_MANAGER = new TextureManager();

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

        Mesh groundMesh = MeshFactory.plane(GROUND_SIZE, GROUND_SIZE, snap.groundTiling, snap.groundTiling);
        Mat4 groundModel = Mat4.identity();

        PointLight l1 = new PointLight(snap.light1.position, snap.light1.color, snap.light1.intensity);
        PointLight l2 = new PointLight(snap.light2.position, snap.light2.color, snap.light2.intensity);
        List<PointLight> lights = new ArrayList<PointLight>();
        lights.add(l1);
        lights.add(l2);

        Vec3 sceneCenter = averageCenter(snap.objects);

        // Kamera-Orbit aus Yaw/Pitch/Distance um den Szenen-Mittelpunkt — VOR den Shadow Maps
        // berechnet, da Cascaded Shadow Maps die Kamera-Frustum-Parameter brauchen.
        float cy = (float) Math.cos(snap.cameraPitch);
        Vec3 offset = new Vec3(
                cy * (float) Math.sin(snap.cameraYaw) * snap.cameraDistance,
                (float) Math.sin(snap.cameraPitch) * snap.cameraDistance,
                cy * (float) Math.cos(snap.cameraYaw) * snap.cameraDistance
        );
        Vec3 cameraPos = sceneCenter.add(offset);
        Vec3 worldUp = new Vec3(0, 1, 0);
        Mat4 view = Mat4.lookAt(cameraPos, sceneCenter, worldUp);
        float fovY = (float) Math.toRadians(50);
        float aspect = (float) width / height;
        float cameraNear = 0.1f;
        float cameraFar = 100f;
        Mat4 projection = Mat4.perspective(fovY, aspect, cameraNear, cameraFar);

        List<ShadowSource> shadowMaps = new ArrayList<ShadowSource>();
        shadowMaps.add(buildShadowMapIfEnabled(snap.light1, meshes, models, sceneCenter, snap.shadowResolution,
                cameraPos, worldUp, fovY, aspect, cameraNear, cameraFar));
        shadowMaps.add(buildShadowMapIfEnabled(snap.light2, meshes, models, sceneCenter, snap.shadowResolution,
                cameraPos, worldUp, fovY, aspect, cameraNear, cameraFar));

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
        if (snap.groundDiffuseMapPath != null) {
            Texture groundTex = loadTextureSafe(snap.groundDiffuseMapPath);
            if (groundTex != null) {
                groundMaterial = groundMaterial.withDiffuseMap(groundTex);
            }
        }
        PhongShader groundShader = new PhongShader(groundMaterial, cameraPos, lights, shadowMaps,
                snap.ambientColor, snap.ambientIntensity);
        Rasterizer.render(groundMesh, groundModel, view, projection, image, zBuffer, groundShader);

        for (int i = 0; i < meshes.size(); i++) {
            RayPhongSnapshot.Obj obj = snap.objects.get(i);
            PhongMaterial material = new PhongMaterial(obj.baseColor, Color.WHITE,
                    obj.ambientK, obj.diffuseK, obj.specularK, obj.shininess);
            material = applyTextures(material, obj);
            PhongShader shader = new PhongShader(material, cameraPos, lights, shadowMaps,
                    snap.ambientColor, snap.ambientIntensity);
            Rasterizer.render(meshes.get(i), models.get(i), view, projection, image, zBuffer, shader);
        }

        long t1 = System.currentTimeMillis();
        return new Result(image, t1 - t0, triangleCount);
    }

    /**
     * Wendet optionale Diffuse-/Specular-/Normal-Map-Pfade aus dem Snapshot auf ein Material an.
     * Fehlschlagende Ladeversuche (Datei gelöscht/verschoben) werden geloggt und einfach
     * übersprungen — ein Renderdurchlauf soll dadurch nicht abstürzen.
     */
    private static PhongMaterial applyTextures(PhongMaterial material, RayPhongSnapshot.Obj obj) {
        if (obj.diffuseMapPath != null) {
            Texture tex = loadTextureSafe(obj.diffuseMapPath);
            if (tex != null) {
                material = material.withDiffuseMap(tex);
            }
        }
        if (obj.specularMapPath != null) {
            Texture tex = loadTextureSafe(obj.specularMapPath);
            if (tex != null) {
                material = material.withSpecularMap(tex);
            }
        }
        if (obj.normalMapPath != null) {
            Texture tex = loadTextureSafe(obj.normalMapPath);
            if (tex != null) {
                material = material.withNormalMap(tex, obj.normalMapStrength);
            }
        }
        if (obj.reflectivity > 0f) {
            Texture envTex = obj.environmentMapPath != null ? loadTextureSafe(obj.environmentMapPath) : null;
            // envTex bleibt null, wenn kein Pfad gesetzt ist ODER das Laden fehlschlägt —
            // PhongShader greift dann automatisch auf den prozeduralen Himmel-Fallback zurück.
            material = material.withReflection(envTex, obj.reflectivity, obj.fresnelF0);
        }
        return material;
    }

    private static Texture loadTextureSafe(String path) {
        try {
            return TEXTURE_MANAGER.loadFromFile(path);
        } catch (IOException ex) {
            System.err.println("Textur konnte nicht geladen werden: " + path + " (" + ex.getMessage() + ")");
            return null;
        }
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

    /**
     * Baut die {@link ShadowSource} für ein Licht: klassische Einzel-Map bei
     * {@code shadowCascades <= 1} (rückwärtskompatibel, identisch zu Phase 3/5), sonst eine
     * {@link CascadedShadowMap} mit der im Slot gewählten Kaskaden-Anzahl.
     */
    private static ShadowSource buildShadowMapIfEnabled(RayPhongSnapshot.Light light, List<Mesh> meshes,
                                                         List<Mat4> models, Vec3 sceneCenter, int resolution,
                                                         Vec3 cameraPos, Vec3 worldUp, float fovY, float aspect,
                                                         float cameraNear, float cameraFar) {
        if (!light.shadowEnabled) {
            return null;
        }

        List<SceneNode> casters = new ArrayList<SceneNode>();
        for (int i = 0; i < meshes.size(); i++) {
            casters.add(new SceneNode(meshes.get(i), models.get(i)));
        }

        if (light.shadowCascades <= 1) {
            Vec3 dir = sceneCenter.sub(light.position).normalize();
            Vec3 up = Math.abs(dir.y) > 0.98f ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
            Mat4 lightView = Mat4.lookAt(light.position, sceneCenter, up);
            Mat4 lightProjection = Mat4.perspective((float) Math.toRadians(65), 1f, 0.5f, 30f);
            return ShadowMap.render(casters, lightView, lightProjection, resolution, resolution);
        }

        Vec3[] basis = CascadedShadowMap.cameraBasis(cameraPos, sceneCenter, worldUp);
        return CascadedShadowMap.build(casters, light.position, sceneCenter,
                cameraPos, basis[0], basis[1], basis[2], fovY, aspect, cameraNear, cameraFar,
                light.shadowCascades, resolution);
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
