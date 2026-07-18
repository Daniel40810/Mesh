package com.dan.foptionpane;

import com.dan.fbutton.FButton;
import com.dan.fbutton.FButtonVariant;
import com.dan.fDialog.FDialog;
import com.dan.ficons.FIconComponent;
import com.dan.ficons.FIconType;
import com.dan.flabel.FLabel;
import com.dan.ftextfield.FTextfield;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

/**
 * FOptionPane — visuelles Pendant zu {@link javax.swing.JOptionPane} im
 * ReagenzglasBar-Stil.
 *
 * <p>Nutzt intern einen {@link FDialog} mit {@link FLabel} für die Nachricht,
 * {@link FIconComponent} als Typ-Symbol und {@link FButton} (Varianten
 * PRIMARY / GHOST / DANGER) für die Schaltflächen. API ist so nah an
 * {@code JOptionPane} wie möglich, damit ein globales Suchen-und-Ersetzen
 * genügt.</p>
 *
 * <p>Unterstützte Methoden:</p>
 * <ul>
 *   <li>{@link #showMessageDialog(Component, Object)} — einfache Meldung</li>
 *   <li>{@link #showMessageDialog(Component, Object, String, int)} — mit Titel und Typ</li>
 *   <li>{@link #showConfirmDialog(Component, Object, String, int)} — Ja/Nein</li>
 * </ul>
 *
 * @author com.dan
 */
public class FOptionPane {

    // ── Konstanten (identisch zu JOptionPane) ──────────────────────────────

    public static final int ERROR_MESSAGE       = 0;
    public static final int INFORMATION_MESSAGE  = 1;
    public static final int WARNING_MESSAGE      = 2;
    public static final int QUESTION_MESSAGE     = 3;
    public static final int PLAIN_MESSAGE        = -1;

    public static final int DEFAULT_OPTION   = -1;
    public static final int YES_NO_OPTION    = 0;
    public static final int YES_NO_CANCEL_OPTION = 1;
    public static final int OK_CANCEL_OPTION = 2;

    public static final int YES_OPTION    = 0;
    public static final int NO_OPTION     = 1;
    public static final int CANCEL_OPTION = 2;
    public static final int OK_OPTION     = 0;
    public static final int CLOSED_OPTION = -1;

    // ── Farben ─────────────────────────────────────────────────────────────

    private static final Color BG      = new Color(0x14, 0x1A, 0x24);
    private static final Color FG      = new Color(0xE8, 0xEE, 0xF4);
    private static final Color ACCENT  = new Color(0x00, 0xB5, 0xAD);
    private static final Color WINE    = new Color(0x8B, 0x00, 0x24);
    private static final Color GOLD    = new Color(0xE8, 0x9B, 0x3A);

    // ── showMessageDialog ──────────────────────────────────────────────────

    /**
     * Zeigt eine Nachricht (INFORMATION).
     */
    public static void showMessageDialog(Component parent, Object message) {
        showMessageDialog(parent, message, "Hinweis", INFORMATION_MESSAGE);
    }

