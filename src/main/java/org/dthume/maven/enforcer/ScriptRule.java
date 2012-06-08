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

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.dthume.maven.util.LogWriter.LogLevel.ERROR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.dthume.maven.util.LogWriter;

/**
 * An {@link EnforcerRule} which evaluates a JSR223 compliant script.
 *
 * @author dth
 */
public final class ScriptRule implements EnforcerRule {
    /** The expression to get the user specified source file encoding. */
    private static final String SOURCE_ENCODING =
            "${project.build.sourceEncoding}";

    /** The key to bind the rule helper to during script evaluation. */
    private String ruleHelperKey = null;

    /** The scripting language to use, defaults to "javascript". */
    private String language = "javascript";

    /** Inline script source. */
    private String script = null;

    /** The file containing the script to execute. */
    private File scriptFile = null;

    /** The key to bind the validation context object to. */
    private String validationContextKey = "ruleContext";

    /** The validation context object, if this rule supports caching. */
    private final Object validationContext = new HashMap<Object, Object>();

    /** Cached instance of the rule helper, to support rule caching. */
    private EnforcerRuleHelper cachedHelper = null;

    /** Inline script to use to validate previous executions. */
    private String validatorScript = null;

    /** File containing a script to use to validate previous executions. */
    private File validatorScriptFile = null;

    /** The result evaluator - determines if the script result is valid. */
    private ScriptResultEvaluator resultEvaluator =
            new DefaultScriptResultEvaluator();

    /** The map of values to bind into the script evaluation context. */
    private Map<String, Object> scriptBindings =
            java.util.Collections.emptyMap();

    /** The message to place into the exception upon rule failure. */
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

    /**
     * Set the key to bind the validation context map instance to during
     * script evaluation.
     *
     * @param validationContextKey the key to bind the validation context to.
     */
    public void setValidationContextKey(String validationContextKey) {
        this.validationContextKey = validationContextKey;
    }

    /**
     * Set an inline script to use to validate previous rule executions.
     * 
     * NOTE: Do not specify both this <i>and</i> {@link #validatorScriptFile}.
     * 
     * @param validatorScript the script source to use for validation.
     */
    public void setValidatorScript(String validatorScript) {
        this.validatorScript = validatorScript;
    }

    /**
     * Set a script file to use to validate previous rule executions
     *
     * NOTE: Do not specify both this <i>and</i> {@link #validatorScript}.
     *
     * @param validatorScriptFile the script file to use for validation.
     */
    public void setValidatorScriptFile(File validatorScriptFile) {
        this.validatorScriptFile = validatorScriptFile;
    }

    /** {@inheritDoc} */
    public boolean isCacheable() {
        return !(null == validatorScriptFile && isBlank(validatorScript));
    }

    /** {@inheritDoc} */
    public boolean isResultValid(final EnforcerRule cached) {
        validateConfig();

        if (!(isCacheable() && cached instanceof ScriptRule))
            return false;

        final ScriptRule rule = (ScriptRule) cached;
        try {
            return new Handler(rule.cachedHelper,
                    rule.validatorScript,
                    rule.validatorScriptFile,
                    rule.validationContext)
                .execute();
        } catch (EnforcerRuleException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public String getCacheId() {
        if (!isCacheable()) return java.util.UUID.randomUUID().toString();
        
        return md5Hex(toCacheId(
                "language", language,
                "script", script,
                "scriptFile", toPathOrNull(scriptFile),
                "validatorScript", validatorScript,
                "validatorScriptFile", toPathOrNull(validatorScriptFile),
                "message", message,
                "ruleHelperKey", ruleHelperKey,
                "validationContextKey", validationContextKey,
                "scriptBindings", toCacheId(scriptBindings)));
    }
    
    private String toPathOrNull(final File file) {
        return null == file ? null : file.getAbsolutePath();
    }

    private String toCacheId(final Object...params) {
        final StringBuilder sb = new StringBuilder();
        for (int ii = 0; ii < params.length; ii+=2) {
            if (0 < ii) sb.append(",");

            sb.append(params[ii])
                .append("=<[[")
                .append(params[ii+1])
                .append("]]>");
        }
        
        return sb.toString();
    }
    
    private String toCacheId(Map<String, Object> map) {
        final List<String> keys = new ArrayList<String>(map.keySet());
        java.util.Collections.sort(keys);
        
        final List<String> values = new ArrayList<String>(keys.size());
        for (final String key : keys)
            values.add(key + "=" + map.get(key));
        
        return new StringBuilder("{")
            .append(join(values, ","))
            .append("}")
            .toString();
    }

    /** {@inheritDoc} */
    public void execute(final EnforcerRuleHelper helper)
            throws EnforcerRuleException {
        cachedHelper = helper;

        validateConfig();

        final Handler handler =
                new Handler(helper, script, scriptFile, validationContext);

        if (!handler.execute())
            throw new EnforcerRuleException(message);
    }

    private void validateConfig() throws IllegalArgumentException {
        String msg = null;

        if (null == scriptFile && isBlank(script))
            msg = "One of script or scriptFile must be set";
        if (!(null == scriptFile || isBlank(script)))
            msg = "Cannot set both scriptFile and script";
        if (!(null == validatorScriptFile || isBlank(validatorScript)))
            msg = "Cannot set both validatorScriptFile and validatorScript";

        if (null != msg) throw new IllegalArgumentException(msg);
    }

    private class Handler {
        private final EnforcerRuleHelper helper;
        private final Log log;
        private final String script;
        private final File scriptFile;
        private final Object validationContext;

        Handler(final EnforcerRuleHelper helper,
                final String script,
                final File file,
                final Object validationContext) {
            this.helper = helper;
            this.log = helper.getLog();
            this.script = script;
            this.scriptFile = file;
            this.validationContext = validationContext;
        }

        public boolean execute() throws EnforcerRuleException {
            final Object result = executeScript();

            if (log.isDebugEnabled())
                log.debug("Script result: " + result);
            
            return resultEvaluator.isValidResult(result);
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
        
        private void configureBindings(final ScriptContext context) {
            final boolean debug = log.isDebugEnabled();
            final Bindings bindings =
                    context.getBindings(ScriptContext.ENGINE_SCOPE);
            
            for (final Map.Entry<String, Object> entry
                    : scriptBindings.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();

                if (debug)
                    log.debug(String.format("Binding %s=%s", key, value));

                bindings.put(key, value);
            }

            if (!isBlank(ruleHelperKey)) {
                if (debug)
                    log.debug("Binding rule helper to key: " + ruleHelperKey);

                bindings.put(ruleHelperKey, helper);
            }

            if (isCacheable()) {
                if (debug)
                    log.debug("Binding validation context to key: "
                            + validationContextKey);

                bindings.put(validationContextKey, validationContext);
            }
        }

        private void configureIO(ScriptContext context) {
            context.setWriter(new LogWriter(log));
            context.setErrorWriter(new LogWriter(log, ERROR));
        }
    }
}
