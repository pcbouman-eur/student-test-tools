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

import com.github.pcbouman_eur.testing.soft_assert.SoftAssertionFailuresError;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Optional;

public final class JUnitLegacyXMLWriter {

    private JUnitLegacyXMLWriter() {}

    public static void writeXml(File output, TestDataListener data) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory docFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element el = doc.createElement("testsuite");
        el.setAttribute("name", "Testing Tools Jupiter Runner");
        el.setAttribute("tests", ""+data.getTotalCount());
        el.setAttribute("skipped", ""+ data.getSkippedCount());
        el.setAttribute("failures", ""+ data.getFailureCount());
        el.setAttribute("errors", ""+ data.getErrorCount());
        el.setAttribute("time", data.getTime());
        doc.appendChild(el);

        for (TestDataListener.TestData test : data.getTestData()) {
            Element testcaseElement = convertTestcase(doc, test);
            el.appendChild(testcaseElement);
        }

        writeDocToFile(doc, output);
    }

    private static Element convertTestcase(Document doc, TestDataListener.TestData data) {
        TestIdentifier id = data.getIdentifier();
        Element result = doc.createElement("testcase");
        result.setAttribute("name", id.getDisplayName());
        result.setAttribute("classname", extractClassName(id));
        result.setAttribute("methodname", extractMethodName(id));
        result.setAttribute("time", data.getTime());

        Throwable t = data.getThrowable();
        if (t != null) {
            if (t instanceof SoftAssertionFailuresError) {
                SoftAssertionFailuresError saf = (SoftAssertionFailuresError) t;
                Element failure = doc.createElement("failure");
                CDATASection cData = doc.createCDATASection(saf.getLayoutDataString());
                failure.appendChild(cData);
                result.appendChild(failure);
            } else if (t instanceof MultipleFailuresError) {
                MultipleFailuresError mfe = (MultipleFailuresError) t;
                Element failure = doc.createElement("failure");
                CDATASection cData = doc.createCDATASection( mfe.getMessage());
                failure.appendChild(cData);
                result.appendChild(failure);
            } else if (t instanceof AssertionFailedError) {
                AssertionFailedError afe = (AssertionFailedError) t;
                Element failure = doc.createElement("failure");
                CDATASection cData = doc.createCDATASection( afe.getMessage());
                failure.appendChild(cData);
                result.appendChild(failure);
            } else {
                Element error = doc.createElement("error");
                try (StringWriter sw = new StringWriter();
                     PrintWriter pw = new PrintWriter(sw)) {
                    t.printStackTrace(pw);
                    CDATASection cData = doc.createCDATASection(sw.toString());
                    error.appendChild(cData);
                } catch (IOException ex) {
                    throw new AssertionError("Unexpected failure while using a StringWriter");
                }
                result.appendChild(error);
            }
        }

        if (data.getReport() != null) {
            Element systemOut = doc.createElement("system-out");
            CDATASection cData = doc.createCDATASection(data.getReport());
            systemOut.appendChild(cData);
            result.appendChild(systemOut);
        }

        return result;
    }

    private static void writeDocToFile(Document doc, File output) throws TransformerException {
        TransformerFactory tFac = TransformerFactory.newInstance();
        Transformer transformer = tFac.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult file = new StreamResult(output);
        transformer.transform(source, file);
    }

    private static String extractClassName(TestIdentifier id) {
        Optional<TestSource> opt = id.getSource();
        if (opt.isPresent()) {
            TestSource src = opt.get();
            if (src instanceof ClassSource) {
                ClassSource cs = (ClassSource) src;
                return cs.getClassName();
            }
            if (src instanceof MethodSource) {
                MethodSource ms = (MethodSource) src;
                return ms.getClassName();
            }
        }
        return "UnknownClass";
    }

    private static String extractMethodName(TestIdentifier id) {
        Optional<TestSource> opt = id.getSource();
        if (opt.isPresent()) {
            TestSource src = opt.get();
            if (src instanceof MethodSource) {
                MethodSource ms = (MethodSource) src;
                return ms.getMethodName();
            }
        }
        return "unknownMethod";
    }

}
