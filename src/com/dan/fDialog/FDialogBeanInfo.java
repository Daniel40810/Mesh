package com.dan.fDialog;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FDialog}: liefert ein programmatisch gemaltes Palette-Icon
 * (16&times;16 und 32&times;32) im ReagenzglasBar-Look und benennt die wichtigsten
 * Properties für die NetBeans-Eigenschaftstabelle.
 *
 * <p>Kein externes Bild-Asset — das Symbol (kleiner Dialog mit Taskbar-Streifen
 * und weinrotem Schließen-Punkt) wird per {@link Graphics2D} gezeichnet.</p>
 *
 * @author com.dan
 */
public class FDialogBeanInfo extends SimpleBeanInfo {

    private static final Class<FDialog> BEAN = FDialog.class;

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor bd = new BeanDescriptor(BEAN);
        bd.setDisplayName("FDialog");
        bd.setShortDescription("Modaler frameloser Dialog im ReagenzglasBar-Stil (F-Style).");
        return bd;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return new PropertyDescriptor[] {
                prop("title",              "Titel",            "Titeltext in der Taskbar."),
                prop("logoType",           "Logo",             "Gemaltes Logo-Glyph (Enum-Dropdown)."),
                prop("componentPaneColor", "Inhaltsfarbe",     "Hintergrund der Inhaltsfläche."),
                prop("taskbarColor1",      "Taskbar links",    "Startfarbe des Taskbar-Verlaufs."),
                prop("taskbarColor2",      "Taskbar rechts",   "Endfarbe des Taskbar-Verlaufs."),
                prop("closeGlowColor",     "Schließen-Glow",   "Glühfarbe des Schließen-Buttons."),
                prop("iconHoverLift",      "Hover-Anhebung",   "Wie weit Icons bei Hover hervortreten."),
                prop("resizable",          "Größe änderbar",   "Größenänderung an Kanten/Ecken."),
                prop("preferredDialogSize","Bevorzugte Größe", "Startgröße des Dialogs."),
            };
        } catch (IntrospectionException ex) {
            // Fällt auf Standard-Introspection zurück.
            return null;
        }
    }

    private static PropertyDescriptor prop(String name, String display, String desc)
            throws IntrospectionException {
        PropertyDescriptor pd = new PropertyDescriptor(name, BEAN);
        pd.setDisplayName(display);
        pd.setShortDescription(desc);
        return pd;
    }

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float s = size;
            float x = s * 0.12f, y = s * 0.16f, w = s - 2 * x, h = s - 2 * y;
            float arc = s * 0.28f;

            // Körper: Türkis→Wein-Verlauf
            g2.setPaint(new GradientPaint(x, y, new Color(0x00, 0xB5, 0xAD),
                    x + w, y + h, new Color(0x8B, 0x00, 0x24)));
            g2.fillRoundRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h),
                    Math.round(arc), Math.round(arc));

            // Taskbar-Streifen oben (dunkler, abgesetzt)
            float tbH = Math.max(2f, h * 0.28f);
            java.awt.Shape oldClip = g2.getClip();
            g2.clipRect(Math.round(x), Math.round(y), Math.round(w), Math.round(tbH));
            g2.setColor(new Color(0x0E, 0x1A, 0x22, 180));
            g2.fillRoundRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h),
                    Math.round(arc), Math.round(arc));
            g2.setClip(oldClip);

            // Schließen-Punkt (weinrot) rechts in der Taskbar
            float dotR = Math.max(1f, s * 0.06f);
            float dcx = x + w - dotR * 2.2f, dcy = y + tbH / 2f;
            g2.setColor(new Color(0xE0, 0x3A, 0x5E));
            g2.fill(new java.awt.geom.Ellipse2D.Float(dcx - dotR, dcy - dotR, 2 * dotR, 2 * dotR));

            // Glasrand
            g2.setColor(new Color(140, 200, 255, 170));
            g2.setStroke(new BasicStroke(Math.max(1f, s / 16f)));
            g2.drawRoundRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h),
                    Math.round(arc), Math.round(arc));

            // Shine (heller Streifen links)
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(Math.max(1f, s / 20f)));
            g2.drawLine(Math.round(x + w * 0.16f), Math.round(y + tbH + h * 0.10f),
                        Math.round(x + w * 0.16f), Math.round(y + h * 0.82f));
        } finally {
            g2.dispose();
        }
        return img;
    }
}
