package com.thoughtworks.xstream.io.xml.xppdom;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class XppDomComparator implements Comparator {
   private final ThreadLocal xpath;

   public XppDomComparator() {
      this(null);
   }

   public XppDomComparator(ThreadLocal xpath) {
      this.xpath = xpath;
   }

   public int compare(Object dom1, Object dom2) {
      StringBuffer xpath = new StringBuffer("/");
      int s = this.compareInternal((XppDom)dom1, (XppDom)dom2, xpath, -1);
      if (this.xpath != null) {
         if (s != 0) {
            this.xpath.set(xpath.toString());
         } else {
            this.xpath.set(null);
         }
      }

      return s;
   }

   private int compareInternal(XppDom dom1, XppDom dom2, StringBuffer xpath, int count) {
      int pathlen = xpath.length();
      String name = dom1.getName();
      int s = name.compareTo(dom2.getName());
      xpath.append(name);
      if (count >= 0) {
         xpath.append('[').append(count).append(']');
      }

      if (s != 0) {
         xpath.append('?');
         return s;
      }

      String[] attributes = dom1.getAttributeNames();
      String[] attributes2 = dom2.getAttributeNames();
      int len = attributes.length;
      s = attributes2.length - len;
      if (s != 0) {
         xpath.append("::count(@*)");
         return s < 0 ? 1 : -1;
      }

      Arrays.sort(attributes);
      Arrays.sort(attributes2);

      for (int i = 0; i < len; i++) {
         String attribute = attributes[i];
         s = attribute.compareTo(attributes2[i]);
         if (s != 0) {
            xpath.append("[@").append(attribute).append("?]");
            return s;
         }

         s = dom1.getAttribute(attribute).compareTo(dom2.getAttribute(attribute));
         if (s != 0) {
            xpath.append("[@").append(attribute).append(']');
            return s;
         }
      }

      int children = dom1.getChildCount();
      s = dom2.getChildCount() - children;
      if (s != 0) {
         xpath.append("::count(*)");
         return s < 0 ? 1 : -1;
      }

      if (children > 0) {
         if (dom1.getValue() != null || dom2.getValue() != null) {
            throw new IllegalArgumentException("XppDom cannot handle mixed mode at " + xpath + "::text()");
         }

         xpath.append('/');
         Map names = new HashMap();

         for (int i = 0; i < children; i++) {
            XppDom child1 = dom1.getChild(i);
            XppDom child2 = dom2.getChild(i);
            String child = child1.getName();
            if (!names.containsKey(child)) {
               names.put(child, new int[1]);
            }

            s = this.compareInternal(child1, child2, xpath, ((int[])names.get(child))[0]++);
            if (s != 0) {
               return s;
            }
         }
      } else {
         String value2 = dom2.getValue();
         String value1 = dom1.getValue();
         if (value1 == null) {
            s = value2 == null ? 0 : -1;
         } else {
            s = value2 == null ? 1 : value1.compareTo(value2);
         }

         if (s != 0) {
            xpath.append("::text()");
            return s;
         }
      }

      xpath.setLength(pathlen);
      return s;
   }
}
