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
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.PropertyState;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.rodm.teamcity.dsl.TeamCityDSLPlugin.CONFIGURATION_NAME;
import static com.github.rodm.teamcity.dsl.TeamCityDSLPlugin.DSL_EXCEPTION_FILENAME;

public class GenerateConfigurationTask extends DefaultTask {

    private static final String CONFIG_MESSAGE = "Generate TeamCity configurations in {} format from {} to {}";

    private PropertyState<String> version = getProject().property(String.class);

    private PropertyState<String> format = getProject().property(String.class);

    private PropertyState<File> baseDir = getProject().property(File.class);

    private PropertyState<File> destDir = getProject().property(File.class);

    public GenerateConfigurationTask() {
        setGroup("TeamCity");
    }

    @TaskAction
    void generate() {
        ExecResult result = getProject().javaexec(new Action<JavaExecSpec>() {
            @Override
            public void execute(JavaExecSpec spec) {
                getLogger().lifecycle(CONFIG_MESSAGE, getFormat(), formatPath(getBaseDir()), formatPath(getDestDir()));
                getLogger().info("Using main class {}", getMainClass());

                Configuration configuration = getProject().getConfigurations().getAt(CONFIGURATION_NAME);
                String toolPath = configuration.getAsPath();
                spec.setIgnoreExitValue(true);
                spec.setClasspath(createToolClasspath(configuration));
                spec.setMain(getMainClass());
                spec.args(getFormat(), getBaseDir().getAbsolutePath(), getDestDir().getAbsolutePath(), toolPath);
            }

            private FileCollection createToolClasspath(Configuration teamcityClasspath) {
                File toolJar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
                List<Object> classPath = new ArrayList<>();
                classPath.add(toolJar);
                classPath.add(teamcityClasspath);
                return getProject().files(classPath);
            }
        });
        if (result.getExitValue() != 0) {
            String message = "Process generating TeamCity configurations failed. See the report at: ";
            String dslReportUrl = asClickableFileUrl(new File(getDestDir(), DSL_EXCEPTION_FILENAME));
            throw new GradleException(message + dslReportUrl);
        }
    }

    private String getMainClass() {
        if (getVersion().startsWith("10.")) {
            return com.github.rodm.teamcity.dsl.v10.GenerateConfigurationMain.class.getName();
        } else if (getVersion().startsWith("2017.1")) {
            return com.github.rodm.teamcity.dsl.v2017.GenerateConfigurationMain.class.getName();
        } else {
            return com.github.rodm.teamcity.dsl.v2017_2.GenerateConfigurationMain.class.getName();
        }
    }

    private String asClickableFileUrl(File file) {
        try {
            return new URI("file", "", file.toURI().getPath(), null, null).toString();
        } catch (URISyntaxException ignore) {
        }
        return file.getAbsolutePath();
    }

    private String formatPath(File dir) {
        Path root = Paths.get(getProject().getRootDir().toURI());
        Path path = Paths.get(dir.toURI());
        if (path.startsWith(root)) {
            return root.relativize(path).toString();
        } else {
            return path.toAbsolutePath().toString();
        }
    }

    @Input
    public String getVersion() {
        return version.get();
    }

    public void setVersion(String version) {
        getLogger().warn("Setting the version per task is not supported.");
    }

    public void setVersion(Provider<String> version) {
        this.version.set(version);
    }

    @Input
    public String getFormat() {
        return format.get();
    }

    public void setFormat(String format) {
        this.format.set(format);
    }

    public void setFormat(Provider<String> format) {
        this.format.set(format);
    }

    @InputDirectory
    public File getBaseDir() {
        return baseDir.get();
    }

    public void setBaseDir(File baseDir) {
        this.baseDir.set(baseDir);
    }

    public void setBaseDir(Provider<File> baseDir) {
        this.baseDir.set(baseDir);
    }

    @OutputDirectory
    public File getDestDir() {
        return destDir.get();
    }

    public void setDestDir(File destDir) {
        this.destDir.set(destDir);
    }

    public void setDestDir(Provider<File> destDir) {
        this.destDir.set(destDir);
    }
}
