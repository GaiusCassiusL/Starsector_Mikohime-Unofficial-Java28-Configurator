package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class XomDriver extends AbstractXmlDriver {
   private final Builder builder;

   public XomDriver() {
      this(new XmlFriendlyNameCoder());
   }

   /** @deprecated */
   public XomDriver(Builder builder) {
      this(builder, new XmlFriendlyNameCoder());
   }

   public XomDriver(NameCoder nameCoder) {
      super(nameCoder);
      this.builder = null;
   }

   /** @deprecated */
   public XomDriver(Builder builder, NameCoder nameCoder) {
      super(nameCoder);
      this.builder = builder;
   }

   /** @deprecated */
   public XomDriver(XmlFriendlyReplacer replacer) {
      this((NameCoder)replacer);
   }

   /** @deprecated */
   public XomDriver(Builder builder, XmlFriendlyReplacer replacer) {
      this(builder, (NameCoder)replacer);
   }

   /** @deprecated */
   protected Builder getBuilder() {
      return this.builder;
   }

   protected Builder createBuilder() {
      Builder builder = this.getBuilder();
      return builder != null ? builder : new Builder();
   }

   public HierarchicalStreamReader createReader(Reader text) {
      try {
         Document document = this.createBuilder().build(text);
         return new XomReader(document, this.getNameCoder());
      } catch (ValidityException e) {
         throw new StreamException(e);
      } catch (ParsingException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(InputStream in) {
      try {
         Document document = this.createBuilder().build(in);
         return new XomReader(document, this.getNameCoder());
      } catch (ValidityException e) {
         throw new StreamException(e);
      } catch (ParsingException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(URL in) {
      try {
         Document document = this.createBuilder().build(in.toExternalForm());
         return new XomReader(document, this.getNameCoder());
      } catch (ValidityException e) {
         throw new StreamException(e);
      } catch (ParsingException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamReader createReader(File in) {
      try {
         Document document = this.createBuilder().build(in);
         return new XomReader(document, this.getNameCoder());
      } catch (ValidityException e) {
         throw new StreamException(e);
      } catch (ParsingException e) {
         throw new StreamException(e);
      } catch (IOException e) {
         throw new StreamException(e);
      }
   }

   public HierarchicalStreamWriter createWriter(Writer out) {
      return new PrettyPrintWriter(out, this.getNameCoder());
   }

   public HierarchicalStreamWriter createWriter(OutputStream out) {
      return new PrettyPrintWriter(new OutputStreamWriter(out), this.getNameCoder());
   }
}
