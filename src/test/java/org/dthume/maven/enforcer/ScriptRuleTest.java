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

import static org.mockito.Mockito.*;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
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
    
    private void executeInlineJSRule(String script) throws Exception {
        executeInlineJSRule(mock(EnforcerRuleHelper.class), script);
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
    public void ruleHelperShouldBeAvailable() throws Exception {
        final String expr = "expr";
        final String script = String.format("%s.evaluate(\"%s\");",
                ScriptRule.RULE_HELPER_KEY, expr);
        final EnforcerRuleHelper helper = mock(EnforcerRuleHelper.class);

        when(helper.evaluate(expr)).thenReturn(Boolean.TRUE);

        executeInlineJSRule(helper, script);
    }
}
