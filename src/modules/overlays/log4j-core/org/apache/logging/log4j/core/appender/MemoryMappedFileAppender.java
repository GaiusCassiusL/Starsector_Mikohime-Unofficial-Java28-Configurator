package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("MemoryMappedFile")
public final class MemoryMappedFileAppender extends AbstractOutputStreamAppender<MemoryMappedFileManager> {
   private static final int BIT_POSITION_1GB = 30;
   private static final int MAX_REGION_LENGTH = 1073741824;
   private static final int MIN_REGION_LENGTH = 256;
   private final String fileName;
   private Object advertisement;
   private final Advertiser advertiser;

   private MemoryMappedFileAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final MemoryMappedFileManager manager,
      final String filename,
      final boolean ignoreExceptions,
      final boolean immediateFlush,
      final Advertiser advertiser,
      final Property[] properties
   ) {
      super(name, layout, filter, ignoreExceptions, immediateFlush, properties, manager);
      if (advertiser != null) {
         Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
         configuration.putAll(manager.getContentFormat());
         configuration.put("contentType", layout.getContentType());
         configuration.put("name", name);
         this.advertisement = advertiser.advertise(configuration);
      }

      this.fileName = filename;
      this.advertiser = advertiser;
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      super.stop(timeout, timeUnit, false);
      if (this.advertiser != null) {
         this.advertiser.unadvertise(this.advertisement);
      }

      this.setStopped();
      return true;
   }

   public String getFileName() {
      return this.fileName;
   }

   public int getRegionLength() {
      return this.getManager().getRegionLength();
   }

   @PluginFactory
   public static <B extends MemoryMappedFileAppender.Builder<B>> B newBuilder() {
      return new MemoryMappedFileAppender.Builder<B>().asBuilder();
   }

   private static int determineValidRegionLength(final String name, final int regionLength) {
      if (regionLength > 1073741824) {
         LOGGER.info("MemoryMappedAppender[{}] Reduced region length from {} to max length: {}", name, regionLength, 1073741824);
         return 1073741824;
      }

      if (regionLength < 256) {
         LOGGER.info("MemoryMappedAppender[{}] Expanded region length from {} to min length: {}", name, regionLength, 256);
         return 256;
      }

      int result = Integers.ceilingNextPowerOfTwo(regionLength);
      if (regionLength != result) {
         LOGGER.info("MemoryMappedAppender[{}] Rounded up region length from {} to next power of two: {}", name, regionLength, result);
      }

      return result;
   }

   public static class Builder<B extends MemoryMappedFileAppender.Builder<B>>
      extends AbstractOutputStreamAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<MemoryMappedFileAppender> {
      @PluginBuilderAttribute("fileName")
      private String fileName;
      @PluginBuilderAttribute("append")
      private boolean append = true;
      @PluginBuilderAttribute("regionLength")
      private int regionLength = 33554432;
      @PluginBuilderAttribute("advertise")
      private boolean advertise;
      @PluginBuilderAttribute("advertiseURI")
      private String advertiseURI;

      public MemoryMappedFileAppender build() {
         String name = this.getName();
         int actualRegionLength = MemoryMappedFileAppender.determineValidRegionLength(name, this.regionLength);
         if (name == null) {
            MemoryMappedFileAppender.LOGGER.error("No name provided for MemoryMappedFileAppender");
            return null;
         } else if (this.fileName == null) {
            MemoryMappedFileAppender.LOGGER.error("No filename provided for MemoryMappedFileAppender with name " + name);
            return null;
         } else {
            Layout layout = this.getOrCreateLayout();
            MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(
               this.fileName, this.append, this.isImmediateFlush(), actualRegionLength, this.advertiseURI, layout
            );
            return manager == null
               ? null
               : new MemoryMappedFileAppender(
                  name,
                  layout,
                  this.getFilter(),
                  manager,
                  this.fileName,
                  this.isIgnoreExceptions(),
                  false,
                  this.advertise ? this.getConfiguration().getAdvertiser() : null,
                  this.getPropertyArray()
               );
         }
      }

      public B setFileName(final String fileName) {
         this.fileName = fileName;
         return this.asBuilder();
      }

      public B setAppend(final boolean append) {
         this.append = append;
         return this.asBuilder();
      }

      public B setRegionLength(final int regionLength) {
         this.regionLength = regionLength;
         return this.asBuilder();
      }

      public B setAdvertise(final boolean advertise) {
         this.advertise = advertise;
         return this.asBuilder();
      }

      public B setAdvertiseURI(final String advertiseURI) {
         this.advertiseURI = advertiseURI;
         return this.asBuilder();
      }
   }
}
