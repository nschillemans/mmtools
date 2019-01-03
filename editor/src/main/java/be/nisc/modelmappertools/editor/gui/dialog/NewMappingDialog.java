package be.nisc.modelmappertools.editor.gui.dialog;

import be.nisc.modelmappertools.api.ClassMapping;

import javax.swing.*;

public class NewMappingDialog extends AbstractClassChooserDialog {

    private ClassMapping classMapping;
    private JTextField from, to;

    public NewMappingDialog(Object classPicker) {
        super(classPicker);
    }

    public ClassMapping prompt() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        from = createChooser(panel, "From", "chooseMappingClass");
        to = createChooser(panel, "To", "chooseMappingClass");

        JButton confirm = new JButton();
        confirm.setText("Confirm");
        confirm.addActionListener(event -> {
            String fromChoice = this.from.getText();
            String toChoice = this.to.getText();

            if (fromChoice != null && toChoice != null && !fromChoice.isEmpty() && !toChoice.isEmpty()) {
                this.setVisible(false);
                this.dispose();

                classMapping = new ClassMapping();
                classMapping.from = fromChoice;
                classMapping.to = toChoice;
            }
        });

        panel.add(confirm);

        this.setSize(500, 200);
        this.getContentPane().add(panel);
        this.setModal(true);
        this.setVisible(true);

        return classMapping;
    }
}
