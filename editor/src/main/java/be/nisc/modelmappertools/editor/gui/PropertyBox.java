package be.nisc.modelmappertools.editor.gui;

public class PropertyBox {
    private String propertyName;
    private Class type;
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

    public PropertyBox(String propertyName, Class type, Role role) {
        this.propertyName = propertyName;
        this.type = type;
        this.role = role;
    }

    public enum Role {
        FROM, TO
    }

    @Override
    public String toString() {
        return propertyName;
    }
}
