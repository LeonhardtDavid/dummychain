package com.github.leonhardtdavid.dummychain.service

import cats.effect.{ Async, Resource }
import com.comcast.ip4s._
import fs2.io.net.Network
import io.circe.Json
import io.circe.syntax._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{ Router, Server }
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{ Http4sServerInterpreter, Http4sServerOptions }
import sttp.tapir.server.model.ValuedEndpointOutput

object Server {

  def resource[F[_]: Async: Network](routes: List[ServerEndpoint[Any, F]], config: ServerConfig): Resource[F, Server] = {
    val serverOptions = Http4sServerOptions
      .customiseInterceptors[F]
      .defaultHandlers { msg =>
        ValuedEndpointOutput(jsonBody[Json], Json.obj("error" -> msg.asJson))
      }
      .options

    EmberServerBuilder
      .default[F]
      .withHost(Host.fromString(config.host.value).get) // Using get is safe thanks to refined types
      .withPort(Port.fromInt(config.port.value).get)
      .withHttpApp(
        Router[F]("/" -> Http4sServerInterpreter[F](serverOptions).toRoutes(routes)).orNotFound
      )
      .build
  }

}
