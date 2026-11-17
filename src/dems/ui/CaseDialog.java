package dems.ui;

import dems.model.CaseRecord;
import dems.model.CaseStatus;

import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/** Form for creating a new case or editing an existing one. */
public class CaseDialog extends FormDialog {

    private static final String UNASSIGNED = "(Unassigned)";

    private final JTextField title = Theme.textField();
    private final JTextArea description = new JTextArea(5, 20);
    private final JComboBox<CaseStatus> status = new JComboBox<>(CaseStatus.values());
    private final JComboBox<String> assignee;

    public CaseDialog(Window owner, CaseRecord existing, boolean canAssign, List<String> investigators) {
        super(owner, existing == null ? "New Case" : "Edit " + existing.getId(),
                existing == null ? "Create Case" : "Save Changes", 460);

        description.setFont(Theme.BASE);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JScrollPane descScroll = Theme.scroll(description);
        descScroll.setPreferredSize(new Dimension(300, 110));

        List<String> names = new ArrayList<>();
        names.add(UNASSIGNED);
        names.addAll(investigators);
        assignee = new JComboBox<>(names.toArray(new String[0]));

        addRow("Case title", title);
        addRow("Description", descScroll);
        if (existing != null) {
            addRow("Status", status);
        }
        if (canAssign) {
            addRow("Assigned investigator", assignee);
        }

        if (existing != null) {
            title.setText(existing.getTitle());
            description.setText(existing.getDescription());
            status.setSelectedItem(existing.getStatus());
            assignee.setSelectedItem(existing.getAssignedTo().isEmpty() ? UNASSIGNED : existing.getAssignedTo());
        }
        showDialog();
    }

    @Override
    protected boolean validateAndSave() {
        if (title.getText().trim().isEmpty()) {
            showError("Please enter a case title.");
            return false;
        }
        return true;
    }

    public String getTitleText() {
        return title.getText();
    }

    public String getDescriptionText() {
        return description.getText();
    }

    public CaseStatus getStatus() {
        return (CaseStatus) status.getSelectedItem();
    }

    public String getAssignee() {
        String selected = (String) assignee.getSelectedItem();
        return selected == null || UNASSIGNED.equals(selected) ? "" : selected;
    }
}
