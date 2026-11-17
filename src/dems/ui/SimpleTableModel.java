package dems.ui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * GENERIC table model: works for any row type T. Each column is defined by a title and a
 * LAMBDA that extracts the cell value from a row object.
 *
 * <pre>new SimpleTableModel&lt;CaseRecord&gt;().column("ID", c -&gt; c.getId())</pre>
 */
public class SimpleTableModel<T> extends AbstractTableModel {

    private final List<String> titles = new ArrayList<>();
    private final List<Function<T, ?>> getters = new ArrayList<>();
    private List<T> rows = new ArrayList<>();

    public SimpleTableModel<T> column(String title, Function<T, ?> getter) {
        titles.add(title);
        getters.add(getter);
        return this;
    }

    public void setRows(Collection<T> data) {
        rows = new ArrayList<>(data);
        fireTableDataChanged();
    }

    public T rowAt(int modelRow) {
        return rows.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return titles.size();
    }

    @Override
    public String getColumnName(int column) {
        return titles.get(column);
    }

    @Override
    public Object getValueAt(int row, int column) {
        return getters.get(column).apply(rows.get(row));
    }
}
