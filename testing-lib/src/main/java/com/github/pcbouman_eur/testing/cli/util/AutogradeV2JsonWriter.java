package com.github.pcbouman_eur.testing.cli.util;

import jakarta.json.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class AutogradeV2JsonWriter {

    private AutogradeV2JsonWriter() {}

    public static void write(File output, TestDataListener listener) throws IOException {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("tag", "unit-test");
        builder.add("name", "JUnit Jupiter");
        builder.add("results", getTestcases(listener));
        JsonObject testObject = builder.build();
        Files.writeString(output.toPath(), testObject.toString(), StandardCharsets.UTF_8);
    }

    private static JsonArrayBuilder getTestcases(TestDataListener listener) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        JsonObjectBuilder obj = Json.createObjectBuilder();
        obj.add("id", "testing-tools-junit-jupiter");
        obj.add("name", "Testing Tools Jupiter Runner");
        JsonArrayBuilder testCases = Json.createArrayBuilder();
        for (TestDataListener.TestData data : listener.getTestData()) {
            JsonObjectBuilder testCase = Json.createObjectBuilder();
            testCase.add("name", data.getIdentifier().getLegacyReportingName());
            testCase.add("weight", 1);
            testCase.add("status", data.isSuccess() ? "success" : "failure");
            if (!data.isSuccess()) {
                testCase.add("reason", data.getReport());
            }
            testCases.add(testCase);
        }
        obj.add("testCases", testCases);
        builder.add(obj);
        return builder;
    }

}
