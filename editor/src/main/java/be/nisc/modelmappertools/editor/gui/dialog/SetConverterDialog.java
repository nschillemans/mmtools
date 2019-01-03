package be.nisc.modelmappertools.editor.gui.dialog;

import javax.swing.*;

public class SetConverterDialog extends AbstractClassChooserDialog {

    private String converter;
    private JTextField converterField;

    public SetConverterDialog(Object classPicker) {
        super(classPicker);
    }

    public String prompt() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        converterField = createChooser(panel, "Converter", "chooseConverter");

        JButton confirm = new JButton();
        confirm.setText("Confirm");
        confirm.addActionListener(event -> {
            this.converter = this.converterField.getText();

            if (converter != null && !converter.isEmpty()) {
                this.setVisible(false);
                this.dispose();
            }
        });

        panel.add(confirm);

        this.setSize(500, 200);
        this.getContentPane().add(panel);
        this.setModal(true);
        this.setVisible(true);

        return converter;
    }
}
