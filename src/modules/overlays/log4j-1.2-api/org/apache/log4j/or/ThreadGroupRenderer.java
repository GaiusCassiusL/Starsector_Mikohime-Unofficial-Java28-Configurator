package org.apache.log4j.or;

import org.apache.log4j.Layout;

public class ThreadGroupRenderer implements ObjectRenderer {
   @Override
   public String doRender(final Object obj) {
      if (!(obj instanceof ThreadGroup)) {
         try {
            return obj.toString();
         } catch (Exception ex) {
            return ex.toString();
         }
      } else {
         StringBuilder sb = new StringBuilder();
         ThreadGroup threadGroup = (ThreadGroup)obj;
         sb.append("java.lang.ThreadGroup[name=");
         sb.append(threadGroup.getName());
         sb.append(", maxpri=");
         sb.append(threadGroup.getMaxPriority());
         sb.append("]");
         Thread[] threads = new Thread[threadGroup.activeCount()];
         threadGroup.enumerate(threads);

         for (Thread thread : threads) {
            sb.append(Layout.LINE_SEP);
            sb.append("   Thread=[");
            sb.append(thread.getName());
            sb.append(",");
            sb.append(thread.getPriority());
            sb.append(",");
            sb.append(thread.isDaemon());
            sb.append("]");
         }

         return sb.toString();
      }
   }
}
