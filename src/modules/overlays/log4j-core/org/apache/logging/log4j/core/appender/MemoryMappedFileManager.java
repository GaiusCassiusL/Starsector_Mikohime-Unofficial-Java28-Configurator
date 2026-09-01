package org.apache.logging.log4j.core.appender;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.util.ReflectionUtil;

public class MemoryMappedFileManager extends OutputStreamManager {
   static final int DEFAULT_REGION_LENGTH = 33554432;
   private static final int MAX_REMAP_COUNT = 10;
   private static final MemoryMappedFileManager.MemoryMappedFileManagerFactory FACTORY = new MemoryMappedFileManager.MemoryMappedFileManagerFactory();
   private static final double NANOS_PER_MILLISEC = 1000000.0;
   private final boolean immediateFlush;
   private final int regionLength;
   private final String advertiseURI;
   private final RandomAccessFile randomAccessFile;
   private MappedByteBuffer mappedBuffer;
   private long mappingOffset;

   protected MemoryMappedFileManager(
      final RandomAccessFile file,
      final String fileName,
      final OutputStream os,
      final boolean immediateFlush,
      final long position,
      final int regionLength,
      final String advertiseURI,
      final Layout layout,
      final boolean writeHeader
   ) throws IOException {
      super(os, fileName, layout, writeHeader, ByteBuffer.wrap(new byte[0]));
      this.immediateFlush = immediateFlush;
      this.randomAccessFile = Objects.requireNonNull(file, "RandomAccessFile");
      this.regionLength = regionLength;
      this.advertiseURI = advertiseURI;
      this.mappedBuffer = mmap(this.randomAccessFile.getChannel(), this.getFileName(), position, regionLength);
      this.byteBuffer = this.mappedBuffer;
      this.mappingOffset = position;
   }

   public static MemoryMappedFileManager getFileManager(
      final String fileName, final boolean append, final boolean immediateFlush, final int regionLength, final String advertiseURI, final Layout layout
   ) {
      return narrow(
         MemoryMappedFileManager.class,
         getManager(fileName, new MemoryMappedFileManager.FactoryData(append, immediateFlush, regionLength, advertiseURI, layout), FACTORY)
      );
   }

   @Deprecated
   public Boolean isEndOfBatch() {
      return Boolean.FALSE;
   }

   @Deprecated
   public void setEndOfBatch(final boolean endOfBatch) {
   }

   @Override
   protected synchronized void write(final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
      int currentOffset = offset;
      int currentLength = length;

      while (currentLength > this.mappedBuffer.remaining()) {
         int chunk = this.mappedBuffer.remaining();
         this.mappedBuffer.put(bytes, currentOffset, chunk);
         currentOffset += chunk;
         currentLength -= chunk;
         this.remap();
      }

      this.mappedBuffer.put(bytes, currentOffset, currentLength);
   }

   private synchronized void remap() {
      long offset = this.mappingOffset + this.mappedBuffer.position();
      int length = this.mappedBuffer.remaining() + this.regionLength;

      try {
         unsafeUnmap(this.mappedBuffer);
         long fileLength = this.randomAccessFile.length() + this.regionLength;
         LOGGER.debug(
            "{} {} extending {} by {} bytes to {}", this.getClass().getSimpleName(), this.getName(), this.getFileName(), this.regionLength, fileLength
         );
         long startNanos = System.nanoTime();
         this.randomAccessFile.setLength(fileLength);
         float millis = (float)((System.nanoTime() - startNanos) / 1000000.0);
         LOGGER.debug("{} {} extended {} OK in {} millis", this.getClass().getSimpleName(), this.getName(), this.getFileName(), millis);
         this.mappedBuffer = mmap(this.randomAccessFile.getChannel(), this.getFileName(), offset, length);
         this.byteBuffer = this.mappedBuffer;
         this.mappingOffset = offset;
      } catch (Exception ex) {
         this.logError("Unable to remap", ex);
      }
   }

   @Override
   public synchronized void flush() {
      this.mappedBuffer.force();
   }

   @Override
   public synchronized boolean closeOutputStream() {
      long position = this.mappedBuffer.position();
      long length = this.mappingOffset + position;

      try {
         unsafeUnmap(this.mappedBuffer);
      } catch (Exception ex) {
         this.logError("Unable to unmap MappedBuffer", ex);
      }

      try {
         LOGGER.debug("MMapAppender closing. Setting {} length to {} (offset {} + position {})", this.getFileName(), length, this.mappingOffset, position);
         this.randomAccessFile.setLength(length);
         this.randomAccessFile.close();
         return true;
      } catch (IOException ex) {
         this.logError("Unable to close MemoryMappedFile", ex);
         return false;
      }
   }

