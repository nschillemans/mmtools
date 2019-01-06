package be.nisc.modelmappertools.editor.gui;

import be.nisc.modelmappertools.api.ClassMapping;
import be.nisc.modelmappertools.editor.gui.plugins.ClassPicker;
import be.nisc.modelmappertools.editor.gui.plugins.SaveHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Workspace extends JPanel {

    private ClassLoader mappingClassLoader;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ClassPicker classPicker;
    private SaveHandler saveHandler;
    private JTabbedPane tabbedPane;
    private File file;

    public Workspace(ClassLoader mappingClassLoader, ClassPicker classPicker, SaveHandler saveHandler, File file) {
        this.mappingClassLoader = mappingClassLoader;
        this.classPicker = classPicker;
        this.saveHandler = saveHandler;
        this.file = file;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Menu
        JButton saveButton = new JButton();
        saveButton.setText("Save");
        saveButton.addActionListener((event) -> this.saveHandler.save(serializeMappings()));

        JButton addButton = new JButton();
        addButton.setText("Add");
        addButton.addActionListener((event) -> {
            String from = classPicker.chooseMappingClass("Create mapping from...");
            String to = null;

            if (from != null) {
                to = classPicker.chooseMappingClass("Create mapping to...");
            }

            ClassMapping classMapping = null;

            if (to != null) {
                classMapping = new ClassMapping();
                classMapping.from = from;
                classMapping.to = to;
            }

            if (classMapping != null) {
                addEditor(classMapping);
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
            List<ClassMapping> mappings = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionLikeType(ArrayList.class, ClassMapping.class));
            mappings.stream().forEach(mapping -> addEditor(mapping));
        } catch (IOException e) {
            throw new RuntimeException("Could not parse mappings file", e);
        }
    }

    private String serializeMappings() {
        List<ClassMapping> mappings = new ArrayList<>();

        for (Component editor : this.tabbedPane.getComponents()) {
            mappings.add(((Editor) editor).getMapping());
        }

        try {
            return objectMapper.writeValueAsString(mappings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize mappings", e);
        }
    }

    private void addEditor(ClassMapping mapping) {
        int existingEditorTabIndex = -1;

        for (int c = 0; c < this.tabbedPane.getTabCount(); c++) {
            ClassMapping existingMapping = ((Editor) this.tabbedPane.getComponent(c)).getMapping();

            if (existingMapping.from.equals(mapping.from) && existingMapping.to.equals(mapping.to)) {
                existingEditorTabIndex = c;
                break;
            }
        }

        if (existingEditorTabIndex == -1) {
            Class fromClass;
            Class toClass;

            try {
                fromClass = this.mappingClassLoader.loadClass(mapping.from);
                toClass = this.mappingClassLoader.loadClass(mapping.to);
            } catch (Exception e) {
                throw new RuntimeException("Could not load specified classes", e);
            }

            Editor editor = new Editor(mappingClassLoader, classPicker, mapping);
            tabbedPane.addTab(fromClass.getSimpleName() + " -> " + toClass.getSimpleName(), editor);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        } else {
            tabbedPane.setSelectedIndex(existingEditorTabIndex);
        }
    }

    public static void main(String[] args) {
        Workspace frame = new Workspace(Workspace.class.getClassLoader(), new TestClassPicker(), new TestSaveHandler(), new File("F:\\Dev\\Java\\Projects\\mmtools\\editor\\src\\main\\resources\\mappings.json"));

        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(600, 600);
        window.setVisible(true);
        window.getContentPane().add(frame);

        frame.start();
    }

    public static class TestSaveHandler implements SaveHandler {
        @Override
        public void save(String output) {
            try {
                Files.write(new File("F:\\Dev\\Java\\Projects\\mmtools\\editor\\src\\main\\resources\\mappings.json").toPath(), output.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TestClassPicker implements ClassPicker {

        private int c = 0;

        @Override
        public String chooseMappingClass(String hint) {
            if (c++ == 0) {
                return "be.nisc.modelmappertools.editor.A";
            } else {
                return "be.nisc.modelmappertools.editor.B";
            }
        }

        @Override
        public String chooseConverter() {
            return "be.nisc.modelmappertools.maven.testClasses.MyConverter";
        }
    };
}
