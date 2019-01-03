package be.nisc.modelmappertools.maven;

import be.nisc.modelmappertools.api.ClassMapping;
import com.squareup.javapoet.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.DestinationSetter;
import org.modelmapper.spi.SourceGetter;

import javax.lang.model.element.Modifier;

public class MappingGenerator {
    private ClassMapping classMapping;
    private ClassName from;
    private ClassName to;


    public MappingGenerator(ClassMapping classMapping) {
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

        classMapping.mappings.stream().forEach(fieldMapping -> {
            TypeSpec setter = generateSourceGetter(fieldMapping.fromPath);
            TypeSpec getter = generateDestinationSetter(fieldMapping.toPath, fieldMapping.toType);
            mappingsClassBuilder.addType(getter);
            mappingsClassBuilder.addType(setter);

            registerBodyBuilder.add("typeMap.addMappings(m -> m");

            if (fieldMapping.converter != null) {
                registerBodyBuilder.add(".using(new $T())", ClassName.bestGuess(fieldMapping.converter));
            }

            registerBodyBuilder.addStatement(".map(new $T(), new $T()))", ClassName.bestGuess(setter.name), ClassName.bestGuess(getter.name));
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

    private String generateName(String[] path, String suffix) {
        StringBuilder nameBuilder = new StringBuilder();

        for (String part : path) {
            nameBuilder.append(capitalizedName(part));
        }

        nameBuilder.append(suffix);

        return nameBuilder.toString();
    }

    private TypeSpec generateSourceGetter(String[] fromPath) {
        String name = generateName(fromPath, "SourceGetter");

        StringBuilder bodyBuilder = new StringBuilder("return target.");

        for (String part : fromPath) {
            bodyBuilder.append("get").append(capitalizedName(part)).append("().");
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

    private TypeSpec generateDestinationSetter(String[] toPath, String toType) {
        String name = generateName(toPath, "DestinationSetter");

        StringBuilder bodyBuilder = new StringBuilder("target.");

        for (int x = 0; x < toPath.length; x++) {
            if (x < toPath.length - 1) {
                bodyBuilder.append("get").append(capitalizedName(toPath[x])).append("().");
            } else {
                bodyBuilder.append("set").append(capitalizedName(toPath[x])).append("(value)");
            }
        }

        TypeSpec setter = TypeSpec.classBuilder(name)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(DestinationSetter.class), to, ClassName.bestGuess(toType)))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(
                        MethodSpec
                                .methodBuilder("accept")
                                .addModifiers(Modifier.PUBLIC)
                                .addParameter(to, "target")
                                .addParameter(ClassName.bestGuess(toType), "value")
                                .addStatement(bodyBuilder.toString())
                                .build()
                )
                .build();

        return setter;
    }
}
