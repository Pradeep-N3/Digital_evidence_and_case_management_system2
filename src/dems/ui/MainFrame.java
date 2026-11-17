package dems.ui;

import dems.model.Permission;
import dems.model.User;
import dems.service.AppContext;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Main window: header, sidebar navigation (shows only what the role may use) and the content area. */
public class MainFrame extends JFrame {

    /** Sidebar button that paints a highlight when selected. */
    private static final class NavButton extends JButton {
        private boolean active;

        NavButton(String text) {
            super(text);
            setFont(Theme.BOLD);
            setForeground(new Color(0xCBD5E1));
            setHorizontalAlignment(LEFT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : new Color(0xCBD5E1));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(Theme.NAVY_LIGHT);
                g2.fillRoundRect(8, 0, getWidth() - 16, getHeight(), 8, 8);
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(8, 8, 4, getHeight() - 16, 4, 4);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(0x24344F));
                g2.fillRoundRect(8, 0, getWidth() - 16, getHeight(), 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private final AppContext ctx;
    private final User user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Refreshable> pages = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final JPanel sidebarButtons = new JPanel();
    private final JLabel statusLabel = Theme.label("Ready", Theme.SMALL, Theme.MUTED);

    public MainFrame(AppContext ctx, User user) {
        super("Digital Evidence & Case Management");
        this.ctx = ctx;
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        content.setBackground(Theme.BG);
        add(content, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        // Pages are added depending on what the role is allowed to see.
        addPage("Cases", new CasePanel(ctx, user, this::setStatus));
        addPage("Evidence", new EvidencePanel(ctx, user, this::setStatus));
        if (user.can(Permission.VIEW_AUDIT)) {
            addPage("Audit Log", new AuditPanel(ctx, user, this::setStatus));
        }
        if (user.can(Permission.MANAGE_USERS)) {
            addPage("Users", new UserPanel(ctx, user, this::setStatus));
        }
        show("Cases");

        setSize(1280, 760);
        setMinimumSize(new Dimension(1100, 640));
        setLocationRelativeTo(null);
    }

    /** Generic method: the panel must be a JPanel AND implement Refreshable. */
    private <P extends JPanel & Refreshable> void addPage(String name, P panel) {
        pages.put(name, panel);
        content.add(panel, name);

        NavButton button = new NavButton(name);
        button.addActionListener(e -> show(name));
        navButtons.put(name, button);
        sidebarButtons.add(button);
    }

    private void show(String name) {
        pages.get(name).refresh();
        cards.show(content, name);
        navButtons.forEach((key, b) -> b.setActive(key.equals(name)));
        setStatus("Ready");
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(Theme.label("Digital Evidence & Case Management", Theme.HEADING, Color.WHITE));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        JLabel who = Theme.label("<html><div style='text-align:right'><b>" + user.getFullName() + "</b><br>"
                + user.getRole().getLabel() + "</div></html>", Theme.BASE, new Color(0xE2E8F0));
        JButton signOut = Theme.secondaryButton("Sign out");
        signOut.addActionListener(e -> signOut());
        right.add(who);
        right.add(signOut);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.NAVY);
        sidebar.setPreferredSize(new Dimension(210, 100));

        sidebarButtons.setOpaque(false);
        sidebarButtons.setLayout(new BoxLayout(sidebarButtons, BoxLayout.Y_AXIS));
        sidebarButtons.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        sidebar.add(sidebarButtons, BorderLayout.NORTH);

        JLabel access = Theme.label("<html><b>Your access</b><br>" + user.getRole().getSummary() + "</html>",
                Theme.SMALL, new Color(0x94A3B8));
        access.setBorder(BorderFactory.createEmptyBorder(14, 20, 20, 16));
        sidebar.add(access, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(6, 22, 6, 22)));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private void signOut() {
        ctx.auth.logout(user);
        dispose();
        new LoginFrame(ctx).setVisible(true);
    }
}
