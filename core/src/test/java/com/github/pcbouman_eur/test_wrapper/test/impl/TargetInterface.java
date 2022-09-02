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

package com.github.pcbouman_eur.test_wrapper.test.impl;

import java.util.List;

public interface TargetInterface {

    void setFlagToTrue();
    boolean getFlag();
    void setPrimitive(int k);
    int getPrimitive();
    void setBoxed(Integer k);
    Integer getBoxed();
    void addToList(String a, String b, String c);
    List<String> getCurrentList();
    void giveMePositive(int number) throws IllegalArgumentException;

}
