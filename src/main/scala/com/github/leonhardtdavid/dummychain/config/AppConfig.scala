package com.github.leonhardtdavid.dummychain.config

import cats.effect.kernel.{ Resource, Sync }
import AppConfig._
import eu.timepit.refined.api._
import eu.timepit.refined.boolean._
import eu.timepit.refined.numeric._
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.string._
import pureconfig._
import pureconfig.generic.auto._

case class ServerConfig(host: Host, port: Port)

case class AppConfig(server: ServerConfig, docsEnabled: Boolean)

object AppConfig {

  type Host = String Refined Or[IPv4, IPv6]
  type Port = Int Refined Positive

  def resource[F[_]: Sync]: Resource[F, AppConfig] =
    Resource.eval(Sync[F].delay(ConfigSource.default.loadOrThrow[AppConfig]))

}
