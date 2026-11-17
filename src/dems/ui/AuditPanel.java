package dems.ui;

import dems.model.User;
import dems.service.AppContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Read-only view of the audit trail (who did what and when), newest first. */
public class AuditPanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final Consumer<String> status;

    private final SimpleTableModel<String[]> model = new SimpleTableModel<String[]>()
            .column("Time", r -> r[0])
            .column("User", r -> r[1])
            .column("Action", r -> r[2])
            .column("Details", r -> r[3]);
    private final JTable table = new JTable(model);
    private List<String[]> current = new ArrayList<>();

    public AuditPanel(AppContext ctx, User user, Consumer<String> status) {
        this.ctx = ctx;
        this.status = status;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("Audit Log", Theme.HEADING, Theme.TEXT), BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        JButton refreshBtn = Theme.secondaryButton("Refresh");
        JButton exportBtn = Theme.secondaryButton("Export to text file");
        refreshBtn.addActionListener(e -> refresh());
        exportBtn.addActionListener(e -> export());
        tools.add(refreshBtn);
        tools.add(exportBtn);
        header.add(tools, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        Theme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(480);
        Theme.colorColumn(table, 2, s -> s.contains("ALERT") || s.contains("DENIED") || s.contains("FAILED")
                || s.contains("BLOCKED") ? Theme.BAD : Theme.TEXT);
        add(Theme.scroll(table), BorderLayout.CENTER);

        refresh();
    }

    @Override
    public void refresh() {
        try {
            current = ctx.audit.readAll();
            List<String[]> newestFirst = new ArrayList<>(current);
            Collections.reverse(newestFirst);
            model.setRows(newestFirst);
        } catch (IOException e) {
            Theme.error(this, "Could not read the audit log: " + e.getMessage());
        }
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("audit-report.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (BufferedWriter out = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            out.write("DIGITAL EVIDENCE AND CASE MANAGEMENT - AUDIT REPORT");
            out.newLine();
            out.write("=====================================================");
            out.newLine();
            for (String[] r : current) {
                out.write(String.format("%s | %-14s | %-18s | %s", r[0], r[1], r[2], r[3]));
                out.newLine();
            }
            status.accept("Audit report saved to " + chooser.getSelectedFile().getName());
        } catch (IOException ex) {
            Theme.error(this, "Could not save the report: " + ex.getMessage());
        }
    }
}
