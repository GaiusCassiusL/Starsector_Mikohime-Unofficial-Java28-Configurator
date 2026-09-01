package org.apache.logging.log4j.core.pattern;

public final class PlainTextRenderer implements TextRenderer {
   private static final PlainTextRenderer INSTANCE = new PlainTextRenderer();

   public static PlainTextRenderer getInstance() {
      return INSTANCE;
   }

   @Override
   public void render(final String input, final StringBuilder output, final String styleName) {
      output.append(input);
   }

   @Override
   public void render(final StringBuilder input, final StringBuilder output) {
      output.append(input);
   }
}
