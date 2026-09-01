package org.apache.logging.log4j.core.util;

import java.util.function.Supplier;

/** @deprecated */
public interface Builder<T> extends Supplier<T> {
   T build();

   @Override
   default T get() {
      return this.build();
   }
}
