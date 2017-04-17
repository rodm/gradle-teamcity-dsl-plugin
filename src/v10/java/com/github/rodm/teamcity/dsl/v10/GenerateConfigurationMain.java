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

package com.github.rodm.teamcity.dsl.v10;

import jetbrains.buildServer.configs.RawConfigsBuilder;
import jetbrains.buildServer.configs.dsl.DslConfigGenerator;
import jetbrains.buildServer.configs.dsl.DslGeneratorProcess;
import jetbrains.buildServer.configs.dsl.DslPluginJars;
import jetbrains.buildServer.configs.dsl.kotlin.KotlinClassPath;
import jetbrains.buildServer.configs.dsl.kotlin.KotlinConfigGenerator;
import jetbrains.buildServer.serverSide.impl.versionedSettings.VersionedSettingsFileSystemImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
            DslConfigGenerator generator = findGenerator(format);
            if (generator == null) {
                System.out.println("Cannot find generator for settings format '" + format + "'");
                System.exit(1);
            } else {
                generator.generate(new VersionedSettingsFileSystemImpl(baseDir), new RawConfigsBuilder(destDir));
            }
        }
        catch (Exception e) {
            System.out.println("Error while generating TeamCity configs: " + e.getMessage());
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private DslConfigGenerator findGenerator(String format) throws Exception {
        return "kotlin".equals(format) ? createKotlinConfigGenerator() : null;
    }

    private DslConfigGenerator createKotlinConfigGenerator() {
        return new KotlinConfigGenerator(new KotlinClassPath(), new DslPluginJars() {
            @NotNull
            public List<File> getJarLocations() {
                return Arrays.stream(classpath.split(File.pathSeparator)).map(File::new).collect(Collectors.toList());
            }
        });
    }
}
