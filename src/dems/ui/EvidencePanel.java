package dems.ui;

import dems.exception.DemsException;
import dems.model.CaseRecord;
import dems.model.Evidence;
import dems.model.IntegrityResult;
import dems.model.Permission;
import dems.model.User;
import dems.service.AppContext;
import dems.util.TimeUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Screen listing evidence items: add new evidence, verify hashes, export, delete. */
public class EvidencePanel extends JPanel implements Refreshable {

    private static final String ALL_CASES = "All cases";

    private final AppContext ctx;
    private final User user;
    private final Consumer<String> status;

    private final SimpleTableModel<Evidence> model = new SimpleTableModel<Evidence>()
            .column("Evidence ID", Evidence::getId)
            .column("Case", Evidence::getCaseId)
            .column("File name", Evidence::getFileName)
            .column("Type", Evidence::getCategoryLabel)
            .column("Size", Evidence::getFormattedSize)
            .column("SHA-256 (start)", e -> e.getSha256().substring(0, 12) + "...")
            .column("Added by", Evidence::getAddedBy)
            .column("Added", e -> TimeUtil.pretty(e.getAddedAt()))
            .column("Integrity", e -> e.getLastStatus().getLabel());
    private final JTable table = new JTable(model);
    private final TableRowSorter<SimpleTableModel<Evidence>> sorter = new TableRowSorter<>(model);
    private final JComboBox<String> caseFilter = new JComboBox<>();
    private final JTextArea details = new JTextArea();

    private final JButton addBtn = Theme.primaryButton("+  Add evidence");
    private final JButton verifyBtn = Theme.secondaryButton("Verify selected");
    private final JButton verifyAllBtn = Theme.secondaryButton("Verify all");
    private final JButton exportBtn = Theme.secondaryButton("Export copy");
    private final JButton deleteBtn = Theme.dangerButton("Delete");
    private boolean adjusting;

    public EvidencePanel(AppContext ctx, User user, Consumer<String> status) {
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
        header.add(Theme.label("Evidence", Theme.HEADING, Theme.TEXT), BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);

        caseFilter.setPreferredSize(new Dimension(150, 34));
        caseFilter.addActionListener(e -> {
            if (!adjusting) {
                applyFilter();
            }
        });
        tools.add(caseFilter);

        if (user.can(Permission.ADD_EVIDENCE)) {
            addBtn.addActionListener(e -> addEvidence());
            tools.add(addBtn);
        }
        if (user.can(Permission.VERIFY_EVIDENCE)) {
            verifyBtn.addActionListener(e -> verifySelected());
            verifyAllBtn.addActionListener(e -> verifyAll());
            tools.add(verifyBtn);
            tools.add(verifyAllBtn);
        }
        if (user.can(Permission.EXPORT_EVIDENCE)) {
            exportBtn.addActionListener(e -> exportSelected());
            tools.add(exportBtn);
        }
        if (user.can(Permission.DELETE_EVIDENCE)) {
            deleteBtn.addActionListener(e -> deleteSelected());
            tools.add(deleteBtn);
        }
        header.add(tools, BorderLayout.EAST);
        return header;
    }

    private JSplitPane buildBody() {
        Theme.styleTable(table);
        table.setRowSorter(sorter);
        int[] widths = {105, 95, 160, 110, 70, 125, 105, 125, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        Theme.colorColumn(table, 8, s -> {
            if (s.equals(IntegrityResult.INTACT.getLabel())) {
                return Theme.OK;
            }
            if (s.equals(IntegrityResult.UNVERIFIED.getLabel())) {
                return Theme.MUTED;
            }
            return Theme.BAD;
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });

        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(false);
        details.setFont(Theme.BASE);
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
        String selected = (String) caseFilter.getSelectedItem();
        sorter.setRowFilter(selected == null || ALL_CASES.equals(selected)
                ? null : RowFilter.regexFilter("^" + selected + "$", 1));
    }

    private Evidence selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.rowAt(table.convertRowIndexToModel(row));
    }

    private void onSelectionChanged() {
        Evidence e = selected();
        verifyBtn.setEnabled(e != null);
        exportBtn.setEnabled(e != null);
        deleteBtn.setEnabled(e != null);
        if (e == null) {
            details.setForeground(Theme.MUTED);
            details.setText("Select an evidence item to see its full SHA-256 fingerprint and details.");
            return;
        }
        details.setForeground(Theme.TEXT);
        details.setText(e.getId() + "  -  " + e.getFileName() + "   (" + e.getFormattedSize() + ")\n"
                + "Integrity: " + e.getLastStatus().getLabel() + "\n"
                + "SHA-256 recorded at intake:\n" + e.getSha256() + "\n\n"
                + e.describe() + "\n"
                + "Notes: " + (e.getDescription().isEmpty() ? "(none)" : e.getDescription()));
        details.setCaretPosition(0);
    }

    // ------------------------------------------------------------------ actions

