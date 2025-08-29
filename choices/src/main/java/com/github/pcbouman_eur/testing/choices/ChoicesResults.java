package com.github.pcbouman_eur.testing.choices;

import com.github.pcbouman_eur.testing.choices.annotations.ChoiceTests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ChoicesResults {

    private static double ROUNDING_EPSILON = 1e-6;

    private static final String JSON_FORMAT = "{ \"tag\" : \"points\", \"points\" : \"%d/%d\" }";

    private ChoiceTests tests;
    private List<ChoiceResult> results;

    public ChoicesResults(ChoiceTests tests) {
        this.tests = tests;
        this.results = new ArrayList<>();
    }

    public void addResult(ChoiceResult result) {
        results.add(result);
    }

    public boolean isProperRun() {
        if (results.isEmpty()) {
            return false;
        }
        for (ChoiceResult result: results) {
            if (!result.isProperRun()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public List<ChoiceResult> getProblematicChoices() {
        return results.stream()
                .filter(cr -> !cr.isProperRun())
                .collect(Collectors.toList());
    }

    public String generateReport(boolean includeNormalized) {
        StringBuilder builder = new StringBuilder();

        String format = getPointsFormat();

        double sum = 0;

        for (ChoiceResult result : results) {
            double earned = result.isPass() ? result.getChoice().points() : 0d;
            if (result.isPass()) {
                builder.append("\u2714 ");
            }
            else {
                builder.append("\u274c ");
            }
            builder.append(String.format(Locale.ROOT, format, earned, result.getChoice().points()));
            builder.append(" ");
            builder.append(result.getChoice().name());
            builder.append("\n");
            sum += earned;
        }

        builder.append("\n");
        builder.append(String.format(Locale.ROOT, "Final score: %.1f out of maximum of %.1f points",
                Math.min(sum, tests.maximumPoints()), tests.maximumPoints()));
        builder.append("\n");

        if (includeNormalized) {
            builder.append("\n\n");
            double normalized = Math.min(sum, tests.maximumPoints()) / tests.maximumPoints();
            builder.append(String.format(Locale.ROOT, "%.1f", normalized));
        }
        return builder.toString();
    }

    public String getPointsJson() {
        double sum = 0;

        for (ChoiceResult result : results) {
            double earned = result.isPass() ? result.getChoice().points() : 0d;
            sum += earned;
        }

        int scale = tests.outputScale();
        double points = Math.min(sum, tests.maximumPoints());
        double max = tests.maximumPoints();

        int intPoints = (int) Math.floor(Math.min(max, points/max) * scale + ROUNDING_EPSILON);

        return String.format(Locale.ROOT, JSON_FORMAT, intPoints, scale);
    }

    private String getPointsFormat() {
        int colWidth = (int) Math.floor(1 +
                Math.log10(results.stream().mapToInt(cr -> (int)Math.floor(cr.getChoice().points()))
                        .max().orElse(1)));
        return "[ %"+colWidth+".1f of %"+colWidth+".1f points ]";
    }

}
