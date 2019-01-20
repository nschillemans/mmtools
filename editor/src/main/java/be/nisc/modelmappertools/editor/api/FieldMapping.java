package be.nisc.modelmappertools.editor.api;

import java.io.Serializable;
import java.util.Objects;

public class FieldMapping implements Serializable {
    public String fromPath, toPath;
    public String fromAccessPath, toAccessPath;
    public String toDefinitionType;
    public String toType;
    public String converter;
    public boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldMapping that = (FieldMapping) o;
        return Objects.equals(fromPath, that.fromPath) &&
                Objects.equals(toPath, that.toPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromPath, toPath);
    }
}
