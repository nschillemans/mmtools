package be.nisc.modelmappertools.editor.gui;

import be.nisc.modelmappertools.api.FieldMapping;

import java.io.Serializable;

public class PropertyBox implements Serializable {
    private String propertyName;
    private Class type;
    private String accessPath;
    private Role role;

    public String getPropertyName() {
        return propertyName;
    }

    public Role getRole() {
        return role;
    }

    public Class getType() {
        return type;
    }

    public PropertyBox(String propertyName, Class type, String accessPath, Role role) {
        this.propertyName = propertyName;
        this.type = type;
        this.accessPath = accessPath;
        this.role = role;
    }

    public String getAccessPath() {
        return accessPath;
    }

    public enum Role {
        FROM, TO
    }

    @Override
    public String toString() {
        return propertyName;
    }
}
