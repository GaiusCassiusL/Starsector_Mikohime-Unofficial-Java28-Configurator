package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.InjectorCallback;
import org.apache.logging.log4j.util.PropertiesUtil;

public class DefaultCallback implements InjectorCallback {
   public void configure(final Injector injector) {
      injector.setReflectionAccessor(object -> object.setAccessible(true));
      injector.registerBundle(new DefaultBundle(injector, PropertiesUtil.getProperties(), Loader.getClassLoader()));
   }

   @Override
   public String toString() {
      return this.getClass().getName();
   }
}
