import Dependencies.Libraries._

ThisBuild / scalaVersion     := "2.13.14"
ThisBuild / organization     := "com.github.leonhardtdavid"
ThisBuild / organizationName := "leonhardtdavid"

lazy val root = (project in file("."))
  .settings(
    name := "dummychain"
  )
  .aggregate(service)

lazy val service = (project in file("modules/service"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name             := "dummychain-service",
    buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.github.leonhardtdavid.dummychain.service",
    libraryDependencies ++= Seq(
      tapirCore,
      tapirHttp4s,
      tapirCirce,
      tapirRefined,
      tapirSwaggerUI,
      http4sServer,
      pureconfig,
      refinedCore,
      refinedCats,
      refinedPureConfig,
      logback,
      bouncyCastle,
      tapirStubServer % Test,
      scalatest       % Test
    ),
    Compile / run / fork := true
  )
