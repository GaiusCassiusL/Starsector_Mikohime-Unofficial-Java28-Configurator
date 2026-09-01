package com.thoughtworks.xstream.io;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

public interface HierarchicalStreamDriver {
   HierarchicalStreamReader createReader(Reader var1);

   HierarchicalStreamReader createReader(InputStream var1);

   HierarchicalStreamReader createReader(URL var1);

   HierarchicalStreamReader createReader(File var1);

   HierarchicalStreamWriter createWriter(Writer var1);

   HierarchicalStreamWriter createWriter(OutputStream var1);
}
