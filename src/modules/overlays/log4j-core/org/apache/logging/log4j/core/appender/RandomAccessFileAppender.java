package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("RandomAccessFile")
public final class RandomAccessFileAppender extends AbstractOutputStreamAppender<RandomAccessFileManager> {
   private final String fileName;
   private Object advertisement;
   private final Advertiser advertiser;

   private RandomAccessFileAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final RandomAccessFileManager manager,
      final String filename,
      final boolean ignoreExceptions,
      final boolean immediateFlush,
      final Advertiser advertiser
   ) {
      super(name, layout, filter, ignoreExceptions, immediateFlush, null, manager);
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

   public int getBufferSize() {
      return this.getManager().getBufferSize();
   }

   @PluginFactory
   public static <B extends RandomAccessFileAppender.Builder<B>> B newBuilder() {
      return new RandomAccessFileAppender.Builder<B>().asBuilder();
   }

   public static class Builder<B extends RandomAccessFileAppender.Builder<B>>
      extends AbstractOutputStreamAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<RandomAccessFileAppender> {
      @PluginBuilderAttribute("fileName")
      private String fileName;
      @PluginBuilderAttribute("append")
      private boolean append = true;
      @PluginBuilderAttribute("advertise")
      private boolean advertise;
      @PluginBuilderAttribute("advertiseURI")
      private String advertiseURI;

      public Builder() {
         this.setBufferSize(262144);
      }

      public RandomAccessFileAppender build() {
         String name = this.getName();
         if (name == null) {
            RandomAccessFileAppender.LOGGER.error("No name provided for RandomAccessFileAppender");
            return null;
         } else if (this.fileName == null) {
            RandomAccessFileAppender.LOGGER.error("No filename provided for RandomAccessFileAppender with name {}", name);
            return null;
         } else {
            Layout layout = this.getOrCreateLayout();
            boolean immediateFlush = this.isImmediateFlush();
            RandomAccessFileManager manager = RandomAccessFileManager.getFileManager(
               this.fileName, this.append, immediateFlush, this.getBufferSize(), this.advertiseURI, layout, null
            );
            return manager == null
               ? null
               : new RandomAccessFileAppender(
                  name,
                  layout,
                  this.getFilter(),
                  manager,
                  this.fileName,
                  this.isIgnoreExceptions(),
                  immediateFlush,
                  this.advertise ? this.getConfiguration().getAdvertiser() : null
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
