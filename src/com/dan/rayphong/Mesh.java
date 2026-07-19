package com.dan.rayphong;

/**
 * Statisches Dreiecksnetz: Vertex-Positionen, Vertex-Normalen (für glattes Shading),
 * UV-Koordinaten und Tangenten (für Tangent-Space Normal Mapping) plus Dreiecks-Indizes.
 * Java 8 kompatibel.
 */
public final class Mesh {

    public final Vec3[] positions;
    public final Vec3[] normals;   // gleiche Länge wie positions, geglättete Vertex-Normalen
    public final Vec2[] uvs;       // gleiche Länge wie positions, Textur-Koordinaten (0..1)
    public final Vec3[] tangents;  // gleiche Länge wie positions, normiert, orthogonal zu normals
    public final int[] faces;      // je 3 Einträge = ein Dreieck (Indizes in positions/normals/uvs)

    /**
     * Bequemer Konstruktor ohne UVs — füllt {@link #uvs} mit {@link Vec2#ZERO}.
     * Bestehender Code (MeshFactory-Primitive vor Texture-Support, alte Aufrufer)
     * bleibt so unverändert lauffähig. Ohne echte UVs sind die berechneten Tangenten
     * beliebig orientiert (nur für Normal Mapping relevant, das ohne Diffuse-UVs ohnehin
     * nicht sinnvoll einsetzbar ist).
     */
    public Mesh(Vec3[] positions, Vec3[] normals, int[] faces) {
        this(positions, normals, defaultUvs(positions.length), faces);
    }

    public Mesh(Vec3[] positions, Vec3[] normals, Vec2[] uvs, int[] faces) {
        if (positions.length != normals.length) {
            throw new IllegalArgumentException("positions und normals müssen gleich lang sein");
        }
        if (positions.length != uvs.length) {
            throw new IllegalArgumentException("positions und uvs müssen gleich lang sein");
        }
        if (faces.length % 3 != 0) {
            throw new IllegalArgumentException("faces müssen ein Vielfaches von 3 sein");
        }
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.faces = faces;
        this.tangents = computeTangents(positions, normals, uvs, faces);
    }

    private static Vec2[] defaultUvs(int count) {
        Vec2[] result = new Vec2[count];
        java.util.Arrays.fill(result, Vec2.ZERO);
        return result;
    }

    /**
     * Berechnet pro-Vertex-Tangenten nach dem Standardverfahren (Lengyel/"Foundations of Game
     * Engine Development"): pro Dreieck aus Positions- und UV-Deltas eine Face-Tangente lösen,
     * auf allen 3 Ecken akkumulieren, anschließend pro Vertex per Gram-Schmidt orthogonal zur
     * (bereits gemittelten) Vertex-Normale machen und normieren.
     *
     * <p>Vereinfachung: Es wird KEINE Bitangenten-Händigkeit (w-Vorzeichen) gespeichert — für
     * gespiegelte UV-Inseln (z. B. symmetrische Charaktermodelle mit halbem UV-Layout) müsste
     * das ergänzt werden. Für die aktuellen Primitive und die meisten OBJ-Assets ohne
     * UV-Spiegelung ist das ausreichend.</p>
     */
    private static Vec3[] computeTangents(Vec3[] positions, Vec3[] normals, Vec2[] uvs, int[] faces) {
        Vec3[] accum = new Vec3[positions.length];
        for (int i = 0; i < accum.length; i++) {
            accum[i] = Vec3.ZERO;
        }

        for (int t = 0; t < faces.length / 3; t++) {
            int a = faces[t * 3];
            int b = faces[t * 3 + 1];
            int c = faces[t * 3 + 2];

            Vec3 edge1 = positions[b].sub(positions[a]);
            Vec3 edge2 = positions[c].sub(positions[a]);
            Vec2 duv1 = uvs[b].sub(uvs[a]);
            Vec2 duv2 = uvs[c].sub(uvs[a]);

            float denom = duv1.u * duv2.v - duv2.u * duv1.v;
            if (Math.abs(denom) < 1e-8f) {
                continue; // entartetes UV-Dreieck (z. B. Default-UVs alle 0) — keine Aussage möglich
            }
            float f = 1f / denom;
            Vec3 faceTangent = new Vec3(
                    f * (duv2.v * edge1.x - duv1.v * edge2.x),
                    f * (duv2.v * edge1.y - duv1.v * edge2.y),
                    f * (duv2.v * edge1.z - duv1.v * edge2.z)
            );

            accum[a] = accum[a].add(faceTangent);
            accum[b] = accum[b].add(faceTangent);
            accum[c] = accum[c].add(faceTangent);
        }

        Vec3[] result = new Vec3[positions.length];
        for (int i = 0; i < result.length; i++) {
            Vec3 n = normals[i];
            Vec3 t = accum[i];
            // Gram-Schmidt: Tangente orthogonal zur Normale erzwingen
            Vec3 orthoT = t.sub(n.scale(n.dot(t)));
            if (orthoT.length() < 1e-6f) {
                // Keine (verwertbare) Tangente akkumuliert — beliebige, aber gültige Orthogonale zur Normale
                Vec3 helper = Math.abs(n.y) < 0.99f ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                orthoT = helper.cross(n);
                if (orthoT.length() < 1e-6f) {
                    orthoT = new Vec3(1, 0, 0); // n war (1,0,0) selbst — letzter Fallback
                }
            }
            result[i] = orthoT.normalize();
        }
        return result;
    }

    public int triangleCount() {
        return faces.length / 3;
    }
}
