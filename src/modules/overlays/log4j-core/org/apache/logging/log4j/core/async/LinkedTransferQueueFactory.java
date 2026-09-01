package org.apache.logging.log4j.core.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "BlockingQueueFactory", printObject = true)
@Plugin("LinkedTransferQueue")
public class LinkedTransferQueueFactory<E> implements BlockingQueueFactory<E> {
   @Override
   public BlockingQueue<E> create(final int capacity) {
      return new LinkedTransferQueue<>();
   }

   @PluginFactory
   public static <E> LinkedTransferQueueFactory<E> createFactory() {
      return new LinkedTransferQueueFactory<>();
   }
}
