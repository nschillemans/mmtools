package be.nisc.modelmappertools.editor.gui.dialog;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ButtonChoiceDialog extends JDialog {

    private int choice = -1;
    private List<String> labels;

    public ButtonChoiceDialog(String title, String... labels) {
        this.labels = Arrays.asList(labels);
        setTitle(title);
    }

    public int prompt() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        labels.forEach(label -> {
            JButton button = new JButton();
            button.setText(label);
            button.addActionListener(e -> {
                this.choice = this.labels.indexOf(label);
                this.setVisible(false);
            });

            panel.add(button);
        });

        this.getContentPane().add(panel);
        this.pack();
        this.setResizable(false);
        this.setModal(true);
        this.setVisible(true);

        return this.choice;
    }
}
