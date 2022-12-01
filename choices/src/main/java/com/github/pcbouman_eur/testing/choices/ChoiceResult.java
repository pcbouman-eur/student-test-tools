package com.github.pcbouman_eur.testing.choices;

import com.github.pcbouman_eur.testing.choices.annotations.Choice;
import com.github.pcbouman_eur.testing.choices.annotations.TestStep;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChoiceResult {

    private final Choice choice;
    private final Map<TestStep,TestExecutionSummary> summaries;

    public ChoiceResult(Choice choice) {
        this.choice = choice;
        this.summaries = new LinkedHashMap<>();
    }

    public void addSummary(TestStep step, TestExecutionSummary summary) {
        summaries.put(step, summary);
    }

    public Choice getChoice() {
        return choice;
    }

    public boolean isPass() {
        for (TestExecutionSummary summary : summaries.values()) {
            if (summary.getTestsAbortedCount() > 0 || summary.getTestsFailedCount() > 0) {
                return false;
            }
        }
        return true;
    }

    public List<TestStep> getEmptyRuns() {
        return summaries.entrySet()
                .stream()
                .filter(e -> e.getValue().getTestsStartedCount() <= 0)
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    public boolean isEmpty() {
        return summaries.isEmpty();
    }

    public boolean isProperRun() {
        if (summaries.isEmpty()) {
            return false;
        }
        for (TestExecutionSummary summary : summaries.values()) {
            if (summary.getTestsStartedCount() <= 0) {
                return false;
            }
        }
        return true;
    }
}
