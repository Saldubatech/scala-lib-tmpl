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
  JavaAppPackaging,
  DockerPlugin
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
    scalacOptions             ++= Seq("-explain", "-Yretain-trees"),
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
val silencerVersion = "1.7.14"

// This is needed just to provide the annotations that are no longer needed with scala 3 per https://github.com/ghik/silencer
ThisBuild / libraryDependencies ++= Seq(
//r  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib_2.13.11" % silencerVersion // % Provided cross CrossVersion.full
)

/*
@startuml
title
= Project Dependencies
end title

component Lib
component Platform
Platform --> Lib
'package Api {
'  component Api.Lib
'  component Api.Types
'  Api.Lib --> Api.Types
'}
package Tenant {
  portin in
  component Tenant.Component
  component Tenant.Api
  component Tenant.Domain
  component Tenant.Impl
  portout out
  in --> Tenant.Component
  Tenant.Component --> Tenant.Api
  Tenant.Component --> Tenant.Impl
  Tenant.Api --> Tenant.Domain
  Tenant.Impl --> Tenant.Domain

  Tenant.Domain -[hidden]-> out

}
out --> Platform
'package Common {
'  component Common.Domain
'}
component App

App --> in
App --> Platform
@enduml
 */

val libProject      = project in file("lib")
val platformProject = (project in file("salduba-platform")).dependsOn(libProject)

//val apiDir          = file("api")
//val libApiProject   = (project in apiDir / "lib").dependsOn(libProject)
//val typesApiProject = (project in apiDir / "types").dependsOn(libProject, libApiProject)

val componentsDir = file("components")

//val commonDir           = componentsDir / "common"
//val commonDomainProject = (project in commonDir / "domain").dependsOn(libProject)

val tenantDir = componentsDir / "tenant"

val tenantDomainProject =
  (project in tenantDir / "domain").dependsOn(platformProject)

val tenantImplProject =
  (project in tenantDir / "implementation")
    .dependsOn(tenantDomainProject, libProject)

val tenantApiProject =
  (project in tenantDir / "api")
    .dependsOn(tenantDomainProject, platformProject)

val tenantComponentProject = (project in tenantDir / "component").dependsOn(tenantApiProject, tenantImplProject)

val appProject = (project in file("app")).dependsOn(tenantComponentProject, platformProject)

lazy val root = (project in file("."))
  .settings(
    name            := "root-composite",
    testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .aggregate(appProject, tenantComponentProject, tenantApiProject, tenantDomainProject, tenantImplProject, platformProject,
    libProject)
