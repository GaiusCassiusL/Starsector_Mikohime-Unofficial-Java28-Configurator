package org.apache.logging.log4j.core.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "BlockingQueueFactory")
@Plugin("ArrayBlockingQueue")
public class ArrayBlockingQueueFactory<E> implements BlockingQueueFactory<E> {
   @Override
   public BlockingQueue<E> create(final int capacity) {
      return new ArrayBlockingQueue<>(capacity);
   }

   @PluginFactory
   public static <E> ArrayBlockingQueueFactory<E> createFactory() {
      return new ArrayBlockingQueueFactory<>();
   }
}
