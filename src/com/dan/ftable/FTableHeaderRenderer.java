package com.dan.ftable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Spaltenkopf-Renderer: Wein→Blau-Verlauf, vertikaler Shine-Streifen links,
 * Tuerkis-Akzentlinie unten und ein programmatisch gemalter Sortier-Pfeil
 * (kein externes Icon). Liest Farben + Sortierzustand von der {@link FTable}.
 */
public class FTableHeaderRenderer extends DefaultTableCellRenderer {

    private Color topColor = new Color(0x2A1622);
    private Color botColor = new Color(0x141B26);
    private Color accent   = new Color(0x00B5AD);
    private Color fgColor  = new Color(0xE8EEF4);
    private SortOrder sortOrder = SortOrder.UNSORTED;

    public FTableHeaderRenderer() {
        setOpaque(false);
        setHorizontalAlignment(JLabel.LEFT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (table instanceof FTable) {
            FTable f = (FTable) table;
            topColor = f.getHeaderTopColor();
            botColor = f.getHeaderBottomColor();
            accent   = f.getAccentColor();
            fgColor  = f.getTextColor();
        }
        setForeground(fgColor);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        // Sortierzustand der zugehoerigen Modellspalte ermitteln
        sortOrder = SortOrder.UNSORTED;
        RowSorter<?> sorter = table.getRowSorter();
        if (sorter != null) {
            int modelCol = table.convertColumnIndexToModel(column);
            List<? extends RowSorter.SortKey> keys = sorter.getSortKeys();
            if (keys != null) {
                for (RowSorter.SortKey k : keys) {
                    if (k.getColumn() == modelCol) {
                        sortOrder = k.getSortOrder();
                        break;
                    }
                }
            }
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1) vertikaler Verlauf Wein → Blau
        g2.setPaint(new GradientPaint(0, 0, topColor, 0, h, botColor));
        g2.fillRect(0, 0, w, h);

        // 2) Glasfuellung (fast transparent, oben heller)
        g2.setPaint(new GradientPaint(0, 0, new Color(180, 220, 255, 22),
                0, h, new Color(180, 220, 255, 0)));
        g2.fillRect(0, 0, w, h);

        // 3) Shine-Streifen links (25% Breite)
        int shineW = Math.max(8, (int) (w * 0.22f));
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 30),
                shineW, 0, new Color(255, 255, 255, 0)));
        g2.fillRect(0, 0, shineW, h);

        // 4) obere Glas-Rim-Linie
        g2.setColor(new Color(140, 200, 255, 90));
        g2.fillRect(0, 0, w, 1);

        // 5) Tuerkis-Akzentlinie unten
        g2.setColor(accent);
        g2.fillRect(0, h - 2, w, 2);

        // 6) feine Spaltentrenner rechts
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect(w - 1, 4, 1, h - 8);

        // 7) Sortier-Pfeil (programmatisch)
        if (sortOrder != SortOrder.UNSORTED) {
            paintSortArrow(g2, w, h, sortOrder == SortOrder.ASCENDING);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private void paintSortArrow(Graphics2D g2, int w, int h, boolean ascending) {
        int size = 7;
        int cx = w - 13;
        int cy = h / 2;
        Path2D p = new Path2D.Double();
        if (ascending) {
            p.moveTo(cx - size / 2.0, cy + size / 2.5);
            p.lineTo(cx + size / 2.0, cy + size / 2.5);
            p.lineTo(cx, cy - size / 2.5);
        } else {
            p.moveTo(cx - size / 2.0, cy - size / 2.5);
            p.lineTo(cx + size / 2.0, cy - size / 2.5);
            p.lineTo(cx, cy + size / 2.5);
        }
        p.closePath();
        g2.setColor(accent);
        g2.fill(p);
    }
}
