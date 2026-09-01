package org.apache.logging.log4j.core.pattern;

public interface ArrayPatternConverter extends PatternConverter {
   void format(final StringBuilder toAppendTo, Object... objects);
}
