package org.apache.logging.log4j.core.script;

import java.util.Set;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Key;

public interface ScriptManager {
   Key<ScriptManager> KEY = new Key<ScriptManager>() {};

   void addScripts(Node child);

   boolean addScript(final Script script);

   boolean isScriptRef(final Script script);

   Set<String> getAllowedLanguages();

   ScriptBindings createBindings(final Script script);

   Script getScript(final String name);

   Object execute(final String name, final ScriptBindings bindings);
}
