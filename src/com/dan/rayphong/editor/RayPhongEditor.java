package com.dan.rayphong.editor;

import com.dan.rayphong.Vec3;
import com.dan.fframe.FFrame;
import com.dan.flabel.FLabel;
import com.dan.fslider.FSlider;
import com.dan.fcombobox.FComboBox;
import com.dan.fcheckbox.FCheckBox;
import com.dan.fbutton.FButton;
import com.dan.faccordion.FAccordion;
import com.dan.faccordion.DefaultFAccordionModel;
import com.dan.fcolorpalette.FHSVColorPicker;
import com.dan.ffilechooser.FFileChooser;
import com.dan.ficons.FIconType;
import com.dan.ftable.FScrollPane;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * RayPhong Studio — Editor-Hauptfenster. Seit Phase 6: Mehrobjekt-Szenen, Preset-Auswahl,
 * PNG-Export und eine Statuszeile mit Render-Statistiken.
 */
public final class RayPhongEditor {

    private static final Color PANEL_BG = new Color(0x12, 0x14, 0x1E);

    private RayPhongScene scene = new RayPhongScene();
    private RayPhongViewport viewport;
    private FFrame frame;
    private FLabel statusLabel;

    // Referenzen für Live-Aktualisierung bei Objekt-/Preset-Wechsel
    private FComboBox objectSelector;
    private FComboBox meshKindBox;
    private FButton loadObjButton;
    private FHSVColorPicker materialColorPicker;
    private FSlider ambientSlider;
    private FSlider diffuseSlider;
    private FSlider specularSlider;
    private FSlider shininessSlider;
    private FLabel ambientLabel;
    private FLabel diffuseLabel;
    private FLabel specularLabel;
    private FLabel shininessLabel;
    private FAccordion accordion;
    private boolean refreshingControls = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RayPhongEditor().show());
    }

    public void show() {
        frame = new FFrame("RayPhong Studio");
        frame.setPreferredFrameSize(new Dimension(1220, 780));
        frame.setComponentPaneColor(new Color(0x12, 0x14, 0x1E));
        frame.setLogoType(FIconType.CAT);

        JPanel root = frame.getComponentPane();
        root.setLayout(new BorderLayout());

        viewport = new RayPhongViewport(scene);
        viewport.setOnRendered(this::onRendered);
        root.add(viewport, BorderLayout.CENTER);

        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(buildWestPanel(), BorderLayout.WEST);

        statusLabel = new FLabel("Rendert...");
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(true);
        south.setBackground(PANEL_BG);
        south.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        south.add(statusLabel, BorderLayout.WEST);
        root.add(south, BorderLayout.SOUTH);

        frame.setVisible(true);
        refreshObjectSelector();
        refreshMaterialControls();
        viewport.markDirty();
    }

    private void onRendered(RayPhongRenderer.Result r) {
        statusLabel.setText(String.format(Locale.GERMANY,
                "Render: %d ms  |  %d Dreiecke  |  %d Objekt(e)",
                r.renderMillis, r.triangleCount, scene.objects.size()));
    }

    // ---------------------------------------------------------------- Toolbar

    private JPanel buildToolbar() {
        JPanel bar = new JPanel();
        bar.setOpaque(true);
        bar.setBackground(PANEL_BG);
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));

        bar.add(new FLabel("Preset:"));
        FComboBox presetBox = new FComboBox(new String[] { "Studio", "Labor", "Edelstein-Schau" });
        presetBox.addActionListener(e -> {
            switch (presetBox.getSelectedIndex()) {
                case 1: scene = RayPhongScene.presetLabor(); break;
                case 2: scene = RayPhongScene.presetEdelstein(); break;
                default: scene = RayPhongScene.presetStudio();
            }
            viewport.setScene(scene);
            rebuildAccordionSections();
            refreshObjectSelector();
            refreshMaterialControls();
            viewport.markDirty();
        });
        bar.add(presetBox);

        bar.add(javax.swing.Box.createHorizontalStrut(16));

        FButton exportButton = new FButton("PNG exportieren");
        exportButton.addActionListener(e -> exportCurrentFrame());
        bar.add(exportButton);

        return bar;
    }

    private void exportCurrentFrame() {
        java.awt.image.BufferedImage img = viewport.getFrontBuffer();
        if (img == null) {
            return;
        }
        File target = FFileChooser.showSaveDialog(frame, "Rendering speichern", "rayphong_render.png");
        if (target == null) {
            return;
        }
        try {
            ImageIO.write(img, "png", target);
            statusLabel.setText("Gespeichert: " + target.getName());
        } catch (IOException ex) {
            statusLabel.setText("Export fehlgeschlagen: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- West (Accordion)

    private JPanel buildWestPanel() {
        accordion = new FAccordion();
        accordion.setMultipleOpen(false);
        rebuildAccordionSections();

        JPanel accordionHolder = new JPanel(new BorderLayout());
        accordionHolder.setOpaque(false);
        accordionHolder.add(accordion, BorderLayout.NORTH);

        FScrollPane scroll = new FScrollPane(accordionHolder);
//        scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(false);

        JPanel west = new JPanel(new BorderLayout());
        west.setOpaque(true);
        west.setBackground(PANEL_BG);
        west.setPreferredSize(new Dimension(385, 10));
        west.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 4));
        west.add(scroll, BorderLayout.CENTER);
        return west;
    }

    /**
     * (Neu-)Baut die Accordion-Sektionen gegen die aktuelle {@link #scene}. Nach einem
     * Preset-Wechsel referenziert {@code scene} ein neues Objekt mit neuen Licht-Slots —
     * die Panels muessen neu gebaut werden, sonst zeigen/veraendern ihre Listener die
     * alte Szene (gleiche Ursache wie der Maus-Orbit-Bug: eingefangene Alt-Referenzen).
     */
    private void rebuildAccordionSections() {
        DefaultFAccordionModel model = new DefaultFAccordionModel();
        model.addSection("Objekte", buildObjectsPanel());
        model.addSection("Material", buildMaterialPanel());
        model.addSection("Lichter", buildLightsPanel());
        model.addSection("Szene", buildScenePanel());
        accordion.setModel(model);
        accordion.setOpenSections(0, 1, 2, 3);
    }

    // ---------------------------------------------------------------- Objekte

    private JPanel buildObjectsPanel() {
        JPanel p = column();

        p.add(new FLabel("Aktives Objekt"));
        objectSelector = new FComboBox(new String[] { "Objekt 1" });
        objectSelector.addActionListener(e -> {
            if (refreshingControls) {
                return;
            }
            scene.selectedObjectIndex = objectSelector.getSelectedIndex();
            refreshMaterialControls();
        });
        p.add(objectSelector);

        JPanel addRemove = new JPanel();
        addRemove.setOpaque(true);
        addRemove.setBackground(PANEL_BG);
        addRemove.setLayout(new BoxLayout(addRemove, BoxLayout.X_AXIS));
        FButton addBtn = new FButton("+ Objekt");
        addBtn.addActionListener(e -> {
            if (scene.objects.size() >= 4) {
                return;
            }
            Vec3 pos = new Vec3(scene.objects.size() * 2.1f - 2.1f, 1.2f, 0f);
            scene.objects.add(new RayPhongScene.ObjectSlot(RayPhongScene.MeshKind.SPHERE, pos,
                    new Color(0x1E, 0x7A, 0xFF)));
            scene.selectedObjectIndex = scene.objects.size() - 1;
            refreshObjectSelector();
            refreshMaterialControls();
            viewport.markDirty();
        });
        FButton removeBtn = new FButton("- Objekt");
        removeBtn.addActionListener(e -> {
            if (scene.objects.size() <= 1) {
                return;
            }
            scene.objects.remove(scene.selectedObjectIndex);
            scene.selectedObjectIndex = Math.max(0, scene.selectedObjectIndex - 1);
            refreshObjectSelector();
            refreshMaterialControls();
            viewport.markDirty();
        });
        addRemove.add(addBtn);
        addRemove.add(javax.swing.Box.createHorizontalStrut(6));
        addRemove.add(removeBtn);
        p.add(addRemove);

        p.add(new FLabel("Mesh-Typ"));
        meshKindBox = new FComboBox(new String[] { "Kugel", "Würfel", "Torus", "OBJ-Datei" });
        meshKindBox.addActionListener(e -> {
            if (refreshingControls) {
                return;
            }
            RayPhongScene.ObjectSlot slot = scene.selected();
            if (slot == null) {
                return;
            }
            switch (meshKindBox.getSelectedIndex()) {
                case 1: slot.meshKind = RayPhongScene.MeshKind.CUBE; break;
                case 2: slot.meshKind = RayPhongScene.MeshKind.TORUS; break;
                case 3: slot.meshKind = RayPhongScene.MeshKind.OBJ; break;
                default: slot.meshKind = RayPhongScene.MeshKind.SPHERE;
            }
            loadObjButton.setVisible(slot.meshKind == RayPhongScene.MeshKind.OBJ);
            viewport.markDirty();
        });
        p.add(meshKindBox);

        loadObjButton = new FButton("OBJ-Datei laden...");
        loadObjButton.setVisible(false);
        loadObjButton.addActionListener(e -> {
            File f = FFileChooser.showOpenDialog(frame, "Wavefront-OBJ laden");
            if (f != null) {
                RayPhongScene.ObjectSlot slot = scene.selected();
                if (slot != null) {
                    slot.objPath = f.getAbsolutePath();
                    viewport.markDirty();
                }
            }
        });
        p.add(loadObjButton);

        return p;
    }

    private void refreshObjectSelector() {
        refreshingControls = true;
        String[] names = new String[scene.objects.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = "Objekt " + (i + 1);
        }
        objectSelector.setModel(new javax.swing.DefaultComboBoxModel(names));
        objectSelector.setSelectedIndex(Math.min(scene.selectedObjectIndex, names.length - 1));
        refreshingControls = false;
    }

    // ---------------------------------------------------------------- Material

    private JPanel buildMaterialPanel() {
        JPanel p = column();

        materialColorPicker = new FHSVColorPicker();
        materialColorPicker.addPropertyChangeListener("color", (PropertyChangeListener) evt -> {
            if (refreshingControls) {
                return;
            }
            RayPhongScene.ObjectSlot slot = scene.selected();
            if (slot != null) {
                slot.baseColor = (Color) evt.getNewValue();
                viewport.markDirty();
            }
        });
        p.add(labeled("Grundfarbe", materialColorPicker));

        ambientLabel = new FLabel("Ambient: 0.15");
        ambientSlider = new FSlider(0, 100, 15);
        ambientSlider.addChangeListener(e -> {
            float v = ambientSlider.getValue() / 100f;
            ambientLabel.setText("Ambient: " + fmt(v));
            if (refreshingControls) {
                return;
            }
            RayPhongScene.ObjectSlot slot = scene.selected();
            if (slot != null) {
                slot.ambientK = v;
                viewport.markDirty();
            }
        });
        p.add(sliderBlock(ambientLabel, ambientSlider));

        diffuseLabel = new FLabel("Diffuse: 0.60");
        diffuseSlider = new FSlider(0, 100, 60);
        diffuseSlider.addChangeListener(e -> {
            float v = diffuseSlider.getValue() / 100f;
            diffuseLabel.setText("Diffuse: " + fmt(v));
            if (refreshingControls) {
                return;
            }
            RayPhongScene.ObjectSlot slot = scene.selected();
            if (slot != null) {
                slot.diffuseK = v;
                viewport.markDirty();
            }
        });
        p.add(sliderBlock(diffuseLabel, diffuseSlider));

        specularLabel = new FLabel("Specular: 0.50");
        specularSlider = new FSlider(0, 100, 50);
        specularSlider.addChangeListener(e -> {
            float v = specularSlider.getValue() / 100f;
            specularLabel.setText("Specular: " + fmt(v));
            if (refreshingControls) {
                return;
            }
            RayPhongScene.ObjectSlot slot = scene.selected();
            if (slot != null) {
                slot.specularK = v;
                viewport.markDirty();
            }
        });
        p.add(sliderBlock(specularLabel, specularSlider));

        shininessLabel = new FLabel("Glanz: 48");
        shininessSlider = new FSlider(1, 200, 48);
        shininessSlider.addChangeListener(e -> {
            int v = shininessSlider.getValue();
            shininessLabel.setText("Glanz: " + v);
            if (refreshingControls) {
                return;
            }
            RayPhongScene.ObjectSlot slot = scene.selected();
            if (slot != null) {
                slot.shininess = v;
                viewport.markDirty();
            }
        });
        p.add(sliderBlock(shininessLabel, shininessSlider));

        return p;
    }

    /** Liest die Werte des aktuell gewählten Objekts in die Material-/Mesh-Kontrollen zurück. */
    private void refreshMaterialControls() {
        RayPhongScene.ObjectSlot slot = scene.selected();
        if (slot == null) {
            return;
        }
        refreshingControls = true;
        materialColorPicker.setColor(slot.baseColor);
        ambientSlider.setValue(Math.round(slot.ambientK * 100));
        ambientLabel.setText("Ambient: " + fmt(slot.ambientK));
        diffuseSlider.setValue(Math.round(slot.diffuseK * 100));
        diffuseLabel.setText("Diffuse: " + fmt(slot.diffuseK));
        specularSlider.setValue(Math.round(slot.specularK * 100));
        specularLabel.setText("Specular: " + fmt(slot.specularK));
        shininessSlider.setValue(Math.round(slot.shininess));
        shininessLabel.setText("Glanz: " + Math.round(slot.shininess));

        int kindIdx;
        switch (slot.meshKind) {
            case CUBE: kindIdx = 1; break;
            case TORUS: kindIdx = 2; break;
            case OBJ: kindIdx = 3; break;
            default: kindIdx = 0;
        }
        meshKindBox.setSelectedIndex(kindIdx);
        loadObjButton.setVisible(slot.meshKind == RayPhongScene.MeshKind.OBJ);
        refreshingControls = false;
    }

    // ---------------------------------------------------------------- Lichter

    private JPanel buildLightsPanel() {
        JPanel p = column();
        p.add(lightSection("Licht 1 (Türkis)", scene.light1));
        p.add(separator());
        p.add(lightSection("Licht 2 (Weinrot)", scene.light2));
        return p;
    }

    private JPanel lightSection(String title, RayPhongScene.LightSlot slot) {
        JPanel p = column();
        FLabel titleLabel = new FLabel(title);
        titleLabel.setAccentVisible(true);
        p.add(titleLabel);

        FHSVColorPicker colorPicker = new FHSVColorPicker();
        colorPicker.setColor(slot.color);
        colorPicker.addPropertyChangeListener("color", (PropertyChangeListener) evt -> {
            slot.color = (Color) evt.getNewValue();
            viewport.markDirty();
        });
        p.add(labeled("Farbe", colorPicker));

        FLabel intensityLabel = new FLabel("Intensität: " + fmt(slot.intensity));
        FSlider intensitySlider = new FSlider(0, 100, (int) (slot.intensity * 10));
        intensitySlider.addChangeListener(e -> {
            float v = intensitySlider.getValue() / 10f;
            intensityLabel.setText("Intensität: " + fmt(v));
            slot.intensity = v;
            viewport.markDirty();
        });
        p.add(sliderBlock(intensityLabel, intensitySlider));

        FCheckBox shadowBox = new FCheckBox("Wirft Schatten");
        shadowBox.setSelected(slot.shadowEnabled);
        shadowBox.addActionListener(e -> {
            slot.shadowEnabled = shadowBox.isSelected();
            viewport.markDirty();
        });
        p.add(shadowBox);

        return p;
    }

    // ---------------------------------------------------------------- Szene

    private JPanel buildScenePanel() {
        JPanel p = column();

        FCheckBox autoRotate = new FCheckBox("Auto-Rotieren");
        autoRotate.setSelected(scene.autoRotate);
        autoRotate.addActionListener(e -> scene.autoRotate = autoRotate.isSelected());
        p.add(autoRotate);

        p.add(new FLabel("Shadow-Map-Auflösung"));
        FComboBox resBox = new FComboBox(new String[] { "512", "1024", "2048" });
        resBox.setSelectedIndex(1);
        resBox.addActionListener(e -> {
            switch (resBox.getSelectedIndex()) {
                case 0: scene.shadowResolution = 512; break;
                case 2: scene.shadowResolution = 2048; break;
                default: scene.shadowResolution = 1024;
            }
            viewport.markDirty();
        });
        p.add(resBox);

        p.add(new FLabel("Ziehen = Kamera drehen, Mausrad = Zoom"));

        return p;
    }

    // ---------------------------------------------------------------- Helfer

    private JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(PANEL_BG);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return p;
    }

    private JPanel labeled(String label, java.awt.Component comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(true);
        p.setBackground(PANEL_BG);
        p.add(new FLabel(label), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel sliderBlock(FLabel label, java.awt.Component slider) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(true);
        p.setBackground(PANEL_BG);
        p.add(label, BorderLayout.NORTH);
        p.add(slider, BorderLayout.CENTER);
        return p;
    }

    private java.awt.Component separator() {
        JPanel sep = new JPanel();
        sep.setOpaque(true);
        sep.setBackground(PANEL_BG);
        sep.setPreferredSize(new Dimension(1, 10));
        return sep;
    }

    private static String fmt(float v) {
        return String.format(Locale.GERMANY, "%.2f", v);
    }
}
