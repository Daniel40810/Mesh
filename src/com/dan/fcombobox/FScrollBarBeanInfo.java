package com.dan.fcombobox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo fuer {@link FScrollBar}. Zeichnet das Palette-Icon programmatisch:
 * dezenter Track mit einem pillenfoermigen Thumb im Weinrot&rarr;Tuerkis-Verlauf,
 * hellblauem Rim und Glanzstreifen.
 */
public class FScrollBarBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float trackW = Math.max(4f, size * 0.34f);
            float trackX = (size - trackW) / 2f;
            float trackR = trackW;

            // Track (dezent, hellblau-transparent).
            g2.setColor(new Color(140, 200, 255, 45));
            g2.fill(new java.awt.geom.RoundRectangle2D.Float(trackX, 2f, trackW, size - 4f, trackR, trackR));

            // Thumb (Pille, Weinrot->Tuerkis vertikal).
            float thumbX = trackX + 1f;
            float thumbW = trackW - 2f;
            float thumbY = size * 0.30f;
            float thumbH = size * 0.46f;
            float arc = thumbW;
            g2.setPaint(new GradientPaint(0, thumbY + thumbH, new Color(0x8B, 0x00, 0x24),
                    0, thumbY, new Color(0x00, 0xB5, 0xAD)));
            g2.fill(new java.awt.geom.RoundRectangle2D.Float(thumbX, thumbY, thumbW, thumbH, arc, arc));

            // Glanzstreifen links.
            g2.setColor(new Color(255, 255, 255, 130));
            g2.fill(new java.awt.geom.RoundRectangle2D.Float(thumbX + 1f, thumbY + 2f,
                    Math.max(1.5f, thumbW * 0.3f), thumbH - 4f, arc, arc));

            // Rim.
            g2.setColor(new Color(140, 200, 255, 180));
            g2.setStroke(new BasicStroke(Math.max(1f, size / 18f)));
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(thumbX, thumbY, thumbW, thumbH, arc, arc));
        } finally {
            g2.dispose();
        }
        return img;
    }
}
