package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.mapper.Mapper;

public class ReferenceByXPathMarshaller extends AbstractReferenceMarshaller {
   private final int mode;

   public ReferenceByXPathMarshaller(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper, int mode) {
      super(writer, converterLookup, mapper);
      this.mode = mode;
   }

   protected String createReference(Path currentPath, Object existingReferenceKey) {
      Path existingPath = (Path)existingReferenceKey;
      Path referencePath = (this.mode & ReferenceByXPathMarshallingStrategy.ABSOLUTE) > 0 ? existingPath : currentPath.relativeTo(existingPath);
      return (this.mode & ReferenceByXPathMarshallingStrategy.SINGLE_NODE) > 0 ? referencePath.explicit() : referencePath.toString();
   }

   protected Object createReferenceKey(Path currentPath, Object item) {
      return currentPath;
   }

   protected void fireValidReference(Object referenceKey) {
   }
}
