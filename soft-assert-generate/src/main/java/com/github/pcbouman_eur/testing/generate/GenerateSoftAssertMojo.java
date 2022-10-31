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

package com.github.pcbouman_eur.testing.generate;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mojo(name = "generate-soft-assert", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateSoftAssertMojo extends AbstractMojo {

    private static Logger log = LoggerFactory.getLogger(GenerateSoftAssertMojo.class);

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/generate-soft-assert", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "org.junit.jupiter.api.Assertions", required = true)
    private String delegateClassName;
    @Parameter(defaultValue = "com.github.pcbouman_eur.testing.soft_assert", required = true)
    private String outputPackage;
    @Parameter(defaultValue = "SoftAssertions", required = true)
    private String outputClassName;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Class<?> delegateClass;
        try {
            delegateClass = GenerateSoftAssertMojo.class.getClassLoader().loadClass(delegateClassName);
        } catch (ClassNotFoundException ex) {
            log.error("Error while loading delegate class "+delegateClassName, ex);
            throw new MojoExecutionException("Error while loading delegate class "+delegateClassName+".\n"
                    +ex.getMessage(), ex);
        }
        log.info("Generating sources for class {} based on delegate class {}", outputPackage+"."+outputClassName,
                delegateClass);
        String src = buildClassDefinition(Assertions.class, "HardToSoft", outputPackage, outputClassName);
        File targetDir = Paths.get(outputDirectory.getPath(), outputPackage.split("\\.")).toFile();
        targetDir.mkdirs();
        File outputFile = new File(targetDir, outputClassName+".java");
        try (PrintWriter pw = new PrintWriter(outputFile)) {
            pw.println(src);
            log.info("Wrote generated source code to {}", outputFile);
        }
        catch (IOException ex) {
            log.error("Error while writing the generated sources", ex);
            throw new MojoExecutionException("Error writing generated sources.\n"+ex.getMessage(), ex);
        }
        log.info("Adding {} to project source root to make sure the generated packages are compiled", outputDirectory);
        project.addCompileSourceRoot(outputDirectory.getPath());
    }

    private static String buildClassDefinition(Class<?> clz, String hardToSoftClass, String outputPackage,
                                               String outputClassName) {
        String delegateClass = clz.getSimpleName();
        StringBuilder sb = new StringBuilder();
        // TODO: custom class name / custom package name?
        sb.append("package "+outputPackage+";\n\n");
        sb.append("import "+clz.getName()+";\n");
        sb.append("import org.opentest4j.AssertionFailedError;\n");
        sb.append("import org.opentest4j.MultipleFailuresError;\n\n");
        sb.append("public final class "+outputClassName+" {\n\n");
        sb.append("\tprivate "+outputClassName+"() {}\n\n");
        Class<Assertions> assertionsClass = Assertions.class;
        for (Method m : assertionsClass.getMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
                sb.append(buildProxyMethodDeclaration(m, delegateClass, hardToSoftClass));
                sb.append("\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String buildProxyMethodDeclaration(Method m, String delegateClass, String hardToSoftClass) {
        StringBuilder sb = new StringBuilder();
        String delegate = buildDelegateCall(m, delegateClass);
        boolean isVoid = m.getGenericReturnType().getTypeName().equals("void");

        sb.append("\t");
        sb.append(buildHeader(m));
        sb.append(" {\n");
        sb.append("\t\tif ("+hardToSoftClass+".isHardAssert()) {\n\t\t\t");
        if (!isVoid) {
            sb.append("return ");
        }
        sb.append(delegate);
        sb.append(";\n");
        if (isVoid) {
            sb.append("\t\t\treturn;\n");
        }
        sb.append("\t\t}\n");
        if (!isVoid) {
            sb.append("\t\t"+m.getGenericReturnType().getTypeName()+" result = null;\n");
        }
        sb.append("\t\tString msg = ");
        sb.append(buildMessageExpression(m));
        sb.append(";\n");
        sb.append("\t\ttry {\n\t\t\t");
        if (!isVoid) {
            sb.append("result = ");
        }
        sb.append(delegate);
        sb.append(";\n\t\t\t");
        sb.append(hardToSoftClass+".reportSuccess(msg);\n");
        sb.append("\t\t} catch (MultipleFailuresError mfe) {\n\t\t\t");
        sb.append(hardToSoftClass+".reportMultipleFailures(mfe, msg);\n\t\t} catch (AssertionFailedError afe) {\n\t\t\t");
        sb.append(hardToSoftClass+".reportFailure(afe, msg);\n\t\t}\n\t");
        if (!isVoid) {
            sb.append("\treturn result;\n\t");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildMessageExpression(Method m) {
        java.lang.reflect.Parameter[] params = m.getParameters();
        if (params.length > 0) {
            java.lang.reflect.Parameter lastParam = params[params.length - 1];
            if (lastParam.getType().equals(String.class)) {
                return lastParam.getName();
            }
            if (lastParam.getType().equals(Supplier.class)) {
                return lastParam.getName() + ".get()";
            }
        }
        return "\"Test without a description that uses " + m.getName() + "()\"";
    }

    private static String buildDelegateCall(Method m, String classToCall) {
        StringBuilder sb = new StringBuilder();
        sb.append(classToCall);
        sb.append(".");
        sb.append(m.getName());
        sb.append("(");
        sb.append(
                Arrays.stream(m.getParameters())
                        .map(param -> param.getName())
                        .collect(Collectors.joining(", "))
        );
        sb.append(")");
        return sb.toString();
    }

    private static String buildHeader(Method m) {
        List<String> preList = new ArrayList<>();
        int mod = m.getModifiers();

        if (Modifier.isNative(mod)) {
            throw new IllegalArgumentException("Native methods are currently not supported by this plugin");
        }

        // Accessibility of the method
        if (Modifier.isPublic(mod)) {
            preList.add("public");
        }
        else if (Modifier.isProtected(mod)) {
            preList.add("protected");
        }
        else if (Modifier.isPrivate(mod)) {
            preList.add("private");
        }

        if (Modifier.isStatic(mod)) {
            preList.add("static");
        }
        else if (Modifier.isAbstract(mod)) {
            preList.add("abstract");
        }

        if (Modifier.isSynchronized(mod)) {
            preList.add("synchronized");
        }

        if (Modifier.isFinal(mod)) {
            preList.add("final");
        }

        if (m.getTypeParameters().length > 0) {
            preList.add("<"+
                Arrays.stream(m.getTypeParameters())
                        .map(GenerateSoftAssertMojo::formatTypeVariable)
                        .collect(Collectors.joining(","))
            +">");
        }

        preList.add(m.getGenericReturnType().getTypeName());

        String method = m.getName() + "(";

        List<String> paramList = new ArrayList<>();
        for (java.lang.reflect.Parameter param : m.getParameters()) {
            paramList.add(param.getParameterizedType().getTypeName()+" "+param.getName());
        }

        return preList.stream().collect(Collectors.joining(" "))
                + " "
                + method
                + paramList.stream().collect(Collectors.joining(", "))
                + ")";
    }

    private static String formatTypeVariable(TypeVariable<?> typeVariable) {
        if (typeVariable.getBounds().length > 1) {
            throw new IllegalArgumentException("Type variables with multiple bounds are not supported");
        }
        if (typeVariable.getBounds().length == 1) {
            return typeVariable.getName() + " extends " + typeVariable.getBounds()[0].getTypeName();
        }
        return typeVariable.getName();
    }
}
