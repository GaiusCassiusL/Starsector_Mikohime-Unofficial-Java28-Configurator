package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class XmlFriendlyNameCoder implements NameCoder, Cloneable {
   private static final BitSet XML_NAME_START_CHARS;
   private static final BitSet XML_NAME_CHARS;
   private final String dollarReplacement;
   private final String escapeCharReplacement;
   private transient Map escapeCache;
   private transient Map unescapeCache;
   private final String hexPrefix;

   public XmlFriendlyNameCoder() {
      this("_-", "__");
   }

   public XmlFriendlyNameCoder(String dollarReplacement, String escapeCharReplacement) {
      this(dollarReplacement, escapeCharReplacement, "_.");
   }

   public XmlFriendlyNameCoder(String dollarReplacement, String escapeCharReplacement, String hexPrefix) {
      this.dollarReplacement = dollarReplacement;
      this.escapeCharReplacement = escapeCharReplacement;
      this.hexPrefix = hexPrefix;
      this.readResolve();
   }

   public String decodeAttribute(String attributeName) {
      return this.decodeName(attributeName);
   }

   public String decodeNode(String elementName) {
      return this.decodeName(elementName);
   }

   public String encodeAttribute(String name) {
      return this.encodeName(name);
   }

   public String encodeNode(String name) {
      return this.encodeName(name);
   }

   private String encodeName(String name) {
      String s = (String)this.escapeCache.get(name);
      if (s == null) {
         int length = name.length();

         int i;
         for (i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (c < 'A' || c > 'Z' && c < 'a' || c > 'z') {
               break;
            }
         }

         if (i == length) {
            return name;
         }

         StringBuffer result = new StringBuffer(length + 8);
         if (i > 0) {
            result.append(name.substring(0, i));
         }

         for (; i < length; i++) {
            char c = name.charAt(i);
            if (c == '$') {
               result.append(this.dollarReplacement);
            } else if (c == '_') {
               result.append(this.escapeCharReplacement);
            } else if (i == 0 && !isXmlNameStartChar(c) || i > 0 && !isXmlNameChar(c)) {
               result.append(this.hexPrefix);
               if (c < 16) {
                  result.append("000");
               } else if (c < 256) {
                  result.append("00");
               } else if (c < 4096) {
                  result.append("0");
               }

               result.append(Integer.toHexString(c));
            } else {
               result.append(c);
            }
         }

         s = result.toString();
         this.escapeCache.put(name, s);
      }

      return s;
   }

   private String decodeName(String name) {
      String s = (String)this.unescapeCache.get(name);
      if (s == null) {
         char dollarReplacementFirstChar = this.dollarReplacement.charAt(0);
         char escapeReplacementFirstChar = this.escapeCharReplacement.charAt(0);
         char hexPrefixFirstChar = this.hexPrefix.charAt(0);
         int length = name.length();

         int i;
         for (i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (c == dollarReplacementFirstChar || c == escapeReplacementFirstChar || c == hexPrefixFirstChar) {
               break;
            }
         }

         if (i == length) {
            return name;
         }

         StringBuffer result = new StringBuffer(length + 8);
         if (i > 0) {
            result.append(name.substring(0, i));
         }

         for (; i < length; i++) {
            char c = name.charAt(i);
            if (c == dollarReplacementFirstChar && name.startsWith(this.dollarReplacement, i)) {
               i += this.dollarReplacement.length() - 1;
               result.append('$');
            } else if (c == hexPrefixFirstChar && name.startsWith(this.hexPrefix, i)) {
               i += this.hexPrefix.length();
               c = (char)Integer.parseInt(name.substring(i, i + 4), 16);
               i += 3;
               result.append(c);
            } else if (c == escapeReplacementFirstChar && name.startsWith(this.escapeCharReplacement, i)) {
               i += this.escapeCharReplacement.length() - 1;
               result.append('_');
            } else {
               result.append(c);
            }
         }

         s = result.toString();
         this.unescapeCache.put(name, s);
      }

      return s;
   }

   public Object clone() {
      try {
         XmlFriendlyNameCoder coder = (XmlFriendlyNameCoder)super.clone();
         coder.readResolve();
         return coder;
      } catch (CloneNotSupportedException e) {
         throw new ObjectAccessException("Cannot clone XmlFriendlyNameCoder", e);
      }
   }

   private Object readResolve() {
      this.escapeCache = this.createCacheMap();
      this.unescapeCache = this.createCacheMap();
      return this;
   }

   protected Map createCacheMap() {
      return new HashMap();
   }

   private static boolean isXmlNameStartChar(int cp) {
      return XML_NAME_START_CHARS.get(cp);
   }

   private static boolean isXmlNameChar(int cp) {
      return XML_NAME_CHARS.get(cp);
   }

   static {
      BitSet XML_NAME_START_CHARS_4TH = new BitSet(1048575);
      XML_NAME_START_CHARS_4TH.set(58);
      XML_NAME_START_CHARS_4TH.set(95);
      XML_NAME_START_CHARS_4TH.set(65, 91);
      XML_NAME_START_CHARS_4TH.set(97, 123);
      XML_NAME_START_CHARS_4TH.set(192, 215);
      XML_NAME_START_CHARS_4TH.set(216, 247);
      BitSet XML_NAME_START_CHARS_5TH = (BitSet)XML_NAME_START_CHARS_4TH.clone();
      XML_NAME_START_CHARS_4TH.set(248, 306);
      XML_NAME_START_CHARS_4TH.set(308, 319);
      XML_NAME_START_CHARS_4TH.set(321, 329);
      XML_NAME_START_CHARS_4TH.set(330, 383);
      XML_NAME_START_CHARS_4TH.set(384, 452);
      XML_NAME_START_CHARS_4TH.set(461, 497);
      XML_NAME_START_CHARS_4TH.set(461, 497);
      XML_NAME_START_CHARS_4TH.set(500, 502);
      XML_NAME_START_CHARS_4TH.set(506, 536);
      XML_NAME_START_CHARS_4TH.set(592, 681);
      XML_NAME_START_CHARS_4TH.set(699, 706);
      XML_NAME_START_CHARS_4TH.set(902);
      XML_NAME_START_CHARS_4TH.set(904, 907);
      XML_NAME_START_CHARS_4TH.set(908);
      XML_NAME_START_CHARS_4TH.set(910, 930);
      XML_NAME_START_CHARS_4TH.set(931, 975);
      XML_NAME_START_CHARS_4TH.set(976, 983);
      XML_NAME_START_CHARS_4TH.set(986);
      XML_NAME_START_CHARS_4TH.set(988);
      XML_NAME_START_CHARS_4TH.set(990);
      XML_NAME_START_CHARS_4TH.set(992);
      XML_NAME_START_CHARS_4TH.set(994, 1012);
      XML_NAME_START_CHARS_4TH.set(1025, 1037);
      XML_NAME_START_CHARS_4TH.set(1038, 1104);
      XML_NAME_START_CHARS_4TH.set(1105, 1117);
      XML_NAME_START_CHARS_4TH.set(1118, 1154);
      XML_NAME_START_CHARS_4TH.set(1168, 1221);
      XML_NAME_START_CHARS_4TH.set(1223, 1225);
      XML_NAME_START_CHARS_4TH.set(1227, 1229);
      XML_NAME_START_CHARS_4TH.set(1232, 1260);
      XML_NAME_START_CHARS_4TH.set(1262, 1270);
      XML_NAME_START_CHARS_4TH.set(1272, 1274);
      XML_NAME_START_CHARS_4TH.set(1329, 1367);
      XML_NAME_START_CHARS_4TH.set(1369);
      XML_NAME_START_CHARS_4TH.set(1377, 1415);
      XML_NAME_START_CHARS_4TH.set(1488, 1515);
      XML_NAME_START_CHARS_4TH.set(1520, 1523);
      XML_NAME_START_CHARS_4TH.set(1569, 1595);
      XML_NAME_START_CHARS_4TH.set(1601, 1611);
      XML_NAME_START_CHARS_4TH.set(1649, 1720);
      XML_NAME_START_CHARS_4TH.set(1722, 1727);
      XML_NAME_START_CHARS_4TH.set(1728, 1743);
      XML_NAME_START_CHARS_4TH.set(1744, 1748);
      XML_NAME_START_CHARS_4TH.set(1749);
      XML_NAME_START_CHARS_4TH.set(1765, 1767);
      XML_NAME_START_CHARS_4TH.set(2309, 2362);
      XML_NAME_START_CHARS_4TH.set(2365);
      XML_NAME_START_CHARS_4TH.set(2392, 2402);
      XML_NAME_START_CHARS_4TH.set(2437, 2445);
      XML_NAME_START_CHARS_4TH.set(2447, 2449);
      XML_NAME_START_CHARS_4TH.set(2451, 2473);
      XML_NAME_START_CHARS_4TH.set(2474, 2481);
      XML_NAME_START_CHARS_4TH.set(2482);
      XML_NAME_START_CHARS_4TH.set(2486, 2490);
      XML_NAME_START_CHARS_4TH.set(2524, 2526);
      XML_NAME_START_CHARS_4TH.set(2527, 2530);
      XML_NAME_START_CHARS_4TH.set(2544, 2546);
      XML_NAME_START_CHARS_4TH.set(2565, 2571);
      XML_NAME_START_CHARS_4TH.set(2575, 2577);
      XML_NAME_START_CHARS_4TH.set(2579, 2601);
      XML_NAME_START_CHARS_4TH.set(2602, 2609);
      XML_NAME_START_CHARS_4TH.set(2610, 2612);
      XML_NAME_START_CHARS_4TH.set(2613, 2615);
      XML_NAME_START_CHARS_4TH.set(2616, 2618);
      XML_NAME_START_CHARS_4TH.set(2649, 2653);
      XML_NAME_START_CHARS_4TH.set(2654);
      XML_NAME_START_CHARS_4TH.set(2674, 2677);
      XML_NAME_START_CHARS_4TH.set(2693, 2700);
      XML_NAME_START_CHARS_4TH.set(2701);
      XML_NAME_START_CHARS_4TH.set(2703, 2706);
      XML_NAME_START_CHARS_4TH.set(2707, 2729);
      XML_NAME_START_CHARS_4TH.set(2730, 2737);
      XML_NAME_START_CHARS_4TH.set(2738, 2740);
      XML_NAME_START_CHARS_4TH.set(2741, 2746);
      XML_NAME_START_CHARS_4TH.set(2749);
      XML_NAME_START_CHARS_4TH.set(2784);
      XML_NAME_START_CHARS_4TH.set(2821, 2829);
      XML_NAME_START_CHARS_4TH.set(2831, 2833);
      XML_NAME_START_CHARS_4TH.set(2835, 2857);
      XML_NAME_START_CHARS_4TH.set(2858, 2865);
      XML_NAME_START_CHARS_4TH.set(2866, 2868);
      XML_NAME_START_CHARS_4TH.set(2870, 2874);
      XML_NAME_START_CHARS_4TH.set(2877);
      XML_NAME_START_CHARS_4TH.set(2908, 2910);
      XML_NAME_START_CHARS_4TH.set(2911, 2914);
      XML_NAME_START_CHARS_4TH.set(2949, 2955);
      XML_NAME_START_CHARS_4TH.set(2958, 2961);
      XML_NAME_START_CHARS_4TH.set(2962, 2966);
      XML_NAME_START_CHARS_4TH.set(2969, 2971);
      XML_NAME_START_CHARS_4TH.set(2972);
      XML_NAME_START_CHARS_4TH.set(2974, 2976);
      XML_NAME_START_CHARS_4TH.set(2979, 2981);
      XML_NAME_START_CHARS_4TH.set(2984, 2987);
      XML_NAME_START_CHARS_4TH.set(2990, 2998);
      XML_NAME_START_CHARS_4TH.set(2999, 3002);
      XML_NAME_START_CHARS_4TH.set(3077, 3085);
      XML_NAME_START_CHARS_4TH.set(3086, 3089);
      XML_NAME_START_CHARS_4TH.set(3090, 3113);
      XML_NAME_START_CHARS_4TH.set(3114, 3124);
      XML_NAME_START_CHARS_4TH.set(3125, 3130);
      XML_NAME_START_CHARS_4TH.set(3168, 3170);
      XML_NAME_START_CHARS_4TH.set(3205, 3213);
      XML_NAME_START_CHARS_4TH.set(3214, 3217);
      XML_NAME_START_CHARS_4TH.set(3218, 3241);
      XML_NAME_START_CHARS_4TH.set(3242, 3252);
      XML_NAME_START_CHARS_4TH.set(3253, 3258);
      XML_NAME_START_CHARS_4TH.set(3294);
      XML_NAME_START_CHARS_4TH.set(3296, 3298);
      XML_NAME_START_CHARS_4TH.set(3333, 3341);
      XML_NAME_START_CHARS_4TH.set(3342, 3345);
      XML_NAME_START_CHARS_4TH.set(3346, 3369);
      XML_NAME_START_CHARS_4TH.set(3370, 3386);
      XML_NAME_START_CHARS_4TH.set(3424, 3426);
      XML_NAME_START_CHARS_4TH.set(3585, 3631);
      XML_NAME_START_CHARS_4TH.set(3632);
      XML_NAME_START_CHARS_4TH.set(3634, 3636);
      XML_NAME_START_CHARS_4TH.set(3648, 3654);
      XML_NAME_START_CHARS_4TH.set(3713, 3715);
      XML_NAME_START_CHARS_4TH.set(3716);
      XML_NAME_START_CHARS_4TH.set(3719, 3721);
      XML_NAME_START_CHARS_4TH.set(3722);
      XML_NAME_START_CHARS_4TH.set(3725);
      XML_NAME_START_CHARS_4TH.set(3732, 3736);
      XML_NAME_START_CHARS_4TH.set(3737, 3744);
      XML_NAME_START_CHARS_4TH.set(3745, 3748);
      XML_NAME_START_CHARS_4TH.set(3749);
      XML_NAME_START_CHARS_4TH.set(3751);
      XML_NAME_START_CHARS_4TH.set(3754, 3756);
      XML_NAME_START_CHARS_4TH.set(3757, 3759);
      XML_NAME_START_CHARS_4TH.set(3760);
      XML_NAME_START_CHARS_4TH.set(3762, 3764);
      XML_NAME_START_CHARS_4TH.set(3773);
      XML_NAME_START_CHARS_4TH.set(3776, 3781);
      XML_NAME_START_CHARS_4TH.set(3904, 3912);
      XML_NAME_START_CHARS_4TH.set(3913, 3946);
      XML_NAME_START_CHARS_4TH.set(4256, 4294);
      XML_NAME_START_CHARS_4TH.set(4304, 4343);
      XML_NAME_START_CHARS_4TH.set(4352);
      XML_NAME_START_CHARS_4TH.set(4354, 4356);
      XML_NAME_START_CHARS_4TH.set(4357, 4360);
      XML_NAME_START_CHARS_4TH.set(4361);
      XML_NAME_START_CHARS_4TH.set(4363, 4365);
      XML_NAME_START_CHARS_4TH.set(4366, 4371);
      XML_NAME_START_CHARS_4TH.set(4412);
      XML_NAME_START_CHARS_4TH.set(4414);
      XML_NAME_START_CHARS_4TH.set(4416);
      XML_NAME_START_CHARS_4TH.set(4428);
      XML_NAME_START_CHARS_4TH.set(4430);
      XML_NAME_START_CHARS_4TH.set(4432);
      XML_NAME_START_CHARS_4TH.set(4436, 4438);
      XML_NAME_START_CHARS_4TH.set(4441);
      XML_NAME_START_CHARS_4TH.set(4447, 4450);
      XML_NAME_START_CHARS_4TH.set(4451);
      XML_NAME_START_CHARS_4TH.set(4453);
      XML_NAME_START_CHARS_4TH.set(4455);
      XML_NAME_START_CHARS_4TH.set(4457);
      XML_NAME_START_CHARS_4TH.set(4461, 4463);
      XML_NAME_START_CHARS_4TH.set(4466, 4468);
      XML_NAME_START_CHARS_4TH.set(4469);
      XML_NAME_START_CHARS_4TH.set(4510);
      XML_NAME_START_CHARS_4TH.set(4520);
      XML_NAME_START_CHARS_4TH.set(4523);
      XML_NAME_START_CHARS_4TH.set(4526, 4528);
      XML_NAME_START_CHARS_4TH.set(4535, 4537);
      XML_NAME_START_CHARS_4TH.set(4538);
      XML_NAME_START_CHARS_4TH.set(4540, 4547);
      XML_NAME_START_CHARS_4TH.set(4587);
      XML_NAME_START_CHARS_4TH.set(4592);
      XML_NAME_START_CHARS_4TH.set(4601);
      XML_NAME_START_CHARS_4TH.set(7680, 7836);
      XML_NAME_START_CHARS_4TH.set(7840, 7930);
      XML_NAME_START_CHARS_4TH.set(7936, 7958);
      XML_NAME_START_CHARS_4TH.set(7960, 7966);
      XML_NAME_START_CHARS_4TH.set(7968, 8006);
      XML_NAME_START_CHARS_4TH.set(8008, 8014);
      XML_NAME_START_CHARS_4TH.set(8016, 8024);
      XML_NAME_START_CHARS_4TH.set(8025);
      XML_NAME_START_CHARS_4TH.set(8027);
      XML_NAME_START_CHARS_4TH.set(8029);
      XML_NAME_START_CHARS_4TH.set(8031, 8062);
      XML_NAME_START_CHARS_4TH.set(8064, 8117);
      XML_NAME_START_CHARS_4TH.set(8118, 8125);
      XML_NAME_START_CHARS_4TH.set(8126);
      XML_NAME_START_CHARS_4TH.set(8130, 8133);
      XML_NAME_START_CHARS_4TH.set(8134, 8141);
      XML_NAME_START_CHARS_4TH.set(8144, 8148);
      XML_NAME_START_CHARS_4TH.set(8150, 8156);
      XML_NAME_START_CHARS_4TH.set(8160, 8173);
      XML_NAME_START_CHARS_4TH.set(8178, 8181);
      XML_NAME_START_CHARS_4TH.set(8182, 8189);
      XML_NAME_START_CHARS_4TH.set(8486);
      XML_NAME_START_CHARS_4TH.set(8490, 8492);
      XML_NAME_START_CHARS_4TH.set(8494);
      XML_NAME_START_CHARS_4TH.set(8576, 8579);
      XML_NAME_START_CHARS_4TH.set(12353, 12437);
      XML_NAME_START_CHARS_4TH.set(12449, 12539);
      XML_NAME_START_CHARS_4TH.set(12549, 12589);
      XML_NAME_START_CHARS_4TH.set(12295);
      XML_NAME_START_CHARS_4TH.set(12321, 12330);
      XML_NAME_START_CHARS_4TH.set(19968, 40870);
      XML_NAME_START_CHARS_4TH.set(44032, 55204);
      XML_NAME_START_CHARS_5TH.set(248, 768);
      XML_NAME_START_CHARS_5TH.set(880, 894);
      XML_NAME_START_CHARS_5TH.set(895, 8192);
      XML_NAME_START_CHARS_5TH.set(8204, 8206);
      XML_NAME_START_CHARS_5TH.set(8304, 8592);
      XML_NAME_START_CHARS_5TH.set(11264, 12272);
      XML_NAME_START_CHARS_5TH.set(12289, 55296);
      XML_NAME_START_CHARS_5TH.set(63744, 64976);
      XML_NAME_START_CHARS_5TH.set(65008, 65534);
      XML_NAME_START_CHARS_5TH.set(65536, 983040);
      BitSet XML_NAME_CHARS_4TH = new BitSet(1048575);
      XML_NAME_CHARS_4TH.set(45);
      XML_NAME_CHARS_4TH.set(46);
      XML_NAME_CHARS_4TH.set(48, 58);
      XML_NAME_CHARS_4TH.set(183);
      BitSet XML_NAME_CHARS_5TH = (BitSet)XML_NAME_CHARS_4TH.clone();
      XML_NAME_CHARS_4TH.or(XML_NAME_START_CHARS_4TH);
      XML_NAME_CHARS_4TH.set(720);
      XML_NAME_CHARS_4TH.set(721);
      XML_NAME_CHARS_4TH.set(768, 838);
      XML_NAME_CHARS_4TH.set(864, 866);
      XML_NAME_CHARS_4TH.set(903);
      XML_NAME_CHARS_4TH.set(1155, 1159);
      XML_NAME_CHARS_4TH.set(1425, 1442);
      XML_NAME_CHARS_4TH.set(1443, 1466);
      XML_NAME_CHARS_4TH.set(1467, 1470);
      XML_NAME_CHARS_4TH.set(1471);
      XML_NAME_CHARS_4TH.set(1473, 1475);
      XML_NAME_CHARS_4TH.set(1476);
      XML_NAME_CHARS_4TH.set(1600);
      XML_NAME_CHARS_4TH.set(1611, 1619);
      XML_NAME_CHARS_4TH.set(1632, 1642);
      XML_NAME_CHARS_4TH.set(1648);
      XML_NAME_CHARS_4TH.set(1750, 1757);
      XML_NAME_CHARS_4TH.set(1757, 1760);
      XML_NAME_CHARS_4TH.set(1760, 1765);
      XML_NAME_CHARS_4TH.set(1767, 1769);
      XML_NAME_CHARS_4TH.set(1770, 1774);
      XML_NAME_CHARS_4TH.set(1776, 1786);
      XML_NAME_CHARS_4TH.set(2305, 2308);
      XML_NAME_CHARS_4TH.set(2364);
      XML_NAME_CHARS_4TH.set(2366, 2381);
      XML_NAME_CHARS_4TH.set(2381);
      XML_NAME_CHARS_4TH.set(2385, 2389);
      XML_NAME_CHARS_4TH.set(2402, 2404);
      XML_NAME_CHARS_4TH.set(2406, 2416);
      XML_NAME_CHARS_4TH.set(2433, 2436);
      XML_NAME_CHARS_4TH.set(2492);
      XML_NAME_CHARS_4TH.set(2494);
      XML_NAME_CHARS_4TH.set(2495);
      XML_NAME_CHARS_4TH.set(2496, 2501);
      XML_NAME_CHARS_4TH.set(2503, 2505);
      XML_NAME_CHARS_4TH.set(2507, 2510);
      XML_NAME_CHARS_4TH.set(2519);
      XML_NAME_CHARS_4TH.set(2530, 2532);
      XML_NAME_CHARS_4TH.set(2534, 2544);
      XML_NAME_CHARS_4TH.set(2562);
      XML_NAME_CHARS_4TH.set(2620);
      XML_NAME_CHARS_4TH.set(2622);
      XML_NAME_CHARS_4TH.set(2623);
      XML_NAME_CHARS_4TH.set(2624, 2627);
      XML_NAME_CHARS_4TH.set(2631, 2633);
      XML_NAME_CHARS_4TH.set(2635, 2638);
      XML_NAME_CHARS_4TH.set(2662, 2672);
      XML_NAME_CHARS_4TH.set(2672, 2674);
      XML_NAME_CHARS_4TH.set(2689, 2692);
      XML_NAME_CHARS_4TH.set(2748);
      XML_NAME_CHARS_4TH.set(2750, 2758);
      XML_NAME_CHARS_4TH.set(2759, 2762);
      XML_NAME_CHARS_4TH.set(2763, 2766);
      XML_NAME_CHARS_4TH.set(2790, 2800);
      XML_NAME_CHARS_4TH.set(2817, 2820);
      XML_NAME_CHARS_4TH.set(2876);
      XML_NAME_CHARS_4TH.set(2878, 2884);
      XML_NAME_CHARS_4TH.set(2887, 2889);
      XML_NAME_CHARS_4TH.set(2891, 2894);
      XML_NAME_CHARS_4TH.set(2902, 2904);
      XML_NAME_CHARS_4TH.set(2918, 2928);
      XML_NAME_CHARS_4TH.set(2946, 2948);
      XML_NAME_CHARS_4TH.set(3006, 3011);
      XML_NAME_CHARS_4TH.set(3014, 3017);
      XML_NAME_CHARS_4TH.set(3018, 3022);
      XML_NAME_CHARS_4TH.set(3031);
      XML_NAME_CHARS_4TH.set(3047, 3056);
      XML_NAME_CHARS_4TH.set(3073, 3076);
      XML_NAME_CHARS_4TH.set(3134, 3141);
      XML_NAME_CHARS_4TH.set(3142, 3145);
      XML_NAME_CHARS_4TH.set(3146, 3150);
      XML_NAME_CHARS_4TH.set(3157, 3159);
      XML_NAME_CHARS_4TH.set(3174, 3184);
      XML_NAME_CHARS_4TH.set(3202, 3204);
      XML_NAME_CHARS_4TH.set(3262, 3269);
      XML_NAME_CHARS_4TH.set(3270, 3273);
      XML_NAME_CHARS_4TH.set(3274, 3278);
      XML_NAME_CHARS_4TH.set(3285, 3287);
      XML_NAME_CHARS_4TH.set(3302, 3312);
      XML_NAME_CHARS_4TH.set(3330, 3332);
      XML_NAME_CHARS_4TH.set(3390, 3396);
      XML_NAME_CHARS_4TH.set(3398, 3401);
      XML_NAME_CHARS_4TH.set(3402, 3406);
      XML_NAME_CHARS_4TH.set(3415);
      XML_NAME_CHARS_4TH.set(3430, 3440);
      XML_NAME_CHARS_4TH.set(3633);
      XML_NAME_CHARS_4TH.set(3636, 3643);
      XML_NAME_CHARS_4TH.set(3654);
      XML_NAME_CHARS_4TH.set(3655, 3663);
      XML_NAME_CHARS_4TH.set(3664, 3674);
      XML_NAME_CHARS_4TH.set(3761);
      XML_NAME_CHARS_4TH.set(3764, 3770);
      XML_NAME_CHARS_4TH.set(3771, 3773);
      XML_NAME_CHARS_4TH.set(3782);
      XML_NAME_CHARS_4TH.set(3784, 3790);
      XML_NAME_CHARS_4TH.set(3792, 3802);
      XML_NAME_CHARS_4TH.set(3864, 3866);
      XML_NAME_CHARS_4TH.set(3872, 3882);
      XML_NAME_CHARS_4TH.set(3893);
      XML_NAME_CHARS_4TH.set(3895);
      XML_NAME_CHARS_4TH.set(3897);
      XML_NAME_CHARS_4TH.set(3902);
      XML_NAME_CHARS_4TH.set(3903);
      XML_NAME_CHARS_4TH.set(3953, 3973);
      XML_NAME_CHARS_4TH.set(3974, 3980);
      XML_NAME_CHARS_4TH.set(3984, 3990);
      XML_NAME_CHARS_4TH.set(3991);
      XML_NAME_CHARS_4TH.set(3993, 4014);
      XML_NAME_CHARS_4TH.set(4017, 4024);
      XML_NAME_CHARS_4TH.set(4025);
      XML_NAME_CHARS_4TH.set(8400, 8413);
      XML_NAME_CHARS_4TH.set(8417);
      XML_NAME_CHARS_4TH.set(12293);
      XML_NAME_CHARS_4TH.set(12330, 12336);
      XML_NAME_CHARS_4TH.set(12337, 12342);
      XML_NAME_CHARS_4TH.set(12441);
      XML_NAME_CHARS_4TH.set(12442);
      XML_NAME_CHARS_4TH.set(12445, 12447);
      XML_NAME_CHARS_4TH.set(12540, 12543);
      XML_NAME_CHARS_5TH.or(XML_NAME_START_CHARS_5TH);
      XML_NAME_CHARS_5TH.set(768, 880);
      XML_NAME_CHARS_5TH.set(8255, 8257);
      XML_NAME_START_CHARS = (BitSet)XML_NAME_START_CHARS_4TH.clone();
      XML_NAME_START_CHARS.and(XML_NAME_START_CHARS_5TH);
      XML_NAME_CHARS = (BitSet)XML_NAME_CHARS_4TH.clone();
      XML_NAME_CHARS.and(XML_NAME_CHARS_5TH);
   }
}
