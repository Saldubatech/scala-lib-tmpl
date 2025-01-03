import sbt.*
object Dependencies {

  val lastUpdated              = "20240606"
  val scalaVersion             = "3.3.1"
  def useFrom2_13(m: ModuleID) = m.cross(CrossVersion.for3Use2_13)

  object Lang {
    object Refined {

      // https://static.javadoc.io/eu.timepit/refined_2.12/0.11.1/eu/timepit/refined/index.html
      // https://github.com/fthomas/refined
      // https://mvnrepository.com/artifact/eu.timepit/refined
      val refinedVersion = "0.11.1"
      val refined        = "eu.timepit" %% "refined"            % refinedVersion
      val cats           = "eu.timepit" %% "refined-cats"       % refinedVersion // optional
      val eval           = "eu.timepit" %% "refined-eval"       % refinedVersion // optional, JVM-only
      val jsonPath       = "eu.timepit" %% "refined-jsonpath"   % refinedVersion // optional, JVM-only
      val pureConfig     = "eu.timepit" %% "refined-pureconfig" % refinedVersion // optional, JVM-only
      val scalaCheck     = "eu.timepit" %% "refined-scalacheck" % refinedVersion // optional
      val scalaz         = "eu.timepit" %% "refined-scalaz"     % refinedVersion // optional
      val refinedSCodec  = "eu.timepit" %% "refined-scodec"     % refinedVersion // optional
      val scopt          = "eu.timepit" %% "refined-scopt"      % refinedVersion // optional
      val shapeless      = "eu.timepit" %% "refined-shapeless"  % refinedVersion // optional

    }
  }

  object Cats {

    val catsVersion = "2.9.0"
    // https://mvnrepository.com/artifact/org.typelevel/cats-core
    val core = "org.typelevel" %% "cats-core" % catsVersion
    // https://mvnrepository.com/artifact/org.typelevel/alleycats-core
    val alley = "org.typelevel" %% "alleycats-core" % catsVersion
    // https://mvnrepository.com/artifact/org.typelevel/algebra
    val algebra = "org.typelevel" %% "algebra" % catsVersion

    // https://mvnrepository.com/artifact/org.typelevel/cats-effect
    val effectsVersion = "3.6-0142603"
    val effect         = "org.typelevel" %% "cats-effect" % effectsVersion

    val kittensVersion = "3.3.0"
    val kittens        = "org.typelevel" %% "kittens" % kittensVersion

  }

  object ApacheCommons {

    // https://mvnrepository.com/artifact/org.apache.commons/commons-math4-core
    val mathVersion = "3.6.1"
    val math        = "org.apache.commons" % "commons-math3" % mathVersion

  }
  object Zio {

    val zioVersion = "2.1.1"
    object Runtime {

      // ZIO Ecosystem
      val zioJsonVersion   = "0.7.3" // "0.6.2" 20241217
      val zioConfigVersion = "4.0.2" // "4.0.0-RC16"
      val zioHttpVersion   = "3.0.1" // "3.0.0-RC8" // Upgrade when ready to put effort in HTTP layer, to update the samples.
      val quillVersion     = "4.8.6" // "4.8.5"

//      val quillCore = "io.getquill" %% "quill-core" % quillVersion ONLY FOR SCALA 2!!!!
      val quillJdbcZio =
        ("io.getquill" %% "quill-jdbc-zio" % quillVersion).excludeAll(ExclusionRule(organization = "org.scala-lang.modules"))

      val quillJdbc    = "io.getquill" %% "quill-jdbc"    % quillVersion
      val quillCaliban = "io.getquill" %% "quill-caliban" % quillVersion

      // https://github.com/ScalaConsultants/zio-slick-interop
      // This is a very small library that may be worth copying/onboarding. (MIT License)
      // NOT AVAILABLE val slickInterop = "io.scalac" %% "zio-slick-interop"  % "0.4.0"
      val reactiveStreamsInterop = "dev.zio" %% "zio-interop-reactivestreams" % "2.0.2"

      val zio            = "dev.zio" %% "zio"                 % zioVersion
      val streams        = "dev.zio" %% "zio-streams"         % zioVersion
      val http           = "dev.zio" %% "zio-http"            % zioHttpVersion
      val config         = "dev.zio" %% "zio-config"          % zioConfigVersion
      val configMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
      val configTypesafe = "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
      val json           = "dev.zio" %% "zio-json"            % zioJsonVersion

      // logging
      val zioLoggingVersion = "2.3.0" // "2.1.15"
      val logging           = "dev.zio" %% "zio-logging"               % zioLoggingVersion
      val sl4jBridge        = "dev.zio" %% "zio-logging-slf4j2-bridge" % zioLoggingVersion
      val slf4j             = "dev.zio" %% "zio-logging-slf4j2"        % zioLoggingVersion

    }
    object Ecosystem {

      // https://mvnrepository.com/artifact/dev.zio/zio-prelude
      // https://zio.dev/zio-prelude/
      val preludeVersion   = "1.0.0-RC27"
      val prelude          = "dev.zio" %% "zio-prelude"           % preludeVersion
      val schemaVersion    = "1.2.0" // "0.4.15"
      val schema           = "dev.zio" %% "zio-schema"            % schemaVersion
      val schemaAvro       = "dev.zio" %% "zio-schema-avro"       % schemaVersion
      val schemaJson       = "dev.zio" %% "zio-schema-json"       % schemaVersion
      val schemaBson       = "dev.zio" %% "zio-schema-bson"       % schemaVersion
      val schemaMsgPack    = "dev.zio" %% "zio-schema-msg-pack"   % schemaVersion
      val schemaProto      = "dev.zio" %% "zio-schema-protobuf"   % schemaVersion
      val schemaThrift     = "dev.zio" %% "zio-schema-thrift"     % schemaVersion
      val schemaDerivation = "dev.zio" %% "zio-schema-derivation" % schemaVersion
      val schemaOptics     = "dev.zio" %% "zio-schema-optics"     % schemaVersion
      val schemaTest       = "dev.zio" %% "zio-schema-zio-test"   % schemaVersion

