package com.dan.rayphong;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimaler Wavefront-OBJ-Parser: liest {@code v} (Positionen), optional {@code vn} (Normalen)
 * und {@code f} (Faces, beliebiges Polygon — wird als Fan trianguliert). Texturkoordinaten
 * ({@code vt}) werden erkannt und ignoriert (kein UV-Mapping in dieser Phase).
 *
 * <p>Face-Indizes im OBJ-Format sind 1-basiert und dürfen negativ (relativ zum Ende) sein;
 * beides wird unterstützt. Unterstützte Face-Schreibweisen: {@code f v}, {@code f v/vt},
 * {@code f v//vn}, {@code f v/vt/vn}, gemischt über die Ecken eines Faces hinweg.</p>
 *
 * <p>Fehlen Normalen im Textblock komplett, werden sie aus den Face-Normalen berechnet und
 * pro Vertex gemittelt (wie in {@link MeshFactory} bei den Standard-Primitiven).</p>
 */
public final class ObjLoader {

    private ObjLoader() {
    }

    public static final class ObjParseException extends RuntimeException {
        public ObjParseException(String message) {
            super(message);
        }
    }

    public static Mesh load(InputStream in) throws IOException {
        List<Vec3> positions = new ArrayList<Vec3>();
        List<Vec3> readNormals = new ArrayList<Vec3>();
        List<int[]> faceVertexIdx = new ArrayList<int[]>();   // je Face: Positions-Indizes (0-basiert)
        List<int[]> faceNormalIdx = new ArrayList<int[]>();   // parallel dazu, -1 = keine Normale angegeben

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int lineNo = 0;
        try {
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] tok = line.split("\\s+");
                if (tok.length == 0) {
                    continue;
                }
                if (tok[0].equals("v")) {
                    positions.add(parseVec3(tok, lineNo));
                } else if (tok[0].equals("vn")) {
                    readNormals.add(parseVec3(tok, lineNo));
                } else if (tok[0].equals("f")) {
                    parseFace(tok, positions.size(), readNormals.size(), faceVertexIdx, faceNormalIdx, lineNo);
                }
                // vt und alles andere (o, g, s, mtllib, usemtl, ...) wird ignoriert
            }
        } finally {
            reader.close();
        }

        if (positions.isEmpty()) {
            throw new ObjParseException("Keine Vertex-Positionen (v) im OBJ gefunden");
        }
        if (faceVertexIdx.isEmpty()) {
            throw new ObjParseException("Keine Faces (f) im OBJ gefunden");
        }

        boolean hasNormals = !readNormals.isEmpty();
        Vec3[] posArr = positions.toArray(new Vec3[0]);
        Vec3[] normalArr;
        int[] faces;

        if (hasNormals) {
            // OBJ erlaubt, dass dieselbe Position mit unterschiedlichen Normalen auftaucht
            // (z. B. scharfe Kanten) — wir übernehmen die im Face referenzierte Normale direkt
            // und lassen dafür Positionen ggf. dupliziert in einer neuen, konsistenten Vertex-Liste
            // zusammenfassen (Position+Normale-Paar = ein Vertex, wie bei MeshFactory.cube()).
            List<Vec3> outPositions = new ArrayList<Vec3>();
            List<Vec3> outNormals = new ArrayList<Vec3>();
            List<Integer> outFaces = new ArrayList<Integer>();

            for (int f = 0; f < faceVertexIdx.size(); f++) {
                int[] vIdx = faceVertexIdx.get(f);
                int[] nIdx = faceNormalIdx.get(f);
                for (int corner = 0; corner < vIdx.length; corner++) {
                    outPositions.add(posArr[vIdx[corner]]);
                    int ni = nIdx[corner];
                    outNormals.add(ni >= 0 ? readNormals.get(ni) : Vec3.ZERO);
                }
                // Fan-Triangulierung des (evtl. n-eckigen) Polygons
                for (int corner = 1; corner < vIdx.length - 1; corner++) {
                    int base = outPositions.size() - vIdx.length;
                    outFaces.add(base);
                    outFaces.add(base + corner);
                    outFaces.add(base + corner + 1);
                }
            }

            posArr = outPositions.toArray(new Vec3[0]);
            normalArr = outNormals.toArray(new Vec3[0]);
            faces = toIntArray(outFaces);

            // Falls einzelne Ecken keine Normale hatten (gemischtes OBJ) -> Face-Normale nachrechnen
            fillMissingNormals(posArr, normalArr, faces);
        } else {
            List<Integer> outFaces = new ArrayList<Integer>();
            for (int[] vIdx : faceVertexIdx) {
                for (int corner = 1; corner < vIdx.length - 1; corner++) {
                    outFaces.add(vIdx[0]);
                    outFaces.add(vIdx[corner]);
                    outFaces.add(vIdx[corner + 1]);
                }
            }
            faces = toIntArray(outFaces);
            normalArr = computeSmoothNormals(posArr, faces);
        }

        return new Mesh(posArr, normalArr, faces);
    }

    // ----------------------------------------------------------------------

    private static Vec3 parseVec3(String[] tok, int lineNo) {
        if (tok.length < 4) {
            throw new ObjParseException("Zeile " + lineNo + ": erwarte 3 Koordinaten, war: " + String.join(" ", tok));
        }
        try {
            return new Vec3(Float.parseFloat(tok[1]), Float.parseFloat(tok[2]), Float.parseFloat(tok[3]));
        } catch (NumberFormatException ex) {
            throw new ObjParseException("Zeile " + lineNo + ": ungültige Zahl in " + String.join(" ", tok));
        }
    }

    private static void parseFace(String[] tok, int vertexCount, int normalCount,
                                   List<int[]> outVertexIdx, List<int[]> outNormalIdx, int lineNo) {
        int cornerCount = tok.length - 1;
        if (cornerCount < 3) {
            throw new ObjParseException("Zeile " + lineNo + ": Face braucht mindestens 3 Ecken");
        }
        int[] vIdx = new int[cornerCount];
        int[] nIdx = new int[cornerCount];
        for (int i = 0; i < cornerCount; i++) {
            String[] parts = tok[i + 1].split("/", -1);
            vIdx[i] = resolveIndex(parts[0], vertexCount, lineNo);
            if (parts.length >= 3 && parts[2].length() > 0) {
                nIdx[i] = resolveIndex(parts[2], normalCount, lineNo);
            } else {
                nIdx[i] = -1;
            }
        }
        outVertexIdx.add(vIdx);
        outNormalIdx.add(nIdx);
    }

    /** OBJ-Indizes sind 1-basiert; negative Werte zählen relativ vom Ende der bisherigen Liste. */
    private static int resolveIndex(String raw, int currentCount, int lineNo) {
        int idx;
        try {
            idx = Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new ObjParseException("Zeile " + lineNo + ": ungültiger Face-Index '" + raw + "'");
        }
        if (idx > 0) {
            return idx - 1;
        } else if (idx < 0) {
            return currentCount + idx;
        }
        throw new ObjParseException("Zeile " + lineNo + ": Face-Index darf nicht 0 sein");
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static void fillMissingNormals(Vec3[] positions, Vec3[] normals, int[] faces) {
        for (int t = 0; t < faces.length / 3; t++) {
            int a = faces[t * 3];
            int b = faces[t * 3 + 1];
            int c = faces[t * 3 + 2];
            if (normals[a].length() < 1e-6f || normals[b].length() < 1e-6f || normals[c].length() < 1e-6f) {
                Vec3 fn = positions[b].sub(positions[a]).cross(positions[c].sub(positions[a])).normalize();
                if (normals[a].length() < 1e-6f) {
                    normals[a] = fn;
                }
                if (normals[b].length() < 1e-6f) {
                    normals[b] = fn;
                }
                if (normals[c].length() < 1e-6f) {
                    normals[c] = fn;
                }
            }
        }
    }

    /** Normalen aus den Face-Normalen berechnen und pro (geteiltem) Vertex mitteln. */
    private static Vec3[] computeSmoothNormals(Vec3[] positions, int[] faces) {
        Vec3[] accum = new Vec3[positions.length];
        for (int i = 0; i < accum.length; i++) {
            accum[i] = Vec3.ZERO;
        }
        for (int t = 0; t < faces.length / 3; t++) {
            int a = faces[t * 3];
            int b = faces[t * 3 + 1];
            int c = faces[t * 3 + 2];
            Vec3 fn = positions[b].sub(positions[a]).cross(positions[c].sub(positions[a]));
            accum[a] = accum[a].add(fn);
            accum[b] = accum[b].add(fn);
            accum[c] = accum[c].add(fn);
        }
        Vec3[] result = new Vec3[accum.length];
        for (int i = 0; i < result.length; i++) {
            Vec3 n = accum[i].normalize();
            result[i] = n.length() < 1e-6f ? new Vec3(0, 1, 0) : n;
        }
        return result;
    }
}
