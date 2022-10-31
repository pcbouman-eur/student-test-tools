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

import org.opentest4j.MultipleFailuresError;

public class SoftAssertionFailuresError extends MultipleFailuresError {

    private final SoftAssertionData data;
    private final SoftAssertionTextLayout layout;

    public SoftAssertionFailuresError(SoftAssertionData data, SoftAssertionTextLayout layout) {
        super(layout.getReport(data), data.getAllErrors());
        this.data = data;
        this.layout = layout;
    }

    public SoftAssertionData getData() {
        return data;
    }

    public SoftAssertionTextLayout getLayout() {
        return layout;
    }

    public String getLayoutDataString() {
        return layout.getReport(data);
    }
}
