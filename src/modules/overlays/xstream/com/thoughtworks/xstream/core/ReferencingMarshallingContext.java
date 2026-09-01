package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.path.Path;

public interface ReferencingMarshallingContext extends MarshallingContext {
   /** @deprecated */
   Path currentPath();

   Object lookupReference(Object var1);

   void replace(Object var1, Object var2);

   void registerImplicit(Object var1);
}
