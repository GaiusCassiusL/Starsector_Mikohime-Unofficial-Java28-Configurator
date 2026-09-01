package org.apache.logging.log4j.core.util;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

public final class Loader {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String TSTR = "Caught Exception while in Loader.getResource. This may be innocuous.";
   static final Boolean ignoreTccl = Boolean.valueOf(
      PropertiesUtil.getProperties().getStringProperty(LoggingSystemProperty.LOADER_IGNORE_THREAD_CONTEXT_LOADER, null)
   );

   private Loader() {
   }

   public static ClassLoader getClassLoader() {
      return getClassLoader(Loader.class, null);
   }

   public static ClassLoader getClassLoader(final Class<?> class1, final Class<?> class2) {
      return LoaderUtil.getClassLoader(class1, class2);
   }

   public static ClassLoader getThreadContextClassLoader() {
      return LoaderUtil.getThreadContextClassLoader();
   }

   public static URL getResource(final String resource, final ClassLoader defaultLoader) {
      try {
         ClassLoader classLoader = getThreadContextClassLoader();
         if (classLoader != null) {
            LOGGER.trace("Trying to find [{}] using context class loader {}.", resource, classLoader);
            URL url = classLoader.getResource(resource);
            if (url != null) {
               return url;
            }
         }

         classLoader = Loader.class.getClassLoader();
         if (classLoader != null) {
            LOGGER.trace("Trying to find [{}] using {} class loader.", resource, classLoader);
            URL url = classLoader.getResource(resource);
            if (url != null) {
               return url;
            }
         }

         if (defaultLoader != null) {
            LOGGER.trace("Trying to find [{}] using {} class loader.", resource, defaultLoader);
            URL url = defaultLoader.getResource(resource);
            if (url != null) {
               return url;
            }
         }
      } catch (Throwable t) {
         LOGGER.warn("Caught Exception while in Loader.getResource. This may be innocuous.", t);
      }

      LOGGER.trace("Trying to find [{}] using ClassLoader.getSystemResource().", resource);
      return ClassLoader.getSystemResource(resource);
   }

   public static InputStream getResourceAsStream(final String resource, final ClassLoader defaultLoader) {
      try {
         ClassLoader classLoader = getThreadContextClassLoader();
         if (classLoader != null) {
            LOGGER.trace("Trying to find [{}] using context class loader {}.", resource, classLoader);
            InputStream is = classLoader.getResourceAsStream(resource);
            if (is != null) {
               return is;
            }
         }

         classLoader = Loader.class.getClassLoader();
         if (classLoader != null) {
            LOGGER.trace("Trying to find [{}] using {} class loader.", resource, classLoader);
            InputStream is = classLoader.getResourceAsStream(resource);
            if (is != null) {
               return is;
            }
         }

         if (defaultLoader != null) {
            LOGGER.trace("Trying to find [{}] using {} class loader.", resource, defaultLoader);
            InputStream is = defaultLoader.getResourceAsStream(resource);
            if (is != null) {
               return is;
            }
         }
      } catch (Throwable t) {
         LOGGER.warn("Caught Exception while in Loader.getResource. This may be innocuous.", t);
      }

      LOGGER.trace("Trying to find [{}] using ClassLoader.getSystemResource().", resource);
      return ClassLoader.getSystemResourceAsStream(resource);
   }

   public static Class<?> loadClass(final String className, final ClassLoader loader) throws ClassNotFoundException {
      return loader != null ? loader.loadClass(className) : null;
   }

   public static Class<?> loadSystemClass(final String className) throws ClassNotFoundException {
      try {
         return Class.forName(className, true, ClassLoader.getSystemClassLoader());
      } catch (Throwable t) {
         LOGGER.trace("Couldn't use SystemClassLoader. Trying Class.forName({}).", className, t);
         return Class.forName(className);
      }
   }

   public static <T> T newInstanceOf(final String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
      return newInstanceOf((Class<T>)loadClass(className));
   }

   public static <T> T newCheckedInstanceOf(final String className, final Class<T> clazz) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
      return newInstanceOf((Class<T>)loadClass(className).asSubclass(clazz));
   }

   public static <T> T newInstanceOf(final Class<T> clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      try {
         return clazz.getConstructor().newInstance();
      } catch (NoSuchMethodException ignored) {
         return clazz.newInstance();
      }
   }

   public static boolean isClassAvailable(final String className) {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

      try {
         Thread.currentThread().setContextClassLoader(getClassLoader());
         return LoaderUtil.isClassAvailable(className);
      } finally {
         Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
   }

   public static boolean isJansiAvailable() {
      return isClassAvailable("org.fusesource.jansi.AnsiRenderer");
   }

   public static Class<?> loadClass(final String className) throws ClassNotFoundException {
      if (ignoreTccl) {
         return Class.forName(className);
      }

      try {
         return getClassLoader().loadClass(className);
      } catch (Throwable ignored) {
         return Class.forName(className);
      }
   }
}
