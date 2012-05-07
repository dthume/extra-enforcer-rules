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
public class ScriptRule implements EnforcerRule {
    public final static String RULE_HELPER_KEY = "mavenEnforcerRuleHelper";
    
    /**
     * Inline script source
     */
    private String script = null;
    
    /** The scripting language to use, defaults to "javascript" */
    private String language = "javascript";
    
    public void setScript(String script) { this.script = script; }
    
    public void setLanguage(String lang) { this.language = lang; }
    
    public boolean isCacheable() {
        return false;
    }

    public boolean isResultValid(EnforcerRule cachedRule) {
        return false;
    }

    public String getCacheId() {
        return null;
    }
    
    public void execute(final EnforcerRuleHelper helper)
            throws EnforcerRuleException {
        new Handler(helper).execute();
    }
    
    private class Handler {
        final EnforcerRuleHelper helper;
        final Log log;

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
                throw new EnforcerRuleException("Script rule evaluated to false");
        }
        
        private boolean isValidResult(final Object result) {
            if (null == result)
                return false;
            if (result instanceof Boolean)
                return ((Boolean)result).booleanValue();
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
