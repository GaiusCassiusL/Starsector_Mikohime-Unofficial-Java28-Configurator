package org.apache.log4j.or.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class MessageRenderer implements ObjectRenderer {
   private static final Logger LOGGER = StatusLogger.getLogger();

   @Override
   public String doRender(final Object obj) {
      if (obj instanceof Message) {
         StringBuilder sb = new StringBuilder();
         Message message = (Message)obj;

         try {
            sb.append("DeliveryMode=");
            switch (message.getJMSDeliveryMode()) {
               case 1:
                  sb.append("NON_PERSISTENT");
                  break;
               case 2:
                  sb.append("PERSISTENT");
                  break;
               default:
                  sb.append("UNKNOWN");
            }

            sb.append(", CorrelationID=");
            sb.append(message.getJMSCorrelationID());
            sb.append(", Destination=");
            sb.append(message.getJMSDestination());
            sb.append(", Expiration=");
            sb.append(message.getJMSExpiration());
            sb.append(", MessageID=");
            sb.append(message.getJMSMessageID());
            sb.append(", Priority=");
            sb.append(message.getJMSPriority());
            sb.append(", Redelivered=");
            sb.append(message.getJMSRedelivered());
            sb.append(", ReplyTo=");
            sb.append(message.getJMSReplyTo());
            sb.append(", Timestamp=");
            sb.append(message.getJMSTimestamp());
            sb.append(", Type=");
            sb.append(message.getJMSType());
         } catch (JMSException e) {
            LOGGER.error("Could not parse Message.", e);
         }

         return sb.toString();
      } else {
         return obj.toString();
      }
   }
}
