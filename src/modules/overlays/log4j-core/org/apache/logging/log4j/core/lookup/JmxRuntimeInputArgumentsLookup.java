package org.apache.logging.log4j.core.lookup;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.Lazy;

@Lookup
@Plugin("jvmrunargs")
public class JmxRuntimeInputArgumentsLookup extends MapLookup {
   private static final Lazy<JmxRuntimeInputArgumentsLookup> INSTANCE = Lazy.lazy(() -> {
      List<String> argsList = ManagementFactory.getRuntimeMXBean().getInputArguments();
      return new JmxRuntimeInputArgumentsLookup(MapLookup.toMap(argsList));
   });

   @PluginFactory
   public static JmxRuntimeInputArgumentsLookup getInstance() {
      return (JmxRuntimeInputArgumentsLookup)INSTANCE.value();
   }

   public JmxRuntimeInputArgumentsLookup() {
   }

   public JmxRuntimeInputArgumentsLookup(final Map<String, String> map) {
      super(map);
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      return this.lookup(key);
   }

   @Override
   public String lookup(final String key) {
      if (key == null) {
         return null;
      }

      Map<String, String> map = this.getMap();
      return map == null ? null : map.get(key);
   }
}
