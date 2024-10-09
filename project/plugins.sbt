val zioSbtVersion = "0.4.0-alpha.28"

addSbtPlugin("dev.zio" % "zio-sbt-ecosystem" % zioSbtVersion)
addSbtPlugin("dev.zio" % "zio-sbt-ci"        % zioSbtVersion)
addSbtPlugin("dev.zio" % "zio-sbt-website"   % zioSbtVersion)

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix"  % "0.13.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt"  % "2.5.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.1.1")
addSbtPlugin("org.typelevel" % "sbt-tpolecat"  % "0.5.2")

resolvers ++= Resolver.sonatypeOssRepos("public")
