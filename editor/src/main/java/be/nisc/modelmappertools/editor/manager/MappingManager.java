package be.nisc.modelmappertools.editor.manager;

import be.nisc.modelmappertools.editor.api.ClassMapping;
import be.nisc.modelmappertools.editor.api.FieldMapping;
import org.modelmapper.ModelMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

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

    private ClassLoader classLoader;
    private InvocationUtils invocationUtils;
    private Class from, to;
    private Set<FieldMapping> fieldMappings = new HashSet<>();

    public static List<ClassMapping> getAvailableClassMappings(ClassLoader classLoader, String mappingsClass) {
        if (mappingsClass == null) {
            return Collections.emptyList();
        }

        InvocationUtils invocationUtils = new InvocationUtils(classLoader);
        Object modelMapper = invocationUtils.instantiate(ModelMapper.class.getCanonicalName());
        invocationUtils.invoke(invocationUtils.instantiate(mappingsClass),"registerMappings", modelMapper);

        Collection<Object> typeMaps = invocationUtils.invoke(modelMapper, "getTypeMaps");

        return typeMaps.stream().map(typeMap -> {
            ClassMapping classMapping = new ClassMapping();
            classMapping.from = ((Class) invocationUtils.invoke(typeMap, "getSourceType")).getCanonicalName();
            classMapping.to = ((Class) invocationUtils.invoke(typeMap, "getDestinationType")).getCanonicalName();
            return classMapping;
        }).collect(Collectors.toList());
    }

    public MappingManager(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.invocationUtils = new InvocationUtils(classLoader);
    }

    public void load(String mappingsClass, String fromClass, String toClass) {
        Object modelMapper = invocationUtils.instantiate(ModelMapper.class.getCanonicalName());

        try {
            this.from = classLoader.loadClass(fromClass);
            this.to = classLoader.loadClass(toClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (mappingsClass != null) {
            invocationUtils.invoke(invocationUtils.instantiate(mappingsClass), "registerMappings", modelMapper);
        }

        Object typeMap = invocationUtils.invoke(modelMapper, "getTypeMap", from, to);

        if (typeMap == null) {
            typeMap = invocationUtils.invoke(modelMapper, "createTypeMap", from, to);
        }

        Collection<Object> propertyMaps = invocationUtils.invoke(typeMap, "getMappings");
        propertyMaps.stream().forEach(propertyMap -> {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.fromPath = calculatePath(invocationUtils.invoke(propertyMap, "getSourceProperties"));
            fieldMapping.toPath = calculatePath(invocationUtils.invoke(propertyMap, "getDestinationProperties"));
            fieldMapping.toType = ((Class) invocationUtils.invoke(invocationUtils.invoke(propertyMap, "getLastDestinationProperty"), "getType")).getCanonicalName();
            fieldMapping.toDefinitionType = ((Class) invocationUtils.invoke(invocationUtils.invoke(propertyMap, "getLastDestinationProperty"), "getInitialType")).getCanonicalName();
            fieldMapping.active = true;

            Object converter = invocationUtils.invoke(propertyMap, "getConverter");

            if (converter != null) {
                fieldMapping.converter = converter.getClass().getCanonicalName();
            }

            this.fieldMappings.add(fieldMapping);
        });
    }

    public Set<FieldMapping> getFieldMappings() {
        return Collections.unmodifiableSet(fieldMappings);
    }

    public ClassMapping getOutput() {
        ClassMapping classMapping = new ClassMapping();
        classMapping.from = from.getCanonicalName();
        classMapping.to = to.getCanonicalName();
        classMapping.mappings = fieldMappings;
        return classMapping;
    }

    public void addMapping(FieldMapping fieldMapping) {
        if (fieldMappings.contains(fieldMapping)) {
            FieldMapping existing = fieldMappings.stream().filter(fm -> fm.equals(fieldMapping)).findAny().get();
            fieldMappings.remove(existing);
        }

        fieldMapping.active = true;
        fieldMappings.add(fieldMapping);
    }

    public void removeMapping(FieldMapping fieldMapping) {
        fieldMapping.active = false;
    }

    public List<FieldInfo> getFromFields() {
        return findFieldsDeep("", "", "get", from, new ArrayList<>());
    }

    public List<FieldInfo> getToFields() {
        return findFieldsDeep("", "", "set", to, new ArrayList<>());
    }

    private List<FieldInfo> findFieldsDeep(String path, String accessPath, String direction, Class clazz, List<Class> upstreamTypes) {
        List<FieldInfo> myFields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            String fieldPath = (path.isEmpty() ? "" : path + ".") + field.getName();
            List<FieldInfo> containedFields = null;

            String access = null;

            if (Modifier.isPublic(field.getModifiers())) {
                access = "FIELD";
            } else if (hasAccessibleMethod(field, clazz, direction)) {
                access = "METHOD";
            }

            if (access != null) {
                access = (accessPath.isEmpty() ? "" : accessPath + ".") + access;

                if (!SIMPLE_FIELDS.contains(field.getType()) && !upstreamTypes.contains(field.getType())) {
                    upstreamTypes.add(field.getType());
                    containedFields = findFieldsDeep(fieldPath, access, direction, field.getType(), upstreamTypes);
                }

                myFields.add(new FieldInfo(fieldPath, field.getType(), access, containedFields));
            }
        }

        return myFields;
    }

    private boolean hasAccessibleMethod(Field field, Class clazz, String prefix) {
        String methodName = prefix + capitalizedName(field.getName());
        Method method = null;

        try {
            if (prefix.equals("set")) {
                method = clazz.getMethod(methodName, field.getType());
            } else if (prefix.equals("get")) {
                method = clazz.getMethod(methodName);
            }
        } catch (NoSuchMethodException e) {
            // Ok then
        }

        return method != null && Modifier.isPublic(method.getModifiers());
    }

    private String capitalizedName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String calculatePath(List<Object> propertyInfos) {
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < propertyInfos.size(); i++) {
            pathBuilder.append(invocationUtils.invoke(propertyInfos.get(i), "getName") + ".");
        }

        pathBuilder.setLength(pathBuilder.length() - 1);

        return pathBuilder.toString();
    }
}
