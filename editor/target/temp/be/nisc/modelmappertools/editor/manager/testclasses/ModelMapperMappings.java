package be.nisc.modelmappertools.editor.manager.testclasses;

import java.lang.Integer;
import java.lang.Object;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.DestinationSetter;
import org.modelmapper.spi.SourceGetter;

public class ModelMapperMappings implements be.nisc.modelmappertools.api.ModelMapperMappings {
  public void registerMappings(ModelMapper modelMapper) {
    new AToBMapping().registerMappings(modelMapper);
  }

  public static class AToBMapping {
    public void registerMappings(ModelMapper modelMapper) {
      TypeMap<A, B> typeMap = modelMapper.getTypeMap(be.nisc.modelmappertools.editor.manager.testclasses.A.class, be.nisc.modelmappertools.editor.manager.testclasses.B.class);
      if (typeMap == null) typeMap = modelMapper.createTypeMap(be.nisc.modelmappertools.editor.manager.testclasses.A.class, be.nisc.modelmappertools.editor.manager.testclasses.B.class);
      typeMap.addMappings(m -> m.map(new ASourceGetter(), new BDestinationSetter()));
    }

    public class BDestinationSetter implements DestinationSetter<B, Integer> {
      public void accept(B target, Integer value) {
        target.setB(value);
      }
    }

    public class ASourceGetter implements SourceGetter<A> {
      public Object get(A target) {
        return target.getA();
      }
    }
  }
}
