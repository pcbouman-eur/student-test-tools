/* Copyright 2022 Paul Bouman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.github.pcbouman_eur.testing.soft_assert;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ExtendWith(SoftAssertExtension.class)
public @interface SoftAssert {
    int storageLimit() default 100;
    int displayLimit() default 3;
    boolean showBooleanDetails() default false;
    boolean allowNoAssertions() default false;
    boolean immediateExceptions() default false;
    int stacktraceDisplayLimit() default -1;
    boolean sanitizeStacktrace() default true;
    boolean retainSuccessData() default true;
}
