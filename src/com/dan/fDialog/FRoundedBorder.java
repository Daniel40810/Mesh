package com.dan.fDialog;

import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * Rahmen mit wahlweise oben und/oder unten abgerundeten Ecken – passend zum
 * gerundeten {@link FDialog}-Inhaltsbereich.
 *
 * <p>Der {@code componentPane} eines {@link FDialog} ist unten abgerundet
 * (Radius {@link FDialog#getContentArc()}) und oben eckig (Anschluss an die
 * Taskbar). Ein gewöhnlicher {@code LineBorder} wirkt an den unteren Ecken
 * „abgeschnitten". Dieser Border folgt der Rundung.</p>
 *
 * <p>Beispiel: {@code panel.setBorder(new FRoundedBorder(accent, 1,
 * dialog.getContentArc(), false, true));}</p>
 */
public class FRoundedBorder extends AbstractBorder {

    private final Color color;
    private final int thickness;
    private final int arc;
    private final boolean roundTop;
    private final boolean roundBottom;

    public FRoundedBorder(Color color, int thickness, int arc,
                          boolean roundTop, boolean roundBottom) {
        this.color = color;
        this.thickness = Math.max(1, thickness);
        this.arc = Math.max(0, arc);
        this.roundTop = roundTop;
        this.roundBottom = roundBottom;
    }

    /** Kurzform: alle vier Ecken abgerundet. */
    public FRoundedBorder(Color color, int thickness, int arc) {
        this(color, thickness, arc, true, true);
    }

    @Override
    public void paintBorder(Component c, Graphics g,
                            int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));

        float t = thickness / 2f;
        float xx = x + t;
        float yy = y + t;
        float ww = width - thickness;
        float hh = height - thickness;
        float a = arc;
        float aTop = roundTop ? a : 0f;
        float aBot = roundBottom ? a : 0f;

        Path2D p = new Path2D.Float();
        // Start links oben (nach der evtl. Rundung)
        p.moveTo(xx + aTop, yy);
        // obere Kante → rechts oben
        p.lineTo(xx + ww - aTop, yy);
        if (aTop > 0) {
            p.quadTo(xx + ww, yy, xx + ww, yy + aTop);
        }
        // rechte Kante → rechts unten
        p.lineTo(xx + ww, yy + hh - aBot);
        if (aBot > 0) {
            p.quadTo(xx + ww, yy + hh, xx + ww - aBot, yy + hh);
        }
        // untere Kante → links unten
        p.lineTo(xx + aBot, yy + hh);
        if (aBot > 0) {
            p.quadTo(xx, yy + hh, xx, yy + hh - aBot);
        }
        // linke Kante → links oben
        p.lineTo(xx, yy + aTop);
        if (aTop > 0) {
            p.quadTo(xx, yy, xx + aTop, yy);
        }
        p.closePath();

        g2.draw(p);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(thickness, thickness, thickness, thickness);
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
