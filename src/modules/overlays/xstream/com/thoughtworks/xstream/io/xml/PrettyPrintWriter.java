package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.io.Writer;

public class PrettyPrintWriter extends AbstractXmlWriter {
   public static int XML_QUIRKS = -1;
   public static int XML_1_0 = 0;
   public static int XML_1_1 = 1;
   public static int XML_1_0_REPLACEMENT = 2;
   public static int XML_1_1_REPLACEMENT = 3;
   private final QuickWriter writer;
   private final FastStack elementStack = new FastStack(16);
   private final char[] lineIndenter;
   private final int mode;
   private boolean tagInProgress;
   protected int depth;
   private boolean readyForNewLine;
   private boolean tagIsEmpty;
   private String newLine;
   private static final char[] NULL = "&#x0;".toCharArray();
   private static final char[] AMP = "&amp;".toCharArray();
   private static final char[] LT = "&lt;".toCharArray();
   private static final char[] GT = "&gt;".toCharArray();
   private static final char[] CR = "&#xd;".toCharArray();
   private static final char[] QUOT = "&quot;".toCharArray();
   private static final char[] APOS = "&apos;".toCharArray();
   private static final char[] CLOSE = "</".toCharArray();
   private static final char[] REPLACEMENT = "&#xfffd;".toCharArray();

   private PrettyPrintWriter(Writer writer, int mode, char[] lineIndenter, NameCoder nameCoder, String newLine) {
      super(nameCoder);
      this.writer = new QuickWriter(writer);
      this.lineIndenter = lineIndenter;
      this.newLine = newLine;
      this.mode = mode;
      if (mode < XML_QUIRKS || mode > XML_1_1_REPLACEMENT) {
         throw new IllegalArgumentException("Not a valid XML mode: " + mode);
      }
   }

   /** @deprecated */
   public PrettyPrintWriter(Writer writer, char[] lineIndenter, String newLine, XmlFriendlyReplacer replacer) {
      this(writer, XML_QUIRKS, lineIndenter, replacer, newLine);
   }

   public PrettyPrintWriter(Writer writer, int mode, char[] lineIndenter, NameCoder nameCoder) {
      this(writer, mode, lineIndenter, nameCoder, "\n");
   }

   /** @deprecated */
   public PrettyPrintWriter(Writer writer, int mode, char[] lineIndenter, XmlFriendlyReplacer replacer) {
      this(writer, mode, lineIndenter, replacer, "\n");
   }

   /** @deprecated */
   public PrettyPrintWriter(Writer writer, char[] lineIndenter, String newLine) {
      this(writer, lineIndenter, newLine, new XmlFriendlyReplacer());
   }

   public PrettyPrintWriter(Writer writer, int mode, char[] lineIndenter) {
      this(writer, mode, lineIndenter, new XmlFriendlyNameCoder());
   }

   public PrettyPrintWriter(Writer writer, char[] lineIndenter) {
      this(writer, XML_QUIRKS, lineIndenter);
   }

   /** @deprecated */
   public PrettyPrintWriter(Writer writer, String lineIndenter, String newLine) {
      this(writer, lineIndenter.toCharArray(), newLine);
   }

   public PrettyPrintWriter(Writer writer, int mode, String lineIndenter) {
      this(writer, mode, lineIndenter.toCharArray());
   }

   public PrettyPrintWriter(Writer writer, String lineIndenter) {
      this(writer, lineIndenter.toCharArray());
   }

   public PrettyPrintWriter(Writer writer, int mode, NameCoder nameCoder) {
      this(writer, mode, new char[]{' ', ' '}, nameCoder);
   }

   /** @deprecated */
   public PrettyPrintWriter(Writer writer, int mode, XmlFriendlyReplacer replacer) {
      this(writer, mode, new char[]{' ', ' '}, replacer);
   }

   public PrettyPrintWriter(Writer writer, NameCoder nameCoder) {
      this(writer, XML_QUIRKS, new char[]{' ', ' '}, nameCoder, "\n");
   }

   /** @deprecated */
   public PrettyPrintWriter(Writer writer, XmlFriendlyReplacer replacer) {
      this(writer, new char[]{' ', ' '}, "\n", replacer);
   }

   public PrettyPrintWriter(Writer writer, int mode) {
      this(writer, mode, new char[]{' ', ' '});
   }

   public PrettyPrintWriter(Writer writer) {
      this(writer, new char[]{' ', ' '});
   }

   public void startNode(String name) {
      String escapedName = this.encodeNode(name);
      this.tagIsEmpty = false;
      this.finishTag();
      this.writer.write('<');
      this.writer.write(escapedName);
      this.elementStack.push(escapedName);
      this.tagInProgress = true;
      this.depth++;
      this.readyForNewLine = true;
      this.tagIsEmpty = true;
   }

