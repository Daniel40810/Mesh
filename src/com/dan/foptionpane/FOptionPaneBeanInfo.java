package com.dan.foptionpane;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FOptionPane}: liefert ein programmatisch gemaltes
 * Palette-Icon (16×16 und 32×32) — ein Dialog-Körper mit Info-Kreis und
 * Ausrufezeichen im ReagenzglasBar-Stil.
 *
 * <p>Da {@code FOptionPane} nur statische Methoden hat, gibt es keine
 * editierbaren Properties; der BeanDescriptor dient lediglich der
 * Palette-Anzeige in NetBeans.</p>
 *
 * @author com.dan
 */
public class FOptionPaneBeanInfo extends SimpleBeanInfo {

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor bd = new BeanDescriptor(FOptionPane.class);
        bd.setDisplayName("FOptionPane");
        bd.setShortDescription("MessageDialog / ConfirmDialog im ReagenzglasBar-Stil.");
        return bd;
    }

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float s = size;
            // Dialog-Körper
            float x = s * 0.10f, y = s * 0.14f, w = s - 2 * x, h = s - 2 * y;
            float arc = s * 0.26f;
            g2.setPaint(new GradientPaint(x, y, new Color(0x01, 0x4B, 0x4E),
                    x + w, y + h, new Color(0x5C, 0x00, 0x2A)));
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, arc, arc));

            // Glasrand
            g2.setColor(new Color(140, 200, 255, 160));
            g2.setStroke(new BasicStroke(Math.max(1f, s / 16f)));
            g2.draw(new RoundRectangle2D.Float(x, y, w, h, arc, arc));

            // Info-Kreis in der Mitte
            float cr = s * 0.22f;
            float cx = s / 2f, cy = s / 2f + s * 0.04f;
            g2.setColor(new Color(0x00, 0xB5, 0xAD));
            g2.fill(new Ellipse2D.Float(cx - cr, cy - cr, 2 * cr, 2 * cr));

            // "i" im Kreis
            g2.setColor(new Color(0x0C, 0x10, 0x15));
            float dotR = Math.max(1f, s * 0.04f);
            float dotY = cy - cr * 0.45f;
            g2.fill(new Ellipse2D.Float(cx - dotR, dotY - dotR, 2 * dotR, 2 * dotR));
            float barW = Math.max(1f, s * 0.06f);
            float barTop = cy - cr * 0.15f;
            float barBot = cy + cr * 0.50f;
            g2.fill(new RoundRectangle2D.Float(cx - barW / 2, barTop, barW, barBot - barTop, barW, barW));

            // Shine
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setStroke(new BasicStroke(Math.max(1f, s / 22f)));
            g2.drawLine(Math.round(x + w * 0.18f), Math.round(y + h * 0.25f),
                        Math.round(x + w * 0.18f), Math.round(y + h * 0.75f));
        } finally {
            g2.dispose();
        }
        return img;
    }
}
