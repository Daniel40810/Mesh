package com.dan.ftable;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * FTable — JTable im ReagenzglasBar- / Laboratory-Dark-Look.
 *
 * <ul>
 *   <li>Glas-Datenzellen mit weichem Tuerkis-Hover-Glow (16ms-Easing-Timer)</li>
 *   <li>alternierende dunkle Zeilen, weinrote Selektion mit Akzentbalken</li>
 *   <li>Header mit Wein→Blau-Verlauf, Shine-Streifen, gemaltem Sortier-Pfeil</li>
 *   <li>vollstaendig ueber Farb-Properties / {@link FTableStyle} themebar</li>
 * </ul>
 *
 * Fuer das Glasgehaeuse + custom Scrollbars in {@link FScrollPane} einbetten.
 */
public class FTable extends JTable {

    // ---- Farb-Properties (per FTableStyle in einem Schritt setzbar) --------
    private Color accentColor    = new Color(0x00B5AD);
    private Color selectionColor = new Color(0x8B0024);
    private Color rowColorA      = new Color(0x1B2530);
    private Color rowColorB      = new Color(0x202A36);
    private Color bodyColor      = new Color(0x141A24);
    private Color headerTopColor = new Color(0x2A1622);
    private Color headerBotColor = new Color(0x141B26);
    private Color textColor      = new Color(0xE8EEF4);
    private Color textSecondary  = new Color(0x9FB0C0);
    private Color gridColor      = new Color(255, 255, 255, 16);

    private FTableDensity density = FTableDensity.NORMAL;
    private boolean animated = true;

    /** Geteilte Renderer-Instanz fuer alle Spalten ohne eigenen Renderer. */
    private final FTableCellRenderer fCellRenderer = new FTableCellRenderer();

    // ---- Hover-Animationszustand (ein Timer, Easing-Muster) ----------------
    private int   hoverRow = -1;     // aktuell ueberfahrene Zeile (-1 = keine)
    private int   glowRow  = -1;     // Zeile, die den Glow zeigt (faded aus)
    private float hoverProgress = 0f;
    private float hoverTarget   = 0f;
    private final float animationSpeed = 0.22f;
    private final Timer timer;

