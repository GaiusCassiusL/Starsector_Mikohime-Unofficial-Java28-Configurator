package org.apache.logging.log4j.core.async;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.jctools.queues.MpscArrayQueue;

@Configurable(elementType = "BlockingQueueFactory", printObject = true)
@Plugin("JCToolsBlockingQueue")
public final class JCToolsBlockingQueueFactory<E> implements BlockingQueueFactory<E> {
   private final JCToolsBlockingQueueFactory.WaitStrategy waitStrategy;

   private JCToolsBlockingQueueFactory(final JCToolsBlockingQueueFactory.WaitStrategy waitStrategy) {
      this.waitStrategy = waitStrategy;
   }

   @Override
   public BlockingQueue<E> create(final int capacity) {
      return new JCToolsBlockingQueueFactory.MpscBlockingQueue<>(capacity, this.waitStrategy);
   }

   @PluginFactory
   public static <E> JCToolsBlockingQueueFactory<E> createFactory(
      @PluginAttribute(defaultString = "PARK") final JCToolsBlockingQueueFactory.WaitStrategy waitStrategy
   ) {
      return new JCToolsBlockingQueueFactory<>(waitStrategy);
   }

   private interface Idle {
      int idle(int idleCounter);
   }

   private static final class MpscBlockingQueue<E> extends MpscArrayQueue<E> implements BlockingQueue<E> {
      private final JCToolsBlockingQueueFactory.WaitStrategy waitStrategy;

      MpscBlockingQueue(final int capacity, final JCToolsBlockingQueueFactory.WaitStrategy waitStrategy) {
         super(capacity);
         this.waitStrategy = waitStrategy;
      }

      @Override
      public int drainTo(final Collection<? super E> c) {
         return this.drainTo(c, this.capacity());
      }

      @Override
      public int drainTo(final Collection<? super E> c, final int maxElements) {
         return this.drain(c::add, maxElements);
      }

      @Override
      public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
         int idleCounter = 0;
         long timeoutNanos = System.nanoTime() + unit.toNanos(timeout);

         while (!this.offer(e)) {
            if (System.nanoTime() - timeoutNanos > 0L) {
               return false;
            }

            idleCounter = this.waitStrategy.idle(idleCounter);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
         }

         return true;
      }

      @Override
      public E poll(final long timeout, final TimeUnit unit) throws InterruptedException {
         int idleCounter = 0;
         long timeoutNanos = System.nanoTime() + unit.toNanos(timeout);

         do {
            E result = this.poll();
            if (result != null) {
               return result;
            }

            if (System.nanoTime() - timeoutNanos > 0L) {
               return null;
            }

            idleCounter = this.waitStrategy.idle(idleCounter);
         } while (!Thread.interrupted());

         throw new InterruptedException();
      }

      @Override
      public void put(final E e) throws InterruptedException {
         int idleCounter = 0;

         while (!this.offer(e)) {
            idleCounter = this.waitStrategy.idle(idleCounter);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
         }
      }

      @Override
      public boolean offer(final E e) {
         return this.offerIfBelowThreshold(e, this.capacity() - 32);
      }

      @Override
      public int remainingCapacity() {
         return this.capacity() - this.size();
      }

      @Override
      public E take() throws InterruptedException {
         int idleCounter = 100;

         do {
            E result = (E)this.relaxedPoll();
            if (result != null) {
               return result;
            }

            idleCounter = this.waitStrategy.idle(idleCounter);
         } while (!Thread.interrupted());

         throw new InterruptedException();
      }
   }

   public enum WaitStrategy {
      SPIN(idleCounter -> idleCounter + 1),
      YIELD(idleCounter -> {
         Thread.yield();
         return idleCounter + 1;
      }),
      PARK(idleCounter -> {
         LockSupport.parkNanos(1L);
         return idleCounter + 1;
      }),
      PROGRESSIVE(idleCounter -> {
         if (idleCounter > 200) {
            LockSupport.parkNanos(1L);
         } else if (idleCounter > 100) {
            Thread.yield();
         }

         return idleCounter + 1;
      });

      private final JCToolsBlockingQueueFactory.Idle idle;

      private int idle(final int idleCounter) {
         return this.idle.idle(idleCounter);
      }

      WaitStrategy(final JCToolsBlockingQueueFactory.Idle idle) {
         this.idle = idle;
      }
   }
}
