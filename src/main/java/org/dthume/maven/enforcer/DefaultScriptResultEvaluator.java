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
 * Basic implementation with support for core Java types.
 * 
 * <ul>
 * <li>{@code null == false}</li>
 * <li>{@link Boolean} values are simply returned</li>
 * <li>{@link Number} types are {@code false} if zero, otherwise {@code true}<li>
 * <li>All other values are {@code true}<li>
 * </ul>
 * 
 * @author dth
 */
public class DefaultScriptResultEvaluator implements ScriptResultEvaluator {
    public boolean isValidResult(Object result) {
        if (null == result)
            return false;
        if (result instanceof Boolean)
            return ((Boolean) result).booleanValue();
        if (result instanceof Integer)
            return 0 != ((Integer) result).intValue();
        if (result instanceof Long)
            return 0L != ((Long) result).longValue();
        if (result instanceof Float)
            return 0.0F != ((Float) result).floatValue();
        if (result instanceof Double)
            return 0.0D != ((Double) result).doubleValue();
        return true;
    }
}
