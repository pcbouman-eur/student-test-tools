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

public class NoAssertionsPerformedException extends IllegalStateException {

    private static final String MESSAGE = "No assertions were performed in this SoftAssertion context. "
            + "Make sure you are using the assertion methods from the SoftAssertions class, or "
            + "set the allowNoAssertions property to true";

    public NoAssertionsPerformedException() {
        super(MESSAGE);
    }
}
