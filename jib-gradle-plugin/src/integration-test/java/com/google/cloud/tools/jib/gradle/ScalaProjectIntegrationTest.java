/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.jib.gradle;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.jib.Command;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.junit.ClassRule;
import org.junit.Test;

/** Integration tests for building Scala projects. */
public class ScalaProjectIntegrationTest {

  @ClassRule
  public static final TestProject scalaTestProject =
      new TestProject("scala-repro").withGradleVersion("9.2.1");

  @Test
  public void testBuild_scala() throws IOException, InterruptedException {
    String targetImage = "scalaimage:gradle" + System.nanoTime();

    // Pass parameters for Scala and Java versions if provided via system properties
    // This allows the test runner to control the versions.
    String scalaVersion = System.getProperty("jib.test.scalaVersion", "2.13.12");
    String javaVersion = System.getProperty("jib.test.javaVersion", "17");
    String mainType = System.getProperty("jib.test.mainType", "extends-app");

    Path sourceFile =
        scalaTestProject.getProjectRoot().resolve("src/main/scala/example/Main.scala");
    Files.createDirectories(sourceFile.getParent());
    String sourceContent = "";
    if ("extends-app".equals(mainType)) {
      sourceContent =
          "package example\n\nobject Main extends App {\n  println(\"Hello World\")\n}";
    } else if ("explicit-main".equals(mainType)) {
      sourceContent =
          "package example\n\nobject Main {\n  def main(args: Array[String]): Unit = println(\"Hello World\")\n}";
    } else if ("annotation-main".equals(mainType)) {
      sourceContent = "package example\n\n@main def run(): Unit = println(\"Hello World\")";
    }
    Files.write(sourceFile, sourceContent.getBytes(StandardCharsets.UTF_8));

    BuildResult buildResult =
        scalaTestProject.build(
            "clean",
            "jibDockerBuild",
            "-PscalaVersion=" + scalaVersion,
            "-PjavaVersion=" + javaVersion,
            "-Djib.console=plain",
            "-D_TARGET_IMAGE=" + targetImage);

    JibRunHelper.assertBuildSuccess(buildResult, "jibDockerBuild", "Built image to Docker daemon as ");
    assertThat(buildResult.getOutput()).contains(targetImage);

    // Verify the container runs
    String output = new Command("docker", "run", "--rm", targetImage).run();
    assertThat(output).contains("Hello World");
  }
}
