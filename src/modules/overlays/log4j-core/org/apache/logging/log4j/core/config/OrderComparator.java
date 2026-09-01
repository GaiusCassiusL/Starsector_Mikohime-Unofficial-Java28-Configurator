package org.apache.logging.log4j.core.config;

import java.util.Comparator;
import java.util.Objects;

public class OrderComparator implements Comparator<Class<?>> {
   private static final Comparator<Class<?>> INSTANCE = new OrderComparator();

   public static Comparator<Class<?>> getInstance() {
      return INSTANCE;
   }

   public int compare(final Class<?> lhs, final Class<?> rhs) {
      Order lhsOrder = Objects.requireNonNull(lhs, "lhs").getAnnotation(Order.class);
      Order rhsOrder = Objects.requireNonNull(rhs, "rhs").getAnnotation(Order.class);
      if (lhsOrder == null && rhsOrder == null) {
         return 0;
      } else if (rhsOrder == null) {
         return -1;
      } else {
         return lhsOrder == null ? 1 : Integer.signum(rhsOrder.value() - lhsOrder.value());
      }
   }
}
