import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import CustomKeys.localConfig
import Utilities.TaskKeyOps

resolvers ++= Resolver.sonatypeOssRepos("public")
resolvers  += GhPackages.repo

enablePlugins(
  //  WebsitePlugin,
//  ZioSbtWebsitePlugin,
  ZioSbtEcosystemPlugin,
  ZioSbtCiPlugin,
  JavaAppPackaging
)

val envFileName = "env.properties"
val pFile       = new File(envFileName)
val lc          = if (pFile.exists()) Some(ConfigFactory.parseFile(pFile).resolve()) else None

inThisBuild(
  List(
    versionScheme              := Some("semver-spec"),
    localConfig                := lc,
    publish / skip             := true,
    organization               := "com.saldubatech",
    name                       := "m-service-root",
    ciUpdateReadmeJobs         := Seq.empty,
    ciReleaseJobs              := Seq.empty,
    ciPostReleaseJobs          := Seq.empty,
    ciCheckWebsiteBuildProcess := Seq.empty,
    scalaVersion               := Dependencies.scalaVersion,
//    ciTargetScalaVersions := makeTargetScalaMap(
//      `sample-app`
//    ).value,
//    ciDefaultTargetJavaVersions := Seq("8"),
    semanticdbEnabled  := true,
    semanticdbVersion  := scalafixSemanticdb.revision,
    Test / logBuffered := false,
    Test / javaOptions ++= Seq(
      "-Xmx4G",
      "-Xms1G",
      "-XX:MaxMetaspaceSize=512m"
    )
  )
)
scalacOptions += "-explain"
val silencerVersion = "1.7.14"

// This is needed just to provide the annotations that are no longer needed with scala 3 per https://github.com/ghik/silencer
ThisBuild / libraryDependencies ++= Seq(
//r  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib_2.13.11" % silencerVersion // % Provided cross CrossVersion.full
)

val libProject  = project in file("lib")
val lib2Project = (project in file("lib2")).dependsOn(libProject)
val appProject  = (project in file("app")).dependsOn(lib2Project, libProject)

lazy val root = (project in file("."))
  .settings(
    name            := "m-service-root",
    testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .aggregate(appProject, lib2Project, libProject)
