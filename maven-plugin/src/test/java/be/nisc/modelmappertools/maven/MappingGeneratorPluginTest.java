package be.nisc.modelmappertools.maven;

import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import javax.tools.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;

public class MappingGeneratorPluginTest {

    private ClassLoader classLoader;

    @Before
    public void buildMappings() {
        MappingGeneratorPlugin mappingGeneratorPlugin = new MappingGeneratorPlugin();
        mappingGeneratorPlugin.setMappingsFile(new File("src/test/resources/mappings.json"));
        mappingGeneratorPlugin.setOutputFolder(new File("target/temp/mappingSources"));
        mappingGeneratorPlugin.setOutputPackageName("be.nisc.modelmappertools.maven.test");
        mappingGeneratorPlugin.execute();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<>();

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(
                new File("target/temp/mappingSources/be/nisc/modelmappertools/maven/test/ModelMapperMappings.java"));

        JavaCompiler.CompilationTask task = compiler.getTask(null,
                fileManager, diagnostics, null, null, fileObjects);

        if (!task.call()) {
            diagnostics.getDiagnostics().forEach(System.out::println);
            throw new RuntimeException("Compilation failure");
        } else {
            URL[] testClassPath = new URL[5];

            try {
                testClassPath[0] = new File("target/test-classes/be/nisc/modelmappertools/maven/testclasses/A.class").toURI().toURL();
                testClassPath[1] = new File("target/test-classes/be/nisc/modelmappertools/maven/testclasses/B.class").toURI().toURL();
                testClassPath[2] = new File("target/test-classes/be/nisc/modelmappertools/maven/testclasses/D.class").toURI().toURL();
                testClassPath[3] = new File("target/test-classes/be/nisc/modelmappertools/maven/testclasses/MyConverter.class").toURI().toURL();
                testClassPath[4] = new File("target/temp/mappingSources/be/nisc/modelmappertools/maven/test/ModelMapperMappings.class").toURI().toURL();

                classLoader = new URLClassLoader(testClassPath);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testMappings() throws Exception {
        Object modelMapper = classLoader.loadClass(ModelMapper.class.getCanonicalName()).getConstructors()[0].newInstance();
        Class mappings = classLoader.loadClass("be.nisc.modelmappertools.maven.test.ModelMapperMappings");
        Object from = classLoader.loadClass("be.nisc.modelmappertools.maven.testclasses.A").newInstance();
        Class toClass = classLoader.loadClass("be.nisc.modelmappertools.maven.testclasses.B");
        mappings.getMethod("registerMappings", modelMapper.getClass()).invoke(null, modelMapper);
        Method map = modelMapper.getClass().getMethod("map", Object.class, Class.class);

        from.getClass().getMethod("setA", String.class).invoke(from, "5");

        Object to = map.invoke(modelMapper, from, toClass);
        Integer mappedA = (Integer) to.getClass().getMethod("getA").invoke(to);
        System.out.println(mappedA);
    }
}
