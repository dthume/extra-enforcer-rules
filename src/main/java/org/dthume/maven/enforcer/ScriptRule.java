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

/**
 * An {@link EnforcerRule} which evaluates a JSR223 compliant script.
 * The {@link EnforcerRuleHelper} will be bound into the evaluation
 * context under the key {@link #RULE_HELPER_KEY}.
 *
 * @author dth
 */
public final class ScriptRule implements EnforcerRule {
    /** The key to bind the {@link EnforcerRuleHelper} to. */
    public static final String RULE_HELPER_KEY = "mavenEnforcerRuleHelper";

    /**
     * Inline script source.
     */
    private String script = null;

    /** The scripting language to use, defaults to "javascript". */
    private String language = "javascript";

    /**
     * Set the inline script source to use.
     *
     * @param script the inline script source to use.
     */
    public void setScript(final String script) { this.script = script; }

    /**
     * Set the scripting language to use.
     *
     * @param lang the scripting language to use.
     */
    public void setLanguage(final String lang) { this.language = lang; }

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
            validate();
            final Object result = executeScript();
            verifyResult(result);
        }

        private void validate() throws EnforcerRuleException {
            // TODO - validate config
        }

        private Object executeScript() throws EnforcerRuleException {
            try {
                return executeScriptInternal();
            } catch (ScriptException e) {
                throw new EnforcerRuleException("Script Exception", e);
            }
        }

        private Object executeScriptInternal() throws ScriptException {
            final ScriptEngine engine = createEngine();
            final ScriptContext context = createScriptContext();
            return engine.eval(script, context);
        }

        private void verifyResult(final Object result)
                throws EnforcerRuleException {
            if (!isValidResult(result))
                throw new EnforcerRuleException("Script evaluated to false");
        }

        private boolean isValidResult(final Object result) {
            if (null == result)
                return false;
            if (result instanceof Boolean)
                return ((Boolean) result).booleanValue();
            if (result instanceof Integer)
                return 0 != ((Integer) result).intValue();
            if (result instanceof Long)
                return 0L != ((Long) result).longValue();
            if (result instanceof Float)
                return 0.0 != ((Float) result).floatValue();
            if (result instanceof Double)
                return 0.0D != ((Double) result).doubleValue();
            return true;
        }

        private ScriptEngine createEngine() {
            return new ScriptEngineManager().getEngineByName(language);
        }

        private ScriptContext createScriptContext() {
            final SimpleScriptContext context = new SimpleScriptContext();

            final Bindings bindings =
                    context.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put(RULE_HELPER_KEY, helper);

            return context;
        }
    }
}
