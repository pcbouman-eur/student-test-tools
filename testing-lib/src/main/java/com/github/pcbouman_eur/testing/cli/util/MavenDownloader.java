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

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to download maven artifacts and their dependencies.
 */

public class MavenDownloader {

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remotes;

    public MavenDownloader(File localRepoDir) {
        this.system = newRepositorySystem();
        this.session = newRepositorySystemSession(system, localRepoDir);
        this.remotes = List.of(
                new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build()
        );
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newRepositorySystemSession(RepositorySystem system, File localRepoDir) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepoDir);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    /**
     * Resolve an artifact and its dependencies.
     */
    public List<File> resolve(String gav) throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(gav);

        CollectRequest collect = new CollectRequest();
        collect.setRoot(new Dependency(artifact, ""));
        collect.setRepositories(remotes);

        DependencyRequest request = new DependencyRequest(collect, null);
        DependencyResult result = system.resolveDependencies(session, request);

        List<File> files = new ArrayList<>();
        for (ArtifactResult ar : result.getArtifactResults()) {
            files.add(ar.getArtifact().getFile());
        }
        return files;
    }

    /**
     * Resolve multiple artifacts and return all jar files.
     */
    public List<File> resolveArtifacts(List<String> gavs) throws DependencyResolutionException {
        List<File> all = new ArrayList<>();
        for (String gav : gavs) {
            all.addAll(resolve(gav));
        }
        return all;
    }

}