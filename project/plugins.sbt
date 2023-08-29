val zioSbtVersion = "0.4.0-alpha.16"

addSbtPlugin("dev.zio" % "zio-sbt-ecosystem" % zioSbtVersion)
addSbtPlugin("dev.zio" % "zio-sbt-ci"        % zioSbtVersion)
addSbtPlugin("dev.zio" % "zio-sbt-website"   % zioSbtVersion)

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix"  % "0.11.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt"  % "2.5.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.8")
addSbtPlugin("org.typelevel" % "sbt-tpolecat"  % "0.5.0")

// TODO Remove when https://github.com/zio/zio-sbt/pull/264 is merged and released
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

resolvers ++= Resolver.sonatypeOssRepos("public")
