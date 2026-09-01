package org.apache.logging.log4j.core.config.builder.api;

public interface ScriptFileComponentBuilder extends ComponentBuilder<ScriptFileComponentBuilder> {
   ScriptFileComponentBuilder addLanguage(String language);

   ScriptFileComponentBuilder addIsWatched(boolean isWatched);

   ScriptFileComponentBuilder addIsWatched(String isWatched);

   ScriptFileComponentBuilder addCharset(String charset);
}
