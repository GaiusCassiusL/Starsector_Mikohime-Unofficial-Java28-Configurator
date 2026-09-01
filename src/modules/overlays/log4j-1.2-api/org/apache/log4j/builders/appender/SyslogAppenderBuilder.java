package org.apache.log4j.builders.appender;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.LayoutAdapter;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.layout.Log4j1SyslogLayout;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.appender.SyslogAppender.Builder;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.net.SyslogAppender")
public class SyslogAppenderBuilder extends AbstractBuilder implements AppenderBuilder {
   private static final String DEFAULT_HOST = "localhost";
   private static int DEFAULT_PORT = 514;
   private static final String DEFAULT_FACILITY = "LOCAL0";
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String FACILITY_PARAM = "Facility";
   private static final String FACILITY_PRINTING_PARAM = "FacilityPrinting";
   private static final String HEADER_PARAM = "Header";
   private static final String PROTOCOL_PARAM = "Protocol";
   private static final String SYSLOG_HOST_PARAM = "SyslogHost";

   public SyslogAppenderBuilder() {
   }

   public SyslogAppenderBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   @Override
   public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
      String name = this.getNameAttribute(appenderElement);
      AtomicReference<Layout> layout = new AtomicReference<>();
      AtomicReference<Filter> filter = new AtomicReference<>();
      AtomicReference<String> facility = new AtomicReference<>();
      AtomicReference<String> level = new AtomicReference<>();
      AtomicReference<String> host = new AtomicReference<>();
      AtomicReference<Protocol> protocol = new AtomicReference<>(Protocol.TCP);
      AtomicBoolean header = new AtomicBoolean(false);
      AtomicBoolean facilityPrinting = new AtomicBoolean(false);
      XmlConfiguration.forEachElement(appenderElement.getChildNodes(), currentElement -> {
         switch (currentElement.getTagName()) {
            case "layout":
               layout.set(config.parseLayout(currentElement));
               break;
            case "filter":
               config.addFilter(filter, currentElement);
               break;
            case "param":
               switch (this.getNameAttributeKey(currentElement)) {
                  case "Facility":
                     this.set("Facility", currentElement, facility);
                     break;
                  case "FacilityPrinting":
                     this.set("FacilityPrinting", currentElement, facilityPrinting);
                     break;
                  case "Header":
                     this.set("Header", currentElement, header);
                     break;
                  case "Protocol":
                     protocol.set(Protocol.valueOf(this.getValueAttribute(currentElement, Protocol.TCP.name())));
                     break;
                  case "SyslogHost":
                     this.set("SyslogHost", currentElement, host);
                     break;
                  case "Threshold":
                     this.set("Threshold", currentElement, level);
               }
         }
      });
      return this.createAppender(
         name, config, layout.get(), facility.get(), filter.get(), host.get(), level.get(), protocol.get(), header.get(), facilityPrinting.get()
      );
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
      Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
      Layout layout = configuration.parseLayout(layoutPrefix, name, props);
      String level = this.getProperty("Threshold");
      String facility = this.getProperty("Facility", "LOCAL0");
      boolean facilityPrinting = this.getBooleanProperty("FacilityPrinting", false);
      boolean header = this.getBooleanProperty("Header", false);
      String protocol = this.getProperty("Protocol", Protocol.TCP.name());
      String syslogHost = this.getProperty("SyslogHost", "localhost:" + DEFAULT_PORT);
      return this.createAppender(name, configuration, layout, facility, filter, syslogHost, level, Protocol.valueOf(protocol), header, facilityPrinting);
   }

   private Appender createAppender(
      final String name,
      final Log4j1Configuration configuration,
      final Layout layout,
      final String facility,
      final Filter filter,
      final String syslogHost,
      final String level,
      final Protocol protocol,
      final boolean header,
      final boolean facilityPrinting
   ) {
      AtomicReference<String> host = new AtomicReference<>();
      AtomicInteger port = new AtomicInteger();
      this.resolveSyslogHost(syslogHost, host, port);
      org.apache.logging.log4j.core.Layout messageLayout = LayoutAdapter.adapt(layout);
      Log4j1SyslogLayout appenderLayout = Log4j1SyslogLayout.newBuilder()
         .setHeader(header)
         .setFacility(Facility.toFacility(facility))
         .setFacilityPrinting(facilityPrinting)
         .setMessageLayout(messageLayout)
         .build();
      org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
      return AppenderWrapper.adapt(
         ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)SyslogAppender.newSyslogAppenderBuilder().setName(name))
                              .setConfiguration(configuration))
                           .setLayout(appenderLayout))
                        .setFilter(fileFilter))
                     .setPort(port.get()))
                  .setProtocol(protocol))
               .setHost(host.get()))
            .build()
      );
   }

   private void resolveSyslogHost(final String syslogHost, final AtomicReference<String> host, final AtomicInteger port) {
      String[] parts = syslogHost != null ? syslogHost.split(":") : Strings.EMPTY_ARRAY;
      if (parts.length == 1) {
         host.set(parts[0]);
         port.set(DEFAULT_PORT);
      } else if (parts.length == 2) {
         host.set(parts[0]);
         port.set(Integer.parseInt(parts[1].trim()));
      } else {
         LOGGER.warn("Invalid {} setting: {}. Using default.", "SyslogHost", syslogHost);
         host.set("localhost");
         port.set(DEFAULT_PORT);
      }
   }
}
