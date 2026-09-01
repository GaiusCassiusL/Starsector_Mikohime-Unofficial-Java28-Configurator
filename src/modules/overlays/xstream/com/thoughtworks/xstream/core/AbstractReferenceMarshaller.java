package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.util.ObjectIdDictionary;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public abstract class AbstractReferenceMarshaller extends TreeMarshaller {
   private ObjectIdDictionary references = new ObjectIdDictionary();
   private ObjectIdDictionary implicitElements = new ObjectIdDictionary();
   private PathTracker pathTracker = new PathTracker();
   private Path lastPath;

   public AbstractReferenceMarshaller(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper) {
      super(writer, converterLookup, mapper);
      this.writer = new PathTrackingWriter(writer, this.pathTracker);
   }

   public void convert(Object item, Converter converter) {
      if (this.getMapper().isImmutableValueType(item.getClass())) {
         converter.marshal(item, this.writer, this);
      } else {
         Path currentPath = this.pathTracker.getPath();
         AbstractReferenceMarshaller.Id existingReference = (AbstractReferenceMarshaller.Id)this.references.lookupId(item);
         if (existingReference != null && existingReference.getPath() != currentPath) {
            String attributeName = this.getMapper().aliasForSystemAttribute("reference");
            if (attributeName != null) {
               this.writer.addAttribute(attributeName, this.createReference(currentPath, existingReference.getItem()));
            }
         } else {
            Object newReferenceKey = existingReference == null ? this.createReferenceKey(currentPath, item) : existingReference.getItem();
            if (this.lastPath == null || !currentPath.isAncestor(this.lastPath)) {
               this.fireValidReference(newReferenceKey);
               this.lastPath = currentPath;
               this.references.associateId(item, new AbstractReferenceMarshaller.Id(newReferenceKey, currentPath));
            }

            converter.marshal(item, this.writer, new AbstractReferenceMarshaller$1(this, newReferenceKey, currentPath));
         }
      }
   }

   protected abstract String createReference(Path var1, Object var2);

   protected abstract Object createReferenceKey(Path var1, Object var2);

   protected abstract void fireValidReference(Object var1);

   private static class Id {
      private Object item;
      private Path path;

      public Id(Object item, Path path) {
         this.item = item;
         this.path = path;
      }

      protected Object getItem() {
         return this.item;
      }

      protected Path getPath() {
         return this.path;
      }
   }

   public static class ReferencedImplicitElementException extends ConversionException {
      public ReferencedImplicitElementException(Object item, Path path) {
         super("Cannot reference implicit element");
         this.add("implicit-element", item.toString());
         this.add("referencing-element", path.toString());
      }
   }
}
