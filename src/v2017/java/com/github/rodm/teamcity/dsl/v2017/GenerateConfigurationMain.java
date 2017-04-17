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

package com.github.rodm.teamcity.dsl.v2017;

import jetbrains.buildServer.configs.RawConfigsBuilder;
import jetbrains.buildServer.configs.dsl.*;
import jetbrains.buildServer.configs.dsl.kotlin.KotlinClassPath;
import jetbrains.buildServer.configs.dsl.kotlin.KotlinConfigGenerator;
import jetbrains.buildServer.configs.dsl.kotlin.KotlinDslExtensionsImpl;
import jetbrains.buildServer.serverSide.impl.versionedSettings.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static jetbrains.buildServer.configs.dsl.DefaultParametersProvider.EMPTY;

public class GenerateConfigurationMain {

    private String format;

    private File baseDir;

    private File destDir;

    private String classpath;

    public static void main(String[] args) {
        GenerateConfigurationMain main = new GenerateConfigurationMain(args);
        main.execute();
    }

    GenerateConfigurationMain(String[] args) {
        format = args[0];
        baseDir = new File(args[1]);
        destDir = new File(args[2]);
        classpath = args[3];
    }

    private void execute() {
        DslGeneratorProcess.disableLog4j();
        DslGeneratorProcess.initTeamCityProperties();

        try {
            ProjectSettingsGenerator generator = findGenerator(format);
            if (generator == null) {
                System.out.println("Cannot find generator for settings format '" + format + "'");
                System.exit(1);
            } else {
                generator.generate(new VersionedSettingsFileSystemImpl(baseDir), new RawConfigsBuilder(destDir));
            }
        }
        catch (Exception e) {
            System.out.println("Error while generating TeamCity configurations: " + e.getMessage());
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private ProjectSettingsGenerator findGenerator(String format) throws Exception {
        return "kotlin".equals(format) ? createKotlinConfigGenerator() : null;
    }

    private ProjectSettingsGenerator createKotlinConfigGenerator() {
        DslPluginJars dslPluginJars = new DslPluginJars() {
            @NotNull
            public List<File> getJarLocations() {
                return Arrays.stream(classpath.split(File.pathSeparator)).map(File::new).collect(Collectors.toList());
            }
        };
        Converters converters;
        try {
            converters = Converters.readFromResources();
        } catch (Exception e) {
            System.out.println("Error while initializing configuration converters " + e.getMessage());
            e.printStackTrace(System.out);
            converters = new Converters();
        }

        DslLauncher dslLauncher = new DslLauncher(EMPTY, EMPTY, EMPTY, EMPTY, converters, dslPluginJars);
        XmlProjectSettingsGenerator defaultGenerator = new XmlProjectSettingsGenerator(new VersionedSettingsOptionsImpl());
        return new KotlinConfigGenerator(new ProjectSettingsGeneratorRegistryImpl(), new KotlinDslExtensionsImpl(), defaultGenerator, new KotlinClassPath(), dslLauncher);
    }
}
