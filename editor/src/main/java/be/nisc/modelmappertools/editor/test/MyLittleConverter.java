package be.nisc.modelmappertools.editor.test;

import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

public class MyLittleConverter implements Converter {
    @Override
    public Object convert(MappingContext mappingContext) {
        return Integer.parseInt((String) mappingContext.getSource()) * 10;
    }
}
