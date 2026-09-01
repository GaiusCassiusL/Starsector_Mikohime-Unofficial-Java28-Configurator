package com.thoughtworks.xstream.io;

import com.thoughtworks.xstream.converters.ErrorReporter;
import com.thoughtworks.xstream.converters.ErrorWriter;
import java.util.Iterator;

public interface HierarchicalStreamReader extends ErrorReporter {
   boolean hasMoreChildren();

   void moveDown();

   void moveUp();

   String getNodeName();

   String getValue();

   String getAttribute(String var1);

   String getAttribute(int var1);

   int getAttributeCount();

   String getAttributeName(int var1);

   Iterator getAttributeNames();

   void appendErrors(ErrorWriter var1);

   void close();

   HierarchicalStreamReader underlyingReader();
}
