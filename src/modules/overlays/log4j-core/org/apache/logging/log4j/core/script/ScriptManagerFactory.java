package org.apache.logging.log4j.core.script;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.WatchManager;

public interface ScriptManagerFactory {
   ScriptManager createScriptManager(Configuration configuration, WatchManager watchManager);
}
