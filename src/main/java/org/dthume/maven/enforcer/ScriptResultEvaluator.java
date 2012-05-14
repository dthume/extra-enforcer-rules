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

/**
 * Strategy interface for determining if the result of a script
 * evaluation inside {@link ScriptRule} should cause the rule
 * to throw an exception.
 *
 * @author dth
 */
public interface ScriptResultEvaluator {
    /**
     * Check if an object represents a valid result. 
     *
     * @param result the object to check.
     * @return {@code false} if the result should cause the
     * {@code ScriptRule} to throw an exception, {@code true} otherwise.
     */
    boolean isValidResult(Object result);
}
