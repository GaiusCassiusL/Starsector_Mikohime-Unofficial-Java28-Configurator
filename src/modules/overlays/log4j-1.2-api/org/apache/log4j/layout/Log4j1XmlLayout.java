package org.apache.log4j.layout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.util.Transform;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.Strings;

@Configurable(elementType = "layout", printObject = true)
@Plugin
public final class Log4j1XmlLayout extends AbstractStringLayout {
   private static final String EOL = "\r\n";
   private final boolean locationInfo;
   private final boolean properties;

   @PluginFactory
   public static Log4j1XmlLayout createLayout(
      @PluginAttribute("locationInfo") final boolean locationInfo, @PluginAttribute("properties") final boolean properties
   ) {
      return new Log4j1XmlLayout(locationInfo, properties);
   }

   private Log4j1XmlLayout(final boolean locationInfo, final boolean properties) {
      super(StandardCharsets.UTF_8);
      this.locationInfo = locationInfo;
      this.properties = properties;
   }

   public boolean isLocationInfo() {
      return this.locationInfo;
   }

   public boolean isProperties() {
      return this.properties;
   }

   public void encode(final LogEvent event, final ByteBufferDestination destination) {
      StringBuilder text = getStringBuilder();
      this.formatTo(event, text);
      this.getStringBuilderEncoder().encode(text, destination);
   }

   public String toSerializable(final LogEvent event) {
      StringBuilder text = getStringBuilder();
      this.formatTo(event, text);
      return text.toString();
   }

   private void formatTo(final LogEvent event, final StringBuilder buf) {
      buf.append("<log4j:event logger=\"");
      buf.append(Transform.escapeHtmlTags(event.getLoggerName()));
      buf.append("\" timestamp=\"");
      buf.append(event.getTimeMillis());
      buf.append("\" level=\"");
      buf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
      buf.append("\" thread=\"");
      buf.append(Transform.escapeHtmlTags(event.getThreadName()));
      buf.append("\">");
      buf.append("\r\n");
      buf.append("<log4j:message><![CDATA[");
      Transform.appendEscapingCData(buf, event.getMessage().getFormattedMessage());
      buf.append("]]></log4j:message>");
      buf.append("\r\n");
      List<String> ndc = event.getContextStack().asList();
      if (!ndc.isEmpty()) {
         buf.append("<log4j:NDC><![CDATA[");
         Transform.appendEscapingCData(buf, Strings.join(ndc, ' '));
         buf.append("]]></log4j:NDC>");
         buf.append("\r\n");
      }

      Throwable thrown = event.getThrown();
      if (thrown != null) {
         buf.append("<log4j:throwable><![CDATA[");
         StringWriter w = new StringWriter();
         thrown.printStackTrace(new PrintWriter(w));
         Transform.appendEscapingCData(buf, w.toString());
         buf.append("]]></log4j:throwable>");
         buf.append("\r\n");
      }

      if (this.locationInfo) {
         StackTraceElement source = event.getSource();
         if (source != null) {
            buf.append("<log4j:locationInfo class=\"");
            buf.append(Transform.escapeHtmlTags(source.getClassName()));
            buf.append("\" method=\"");
            buf.append(Transform.escapeHtmlTags(source.getMethodName()));
            buf.append("\" file=\"");
            buf.append(Transform.escapeHtmlTags(source.getFileName()));
            buf.append("\" line=\"");
            buf.append(source.getLineNumber());
            buf.append("\"/>");
            buf.append("\r\n");
         }
      }

      if (this.properties) {
         ReadOnlyStringMap contextMap = event.getContextData();
         if (!contextMap.isEmpty()) {
            buf.append("<log4j:properties>\r\n");
            contextMap.forEach((key, val) -> {
               if (val != null) {
                  buf.append("<log4j:data name=\"");
                  buf.append(Transform.escapeHtmlTags(key));
                  buf.append("\" value=\"");
                  buf.append(Transform.escapeHtmlTags(Objects.toString(val, null)));
                  buf.append("\"/>");
                  buf.append("\r\n");
               }
            });
            buf.append("</log4j:properties>");
            buf.append("\r\n");
         }
      }

      buf.append("</log4j:event>");
      buf.append("\r\n");
      buf.append("\r\n");
   }
}
