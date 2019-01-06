package be.nisc.modelmappertools.maven.testclasses;

import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

public class MyConverter implements Converter {
    @Override
    public Object convert(MappingContext mappingContext) {
        System.out.println("mappy");
        return 90;
    }
}
