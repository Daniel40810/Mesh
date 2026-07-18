package com.dan.ffilechooser;

import com.dan.fDialog.FDialog;
import com.dan.fbutton.FButton;
import com.dan.fbutton.FButtonVariant;
import com.dan.fcombobox.FComboBox;
import com.dan.ficons.FIconComponent;
import com.dan.ficons.FIconType;
import com.dan.flabel.FLabel;
import com.dan.foptionpane.FOptionPane;
import com.dan.fstyle.FTheme;
import com.dan.ftable.FTable;
import com.dan.ftextfield.FTextfield;
import com.dan.ftable.FScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * F-Style Datei-Dialog (`com.dan.ffilechooser`), Java 8.
 * <p>
 * Baut ausschließlich auf vorhandenen F-Komponenten auf statt sie
 * nachzubauen: {@link FDialog} als Fenster (statt rohem JDialog),
 * {@link FButton}/{@link FIconComponent} für Aktionen, {@link FTextfield}
 * für den Dateinamen (nutzt dessen eingebautes Floating-Label statt
 * separatem JLabel), {@link FComboBox} für den Typ-Filter, {@link FTable}
 * in einer {@link FScrollPane} für das Datei-Listing, {@link FLabel} für
 * klickbare Breadcrumb-Segmente und den Status.
 * <p>
 * Farben/Schriften kommen automatisch über die jeweilige Komponente aus
 * {@link FTheme#getInstance()} - hier wird nichts mehr manuell nachgemalt.
 * <p>
 * Nutzung:
 * <pre>
 *   File f = FFileChooser.showOpenDialog(parentComponent, "Bild öffnen");
 *   File f = FFileChooser.showSaveDialog(parentComponent, "Speichern unter", "export.png");
 *   File d = FFileChooser.showDirectoryDialog(parentComponent, "Ordner wählen");
 * </pre>
 */
public final class FFileChooser extends FDialog {

    private FFileChooserMode mode = FFileChooserMode.OPEN_FILE;
    private File currentDirectory;
    private File selectedFile;
    private boolean approved = false;
    private boolean computerView = false;

    private final List<FFileFilter> filters = new ArrayList<>();
    private FFileFilter activeFilter = FFileFilter.ALL_FILES;
    private final Deque<File> backStack = new ArrayDeque<>();
    private final List<File> currentEntries = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private JPanel breadcrumbBar;
    private FTable table;
    private FTextfield fileNameField;
    private FComboBox filterCombo;
    private FButton approveButton;
    private FButton backBtn;
    private FButton upBtn;
    private FIconComponent refreshIcon;
    private FIconComponent newFolderIcon;

    // ---------------------------------------------------------------- Konstruktion

    public FFileChooser() {
        this(null);
    }

    public FFileChooser(Window owner) {
        super(owner, "Datei öffnen", ModalityType.APPLICATION_MODAL);
        filters.add(FFileFilter.ALL_FILES);
        buildUi();
        setPreferredDialogSize(new Dimension(760, 480));
        setLogoType(FIconType.SEARCH);
        setLocationRelativeTo(owner);
        // Absichtlich noch KEIN setCurrentDirectory()/navigateTo() hier: Modus
        // und Filter werden von den showXxxDialog()-Fabrikmethoden erst danach
        // gesetzt. Würde hier schon initial navigiert, baut jede spätere
        // addFilter()/setSelectedItem()/setMode()-Änderung Breadcrumb + Tabelle
        // erneut auf (bis zu 3x vor dem ersten Anzeigen) - das sichtbare
        // Neuaufbauen führte zum Flackern beim Öffnen. showDialog() navigiert
        // stattdessen genau einmal, wenn noch kein Verzeichnis gesetzt wurde.
    }

    // ---------------------------------------------------------------- Öffentliche API

    public void setMode(FFileChooserMode mode) {
        this.mode = mode;
        switch (mode) {
            case OPEN_FILE:
                setTitle("Datei öffnen");
                setLogoType(FIconType.SEARCH);
                approveButton.setText("Öffnen");
                fileNameField.setEditable(false);
                break;
            case SAVE_FILE:
                setTitle("Speichern unter");
                setLogoType(FIconType.SAVE);
                approveButton.setText("Speichern");
                fileNameField.setEditable(true);
                break;
            case SELECT_DIRECTORY:
                setTitle("Ordner auswählen");
                // Kein FOLDER-Icon in FIconType vorhanden - DATABASE als
                // nächstliegende "Speicherort"-Anlehnung. Bei Bedarf mit
                // dem ficon-custom-Skill um FOLDER erweitern.
                setLogoType(FIconType.DATABASE);
                approveButton.setText("Auswählen");
                fileNameField.setEditable(false);
                break;
            default:
                break;
        }
        if (currentDirectory != null) {
            navigateTo(currentDirectory);
        }
    }

    public FFileChooserMode getMode() {
        return mode;
    }

    public void setCurrentDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            navigateTo(dir);
        }
    }

    public void addFilter(FFileFilter filter) {
        filters.add(filter);
        addComboItem(filterCombo, filter);
    }

    public void setFileName(String name) {
        fileNameField.setText(name == null ? "" : name);
    }

    public String getFileName() {
        return fileNameField.getText();
    }

    /** Zeigt den Dialog modal an und liefert die gewählte Datei/den Ordner oder {@code null} bei Abbruch. */
    public File showDialog() {
        if (currentDirectory == null && !computerView) {
            // Einmalige initiale Navigation, nachdem Modus/Filter der
            // showXxxDialog()-Fabrikmethoden bereits feststehen.
            navigateTo(new File(System.getProperty("user.home")));
        }
        setVisible(true);
        return approved ? selectedFile : null;
    }

    public static File showOpenDialog(Component parent, String title) {
        FFileChooser fc = new FFileChooser(toWindow(parent));
        fc.setMode(FFileChooserMode.OPEN_FILE);
        if (title != null) {
            fc.setTitle(title);
        }
        return fc.showDialog();
    }

    public static File showOpenDialog(Component parent, String title, FFileFilter filter) {
        FFileChooser fc = new FFileChooser(toWindow(parent));
        fc.addFilter(filter);
        fc.filterCombo.setSelectedItem(filter);
        fc.setMode(FFileChooserMode.OPEN_FILE);
        if (title != null) {
            fc.setTitle(title);
        }
        return fc.showDialog();
    }

    public static File showSaveDialog(Component parent, String title, String defaultFileName) {
        FFileChooser fc = new FFileChooser(toWindow(parent));
        fc.setMode(FFileChooserMode.SAVE_FILE);
        if (title != null) {
            fc.setTitle(title);
        }
        if (defaultFileName != null) {
            fc.setFileName(defaultFileName);
        }
        return fc.showDialog();
    }

    public static File showDirectoryDialog(Component parent, String title) {
        FFileChooser fc = new FFileChooser(toWindow(parent));
        fc.setMode(FFileChooserMode.SELECT_DIRECTORY);
        if (title != null) {
            fc.setTitle(title);
        }
        return fc.showDialog();
    }

    private static Window toWindow(Component c) {
        if (c == null) {
            return null;
        }
        return c instanceof Window ? (Window) c : SwingUtilities.getWindowAncestor(c);
    }

    // ---------------------------------------------------------------- UI-Aufbau

    private void buildUi() {
        JPanel pane = getComponentPane();
        pane.setLayout(new BorderLayout());
        pane.add(buildNavBar(), BorderLayout.NORTH);
        pane.add(buildCenter(), BorderLayout.CENTER);
        pane.add(buildBottomBar(), BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(
                e -> cancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JComponent buildNavBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(8, 10, 4, 10));

        backBtn = compactButton("◀", "Zurück");
        upBtn = compactButton("▲", "Eine Ebene hoch");
        FButton homeBtn = compactButton("⌂", "Home-Verzeichnis");
        backBtn.addActionListener(e -> goBack());
        upBtn.addActionListener(e -> goUp());
        homeBtn.addActionListener(e -> navigateTo(new File(System.getProperty("user.home"))));

        refreshIcon = new FIconComponent(FIconType.REFRESH);
        refreshIcon.setToolTipText("Aktualisieren");
        refreshIcon.addActionListener(e -> {
            if (!computerView && currentDirectory != null) {
                navigateTo(currentDirectory);
            }
        });

        newFolderIcon = new FIconComponent(FIconType.ADD);
        newFolderIcon.setToolTipText("Neuer Ordner");
        newFolderIcon.addActionListener(e -> createNewFolder());

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        navButtons.setOpaque(false);
        navButtons.add(backBtn);
        navButtons.add(upBtn);
        navButtons.add(homeBtn);
        navButtons.add(refreshIcon);
        navButtons.add(newFolderIcon);

        breadcrumbBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        breadcrumbBar.setOpaque(false);
        JScrollPane breadcrumbScroll = new JScrollPane(breadcrumbBar,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        breadcrumbScroll.setBorder(BorderFactory.createEmptyBorder());
        breadcrumbScroll.setOpaque(false);
        breadcrumbScroll.getViewport().setOpaque(false);

        bar.add(navButtons, BorderLayout.WEST);
        bar.add(breadcrumbScroll, BorderLayout.CENTER);
        return bar;
    }

    private FButton compactButton(String glyph, String tooltip) {
        FButton b = new FButton(glyph);
        b.setVariant(FButtonVariant.GHOST);
        b.setToolTipText(tooltip);
        b.setPadX(10);
        b.setPadY(4);
        b.setAccentVisible(false);
        return b;
    }

    private JComponent buildCenter() {
        table = new FTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onRowActivated(table.rowAtPoint(e.getPoint()));
                }
            }
        });
        return new FScrollPane(table);
    }

    private JComponent buildBottomBar() {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(6, 10, 10, 10));
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new BorderLayout(8, 0));
        row1.setOpaque(false);

        fileNameField = new FTextfield();
        fileNameField.setLabelText("Dateiname");
        fileNameField.addActionListener(e -> approve());

        @SuppressWarnings("unchecked")
        FComboBox combo = new FComboBox(filters.toArray());
        filterCombo = combo;
        filterCombo.addActionListener(e -> {
            activeFilter = (FFileFilter) filterCombo.getSelectedItem();
            if (currentDirectory != null && !computerView) {
                navigateTo(currentDirectory);
            }
        });

        row1.add(fileNameField, BorderLayout.CENTER);
        row1.add(filterCombo, BorderLayout.EAST);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        row2.setOpaque(false);

        FButton cancelBtn = new FButton("Abbrechen");
        cancelBtn.setVariant(FButtonVariant.GHOST);
        cancelBtn.addActionListener(e -> cancel());

        approveButton = new FButton("Öffnen");
        approveButton.setVariant(FButtonVariant.PRIMARY);
        approveButton.addActionListener(e -> approve());

        row2.add(cancelBtn);
        row2.add(approveButton);

        bar.add(row1);
        bar.add(Box.createVerticalStrut(4));
        bar.add(row2);
        return bar;
    }

    // ---------------------------------------------------------------- Navigation

    private void navigateTo(File dir) {
        if (dir == null) {
            return;
        }
        if (currentDirectory != null && !currentDirectory.equals(dir)) {
            backStack.push(currentDirectory);
        }
        currentDirectory = dir;
        computerView = false;
        rebuildBreadcrumb();
        reloadList();
        backBtn.setEnabled(!backStack.isEmpty());
        upBtn.setEnabled(dir.getParentFile() != null);
    }

    private void showComputerView() {
        computerView = true;
        currentDirectory = null;
        breadcrumbBar.removeAll();
        breadcrumbBar.add(breadcrumbSegment("Computer", null));
        breadcrumbBar.revalidate();
        breadcrumbBar.repaint();

        currentEntries.clear();
        currentEntries.addAll(Arrays.asList(File.listRoots()));
        Object[][] rows = new Object[currentEntries.size()][4];
        for (int i = 0; i < currentEntries.size(); i++) {
            File root = currentEntries.get(i);
            rows[i][0] = root.getPath();
            rows[i][1] = "Laufwerk";
            rows[i][2] = "-";
            rows[i][3] = "-";
        }
        table.setData(columnNames(), rows);

        backBtn.setEnabled(!backStack.isEmpty());
        upBtn.setEnabled(false);
    }

    private void goUp() {
        if (currentDirectory == null) {
            return;
        }
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            navigateTo(parent);
        } else {
            showComputerView();
        }
    }

    private void goBack() {
        if (backStack.isEmpty()) {
            return;
        }
        File prev = backStack.pop();
        currentDirectory = null; // verhindert erneutes Pushen des aktuellen Verzeichnisses
        navigateTo(prev);
        if (!backStack.isEmpty()) {
            backStack.pop(); // Duplikat entfernen, das navigateTo() gerade gepusht hat
        }
    }

    private void reloadList() {
        currentEntries.clear();
        if (currentDirectory != null) {
            File[] children = currentDirectory.listFiles();
            if (children != null) {
                List<File> entries = new ArrayList<>(Arrays.asList(children));
                entries.removeIf(f -> mode == FFileChooserMode.SELECT_DIRECTORY ? !f.isDirectory()
                        : !(f.isDirectory() || activeFilter.accept(f)));
                entries.sort(Comparator
                        .comparing(File::isDirectory).reversed()
                        .thenComparing(f -> f.getName().toLowerCase(Locale.ROOT)));
                currentEntries.addAll(entries);
            }
        }
        Object[][] rows = new Object[currentEntries.size()][4];
        for (int i = 0; i < currentEntries.size(); i++) {
            File f = currentEntries.get(i);
            rows[i][0] = f.getName().isEmpty() ? f.getPath() : f.getName();
            rows[i][1] = f.isDirectory() ? "Ordner" : "Datei";
            rows[i][2] = f.isDirectory() ? "-" : formatSize(f.length());
            rows[i][3] = dateFormat.format(new Date(f.lastModified()));
        }
        table.setData(columnNames(), rows);
    }

    private Object[] columnNames() {
        return new Object[]{"Name", "Typ", "Größe", "Geändert"};
    }

    private void rebuildBreadcrumb() {
        breadcrumbBar.removeAll();
        if (currentDirectory != null) {
            Deque<File> chain = new ArrayDeque<>();
            File cur = currentDirectory;
            while (cur != null) {
                chain.push(cur);
                cur = cur.getParentFile();
            }
            boolean first = true;
            for (File segment : chain) {
                if (!first) {
                    JLabel sep = new JLabel(">");
                    sep.setForeground(FTheme.getInstance().getTextMuted());
                    breadcrumbBar.add(sep);
                }
                String label = segment.getName().isEmpty() ? segment.getPath() : segment.getName();
                breadcrumbBar.add(breadcrumbSegment(label, segment));
                first = false;
            }
        }
        breadcrumbBar.revalidate();
        breadcrumbBar.repaint();
    }

    private FLabel breadcrumbSegment(String label, File target) {
        FLabel l = new FLabel(label);
        l.setHoverEnabled(true);
        l.setRippleEnabled(target != null);
        l.setAccentVisible(false);
        l.setPadX(6);
        l.setPadY(3);
        if (target != null) {
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigateTo(target);
                }
            });
        }
        return l;
    }

    private void createNewFolder() {
        if (currentDirectory == null) {
            return;
        }
        String name = FOptionPane.showInputDialog(this, "Name des neuen Ordners:", "Neuer Ordner",
                FOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        File newDir = new File(currentDirectory, name.trim());
        if (newDir.mkdir()) {
            reloadList();
        } else {
            FOptionPane.showMessageDialog(this, "Ordner konnte nicht erstellt werden.",
                    "Fehler", FOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSelectionChanged() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentEntries.size()) {
            return;
        }
        File sel = currentEntries.get(row);
        if (sel.isFile() && mode != FFileChooserMode.SELECT_DIRECTORY) {
            fileNameField.setText(sel.getName());
        } else if (sel.isDirectory() && mode == FFileChooserMode.SELECT_DIRECTORY) {
            fileNameField.setText(sel.getName());
        }
    }

    private void onRowActivated(int row) {
        if (row < 0 || row >= currentEntries.size()) {
            return;
        }
        File sel = currentEntries.get(row);
        if (sel.isDirectory()) {
            navigateTo(sel);
        } else if (mode != FFileChooserMode.SELECT_DIRECTORY) {
            approve();
        }
    }

    // ---------------------------------------------------------------- Bestätigung

    private void approve() {
        File result;
        switch (mode) {
            case SELECT_DIRECTORY: {
                int row = table.getSelectedRow();
                File sel = (row >= 0 && row < currentEntries.size()) ? currentEntries.get(row) : null;
                result = (sel != null && sel.isDirectory()) ? sel : currentDirectory;
                break;
            }
            case SAVE_FILE: {
                String name = fileNameField.getText().trim();
                if (name.isEmpty() || currentDirectory == null) {
                    notifyInvalid();
                    return;
                }
                result = new File(currentDirectory, name);
                if (result.exists()) {
                    int c = FOptionPane.showConfirmDialog(this,
                            "\"" + result.getName() + "\" existiert bereits. Überschreiben?",
                            "Datei überschreiben", FOptionPane.YES_NO_OPTION);
                    if (c != FOptionPane.YES_OPTION) {
                        return;
                    }
                }
                break;
            }
            case OPEN_FILE:
            default: {
                int row = table.getSelectedRow();
                File sel = (row >= 0 && row < currentEntries.size()) ? currentEntries.get(row) : null;
                if (sel != null && sel.isFile()) {
                    result = sel;
                } else if (currentDirectory != null && !fileNameField.getText().trim().isEmpty()) {
                    result = new File(currentDirectory, fileNameField.getText().trim());
                } else {
                    notifyInvalid();
                    return;
                }
                if (!result.isFile()) {
                    notifyInvalid();
                    return;
                }
                break;
            }
        }
        selectedFile = result;
        approved = true;
        dispose();
    }

    private void cancel() {
        approved = false;
        selectedFile = null;
        dispose();
    }

    private void notifyInvalid() {
        Toolkit.getDefaultToolkit().beep();
    }

    @SuppressWarnings("unchecked")
    private static void addComboItem(JComboBox combo, Object item) {
        combo.addItem(item);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.GERMANY, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.GERMANY, "%.1f MB", mb);
        }
        return String.format(Locale.GERMANY, "%.1f GB", mb / 1024.0);
    }
}
