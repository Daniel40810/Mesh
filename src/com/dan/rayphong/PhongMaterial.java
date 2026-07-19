package com.dan.rayphong;

import com.dan.rayphong.texture.Texture;

import java.awt.Color;

/**
 * Material-Parameter für die Phong-Beleuchtungsgleichung.
 * Ambient/Diffuse/Specular-Koeffizienten steuern, wie stark jede Komponente einfließt;
 * {@code baseColor} färbt Ambient+Diffuse, {@code specularColor} meist neutral (weiß) für
 * "glänzende" statt "gefärbte" Reflexe.
 *
 * <p>Seit dem Texture-Support (siehe {@code com.dan.rayphong.texture}) können optional
 * {@link #diffuseMap} und {@link #specularMap} gesetzt werden — sind sie {@code null},
 * verhält sich das Material exakt wie zuvor (reine Vertex-/Uniform-Farbe).</p>
 */
public final class PhongMaterial {

    public final Color baseColor;
    public final Color specularColor;
    public final float ambientK;
    public final float diffuseK;
    public final float specularK;
    public final float shininess; // Glanzexponent, typ. 1 (matt) .. 200 (sehr glänzend)

    /** Optional: moduliert baseColor pro Pixel. {@code null} = keine Textur, reine Uniform-Farbe. */
    public final Texture diffuseMap;
    /** Optional: moduliert specularK pro Pixel (Graustufen-Map, z. B. Glanz-Maske). {@code null} = kein Effekt. */
    public final Texture specularMap;
    /** Optional: Tangent-Space Normal Map, verzerrt die Schattierungs-Normale pro Pixel. {@code null} = kein Effekt. */
    public final Texture normalMap;
    /** Intensität der Normal-Map-Verzerrung: 0 = kein Effekt, 1 = wie kodiert, >1 = verstärkt. */
    public final float normalMapStrength;

    /**
     * Optional: äquirektangulare Environment-Map (u = Azimut 0..1, v = Polarwinkel 0..1) für
     * Spiegelungen. {@code null} = prozeduraler Himmel-Fallback (siehe {@link PhongShader}) —
     * so gibt's auch ohne geladene Textur sofort einen plausiblen Spiegel-Effekt.
     */
    public final Texture environmentMap;
    /** Grund-Reflexionsstärke, 0 = kein Spiegel-Effekt (Standard, exakt wie vor Fresnel-Support), 1 = voll. */
    public final float reflectivity;
    /**
     * Fresnel-Basisreflexion bei senkrechtem Blick (Schlick-Näherung). Typ. 0.02–0.05 für
     * Dielektrika (Kunststoff, Glas, Lack), 0.5–1.0 für Metalle (dort ist praktisch die ganze
     * Reflexion "Fresnel", das Metall hat kaum diffuse Farbe).
     */
    public final float fresnelF0;

    public PhongMaterial(Color baseColor, Color specularColor,
                          float ambientK, float diffuseK, float specularK, float shininess) {
        this(baseColor, specularColor, ambientK, diffuseK, specularK, shininess, null, null, null, 1f);
    }

    public PhongMaterial(Color baseColor, Color specularColor,
                          float ambientK, float diffuseK, float specularK, float shininess,
                          Texture diffuseMap, Texture specularMap) {
        this(baseColor, specularColor, ambientK, diffuseK, specularK, shininess, diffuseMap, specularMap, null, 1f);
    }

    public PhongMaterial(Color baseColor, Color specularColor,
                          float ambientK, float diffuseK, float specularK, float shininess,
                          Texture diffuseMap, Texture specularMap, Texture normalMap) {
        this(baseColor, specularColor, ambientK, diffuseK, specularK, shininess, diffuseMap, specularMap, normalMap, 1f);
    }

    public PhongMaterial(Color baseColor, Color specularColor,
                          float ambientK, float diffuseK, float specularK, float shininess,
                          Texture diffuseMap, Texture specularMap, Texture normalMap, float normalMapStrength) {
        this(baseColor, specularColor, ambientK, diffuseK, specularK, shininess,
                diffuseMap, specularMap, normalMap, normalMapStrength, null, 0f, 0.04f);
    }

    public PhongMaterial(Color baseColor, Color specularColor,
                          float ambientK, float diffuseK, float specularK, float shininess,
                          Texture diffuseMap, Texture specularMap, Texture normalMap, float normalMapStrength,
                          Texture environmentMap, float reflectivity, float fresnelF0) {
        this.baseColor = baseColor;
        this.specularColor = specularColor;
        this.ambientK = ambientK;
        this.diffuseK = diffuseK;
        this.specularK = specularK;
        this.shininess = shininess;
        this.diffuseMap = diffuseMap;
        this.specularMap = specularMap;
        this.normalMap = normalMap;
        this.normalMapStrength = normalMapStrength;
        this.environmentMap = environmentMap;
        this.reflectivity = reflectivity;
        this.fresnelF0 = fresnelF0;
    }

    /** Neues Material mit denselben Koeffizienten, aber gesetzter Diffuse-Textur. */
    public PhongMaterial withDiffuseMap(Texture tex) {
        return new PhongMaterial(baseColor, specularColor, ambientK, diffuseK, specularK, shininess,
                tex, specularMap, normalMap, normalMapStrength, environmentMap, reflectivity, fresnelF0);
    }

    /** Neues Material mit denselben Koeffizienten, aber gesetzter Specular-Textur. */
    public PhongMaterial withSpecularMap(Texture tex) {
        return new PhongMaterial(baseColor, specularColor, ambientK, diffuseK, specularK, shininess,
                diffuseMap, tex, normalMap, normalMapStrength, environmentMap, reflectivity, fresnelF0);
    }

    /** Neues Material mit denselben Koeffizienten, aber gesetzter Normal-Map (Stärke = 1.0). */
    public PhongMaterial withNormalMap(Texture tex) {
        return withNormalMap(tex, 1f);
    }

    /** Neues Material mit denselben Koeffizienten, aber gesetzter Normal-Map und Stärke. */
    public PhongMaterial withNormalMap(Texture tex, float strength) {
        return new PhongMaterial(baseColor, specularColor, ambientK, diffuseK, specularK, shininess,
                diffuseMap, specularMap, tex, strength, environmentMap, reflectivity, fresnelF0);
    }

    /**
     * Neues Material mit Fresnel-Spiegelreflexion. {@code envMap} kann {@code null} sein — dann
     * greift der prozedurale Himmel-Fallback in {@link PhongShader}. {@code reflectivity} ist
     * die Grundstärke (0 = aus), {@code fresnelF0} die Basisreflexion bei Frontalblick
     * (0.02–0.05 dielektrisch, 0.5+ metallisch).
     */
    public PhongMaterial withReflection(Texture envMap, float reflectivity, float fresnelF0) {
        return new PhongMaterial(baseColor, specularColor, ambientK, diffuseK, specularK, shininess,
                diffuseMap, specularMap, normalMap, normalMapStrength, envMap, reflectivity, fresnelF0);
    }

    /** Bequemlichkeits-Overload: nur Reflexionsstärke setzen, prozeduraler Himmel, dielektrisches F0=0.04. */
    public PhongMaterial withReflection(float reflectivity) {
        return withReflection(environmentMap, reflectivity, fresnelF0 > 0f ? fresnelF0 : 0.04f);
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
