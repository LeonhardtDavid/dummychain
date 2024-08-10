import Dependencies.Libraries._

ThisBuild / scalaVersion     := "2.13.14"
ThisBuild / organization     := "com.github.leonhardtdavid"
ThisBuild / organizationName := "leonhardtdavid"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name             := "dummychain",
    buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.github.leonhardtdavid.dummychain",
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
      tapirStubServer % Test,
      scalatest       % Test
    ),
    Compile / run / fork := true
  )
