package org.apache.logging.log4j.core.script;

import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.core.appender.rolling.action.PathWithAttributes;

public interface ScriptConditional {
   List<PathWithAttributes> selectFilesToDelete(final Path basePath, final List<PathWithAttributes> candidates);
}
