package dems.ui;

import dems.exception.AuthenticationException;
import dems.model.User;
import dems.service.AppContext;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagLayout;

/** Sign-in screen. */
public class LoginFrame extends JFrame {

    private final AppContext ctx;
    private final JTextField username = Theme.textField();
    private final JPasswordField password = Theme.passwordField();
    private final JLabel message = Theme.label(" ", Theme.BASE, Theme.BAD);

    public LoginFrame(AppContext ctx) {
        super("Digital Evidence & Case Management");
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.NAVY);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createEmptyBorder(34, 40, 30, 40));

        left(card, Theme.label("SECURE ACCESS", Theme.SMALL.deriveFont(java.awt.Font.BOLD), Theme.ACCENT));
        card.add(Box.createVerticalStrut(6));
        left(card, Theme.label("Digital Evidence", Theme.TITLE, Theme.TEXT));
        left(card, Theme.label("& Case Management", Theme.TITLE, Theme.TEXT));
        card.add(Box.createVerticalStrut(8));
        left(card, Theme.label("Sign in to continue", Theme.BASE, Theme.MUTED));
        card.add(Box.createVerticalStrut(22));

        left(card, Theme.label("Username", Theme.BOLD, Theme.MUTED));
        card.add(Box.createVerticalStrut(5));
        stretch(card, username);
        card.add(Box.createVerticalStrut(14));
        left(card, Theme.label("Password", Theme.BOLD, Theme.MUTED));
        card.add(Box.createVerticalStrut(5));
        stretch(card, password);
        card.add(Box.createVerticalStrut(8));
        left(card, message);
        card.add(Box.createVerticalStrut(10));

        JButton signIn = Theme.primaryButton("Sign in");
        signIn.addActionListener(e -> doLogin());
        stretch(card, signIn);
        card.add(Box.createVerticalStrut(20));

        left(card, Theme.label("<html><b>Demo accounts</b><br>"
                + "admin / admin123<br>investigator / invest123<br>auditor / audit123</html>",
                Theme.SMALL, Theme.MUTED));

        root.add(card);
        setContentPane(root);
        getRootPane().setDefaultButton(signIn);
        setSize(520, 620);
        setLocationRelativeTo(null);
    }

    private static void left(JPanel panel, JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(c);
    }

    private static void stretch(JPanel panel, JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        Dimension pref = new Dimension(340, c instanceof JButton ? 40 : 38);
        c.setPreferredSize(pref);
        c.setMaximumSize(pref);
        panel.add(c);
    }

    private void doLogin() {
        try {
            User user = ctx.auth.login(username.getText(), new String(password.getPassword()));
            dispose();
            new MainFrame(ctx, user).setVisible(true);
        } catch (AuthenticationException ex) {
            message.setText(ex.getMessage());
            password.setText("");
            password.requestFocusInWindow();
        }
    }
}
