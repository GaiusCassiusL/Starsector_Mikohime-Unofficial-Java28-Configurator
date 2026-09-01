package org.apache.logging.log4j.core.message;

import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import org.apache.logging.log4j.message.ThreadInformation;
import org.apache.logging.log4j.util.StringBuilders;

class ExtendedThreadInformation implements ThreadInformation {
   private final ThreadInfo threadInfo;

   ExtendedThreadInformation(final ThreadInfo thread) {
      this.threadInfo = thread;
   }

   public void printThreadInfo(final StringBuilder sb) {
      StringBuilders.appendDqValue(sb, this.threadInfo.getThreadName());
      sb.append(" Id=").append(this.threadInfo.getThreadId()).append(' ');
      this.formatState(sb, this.threadInfo);
      if (this.threadInfo.isSuspended()) {
         sb.append(" (suspended)");
      }

      if (this.threadInfo.isInNative()) {
         sb.append(" (in native)");
      }

      sb.append('\n');
   }

   public void printStack(final StringBuilder sb, final StackTraceElement[] stack) {
      int i = 0;

      for (StackTraceElement element : stack) {
         sb.append("\tat ").append(element.toString());
         sb.append('\n');
         if (i == 0 && this.threadInfo.getLockInfo() != null) {
            State ts = this.threadInfo.getThreadState();
            switch (ts) {
               case BLOCKED:
                  sb.append("\t-  blocked on ");
                  this.formatLock(sb, this.threadInfo.getLockInfo());
                  sb.append('\n');
                  break;
               case WAITING:
                  sb.append("\t-  waiting on ");
                  this.formatLock(sb, this.threadInfo.getLockInfo());
                  sb.append('\n');
                  break;
               case TIMED_WAITING:
                  sb.append("\t-  waiting on ");
                  this.formatLock(sb, this.threadInfo.getLockInfo());
                  sb.append('\n');
            }
         }

         for (MonitorInfo mi : this.threadInfo.getLockedMonitors()) {
            if (mi.getLockedStackDepth() == i) {
               sb.append("\t-  locked ");
               this.formatLock(sb, mi);
               sb.append('\n');
            }
         }

         i++;
      }

      LockInfo[] locks = this.threadInfo.getLockedSynchronizers();
      if (locks.length > 0) {
         sb.append("\n\tNumber of locked synchronizers = ").append(locks.length).append('\n');

         for (LockInfo li : locks) {
            sb.append("\t- ");
            this.formatLock(sb, li);
            sb.append('\n');
         }
      }
   }

   private void formatLock(final StringBuilder sb, final LockInfo lock) {
      sb.append('<').append(lock.getIdentityHashCode()).append("> (a ");
      sb.append(lock.getClassName()).append(')');
   }

   private void formatState(final StringBuilder sb, final ThreadInfo info) {
      State state = info.getThreadState();
      sb.append(state);
      switch (state) {
         case BLOCKED:
            sb.append(" (on object monitor owned by \"");
            sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId()).append(')');
            break;
         case WAITING:
            StackTraceElement elementx = info.getStackTrace()[0];
            String classNamex = elementx.getClassName();
            String methodx = elementx.getMethodName();
            if (classNamex.equals("java.lang.Object") && methodx.equals("wait")) {
               sb.append(" (on object monitor");
               if (info.getLockOwnerName() != null) {
                  sb.append(" owned by \"");
                  sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
               }

               sb.append(')');
            } else if (classNamex.equals("java.lang.Thread") && methodx.equals("join")) {
               sb.append(" (on completion of thread ").append(info.getLockOwnerId()).append(')');
            } else {
               sb.append(" (parking for lock");
               if (info.getLockOwnerName() != null) {
                  sb.append(" owned by \"");
                  sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
               }

               sb.append(')');
            }
            break;
         case TIMED_WAITING:
            StackTraceElement element = info.getStackTrace()[0];
            String className = element.getClassName();
            String method = element.getMethodName();
            if (className.equals("java.lang.Object") && method.equals("wait")) {
               sb.append(" (on object monitor");
               if (info.getLockOwnerName() != null) {
                  sb.append(" owned by \"");
                  sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
               }

               sb.append(')');
            } else if (className.equals("java.lang.Thread") && method.equals("sleep")) {
               sb.append(" (sleeping)");
            } else if (className.equals("java.lang.Thread") && method.equals("join")) {
               sb.append(" (on completion of thread ").append(info.getLockOwnerId()).append(')');
            } else {
               sb.append(" (parking for lock");
               if (info.getLockOwnerName() != null) {
                  sb.append(" owned by \"");
                  sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
               }

               sb.append(')');
            }
      }
   }
}
