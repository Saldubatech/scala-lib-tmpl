import CustomKeys.localConfig

enablePlugins(
  //  WebsitePlugin,
  ZioSbtEcosystemPlugin,
  ZioSbtCiPlugin,
  JavaAppPackaging
)

name := "tenant-api"

Compile / run / fork := true
Test / run / fork    := true
run / envVars        += "DB_PASSWORD" -> localConfig.value.fold("")(_.getString("DB_PASSWORD"))
run / envVars        += "DB_PORT"     -> localConfig.value.fold("")(_.getString("DB_PORT"))

dependencyOverrides += "org.slf4j" % "slf4j-api" % "2.0.9"

val zioRuntimeDeps = {
  import Dependencies.Zio.Runtime.*
  Seq(quillJdbcZio, quillCaliban, zio, streams, http, config, configTypesafe, json, slf4j)
}

val tapirDeps = {
  import Dependencies.Tapir.*
  Seq(zio, zioHttp, circe)
}

libraryDependencies ++= tapirDeps ++ zioRuntimeDeps ++ Seq(
  Dependencies.Persistence.postgres,

  // logging
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

testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

assembly / mainClass := Some("com.example.Boot")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case x                                   => MergeStrategy.preferProject
}
