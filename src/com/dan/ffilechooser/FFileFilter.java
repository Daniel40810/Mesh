package com.dan.ffilechooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Einfacher, unveränderlicher Datei-Endungsfilter für {@link FFileChooser}.
 * Ordner werden von {@link #accept(File)} immer akzeptiert (für die Navigation).
 */
public final class FFileFilter {

    /** Vordefinierter Filter, der jede Datei akzeptiert. */
    public static final FFileFilter ALL_FILES = new FFileFilter("Alle Dateien");

    private final String description;
    private final List<String> extensions;

    public FFileFilter(String description, String... extensions) {
        this.description = description;
        List<String> ext = new ArrayList<>();
        for (String e : extensions) {
            ext.add(e.toLowerCase(Locale.ROOT));
        }
        this.extensions = ext;
    }

    /** true, wenn die Datei zu diesem Filter passt (Ordner immer true). */
    public boolean accept(File f) {
        if (f == null) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        if (extensions.isEmpty()) {
            return true;
        }
        String name = f.getName().toLowerCase(Locale.ROOT);
        for (String ext : extensions) {
            if (name.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getExtensions() {
        return new ArrayList<>(extensions);
    }

    /** Anzeigetext für die Filter-ComboBox, z. B. "Bilder (*.png, *.jpg)". */
    public String getDescription() {
        if (extensions.isEmpty()) {
            return description;
        }
        StringBuilder sb = new StringBuilder(description).append(" (");
        for (int i = 0; i < extensions.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("*.").append(extensions.get(i));
        }
        return sb.append(")").toString();
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /** Bequemer Bild-Filter (PNG/JPG/JPEG/GIF/BMP). */
    public static FFileFilter images() {
        return new FFileFilter("Bilder", "png", "jpg", "jpeg", "gif", "bmp");
    }
}
