package org.apache.logging.log4j.core.async;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.conversantmedia.util.concurrent.SpinPolicy;
import java.util.concurrent.BlockingQueue;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "BlockingQueueFactory", printObject = true)
@Plugin("DisruptorBlockingQueue")
public final class DisruptorBlockingQueueFactory<E> implements BlockingQueueFactory<E> {
   private final SpinPolicy spinPolicy;

   private DisruptorBlockingQueueFactory(final SpinPolicy spinPolicy) {
      this.spinPolicy = spinPolicy;
   }

   @Override
   public BlockingQueue<E> create(final int capacity) {
      return new DisruptorBlockingQueue(capacity, this.spinPolicy);
   }

   @PluginFactory
   public static <E> DisruptorBlockingQueueFactory<E> createFactory(@PluginAttribute(defaultString = "WAITING") final SpinPolicy spinPolicy) {
      return new DisruptorBlockingQueueFactory<>(spinPolicy);
   }
}
