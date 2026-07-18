package com.dan.rayphong.editor;

import com.dan.rayphong.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unveraenderliche Momentaufnahme des gesamten vom {@link RayPhongRenderer} gelesenen
 * Szenen-Zustands. Wird auf dem EDT (dem einzigen Thread, der {@link RayPhongScene}
 * mutiert) erzeugt und an den Render-Thread uebergeben.
 *
 * <p>Dadurch liest der Renderer niemals mehr aus dem lebenden, veraenderlichen
 * {@code RayPhongScene}: Es gibt keine Data Races zwischen EDT-Mutationen
 * (+/- Objekt, Preset-Wechsel, Kamera-Orbit, Slider, Auto-Rotate-Timer) und dem
 * Hintergrund-Rendern. Alle enthaltenen Referenztypen ({@link Vec3}, {@link Color},
 * {@link RayPhongScene.MeshKind}, {@link String}) sind selbst unveraenderlich, sodass
 * ein flaches Kopieren der Felder genuegt.</p>
 */
public final class RayPhongSnapshot {

    /** Unveraenderlicher Objekt-Zustand (Geometrie + Material). */
    public static final class Obj {
        public final RayPhongScene.MeshKind meshKind;
        public final String objPath;
        public final Vec3 position;
        public final float rotationY;
        public final Color baseColor;
        public final float ambientK;
        public final float diffuseK;
        public final float specularK;
        public final float shininess;

        Obj(RayPhongScene.ObjectSlot s) {
            this.meshKind = s.meshKind;
            this.objPath = s.objPath;
            this.position = s.position;   // Vec3 ist unveraenderlich
            this.rotationY = s.rotationY;
            this.baseColor = s.baseColor; // Color ist unveraenderlich
            this.ambientK = s.ambientK;
            this.diffuseK = s.diffuseK;
            this.specularK = s.specularK;
            this.shininess = s.shininess;
        }
    }

    /** Unveraenderlicher Licht-Zustand. */
    public static final class Light {
        public final Color color;
        public final float intensity;
        public final boolean shadowEnabled;
        public final Vec3 position;

        Light(RayPhongScene.LightSlot l) {
            this.color = l.color;
            this.intensity = l.intensity;
            this.shadowEnabled = l.shadowEnabled;
            this.position = l.position;
        }
    }

    public final List<Obj> objects;
    public final Light light1;
    public final Light light2;
    public final Color ambientColor;
    public final float ambientIntensity;
    public final int shadowResolution;
    public final float modelRotationY;
    public final float cameraYaw;
    public final float cameraPitch;
    public final float cameraDistance;
    public final int width;
    public final int height;

    /**
     * Zieht die Momentaufnahme. <b>Muss auf dem EDT aufgerufen werden</b>, damit die
     * gelesenen {@code scene}-Felder konsistent sind (der EDT ist der einzige Mutator).
     */
    public RayPhongSnapshot(RayPhongScene scene, int width, int height) {
        List<Obj> objs = new ArrayList<Obj>(scene.objects.size());
        for (RayPhongScene.ObjectSlot s : scene.objects) {
            objs.add(new Obj(s));
        }
        this.objects = Collections.unmodifiableList(objs);
        this.light1 = new Light(scene.light1);
        this.light2 = new Light(scene.light2);
        this.ambientColor = scene.ambientColor;
        this.ambientIntensity = scene.ambientIntensity;
        this.shadowResolution = scene.shadowResolution;
        this.modelRotationY = scene.modelRotationY;
        this.cameraYaw = scene.cameraYaw;
        this.cameraPitch = scene.cameraPitch;
        this.cameraDistance = scene.cameraDistance;
        this.width = width;
        this.height = height;
    }
}
