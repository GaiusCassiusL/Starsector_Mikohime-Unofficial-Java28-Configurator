package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.AttributeNameIterator;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.util.Iterator;

public abstract class AbstractPullReader extends AbstractXmlReader {
   protected static final int START_NODE = 1;
   protected static final int END_NODE = 2;
   protected static final int TEXT = 3;
   protected static final int COMMENT = 4;
   protected static final int OTHER = 0;
   private final FastStack elementStack = new FastStack(16);
   private final FastStack pool = new FastStack(16);
   private final FastStack lookahead = new FastStack(4);
   private final FastStack lookback = new FastStack(4);
   private boolean marked;

   protected AbstractPullReader(NameCoder nameCoder) {
      super(nameCoder);
   }

   /** @deprecated */
   protected AbstractPullReader(XmlFriendlyReplacer replacer) {
      this((NameCoder)replacer);
   }

   protected abstract int pullNextEvent();

   protected abstract String pullElementName();

   protected abstract String pullText();

   public boolean hasMoreChildren() {
      this.mark();

      while (true) {
         switch (this.readEvent().type) {
            case 1:
               this.reset();
               return true;
            case 2:
               this.reset();
               return false;
         }
      }
   }

   public void moveDown() {
      int currentDepth = this.elementStack.size();

      while (this.elementStack.size() <= currentDepth) {
         this.move();
         if (this.elementStack.size() < currentDepth) {
            throw new RuntimeException();
         }
      }
   }

   public void moveUp() {
      int currentDepth = this.elementStack.size();

      while (this.elementStack.size() >= currentDepth) {
         this.move();
      }
   }

   private void move() {
      AbstractPullReader.Event event = this.readEvent();
      this.pool.push(event);
      switch (event.type) {
         case 1:
            this.elementStack.push(this.pullElementName());
            break;
         case 2:
            this.elementStack.pop();
      }
   }

   private AbstractPullReader.Event readEvent() {
      if (this.marked) {
         return this.lookback.hasStuff()
            ? (AbstractPullReader.Event)this.lookahead.push(this.lookback.pop())
            : (AbstractPullReader.Event)this.lookahead.push(this.readRealEvent());
      } else {
         return this.lookback.hasStuff() ? (AbstractPullReader.Event)this.lookback.pop() : this.readRealEvent();
      }
   }

   private AbstractPullReader.Event readRealEvent() {
      AbstractPullReader.Event event = this.pool.hasStuff() ? (AbstractPullReader.Event)this.pool.pop() : new AbstractPullReader.Event(null);
      event.type = this.pullNextEvent();
      if (event.type == 3) {
         event.value = this.pullText();
      } else if (event.type == 1) {
         event.value = this.pullElementName();
      } else {
         event.value = null;
      }

      return event;
   }

   public void mark() {
      this.marked = true;
   }

   public void reset() {
      while (this.lookahead.hasStuff()) {
         this.lookback.push(this.lookahead.pop());
      }

      this.marked = false;
   }

   public String getValue() {
      String last = null;
      StringBuffer buffer = null;
      this.mark();
      AbstractPullReader.Event event = this.readEvent();

      while (true) {
         if (event.type == 3) {
            String text = event.value;
            if (text != null && text.length() > 0) {
               if (last == null) {
                  last = text;
               } else {
                  if (buffer == null) {
                     buffer = new StringBuffer(last);
                  }

                  buffer.append(text);
               }
            }
         } else if (event.type != 4) {
            this.reset();
            if (buffer != null) {
               return buffer.toString();
            }

            return last == null ? "" : last;
         }

         event = this.readEvent();
      }
   }

   public Iterator getAttributeNames() {
      return new AttributeNameIterator(this);
   }

   public String getNodeName() {
      return this.unescapeXmlName((String)this.elementStack.peek());
   }

   public String peekNextChild() {
      this.mark();

      while (true) {
         AbstractPullReader.Event ev = this.readEvent();
         switch (ev.type) {
            case 1:
               this.reset();
               return ev.value;
            case 2:
               this.reset();
               return null;
         }
      }
   }

   private static class Event {
      int type;
      String value;

      private Event() {
      }
   }
}
