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

package com.github.pcbouman_eur.testing.cli;

import picocli.CommandLine;

@CommandLine.Command(description = "Testing tools for student code", name="test", mixinStandardHelpOptions = true,
    subcommands = {TestRunner.class, ChoicesRunner.class})
public class Main {

    public static void main(String [] args) throws Exception {
        CommandLine cli = new CommandLine(new Main());
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }
}
