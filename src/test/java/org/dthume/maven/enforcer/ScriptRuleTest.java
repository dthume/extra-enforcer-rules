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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

public class ScriptRuleTest {

    private ScriptRule newJSRule() {
        final ScriptRule rule = new ScriptRule();
        rule.setLanguage("javascript");
        return rule;
    }
    
    private ScriptRule newInlineJSRule(String script) {
        final ScriptRule rule = newJSRule();
        rule.setScript(script);
        return rule;
    }
    
    private EnforcerRuleHelper mockHelper() {
        final EnforcerRuleHelper helper = mock(EnforcerRuleHelper.class);
        when(helper.getLog()).thenReturn(mock(Log.class));
        return helper;
    }
    
    private void executeInlineJSRule(String script) throws Exception {
        executeInlineJSRule(mockHelper(), script);
    }
    
    private void executeInlineJSRule(EnforcerRuleHelper helper, String script)
            throws Exception {
        newInlineJSRule(script).execute(helper);
    }
    
    @Test(expected = EnforcerRuleException.class)
    public void falseInlineScriptShouldFail() throws Exception {
        executeInlineJSRule("false;");
    }

    @Test(expected = EnforcerRuleException.class)
    public void nullInlineScriptShouldFail() throws Exception {
        executeInlineJSRule("null;");
    }
    
    @Test
    public void trueInlineScriptShouldPass() 
            throws Exception {
        executeInlineJSRule("true;");        
    }

    @Test(expected = EnforcerRuleException.class)
    public void zeroInlineScriptShouldFail() throws Exception {
        executeInlineJSRule("0;");
    }

    @Test
    public void integerOneInlineScriptShouldPass() 
            throws Exception {
        executeInlineJSRule("1;");        
    }
    
    @Test
    public void ruleHelperShouldBeAvailableIfKeySet() throws Exception {
        final String expr = "expr";
        final String key = "helper";
        final String script =
                String.format("%s.evaluate(\"%s\");", key, expr);
        final EnforcerRuleHelper helper = mockHelper();

        when(helper.evaluate(expr)).thenReturn(Boolean.TRUE);

        final ScriptRule rule = newInlineJSRule(script);
        rule.setRuleHelperKey(key);
        rule.execute(helper);
    }

    @Test
    public void ruleBindingsShouldBeAvailable() throws Exception {
        final String script = "binding1 === binding2;";
        final String value = "foo";

        final Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("binding1", value);
        bindings.put("binding2", value);

        final EnforcerRuleHelper helper = mockHelper();
        final ScriptRule rule = newInlineJSRule(script);
        rule.setScriptBindings(bindings);

        rule.execute(helper);
    }

    @Test
    public void noValidationShouldNotCache() throws Exception {;
        final EnforcerRuleHelper helper = mockHelper();

        final ScriptRule rule = newInlineJSRule("true;");

        rule.execute(helper);
        assertFalse("cached result should be invalid", rule.isResultValid(rule));
    }
    
    @Test
    public void inlineValidationShouldSucceed() throws Exception {
        final String script = "ruleContext.put(\"foo\", 1); true;";
        final String validation = "1 == ruleContext.get(\"foo\");";
        final EnforcerRuleHelper helper = mockHelper();

        final ScriptRule rule = newInlineJSRule(script);
        rule.setValidatorScript(validation);

        rule.execute(helper);

        assertTrue("cached result should be valid", rule.isResultValid(rule));
    }
}
