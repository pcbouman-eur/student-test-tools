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


package com.github.pcbouman_eur.testing.compiling;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class TeacherCompileJob {

    private List<Path> providedSources;
    private List<Path> studentSources;
    private List<Path> teacherSources;

    private TeacherCompileJob(List<Path> providedSources, List<Path> studentSources, List<Path> teacherSources) {
        this.providedSources = providedSources;
        this.studentSources = studentSources;
        this.teacherSources = teacherSources;
    }

    public static TeacherCompileJob forSources(List<Path> provided, List<Path> student, List<Path> teacher)
            throws IOException {
        Set<Path> providedFiles = getJavaFiles(provided);
        Set<Path> studentFiles = getJavaFiles(student);
        Set<Path> teacherFiles = getJavaFiles(teacher);
        teacherFiles.removeAll(providedFiles);
        return new TeacherCompileJob(new ArrayList<>(providedFiles),
                new ArrayList<>(studentFiles), new ArrayList<>(teacherFiles));
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(providedSources.size()).append(" provided sources\n");
        providedSources.forEach(p -> sb.append(" - ").append(p).append("\n"));
        sb.append("\n").append(studentSources.size()).append(" student sources\n");
        studentSources.forEach(p -> sb.append(" - ").append(p).append("\n"));
        sb.append("\n").append(teacherSources.size()).append(" teacher sources\n");
        teacherSources.forEach(p -> sb.append(" - ").append(p).append("\n"));
        return sb.toString();
    }

    public TeacherAwareFiles getTeacherAwareFiles(StandardJavaFileManager stdFm) {
        Set<Path> phase1Files = new TreeSet<>();
        phase1Files.addAll(providedSources);
        phase1Files.addAll(studentSources);
        Set<Path> teacherFiles = new TreeSet<>(teacherSources);
        teacherSources.removeAll(providedSources);
        return new TeacherAwareFiles(phase1Files, teacherFiles, stdFm);
    }

    public static Set<Path> getJavaFiles(List<Path> sources) throws IOException {
        Set<Path> files = new TreeSet<>();
        for (Path p : sources) {
            if (Files.isDirectory(p)) {
                try (Stream<Path> walker = Files.walk(p)) {
                    walker
                            .filter(f -> f.toString().toLowerCase(Locale.ROOT).endsWith(".java"))
                            .forEach(files::add);
                }
            }
            else if (p.toString().endsWith(".java")){
                files.add(p);
            }
        }
        return files;
    }

    public static final class TeacherAwareFiles {

        private final List<JavaFileObject> phase1;
        private final List<JavaFileObject> phase2;
        private final Set<JavaFileObject> teacher;

        private TeacherAwareFiles(Set<Path> phase1Files, Set<Path> teacherFiles, StandardJavaFileManager fm) {
            this.phase1 = new ArrayList<>();
            this.phase2 = new ArrayList<>();
            this.teacher = new LinkedHashSet<>();

            for (JavaFileObject jfo : fm.getJavaFileObjectsFromPaths(phase1Files)) {
                phase1.add(jfo);
                phase2.add(jfo);
            }
            for (JavaFileObject jfo : fm.getJavaFileObjectsFromPaths(teacherFiles)) {
                phase2.add(jfo);
                teacher.add(jfo);
            }
        }

        public List<JavaFileObject> getPhase1Files() {
            return Collections.unmodifiableList(phase1);
        }

        public List<JavaFileObject> getPhase2Files() {
            return Collections.unmodifiableList(phase2);
        }

        public boolean isTeacherFile(JavaFileObject jfo) {
            return teacher.contains(jfo);
        }

    }

}
