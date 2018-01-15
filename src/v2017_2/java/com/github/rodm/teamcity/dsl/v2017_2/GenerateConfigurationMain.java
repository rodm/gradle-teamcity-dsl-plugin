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

package com.github.rodm.teamcity.dsl.v2017_2;

import jetbrains.buildServer.configs.RawConfigsBuilder;
import jetbrains.buildServer.configs.dsl.*;
import jetbrains.buildServer.configs.dsl.kotlin.*;
import jetbrains.buildServer.plugins.ServerPluginManager;
import jetbrains.buildServer.serverSide.impl.versionedSettings.*;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static jetbrains.buildServer.configs.dsl.DefaultParametersProvider.EMPTY;
import static jetbrains.buildServer.configs.dsl.DslDataManager.NO_OP;

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
            FileUtil.delete(destDir);
            ProjectSettingsGenerator generator = findGenerator(format);
            if (generator == null) {
                System.out.println("Cannot find generator for settings format '" + format + "'");
            } else {
                GenOptions options = new GenOptions();
                options.setServerSettingsUpdate(false);
                generator.generate(new VersionedSettingsFileSystemImpl(baseDir), new RawConfigsBuilder(destDir), options);
            }

        }
        catch (VersionedSettingsException vse) {
            if (vse.getErrors().isEmpty()) {
                System.out.println("Error while generating TeamCity configurations: " + vse.getMessage());
                vse.printStackTrace(System.out);
            } else {
                System.out.println("Error while generating TeamCity configurations:");

                StringBuilder errorMessage;
                for (VersionedSettingsError error : vse.getErrors()) {
                    errorMessage = new StringBuilder();
                    errorMessage.append(error.getDescription());
                    System.out.println(errorMessage.toString());
                }
            }
            System.exit(1);
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

    private ProjectSettingsGenerator createKotlinConfigGenerator() throws Exception {
        List<String> classpathList = Arrays.asList(classpath.split(File.pathSeparator));
        KotlinLibsMaven kotlinLibs = new KotlinLibsMaven(classpathList);
        final Set<File> excludeFromLibs = new HashSet<>();
        excludeFromLibs.add(kotlinLibs.getRuntimeJar());
        excludeFromLibs.add(kotlinLibs.getStdLibJar());
        excludeFromLibs.add(kotlinLibs.getScriptRuntimeJar());
        excludeFromLibs.add(kotlinLibs.getReflectJar());
        excludeFromLibs.add(ClasspathUtil.getDefiningJar(this.getClass().getClassLoader().loadClass("jetbrains.buildServer.configs.kotlin.Context")));
        DslPluginJars dslPluginJars = new DslPluginJars() {
            @NotNull
            public List<File> getJarLocations() {
                return classpathList.stream().map(File::new).filter((f) -> {
                    if (excludeFromLibs.contains(f)) {
                        return false;
                    } else {
                        String name = f.getName();
                        return !name.startsWith("kotlin-compiler-embeddable") && !name.startsWith("dokka-fatjar") || !name.endsWith(".jar");
                    }
                }).collect(Collectors.toList());
            }
        };

        Converters converters;
        try {
            converters = Converters.readFromResources();
        }
        catch (Exception e) {
            System.out.println("Error while initializing configuration converters " + e.getMessage());
            e.printStackTrace(System.out);
            converters = new Converters();
        }

        DslLauncher dslLauncher = new DslLauncher(new DefaultsProviders(EMPTY, EMPTY, EMPTY, EMPTY), converters, dslPluginJars, NO_OP, new ServerPluginManager() {
            @NotNull
            public Collection<File> getPluginDirs() {
                return Collections.emptyList();
            }
        }, DslDependencies.NO_OP, new KotlinReadOnlyReasonTracker());
        XmlProjectSettingsGenerator defaultGenerator = new XmlProjectSettingsGenerator(new VersionedSettingsOptionsImpl());
        return new KotlinConfigGenerator(new ProjectSettingsGeneratorRegistryImpl(), new KotlinDslExtensionsImpl(), defaultGenerator, new KotlinClassPath(kotlinLibs), dslLauncher, new KotlinReadOnlyReasonTracker());
    }

    static class KotlinLibsMaven implements KotlinLibs {
        private final List<String> myClasspath;

        public KotlinLibsMaven(@NotNull List<String> classpath) {
            this.myClasspath = classpath;
        }

        @NotNull
        public File getCompilerJar() throws Exception {
            return this.getJarFromClasspath(this.stripJar("kotlin-compiler-embeddable.jar"));
        }

        @NotNull
        public File getRuntimeJar() throws Exception {
            return this.getJarFromClasspath(this.stripJar("kotlin-runtime.jar"));
        }

        @NotNull
        public File getStdLibJar() throws Exception {
            return this.getJarFromClasspath(this.stripJar("kotlin-stdlib.jar"));
        }

        @NotNull
        public File getScriptRuntimeJar() throws Exception {
            return this.getJarFromClasspath(this.stripJar("kotlin-script-runtime.jar"));
        }

        @NotNull
        public File getReflectJar() throws Exception {
            return this.getJarFromClasspath(this.stripJar("kotlin-reflect.jar"));
        }

        @NotNull
        private String stripJar(@NotNull String str) {
            if (!str.endsWith(".jar")) {
                throw new IllegalArgumentException(str + " doesn't end with .jar");
            } else {
                return str.substring(0, str.length() - ".jar".length());
            }
        }

        @NotNull
        private File getJarFromClasspath(@NotNull String jarPrefix) {
            Iterator iterator = this.myClasspath.iterator();

            File f;
            do {
                if (!iterator.hasNext()) {
                    throw new IllegalStateException("Cannot find jar " + jarPrefix);
                }
                String element = (String)iterator.next();
                f = new File(element);
            } while(!f.isFile() || !f.getName().startsWith(jarPrefix) || !f.getName().endsWith(".jar"));
            return f;
        }
    }
}
