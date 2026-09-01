package org.apache.logging.log4j.core.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.util.PropertiesUtil;

public final class UuidUtil {
   private static final String ASSIGNED_SEQUENCES = "org.apache.logging.log4j.assignedSequences";
   private static final AtomicInteger COUNT = new AtomicInteger(0);
   private static final long TYPE1 = 4096L;
   private static final byte VARIANT = -128;
   private static final int SEQUENCE_MASK = 16383;
   private static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 122192928000000000L;
   private static final long INITIAL_UUID_SEQNO = PropertiesUtil.getProperties().getLongProperty(Log4jPropertyKey.UUID_SEQUENCE, 0L);
   private static final long LOW_MASK = 4294967295L;
   private static final long MID_MASK = 281470681743360L;
   private static final long HIGH_MASK = 1152640029630136320L;
   private static final int NODE_SIZE = 8;
   private static final int SHIFT_2 = 16;
   private static final int SHIFT_4 = 32;
   private static final int SHIFT_6 = 48;
   private static final int HUNDRED_NANOS_PER_MILLI = 10000;
   private static final long LEAST = initialize(NetUtils.getMacAddress());

   private UuidUtil() {
   }

   static long initialize(byte[] mac) {
      Random randomGenerator = new SecureRandom();
      if (mac == null || mac.length == 0) {
         mac = new byte[6];
         randomGenerator.nextBytes(mac);
      }

      int length = Math.min(mac.length, 6);
      int index = mac.length >= 6 ? mac.length - 6 : 0;
      byte[] node = new byte[8];
      node[0] = -128;
      node[1] = 0;

      for (int i = 2; i < 8; i++) {
         node[i] = 0;
      }

      System.arraycopy(mac, index, node, 2, length);
      ByteBuffer buf = ByteBuffer.wrap(node);
      long rand = INITIAL_UUID_SEQNO;
      String assigned = PropertiesUtil.getProperties().getStringProperty("org.apache.logging.log4j.assignedSequences");
      long[] sequences;
      if (assigned == null) {
         sequences = new long[0];
      } else {
         String[] array = assigned.split(Patterns.COMMA_SEPARATOR);
         sequences = new long[array.length];
         int i = 0;

         for (String value : array) {
            sequences[i] = Long.parseLong(value);
            i++;
         }
      }

      if (rand == 0L) {
         rand = randomGenerator.nextLong();
      }

      rand &= 16383L;

      boolean duplicate;
      do {
         duplicate = false;

         for (long sequence : sequences) {
            if (sequence == rand) {
               duplicate = true;
               break;
            }
         }

         if (duplicate) {
            rand = rand + 1L & 16383L;
         }
      } while (duplicate);

      assigned = assigned == null ? Long.toString(rand) : assigned + "," + Long.toString(rand);
      System.setProperty("org.apache.logging.log4j.assignedSequences", assigned);
      return buf.getLong() | rand << 48;
   }

   public static UUID getTimeBasedUuid() {
      long time = System.currentTimeMillis() * 10000L + 122192928000000000L + COUNT.incrementAndGet() % 10000;
      long timeLow = (time & 4294967295L) << 32;
      long timeMid = (time & 281470681743360L) >> 16;
      long timeHi = (time & 1152640029630136320L) >> 48;
      long most = timeLow | timeMid | 4096L | timeHi;
      return new UUID(most, LEAST);
   }
}
