package org.apache.logging.log4j.core.layout.internal;

public interface ListChecker {
   ListChecker.NoopChecker NOOP_CHECKER = new ListChecker.NoopChecker();

   boolean check(final String key);

   class NoopChecker implements ListChecker {
      @Override
      public boolean check(final String key) {
         return true;
      }

      @Override
      public String toString() {
         return "";
      }
   }
}
