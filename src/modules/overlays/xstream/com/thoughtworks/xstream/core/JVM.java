package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.util.Base64Encoder;
import com.thoughtworks.xstream.core.util.CustomObjectOutputStream;
import com.thoughtworks.xstream.core.util.DependencyInjectionFactory;
import com.thoughtworks.xstream.core.util.PresortedMap;
import com.thoughtworks.xstream.core.util.PresortedSet;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.AttributedString;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class JVM implements Caching {
   private ReflectionProvider reflectionProvider;
   private static final boolean isAWTAvailable;
   private static final boolean isSwingAvailable;
   private static final boolean isSQLAvailable;
   private static final boolean canAllocateWithUnsafe;
   private static final boolean canWriteWithUnsafe;
   private static final boolean optimizedTreeSetAddAll;
   private static final boolean optimizedTreeMapPutAll;
   private static final boolean canParseUTCDateFormat;
   private static final boolean canParseISO8601TimeZoneInDateFormat;
   private static final boolean canCreateDerivedObjectOutputStream;
   private static final String vendor = System.getProperty("java.vm.vendor");
   private static final float majorJavaVersion = getMajorJavaVersion();
   private static final float DEFAULT_JAVA_VERSION = 1.4F;
   private static final boolean reverseFieldOrder = false;
   private static final Class reflectionProviderType;
   private static final StringCodec base64Codec;

   private static final float getMajorJavaVersion() {
      try {
         return isAndroid() ? 1.5F : Float.parseFloat(System.getProperty("java.specification.version"));
      } catch (NumberFormatException e) {
         return 1.4F;
      }
   }

   /** @deprecated */
   public static boolean is14() {
      return isVersion(4);
   }

   /** @deprecated */
   public static boolean is15() {
      return isVersion(5);
   }

   /** @deprecated */
   public static boolean is16() {
      return isVersion(6);
   }

   /** @deprecated */
   public static boolean is17() {
      return isVersion(7);
   }

   /** @deprecated */
   public static boolean is18() {
      return isVersion(8);
   }

   /** @deprecated */
   public static boolean is19() {
      return majorJavaVersion >= 1.9F;
   }

   /** @deprecated */
   public static boolean is9() {
      return isVersion(9);
   }

   public static boolean isVersion(int version) {
      if (version < 1) {
         throw new IllegalArgumentException("Java version range starts with at least 1.");
      }

      float v = majorJavaVersion < 9.0F ? 1.0F + version * 0.1F : version;
      return majorJavaVersion >= v;
   }

   private static boolean isIBM() {
      return vendor.indexOf("IBM") != -1;
   }

   private static boolean isAndroid() {
      return vendor.indexOf("Android") != -1;
   }

   public static Class loadClassForName(String name) {
      return loadClassForName(name, true);
   }

   /** @deprecated */
   public Class loadClass(String name) {
      return loadClassForName(name, true);
   }

   public static Class loadClassForName(String name, boolean initialize) {
      try {
         return Class.forName(name, initialize, JVM.class.getClassLoader());
      } catch (LinkageError e) {
         return null;
      } catch (ClassNotFoundException e) {
         return null;
      }
   }

   /** @deprecated */
   public Class loadClass(String name, boolean initialize) {
      return loadClassForName(name, initialize);
   }

   public static ReflectionProvider newReflectionProvider() {
      return (ReflectionProvider)DependencyInjectionFactory.newInstance(reflectionProviderType, null);
   }

   public static ReflectionProvider newReflectionProvider(FieldDictionary dictionary) {
      return (ReflectionProvider)DependencyInjectionFactory.newInstance(reflectionProviderType, new Object[]{dictionary});
   }

   public static Class getStaxInputFactory() throws ClassNotFoundException {
      if (isVersion(6)) {
         return isIBM() ? Class.forName("com.ibm.xml.xlxp.api.stax.XMLInputFactoryImpl") : Class.forName("com.sun.xml.internal.stream.XMLInputFactoryImpl");
      } else {
         return null;
      }
   }

   public static Class getStaxOutputFactory() throws ClassNotFoundException {
      if (isVersion(6)) {
         return isIBM() ? Class.forName("com.ibm.xml.xlxp.api.stax.XMLOutputFactoryImpl") : Class.forName("com.sun.xml.internal.stream.XMLOutputFactoryImpl");
      } else {
         return null;
      }
   }

   public static StringCodec getBase64Codec() {
      return base64Codec;
   }

   /** @deprecated */
   public synchronized ReflectionProvider bestReflectionProvider() {
      if (this.reflectionProvider == null) {
         this.reflectionProvider = newReflectionProvider();
      }

      return this.reflectionProvider;
   }

   private static boolean canUseSunUnsafeReflectionProvider() {
      return canAllocateWithUnsafe;
   }

   private static boolean canUseSunLimitedUnsafeReflectionProvider() {
      return canWriteWithUnsafe;
   }

   /** @deprecated */
   public static boolean reverseFieldDefinition() {
      return false;
   }

   public static boolean isAWTAvailable() {
      return isAWTAvailable;
   }

   /** @deprecated */
   public boolean supportsAWT() {
      return isAWTAvailable;
   }

   public static boolean isSwingAvailable() {
      return isSwingAvailable;
   }

   /** @deprecated */
   public boolean supportsSwing() {
      return isSwingAvailable;
   }

   public static boolean isSQLAvailable() {
      return isSQLAvailable;
   }

   /** @deprecated */
   public boolean supportsSQL() {
      return isSQLAvailable;
   }

   public static boolean hasOptimizedTreeSetAddAll() {
      return optimizedTreeSetAddAll;
   }

   public static boolean hasOptimizedTreeMapPutAll() {
      return optimizedTreeMapPutAll;
   }

   public static boolean canParseUTCDateFormat() {
      return canParseUTCDateFormat;
   }

   public static boolean canParseISO8601TimeZoneInDateFormat() {
      return canParseISO8601TimeZoneInDateFormat;
   }

   public static boolean canCreateDerivedObjectOutputStream() {
      return canCreateDerivedObjectOutputStream;
   }

   /** @deprecated */
   public void flushCache() {
   }

   public static void main(String[] args) {
      boolean reverseJDK = false;
      Field[] fields = AttributedString.class.getDeclaredFields();

      for (int i = 0; i < fields.length; i++) {
         if (fields[i].getName().equals("text")) {
            reverseJDK = i > 3;
            break;
         }
      }

      boolean reverseLocal = false;
      fields = JVM.Test.class.getDeclaredFields();

      for (int i = 0; i < fields.length; i++) {
         if (fields[i].getName().equals("o")) {
            reverseLocal = i > 3;
            break;
         }
      }

      String staxInputFactory = null;

      try {
         staxInputFactory = getStaxInputFactory().getName();
      } catch (ClassNotFoundException e) {
         staxInputFactory = e.getMessage();
      } catch (NullPointerException var10) {
      }

      String staxOutputFactory = null;

      try {
         staxOutputFactory = getStaxOutputFactory().getName();
      } catch (ClassNotFoundException e) {
         staxOutputFactory = e.getMessage();
      } catch (NullPointerException var8) {
      }

      System.out.println("XStream JVM diagnostics");
      System.out.println("java.specification.version: " + System.getProperty("java.specification.version"));
      System.out.println("java.specification.vendor: " + System.getProperty("java.specification.vendor"));
      System.out.println("java.specification.name: " + System.getProperty("java.specification.name"));
      System.out.println("java.vm.vendor: " + vendor);
      System.out.println("java.vendor: " + System.getProperty("java.vendor"));
      System.out.println("java.vm.name: " + System.getProperty("java.vm.name"));
      System.out.println("Version: " + majorJavaVersion);
      System.out.println("XStream support for enhanced Mode: " + canUseSunUnsafeReflectionProvider());
      System.out.println("XStream support for reduced Mode: " + canUseSunLimitedUnsafeReflectionProvider());
      System.out.println("Supports AWT: " + isAWTAvailable());
      System.out.println("Supports Swing: " + isSwingAvailable());
      System.out.println("Supports SQL: " + isSQLAvailable());
      System.out.println("Java Beans EventHandler present: " + (loadClassForName("java.beans.EventHandler") != null));
      System.out.println("Standard StAX XMLInputFactory: " + staxInputFactory);
      System.out.println("Standard StAX XMLOutputFactory: " + staxOutputFactory);
      System.out.println("Standard Base64 Codec: " + getBase64Codec().getClass().toString());
      System.out.println("Optimized TreeSet.addAll: " + hasOptimizedTreeSetAddAll());
      System.out.println("Optimized TreeMap.putAll: " + hasOptimizedTreeMapPutAll());
      System.out.println("Can parse UTC date format: " + canParseUTCDateFormat());
      System.out.println("Can create derive ObjectOutputStream: " + canCreateDerivedObjectOutputStream());
      System.out.println("Reverse field order detected for JDK: " + reverseJDK);
      System.out.println("Reverse field order detected (only if JVM class itself has been compiled): " + reverseLocal);
   }

   static {
      boolean test = true;
      Object unsafe = null;

      try {
         Class unsafeClass = Class.forName("sun.misc.Unsafe");
         Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
         unsafeField.setAccessible(true);
         unsafe = unsafeField.get(null);
         Method allocateInstance = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);
         allocateInstance.setAccessible(true);
         test = allocateInstance.invoke(unsafe, JVM.Test.class) != null;
      } catch (Exception e) {
         test = false;
      } catch (Error e) {
         test = false;
      }

      canAllocateWithUnsafe = test;
      test = false;
      Class type = PureJavaReflectionProvider.class;
      if (canUseSunUnsafeReflectionProvider()) {
         Class cls = loadClassForName("com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider");
         if (cls != null) {
            try {
               ReflectionProvider provider = (ReflectionProvider)DependencyInjectionFactory.newInstance(cls, null);
               JVM.Test t = (JVM.Test)provider.newInstance(JVM.Test.class);

               try {
                  provider.writeField(t, "o", "object", JVM.Test.class);
                  provider.writeField(t, "c", new Character('c'), JVM.Test.class);
                  provider.writeField(t, "b", new Byte((byte)1), JVM.Test.class);
                  provider.writeField(t, "s", new Short((short)1), JVM.Test.class);
                  provider.writeField(t, "i", new Integer(1), JVM.Test.class);
                  provider.writeField(t, "l", new Long(1L), JVM.Test.class);
                  provider.writeField(t, "f", new Float(1.0F), JVM.Test.class);
                  provider.writeField(t, "d", new Double(1.0), JVM.Test.class);
                  provider.writeField(t, "bool", Boolean.TRUE, JVM.Test.class);
                  test = true;
               } catch (IncompatibleClassChangeError e) {
                  cls = null;
               } catch (ObjectAccessException e) {
                  cls = null;
               }

               if (cls == null) {
                  cls = loadClassForName("com.thoughtworks.xstream.converters.reflection.SunLimitedUnsafeReflectionProvider");
               }

               type = cls;
            } catch (ObjectAccessException var21) {
            }
         }
      }

      reflectionProviderType = type;
      canWriteWithUnsafe = test;
      Comparator comparator = new JVM$1();
      SortedMap map = new PresortedMap(comparator);
      map.put("one", null);
      map.put("two", null);

      try {
         new TreeMap(comparator).putAll(map);
         test = true;
      } catch (RuntimeException e) {
         test = false;
      }

      optimizedTreeMapPutAll = test;
      SortedSet set = new PresortedSet(comparator);
      set.addAll(map.keySet());

      try {
         new TreeSet(comparator).addAll(set);
         test = true;
      } catch (RuntimeException e) {
         test = false;
      }

      optimizedTreeSetAddAll = test;

      try {
         new SimpleDateFormat("z").parse("UTC");
         test = true;
      } catch (RuntimeException e) {
         test = false;
      } catch (ParseException e) {
         test = false;
      }

      canParseUTCDateFormat = test;

      try {
         new SimpleDateFormat("X").parse("Z");
         test = true;
      } catch (RuntimeException e) {
         test = false;
      } catch (ParseException e) {
         test = false;
      }

      canParseISO8601TimeZoneInDateFormat = test;

      try {
         test = new CustomObjectOutputStream(null) != null;
      } catch (RuntimeException e) {
         test = false;
      } catch (IOException e) {
         test = false;
      }

      canCreateDerivedObjectOutputStream = test;
      isAWTAvailable = loadClassForName("java.awt.Color", false) != null;
      isSwingAvailable = loadClassForName("javax.swing.LookAndFeel", false) != null;
      isSQLAvailable = loadClassForName("java.sql.Date") != null;
      StringCodec base64 = null;
      Class base64Class = loadClassForName("com.thoughtworks.xstream.core.util.Base64JavaUtilCodec");
      if (base64Class == null) {
         base64Class = loadClassForName("com.thoughtworks.xstream.core.util.Base64JAXBCodec");
      }

      if (base64Class != null) {
         try {
            base64 = (StringCodec)base64Class.newInstance();
         } catch (Exception var9) {
         } catch (Error var10) {
         }
      }

      if (base64 == null) {
         base64 = new Base64Encoder();
      }

      base64Codec = base64;
   }

   static class Test {
      private Object o;
      private char c;
      private byte b;
      private short s;
      private int i;
      private long l;
      private float f;
      private double d;
      private boolean bool;

      Test() {
         throw new UnsupportedOperationException();
      }
   }
}
