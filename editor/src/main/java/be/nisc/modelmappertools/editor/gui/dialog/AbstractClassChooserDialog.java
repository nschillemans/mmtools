package be.nisc.modelmappertools.editor.gui.dialog;

import javax.swing.*;
import java.util.function.Supplier;

public class AbstractClassChooserDialog extends JDialog {

    protected JTextField createChooser(JPanel target, String text, Supplier<String> choiceSupplier) {
        JPanel chooser = new JPanel();
        chooser.setLayout(new BoxLayout(chooser, BoxLayout.LINE_AXIS));

        JLabel label = new JLabel();
        label.setText(text);
        chooser.add(label);

        JTextField textField = new JTextField();
        textField.setEditable(false);
        chooser.add(textField);

        JButton button = new JButton();
        button.setText("Choose");
        chooser.add(button);

        button.addActionListener(event -> {
            String choice = choiceSupplier.get();

            if (choice != null) {
                textField.setText(choice);
            }
        });

        target.add(chooser);

        return textField;
    }
}
