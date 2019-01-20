package be.nisc.modelmappertools.editor.gui;

import be.nisc.modelmappertools.editor.api.ClassMapping;
import be.nisc.modelmappertools.editor.gui.plugins.ClassPicker;
import be.nisc.modelmappertools.editor.manager.MappingClassGenerator;
import be.nisc.modelmappertools.editor.manager.MappingManager;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Workspace extends JPanel {

    private ClassLoader mappingClassLoader;
    private ClassPicker classPicker;
    private JTabbedPane tabbedPane;
    private String mappingClass;

    public Workspace(ClassLoader mappingClassLoader, ClassPicker classPicker, File outputFile, String mappingClass) {
        this.mappingClassLoader = mappingClassLoader;
        this.classPicker = classPicker;
        this.mappingClass = mappingClass;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Menu
        JButton saveButton = new JButton();
        saveButton.setText("Save");
        saveButton.addActionListener((event) -> {
            List<ClassMapping> classMappings = new ArrayList<>();

            for (int c = 0; c < this.tabbedPane.getTabCount(); c++) {
                Editor editor = ((Editor) this.tabbedPane.getComponent(c));
                classMappings.add(editor.getMapping());
            }

            new MappingClassGenerator(mappingClassLoader).write(outputFile, classMappings);
        });

        JButton addButton = new JButton();
        addButton.setText("Add");
        addButton.addActionListener((event) -> {
            String from = classPicker.chooseMappingClass("Create mapping from...");
            String to = null;

            if (from != null) {
                to = classPicker.chooseMappingClass("Create mapping to...");
            }

            if (to != null) {
                addEditor(null, from, to);
            }
        });

        JButton deleteButton = new JButton();
        deleteButton.setText("Remove current");
        deleteButton.addActionListener((event) -> {
            int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this mapping?", "Remove mapping?", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                this.tabbedPane.removeTabAt(this.tabbedPane.getSelectedIndex());
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(saveButton);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        this.add(buttonPanel);

        this.tabbedPane = new JTabbedPane();
        this.add(this.tabbedPane);
    }

    public void start() {
        try {
            MappingManager.getAvailableClassMappings(mappingClassLoader, mappingClass)
                    .forEach(mapping -> addEditor(mappingClass, mapping.from, mapping.to));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse mappings file", e);
        }
    }

    private void addEditor(String mappingClass, String fromClass, String toClass) {
        int existingEditorTabIndex = -1;

        for (int c = 0; c < this.tabbedPane.getTabCount(); c++) {
            Editor existingEditor = ((Editor) this.tabbedPane.getComponent(c));

            if (existingEditor.getFromClass().equals(fromClass) && existingEditor.getToClass().equals(toClass)) {
                existingEditorTabIndex = c;
                break;
            }
        }

        if (existingEditorTabIndex == -1) {
            Editor editor = new Editor(mappingClassLoader, classPicker, mappingClass, fromClass, toClass);
            tabbedPane.addTab(fromClass + " -> " + toClass, editor);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        } else {
            tabbedPane.setSelectedIndex(existingEditorTabIndex);
        }
    }

    public static void main(String[] args) {
        Workspace frame = new Workspace(Workspace.class.getClassLoader(), new TestClassPicker(), new File("F:\\Dev\\Java\\Projects\\mmtools\\editor\\src\\main\\java\\be\\nisc\\modelmappertools\\editor\\test\\ModelMapperMappings.java"), "be.nisc.modelmappertools.editor.test.ModelMapperMappings");

        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(600, 600);
        window.setVisible(true);
        window.getContentPane().add(frame);

        frame.start();
    }

    public static class TestClassPicker implements ClassPicker {

        private int c = 0;

        @Override
        public String chooseMappingClass(String hint) {
            if (c++ == 0) {
                return "be.nisc.modelmappertools.editor.test.A";
            } else {
                return "be.nisc.modelmappertools.editor.test.C";
            }
        }

        @Override
        public String chooseConverter() {
            return "be.nisc.modelmappertools.editor.test.MyConverter";
        }
    };
}
