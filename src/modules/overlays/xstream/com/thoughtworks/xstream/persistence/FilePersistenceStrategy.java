package com.thoughtworks.xstream.persistence;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;

public class FilePersistenceStrategy extends AbstractFilePersistenceStrategy {
   private final String illegalChars;

   public FilePersistenceStrategy(File baseDirectory) {
      this(baseDirectory, new XStream(new DomDriver()));
   }

   public FilePersistenceStrategy(File baseDirectory, XStream xstream) {
      this(baseDirectory, xstream, "utf-8", "<>?:/\\\"|*%");
   }

   public FilePersistenceStrategy(File baseDirectory, XStream xstream, String encoding, String illegalChars) {
      super(baseDirectory, xstream, encoding);
      this.illegalChars = illegalChars;
   }

   protected boolean isValid(File dir, String name) {
      return super.isValid(dir, name) && name.indexOf(64) > 0;
   }

   protected Object extractKey(String name) {
      String key = this.unescape(name.substring(0, name.length() - 4));
      if ("null@null".equals(key)) {
         return null;
      } else {
         int idx = key.indexOf(64);
         if (idx < 0) {
            ConversionException exception = new ConversionException("No valid key");
            exception.add("key", key);
            throw exception;
         } else {
            Class type = this.getMapper().realClass(key.substring(0, idx));
            Converter converter = this.getConverterLookup().lookupConverterForType(type);
            if (converter instanceof SingleValueConverter) {
               SingleValueConverter svConverter = (SingleValueConverter)converter;
               return svConverter.fromString(key.substring(idx + 1));
            } else {
               ConversionException exception = new ConversionException("No SingleValueConverter available for key type");
               exception.add("key-type", type.getName());
               throw exception;
            }
         }
      }
   }

   protected String unescape(String name) {
      StringBuffer buffer = new StringBuffer();

      for (int idx = name.indexOf(37); idx >= 0; idx = name.indexOf(37)) {
         buffer.append(name.substring(0, idx));
         int c = Integer.parseInt(name.substring(idx + 1, idx + 3), 16);
         buffer.append((char)c);
         name = name.substring(idx + 3);
      }

      buffer.append(name);
      return buffer.toString();
   }

   protected String getName(Object key) {
      if (key == null) {
         return "null@null.xml";
      } else {
         Class type = key.getClass();
         Converter converter = this.getConverterLookup().lookupConverterForType(type);
         if (converter instanceof SingleValueConverter) {
            SingleValueConverter svConverter = (SingleValueConverter)converter;
            return this.getMapper().serializedClass(type) + '@' + this.escape(svConverter.toString(key)) + ".xml";
         } else {
            ConversionException exception = new ConversionException("No SingleValueConverter available for key type");
            exception.add("key-type", type.getName());
            throw exception;
         }
      }
   }

   protected String escape(String key) {
      StringBuffer buffer = new StringBuffer();
      char[] array = key.toCharArray();

      for (int i = 0; i < array.length; i++) {
         char c = array[i];
         if (c >= ' ' && this.illegalChars.indexOf(c) < 0) {
            buffer.append(c);
         } else {
            buffer.append("%" + Integer.toHexString(c).toUpperCase());
         }
      }

      return buffer.toString();
   }
}
