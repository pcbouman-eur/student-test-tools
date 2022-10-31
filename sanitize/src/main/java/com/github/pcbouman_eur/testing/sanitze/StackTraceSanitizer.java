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

package com.github.pcbouman_eur.testing.sanitze;

import com.github.pcbouman_eur.testing.wrapper.WrapperFactory;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.*;
import java.util.stream.Collectors;

public final class StackTraceSanitizer {

    private static final List<String> DEFAULT_PREFIXES = List.of("sun.reflect", "jdk.internal.reflect");
    private static final Set<String> DEFAULT_CLASSNAMES = Set.of(WrapperFactory.class.getName());

    private final List<String> prefixes;
    private final Set<String> classNames;

    public StackTraceSanitizer() {
        super();
        this.prefixes = DEFAULT_PREFIXES;
        this.classNames = DEFAULT_CLASSNAMES;
    }

    public StackTraceSanitizer(ExtensionContext ctx) {
        super();
        this.prefixes = DEFAULT_PREFIXES;
        Set<String> classNames = new HashSet<>(DEFAULT_CLASSNAMES);
        classNames.add(ctx.getRequiredTestClass().getName());
        ctx.getTestClass().ifPresent(clz -> classNames.add(clz.getName()));
        this.classNames = Collections.unmodifiableSet(classNames);
    }

    public StackTraceSanitizer(Collection<String> prefixes, Collection<String> postFixes,
                              Collection<String> classNames) {
        super();
        this.prefixes = new ArrayList<>(prefixes);
        this.classNames = new HashSet<>(classNames);
    }


    public StackTraceElement[] sanitize(StackTraceElement[] st) {
        List<StackTraceElement> result =
                Arrays.stream(st)
                        .takeWhile(this::showTraceElement)
                        .collect(Collectors.toList());
        result.add(new StackTraceElement("TeacherTestClass", "testCaseMethod", null, -2));
        return result.toArray(new StackTraceElement[0]);
    }

    private boolean showTraceElement(StackTraceElement st) {
        String className = st.getClassName();
        boolean result = !classNames.contains(className) &&
                !prefixes.stream().anyMatch(pref -> className.startsWith(pref));
        return result;
    }

}
