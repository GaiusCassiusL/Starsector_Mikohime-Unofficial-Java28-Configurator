package org.apache.log4j.or;

import java.util.Hashtable;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.RendererSupport;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;

public class RendererMap {
   Hashtable map = new Hashtable();
   static ObjectRenderer defaultRenderer = new DefaultRenderer();

   public static void addRenderer(final RendererSupport repository, final String renderedClassName, final String renderingClassName) {
      StatusLogger.getLogger().debug("Rendering class: [" + renderingClassName + "], Rendered class: [" + renderedClassName + "].");
      ObjectRenderer renderer = (ObjectRenderer)OptionConverter.instantiateByClassName(renderingClassName, ObjectRenderer.class, null);
      if (renderer == null) {
         StatusLogger.getLogger().error("Could not instantiate renderer [" + renderingClassName + "].");
      } else {
         try {
            Class renderedClass = Loader.loadClass(renderedClassName);
            repository.setRenderer(renderedClass, renderer);
         } catch (ClassNotFoundException e) {
            StatusLogger.getLogger().error("Could not find class [" + renderedClassName + "].", e);
         }
      }
   }

   public String findAndRender(final Object o) {
      return o == null ? null : this.get(o.getClass()).doRender(o);
   }

   public ObjectRenderer get(final Object o) {
      return o == null ? null : this.get(o.getClass());
   }

   public ObjectRenderer get(final Class clazz) {
      ObjectRenderer r = null;

      for (Class c = clazz; c != null; c = c.getSuperclass()) {
         r = (ObjectRenderer)this.map.get(c);
         if (r != null) {
            return r;
         }

         r = this.searchInterfaces(c);
         if (r != null) {
            return r;
         }
      }

      return defaultRenderer;
   }

   ObjectRenderer searchInterfaces(Class c) {
      ObjectRenderer r = (ObjectRenderer)this.map.get(c);
      if (r != null) {
         return r;
      }

      Class[] ia = c.getInterfaces();

      for (int i = 0; i < ia.length; i++) {
         r = this.searchInterfaces(ia[i]);
         if (r != null) {
            return r;
         }
      }

      return null;
   }

   public ObjectRenderer getDefaultRenderer() {
      return defaultRenderer;
   }

   public void clear() {
      this.map.clear();
   }

   public void put(final Class clazz, final ObjectRenderer or) {
      this.map.put(clazz, or);
   }
}
