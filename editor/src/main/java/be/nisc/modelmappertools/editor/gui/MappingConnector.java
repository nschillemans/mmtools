package be.nisc.modelmappertools.editor.gui;

import be.nisc.modelmappertools.editor.api.FieldMapping;

import java.io.Serializable;

public class MappingConnector implements Serializable {
        private FieldMapping fieldMapping;

        public MappingConnector(FieldMapping fieldMapping) {
            this.fieldMapping = fieldMapping;
        }

        public FieldMapping getFieldMapping() {
            return fieldMapping;
        }

        @Override
        public String toString() {
            return fieldMapping.converter != null ? fieldMapping.converter : "";
        }
    }