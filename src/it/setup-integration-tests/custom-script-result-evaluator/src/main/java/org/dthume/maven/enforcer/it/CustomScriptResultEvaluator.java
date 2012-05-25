package org.dthume.maven.enforcer.it;

import org.dthume.maven.enforcer.ScriptResultEvaluator;

public class CustomScriptResultEvaluator implements ScriptResultEvaluator {
    private String expected = "custom";
    
    public boolean isValidResult(Object result) {
        return expected.equals(result);
    }
}
