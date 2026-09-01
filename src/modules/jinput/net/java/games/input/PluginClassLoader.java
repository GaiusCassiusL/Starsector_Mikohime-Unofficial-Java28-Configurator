package net.java.games.input;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class PluginClassLoader extends ClassLoader {
   // decompiler artifact: synthetic field for assert statements, not emitted by Vineflower
   static final boolean $assertionsDisabled = !PluginClassLoader.class.desiredAssertionStatus();
   private static String pluginDirectory;
   private static final FileFilter JAR_FILTER = new PluginClassLoader.JarFileFilter();

   public PluginClassLoader() {
      super(Thread.currentThread().getContextClassLoader());
   }

   protected Class findClass(String name) throws ClassNotFoundException {
      byte[] b = this.loadClassData(name);
      return this.defineClass(name, b, 0, b.length);
   }

   private byte[] readClassBytes(InputStream inputStream, long expectedSize) throws IOException {
      if (expectedSize > Integer.MAX_VALUE) {
         throw new IOException("Class file is too large: " + expectedSize + " bytes");
      } else {
         int initialCapacity = expectedSize > 0L ? (int)Math.min(expectedSize, 65536L) : 4096;
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream(initialCapacity);
         byte[] buffer = new byte[4096];

         int read;
         while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
         }

         byte[] bytes = outputStream.toByteArray();
         if (expectedSize >= 0L && bytes.length != expectedSize) {
            throw new IOException("Class file size changed while reading: expected " + expectedSize + " bytes, read " + bytes.length);
         }
         return bytes;
      }
   }

   private byte[] loadClassData(String name) throws ClassNotFoundException {
      if (pluginDirectory == null) {
         pluginDirectory = DefaultControllerEnvironment.libPath + File.separator + "controller";
      }

      try {
         return this.loadClassFromDirectory(name);
      } catch (Exception e) {
         try {
            return this.loadClassFromJAR(name);
         } catch (IOException e2) {
            throw new ClassNotFoundException(name, e2);
         }
      }
   }

   private byte[] loadClassFromDirectory(String name) throws ClassNotFoundException, IOException {
      StringTokenizer tokenizer = new StringTokenizer(name, ".");
      StringBuffer path = new StringBuffer(pluginDirectory);

      while (tokenizer.hasMoreTokens()) {
         path.append(File.separator);
         path.append(tokenizer.nextToken());
      }

      path.append(".class");
      File file = new File(path.toString());
      if (!file.exists()) {
         throw new ClassNotFoundException(name);
      } else {
         if (!$assertionsDisabled && file.length() > 2147483647L) {
            throw new AssertionError();
         } else {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
               return this.readClassBytes(fileInputStream, file.length());
            }
         }
      }
   }

   private byte[] loadClassFromJAR(String name) throws ClassNotFoundException, IOException {
      File dir = new File(pluginDirectory);
      File[] jarFiles = dir.listFiles(JAR_FILTER);
      if (jarFiles == null) {
         throw new ClassNotFoundException("Could not find class " + name);
      }

      String entryName = name.replace('.', '/') + ".class";

      for (int i = 0; i < jarFiles.length; i++) {
         try (JarFile jarfile = new JarFile(jarFiles[i])) {
            JarEntry jarentry = jarfile.getJarEntry(entryName);
            if (jarentry != null) {
               try (InputStream jarInputStream = jarfile.getInputStream(jarentry)) {
                  return this.readClassBytes(jarInputStream, jarentry.getSize());
               }
            }
         }
      }

      throw new FileNotFoundException(name);
   }

   private static class JarFileFilter implements FileFilter {
      private JarFileFilter() {
      }

      public boolean accept(File file) {
         return file.isFile() && file.getName().toUpperCase(Locale.ROOT).endsWith(".JAR");
      }
   }
}
