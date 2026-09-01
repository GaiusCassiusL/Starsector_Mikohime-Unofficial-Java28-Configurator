package com.thoughtworks.xstream.core.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

class ThreadSafeSimpleDateFormat$1 implements Pool.Factory {
   ThreadSafeSimpleDateFormat$1(ThreadSafeSimpleDateFormat this$0, Locale var2, boolean var3) {
      this.this$0 = this$0;
      this.val$locale = var2;
      this.val$lenient = var3;
   }

   public Object newInstance() {
      SimpleDateFormat dateFormat = new SimpleDateFormat(ThreadSafeSimpleDateFormat.access$000(this.this$0), this.val$locale);
      dateFormat.setLenient(this.val$lenient);
      return dateFormat;
   }
}
