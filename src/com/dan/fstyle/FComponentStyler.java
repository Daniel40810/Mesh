package com.dan.fstyle;

import com.dan.fbutton.FButton;
import com.dan.flabel.FLabel;

import javax.swing.JComponent;
import java.awt.Color;
import java.lang.ref.WeakReference;

/**
 * Wendet {@link FTheme} auf F-Komponenten an &mdash; extern, ohne die
 * Komponenten selbst zu verändern (Styler-Prinzip des f-style-swing-Skills).
 *
 * <p>Zwei Modi:</p>
 * <ul>
 *   <li><b>Einmalig</b>: {@link #styleLabel(FLabel)} überträgt den aktuellen
 *       Theme-Zustand auf eine Instanz.</li>
 *   <li><b>Live</b>: {@link #bindLabel(FLabel)} registriert einen
 *       <i>schwachen</i> Listener, der die Komponente bei jeder Theme-Änderung
 *       neu stylt und sich automatisch abmeldet, sobald die Komponente vom GC
 *       eingesammelt wurde (kein Leak durch den Singleton).</li>
 * </ul>
 *
 * @author com.dan
 */
public final class FComponentStyler {

    private FComponentStyler() { }

    // ============================================================== FLabel

    /** Überträgt den aktuellen Theme-Zustand einmalig auf {@code label}. */
    public static void styleLabel(FLabel label) {
        if (label == null) return;
        FTheme t = FTheme.getInstance();

        // Farben
        label.setLiquidColorTop(t.getPrimary());
        label.setLiquidColorBottom(t.getAccent());
        label.setForeground(t.getText());
        label.setHoverTextColor(FColors.lighten(t.getText(), 0.18f));
        label.setShadowColor(t.getShadow());
        label.setGlassRim(t.getGlassRim());
        label.setGlassBody(t.getGlassBody());
        label.setGlassHighlight(t.getGlassHighlight());
        label.setRippleColor(t.getRipple());
        label.setBgAlpha(t.getGlassFillAlpha());

        // Schrift
        label.setFont(t.getFont(FFontRole.LABEL));

        // Animation
        FAnimationConfig a = t.getAnimationConfig();
        label.setHoverEaseIn(a.hoverEaseIn);
        label.setHoverEaseOut(a.hoverEaseOut);
        label.setRippleSpeed(a.rippleSpeed);
        label.setRippleFade(a.rippleFade);

        // Geometrie
        FGeometryConfig g = t.getGeometryConfig();
        label.setPadX(g.padX);
        label.setPadY(g.padY);
        label.setArc(g.arc);
        label.setRimStrokeWidth(g.rimStrokeWidth);
        label.setAccentHeight(g.accentHeight);
    }

    /**
     * Wie {@link #styleLabel(FLabel)}, registriert die Komponente aber zusätzlich
     * für Live-Updates. Der zurückgegebene Listener kann via
     * {@link FTheme#removeThemeListener} manuell wieder entfernt werden.
     */
    public static FTheme.ThemeListener bindLabel(final FLabel label) {
        return bindComponent(label, STYLE_LABEL);
    }

    // ============================================================== FButton

    /** Überträgt den aktuellen Theme-Zustand einmalig auf {@code button}. */
    public static void styleButton(FButton button) {
        if (button == null) return;
        FTheme t = FTheme.getInstance();

        // Farben
        button.setLiquidColorTop(t.getPrimary());
        button.setLiquidColorBottom(t.getAccent());
        button.setGlassRim(t.getGlassRim());
        button.setGlassBody(t.getGlassBody());
        button.setGlassHighlight(t.getGlassHighlight());
        button.setShadowColor(t.getShadow());
        button.setRippleColor(t.getRipple());
        button.setNormalTextColor(t.getText());
        button.setHoverTextColor(FColors.lighten(t.getText(), 0.18f));

        // Schrift
        button.setFont(t.getFont(FFontRole.BUTTON));

        // Animation
        FAnimationConfig a = t.getAnimationConfig();
        button.setHoverEaseIn(a.hoverEaseIn);
        button.setHoverEaseOut(a.hoverEaseOut);
        button.setRippleSpeed(a.rippleSpeed);
        button.setRippleFade(a.rippleFade);

        // Geometrie
        FGeometryConfig g = t.getGeometryConfig();
        button.setPadX(g.padX);
        button.setPadY(g.padY);
        button.setArc(g.arc);
        button.setRimStrokeWidth(g.rimStrokeWidth);
        button.setAccentHeight(g.accentHeight);
    }

