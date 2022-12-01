package com.github.pcbouman_eur.testing.choices;

import com.github.pcbouman_eur.testing.choices.annotations.Choice;
import com.github.pcbouman_eur.testing.choices.annotations.ChoiceTests;
import com.github.pcbouman_eur.testing.choices.annotations.TestStep;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.util.Arrays;

public class Runner {

    public static ChoicesResults runChoices(Class<?> choiceSpecification) {
        ChoiceTests specification = choiceSpecification.getAnnotation(ChoiceTests.class);
        if (specification == null) {
            throw new IllegalArgumentException("Class "+choiceSpecification+" is not annotated with @ChoiceTests");
        }
        ChoicesResults results = new ChoicesResults(specification);
        for (Choice choice : specification.choices()) {
            results.addResult(runChoice(choice));
        }
        return results;
    }

    public static ChoiceResult runChoice(Choice choice) {
        ChoiceResult result = new ChoiceResult(choice);
        for (TestStep step : choice.steps()) {
            LauncherDiscoveryRequest request = getTestStepLauncher(step);
            Launcher launcher = LauncherFactory.create();
            TestPlan testPlan = launcher.discover(request);
            SummaryGeneratingListener sumListener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(sumListener);
            launcher.execute(testPlan);
            result.addSummary(step, sumListener.getSummary());
        }
        return result;
    }

    public static LauncherDiscoveryRequest getTestStepLauncher(TestStep step) {
        ClassSelector[] classSelectors = Arrays.stream(step.testClasses())
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);
        String [] tags = step.tags();
        if (tags != null && tags.length > 0) {
            return LauncherDiscoveryRequestBuilder.request()
                    .selectors(classSelectors)
                    .filters(TagFilter.includeTags(tags))
                    .build();
        }
        return LauncherDiscoveryRequestBuilder.request()
                .selectors(classSelectors)
                .build();
    }

}
