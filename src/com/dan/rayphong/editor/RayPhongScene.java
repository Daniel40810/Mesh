package com.dan.rayphong.editor;

import com.dan.rayphong.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Kompletter editierbarer Zustand einer RayPhong-Szene. Reines Datenobjekt — der
 * {@link RayPhongRenderer} liest daraus, die Editor-UI schreibt hinein.
 *
 * <p>Seit Phase 6 trägt die Szene eine Liste von {@link ObjectSlot}s statt eines einzelnen
 * Objekts — Grundlage für Mehrobjekt-Szenen und Preset-Wechsel.</p>
 */
public final class RayPhongScene {

    public enum MeshKind { SPHERE, CUBE, TORUS, OBJ }

    public static final class ObjectSlot {
        public MeshKind meshKind = MeshKind.SPHERE;
        public String objPath;       // nur relevant wenn meshKind == OBJ
        public Vec3 position;
        public float rotationY;
        public Color baseColor;
        public float ambientK;
        public float diffuseK;
        public float specularK;
        public float shininess;

        // Optionale Texturen — null bedeutet "keine", genau wie bei objPath.
        public String diffuseMapPath;
        public String specularMapPath;
        public String normalMapPath;
        public float normalMapStrength = 1.0f; // 0 = kein Effekt, 1 = normal, >1 = verstärkt

        // Environment-Reflexion (Fresnel) — siehe PhongMaterial.withReflection().
        public String environmentMapPath; // null = kein Environment-Bild, prozeduraler Himmel-Fallback
        public float reflectivity = 0f;   // 0 = kein Spiegel-Effekt (Standard), 1 = voll
        public float fresnelF0 = 0.04f;   // Basisreflexion bei Frontalblick: 0.02-0.05 dielektrisch, 0.5+ Metall

        public ObjectSlot(MeshKind kind, Vec3 position, Color baseColor) {
            this.meshKind = kind;
            this.position = position;
            this.baseColor = baseColor;
            this.ambientK = 0.15f;
            this.diffuseK = 0.6f;
            this.specularK = 0.5f;
            this.shininess = 48f;
        }
    }

    public static final class LightSlot {
        public Color color;
        public float intensity;
        public boolean shadowEnabled;
        public Vec3 position;
        /** 1 = klassische Einzel-Shadow-Map (Standard, rückwärtskompatibel), >1 = Cascaded Shadow Maps. */
        public int shadowCascades = 1;

        public LightSlot(Color color, float intensity, boolean shadowEnabled, Vec3 position) {
            this.color = color;
            this.intensity = intensity;
            this.shadowEnabled = shadowEnabled;
            this.position = position;
        }
    }

    public List<ObjectSlot> objects = new ArrayList<ObjectSlot>();
    public int selectedObjectIndex = 0;

    // Lichter (2 — Erweiterung auf mehr ist mechanisch identisch, siehe README)
    public LightSlot light1 = new LightSlot(new Color(0x00, 0xB5, 0xAD), 3.0f, true, new Vec3(-3.2f, 4.5f, 3.5f));
    public LightSlot light2 = new LightSlot(new Color(0x8B, 0x00, 0x24), 2.2f, false, new Vec3(3.5f, 2.0f, 2.0f));

    public Color ambientColor = new Color(0x20, 0x22, 0x2C);
    public float ambientIntensity = 0.3f;

    // Boden-Material
    public String groundDiffuseMapPath; // null = keine Textur, reine Uniform-Farbe wie bisher
    public float groundTiling = 4.0f;   // Wiederholungen der Textur über die gesamte Bodenbreite/-tiefe

    // Szene / Qualität
    public int shadowResolution = 1024;
    public boolean autoRotate = true;
    public float modelRotationY = 0f; // wird vom Auto-Rotate-Timer fortgeschrieben, gilt für alle Objekte

    // Kamera-Orbit (per Maus gesteuert)
    public float cameraYaw = 0.5f;
    public float cameraPitch = 0.28f;
    public float cameraDistance = 8.5f;

    public RayPhongScene() {
        objects.add(new ObjectSlot(MeshKind.SPHERE, new Vec3(0f, 1.3f, 0f), new Color(0x00, 0xB5, 0xAD)));
    }

    public ObjectSlot selected() {
        if (objects.isEmpty()) {
            return null;
        }
        int idx = Math.max(0, Math.min(objects.size() - 1, selectedObjectIndex));
        return objects.get(idx);
    }

    // ------------------------------------------------------------ Presets

    public static RayPhongScene presetStudio() {
        RayPhongScene s = new RayPhongScene();
        s.objects.clear();
        s.objects.add(new ObjectSlot(MeshKind.SPHERE, new Vec3(0f, 1.3f, 0f), new Color(0x00, 0xB5, 0xAD)));
        s.objects.get(0).specularK = 0.85f;
        s.objects.get(0).shininess = 128f;
        s.light2.shadowEnabled = false;
        s.light2.intensity = 1.4f;
        return s;
    }

    public static RayPhongScene presetLabor() {
        RayPhongScene s = new RayPhongScene();
        s.objects.clear();
        s.objects.add(new ObjectSlot(MeshKind.SPHERE, new Vec3(-2.1f, 1.1f, 0f), new Color(0x00, 0xB5, 0xAD)));
        s.objects.add(new ObjectSlot(MeshKind.CUBE, new Vec3(0f, 0.9f, 0f), new Color(0x1E, 0x7A, 0xFF)));
        s.objects.add(new ObjectSlot(MeshKind.TORUS, new Vec3(2.2f, 1.0f, 0f), new Color(0x8B, 0x00, 0x24)));
        s.objects.get(2).specularK = 0.85f;
        s.objects.get(2).shininess = 128f;
        s.light1.shadowEnabled = true;
        s.light2.shadowEnabled = true;
        s.cameraDistance = 10.5f;
        return s;
    }

    public static RayPhongScene presetEdelstein() {
        RayPhongScene s = new RayPhongScene();
        s.objects.clear();
        ObjectSlot gem = new ObjectSlot(MeshKind.OBJ, new Vec3(-1.3f, 1.4f, 0f), new Color(0x00, 0xB5, 0xAD));
        gem.objPath = "/home/claude/rayphong/assets/gem.obj";
        gem.specularK = 0.85f;
        gem.shininess = 160f;
        ObjectSlot pyramid = new ObjectSlot(MeshKind.OBJ, new Vec3(1.3f, 1.6f, 0f), new Color(0xB8, 0xC4, 0xCE));
        pyramid.objPath = "/home/claude/rayphong/assets/pyramid.obj";
        pyramid.specularK = 0.2f;
        pyramid.shininess = 16f;
        s.objects.add(gem);
        s.objects.add(pyramid);
        s.autoRotate = true;
        s.cameraDistance = 9f;
        return s;
    }
}
