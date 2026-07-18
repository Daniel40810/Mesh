package com.dan.rayphong;

import java.awt.Color;

/**
 * Material-Parameter für die Phong-Beleuchtungsgleichung.
 * Ambient/Diffuse/Specular-Koeffizienten steuern, wie stark jede Komponente einfließt;
 * {@code baseColor} färbt Ambient+Diffuse, {@code specularColor} meist neutral (weiß) für
 * "glänzende" statt "gefärbte" Reflexe.
 */
public final class PhongMaterial {

    public final Color baseColor;
    public final Color specularColor;
    public final float ambientK;
    public final float diffuseK;
    public final float specularK;
    public final float shininess; // Glanzexponent, typ. 1 (matt) .. 200 (sehr glänzend)

    public PhongMaterial(Color baseColor, Color specularColor,
                          float ambientK, float diffuseK, float specularK, float shininess) {
        this.baseColor = baseColor;
        this.specularColor = specularColor;
        this.ambientK = ambientK;
        this.diffuseK = diffuseK;
        this.specularK = specularK;
        this.shininess = shininess;
    }

    /** Ausgewogenes Standardmaterial mit mäßigem Glanz. */
    public static PhongMaterial standard(Color baseColor) {
        return new PhongMaterial(baseColor, Color.WHITE, 0.15f, 0.7f, 0.4f, 32f);
    }

    /** Mattes Material, kaum Specular (z. B. Gips/Ton). */
    public static PhongMaterial matte(Color baseColor) {
        return new PhongMaterial(baseColor, Color.WHITE, 0.2f, 0.85f, 0.05f, 4f);
    }

    /** Stark glänzendes Material mit engem, hellem Glanzpunkt (z. B. lackiert, Glas-Look). */
    public static PhongMaterial glossy(Color baseColor) {
        return new PhongMaterial(baseColor, Color.WHITE, 0.1f, 0.55f, 0.85f, 128f);
    }
}
