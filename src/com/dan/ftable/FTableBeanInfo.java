package com.dan.ftable;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 * Palette-Icon fuer {@link FTable}: gemaltes Tabellengitter mit Wein→Tuerkis-
 * Header, alternierenden Zeilen und Glas-Rim. Keine externen Bild-Assets.
 */
public class FTableBeanInfo extends SimpleBeanInfo {

    @Override
    public java.awt.Image getIcon(int iconKind) {
        switch (iconKind) {
            case BeanInfo.ICON_COLOR_16x16:
            case BeanInfo.ICON_MONO_16x16:
                return paint(16);
            case BeanInfo.ICON_COLOR_32x32:
            case BeanInfo.ICON_MONO_32x32:
                return paint(32);
            default:
                return null;
        }
    }

    private BufferedImage paint(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float scale = s / 32f;
        int pad   = Math.round(2 * scale);
        int w     = s - 2 * pad;
        int h     = s - 2 * pad;
        int arc   = Math.round(6 * scale);
        int headH = Math.round(8 * scale);

        // dunkler Glaskoerper
        g.setColor(new Color(0x141A24));
        g.fillRoundRect(pad, pad, w, h, arc, arc);

        // Header: Wein → Blau + Tuerkis-Akzentlinie
        java.awt.Shape oldClip = g.getClip();
        g.clip(new java.awt.geom.RoundRectangle2D.Float(pad, pad, w, h, arc, arc));
        g.setPaint(new GradientPaint(0, pad, new Color(0x2A1622), 0, pad + headH, new Color(0x141B26)));
        g.fillRect(pad, pad, w, headH);
        g.setColor(new Color(0x00B5AD));
        g.fillRect(pad, pad + headH - Math.max(1, Math.round(scale)), w, Math.max(1, Math.round(scale)));

        // alternierende Zeilen
        int rows = (s == 16) ? 2 : 3;
        int rowH = (h - headH) / (rows + 1);
        for (int i = 0; i < rows; i++) {
            int ry = pad + headH + i * rowH;
            g.setColor(((i & 1) == 0) ? new Color(0x1B2530) : new Color(0x202A36));
            g.fillRect(pad, ry, w, rowH);
        }
        // Hover-Glow auf einer Zeile (Tuerkis-Schleier + Akzentbalken)
        int gy = pad + headH + rowH * (rows - 1);
        g.setColor(new Color(0x00, 0xB5, 0xAD, 70));
        g.fillRect(pad, gy, w, rowH);
        g.setColor(new Color(0x00B5AD));
        g.fillRect(pad, gy, Math.max(1, Math.round(2 * scale)), rowH);
        g.setClip(oldClip);

        // Spaltentrenner
        g.setColor(new Color(255, 255, 255, 28));
        int colX = pad + Math.round(w * 0.5f);
        g.drawLine(colX, pad + headH, colX, pad + h - 2);

        // Glas-Rim
        g.setColor(new Color(140, 200, 255, 150));
        g.drawRoundRect(pad, pad, w - 1, h - 1, arc, arc);

        g.dispose();
        return img;
    }
}
