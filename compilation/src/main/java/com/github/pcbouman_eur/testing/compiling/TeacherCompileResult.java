/* Copyright 2025 Paul Bouman

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

package com.github.pcbouman_eur.testing.compiling;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

public class TeacherCompileResult {

    private final boolean ok;
    private final String output, log;

    private final List<TeacherAwareDiagnostic> diagnostics;

    public TeacherCompileResult(boolean ok, String output, String log, List<Diagnostic<? extends JavaFileObject>> diagnostics,
                                TeacherCompileJob.TeacherAwareFiles files) {
        this.ok = ok;
        this.output = output;
        this.log = log;
        this.diagnostics = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
            TeacherAwareDiagnostic tad = new TeacherAwareDiagnostic(d, files.isTeacherFile(d.getSource()));
            this.diagnostics.add(tad);
        }
    }

    public boolean isOk() {
        return ok;
    }

    public String getLog() {
        return log;
    }

    public String getOutput() {
        return output;
    }

    public List<TeacherAwareDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
