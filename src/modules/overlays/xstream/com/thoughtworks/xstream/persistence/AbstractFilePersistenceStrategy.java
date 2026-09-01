package com.thoughtworks.xstream.persistence;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

public abstract class AbstractFilePersistenceStrategy implements PersistenceStrategy {
   private final FilenameFilter filter;
   private final File baseDirectory;
   private final String encoding;
   private final transient XStream xstream;

   public AbstractFilePersistenceStrategy(File baseDirectory, XStream xstream, String encoding) {
      this.baseDirectory = baseDirectory;
      this.xstream = xstream;
      this.encoding = encoding;
      this.filter = new AbstractFilePersistenceStrategy.ValidFilenameFilter();
   }

   protected ConverterLookup getConverterLookup() {
      return this.xstream.getConverterLookup();
   }

   protected Mapper getMapper() {
      return this.xstream.getMapper();
   }

   protected boolean isValid(File dir, String name) {
      return name.endsWith(".xml");
   }

   protected abstract Object extractKey(String var1);

   protected abstract String getName(Object var1);

   private void writeFile(File file, Object value) {
      try {
         FileOutputStream out = new FileOutputStream(file);
         Writer writer = this.encoding != null ? new OutputStreamWriter(out, this.encoding) : new OutputStreamWriter(out);

         try {
            this.xstream.toXML(value, writer);
         } finally {
            writer.close();
         }
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   private File getFile(String filename) {
      return new File(this.baseDirectory, filename);
   }

   private Object readFile(File file) {
      try {
         FileInputStream in = new FileInputStream(file);
         Reader reader = this.encoding != null ? new InputStreamReader(in, this.encoding) : new InputStreamReader(in);

         try {
            return this.xstream.fromXML(reader);
         } finally {
            reader.close();
         }
      } catch (FileNotFoundException e) {
         return null;
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public Object put(Object key, Object value) {
      Object oldValue = this.get(key);
      String filename = this.getName(key);
      this.writeFile(new File(this.baseDirectory, filename), value);
      return oldValue;
   }

   public Iterator iterator() {
      return new AbstractFilePersistenceStrategy.XmlMapEntriesIterator();
   }

   public int size() {
      return this.baseDirectory.list(this.filter).length;
   }

   public boolean containsKey(Object key) {
      File file = this.getFile(this.getName(key));
      return file.isFile();
   }

   public Object get(Object key) {
      return this.readFile(this.getFile(this.getName(key)));
   }

   public Object remove(Object key) {
      File file = this.getFile(this.getName(key));
      Object value = null;
      if (file.isFile()) {
         value = this.readFile(file);
         file.delete();
      }

      return value;
   }

   protected class ValidFilenameFilter implements FilenameFilter {
      public boolean accept(File dir, String name) {
         return new File(dir, name).isFile() && AbstractFilePersistenceStrategy.this.isValid(dir, name);
      }
   }

   protected class XmlMapEntriesIterator implements Iterator {
      private final File[] files = AbstractFilePersistenceStrategy.this.baseDirectory.listFiles(AbstractFilePersistenceStrategy.this.filter);
      private int position = -1;
      private File current = null;

      public boolean hasNext() {
         return this.position + 1 < this.files.length;
      }

      public void remove() {
         if (this.current == null) {
            throw new IllegalStateException();
         }

         this.current.delete();
      }

      public Object next() {
         return new AbstractFilePersistenceStrategy$XmlMapEntriesIterator$1(this);
      }
   }
}
