package org.apache.log4j.builders.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.RewritePolicyAdapter;
import org.apache.log4j.bridge.RewritePolicyWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.builders.BuilderManager;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.rewrite.RewriteAppender")
public class RewriteAppenderBuilder extends AbstractBuilder implements AppenderBuilder {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String REWRITE_POLICY_TAG = "rewritePolicy";

   public RewriteAppenderBuilder() {
   }

   public RewriteAppenderBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   @Override
   public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
      String name = this.getNameAttribute(appenderElement);
      AtomicReference<List<String>> appenderRefs = new AtomicReference<>(new ArrayList<>());
      AtomicReference<RewritePolicy> rewritePolicyHolder = new AtomicReference<>();
      AtomicReference<String> level = new AtomicReference<>();
      AtomicReference<Filter> filter = new AtomicReference<>();
      XmlConfiguration.forEachElement(appenderElement.getChildNodes(), currentElement -> {
         switch (currentElement.getTagName()) {
            case "appender-ref":
               Appender appender = config.findAppenderByReference(currentElement);
               if (appender != null) {
                  appenderRefs.get().add(appender.getName());
               }
               break;
            case "rewritePolicy":
               RewritePolicy policy = config.parseRewritePolicy(currentElement);
               if (policy != null) {
                  rewritePolicyHolder.set(policy);
               }
               break;
            case "filter":
               config.addFilter(filter, currentElement);
               break;
            case "param":
               if (this.getNameAttributeKey(currentElement).equalsIgnoreCase("Threshold")) {
                  this.set("Threshold", currentElement, level);
               }
         }
      });
      return this.createAppender(name, level.get(), appenderRefs.get().toArray(Strings.EMPTY_ARRAY), rewritePolicyHolder.get(), filter.get(), config);
   }

   @Override
   public Appender parseAppender(
      final String name,
      final String appenderPrefix,
      final String layoutPrefix,
      final String filterPrefix,
      final Properties props,
      final PropertiesConfiguration configuration
   ) {
      String appenderRef = this.getProperty("appender-ref");
      Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
      String policyPrefix = appenderPrefix + ".rewritePolicy";
      String className = this.getProperty(policyPrefix);
      RewritePolicy policy = configuration.getBuilderManager().parse(className, policyPrefix, props, configuration, BuilderManager.INVALID_REWRITE_POLICY);
      String level = this.getProperty("Threshold");
      if (appenderRef == null) {
         LOGGER.error("No appender references configured for RewriteAppender {}", name);
         return null;
      } else {
         Appender appender = configuration.parseAppender(props, appenderRef);
         if (appender == null) {
            LOGGER.error("Cannot locate Appender {}", appenderRef);
            return null;
         } else {
            return this.createAppender(name, level, new String[]{appenderRef}, policy, filter, configuration);
         }
      }
   }

   private <T extends Log4j1Configuration> Appender createAppender(
      final String name, final String level, final String[] appenderRefs, final RewritePolicy policy, final Filter filter, final T configuration
   ) {
      if (appenderRefs.length == 0) {
         LOGGER.error("No appender references configured for RewriteAppender {}", name);
         return null;
      }

      Level logLevel = OptionConverter.convertLevel(level, Level.TRACE);
      AppenderRef[] refs = new AppenderRef[appenderRefs.length];
      int index = 0;

      for (String appenderRef : appenderRefs) {
         refs[index++] = AppenderRef.createAppenderRef(appenderRef, logLevel, null);
      }

      org.apache.logging.log4j.core.Filter rewriteFilter = buildFilters(level, filter);
      org.apache.logging.log4j.core.appender.rewrite.RewritePolicy rewritePolicy;
      if (policy instanceof RewritePolicyWrapper) {
         rewritePolicy = ((RewritePolicyWrapper)policy).getPolicy();
      } else {
         rewritePolicy = new RewritePolicyAdapter(policy);
      }

      return AppenderWrapper.adapt(RewriteAppender.createAppender(name, true, refs, configuration, rewritePolicy, rewriteFilter));
   }
}
