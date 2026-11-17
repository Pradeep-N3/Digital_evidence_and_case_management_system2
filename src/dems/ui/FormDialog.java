package dems.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * ABSTRACT CLASS: common look and behaviour of all pop-up forms
 * (title, labelled fields, Cancel / OK buttons). Subclasses add their fields and
 * implement validateAndSave(), which returns false to keep the dialog open.
 */
public abstract class FormDialog extends JDialog {

    private final JPanel form = new JPanel(new GridBagLayout());
    private final JButton okButton;
    private final int width;
    private int row = 0;
    private boolean confirmed;

    protected FormDialog(Window owner, String heading, String okText, int width) {
        super(owner, heading, ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 18, 26));

        root.add(Theme.label(heading, Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);

        form.setOpaque(false);
        root.add(form, BorderLayout.CENTER);

        JButton cancel = Theme.secondaryButton("Cancel");
        okButton = Theme.primaryButton(okText);
        cancel.addActionListener(e -> dispose());
        okButton.addActionListener(e -> {
            if (validateAndSave()) {
                confirmed = true;
                dispose();
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(okButton);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(okButton);
        this.width = width;
    }

    /** Adds one "Label / field" row to the form. */
    protected void addRow(String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(8, 0, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        JLabel l = Theme.label(label, Theme.BOLD, Theme.MUTED);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        form.add(l, c);
        c.gridy = row + 1;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(field, c);
        row += 2;
    }

    /** Call after all rows have been added. */
    protected void showDialog() {
        setResizable(false);
        pack();
        getContentPane().setPreferredSize(new Dimension(width, getContentPane().getPreferredSize().height));
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    protected abstract boolean validateAndSave();

    protected void showError(String message) {
        Theme.error(this, message);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
