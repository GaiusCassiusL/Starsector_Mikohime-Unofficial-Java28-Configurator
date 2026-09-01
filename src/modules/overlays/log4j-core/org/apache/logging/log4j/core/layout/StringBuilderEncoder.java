package org.apache.logging.log4j.core.layout;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;

public class StringBuilderEncoder implements Encoder<StringBuilder> {
   private final ThreadLocal<Object[]> threadLocal = new ThreadLocal<>();
   private final Charset charset;
   private final int charBufferSize;
   private final int byteBufferSize;

   public StringBuilderEncoder(final Charset charset) {
      this(charset, Constants.ENCODER_CHAR_BUFFER_SIZE, Constants.ENCODER_BYTE_BUFFER_SIZE);
   }

   public StringBuilderEncoder(final Charset charset, final int charBufferSize, final int byteBufferSize) {
      this.charBufferSize = charBufferSize;
      this.byteBufferSize = byteBufferSize;
      this.charset = Objects.requireNonNull(charset, "charset");
   }

   public void encode(final StringBuilder source, final ByteBufferDestination destination) {
      try {
         Object[] threadLocalState = this.getThreadLocalState();
         CharsetEncoder charsetEncoder = (CharsetEncoder)threadLocalState[0];
         CharBuffer charBuffer = (CharBuffer)threadLocalState[1];
         ByteBuffer byteBuffer = (ByteBuffer)threadLocalState[2];
         TextEncoderHelper.encodeText(charsetEncoder, charBuffer, byteBuffer, source, destination);
      } catch (Exception ex) {
         logEncodeTextException(ex, source);
         TextEncoderHelper.encodeTextFallBack(this.charset, source, destination);
      }
   }

   private Object[] getThreadLocalState() {
      Object[] threadLocalState = this.threadLocal.get();
      if (threadLocalState == null) {
         threadLocalState = new Object[]{
            this.charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE),
            CharBuffer.allocate(this.charBufferSize),
            ByteBuffer.allocate(this.byteBufferSize)
         };
         this.threadLocal.set(threadLocalState);
      } else {
         ((CharsetEncoder)threadLocalState[0]).reset();
         ((CharBuffer)threadLocalState[1]).clear();
         ((ByteBuffer)threadLocalState[2]).clear();
      }

      return threadLocalState;
   }

   private static void logEncodeTextException(final Exception ex, final StringBuilder text) {
      StatusLogger.getLogger().error("Recovering from StringBuilderEncoder.encode('{}') error: {}", text, ex, ex);
   }
}
