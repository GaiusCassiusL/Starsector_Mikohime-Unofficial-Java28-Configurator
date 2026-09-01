package org.apache.logging.log4j.core.appender;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.FileUtils;

public class FileManager extends OutputStreamManager {
   private static final FileManager.FileManagerFactory FACTORY = new FileManager.FileManagerFactory();
   private final boolean isAppend;
   private final boolean createOnDemand;
   private final boolean isLocking;
   private final String advertiseURI;
   private final int bufferSize;
   private final Set<PosixFilePermission> filePermissions;
   private final String fileOwner;
   private final String fileGroup;
   private final boolean attributeViewEnabled;

   protected FileManager(
      final LoggerContext loggerContext,
      final String fileName,
      final OutputStream os,
      final boolean append,
      final boolean locking,
      final boolean createOnDemand,
      final String advertiseURI,
      final Layout layout,
      final String filePermissions,
      final String fileOwner,
      final String fileGroup,
      final boolean writeHeader,
      final ByteBuffer buffer
   ) {
      super(loggerContext, os, fileName, createOnDemand, layout, writeHeader, buffer);
      this.isAppend = append;
      this.createOnDemand = createOnDemand;
      this.isLocking = locking;
      this.advertiseURI = advertiseURI;
      this.bufferSize = buffer.capacity();
      Set<String> views = FileSystems.getDefault().supportedFileAttributeViews();
      if (views.contains("posix")) {
         this.filePermissions = filePermissions != null ? PosixFilePermissions.fromString(filePermissions) : null;
         this.fileGroup = fileGroup;
      } else {
         this.filePermissions = null;
         this.fileGroup = null;
         if (filePermissions != null) {
            LOGGER.warn("Posix file attribute permissions defined but it is not supported by this files system.");
         }

         if (fileGroup != null) {
            LOGGER.warn("Posix file attribute group defined but it is not supported by this files system.");
         }
      }

      if (views.contains("owner")) {
         this.fileOwner = fileOwner;
      } else {
         this.fileOwner = null;
         if (fileOwner != null) {
            LOGGER.warn("Owner file attribute defined but it is not supported by this files system.");
         }
      }

      this.attributeViewEnabled = this.filePermissions != null || this.fileOwner != null || this.fileGroup != null;
   }

   public static FileManager getFileManager(
      final String fileName,
      final boolean append,
      boolean locking,
      final boolean bufferedIo,
      final boolean createOnDemand,
      final String advertiseUri,
      final Layout layout,
      final int bufferSize,
      final String filePermissions,
      final String fileOwner,
      final String fileGroup,
      final Configuration configuration
   ) {
      if (locking && bufferedIo) {
         locking = false;
      }

      return narrow(
         FileManager.class,
         getManager(
            fileName,
            new FileManager.FactoryData(
               append, locking, bufferedIo, bufferSize, createOnDemand, advertiseUri, layout, filePermissions, fileOwner, fileGroup, configuration
            ),
            FACTORY
         )
      );
   }

   @Override
   protected OutputStream createOutputStream() throws IOException {
      String filename = this.getFileName();
      LOGGER.debug("Now writing to {} at {}", filename, new Date());
      File file = new File(filename);
      this.createParentDir(file);
      FileOutputStream fos = new FileOutputStream(file, this.isAppend);
      if (file.exists() && file.length() == 0L) {
         try {
            FileTime now = FileTime.fromMillis(System.currentTimeMillis());
            Files.setAttribute(file.toPath(), "creationTime", now);
         } catch (Exception ex) {
            LOGGER.warn("Unable to set current file time for {}", filename);
         }

         this.writeHeader(fos);
      }

      this.defineAttributeView(Paths.get(filename));
      return fos;
   }

   protected void createParentDir(final File file) {
   }

   protected void defineAttributeView(final Path path) {
      if (this.attributeViewEnabled) {
         try {
            path.toFile().createNewFile();
            FileUtils.defineFilePosixAttributeView(path, this.filePermissions, this.fileOwner, this.fileGroup);
         } catch (Exception e) {
            LOGGER.error("Could not define attribute view on path \"{}\" got {}", path, e.getMessage(), e);
         }
      }
   }

