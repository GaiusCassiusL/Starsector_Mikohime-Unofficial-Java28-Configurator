package com.thoughtworks.xstream.io.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.List;

public interface DocumentWriter extends HierarchicalStreamWriter {
   List getTopLevelNodes();
}
