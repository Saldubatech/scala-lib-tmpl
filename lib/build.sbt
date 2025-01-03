import CustomKeys.localConfig
import sbt.internal.IvyConsole

enablePlugins(
  //  WebsitePlugin,
  ZioSbtEcosystemPlugin,
  ZioSbtCiPlugin,
  JavaAppPackaging
)

//scalacOptions += "-explain"

name    := "lib"
version := "1.0.0.alpha0"

Compile / run / fork := true
Test / run / fork    := true
Test / logBuffered   := false
run / envVars        += "DB_PASSWORD" -> localConfig.value.fold("")(_.getString("DB_PASSWORD"))
run / envVars        += "DB_PORT"     -> localConfig.value.fold("")(_.getString("DB_PORT"))

//scalacOptions += "-Xcheck-macros"
//scalacOptions += "-explain -Xcheck-macros"

val tapirDeps = {
  import Dependencies.Tapir.*
  Seq(zio, zioHttp, circe)
}

val zioDeps = {
  import Dependencies.Zio.*
  Seq(
    // Schema & Optics
    Ecosystem.optics, Ecosystem.schema, Ecosystem.schemaJson, Ecosystem.schemaDerivation, Ecosystem.schemaOptics,

    // ZIO Runtime
    Runtime.zio,
    // Needed to access the "Chunk" type.
    // Runtime.streams,
    Runtime.http, Runtime.config, Runtime.configTypesafe, Runtime.json, Runtime.reactiveStreamsInterop,

    // Persistence
    Runtime.quillJdbcZio // , Runtime.quillCaliban
  )
}

dependencyOverrides += "org.slf4j" % "slf4j-api" % "2.0.9"
libraryDependencies ++= zioDeps ++ tapirDeps ++ Seq(
  // Basic Utilities
  // sl4j Core
  Dependencies.Logging.logbackCore,
  Dependencies.Logging.sl4jApi,
  // Basic Log Implementation
  // This needs to move to Test when ready for "production"
  Dependencies.Logging.logbackClassic,
  // Apache Math
  Dependencies.ApacheCommons.math,

  // Cats Functional Types
//  Dependencies.Cats.core,
//  Dependencies.Cats.alley,
//  Dependencies.Cats.kittens,
//  Dependencies.Cats.algebra,
  // Dependencies.Cats.effect

  // Magnolia
  // Dependencies.Magnolia.magnolia,

  // Circe
  Dependencies.Circe.core,
  Dependencies.Circe.generic,
  Dependencies.Circe.parser,

  // Actors
//  Dependencies.Pekko.actor,

  // Math, etc...
  Dependencies.Spark.mlLib,

  // logging
  //  Dependencies.Zio.Runtime.logging,
  //  Dependencies.Zio.Runtime.sl4jBridge,
  Dependencies.Zio.Runtime.slf4j,
  //  Dependencies.Logging.sl4jSimple,
  //  Dependencies.Logging.logbackClassic,
  //  Dependencies.Logging.logbackCore,

  // Persistence
  Dependencies.Persistence.postgres,
  Dependencies.Persistence.slick,
  Dependencies.Persistence.slickPg,
  Dependencies.Persistence.pgCirce,
  Dependencies.Persistence.slickHikari,
  // Dependencies.Persistence.flywayDb,
  Dependencies.Persistence.flywayPostgres,

  // logging
//  Dependencies.Zio.Runtime.logging,
//  Dependencies.Zio.Runtime.sl4jBridge,
  Dependencies.Zio.Runtime.slf4j,
//  Dependencies.Logging.sl4jSimple,
//  Dependencies.Logging.logbackClassic,
//  Dependencies.Logging.logbackCore,

  // test
  // Dependencies.Logging.sl4jSimple % Test,
  Dependencies.Zio.Testing.zio   % Test,
  Dependencies.Zio.Testing.sbt   % Test,
  Dependencies.Zio.Testing.junit % Test,
  Dependencies.Zio.Testing.mock  % Test,
  Dependencies.Testing.containersPostgres, // No testing because it builds library for others.
  Dependencies.Zio.Testing.magnolia     % Test,
  Dependencies.Testing.scalactic        % Test, // Currently not used in production code.
  Dependencies.Testing.scalaTest, // Needed in production for developing tests by client libraries.
  Dependencies.Zio.Ecosystem.schemaTest % Test,
  Dependencies.Pekko.test // Could be Needed to provide library support for testing for other projects.

)
excludeDependencies += ExclusionRule("org.apache.logging.log4j", "log4j-slf4j2-impl")
publish / skip      := false
testFrameworks     ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

val PACKAGES_TOKEN_VAR = "GH_PUBLISH_TO_PACKAGES"

//GhPackages.credentials("jmpicnic", PACKAGES_TOKEN_VAR).foreach( cred => credentials += cred)
githubOwner      := "Saldubatech"
githubRepository := "packages"

val ghCredentials: Seq[Credentials] = GhPackages.credentials("jmpicnic", PACKAGES_TOKEN_VAR) match {
  case None       => Seq()
  case Some(cred) => Seq(cred)
}

credentials ++= ghCredentials

// Configure publishing settings
publishTo := Some(GhPackages.repo)