   public void startNode(String name, Class clazz) {
      this.startNode(name);
   }

   public void setValue(String text) {
      this.readyForNewLine = false;
      this.tagIsEmpty = false;
      this.finishTag();
      this.writeText(this.writer, text);
   }

   public void addAttribute(String key, String value) {
      this.writer.write(' ');
      this.writer.write(this.encodeAttribute(key));
      this.writer.write('=');
      this.writer.write('"');
      this.writeAttributeValue(this.writer, value);
      this.writer.write('"');
   }

   protected void writeAttributeValue(QuickWriter writer, String text) {
      this.writeText(text, true);
   }

   protected void writeText(QuickWriter writer, String text) {
      this.writeText(text, false);
   }

   private void writeText(String text, boolean isAttribute) {
      int length = text.length();

      for (int i = 0; i < length; i++) {
         char c = text.charAt(i);
         switch (c) {
            case '\u0000':
               if (this.mode == XML_QUIRKS) {
                  this.writer.write(NULL);
               } else {
                  if (this.mode != XML_1_0_REPLACEMENT && this.mode != XML_1_1_REPLACEMENT) {
                     throw new StreamException("Invalid character 0x0 in XML stream");
                  }

                  this.writer.write(REPLACEMENT);
               }
               break;
            case '\t':
            case '\n':
               if (!isAttribute) {
                  this.writer.write(c);
                  break;
               }
            default:
               if (Character.isDefined(c) && !Character.isISOControl(c)) {
                  boolean replaced = false;
                  if (this.mode != XML_QUIRKS && c > '\ud7ff' && c < '\ue000') {
                     if (this.mode != XML_1_0_REPLACEMENT && this.mode != XML_1_1_REPLACEMENT) {
                        throw new StreamException("Invalid character 0x" + Integer.toHexString(c) + " in XML stream");
                     }

                     this.writer.write(REPLACEMENT);
                     replaced = true;
                  }

                  if (!replaced) {
                     this.writer.write(c);
                  }
               } else {
                  boolean replaced = false;
                  if ((this.mode == XML_1_0 || this.mode == XML_1_0_REPLACEMENT) && (c < '\t' || c == 11 || c == '\f' || c == 14 || c >= 15 && c <= 31)) {
                     if (this.mode != XML_1_0_REPLACEMENT) {
                        throw new StreamException("Invalid character 0x" + Integer.toHexString(c) + " in XML 1.0 stream");
                     }

                     this.writer.write(REPLACEMENT);
                     replaced = true;
                  }

                  if (this.mode != XML_QUIRKS && (c == '\ufffe' || c == '\uffff')) {
                     if (this.mode != XML_1_0_REPLACEMENT && this.mode != XML_1_1_REPLACEMENT) {
                        throw new StreamException("Invalid character 0x" + Integer.toHexString(c) + " in XML stream");
                     }

                     this.writer.write(REPLACEMENT);
                     replaced = true;
                  }

                  if (!replaced) {
                     this.writer.write("&#x");
                     this.writer.write(Integer.toHexString(c));
                     this.writer.write(';');
                  }
               }
               break;
            case '\r':
               this.writer.write(CR);
               break;
            case '"':
               this.writer.write(QUOT);
               break;
            case '&':
               this.writer.write(AMP);
               break;
            case '\'':
               this.writer.write(APOS);
               break;
            case '<':
               this.writer.write(LT);
               break;
            case '>':
               this.writer.write(GT);
         }
      }
   }

   public void endNode() {
      this.depth--;
      if (this.tagIsEmpty) {
         this.writer.write('/');
         this.readyForNewLine = false;
         this.finishTag();
         this.elementStack.popSilently();
      } else {
         this.finishTag();
         this.writer.write(CLOSE);
         this.writer.write((String)this.elementStack.pop());
         this.writer.write('>');
      }

      this.readyForNewLine = true;
      if (this.depth == 0) {
         this.writer.flush();
      }
   }

   private void finishTag() {
      if (this.tagInProgress) {
         this.writer.write('>');
      }

      this.tagInProgress = false;
      if (this.readyForNewLine) {
         this.endOfLine();
      }

      this.readyForNewLine = false;
      this.tagIsEmpty = false;
   }

   protected void endOfLine() {
      this.writer.write(this.getNewLine());

      for (int i = 0; i < this.depth; i++) {
         this.writer.write(this.lineIndenter);
      }
   }

   public void flush() {
      this.writer.flush();
   }

   public void close() {
      this.writer.close();
   }

   protected String getNewLine() {
      return this.newLine;
   }
}
