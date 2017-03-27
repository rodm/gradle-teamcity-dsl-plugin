/*
 * Copyright 2017 Rod MacKenzie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rodm.teamcity.dsl;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.ConventionMapping;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.Callable;

public class TeamCityDSLPlugin implements Plugin<Project> {

    @Override
    public final void apply(Project project) {
        TeamCityDSLExtension extension = createExtension(project);
        Configuration configuration = createConfiguration(project);
        configureDefaultDependencies(project, configuration, extension);
        configureTask(project, extension);
    }

    @NotNull
    private TeamCityDSLExtension createExtension(Project project) {
        TeamCityDSLExtension extension = project.getExtensions().create("teamcityConfig", TeamCityDSLExtension.class);
        extension.setTeamcityVersion("10.0.5");
        extension.setFormat("kotlin");
        extension.setBaseDir(new File(project.getRootDir(), ".teamcity"));
        extension.setDestDir(new File(project.getBuildDir(), "generated-configs"));
        return extension;
    }

    private Configuration createConfiguration(Project project) {
        return project.getConfigurations().create("teamcity");
    }

    private void configureDefaultDependencies(Project project, Configuration configuration, TeamCityDSLExtension extension) {
        configuration.defaultDependencies(new Action<DependencySet>() {
            @Override
            public void execute(DependencySet dependencies) {
                DependencyHandler handler = project.getDependencies();
                dependencies.add(handler.create("org.jetbrains.kotlin:kotlin-stdlib:1.0.3"));
                dependencies.add(handler.create("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3"));
                String teamcityVersion = extension.getTeamcityVersion();
                dependencies.add(handler.create("org.jetbrains.teamcity:server-api:" + teamcityVersion));
                dependencies.add(handler.create("org.jetbrains.teamcity.internal:server:" + teamcityVersion));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-server:" + teamcityVersion));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin:" + teamcityVersion));

                //    compile 'org.jetbrains.teamcity:configs-dsl-kotlin-plugins:1.0-SNAPSHOT:pom'
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-ant:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-bugzilla:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-bundled:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-charisma:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-commandLineRunner:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-commit-status-publisher:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-dotNetRunners:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-file-content-replacer:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-gradle:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-jetbrains.git:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-jira:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-Maven2:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-mercurial:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-perforce:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-ssh-manager:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-svn:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-swabra:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-teamcity-powershell:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-tfs:1.0-SNAPSHOT"));
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin-visualstudiotest:1.0-SNAPSHOT"));
            }
        });
    }

    private void configureTask(Project project, TeamCityDSLExtension extension) {
        GenerateConfigurationTask task = project.getTasks().create("generateConfiguration", GenerateConfigurationTask.class);
        ConventionMapping taskMapping = task.getConventionMapping();
        taskMapping.map("format", new Callable<String>() {
            @Override
            public String call() {
                return extension.getFormat();
            }
        });
        taskMapping.map("baseDir", new Callable<File>() {
            @Override
            public File call() {
                return extension.getBaseDir();
            }
        });
        taskMapping.map("destDir", new Callable<File>() {
            @Override
            public File call() {
                return extension.getDestDir();
            }
        });
    }
}
