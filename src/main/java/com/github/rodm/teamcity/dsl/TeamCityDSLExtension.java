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

import org.gradle.api.Project;
import org.gradle.api.provider.PropertyState;
import org.gradle.api.provider.Provider;

import java.io.File;

public class TeamCityDSLExtension {

    private PropertyState<String> teamcityVersion;

    private PropertyState<String> format;

    private PropertyState<File> baseDir;

    private PropertyState<File> destDir;

    public TeamCityDSLExtension(Project project) {
        teamcityVersion = project.property(String.class);
        format = project.property(String.class);
        baseDir = project.property(File.class);
        destDir = project.property(File.class);
    }

    public String getTeamcityVersion() {
        return teamcityVersion.get();
    }

    public Provider<String> getTeamcityVersionProvider() {
        return teamcityVersion;
    }

    public void setTeamcityVersion(String teamcityVersion) {
        this.teamcityVersion.set(teamcityVersion);
    }

    public String getFormat() {
        return format.get();
    }

    public Provider<String> getFormatProvider() {
        return format;
    }

    public void setFormat(String format) {
        this.format.set(format);
    }

    public File getBaseDir() {
        return baseDir.get();
    }

    public Provider<File> getBaseDirProvider() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir.set(baseDir);
    }

    public File getDestDir() {
        return destDir.get();
    }

    public Provider<File> getDestDirProvider() {
        return destDir;
    }

    public void setDestDir(File destDir) {
        this.destDir.set(destDir);
    }
}
