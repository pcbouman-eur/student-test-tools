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

package com.github.pcbouman_eur.testing.compiler_plugin;

public class PackageDetectedException extends RuntimeException {

    private final String packageName;
    private final String unitName;

    public PackageDetectedException(String packageName, String unitName) {
        super("\nThe source file '"+unitName+"' is declared with the package '"+packageName+"'\n"+
                "You should not use custom packages in this assignment. Make sure your code is in the default package.");
        this.packageName = packageName;
        this.unitName = unitName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getUnitName() {
        return unitName;
    }
}