    /**
     * Zeigt eine Nachricht mit Titel und Typ (ERROR / WARNING / INFORMATION).
     */
    public static void showMessageDialog(Component parent, Object message,
                                          String title, int messageType) {
        final int[] result = { CLOSED_OPTION };
        FDialog dlg = createDialog(parent, title, messageType);

        JPanel body = buildBody(message, messageType);
        dlg.getComponentPane().add(body, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttons.setOpaque(false);
        FButton ok = new FButton("OK");
        ok.setVariant(variantForType(messageType));
        ok.addActionListener(e -> { result[0] = OK_OPTION; dlg.dispose(); });
        buttons.add(ok);
        dlg.getComponentPane().add(buttons, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(ok);

        sizeDialogForMessage(dlg, message);
        dlg.setVisible(true);
    }

    // ── showConfirmDialog ──────────────────────────────────────────────────

    /**
     * Zeigt eine Ja/Nein-Frage (Icon/Taskbar-Farbe fest auf {@link #QUESTION_MESSAGE}).
     * Gibt {@link #YES_OPTION} oder {@link #NO_OPTION} zurück.
     */
    public static int showConfirmDialog(Component parent, Object message,
                                         String title, int optionType) {
        return showConfirmDialog(parent, message, title, optionType, QUESTION_MESSAGE);
    }

    /**
     * Wie {@link #showConfirmDialog(Component, Object, String, int)}, zusätzlich mit
     * wählbarem {@code messageType} (z.&nbsp;B. {@link #PLAIN_MESSAGE}), das Icon und
     * Taskbar-Farbe des Dialogs bestimmt - Pendant zu
     * {@code JOptionPane.showConfirmDialog(parent, message, title, optionType, messageType)}.
     */
    public static int showConfirmDialog(Component parent, Object message,
                                         String title, int optionType, int messageType) {
        final int[] result = { CLOSED_OPTION };
        FDialog dlg = createDialog(parent, title, messageType);

        JPanel body = buildBody(message, messageType);
        dlg.getComponentPane().add(body, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttons.setOpaque(false);

        FButton btnYes = new FButton("Ja");
        btnYes.setVariant(FButtonVariant.PRIMARY);
        btnYes.addActionListener(e -> { result[0] = YES_OPTION; dlg.dispose(); });

        FButton btnNo = new FButton("Nein");
        btnNo.setVariant(FButtonVariant.GHOST);
        btnNo.addActionListener(e -> { result[0] = NO_OPTION; dlg.dispose(); });

        buttons.add(btnYes);
        buttons.add(btnNo);

        if (optionType == YES_NO_CANCEL_OPTION) {
            FButton btnCancel = new FButton("Abbrechen");
            btnCancel.setVariant(FButtonVariant.GHOST);
            btnCancel.addActionListener(e -> { result[0] = CANCEL_OPTION; dlg.dispose(); });
            buttons.add(btnCancel);
        }

        dlg.getComponentPane().add(buttons, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnYes);

        sizeDialogForMessage(dlg, message);
        dlg.setVisible(true);
        return result[0];
    }

    // ── showInputDialog ────────────────────────────────────────────────────

    /**
     * Zeigt einen Eingabe-Dialog mit FTextfield.
     * Gibt den eingegebenen Text zurück, oder {@code null} wenn abgebrochen.
     */
    public static String showInputDialog(Component parent, Object message,
                                         String title, int messageType) {
        final String[] result = { null };
        FDialog dlg = createDialog(parent, title, messageType);
        dlg.setPreferredDialogSize(new Dimension(420, 250));

        JPanel body = buildBody(message, messageType);
        dlg.getComponentPane().add(body, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        inputPanel.setOpaque(false);
        FTextfield field = new FTextfield();
        field.setPreferredSize(new Dimension(360, 54));
        inputPanel.add(field);
        dlg.getComponentPane().add(inputPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttons.setOpaque(false);

        FButton btnOk = new FButton("OK");
        btnOk.setVariant(FButtonVariant.PRIMARY);
        btnOk.addActionListener(e -> { result[0] = field.getText(); dlg.dispose(); });

        FButton btnCancel = new FButton("Abbrechen");
        btnCancel.setVariant(FButtonVariant.GHOST);
        btnCancel.addActionListener(e -> dlg.dispose());

        buttons.add(btnOk);
        buttons.add(btnCancel);
        dlg.getComponentPane().add(buttons, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnOk);

        dlg.setVisible(true);
        return result[0];
    }

    // ── Internes ───────────────────────────────────────────────────────────

    /**
     * {@link #createDialog(Component, String, int)} setzt für kompakte
     * Text-/Frage-Dialoge fest 420×210. Ist {@code message} aber ein
     * {@link Component} (z.&nbsp;B. ein Formular-Panel mit mehreren
     * {@code FTextfield}s), reicht das oft nicht - der Inhalt würde
     * abgeschnitten/zusammengequetscht. Deshalb hier anhand der
     * tatsächlichen {@code getPreferredSize()} des Panels vergrößern
     * (Icon-Spalte + Ränder + Button-/Taskbar-Zeile mit eingerechnet).
     */
    private static void sizeDialogForMessage(FDialog dlg, Object message) {
        if (!(message instanceof Component)) {
            return; // Standardgröße aus createDialog() reicht für reinen Text.
        }
        Dimension pref = ((Component) message).getPreferredSize();
        int width = Math.max(420, pref.width + 36 /* Icon-Spalte */ + 32 /* Rand */);
        int height = Math.max(210, pref.height + 130 /* Taskbar + Button-Zeile + Rand */);
        dlg.setPreferredDialogSize(new Dimension(width, height));
    }

    private static FDialog createDialog(Component parent, String title, int type) {
        Window owner = (parent instanceof Window)
                ? (Window) parent
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        FDialog dlg = new FDialog(owner, title);
        dlg.setComponentPaneColor(BG);
        dlg.setLogoType(iconForType(type));
        dlg.setTaskbarColors(taskbarColor1(type), taskbarColor2(type));
        dlg.setResizable(false);

        JPanel cp = dlg.getComponentPane();
        cp.setLayout(new BorderLayout(0, 12));
        cp.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16));

        dlg.setPreferredDialogSize(new Dimension(420, 210));
        return dlg;
    }

    /**
     * Baut den Dialoginhalt. Ist {@code message} bereits ein {@link Component}
     * (z.&nbsp;B. ein selbstgebautes Formular-{@code JPanel} mit
     * {@code FTextfield}/{@code FPasswordfield}), wird es <b>direkt</b>
     * eingebettet - genau wie bei {@code JOptionPane}. Andernfalls wird der
     * Wert wie bisher über {@code String.valueOf(...)} in mehrzeilige
     * {@link FLabel}s umgewandelt.
     * <p>
     * Vorher wurde hier immer {@code String.valueOf(message)} aufgerufen,
     * wodurch ein übergebenes {@code JPanel} nur als sein {@code toString()}
     * (z.&nbsp;B. {@code "javax.swing.JPanel[...]"}) angezeigt wurde, statt
     * die enthaltenen Eingabefelder darzustellen - das war der Grund, warum
     * die Panels "nicht funktionierten".
     */
    private static JPanel buildBody(Object message, int type) {
        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);

        // Icon links
        FIconComponent icon = new FIconComponent(iconForType(type));
        icon.setPreferredSize(new Dimension(36, 36));
        icon.setHoverGlowEnabled(false);
        icon.setPressEnabled(false);
        body.add(icon, BorderLayout.WEST);

        if (message instanceof Component) {
            // Fertiges Formular/Panel unverändert übernehmen.
            body.add((Component) message, BorderLayout.CENTER);
            return body;
        }

        // Nachricht rechts (mehrzeilig, Zeilenumbruch bei \n)
        JPanel msgPanel = new JPanel(new java.awt.GridBagLayout());
        msgPanel.setOpaque(false);
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new java.awt.Insets(0, 0, 2, 0);

        String[] lines = String.valueOf(message).split("\n");
        for (String line : lines) {
            FLabel lbl = new FLabel(line);
            lbl.setForeground(FG);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 13f));
            lbl.setHoverEnabled(false);
            lbl.setAccentVisible(false);
            msgPanel.add(lbl, gbc);
            gbc.gridy++;
        }
        body.add(msgPanel, BorderLayout.CENTER);
        return body;
    }

