package be.nisc.modelmappertools.editor.test;

import java.lang.Integer;
import java.lang.Object;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.DestinationSetter;
import org.modelmapper.spi.SourceGetter;

public class ModelMapperMappings implements be.nisc.modelmappertools.api.ModelMapperMappings {
  public void registerMappings(ModelMapper modelMapper) {
    new AToCMapping().registerMappings(modelMapper);
    new AToBMapping().registerMappings(modelMapper);
  }

  public static class AToCMapping {
    public void registerMappings(ModelMapper modelMapper) {
      TypeMap<A, C> typeMap = modelMapper.getTypeMap(be.nisc.modelmappertools.editor.test.A.class, be.nisc.modelmappertools.editor.test.C.class);
      if (typeMap == null) typeMap = modelMapper.createTypeMap(be.nisc.modelmappertools.editor.test.A.class, be.nisc.modelmappertools.editor.test.C.class);
      typeMap.addMappings(m -> m.map(new BSourceGetter(), new ZDestinationSetter()));
    }

    public class ZDestinationSetter implements DestinationSetter<C, Integer> {
      public void accept(C target, Integer value) {
        target.setZ(value);
      }
    }

    public class BSourceGetter implements SourceGetter<A> {
      public Object get(A target) {
        return target.getB();
      }
    }
  }

  public static class AToBMapping {
    public void registerMappings(ModelMapper modelMapper) {
      TypeMap<A, B> typeMap = modelMapper.getTypeMap(be.nisc.modelmappertools.editor.test.A.class, be.nisc.modelmappertools.editor.test.B.class);
      if (typeMap == null) typeMap = modelMapper.createTypeMap(be.nisc.modelmappertools.editor.test.A.class, be.nisc.modelmappertools.editor.test.B.class);
      typeMap.addMappings(m -> m.map(new ZSourceGetter(), new TypeDestinationSetter()));
    }

    public class TypeDestinationSetter implements DestinationSetter<B, Integer> {
      public void accept(B target, Integer value) {
        target.setType(value);
      }
    }

    public class ZSourceGetter implements SourceGetter<A> {
      public Object get(A target) {
        return target.getZ();
      }
    }
  }
}
