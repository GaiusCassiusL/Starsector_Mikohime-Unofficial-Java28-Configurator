package org.apache.logging.log4j.core.layout;

import java.nio.ByteBuffer;

public interface ByteBufferDestination {
   ByteBuffer getByteBuffer();

   ByteBuffer drain(ByteBuffer buf);

   void writeBytes(ByteBuffer data);

   void writeBytes(byte[] data, int offset, int length);
}
