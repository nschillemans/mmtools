package be.nisc.modelmappertools.editor;

import java.util.List;

public class FieldInfo {
    public String path;
    public Class type;
    public List<FieldInfo> contained;

    public FieldInfo(String path, Class type, List<FieldInfo> contained) {
        this.path = path;
        this.type = type;
        this.contained = contained;
    }
}
