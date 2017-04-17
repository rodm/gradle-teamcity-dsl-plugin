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

package com.github.rodm.teamcity.dsl

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class TeamCityDSLPluginFunctionalTest {

    static final String BUILD_SCRIPT = '''
        plugins {
            id 'com.github.rodm.teamcity-dsl'
        }

        repositories {
            maven {
                url "http://${server}:8111/app/dsl-plugins-repository"
            }
        }
    '''.stripIndent()

    static final String VALID_SETTINGS_FILE = '''
        package Project

        import jetbrains.buildServer.configs.kotlin.v10.*

        version = "10.0"
        project {
            uuid = "2c4c777e-8e46-4eaf-bf5d-eea999fdbd98"
            extId = "Project"
            name = "Project"
            description = "Test project"
        }
    '''.stripIndent()

    static final String INVALID_SETTINGS_FILE = '''
        package Project

        import jetbrains.buildServer.configs.kotlin.v10.*

        version = "10.0"
        project {
            extId = "Project"
            name = "Project"
            description = "Test project"
        }
    '''.stripIndent()

    static final String PROJECT_CONFIG_PATH = 'build/generated-configs/Project/project-config.xml'

    static final String DSL_EXCEPTION_PATH = 'build/generated-configs/dsl_exception.xml'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile

    private List<String> arguments

    @Before
    void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")
        arguments = ['-S', 'generateConfiguration', '-Pserver=' + System.properties['teamcity.server.host']]
    }

    @Test
    void 'generate configuration of TeamCity settings'() {
        buildFile << BUILD_SCRIPT

        File projectDir = testProjectDir.newFolder('.teamcity', 'Project')
        File settingsFile = new File(projectDir, 'settings.kts')
        settingsFile << VALID_SETTINGS_FILE

        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .build()

        assertEquals(result.task(":generateConfiguration").getOutcome(), SUCCESS)
        File projectFile = new File(testProjectDir.root, PROJECT_CONFIG_PATH)
        assertTrue(projectFile.exists())
        File exceptionFile = new File(testProjectDir.root, DSL_EXCEPTION_PATH)
        assertFalse(exceptionFile.exists())
    }

    @Test
    void 'generate configuration fails with invalid settings'() {
        buildFile << BUILD_SCRIPT

        File projectDir = testProjectDir.newFolder('.teamcity', 'Project')
        File settingsFile = new File(projectDir, 'settings.kts')
        settingsFile << INVALID_SETTINGS_FILE

        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .buildAndFail()

        assertEquals(FAILED, result.task(":generateConfiguration").getOutcome())
        File projectFile = new File(testProjectDir.root, PROJECT_CONFIG_PATH)
        assertFalse(projectFile.exists())
        File exceptionFile = new File(testProjectDir.root, DSL_EXCEPTION_PATH)
        assertTrue(exceptionFile.exists())
    }

    @Test
    void 'generate configuration task removes dsl exception file from previous run'() {
        buildFile << BUILD_SCRIPT

        File projectDir = testProjectDir.newFolder('.teamcity', 'Project')
        File settingsFile = new File(projectDir, 'settings.kts')
        settingsFile << INVALID_SETTINGS_FILE

        GradleRunner.create()
                .forwardOutput()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .buildAndFail()

        File exceptionFile = new File(testProjectDir.root, DSL_EXCEPTION_PATH)
        assertTrue(exceptionFile.exists())

        settingsFile.text = VALID_SETTINGS_FILE

        GradleRunner.create()
                .forwardOutput()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .build()

        assertFalse(exceptionFile.exists())
    }
}
