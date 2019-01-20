package be.nisc.modelmappertools.editor.api;

import java.io.Serializable;
import java.util.Set;

public class ClassMapping implements Serializable {
    public String from, to;
    public Set<FieldMapping> mappings;
}