    /**
     * Wie {@link #styleButton(FButton)}, registriert die Komponente aber zusätzlich
     * für Live-Updates (Weak-Listener, GC-sicher).
     *
     * <p>Ersetzt {@code FButton.bindTheme(Object)}: jenes Reflection-Binding erwartet
     * eine Theme-API ({@code getLiquidColorTop()}, {@code addPropertyChangeListener()}),
     * die {@link FTheme} nie hatte — der Aufruf scheiterte bisher lautlos und wirkte nie.
     * Dieser Weg bindet direkt gegen die echte {@link FTheme}-API.</p>
     */
    public static FTheme.ThemeListener bindButton(final FButton button) {
        return bindComponent(button, STYLE_BUTTON);
    }

    // ============================================================== generisch

    /**
     * Ein einzelner Theme-Anwender für eine Komponente vom Typ {@code T}.
     * Zusammen mit {@link #bindComponent} erspart das jeder neuen Komponentenfamilie
     * das eigene Weak-Listener-Boilerplate (bisher pro Komponente dupliziert, in
     * {@code FButton} sogar über kaputtes Reflection statt echtem Typzugriff).
     */
    public interface FThemeApplier<T extends JComponent> {
        void apply(T component, FTheme theme);
    }

    private static final FThemeApplier<FLabel> STYLE_LABEL = new FThemeApplier<FLabel>() {
        @Override public void apply(FLabel label, FTheme theme) { styleLabel(label); }
    };

    private static final FThemeApplier<FButton> STYLE_BUTTON = new FThemeApplier<FButton>() {
        @Override public void apply(FButton button, FTheme theme) { styleButton(button); }
    };

    /**
     * Generische Live-Bindung: wendet {@code applier} sofort an und danach bei jeder
     * Theme-Änderung erneut. Die Komponente wird nur über eine {@link WeakReference}
     * gehalten &mdash; sobald sie vom GC eingesammelt wurde, meldet sich der Listener beim
     * nächsten Theme-Wechsel selbst vom {@link FTheme}-Singleton ab (kein Leak).
     *
     * <pre>{@code
     * // Neue Komponentenfamilie anbinden, ohne eigenes Weak-Listener-Boilerplate:
     * FComponentStyler.bindComponent(myWidget, (w, theme) -> {
     *     w.setBackground(theme.getSurface());
     *     w.setForeground(theme.getText());
     * });
     * }</pre>
     *
     * @param component Zielkomponente (nicht {@code null})
     * @param applier    liest Werte aus dem Theme und setzt sie auf {@code component}
     * @return der registrierte Listener, manuell entfernbar via
     *         {@link FTheme#removeThemeListener}
     */
    public static <T extends JComponent> FTheme.ThemeListener bindComponent(
            final T component, final FThemeApplier<T> applier) {
        if (component == null || applier == null) return null;

        applier.apply(component, FTheme.getInstance());

        final WeakReference<T> ref = new WeakReference<T>(component);
        final FTheme.ThemeListener[] holder = new FTheme.ThemeListener[1];
        FTheme.ThemeListener l = new FTheme.ThemeListener() {
            @Override public void themeChanged(FTheme theme) {
                T c = ref.get();
                if (c == null) {
                    theme.removeThemeListener(holder[0]); // selbst abmelden
                    return;
                }
                applier.apply(c, theme);
            }
        };
        holder[0] = l;
        FTheme.getInstance().addThemeListener(l);
        return l;
    }

    /** Setzt Schrift (per Rolle) und Vordergrundfarbe aus dem Theme. */
    public static void applyText(JComponent c, FFontRole role) {
        if (c == null) return;
        FTheme t = FTheme.getInstance();
        c.setFont(t.getFont(role));
        c.setForeground(t.getText());
    }

    /** Färbt {@code c} flächig als Theme-Hintergrund (Background) ein. */
    public static void applyBackground(JComponent c) {
        if (c == null) return;
        c.setOpaque(true);
        c.setBackground(FTheme.getInstance().getBackground());
    }

    /** Bequemer Zugriff auf eine getönte Akzentfarbe (z.&nbsp;B. Hover-Tint). */
    public static Color accentTint(float alpha01) {
        return FColors.withAlpha(FTheme.getInstance().getPrimary(), alpha01);
    }
}
