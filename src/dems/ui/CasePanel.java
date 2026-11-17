package dems.ui;

import dems.exception.DemsException;
import dems.model.CaseRecord;
import dems.model.CaseStatus;
import dems.model.Permission;
import dems.model.User;
import dems.service.AppContext;
import dems.util.TimeUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Screen listing the cases the current user is allowed to see. */
public class CasePanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final User user;
    private final Consumer<String> status;

    private final SimpleTableModel<CaseRecord> model = new SimpleTableModel<CaseRecord>()
            .column("Case ID", CaseRecord::getId)
            .column("Title", CaseRecord::getTitle)
            .column("Status", c -> c.getStatus().getLabel())
            .column("Assigned to", c -> c.getAssignedTo().isEmpty() ? "-" : c.getAssignedTo())
            .column("Created by", CaseRecord::getCreatedBy)
            .column("Created", c -> TimeUtil.pretty(c.getCreatedAt()));
    private final JTable table = new JTable(model);
    private final TableRowSorter<SimpleTableModel<CaseRecord>> sorter = new TableRowSorter<>(model);
    private final JTextField search = Theme.searchField("Search cases...");
    private final JTextArea details = new JTextArea();
    private final JButton editBtn = Theme.secondaryButton("Edit case");
    private final JButton deleteBtn = Theme.dangerButton("Delete");

    public CasePanel(AppContext ctx, User user, Consumer<String> status) {
        this.ctx = ctx;
        this.user = user;
        this.status = status;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("Cases", Theme.HEADING, Theme.TEXT), BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);

        search.setPreferredSize(new Dimension(220, 34));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        tools.add(search);

        if (user.can(Permission.CREATE_CASE)) {
            JButton newBtn = Theme.primaryButton("+  New case");
            newBtn.addActionListener(e -> createCase());
            tools.add(newBtn);
        }
        if (user.can(Permission.EDIT_CASE)) {
            editBtn.addActionListener(e -> editCase());
            tools.add(editBtn);
        }
        if (user.can(Permission.DELETE_CASE)) {
            deleteBtn.addActionListener(e -> deleteCase());
            tools.add(deleteBtn);
        }
        header.add(tools, BorderLayout.EAST);
        return header;
    }

    private JSplitPane buildBody() {
        Theme.styleTable(table);
        table.setRowSorter(sorter);
        int[] widths = {95, 300, 150, 120, 110, 135};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        Theme.colorColumn(table, 2, s -> {
            if (s.equals(CaseStatus.CLOSED.getLabel())) {
                return Theme.MUTED;
            }
            return s.equals(CaseStatus.OPEN.getLabel()) ? Theme.OK : Theme.WARN;
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });

        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(Theme.BASE);
        details.setForeground(Theme.TEXT);
        details.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JScrollPane detailScroll = Theme.scroll(details);
        detailScroll.setPreferredSize(new Dimension(100, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Theme.scroll(table), detailScroll);
        split.setResizeWeight(0.72);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setOpaque(false);
        onSelectionChanged();
        return split;
    }

    private void applyFilter() {
        String text = search.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    private CaseRecord selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.rowAt(table.convertRowIndexToModel(row));
    }

    private void onSelectionChanged() {
        CaseRecord c = selected();
        editBtn.setEnabled(c != null && ctx.cases.canModify(user, c));
        deleteBtn.setEnabled(c != null);
        if (c == null) {
            details.setText("Select a case to see its details.");
            details.setForeground(Theme.MUTED);
            return;
        }
        details.setForeground(Theme.TEXT);
        int evidenceCount = ctx.cases.evidenceForCase(c.getId()).size();
        details.setText(c.getId() + "  -  " + c.getTitle() + "\n"
                + "Status: " + c.getStatus().getLabel()
                + "     Evidence items: " + evidenceCount
                + "     Created by: " + c.getCreatedBy() + "\n\n"
                + (c.getDescription().isEmpty() ? "(no description)" : c.getDescription()));
        details.setCaretPosition(0);
    }

    private void createCase() {
        CaseDialog d = new CaseDialog(SwingUtilities.getWindowAncestor(this), null,
                user.can(Permission.ASSIGN_CASE), ctx.auth.investigatorUsernames());
        if (!d.isConfirmed()) {
            return;
        }
        try {
            CaseRecord c = ctx.cases.createCase(user, d.getTitleText(), d.getDescriptionText(), d.getAssignee());
            refresh();
            status.accept("Case " + c.getId() + " created.");
        } catch (DemsException | IOException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    private void editCase() {
        CaseRecord c = selected();
        if (c == null) {
            return;
        }
        CaseDialog d = new CaseDialog(SwingUtilities.getWindowAncestor(this), c,
                user.can(Permission.ASSIGN_CASE), ctx.auth.investigatorUsernames());
        if (!d.isConfirmed()) {
            return;
        }
        try {
            ctx.cases.updateCase(user, c.getId(), d.getTitleText(), d.getDescriptionText(),
                    d.getStatus(), d.getAssignee());
            refresh();
            status.accept("Case " + c.getId() + " updated.");
        } catch (DemsException | IOException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    private void deleteCase() {
        CaseRecord c = selected();
        if (c == null) {
            return;
        }
        if (!Theme.confirm(this, "Delete " + c.getId() + " and all of its evidence files?\nThis cannot be undone.")) {
            return;
        }
        try {
            ctx.cases.deleteCase(user, c.getId());
            refresh();
            status.accept("Case " + c.getId() + " deleted.");
        } catch (DemsException | IOException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        CaseRecord keep = selected();
        model.setRows(ctx.cases.visibleCases(user));
        if (keep != null) {
            for (int i = 0; i < table.getRowCount(); i++) {
                if (model.rowAt(table.convertRowIndexToModel(i)).getId().equals(keep.getId())) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        onSelectionChanged();
    }
}
