package com.dan.rayphong;

/**
 * Statisches Dreiecksnetz: Vertex-Positionen, Vertex-Normalen (für glattes Shading)
 * und Dreiecks-Indizes. Java 8 kompatibel.
 */
public final class Mesh {

    public final Vec3[] positions;
    public final Vec3[] normals;   // gleiche Länge wie positions, geglättete Vertex-Normalen
    public final int[] faces;      // je 3 Einträge = ein Dreieck (Indizes in positions/normals)

    public Mesh(Vec3[] positions, Vec3[] normals, int[] faces) {
        if (positions.length != normals.length) {
            throw new IllegalArgumentException("positions und normals müssen gleich lang sein");
        }
        if (faces.length % 3 != 0) {
            throw new IllegalArgumentException("faces müssen ein Vielfaches von 3 sein");
        }
        this.positions = positions;
        this.normals = normals;
        this.faces = faces;
    }

    public int triangleCount() {
        return faces.length / 3;
    }
}
