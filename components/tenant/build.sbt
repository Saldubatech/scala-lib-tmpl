import CustomKeys.localConfig

enablePlugins(
  //  WebsitePlugin,
  ZioSbtEcosystemPlugin,
  ZioSbtCiPlugin,
  JavaAppPackaging
)

name := "tenant-component"

Compile / run / fork := true
Test / run / fork    := true
run / envVars        += "DB_PASSWORD" -> localConfig.value.fold("")(_.getString("DB_PASSWORD"))
run / envVars        += "DB_PORT"     -> localConfig.value.fold("")(_.getString("DB_PORT"))
val wkw = ExclusionRule()

val zioRuntimeDeps = {
  import Dependencies.Zio.Runtime.*
  Seq(
    quillJdbcZio, quillCaliban, zio, streams, http, config, configMagnolia, configTypesafe, json, slf4j
  )
}

val tapirDeps = {
  import Dependencies.Tapir.*
  Seq(zio, zioHttp, circe)
}

dependencyOverrides += "org.slf4j" % "slf4j-api" % "2.0.9"
libraryDependencies ++= zioRuntimeDeps ++ tapirDeps ++ Seq(
  Dependencies.Persistence.postgres,

  // logging
//  Dependencies.Zio.Runtime.logging,
//  Dependencies.Zio.Runtime.sl4jBridge,
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
