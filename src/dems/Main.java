package dems;

import dems.service.AppContext;
import dems.ui.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Entry point. Creates the "database" in a folder called "data" next to where the program
 * is started, then shows the login screen.
 */
public class Main {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }

        SwingUtilities.invokeLater(() -> {
            try {
                AppContext ctx = AppContext.start(Paths.get("data"));
                Runtime.getRuntime().addShutdownHook(new Thread(ctx::shutdown));
                new LoginFrame(ctx).setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Could not open the data folder:\n" + e.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
