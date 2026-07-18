package com.dan.rayphong.editor;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

/**
 * Zeigt das RayPhong-Rendering an und erlaubt Maus-Orbit (Ziehen) + Zoom (Mausrad).
 * Das eigentliche Rendern läuft auf einem dedizierten Hintergrund-Thread (wait/notify,
 * "neuester Zustand gewinnt") — der Rasterizer würde bei 1024er-Shadow-Maps sonst den EDT
 * blockieren (gleiche Lehre wie der SpatialTerrainStudio-Fix: Rasterisierung nie auf dem EDT).
 * Ein 16ms-Timer (Standard-Animationsmuster) treibt nur die leichte Auto-Rotate-Winkel-Fortschreibung.
 */
public final class RayPhongViewport extends JPanel {

    private RayPhongScene scene;
    private volatile BufferedImage frontBuffer;
    private boolean dirty = false;
    private volatile boolean running = true;
    private volatile RayPhongRenderer.Result lastResult;
    private java.util.function.Consumer<RayPhongRenderer.Result> onRendered;

    // Vom EDT erzeugte, unveraenderliche Momentaufnahme, die der Render-Thread
    // abholt ("neuester Zustand gewinnt"). Guarded by this.
    private RayPhongSnapshot pendingSnapshot;

    private int lastMouseX, lastMouseY;

    public void setOnRendered(java.util.function.Consumer<RayPhongRenderer.Result> callback) {
        this.onRendered = callback;
    }

    public RayPhongRenderer.Result getLastResult() {
        return lastResult;
    }

    public BufferedImage getFrontBuffer() {
        return frontBuffer;
    }

    public void setScene(RayPhongScene newScene) {
        this.scene = newScene;
        markDirty();
    }

    public RayPhongViewport(RayPhongScene initialScene) {
        // Parameter bewusst NICHT 'scene' nennen: sonst wuerde er das Feld ueberdecken
        // und die inneren Klassen (Maus-Orbit, Mausrad, Auto-Rotate-Timer) wuerden dauerhaft
        // die eingefangene Start-Szene mutieren statt this.scene — nach setScene()/Preset-
        // Wechsel liefen Drehen/Ziehen dann ins Leere.
        this.scene = initialScene;
        setOpaque(true);
        setBackground(new java.awt.Color(0x12, 0x14, 0x1E));
        setPreferredSize(new java.awt.Dimension(720, 540));

        MouseAdapter orbit = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        };
        addMouseListener(orbit);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                scene.cameraYaw -= dx * 0.01f;
                scene.cameraPitch = clampPitch(scene.cameraPitch + dy * 0.01f);
                markDirty();
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scene.cameraDistance = clampDistance(scene.cameraDistance + e.getWheelRotation() * 0.4f);
                markDirty();
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                markDirty();
            }
        });

        // Auto-Rotate: 16ms-Timer, wie im f-style-swing-Skill für Animationszustand vorgeschrieben.
        Timer rotateTimer = new Timer(16, e -> {
            if (scene.autoRotate) {
                scene.modelRotationY += 0.006f;
                markDirty();
            }
        });
        rotateTimer.start();

        Thread renderThread = new Thread(this::renderLoop, "RayPhong-Render");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    /**
     * Zieht auf dem EDT eine unveraenderliche Momentaufnahme des aktuellen Szenen-
     * Zustands und uebergibt sie dem Render-Thread. <b>Muss auf dem EDT laufen</b> —
     * saemtliche Aufrufer (Maus-Handler, Auto-Rotate-Timer, {@link #setScene},
     * Editor-Buttons) sind EDT-gebunden, der einzige Thread der {@code scene} mutiert.
     */
    public void markDirty() {
        int w = Math.max(64, getWidth());
        int h = Math.max(64, getHeight());
        RayPhongSnapshot snap = new RayPhongSnapshot(scene, w, h);
        synchronized (this) {
            pendingSnapshot = snap;
            dirty = true;
            notifyAll();
        }
    }

    public void dispose() {
        running = false;
        synchronized (this) {
            notifyAll();
        }
    }

    private static float clampPitch(float p) {
        float limit = (float) Math.toRadians(85);
        return Math.max(-limit, Math.min(limit, p));
    }

    private static float clampDistance(float d) {
        return Math.max(3f, Math.min(20f, d));
    }

    private void renderLoop() {
        while (running) {
            RayPhongSnapshot snap;
            synchronized (this) {
                while (!dirty && running) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (!running) {
                    return;
                }
                dirty = false;
                snap = pendingSnapshot;   // neuester Zustand gewinnt
            }
            if (snap == null) {
                continue;
            }

            final RayPhongRenderer.Result result = RayPhongRenderer.render(snap);

            SwingUtilities.invokeLater(() -> {
                frontBuffer = result.image;
                lastResult = result;
                repaint();
                if (onRendered != null) {
                    onRendered.accept(result);
                }
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage img = frontBuffer;
        if (img == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        g2.dispose();
    }
}
