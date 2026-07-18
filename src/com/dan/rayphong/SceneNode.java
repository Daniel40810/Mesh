package com.dan.rayphong;

/**
 * Ein Mesh mit seiner Platzierung in der Szene (Model-Matrix). Rein geometrischer Container —
 * kennt kein Material/Shader, damit er sowohl für den Shadow-Depth-Pass (nur Geometrie zählt)
 * als auch für den Color-Pass (Shader wird dort separat zugeordnet) wiederverwendbar ist.
 */
public final class SceneNode {

    public final Mesh mesh;
    public final Mat4 model;

    public SceneNode(Mesh mesh, Mat4 model) {
        this.mesh = mesh;
        this.model = model;
    }
}