   @Override
   protected synchronized void write(final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
      if (this.isLocking) {
         try {
            FileChannel channel = ((FileOutputStream)this.getOutputStream()).getChannel();

            try (FileLock lock = channel.lock(0L, Long.MAX_VALUE, false)) {
               super.write(bytes, offset, length, immediateFlush);
            }
         } catch (IOException ex) {
            throw new AppenderLoggingException("Unable to obtain lock on " + this.getName(), ex);
         }
      } else {
         super.write(bytes, offset, length, immediateFlush);
      }
   }

   @Override
   protected synchronized void writeToDestination(final byte[] bytes, final int offset, final int length) {
      if (this.isLocking) {
         try {
            FileChannel channel = ((FileOutputStream)this.getOutputStream()).getChannel();

            try (FileLock lock = channel.lock(0L, Long.MAX_VALUE, false)) {
               super.writeToDestination(bytes, offset, length);
            }
         } catch (IOException ex) {
            throw new AppenderLoggingException("Unable to obtain lock on " + this.getName(), ex);
         }
      } else {
         super.writeToDestination(bytes, offset, length);
      }
   }

   public String getFileName() {
      return this.getName();
   }

   public boolean isAppend() {
      return this.isAppend;
   }

   public boolean isCreateOnDemand() {
      return this.createOnDemand;
   }

   public boolean isLocking() {
      return this.isLocking;
   }

   public int getBufferSize() {
      return this.bufferSize;
   }

   public Set<PosixFilePermission> getFilePermissions() {
      return this.filePermissions;
   }

   public String getFileOwner() {
      return this.fileOwner;
   }

   public String getFileGroup() {
      return this.fileGroup;
   }

   public boolean isAttributeViewEnabled() {
      return this.attributeViewEnabled;
   }

   @Override
   public Map<String, String> getContentFormat() {
      Map<String, String> result = new HashMap<>(super.getContentFormat());
      result.put("fileURI", this.advertiseURI);
      return result;
   }

   private static class FactoryData extends ConfigurationFactoryData {
      private final boolean append;
      private final boolean locking;
      private final boolean bufferedIo;
      private final int bufferSize;
      private final boolean createOnDemand;
      private final String advertiseURI;
      private final Layout layout;
      private final String filePermissions;
      private final String fileOwner;
      private final String fileGroup;

      public FactoryData(
         final boolean append,
         final boolean locking,
         final boolean bufferedIo,
         final int bufferSize,
         final boolean createOnDemand,
         final String advertiseURI,
         final Layout layout,
         final String filePermissions,
         final String fileOwner,
         final String fileGroup,
         final Configuration configuration
      ) {
         super(configuration);
         this.append = append;
         this.locking = locking;
         this.bufferedIo = bufferedIo;
         this.bufferSize = bufferSize;
         this.createOnDemand = createOnDemand;
         this.advertiseURI = advertiseURI;
         this.layout = layout;
         this.filePermissions = filePermissions;
         this.fileOwner = fileOwner;
         this.fileGroup = fileGroup;
      }
   }

   private static class FileManagerFactory implements ManagerFactory<FileManager, FileManager.FactoryData> {
      public FileManager createManager(final String name, final FileManager.FactoryData data) {
         File file = new File(name);

         try {
            FileUtils.makeParentDirs(file);
            int actualSize = data.bufferedIo ? data.bufferSize : Constants.ENCODER_BYTE_BUFFER_SIZE;
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[actualSize]);
            FileOutputStream fos = data.createOnDemand ? null : new FileOutputStream(file, data.append);
            boolean writeHeader = file.exists() && file.length() == 0L;
            FileManager fm = new FileManager(
               data.getLoggerContext(),
               name,
               fos,
               data.append,
               data.locking,
               data.createOnDemand,
               data.advertiseURI,
               data.layout,
               data.filePermissions,
               data.fileOwner,
               data.fileGroup,
               writeHeader,
               byteBuffer
            );
            if (fos != null && fm.attributeViewEnabled) {
               fm.defineAttributeView(file.toPath());
            }

            return fm;
         } catch (IOException ex) {
            AbstractManager.LOGGER.error("FileManager (" + name + ") " + ex, ex);
            return null;
         }
      }
   }
}
