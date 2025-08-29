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

package com.github.pcbouman_eur.testing.cli.util;

import com.puppycrawl.tools.checkstyle.*;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Separate class used to run Checkstyle. This is to avoid that the CheckStyle command class needs to load
 * checkstyle-related dependency classes that may not yet be on the classpath at install time.
 */
public class CheckStyleRunner {

    private static AuditListener getListener(Path output) throws IOException {
        OutputStream out;
        AbstractAutomaticBean.OutputStreamOptions options;
        if (output == null) {
            out = System.out;
            options = AbstractAutomaticBean.OutputStreamOptions.NONE;
        }
        else {
            out = Files.newOutputStream(output);
            options = AbstractAutomaticBean.OutputStreamOptions.CLOSE;
        }
        return new XMLLogger(out, options);
    }

    public static int runCheckStyle(String configFile, Path output, List<File> files) throws IOException {
        try {
            Properties props = System.getProperties();
            Configuration config = ConfigurationLoader.loadConfiguration(configFile, new PropertiesExpander(props));
            ClassLoader moduleClassLoader = Checker.class.getClassLoader();
            ModuleFactory moduleFactory = new PackageObjectFactory(Checker.class.getPackage().getName(), moduleClassLoader);
            RootModule rootModule = (RootModule) moduleFactory.createModule(config.getName());
            rootModule.setModuleClassLoader(moduleClassLoader);
            rootModule.configure(config);
            rootModule.addListener(getListener(output));
            return rootModule.process(files);
        }
        catch (CheckstyleException ex) {
            throw new RuntimeException("Error while running checkstyle", ex);
        }
    }
}
