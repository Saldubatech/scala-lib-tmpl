import CustomKeys.localConfig
import sbt.internal.IvyConsole

enablePlugins(
  //  WebsitePlugin,
  ZioSbtEcosystemPlugin,
  ZioSbtCiPlugin,
  JavaAppPackaging
)

//scalacOptions += "-explain"

name := "salduba-platform"

Compile / run / fork := true
Test / run / fork    := true
Test / logBuffered   := false
run / envVars        += "DB_PASSWORD" -> localConfig.value.fold("")(_.getString("DB_PASSWORD"))
run / envVars        += "DB_PORT"     -> localConfig.value.fold("")(_.getString("DB_PORT"))

//dependencyOverrides  += "org.slf4j" % "slf4j-api" % "2.0.9"
libraryDependencies ++= Seq()
excludeDependencies  += ExclusionRule("org.apache.logging.log4j", "log4j-slf4j2-impl")
publish / skip       := false
//testFrameworks      ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

val PACKAGES_TOKEN_VAR = "GH_PUBLISH_TO_PACKAGES"

//GhPackages.credentials("jmpicnic", PACKAGES_TOKEN_VAR).foreach( cred => credentials += cred)

val ghCredentials: Seq[Credentials] = GhPackages.credentials("jmpicnic", PACKAGES_TOKEN_VAR) match {
  case None       => Seq()
  case Some(cred) => Seq(cred)
}

credentials ++= ghCredentials

// Configure publishing settings
publishTo := Some(GhPackages.repo)
