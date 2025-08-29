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

package pmd;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PmdCheck extends AbstractCheck {

    private String [] ruleFiles = new String[0];
    private String[] rules = new String[0];

    public void setRuleFile(String [] files) {
        this.ruleFiles = files;
    }

    public void setRule(String[] rules) {
        this.rules = rules;
    }

    private String getRuleSetXML() {
        StringBuilder xml = new StringBuilder();
        xml.append("<ruleset name=\"bridge\" xmlns=\"http://pmd.sourceforge.net/ruleset/2.0.0\">\n");
        for (String rule : rules) {
            if (rule.contains("/")) {
                // treat as fully qualified
                xml.append("\t<rule ref=\"").append(rule).append("\"/>\n");
            } else {
                // fallback to bestpractices
                xml.append("\t<rule ref=\"category/java/bestpractices.xml/").append(rule).append("\"/>\n");
            }
        }
        xml.append("</ruleset>");
        return xml.toString();
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        String path = getFilePath();
        File sourceFile = new File(path);

        LanguageVersion javaLang = LanguageRegistry.PMD.getLanguageById("java").getDefaultVersion();

        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(javaLang);

        if (rules.length == 0 && ruleFiles.length == 0) {
            log(0, "CONFIGURATION ERROR - No rules or rule files defined for PMD.");
            return;
        }

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            RuleSetLoader loader = RuleSetLoader.fromPmdConfig(config);
            if (rules.length > 0) {
                String rulesetXML = getRuleSetXML();
                pmd.addRuleSet(loader.loadFromString("bridge.xml", rulesetXML));
            }
            for (String ruleFile : ruleFiles) {
                File f = new File(ruleFile);
                try {
                    String content = Files.readString(f.toPath());
                    RuleSet ruleSet = loader.loadFromString(f.getName(), content);
                    pmd.addRuleSet(ruleSet);
                } catch (IOException ex) {
                    log(0, "CONFIGURATION ERROR - Error while loading ruleset file {0}: {1}",
                            ruleFile, ex.getMessage());
                }
            }
            pmd.files().addFile(sourceFile.toPath());
            Report report = pmd.performAnalysisAndCollectReport();
            for (RuleViolation violation : report.getViolations()) {
                log(violation.getBeginLine(), violation.getBeginColumn(),
                        "{0}: {1}",
                        violation.getRule().getName(),
                        violation.getDescription());
            }
        } catch (Exception e) {
            log(0, "CONFIGURATION ERROR - Error running PMD: " + e.getMessage());
        }
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[0];
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[0];
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[0];
    }
}
