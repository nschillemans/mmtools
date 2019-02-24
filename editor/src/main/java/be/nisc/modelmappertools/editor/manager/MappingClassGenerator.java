package be.nisc.modelmappertools.editor.manager;

import be.nisc.modelmappertools.editor.api.ClassMapping;
import be.nisc.modelmappertools.editor.api.FieldMapping;
import be.nisc.modelmappertools.editor.test.ModelMapperMappings;
import com.squareup.javapoet.*;
import org.apache.commons.io.FileUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.DestinationSetter;
import org.modelmapper.spi.SourceGetter;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MappingClassGenerator {

    private ClassLoader classLoader;

    public MappingClassGenerator(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void write(File javaFile, String packageName, List<ClassMapping> classMappings) {

        // Find target package from given file if exists
        if (javaFile.exists()) {
            Launcher launcher = new Launcher();
            launcher.addInputResource(javaFile.getAbsolutePath());
            CtModel model = launcher.buildModel();

            if (model.getAllTypes().size() == 0) {
                throw new RuntimeException(String.format("Could not parse java source file [%s]", javaFile.getAbsolutePath()));
            }

            CtType currentClass = model.getAllTypes().iterator().next();
            packageName = currentClass.getPackage().getQualifiedName();
        }

        TypeSpec.Builder output = TypeSpec.classBuilder("ModelMapperMappings")
                .addSuperinterface(ClassName.get(be.nisc.modelmappertools.api.ModelMapperMappings.class))
                .addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder registerMethodBuilder = MethodSpec.methodBuilder("registerMappings")
                .addParameter(ModelMapper.class, "modelMapper")
                .addModifiers(Modifier.PUBLIC);

        classMappings.forEach(classMapping -> {
            try {
                TypeSpec classMapper = new MappingGenerator(classLoader, classMapping).generateCustomMappings();
                output.addType(classMapper);
                registerMethodBuilder.addStatement("new $T().registerMappings(modelMapper)", ClassName.bestGuess(classMapper.name));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        output.addMethod(registerMethodBuilder.build());

        try (FileWriter fileWriter = new FileWriter(javaFile)) {
            JavaFile.builder(packageName, output.build()).build().writeTo(fileWriter);
        } catch (IOException e) {
            throw new RuntimeException("Could not open writer on output file", e);
        }
    }

    private static class MappingGenerator {
        private ClassLoader classLoader;
        private ClassMapping classMapping;
        private ClassName from;
        private ClassName to;

        public MappingGenerator(ClassLoader classLoader, ClassMapping classMapping) {
            this.classLoader = classLoader;
            this.classMapping = classMapping;
            from = ClassName.bestGuess(classMapping.from);
            to = ClassName.bestGuess(classMapping.to);
        }

        private String capitalizedName(String name) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        public TypeSpec generateCustomMappings() {
            TypeSpec.Builder mappingsClassBuilder = TypeSpec
                    .classBuilder(capitalizedName(from.simpleName()) + "To" + capitalizedName(to.simpleName()) + "Mapping")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            CodeBlock.Builder registerBodyBuilder = CodeBlock.builder();

            registerBodyBuilder
                    .addStatement("$T<$T, $T> typeMap = modelMapper.getTypeMap($L.class, $L.class)", TypeMap.class, from, to, from.reflectionName(), to.reflectionName())
                    .addStatement("if (typeMap == null) typeMap = modelMapper.createTypeMap($L.class, $L.class)", from.reflectionName(), to.reflectionName());

            MappingManager mappingManager = new MappingManager(classLoader);
            mappingManager.load(null, classMapping.from, classMapping.to);
            Set<FieldMapping> autoGenerated = mappingManager.getFieldMappings();
            Set<FieldMapping> skips = new HashSet<>();
            Set<String> mappedToPaths = new HashSet<>();

            // Write field mappings
            classMapping.mappings.stream().forEach(fieldMapping -> {
                if (!autoGenerated.contains(fieldMapping) && fieldMapping.active) {
                    TypeSpec setter = generateSourceGetter(fieldMapping.fromPath, fieldMapping.fromAccessPath);
                    TypeSpec getter = generateDestinationSetter(fieldMapping.toPath, fieldMapping.toAccessPath, fieldMapping.toType);
                    mappingsClassBuilder.addType(getter);
                    mappingsClassBuilder.addType(setter);

                    registerBodyBuilder.add("typeMap.addMappings(m -> m");

                    if (fieldMapping.converter != null) {
                        registerBodyBuilder.add(".using(new $T())", ClassName.bestGuess(fieldMapping.converter));
                    }

                    registerBodyBuilder.addStatement(".map(new $T(), new $T()))", ClassName.bestGuess(setter.name), ClassName.bestGuess(getter.name));
                    mappedToPaths.add(fieldMapping.toPath);
                } else if (autoGenerated.contains(fieldMapping) && !fieldMapping.active) {
                    skips.add(fieldMapping);
                }
            });

            // Write skipped properties
            skips.stream().filter(skip -> !mappedToPaths.contains(skip.toPath)).forEach(skip -> {
                String setPropertyName = capitalizedName(skip.toPath.substring(skip.toPath.lastIndexOf(".") + 1));
                registerBodyBuilder.addStatement("typeMap.addMappings(m -> m.skip($L::set$L))", skip.toDefinitionType, setPropertyName);
            });

            mappingsClassBuilder.addMethod(
                    MethodSpec
                            .methodBuilder("registerMappings")
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(ModelMapper.class, "modelMapper")
                            .addCode(registerBodyBuilder.build())
                            .build()
            );

            return mappingsClassBuilder.build();
        }

        private String generateName(String path, String suffix) {
            StringBuilder nameBuilder = new StringBuilder();

            for (String part : path.split("\\.")) {
                nameBuilder.append(capitalizedName(part));
            }

            nameBuilder.append(suffix);

            return nameBuilder.toString();
        }

        private TypeSpec generateSourceGetter(String fromPath, String accessPath) {
            String name = generateName(fromPath, "SourceGetter");

            StringBuilder bodyBuilder = new StringBuilder("return target.");

            String[] pathSplit = fromPath.split("\\.");
            //String[] accessPathSplit = accessPath.split("\\.");

            for (int p = 0; p < pathSplit.length; p++) {
                //if (accessPathSplit[p].equals("FIELD")) {
                //    bodyBuilder.append(pathSplit[p]).append(".");
                //} else if (accessPathSplit[p].equals("METHOD")) {
                    bodyBuilder.append("get").append(capitalizedName(pathSplit[p])).append("().");
                //}
            }

            bodyBuilder.setLength(bodyBuilder.length() - 1);

            TypeSpec getter = TypeSpec.classBuilder(name)
                    .addSuperinterface(ParameterizedTypeName.get(ClassName.get(SourceGetter.class), from))
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(
                            MethodSpec
                                    .methodBuilder("get")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(from, "target")
                                    .addStatement(bodyBuilder.toString())
                                    .returns(Object.class)
                                    .build()
                    )
                    .build();

            return getter;
        }

        private TypeSpec generateDestinationSetter(String toPath, String accessPath, String toType) {
            String[] pathSplit = toPath.split("\\.");
            //String[] accessPathSplit = accessPath.split("\\.");

            String name = generateName(toPath, "DestinationSetter");

            StringBuilder bodyBuilder = new StringBuilder("target.");

            for (int x = 0; x < pathSplit.length; x++) {
                if (x < pathSplit.length - 1) {
                    //if (accessPathSplit[x].equals("FIELD")) {
                    //    bodyBuilder.append(pathSplit[x]).append(".");
                    //} else if (accessPathSplit[x].equals("METHOD")) {
                        bodyBuilder.append("get").append(capitalizedName(pathSplit[x])).append("().");
                    //}
                } else {
                    //if (accessPathSplit[x].equals("FIELD")) {
                    //    bodyBuilder.append(pathSplit[x]).append(" = value");
                    //} else if (accessPathSplit[x].equals("METHOD")) {
                        bodyBuilder.append("set").append(capitalizedName(pathSplit[x])).append("(value)");
                    //}
                }
            }

            TypeSpec setter = TypeSpec.classBuilder(name)
                    .addSuperinterface(ParameterizedTypeName.get(ClassName.get(DestinationSetter.class), to, ClassName.bestGuess(getClassType(toType))))
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(
                            MethodSpec
                                    .methodBuilder("accept")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(to, "target")
                                    .addParameter(ClassName.bestGuess(getClassType(toType)), "value")
                                    .addStatement(bodyBuilder.toString())
                                    .build()
                    )
                    .build();

            return setter;
        }

        private String getClassType(String type) {
            if (type.equals("int")) {
                return "java.lang.Integer";
            } else {
                return type;
            }
        }
    }
}
