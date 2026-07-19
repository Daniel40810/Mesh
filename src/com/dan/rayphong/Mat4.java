package com.dan.rayphong;

/**
 * 4x4-Matrix (Column-Major, wie OpenGL/klassische Grafik-Pipelines).
 * m[col * 4 + row].
 */
public final class Mat4 {

    public final float[] m; // 16 Werte, Column-Major

    private Mat4(float[] m) {
        this.m = m;
    }

    public static Mat4 identity() {
        float[] v = new float[16];
        v[0] = 1;
        v[5] = 1;
        v[10] = 1;
        v[15] = 1;
        return new Mat4(v);
    }

    public static Mat4 translation(float x, float y, float z) {
        Mat4 r = identity();
        r.m[12] = x;
        r.m[13] = y;
        r.m[14] = z;
        return r;
    }

    public static Mat4 scale(float sx, float sy, float sz) {
        Mat4 r = identity();
        r.m[0] = sx;
        r.m[5] = sy;
        r.m[10] = sz;
        return r;
    }

    public static Mat4 rotationX(float radians) {
        Mat4 r = identity();
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        r.m[5] = c;
        r.m[6] = s;
        r.m[9] = -s;
        r.m[10] = c;
        return r;
    }

    public static Mat4 rotationY(float radians) {
        Mat4 r = identity();
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        r.m[0] = c;
        r.m[2] = -s;
        r.m[8] = s;
        r.m[10] = c;
        return r;
    }

    public static Mat4 rotationZ(float radians) {
        Mat4 r = identity();
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        r.m[0] = c;
        r.m[1] = s;
        r.m[4] = -s;
        r.m[5] = c;
        return r;
    }

    /** Rechtshändiges Blickfeld, klassisches lookAt (eye, target, up). */
    public static Mat4 lookAt(Vec3 eye, Vec3 target, Vec3 up) {
        Vec3 f = target.sub(eye).normalize();       // forward
        Vec3 s = f.cross(up).normalize();           // right
        Vec3 u = s.cross(f);                        // wahres up

        float[] v = new float[16];
        v[0] = s.x;
        v[4] = s.y;
        v[8] = s.z;
        v[1] = u.x;
        v[5] = u.y;
        v[9] = u.z;
        v[2] = -f.x;
        v[6] = -f.y;
        v[10] = -f.z;
        v[12] = -s.dot(eye);
        v[13] = -u.dot(eye);
        v[14] = f.dot(eye);
        v[15] = 1;
        return new Mat4(v);
    }

    /** Perspektivische Projektion. fovYRadians = vertikales Sichtfeld, aspect = w/h. */
    public static Mat4 perspective(float fovYRadians, float aspect, float near, float far) {
        float tanHalfFovy = (float) Math.tan(fovYRadians / 2.0);
        float[] v = new float[16];
        v[0] = 1f / (aspect * tanHalfFovy);
        v[5] = 1f / tanHalfFovy;
        v[10] = -(far + near) / (far - near);
        v[11] = -1f;
        v[14] = -(2f * far * near) / (far - near);
        return new Mat4(v);
    }

    /**
     * Orthographische Projektion (kein perspektivischer Verzug) — für Cascaded Shadow Maps,
     * wo jede Kaskade eine eng an ihr Kamera-Frustum-Segment angepasste Box aus Licht-Sicht
     * bekommt. Gleiche Blickrichtungs-Konvention wie {@link #perspective}: Kamera blickt
     * entlang -Z, {@code near}/{@code far} sind positive Distanzen (near &lt; far).
     *
     * @param left/right/bottom/top Bounding-Box-Grenzen in Licht-View-Raum (x/y)
     * @param near/far positive Distanzen entlang -Z, die auf NDC [-1,1] abgebildet werden
     */
    public static Mat4 orthographic(float left, float right, float bottom, float top, float near, float far) {
        float[] v = new float[16];
        v[0] = 2f / (right - left);
        v[5] = 2f / (top - bottom);
        v[10] = -2f / (far - near);
        v[12] = -(right + left) / (right - left);
        v[13] = -(top + bottom) / (top - bottom);
        v[14] = -(far + near) / (far - near);
        v[15] = 1f;
        return new Mat4(v);
    }

    /** this * o (this wird zuerst logisch "innen" angewendet: result = this ⨯ o). */
    public Mat4 multiply(Mat4 o) {
        float[] a = this.m;
        float[] b = o.m;
        float[] r = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0f;
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + row] * b[col * 4 + k];
                }
                r[col * 4 + row] = sum;
            }
        }
        return new Mat4(r);
    }

    public Vec4 transform(Vec3 p, float w) {
        float x = p.x, y = p.y, z = p.z;
        float rx = m[0] * x + m[4] * y + m[8] * z + m[12] * w;
        float ry = m[1] * x + m[5] * y + m[9] * z + m[13] * w;
        float rz = m[2] * x + m[6] * y + m[10] * z + m[14] * w;
        float rw = m[3] * x + m[7] * y + m[11] * z + m[15] * w;
        return new Vec4(rx, ry, rz, rw);
    }

    /** Transformiert einen Richtungsvektor (Normalen) ohne Translation, w=0. */
    public Vec3 transformDirection(Vec3 dir) {
        Vec4 r = transform(dir, 0f);
        return new Vec3(r.x, r.y, r.z);
    }

    /**
     * Normal-Matrix: inverse Transponierte der oberen 3x3 der Model-Matrix.
     * Für reine Rotation/gleichförmige Skalierung genügt die 3x3 der Model-Matrix selbst
     * (Rotation ist orthogonal) — hier vereinfachte Variante, die bei ungleichförmiger Skalierung
     * korrigiert werden müsste. Ausreichend für Phase 1/2.
     */
    public Mat4 upper3x3AsMat4() {
        float[] v = new float[16];
        v[0] = m[0];
        v[1] = m[1];
        v[2] = m[2];
        v[4] = m[4];
        v[5] = m[5];
        v[6] = m[6];
        v[8] = m[8];
        v[9] = m[9];
        v[10] = m[10];
        v[15] = 1;
        return new Mat4(v);
    }
}
