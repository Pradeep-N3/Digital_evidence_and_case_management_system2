package dems.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.function.Function;

/** One place for colours, fonts and small styling helpers so every screen looks the same. */
public final class Theme {

    public static final Color NAVY = new Color(0x1B2A41);
    public static final Color NAVY_LIGHT = new Color(0x2C4062);
    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color ACCENT_HOVER = new Color(0x1D4ED8);
    public static final Color BG = new Color(0xF3F5F9);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xDCE1E9);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color OK = new Color(0x15803D);
    public static final Color BAD = new Color(0xB91C1C);
    public static final Color WARN = new Color(0xB45309);

    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD = BASE.deriveFont(Font.BOLD);
    public static final Font SMALL = BASE.deriveFont(11.5f);
    public static final Font HEADING = BASE.deriveFont(Font.BOLD, 18f);
    public static final Font TITLE = BASE.deriveFont(Font.BOLD, 24f);

    private Theme() {
    }

    // ------------------------------------------------------------------ buttons

    /** A flat rounded button that looks the same on every look-and-feel. */
    private static final class FlatButton extends JButton {
        private final Color base;
        private final Color hover;
        private final Color outline;
        private final Color activeForeground;

        FlatButton(String text, Color base, Color hover, Color foreground, Color outline) {
            super(text);
            this.base = base;
            this.hover = hover;
            this.outline = outline;
            this.activeForeground = foreground;
            setForeground(foreground);
            setFont(BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setForeground(enabled ? activeForeground : new Color(0x9CA3AF));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = base;
            if (!isEnabled()) {
                fill = new Color(0xE5E7EB);
            } else if (getModel().isPressed()) {
                fill = hover.darker();
            } else if (getModel().isRollover()) {
                fill = hover;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (outline != null && isEnabled()) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton primaryButton(String text) {
        return new FlatButton(text, ACCENT, ACCENT_HOVER, Color.WHITE, null);
    }

    public static JButton secondaryButton(String text) {
        return new FlatButton(text, Color.WHITE, new Color(0xF3F4F6), TEXT, BORDER);
    }

    public static JButton dangerButton(String text) {
        return new FlatButton(text, Color.WHITE, new Color(0xFEF2F2), BAD, new Color(0xFCA5A5));
    }

    // ------------------------------------------------------------------ inputs and labels

    public static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(6, 9, 6, 9));
    }

    public static JTextField textField() {
        JTextField f = new JTextField();
        f.setFont(BASE);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(220, 34));
        return f;
    }

    /** Text field that shows a grey hint while it is empty. */
    public static JTextField searchField(String hint) {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(0x9CA3AF));
                    g2.setFont(getFont());
                    Insets in = getInsets();
                    g2.drawString(hint, in.left, (getHeight() + g2.getFontMetrics().getAscent()
                            - g2.getFontMetrics().getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        f.setFont(BASE);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(220, 34));
        return f;
    }

    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(BASE);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(220, 34));
        return f;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // ------------------------------------------------------------------ tables

    public static void styleTable(JTable table) {
        table.setFont(BASE);
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEDF0F5));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(0xDBEAFE));
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 34));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
                l.setFont(BOLD);
                l.setForeground(MUTED);
                l.setBackground(new Color(0xF8FAFC));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)));
                return l;
            }
        });

        // default cell renderer with left padding
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, false, row, col);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!sel) {
                    l.setBackground(Color.WHITE);
                }
                return l;
            }
        });
    }

    /** Colours the text of one column depending on its value (e.g. Intact = green, Tampered = red). */
    public static void colorColumn(JTable table, int column, Function<String, Color> colorFor) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, false, row, col);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                l.setFont(BOLD);
                l.setForeground(colorFor.apply(String.valueOf(value)));
                if (!sel) {
                    l.setBackground(Color.WHITE);
                }
                return l;
            }
        });
    }

    // ------------------------------------------------------------------ dialogs

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Action not completed", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static void card(JComponent c) {
        c.setBackground(CARD);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    }
}
