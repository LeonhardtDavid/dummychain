import Dependencies.Libraries._

ThisBuild / scalaVersion     := "2.13.14"
ThisBuild / organization     := "com.github.leonhardtdavid"
ThisBuild / organizationName := "leonhardtdavid"

lazy val root = (project in file("."))
  .settings(
    name := "dummychain"
  )
  .aggregate(shared, helpers, service)

lazy val shared = (project in file("modules/shared"))
  .settings(
    name := "dummychain-shared",
    libraryDependencies ++= Seq(
      bouncyCastle,
      catsEffect
    )
  )

lazy val helpers = (project in file("modules/helpers"))
  .settings(
    name := "dummychain-helpers",
    libraryDependencies ++= Seq(
      catsEffect
    ),
    Compile / run / fork := true
  )
  .dependsOn(shared)

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
      circeRefined,
      pureconfig,
      refinedCore,
      refinedCats,
      refinedPureConfig,
      logback,
      tapirStubServer % Test,
      scalatest       % Test
    ),
    Compile / run / fork := true
  )
  .dependsOn(shared)
