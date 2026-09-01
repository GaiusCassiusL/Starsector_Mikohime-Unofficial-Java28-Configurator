package com.thoughtworks.xstream;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.javabean.JavaBeanProvider;
import com.thoughtworks.xstream.converters.reflection.FieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.security.TypeHierarchyPermission;
import com.thoughtworks.xstream.security.TypePermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.datatype.DatatypeFactory;

public class XStreamer {
   private static final TypePermission[] PERMISSIONS = new TypePermission[]{
      new TypeHierarchyPermission(ConverterMatcher.class),
      new TypeHierarchyPermission(Mapper.class),
      new TypeHierarchyPermission(XStream.class),
      new TypeHierarchyPermission(ReflectionProvider.class),
      new TypeHierarchyPermission(JavaBeanProvider.class),
      new TypeHierarchyPermission(FieldKeySorter.class),
      new TypeHierarchyPermission(ConverterLookup.class),
      new TypeHierarchyPermission(ConverterRegistry.class),
      new TypeHierarchyPermission(HierarchicalStreamDriver.class),
      new TypeHierarchyPermission(MarshallingStrategy.class),
      new TypeHierarchyPermission(MarshallingContext.class),
      new TypeHierarchyPermission(UnmarshallingContext.class),
      new TypeHierarchyPermission(NameCoder.class),
      new TypeHierarchyPermission(TypePermission.class),
      new WildcardTypePermission(true, new String[]{JVM.class.getPackage().getName() + ".**"}),
      new TypeHierarchyPermission(DatatypeFactory.class)
   };

   public String toXML(XStream xstream, Object obj) throws ObjectStreamException {
      Writer writer = new StringWriter();

      try {
         this.toXML(xstream, obj, writer);
      } catch (ObjectStreamException e) {
         throw e;
      } catch (IOException e) {
         throw new StreamException("Unexpected IO error from a StringWriter", e);
      }

      return writer.toString();
   }

   public void toXML(XStream xstream, Object obj, Writer out) throws IOException {
      XStream outer = new XStream();
      ObjectOutputStream oos = outer.createObjectOutputStream(out);

      try {
         oos.writeObject(xstream);
         oos.flush();
         xstream.toXML(obj, out);
      } finally {
         oos.close();
      }
   }

   public Object fromXML(String xml) throws ClassNotFoundException, ObjectStreamException {
      try {
         return this.fromXML(new StringReader(xml));
      } catch (ObjectStreamException e) {
         throw e;
      } catch (IOException e) {
         throw new StreamException("Unexpected IO error from a StringReader", e);
      }
   }

   public Object fromXML(String xml, TypePermission[] permissions) throws ClassNotFoundException, ObjectStreamException {
      try {
         return this.fromXML(new StringReader(xml), permissions);
      } catch (ObjectStreamException e) {
         throw e;
      } catch (IOException e) {
         throw new StreamException("Unexpected IO error from a StringReader", e);
      }
   }

   public Object fromXML(HierarchicalStreamDriver driver, String xml) throws ClassNotFoundException, ObjectStreamException {
      try {
         return this.fromXML(driver, new StringReader(xml));
      } catch (ObjectStreamException e) {
         throw e;
      } catch (IOException e) {
         throw new StreamException("Unexpected IO error from a StringReader", e);
      }
   }

   public Object fromXML(HierarchicalStreamDriver driver, String xml, TypePermission[] permissions) throws ClassNotFoundException, ObjectStreamException {
      try {
         return this.fromXML(driver, new StringReader(xml), permissions);
      } catch (ObjectStreamException e) {
         throw e;
      } catch (IOException e) {
         throw new StreamException("Unexpected IO error from a StringReader", e);
      }
   }

   public Object fromXML(Reader xml) throws IOException, ClassNotFoundException {
      return this.fromXML(new XppDriver(), xml);
   }

   public Object fromXML(Reader xml, TypePermission[] permissions) throws IOException, ClassNotFoundException {
      return this.fromXML(new XppDriver(), xml, permissions);
   }

   public Object fromXML(HierarchicalStreamDriver driver, Reader xml) throws IOException, ClassNotFoundException {
      return this.fromXML(driver, xml, PERMISSIONS);
   }

   public Object fromXML(HierarchicalStreamDriver driver, Reader xml, TypePermission[] permissions) throws IOException, ClassNotFoundException {
      XStream outer = new XStream(driver);

      for (int i = 0; i < permissions.length; i++) {
         outer.addPermission(permissions[i]);
      }

      HierarchicalStreamReader reader = driver.createReader(xml);
      ObjectInputStream configIn = outer.createObjectInputStream(reader);

      try {
         XStream configured = (XStream)configIn.readObject();
         ObjectInputStream in = configured.createObjectInputStream(reader);

         try {
            return in.readObject();
         } finally {
            in.close();
         }
      } finally {
         configIn.close();
      }
   }

   public static TypePermission[] getDefaultPermissions() {
      return (TypePermission[])PERMISSIONS.clone();
   }
}
