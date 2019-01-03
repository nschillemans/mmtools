package be.nisc.modelmappertools.editor.gui;

import be.nisc.modelmappertools.api.ClassMapping;
import be.nisc.modelmappertools.editor.gui.dialog.NewMappingDialog;
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

public class Workspace extends JFrame {

    private ObjectMapper objectMapper = new ObjectMapper();
    private Object classPicker;
    private Object saveHandler;
    private JTabbedPane tabbedPane;

    public Workspace(Object classPicker, Object saveHandler, File file) {
        this.classPicker = classPicker;
        this.saveHandler = saveHandler;

        // Menu
        JMenuBar menubar = new JMenuBar();

        JButton saveButton = new JButton();
        saveButton.setText("Save");
        saveButton.addActionListener((event) -> {
            try {
                saveHandler.getClass().getMethod("save", String.class).invoke(saveHandler, serializeMappings());
            } catch (Exception e) {
                throw new RuntimeException("Could not invoke save handler", e);
            }
        });

        menubar.add(saveButton);

        JButton addButton = new JButton();
        addButton.setText("Add");
        addButton.addActionListener((event) -> {
            ClassMapping classMapping = new NewMappingDialog(classPicker).prompt();

            if (classMapping != null) {
                addEditor(classMapping);
            }
        });

        menubar.add(addButton);

        setJMenuBar(menubar);

        this.tabbedPane = new JTabbedPane();

        try {
            List<ClassMapping> mappings = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionLikeType(ArrayList.class, ClassMapping.class));
            mappings.stream().forEach(mapping -> addEditor(mapping));
        } catch (IOException e) {
            throw new RuntimeException("Could not parse mappings file", e);
        }

        this.getContentPane().add(this.tabbedPane);
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
        Class fromClass;
        Class toClass;

        try {
            fromClass = Class.forName(mapping.from);
            toClass = Class.forName(mapping.to);
        } catch (Exception e) {
            throw new RuntimeException("Could not load specified classes", e);
        }

        Editor editor = new Editor(classPicker, mapping);
        tabbedPane.addTab(fromClass.getSimpleName() + " -> " + toClass.getSimpleName(), editor);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    public static void main(String[] args) {
        Workspace frame = new Workspace(new TestClassPicker(), new TestSaveHandler(), new File("F:\\Dev\\Java\\modelmappertools\\editor\\src\\main\\resources\\mappings.json"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setVisible(true);
    }

    public static class TestSaveHandler implements SaveHandler {
        @Override
        public void save(String output) {
            try {
                Files.write(new File("F:\\Dev\\Java\\modelmappertools\\editor\\src\\main\\resources\\mappings.json").toPath(), output.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TestClassPicker implements ClassPicker {

        private int c = 0;

        @Override
        public String chooseMappingClass() {
            if (c++ == 0) {
                return "be.nisc.modelmappertools.editor.A";
            } else {
                return "be.nisc.modelmappertools.editor.C";
            }
        }

        @Override
        public String chooseConverter() {
            return "brolCOnverter";
        }
    };
}
