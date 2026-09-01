package org.apache.logging.log4j.core.lookup;

import java.util.Locale;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.Strings;

@Lookup
@Plugin("java")
public class JavaLookup extends AbstractLookup {
   private final SystemPropertiesLookup spLookup = new SystemPropertiesLookup();

   public String getHardware() {
      return "processors: "
         + Runtime.getRuntime().availableProcessors()
         + ", architecture: "
         + this.getSystemProperty("os.arch")
         + this.getSystemProperty("-", "sun.arch.data.model")
         + this.getSystemProperty(", instruction sets: ", "sun.cpu.isalist");
   }

   public String getLocale() {
      return "default locale: " + Locale.getDefault() + ", platform encoding: " + this.getSystemProperty("file.encoding");
   }

   public String getOperatingSystem() {
      return this.getSystemProperty("os.name")
         + " "
         + this.getSystemProperty("os.version")
         + this.getSystemProperty(" ", "sun.os.patch.level")
         + ", architecture: "
         + this.getSystemProperty("os.arch")
         + this.getSystemProperty("-", "sun.arch.data.model");
   }

   public String getRuntime() {
      return this.getSystemProperty("java.runtime.name")
         + " (build "
         + this.getSystemProperty("java.runtime.version")
         + ") from "
         + this.getSystemProperty("java.vendor");
   }

   private String getSystemProperty(final String name) {
      return this.spLookup.lookup(name);
   }

   private String getSystemProperty(final String prefix, final String name) {
      String value = this.getSystemProperty(name);
      return Strings.isEmpty(value) ? "" : prefix + value;
   }

   public String getVirtualMachine() {
      return this.getSystemProperty("java.vm.name")
         + " (build "
         + this.getSystemProperty("java.vm.version")
         + ", "
         + this.getSystemProperty("java.vm.info")
         + ")";
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      switch (key) {
         case "version":
            return "Java version " + this.getSystemProperty("java.version");
         case "runtime":
            return this.getRuntime();
         case "vm":
            return this.getVirtualMachine();
         case "os":
            return this.getOperatingSystem();
         case "hw":
            return this.getHardware();
         case "locale":
            return this.getLocale();
         default:
            throw new IllegalArgumentException(key);
      }
   }
}
