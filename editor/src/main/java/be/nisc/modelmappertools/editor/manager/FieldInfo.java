package be.nisc.modelmappertools.editor.manager;

import java.util.List;

public class FieldInfo {
    public String path;
    public Class type;
    public List<FieldInfo> contained;
    public String accessPath;

    public FieldInfo(String path, Class type, String accessPath, List<FieldInfo> contained) {
        this.path = path;
        this.type = type;
        this.accessPath = accessPath;
        this.contained = contained;
    }
}
