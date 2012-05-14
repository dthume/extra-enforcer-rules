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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.dthume.maven.enforcer.LogWriter.LogLevel;

/**
 * An {@link EnforcerRule} which evaluates a JSR223 compliant script.
 *
 * @author dth
 */
public final class ScriptRule implements EnforcerRule {
    /** The expression to get the user specified source file encoding. */
    private static final String SOURCE_ENCODING =
            "${project.build.sourceEncoding}";

    /** The key to bind the rule helper to during script evaluation */
    private String ruleHelperKey = null;
    
    /** The scripting language to use, defaults to "javascript". */
    private String language = "javascript";
    
    /** Inline script source. */
    private String script = null;

    /** The file containing the script to execute. */
    private File scriptFile = null;
    
    /** The result evaluator - determines if the script result is valid */
    private ScriptResultEvaluator resultEvaluator =
            new DefaultScriptResultEvaluator();
    
    /** The map of values to bind into the script evaluation context */
    private Map<String, Object> scriptBindings =
            java.util.Collections.emptyMap();
    
    /** The message to place into the exception upon rule failure */
    private String message = "Script evaluated to false";
    
    /**
     * Set the key to bind the {@link EnforcerRuleHelper} to.
     * 
     * @param key the name to bind the {@code EnforcerRuleHelper} to.
     */
    public void setRuleHelperKey(String key) { ruleHelperKey = key; }

    /**
     * Set the script source to evaluate.
     * 
     * NOTE: Do not specify both this <i>and</i> {@link #scriptFile}.
     *
     * @param script the inline script source to use.
     */
    public void setScript(final String script) { this.script = script; }

    /**
     * Set the file containing the script to evaluate.
     * 
     * NOTE: Do not specify both this <i>and</i> {@link #script}.
     *
     * @param file the inline script source to use.
     */
    public void setScriptFile(final File file) { this.scriptFile = file; }
    
    /**
     * Set the scripting language to use.
     *
     * @param lang the scripting language to use.
     */
    public void setLanguage(final String lang) { this.language = lang; }

    /**
     * Set the evaluator to use to determine if the script result is valid 
     * 
     * @param evaluator the result evaluator to use
     */
    public void setResultEvaluator(ScriptResultEvaluator evaluator) {
        this.resultEvaluator = evaluator;
    }
    
    /**
     * Set the map of bindings to apply during script evaluation 
     * 
     * @param bindings the map of bindings to apply during script evaluation
     */
    public void setScriptBindings(Map<String, Object> bindings) {
        this.scriptBindings = bindings;
    }
    
    /**
     * Set the message to be placed into the {@link EnforcerRuleException}
     * if script evaluation returns {@code false};
     * 
     * @param message the message to place into the exception on rule failure
     */
    public void setMessage(String message) { this.message = message; }
    
    /** {@inheritDoc} */
    public boolean isCacheable() { return false; }

    /** {@inheritDoc} */
    public boolean isResultValid(final EnforcerRule cached) { return false; }

    /** {@inheritDoc} */
    public String getCacheId() { return null; }

    /** {@inheritDoc} */
    public void execute(final EnforcerRuleHelper helper)
            throws EnforcerRuleException {
        new Handler(helper).execute();
    }

    private class Handler {
        private final EnforcerRuleHelper helper;
        private final Log log;

        Handler(final EnforcerRuleHelper helper) {
            this.helper = helper;
            this.log = helper.getLog();
        }

        private void execute() throws EnforcerRuleException {
            validateConfig();
            
            final Object result = executeScript();

            if (log.isDebugEnabled())
                log.debug("Script result: " + result);
            
            if (!resultEvaluator.isValidResult(result))
                throw new EnforcerRuleException(message);
        }
        
        private void validateConfig() throws IllegalArgumentException {
            String msg = null;
            
            if (null == scriptFile && isBlank(script))
                msg = "One of script or scriptFile must be set";
            if (!(null == scriptFile || isBlank(script)))
                msg = "Cannot set both scriptFile and script";
            
            if (null != msg) throw new IllegalArgumentException(msg);
        }

        private Object executeScript() throws EnforcerRuleException {
            try {
                final ScriptEngine engine = createEngine();
                final ScriptContext context = createScriptContext();
                final Reader reader = getScriptReader();
                
                return engine.eval(reader, context);
            } catch (ScriptException e) {
                throw new EnforcerRuleException("Script Exception", e);
            }
        }
        
        private Reader getScriptReader() throws EnforcerRuleException {
            Reader reader = null;
            if (isBlank(script)) {
                if (log.isDebugEnabled())
                    log.debug("Using script file: " + scriptFile);

                reader = getScriptFileReader();
            } else {
                log.debug("Using inline script");
                reader = new StringReader(script);
            }
            return reader;
        }
        
        private Reader getScriptFileReader() throws EnforcerRuleException {
            try {
                final InputStream in = new FileInputStream(scriptFile);
                return new InputStreamReader(in, getSourceEncoding());
            } catch (FileNotFoundException e) {
                throw new EnforcerRuleException("Script file not found", e);
            } catch (UnsupportedEncodingException e) {
                throw new EnforcerRuleException("Unsupported encoding", e);
            }
        }
        
        private String getSourceEncoding() {
            String encoding = null;
            try {
                encoding = (String)helper.evaluate(SOURCE_ENCODING);
            } catch (ExpressionEvaluationException e) {
                log.debug("Caught exception looking up source encoding", e);
            }
            
            if (isBlank(encoding)) {
                log.debug("Using platform encoding");
                encoding = java.nio.charset.Charset.defaultCharset().name();
            }
            
            if (log.isDebugEnabled())
                log.debug("Encoding to use for source files: " + encoding);
            
            return encoding;
        }

        private ScriptEngine createEngine() {
            return new ScriptEngineManager().getEngineByName(language);
        }

        private ScriptContext createScriptContext() {
            final SimpleScriptContext context = new SimpleScriptContext();
            configureIO(context);
            configureBindings(context);
            return context;
        }
        
        private void configureBindings(ScriptContext context) {
            final Bindings bindings =
                    context.getBindings(ScriptContext.ENGINE_SCOPE);
            
            if (!isBlank(ruleHelperKey)) {
                if (log.isDebugEnabled())
                    log.debug("Binding rule helper to key: " + ruleHelperKey);
                
                bindings.put(ruleHelperKey, helper);
            }
            
            for (Map.Entry<String, Object> entry : scriptBindings.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                
                if (log.isDebugEnabled())
                    log.debug(String.format("Binding %s=%s", key, value));
                
                bindings.put(key, value);
            }
        }
        
        private void configureIO(ScriptContext context) {
            context.setWriter(new LogWriter(log));
            context.setErrorWriter(new LogWriter(log, LogLevel.ERROR));
        }
    }
}
