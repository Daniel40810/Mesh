package com.dan.rayphong;

import java.util.ArrayList;
import java.util.List;

/** Erzeugt Standard-Meshes. Rein statische Fabrikmethoden. */
public final class MeshFactory {

    private MeshFactory() {
    }

    /**
     * UV-Kugel um den Ursprung. Vertex-Normalen = normierte Position (glattes Shading).
     *
     * @param radius Radius
     * @param stacks Anzahl horizontaler Ringe (>= 2)
     * @param slices Anzahl vertikaler Segmente (>= 3)
     */
    public static Mesh sphere(float radius, int stacks, int slices) {
        List<Vec3> positions = new ArrayList<Vec3>();
        List<Vec3> normals = new ArrayList<Vec3>();

        for (int i = 0; i <= stacks; i++) {
            float v = (float) i / stacks;              // 0..1
            float phi = (float) (v * Math.PI);          // 0..PI (Pol zu Pol)
            float y = (float) Math.cos(phi);
            float ringRadius = (float) Math.sin(phi);

            for (int j = 0; j <= slices; j++) {
                float u = (float) j / slices;            // 0..1
                float theta = (float) (u * 2 * Math.PI);
                float x = ringRadius * (float) Math.cos(theta);
                float z = ringRadius * (float) Math.sin(theta);

                Vec3 n = new Vec3(x, y, z); // bereits Einheitsvektor auf Einheitskugel
                positions.add(n.scale(radius));
                normals.add(n);
            }
        }

        List<Integer> faces = new ArrayList<Integer>();
        int rowLen = slices + 1;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * rowLen + j;
                int b = a + rowLen;
                int c = a + 1;
                int d = b + 1;

                // zwei Dreiecke pro Quad, CCW von außen betrachtet (konsistent mit den
                // Auswärts-Vertex-Normalen — verifiziert per Debug-Test gegen faceNormal.dot(vertexNormal))
                faces.add(a);
                faces.add(c);
                faces.add(b);

                faces.add(c);
                faces.add(d);
                faces.add(b);
            }
        }

        return toMesh(positions, normals, faces);
    }

    /**
     * Würfel mit scharfen Kanten (24 Vertices, pro Seite eigene Flach-Normalen).
     *
     * @param size Kantenlänge
     */
    public static Mesh cube(float size) {
        float h = size / 2f;

        List<Vec3> positions = new ArrayList<Vec3>();
        List<Vec3> normals = new ArrayList<Vec3>();
        List<Integer> faces = new ArrayList<Integer>();

        // Für jede der 6 Seiten: 4 Vertices mit gemeinsamer Flach-Normalen, 2 Dreiecke (CCW nach außen)
        addQuadFace(positions, normals, faces,
                new Vec3(-h, -h, h), new Vec3(h, -h, h), new Vec3(h, h, h), new Vec3(-h, h, h),
                new Vec3(0, 0, 1)); // +Z (vorne)

        addQuadFace(positions, normals, faces,
                new Vec3(h, -h, -h), new Vec3(-h, -h, -h), new Vec3(-h, h, -h), new Vec3(h, h, -h),
                new Vec3(0, 0, -1)); // -Z (hinten)

        addQuadFace(positions, normals, faces,
                new Vec3(h, -h, h), new Vec3(h, -h, -h), new Vec3(h, h, -h), new Vec3(h, h, h),
                new Vec3(1, 0, 0)); // +X (rechts)

        addQuadFace(positions, normals, faces,
                new Vec3(-h, -h, -h), new Vec3(-h, -h, h), new Vec3(-h, h, h), new Vec3(-h, h, -h),
                new Vec3(-1, 0, 0)); // -X (links)

        addQuadFace(positions, normals, faces,
                new Vec3(-h, h, h), new Vec3(h, h, h), new Vec3(h, h, -h), new Vec3(-h, h, -h),
                new Vec3(0, 1, 0)); // +Y (oben)

        addQuadFace(positions, normals, faces,
                new Vec3(-h, -h, -h), new Vec3(h, -h, -h), new Vec3(h, -h, h), new Vec3(-h, -h, h),
                new Vec3(0, -1, 0)); // -Y (unten)

        return toMesh(positions, normals, faces);
    }

    /**
     * Flache, horizontale Bodenebene bei y=0 (Normale (0,1,0)), zentriert im Ursprung.
     * Gleiche Wickelrichtung wie die +Y-Seite von {@link #cube(float)} (verifiziert korrekt).
     */
    public static Mesh plane(float width, float depth) {
        float hw = width / 2f;
        float hd = depth / 2f;

        List<Vec3> positions = new ArrayList<Vec3>();
        List<Vec3> normals = new ArrayList<Vec3>();
        List<Integer> faces = new ArrayList<Integer>();

        addQuadFace(positions, normals, faces,
                new Vec3(-hw, 0, hd), new Vec3(hw, 0, hd), new Vec3(hw, 0, -hd), new Vec3(-hw, 0, -hd),
                new Vec3(0, 1, 0));

        return toMesh(positions, normals, faces);
    }

    /**
     * Torus (Donut) um den Ursprung, Rotationsachse = Y. Glatte Vertex-Normalen.
     *
     * @param majorRadius Abstand Ringmitte -> Rohrmitte
     * @param minorRadius Rohrradius (Dicke)
     * @param majorSegments Segmente um die große Ringachse (>= 3)
     * @param minorSegments Segmente um den Rohrquerschnitt (>= 3)
     */
    public static Mesh torus(float majorRadius, float minorRadius, int majorSegments, int minorSegments) {
        List<Vec3> positions = new ArrayList<Vec3>();
        List<Vec3> normals = new ArrayList<Vec3>();

        for (int i = 0; i <= majorSegments; i++) {
            float u = (float) (2 * Math.PI * i / majorSegments);
            float cu = (float) Math.cos(u);
            float su = (float) Math.sin(u);
            // Mittelpunkt des Rohrquerschnitts an diesem Punkt des großen Rings
            Vec3 ringCenter = new Vec3(cu * majorRadius, 0, su * majorRadius);

            for (int j = 0; j <= minorSegments; j++) {
                float v = (float) (2 * Math.PI * j / minorSegments);
                float cv = (float) Math.cos(v);
                float sv = (float) Math.sin(v);

                // Normale zeigt radial vom Rohrquerschnitt weg (unabhängig vom Radius selbst)
                Vec3 normal = new Vec3(cu * cv, sv, su * cv);
                Vec3 pos = ringCenter.add(normal.scale(minorRadius));

                positions.add(pos);
                normals.add(normal);
            }
        }

        List<Integer> faces = new ArrayList<Integer>();
        int rowLen = minorSegments + 1;
        for (int i = 0; i < majorSegments; i++) {
            for (int j = 0; j < minorSegments; j++) {
                int a = i * rowLen + j;
                int b = a + rowLen;
                int c = a + 1;
                int d = b + 1;

                // Gleiche Wickelrichtung wie die (gefixte) sphere() — verifiziert per Debug-Test
                faces.add(a);
                faces.add(c);
                faces.add(b);

                faces.add(c);
                faces.add(d);
                faces.add(b);
            }
        }

        return toMesh(positions, normals, faces);
    }

    private static void addQuadFace(List<Vec3> positions, List<Vec3> normals, List<Integer> faces,
                                     Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 normal) {
        int base = positions.size();
        positions.add(p0);
        positions.add(p1);
        positions.add(p2);
        positions.add(p3);
        for (int i = 0; i < 4; i++) {
            normals.add(normal);
        }
        faces.add(base);
        faces.add(base + 1);
        faces.add(base + 2);
        faces.add(base);
        faces.add(base + 2);
        faces.add(base + 3);
    }

    private static Mesh toMesh(List<Vec3> positions, List<Vec3> normals, List<Integer> faces) {
        Vec3[] pArr = positions.toArray(new Vec3[0]);
        Vec3[] nArr = normals.toArray(new Vec3[0]);
        int[] fArr = new int[faces.size()];
        for (int i = 0; i < fArr.length; i++) {
            fArr[i] = faces.get(i);
        }
        return new Mesh(pArr, nArr, fArr);
    }
}
