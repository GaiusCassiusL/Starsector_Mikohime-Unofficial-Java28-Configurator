package org.apache.logging.log4j.core;

import java.util.List;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

public interface ContextDataInjector {
   Key<ContextDataInjector> KEY = new Key<ContextDataInjector>() {};

   StringMap injectContextData(final List<Property> properties, final StringMap reusable);

   ReadOnlyStringMap rawContextData();
}