      val opticsVersion = "0.2.1" // Pending release of 2.0.0
      val optics        = "dev.zio" %% "zio-optics" % opticsVersion

      // https://mvnrepository.com/artifact/dev.zio/zio-interop-cats
      val interopCatsVersion = "23.1.0.2"
      val interopCats        = "dev.zio" %% "zio-interop-cats" % interopCatsVersion

      // https://mvnrepository.com/artifact/dev.zio/zio-actors
      // NOT AVAILABLE FOR SCALA 3!!!
      // val zioActorsVersion =  "0.1.0"
      // val actors = "dev.zio" %% "zio-actors" % zioActorsVersion
    }

    object Testing {

      val zioMockVersion = "1.0.0-RC12"
      val zio            = "dev.zio" %% "zio-test"          % zioVersion
      val sbt            = "dev.zio" %% "zio-test-sbt"      % zioVersion
      val junit          = "dev.zio" %% "zio-test-junit"    % zioVersion
      val mock           = "dev.zio" %% "zio-mock"          % zioMockVersion
      val magnolia       = "dev.zio" %% "zio-test-magnolia" % zioVersion

    }

  }

  object Tapir {

    // https://tapir.softwaremill.com/en/latest/index.html

    val tapir   = "com.softwaremill.sttp.tapir"
    val version = "1.11.10"

    val core = tapir %% "tapir-core" % version

    val zio = tapir %% "tapir-zio" % version
    // https://mvnrepository.com/artifact/com.softwaremill.sttp.tapir/tapir-zio-http-server
    val zioHttp = tapir %% "tapir-zio-http-server" % version
    // https://mvnrepository.com/artifact/com.softwaremill.sttp.tapir/tapir-json-circe
    val circe = tapir %% "tapir-json-circe" % version

  }

  object Pekko {

    // https://mvnrepository.com/artifact/org.apache.pekko/pekko-actor
    val pekkoVersion = "1.1.2"
    val actor        = "org.apache.pekko" %% "pekko-actor-typed"         % pekkoVersion
    val test         = "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion

  }

  object Magnolia {

    val version  = "1.3.8"
    val magnolia = "com.softaremill.magnolia1_3" %% "magnolia" % version

  }
  object Circe {

    val version = "0.14.10"
    val core    = "io.circe" %% "circe-core"    % version
    val generic = "io.circe" %% "circe-generic" % version
    val parser  = "io.circe" %% "circe-parser"  % version

  }

  object Spark {

    val version = "4.0.0-preview1"

    // https://mvnrepository.com/artifact/org.apache.spark/spark-mllib
    val mlLib = useFrom2_13(
      ("org.apache.spark" %% "spark-mllib" % version)
        .excludeAll(
          ExclusionRule(organization = "org.scala-lang.modules"),
          ExclusionRule(organization = "org.typelevel")
        )
    )

  }

  object Logging {

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    val sl4jVersion    = "2.0.13" // "2.0.9"
    val sl4jApi        = "org.slf4j"      % "slf4j-api"       % sl4jVersion
    val sl4jSimple     = "org.slf4j"      % "slf4j-simple"    % sl4jVersion
    val logbackVersion = "1.5.6"
    val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
    val logbackCore    = "ch.qos.logback" % "logback-core"    % logbackVersion

  }
  object Persistence {

    // Slick
    val slickVersion   = "3.5.1"
    val slick          = "com.typesafe.slick"  %% "slick"               % slickVersion
    val slickHikari    = "com.typesafe.slick"  %% "slick-hikaricp"      % slickVersion
    val slickPgVersion = "0.22.2"
    val slickPg        = "com.github.tminglei" %% "slick-pg"            % slickPgVersion
    val pgCirce        = "com.github.tminglei" %% "slick-pg_circe-json" % slickPgVersion

    val slickTest = "com.typesafe.slick" %% "slick-testkit" % slickVersion

    // https://flywaydb.org/
    // https://alexn.org/blog/2020/11/15/managing-database-migrations-scala/
    val flywayDbVersion = "11.1.0" // "10.19.0" 20241218
    // val flywayDb        = "org.flywaydb" % "flyway-core" % flywayDbVersion
    // https://mvnrepository.com/artifact/org.flywaydb/flyway-database-postgresql
    val flywayPostgres = "org.flywaydb" % "flyway-database-postgresql" % flywayDbVersion // % "runtime"

    val postgresqlVersion = "42.7.4" // "42.7.4" 20241219
    val postgres          = "org.postgresql" % "postgresql" % postgresqlVersion

  }

  object Testing {

    val containersPostgresVersion = "0.41.5" // "0.41.3" 20241220

    val containersPostgres =
      "com.dimafeng" %% "testcontainers-scala-postgresql" % containersPostgresVersion

    val scalaTestVersion = "3.2.18"
    val scalaTest        = "org.scalatest" %% "scalatest" % scalaTestVersion
    val scalactic        = "org.scalactic" %% "scalactic" % scalaTestVersion

  }

}
