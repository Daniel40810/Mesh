package com.dan.foptionpane;

import com.dan.ficons.FIconButtonStyler;
import com.dan.ficons.FIconComponent;
import com.dan.ficons.FIconType;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;

/**
 * FDialog — modaler, frameloser Dialog im ReagenzglasBar-Stil, das Dialog-
 * Gegenstück zu {@link FFrame}.
 *
 * <p>Gleiche Optik wie {@link FFrame}: abgerundete Ecken, transparenter
 * Hintergrund, Gradient-Taskbar mit gemaltem {@link FIconType#CAT}-Logo, Titel
 * und einem animierten {@link FIconComponent}-Schließen-Button (weinroter Glow).
 * Verschieben über die Taskbar; Größenänderung optional an allen Kanten/Ecken
 * ({@link #setResizable(boolean)}).</p>
 *
 * <p>Da ein Dialog kein Minimieren/Maximieren braucht, trägt die Taskbar nur
 * Logo, Titel und Schließen. Inhalt wird — wie bei {@link FFrame} — über
 * {@link #getComponentPane()} eingehängt.</p>
 *
 * <p>Standard-Modalität ist {@link ModalityType#APPLICATION_MODAL}. Für die
 * NetBeans-Palette existiert ein public no-arg Konstruktor.</p>
 *
 * @author com.dan
 */
public class FDialog extends JDialog {

    private Dimension preferredDialogSize = new Dimension(460, 320);
    private Color componentPaneColor = new Color(0x0C, 0x10, 0x15);
    private Color taskbarColor1 = new Color(0x01, 0x4B, 0x4E);
    private Color taskbarColor2 = new Color(0x5C, 0x00, 0x2A);
    private String dialogTitle = "FDialog";

    // Icon-Styling (öffentlich für Palette/JAR)
    public Color closeGlowColor = new Color(0x8B, 0x00, 0x24);  // Weinrot
    public float iconHoverLift  = 0.16f;
    public float iconGlowAlpha  = 90f;

    private FIconType logoType = FIconType.CAT;
    private boolean resizable = true;

    private static final int ARC = 20;
    private static final int TASKBAR_HEIGHT = 38;
    private static final int RESIZE_MARGIN = 6;

    private JPanel componentPane;
    private FTaskbar taskbar;
    private JPanel outerPanel;
    private FResizer resizer;

    // ============================================================ Konstruktoren

    /** No-arg Konstruktor für die NetBeans-Palette (nicht-modal, ohne Owner). */
    public FDialog() {
        super();
        setModalityType(ModalityType.MODELESS);
        this.dialogTitle = "FDialog";
        initUI();
    }

    /** Modaler Dialog über einem Owner-Fenster. */
    public FDialog(Window owner, String title) {
        this(owner, title, ModalityType.APPLICATION_MODAL);
    }

    /** Dialog mit frei wählbarer Modalität. */
    public FDialog(Window owner, String title, ModalityType modality) {
        super(owner, title, modality);
        this.dialogTitle = title;
        initUI();
    }

    // ============================================================ UI-Aufbau

