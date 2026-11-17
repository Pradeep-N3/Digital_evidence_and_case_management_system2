package dems.ui;

import dems.model.CaseRecord;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/** Form for adding a new evidence file to a case. */
public class EvidenceDialog extends FormDialog {

    private final JComboBox<CaseRecord> caseBox;
    private final JTextField filePath = Theme.textField();
    private final JTextArea description = new JTextArea(4, 20);

    public EvidenceDialog(Window owner, List<CaseRecord> cases, String preselectCaseId) {
        super(owner, "Add Evidence", "Add Evidence", 500);

        caseBox = new JComboBox<>(cases.toArray(new CaseRecord[0]));
        caseBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof CaseRecord) {
                    CaseRecord c = (CaseRecord) value;
                    l.setText(c.getId() + "  -  " + c.getTitle());
                }
                return l;
            }
        });
        cases.stream().filter(c -> c.getId().equals(preselectCaseId)).findFirst().ifPresent(caseBox::setSelectedItem);

        filePath.setEditable(false);
        JButton browse = Theme.secondaryButton("Browse...");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose the evidence file");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                filePath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel fileRow = new JPanel(new BorderLayout(8, 0));
        fileRow.setOpaque(false);
        fileRow.add(filePath, BorderLayout.CENTER);
        fileRow.add(browse, BorderLayout.EAST);

        description.setFont(Theme.BASE);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        javax.swing.JScrollPane descScroll = Theme.scroll(description);
        descScroll.setPreferredSize(new Dimension(300, 90));

        addRow("Case", caseBox);
        addRow("Evidence file", fileRow);
        addRow("Description / notes", descScroll);
        addRow("", Theme.label("A SHA-256 fingerprint is recorded when the file is added.",
                Theme.SMALL, Theme.MUTED));
        showDialog();
    }

    @Override
    protected boolean validateAndSave() {
        if (caseBox.getSelectedItem() == null) {
            showError("Select a case first.");
            return false;
        }
        if (filePath.getText().trim().isEmpty()) {
            showError("Please choose an evidence file.");
            return false;
        }
        return true;
    }

    public String getCaseId() {
        return ((CaseRecord) caseBox.getSelectedItem()).getId();
    }

    public Path getFile() {
        return new File(filePath.getText()).toPath();
    }

    public String getDescriptionText() {
        return description.getText();
    }
}
