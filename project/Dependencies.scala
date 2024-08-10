import sbt._

object Dependencies {

  object V {
    val tapir        = "1.11.1"
    val http4s       = "0.23.27"
    val pureconfig   = "0.17.7"
    val refined      = "0.11.2"
    val logback      = "1.5.6"
    val bouncyCastle = "1.78.1"
    val scalatest    = "3.2.19"
  }

  object Libraries {
    val tapirCore       = "com.softwaremill.sttp.tapir" %% "tapir-core"              % V.tapir
    val tapirHttp4s     = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % V.tapir
    val tapirCirce      = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % V.tapir
    val tapirRefined    = "com.softwaremill.sttp.tapir" %% "tapir-refined"           % V.tapir
    val tapirSwaggerUI  = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapir
    val tapirStubServer = "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"  % V.tapir

    val http4sServer = "org.http4s" %% "http4s-ember-server" % V.http4s

    val pureconfig = "com.github.pureconfig" %% "pureconfig" % V.pureconfig

    val refinedCore       = "eu.timepit" %% "refined"            % V.refined
    val refinedCats       = "eu.timepit" %% "refined-cats"       % V.refined
    val refinedPureConfig = "eu.timepit" %% "refined-pureconfig" % V.refined

    val logback = "ch.qos.logback" % "logback-classic" % V.logback

    val bouncyCastle = "org.bouncycastle" % "bcprov-jdk18on" % V.bouncyCastle

    val scalatest = "org.scalatest" %% "scalatest" % V.scalatest
  }

}
