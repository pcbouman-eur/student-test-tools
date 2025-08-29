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
import java.util.Locale;

public class TeacherAwareDiagnostic {

    private Diagnostic<? extends JavaFileObject> diagnostic;
    private boolean teacherCode;

    public TeacherAwareDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic, boolean teacherCode) {
        this.diagnostic = diagnostic;
        this.teacherCode = teacherCode;
    }

    public boolean isTeacherCode() {
        return teacherCode;
    }

    public static String mapKind(Diagnostic.Kind kind) {
        if (kind == Diagnostic.Kind.ERROR) {
            return "error";
        }
        else if (kind == Diagnostic.Kind.MANDATORY_WARNING) {
            return "error";
        }
        else if (kind == Diagnostic.Kind.WARNING) {
            return "warning";
        }
        return "info";
    }

    public String toGenericLinterFeedback() {
        String file = diagnostic.getSource().getName();
        if  (file.startsWith("./")) {
            file = file.substring(2);
        }
        // TODO: more precise positions than just the line should be possible
        long line = diagnostic.getLineNumber();
        long column = diagnostic.getColumnNumber();
        String severity = mapKind(diagnostic.getKind());

        String message = diagnostic.getMessage(Locale.ENGLISH);
        String safeMessage = message
                .replaceAll("\r\n"," ")
                .replaceAll("\n", " ")
                .replaceAll(":"," ");

        return file + ":" + line + ":" + column + ":" + severity + ":" + safeMessage;
    }

}
