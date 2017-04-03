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
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionAwareHelper;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaExecSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.github.rodm.teamcity.dsl.TeamCityDSLPlugin.CONFIGURATION_NAME;

public class GenerateConfigurationTask extends DefaultTask implements IConventionAware {

    private ConventionMapping conventionMapping = new ConventionAwareHelper(this, this.getProject().getConvention());

    private String format;

    private File baseDir;

    private File destDir;

    public GenerateConfigurationTask() {
        setGroup("TeamCity");
    }

    public ConventionMapping getConventionMapping() {
        return this.conventionMapping;
    }

    @TaskAction
    void generate() {
        getProject().javaexec(new Action<JavaExecSpec>() {
            @Override
            public void execute(JavaExecSpec spec) {
                Configuration configuration = getProject().getConfigurations().getAt(CONFIGURATION_NAME);
                String toolPath = configuration.getAsPath();
                spec.setClasspath(createToolClasspath(configuration));
                spec.setMain("com.github.rodm.teamcity.dsl.internal.GenerateConfigurationMain");
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
    }

    @Input
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @InputDirectory
    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    @OutputDirectory
    public File getDestDir() {
        return destDir;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }
}
