package com.dan.ftable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Datenzellen-Renderer im Glas-Look: alternierende Zeilenfarben, weicher
 * Tuerkis-Hover-Glow (Staerke aus {@link FTable#getHoverProgress()}),
 * weinrote Selektion mit Tuerkis-Akzentbalken in Spalte 0 sowie eine
 * dezente untere Trennlinie statt eines vollen Gitters.
 */
public class FTableCellRenderer extends DefaultTableCellRenderer {

    private int     row;
    private int     column;
    private boolean selected;
    private float   glow;          // 0..1 Hover-Staerke fuer diese Zeile
    private Color   rowA   = new Color(0x1B2530);
    private Color   rowB   = new Color(0x202A36);
    private Color   accent = new Color(0x00B5AD);
    private Color   sel    = new Color(0x8B0024);
    private Color   grid   = new Color(255, 255, 255, 16);

    public FTableCellRenderer() {
        setOpaque(false);
        setHorizontalAlignment(JLabel.LEFT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        this.row = row;
        this.column = column;
        this.selected = isSelected;
        this.glow = 0f;

        Color fg = new Color(0xE8EEF4);
        if (table instanceof FTable) {
            FTable f = (FTable) table;
            rowA   = f.getRowColorA();
            rowB   = f.getRowColorB();
            accent = f.getAccentColor();
            sel    = f.getSelectionColor();
            grid   = f.getGridColor();
            fg     = f.getTextColor();
            if (f.getGlowRow() == row) {
                this.glow = f.getHoverProgress();
            }
        }
        setForeground(isSelected ? brighten(fg, 0.12f) : fg);

        // Ausrichtung nach Werttyp: Zahlen rechts, Boolean zentriert, sonst links
        if (value instanceof Number) {
            setHorizontalAlignment(JLabel.RIGHT);
        } else if (value instanceof Boolean) {
            setHorizontalAlignment(JLabel.CENTER);
        } else {
            setHorizontalAlignment(JLabel.LEFT);
        }

        // Linker Innenabstand groesser, wenn Akzentbalken sichtbar
        boolean accentBar = (column == 0) && (isSelected || glow > 0.01f);
        int left = accentBar ? 14 : 12;
        setBorder(BorderFactory.createEmptyBorder(0, left, 0, 10));
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1) Basis: alternierende Zeilenfarbe
        Color base = ((row & 1) == 0) ? rowA : rowB;
        g2.setColor(base);
        g2.fillRect(0, 0, w, h);

        // 2) Selektion: weinroter Verlauf
        if (selected) {
            GradientPaint gp = new GradientPaint(
                    0, 0, alpha(sel, 210),
                    0, h, alpha(sel.darker(), 170));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }

        // 3) Hover-Glow: Tuerkis-Schleier (animiert)
        if (glow > 0.01f) {
            int a = (int) (70 * glow);
            GradientPaint gp = new GradientPaint(
                    0, 0, alpha(accent, a),
                    0, h, alpha(accent, Math.max(0, a - 28)));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            // feine obere Glanzkante
            g2.setColor(alpha(Color.WHITE, (int) (26 * glow)));
            g2.fillRect(0, 0, w, 1);
        }

        // 4) Akzentbalken links (nur Spalte 0)
        if (column == 0) {
            float barStrength = selected ? 1f : glow;
            if (barStrength > 0.01f) {
                int barA = (int) (235 * barStrength);
                g2.setColor(alpha(accent, barA));
                g2.fillRect(0, 0, 3, h);
                // sanfter Schein neben dem Balken
                g2.setPaint(new GradientPaint(3, 0, alpha(accent, (int) (60 * barStrength)),
                        16, 0, alpha(accent, 0)));
                g2.fillRect(3, 0, 13, h);
            }
        }

        // 5) dezente untere Trennlinie (kein volles Gitter)
        g2.setColor(grid);
        g2.fillRect(0, h - 1, w, 1);

        g2.dispose();

        // Text/Icon ueber den gemalten Hintergrund
        super.paintComponent(g);
    }

    // ---- Helfer ----------------------------------------------------------
    private static Color alpha(Color c, int a) {
        a = Math.max(0, Math.min(255, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Color brighten(Color c, float f) {
        int r = (int) Math.min(255, c.getRed()   + 255 * f);
        int g = (int) Math.min(255, c.getGreen() + 255 * f);
        int b = (int) Math.min(255, c.getBlue()  + 255 * f);
        return new Color(r, g, b);
    }
}