    private void initUI() {
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(preferredDialogSize);
        setMinimumSize(new Dimension(240, 130));

        outerPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) { /* transparent */ }
        };
        outerPanel.setOpaque(false);

        taskbar = new FTaskbar();
        outerPanel.add(taskbar, BorderLayout.NORTH);

        componentPane = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(componentPaneColor);
                int w = getWidth(), h = getHeight();
                g2.fillRoundRect(0, -ARC, w, h + ARC, ARC * 2, ARC * 2);
                g2.dispose();
            }
        };
        componentPane.setOpaque(false);
        componentPane.setLayout(new BorderLayout());
        outerPanel.add(componentPane, BorderLayout.CENTER);

        resizer = new FResizer(this);
        outerPanel.addMouseListener(resizer);
        outerPanel.addMouseMotionListener(resizer);

        setContentPane(outerPanel);
        pack();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            pack();
            setLocationRelativeTo(getOwner());
        }
        super.setVisible(visible);
    }

    // ============================================================ Public API

    /** Inhalt hier einhängen (BorderLayout), analog zu {@link FFrame#getComponentPane()}. */
    public JPanel getComponentPane() {
        return componentPane;
    }

    public Dimension getPreferredDialogSize() { return preferredDialogSize; }
    public void setPreferredDialogSize(Dimension d) {
        this.preferredDialogSize = d;
        setPreferredSize(d);
        pack();
    }

    public Color getComponentPaneColor() { return componentPaneColor; }
    public void setComponentPaneColor(Color c) {
        this.componentPaneColor = c;
        if (componentPane != null) componentPane.repaint();
    }

    public void setTaskbarColors(Color c1, Color c2) {
        this.taskbarColor1 = c1;
        this.taskbarColor2 = c2;
        if (taskbar != null) taskbar.repaint();
    }

    public Color getTaskbarColor1() { return taskbarColor1; }
    public void setTaskbarColor1(Color c) { this.taskbarColor1 = c; if (taskbar != null) taskbar.repaint(); }

    public Color getTaskbarColor2() { return taskbarColor2; }
    public void setTaskbarColor2(Color c) { this.taskbarColor2 = c; if (taskbar != null) taskbar.repaint(); }

    @Override
    public void setTitle(String title) {
        this.dialogTitle = title;
        if (taskbar != null) taskbar.repaint();
    }

    @Override
    public String getTitle() { return dialogTitle; }

    /** Logo-Symbol austauschen (Standard: Katze). */
    public FIconType getLogoType() { return logoType; }
    public void setLogoType(FIconType type) {
        this.logoType = (type != null) ? type : FIconType.CAT;
        if (taskbar != null) taskbar.logo.setType(this.logoType);
    }

    public Color getCloseGlowColor() { return closeGlowColor; }
    public void setCloseGlowColor(Color c) {
        this.closeGlowColor = c;
        if (taskbar != null) taskbar.btnClose.setGlowColor(c);
    }

    public float getIconHoverLift() { return iconHoverLift; }
    public void setIconHoverLift(float v) {
        this.iconHoverLift = v;
        if (taskbar != null) {
            taskbar.btnClose.setHoverLift(v);
            taskbar.logo.setHoverLift(v);
        }
    }

    public boolean isResizable() { return resizable; }
    /** Schaltet die Größenänderung an Kanten/Ecken ein/aus. */
    public void setResizable(boolean b) { this.resizable = b; }

    // ============================================================ Taskbar

    private class FTaskbar extends JPanel {

        private final FIconComponent logo;
        private final FIconComponent btnClose;
        private Point dragStart;

        FTaskbar() {
            setPreferredSize(new Dimension(0, TASKBAR_HEIGHT));
            setOpaque(false);
            setLayout(null);

            logo = new FIconComponent(logoType);
            logo.setHoverGlowEnabled(false);
            logo.setPressEnabled(false);
            logo.setHoverLift(0.10f);
            logo.setIconGap(3);

            FIconButtonStyler style = new FIconButtonStyler();
            style.iconSize = 18;
            style.iconGap = 6;
            style.maxGlowAlpha = iconGlowAlpha;

            btnClose = style.build(FIconType.CLOSE);
            btnClose.setGlowColor(closeGlowColor);
            btnClose.setHoverLift(iconHoverLift);
            btnClose.setToolTipText("Schließen");
            btnClose.addActionListener(e -> {
                Window w = SwingUtilities.getWindowAncestor(btnClose);
                if (w != null) w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
            });

            add(logo);
            add(btnClose);

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { dragStart = e.getPoint(); }
                @Override public void mouseReleased(MouseEvent e) { dragStart = null; }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        Point loc = FDialog.this.getLocation();
                        FDialog.this.setLocation(
                                loc.x + e.getX() - dragStart.x,
                                loc.y + e.getY() - dragStart.y);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            if (w <= 1) w = FDialog.this.getWidth();
            int h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, taskbarColor1, w, 0, taskbarColor2));
            g2.fillRoundRect(0, 0, w, h + ARC, ARC * 2, ARC * 2);

            g2.setColor(new Color(255, 255, 255, 55));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h + ARC, ARC * 2, ARC * 2);

            int textX = logo.getX() + logo.getWidth() + 4;
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(dialogTitle, textX, textY);

            g2.dispose();
        }

        @Override
        public void doLayout() {
            super.doLayout();
            int h = getHeight();
            int logoSz = h - 8;
            logo.setBounds(8, (h - logoSz) / 2, logoSz, logoSz);

            int btnW = 30, btnH = 26;
            int y = (h - btnH) / 2;
            int x = getWidth() - btnW - 6;
            btnClose.setBounds(x, y, btnW, btnH);
        }
    }

    // ============================================================ FResizer

    /** Größenänderung an Kanten/Ecken — respektiert {@link #isResizable()}. */
    private static class FResizer extends MouseAdapter {

        private final FDialog dialog;
        private int direction = 0;
        private Point dragStart = null;
        private Rectangle startBounds = null;

        private static final int N = 1, S = 2, W = 4, E = 8;

        FResizer(FDialog dialog) { this.dialog = dialog; }

        @Override public void mouseMoved(MouseEvent e) {
            if (!dialog.isResizable()) { dialog.setCursor(Cursor.getDefaultCursor()); return; }
            direction = getDirection(e.getPoint(), dialog);
            dialog.setCursor(getCursor(direction));
        }

        @Override public void mousePressed(MouseEvent e) {
            if (!dialog.isResizable()) return;
            direction = getDirection(e.getPoint(), dialog);
            if (direction != 0) {
                dragStart = e.getLocationOnScreen();
                startBounds = dialog.getBounds();
            }
        }

        @Override public void mouseDragged(MouseEvent e) {
            if (direction == 0 || dragStart == null) return;
            Point cur = e.getLocationOnScreen();
            int dx = cur.x - dragStart.x, dy = cur.y - dragStart.y;
            int x = startBounds.x, y = startBounds.y, w = startBounds.width, h = startBounds.height;
            int minW = dialog.getMinimumSize().width, minH = dialog.getMinimumSize().height;
            if ((direction & E) != 0) w = Math.max(minW, w + dx);
            if ((direction & S) != 0) h = Math.max(minH, h + dy);
            if ((direction & W) != 0) { int nw = Math.max(minW, w - dx); x = x + w - nw; w = nw; }
            if ((direction & N) != 0) { int nh = Math.max(minH, h - dy); y = y + h - nh; h = nh; }
            dialog.setBounds(x, y, w, h);
        }

        @Override public void mouseReleased(MouseEvent e) {
            dragStart = null; startBounds = null; direction = 0;
            dialog.setCursor(Cursor.getDefaultCursor());
        }

        private static int getDirection(Point p, FDialog d) {
            int m = RESIZE_MARGIN, w = d.getWidth(), h = d.getHeight(), dir = 0;
            if (p.y < m) dir |= N;
            if (p.y > h - m) dir |= S;
            if (p.x < m) dir |= W;
            if (p.x > w - m) dir |= E;
            return dir;
        }

        private static Cursor getCursor(int dir) {
            switch (dir) {
                case N: return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                case S: return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                case W: return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                case E: return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                case N | W: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                case N | E: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                case S | W: return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                case S | E: return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                default: return Cursor.getDefaultCursor();
            }
        }
    }
}
