/*
 * Copyright 2017 the original author or authors.
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
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

public class TeamCityDSLPlugin implements Plugin<Project> {

    static final String CONFIGURATION_NAME = "teamcity";

    static final String DSL_EXCEPTION_FILENAME = "dsl_exception.xml";

    private static final String EXTENSION_NAME = "teamcityConfig";
    private static final String SOURCE_SET_NAME = "teamcity";

    private static final String DEFAULT_TEAMCITY_VERSION = "10.0.5";
    private static final String DEFAULT_FORMAT = "kotlin";
    private static final String DEFAULT_BASE_DIR = ".teamcity";
    private static final String DEFAULT_DEST_DIR = "generated-configs";

    private static final String JETBRAINS_MAVEN_REPOSITORY = "https://download.jetbrains.com/teamcity-repository";

    @Override
    public final void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        TeamCityDSLExtension extension = createExtension(project);
        Configuration configuration = createConfiguration(project);
        configureRepositories(project);
        configureSourceSet(project, configuration, extension);
        configureDefaultDependencies(project, configuration, extension);
        configureTask(project, extension);
        configureTaskType(project);
    }

    @NotNull
    private TeamCityDSLExtension createExtension(Project project) {
        TeamCityDSLExtension extension = project.getExtensions().create(EXTENSION_NAME, TeamCityDSLExtension.class);
        extension.setTeamcityVersion(DEFAULT_TEAMCITY_VERSION);
        extension.setFormat(DEFAULT_FORMAT);
        extension.setBaseDir(new File(project.getRootDir(), DEFAULT_BASE_DIR));
        extension.setDestDir(new File(project.getBuildDir(), DEFAULT_DEST_DIR));
        return extension;
    }

    private Configuration createConfiguration(Project project) {
        return project.getConfigurations().create(CONFIGURATION_NAME);
    }

    private void configureRepositories(Project project) {
        RepositoryHandler handler = project.getRepositories();
        handler.mavenCentral();
        handler.maven(repository -> repository.setUrl(JETBRAINS_MAVEN_REPOSITORY));
    }

    private void configureSourceSet(Project project, Configuration configuration, TeamCityDSLExtension extension) {
        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSet sourceSet = javaConvention.getSourceSets().create(SOURCE_SET_NAME, new Action<SourceSet>() {
            @Override
            public void execute(SourceSet sourceSet) {
                sourceSet.getJava().setSrcDirs(Collections.singletonList(new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return extension.getBaseDir();
                    }
                }));
            }
        });
        sourceSet.setCompileClasspath(configuration);
    }

    private void configureDefaultDependencies(Project project, Configuration configuration, TeamCityDSLExtension extension) {
        configuration.defaultDependencies(dependencies -> {
            DependencyHandler handler = project.getDependencies();
            dependencies.add(handler.create("org.jetbrains.kotlin:kotlin-stdlib:1.0.3"));
            dependencies.add(handler.create("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3"));
            String teamcityVersion = extension.getTeamcityVersion();
            dependencies.add(handler.create("org.jetbrains.teamcity:server-api:" + teamcityVersion));
            dependencies.add(handler.create("org.jetbrains.teamcity.internal:server:" + teamcityVersion));
            dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-server:" + teamcityVersion));
            dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-kotlin:" + teamcityVersion));

            //    compile 'org.jetbrains.teamcity:configs-dsl-kotlin-plugins:1.0-SNAPSHOT:pom'
            if (!teamcityVersion.startsWith("10.0")) {
                dependencies.add(handler.create("org.jetbrains.teamcity:configs-dsl-converters:" + teamcityVersion));
            }
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
        });
    }

    private void configureTask(Project project, TeamCityDSLExtension extension) {
        GenerateConfigurationTask task = project.getTasks().create("generateConfiguration", GenerateConfigurationTask.class);
        ConventionMapping taskMapping = task.getConventionMapping();
        taskMapping.map("version", extension::getTeamcityVersion);
        taskMapping.map("format", extension::getFormat);
        taskMapping.map("baseDir", extension::getBaseDir);
        taskMapping.map("destDir", extension::getDestDir);
        task.doFirst(new Action<Task>() {
            @Override
            public void execute(Task task) {
                project.delete(new File(extension.getDestDir(), DSL_EXCEPTION_FILENAME));
            }
        });
    }

    private void configureTaskType(Project project) {
        Class type = GenerateConfigurationTask.class;
        project.getExtensions().getExtraProperties().set(type.getSimpleName(), type);
    }
}
