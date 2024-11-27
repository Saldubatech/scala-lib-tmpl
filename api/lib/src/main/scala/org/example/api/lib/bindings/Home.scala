package org.example.api.lib.bindings

import zio.http.{Method, RoutePattern}
import com.typesafe.config
import zio.config.*
import zio.{Chunk, Config, RLayer, ZIO, ZLayer}
import zio.config.typesafe.TypesafeConfigProvider
import zio.config.magnolia.*

import scala.util.Try

case class Home(name: String, protocol: Home.Protocol, host: Home.HostName, port: Home.PortNumber, path: Option[Home.Path]):

  val url = {
    val u = s"${protocol.l4}://$host:$port/"
    path match
      case None    => u
      case Some(p) => u + p.mkString(Home.pathSeparator)
  }

end Home // case class

object Home:

  enum Protocol(val l4: String):

    case OAS3  extends Protocol("http")
    case OAS3s extends Protocol("https")
    case HTTP  extends Protocol("http")
    case HTTPS extends Protocol("https")
    case REST  extends Protocol("http")
    case RESTs extends Protocol("https")
    case GRPC  extends Protocol("http")
    case GRPCS extends Protocol("https")

  end Protocol

  type HostName   = String
  type PortNumber = Int
  type Path       = List[String]
  val pathSeparator = "/"

  case class Configuration(protocol: String, host: String, port: Int, path: Option[String]) // , path: Option[String])
  type AllHomesConfiguration = Map[String, Configuration]

  private val allHomesConfigurationDef = deriveConfig[AllHomesConfiguration]

  def allHomesConfig(path: String = "homes"): Config[AllHomesConfiguration] = Config.Nested(path, allHomesConfigurationDef)

  def allHomesConfigLayer(path: String = "homes"): ZLayer[config.Config, Config.Error, AllHomesConfiguration] =
    ZLayer(
      for {
        rootConfig <- ZIO.service[config.Config]
        rs         <- read(allHomesConfig(path).from(TypesafeConfigProvider.fromTypesafeConfig(rootConfig)))
      } yield rs
    )

  private val homeConfigDef: Config[Configuration] = deriveConfig[Configuration]

  def homeConfig(name: String): Config[Configuration] = Config.Nested(name, homeConfigDef)

  def homeConfigLayer(name: String): ZLayer[AllHomesConfiguration, Config.Error, Configuration] =
    ZLayer(
      for {
        allHomes <- ZIO.service[AllHomesConfiguration]
        rs <- allHomes.get(name) match
                case None    => ZIO.fail(Config.Error.MissingData(Chunk(name), s"$name not found in Homes configuration"))
                case Some(c) => ZIO.succeed(c)
      } yield rs
    )

  val homeLayer: RLayer[Configuration, Home] = ZLayer(
    for {
      cfg <- ZIO.service[Configuration]
      rs <- ZIO.fromTry(
              Try(cfg.path match
                case None    => Home("", Protocol.valueOf(cfg.protocol), cfg.host, cfg.port, None)
                case Some(p) => Home("", Protocol.valueOf(cfg.protocol), cfg.host, cfg.port, Some(p.split(pathSeparator).toList)))
            )
    } yield rs
  )

end Home // object
