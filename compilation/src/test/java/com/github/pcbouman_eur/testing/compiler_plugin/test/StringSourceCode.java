package com.github.pcbouman_eur.testing.compiler_plugin.test;/* Copyright 2022 Paul Bouman

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

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;

public class StringSourceCode extends SimpleJavaFileObject {

    private final String sourceCode;

    /**
     * Creates a TestSourceFile based on a filename and the source code of the file
     * @param name the name of the compilation unit
     * @param sourceCode the source code contents of the file
     */
    protected StringSourceCode(String name, String sourceCode) {
        super(URI.create("string:///" + name.replace(".", "/") + Kind.SOURCE.extension),
                Kind.SOURCE);
        this.sourceCode = sourceCode;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return sourceCode;
    }
}
