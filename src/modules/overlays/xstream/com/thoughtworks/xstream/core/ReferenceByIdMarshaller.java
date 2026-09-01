package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.mapper.Mapper;

public class ReferenceByIdMarshaller extends AbstractReferenceMarshaller {
   private final ReferenceByIdMarshaller.IDGenerator idGenerator;

   public ReferenceByIdMarshaller(
      HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper, ReferenceByIdMarshaller.IDGenerator idGenerator
   ) {
      super(writer, converterLookup, mapper);
      this.idGenerator = idGenerator;
   }

   public ReferenceByIdMarshaller(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper) {
      this(writer, converterLookup, mapper, new SequenceGenerator(1));
   }

   protected String createReference(Path currentPath, Object existingReferenceKey) {
      return existingReferenceKey.toString();
   }

   protected Object createReferenceKey(Path currentPath, Object item) {
      return this.idGenerator.next(item);
   }

   protected void fireValidReference(Object referenceKey) {
      String attributeName = this.getMapper().aliasForSystemAttribute("id");
      if (attributeName != null) {
         this.writer.addAttribute(attributeName, referenceKey.toString());
      }
   }

   public interface IDGenerator {
      String next(Object var1);
   }
}
