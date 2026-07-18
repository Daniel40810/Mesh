import com.dan.fbutton.FButton;
import com.dan.flabel.FLabel;
import com.dan.fstyle.FComponentStyler;
import com.dan.fstyle.FTheme;
import com.dan.fstyle.FThemePreset;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ThemeBindingSmokeTest {
    public static void main(String[] args) throws Exception {
        FTheme theme = FTheme.getInstance();
        theme.applyPreset(FThemePreset.LABORATORY_DARK);

        // --- FLabel: bestehendes bindLabel muss weiter funktionieren (Regression) ---
        FLabel label = new FLabel("Test");
        FComponentStyler.bindLabel(label);
        Color labelColorBefore = label.getLiquidColorTop();
        assertTrue(labelColorBefore != null, "FLabel: liquidColorTop nach bindLabel gesetzt");

        // --- FButton: neues bindButton() muss ECHT wirken (im Gegensatz zum kaputten bindTheme) ---
        FButton button = new FButton("Klick");
        FComponentStyler.bindButton(button);
        Color buttonColorBefore = button.getLiquidColorTop();
        assertTrue(buttonColorBefore != null, "FButton: liquidColorTop nach bindButton gesetzt");
        assertTrue(buttonColorBefore.equals(theme.getPrimary()),
                "FButton: liquidColorTop == theme.getPrimary()");

        // --- Live-Update bei Preset-Wechsel ---
        theme.applyPreset(FThemePreset.NEON_CYBERPUNK);
        Color buttonColorAfter = button.getLiquidColorTop();
        assertTrue(!buttonColorAfter.equals(buttonColorBefore),
                "FButton: liquidColorTop aendert sich live bei Theme-Wechsel");
        assertTrue(buttonColorAfter.equals(theme.getPrimary()),
                "FButton: liquidColorTop == neues theme.getPrimary()");

        Color labelColorAfter = label.getLiquidColorTop();
        assertTrue(!labelColorAfter.equals(labelColorBefore),
                "FLabel: liquidColorTop aendert sich weiterhin live (Regression check)");

        // --- Paint-Pfade nicht kaputt (echtes Rendering erzwingen) ---
        BufferedImage img = new BufferedImage(300, 100, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        button.setSize(200, 50);
        button.paint(g);
        label.setSize(200, 30);
        label.paint(g);
        g.dispose();

        // --- alter bindTheme() bleibt kompilierbar (deprecated, aber nicht entfernt) ---
        button.bindTheme(theme);

        System.out.println("ALLE CHECKS BESTANDEN (" + checks + " von " + checks + ")");
    }

    static int checks = 0;
    static void assertTrue(boolean cond, String label) {
        checks++;
        if (!cond) {
            throw new AssertionError("FEHLGESCHLAGEN: " + label);
        }
        System.out.println("OK: " + label);
    }
}
