package dems.ui;

import dems.exception.DemsException;
import dems.model.User;
import dems.service.AppContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.function.Consumer;

/** Admin-only screen to see and create user accounts. */
public class UserPanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final User admin;
    private final Consumer<String> status;

    private final SimpleTableModel<User> model = new SimpleTableModel<User>()
            .column("Username", User::getUsername)
            .column("Full name", User::getFullName)
            .column("Role", u -> u.getRole().getLabel())
            .column("Access", u -> u.getRole().getSummary());
    private final JTable table = new JTable(model);

    public UserPanel(AppContext ctx, User admin, Consumer<String> status) {
        this.ctx = ctx;
        this.admin = admin;
        this.status = status;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("Users", Theme.HEADING, Theme.TEXT), BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        JButton addBtn = Theme.primaryButton("+  Add user");
        addBtn.addActionListener(e -> addUser());
        tools.add(addBtn);
        header.add(tools, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        Theme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        add(Theme.scroll(table), BorderLayout.CENTER);

        refresh();
    }

    private void addUser() {
        UserDialog d = new UserDialog(SwingUtilities.getWindowAncestor(this));
        if (!d.isConfirmed()) {
            return;
        }
        try {
            ctx.auth.register(admin, d.getRole(), d.getUsername(), d.getFullName(), d.getPassword());
            refresh();
            status.accept("User '" + d.getUsername().trim().toLowerCase() + "' created.");
        } catch (DemsException | IOException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        try {
            model.setRows(ctx.auth.listUsers(admin));
        } catch (DemsException e) {
            Theme.error(this, e.getMessage());
        }
    }
}
