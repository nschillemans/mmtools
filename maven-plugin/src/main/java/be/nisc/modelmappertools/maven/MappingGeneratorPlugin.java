package be.nisc.modelmappertools.maven;

import be.nisc.modelmappertools.api.ClassMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.modelmapper.ModelMapper;

import javax.lang.model.element.Modifier;
import java.io.File;

@Mojo(name = "generate")
public class MappingGeneratorPlugin extends AbstractMojo {
    @Parameter(name = "mappingsFile", required = true)
    private File mappingsFile;

    @Parameter(name = "outputFolder", required = true)
    private File outputFolder;

    @Parameter(name = "outputPackageName", required = true)
    private String outputPackageName;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute() {
        try {
            TypeSpec.Builder output = TypeSpec.classBuilder("ModelMapperMappings")
                    .addModifiers(Modifier.PUBLIC);

            MethodSpec.Builder registerMethodBuilder = MethodSpec.methodBuilder("registerMappings")
                    .addParameter(ModelMapper.class, "modelMapper")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            objectMapper.readTree(mappingsFile).forEach(node -> {
                try {
                    ClassMapping classMapping = objectMapper.readValue(node.toString(), ClassMapping.class);
                    TypeSpec classMapper = new MappingGenerator(classMapping).generateCustomMappings();
                    output.addType(classMapper);
                    registerMethodBuilder.addStatement("new $T().registerMappings(modelMapper)", ClassName.bestGuess(classMapper.name));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            output.addMethod(registerMethodBuilder.build());

            JavaFile.builder(outputPackageName, output.build()).build().writeTo(outputFolder);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
