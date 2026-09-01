package org.apache.logging.log4j.core.layout;

import java.nio.ByteBuffer;

public final class ByteBufferDestinationHelper {
   private ByteBufferDestinationHelper() {
   }

   public static void writeToUnsynchronized(final ByteBuffer source, final ByteBufferDestination destination) {
      ByteBuffer destBuff;
      for (destBuff = destination.getByteBuffer(); source.remaining() > destBuff.remaining(); destBuff = destination.drain(destBuff)) {
         int originalLimit = source.limit();
         source.limit(Math.min(source.limit(), source.position() + destBuff.remaining()));
         destBuff.put(source);
         source.limit(originalLimit);
      }

      destBuff.put(source);
   }

   public static void writeToUnsynchronized(final byte[] data, final int offset, final int length, final ByteBufferDestination destination) {
      ByteBuffer buffer = destination.getByteBuffer();
      int currentOffset = offset;

      int currentLength;
      for (currentLength = length; currentLength > buffer.remaining(); buffer = destination.drain(buffer)) {
         int chunk = buffer.remaining();
         buffer.put(data, currentOffset, chunk);
         currentOffset += chunk;
         currentLength -= chunk;
      }

      buffer.put(data, currentOffset, currentLength);
   }
}
