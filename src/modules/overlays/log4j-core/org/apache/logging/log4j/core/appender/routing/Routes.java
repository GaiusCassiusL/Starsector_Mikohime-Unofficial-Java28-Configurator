package org.apache.logging.log4j.core.appender.routing;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.script.Script;
import org.apache.logging.log4j.core.script.ScriptBindings;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin
public final class Routes {
   private static final String LOG_EVENT_KEY = "logEvent";
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Configuration configuration;
   private final String pattern;
   private final Script patternScript;
   private final Route[] routes;

   @PluginFactory
   public static Routes.Builder newBuilder() {
      return new Routes.Builder();
   }

   private Routes(final Configuration configuration, final Script patternScript, final String pattern, final Route... routes) {
      this.configuration = configuration;
      this.patternScript = patternScript;
      this.pattern = pattern;
      this.routes = routes;
   }

   public String getPattern(final LogEvent event, final ConcurrentMap<Object, Object> scriptStaticVariables) {
      if (this.patternScript != null) {
         ScriptManager scriptManager = this.configuration.getScriptManager();
         ScriptBindings bindings = scriptManager.createBindings(this.patternScript);
         bindings.put("staticVariables", scriptStaticVariables);
         bindings.put("logEvent", event);
         Object object = scriptManager.execute(this.patternScript.getName(), bindings);
         bindings.remove("logEvent");
         return Objects.toString(object, null);
      } else {
         return this.pattern;
      }
   }

   public Script getPatternScript() {
      return this.patternScript;
   }

   public Route getRoute(final String key) {
      for (Route route : this.routes) {
         if (Objects.equals(route.getKey(), key)) {
            return route;
         }
      }

      return null;
   }

   public Route[] getRoutes() {
      return this.routes;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("{");
      boolean first = true;

      for (Route route : this.routes) {
         if (!first) {
            sb.append(',');
         }

         first = false;
         sb.append(route.toString());
      }

      sb.append('}');
      return sb.toString();
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<Routes> {
      @Inject
      private Configuration configuration;
      @PluginAttribute
      private String pattern;
      @PluginElement("Script")
      private Script patternScript;
      @PluginElement
      @Required
      private Route[] routes;

      public Routes build() {
         if (this.routes != null && this.routes.length != 0) {
            if (this.patternScript != null && this.pattern != null || this.patternScript == null && this.pattern == null) {
               Routes.LOGGER.warn("In a Routes element, you must configure either a Script element or a pattern attribute.");
            }

            if (this.patternScript != null) {
               if (this.configuration == null) {
                  Routes.LOGGER.error("No Configuration defined for Routes; required for Script");
               } else {
                  ScriptManager scriptManager = this.configuration.getScriptManager();
                  if (scriptManager == null) {
                     Routes.LOGGER.error("Script support is not enabled");
                     return null;
                  }

                  if (!scriptManager.addScript(this.patternScript)
                     && !scriptManager.isScriptRef(this.patternScript)
                     && !scriptManager.addScript(this.patternScript)) {
                     return null;
                  }
               }
            }

            return new Routes(this.configuration, this.patternScript, this.pattern, this.routes);
         } else {
            Routes.LOGGER.error("No Routes configured.");
            return null;
         }
      }

      public Configuration getConfiguration() {
         return this.configuration;
      }

      public String getPattern() {
         return this.pattern;
      }

      public Script getPatternScript() {
         return this.patternScript;
      }

      public Route[] getRoutes() {
         return this.routes;
      }

      public Routes.Builder setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this;
      }

      public Routes.Builder setPattern(final String pattern) {
         this.pattern = pattern;
         return this;
      }

      public Routes.Builder setPatternScript(final Script patternScript) {
         this.patternScript = patternScript;
         return this;
      }

      public Routes.Builder setRoutes(final Route... routes) {
         this.routes = routes;
         return this;
      }
   }
}
