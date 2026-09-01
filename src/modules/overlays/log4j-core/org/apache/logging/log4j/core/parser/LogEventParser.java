package org.apache.logging.log4j.core.parser;

import org.apache.logging.log4j.core.LogEvent;

public interface LogEventParser {
   LogEvent parseFrom(byte[] input) throws ParseException;

   LogEvent parseFrom(byte[] input, int offset, int length) throws ParseException;
}
