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
        executeInlineJSRule(script, mock(EnforcerRuleHelper.class));
    }
    
    private void executeInlineJSRule(String script, EnforcerRuleHelper helper)
            throws Exception {
        final ScriptRule rule = newInlineJSRule(script);        
        rule.execute(helper);
    }
    
    @Test
    public void trueInlineScriptShouldPass() 
            throws Exception {
        executeInlineJSRule("true;");        
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
    public void ruleHelperShouldBeAvailable() throws Exception {
        final String expr = "${expr}";
        final String script = String.format("%s.evaluate(\"%s\");",
                ScriptRule.RULE_HELPER_KEY, expr);
        
        final EnforcerRuleHelper helper = mock(EnforcerRuleHelper.class);
        when(helper.evaluate(expr)).thenReturn(Boolean.TRUE);
        
        executeInlineJSRule(script, helper);
    }
}
