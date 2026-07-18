package com.dan.ftable;

import java.awt.Color;

/**
 * Wiederverwendbare Farb-Presets fuer {@link FTable} im ReagenzglasBar-/
 * Laboratory-Dark-Look. Setzt man {@code setStyle(...)}, werden alle Farb-
 * Properties der Tabelle in einem Schritt belegt (Styler-Ersatz).
 *
 * <p>Feste Auswahlmenge → Enum (Palette-freundliches Dropdown in NetBeans).</p>
 */
public enum FTableStyle {

    /** Standard: Tuerkis-Akzent, weinrote Selektion, dunkler Glaskoerper. */
    LAB_DARK(
            new Color(0x00B5AD),  // accent (Tuerkis)
            new Color(0x8B0024),  // selection (Weinrot)
            new Color(0x1B2530),  // rowA
            new Color(0x202A36),  // rowB
            new Color(0x141A24),  // body / Hintergrund
            new Color(0x2A1622),  // headerTop (dunkles Weinrot)
            new Color(0x141B26),  // headerBottom (dunkelblau)
            new Color(0xE8EEF4),  // text hell
            new Color(0x9FB0C0)), // text sekundaer

    /** Tuerkis-betont: kuehler, headerseitig Tuerkis statt Wein. */
    TURQUOISE_NIGHT(
            new Color(0x18E0D4),
            new Color(0x0E6E6A),
            new Color(0x122029),
            new Color(0x172832),
            new Color(0x0E161D),
            new Color(0x0C3A38),
            new Color(0x0E1820),
            new Color(0xEAF6F5),
            new Color(0x8FB7B4)),

    /** Wein-betont: warmer Look, weinrote Akzente. */
    WINE_NIGHT(
            new Color(0xE03A5E),
            new Color(0x8B0024),
            new Color(0x21161C),
            new Color(0x281A21),
            new Color(0x180F14),
            new Color(0x3A0E1E),
            new Color(0x1A0E14),
            new Color(0xF4E8EE),
            new Color(0xC09FAE));

    public final Color accent;
    public final Color selection;
    public final Color rowA;
    public final Color rowB;
    public final Color body;
    public final Color headerTop;
    public final Color headerBottom;
    public final Color text;
    public final Color textSecondary;

    FTableStyle(Color accent, Color selection, Color rowA, Color rowB, Color body,
                Color headerTop, Color headerBottom, Color text, Color textSecondary) {
        this.accent = accent;
        this.selection = selection;
        this.rowA = rowA;
        this.rowB = rowB;
        this.body = body;
        this.headerTop = headerTop;
        this.headerBottom = headerBottom;
        this.text = text;
        this.textSecondary = textSecondary;
    }
}
