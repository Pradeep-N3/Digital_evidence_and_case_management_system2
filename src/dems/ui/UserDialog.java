package dems.ui;

import dems.model.Role;

import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Window;

/** Form for creating a new system user (admin only). */
public class UserDialog extends FormDialog {

    private final JTextField username = Theme.textField();
    private final JTextField fullName = Theme.textField();
    private final JPasswordField password = Theme.passwordField();
    private final JComboBox<Role> role = new JComboBox<>(Role.values());

    public UserDialog(Window owner) {
        super(owner, "Add User", "Create User", 420);
        addRow("Username", username);
        addRow("Full name", fullName);
        addRow("Password (min. 6 characters)", password);
        addRow("Role", role);
        showDialog();
    }

    @Override
    protected boolean validateAndSave() {
        if (username.getText().trim().isEmpty() || fullName.getText().trim().isEmpty()
                || password.getPassword().length == 0) {
            showError("Please fill in all fields.");
            return false;
        }
        return true;
    }

    public String getUsername() {
        return username.getText();
    }

    public String getFullName() {
        return fullName.getText();
    }

    public String getPassword() {
        return new String(password.getPassword());
    }

    public Role getRole() {
        return (Role) role.getSelectedItem();
    }
}
