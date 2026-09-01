package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathConverter extends AbstractSingleValueConverter {
   @Override
   public boolean canConvert(Class type) {
      return type != null && Path.class.isAssignableFrom(type);
   }

   public Path fromString(String str) {
      try {
         try {
            URI uri = new URI(str);
            return uri.getScheme() != null && uri.getScheme().length() != 1
               ? Paths.get(uri)
               : Paths.get(File.separatorChar != '/' ? str.replace('/', File.separatorChar) : str);
         } catch (URISyntaxException e) {
            return Paths.get(str);
         }
      } catch (InvalidPathException e) {
         throw new ConversionException(e);
      }
   }

   @Override
   public String toString(Object obj) {
      Path path = (Path)obj;
      if (path.getFileSystem() == FileSystems.getDefault()) {
         String localPath = path.toString();
         return File.separatorChar != '/' ? localPath.replace(File.separatorChar, '/') : localPath;
      } else {
         return path.toUri().toString();
      }
   }
}
