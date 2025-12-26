plugins {
  scala
  id("com.google.cloud.tools.jib")
}

repositories {
  mavenCentral()
}

val scalaVer = property("scalaVersion").toString()
val javaVer = property("javaVersion").toString().toInt()

scala {
  scalaVersion = scalaVer
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVer))
  }
}

jib {
  to {
    image = System.getProperty("_TARGET_IMAGE") ?: "scala-test"
  }
}
