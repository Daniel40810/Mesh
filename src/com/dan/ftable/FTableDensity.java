package com.dan.ftable;

/**
 * Zeilen-/Header-Dichte fuer {@link FTable}. JavaBean-/Palette-freundlich:
 * NetBeans bietet die Werte als Dropdown im Property-Sheet an.
 */
public enum FTableDensity {
    /** Eng gepackt: kompakte Listen, viele Zeilen sichtbar. */
    COMPACT(26, 30),
    /** Standard: ausgewogenes Verhaeltnis. */
    NORMAL(34, 40),
    /** Luftig: viel Atemraum pro Zeile. */
    COMFORTABLE(44, 50);

    /** Hoehe einer Datenzeile in px. */
    public final int rowHeight;
    /** Hoehe des Spaltenkopfs in px. */
    public final int headerHeight;

    FTableDensity(int rowHeight, int headerHeight) {
        this.rowHeight = rowHeight;
        this.headerHeight = headerHeight;
    }
}
