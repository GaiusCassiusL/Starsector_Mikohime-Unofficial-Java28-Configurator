package org.apache.logging.log4j.core.pattern;

public interface TextRenderer {
   void render(String input, StringBuilder output, String styleName);

   void render(StringBuilder input, StringBuilder output);
}
