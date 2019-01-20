package be.nisc.modelmappertools.editor.gui;

import be.nisc.modelmappertools.editor.api.ClassMapping;
import be.nisc.modelmappertools.editor.api.FieldMapping;
import be.nisc.modelmappertools.editor.gui.dialog.ButtonChoiceDialog;
import be.nisc.modelmappertools.editor.gui.plugins.ClassPicker;
import be.nisc.modelmappertools.editor.manager.FieldInfo;
import be.nisc.modelmappertools.editor.manager.MappingManager;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class Editor extends JPanel {

    private static final int FROM_X = 20, TO_X = 300;

    private boolean initializing = true;
    private ClassPicker classPicker;
    private MappingManager mappingManager;
    private Map<String, mxCell> fromVertices;
    private Map<String, mxCell> toVertices;
    private mxGraph graph;
    private String fromClass, toClass;

    public Editor(ClassLoader classLoader, ClassPicker classPicker, String mappingsClass, String fromClass, String toClass) {
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.classPicker = classPicker;
        this.mappingManager = new MappingManager(classLoader);
        this.fromVertices = new HashMap<>();
        this.toVertices = new HashMap<>();

        // Graph
        graph = new mxGraph();

        graph.setAllowDanglingEdges(false);
        graph.setCellsMovable(false);
        graph.setCellsLocked(true);

        mxGraphComponent graphComponent = new mxGraphComponent(graph);

        graph.addListener(mxEvent.CELL_CONNECTED, (Object sender, mxEventObject evt) -> {
            if (!initializing && !(boolean) evt.getProperty("source")) {
                mxCell edge = (mxCell) evt.getProperty("edge");
                if (validateNewMapping(edge)) {
                    FieldMapping fieldMapping;

                    if (edge.getValue() instanceof MappingConnector) {
                        fieldMapping = ((MappingConnector) edge.getValue()).getFieldMapping();
                    } else {
                        fieldMapping = edgeToFieldMapping(edge);
                        edge.setValue(new MappingConnector(fieldMapping));
                    }

                    mappingManager.addMapping(fieldMapping);
                } else {
                    graph.removeCells(new Object[] { edge });
                }
            }
        });

        graphComponent.getGraphControl().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    mxCell clicked = (mxCell) graphComponent.getCellAt(e.getX(), e.getY());

                    if (clicked != null && clicked.isEdge()) {
                        MappingConnector mappingConnector = (MappingConnector) clicked.getValue();

                        int choice = 1;

                        if (mappingConnector.getFieldMapping().converter != null) {
                            choice = new ButtonChoiceDialog("Converter is present","Remove", "Replace", "Do nothing").prompt();
                        }

                        if (choice == 0) {
                            (((MappingConnector) clicked.getValue()).getFieldMapping()).converter = null;
                            graph.refresh();
                        } else if (choice == 1) {
                            String converter = classPicker.chooseConverter();

                            if (converter != null) {
                                (((MappingConnector) clicked.getValue()).getFieldMapping()).converter = converter;
                                graph.refresh();
                            }
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        graphComponent.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    mxCell selected = (mxCell) graph.getSelectionCell();

                    if (selected.isEdge()) {
                        mappingManager.removeMapping(((MappingConnector) selected.getValue()).getFieldMapping());
                        graph.removeCells(new Object[] { selected });
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        mappingManager.load(mappingsClass, fromClass, toClass);
        initializeField();

        setLayout(new BorderLayout());
        add(graphComponent, BorderLayout.CENTER);

        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                toVertices.values().stream().forEach(v -> {
                    int level = ((PropertyBox) v.getValue()).getPropertyName().split("\\.").length;
                    int newX = (e.getComponent().getSize().width - 200) + level * 20;
                    v.getGeometry().setX(newX);
                });
                graph.refresh();
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });

        initializing = false;
    }

    public String getFromClass() {
        return fromClass;
    }

    public String getToClass() {
        return toClass;
    }

    public ClassMapping getMapping() {
        return mappingManager.getOutput();
    }

    private FieldMapping edgeToFieldMapping(mxCell edge) {
        if (!edge.isEdge()) {
            throw new RuntimeException("Is not an edge: " + edge);
        } else {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.fromPath = ((PropertyBox) edge.getSource().getValue()).getPropertyName();
            fieldMapping.toAccessPath = ((PropertyBox) edge.getSource().getValue()).getAccessPath();
            fieldMapping.toPath = ((PropertyBox) edge.getTarget().getValue()).getPropertyName();
            fieldMapping.toType = ((PropertyBox) edge.getTarget().getValue()).getType().getCanonicalName();
            fieldMapping.toAccessPath = ((PropertyBox) edge.getTarget().getValue()).getAccessPath();

            return fieldMapping;
        }
    }

    private boolean validateNewMapping(mxCell edge) {
        return (((PropertyBox) edge.getTarget().getValue()).getRole().equals(PropertyBox.Role.TO) &&
                ((PropertyBox) edge.getSource().getValue()).getRole().equals(PropertyBox.Role.FROM) &&
                edge.getTarget().getEdgeCount() == 1);
    }

    private void initializeField() {
        graph.getModel().beginUpdate();

        try {
            graph.removeCells();
            drawProperties();
            drawMappings();
        } finally {
            graph.getModel().endUpdate();
        }
    }

    private void drawProperties() {
        drawPropertiesFor(fromVertices, this.mappingManager.getFromFields(), FROM_X, 20, PropertyBox.Role.FROM);
        drawPropertiesFor(toVertices, this.mappingManager.getToFields(), TO_X, 20, PropertyBox.Role.TO);
    }

    private int drawPropertiesFor(Map<String, mxCell> vertexMap, java.util.List<FieldInfo> fieldInfoList, int xStart, int yStart, PropertyBox.Role role) {
        int displayed = 0;

        for (FieldInfo fi : fieldInfoList) {
            mxCell fromVertex = (mxCell) graph.insertVertex(graph.getDefaultParent(), null, new PropertyBox(fi.path, fi.type, fi.accessPath, role), xStart, yStart + displayed++ * 50, 80, 30);
            vertexMap.put(fi.path, fromVertex);

            if (fi.contained != null) {
                displayed += drawPropertiesFor(vertexMap, fi.contained, xStart + 20, yStart + displayed * 50, role);
            }
        }

        return displayed;
    }

    private void drawMappings() {
        for (FieldMapping mapping : this.mappingManager.getFieldMappings()) {
            if (mapping.active) {
                graph.insertEdge(graph.getDefaultParent(), null, new MappingConnector(mapping), fromVertices.get(mapping.fromPath), toVertices.get(mapping.toPath));
            }
        }
    }
}