    private static FIconType iconForType(int type) {
        switch (type) {
            case ERROR_MESSAGE:   return FIconType.CLOSE;
            case WARNING_MESSAGE: return FIconType.SAVE;
            case QUESTION_MESSAGE:return FIconType.SEARCH;
            default:              return FIconType.SETTINGS;
        }
    }

    private static FButtonVariant variantForType(int type) {
        switch (type) {
            case ERROR_MESSAGE:   return FButtonVariant.DANGER;
            case WARNING_MESSAGE: return FButtonVariant.GHOST;
            default:              return FButtonVariant.PRIMARY;
        }
    }

    private static Color taskbarColor1(int type) {
        switch (type) {
            case ERROR_MESSAGE:   return new Color(0x5C, 0x00, 0x1A);
            case WARNING_MESSAGE: return new Color(0x5C, 0x3A, 0x00);
            default:              return new Color(0x01, 0x4B, 0x4E);
        }
    }

    private static Color taskbarColor2(int type) {
        switch (type) {
            case ERROR_MESSAGE:   return new Color(0x8B, 0x00, 0x24);
            case WARNING_MESSAGE: return new Color(0x8B, 0x5E, 0x00);
            default:              return new Color(0x5C, 0x00, 0x2A);
        }
    }

    private FOptionPane() { }
}