    public FTable() {
        super();
        timer = new Timer(16, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { tick(); }
        });
        configure();
    }

    public FTable(TableModel model) {
        super(model);
        timer = new Timer(16, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { tick(); }
        });
        configure();
    }

    private void configure() {
        setOpaque(false);
        setFillsViewportHeight(false);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setRowHeight(density.rowHeight);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // Eigener Renderer fuer ALLE Spalten (siehe getCellRenderer-Override),
        // damit JTables eingebaute Number-/Boolean-Renderer nicht durchschlagen.
        setDefaultRenderer(Object.class, fCellRenderer);
        JTableHeader header = getTableHeader();
        if (header != null) {
            header.setDefaultRenderer(new FTableHeaderRenderer());
            header.setOpaque(false);
            header.setReorderingAllowed(true);
            header.setResizingAllowed(true);
            header.setPreferredSize(new Dimension(0, density.headerHeight));
        }

        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e)  { updateHover(e.getPoint()); }
            @Override public void mouseDragged(MouseEvent e){ updateHover(e.getPoint()); }
            @Override public void mouseExited(MouseEvent e) { updateHover(null); }
        };
        addMouseListener(hover);
        addMouseMotionListener(hover);
    }

    // ----------------------------------------------------------------------
    //  Hover-Logik + Easing-Tick
    // ----------------------------------------------------------------------
    private void updateHover(Point p) {
        int row = (p == null) ? -1 : rowAtPoint(p);
        if (row == hoverRow) {
            return;
        }
        hoverRow = row;
        if (row >= 0) {
            glowRow = row;          // Glow wandert sofort auf die neue Zeile
            hoverTarget = 1f;
        } else {
            hoverTarget = 0f;       // Glow auf der letzten Zeile ausfaden lassen
        }
        if (animated) {
            ensureRunning();
        } else {
            hoverProgress = hoverTarget;
            repaint();
        }
    }

    private void tick() {
        hoverProgress += (hoverTarget - hoverProgress) * animationSpeed;
        if (Math.abs(hoverTarget - hoverProgress) < 0.004f) {
            hoverProgress = hoverTarget;
            if (hoverProgress == 0f) {
                glowRow = -1;
            }
            timer.stop();
        }
        repaint();
    }

    private void ensureRunning() {
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    /** Vom Cell-Renderer gelesen: Glow-Zeile bzw. -Staerke. */
    int   getGlowRow()       { return glowRow; }
    float getHoverProgress() { return hoverProgress; }

    /**
     * Liefert fuer jede Spalte den F-Renderer, sofern die Spalte keinen
     * eigenen Renderer gesetzt hat. So umgehen wir JTables eingebaute
     * Number-/Double-/Boolean-Renderer, die sonst die Glas-Optik in
     * Zahlenspalten ueberschreiben wuerden.
     */
    @Override
    public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
        javax.swing.table.TableColumn col = getColumnModel().getColumn(column);
        javax.swing.table.TableCellRenderer custom = col.getCellRenderer();
        if (custom != null) {
            return custom;
        }
        return fCellRenderer;
    }

    // ----------------------------------------------------------------------
    //  Bequeme Daten-API
    // ----------------------------------------------------------------------

    /** Setzt Spalten + Datenmatrix ueber ein {@link DefaultFTableModel}. */
    public void setData(Object[] columnNames, Object[][] rows) {
        DefaultFTableModel m = new DefaultFTableModel(rows, columnNames);
        setModel(m);
    }

    // ----------------------------------------------------------------------
    //  Style / Properties (JavaBean)
    // ----------------------------------------------------------------------

    /** Wendet ein komplettes Farb-Preset an. */
    public void setStyle(FTableStyle style) {
        if (style == null) {
            return;
        }
        this.accentColor    = style.accent;
        this.selectionColor = style.selection;
        this.rowColorA      = style.rowA;
        this.rowColorB      = style.rowB;
        this.bodyColor      = style.body;
        this.headerTopColor = style.headerTop;
        this.headerBotColor = style.headerBottom;
        this.textColor      = style.text;
        this.textSecondary  = style.textSecondary;
        repaint();
        if (getTableHeader() != null) {
            getTableHeader().repaint();
        }
    }

    public void setDensity(FTableDensity density) {
        if (density == null) {
            return;
        }
        this.density = density;
        setRowHeight(density.rowHeight);
        if (getTableHeader() != null) {
            getTableHeader().setPreferredSize(new Dimension(0, density.headerHeight));
            getTableHeader().revalidate();
        }
        revalidate();
        repaint();
    }

    public FTableDensity getDensity() { return density; }

    public void setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated && timer != null) {
            timer.stop();
            hoverProgress = hoverTarget;
        }
        repaint();
    }

    public boolean isAnimated() { return animated; }

    public Color getAccentColor() { return accentColor; }
    public void setAccentColor(Color c) { if (c != null) { accentColor = c; repaint(); } }

    public Color getSelectionColor() { return selectionColor; }
    public void setSelectionColor(Color c) { if (c != null) { selectionColor = c; repaint(); } }

    public Color getRowColorA() { return rowColorA; }
    public void setRowColorA(Color c) { if (c != null) { rowColorA = c; repaint(); } }

    public Color getRowColorB() { return rowColorB; }
    public void setRowColorB(Color c) { if (c != null) { rowColorB = c; repaint(); } }

    public Color getBodyColor() { return bodyColor; }
    public void setBodyColor(Color c) { if (c != null) { bodyColor = c; repaint(); } }

    public Color getHeaderTopColor() { return headerTopColor; }
    public void setHeaderTopColor(Color c) {
        if (c != null) { headerTopColor = c; if (getTableHeader() != null) getTableHeader().repaint(); }
    }

    public Color getHeaderBottomColor() { return headerBotColor; }
    public void setHeaderBottomColor(Color c) {
        if (c != null) { headerBotColor = c; if (getTableHeader() != null) getTableHeader().repaint(); }
    }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color c) { if (c != null) { textColor = c; repaint(); } }

    public Color getTextSecondaryColor() { return textSecondary; }
    public void setTextSecondaryColor(Color c) { if (c != null) { textSecondary = c; repaint(); } }

    public Color getGridColor() { return gridColor; }
    public void setGridColor(Color c) { if (c != null) { gridColor = c; repaint(); } }
}
