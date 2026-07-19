package com.dan.rayphong;

/**
 * Gemeinsame Abstraktion für alles, was einem {@link PhongShader} einen Schattenfaktor für
 * einen Weltpunkt liefern kann — eine einzelne {@link ShadowMap} (Phase 3/5) genauso wie eine
 * {@link CascadedShadowMap} (mehrere Shadow Maps, je nach Kamera-Abstand ausgewählt).
 *
 * <p>{@code cameraPos} wird durchgereicht, weil nur die Kaskaden-Variante ihn braucht (um die
 * passende Kaskade zu wählen); eine einzelne {@link ShadowMap} ignoriert ihn einfach.</p>
 */
public interface ShadowSource {
    /**
     * @return Schattenfaktor: 1.0 = voll beleuchtet, 0.0 = voll verschattet, Zwischenwerte an
     *         weichen PCF-Rändern (siehe {@link ShadowMap#sampleShadowFactor}).
     */
    float sampleShadowFactor(Vec3 worldPos, float ndotl, Vec3 cameraPos);
}
