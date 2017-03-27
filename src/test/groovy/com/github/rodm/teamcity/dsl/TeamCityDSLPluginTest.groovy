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

package com.github.rodm.teamcity.dsl

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class TeamCityDSLPluginTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    private Project project

    @Before
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
    }

    @Test
    void 'applying plugin adds teamcityConfig extension'() {
        project.apply plugin: 'com.github.rodm.teamcity-dsl'

        assertTrue(project.extensions.getByName('teamcityConfig') instanceof TeamCityDSLExtension)
    }

    @Test
    void 'applying plugin adds teamcity configuration'() {
        project.apply plugin: 'com.github.rodm.teamcity-dsl'

        def configuration = project.configurations.findByName('teamcity')
        assertNotNull(configuration)
    }

    @Test
    void 'applying plugin adds generateConfiguration task'() {
        project.apply plugin: 'com.github.rodm.teamcity-dsl'

        def task = project.tasks.findByName('generateConfiguration')
        assertNotNull(task)
    }

    @Test
    void 'generateConfiguration task is configured with default values'() {
        project.apply plugin: 'com.github.rodm.teamcity-dsl'

        GenerateConfigurationTask task = project.tasks.findByName('generateConfiguration') as GenerateConfigurationTask
        assertThat(task.format, is('kotlin'))
        assertThat(normalizePath(task.baseDir), endsWith('/.teamcity'))
        assertThat(normalizePath(task.destDir), endsWith('/build/generated-configs'))
    }

    @Test
    void 'generateConfiguration task is configured with alternative values'() {
        project.apply plugin: 'com.github.rodm.teamcity-dsl'
        project.teamcityConfig {
            baseDir = project.file('src/test/teamcity')
            destDir = project.file('data/10.0/config/projects')
        }

        GenerateConfigurationTask task = project.tasks.findByName('generateConfiguration') as GenerateConfigurationTask
        assertThat(normalizePath(task.baseDir), endsWith('/src/test/teamcity'))
        assertThat(normalizePath(task.destDir), endsWith('/data/10.0/config/projects'))
    }

    private static String normalizePath(File path) {
        path.canonicalPath.replace('\\', '/')
    }
}
