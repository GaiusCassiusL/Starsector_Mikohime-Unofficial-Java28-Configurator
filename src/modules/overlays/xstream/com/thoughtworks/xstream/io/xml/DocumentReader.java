package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public interface DocumentReader extends HierarchicalStreamReader {
   Object getCurrent();
}