    private void addEvidence() {
        List<CaseRecord> cases = ctx.cases.modifiableCases(user);
        if (cases.isEmpty()) {
            Theme.info(this, "There is no open case you can add evidence to.\nCreate a case first.");
            return;
        }
        String preselect = (String) caseFilter.getSelectedItem();
        EvidenceDialog d = new EvidenceDialog(SwingUtilities.getWindowAncestor(this), cases, preselect);
        if (!d.isConfirmed()) {
            return;
        }
        String caseId = d.getCaseId();
        Path file = d.getFile();
        String notes = d.getDescriptionText();

        setBusy(true, "Storing evidence and computing SHA-256...");
        new SwingWorker<Evidence, Void>() {
            @Override
            protected Evidence doInBackground() throws Exception {
                return ctx.cases.addEvidence(user, caseId, file, notes);
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    Evidence added = get();
                    refresh();
                    status.accept("Evidence " + added.getId() + " added.");
                    Theme.info(EvidencePanel.this, added.getId() + " was added to " + caseId
                            + ".\n\nSHA-256:\n" + added.getSha256());
                } catch (InterruptedException | ExecutionException ex) {
                    reportFailure(ex);
                }
            }
        }.execute();
    }

    private void verifySelected() {
        Evidence e = selected();
        if (e == null) {
            return;
        }
        try {
            IntegrityResult result = ctx.cases.verifyEvidence(user, e.getId());
            refresh();
            status.accept(e.getId() + ": " + result.getLabel());
            if (result == IntegrityResult.INTACT) {
                Theme.info(this, e.getId() + " is INTACT.\nThe file matches the SHA-256 recorded at intake.");
            } else {
                Theme.error(this, "WARNING: " + e.getId() + " is " + result.getLabel().toUpperCase() + ".\n"
                        + "The file no longer matches the SHA-256 recorded when it was added.");
            }
        } catch (DemsException | IOException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    /** Runs the multi-threaded scan off the UI thread so the window stays responsive. */
    private void verifyAll() {
        setBusy(true, "Verifying all evidence with parallel threads...");
        new SwingWorker<Map<String, IntegrityResult>, Void>() {
            @Override
            protected Map<String, IntegrityResult> doInBackground() throws Exception {
                return ctx.cases.verifyAll(user);
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    Map<String, IntegrityResult> results = get();
                    refresh();
                    long bad = results.values().stream().filter(r -> r != IntegrityResult.INTACT).count();
                    status.accept("Verified " + results.size() + " item(s): " + bad + " problem(s).");
                    if (results.isEmpty()) {
                        Theme.info(EvidencePanel.this, "There is no evidence to verify yet.");
                    } else if (bad == 0) {
                        Theme.info(EvidencePanel.this, "All " + results.size() + " evidence item(s) are intact.");
                    } else {
                        StringBuilder sb = new StringBuilder("WARNING: " + bad + " item(s) failed verification:\n\n");
                        results.forEach((id, r) -> {
                            if (r != IntegrityResult.INTACT) {
                                sb.append(id).append("  ->  ").append(r.getLabel()).append('\n');
                            }
                        });
                        Theme.error(EvidencePanel.this, sb.toString());
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    reportFailure(ex);
                }
            }
        }.execute();
    }

    private void exportSelected() {
        Evidence e = selected();
        if (e == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a folder to export " + e.getFileName() + " to");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path target = ctx.cases.exportEvidence(user, e.getId(), chooser.getSelectedFile().toPath());
            refresh();
            status.accept("Exported " + e.getId() + ".");
            Theme.info(this, "Verified and exported to:\n" + target);
        } catch (DemsException | IOException ex) {
            refresh();
            Theme.error(this, ex.getMessage());
        }
    }

    private void deleteSelected() {
        Evidence e = selected();
        if (e == null || !Theme.confirm(this, "Permanently delete " + e.getId() + " (" + e.getFileName() + ")?")) {
            return;
        }
        try {
            ctx.cases.deleteEvidence(user, e.getId());
            refresh();
            status.accept(e.getId() + " deleted.");
        } catch (DemsException | IOException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ helpers

    private void reportFailure(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        Theme.error(this, cause.getMessage() == null ? cause.toString() : cause.getMessage());
    }

    private void setBusy(boolean busy, String message) {
        addBtn.setEnabled(!busy);
        verifyAllBtn.setEnabled(!busy);
        verifyBtn.setEnabled(!busy && selected() != null);
        if (message != null) {
            status.accept(message);
        }
    }

    @Override
    public void refresh() {
        Evidence keep = selected();
        List<Evidence> items = ctx.cases.visibleEvidence(user);
        model.setRows(items);

        adjusting = true;
        Object previous = caseFilter.getSelectedItem();
        caseFilter.removeAllItems();
        caseFilter.addItem(ALL_CASES);
        List<String> ids = new ArrayList<>();
        ctx.cases.visibleCases(user).forEach(c -> ids.add(c.getId()));
        ids.stream().sorted().forEach(caseFilter::addItem);
        caseFilter.setSelectedItem(previous != null && ids.contains(previous) ? previous : ALL_CASES);
        adjusting = false;
        applyFilter();

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
