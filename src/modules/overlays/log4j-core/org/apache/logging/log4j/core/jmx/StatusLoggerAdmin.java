package org.apache.logging.log4j.core.jmx;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;

public class StatusLoggerAdmin extends NotificationBroadcasterSupport implements StatusListener, StatusLoggerAdminMBean {
   private final AtomicLong sequenceNo = new AtomicLong();
   private final ObjectName objectName;
   private final String contextName;
   private Level level = Level.WARN;

   public StatusLoggerAdmin(final String contextName, final Executor executor) {
      super(executor, createNotificationInfo());
      this.contextName = contextName;

      try {
         String mbeanName = String.format("org.apache.logging.log4j2:type=%s,component=StatusLogger", Server.escape(contextName));
         this.objectName = new ObjectName(mbeanName);
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }

      this.removeListeners(contextName);
      StatusLogger.getLogger().registerListener(this);
   }

   private void removeListeners(final String ctxName) {
      StatusLogger logger = StatusLogger.getLogger();

      for (StatusListener statusListener : logger.getListeners()) {
         if (statusListener instanceof StatusLoggerAdmin) {
            StatusLoggerAdmin adminListener = (StatusLoggerAdmin)statusListener;
            if (ctxName != null && ctxName.equals(adminListener.contextName)) {
               logger.removeListener(adminListener);
            }
         }
      }
   }

   private static MBeanNotificationInfo createNotificationInfo() {
      String[] notifTypes = new String[]{"com.apache.logging.log4j.core.jmx.statuslogger.data", "com.apache.logging.log4j.core.jmx.statuslogger.message"};
      String name = Notification.class.getName();
      String description = "StatusLogger has logged an event";
      return new MBeanNotificationInfo(notifTypes, name, "StatusLogger has logged an event");
   }

   @Override
   public String[] getStatusDataHistory() {
      List<StatusData> data = this.getStatusData();
      String[] result = new String[data.size()];

      for (int i = 0; i < result.length; i++) {
         result[i] = data.get(i).getFormattedStatus();
      }

      return result;
   }

   @Override
   public List<StatusData> getStatusData() {
      return StatusLogger.getLogger().getStatusData();
   }

   @Override
   public String getLevel() {
      return this.level.name();
   }

   public Level getStatusLevel() {
      return this.level;
   }

   @Override
   public void setLevel(final String level) {
      this.level = Level.toLevel(level, Level.ERROR);
   }

   @Override
   public String getContextName() {
      return this.contextName;
   }

   public void log(final StatusData data) {
      Notification notifMsg = new Notification(
         "com.apache.logging.log4j.core.jmx.statuslogger.message", this.getObjectName(), this.nextSeqNo(), this.nowMillis(), data.getFormattedStatus()
      );
      this.sendNotification(notifMsg);
      Notification notifData = new Notification("com.apache.logging.log4j.core.jmx.statuslogger.data", this.getObjectName(), this.nextSeqNo(), this.nowMillis());
      notifData.setUserData(data);
      this.sendNotification(notifData);
   }

   @Override
   public ObjectName getObjectName() {
      return this.objectName;
   }

   private long nextSeqNo() {
      return this.sequenceNo.getAndIncrement();
   }

   private long nowMillis() {
      return System.currentTimeMillis();
   }

   public void close() throws IOException {
   }
}
