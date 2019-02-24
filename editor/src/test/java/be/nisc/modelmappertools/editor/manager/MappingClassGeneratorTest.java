package be.nisc.modelmappertools.editor.manager;

import be.nisc.modelmappertools.editor.api.ClassMapping;
import be.nisc.modelmappertools.editor.api.FieldMapping;
import be.nisc.modelmappertools.editor.manager.testclasses.A;
import be.nisc.modelmappertools.editor.manager.testclasses.B;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MappingClassGeneratorTest {
    @Test
    public void testGenerate() throws IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {


        ClassMapping classMapping = new ClassMapping();
        classMapping.from = A.class.getCanonicalName();
        classMapping.to = B.class.getCanonicalName();

        Set<FieldMapping> fieldMappingSet = new HashSet<>();
        classMapping.mappings = fieldMappingSet;

        FieldMapping fieldMapping = new FieldMapping();
        fieldMapping.fromPath = "a";
        fieldMapping.toPath = "b";
        fieldMapping.active = true;
        fieldMapping.toType = Integer.class.getCanonicalName();
        fieldMappingSet.add(fieldMapping);

        fieldMapping = new FieldMapping();
        fieldMapping.fromPath = "b";
        fieldMapping.toPath = "b";
        fieldMapping.active = false;
        fieldMappingSet.add(fieldMapping);

        ClassLoader cl = generateAndCompile(Collections.singletonList(classMapping));

        Object mappings = cl.loadClass("be.nisc.modelmappertools.editor.manager.testclasses.ModelMapperMappings").getConstructors()[0].newInstance();
        mappings.getClass().getMethod("registerMappings", 
    }

    private ClassLoader generateAndCompile(List<ClassMapping> classMappings) throws IOException {
        File folder = new File("target/temp");

        FileUtils.deleteDirectory(folder);
        Files.createDirectories(new File("target/temp/be/nisc/modelmappertools/editor/manager/testclasses").toPath());

        File mappings = new File("target/temp/be/nisc/modelmappertools/editor/manager/testclasses/ModelMapperMappings.java");
        MappingClassGenerator generator = new MappingClassGenerator(this.getClass().getClassLoader());
        generator.write(mappings, "be.nisc.modelmappertools.editor.manager.testclasses", classMappings);

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null );
        final Iterable<? extends JavaFileObject> sources = manager.getJavaFileObjectsFromFiles(Arrays.asList(
                mappings
        ));

        final JavaCompiler.CompilationTask task = compiler.getTask( null, manager, diagnostics, null, null, sources );
        boolean success = task.call();

        if (!success) {
            diagnostics.getDiagnostics().stream().forEach(diagnostic -> System.out.println(diagnostic.getMessage(Locale.getDefault())));
            Assert.fail("Could not compile generated mappings");
        }

        Files.walk(new File("target/temp").toPath(), FileVisitOption.FOLLOW_LINKS)
                .filter(path -> path.toFile().getName().endsWith(".class"))
                .forEach(path -> {
                    try {
                        Files.copy(path, new File(path.toFile().getAbsolutePath().replace("target/temp", "target/test-classes")).toPath());
                        System.out.println("Copying " + path.toFile().getAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        List<URL> classes = Files.walk(new File("target/test-classes").toPath(), FileVisitOption.FOLLOW_LINKS)
                .filter(p ->
                        p.toFile().getName().endsWith(".class")
                )
                .map(p -> {
                    try {
                        return p.toFile().toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        return new URLClassLoader(classes.toArray(new URL[0]));
    }
}
