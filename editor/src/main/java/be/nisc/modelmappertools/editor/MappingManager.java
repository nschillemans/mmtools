package be.nisc.modelmappertools.editor;

import be.nisc.modelmappertools.api.ClassMapping;
import be.nisc.modelmappertools.api.FieldMapping;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.PropertyInfo;
import org.modelmapper.spi.PropertyMapping;

import java.lang.reflect.Field;
import java.util.*;

public class MappingManager {

    private static final List<Class> SIMPLE_FIELDS;

    static {
        SIMPLE_FIELDS = new ArrayList<>();
        SIMPLE_FIELDS.add(String.class);
        SIMPLE_FIELDS.add(Integer.class);
        SIMPLE_FIELDS.add(Long.class);
        SIMPLE_FIELDS.add(Double.class);
        SIMPLE_FIELDS.add(Float.class);
    }

    private Class from, to;
    private ModelMapper modelMapper;
    private Set<FieldMapping> allFieldMappings;

    public MappingManager() {

    }

    public void load(ClassMapping classMapping) {
        try {
            this.from = Class.forName(classMapping.from);
            this.to = Class.forName(classMapping.to);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        this.allFieldMappings = classMapping.mappings;

        if (classMapping.mappings == null) {
            this.allFieldMappings = new HashSet<>();
            initializeModelMapper();
        }
    }

    private void initializeModelMapper() {
        this.modelMapper = new ModelMapper();
        this.modelMapper.createTypeMap(from, to);

        modelMapper.getTypeMaps().stream().forEach(typeMap -> typeMap.getMappings().forEach(propMap -> {
            PropertyMapping mmMapping = (PropertyMapping) propMap;
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.fromPath = calculatePath(mmMapping.getSourceProperties());
            fieldMapping.toPath = calculatePath(mmMapping.getDestinationProperties());
            fieldMapping.autoGenerated = true;
            fieldMapping.active = true;
            fieldMapping.toType = mmMapping.getDestinationProperties().get(mmMapping.getDestinationProperties().size() - 1).getType().getCanonicalName();
            allFieldMappings.add(fieldMapping);
        }));
    }

    public Set<FieldMapping> getFieldMappings() {
        return Collections.unmodifiableSet(allFieldMappings);
    }

    public ClassMapping getOutput() {
        ClassMapping classMapping = new ClassMapping();
        classMapping.from = from.getCanonicalName();
        classMapping.to = to.getCanonicalName();
        classMapping.mappings = allFieldMappings;
        return classMapping;
    }

    public void addMapping(FieldMapping fieldMapping) {
        if (fieldMapping.autoGenerated && !allFieldMappings.contains(fieldMapping)) {
            throw new RuntimeException("Auto-generated mappings can only be created by the MappingManager");
        }

        fieldMapping.active = true;

        if (allFieldMappings.contains(fieldMapping)) {
            FieldMapping existing = allFieldMappings.stream().filter(fm -> fm.equals(fieldMapping)).findAny().get();
            fieldMapping.autoGenerated = existing.autoGenerated;
            allFieldMappings.remove(existing);
        }

        allFieldMappings.add(fieldMapping);
    }

    public void removeMapping(FieldMapping fieldMapping) {
        if (fieldMapping.autoGenerated) {
            fieldMapping.active = false;
        } else {
            allFieldMappings.remove(fieldMapping);
        }
    }

    public List<FieldInfo> getFromFields() {
        return findFieldsDeep("", from);
    }

    public List<FieldInfo> getToFields() {
        return findFieldsDeep("", to);
    }

    private List<FieldInfo> findFieldsDeep(String path, Class clazz) {
        List<FieldInfo> myFields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            String fieldPath = (path.isEmpty() ? "" : path + ".") + field.getName();
            List<FieldInfo> containedFields = null;

            if (!SIMPLE_FIELDS.contains(field.getType())) {
                containedFields = findFieldsDeep(fieldPath, field.getType());
            }

            myFields.add(new FieldInfo(fieldPath, field.getType(), containedFields));
        }

        return myFields;
    }

    private String[] calculatePath(List<? extends PropertyInfo> propertyInfo) {
        String[] path = new String[propertyInfo.size()];

        for (int i = 0; i < propertyInfo.size(); i++) {
            path[i] = propertyInfo.get(i).getName();
        }

        return path;
    }
}
