package com.dan.ffilechooser;

/**
 * Betriebsmodus des {@link FFileChooser}.
 */
public enum FFileChooserMode {
    /** Eine bestehende Datei zum Öffnen auswählen. */
    OPEN_FILE,
    /** Dateinamen zum Speichern eingeben/auswählen. */
    SAVE_FILE,
    /** Nur Ordner sind auswählbar (kein Datei-Listing). */
    SELECT_DIRECTORY
}