   public static MappedByteBuffer mmap(final FileChannel fileChannel, final String fileName, final long start, final int size) throws IOException {
      int i = 1;

      while (true) {
         try {
            LOGGER.debug("MMapAppender remapping {} start={}, size={}", fileName, start, size);
            long startNanos = System.nanoTime();
            MappedByteBuffer map = fileChannel.map(MapMode.READ_WRITE, start, size);
            map.order(ByteOrder.nativeOrder());
            float millis = (float)((System.nanoTime() - startNanos) / 1000000.0);
            LOGGER.debug("MMapAppender remapped {} OK in {} millis", fileName, millis);
            return map;
         } catch (IOException e) {
            if (e.getMessage() == null || !e.getMessage().endsWith("user-mapped section open")) {
               throw e;
            }

            LOGGER.debug("Remap attempt {}/{} failed. Retrying...", i, 10, e);
            if (i < 10) {
               Thread.yield();
            } else {
               try {
                  Thread.sleep(1L);
               } catch (InterruptedException ignored) {
                  Thread.currentThread().interrupt();
                  throw e;
               }
            }

            i++;
         }
      }
   }

   private static void unsafeUnmap(final MappedByteBuffer mbb) throws PrivilegedActionException {
      LOGGER.debug("MMapAppender unmapping old buffer...");
      long startNanos = System.nanoTime();
      AccessController.doPrivileged(() -> {
         Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
         Object unsafe = ReflectionUtil.getStaticFieldValue(unsafeClass.getDeclaredField("theUnsafe"));
         Method invokeCleaner = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
         invokeCleaner.invoke(unsafe, mbb);
         return null;
      });
      float millis = (float)((System.nanoTime() - startNanos) / 1000000.0);
      LOGGER.debug("MMapAppender unmapped buffer OK in {} millis", millis);
   }

   public String getFileName() {
      return this.getName();
   }

   public int getRegionLength() {
      return this.regionLength;
   }

   public boolean isImmediateFlush() {
      return this.immediateFlush;
   }

   @Override
   public Map<String, String> getContentFormat() {
      Map<String, String> result = new HashMap<>(super.getContentFormat());
      result.put("fileURI", this.advertiseURI);
      return result;
   }

   @Override
   protected void flushBuffer(final ByteBuffer buffer) {
   }

   @Override
   public ByteBuffer getByteBuffer() {
      return this.mappedBuffer;
   }

   @Override
   public ByteBuffer drain(final ByteBuffer buf) {
      this.remap();
      return this.mappedBuffer;
   }

   private static class FactoryData {
      private final boolean append;
      private final boolean immediateFlush;
      private final int regionLength;
      private final String advertiseURI;
      private final Layout layout;

      public FactoryData(final boolean append, final boolean immediateFlush, final int regionLength, final String advertiseURI, final Layout layout) {
         this.append = append;
         this.immediateFlush = immediateFlush;
         this.regionLength = regionLength;
         this.advertiseURI = advertiseURI;
         this.layout = layout;
      }
   }

   private static class MemoryMappedFileManagerFactory implements ManagerFactory<MemoryMappedFileManager, MemoryMappedFileManager.FactoryData> {
      public MemoryMappedFileManager createManager(final String name, final MemoryMappedFileManager.FactoryData data) {
         File file = new File(name);
         if (!data.append) {
            file.delete();
         }

         boolean writeHeader = !data.append || !file.exists();
         OutputStream os = NullOutputStream.getInstance();
         RandomAccessFile raf = null;

         try {
            FileUtils.makeParentDirs(file);
            raf = new RandomAccessFile(name, "rw");
            long position = data.append ? raf.length() : 0L;
            raf.setLength(position + data.regionLength);
            return new MemoryMappedFileManager(raf, name, os, data.immediateFlush, position, data.regionLength, data.advertiseURI, data.layout, writeHeader);
         } catch (Exception ex) {
            AbstractManager.LOGGER.error("MemoryMappedFileManager (" + name + ") " + ex, ex);
            Closer.closeSilently(raf);
            return null;
         }
      }
   }
}
