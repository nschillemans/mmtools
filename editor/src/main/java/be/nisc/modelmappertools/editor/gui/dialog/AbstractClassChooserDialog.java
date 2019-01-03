package be.nisc.modelmappertools.editor.gui.dialog;

import javax.swing.*;

public class AbstractClassChooserDialog extends JDialog {

    private Object classPicker;

    public AbstractClassChooserDialog(Object classPicker) {
        this.classPicker = classPicker;
    }

    protected JTextField createChooser(JPanel target, String text, String classPickerMethod) {
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
            try {
                String choice = (String) classPicker.getClass().getMethod(classPickerMethod).invoke(classPicker);

                if (choice != null) {
                    textField.setText(choice);
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not invoke classpicker method through reflection", e);
            }
        });

        target.add(chooser);

        return textField;
    }
}
