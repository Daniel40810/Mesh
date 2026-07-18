package com.dan.ftable;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

/**
 * Bequeme, mutable Default-Datenquelle fuer {@link FTable}.
 *
 * <p>F-Praefix gegen JDK-Kollision (vgl. {@code FTreeModel} vs.
 * {@code javax.swing.tree.TreeModel}). Erweitert {@link DefaultTableModel},
 * bleibt damit voll kompatibel zu {@code JTable#setModel(TableModel)} und
 * unterstuetzt zugleich typisierte Spalten-Klassen fuer korrektes Sortieren.</p>
 */
public class DefaultFTableModel extends DefaultTableModel {

    private Class<?>[] columnTypes;
    private boolean editable = false;

    public DefaultFTableModel() {
        super();
    }

    public DefaultFTableModel(Object[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }

    public DefaultFTableModel(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
    }

    /**
     * Setzt explizite Spaltentypen, damit der RowSorter numerisch statt
     * lexikografisch sortiert. Laenge muss der Spaltenzahl entsprechen.
     */
    public void setColumnTypes(Class<?>... types) {
        this.columnTypes = (types == null) ? null : types.clone();
        fireTableStructureChanged();
    }

    /** Schaltet die Bearbeitbarkeit aller Zellen global um (Default: aus). */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        return editable;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnTypes != null && columnIndex >= 0 && columnIndex < columnTypes.length
                && columnTypes[columnIndex] != null) {
            return columnTypes[columnIndex];
        }
        return super.getColumnClass(columnIndex);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return editable;
    }

    /** Bequemes Hinzufuegen einer Zeile aus Einzelwerten. */
    public void addRow(Object... rowData) {
        Vector<Object> v = new Vector<Object>();
        if (rowData != null) {
            for (Object o : rowData) {
                v.add(o);
            }
        }
        super.addRow(v);
    }
}
