package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class ThrowableConverter implements Converter {
   private Converter defaultConverter;
   private final ConverterLookup lookup;

   /** @deprecated */
   public ThrowableConverter(Converter defaultConverter) {
      this.defaultConverter = defaultConverter;
      this.lookup = null;
   }

   public ThrowableConverter(ConverterLookup lookup) {
      this.lookup = lookup;
   }

   public boolean canConvert(Class type) {
      return type != null && Throwable.class.isAssignableFrom(type);
   }

   public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Throwable throwable = (Throwable)source;
      if (throwable.getCause() == null) {
         try {
            throwable.initCause(null);
         } catch (IllegalStateException var6) {
         }
      }

      throwable.getStackTrace();
      this.getConverter().marshal(throwable, writer, context);
   }

   private Converter getConverter() {
      return this.defaultConverter != null ? this.defaultConverter : this.lookup.lookupConverterForType(Object.class);
   }

   public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      return this.getConverter().unmarshal(reader, context);
   }
}
