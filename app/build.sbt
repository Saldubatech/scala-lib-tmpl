import CustomKeys.localConfig

enablePlugins(
  //  WebsitePlugin,
  ZioSbtEcosystemPlugin,
  ZioSbtCiPlugin,
  JavaAppPackaging
)

name    := "app"
version := "1.0.0-SNAPSHOT"

Compile / run / fork := true
Test / run / fork    := true
run / envVars        += "DB_PASSWORD" -> localConfig.value.fold("")(_.getString("DB_PASSWORD"))
run / envVars        += "DB_PORT"     -> localConfig.value.fold("")(_.getString("DB_PORT"))

dependencyOverrides += "org.slf4j" % "slf4j-api" % "2.0.9"
libraryDependencies ++= Seq(
  Dependencies.Zio.Runtime.quillJdbcZio,
  Dependencies.Zio.Runtime.quillCaliban,
  Dependencies.Persistence.postgres,
  Dependencies.Zio.Runtime.zio,
  Dependencies.Zio.Runtime.streams,
  Dependencies.Zio.Runtime.http,
  Dependencies.Zio.Runtime.config,
  Dependencies.Zio.Runtime.configMagnolia,
  Dependencies.Zio.Runtime.configTypesafe,
  Dependencies.Zio.Runtime.json,
  // Dependencies.Persistence.flywayDb,
  Dependencies.Persistence.flywayPostgres,

  // logging
//  Dependencies.Zio.Runtime.logging,
//  Dependencies.Zio.Runtime.sl4jBridge,
  Dependencies.Zio.Runtime.slf4j,
  Dependencies.Logging.sl4jSimple,
//  Dependencies.Logging.logbackClassic,
//  Dependencies.Logging.logbackCore,

  // test
  Dependencies.Logging.sl4jSimple         % Test,
  Dependencies.Zio.Testing.zio            % Test,
  Dependencies.Zio.Testing.sbt            % Test,
  Dependencies.Zio.Testing.junit          % Test,
  Dependencies.Zio.Testing.mock           % Test,
  Dependencies.Testing.containersPostgres % Test,
  Dependencies.Zio.Testing.magnolia       % Test
)
excludeDependencies += ExclusionRule("org.apache.logging.log4j", "log4j-slf4j2-impl")

testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

assembly / mainClass := Some("com.example.Boot")

assemblyMergeStrategy := {
  // This is needed to ensure that Flyway includes the Postgres extensions when starting up.
  case PathList("META-INF", "services", "org.flywaydb.core.extensibility.Plugin") => MergeStrategy.concat
  case PathList("META-INF", "MANIFEST.MF")                                        => MergeStrategy.discard
  case PathList("META-INF", "DUMMY.SF")                                           => MergeStrategy.discard // introduces a weird signature that breaks the jar
  case x                                                                          => MergeStrategy.preferProject
}
