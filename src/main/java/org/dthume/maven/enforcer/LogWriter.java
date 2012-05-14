/**
 * Copyright (C) 2011 David Thomas Hume <dth@dthu.me>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dthume.maven.enforcer;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;

public class LogWriter extends Writer {
    
    public static enum LogLevel {
        DEBUG {
            boolean isEnabled(Log log) { return log.isDebugEnabled(); }
            void write(Log log, String line) { log.debug(line); }
        },
        INFO {
            boolean isEnabled(Log log) { return log.isInfoEnabled(); }
            void write(Log log, String line) { log.info(line); }
        },
        WARN {
            boolean isEnabled(Log log) { return log.isWarnEnabled(); }
            void write(Log log, String line) { log.warn(line); }
        },
        ERROR {
            boolean isEnabled(Log log) { return log.isErrorEnabled(); }
            void write(Log log, String line) { log.error(line); }
        };
        
        abstract boolean isEnabled(Log log);
        abstract void write(Log log, String line);
    }
    
    private final static Pattern NEWLINES = Pattern.compile("\\n");
    
    private final Log log;
    private final LogLevel level;
    private final StringBuffer buffer = new StringBuffer();
    
    public LogWriter(Log log) {
        this(log, LogLevel.INFO);
    }
    
    public LogWriter(Log log, LogLevel level) {
        this.log = log;
        this.level = level;
    }
    
    @Override
    public void write(char[] buf, int off, int len) throws IOException {
        buffer.append(buf, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (level.isEnabled(log))
            for (final String line : NEWLINES.split(buffer))
                level.write(log, line);
        
        buffer.setLength(0);
    }
    
    @Override
    public void close() throws IOException {
        flush();
    }
}
