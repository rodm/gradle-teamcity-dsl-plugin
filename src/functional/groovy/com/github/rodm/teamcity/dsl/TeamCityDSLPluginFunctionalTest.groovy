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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class TeamCityDSLPluginFunctionalTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile

    @Before
    void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    @Test
    void 'generate configuration of TeamCity settings'() {
        buildFile << '''
            plugins {
                id 'com.github.rodm.teamcity-dsl'
            }

            repositories {
                maven {
                    url "http://${server}:8111/app/dsl-plugins-repository"
                }
            }
        '''

        File projectDir = testProjectDir.newFolder('.teamcity', 'Project')
        File settingsFile = new File(projectDir, 'settings.kts')
        settingsFile << '''
            package Project

            import jetbrains.buildServer.configs.kotlin.v10.*

            version = "10.0"
            project {
                uuid = "2c4c777e-8e46-4eaf-bf5d-eea999fdbd98"
                extId = "Project"
                name = "Project"
                description = "Test project"
            }
        '''

        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(['-S', 'generateConfiguration', '-Pserver=' + System.properties['teamcity.server.host']])
                .withPluginClasspath()
                .build()

        assertEquals(result.task(":generateConfiguration").getOutcome(), SUCCESS)
        File projectFile = new File(testProjectDir.root, 'build/generated-configs/Project/project-config.xml')
        assertTrue(projectFile.exists())
        File exceptionFile = new File(testProjectDir.root, 'build/generated-configs/dsl_exception.xml')
        assertFalse(exceptionFile.exists())
    }
}
